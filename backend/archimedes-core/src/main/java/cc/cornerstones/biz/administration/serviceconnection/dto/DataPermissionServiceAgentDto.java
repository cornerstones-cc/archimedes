package cc.cornerstones.biz.administration.serviceconnection.dto;

import cc.cornerstones.almond.types.BaseDto;
import cc.cornerstones.almond.types.UserBriefInformation;
import lombok.Data;

@Data
public class DataPermissionServiceAgentDto extends BaseDto {

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
     * Enabled
     */
    private Boolean enabled;

    /**
     * Service component
     */
    private DataPermissionServiceComponentDto serviceComponent;

    /**
     * Configuration
     */
    private String configuration;

    /**
     * User identification
     */
    private Long accountTypeUid;
}
