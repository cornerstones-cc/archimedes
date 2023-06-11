package cc.cornerstones.biz.operations.migration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class MigrationDeploymentDto {
    private Map<Long, Long> dataSourceUidMapping;
    private Map<Long, Long> dictionaryCategoryUidMapping;
    private Map<Long, Long> dfsServiceAgentUidMapping;
    private Map<Long, Long> dataPermissionServiceAgentUidMapping;

    private List<DataFacetWrapperDto> dataFacetWrapperList;
}
