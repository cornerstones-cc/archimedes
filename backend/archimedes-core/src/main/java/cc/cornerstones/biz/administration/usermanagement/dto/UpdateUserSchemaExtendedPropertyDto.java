package cc.cornerstones.biz.administration.usermanagement.dto;

import cc.cornerstones.almond.constants.DatabaseFieldTypeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class UpdateUserSchemaExtendedPropertyDto {
    /**
     * Name
     * <p>
     * A name is used to identify the object.
     */
    @Pattern(regexp = "^[\\u4e00-\\u9fa5_a-zA-Z0-9\\s-]+$", message = "Only Chinese characters, or English letters, or numbers, or spaces, or underscores, or hyphens are allowed")
    @Size(min = 1, max = 64,
            message = "The name cannot exceed 64 characters in length")
    private String name;

    /**
     * Description
     * <p>
     * A meaning description helps you remembers the differences between objects.
     */
    @Size(min = 0, max = 255,
            message = "description cannot exceed 255 characters in length")
    private String description;

    /**
     * sequence
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