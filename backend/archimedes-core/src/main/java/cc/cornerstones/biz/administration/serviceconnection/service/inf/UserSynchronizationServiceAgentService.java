package cc.cornerstones.biz.administration.serviceconnection.service.inf;

import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.serviceconnection.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface UserSynchronizationServiceAgentService {
    UserSynchronizationServiceAgentDto createUserSynchronizationServiceAgent(
            CreateUserSynchronizationServiceAgentDto createUserSynchronizationServiceAgentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void updateUserSynchronizationServiceAgent(
            Long uid,
            UpdateUserSynchronizationServiceAgentDto updateUserSynchronizationServiceAgentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<String> listAllReferencesToUserSynchronizationServiceAgent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void deleteUserSynchronizationServiceAgent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    UserSynchronizationServiceAgentDto getUserSynchronizationServiceAgent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<UserSynchronizationServiceAgentDto> listingQueryUserSynchronizationServiceAgents(
            Long uid,
            String name,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Page<UserSynchronizationServiceAgentDto> pagingQueryUserSynchronizationServiceAgents(
            Long uid,
            String name,
            String description,
            Boolean enabled,
            List<Long> userUidListOfLastModifiedBy,
            List<String> lastModifiedTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Page<UserSynchronizationExecutionInstanceDto> pagingQueryUserSynchronizationExecutionInstances(
            Long userSynchronizationServiceAgentUid,
            Long uid,
            List<JobStatusEnum> jobStatusList,
            List<String> createdTimestampAsStringList,
            List<String> lastModifiedTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    UserSynchronizationExecutionInstanceDto createUserSynchronizationExecutionInstance(
            Long userSynchronizationServiceAgentUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
