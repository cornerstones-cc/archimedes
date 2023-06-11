package cc.cornerstones.biz.datatable.dto;

import cc.cornerstones.biz.datasource.share.constants.DataColumnTypeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * Create Data Column
 *
 * @author bbottong
 *
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class CreateDataColumnDto {
    /**
     * Name
     *
     * A name is used to identify the object.
     */
    @NotBlank(message = "name is required")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5_a-zA-Z0-9\\s-]+$", message = "Only Chinese characters, or English letters, or numbers, or spaces, or underscores, or hyphens are allowed")
    @Size(min = 1, max = 64,
            message = "The name cannot exceed 64 characters in length")
    private String name;

    /**
     * Description
     *
     * A meaning description helps you remembers the differences between objects.
     */
    @Size(min = 0, max = 255,
            message = "The description cannot exceed 255 characters in length")
    private String description;

    /**
     * Type
     */
    private DataColumnTypeEnum type;

    /**
     * Ordinal Position
     *
     * 在所有字段中的序号（从0开始计数）
     */
    private Float ordinalPosition;
}