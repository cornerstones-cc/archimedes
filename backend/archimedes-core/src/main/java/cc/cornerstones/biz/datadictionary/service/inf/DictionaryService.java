package cc.cornerstones.biz.datadictionary.service.inf;

import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.*;
import cc.cornerstones.biz.datadictionary.dto.CreateOrReplaceDictionaryBuildDto;
import cc.cornerstones.biz.datadictionary.dto.DictionaryBuildDto;
import cc.cornerstones.biz.datadictionary.dto.DictionaryBuildInstanceDto;
import cc.cornerstones.biz.datadictionary.dto.TestDictionaryBuildDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DictionaryService {
    TreeNode createDirectoryNodeForDictionaryCategoryHierarchy(
            Long parentUid,
            CreateDirectoryTreeNode createDirectoryTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void updateDirectoryNodeOfDictionaryCategoryHierarchy(
            Long uid,
            UpdateDirectoryTreeNode updateDirectoryTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    TreeNode createEntityNodeForDictionaryCategoryHierarchy(
            Long parentUid,
            CreateEntityTreeNode createEntityTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void updateEntityNodeOfDictionaryCategoryHierarchy(
            Long uid,
            UpdateEntityTreeNode updateEntityTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void replaceNodeRelationshipOfDictionaryCategoryHierarchy(
            Long uid,
            ReplaceTreeNodeRelationship replaceTreeNodeRelationship,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<String> listAllReferencesToNodeOfDictionaryCategoryHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void deleteNodeOfDictionaryCategoryHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<TreeNode> treeListingAllNodesOfDictionaryCategoryHierarchy(
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    TreeNode createEntityNodeForDictionaryStructureHierarchy(
            Long dictionaryCategoryUid,
            Long parentUid,
            CreateEntityTreeNode createEntityTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void updateEntityNodeOfDictionaryStructureHierarchy(
            Long uid,
            UpdateEntityTreeNode updateEntityTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void replaceNodeRelationshipOfDictionaryStructureHierarchy(
            Long uid,
            ReplaceTreeNodeRelationship replaceTreeNodeRelationship,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<String> listAllReferencesToNodeOfDictionaryStructureHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void deleteNodeOfDictionaryStructureHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    TreeNode treeListingAllNodesOfDictionaryStructureHierarchy(
            Long dictionaryCategoryUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    TreeNode createEntityNodeForDictionaryContentHierarchy(
            Long dictionaryCategoryUid,
            Long parentUid,
            CreateEntityTreeNode createEntityTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void updateEntityNodeOfDictionaryContentHierarchy(
            Long uid,
            UpdateEntityTreeNode updateEntityTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void replaceNodeRelationshipOfDictionaryContentHierarchy(
            Long uid,
            ReplaceTreeNodeRelationship replaceTreeNodeRelationship,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<String> listAllReferencesToNodeOfDictionaryContentHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void deleteNodeOfDictionaryContentHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<TreeNode> treeListingAllNodesOfDictionaryContentHierarchy(
            Long dictionaryCategoryUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<TreeNode> treeListingFirstLevelOfDictionaryContentHierarchy(
            Long dictionaryCategoryUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<TreeNode> treeListingNextLevelOfDictionaryContentHierarchy(
            Long dictionaryContentUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<TreeNode> treeQueryingNodesOfDictionaryContentHierarchy(
            Long dictionaryCategoryUid,
            String value,
            String label,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<TreeNode> treeListingFirstLevelOfDictionaryContentHierarchyWithTaggingSelected(
            Long dictionaryCategoryUid,
            List<Long> uidList,
            List<String> valueList,
            List<String> labelList,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    DictionaryBuildDto createOrReplaceDictionaryBuild(
            Long dictionaryCategoryUid,
            CreateOrReplaceDictionaryBuildDto createOrReplaceDictionaryBuildDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    DictionaryBuildDto findDictionaryBuildByDataDictionaryUid(
            Long dictionaryCategoryUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<TreeNode> testDictionaryBuild(
            TestDictionaryBuildDto testDictionaryBuildDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void executeOnceDictionaryBuild(
            Long dictionaryCategoryUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Page<DictionaryBuildInstanceDto> pagingQueryDictionaryBuildInstances(
            Long dictionaryCategoryUid,
            Long uid,
            List<JobStatusEnum> statuses,
            List<String> createdTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
