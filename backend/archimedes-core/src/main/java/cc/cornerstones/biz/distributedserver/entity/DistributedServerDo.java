package cc.cornerstones.biz.distributedserver.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import cc.cornerstones.biz.distributedserver.share.constants.DistributedServerStatus;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * Distributed server
 *
 * @author bbottong
 */
@TypeDef(name = "json", typeClass = JsonStringType.class)
@TinyId(bizType = DistributedServerDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = DistributedServerDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class DistributedServerDo extends BaseDo {
    public static final String RESOURCE_NAME = "t7_server";
    public static final String RESOURCE_SYMBOL = "Distributed server";

    /**
     * UID
     */
    @Column(name = "uid")
    private Long uid;

    /**
     * Hostname
     */
    @Column(name = "hostname", length = 255)
    private String hostname;

    /**
     * IP Address
     */
    @Column(name = "ip_address", length = 255)
    private String ipAddress;

    /**
     * Status
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status")
    private DistributedServerStatus status;

    /**
     * Last Heartbeat Timestamp
     */
    @Column(name = "last_heartbeat_timestamp")
    private LocalDateTime lastHeartbeatTimestamp;
}