package cc.cornerstones.biz.administration.usermanagement.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserBriefInformation;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.usermanagement.dto.*;
import cc.cornerstones.biz.settings.dto.UpdateUserCredentialDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;

public interface UserService {
    UserProfile getUserProfile(
            Long uid) throws AbcUndefinedException;

    UserProfile getUserProfile(
            Long accountTypeUid,
            String accountName) throws AbcUndefinedException;

    UserDto createUser(
            CreateUserDto createUserDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void replaceUser(
            Long uid,
            ReplaceUserDto replaceUserDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<String> listAllReferencesToUser(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void deleteUser(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    UserDto getUser(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    UserDto getUser(
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    UserDto getUser(
            Long uid) throws AbcUndefinedException;

    UserDto getUser(
            Long accountTypeUid,
            String accountName) throws AbcUndefinedException;

    UserSimplifiedDto getUserSimplified(
            Long uid) throws AbcUndefinedException;

    UserSimplifiedDto getUserSimplified(
            Long accountTypeUid,
            String accountName) throws AbcUndefinedException;

    UserDetailedDto getUserDetailed(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<UserDto> listingQueryUsers(
            Long uid,
            String displayName,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Page<UserDto> pagingQueryUsers(
            Boolean enabled,
            List<Long> userUidListOfLastModifiedBy,
            List<String> lastModifiedTimestampAsStringList,
            Long uid,
            String displayName,
            List<String> extendedPropertyList,
            List<String> accountList,
            List<Long> roleUidList,
            List<Long> groupUidList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Page<UserOverviewDto> pagingQueryUserOverview(
            List<Long> userUidListOfUser,
            List<Long> roleUidList,
            List<Long> groupUidList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<Long> listingQueryUidOfUsers(
            Long uid,
            String displayName,
            List<String> extendedPropertyList,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<Long> listingQueryUidOfUsers(
            String encodedInput,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<Long> listingQueryUidOfUsersByRole(
            List<Long> roleUidList,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<Long> listingQueryUidOfUsersByGroup(
            List<Long> groupUidList,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<UserBriefInformation> listingUserBriefInformation(
            List<Long> uidList,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void updateUserCredentials(
            UpdateUserCredentialDto updateUserCredentialDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void updateUserCredentials(
            Long uid,
            UpdateUserCredentialDto updateUserCredentialDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void batchCreateUsers(
            List<CreateUserDto> createUserDtoList,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void batchReplaceUsers(
            List<ReplaceUserDto> replaceUserDtoList,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Long createOrganizationUser(
            String displayName,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
