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
public class PreparedMigrateOutDto {
    private Map<Long, String> dataSourceMap;

    private Map<Long, String> dictionaryCategoryMap;

    private Map<Long, String> dfsServiceAgentMap;

    private Map<Long, String> dataPermissionServiceAgentMap;

}
