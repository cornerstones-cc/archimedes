package cc.cornerstones.biz.administration.serviceconnection.dto;

import cc.cornerstones.almond.types.BaseDto;
import cc.cornerstones.almond.types.UserBriefInformation;
import cc.cornerstones.arbutus.pf4j.share.types.PluginProfile;
import cc.cornerstones.biz.administration.serviceconnection.share.constants.ServiceComponentTypeEnum;
import lombok.Data;

@Data
public class DataPermissionServiceComponentDto extends BaseDto {

    /**
     * UID
     */
    private Long uid;

    /**
     * Name
     * <p>
     * A name is used to identify the object.
     */
    private String name;

    /**
     * Object Name
     * <p>
     * An object name is how the object is referenced programmatically.
     */
    private String objectName;

    /**
     * Description
     * <p>
     * A meaning description helps you remembers the differences between objects.
     */
    private String description;

    /**
     * Sequence
     */
    private Float sequence;

    /**
     * Type
     */
    private ServiceComponentTypeEnum type;

    /**
     * for BUILTIN type
     * Resource name
     */
    private String resourceName;

    /**
     * for BUILTIN type
     * Entry class name
     */
    private String entryClassName;

    /**
     * for PLUGIN type
     * Front-end component file id
     */
    private String frontEndComponentFileId;

    /**
     * for PLUGIN type
     * DFS service agent uid of front-end component file id
     */
    private Long dfsServiceAgentUidOfFrontEndComponentFileId;

    /**
     * for both BUILTIN & PLUGIN type
     * Front-end component code
     */
    private String frontEndComponent;

    /**
     * for PLUGIN type
     * Back-end component file id
     */
    private String backEndComponentFileId;

    /**
     * for PLUGIN type
     * DFS service agent uid of back-end component file id
     */
    private Long dfsServiceAgentUidOfBackEndComponentFileId;

    /**
     * for PLUGIN type
     * Back-end component metadata
     */
    private PluginProfile backEndComponentMetadata;

    /**
     * for both PLUGIN and BUILTIN type
     * configuration template
     */
    private String configurationTemplate;
}
