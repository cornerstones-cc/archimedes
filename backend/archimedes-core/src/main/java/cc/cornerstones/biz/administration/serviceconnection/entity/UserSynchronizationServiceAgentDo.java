package cc.cornerstones.biz.administration.serviceconnection.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * User synchronization service agent
 *
 * @author bbottong
 */
@TypeDef(name = "json", typeClass = JsonStringType.class)
@TinyId(bizType = UserSynchronizationServiceAgentDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100,
        remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = UserSynchronizationServiceAgentDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class UserSynchronizationServiceAgentDo extends BaseDo {
    public static final String RESOURCE_NAME = "k3_user_sync_svc_agent";
    public static final String RESOURCE_SYMBOL = "User synchronization service agent";

    /**
     * UID
     */
    @Column(name = "uid")
    private Long uid;

    /**
     * Name
     * <p>
     * A name is used to identify the object.
     */
    @Column(name = "name", length = 129)
    private String name;

    /**
     * Object Name
     * <p>
     * An object name is how the object is referenced programmatically.
     */
    @Column(name = "object_name", length = 150)
    private String objectName;

    /**
     * Description
     * <p>
     * A meaning description helps you remembers the differences between objects.
     */
    @Column(name = "description", length = 255)
    private String description;

    /**
     * Sequence
     */
    @Column(name = "sequence")
    private Float sequence;

    /**
     * Enabled
     */
    @Column(name = "is_enabled")
    private Boolean enabled;

    /**
     * Service component uid
     */
    @Column(name = "service_component_uid")
    private Long serviceComponentUid;

    /**
     * Configuration
     */
    @Lob
    @Column(name = "configuration")
    private String configuration;

    /**
     * Cron expression for scheduling
     */
    @Column(name = "cron_expression")
    private String cronExpression;

    /**
     * Job UID
     */
    @Column(name = "job_uid")
    private Long jobUid;
}