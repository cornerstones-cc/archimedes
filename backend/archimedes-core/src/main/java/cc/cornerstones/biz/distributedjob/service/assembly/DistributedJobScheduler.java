package cc.cornerstones.biz.distributedjob.service.assembly;

import cc.cornerstones.almond.constants.InfrastructureConstants;
import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceIntegrityException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.utils.AbcDateUtils;
import cc.cornerstones.arbutus.lock.entity.LockDo;
import cc.cornerstones.arbutus.lock.persistence.LockRepository;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.distributedjob.dto.CreateDistributedJobExecutionDto;
import cc.cornerstones.biz.distributedserver.service.assembly.DistributedServer;
import cc.cornerstones.biz.distributedjob.entity.DistributedJobDo;
import cc.cornerstones.biz.distributedserver.entity.DistributedServerDo;
import cc.cornerstones.biz.distributedjob.entity.DistributedJobExecutionDo;
import cc.cornerstones.biz.distributedjob.entity.DistributedJobSchedulerDo;
import cc.cornerstones.biz.distributedserver.persistence.DistributedServerRepository;
import cc.cornerstones.biz.distributedjob.persistence.DistributedJobExecutionRepository;
import cc.cornerstones.biz.distributedjob.persistence.DistributedJobRepository;
import cc.cornerstones.biz.distributedjob.persistence.DistributedJobSchedulerRepository;
import cc.cornerstones.biz.distributedserver.share.constants.DistributedServerStatus;
import cc.cornerstones.biz.share.event.*;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class DistributedJobScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedJobScheduler.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private LockRepository lockRepository;

    @Autowired
    private DistributedServer distributedServer;

    @Autowired
    private DistributedServerRepository distributedServerRepository;

    @Autowired
    private DistributedJobSchedulerRepository distributedJobSchedulerRepository;

    @Autowired
    private DistributedJobRepository distributedJobRepository;

    @Autowired
    private DistributedJobExecutionRepository distributedJobExecutionRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${server.port}")
    private Integer serverPort;

    @Value("${server.ssl.enabled}")
    private Boolean serverSslEnabled;

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    @Scheduled(cron = "0/7 * * * * ?")
    public void run() throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        String serialNumber = UUID.randomUUID().toString();

        //
        // Step 1.1, 获取本机信息
        //
        String thisServerHostname = this.distributedServer.getServerHostname();
        String thisServerIpAddress = this.distributedServer.getServerIpAddress();
        if (ObjectUtils.isEmpty(thisServerHostname)
                || ObjectUtils.isEmpty(thisServerIpAddress)) {
            LOGGER.info("[d-job] serial number:{}, waiting for server hostname and ip address initialized",
                    serialNumber);
            return;
        }

        //
        // Step 1.2, 找出正在当值的 scheduler，判断本机是否需要参与竞选新的 scheduler

        // 1) 如果找不到，本机需要竞选 scheduler；
        // 2) 如果找到，继续分更细致的情况：
        //    2.1) 如果当值 scheduler 就是本机，则本机继续当值；
        //    2.2) 如果当值 scheduler 不是本机，继续分更细致的情况：
        //       2.2.1) 如果当值 scheduler 所在 host 存在且状态是 UP，则本机不需要竞选 scheduler；
        //       2.2.2) 如果当值 scheduler 所在 host 不存在或状态不是 UP，则本机需要竞选 scheduler；
        //
        DistributedJobSchedulerDo currentOnDutySchedulerDo = null;
        try {
            currentOnDutySchedulerDo = this.distributedJobSchedulerRepository.findEffective();
        } catch (Exception e) {
            LOGGER.error("[d-job] serial number:{}, fail to find effective scheduler",
                    serialNumber, e);
            throw new AbcResourceIntegrityException("fail to find effective scheduler");
        }

        // 本机是否需要竞选 scheduler
        boolean requiredToCampaignScheduler = false;
        // 本机是否最新当值 scheduler
        boolean latestOnDutyScheduler = false;

        if (currentOnDutySchedulerDo == null) {
            requiredToCampaignScheduler = true;
        } else {
            if (thisServerHostname.equals(currentOnDutySchedulerDo.getHostname())
                    && thisServerIpAddress.equals(currentOnDutySchedulerDo.getIpAddress())) {
                // 当值 scheduler 就是本机，则本机继续当值
                latestOnDutyScheduler = true;
            } else {
                // 当值 scheduler 不是本机，继续分更细致的情况
                DistributedServerDo thatServerDo =
                        this.distributedServerRepository.findByHostnameAndIpAddress(
                                currentOnDutySchedulerDo.getHostname(),
                                currentOnDutySchedulerDo.getIpAddress());
                if (thatServerDo == null) {
                    LOGGER.error("[d-job] serial number:{}, cannot find server {} ({})",
                            serialNumber,
                            currentOnDutySchedulerDo.getHostname(),
                            currentOnDutySchedulerDo.getIpAddress());
                    requiredToCampaignScheduler = true;
                } else {
                    if (!DistributedServerStatus.UP.equals(thatServerDo.getStatus())) {
                        requiredToCampaignScheduler = true;
                    }

                    // 当值 scheduler 不是本机，其所在 host 也工作正常，继续当值
                }
            }
        }

        //
        // Step 1.3, 本机需要参与竞选新的 scheduler
        //
        if (requiredToCampaignScheduler) {
            // Step 1.3.1, 先要获取悲观锁
            LOGGER.info("[d-job] serial number:{}, begin to get lock for server {} ({})",
                    serialNumber,
                    thisServerHostname,
                    thisServerIpAddress);
            try {
                // 获取悲观锁
                LockDo lockDo = new LockDo();
                lockDo.setName("Job Scheduler");
                lockDo.setResource("NA");
                lockDo.setVersion(1L);
                lockDo.setCreatedTimestamp(LocalDateTime.now());
                lockDo.setLastModifiedTimestamp(LocalDateTime.now());
                this.lockRepository.save(lockDo);
                LOGGER.info("[d-job] serial number:{}, end to get lock for server {} ({})",
                        serialNumber,
                        thisServerHostname, thisServerIpAddress);
            } catch (Exception e) {
                LOGGER.info("[d-job] serial number:{}, fail to get lock for server {} ({})",
                        serialNumber,
                        thisServerHostname, thisServerIpAddress);
                return;
            }

            // Step 1.3.2, 已经获取到悲观锁，但还不能代表本机竞选新的 scheduler 成功
            // 需要继续检查此刻最新的当值 scheduler 还是不是本次调用开始时发现的那个当值 scheduler。
            // 如果不是，表示本次调用的同一时间，有其它主机当选了新的 scheduler。
            // 如果是，代表本机竞选 scheduler 成功，作为新的当值 scheduler

            try {
                DistributedJobSchedulerDo latestOnDutySchedulerDo = null;
                try {
                    latestOnDutySchedulerDo = this.distributedJobSchedulerRepository.findEffective();
                } catch (Exception e) {
                    LOGGER.error("[d-job] serial number:{}, fail to find effective scheduler",
                            serialNumber, e);
                    throw new AbcResourceIntegrityException("fail to find effective scheduler");
                }

                if (currentOnDutySchedulerDo == null) {
                    if (latestOnDutySchedulerDo != null) {
                        // stop
                        // 本次调用同一时间，已经有其它主机当选为新的 scheduler。
                        // 本机退出本次竞选。
                        LOGGER.info("[d-job] serial number:{}, this server {} ({}) abandons to campaign scheduler",
                                serialNumber,
                                thisServerHostname, thisServerIpAddress);
                        return;
                    }
                } else {
                    if (!currentOnDutySchedulerDo.getHostname().equals(latestOnDutySchedulerDo.getHostname())
                            || !currentOnDutySchedulerDo.getIpAddress().equals(latestOnDutySchedulerDo.getIpAddress())) {
                        // stop
                        // 本次调用同一时间，已经有其它主机当选为新的 scheduler。
                        // 本机退出本次竞选。
                        LOGGER.info("[d-job] serial number:{}, this server {} ({}) abandons to campaign scheduler",
                                serialNumber,
                                thisServerHostname, thisServerIpAddress);
                        return;
                    }
                }

                // Step 1.3.3, 已经确认本机当选新的 scheduler

                // 先 invalidate the current scheduler

                if (currentOnDutySchedulerDo != null) {
                    try {
                        currentOnDutySchedulerDo.setEffective(Boolean.FALSE);
                        currentOnDutySchedulerDo.setEffectiveTo(LocalDateTime.now());
                        BaseDo.update(currentOnDutySchedulerDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
                        this.distributedJobSchedulerRepository.save(currentOnDutySchedulerDo);
                    } catch (Exception e) {
                        LOGGER.error("[d-job] serial number:{}, fail to invalidate the current on duty scheduler {} ({})",
                                serialNumber,
                                currentOnDutySchedulerDo.getHostname(), currentOnDutySchedulerDo.getIpAddress(), e);
                        throw new AbcResourceIntegrityException("fail to invalidate the old on duty scheduler");
                    }
                }

                // 再将本机设置为新的 effective scheduler

                DistributedJobSchedulerDo newDistributedJobSchedulerDo = new DistributedJobSchedulerDo();
                newDistributedJobSchedulerDo.setUid(this.idHelper.getNextDistributedId(DistributedJobSchedulerDo.RESOURCE_NAME));
                newDistributedJobSchedulerDo.setHostname(thisServerHostname);
                newDistributedJobSchedulerDo.setIpAddress(thisServerIpAddress);
                newDistributedJobSchedulerDo.setEffective(Boolean.TRUE);
                newDistributedJobSchedulerDo.setEffectiveFrom(LocalDateTime.now());
                BaseDo.create(newDistributedJobSchedulerDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
                this.distributedJobSchedulerRepository.save(newDistributedJobSchedulerDo);

                latestOnDutyScheduler = true;
            } finally {
                // Step 1.3.4, 释放悲观锁
                LockDo lockDo = this.lockRepository.findByNameAndResourceAndVersion(
                        "Job Scheduler", "NA", 1L);
                if (lockDo != null) {
                    this.lockRepository.delete(lockDo);
                }
            }
        }

        if (!latestOnDutyScheduler) {
            LOGGER.info("[d-job] serial number:{}, this server {} ({}) is not the latest on duty scheduler, not working",
                    serialNumber,
                    thisServerHostname, thisServerIpAddress);
            return;
        }

        //
        // Step 2, core-processing
        //

        // 本机是当值 scheduler，则需要开始 schedule jobs 工作。
        synchronized (this) {
            try {
                auditJobExecutions(serialNumber);
                scheduleJobs(serialNumber);
            } catch (Exception e) {
                LOGGER.error("[d-job] serial number:{}, fail to schedule jobs",
                        serialNumber, e);
            }
        }
    }

    public void auditJobExecutions(String serialNumber) throws AbcUndefinedException {
        Specification<DistributedJobDo> jobSpecification = new Specification<DistributedJobDo>() {
            @Override
            public Predicate toPredicate(Root<DistributedJobDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();

                predicateList.add(criteriaBuilder.equal(root.get("enabled"), true));

                predicateList.add(criteriaBuilder.isNotNull(root.get("timeoutDurationInSecs")));

                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        Map<Long, Long> distributedJobUidAndTimeoutDurationInSecondsMap = new HashMap<>();
        Iterable<DistributedJobDo> distributedJobDoIterable = this.distributedJobRepository.findAll(jobSpecification,
                Sort.by(Sort.Order.asc(BaseDo.ID_FIELD_NAME)));
        distributedJobDoIterable.forEach(distributedJobDo -> {
            distributedJobUidAndTimeoutDurationInSecondsMap.put(distributedJobDo.getUid(), distributedJobDo.getTimeoutDurationInSecs());
        });

        if (CollectionUtils.isEmpty(distributedJobUidAndTimeoutDurationInSecondsMap)) {
            LocalDateTime nowDateTime = LocalDateTime.now();
            LocalDateTime thresholdCreatedTimestamp = nowDateTime.minusHours(4);

            Specification<DistributedJobExecutionDo> jobExecutionSpecification =
                    new Specification<DistributedJobExecutionDo>() {
                        @Override
                        public Predicate toPredicate(Root<DistributedJobExecutionDo> root, CriteriaQuery<?> query,
                                                     CriteriaBuilder criteriaBuilder) {
                            List<Predicate> predicateList = new ArrayList<>();

                            CriteriaBuilder.In<JobStatusEnum> in = criteriaBuilder.in(root.get("status"));
                            in.value(JobStatusEnum.INITIALIZING);
                            in.value(JobStatusEnum.CREATED);
                            in.value(JobStatusEnum.RUNNING);
                            in.value(JobStatusEnum.FAILING);
                            in.value(JobStatusEnum.CANCELLING);
                            in.value(JobStatusEnum.RESTARTING);
                            in.value(JobStatusEnum.RECONCILING);

                            predicateList.add(in);

                            predicateList.add(criteriaBuilder.lessThan(root.get(BaseDo.CREATED_TIMESTAMP_FIELD_NAME), thresholdCreatedTimestamp));

                            return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
                        }
                    };

            Iterable<DistributedJobExecutionDo> jobExecutionDoIterable =
                    this.distributedJobExecutionRepository.findAll(jobExecutionSpecification, Sort.by(Sort.Order.asc(BaseDo.ID_FIELD_NAME)));
            jobExecutionDoIterable.forEach(jobExecutionDo -> {
                jobExecutionDo.setStatus(JobStatusEnum.FAILED);
                jobExecutionDo.setRemark("timeout");
                BaseDo.update(jobExecutionDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());

                LOGGER.warn("[d-job] serial number:{}, found timeout job execution::uid={}, job_uid={}, created_timestamp={}, " +
                                "duration={}",
                        serialNumber,
                        jobExecutionDo.getUid(),
                        jobExecutionDo.getJobUid(), jobExecutionDo.getCreatedTimestamp(),
                        AbcDateUtils.format(Duration.between(jobExecutionDo.getCreatedTimestamp(), nowDateTime).toMillis()));
            });
            this.distributedJobExecutionRepository.saveAll(jobExecutionDoIterable);
        } else {
            LocalDateTime nowDateTime = LocalDateTime.now();

            Specification<DistributedJobExecutionDo> jobExecutionSpecification =
                    new Specification<DistributedJobExecutionDo>() {
                        @Override
                        public Predicate toPredicate(Root<DistributedJobExecutionDo> root, CriteriaQuery<?> query,
                                                     CriteriaBuilder criteriaBuilder) {
                            List<Predicate> predicateList = new ArrayList<>();

                            CriteriaBuilder.In<JobStatusEnum> in = criteriaBuilder.in(root.get("status"));
                            in.value(JobStatusEnum.INITIALIZING);
                            in.value(JobStatusEnum.CREATED);
                            in.value(JobStatusEnum.RUNNING);
                            in.value(JobStatusEnum.FAILING);
                            in.value(JobStatusEnum.CANCELLING);
                            in.value(JobStatusEnum.RESTARTING);
                            in.value(JobStatusEnum.RECONCILING);

                            predicateList.add(in);

                            return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
                        }
                    };

            Iterable<DistributedJobExecutionDo> jobExecutionDoIterable =
                    this.distributedJobExecutionRepository.findAll(jobExecutionSpecification, Sort.by(Sort.Order.asc(BaseDo.ID_FIELD_NAME)));
            jobExecutionDoIterable.forEach(jobExecutionDo -> {
                LocalDateTime thresholdCreatedTimestamp = nowDateTime.minusHours(4);

                if (distributedJobUidAndTimeoutDurationInSecondsMap.containsKey(jobExecutionDo.getJobUid())) {
                    thresholdCreatedTimestamp =
                            nowDateTime.minusSeconds(distributedJobUidAndTimeoutDurationInSecondsMap.get(jobExecutionDo.getJobUid()));
                }

                if (thresholdCreatedTimestamp.isAfter(jobExecutionDo.getCreatedTimestamp())) {
                    jobExecutionDo.setStatus(JobStatusEnum.FAILED);
                    jobExecutionDo.setRemark("timeout");
                    BaseDo.update(jobExecutionDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());

                    LOGGER.warn("[d-job] serial number:{}, found timeout job execution::uid={}, job_uid={}, created_timestamp={}, " +
                                    "duration={}",
                            serialNumber,
                            jobExecutionDo.getUid(),
                            jobExecutionDo.getJobUid(), jobExecutionDo.getCreatedTimestamp(),
                            AbcDateUtils.format(Duration.between(jobExecutionDo.getCreatedTimestamp(), nowDateTime).toMillis()));
                }
            });
            this.distributedJobExecutionRepository.saveAll(jobExecutionDoIterable);
        }
    }

    public void scheduleJobs(String serialNumber) throws AbcUndefinedException {
        LocalDateTime thresholdDateTime = LocalDateTime.now().plusSeconds(1);

        Specification<DistributedJobDo> specification = new Specification<DistributedJobDo>() {
            @Override
            public Predicate toPredicate(Root<DistributedJobDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();

                predicateList.add(criteriaBuilder.equal(root.get("enabled"), true));

                predicateList.add(criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("nextExecutionTimestamp")),
                        criteriaBuilder.lessThan(root.get("nextExecutionTimestamp"), thresholdDateTime)));

                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("nextExecutionTimestamp"),
                Sort.Order.asc(BaseDo.ID_FIELD_NAME)));

        Page<DistributedJobDo> jobDoPage = this.distributedJobRepository.findAll(
                specification,
                pageable);
        if (jobDoPage.isEmpty()) {
            return;
        }
        jobDoPage.getContent().forEach(jobDo -> {
            try {
                LOGGER.info("[d-job] serial number:{}, begin to schedule job {}",
                        serialNumber,
                        jobDo);
                scheduleJob(serialNumber, jobDo);
            } finally {
                LOGGER.info("[d-job] serial number:{}, end to schedule job {}",
                        serialNumber,
                        jobDo);
            }
        });
    }

    public void scheduleJob(
            String serialNumber,
            DistributedJobDo distributedJobDo) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        // 找出 distributed server(s)
        Specification<DistributedServerDo> specification = new Specification<DistributedServerDo>() {
            @Override
            public Predicate toPredicate(Root<DistributedServerDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();

                predicateList.add(criteriaBuilder.equal(root.get("status"), DistributedServerStatus.UP));

                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        List<DistributedServerDo> distributedServerDoList = new LinkedList<>();
        this.distributedServerRepository.findAll(specification, Sort.by(Sort.Order.asc("uid"))).forEach(serverDo -> {
            distributedServerDoList.add(serverDo);
        });

        if (CollectionUtils.isEmpty(distributedServerDoList)) {
            return;
        }

        //
        // Step 2, core-processing
        //
        switch (distributedJobDo.getRoutingAlgorithm()) {
            case RANDOM: {
                distributedJobDo.setLastExecutionTimestamp(LocalDateTime.now());

                Random random = new Random();
                int index = random.nextInt(distributedServerDoList.size());
                DistributedServerDo distributedServerDo = distributedServerDoList.get(index);

                assignJobToExecutor(serialNumber, distributedJobDo, distributedServerDo);

                distributedJobDo.setLastExecutorHostname(distributedServerDo.getHostname());
                distributedJobDo.setLastExecutorIpAddress(distributedServerDo.getIpAddress());
            }
            break;
            case BROADCAST: {
                distributedJobDo.setLastExecutionTimestamp(LocalDateTime.now());

                distributedServerDoList.forEach(distributedServerDo -> {
                    assignJobToExecutor(serialNumber, distributedJobDo, distributedServerDo);
                });

                distributedJobDo.setLastExecutorHostname(distributedServerDoList.get(distributedServerDoList.size() - 1).getHostname());
                distributedJobDo.setLastExecutorIpAddress(distributedServerDoList.get(distributedServerDoList.size() - 1).getIpAddress());
            }
            break;
            case ROUND_ROBIN: {
                distributedJobDo.setLastExecutionTimestamp(LocalDateTime.now());

                DistributedServerDo distributedServerDo = null;
                if (distributedJobDo.getLastExecutorHostname() == null) {
                    distributedServerDo = distributedServerDoList.get(0);
                } else {
                    for (int index = 0; index < distributedServerDoList.size(); index++) {
                        if (distributedServerDoList.get(index).getHostname().equals(distributedJobDo.getLastExecutorHostname())) {
                            if (index < distributedServerDoList.size() - 1) {
                                distributedServerDo = distributedServerDoList.get(index + 1);
                            } else {
                                distributedServerDo = distributedServerDoList.get(0);
                            }
                        }
                    }

                    if (distributedServerDo == null) {
                        distributedServerDo = distributedServerDoList.get(0);
                    }
                }

                assignJobToExecutor(serialNumber, distributedJobDo, distributedServerDo);

                distributedJobDo.setLastExecutorHostname(distributedServerDo.getHostname());
                distributedJobDo.setLastExecutorIpAddress(distributedServerDo.getIpAddress());
            }
            break;
            default:
                throw new AbcResourceConflictException(String.format("unsupported routing algorithm:%s",
                        distributedJobDo.getRoutingAlgorithm()));
        }

        //
        // Step 3, post-processing
        //
        // 更新计划下次运行时间
        CronExpression cronExpression = CronExpression.parse(distributedJobDo.getCronExpression());
        distributedJobDo.setNextExecutionTimestamp(cronExpression.next(LocalDateTime.now()));
        BaseDo.update(distributedJobDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
        this.distributedJobRepository.save(distributedJobDo);
    }

    private void assignJobToExecutor(
            String serialNumber,
            DistributedJobDo distributedJobDo,
            DistributedServerDo distributedServerDo) {
        StringBuilder url = new StringBuilder();
        if (Boolean.TRUE.equals(this.serverSslEnabled)) {
            url.append("https://");
        } else {
            url.append("http://");
        }
        url.append(distributedServerDo.getIpAddress()).append(":").append(this.serverPort)
                .append("/utilities/d-job/job-execution").append("?job_uid=").append(distributedJobDo.getUid());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        CreateDistributedJobExecutionDto createDistributedJobExecutionDto = new CreateDistributedJobExecutionDto();
        HttpEntity<CreateDistributedJobExecutionDto> httpEntity = new HttpEntity<>(createDistributedJobExecutionDto, httpHeaders);

        long beginTime = System.currentTimeMillis();
        LOGGER.info("[d-job] serial number:{}, begin to assign job {} to executor {} ({}), duration={}",
                serialNumber,
                distributedJobDo.getUid(),
                distributedServerDo.getHostname(),
                distributedServerDo.getIpAddress(),
                AbcDateUtils.format(System.currentTimeMillis() - beginTime));
        try {
            Response response = this.restTemplate.postForObject(url.toString(), httpEntity, Response.class);
            if (response.isSuccessful()) {
                LOGGER.info("[d-job] serial number:{}, end to assign job {} to executor {} ({}), duration={}",
                        serialNumber,
                        distributedJobDo.getUid(),
                        distributedServerDo.getHostname(),
                        distributedServerDo.getIpAddress(),
                        AbcDateUtils.format(System.currentTimeMillis() - beginTime));
            } else {
                LOGGER.error("[d-job] serial number:{}, fail to assign job {} to executor {} ({}), err_code={}, err_message={}, " +
                                "duration={}",
                        serialNumber,
                        distributedJobDo.getUid(),
                        distributedServerDo.getHostname(),
                        distributedServerDo.getIpAddress(),
                        response.getErrCode(),
                        response.getErrMessage(),
                        AbcDateUtils.format(System.currentTimeMillis() - beginTime));
            }
        } catch (Exception e) {
            LOGGER.error("[d-job] serial number:{}, fail to assign job {} to executor {} ({}), duration={}",
                    serialNumber,
                    distributedJobDo.getUid(),
                    distributedServerDo.getHostname(),
                    distributedServerDo.getIpAddress(),
                    AbcDateUtils.format(System.currentTimeMillis() - beginTime),
                    e);
        }
    }

    /**
     * 在 event bus 中注册成为 subscriber
     */
    @PostConstruct
    public void init() {
        this.eventBusManager.registerSubscriber(this);
    }

    /**
     * event handler
     *
     * @param event
     */
    @Subscribe
    public void handleServerDownEvent(DistributedServerDownEvent event) {
        LOGGER.info("rcv event:{}", event);

        //
        // clean impacted job scheduler
        //
        DistributedJobSchedulerDo distributedJobSchedulerDo = this.distributedJobSchedulerRepository.findEffective();
        if (distributedJobSchedulerDo != null) {
            if (distributedJobSchedulerDo.getHostname().equals(event.getHostname())
                    && distributedJobSchedulerDo.getIpAddress().equals(event.getIpAddress())) {
                distributedJobSchedulerDo.setEffective(Boolean.FALSE);
                distributedJobSchedulerDo.setEffectiveTo(LocalDateTime.now());
                BaseDo.update(distributedJobSchedulerDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
                this.distributedJobSchedulerRepository.save(distributedJobSchedulerDo);

                // release lock if have
                LockDo lockDo = this.lockRepository.findByNameAndResourceAndVersion(
                        "Job Scheduler", "NA", 1L);
                if (lockDo != null) {
                    this.lockRepository.delete(lockDo);
                }
            }
        }

        //
        // clean impacted job execution(s)
        //
        Specification<DistributedJobExecutionDo> specification = new Specification<DistributedJobExecutionDo>() {
            @Override
            public Predicate toPredicate(Root<DistributedJobExecutionDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();

                predicateList.add(criteriaBuilder.equal(root.get("executorHostname"), event.getHostname()));
                predicateList.add(criteriaBuilder.equal(root.get("executorIpAddress"), event.getIpAddress()));

                CriteriaBuilder.In<JobStatusEnum> in = criteriaBuilder.in(root.get("status"));
                in.value(JobStatusEnum.INITIALIZING);
                in.value(JobStatusEnum.CREATED);
                in.value(JobStatusEnum.RUNNING);
                in.value(JobStatusEnum.CANCELLING);
                in.value(JobStatusEnum.FAILING);
                predicateList.add(in);

                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        LocalDateTime now = LocalDateTime.now();
        Sort sort = Sort.by(Sort.Order.asc(BaseDo.ID_FIELD_NAME));
        Iterable<DistributedJobExecutionDo> distributedJobExecutionDoIterable =
                this.distributedJobExecutionRepository.findAll(specification, sort);
        distributedJobExecutionDoIterable.forEach(distributedJobExecutionDo -> {
            distributedJobExecutionDo.setStatus(JobStatusEnum.CANCELED);
            distributedJobExecutionDo.setRemark("server down");
            distributedJobExecutionDo.setEndTimestamp(LocalDateTime.now());
            BaseDo.update(distributedJobExecutionDo, InfrastructureConstants.ROOT_USER_UID, now);
        });
        this.distributedJobExecutionRepository.saveAll(distributedJobExecutionDoIterable);
    }

    /**
     * event handler
     *
     * @param event
     */
    @Subscribe
    public void handleServerUpEvent(DistributedServerUpEvent event) {
        LOGGER.info("rcv event:{}", event);

    }
}
