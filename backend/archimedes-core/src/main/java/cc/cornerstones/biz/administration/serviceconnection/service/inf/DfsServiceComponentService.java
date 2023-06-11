package cc.cornerstones.biz.administration.serviceconnection.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.serviceconnection.dto.DfsServiceComponentDto;
import cc.cornerstones.biz.administration.serviceconnection.dto.BackEndComponentParsedResultDto;
import cc.cornerstones.biz.administration.serviceconnection.dto.CreatePluginDfsServiceComponentDto;
import cc.cornerstones.biz.administration.serviceconnection.dto.UpdateDfsServiceComponentDto;
import cc.cornerstones.biz.administration.serviceconnection.share.constants.ServiceComponentTypeEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.File;
import java.util.List;

public interface DfsServiceComponentService {
    DfsServiceComponentDto createPluginDfsServiceComponent(
            CreatePluginDfsServiceComponentDto createPluginDfsServiceComponentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    BackEndComponentParsedResultDto parseBackEndComponent(
            File file,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void updateDfsServiceComponent(
            Long uid,
            UpdateDfsServiceComponentDto updateDfsServiceComponentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<String> listAllReferencesToDfsServiceComponent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void deleteDfsServiceComponent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    DfsServiceComponentDto getDfsServiceComponent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<DfsServiceComponentDto> listingQueryDfsServiceComponents(
            Long uid,
            String name,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Page<DfsServiceComponentDto> pagingQueryDfsServiceComponents(
            Long uid,
            String name,
            String description,
            List<ServiceComponentTypeEnum> typeList,
            Boolean enabled,
            Boolean preferred,
            List<Long> userUidListOfLastModifiedBy,
            List<String> lastModifiedTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
