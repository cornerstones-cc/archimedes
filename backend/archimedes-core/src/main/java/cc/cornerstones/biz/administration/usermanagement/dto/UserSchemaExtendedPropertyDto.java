package cc.cornerstones.biz.administration.usermanagement.dto;

import cc.cornerstones.almond.constants.DatabaseFieldTypeEnum;
import cc.cornerstones.almond.types.BaseDto;
import cc.cornerstones.almond.types.UserBriefInformation;
import lombok.Data;

/**
 * User schema extended property
 *
 * @author bbottong
 */
@Data
public class UserSchemaExtendedPropertyDto extends BaseDto {
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
     * Label
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
    private DatabaseFieldTypeEnum type;

    /**
     * Length
     */
    private String length;

    /**
     * Input validation regex
     */
    private String inputValidationRegex;

    /**
     * Nullable
     */
    private Boolean nullable;

    /**
     * Show in filter
     */
    private Boolean showInFilter;

    /**
     * Show in detailed information
     */
    private Boolean showInDetailedInformation;

    /**
     * Show in brief information
     */
    private Boolean showInBriefInformation;
}