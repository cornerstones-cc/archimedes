package cc.cornerstones.biz.openapi.api;

import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.authentication.service.inf.OpenApiAuthService;
import cc.cornerstones.biz.datawidget.dto.DataWidgetDto;
import cc.cornerstones.biz.datawidget.service.inf.DataWidgetService;
import cc.cornerstones.biz.serve.dto.CreateSessionDto;
import cc.cornerstones.biz.serve.dto.SessionDto;
import cc.cornerstones.biz.serve.service.inf.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "[Open API] Explore / Sessions")
@RestController
@RequestMapping(value = "/open-api/explore/sessions")
public class OpenApiExploreSessionApi {
    @Autowired
    private OpenApiAuthService openApiAuthService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private DataWidgetService dataWidgetService;

    @Operation(summary = "为指定 Data widget 创建 Session")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false),
            @Parameter(name = "data_widget_uid", description = "Data widget 的 UID", required = true)
    })
    @PostMapping("")
    @ResponseBody
    public Response<SessionDto> createSessionForDataWidget(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "access_token") String accessToken,
            @RequestParam(name = "subject", required = false) String subject,
            @RequestParam(name = "data_widget_uid", required = true) Long dataWidgetUid,
            @Valid @RequestBody CreateSessionDto createSessionDto) throws Exception {
        //
        // Step 1, Open api authentication & retrieving operating user's profile
        //
        this.openApiAuthService.authenticate(clientId, accessToken);
        UserProfile operatingUserProfile = this.openApiAuthService.getUserProfile(clientId, subject);

        //
        // Step 2, core-processing
        //

        DataWidgetDto dataWidgetDto = this.dataWidgetService.getDataWidget(dataWidgetUid, operatingUserProfile);
        if (dataWidgetDto == null) {
            return Response.buildFailure("cannot find data widget");
        } else {
            this.openApiAuthService.authorizeDataFacet(dataWidgetDto.getDataFacetUid(), operatingUserProfile);

            return Response.buildSuccess(
                    this.sessionService.createSessionForDataWidget(
                            dataWidgetUid, createSessionDto,
                            operatingUserProfile));
        }
    }

    @Operation(summary = "删除当前登录用户的指定 Session")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false),
            @Parameter(name = "uid", description = "Session 的 UID", required = true)
    })
    @DeleteMapping("")
    @ResponseBody
    public Response deleteMySessionOfDataWidget(
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

        this.sessionService.deleteMySessionOfDataWidget(
                uid,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "列表查询当前登录用户自己针对指定 Data widget 的 Sessions")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false),
            @Parameter(name = "data_widget_uid", description = "Data widget 的 UID", required = true),
            @Parameter(name = "uid", description = "Session 的 UID", required = false),
            @Parameter(name = "name", description = "Session 的 UID", required = false),
            @Parameter(name = "last_modified_timestamp", description = "Session 的 Last modified timestamp", required =
                    false)
    })
    @GetMapping("/listing-query")
    @ResponseBody
    public Response<List<SessionDto>> listingQueryMySessionsOfDataWidget(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "access_token") String accessToken,
            @RequestParam(name = "subject", required = false) String subject,
            @RequestParam(name = "data_widget_uid", required = true) Long dataWidgetUid,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "last_modified_timestamp", required = false) List<String> lastModifiedTimestampAsStringList,
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

        DataWidgetDto dataWidgetDto = this.dataWidgetService.getDataWidget(dataWidgetUid, operatingUserProfile);
        if (dataWidgetDto == null) {
            return Response.buildFailure("cannot find data widget");
        } else {
            this.openApiAuthService.authorizeDataFacet(dataWidgetDto.getDataFacetUid(), operatingUserProfile);

            return Response.buildSuccess(
                    this.sessionService.listingQuerySessionsOfDataWidget(
                            operatingUserProfile.getUid(),
                            dataWidgetUid,
                            uid, name, lastModifiedTimestampAsStringList,
                            sort,
                            operatingUserProfile));
        }
    }
}
