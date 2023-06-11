package cc.cornerstones.biz.datawidget.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.datawidget.dto.CreateDataWidgetDto;
import cc.cornerstones.biz.datawidget.dto.DataWidgetDto;
import cc.cornerstones.biz.datawidget.dto.UpdateDataWidgetDto;
import cc.cornerstones.biz.datawidget.service.inf.DataWidgetService;
import cc.cornerstones.biz.datawidget.share.constants.DataWidgetTypeEnum;
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

@Tag(name = "[Biz] Build / Data widgets")
@RestController
@RequestMapping(value = "/build/data-widgets")
public class BuildDataWidgetApi {
    @Autowired
    private UserService userService;

    @Autowired
    private DataWidgetService dataWidgetService;

    @Operation(summary = "为指定 Data facet 创建 Data widget")
    @PostMapping("")
    @ResponseBody
    public Response<DataWidgetDto> createDataWidget(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid,
            @Valid @RequestBody CreateDataWidgetDto createDataWidgetDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataWidgetService.createDataWidget(
                        dataFacetUid, createDataWidgetDto, operatingUserProfile));
    }

    @Operation(summary = "修改指定 Data widget")
    @PatchMapping("")
    @ResponseBody
    public Response updateDataWidget(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long dataWidgetUid,
            @Valid @RequestBody UpdateDataWidgetDto updateDataWidgetDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dataWidgetService.updateDataWidget(
                dataWidgetUid, updateDataWidgetDto, operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "获取指定 Data widget")
    @GetMapping("")
    @ResponseBody
    public Response<DataWidgetDto> getDataWidget(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long dataWidgetUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataWidgetService.getDataWidget(
                        dataWidgetUid, operatingUserProfile));
    }

    @Operation(summary = "列出针对指定 Data widget 的所有引用")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data widget 的 UID", required = true)
    })
    @GetMapping("/references")
    @ResponseBody
    public Response<List<String>> listAllReferencesToDataWidget(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long dataWidgetUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataWidgetService.listAllReferencesToDataWidget(
                        dataWidgetUid,
                        operatingUserProfile));
    }

    @Operation(summary = "删除指定 Data widget")
    @DeleteMapping("")
    @ResponseBody
    public Response deleteDataWidget(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long dataWidgetUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dataWidgetService.deleteDataWidget(
                dataWidgetUid, operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "分页查询 Data widgets")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data widget 的 UID", required = false),
            @Parameter(name = "name", description = "Data widget 的 Name", required = false),
            @Parameter(name = "type", description = "Data widget 的 Type", required = false)
    })
    @GetMapping("/paging-query")
    @ResponseBody
    public Response<Page<DataWidgetDto>> pagingQueryDataWidgets(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long dataWidgetUid,
            @RequestParam(name = "name", required = false) String dataWidgetName,
            @RequestParam(name = "type", required = false) DataWidgetTypeEnum dataWidgetType,
            Pageable pageable) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // pageable 中的 sort 特殊处理
        pageable = AbcApiUtils.transformPageable(pageable);

        return Response.buildSuccess(
                this.dataWidgetService.pagingQueryDataWidgets(
                        dataWidgetUid, dataWidgetName, dataWidgetType,
                        pageable,
                        operatingUserProfile));
    }

    @Operation(summary = "列表查询 Data widgets")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data widget 的 UID", required = false),
            @Parameter(name = "name", description = "Data widget 的 Name", required = false),
            @Parameter(name = "type", description = "Data widget 的 Type", required = false)
    })
    @GetMapping("/listing-query")
    @ResponseBody
    public Response<List<DataWidgetDto>> listingQueryDataWidgets(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long dataWidgetUid,
            @RequestParam(name = "name", required = false) String dataWidgetName,
            @RequestParam(name = "type", required = false) DataWidgetTypeEnum dataWidgetType,
            Sort sort) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // sort 特殊处理
        sort = AbcApiUtils.transformSort(sort);

        return Response.buildSuccess(
                this.dataWidgetService.listingQueryDataWidgets(
                        dataWidgetUid, dataWidgetName, dataWidgetType,
                        sort,
                        operatingUserProfile));
    }

}
