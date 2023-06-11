package cc.cornerstones.biz.datafacet.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class DataPermissionContentDto {
    /**
     * 选中的 data field name
     */
    private List<String> fieldNameList;

    /**
     * 选中的 data permission service agent 的 uid
     */
    private Long dataPermissionServiceAgentUid;

    /**
     * 选中的 resource category 的 uid
     */
    private Long resourceCategoryUid;

    /**
     * 选中的 resource category 的 name
     */
    private String resourceCategoryName;

    /**
     * resource structure level 和 data field name 之间的一一对应关系
     * <p>
     * Key --- resource structure level uid , Value --- data field name
     */
    private Map<Long, String> resourceStructureLevelMapping;
}
