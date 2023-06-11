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
 * Distributed file system (dfs) service agent
 *
 * @author bbottong
 */
@TypeDef(name = "json", typeClass = JsonStringType.class)
@TinyId(bizType = DfsServiceAgentDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = DfsServiceAgentDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class DfsServiceAgentDo extends BaseDo {
    public static final String RESOURCE_NAME = "k3_dfs_svc_agent";
    public static final String RESOURCE_SYMBOL = "DFS service agent";

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
     * Preferred
     */
    @Column(name = "is_preferred")
    private Boolean preferred;

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
}