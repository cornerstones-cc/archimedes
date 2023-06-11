package cc.cornerstones.biz.datatable.api;


import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.TreeNode;
import cc.cornerstones.almond.types.UserProfile;

import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.datasource.service.assembly.database.QueryResult;
import cc.cornerstones.biz.datatable.dto.CreateIndirectDataTableDto;
import cc.cornerstones.biz.datatable.dto.DataTableDto;
import cc.cornerstones.biz.datatable.dto.UpdateIndirectDataTableDto;
import cc.cornerstones.biz.datatable.service.inf.DataTableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "[Biz] Build / Data tables")
@RestController
@RequestMapping(value = "/build/data-tables")
public class BuildDataTableApi {
    @Autowired
    private UserService userService;

    @Autowired
    private DataTableService dataTableService;

    @Operation(summary = "为指定 Data source 创建 Data table (Indirect)")
    @PostMapping("")
    @ResponseBody
    public Response<DataTableDto> createIndirectDataTable(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_source_uid", required = false) Long dataSourceUid,
            @Valid @RequestBody CreateIndirectDataTableDto createIndirectDataTableDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataTableService.createIndirectDataTable(dataSourceUid, createIndirectDataTableDto,
                        operatingUserProfile));
    }

    @Operation(summary = "更新指定 Data table (Indirect)")
    @PatchMapping("")
    @ResponseBody
    public Response updateIndirectDataTable(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long dataTableUid,
            @Valid @RequestBody UpdateIndirectDataTableDto updateIndirectDataTableDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dataTableService.updateIndirectDataTable(dataTableUid, updateIndirectDataTableDto,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "获取指定 Data table")
    @GetMapping("")
    @ResponseBody
    public Response<DataTableDto> getDataTable(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long dataTableUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(this.dataTableService.getDataTable(dataTableUid,
                operatingUserProfile));
    }

    @Operation(summary = "列出针对指定 Data table (Indirect) 的所有引用")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data table (Indirect) 的 UID", required = true)
    })
    @GetMapping("/references")
    @ResponseBody
    public Response<List<String>> listAllReferencesToDataTable(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long dataTableUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataTableService.listAllReferencesToDataTable(
                        dataTableUid,
                        operatingUserProfile));
    }

    @Operation(summary = "删除指定 Data table (Indirect)")
    @DeleteMapping("")
    @ResponseBody
    public Response deleteDataTable(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long dataTableUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dataTableService.deleteDataTable(dataTableUid,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "树形列出指定 Data source 或者所有 Data sources 的 Data tables")
    @Parameters(value = {
            @Parameter(name = "data_source_uid", description = "Data source 的 UID", required = false)
    })
    @GetMapping("/data-table-hierarchy/tree-listing-all-nodes")
    @ResponseBody
    public Response<List<TreeNode>> treeListingAllNodes(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_source_uid", required = false) Long dataSourceUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataTableService.treeListingAllDataTables(
                        dataSourceUid, operatingUserProfile));
    }

    @Operation(summary = "重新获取指定 Data table 的 Metadata，包括 columns and indexes")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data table 的 UID", required = true)
    })
    @PostMapping("/async-metadata-retrieval")
    @ResponseBody
    public Response<Long> asyncRetrieveMetadataOfDataTable(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long dataTableUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataTableService.asyncRetrieveMetadataOfDataTable(
                        dataTableUid, operatingUserProfile));
    }

    @Operation(summary = "获取指定 async metadata retrieval of data table 的任务状态")
    @Parameters(value = {
            @Parameter(name = "task_uid", description = "Async task 的 UID", required = true)
    })
    @GetMapping("/async-metadata-retrieval")
    @ResponseBody
    public Response<JobStatusEnum> getTaskStatusOfAsyncRetrieveMetadataOfDataTable(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "task_uid", required = true) Long taskUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataTableService.getTaskStatusOfAsyncRetrieveMetadataOfDataTable(
                        taskUid, operatingUserProfile));
    }

    @Operation(summary = "重新获取指定 Context path 的 Metadata，包括 Data tables 及每个 Data table 的 columns and indexes")
    @Parameters(value = {
            @Parameter(name = "data_source_uid", description = "Data source 的 UID", required = true),
            @Parameter(name = "context_path", description = "Context path", required = true)
    })
    @PostMapping("/context-path/async-metadata-retrieval")
    @ResponseBody
    public Response<Long> asyncRetrieveMetadataOfContextPath(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_source_uid", required = true) Long dataSourceUid,
            @RequestParam(name = "context_path", required = true) String contextPath) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataTableService.asyncRetrieveMetadataOfContextPath(
                        dataSourceUid, contextPath, operatingUserProfile));
    }

    @Operation(summary = "获取指定 async metadata retrieval of context path 的任务状态")
    @Parameters(value = {
            @Parameter(name = "task_uid", description = "Async task 的 UID", required = true)
    })
    @GetMapping("/context-path/async-metadata-retrieval")
    @ResponseBody
    public Response<JobStatusEnum> getTaskStatusOfAsyncRetrieveMetadataOfContextPath(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "task_uid", required = true) Long taskUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataTableService.getTaskStatusOfAsyncRetrieveMetadataOfContextPath(
                        taskUid, operatingUserProfile));
    }

    @Operation(summary = "获取指定 Data table 的 Sample data")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data table 的 UID", required = true)
    })
    @GetMapping("/sample-data")
    @ResponseBody
    public Response<QueryResult> getSampleDataOfDataTable(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long dataTableUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataTableService.getSampleDataOfDataTable(
                        dataTableUid,
                        operatingUserProfile));
    }
}
