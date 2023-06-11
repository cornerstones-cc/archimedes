package cc.cornerstones.biz.administration.usermanagement.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.*;
import cc.cornerstones.biz.administration.usermanagement.dto.*;

import java.util.List;

public interface GroupService {
    List<TreeNode> treeListingAllNodesOfGroupHierarchy(
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    TreeNode createEntityNodeForGroupHierarchy(
            Long parentUid,
            CreateEntityTreeNode createEntityTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void updateEntityNodeOfGroupHierarchy(
            Long uid,
            UpdateEntityTreeNode updateEntityTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void replaceNodeRelationshipOfFunctionalHierarchy(
            Long uid,
            ReplaceTreeNodeRelationship replaceTreeNodeRelationship,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<String> listAllReferencesToNodeOfGroupHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void deleteNodeOfGroupHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void createOrReplaceCommonRolesForAllGroups(
            CreateOrReplaceRolesDto createOrReplaceRolesDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void createOrReplaceRolesForGivenGroup(
            Long uid,
            CreateOrReplaceRolesDto createOrReplaceRolesDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<Long> getCommonRolesOfAllGroups(
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<Long> getRolesOfGivenGroup(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

}
