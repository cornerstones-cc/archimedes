package cc.cornerstones.biz.app.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.TreeNode;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.types.CreateDirectoryTreeNode;
import cc.cornerstones.almond.types.CreateEntityTreeNode;
import cc.cornerstones.almond.types.UpdateDirectoryTreeNode;
import cc.cornerstones.almond.types.ReplaceTreeNodeRelationship;

import java.util.List;

public interface AppDataFacetService {

    TreeNode createDirectoryNodeForDataFacetHierarchy(
            Long appUid,
            Long parentUid,
            CreateDirectoryTreeNode createDirectoryTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void updateDirectoryNodeOfDataFacetHierarchy(
            Long uid,
            UpdateDirectoryTreeNode updateDirectoryTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    TreeNode createEntityNodeForDataFacetHierarchy(
            Long appUid,
            Long parentUid,
            CreateEntityTreeNode createEntityTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<TreeNode> batchCreateEntityNodesForDataFacetHierarchy(
            Long appUid,
            Long parentUid,
            List<CreateEntityTreeNode> createEntityTreeNodeList,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void replaceNodeRelationshipOfDataFacetHierarchy(
            Long uid,
            ReplaceTreeNodeRelationship replaceTreeNodeRelationship,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<String> listAllReferencesToNodeOfDataFacetHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void deleteNodeOfDataFacetHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<TreeNode> treeListingAllNodesOfDataFacetHierarchyOfOneApp(
            Long appUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<TreeNode> treeListingAllNodesOfDataFacetHierarchyOfAllApps(
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
