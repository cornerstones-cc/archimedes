package cc.cornerstones.biz.datafacet.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class AdvancedFeatureDto {

    /**
     * Content
     *
     */
    private AdvancedFeatureContentDto content;

    /**
     * 所属 Data Facet 的 UID
     */
    private Long dataFacetUid;
}
