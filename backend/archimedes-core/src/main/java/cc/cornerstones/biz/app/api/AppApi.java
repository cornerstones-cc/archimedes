package cc.cornerstones.biz.app.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.app.dto.AppDto;
import cc.cornerstones.biz.app.dto.CreateAppDto;
import cc.cornerstones.biz.app.dto.UpdateAppDto;
import cc.cornerstones.biz.app.service.inf.AppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.LinkedList;
import java.util.List;

@Tag(name = "[Biz] Build / App")
@RestController
@RequestMapping(value = "/build/apps")
public class AppApi {
    @Autowired
    private UserService userService;

    @Autowired
    private AppService appService;

    @Operation(summary = "创建一个 App")
    @PostMapping("")
    @ResponseBody
    public Response<AppDto> createApp(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @Valid @RequestBody CreateAppDto createAppDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (createAppDto.getSequence() == null) {
            createAppDto.setSequence(0.1f);
        }

        return Response.buildSuccess(
                this.appService.createApp(
                        createAppDto,
                        operatingUserProfile));
    }

    @Operation(summary = "修改指定 App")
    @Parameters(value = {
            @Parameter(name = "uid", description = "App 的 UID", required = true)
    })
    @PatchMapping("")
    @ResponseBody
    public Response updateApp(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid,
            @Valid @RequestBody UpdateAppDto updateAppDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.appService.updateApp(
                uid, updateAppDto,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "列出针对指定 App 的所有引用")
    @Parameters(value = {
            @Parameter(name = "uid", description = "App 的 UID", required = true)
    })
    @GetMapping("/references")
    @ResponseBody
    public Response<List<String>> listAllReferencesToApp(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.appService.listAllReferencesToApp(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "删除指定 App")
    @Parameters(value = {
            @Parameter(name = "uid", description = "App 的 UID", required = true)
    })
    @DeleteMapping("")
    @ResponseBody
    public Response deleteApp(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.appService.deleteApp(
                uid,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "获取指定 App")
    @Parameters(value = {
            @Parameter(name = "uid", description = "App 的 UID", required = true)
    })
    @GetMapping("")
    @ResponseBody
    public Response<AppDto> getApp(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.appService.getApp(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "列表查询 Apps")
    @GetMapping("/listing-query")
    public Response<List<AppDto>> listingQueryApps(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "name", required = false) String name,
            Sort sort) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // sort 特殊处理
        sort = AbcApiUtils.transformSort(sort);

        // 业务逻辑
        return Response.buildSuccess(
                this.appService.listingQueryApps(
                        uid, name, sort,
                        operatingUserProfile));
    }

    @Operation(summary = "分页查询 Apps")
    @GetMapping("/paging-query")
    public Response<Page<AppDto>> pagingQueryApps(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "description", required = false) String description,
            @RequestParam(name = "enabled", required = false) Boolean enabled,
            @RequestParam(name = "last_modified_by", required = false) String lastModifiedBy,
            @RequestParam(name = "last_modified_timestamp", required = false) List<String> lastModifiedTimestampAsStringList,
            Pageable pageable) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // pageable 中的 sort 特殊处理
        pageable = AbcApiUtils.transformPageable(pageable);

        // 查询用户 lastModifiedBy
        List<Long> userUidListOfLastModifiedBy = null;
        if (!ObjectUtils.isEmpty(lastModifiedBy)) {
            userUidListOfLastModifiedBy = this.userService.listingQueryUidOfUsers(
                    lastModifiedBy, operatingUserProfile);
            if (CollectionUtils.isEmpty(userUidListOfLastModifiedBy)) {
                Page<AppDto> result = new PageImpl<>(new LinkedList<>(), pageable, 0);
                return Response.buildSuccess(result);
            }
        }

        // 业务逻辑
        return Response.buildSuccess(
                this.appService.pagingQueryApps(
                        uid, name, description, enabled, userUidListOfLastModifiedBy, lastModifiedTimestampAsStringList,
                        pageable,
                        operatingUserProfile));
    }
}
