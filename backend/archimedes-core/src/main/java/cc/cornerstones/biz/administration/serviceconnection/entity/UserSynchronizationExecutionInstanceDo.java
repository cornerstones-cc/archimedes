package cc.cornerstones.biz.administration.serviceconnection.entity;

import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import cc.cornerstones.biz.administration.serviceconnection.dto.UserSynchronizationExecutionSummaryDto;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * User synchronization execution instance
 *
 * @author bbottong
 */
@TypeDef(name = "json", typeClass = JsonStringType.class)
@TinyId(bizType = UserSynchronizationExecutionInstanceDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100,
        remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = UserSynchronizationExecutionInstanceDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class UserSynchronizationExecutionInstanceDo extends BaseDo {
    public static final String RESOURCE_NAME = "k3_user_sync_exec_instance";
    public static final String RESOURCE_SYMBOL = "User synchronization execution instance";

    /**
     * UID
     */
    @Column(name = "uid")
    private Long uid;

    /**
     * Status
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status")
    private JobStatusEnum status;

    /**
     * Begin timestamp
     */
    @Column(name = "begin_timestamp")
    private LocalDateTime beginTimestamp;

    /**
     * End timestamp
     */
    @Column(name = "end_timestamp")
    private LocalDateTime endTimestamp;

    /**
     * Remark
     */
    @Column(name = "remark", length = 255)
    private String remark;

    /**
     * Summary
     */
    @Type(type = "json")
    @Column(name = "summary", columnDefinition = "json")
    private UserSynchronizationExecutionSummaryDto summary;

    /**
     * Service agent uid
     */
    @Column(name = "service_agent_uid")
    private Long serviceAgentUid;
}