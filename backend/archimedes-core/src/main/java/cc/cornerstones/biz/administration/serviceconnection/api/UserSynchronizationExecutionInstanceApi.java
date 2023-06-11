package cc.cornerstones.biz.administration.serviceconnection.api;

import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.serviceconnection.dto.CreateUserSynchronizationServiceAgentDto;
import cc.cornerstones.biz.administration.serviceconnection.dto.UpdateUserSynchronizationServiceAgentDto;
import cc.cornerstones.biz.administration.serviceconnection.dto.UserSynchronizationExecutionInstanceDto;
import cc.cornerstones.biz.administration.serviceconnection.dto.UserSynchronizationServiceAgentDto;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DfsServiceAgentService;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.UserSynchronizationServiceAgentService;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "[Biz] Admin / Service connection / User synchronization service / Execution instances")
@RestController
@RequestMapping(value = "/admin/service-connection/user-synchronization-service/execution-instances")
public class UserSynchronizationExecutionInstanceApi {
    @Autowired
    private UserService userService;

    @Autowired
    private UserSynchronizationServiceAgentService userSynchronizationServiceAgentService;

    @Autowired
    private DfsServiceAgentService dfsServiceAgentService;

    @Operation(summary = "分页查询 User synchronization execution instances")
    @GetMapping("/paging-query")
    @ResponseBody
    public Response<Page<UserSynchronizationExecutionInstanceDto>> pagingQueryUserSynchronizationExecutionInstances(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "user_synchronization_service_agent_uid", required = false) Long userSynchronizationServiceAgentUid,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "status", required = false) List<JobStatusEnum> jobStatusList,
            @RequestParam(name = "created_timestamp", required = false) List<String> createdTimestampAsStringList,
            @RequestParam(name = "last_modified_timestamp", required = false) List<String> lastModifiedTimestampAsStringList,
            Pageable pageable) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // pageable 中的 sort 特殊处理
        pageable = AbcApiUtils.transformPageable(pageable);

        // 业务逻辑
        return Response.buildSuccess(
                this.userSynchronizationServiceAgentService.pagingQueryUserSynchronizationExecutionInstances(
                        userSynchronizationServiceAgentUid, uid, jobStatusList,
                        createdTimestampAsStringList, lastModifiedTimestampAsStringList,
                        pageable,
                        operatingUserProfile));
    }

    @Operation(summary = "启动执行一次 User synchronization")
    @PostMapping("")
    @ResponseBody
    public Response<UserSynchronizationExecutionInstanceDto> createUserSynchronizationExecutionInstance(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "user_synchronization_service_agent_uid", required = false) Long userSynchronizationServiceAgentUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // 业务逻辑
        return Response.buildSuccess(
                this.userSynchronizationServiceAgentService.createUserSynchronizationExecutionInstance(
                        userSynchronizationServiceAgentUid,
                        operatingUserProfile));
    }
}
