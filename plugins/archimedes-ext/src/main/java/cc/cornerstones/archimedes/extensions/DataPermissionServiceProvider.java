package cc.cornerstones.archimedes.extensions;

import cc.cornerstones.archimedes.extensions.types.TreeNode;
import org.pf4j.ExtensionPoint;

import java.util.List;

public abstract class DataPermissionServiceProvider implements ExtensionPoint {
    public String getConfigurationTemplate() throws Exception {
        return null;
    }

    public List<TreeNode> treeListingAllNodesOfResourceCategoryHierarchy(
            String configuration) throws Exception {
        return null;
    }

    public TreeNode treeListingAllNodesOfResourceStructureHierarchy(
            Long resourceCategoryUid,
            String configuration) throws Exception {
        return null;
    }

    public List<TreeNode> treeListingAllNodesOfResourceContentHierarchy(
            Long resourceCategoryUid,
            String configuration,
            String username) throws Exception {
        return null;
    }
}
