package cc.cornerstones.biz.resourceownership.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;

import java.util.List;

public interface ResourceOwnershipService {
    List<cc.cornerstones.archimedes.extensions.types.TreeNode> treeListingAllNodesOfResourceCategoryHierarchy(
            Long dataPermissionServiceAgentUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    cc.cornerstones.archimedes.extensions.types.TreeNode treeListingAllNodesOfResourceStructureHierarchy(
            Long dataPermissionServiceAgentUid,
            Long resourceCategoryUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<cc.cornerstones.archimedes.extensions.types.TreeNode> treeListingAllNodesOfResourceContentHierarchy(
            Long dataPermissionServiceAgentUid,
            Long resourceCategoryUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
