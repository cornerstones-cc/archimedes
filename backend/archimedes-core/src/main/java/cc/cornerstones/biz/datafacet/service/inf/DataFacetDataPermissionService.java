package cc.cornerstones.biz.datafacet.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.datafacet.dto.CreateDataPermissionDto;
import cc.cornerstones.biz.datafacet.dto.DataPermissionDto;
import cc.cornerstones.biz.datafacet.dto.DataPermissionContentDto;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface DataFacetDataPermissionService {
    DataPermissionDto createDataPermissionForDataFacet(
            Long dataFacetUid,
            CreateDataPermissionDto createDataPermissionDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void replaceDataPermission(
            Long uid,
            CreateDataPermissionDto createDataPermissionDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<String> listAllReferencesToDataPermission(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void deleteDataPermission(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<DataPermissionDto> listingQueryDataPermissionsOfDataFacet(
            Long dataFacetUid,
            Boolean enabled,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

}
