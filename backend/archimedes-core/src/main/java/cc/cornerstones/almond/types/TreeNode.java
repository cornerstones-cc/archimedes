package cc.cornerstones.almond.types;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author bbottong
 */
@Data
public class TreeNode {
    private String ids;
    private Long uid;
    private String name;
    private String description;
    private String type;
    private List<TreeNode> children;

    /**
     * 标签，用于扩展 node 的信息
     */
    private Map<String, Object> tags;

    public static final String GENERAL_TYPE_UNSPECIFIED = "unspecified";
    public static final String GENERAL_TYPE_DIRECTORY = "directory";
    public static final String GENERAL_TYPE_ENTITY = "entity";

    public static final String GENERAL_TAG_SELECTED = "selected";
}
