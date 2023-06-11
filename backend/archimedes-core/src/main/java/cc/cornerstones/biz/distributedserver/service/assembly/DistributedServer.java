package cc.cornerstones.biz.distributedserver.service.assembly;

import cc.cornerstones.almond.constants.InfrastructureConstants;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.AbcTuple2;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.utils.AbcNetworkUtils;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.distributedserver.entity.DistributedServerDo;
import cc.cornerstones.biz.distributedserver.persistence.DistributedServerRepository;
import cc.cornerstones.biz.distributedserver.share.constants.DistributedServerStatus;
import cc.cornerstones.biz.share.event.DistributedServerDownEvent;
import cc.cornerstones.biz.share.event.EventBusManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DistributedServer implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedServer.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private DistributedServerRepository distributedServerRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${server.port}")
    private Integer serverPort;

    @Value("${server.ssl.enabled}")
    private Boolean serverSslEnabled;

    private String serverHostname;

    private String serverIpAddress;

    private Boolean serverRegistered = false;

    /**
     * Handle an application event.
     *
     * @param event the event to respond to
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        register();
    }

    public String getServerHostname() {
        return serverHostname;
    }

    public String getServerIpAddress() {
        return serverIpAddress;
    }

    public void register() throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        // find out the local hostname and ip address
        List<AbcTuple2<String, String>> tuples = AbcNetworkUtils.getServerHostnameAndIpAddress();
        if (CollectionUtils.isEmpty(tuples)) {
            throw new AbcResourceNotFoundException("server hostname and ip address");
        }
        for (AbcTuple2<String, String> tuple : tuples) {
            String candidateServerHostname = tuple.f;
            String candidateServerIpAddress = tuple.s;

            StringBuilder url = new StringBuilder();
            if (Boolean.TRUE.equals(this.serverSslEnabled)) {
                url.append("https://");
            } else {
                url.append("http://");
            }
            url.append(candidateServerIpAddress).append(":").append(this.serverPort)
                    .append("/utilities/d-server/server-ip-address");
            try {
                this.restTemplate.getForObject(url.toString(), String.class);
                this.serverIpAddress = candidateServerIpAddress;
                this.serverHostname = candidateServerHostname;
                break;
            } catch (Exception e) {
                LOGGER.warn("failed to ping d-server by {}", url, e);
                continue;
            }
        }
        if (ObjectUtils.isEmpty(this.serverHostname)
                || ObjectUtils.isEmpty(this.serverIpAddress)) {
            throw new AbcResourceNotFoundException("server hostname and ip address");
        }

        //
        // Step 2, core-processing
        //
        DistributedServerDo serverDo = this.distributedServerRepository.findByHostnameAndIpAddress(this.serverHostname,
                this.serverIpAddress);
        LocalDateTime now = LocalDateTime.now();
        if (serverDo == null) {
            serverDo = new DistributedServerDo();
            serverDo.setUid(this.idHelper.getNextDistributedId(DistributedServerDo.RESOURCE_NAME));
            serverDo.setHostname(this.serverHostname);
            serverDo.setIpAddress(this.serverIpAddress);
            serverDo.setStatus(DistributedServerStatus.UP);
            serverDo.setLastHeartbeatTimestamp(now);
            BaseDo.create(serverDo, InfrastructureConstants.ROOT_USER_UID, now);
            this.distributedServerRepository.save(serverDo);
        } else {
            LOGGER.info("[d-server] found existing server {} ({}), status={}, " +
                            "last_heartbeat_timestamp={}",
                    serverDo.getHostname(),
                    serverDo.getIpAddress(),
                    serverDo.getStatus(),
                    serverDo.getLastHeartbeatTimestamp());
            if (!DistributedServerStatus.UP.equals(serverDo.getStatus())) {
                LOGGER.info("[d-server] convert the status of an existing server {} ({}), from {} to {}",
                        serverDo.getHostname(), serverDo.getIpAddress(),
                        serverDo.getStatus(), DistributedServerStatus.UP);
                serverDo.setStatus(DistributedServerStatus.UP);
                serverDo.setLastHeartbeatTimestamp(now);
                BaseDo.update(serverDo, InfrastructureConstants.ROOT_USER_UID, now);
                this.distributedServerRepository.save(serverDo);
            }
        }

        //
        // Step 3, post-processing
        //
        this.serverRegistered = true;
    }

    @PreDestroy
    public void deregister() throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        if (!Boolean.TRUE.equals(this.serverRegistered)) {
            return;
        }

        DistributedServerDo distributedServerDo = this.distributedServerRepository.findByHostnameAndIpAddress(this.serverHostname,
                this.serverIpAddress);
        if (distributedServerDo == null) {
            LOGGER.warn("[d-server] cannot find server {} ({})", this.serverHostname,
                    this.serverIpAddress);
            return;
        }

        //
        // Step 2, core-processing
        //
        LOGGER.info("[d-server] convert the status of an existing server {} ({}), from {} to {}",
                distributedServerDo.getHostname(), distributedServerDo.getIpAddress(),
                distributedServerDo.getStatus(), DistributedServerStatus.DOWN);
        distributedServerDo.setStatus(DistributedServerStatus.DOWN);
        BaseDo.update(distributedServerDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
        this.distributedServerRepository.save(distributedServerDo);

        //
        // Step 3, post-processing
        //

        // event post
        DistributedServerDownEvent event = new DistributedServerDownEvent();
        event.setHostname(distributedServerDo.getHostname());
        event.setIpAddress(distributedServerDo.getIpAddress());
        this.eventBusManager.send(event);
    }

    @Scheduled(cron = "0/5 * * * * ?")
    public void heartbeat() throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        if (!Boolean.TRUE.equals(this.serverRegistered)) {
            return;
        }

        DistributedServerDo distributedServerDo = this.distributedServerRepository.findByHostnameAndIpAddress(this.serverHostname,
                this.serverIpAddress);
        if (distributedServerDo == null) {
            LOGGER.warn("[d-server] cannot find server {} ({})",
                    this.serverHostname, this.serverIpAddress);
            return;
        }

        //
        // Step 2, core-processing
        //
        distributedServerDo.setLastHeartbeatTimestamp(LocalDateTime.now());
        BaseDo.update(distributedServerDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
        this.distributedServerRepository.save(distributedServerDo);

        //
        // Step 3, post-processing
        //
    }

}
