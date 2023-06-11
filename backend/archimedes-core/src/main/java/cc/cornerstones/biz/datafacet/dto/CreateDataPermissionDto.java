package cc.cornerstones.biz.datafacet.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class CreateDataPermissionDto {
    private Boolean enabled;

    /**
     *  选中的 data field name
     */
    private List<String> fieldNameList;

    /**
     * 选中的 data permission service agent 的 uid
     */
    @NotNull(message = "data_permission_service_agent_uid is required")
    private Long dataPermissionServiceAgentUid;

    /**
     * 选中的 resource category 的 uid
     */
    @NotNull(message = "resource_category_uid is required")
    private Long resourceCategoryUid;

    /**
     * 选中的 resource category 的 name
     */
    @NotBlank(message = "resource_category_name is required")
    @Size(min = 0, max = 255,
            message = "The description cannot exceed 255 characters in length")
    private String resourceCategoryName;

    /**
     * data field name 和 resource structure level 之间的一一对应关系
     *
     * Key --- data field name, Value --- resource structure level uid
     */
    @NotNull(message = "resource_structure_level_mapping is required")
    private Map<Long, String> resourceStructureLevelMapping;
}
