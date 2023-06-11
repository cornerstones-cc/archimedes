package cc.cornerstones.biz.administration.serviceconnection.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.serviceconnection.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface AuthenticationServiceAgentService {
    AuthenticationServiceAgentDto createAuthenticationServiceAgent(
            CreateAuthenticationServiceAgentDto createAuthenticationServiceAgentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void updateAuthenticationServiceAgent(
            Long uid,
            UpdateAuthenticationServiceAgentDto updateAuthenticationServiceAgentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<String> listAllReferencesToAuthenticationServiceAgent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void deleteAuthenticationServiceAgent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    AuthenticationServiceAgentDto getAuthenticationServiceAgent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<AuthenticationServiceAgentDto> listingQueryAuthenticationServiceAgents(
            Long uid,
            String name,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Page<AuthenticationServiceAgentDto> pagingQueryAuthenticationServiceAgents(
            Long uid,
            String name,
            String description,
            Boolean enabled,
            Boolean preferred,
            List<Long> userUidListOfLastModifiedBy,
            List<String> lastModifiedTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
