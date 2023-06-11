package cc.cornerstones.biz.administration.serviceconnection.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.serviceconnection.dto.BackEndComponentParsedResultDto;
import cc.cornerstones.biz.administration.serviceconnection.dto.CreatePluginAuthenticationServiceComponentDto;
import cc.cornerstones.biz.administration.serviceconnection.dto.UpdateAuthenticationServiceComponentDto;
import cc.cornerstones.biz.administration.serviceconnection.dto.AuthenticationServiceComponentDto;
import cc.cornerstones.biz.administration.serviceconnection.share.constants.ServiceComponentTypeEnum;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.File;
import java.util.List;

public interface AuthenticationServiceComponentService {
    AuthenticationServiceComponentDto createPluginAuthenticationServiceComponent(
            CreatePluginAuthenticationServiceComponentDto createPluginAuthenticationServiceComponentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    BackEndComponentParsedResultDto parseBackEndComponent(
            File file,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void updateAuthenticationServiceComponent(
            Long uid,
            UpdateAuthenticationServiceComponentDto updateAuthenticationServiceComponentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<String> listAllReferencesToAuthenticationServiceComponent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void deleteAuthenticationServiceComponent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    AuthenticationServiceComponentDto getAuthenticationServiceComponent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<AuthenticationServiceComponentDto> listingQueryAuthenticationServiceComponents(
            Long uid,
            String name,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Page<AuthenticationServiceComponentDto> pagingQueryAuthenticationServiceComponents(
            Long uid,
            String name,
            String description,
            List<ServiceComponentTypeEnum> typeList,
            List<Long> userUidListOfLastModifiedBy,
            List<String> lastModifiedTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Resource getFrontEndComponentInterface(
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Resource getBackEndComponentInterface(
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
