package cc.cornerstones.biz.administration.usermanagement.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.*;
import cc.cornerstones.biz.administration.usermanagement.dto.*;
import cc.cornerstones.biz.administration.usermanagement.share.types.SimplePermissions;

import java.util.List;

public interface RoleService {
    List<TreeNode> treeListingAllNodesOfRoleHierarchy(
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    TreeNode createEntityNodeForRoleHierarchy(
            Long parentUid,
            CreateEntityTreeNode createEntityTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void updateEntityNodeOfRoleHierarchy(
            Long uid,
            UpdateEntityTreeNode updateEntityTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void replaceNodeRelationshipOfRoleHierarchy(
            Long uid,
            ReplaceTreeNodeRelationship replaceTreeNodeRelationship,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<String> listAllReferencesToNodeOfRoleHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void deleteNodeOfRoleHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void createOrReplaceCommonPermissionsForAllRoles(
            CreateOrReplacePermissionsDto createOrReplacePermissionsDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void createOrReplacePermissionsForGivenRole(
            Long uid,
            CreateOrReplacePermissionsDto createOrReplacePermissionsDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    SimplePermissions getCommonPermissionsOfAllRoles(
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    SimplePermissions getPermissionsOfGivenRole(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
