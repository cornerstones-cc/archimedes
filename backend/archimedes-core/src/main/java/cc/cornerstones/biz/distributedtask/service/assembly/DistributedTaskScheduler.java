package cc.cornerstones.biz.distributedtask.service.assembly;

import cc.cornerstones.almond.constants.InfrastructureConstants;
import cc.cornerstones.almond.constants.TaskStatusEnum;
import cc.cornerstones.almond.exceptions.AbcResourceIntegrityException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.utils.AbcDateUtils;
import cc.cornerstones.arbutus.lock.entity.LockDo;
import cc.cornerstones.arbutus.lock.persistence.LockRepository;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.distributedserver.entity.DistributedServerDo;
import cc.cornerstones.biz.distributedserver.persistence.DistributedServerRepository;
import cc.cornerstones.biz.distributedserver.service.assembly.DistributedServer;
import cc.cornerstones.biz.distributedserver.share.constants.DistributedServerStatus;
import cc.cornerstones.biz.distributedtask.dto.StartDistributedTaskDto;
import cc.cornerstones.biz.distributedtask.entity.DistributedTaskSchedulerDo;
import cc.cornerstones.biz.distributedtask.entity.DistributedTaskDo;
import cc.cornerstones.biz.distributedtask.persistence.DistributedTaskSchedulerRepository;
import cc.cornerstones.biz.distributedtask.persistence.DistributedTaskRepository;
import cc.cornerstones.biz.share.event.DistributedServerDownEvent;
import cc.cornerstones.biz.share.event.DistributedServerUpEvent;
import cc.cornerstones.biz.share.event.EventBusManager;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
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
public class DistributedTaskScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedTaskScheduler.class);

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
    private DistributedTaskSchedulerRepository distributedTaskSchedulerRepository;

    @Autowired
    private DistributedTaskRepository distributedTaskRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${server.port}")
    private Integer serverPort;

    @Value("${server.ssl.enabled}")
    private Boolean serverSslEnabled;

    @Scheduled(cron = "0/3 * * * * ?")
    public void run() throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        //
        // Step 1.1, 获取本机信息
        //
        String thisServerHostname = this.distributedServer.getServerHostname();
        String thisServerIpAddress = this.distributedServer.getServerIpAddress();
        if (ObjectUtils.isEmpty(thisServerHostname)
                || ObjectUtils.isEmpty(thisServerIpAddress)) {
            LOGGER.info("[d-task] waiting for server hostname and ip address initialized");
            return;
        }

        //
        // Step 1.2, 找出正在当值的 scheduler，判断本机是否需要参与竞选新的 scheduler
        //

        // 1) 如果找不到，本机需要竞选 scheduler；
        // 2) 如果找到，继续分更细致的情况：
        //    2.1) 如果当值 scheduler 就是本机，则本机继续当值；
        //    2.2) 如果当值 scheduler 不是本机，继续更细致的情况：
        //       2.2.1) 如果当值 scheduler 所在 host 存在且状态是 UP，则本机不需要竞选 scheduler；
        //       2.2.2) 如果当值 scheduler 所在 host 不存在或状态不是 UP，则本机需要竞选 scheduler；
        //
        DistributedTaskSchedulerDo currentOnDutySchedulerDo = null;
        try {
            currentOnDutySchedulerDo = this.distributedTaskSchedulerRepository.findEffective();
        } catch (Exception e) {
            LOGGER.error("[d-task] fail to find effective scheduler", e);
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
                    LOGGER.error("[d-task] cannot find server {} ({})",
                            currentOnDutySchedulerDo.getHostname(),
                            currentOnDutySchedulerDo.getIpAddress());
                    requiredToCampaignScheduler = true;
                } else {
                    if (!DistributedServerStatus.UP.equals(thatServerDo.getStatus())) {
                        requiredToCampaignScheduler = true;
                    }

                    // 当值 scheduler 不是本机，其所在 host 也工作正常
                }
            }
        }

        //
        // Step 1.3, 本机需要参与竞选新的 scheduler
        //
        if (requiredToCampaignScheduler) {
            // Step 1.3.1, 先要获取悲观锁
            LOGGER.info("[d-task] begin to get lock for server {} ({})", thisServerHostname, thisServerIpAddress);
            try {
                // 获取悲观锁
                LockDo lockDo = new LockDo();
                lockDo.setName("Task Scheduler");
                lockDo.setResource("NA");
                lockDo.setVersion(1L);
                lockDo.setCreatedTimestamp(LocalDateTime.now());
                lockDo.setLastModifiedTimestamp(LocalDateTime.now());
                this.lockRepository.save(lockDo);
                LOGGER.info("[d-task] end to get lock for server {} ({})", thisServerHostname, thisServerIpAddress);
            } catch (Exception e) {
                LOGGER.info("[d-task] fail to get lock server {} ({})", thisServerHostname, thisServerIpAddress);
                return;
            }

            // Step 1.3.2, 已经获取到悲观锁，但还不能代表本机竞选新的 scheduler 成功
            // 需要继续检查此刻最新的当值 scheduler 还是不是本次调用开始时发现的那个当值 scheduler。
            // 如果不是，表示本次调用的同一时间，有其它主机当选了新的 scheduler。
            // 如果是，代表本机竞选 scheduler 成功，作为新的当值 scheduler

            try {
                DistributedTaskSchedulerDo latestOnDutySchedulerDo = null;
                try {
                    latestOnDutySchedulerDo = this.distributedTaskSchedulerRepository.findEffective();
                } catch (Exception e) {
                    LOGGER.error("[d-task] fail to find effective scheduler", e);
                    throw new AbcResourceIntegrityException("fail to find effective scheduler");
                }

                if (currentOnDutySchedulerDo == null) {
                    if (latestOnDutySchedulerDo != null) {
                        // stop
                        // 本次调用同一时间，已经有其它主机当选为新的 scheduler。
                        // 本机退出本次竞选。
                        LOGGER.info("[d-task] this server {} ({}) abandons to campaign scheduler",
                                thisServerHostname, thisServerIpAddress);
                        return;
                    }
                } else {
                    if (!currentOnDutySchedulerDo.getHostname().equals(latestOnDutySchedulerDo.getHostname())
                            || !currentOnDutySchedulerDo.getIpAddress().equals(latestOnDutySchedulerDo.getIpAddress())) {
                        // stop
                        // 本次调用同一时间，已经有其它主机当选为新的 scheduler。
                        // 本机退出本次竞选。
                        LOGGER.info("[d-task] this server {} ({}) abandons to campaign scheduler",
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
                        this.distributedTaskSchedulerRepository.save(currentOnDutySchedulerDo);
                    } catch (Exception e) {
                        LOGGER.error("[d-task] fail to invalidate the current on duty scheduler {} ({})",
                                currentOnDutySchedulerDo.getHostname(), currentOnDutySchedulerDo.getIpAddress(), e);
                        throw new AbcResourceIntegrityException("fail to invalidate the old on duty scheduler");
                    }
                }

                // 再将本机设置为新的 effective scheduler

                DistributedTaskSchedulerDo newDistributedTaskSchedulerDo = new DistributedTaskSchedulerDo();
                newDistributedTaskSchedulerDo.setUid(this.idHelper.getNextDistributedId(DistributedTaskSchedulerDo.RESOURCE_NAME));
                newDistributedTaskSchedulerDo.setHostname(thisServerHostname);
                newDistributedTaskSchedulerDo.setIpAddress(thisServerIpAddress);
                newDistributedTaskSchedulerDo.setEffective(Boolean.TRUE);
                newDistributedTaskSchedulerDo.setEffectiveFrom(LocalDateTime.now());
                BaseDo.create(newDistributedTaskSchedulerDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
                this.distributedTaskSchedulerRepository.save(newDistributedTaskSchedulerDo);

                latestOnDutyScheduler = true;
            } finally {
                // Step 1.3.4, 释放悲观锁
                LockDo lockDo = this.lockRepository.findByNameAndResourceAndVersion(
                        "Task Scheduler", "NA", 1L);
                if (lockDo != null) {
                    this.lockRepository.delete(lockDo);
                }
            }
        }

        if (!latestOnDutyScheduler) {
            LOGGER.info("[d-task] this server {} ({}) is not the latest on duty scheduler, not working",
                    thisServerHostname,
                    thisServerIpAddress);
            return;
        }

        //
        // Step 2, core-processing
        //

        // 本机是当值 scheduler，则需要开始 schedule tasks 工作。
        try {
            auditTasks();
            scheduleTasks();
        } catch (Exception e) {
            LOGGER.error("[d-task] fail to schedule tasks", e);
        }
    }

    private void auditTasks() throws AbcUndefinedException {
        LocalDateTime nowDateTime = LocalDateTime.now();
        LocalDateTime thresholdBeginTimestamp = nowDateTime.minusHours(4);

        Specification<DistributedTaskDo> specification =
                new Specification<DistributedTaskDo>() {
                    @Override
                    public Predicate toPredicate(Root<DistributedTaskDo> root, CriteriaQuery<?> query,
                                                 CriteriaBuilder criteriaBuilder) {
                        List<Predicate> predicateList = new ArrayList<>();

                        CriteriaBuilder.In<TaskStatusEnum> in = criteriaBuilder.in(root.get("status"));
                        in.value(TaskStatusEnum.SCHEDULING);
                        in.value(TaskStatusEnum.RUNNING);
                        in.value(TaskStatusEnum.CANCELLING);
                        in.value(TaskStatusEnum.FAILING);
                        predicateList.add(in);

                        predicateList.add(criteriaBuilder.lessThan(root.get("beginTimestamp"),
                                thresholdBeginTimestamp));

                        return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
                    }
                };

        Iterable<DistributedTaskDo> distributedTaskDoIterable =
                this.distributedTaskRepository.findAll(specification, Sort.by(Sort.Order.asc(BaseDo.ID_FIELD_NAME)));
        distributedTaskDoIterable.forEach(distributedTaskDo -> {
            distributedTaskDo.setStatus(TaskStatusEnum.FAILED);
            distributedTaskDo.setRemark("timeout");
            BaseDo.update(distributedTaskDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());

            LOGGER.warn("[d-task] found timeout task::uid={}, task_uid={}, begin_timestamp={}, duration={}",
                    distributedTaskDo.getUid(),
                    distributedTaskDo.getUid(), distributedTaskDo.getBeginTimestamp(),
                    AbcDateUtils.format(Duration.between(distributedTaskDo.getBeginTimestamp(), nowDateTime).toMillis()));

        });
        this.distributedTaskRepository.saveAll(distributedTaskDoIterable);
    }

    private void scheduleTasks() throws AbcUndefinedException {
        Specification<DistributedTaskDo> specification = new Specification<DistributedTaskDo>() {
            @Override
            public Predicate toPredicate(Root<DistributedTaskDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();

                CriteriaBuilder.In<TaskStatusEnum> in = criteriaBuilder.in(root.get("status"));
                in.value(TaskStatusEnum.CREATED);
                predicateList.add(in);

                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        Iterable<DistributedTaskDo> distributedTaskDoIterable = this.distributedTaskRepository.findAll(
                specification,
                Sort.by(Sort.Order.asc(BaseDo.ID_FIELD_NAME)));
        distributedTaskDoIterable.forEach(distributedTaskDo -> {
            scheduleTask(distributedTaskDo);
        });
    }

    private void scheduleTask(DistributedTaskDo distributedTaskDo) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
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
        if (distributedServerDoList.isEmpty()) {
            LOGGER.warn("[d-task] no d-server");
        }


        //
        // Step 2, core-processing
        //

        // 随机选择一个 server 作为 executor
        Random random = new Random();
        int index = random.nextInt(distributedServerDoList.size());
        DistributedServerDo distributedServerDo = distributedServerDoList.get(index);
        assignTaskToExecutor(distributedTaskDo, distributedServerDo);

        //
        // Step 3, post-processing
        //
    }

    private boolean assignTaskToExecutor(DistributedTaskDo distributedTaskDo, DistributedServerDo serverDo) {
        StringBuilder url = new StringBuilder();
        if (Boolean.TRUE.equals(this.serverSslEnabled)) {
            url.append("https://");
        } else {
            url.append("http://");
        }
        url.append(serverDo.getIpAddress()).append(":").append(this.serverPort)
                .append("/utilities/d-task/tasks/start").append("?task_uid=").append(distributedTaskDo.getUid());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        StartDistributedTaskDto startDistributedTaskDto = new StartDistributedTaskDto();
        HttpEntity<StartDistributedTaskDto> httpEntity = new HttpEntity<>(startDistributedTaskDto, httpHeaders);

        long beginTime = System.currentTimeMillis();
        LOGGER.info("[d-task] begin to assign task {} to executor {} ({}), duration={}",
                distributedTaskDo.getUid(),
                serverDo.getHostname(),
                serverDo.getIpAddress(),
                AbcDateUtils.format(System.currentTimeMillis() - beginTime));
        try {
            Response response = this.restTemplate.postForObject(url.toString(), httpEntity, Response.class);
            if (response.isSuccessful()) {
                LOGGER.info("[d-task] end to assign task {} to executor {} ({}), duration={}",
                        distributedTaskDo.getUid(),
                        serverDo.getHostname(),
                        serverDo.getIpAddress(),
                        AbcDateUtils.format(System.currentTimeMillis() - beginTime));
                return true;
            } else {
                LOGGER.error("[d-task] fail to assign task {} to executor {} ({}), err_code={}, err_message={}, " +
                                "duration={}",
                        distributedTaskDo.getUid(),
                        serverDo.getHostname(),
                        serverDo.getIpAddress(),
                        response.getErrCode(),
                        response.getErrMessage(),
                        AbcDateUtils.format(System.currentTimeMillis() - beginTime));
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("[d-task] fail to assign task {} to executor {} ({}), duration={}",
                    distributedTaskDo.getUid(),
                    serverDo.getHostname(),
                    serverDo.getIpAddress(),
                    AbcDateUtils.format(System.currentTimeMillis() - beginTime),
                    e);
            return false;
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
        // clean impacted task scheduler
        //
        DistributedTaskSchedulerDo distributedTaskSchedulerDo = this.distributedTaskSchedulerRepository.findEffective();
        if (distributedTaskSchedulerDo != null) {
            if (distributedTaskSchedulerDo.getHostname().equals(event.getHostname())
                    && distributedTaskSchedulerDo.getIpAddress().equals(event.getIpAddress())) {
                distributedTaskSchedulerDo.setEffective(Boolean.FALSE);
                distributedTaskSchedulerDo.setEffectiveTo(LocalDateTime.now());
                BaseDo.update(distributedTaskSchedulerDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
                this.distributedTaskSchedulerRepository.save(distributedTaskSchedulerDo);

                // release lock if have
                LockDo lockDo = this.lockRepository.findByNameAndResourceAndVersion(
                        "Task Scheduler", "NA", 1L);
                if (lockDo != null) {
                    this.lockRepository.delete(lockDo);
                }
            }
        }

        //
        // clean impacted task(s)
        //
        Specification<DistributedTaskDo> specification = new Specification<DistributedTaskDo>() {
            @Override
            public Predicate toPredicate(Root<DistributedTaskDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();

                predicateList.add(criteriaBuilder.equal(root.get("executorHostname"), event.getHostname()));
                predicateList.add(criteriaBuilder.equal(root.get("executorIpAddress"), event.getIpAddress()));

                CriteriaBuilder.In<TaskStatusEnum> in = criteriaBuilder.in(root.get("status"));
                in.value(TaskStatusEnum.SCHEDULING);
                in.value(TaskStatusEnum.RUNNING);
                in.value(TaskStatusEnum.CANCELLING);
                in.value(TaskStatusEnum.FAILING);
                predicateList.add(in);

                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        LocalDateTime now = LocalDateTime.now();
        Sort sort = Sort.by(Sort.Order.asc(BaseDo.ID_FIELD_NAME));
        Iterable<DistributedTaskDo> distributedTaskDoIterable =
                this.distributedTaskRepository.findAll(specification, sort);
        distributedTaskDoIterable.forEach(distributedTaskDo -> {
            distributedTaskDo.setStatus(TaskStatusEnum.CANCELED);
            distributedTaskDo.setRemark("server down");
            distributedTaskDo.setEndTimestamp(LocalDateTime.now());
            BaseDo.update(distributedTaskDo, InfrastructureConstants.ROOT_USER_UID, now);
        });
        this.distributedTaskRepository.saveAll(distributedTaskDoIterable);
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
