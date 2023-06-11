package cc.cornerstones.biz.datafacet.dto;

import cc.cornerstones.almond.types.BaseDto;
import cc.cornerstones.biz.datafacet.share.constants.ExportExtendedTemplateVisibilityEnum;
import cc.cornerstones.biz.datafacet.share.constants.TemplateColumnHeaderSourceEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import javax.validation.constraints.NotNull;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ExportExtendedTemplateDto extends BaseDto {
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
     * Enabled
     */
    private Boolean enabled;

    /**
     * File
     */
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

    /**
     * 所属 Data Facet 的 UID
     */
    private Long dataFacetUid;
}
