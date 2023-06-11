package cc.cornerstones.biz.distributedserver.service.assembly;

import cc.cornerstones.almond.constants.InfrastructureConstants;
import cc.cornerstones.almond.exceptions.AbcResourceIntegrityException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.lock.entity.LockDo;
import cc.cornerstones.arbutus.lock.persistence.LockRepository;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.distributedserver.entity.DistributedServerMaintainerDo;
import cc.cornerstones.biz.distributedserver.persistence.DistributedServerRepository;
import cc.cornerstones.biz.distributedserver.persistence.DistributedServerMaintainerRepository;
import cc.cornerstones.biz.distributedserver.entity.DistributedServerDo;
import cc.cornerstones.biz.distributedserver.share.constants.DistributedServerStatus;
import cc.cornerstones.biz.share.event.DistributedServerDownEvent;
import cc.cornerstones.biz.share.event.DistributedServerUpEvent;
import cc.cornerstones.biz.share.event.EventBusManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;

@Component
public class DistributedServerMaintainer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedServerMaintainer.class);

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
    private DistributedServerMaintainerRepository distributedMaintainerRepository;

    @Scheduled(cron = "0/59 * * * * ?")
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
            LOGGER.info("[d-server] waiting for server hostname and ip address initialized");
            return;
        }

        //
        // Step 1.2, 找出正在当值的 maintainer，判断本机是否需要参与竞选新的 maintainer

        // 1) 如果找不到，本机需要竞选 maintainer；
        // 2) 如果找到，继续分更细致的情况：
        //    2.1) 如果当值 maintainer 就是本机，则本机继续当值；
        //    2.2) 如果当值 maintainer 不是本机，继续分更细致的情况：
        //       2.2.1) 如果当值 maintainer 所在 host 存在且状态是 UP，则本机不需要竞选 maintainer；
        //       2.2.2) 如果当值 maintainer 所在 host 不存在或状态不是 UP，则本机需要竞选 maintainer；
        //
        DistributedServerMaintainerDo currentOnDutyMaintainerDo = null;
        try {
            currentOnDutyMaintainerDo = this.distributedMaintainerRepository.findEffective();
        } catch (Exception e) {
            LOGGER.error("[d-server] fail to find effective maintainer", e);
            throw new AbcResourceIntegrityException("fail to find effective maintainer");
        }

        // 本机是否需要竞选 maintainer
        boolean requiredToCampaignMaintainer = false;
        // 本机是否最新当值 maintainer
        boolean latestOnDutyMaintainer = false;

        if (currentOnDutyMaintainerDo == null) {
            requiredToCampaignMaintainer = true;
        } else {
            if (thisServerHostname.equals(currentOnDutyMaintainerDo.getHostname())
                    && thisServerIpAddress.equals(currentOnDutyMaintainerDo.getIpAddress())) {
                // 当值 maintainer 就是本机，则本机继续当值
                latestOnDutyMaintainer = true;
            } else {
                // 当值 maintainer 不是本机，继续分更细致的情况
                DistributedServerDo thatServerDo =
                        this.distributedServerRepository.findByHostnameAndIpAddress(
                                currentOnDutyMaintainerDo.getHostname(),
                                currentOnDutyMaintainerDo.getIpAddress());
                if (thatServerDo == null) {
                    LOGGER.error("[d-server] cannot find that server {} ({})",
                            currentOnDutyMaintainerDo.getHostname(),
                            currentOnDutyMaintainerDo.getIpAddress());
                    requiredToCampaignMaintainer = true;
                } else {
                    if (!DistributedServerStatus.UP.equals(thatServerDo.getStatus())) {
                        requiredToCampaignMaintainer = true;
                    } else {
                        // that server 没有正常退出，也没有其它 maintainer 负责清理 that server 遗留状态的情况
                        LocalDateTime lastHeartbeatTimestampOfThatServer = thatServerDo.getLastHeartbeatTimestamp();
                        if (lastHeartbeatTimestampOfThatServer == null) {
                            requiredToCampaignMaintainer = true;
                        } else {
                            LocalDateTime thresholdDateTime = LocalDateTime.now().minusSeconds(17);

                            if (lastHeartbeatTimestampOfThatServer.isBefore(thresholdDateTime)) {
                                // unexpected
                                requiredToCampaignMaintainer = true;
                            } else {
                                // 当值 maintainer 不是本机，其所在 host 也工作正常，继续当值
                            }
                        }
                    }
                }
            }
        }

        //
        // Step 1.3, 本机需要参与竞选新的 maintainer
        //
        if (requiredToCampaignMaintainer) {
            // Step 1.3.1, 先要获取悲观锁
            LOGGER.info("[d-server] begin to get lock for server {} ({})", thisServerHostname, thisServerIpAddress);
            try {
                // 获取悲观锁
                LockDo lockDo = new LockDo();
                lockDo.setName("Distributed Maintainer");
                lockDo.setResource("NA");
                lockDo.setVersion(1L);
                lockDo.setCreatedTimestamp(LocalDateTime.now());
                lockDo.setLastModifiedTimestamp(LocalDateTime.now());
                this.lockRepository.save(lockDo);
                LOGGER.info("[d-server] end to get lock for server {} ({})", thisServerHostname, thisServerIpAddress);
            } catch (Exception e) {
                LOGGER.info("[d-server] fail to get lock for server {} ({})", thisServerHostname, thisServerIpAddress);
                return;
            }

            // Step 1.3.2, 已经获取到悲观锁，但还不能代表本机竞选新的 maintainer 成功
            // 需要继续检查此刻最新的当值 maintainer 还是不是本次调用开始时发现的那个当值 maintainer。
            // 如果不是，表示本次调用的同一时间，有其它主机当选了新的 maintainer。
            // 如果是，代表本机竞选 maintainer 成功，作为新的当值 maintainer

            try {
                DistributedServerMaintainerDo latestOnDutyMaintainerDo = null;
                try {
                    latestOnDutyMaintainerDo = this.distributedMaintainerRepository.findEffective();
                } catch (Exception e) {
                    LOGGER.error("[d-server] fail to find effective maintainer", e);
                    throw new AbcResourceIntegrityException("fail to find effective maintainer");
                }

                if (currentOnDutyMaintainerDo == null) {
                    if (latestOnDutyMaintainerDo != null) {
                        // stop
                        // 本次调用同一时间，已经有其它主机当选为新的 maintainer。
                        // 本机退出本次竞选。
                        LOGGER.info("[d-server] this server {} ({}) abandons to campaign maintainer",
                                thisServerHostname, thisServerIpAddress);
                        return;
                    }
                } else {
                    if (!currentOnDutyMaintainerDo.getHostname().equals(latestOnDutyMaintainerDo.getHostname())
                            || !currentOnDutyMaintainerDo.getIpAddress().equals(latestOnDutyMaintainerDo.getIpAddress())) {
                        // stop
                        // 本次调用同一时间，已经有其它主机当选为新的 maintainer。
                        // 本机退出本次竞选。
                        LOGGER.info("[d-server] this server {} ({}) abandons to campaign maintainer",
                                thisServerHostname, thisServerIpAddress);
                        return;
                    }
                }

                // Step 1.3.3, 已经确认本机当选新的 maintainer

                // 先 invalidate the current maintainer

                if (currentOnDutyMaintainerDo != null) {
                    try {
                        currentOnDutyMaintainerDo.setEffective(Boolean.FALSE);
                        currentOnDutyMaintainerDo.setEffectiveTo(LocalDateTime.now());
                        BaseDo.update(currentOnDutyMaintainerDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
                        this.distributedMaintainerRepository.save(currentOnDutyMaintainerDo);
                    } catch (Exception e) {
                        LOGGER.error("[d-server] fail to invalidate the current on duty maintainer {} ({})",
                                currentOnDutyMaintainerDo.getHostname(), currentOnDutyMaintainerDo.getIpAddress(), e);
                        throw new AbcResourceIntegrityException("fail to invalidate the old on duty maintainer");
                    }
                }

                // 再将本机设置为新的 effective maintainer

                DistributedServerMaintainerDo newDistributedServerMaintainerDo = new DistributedServerMaintainerDo();
                newDistributedServerMaintainerDo.setUid(this.idHelper.getNextDistributedId(DistributedServerMaintainerDo.RESOURCE_NAME));
                newDistributedServerMaintainerDo.setHostname(thisServerHostname);
                newDistributedServerMaintainerDo.setIpAddress(thisServerIpAddress);
                newDistributedServerMaintainerDo.setEffective(Boolean.TRUE);
                newDistributedServerMaintainerDo.setEffectiveFrom(LocalDateTime.now());
                BaseDo.create(newDistributedServerMaintainerDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
                this.distributedMaintainerRepository.save(newDistributedServerMaintainerDo);

                latestOnDutyMaintainer = true;
            } finally {
                // Step 1.3.4, 释放悲观锁
                LockDo lockDo = this.lockRepository.findByNameAndResourceAndVersion(
                        "Distributed Maintainer", "NA", 1L);
                if (lockDo != null) {
                    this.lockRepository.delete(lockDo);
                }
            }
        }

        if (!latestOnDutyMaintainer) {
            LOGGER.info("[d-server] this server {} ({}) is not the latest on duty maintainer",
                    thisServerHostname,
                    thisServerIpAddress);
            return;
        }

        //
        // Step 2, core-processing
        //

        // 本机是当值 maintainer，则需要开始 maintain servers 工作。
        try {
            LOGGER.info("[d-server] this server {} ({}) is the latest on duty maintainer, begin to maintain " +
                            "servers",
                    thisServerHostname,
                    thisServerIpAddress);

            maintainServers();

            LOGGER.info("[d-server] this server {} ({}) is the latest on duty maintainer, end to maintain " +
                            "servers",
                    thisServerHostname,
                    thisServerIpAddress);
        } catch (Exception e) {
            LOGGER.error("[d-server] this server {} ({}) is the latest on duty maintainer, fail to maintain " +
                    "servers", thisServerHostname, thisServerIpAddress, e);
        }
    }

    private void maintainServers() throws AbcUndefinedException {
        LocalDateTime thresholdDateTime = LocalDateTime.now().minusSeconds(17);

        this.distributedServerRepository.findAll().forEach(serverDo -> {
            LocalDateTime lastHeartbeatTimestamp = serverDo.getLastHeartbeatTimestamp();
            if (lastHeartbeatTimestamp == null) {
                // unexpected
                switch (serverDo.getStatus()) {
                    case UP: {
                        serverDo.setStatus(DistributedServerStatus.DOWN);
                        BaseDo.update(serverDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
                        this.distributedServerRepository.save(serverDo);

                        LOGGER.warn("[d-server] heartbeat detects server {} ({}) down, no heartbeat",
                                serverDo.getHostname(), serverDo.getIpAddress());

                        postServerDownEvent(serverDo);
                    }
                    break;
                    default:
                        break;
                }
            } else {
                if (lastHeartbeatTimestamp.isBefore(thresholdDateTime)) {
                    // unexpected
                    switch (serverDo.getStatus()) {
                        case UP: {
                            serverDo.setStatus(DistributedServerStatus.DOWN);
                            BaseDo.update(serverDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
                            this.distributedServerRepository.save(serverDo);

                            LOGGER.warn("heartbeat detects server {} ({}) down, long time no heartbeat",
                                    serverDo.getHostname(), serverDo.getIpAddress());

                            postServerDownEvent(serverDo);
                        }
                        break;
                        default:
                            break;
                    }
                } else {
                    // expected
                    switch (serverDo.getStatus()) {
                        case DOWN: {
                            serverDo.setStatus(DistributedServerStatus.UP);
                            BaseDo.update(serverDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
                            this.distributedServerRepository.save(serverDo);

                            LOGGER.warn("heartbeat detects server {} ({}) up", serverDo.getHostname(), serverDo.getIpAddress());

                            postServerUpEvent(serverDo);
                        }
                        break;
                        default:
                            break;
                    }
                }
            }
        });
    }

    private void postServerDownEvent(DistributedServerDo distributedServerDo) {
        // event post
        DistributedServerDownEvent event = new DistributedServerDownEvent();
        event.setHostname(distributedServerDo.getHostname());
        event.setIpAddress(distributedServerDo.getIpAddress());
        this.eventBusManager.send(event);
    }

    private void postServerUpEvent(DistributedServerDo distributedServerDo) {
        // event post
        DistributedServerUpEvent event = new DistributedServerUpEvent();
        event.setHostname(distributedServerDo.getHostname());
        event.setIpAddress(distributedServerDo.getIpAddress());
        this.eventBusManager.send(event);
    }
}
