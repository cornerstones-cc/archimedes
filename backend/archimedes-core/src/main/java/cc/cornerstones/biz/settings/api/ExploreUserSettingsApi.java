package cc.cornerstones.biz.settings.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.usermanagement.dto.UserDto;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.settings.dto.UpdateUserCredentialDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Tag(name = "[Biz] Settings / User settings")
@RestController
@RequestMapping(value = "/settings/users")
public class ExploreUserSettingsApi {
    @Autowired
    private UserService userService;

    @Operation(summary = "获取当前登录用户的 User profile")
    @GetMapping("/user-profile")
    @ResponseBody
    public Response<UserDto> getUserProfile(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid) throws Exception {
        return Response.buildSuccess(
                this.userService.getUser(operatingUserUid));
    }

    @Operation(summary = "Update credentials")
    @PatchMapping("/user-credentials")
    @ResponseBody
    public Response updateUserCredentials(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @Valid @RequestBody UpdateUserCredentialDto updateUserCredentialDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.userService.updateUserCredentials(updateUserCredentialDto, operatingUserProfile);

        return Response.buildSuccess();
    }
}
