package cc.cornerstones.biz.administration.systemsettings.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.systemsettings.dto.*;
import cc.cornerstones.biz.administration.systemsettings.service.inf.SystemSettingsService;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Tag(name = "[Biz] Admin / System settings")
@RestController
@RequestMapping(value = "/admin/system-settings")
public class SystemSettingsApi {
    @Autowired
    private UserService userService;

    @Autowired
    private SystemSettingsService systemSettingsService;

    @Operation(summary = "创建 System release")
    @PostMapping("/system-releases")
    @ResponseBody
    public Response<SystemReleaseDto> createSystemRelease(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @Valid @RequestBody CreateSystemReleaseDto createSystemReleaseDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.systemSettingsService.createSystemRelease(
                        createSystemReleaseDto, operatingUserProfile));
    }

    @Operation(summary = "获取最新的 release")
    @GetMapping("/system-releases/latest")
    @ResponseBody
    public Response<SystemReleaseDto> getLatestSystemRelease(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.systemSettingsService.getLatestSystemRelease(
                        operatingUserProfile));
    }

    @Operation(summary = "创建或替换 Archive & Cleanup settings")
    @PutMapping("/archive-cleanup")
    @ResponseBody
    public Response<ArchiveCleanupSettingsDto> createOrReplaceArchiveCleanupSettings(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @Valid @RequestBody CreateOrReplaceArchiveCleanupSettingsDto createOrReplaceArchiveCleanupSettingsDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (createOrReplaceArchiveCleanupSettingsDto.getEnabledCleanup() == null) {
            createOrReplaceArchiveCleanupSettingsDto.setEnabledCleanup(Boolean.FALSE);
        }

        return Response.buildSuccess(
                this.systemSettingsService.createOrReplaceArchiveCleanupSettings(
                        createOrReplaceArchiveCleanupSettingsDto, operatingUserProfile));
    }

    @Operation(summary = "获取 Archive & Cleanup settings")
    @GetMapping("/archive-cleanup")
    @ResponseBody
    public Response<ArchiveCleanupSettingsDto> getArchiveCleanupSettings(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.systemSettingsService.getArchiveCleanupSettings(operatingUserProfile));
    }

    @Operation(summary = "创建或替换 Capacity control settings")
    @PutMapping("/capacity-control")
    @ResponseBody
    public Response<CapacityControlSettingsDto> createOrReplaceCapacityControlSettings(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @Valid @RequestBody CreateOrReplaceCapacityControlSettingsDto createOrReplaceCapacityControlSettingsDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.systemSettingsService.createOrReplaceCapacityControlSettings(
                        createOrReplaceCapacityControlSettingsDto, operatingUserProfile));
    }

    @Operation(summary = "获取 Capacity control settings")
    @GetMapping("/capacity-control")
    @ResponseBody
    public Response<CapacityControlSettingsDto> getCapacityControlSettings(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.systemSettingsService.getCapacityControlSettings(operatingUserProfile));
    }
}
