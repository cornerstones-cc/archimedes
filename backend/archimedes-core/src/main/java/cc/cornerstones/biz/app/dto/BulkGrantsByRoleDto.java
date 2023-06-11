package cc.cornerstones.biz.app.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class BulkGrantsByRoleDto {
    private List<Long> roleUidList;
    private List<Long> dataFacetHierarchyNodeUidList;
}
