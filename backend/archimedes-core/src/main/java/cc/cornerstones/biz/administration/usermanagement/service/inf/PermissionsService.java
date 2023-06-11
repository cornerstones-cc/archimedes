package cc.cornerstones.biz.administration.usermanagement.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.*;
import cc.cornerstones.biz.administration.usermanagement.dto.*;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface PermissionsService {
    List<ApiDto> listingQueryApis(
            String tag,
            String method,
            String uri,
            String summary,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<TreeNode> treeListingAllNodesOfApiHierarchy(
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<TreeNode> treeListingAllNodesOfFunctionHierarchy(
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    TreeNode createDirectoryNodeForFunctionHierarchy(
            Long parentUid,
            CreateDirectoryTreeNode createDirectoryTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void updateDirectoryNodeOfFunctionHierarchy(
            Long uid,
            UpdateDirectoryTreeNode updateDirectoryTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    TreeNode createEntityNodeForFunctionHierarchy(
            Long parentUid,
            CreateEntityTreeNode createEntityTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<TreeNode> batchCreateEntityNodesForFunctionHierarchy(
            Long parentUid,
            List<CreateEntityTreeNode> createEntityTreeNodeList,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void replaceNodeRelationshipOfFunctionHierarchy(
            Long uid,
            ReplaceTreeNodeRelationship replaceTreeNodeRelationship,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<String> listAllReferencesToNodeOfFunctionHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void deleteNodeOfFunctionHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<TreeNode> treeListingAllNodesOfNavigationMenuHierarchy(
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    TreeNode createDirectoryNodeForNavigationMenuHierarchy(
            Long parentUid,
            CreateDirectoryTreeNode createDirectoryTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void updateDirectoryNodeOfNavigationMenuHierarchy(
            Long uid,
            UpdateDirectoryTreeNode updateDirectoryTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    TreeNode createEntityNodeForNavigationMenuHierarchy(
            Long parentUid,
            CreateEntityTreeNode createEntityTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void updateEntityNodeOfNavigationMenuHierarchy(
            Long uid,
            UpdateEntityTreeNode updateEntityTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void replaceNodeRelationshipOfNavigationMenuHierarchy(
            Long uid,
            ReplaceTreeNodeRelationship replaceTreeNodeRelationship,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<String> listAllReferencesToNodeOfNavigationMenuHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void deleteNodeOfNavigationMenuHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
