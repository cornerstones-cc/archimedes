package cc.cornerstones.biz.administration.serviceconnection.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.serviceconnection.dto.BackEndComponentParsedResultDto;
import cc.cornerstones.biz.administration.serviceconnection.dto.CreatePluginDataPermissionServiceComponentDto;
import cc.cornerstones.biz.administration.serviceconnection.dto.UpdateDataPermissionServiceComponentDto;
import cc.cornerstones.biz.administration.serviceconnection.dto.DataPermissionServiceComponentDto;
import cc.cornerstones.biz.administration.serviceconnection.share.constants.ServiceComponentTypeEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.File;
import java.util.List;

public interface DataPermissionServiceComponentService {
    DataPermissionServiceComponentDto createPluginDataPermissionServiceComponent(
            CreatePluginDataPermissionServiceComponentDto createPluginDataPermissionServiceComponentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    BackEndComponentParsedResultDto parseBackEndComponent(
            File file,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void updateDataPermissionServiceComponent(
            Long uid,
            UpdateDataPermissionServiceComponentDto updateDataPermissionServiceComponentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<String> listAllReferencesToDataPermissionServiceComponent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void deleteDataPermissionServiceComponent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    DataPermissionServiceComponentDto getDataPermissionServiceComponent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<DataPermissionServiceComponentDto> listingQueryDataPermissionServiceComponents(
            Long uid,
            String name,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Page<DataPermissionServiceComponentDto> pagingQueryDataPermissionServiceComponents(
            Long uid,
            String name,
            String description,
            List<ServiceComponentTypeEnum> typeList,
            Boolean enabled,
            List<Long> userUidListOfLastModifiedBy,
            List<String> lastModifiedTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
