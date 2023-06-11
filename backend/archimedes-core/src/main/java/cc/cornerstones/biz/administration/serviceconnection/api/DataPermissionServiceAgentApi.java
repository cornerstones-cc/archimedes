package cc.cornerstones.biz.administration.serviceconnection.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.serviceconnection.dto.CreateDataPermissionServiceAgentDto;
import cc.cornerstones.biz.administration.serviceconnection.dto.DataPermissionServiceAgentDto;
import cc.cornerstones.biz.administration.serviceconnection.dto.UpdateDataPermissionServiceAgentDto;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DataPermissionServiceAgentService;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DfsServiceAgentService;
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

@Tag(name = "[Biz] Admin / Service connection / Data permission service / Service agent")
@RestController
@RequestMapping(value = "/admin/service-connection/data-permission-service/service-agents")
public class DataPermissionServiceAgentApi {
    @Autowired
    private UserService userService;

    @Autowired
    private DataPermissionServiceAgentService dataPermissionServiceAgentService;

    @Autowired
    private DfsServiceAgentService dfsServiceAgentService;

    @Operation(summary = "创建一个 Data permission service agent")
    @PostMapping("")
    @ResponseBody
    public Response<DataPermissionServiceAgentDto> createDataPermissionServiceAgent(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @Valid @RequestBody CreateDataPermissionServiceAgentDto createDataPermissionServiceAgentDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (createDataPermissionServiceAgentDto.getEnabled() == null) {
            createDataPermissionServiceAgentDto.setEnabled(Boolean.FALSE);
        }

        return Response.buildSuccess(
                this.dataPermissionServiceAgentService.createDataPermissionServiceAgent(
                        createDataPermissionServiceAgentDto,
                        operatingUserProfile));
    }

    @Operation(summary = "修改指定 Data permission service agent")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data permission service agent 的 UID", required = true)
    })
    @PatchMapping("")
    @ResponseBody
    public Response updateDataPermissionServiceAgent(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid,
            @Valid @RequestBody UpdateDataPermissionServiceAgentDto updateDataPermissionServiceAgentDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dataPermissionServiceAgentService.updateDataPermissionServiceAgent(
                uid, updateDataPermissionServiceAgentDto,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "列出针对指定 Data permission service agent 的所有引用")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data permission service agent 的 UID", required = true)
    })
    @GetMapping("/references")
    @ResponseBody
    public Response<List<String>> listAllReferencesToDataPermissionServiceAgent(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataPermissionServiceAgentService.listAllReferencesToDataPermissionServiceAgent(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "删除指定 Data permission service agent")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data permission service agent 的 UID", required = true)
    })
    @DeleteMapping("")
    @ResponseBody
    public Response deleteDataPermissionServiceAgent(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dataPermissionServiceAgentService.deleteDataPermissionServiceAgent(
                uid,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "获取指定 Data permission service agent")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data permission service agent 的 UID", required = true)
    })
    @GetMapping("")
    @ResponseBody
    public Response<DataPermissionServiceAgentDto> getDataPermissionServiceAgent(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataPermissionServiceAgentService.getDataPermissionServiceAgent(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "列表查询 Data permission service agents")
    @GetMapping("/listing-query")
    public Response<List<DataPermissionServiceAgentDto>> listingQueryDataPermissionServiceAgents(
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
                this.dataPermissionServiceAgentService.listingQueryDataPermissionServiceAgents(
                        uid, name, sort,
                        operatingUserProfile));
    }

    @Operation(summary = "分页查询 Data permission service agents")
    @GetMapping("/paging-query")
    public Response<Page<DataPermissionServiceAgentDto>> pagingQueryDataPermissionServiceAgents(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "description", required = false) String description,
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
                Page<DataPermissionServiceAgentDto> result = new PageImpl<>(new LinkedList<>(), pageable, 0);
                return Response.buildSuccess(result);
            }
        }

        // 业务逻辑
        return Response.buildSuccess(
                this.dataPermissionServiceAgentService.pagingQueryDataPermissionServiceAgents(
                        uid, name, description, enabled, userUidListOfLastModifiedBy, lastModifiedTimestampAsStringList,
                        pageable,
                        operatingUserProfile));
    }
}
