package cc.cornerstones.biz.administration.serviceconnection.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.serviceconnection.dto.*;
import cc.cornerstones.biz.administration.serviceconnection.service.assembly.DfsServiceHandler;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.UserSynchronizationServiceComponentService;
import cc.cornerstones.biz.administration.serviceconnection.share.constants.ServiceComponentTypeEnum;
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
import java.io.File;
import java.util.LinkedList;
import java.util.List;

@Tag(name = "[Biz] Admin / Service connection / User synchronization service / Service component")
@RestController
@RequestMapping(value = "/admin/service-connection/user-synchronization-service/service-components")
public class UserSynchronizationServiceComponentApi {
    @Autowired
    private UserService userService;

    @Autowired
    private UserSynchronizationServiceComponentService userSynchronizationServiceComponentService;

    @Autowired
    private DfsServiceHandler dfsServiceHandler;

    @Operation(summary = "创建一个 Plug-in user synchronization service component")
    @PostMapping("")
    @ResponseBody
    public Response<UserSynchronizationServiceComponentDto> createPluginUserSynchronizationServiceComponent(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @Valid @RequestBody CreatePluginUserSynchronizationServiceComponentDto createPluginUserSynchronizationServiceComponentDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (!ObjectUtils.isEmpty(createPluginUserSynchronizationServiceComponentDto.getBackEndComponentFileId())) {
            if (createPluginUserSynchronizationServiceComponentDto.getDfsServiceAgentUidOfBackEndComponentFileId() == null) {
                throw new AbcIllegalParameterException("dfs_service_agent_uid_of_back_end_component_file_id should " +
                        "not be null if back_end_component_file_id is not null or empty");
            }
        }

        if (!ObjectUtils.isEmpty(createPluginUserSynchronizationServiceComponentDto.getFrontEndComponentFileId())) {
            if (createPluginUserSynchronizationServiceComponentDto.getDfsServiceAgentUidOfFrontEndComponentFileId() == null) {
                throw new AbcIllegalParameterException("dfs_service_agent_uid_of_front_end_component_file_id should " +
                        "not be null if front_end_component_file_id is not null or empty");
            }
        }

        return Response.buildSuccess(
                this.userSynchronizationServiceComponentService.createPluginUserSynchronizationServiceComponent(
                        createPluginUserSynchronizationServiceComponentDto,
                        operatingUserProfile));
    }

    @Operation(summary = "修改指定 User synchronization service component")
    @Parameters(value = {
            @Parameter(name = "uid", description = "User synchronization service component 的 UID", required = true)
    })
    @PatchMapping("")
    @ResponseBody
    public Response updateUserSynchronizationServiceComponent(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid,
            @Valid @RequestBody UpdateUserSynchronizationServiceComponentDto updateUserSynchronizationServiceComponentDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (!ObjectUtils.isEmpty(updateUserSynchronizationServiceComponentDto.getBackEndComponentFileId())) {
            if (updateUserSynchronizationServiceComponentDto.getDfsServiceAgentUidOfBackEndComponentFileId() == null) {
                throw new AbcIllegalParameterException("dfs_service_agent_uid_of_back_end_component_file_id should " +
                        "not be null if back_end_component_file_id is not null or empty");
            }
        }

        if (!ObjectUtils.isEmpty(updateUserSynchronizationServiceComponentDto.getFrontEndComponentFileId())) {
            if (updateUserSynchronizationServiceComponentDto.getDfsServiceAgentUidOfFrontEndComponentFileId() == null) {
                throw new AbcIllegalParameterException("dfs_service_agent_uid_of_front_end_component_file_id should " +
                        "not be null if front_end_component_file_id is not null or empty");
            }
        }

        this.userSynchronizationServiceComponentService.updateUserSynchronizationServiceComponent(
                uid, updateUserSynchronizationServiceComponentDto,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "解析 Plug-in user synchronization service component 的 back-end component 的程序文件包")
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
                this.userSynchronizationServiceComponentService.parseBackEndComponent(
                        file,
                        operatingUserProfile));
    }

    @Operation(summary = "解析 Plug-in user synchronization service component 的 back-end component 的程序文件包")
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
                this.userSynchronizationServiceComponentService.parseBackEndComponent(
                        file,
                        operatingUserProfile));
    }

    @Operation(summary = "列出针对指定 User synchronization service component 的所有引用")
    @Parameters(value = {
            @Parameter(name = "uid", description = "User synchronization service component 的 UID", required = true)
    })
    @GetMapping("/references")
    @ResponseBody
    public Response<List<String>> listAllReferencesToUserSynchronizationServiceComponent(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.userSynchronizationServiceComponentService.listAllReferencesToUserSynchronizationServiceComponent(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "删除指定 User synchronization service component")
    @Parameters(value = {
            @Parameter(name = "uid", description = "User synchronization service component 的 UID", required = true)
    })
    @DeleteMapping("")
    @ResponseBody
    public Response deleteUserSynchronizationServiceComponent(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.userSynchronizationServiceComponentService.deleteUserSynchronizationServiceComponent(
                uid,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "获取指定 User synchronization service component")
    @Parameters(value = {
            @Parameter(name = "uid", description = "User synchronization service component 的 UID", required = true)
    })
    @GetMapping("")
    @ResponseBody
    public Response<UserSynchronizationServiceComponentDto> getUserSynchronizationServiceComponent(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.userSynchronizationServiceComponentService.getUserSynchronizationServiceComponent(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "列表查询 User synchronization service components")
    @GetMapping("/listing-query")
    public Response<List<UserSynchronizationServiceComponentDto>> listingQueryUserSynchronizationServiceComponents(
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
                this.userSynchronizationServiceComponentService.listingQueryUserSynchronizationServiceComponents(
                        uid, name, sort,
                        operatingUserProfile));
    }

    @Operation(summary = "分页查询 User synchronization service components")
    @GetMapping("/paging-query")
    public Response<Page<UserSynchronizationServiceComponentDto>> pagingQueryUserSynchronizationServiceComponents(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "description", required = false) String description,
            @RequestParam(name = "type", required = false) List<ServiceComponentTypeEnum> typeList,
            @RequestParam(name = "enabled", required = false) Boolean enabled,
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
                Page<UserSynchronizationServiceComponentDto> result = new PageImpl<>(new LinkedList<>(), pageable, 0);
                return Response.buildSuccess(result);
            }
        }

        // 业务逻辑
        return Response.buildSuccess(
                this.userSynchronizationServiceComponentService.pagingQueryUserSynchronizationServiceComponents(
                        uid, name, description, typeList, enabled, userUidListOfLastModifiedBy, lastModifiedTimestampAsStringList,
                        pageable,
                        operatingUserProfile));
    }
}
