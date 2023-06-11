package cc.cornerstones.biz.administration.usermanagement.api;

import cc.cornerstones.almond.constants.InfrastructureConstants;
import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;

import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.usermanagement.dto.*;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.administration.usermanagement.share.types.Account;
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Tag(name = "[Biz] Admin / User management / Users")
@RestController
@RequestMapping(value = "/admin/user-mgmt/users")
public class UserApi {
    @Autowired
    private UserService userService;

    @Operation(summary = "创建 User")
    @PostMapping("")
    @ResponseBody
    public Response<UserDto> createUser(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @Valid @RequestBody CreateUserDto createUserDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (ObjectUtils.isEmpty(createUserDto.getDisplayName())) {
            throw new AbcIllegalParameterException("display_name should not be null or empty");
        }
        if (createUserDto.getDisplayName().equalsIgnoreCase(InfrastructureConstants.ROOT_USER_DISPLAY_NAME)) {
            throw new AbcIllegalParameterException("display_name is reserved");
        }
        if (CollectionUtils.isEmpty(createUserDto.getAccountList())) {
            throw new AbcIllegalParameterException("at least 1 account is required");
        } else {
            Map<Long, String> accountMap = new HashMap<>();
            for (Account account : createUserDto.getAccountList()) {
                if (accountMap.containsKey(account.getAccountTypeUid())) {
                    throw new AbcIllegalParameterException("at most 1 account name is allowed for 1 account type");
                }
                if (ObjectUtils.isEmpty(account.getAccountName())) {
                    throw new AbcIllegalParameterException("account name should not be null or empty");
                }
                accountMap.put(account.getAccountTypeUid(), account.getAccountName());
            }
        }
        if (createUserDto.getEnabled() == null) {
            createUserDto.setEnabled(Boolean.TRUE);
        }

        return Response.buildSuccess(
                this.userService.createUser(
                        createUserDto,
                        operatingUserProfile));
    }

    @Operation(summary = "替换指定 User")
    @Parameters(value = {
            @Parameter(name = "uid", description = "User 的 UID", required = true)
    })
    @PutMapping("")
    @ResponseBody
    public Response replaceUser(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid,
            @Valid @RequestBody ReplaceUserDto replaceUserDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (ObjectUtils.isEmpty(replaceUserDto.getDisplayName())) {
            throw new AbcIllegalParameterException("display_name should not be null or empty");
        }
        if (replaceUserDto.getDisplayName().equalsIgnoreCase(InfrastructureConstants.ROOT_USER_DISPLAY_NAME)) {
            throw new AbcIllegalParameterException("display_name is reserved");
        }
        if (CollectionUtils.isEmpty(replaceUserDto.getAccountList())) {
            throw new AbcIllegalParameterException("at least 1 account is required");
        } else {
            Map<Long, String> accountMap = new HashMap<>();
            for (Account account : replaceUserDto.getAccountList()) {
                if (accountMap.containsKey(account.getAccountTypeUid())) {
                    throw new AbcIllegalParameterException("at most 1 account name is allowed for 1 account type");
                }
                if (ObjectUtils.isEmpty(account.getAccountName())) {
                    throw new AbcIllegalParameterException("account name should not be null or empty");
                }
                accountMap.put(account.getAccountTypeUid(), account.getAccountName());
            }
        }
        if (replaceUserDto.getEnabled() == null) {
            replaceUserDto.setEnabled(Boolean.TRUE);
        }

        this.userService.replaceUser(
                uid, replaceUserDto,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "列出针对指定 User 的所有引用")
    @Parameters(value = {
            @Parameter(name = "uid", description = "User 的 UID", required = true)
    })
    @GetMapping("/references")
    @ResponseBody
    public Response<List<String>> listAllReferencesToUser(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.userService.listAllReferencesToUser(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "删除指定 User")
    @Parameters(value = {
            @Parameter(name = "uid", description = "User 的 UID", required = true)
    })
    @DeleteMapping("")
    @ResponseBody
    public Response deleteUser(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.userService.deleteUser(
                uid,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "获取指定 User")
    @Parameters(value = {
            @Parameter(name = "uid", description = "User 的 UID", required = true)
    })
    @GetMapping("")
    @ResponseBody
    public Response<UserDto> getUser(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.userService.getUser(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "获取指定 User 的明细")
    @Parameters(value = {
            @Parameter(name = "uid", description = "User 的 UID", required = true)
    })
    @GetMapping("/details")
    @ResponseBody
    public Response<UserDetailedDto> getDetailsOfUser(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.userService.getUserDetailed(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "列表查询 Users")
    @GetMapping("/listing-query")
    public Response<List<UserDto>> listingQueryUsers(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "display_name", required = false) String displayName,
            Sort sort) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // sort 特殊处理
        sort = AbcApiUtils.transformSort(sort);

        // 业务逻辑
        return Response.buildSuccess(
                this.userService.listingQueryUsers(
                        uid, displayName,
                        sort,
                        operatingUserProfile));
    }

    @Operation(summary = "分页查询 Users")
    @GetMapping("/another-paging-query")
    public Response<Page<UserDto>> anotherPagingQueryUsers(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "enabled", required = false) Boolean enabled,
            @RequestParam(name = "last_modified_by", required = false) String lastModifiedBy,
            @RequestParam(name = "last_modified_timestamp", required = false) List<String> lastModifiedTimestampAsStringList,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "display_name", required = false) String displayName,
            @RequestParam(name = "extended_property", required = false) List<String> extendedPropertyList,
            @RequestParam(name = "account", required = false) List<String> accountList,
            @RequestParam(name = "role_uid", required = false) List<Long> roleUidList,
            @RequestParam(name = "group_uid", required = false) List<Long> groupUidList,
            Pageable pageable,
            HttpServletRequest request) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // pageable 中的 sort 特殊处理
        pageable = AbcApiUtils.transformPageable(pageable);

        // 查询用户 lastModifiedBy
        List<Long> userUidListOfLastModifiedBy = null;
        if (!ObjectUtils.isEmpty(lastModifiedBy)) {
            userUidListOfLastModifiedBy = this.userService.listingQueryUidOfUsers(
                    lastModifiedBy, operatingUserProfile);
            if (CollectionUtils.isEmpty(userUidListOfLastModifiedBy)) {
                Page<UserDto> result = new PageImpl<>(new LinkedList<>(), pageable, 0);
                return Response.buildSuccess(result);
            }
        }

        // 业务逻辑
        return Response.buildSuccess(
                this.userService.pagingQueryUsers(
                        enabled, userUidListOfLastModifiedBy, lastModifiedTimestampAsStringList,
                        uid, displayName,
                        extendedPropertyList,
                        accountList,
                        roleUidList,
                        groupUidList,
                        pageable,
                        operatingUserProfile));
    }

    @Operation(summary = "利用 User 的基本属性和扩展属性查询 Users，返回每个 User 的 UID")
    @GetMapping("/listing-query-uid")
    public Response<List<Long>> listingQueryUidOfUsers(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "display_name", required = false) String displayName,
            @RequestParam(name = "extended_property", required = false) List<String> extendedPropertyList,
            HttpServletRequest request) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // 业务逻辑
        return Response.buildSuccess(
                this.userService.listingQueryUidOfUsers(
                        uid, displayName,
                        extendedPropertyList,
                        operatingUserProfile));
    }

    @Operation(summary = "分页查询 User overview")
    @GetMapping("/overview/paging-query")
    public Response<Page<UserOverviewDto>> pagingQueryUserOverview(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
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

        // 查询用户 user
        List<Long> userUidListOfUser = null;
        if (!ObjectUtils.isEmpty(user)) {
            userUidListOfUser = this.userService.listingQueryUidOfUsers(
                    user, operatingUserProfile);
            if (CollectionUtils.isEmpty(userUidListOfUser)) {
                Page<UserOverviewDto> result = new PageImpl<>(new LinkedList<>(), pageable, 0);
                return Response.buildSuccess(result);
            }
        }

        // 业务逻辑
        return Response.buildSuccess(
                this.userService.pagingQueryUserOverview(
                        userUidListOfUser,
                        roleUidList, groupUidList,
                        pageable,
                        operatingUserProfile));
    }
}
