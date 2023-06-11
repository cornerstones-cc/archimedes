package cc.cornerstones.biz.administration.serviceconnection.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.serviceconnection.dto.BackEndComponentParsedResultDto;
import cc.cornerstones.biz.administration.serviceconnection.dto.CreatePluginAuthenticationServiceComponentDto;
import cc.cornerstones.biz.administration.serviceconnection.dto.UpdateAuthenticationServiceComponentDto;
import cc.cornerstones.biz.administration.serviceconnection.dto.AuthenticationServiceComponentDto;
import cc.cornerstones.biz.administration.serviceconnection.service.assembly.DfsServiceHandler;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.AuthenticationServiceComponentService;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DfsServiceAgentService;
import cc.cornerstones.biz.administration.serviceconnection.share.constants.ServiceComponentTypeEnum;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.app.dto.AppDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

@Tag(name = "[Biz] Admin / Service connection / Authentication service / Service components")
@RestController
@RequestMapping(value = "/admin/service-connection/authentication-service/service-components")
public class AuthenticationServiceComponentApi {
    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationServiceComponentService authenticationServiceComponentService;

    @Autowired
    private DfsServiceHandler dfsServiceHandler;

    @Operation(summary = "创建一个 Plug-in authentication service component")
    @PostMapping("")
    @ResponseBody
    public Response<AuthenticationServiceComponentDto> createPluginAuthenticationServiceComponent(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @Valid @RequestBody CreatePluginAuthenticationServiceComponentDto createPluginAuthenticationServiceComponentDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (!ObjectUtils.isEmpty(createPluginAuthenticationServiceComponentDto.getBackEndComponentFileId())) {
            if (createPluginAuthenticationServiceComponentDto.getDfsServiceAgentUidOfBackEndComponentFileId() == null) {
                throw new AbcIllegalParameterException("dfs_service_agent_uid_of_back_end_component_file_id should " +
                        "not be null if back_end_component_file_id is not null or empty");
            }
        }

        if (!ObjectUtils.isEmpty(createPluginAuthenticationServiceComponentDto.getFrontEndComponentFileId())) {
            if (createPluginAuthenticationServiceComponentDto.getDfsServiceAgentUidOfFrontEndComponentFileId() == null) {
                throw new AbcIllegalParameterException("dfs_service_agent_uid_of_front_end_component_file_id should " +
                        "not be null if front_end_component_file_id is not null or empty");
            }
        }

        return Response.buildSuccess(
                this.authenticationServiceComponentService.createPluginAuthenticationServiceComponent(
                        createPluginAuthenticationServiceComponentDto,
                        operatingUserProfile));
    }

    @Operation(summary = "修改指定 Plug-in Authentication service component")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Authentication service component 的 UID", required = true)
    })
    @PatchMapping("")
    @ResponseBody
    public Response updateAuthenticationServiceComponent(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid,
            @Valid @RequestBody UpdateAuthenticationServiceComponentDto updateAuthenticationServiceComponentDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (!ObjectUtils.isEmpty(updateAuthenticationServiceComponentDto.getBackEndComponentFileId())) {
            if (updateAuthenticationServiceComponentDto.getDfsServiceAgentUidOfBackEndComponentFileId() == null) {
                throw new AbcIllegalParameterException("dfs_service_agent_uid_of_back_end_component_file_id should " +
                        "not be null if back_end_component_file_id is not null or empty");
            }
        }

        if (!ObjectUtils.isEmpty(updateAuthenticationServiceComponentDto.getFrontEndComponentFileId())) {
            if (updateAuthenticationServiceComponentDto.getDfsServiceAgentUidOfFrontEndComponentFileId() == null) {
                throw new AbcIllegalParameterException("dfs_service_agent_uid_of_front_end_component_file_id should " +
                        "not be null if front_end_component_file_id is not null or empty");
            }
        }

        this.authenticationServiceComponentService.updateAuthenticationServiceComponent(
                uid, updateAuthenticationServiceComponentDto,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "解析 Plug-in authentication service component 的 back-end component 的程序文件包")
    @PostMapping("/parsing-back-end-component")
    @ResponseBody
    public Response<BackEndComponentParsedResultDto> postParseBackEndComponent(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "file_id") String fileId) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        File file = this.dfsServiceHandler.downloadFile(fileId, operatingUserProfile);

        return Response.buildSuccess(
                this.authenticationServiceComponentService.parseBackEndComponent(
                        file,
                        operatingUserProfile));
    }

    @Operation(summary = "解析 Plug-in authentication service component 的 back-end component 的程序文件包")
    @GetMapping("/parsing-back-end-component")
    @ResponseBody
    public Response<BackEndComponentParsedResultDto> getParseBackEndComponent(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "file_id") String fileId) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        File file = this.dfsServiceHandler.downloadFile(fileId, operatingUserProfile);

        return Response.buildSuccess(
                this.authenticationServiceComponentService.parseBackEndComponent(
                        file,
                        operatingUserProfile));
    }

    @Operation(summary = "列出针对指定 Plug-in Authentication service component 的所有引用")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Authentication service component 的 UID", required = true)
    })
    @GetMapping("/references")
    @ResponseBody
    public Response<List<String>> listAllReferencesToAuthenticationServiceComponent(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.authenticationServiceComponentService.listAllReferencesToAuthenticationServiceComponent(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "删除指定 Plug-in Authentication service component")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Authentication service Component 的 UID", required = true)
    })
    @DeleteMapping("")
    @ResponseBody
    public Response deleteAuthenticationServiceComponent(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.authenticationServiceComponentService.deleteAuthenticationServiceComponent(
                uid,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "获取指定 Plug-in Authentication service component")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Authentication service component 的 UID", required = true)
    })
    @GetMapping("")
    @ResponseBody
    public Response<AuthenticationServiceComponentDto> getAuthenticationServiceComponent(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.authenticationServiceComponentService.getAuthenticationServiceComponent(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "列表查询 Authentication service components")
    @GetMapping("/listing-query")
    public Response<List<AuthenticationServiceComponentDto>> listingQueryAuthenticationServiceComponents(
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
                this.authenticationServiceComponentService.listingQueryAuthenticationServiceComponents(
                        uid, name, sort,
                        operatingUserProfile));
    }

    @Operation(summary = "分页查询 Authentication service components")
    @GetMapping("/paging-query")
    public Response<Page<AuthenticationServiceComponentDto>> pagingQueryAuthenticationServiceComponents(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "description", required = false) String description,
            @RequestParam(name = "type", required = false) List<ServiceComponentTypeEnum> typeList,
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
                Page<AuthenticationServiceComponentDto> result = new PageImpl<>(new LinkedList<>(), pageable, 0);
                return Response.buildSuccess(result);
            }
        }

        // 业务逻辑
        return Response.buildSuccess(
                this.authenticationServiceComponentService.pagingQueryAuthenticationServiceComponents(
                        uid, name, description, typeList, userUidListOfLastModifiedBy,
                        lastModifiedTimestampAsStringList,
                        pageable,
                        operatingUserProfile));
    }

    @Operation(summary = "获取 Front end component interface")
    @GetMapping("/front-end-component-interface")
    @ResponseBody
    public ResponseEntity<Resource> getFrontEndComponentInterface(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

       Resource resource = this.authenticationServiceComponentService.getFrontEndComponentInterface(
                operatingUserProfile);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/force-download"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + new String(resource.getFilename().getBytes(StandardCharsets.UTF_8),
                                StandardCharsets.ISO_8859_1) + "\"")
                .body(resource);
    }

    @Operation(summary = "获取 Back end component interface")
    @GetMapping("/back-end-component-interface")
    @ResponseBody
    public ResponseEntity<Resource> getBackEndComponentInterface(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        Resource resource = this.authenticationServiceComponentService.getBackEndComponentInterface(
                operatingUserProfile);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/force-download"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + new String(resource.getFilename().getBytes(StandardCharsets.UTF_8),
                                StandardCharsets.ISO_8859_1) + "\"")
                .body(resource);
    }
}
