package cc.cornerstones.biz.administration.serviceconnection.dto;

import cc.cornerstones.almond.types.AbcTuple2;
import cc.cornerstones.almond.types.BaseDto;
import cc.cornerstones.almond.types.UserBriefInformation;
import lombok.Data;

import java.util.List;

@Data
public class AuthenticationServiceAgentDto extends BaseDto {

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
     * Preferred
     */
    private Boolean preferred;

    /**
     * Service component
     */
    private AuthenticationServiceComponentDto serviceComponent;

    /**
     * Configuration
     */
    private String configuration;

    /**
     * Account type's uid
     */
    private List<Long> accountTypeUidList;

    /**
     * User schema extended properties mapping to userinfo
     * f (field 1) --- extended property's UID,
     * s (field 2) --- field path (eg., a.b.c) in the userinfo result object
     */
    private List<AbcTuple2<Long, String>> properties;
}
