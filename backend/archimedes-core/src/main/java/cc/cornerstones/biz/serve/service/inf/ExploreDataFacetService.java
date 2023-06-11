package cc.cornerstones.biz.serve.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.TreeNode;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.serve.dto.FlexibleQueryRequestDto;
import cc.cornerstones.biz.share.types.QueryContentResult;

import java.util.List;

public interface ExploreDataFacetService {
    List<TreeNode> treeListingAllNodesOfDataFacetHierarchyOfApp(
            Long appUid, UserProfile operatingUserProfile) throws AbcUndefinedException;

    QueryContentResult flexibleQuery(
            Long dataFacetUid,
            FlexibleQueryRequestDto flexibleQueryRequestDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<TreeNode> treeListingAllNodesOfDataFacetHierarchyOfAllApps(
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
