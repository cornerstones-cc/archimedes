package cc.cornerstones.biz.operations.migration.dto;

import cc.cornerstones.almond.types.AbcTuple2;
import cc.cornerstones.biz.operations.migration.share.constants.CopyStrategyEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class StartMigrateOutDto {
    /**
     * source data facet uid list, waiting to be migrated
     */
    private List<Long> sourceDataFacetUidList;

    /**
     * Key --- source data facet uid, Value --- a tuple of (new/update) and target data facet uid if update
     */
    private Map<Long, AbcTuple2<CopyStrategyEnum, Long>> copySettings;

    /**
     * source data source uid, target data source uid
     */
    private List<String> dataSourceUidMapping;

    /**
     * source dictionary category uid, target dictionary category uid
     */
    private List<String> dictionaryCategoryUidMapping;

    /**
     * source dfs service agent uid, target service agent uid
     */
    private List<String> dfsServiceAgentUidMapping;

    /**
     * source data permission service agent uid, target data permission service agent uid
     */
    private List<String> dataPermissionServiceAgentUidMapping;
}
