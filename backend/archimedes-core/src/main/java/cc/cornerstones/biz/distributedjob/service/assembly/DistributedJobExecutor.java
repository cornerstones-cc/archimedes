package cc.cornerstones.biz.distributedjob.service.assembly;

import cc.cornerstones.almond.constants.DatabaseConstants;
import cc.cornerstones.almond.constants.InfrastructureConstants;
import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.exceptions.AbcCapacityLimitException;
import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.utils.AbcDateUtils;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.distributedserver.service.assembly.DistributedServer;
import cc.cornerstones.biz.distributedjob.dto.DistributedJobExecutionDto;
import cc.cornerstones.biz.distributedjob.entity.DistributedJobDo;
import cc.cornerstones.biz.distributedjob.entity.DistributedJobExecutionDo;
import cc.cornerstones.biz.distributedjob.persistence.DistributedJobExecutionRepository;
import cc.cornerstones.biz.distributedjob.share.types.JobHandler;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Component
public class DistributedJobExecutor implements SmartInitializingSingleton, DisposableBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedJobExecutor.class);

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private DistributedJobExecutionRepository distributedJobExecutionRepository;

    @Autowired
    private DistributedServer distributedServer;

    @Autowired
    private ApplicationContext applicationContext;

    private ConcurrentMap<String, JobOperator> jobOperatorCache = new ConcurrentHashMap<String,
            JobOperator>();

    private final ThreadFactory NAMED_THREAD_FACTORY = new ThreadFactoryBuilder()
            .setNameFormat("d-job-%d").build();
    private final ExecutorService executorService = new ThreadPoolExecutor(
            3,
            10,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(300),
            NAMED_THREAD_FACTORY,
            new ThreadPoolExecutor.AbortPolicy());

    public DistributedJobExecutionDto execute(
            DistributedJobDo distributedJobDo) throws AbcUndefinedException {
        LOGGER.info("[d-job] rcv assignment to execute job {} ({}) on this server {} ({})", distributedJobDo.getUid(),
                distributedJobDo.getName(),
                distributedServer.getServerHostname(), distributedServer.getServerIpAddress());

        //
        // Step 1, pre-processing
        //
        Specification<DistributedJobExecutionDo> specification = new Specification<DistributedJobExecutionDo>() {
            @Override
            public Predicate toPredicate(Root<DistributedJobExecutionDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();

                predicateList.add(criteriaBuilder.equal(root.get("jobUid"), distributedJobDo.getUid()));

                predicateList.add(criteriaBuilder.or(
                        criteriaBuilder.equal(root.get("status"), JobStatusEnum.INITIALIZING),
                        criteriaBuilder.equal(root.get("status"), JobStatusEnum.CREATED),
                        criteriaBuilder.equal(root.get("status"), JobStatusEnum.RUNNING),
                        criteriaBuilder.equal(root.get("status"), JobStatusEnum.FAILING),
                        criteriaBuilder.equal(root.get("status"), JobStatusEnum.CANCELLING),
                        criteriaBuilder.equal(root.get("status"), JobStatusEnum.RESTARTING),
                        criteriaBuilder.equal(root.get("status"), JobStatusEnum.RECONCILING)));

                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        Page<DistributedJobExecutionDo> distributedJobExecutionDoPage = this.distributedJobExecutionRepository.findAll(
                specification, PageRequest.of(0, 1));
        if (!distributedJobExecutionDoPage.isEmpty()) {
            LOGGER.warn("found existing job execution in progress, ignore creating new one, job uid {}",
                    distributedJobDo.getUid());
            return null;
        }

        JobOperator jobOperator = this.jobOperatorCache.get(distributedJobDo.getHandlerName());
        if (jobOperator == null) {
            throw new AbcResourceNotFoundException(String.format("job operator of job handler %s",
                    distributedJobDo.getHandlerName()));
        }

        //
        // Step 2, core-processing
        //

        // create an instance, status is INITIALIZING
        final Long finalJobExecutionUid = this.idHelper.getNextDistributedId(DistributedJobExecutionDo.RESOURCE_NAME);
        DistributedJobExecutionDo distributedJobExecutionDo = new DistributedJobExecutionDo();
        distributedJobExecutionDo.setUid(finalJobExecutionUid);
        distributedJobExecutionDo.setStatus(JobStatusEnum.CREATED);
        distributedJobExecutionDo.setExecutorHostname(this.distributedServer.getServerHostname());
        distributedJobExecutionDo.setExecutorIpAddress(this.distributedServer.getServerIpAddress());
        distributedJobExecutionDo.setJobUid(distributedJobDo.getUid());
        BaseDo.create(distributedJobExecutionDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
        this.distributedJobExecutionRepository.save(distributedJobExecutionDo);

        try {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    // pre-processing, status transition
                    updateJobExecutionStatusDo(finalJobExecutionUid,
                            JobStatusEnum.RUNNING, null,
                            JobStatusEnum.CREATED);

                    long beginTime = System.currentTimeMillis();

                    try {
                        LOGGER.info("[d-job] job execution:{}, begin to execute job {} ({})",
                                finalJobExecutionUid, distributedJobDo.getUid(), distributedJobDo.getName());

                        // core-processing
                        jobOperator.init();
                        jobOperator.execute(distributedJobDo.getParameters());
                        jobOperator.destroy();

                        // post-processing, status transition
                        updateJobExecutionStatusDo(finalJobExecutionUid,
                                JobStatusEnum.FINISHED, null,
                                JobStatusEnum.RUNNING);

                        LOGGER.info("[d-job] job execution:{}, end to execute job {} ({}), duration:{}",
                                finalJobExecutionUid, distributedJobDo.getUid(), distributedJobDo.getName(),
                                AbcDateUtils.format(System.currentTimeMillis() - beginTime));
                    } catch (Exception e) {
                        // post-processing, status transition
                        String remark = null;
                        if (!ObjectUtils.isEmpty(e.getMessage())) {
                            remark = e.getMessage();
                        }
                        updateJobExecutionStatusDo(finalJobExecutionUid,
                                JobStatusEnum.FAILED, remark,
                                JobStatusEnum.RUNNING);

                        LOGGER.error("[d-job] job execution:{}, fail to execute job {} ({})" +
                                        ", duration:{}",
                                finalJobExecutionUid, distributedJobDo.getUid(), distributedJobDo.getName(),
                                AbcDateUtils.format(System.currentTimeMillis() - beginTime), e);
                    }
                }
            });
        } catch (RejectedExecutionException e) {
            LOGGER.warn("[d-job] reach queue capacity limit", e);

            // post-processing, status transition
            updateJobExecutionStatusDo(finalJobExecutionUid,
                    JobStatusEnum.FAILED, "rejected",
                    JobStatusEnum.CREATED);

            throw new AbcCapacityLimitException("job execution task is busy, please try again later");
        }

        //
        // Step 3, post-processing
        //
        DistributedJobExecutionDto distributedJobExecutionDto = new DistributedJobExecutionDto();
        BeanUtils.copyProperties(distributedJobExecutionDo, distributedJobExecutionDto);
        return distributedJobExecutionDto;
    }

    @Transactional(isolation = Isolation.READ_UNCOMMITTED, rollbackFor = Exception.class)
    private void updateJobExecutionStatusDo(
            Long uid,
            JobStatusEnum newStatus,
            String remark,
            JobStatusEnum... allowedOldStatuses) {
        DistributedJobExecutionDo distributedJobExecutionDo =
                this.distributedJobExecutionRepository.findByUid(uid);
        if (distributedJobExecutionDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DistributedJobExecutionDo.RESOURCE_SYMBOL, uid));
        }

        if (newStatus.equals(distributedJobExecutionDo.getStatus())) {
            return;
        }

        if (allowedOldStatuses != null && allowedOldStatuses.length > 0) {
            boolean allowed = false;
            StringBuilder allowedAsString = new StringBuilder();
            for (JobStatusEnum allowedOldStatus : allowedOldStatuses) {
                if (allowedAsString.length() > 0) {
                    allowedAsString.append(",").append(allowedOldStatus);
                } else {
                    allowedAsString.append(allowedOldStatus);
                }

                if (allowedOldStatus.equals(distributedJobExecutionDo.getStatus())) {
                    allowed = true;
                }
            }

            if (!allowed) {
                throw new AbcResourceConflictException(String.format("trying new status:%s, expected old status:" +
                                "%s, " +
                                "but " +
                                "found:%s, job execution uid:%d, job uid:%d", newStatus, allowedAsString,
                        distributedJobExecutionDo.getStatus(),
                        uid, distributedJobExecutionDo.getJobUid()));
            }
        }

        LocalDateTime now = LocalDateTime.now();
        switch (newStatus) {
            case RUNNING:
                distributedJobExecutionDo.setBeginTimestamp(now);
                break;
            case FINISHED:
            case FAILED:
            case CANCELED:
                distributedJobExecutionDo.setEndTimestamp(now);
                break;
            default:
                break;
        }

        distributedJobExecutionDo.setStatus(newStatus);
        if (!ObjectUtils.isEmpty(remark)) {
            if (remark.length() > DatabaseConstants.DEFAULT_DESCRIPTION_LENGTH) {
                remark = remark.substring(0, DatabaseConstants.DEFAULT_DESCRIPTION_LENGTH);
            }
            distributedJobExecutionDo.setRemark(remark);
        }
        BaseDo.update(distributedJobExecutionDo, InfrastructureConstants.ROOT_USER_UID, now);
        distributedJobExecutionRepository.save(distributedJobExecutionDo);
    }

    @Override
    public void destroy() throws Exception {
        this.jobOperatorCache.clear();
    }

    @Override
    public void afterSingletonsInstantiated() {
        resolveAndRegisterAllJobHandlers();
    }

    private void resolveAndRegisterAllJobHandlers() {
        // init job handler from method
        String[] beanNames = this.applicationContext.getBeanNamesForType(Object.class, false, true);
        for (String beanName : beanNames) {
            Object bean = this.applicationContext.getBean(beanName);

            Map<Method, JobHandler> annotatedMethods = null;
            try {
                annotatedMethods = MethodIntrospector.selectMethods(bean.getClass(),
                        new MethodIntrospector.MetadataLookup<JobHandler>() {
                            @Override
                            public JobHandler inspect(Method method) {
                                return AnnotatedElementUtils.findMergedAnnotation(method, JobHandler.class);
                            }
                        });
            } catch (Throwable ex) {
                LOGGER.error("[d-job] fail to resolve job handler from bean {}", beanName, ex);
            }
            if (annotatedMethods == null || annotatedMethods.isEmpty()) {
                continue;
            }

            for (Map.Entry<Method, JobHandler> entry : annotatedMethods.entrySet()) {
                Method method = entry.getKey();
                JobHandler jobHandler = entry.getValue();
                registerJobHandler(bean, method, jobHandler);
            }
        }
    }

    private void registerJobHandler(Object bean, Method method, JobHandler jobHandler) {
        //
        // Step 1, pre-processing
        //
        if (bean == null || method == null || jobHandler == null) {
            return;
        }

        Class<?> clazz = bean.getClass();
        String methodName = method.getName();

        if (ObjectUtils.isEmpty(jobHandler.name())) {
            throw new RuntimeException(String.format("illegal job handler name (null or empty) found in %s, %s",
                    clazz,
                    methodName));
        }

        if (this.jobOperatorCache.containsKey(jobHandler.name())) {
            throw new RuntimeException(String.format("illegal job handler name (duplicate) found in %s, %s, job " +
                            "handler name is %s",
                    clazz,
                    methodName,
                    jobHandler.name()));
        }

        //
        // Step 2, core-processing
        //
        //
        method.setAccessible(true);

        // init
        Method initMethod = null;
        if (!ObjectUtils.isEmpty(jobHandler.init())) {
            try {
                initMethod = clazz.getDeclaredMethod(jobHandler.init());
                initMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(String.format("illegal init property found in %s, %s, job " +
                                "handler name is %s, init property is %s",
                        clazz,
                        methodName,
                        jobHandler.name(),
                        jobHandler.init()));
            }
        }

        // destroy
        Method destroyMethod = null;
        if (!ObjectUtils.isEmpty(jobHandler.destroy())) {
            try {
                destroyMethod = clazz.getDeclaredMethod(jobHandler.destroy());
                destroyMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(String.format("illegal destroy property found in %s, %s, job " +
                                "handler name is %s, destroy property is %s",
                        clazz,
                        methodName,
                        jobHandler.name(),
                        jobHandler.destroy()));
            }
        }

        // build a builtin job operator
        JobBuiltinOperator jobBuiltinOperator = new JobBuiltinOperator(bean, method, initMethod, destroyMethod);
        this.jobOperatorCache.put(jobHandler.name(), jobBuiltinOperator);
        LOGGER.info("[d-job] finish to register job handler {} of {}", jobHandler.name(), clazz);
    }

    public DistributedJobExecutionDto stop(Long jobExecutionUid) throws AbcUndefinedException {
        DistributedJobExecutionDo distributedJobExecutionDo = this.distributedJobExecutionRepository.findByUid(jobExecutionUid);
        if (distributedJobExecutionDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DistributedJobExecutionDo.RESOURCE_SYMBOL,
                    jobExecutionUid));
        }

        switch (distributedJobExecutionDo.getStatus()) {
            case INITIALIZING:
            case CREATED: {
                distributedJobExecutionDo.setStatus(JobStatusEnum.CANCELED);
                BaseDo.update(distributedJobExecutionDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
                this.distributedJobExecutionRepository.save(distributedJobExecutionDo);
            }
            break;
            case RUNNING: {
                distributedJobExecutionDo.setStatus(JobStatusEnum.CANCELLING);
                BaseDo.update(distributedJobExecutionDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
                this.distributedJobExecutionRepository.save(distributedJobExecutionDo);
            }
            break;
            default: {
                throw new AbcResourceConflictException(String.format("stop operation in the status {} is not supported",
                        distributedJobExecutionDo.getStatus()));
            }
        }

        DistributedJobExecutionDto distributedJobExecutionDto = new DistributedJobExecutionDto();
        BeanUtils.copyProperties(distributedJobExecutionDo, distributedJobExecutionDto);
        return distributedJobExecutionDto;
    }
}
