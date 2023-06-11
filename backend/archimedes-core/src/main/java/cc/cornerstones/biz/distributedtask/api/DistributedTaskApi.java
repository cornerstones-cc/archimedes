package cc.cornerstones.biz.distributedtask.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.constants.TaskStatusEnum;
import cc.cornerstones.almond.types.Response;

import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.distributedtask.dto.StartDistributedTaskDto;
import cc.cornerstones.biz.distributedtask.dto.DistributedTaskDto;
import cc.cornerstones.biz.distributedtask.service.inf.DistributedTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "[Biz] Utilities / Distributed task / Task execution")
@RestController
@RequestMapping(value = "/utilities/d-task")
public class DistributedTaskApi {
    @Autowired
    private UserService userService;

    @Autowired
    private DistributedTaskService taskService;

    @Operation(summary = "启动 Task")
    @PostMapping("/tasks/start")
    @ResponseBody
    public Response<DistributedTaskDto> startTask(
            @RequestParam(name = "task_uid") Long taskUid,
            @Valid @RequestBody StartDistributedTaskDto startDistributedTaskDto) throws Exception {
        this.taskService.startTask(taskUid,null);
        return Response.buildSuccess();
    }

    @Operation(summary = "停止指定 Task")
    @PostMapping("/tasks/stop")
    @ResponseBody
    public Response<DistributedTaskDto> stopTask(
            @RequestParam(name = "task_uid") Long taskUid) throws Exception {
        this.taskService.stopTask(taskUid, null);
        return Response.buildSuccess();
    }

    @Operation(summary = "获取指定 Task")
    @GetMapping("/tasks")
    @ResponseBody
    public Response<DistributedTaskDto> getTask(
            @RequestParam(name = "uid") Long taskUid) throws Exception {
        return Response.buildSuccess(
                this.taskService.getTask(taskUid, null));
    }

    @Operation(summary = "分页查询 Tasks")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Task 的 UID", required = false),
            @Parameter(name = "name", description = "Task 的 name", required = false),
            @Parameter(name = "status", description = "Task 的 status", required = false),
            @Parameter(name = "created_timestamp", description = "Task 的 created timestamp", required = false),
            @Parameter(name = "last_modified_timestamp", description = "Task 的 last modified timestamp",
                    required = false)
    })
    @GetMapping("/tasks/paging-query")
    @ResponseBody
    public Response<Page<DistributedTaskDto>> pagingQueryTasks(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "status", required = false) List<TaskStatusEnum> statusList,
            @RequestParam(name = "created_timestamp", required = false) List<String> createdTimestampAsStringList,
            @RequestParam(name = "last_modified_timestamp", required = false) List<String> lastModifiedTimestampAsStringList,
            Pageable pageable) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // pageable 中的 sort 特殊处理
        pageable = AbcApiUtils.transformPageable(pageable);

        return Response.buildSuccess(
                this.taskService.pagingQueryTasks(
                        uid, name, statusList, createdTimestampAsStringList, lastModifiedTimestampAsStringList,
                        pageable,
                        operatingUserProfile));
    }
}
