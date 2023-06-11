package cc.cornerstones.biz.administration.serviceconnection.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.AbcTuple3;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.serviceconnection.dto.CreateDfsServiceAgentDto;
import cc.cornerstones.biz.administration.serviceconnection.dto.DfsServiceAgentDto;
import cc.cornerstones.biz.administration.serviceconnection.dto.UpdateDfsServiceAgentDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.File;
import java.util.List;

public interface DfsServiceAgentService {
    DfsServiceAgentDto createDfsServiceAgent(
            CreateDfsServiceAgentDto createDfsServiceAgentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void updateDfsServiceAgent(
            Long uid,
            UpdateDfsServiceAgentDto updateDfsServiceAgentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<String> listAllReferencesToDfsServiceAgent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void deleteDfsServiceAgent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    DfsServiceAgentDto getDfsServiceAgent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    DfsServiceAgentDto getPreferredDfsServiceAgent(
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<DfsServiceAgentDto> listingQueryDfsServiceAgents(
            Long uid,
            String name,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Page<DfsServiceAgentDto> pagingQueryDfsServiceAgents(
            Long uid,
            String name,
            String description,
            Boolean enabled,
            Boolean preferred,
            List<Long> userUidListOfLastModifiedBy,
            List<String> lastModifiedTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    String uploadFile(
            File file,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    String uploadFile(
            Long uid,
            File file,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    File downloadFile(
            String fileId,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    File downloadFile(
            Long uid,
            String fileId,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void downloadFiles(
            Long uid,
            List<AbcTuple3<String, String, String>> inputList,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
