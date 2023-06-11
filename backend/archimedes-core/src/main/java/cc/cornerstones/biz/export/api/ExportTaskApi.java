package cc.cornerstones.biz.export.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;

import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.datasource.dto.DataSourceDto;
import cc.cornerstones.biz.datasource.share.constants.DatabaseServerTypeEnum;
import cc.cornerstones.biz.export.dto.ExportTaskDto;
import cc.cornerstones.biz.export.service.inf.ExportTaskService;
import cc.cornerstones.biz.export.share.constants.ExportTaskStatusEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "[Biz] Utilities / Export")
@RestController
@RequestMapping(value = "/utilities/export")
public class ExportTaskApi {
    @Autowired
    private UserService userService;

    @Autowired
    private ExportTaskService exportTaskService;

    @Operation(summary = "获取指定导出任务的进展")
    @GetMapping("/tasks")
    @ResponseBody
    public Response<ExportTaskDto> getExportTask(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "task_uid") Long taskUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.exportTaskService.getExportTask(
                        taskUid, operatingUserProfile));
    }

    @Operation(summary = "取消指定导出任务")
    @PostMapping("/tasks/cancel")
    @ResponseBody
    public Response<ExportTaskDto> cancelExportTask(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "task_uid") Long taskUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.exportTaskService.cancelExportTask(
                        taskUid, operatingUserProfile));
    }

    @Operation(summary = "列表查询导出任务")
    @Parameters(value = {
            @Parameter(name = "task_uid", description = "Export task 的 UID", required = false),
            @Parameter(name = "task_name", description = "Export task 的 Name", required = false),
            @Parameter(name = "task_status", description = "Export task 的 Type", required = false)
    })
    @GetMapping("/tasks/listing-query")
    @ResponseBody
    public Response<List<ExportTaskDto>> listingQueryExportTasks(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "task_uid", required = false) Long taskUid,
            @RequestParam(name = "task_name", required = false) String taskName,
            @RequestParam(name = "task_status", required = false) List<ExportTaskStatusEnum> taskStatusList,
            Sort sort) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // sort 特殊处理
        sort = AbcApiUtils.transformSort(sort);

        return Response.buildSuccess(
                this.exportTaskService.listingQueryExportTasks(
                        taskUid, taskName, taskStatusList,
                        sort,
                        operatingUserProfile));
    }
}
