package cc.cornerstones.biz.openapi.api;

import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.authentication.service.inf.OpenApiAuthService;
import cc.cornerstones.biz.datawidget.dto.DataWidgetDto;
import cc.cornerstones.biz.datawidget.share.constants.DataWidgetTypeEnum;
import cc.cornerstones.biz.operations.accesslogging.dto.CreateOrUpdateQueryLogDto;
import cc.cornerstones.biz.operations.accesslogging.service.inf.AccessLoggingService;
import cc.cornerstones.biz.serve.service.inf.ExploreDataWidgetService;
import cc.cornerstones.biz.share.types.QueryContentResult;
import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "[Open API] Explore / Data widgets")
@RestController
@RequestMapping(value = "/open-api/explore/data-widgets")
public class OpenApiExploreDataWidgetApi {
    @Autowired
    private OpenApiAuthService openApiAuthService;

    @Autowired
    private ExploreDataWidgetService exploreDataWidgetService;

    @Autowired
    private AccessLoggingService accessLoggingService;

    @Operation(summary = "列表查询指定 Data facet 的所有 Data widgets")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false),
            @Parameter(name = "data_facet_uid", description = "Data facet 的 UID", required = true),
            @Parameter(name = "uid", description = "Data widget 的 UID", required = false),
            @Parameter(name = "name", description = "Data widget 的 Name", required = false),
            @Parameter(name = "type", description = "Data widget 的 Type", required = false)
    })
    @GetMapping("/listing-query")
    @ResponseBody
    public Response<List<DataWidgetDto>> listingQueryDataWidgetsOfDataFacet(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "access_token") String accessToken,
            @RequestParam(name = "subject", required = false) String subject,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "type", required = false) DataWidgetTypeEnum type,
            Sort sort) throws Exception {
        //
        // Step 1, Open api authentication & retrieving operating user's profile
        //
        this.openApiAuthService.authenticate(clientId, accessToken);

        UserProfile operatingUserProfile = this.openApiAuthService.getUserProfile(clientId, subject);
        this.openApiAuthService.authorizeDataFacet(dataFacetUid, operatingUserProfile);

        // sort 特殊处理
        sort = AbcApiUtils.transformSort(sort);

        //
        // Step 2, core-processing
        //
        return Response.buildSuccess(
                this.exploreDataWidgetService.listingQueryDataWidgetsOfDataFacet(
                        dataFacetUid,
                        uid, name, type,
                        sort,
                        operatingUserProfile));
    }

    @Operation(summary = "获取指定 Data widget 的配置")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false),
            @Parameter(name = "uid", description = "Data widget 的 UID", required = false)
    })
    @GetMapping("")
    @ResponseBody
    public Response<DataWidgetDto> getDataWidget(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "access_token") String accessToken,
            @RequestParam(name = "subject", required = false) String subject,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        //
        // Step 1, Open api authentication & retrieving operating user's profile
        //
        this.openApiAuthService.authenticate(clientId, accessToken);
        UserProfile operatingUserProfile = this.openApiAuthService.getUserProfile(clientId, subject);

        //
        // Step 2, core-processing
        //
        DataWidgetDto dataWidgetDto = this.exploreDataWidgetService.getDataWidget(
                uid,
                operatingUserProfile);

        if (dataWidgetDto == null) {
            return Response.buildSuccess();
        } else {
            this.openApiAuthService.authorizeDataFacet(dataWidgetDto.getDataFacetUid(), operatingUserProfile);

            return Response.buildSuccess(dataWidgetDto);
        }
    }

    @Operation(summary = "查询内容")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false),
            @Parameter(name = "uid", description = "Data widget 的 UID", required = false)
    })
    @PostMapping("/content/query")
    @ResponseBody
    public Response<QueryContentResult> queryContent(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "access_token") String accessToken,
            @RequestParam(name = "subject", required = false) String subject,
            @RequestParam(name = "uid", required = true) Long uid,
            @Valid @RequestBody JSONObject request) throws Exception {
        //
        // Step 1, Open api authentication & retrieving operating user's profile
        //
        this.openApiAuthService.authenticate(clientId, accessToken);
        UserProfile operatingUserProfile = this.openApiAuthService.getUserProfile(clientId, subject);

        //
        // Step 2, core-processing
        //

        DataWidgetDto dataWidgetDto = this.exploreDataWidgetService.getDataWidget(
                uid,
                operatingUserProfile);

        if (dataWidgetDto == null) {
            return Response.buildSuccess();
        } else {
            this.openApiAuthService.authorizeDataFacet(dataWidgetDto.getDataFacetUid(), operatingUserProfile);

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
    }

    @Operation(summary = "导出内容")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false),
            @Parameter(name = "uid", description = "Data widget 的 UID", required = false)
    })
    @PostMapping("/content/export")
    @ResponseBody
    public Response<Long> exportContent(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "access_token") String accessToken,
            @RequestParam(name = "subject", required = false) String subject,
            @RequestParam(name = "uid", required = true) Long uid,
            @Valid @RequestBody JSONObject request) throws Exception {
        //
        // Step 1, Open api authentication & retrieving operating user's profile
        //
        this.openApiAuthService.authenticate(clientId, accessToken);
        UserProfile operatingUserProfile = this.openApiAuthService.getUserProfile(clientId, subject);

        //
        // Step 2, core-processing
        //

        DataWidgetDto dataWidgetDto = this.exploreDataWidgetService.getDataWidget(
                uid,
                operatingUserProfile);

        if (dataWidgetDto == null) {
            return Response.buildSuccess();
        } else {
            this.openApiAuthService.authorizeDataFacet(dataWidgetDto.getDataFacetUid(), operatingUserProfile);

            return Response.buildSuccess(
                    this.exploreDataWidgetService.exportContent(
                            uid, request, operatingUserProfile));
        }
    }
}
