package cc.cornerstones.biz.distributedtask.service.assembly;

import cc.cornerstones.almond.constants.DatabaseConstants;
import cc.cornerstones.almond.constants.InfrastructureConstants;
import cc.cornerstones.almond.constants.TaskStatusEnum;
import cc.cornerstones.almond.exceptions.AbcCapacityLimitException;
import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.utils.AbcDateUtils;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.distributedserver.service.assembly.DistributedServer;
import cc.cornerstones.biz.distributedtask.entity.DistributedTaskDo;
import cc.cornerstones.biz.distributedtask.persistence.DistributedTaskRepository;
import cc.cornerstones.biz.distributedtask.share.types.TaskHandler;
import cc.cornerstones.biz.share.event.EventBusManager;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;

@Component
public class DistributedTaskExecutor implements SmartInitializingSingleton, DisposableBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedTaskExecutor.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private DistributedTaskRepository distributedTaskRepository;

    @Autowired
    private DistributedServer distributedServer;

    @Autowired
    private ApplicationContext applicationContext;

    private ConcurrentMap<String, TaskOperator> taskOperatorCache = new ConcurrentHashMap<String,
            TaskOperator>();

    private final ThreadFactory NAMED_THREAD_FACTORY = new ThreadFactoryBuilder()
            .setNameFormat("d-task-%d").build();
    private final ExecutorService executorService = new ThreadPoolExecutor(
            3,
            5,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(100),
            NAMED_THREAD_FACTORY,
            new ThreadPoolExecutor.AbortPolicy());

    private Map<Long, DistributedTaskDo> cache = new ConcurrentHashMap<>();

    @Transactional(rollbackFor = Exception.class)
    public void execute(DistributedTaskDo distributedTaskDo) throws AbcUndefinedException {
        if (cache.containsKey(distributedTaskDo.getUid())) {
            LOGGER.info("[d-task] ignore again rcv assignment to execute task {} ({}) on this server {} ({})",
                    distributedTaskDo.getUid(),
                    distributedTaskDo.getName(),
                    distributedServer.getServerHostname(), distributedServer.getServerIpAddress());
            return;
        }

        cache.put(distributedTaskDo.getUid(), distributedTaskDo);

        LOGGER.info("[d-task] rcv assignment to execute task {} ({}) on this server {} ({})",
                distributedTaskDo.getUid(),
                distributedTaskDo.getName(),
                distributedServer.getServerHostname(), distributedServer.getServerIpAddress());

        //
        // Step 1, pre-processing
        //
        TaskOperator taskOperator = this.taskOperatorCache.get(distributedTaskDo.getHandlerName());
        if (taskOperator == null) {
            throw new AbcResourceNotFoundException(String.format("task operator of task handler %s",
                    distributedTaskDo.getHandlerName()));
        }

        distributedTaskDo.setExecutorHostname(this.distributedServer.getServerHostname());
        distributedTaskDo.setExecutorIpAddress(this.distributedServer.getServerIpAddress());
        distributedTaskDo.setStatus(TaskStatusEnum.SCHEDULING);
        BaseDo.update(distributedTaskDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
        this.distributedTaskRepository.save(distributedTaskDo);

        //
        // Step 2, core-processing
        //

        try {
            executorService.execute(new Runnable() {
                @Override
                public void run() {

                    // pre-processing, status transition
                    updateTaskStatusDo(distributedTaskDo,
                            TaskStatusEnum.RUNNING, null,
                            TaskStatusEnum.SCHEDULING);

                    long beginTime = System.currentTimeMillis();

                    try {
                        LOGGER.info("[d-task] begin to execute task {} ({})",
                                distributedTaskDo.getUid(), distributedTaskDo.getName());

                        // core-processing
                        taskOperator.init();
                        taskOperator.execute(
                                distributedTaskDo.getUid(),
                                distributedTaskDo.getName(),
                                distributedTaskDo.getPayload());
                        taskOperator.destroy();

                        // post-processing, status transition
                        updateTaskStatusDo(distributedTaskDo,
                                TaskStatusEnum.FINISHED, null,
                                TaskStatusEnum.RUNNING);

                        LOGGER.info("[d-task] end to execute task {} ({}), duration:{}",
                                distributedTaskDo.getUid(), distributedTaskDo.getName(),
                                AbcDateUtils.format(System.currentTimeMillis() - beginTime));
                    } catch (Exception e) {
                        // post-processing, status transition
                        String remark = null;
                        if (!ObjectUtils.isEmpty(e.getMessage())) {
                            remark = e.getMessage();
                        }
                        updateTaskStatusDo(distributedTaskDo,
                                TaskStatusEnum.FAILED, remark,
                                TaskStatusEnum.RUNNING);

                        LOGGER.error("[d-task] fail to execute task {} ({}), duration:{}",
                                distributedTaskDo.getUid(), distributedTaskDo.getName(),
                                AbcDateUtils.format(System.currentTimeMillis() - beginTime), e);
                    } finally {
                        cache.remove(distributedTaskDo.getUid());
                    }
                }
            });
        } catch (RejectedExecutionException e) {
            LOGGER.warn("[d-task] reach queue capacity limit", e);

            cache.remove(distributedTaskDo.getUid());

            updateTaskStatusDo(distributedTaskDo,
                    TaskStatusEnum.CREATED, "reach capacity limit, waiting for next scheduling",
                    TaskStatusEnum.SCHEDULING);

            throw new AbcCapacityLimitException("task pool is full, please try again later");
        }

        //
        // Step 3, post-processing
        //
    }

    @Transactional(rollbackFor = Exception.class)
    private void updateTaskStatusDo(
            DistributedTaskDo distributedTaskDo,
            TaskStatusEnum newStatus,
            String remark,
            TaskStatusEnum... allowedOldStatuses) {
        if (newStatus.equals(distributedTaskDo.getStatus())) {
            return;
        }

        if (allowedOldStatuses != null && allowedOldStatuses.length > 0) {
            boolean allowed = false;
            StringBuilder allowedOldStatusesAsString = new StringBuilder();
            for (TaskStatusEnum allowedOldStatus : allowedOldStatuses) {
                if (allowedOldStatusesAsString.length() > 0) {
                    allowedOldStatusesAsString.append(",").append(allowedOldStatus);
                } else {
                    allowedOldStatusesAsString.append(allowedOldStatus);
                }

                if (allowedOldStatus.equals(distributedTaskDo.getStatus())) {
                    allowed = true;
                }
            }

            if (!allowed) {
                throw new AbcResourceConflictException(String.format("trying new status:%s, expected old status:%s," +
                                " but " +
                                "found:%s, task uid:%d", newStatus, allowedOldStatusesAsString,
                        distributedTaskDo.getStatus(),
                        distributedTaskDo.getUid()));
            }
        }

        LocalDateTime now = LocalDateTime.now();
        switch (newStatus) {
            case RUNNING:
                distributedTaskDo.setBeginTimestamp(now);
                break;
            case FINISHED:
            case FAILED:
            case CANCELED:
                distributedTaskDo.setEndTimestamp(now);
                break;
            default:
                break;
        }

        distributedTaskDo.setStatus(newStatus);
        if (remark != null && remark.length() > DatabaseConstants.DEFAULT_DESCRIPTION_LENGTH) {
            remark = remark.substring(0, DatabaseConstants.DEFAULT_DESCRIPTION_LENGTH);
        }
        distributedTaskDo.setRemark(remark);
        BaseDo.update(distributedTaskDo, InfrastructureConstants.ROOT_USER_UID, now);
        this.distributedTaskRepository.save(distributedTaskDo);
    }

    @Override
    public void destroy() throws Exception {
        this.taskOperatorCache.clear();
    }

    @Override
    public void afterSingletonsInstantiated() {
        resolveAndRegisterAllTaskHandlers();
    }

    private void resolveAndRegisterAllTaskHandlers() {
        // init task handler from method
        String[] beanNames = this.applicationContext.getBeanNamesForType(Object.class, false, true);
        for (String beanName : beanNames) {
            Object bean = this.applicationContext.getBean(beanName);

            Map<Method, TaskHandler> annotatedMethods = null;
            try {
                annotatedMethods = MethodIntrospector.selectMethods(bean.getClass(),
                        new MethodIntrospector.MetadataLookup<TaskHandler>() {
                            @Override
                            public TaskHandler inspect(Method method) {
                                return AnnotatedElementUtils.findMergedAnnotation(method, TaskHandler.class);
                            }
                        });
            } catch (Throwable ex) {
                LOGGER.error("[d-task] fail to resolve task handler from bean {}", beanName, ex);
            }
            if (annotatedMethods == null || annotatedMethods.isEmpty()) {
                continue;
            }

            for (Map.Entry<Method, TaskHandler> entry : annotatedMethods.entrySet()) {
                Method method = entry.getKey();
                TaskHandler taskHandler = entry.getValue();
                registerTaskHandler(bean, method, taskHandler);
            }
        }
    }

    private void registerTaskHandler(Object bean, Method method, TaskHandler taskHandler) {
        //
        // Step 1, pre-processing
        //
        if (bean == null || method == null || taskHandler == null) {
            return;
        }

        Class<?> clazz = bean.getClass();
        String methodName = method.getName();

        if (ObjectUtils.isEmpty(taskHandler.name())) {
            throw new RuntimeException(String.format("illegal task handler name (null or empty) found in %s, %s",
                    clazz,
                    methodName));
        }

        if (this.taskOperatorCache.containsKey(taskHandler.name())) {
            throw new RuntimeException(String.format("illegal task handler name (duplicate) found in %s, %s, task " +
                            "handler name is %s",
                    clazz,
                    methodName,
                    taskHandler.name()));
        }

        //
        // Step 2, core-processing
        //
        //
        method.setAccessible(true);

        // init
        Method initMethod = null;
        if (!ObjectUtils.isEmpty(taskHandler.init())) {
            try {
                initMethod = clazz.getDeclaredMethod(taskHandler.init());
                initMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(String.format("illegal init property found in %s, %s, task " +
                                "handler name is %s, init property is %s",
                        clazz,
                        methodName,
                        taskHandler.name(),
                        taskHandler.init()));
            }
        }

        // destroy
        Method destroyMethod = null;
        if (!ObjectUtils.isEmpty(taskHandler.destroy())) {
            try {
                destroyMethod = clazz.getDeclaredMethod(taskHandler.destroy());
                destroyMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(String.format("illegal destroy property found in %s, %s, task " +
                                "handler name is %s, destroy property is %s",
                        clazz,
                        methodName,
                        taskHandler.name(),
                        taskHandler.destroy()));
            }
        }

        // build a builtin task operator
        TaskBuiltinOperator taskBuiltinOperator = new TaskBuiltinOperator(bean, method, initMethod, destroyMethod);
        this.taskOperatorCache.put(taskHandler.name(), taskBuiltinOperator);
        LOGGER.info("[d-task] finish to register task handler {} of {}", taskHandler.name(), clazz);
    }

    public void stop(Long taskUid) throws AbcUndefinedException {
        DistributedTaskDo distributedTaskDo = this.distributedTaskRepository.findByUid(taskUid);
        if (distributedTaskDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DistributedTaskDo.RESOURCE_SYMBOL,
                    taskUid));
        }

        switch (distributedTaskDo.getStatus()) {
            case CREATED:
            case RUNNING: {
                distributedTaskDo.setStatus(TaskStatusEnum.CANCELED);
                BaseDo.update(distributedTaskDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
                this.distributedTaskRepository.save(distributedTaskDo);
            }
            break;
            default: {
                throw new AbcResourceConflictException(String.format("stop operation in the status {} is not supported",
                        distributedTaskDo.getStatus()));
            }
        }
    }
}
