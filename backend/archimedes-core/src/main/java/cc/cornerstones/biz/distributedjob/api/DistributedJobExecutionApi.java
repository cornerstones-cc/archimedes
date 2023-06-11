package cc.cornerstones.biz.distributedjob.api;

import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;

import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.distributedjob.dto.CreateDistributedJobExecutionDto;
import cc.cornerstones.biz.distributedjob.dto.DistributedJobExecutionDto;
import cc.cornerstones.biz.distributedjob.service.inf.DistributedJobService;
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

@Tag(name = "[Biz] Utilities / Distributed job / Job execution")
@RestController
@RequestMapping(value = "/utilities/d-job/job-execution")
public class DistributedJobExecutionApi {
    @Autowired
    private UserService userService;

    @Autowired
    private DistributedJobService distributedJobService;

    @Operation(summary = "为指定 Job 创建/启动 Job execution")
    @PostMapping("")
    @ResponseBody
    public Response<DistributedJobExecutionDto> startJobExecution(
            @RequestParam(name = "job_uid") Long jobUid,
            @Valid @RequestBody CreateDistributedJobExecutionDto createDistributedJobExecutionDto) throws Exception {
        return Response.buildSuccess(this.distributedJobService.startJobExecution(jobUid));
    }

    @Operation(summary = "停止指定 Job execution")
    @PostMapping("/stop")
    @ResponseBody
    public Response<DistributedJobExecutionDto> stopJobExecution(
            @RequestParam(name = "uid") Long jobExecutionUid) throws Exception {
        return Response.buildSuccess(this.distributedJobService.stopJobExecution(jobExecutionUid));
    }

    @Operation(summary = "分页查询 Job executions")
    @Parameters(value = {
            @Parameter(name = "job_uid", description = "Job 的 UID", required = false),
            @Parameter(name = "uid", description = "Job execution 的 UID", required = false),
            @Parameter(name = "status", description = "Job execution 的 status", required = false),
            @Parameter(name = "created_timestamp", description = "Job execution 的 created timestamp", required = false),
            @Parameter(name = "last_modified_timestamp", description = "Job execution 的 last modified timestamp",
                    required = false)
    })
    @GetMapping("/paging-query")
    @ResponseBody
    public Response<Page<DistributedJobExecutionDto>> pagingQueryJobExecutions(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "job_uid", required = false) Long jobUid,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "status", required = false) List<JobStatusEnum> statusList,
            @RequestParam(name = "created_timestamp", required = false) List<String> createdTimestampAsStringList,
            @RequestParam(name = "last_modified_timestamp", required = false) List<String> lastModifiedTimestampAsStringList,
            Pageable pageable) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // pageable 中的 sort 特殊处理
        pageable = AbcApiUtils.transformPageable(pageable);

        return Response.buildSuccess(
                this.distributedJobService.pagingQueryJobExecutions(
                        jobUid, uid, statusList, createdTimestampAsStringList, lastModifiedTimestampAsStringList,
                        pageable,
                        operatingUserProfile));
    }

}
