package cc.cornerstones.biz.app.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.app.dto.*;
import cc.cornerstones.biz.app.share.constants.AppMembershipEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;

public interface AppMemberService {

    AppMemberInviteStrategyDto getInviteStrategyOfApp(
            Long appUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    AppMemberInviteStrategyDto createOrReplaceInviteStrategyForApp(
            Long appUid,
            CreateOrReplaceAppInviteStrategyDto createOrReplaceAppInviteStrategyDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    AppMemberDto createMemberForApp(
            Long appUid,
            CreateAppMemberDto createAppMemberDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<AppMemberDto> batchCreateMembersForApp(
            Long appUid,
            BatchCreateAppMemberDto batchCreateAppMemberDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void updateMemberOfApp(
            Long appUid,
            Long userUid,
            UpdateAppMemberDto updateAppMemberDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<String> listAllReferencesToMemberOfApp(
            Long appUid,
            Long userUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void deleteMemberOfApp(
            Long appUid,
            Long userUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void bulkDeleteMembersOfApp(
            Long appUid,
            BulkRemoveAppMembersDto bulkRemoveAppMembersDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<AppMemberUserDto> listingQueryMembersOfApp(
            Long appUid,
            List<AppMembershipEnum> membershipList,
            List<Long> userUidListOfUser,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Page<AppMemberUserDto> pagingQueryMembersOfApp(
            Long appUid,
            List<AppMembershipEnum> membershipList,
            List<Long> userUidListOfMembershipLastModifiedBy,
            List<String> membershipLastModifiedTimestampAsStringList,
            List<Long> userUidListOfUser,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

}
