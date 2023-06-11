package cc.cornerstones.almond.types;

import cc.cornerstones.almond.constants.TreeNodePositionEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import javax.validation.constraints.NotNull;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ReplaceTreeNodeRelationship {
    /**
     * 参数节点 UID
     */
    private Long referenceTreeNodeUid;

    /**
     * 新位置在参加节点的 FRONT（前方），CENTER（中间，作为参照节点的最后一个子节点），REAR（后方）
     */
    @NotNull(message = "position is required")
    private TreeNodePositionEnum position;
}
