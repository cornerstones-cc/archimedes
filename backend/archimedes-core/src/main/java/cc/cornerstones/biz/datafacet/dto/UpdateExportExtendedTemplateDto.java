package cc.cornerstones.biz.datafacet.dto;

import cc.cornerstones.biz.datafacet.share.constants.ExportExtendedTemplateVisibilityEnum;
import cc.cornerstones.biz.datafacet.share.constants.TemplateColumnHeaderSourceEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class UpdateExportExtendedTemplateDto {
    /**
     * Name
     *
     * A name is used to identify the object.
     */
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
     * Enabled
     */
    private Boolean enabled;

    /**
     * File id
     */
    @NotBlank(message = "file_id is required")
    private String fileId;

    /**
     * DFS service agent uid
     */
    private Long dfsServiceAgentUid;

    /**
     * Visibility
     */
    private ExportExtendedTemplateVisibilityEnum visibility;


    /**
     * Column header source
     */
    private TemplateColumnHeaderSourceEnum columnHeaderSource;
}
