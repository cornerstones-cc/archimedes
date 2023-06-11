package cc.cornerstones.biz.datasource.api;

import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;

import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.datasource.dto.CreateDataSourceDto;
import cc.cornerstones.biz.datasource.dto.DataSourceDto;
import cc.cornerstones.biz.datasource.dto.UpdateDataSourceDto;
import cc.cornerstones.biz.datasource.service.assembly.database.QueryResult;
import cc.cornerstones.biz.datasource.service.inf.DataSourceService;
import cc.cornerstones.biz.datasource.share.constants.DatabaseServerTypeEnum;
import cc.cornerstones.biz.datatable.dto.TestQueryStatementDto;
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

@Tag(name = "[Biz] Build / Data sources")
@RestController
@RequestMapping(value = "/build/data-sources")
public class BuildDataSourceApi {
    @Autowired
    private UserService userService;

    @Autowired
    private DataSourceService dataSourceService;

    @Operation(summary = "创建 Data source")
    @PostMapping("")
    @ResponseBody
    public Response<DataSourceDto> createDataSource(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @Valid @RequestBody CreateDataSourceDto createDataSourceDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataSourceService.createDataSource(createDataSourceDto, operatingUserProfile));
    }

    @Operation(summary = "获取指定 Data source")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data source 的 UID", required = true)
    })
    @GetMapping("")
    @ResponseBody
    public Response<DataSourceDto> getDataSource(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long dataSourceUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataSourceService.getDataSource(dataSourceUid, operatingUserProfile));
    }

    @Operation(summary = "更新指定 Data source")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data source 的 UID", required = true)
    })
    @PatchMapping("")
    @ResponseBody
    public Response updateDataSource(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long dataSourceUid,
            @Valid @RequestBody UpdateDataSourceDto updateDataSourceDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dataSourceService.updateDataSource(
                dataSourceUid,
                updateDataSourceDto,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "列出针对指定 Data source 的所有引用")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data source 的 UID", required = true)
    })
    @GetMapping("/references")
    @ResponseBody
    public Response<List<String>> listAllReferencesToDataSource(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long dataSourceUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataSourceService.listAllReferencesToDataSource(
                        dataSourceUid,
                        operatingUserProfile));
    }

    @Operation(summary = "删除指定 Data source")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data source 的 UID", required = true)
    })
    @DeleteMapping("")
    @ResponseBody
    public Response deleteDataSource(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long dataSourceUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dataSourceService.deleteDataSource(
                dataSourceUid,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "分页查询 Data sources")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data source 的 UID", required = false),
            @Parameter(name = "name", description = "Data source 的 Name", required = false),
            @Parameter(name = "type", description = "Data source 的 Type", required = false)
    })
    @GetMapping("/paging-query")
    @ResponseBody
    public Response<Page<DataSourceDto>> pagingQueryDataSources(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long dataSourceUid,
            @RequestParam(name = "name", required = false) String dataSourceName,
            @RequestParam(name = "type", required = false) DatabaseServerTypeEnum dataSourceType,
            Pageable pageable) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // pageable 中的 sort 特殊处理
        pageable = AbcApiUtils.transformPageable(pageable);

        return Response.buildSuccess(
                this.dataSourceService.pagingQueryDataSources(
                        dataSourceUid, dataSourceName, dataSourceType,
                        pageable,
                        operatingUserProfile));
    }

    @Operation(summary = "列表查询 Data sources")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data source 的 UID", required = false),
            @Parameter(name = "name", description = "Data source 的 Name", required = false),
            @Parameter(name = "type", description = "Data source 的 Type", required = false)
    })
    @GetMapping("/listing-query")
    @ResponseBody
    public Response<List<DataSourceDto>> listingQueryDataSources(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long dataSourceUid,
            @RequestParam(name = "name", required = false) String dataSourceName,
            @RequestParam(name = "type", required = false) DatabaseServerTypeEnum dataSourceType,
            Sort sort) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // sort 特殊处理
        sort = AbcApiUtils.transformSort(sort);

        return Response.buildSuccess(
                this.dataSourceService.listingQueryDataSources(
                        dataSourceUid, dataSourceName, dataSourceType,
                        sort,
                        operatingUserProfile));
    }

    @Operation(summary = "重新获取指定 Data Source 的 Metadata，包括 tables & views, columns, and indexes")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data Source 的 UID", required = true)
    })
    @PostMapping("/async-metadata-retrieval")
    @ResponseBody
    public Response<Long> asyncRetrieveMetadataOfDataSource(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long dataSourceUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataSourceService.asyncRetrieveMetadataOfDataSource(
                        dataSourceUid, operatingUserProfile));
    }

    @Operation(summary = "获取指定 async metadata retrieval of data source 的任务状态")
    @Parameters(value = {
            @Parameter(name = "task_uid", description = "Async task 的 uid", required = true)
    })
    @GetMapping("/async-metadata-retrieval")
    @ResponseBody
    public Response<JobStatusEnum> getTaskStatusOfAsyncRetrieveMetadataOfDataSource(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "task_uid", required = true) Long taskUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataSourceService.getTaskStatusOfAsyncRetrieveMetadataOfDataSource(
                        taskUid, operatingUserProfile));
    }

    @Operation(summary = "针对指定 Data Source 测试 Query Statement")
    @PostMapping("/test-query-statement")
    @ResponseBody
    public Response<QueryResult> testQueryStatement(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long dataSourceUid,
            @Valid @RequestBody TestQueryStatementDto testQueryStatementDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataSourceService.testQueryStatement(
                        dataSourceUid, testQueryStatementDto, operatingUserProfile));
    }
}
