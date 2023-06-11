package cc.cornerstones.biz.serve.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;

import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.datawidget.dto.DataWidgetDto;
import cc.cornerstones.biz.datawidget.share.constants.DataWidgetTypeEnum;
import cc.cornerstones.biz.serve.service.inf.ExploreDataWidgetService;
import cc.cornerstones.biz.operations.accesslogging.dto.CreateOrUpdateQueryLogDto;
import cc.cornerstones.biz.operations.accesslogging.service.inf.AccessLoggingService;
import cc.cornerstones.biz.share.types.QueryContentResult;
import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "[Biz] Explore / Data widgets")
@RestController
@RequestMapping(value = "/explore/data-widgets")
public class ExploreDataWidgetApi {
    @Autowired
    private UserService userService;

    @Autowired
    private ExploreDataWidgetService exploreDataWidgetService;

    @Autowired
    private AccessLoggingService accessLoggingService;

    @Operation(summary = "列表查询指定 Data facet 的所有 Data widgets")
    @Parameters(value = {
            @Parameter(name = "data_facet_uid", description = "Data facet 的 UID", required = true),
            @Parameter(name = "uid", description = "Data widget 的 UID", required = false),
            @Parameter(name = "name", description = "Data widget 的 Name", required = false),
            @Parameter(name = "type", description = "Data widget 的 Type", required = false)
    })
    @GetMapping("/listing-query")
    @ResponseBody
    public Response<List<DataWidgetDto>> listingQueryDataWidgetsOfDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "type", required = false) DataWidgetTypeEnum type,
            Sort sort) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // sort 特殊处理
        sort = AbcApiUtils.transformSort(sort);

        return Response.buildSuccess(
                this.exploreDataWidgetService.listingQueryDataWidgetsOfDataFacet(
                        dataFacetUid,
                        uid, name, type,
                        sort,
                        operatingUserProfile));
    }

    @Operation(summary = "获取指定 Data widget 的配置")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data widget 的 UID", required = false)
    })
    @GetMapping("")
    @ResponseBody
    public Response<DataWidgetDto> getDataWidget(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.exploreDataWidgetService.getDataWidget(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "查询内容")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data widget 的 UID", required = true)
    })
    @PostMapping("/content/query")
    @ResponseBody
    public Response<QueryContentResult> queryContent(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid,
            @RequestBody(required = false) JSONObject request) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // Tracking
        CreateOrUpdateQueryLogDto createQueryLogDto = new CreateOrUpdateQueryLogDto();
        createQueryLogDto.setTrackingSerialNumber(operatingUserProfile.getTrackingSerialNumber());
        createQueryLogDto.setUserUid(operatingUserProfile.getUid());
        createQueryLogDto.setDisplayName(operatingUserProfile.getDisplayName());
        createQueryLogDto.setRequest(request);
        this.accessLoggingService.createQueryLog(createQueryLogDto, operatingUserProfile);

        return Response.buildSuccess(
                this.exploreDataWidgetService.queryContent(
                        uid, request, operatingUserProfile));
    }

    @Operation(summary = "导出内容")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data widget 的 UID", required = true)
    })
    @PostMapping("/content/export")
    @ResponseBody
    public Response<Long> exportContent(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid,
            @RequestBody(required = false) JSONObject request) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.exploreDataWidgetService.exportContent(
                        uid, request, operatingUserProfile));
    }
}
