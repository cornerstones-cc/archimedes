package cc.cornerstones.biz.administration.serviceconnection.service.inf;

import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.serviceconnection.dto.*;
import cc.cornerstones.biz.administration.serviceconnection.share.constants.ServiceComponentTypeEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.File;
import java.util.List;

public interface UserSynchronizationServiceComponentService {
    UserSynchronizationServiceComponentDto createPluginUserSynchronizationServiceComponent(
            CreatePluginUserSynchronizationServiceComponentDto createPluginUserSynchronizationServiceComponentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    BackEndComponentParsedResultDto parseBackEndComponent(
            File file,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void updateUserSynchronizationServiceComponent(
            Long uid,
            UpdateUserSynchronizationServiceComponentDto updateUserSynchronizationServiceComponentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<String> listAllReferencesToUserSynchronizationServiceComponent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void deleteUserSynchronizationServiceComponent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    UserSynchronizationServiceComponentDto getUserSynchronizationServiceComponent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<UserSynchronizationServiceComponentDto> listingQueryUserSynchronizationServiceComponents(
            Long uid,
            String name,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Page<UserSynchronizationServiceComponentDto> pagingQueryUserSynchronizationServiceComponents(
            Long uid,
            String name,
            String description,
            List<ServiceComponentTypeEnum> typeList,
            Boolean enabled,
            List<Long> userUidListOfLastModifiedBy,
            List<String> lastModifiedTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Page<UserSynchronizationExecutionInstanceDto> pagingQueryUserSynchronizationExecutionInstances(
            Long userSynchronizationServiceAgentUid,
            Long uid,
            List<JobStatusEnum> jobStatusList,
            List<Long> userUidListOfLastModifiedBy,
            List<String> lastModifiedTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    UserSynchronizationExecutionInstanceDto createUserSynchronizationExecutionInstance(
            Long userSynchronizationServiceAgentUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
