package cc.cornerstones.biz.datafacet.dto;

import cc.cornerstones.biz.administration.serviceconnection.dto.DataPermissionServiceAgentDto;
import cc.cornerstones.biz.administration.serviceconnection.dto.DataPermissionServiceComponentDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import javax.persistence.Column;
import java.util.List;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class DataPermissionDto {

    /**
     * UID
     */
    private Long uid;

    /**
     * Enabled
     */
    private Boolean enabled;

    /**
     * 所属 Data Facet 的 UID
     */
    @Column(name = "data_facet_uid")
    private Long dataFacetUid;

    /**
     * 选中的 data field name
     */
    private List<String> fieldNameList;

    /**
     * 选中的 data permission service agent
     */
    private DataPermissionServiceAgentDto serviceAgent;

    /**
     * 选中的 resource category 的 uid
     */
    private Long resourceCategoryUid;

    /**
     * 选中的 resource category 的 name
     */
    private String resourceCategoryName;

    /**
     * 选中的 resource category 的 resource structure
     */
    private cc.cornerstones.archimedes.extensions.types.TreeNode resourceStructure;

    /**
     * resource structure level 和  data field name 之间的一一对应关系
     * <p>
     * Key --- resource structure level uid, Value --- data field name
     */
    private Map<Long, String> resourceStructureLevelMapping;
}
