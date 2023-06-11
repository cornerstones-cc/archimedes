package cc.cornerstones.biz.openapi.api;

import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.authentication.service.inf.OpenApiAuthService;
import cc.cornerstones.biz.export.dto.ExportTaskDto;
import cc.cornerstones.biz.export.service.inf.ExportTaskService;
import cc.cornerstones.biz.export.share.constants.ExportTaskStatusEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "[Open API] Utilities / Export / Tasks")
@RestController
@RequestMapping(value = "/open-api/utilities/export/tasks")
public class OpenApiExportTaskApi {
    @Autowired
    private OpenApiAuthService openApiAuthService;


    @Autowired
    private ExportTaskService exportTaskService;

    @Operation(summary = "获取指定导出任务的进展")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false),
            @Parameter(name = "task_uid", description = "Export task 的 UID", required = true)
    })
    @GetMapping("")
    @ResponseBody
    public Response<ExportTaskDto> getExportTask(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "access_token") String accessToken,
            @RequestParam(name = "subject", required = false) String subject,
            @RequestParam(name = "task_uid") Long taskUid) throws Exception {
        //
        // Step 1, Open api authentication & retrieving operating user's profile
        //
        this.openApiAuthService.authenticate(clientId, accessToken);
        UserProfile operatingUserProfile = this.openApiAuthService.getUserProfile(clientId, subject);

        //
        // Step 2, core-processing
        //
        return Response.buildSuccess(
                this.exportTaskService.getExportTask(
                        taskUid, operatingUserProfile));
    }

    @Operation(summary = "列表查询导出任务")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false),
            @Parameter(name = "task_uid", description = "Export task 的 UID", required = false),
            @Parameter(name = "task_name", description = "Export task 的 Name", required = false),
            @Parameter(name = "task_status", description = "Export task 的 Type", required = false)
    })
    @GetMapping("/listing-query")
    @ResponseBody
    public Response<List<ExportTaskDto>> listingQueryExportTasks(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "access_token") String accessToken,
            @RequestParam(name = "subject", required = false) String subject,
            @RequestParam(name = "task_uid", required = false) Long taskUid,
            @RequestParam(name = "task_name", required = false) String taskName,
            @RequestParam(name = "task_status", required = false) List<ExportTaskStatusEnum> taskStatusList,
            Sort sort) throws Exception {
        //
        // Step 1, Open api authentication & retrieving operating user's profile
        //
        this.openApiAuthService.authenticate(clientId, accessToken);
        UserProfile operatingUserProfile = this.openApiAuthService.getUserProfile(clientId, subject);

        // sort 特殊处理
        sort = AbcApiUtils.transformSort(sort);

        //
        // Step 2, core-processing
        //

        return Response.buildSuccess(
                this.exportTaskService.listingQueryExportTasks(
                        taskUid, taskName, taskStatusList,
                        sort,
                        operatingUserProfile));
    }
}
