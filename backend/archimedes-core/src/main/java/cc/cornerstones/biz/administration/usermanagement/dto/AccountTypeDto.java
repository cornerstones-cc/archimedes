package cc.cornerstones.biz.administration.usermanagement.dto;

import cc.cornerstones.almond.types.BaseDto;
import lombok.Data;

/**
 * Create Account Type
 *
 * @author bbottong
 *
 */
@Data
public class AccountTypeDto extends BaseDto {
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
}