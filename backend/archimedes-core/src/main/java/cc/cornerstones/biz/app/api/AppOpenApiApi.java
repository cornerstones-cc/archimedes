package cc.cornerstones.biz.app.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.app.dto.*;
import cc.cornerstones.biz.app.service.inf.AppOpenApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Tag(name = "[Biz] Build / App / Open API")
@RestController
@RequestMapping(value = "/build/apps/open-api")
public class AppOpenApiApi {
    @Autowired
    private UserService userService;
    
    @Autowired
    private AppOpenApiService appOpenApiService;
    
    @Operation(summary = "为指定 App 的 Open API 获取 Credential")
    @GetMapping("/credential")
    @ResponseBody
    public Response<AppOpenApiCredentialDto> getOpenApiCredentialOfApp(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "app_uid", required = true) Long appUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.appOpenApiService.getOpenApiCredentialOfApp(
                        appUid,
                        operatingUserProfile));
    }

    @Operation(summary = "为指定 App 的 Open API 创建或替换 Credential")
    @PutMapping("/credential")
    @ResponseBody
    public Response<AppOpenApiCredentialDto> createOrReplaceOpenApiCredentialForApp(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "app_uid", required = true) Long appUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.appOpenApiService.createOrReplaceOpenApiCredentialForApp(
                        appUid,
                        operatingUserProfile));
    }

    @Operation(summary = "为指定 App 的 Open API 获取 Account type")
    @GetMapping("/account-type")
    @ResponseBody
    public Response<AppOpenApiAccountTypeDto> getAccountTypesOfApp(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "app_uid", required = true) Long appUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.appOpenApiService.getAccountTypeOfApp(
                        appUid,
                        operatingUserProfile));
    }

    @Operation(summary = "为指定 App 的 Open API 创建或替换 Account type")
    @PutMapping("/account-type")
    @ResponseBody
    public Response<AppOpenApiAccountTypeDto> createOrReplaceAccountTypeForApp(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "app_uid", required = true) Long appUid,
            @Valid @RequestBody CreateOrReplaceAppAccountTypeDto createOrReplaceAppAccountTypeDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.appOpenApiService.createOrReplaceAccountTypeForApp(
                        appUid, createOrReplaceAppAccountTypeDto,
                        operatingUserProfile));
    }
}
