package cc.cornerstones.biz.operations.statisticalanalysis.dto;

import cc.cornerstones.biz.operations.statisticalanalysis.share.types.Cell;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class StatisticalAnalysisRankingDto {
    private List<Cell> queries;
    private List<Cell> exports;
}
