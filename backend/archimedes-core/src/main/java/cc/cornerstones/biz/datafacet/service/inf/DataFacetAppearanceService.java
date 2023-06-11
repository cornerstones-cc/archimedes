package cc.cornerstones.biz.datafacet.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.TreeNode;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.datafacet.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

public interface DataFacetAppearanceService {

    List<FilteringDataFieldAnotherDto> listingQueryFilteringDataFieldsOfDataFacet(
            Long dataFacetUid,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    FilteringExtendedDto getFilteringExtendedOfDataFacet(
            Long dataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void replaceAllFilteringDataFieldsOfDataFacet(
            Long dataFacetUid,
            List<FilteringDataFieldDto> filteringDataFieldDtoList,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void replaceFilteringExtendedOfDataFacet(
            Long dataFacetUid,
            FilteringExtendedDto filteringExtendedDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<ListingDataFieldAnotherDto> listingQueryListingDataFieldsOfDataFacet(
            Long dataFacetUid,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    ListingExtendedDto getListingExtendedOfDataFacet(
            Long dataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void replaceAllListingDataFieldsOfDataFacet(
            Long dataFacetUid,
            List<ListingDataFieldDto> listingDataFieldDtoList,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void replaceListingExtendedOfDataFacet(
            Long dataFacetUid,
            ListingExtendedDto listingExtendedDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    File exportSequenceOfAllListingDataFieldsOfDataFacet(
            Long dataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<SortingDataFieldDto> listingQuerySortingDataFieldsOfDataFacet(
            Long dataFacetUid,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void replaceAllSortingDataFieldsOfDataFacet(
            Long dataFacetUid,
            List<SortingDataFieldDto> sortingDataFieldDtoList,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void importSequenceOfAllListingDataFieldsOfDataFacet(
            Long dataFacetUid,
            MultipartFile file,
            UserProfile operatingUserProfile);
}
