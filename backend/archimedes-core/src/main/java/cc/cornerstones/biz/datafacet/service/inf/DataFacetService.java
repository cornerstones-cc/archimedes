package cc.cornerstones.biz.datafacet.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.TreeNode;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.datafacet.dto.CreateDataFacetDto;
import cc.cornerstones.biz.datafacet.dto.DataFacetDto;
import cc.cornerstones.biz.datafacet.dto.DataFieldDto;
import cc.cornerstones.biz.datafacet.dto.UpdateDataFacetDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface DataFacetService {
    DataFacetDto createDataFacet(
            Long dataTableUid,
            CreateDataFacetDto createDataFacetDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    DataFacetDto getDataFacet(
            Long dataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    DataFacetDto updateDataFacet(
            Long dataFacetUid,
            UpdateDataFacetDto updateDataFacetDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<String> listAllReferencesToDataFacet(
            Long dataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void deleteDataFacet(
            Long dataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Page<DataFacetDto> pagingQueryDataFacets(
            Long dataFacetUid,
            String dataFacetName,
            String description,
            List<Long> userUidListOfLastModifiedBy,
            List<String> lastModifiedTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<DataFacetDto> listingQueryDataFacets(
            Long dataFacetUid,
            String dataFacetName,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<TreeNode> treeListingAllDataFacets(
            Long dataSourceUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<TreeNode> treeListingAllDataFacets(
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<TreeNode> treeListingAllDataObjects(
            Long dataSourceUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<TreeNode> treeListingAllDataObjects(
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
