package cc.cornerstones.biz.distributedjob.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;

import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.distributedjob.dto.CreateDistributedJobDto;
import cc.cornerstones.biz.distributedjob.dto.DistributedJobDto;
import cc.cornerstones.biz.distributedjob.dto.UpdateDistributedJobDto;
import cc.cornerstones.biz.distributedjob.service.inf.DistributedJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;


@Tag(name = "[Biz] Utilities / Distributed job / Job registration")
@RestController
@RequestMapping(value = "/utilities/d-job/job-registration")
public class DistributedJobRegistrationApi {
    @Autowired
    private UserService userService;

    @Autowired
    private DistributedJobService distributedJobService;

    @Operation(summary = "创建 Job")
    @PostMapping("")
    @ResponseBody
    public Response<DistributedJobDto> createJob(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @Valid @RequestBody CreateDistributedJobDto createDistributedJobDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.distributedJobService.createJob(
                        createDistributedJobDto, operatingUserProfile));
    }

    @Operation(summary = "修改指定 Job")
    @PatchMapping("")
    @ResponseBody
    public Response updateJob(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long jobUid,
            @Valid @RequestBody UpdateDistributedJobDto updateDistributedJobDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.distributedJobService.updateJob(
                jobUid, updateDistributedJobDto, operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "获取指定 Job")
    @GetMapping("")
    @ResponseBody
    public Response<DistributedJobDto> getJob(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long jobUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.distributedJobService.getJob(
                        jobUid, operatingUserProfile));
    }

    @Operation(summary = "列出针对指定 Job 的所有引用")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Job 的 UID", required = true)
    })
    @GetMapping("/references")
    @ResponseBody
    public Response<List<String>> listAllReferencesToJob(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long jobUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.distributedJobService.listAllReferencesToJob(
                        jobUid,
                        operatingUserProfile));
    }

    @Operation(summary = "删除指定 Job")
    @DeleteMapping("")
    @ResponseBody
    public Response deleteJob(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long jobUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.distributedJobService.deleteJob(
                jobUid, operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "分页查询 Jobs")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Job 的 UID", required = false),
            @Parameter(name = "name", description = "Job 的 Name", required = false),
            @Parameter(name = "enabled", description = "Job 的 Enabled", required = false),
    })
    @GetMapping("/paging-query")
    @ResponseBody
    public Response<Page<DistributedJobDto>> pagingQueryJobs(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long jobUid,
            @RequestParam(name = "name", required = false) String jobName,
            @RequestParam(name = "enabled", required = false) Boolean enabled,
            Pageable pageable) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // pageable 中的 sort 特殊处理
        pageable = AbcApiUtils.transformPageable(pageable);

        return Response.buildSuccess(
                this.distributedJobService.pagingQueryJobs(
                        jobUid, jobName, enabled,
                        pageable,
                        operatingUserProfile));
    }


    @Operation(summary = "获取 Server IP Address")
    @GetMapping("/server-ip-address")
    @ResponseBody
    public Response<String> getServerIpAddress(
            HttpServletRequest request) throws Exception {
        return Response.buildSuccess(request.getLocalAddr());
    }
}
