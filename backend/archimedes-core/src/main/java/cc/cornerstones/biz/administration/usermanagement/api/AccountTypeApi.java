package cc.cornerstones.biz.administration.usermanagement.api;


import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.usermanagement.dto.AccountTypeDto;
import cc.cornerstones.biz.administration.usermanagement.dto.CreateAccountTypeDto;
import cc.cornerstones.biz.administration.usermanagement.dto.UpdateAccountTypeDto;
import cc.cornerstones.biz.administration.usermanagement.service.inf.AccountTypeService;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.app.dto.AppDto;
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

import javax.validation.Valid;
import java.util.LinkedList;
import java.util.List;

@Tag(name = "[Biz] Admin / User management / Account types")
@RestController
@RequestMapping(value = "/admin/user-mgmt/account-types")
public class AccountTypeApi {
    @Autowired
    private UserService userService;

    @Autowired
    private AccountTypeService accountTypeService;

    @Operation(summary = "创建 Account type (帐号类型)")
    @PostMapping("")
    @ResponseBody
    public Response<AccountTypeDto> createAccountType(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @Valid @RequestBody CreateAccountTypeDto createAccountTypeDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (createAccountTypeDto.getSequence() == null) {
            createAccountTypeDto.setSequence(1.0f);
        }
        if (createAccountTypeDto.getEnabled() == null) {
            createAccountTypeDto.setEnabled(Boolean.FALSE);
        }

        return Response.buildSuccess(
                this.accountTypeService.createAccountType(
                        createAccountTypeDto,
                        operatingUserProfile));
    }

    @Operation(summary = "更新指定 Account type (帐号类型)")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Account type 的 UID", required = true)
    })
    @PatchMapping("")
    @ResponseBody
    public Response updateAccountType(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid,
            @Valid @RequestBody UpdateAccountTypeDto updateAccountTypeDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.accountTypeService.updateAccountType(
                uid, updateAccountTypeDto,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "列出针对指定 Account type (帐号类型) 的所有引用")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Account type 的 UID", required = true)
    })
    @GetMapping("/references")
    @ResponseBody
    public Response<List<String>> listAllReferencesToAccountType(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.accountTypeService.listAllReferencesToAccountType(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "删除指定 Account type (帐号类型)")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Account type 的 UID", required = true)
    })
    @DeleteMapping("")
    @ResponseBody
    public Response deleteAccountType(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.accountTypeService.deleteAccountType(
                uid,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "获取指定 Account type (帐号类型)")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Account type 的 UID", required = true)
    })
    @GetMapping("")
    @ResponseBody
    public Response<AccountTypeDto> getAccountType(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.accountTypeService.getAccountType(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "列表查询 Account types (帐号类型)")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Account type 的 UID", required = false),
            @Parameter(name = "name", description = "Account type 的 name", required = false)
    })
    @GetMapping("/listing-query")
    public Response<List<AccountTypeDto>> listingQueryAccountTypes(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "name", required = false) String name,
            Sort sort) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // sort 特殊处理
        sort = AbcApiUtils.transformSort(sort);

        // 业务逻辑
        return Response.buildSuccess(
                this.accountTypeService.listingQueryAccountTypes(
                        uid, name,
                        sort,
                        operatingUserProfile));
    }

    @Operation(summary = "分页查询 Account types (帐号类型)")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Account type 的 UID", required = false),
            @Parameter(name = "name", description = "Account type 的 name", required = false),
            @Parameter(name = "description", description = "Account type 的 description", required = false),
            @Parameter(name = "last_modified_by", description = "Account type 的 last modified by", required = false),
            @Parameter(name = "last_modified_timestamp", description = "Account type 的 last modified timestamp",
                    required = false)
    })
    @GetMapping("/paging-query")
    public Response<Page<AccountTypeDto>> pagingQueryAccountTypes(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "description", required = false) String description,
            @RequestParam(name = "last_modified_by", required = false) String lastModifiedBy,
            @RequestParam(name = "last_modified_timestamp", required = false) List<String> lastModifiedTimestampAsStringList,
            Pageable pageable) throws Exception {
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
                Page<AccountTypeDto> result = new PageImpl<>(new LinkedList<>(), pageable, 0);
                return Response.buildSuccess(result);
            }
        }

        // 业务逻辑
        return Response.buildSuccess(
                this.accountTypeService.pagingQueryAccountTypes(
                        uid, name, description, userUidListOfLastModifiedBy, lastModifiedTimestampAsStringList,
                        pageable,
                        operatingUserProfile));
    }
}
