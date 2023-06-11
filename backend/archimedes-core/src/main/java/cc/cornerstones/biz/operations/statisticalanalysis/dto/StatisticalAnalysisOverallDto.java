package cc.cornerstones.biz.operations.statisticalanalysis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class StatisticalAnalysisOverallDto {
    private Long totalNumberOfDataFacets;

    private Long totalNumberOfApps;

    private Long totalNumberOfUsers;

    private Long totalNumberOfQueries;

    private Long totalNumberOfExports;
}
