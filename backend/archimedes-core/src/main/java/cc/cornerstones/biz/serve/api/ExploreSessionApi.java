package cc.cornerstones.biz.serve.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
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

@Tag(name = "[Biz] Explore / Sessions")
@RestController
@RequestMapping(value = "/explore/sessions")
public class ExploreSessionApi {
    @Autowired
    private UserService userService;

    @Autowired
    private SessionService sessionService;

    @Operation(summary = "为指定 Data widget 创建 Session")
    @Parameters(value = {
            @Parameter(name = "data_widget_uid", description = "Data widget 的 UID", required = true)
    })
    @PostMapping("")
    @ResponseBody
    public Response<SessionDto> createSessionForDataWidget(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_widget_uid", required = true) Long dataWidgetUid,
            @Valid @RequestBody CreateSessionDto createSessionDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.sessionService.createSessionForDataWidget(
                        dataWidgetUid, createSessionDto,
                        operatingUserProfile));
    }

    @Operation(summary = "删除当前登录用户的指定 Session")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Session 的 UID", required = true)
    })
    @DeleteMapping("")
    @ResponseBody
    public Response deleteMySessionOfDataWidget(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.sessionService.deleteMySessionOfDataWidget(
                uid,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "列表查询当前登录用户自己针对指定 Data facet 的 Sessions")
    @Parameters(value = {
            @Parameter(name = "data_widget_uid", description = "Data widget 的 UID", required = true),
            @Parameter(name = "uid", description = "Session 的 UID", required = false),
            @Parameter(name = "name", description = "Session 的 Name", required = false),
            @Parameter(name = "last_modified_timestamp", description = "Session 的 Last modified timestamp", required =
                    false)
    })
    @GetMapping("/listing-query")
    @ResponseBody
    public Response<List<SessionDto>> listingQueryMySessionsOfDataWidget(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_widget_uid", required = true) Long dataWidgetUid,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "last_modified_timestamp", required = false) List<String> lastModifiedTimestampAsStringList,
            Sort sort) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // sort 特殊处理
        sort = AbcApiUtils.transformSort(sort);

        return Response.buildSuccess(
                this.sessionService.listingQuerySessionsOfDataWidget(
                        operatingUserUid,
                        dataWidgetUid,
                        uid, name, lastModifiedTimestampAsStringList,
                        sort,
                        operatingUserProfile));
    }
}
