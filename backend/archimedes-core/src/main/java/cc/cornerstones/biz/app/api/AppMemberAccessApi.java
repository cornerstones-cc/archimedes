package cc.cornerstones.biz.app.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.app.dto.*;
import cc.cornerstones.biz.app.service.inf.AppMemberAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Tag(name = "[Biz] Build / App / Member access")
@RestController
@RequestMapping(value = "/build/apps/member-access")
public class AppMemberAccessApi {
    @Autowired
    private UserService userService;

    @Autowired
    private AppMemberAccessService appMemberAccessService;

    @Operation(summary = "获取指定 App 针对 Member access 的 Grant strategy")
    @GetMapping("/grant-strategy")
    @ResponseBody
    public Response<AppMemberAccessGrantStrategyDto> getGrantStrategyOfApp(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "app_uid", required = true) Long appUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.appMemberAccessService.getGrantStrategyOfApp(
                        appUid,
                        operatingUserProfile));
    }

    @Operation(summary = "为指定 App 针对 Member access 创建或替换 Grant strategy")
    @PutMapping("/grant-strategy")
    @ResponseBody
    public Response<AppMemberAccessGrantStrategyDto> createOrReplaceGrantStrategyForApp(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "app_uid", required = true) Long appUid,
            @Valid @RequestBody CreateOrReplaceAppGrantStrategyDto createOrReplaceAppGrantStrategyDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (Boolean.TRUE.equals(createOrReplaceAppGrantStrategyDto.getEnabledEntireGrant())) {
            if (Boolean.TRUE.equals(createOrReplaceAppGrantStrategyDto.getEnabledGranularGrant())) {
                throw new AbcIllegalParameterException("enabled_entire_grant and enabled_granular_grant should not be" +
                        " enabled at the same time");
            }
        }
        if (!Boolean.TRUE.equals(createOrReplaceAppGrantStrategyDto.getEnabledEntireGrant())) {
            if (!Boolean.TRUE.equals(createOrReplaceAppGrantStrategyDto.getEnabledGranularGrant())) {
                throw new AbcIllegalParameterException("enabled_entire_grant and enabled_granular_grant should be " +
                        " enabled one");
            }
        }

        return Response.buildSuccess(
                this.appMemberAccessService.createOrReplaceGrantStrategyForApp(
                        appUid, createOrReplaceAppGrantStrategyDto,
                        operatingUserProfile));
    }

    @Operation(summary = "分页查询指定 App 针对 Member access 的 Grants")
    @Parameters(value = {
            @Parameter(name = "app_uid", description = "App 的 UID", required = true),
            @Parameter(name = "data_facet_hierarchy_node_uid", description = "App 的 Data facet hierarchy 上的 " +
                    "Hierarchy node UID", required =
                    false),
            @Parameter(name = "user", description = "App member 的 User", required = true)
    })
    @GetMapping("/grants/paging-query")
    public Response<Page<AppMemberUserAccessGrantDto>> pagingQueryMemberGrantsOfApp(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "app_uid", required = true) Long appUid,
            @RequestParam(name = "data_facet_hierarchy_node_uid", required = false) List<Long> dataFacetHierarchyNodeUidList,
            @RequestParam(name = "user", required = false) String user,
            @RequestParam(name = "role_uid", required = false) List<Long> roleUidList,
            @RequestParam(name = "group_uid", required = false) List<Long> groupUidList,
            Pageable pageable) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // pageable 中的 sort 特殊处理
        pageable = AbcApiUtils.transformPageable(pageable);

        // 查询用户 user
        List<Long> userUidListOfUser = null;
        if (!ObjectUtils.isEmpty(user)) {
            userUidListOfUser = this.userService.listingQueryUidOfUsers(
                    user, operatingUserProfile);
            if (CollectionUtils.isEmpty(userUidListOfUser)) {
                Page<AppMemberUserAccessGrantDto> result = new PageImpl<>(new LinkedList<>(), pageable, 0);
                return Response.buildSuccess(result);
            }
        }

        // 查询 role
        if (!CollectionUtils.isEmpty(roleUidList)) {
            List<Long> userUidListOfRole = this.userService.listingQueryUidOfUsersByRole(roleUidList,
                    operatingUserProfile);
            if (CollectionUtils.isEmpty(userUidListOfRole)) {
                Page<AppMemberUserAccessGrantDto> result = new PageImpl<>(new LinkedList<>(), pageable, 0);
                return Response.buildSuccess(result);
            } else {
                if (userUidListOfUser == null) {
                    userUidListOfUser = new LinkedList<>();
                    userUidListOfUser.addAll(userUidListOfRole);
                } else {
                    userUidListOfUser.retainAll(userUidListOfRole);

                    if (CollectionUtils.isEmpty(userUidListOfUser)) {
                        Page<AppMemberUserAccessGrantDto> result = new PageImpl<>(new LinkedList<>(), pageable, 0);
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
                Page<AppMemberUserAccessGrantDto> result = new PageImpl<>(new LinkedList<>(), pageable, 0);
                return Response.buildSuccess(result);
            } else {
                if (userUidListOfUser == null) {
                    userUidListOfUser = new LinkedList<>();
                    userUidListOfUser.addAll(userUidListOfGroup);
                } else {
                    userUidListOfUser.retainAll(userUidListOfGroup);

                    if (CollectionUtils.isEmpty(userUidListOfUser)) {
                        Page<AppMemberUserAccessGrantDto> result = new PageImpl<>(new LinkedList<>(), pageable, 0);
                        return Response.buildSuccess(result);
                    }
                }
            }
        }

        // 业务逻辑
        return Response.buildSuccess(
                this.appMemberAccessService.pagingQueryMemberGrantsOfApp(
                        appUid, dataFacetHierarchyNodeUidList,
                        userUidListOfUser,
                        pageable,
                        operatingUserProfile));
    }

    @Operation(summary = "获取指定用户在指定 App 的 Member access 方面的 Grants")
    @GetMapping("/grants/members")
    @ResponseBody
    public Response<AppGrantDto> getMemberGrantsOfMemberOfApp(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "app_uid", required = true) Long appUid,
            @RequestParam(name = "user_uid", required = true) Long userUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.appMemberAccessService.getMemberGrantOfApp(
                        appUid, userUid,
                        operatingUserProfile));
    }

    @Operation(summary = "为指定用户在指定 App 的 Member access 方面创建或替换 Grants")
    @PutMapping("/grants/members")
    @ResponseBody
    public Response createOrReplaceMemberGrantsForMemberForApp(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "app_uid", required = true) Long appUid,
            @RequestParam(name = "user_uid", required = true) Long userUid,
            @Valid @RequestBody CreateOrReplaceAppGrantDto createOrReplaceAppGrantDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        BulkGrantsByMemberDto bulkGrantsByMemberDto = new BulkGrantsByMemberDto();
        bulkGrantsByMemberDto.setUserUidList(new ArrayList<>(1));
        bulkGrantsByMemberDto.getUserUidList().add(userUid);
        bulkGrantsByMemberDto.setDataFacetHierarchyNodeUidList(
                createOrReplaceAppGrantDto.getDataFacetHierarchyNodeUidList());

        this.appMemberAccessService.bulkGrantsByMemberForApp(
                appUid, bulkGrantsByMemberDto,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "为指定 App 针对 Member access 执行 bulk grants by member")
    @PutMapping("/grants/bulk-grants-by-member")
    @ResponseBody
    public Response bulkGrantsByMemberForApp(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "app_uid", required = true) Long appUid,
            @Valid @RequestBody BulkGrantsByMemberDto bulkGrantsByMemberDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.appMemberAccessService.bulkGrantsByMemberForApp(
                appUid, bulkGrantsByMemberDto,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "为指定 App 针对 Member access 执行 bulk grants by role")
    @PutMapping("/grants/bulk-grants-by-role")
    @ResponseBody
    public Response bulkGrantsByRoleForApp(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "app_uid", required = true) Long appUid,
            @Valid @RequestBody BulkGrantsByRoleDto bulkGrantsByRoleDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.appMemberAccessService.bulkGrantsByRoleForApp(
                appUid, bulkGrantsByRoleDto,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "为指定 App 针对 Member access 执行 bulk grants by group")
    @PutMapping("/grants/bulk-grants-by-group")
    @ResponseBody
    public Response bulkGrantsByGroupForApp(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "app_uid", required = true) Long appUid,
            @Valid @RequestBody BulkGrantsByGroupDto bulkGrantsByGroupDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.appMemberAccessService.bulkGrantsByGroupForApp(
                appUid, bulkGrantsByGroupDto,
                operatingUserProfile);

        return Response.buildSuccess();
    }

}
