package cc.cornerstones.biz.app.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.app.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AppMemberAccessService {

    AppMemberAccessGrantStrategyDto getGrantStrategyOfApp(
            Long appUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    AppMemberAccessGrantStrategyDto createOrReplaceGrantStrategyForApp(
            Long appUid,
            CreateOrReplaceAppGrantStrategyDto createOrReplaceAppGrantStrategyDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Page<AppMemberUserAccessGrantDto> pagingQueryMemberGrantsOfApp(
            Long appUid,
            List<Long> dataFacetHierarchyNodeUidList,
            List<Long> userUidListOfUser,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    AppGrantDto getMemberGrantOfApp(
            Long appUid,
            Long userUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void bulkGrantsByMemberForApp(
            Long appUid,
            BulkGrantsByMemberDto bulkGrantsByMemberDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void bulkGrantsByRoleForApp(
            Long appUid,
            BulkGrantsByRoleDto bulkGrantsByRoleDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void bulkGrantsByGroupForApp(
            Long appUid,
            BulkGrantsByGroupDto bulkGrantsByGroupDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;


}
