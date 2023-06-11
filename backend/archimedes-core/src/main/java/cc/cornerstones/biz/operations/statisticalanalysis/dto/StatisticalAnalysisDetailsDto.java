package cc.cornerstones.biz.operations.statisticalanalysis.dto;

import cc.cornerstones.almond.types.AbcTuple3;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class StatisticalAnalysisDetailsDto {
    /**
     * key --- data facet name, value --- trending
     */
    private List<AbcTuple3<String, String, Long>> units;
}
