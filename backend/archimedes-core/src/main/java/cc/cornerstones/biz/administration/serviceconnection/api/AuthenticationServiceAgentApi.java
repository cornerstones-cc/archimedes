package cc.cornerstones.biz.administration.serviceconnection.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.serviceconnection.dto.*;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.AuthenticationServiceAgentService;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
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

@Tag(name = "[Biz] Admin / Service connection / Authentication service / Service agents")
@RestController
@RequestMapping(value = "/admin/service-connection/authentication-service/service-agents")
public class AuthenticationServiceAgentApi {
    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationServiceAgentService authenticationServiceAgentService;

    @Operation(summary = "创建一个 Authentication service agent")
    @PostMapping("")
    @ResponseBody
    public Response<AuthenticationServiceAgentDto> createAuthenticationServiceAgent(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @Valid @RequestBody CreateAuthenticationServiceAgentDto createAuthenticationServiceAgentDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (CollectionUtils.isEmpty(createAuthenticationServiceAgentDto.getAccountTypeUidList())) {
            throw new AbcIllegalParameterException("account_type_uid_list should not be null or empty");
        }
        if (createAuthenticationServiceAgentDto.getEnabled() == null) {
            createAuthenticationServiceAgentDto.setEnabled(Boolean.FALSE);
        }
        if (createAuthenticationServiceAgentDto.getPreferred() == null) {
            createAuthenticationServiceAgentDto.setPreferred(Boolean.FALSE);
        }

        return Response.buildSuccess(
                this.authenticationServiceAgentService.createAuthenticationServiceAgent(
                        createAuthenticationServiceAgentDto,
                        operatingUserProfile));
    }

    @Operation(summary = "修改指定 Authentication service agent")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Authentication service agent 的 UID", required = true)
    })
    @PatchMapping("")
    @ResponseBody
    public Response updateAuthenticationServiceAgent(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid,
            @Valid @RequestBody UpdateAuthenticationServiceAgentDto updateAuthenticationServiceAgentDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.authenticationServiceAgentService.updateAuthenticationServiceAgent(
                uid, updateAuthenticationServiceAgentDto,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "列出针对指定 Authentication service agent 的所有引用")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Authentication service agent 的 UID", required = true)
    })
    @GetMapping("/references")
    @ResponseBody
    public Response<List<String>> listAllReferencesToAuthenticationServiceAgent(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.authenticationServiceAgentService.listAllReferencesToAuthenticationServiceAgent(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "删除指定 Authentication service agent")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Authentication service agent 的 UID", required = true)
    })
    @DeleteMapping("")
    @ResponseBody
    public Response deleteAuthenticationServiceAgent(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.authenticationServiceAgentService.deleteAuthenticationServiceAgent(
                uid,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "获取指定 Authentication service agent")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Authentication service agent 的 UID", required = true)
    })
    @GetMapping("")
    @ResponseBody
    public Response<AuthenticationServiceAgentDto> getAuthenticationServiceAgent(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.authenticationServiceAgentService.getAuthenticationServiceAgent(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "列表查询 Authentication service agents")
    @GetMapping("/listing-query")
    public Response<List<AuthenticationServiceAgentDto>> listingQueryAuthenticationServiceAgents(
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
                this.authenticationServiceAgentService.listingQueryAuthenticationServiceAgents(
                        uid, name, sort,
                        operatingUserProfile));
    }

    @Operation(summary = "分页查询 Authentication service agents")
    @GetMapping("/paging-query")
    public Response<Page<AuthenticationServiceAgentDto>> pagingQueryAuthenticationServiceAgents(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "description", required = false) String description,
            @RequestParam(name = "enabled", required = false) Boolean enabled,
            @RequestParam(name = "preferred", required = false) Boolean preferred,
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
                Page<AuthenticationServiceAgentDto> result = new PageImpl<>(new LinkedList<>(), pageable, 0);
                return Response.buildSuccess(result);
            }
        }

        // 业务逻辑
        return Response.buildSuccess(
                this.authenticationServiceAgentService.pagingQueryAuthenticationServiceAgents(
                        uid, name, description, enabled, preferred, userUidListOfLastModifiedBy,
                        lastModifiedTimestampAsStringList,
                        pageable,
                        operatingUserProfile));
    }
}
