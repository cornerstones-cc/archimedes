package cc.cornerstones.biz.administration.serviceconnection.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.serviceconnection.dto.CreateDataPermissionServiceAgentDto;
import cc.cornerstones.biz.administration.serviceconnection.dto.DataPermissionServiceAgentDto;
import cc.cornerstones.biz.administration.serviceconnection.dto.UpdateDataPermissionServiceAgentDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface DataPermissionServiceAgentService {
    DataPermissionServiceAgentDto createDataPermissionServiceAgent(
            CreateDataPermissionServiceAgentDto createDataPermissionServiceAgentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void updateDataPermissionServiceAgent(
            Long uid,
            UpdateDataPermissionServiceAgentDto updateDataPermissionServiceAgentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<String> listAllReferencesToDataPermissionServiceAgent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void deleteDataPermissionServiceAgent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    DataPermissionServiceAgentDto getDataPermissionServiceAgent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<DataPermissionServiceAgentDto> listingQueryDataPermissionServiceAgents(
            Long uid,
            String name,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Page<DataPermissionServiceAgentDto> pagingQueryDataPermissionServiceAgents(
            Long uid,
            String name,
            String description,
            Boolean enabled,
            List<Long> userUidListOfLastModifiedBy,
            List<String> lastModifiedTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
