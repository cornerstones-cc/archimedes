package cc.cornerstones.biz.administration.serviceconnection.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.pf4j.share.types.PluginProfile;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import cc.cornerstones.biz.administration.serviceconnection.share.constants.ServiceComponentTypeEnum;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * User synchronization service component
 *
 * @author bbottong
 */
@TypeDef(name = "json", typeClass = JsonStringType.class)
@TinyId(bizType = UserSynchronizationServiceComponentDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100,
        remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = UserSynchronizationServiceComponentDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class UserSynchronizationServiceComponentDo extends BaseDo {
    public static final String RESOURCE_NAME = "k3_user_sync_svc_component";
    public static final String RESOURCE_SYMBOL = "User synchronization service component";

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
     * Type
     */
    @Column(name = "type")
    private ServiceComponentTypeEnum type;

    /**
     * for BUILTIN type
     * Resource name
     */
    @Column(name = "resource_name", length = 255)
    private String resourceName;

    /**
     * for BUILTIN type
     * Entry class name
     */
    @Column(name = "entry_class_name", length = 255)
    private String entryClassName;

    /**
     * for PLUGIN type
     * Front-end component file id
     */
    @Column(name = "front_end_component_file_id", length = 255)
    private String frontEndComponentFileId;

    /**
     * for PLUGIN type
     * DFS service agent uid of front-end component file id
     */
    @Column(name = "front_end_dfs_service_agent_uid")
    private Long dfsServiceAgentUidOfFrontEndComponentFileId;

    /**
     * for PLUGIN type
     * Back-end component file id
     */
    @Column(name = "back_end_component_file_id", length = 255)
    private String backEndComponentFileId;

    /**
     * for PLUGIN type
     * DFS service agent uid of back-end component file id
     */
    @Column(name = "back_end_dfs_service_agent_uid")
    private Long dfsServiceAgentUidOfBackEndComponentFileId;

    /**
     * for PLUGIN type
     * Back-end component metadata
     */
    @Type(type = "json")
    @Column(name = "back_end_component_metadata", columnDefinition = "json")
    private PluginProfile backEndComponentMetadata;

    /**
     * for both PLUGIN and BUILTIN type
     * configuration
     */
    @Lob
    @Column(name = "configuration_template")
    private String configurationTemplate;
}