package cc.cornerstones.biz.authentication.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.biz.administration.usermanagement.dto.UserAccountDto;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.authentication.dto.UserSignedInDto;
import cc.cornerstones.biz.authentication.dto.UserAuthenticationInstanceDto;
import cc.cornerstones.biz.authentication.service.inf.UserAuthenticationService;
import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Tag(name = "[Authentication] User / Sign")
@RestController
@RequestMapping(value = "/authn")
public class UserSignApi {
    @Autowired
    private UserService userService;

    @Autowired
    private UserAuthenticationService userAuthenticationService;

    @Operation(summary = "Sign in")
    @PostMapping("/sign-in")
    @ResponseBody
    public Response<UserSignedInDto> signIn(
            @RequestParam(name = "uid", required = true) Long authenticationServiceAgentUid,
            @Valid @RequestBody JSONObject signInDto) throws Exception {
        //
        UserAccountDto userAccountDto =
                this.userAuthenticationService.signIn(authenticationServiceAgentUid, signInDto);

        // 记录本次登录和颁发访问凭证
        UserAuthenticationInstanceDto userAuthenticationInstanceDto =
                this.userAuthenticationService.createUserAuthenticationInstance(userAccountDto);

        // 构造返回结果
        UserSignedInDto userSignedInDto = new UserSignedInDto();
        userSignedInDto.setAccessToken(userAuthenticationInstanceDto.getAccessToken());
        userSignedInDto.setTokenType(userAuthenticationInstanceDto.getTokenType());
        userSignedInDto.setExpiresAtTimestamp(userAuthenticationInstanceDto.getExpiresAtTimestamp());
        userSignedInDto.setExpiresInSeconds(userAuthenticationInstanceDto.getExpiresInSeconds());
        userSignedInDto.setCreatedTimestamp(userAuthenticationInstanceDto.getCreatedTimestamp());
        userSignedInDto.setUserUid(userAuthenticationInstanceDto.getUserUid());
        userSignedInDto.setUserDisplayName(userAuthenticationInstanceDto.getUserDisplayName());
        userSignedInDto.setUserAccountTypeUid(userAuthenticationInstanceDto.getUserAccountTypeUid());
        userSignedInDto.setUserAccountName(userAuthenticationInstanceDto.getUserAccountName());

        return Response.buildSuccess(userSignedInDto);
    }

    @Operation(summary = "Sign out")
    @PostMapping("/sign-out")
    @ResponseBody
    public Response signOut(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long authenticationServiceAgentUid) throws Exception {
        this.userAuthenticationService.signOut(authenticationServiceAgentUid, operatingUserUid);

        this.userAuthenticationService.revokeUserAuthenticationInstance(operatingUserUid);

        return Response.buildSuccess();
    }

}
