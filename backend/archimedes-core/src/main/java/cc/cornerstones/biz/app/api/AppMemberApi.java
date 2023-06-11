package cc.cornerstones.biz.app.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.app.dto.*;
import cc.cornerstones.biz.app.service.inf.AppMemberService;
import cc.cornerstones.biz.app.share.constants.AppMembershipEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Tag(name = "[Biz] Build / App / Member")
@RestController
@RequestMapping(value = "/build/apps/members")
public class AppMemberApi {
    @Autowired
    private UserService userService;
    
    @Autowired
    private AppMemberService appMemberService;
    
    @Operation(summary = "为指定 App 获取 Invite strategy")
    @Parameters(value = {
            @Parameter(name = "app_uid", description = "App 的 UID", required = true)
    })
    @GetMapping("/invite-strategy")
    @ResponseBody
    public Response<AppMemberInviteStrategyDto> getInviteStrategyOfApp(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "app_uid", required = true) Long appUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.appMemberService.getInviteStrategyOfApp(
                        appUid,
                        operatingUserProfile));
    }

    @Operation(summary = "为指定 App 创建或替换 Invite strategy")
    @Parameters(value = {
            @Parameter(name = "app_uid", description = "App 的 UID", required = true)
    })
    @PutMapping("/invite-strategy")
    @ResponseBody
    public Response<AppMemberInviteStrategyDto> createOrReplaceInviteStrategyForApp(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "app_uid", required = true) Long appUid,
            @Valid @RequestBody CreateOrReplaceAppInviteStrategyDto createOrReplaceAppInviteStrategyDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (!Boolean.TRUE.equals(createOrReplaceAppInviteStrategyDto.getEnabledRoles())) {
            createOrReplaceAppInviteStrategyDto.setRoleUidList(null);
        }
        if (!Boolean.TRUE.equals(createOrReplaceAppInviteStrategyDto.getEnabledGroups())) {
            createOrReplaceAppInviteStrategyDto.setGroupUidList(null);
        }

        return Response.buildSuccess(
                this.appMemberService.createOrReplaceInviteStrategyForApp(
                        appUid, createOrReplaceAppInviteStrategyDto,
                        operatingUserProfile));
    }

    @Operation(summary = "为指定 App 添加一个 Member")
    @Parameters(value = {
            @Parameter(name = "app_uid", description = "App 的 UID", required = true)
    })
    @PostMapping("")
    @ResponseBody
    public Response<AppMemberDto> createMemberForApp(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "app_uid", required = true) Long appUid,
            @Valid @RequestBody CreateAppMemberDto createAppMemberDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (createAppMemberDto.getUserUid() == null) {
            throw new AbcIllegalParameterException("user_uid should not be null");
        }

        if (createAppMemberDto.getMembership() == null) {
            createAppMemberDto.setMembership(AppMembershipEnum.MEMBER);
        }

        return Response.buildSuccess(
                this.appMemberService.createMemberForApp(
                        appUid, createAppMemberDto,
                        operatingUserProfile));
    }

    @Operation(summary = "为指定 App 添加一个或多个 Member(s)")
    @Parameters(value = {
            @Parameter(name = "app_uid", description = "App 的 UID", required = true)
    })
    @PostMapping("/batch")
    @ResponseBody
    public Response<List<AppMemberDto>> batchCreateMembersForApp(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "app_uid", required = true) Long appUid,
            @Valid @RequestBody BatchCreateAppMemberDto batchCreateAppMemberDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (CollectionUtils.isEmpty(batchCreateAppMemberDto.getUserUidList())) {
            throw new AbcIllegalParameterException("user_uid_list should not be null or empty");
        }
        for (Long userUid : batchCreateAppMemberDto.getUserUidList()) {
            if (userUid == null) {
                throw new AbcIllegalParameterException("user_uid should not be null");
            }
        }

        if (batchCreateAppMemberDto.getMembership() == null) {
            batchCreateAppMemberDto.setMembership(AppMembershipEnum.MEMBER);
        }

        return Response.buildSuccess(
                this.appMemberService.batchCreateMembersForApp(
                        appUid, batchCreateAppMemberDto,
                        operatingUserProfile));
    }

    @Operation(summary = "为指定 App 修改指定 Member")
    @Parameters(value = {
            @Parameter(name = "app_uid", description = "App 的 UID", required = true),
            @Parameter(name = "user_uid", description = "App member 的 User UID", required = true)
    })
    @PatchMapping("")
    @ResponseBody
    public Response updateMemberOfApp(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "app_uid", required = true) Long appUid,
            @RequestParam(name = "user_uid", required = true) Long userUid,
            @Valid @RequestBody UpdateAppMemberDto updateAppMemberDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (updateAppMemberDto.getMembership() == null) {
            throw new AbcIllegalParameterException("membership should not be null");
        }

        this.appMemberService.updateMemberOfApp(
                appUid, userUid, updateAppMemberDto,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "列出针对指定 App 的指定 Member 的所有引用")
    @Parameters(value = {
            @Parameter(name = "app_uid", description = "App 的 UID", required = true),
            @Parameter(name = "user_uid", description = "App member 的 User UID", required = true)
    })
    @GetMapping("/references")
    @ResponseBody
    public Response<List<String>> listAllReferencesToMemberOfApp(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "app_uid", required = true) Long appUid,
            @RequestParam(name = "user_uid", required = true) Long userUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.appMemberService.listAllReferencesToMemberOfApp(
                        appUid, userUid,
                        operatingUserProfile));
    }

    @Operation(summary = "为指定 App 删除指定 Member")
    @Parameters(value = {
            @Parameter(name = "app_uid", description = "App 的 UID", required = true),
            @Parameter(name = "user_uid", description = "App member 的 User UID", required = true)
    })
    @DeleteMapping("")
    @ResponseBody
    public Response deleteMemberOfApp(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "app_uid", required = true) Long appUid,
            @RequestParam(name = "user_uid", required = true) Long userUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.appMemberService.deleteMemberOfApp(
                appUid, userUid,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "为指定 App 批量删除指定 Members")
    @Parameters(value = {
            @Parameter(name = "app_uid", description = "App 的 UID", required = true),
            @Parameter(name = "user_uid", description = "App member 的 User UID", required = true)
    })
    @DeleteMapping("/bulk")
    @ResponseBody
    public Response bulkDeleteMembersOfApp(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "app_uid", required = true) Long appUid,
            @RequestBody BulkRemoveAppMembersDto bulkRemoveAppMembersDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.appMemberService.bulkDeleteMembersOfApp(
                appUid, bulkRemoveAppMembersDto,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "列表查询指定 App 的 Members")
    @Parameters(value = {
            @Parameter(name = "app_uid", description = "App 的 UID", required = true),
            @Parameter(name = "membership", description = "App member 的 Membership", required = false),
            @Parameter(name = "user", description = "App member 的 User", required = false)
    })
    @GetMapping("/listing-query")
    public Response<List<AppMemberUserDto>> listingQueryMembersOfApp(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "app_uid", required = true) Long appUid,
            @RequestParam(name = "membership", required = false) List<AppMembershipEnum> membershipList,
            @RequestParam(name = "user", required = false) String user,
            Sort sort) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // sort 特殊处理
        sort = AbcApiUtils.transformSort(sort);

        // 查询用户 user
        List<Long> userUidListOfUser = null;
        if (!ObjectUtils.isEmpty(user)) {
            userUidListOfUser = this.userService.listingQueryUidOfUsers(
                    user, operatingUserProfile);
            if (CollectionUtils.isEmpty(userUidListOfUser)) {
                return Response.buildSuccess(null);
            }
        }

        // 业务逻辑
        return Response.buildSuccess(
                this.appMemberService.listingQueryMembersOfApp(
                        appUid,
                        membershipList,
                        userUidListOfUser,
                        sort,
                        operatingUserProfile));
    }

    @Operation(summary = "分页查询指定 App 的 Members")
    @Parameters(value = {
            @Parameter(name = "app_uid", description = "App 的 UID", required = true),
            @Parameter(name = "membership", description = "App member 的 Membership", required = false),
            @Parameter(name = "last_modified_by", description = "App member 的 Membership 的 last modified by",
                    required =
                            false),
            @Parameter(name = "last_modified_timestamp", description = "App member 的 Membership last " +
                    "modified timestamp", required =
                    false),
            @Parameter(name = "user", description = "App member 的 User", required = true)
    })
    @GetMapping("/paging-query")
    public Response<Page<AppMemberUserDto>> pagingQueryMembersOfApp(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "app_uid", required = true) Long appUid,
            @RequestParam(name = "membership", required = false) List<AppMembershipEnum> membershipList,
            @RequestParam(name = "last_modified_by", required = false) String membershipLastModifiedBy,
            @RequestParam(name = "last_modified_timestamp", required = false) List<String> membershipLastModifiedTimestampAsStringList,
            @RequestParam(name = "user", required = false) String user,
            @RequestParam(name = "role_uid", required = false) List<Long> roleUidList,
            @RequestParam(name = "group_uid", required = false) List<Long> groupUidList,
            Pageable pageable,
            HttpServletRequest request) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // pageable 中的 sort 特殊处理
        pageable = AbcApiUtils.transformPageable(pageable);

        // 查询用户 membershipLastModifiedBy
        List<Long> userUidListOfMembershipLastModifiedBy = null;
        if (!ObjectUtils.isEmpty(membershipLastModifiedBy)) {
            userUidListOfMembershipLastModifiedBy = this.userService.listingQueryUidOfUsers(
                    membershipLastModifiedBy, operatingUserProfile);
            if (CollectionUtils.isEmpty(userUidListOfMembershipLastModifiedBy)) {
                Page<AppMemberUserDto> result = new PageImpl<>(new LinkedList<>(), pageable, 0);
                return Response.buildSuccess(result);
            }
        }

        // 查询用户 user
        // 查询用户 lastModifiedBy
        List<Long> userUidListOfUser = null;
        if (!ObjectUtils.isEmpty(user)) {
            userUidListOfUser = this.userService.listingQueryUidOfUsers(
                    user, operatingUserProfile);
            if (CollectionUtils.isEmpty(userUidListOfUser)) {
                Page<AppMemberUserDto> result = new PageImpl<>(new LinkedList<>(), pageable, 0);
                return Response.buildSuccess(result);
            }
        }

        // 查询 role
        if (!CollectionUtils.isEmpty(roleUidList)) {
            List<Long> userUidListOfRole = this.userService.listingQueryUidOfUsersByRole(roleUidList,
                    operatingUserProfile);
            if (CollectionUtils.isEmpty(userUidListOfRole)) {
                Page<AppMemberUserDto> result = new PageImpl<>(new LinkedList<>(), pageable, 0);
                return Response.buildSuccess(result);
            } else {
                if (userUidListOfUser == null) {
                    userUidListOfUser = new LinkedList<>();
                    userUidListOfUser.addAll(userUidListOfRole);
                } else {
                    userUidListOfUser.retainAll(userUidListOfRole);

                    if (CollectionUtils.isEmpty(userUidListOfUser)) {
                        Page<AppMemberUserDto> result = new PageImpl<>(new LinkedList<>(), pageable, 0);
                        return Response.buildSuccess(result);
                    }
                }
            }
        }

        // 查询 group
        if (!CollectionUtils.isEmpty(groupUidList)) {
            List<Long> userUidListOfGroup = this.userService.listingQueryUidOfUsersByGroup(groupUidList,
                    operatingUserProfile);
            if (CollectionUtils.isEmpty(userUidListOfGroup)) {
                Page<AppMemberUserDto> result = new PageImpl<>(new LinkedList<>(), pageable, 0);
                return Response.buildSuccess(result);
            } else {
                if (userUidListOfUser == null) {
                    userUidListOfUser = new LinkedList<>();
                    userUidListOfUser.addAll(userUidListOfGroup);
                } else {
                    userUidListOfUser.retainAll(userUidListOfGroup);

                    if (CollectionUtils.isEmpty(userUidListOfUser)) {
                        Page<AppMemberUserDto> result = new PageImpl<>(new LinkedList<>(), pageable, 0);
                        return Response.buildSuccess(result);
                    }
                }
            }
        }

        // 业务逻辑
        return Response.buildSuccess(
                this.appMemberService.pagingQueryMembersOfApp(
                        appUid,
                        membershipList, userUidListOfMembershipLastModifiedBy, membershipLastModifiedTimestampAsStringList,
                        userUidListOfUser,
                        pageable,
                        operatingUserProfile));
    }
}
