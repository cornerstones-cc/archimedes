package cc.cornerstones.biz.settings.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.TreeNode;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.systemsettings.dto.SystemReleaseDto;
import cc.cornerstones.biz.administration.systemsettings.service.inf.SystemSettingsService;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.settings.dto.SignInOptionDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "[Biz] Settings / System settings")
@RestController
@RequestMapping(value = "/settings/system")
public class ExploreSystemSettingsApi {
    @Autowired
    private UserService userService;

    @Autowired
    private SystemSettingsService systemSettingsService;

    @Operation(summary = "获取 System profile")
    @GetMapping("/system-profile")
    @ResponseBody
    public Response<SystemReleaseDto> getSystemProfile() throws Exception {
        return Response.buildSuccess(
                this.systemSettingsService.getLatestSystemRelease(null));
    }

    @Operation(summary = "列出所有 Sign in options")
    @GetMapping("/sign-in-options")
    @ResponseBody
    public Response<List<SignInOptionDto>> listingAllSignInOptions() throws Exception {
        return Response.buildSuccess(
                this.systemSettingsService.listingQuerySignInOptions());
    }

    @Operation(summary = "列出所有 Navigation menus")
    @GetMapping("/navigation-menus")
    @ResponseBody
    public Response<List<TreeNode>> listingAllNavigationMenus(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.systemSettingsService.listingQueryNavigationMenus(operatingUserProfile));
    }

}
