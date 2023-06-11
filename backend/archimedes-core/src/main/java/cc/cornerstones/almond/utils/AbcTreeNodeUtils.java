package cc.cornerstones.almond.utils;

import org.springframework.util.CollectionUtils;

import java.util.LinkedList;
import java.util.List;

public class AbcTreeNodeUtils {
    public static void transformTagValueFromHierarchyToFlat(
            List<List<Object>> rows,
            List<Object> parentColumns,
            List<cc.cornerstones.almond.types.TreeNode> treeNodeList) {
        treeNodeList.forEach(treeNode -> {
            if (CollectionUtils.isEmpty(treeNode.getChildren())) {
                // 叶子节点
                List<Object> row = new LinkedList<>();
                row.addAll(parentColumns);
                if (treeNode.getTags().containsKey("value")) {
                    row.add(treeNode.getTags().get("value"));
                } else {
                    row.add(null);
                }
                rows.add(row);
            } else {
                if (treeNode.getTags().containsKey("value")) {
                    parentColumns.add(treeNode.getTags().get("value"));
                } else {
                    parentColumns.add(null);
                }
                transformHierarchyToFlat(rows, parentColumns, treeNode.getChildren());
            }
        });
    }

    public static void transformHierarchyToFlat(
            List<List<Object>> rows,
            List<Object> parentColumns,
            List<cc.cornerstones.almond.types.TreeNode> treeNodeList) {
        treeNodeList.forEach(treeNode -> {
            if (CollectionUtils.isEmpty(treeNode.getChildren())) {
                // 叶子节点，一行就绪
                List<Object> row = new LinkedList<>();
                row.addAll(parentColumns);
                row.add(treeNode.getName());
                rows.add(row);
            } else {
                parentColumns.add(treeNode.getName());
                transformHierarchyToFlat(rows, parentColumns, treeNode.getChildren());
            }
        });

        if (!parentColumns.isEmpty()) {
            parentColumns.remove(parentColumns.size() - 1);
        }
    }
}
