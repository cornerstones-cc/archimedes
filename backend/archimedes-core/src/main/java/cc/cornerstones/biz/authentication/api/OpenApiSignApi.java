package cc.cornerstones.biz.authentication.api;

import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.usermanagement.dto.UserSimplifiedDto;
import cc.cornerstones.biz.authentication.dto.*;
import cc.cornerstones.biz.authentication.service.inf.OpenApiAuthService;
import com.google.common.base.Strings;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "[Authentication] Open API / Sign")
@RestController
@RequestMapping(value = "/open-api")
public class OpenApiSignApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenApiSignApi.class);

    @Autowired
    private OpenApiAuthService openApiAuthService;

    @Operation(summary = "Sign in")
    @PostMapping("/authn/sign-in")
    @ResponseBody
    public Response<OpenApiSignedInDto> signIn(
            @RequestBody OpenApiSignInDto signInDto) throws Exception {
        if (ObjectUtils.isEmpty(signInDto.getClientId())) {
            throw new AbcIllegalParameterException("client_id is required");
        }
        if (ObjectUtils.isEmpty(signInDto.getGrantType())) {
            throw new AbcIllegalParameterException("grant_type is required");
        }
        if (!AbcApiUtils.OAUTH_GRANT_TYPE_CLIENT_CREDENTIALS.equalsIgnoreCase(signInDto.getGrantType())) {
            throw new AbcIllegalParameterException("grant_type should be "
                    + AbcApiUtils.OAUTH_GRANT_TYPE_CLIENT_CREDENTIALS);
        }
        if (ObjectUtils.isEmpty(signInDto.getSignature())) {
            throw new AbcIllegalParameterException("signature is required");
        }

        return Response.buildSuccess(
                this.openApiAuthService.signIn(signInDto));
    }

    @ApiOperation("刷新 Access token")
    @PutMapping("/authn/refresh")
    @ResponseBody
    public Response<RefreshedAccessTokenDto> refresh(
            @RequestBody RefreshAccessTokenDto refreshAccessTokenDto) throws Exception {
        if (Strings.isNullOrEmpty(refreshAccessTokenDto.getClientId())) {
            throw new AbcIllegalParameterException("client_id shout is required");
        }
        if (Strings.isNullOrEmpty(refreshAccessTokenDto.getRefreshToken())) {
            throw new AbcIllegalParameterException("refresh_token required");
        }
        if (Strings.isNullOrEmpty(refreshAccessTokenDto.getGrantType())) {
            throw new AbcIllegalParameterException("grant_type required");
        }
        if (!AbcApiUtils.OAUTH_GRANT_TYPE_REFRESH_TOKEN.equalsIgnoreCase(refreshAccessTokenDto.getGrantType())) {
            throw new AbcIllegalParameterException("grant_type should be "
                    + AbcApiUtils.OAUTH_GRANT_TYPE_REFRESH_TOKEN);
        }
        if (Strings.isNullOrEmpty(refreshAccessTokenDto.getSignature())) {
            throw new AbcIllegalParameterException("signature is required");
        }

        return Response.buildSuccess(
                this.openApiAuthService.refreshAccessToken(refreshAccessTokenDto));
    }

    @ApiOperation("获取 Subject 的 User profile")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false)
    })
    @GetMapping
            ("/authn/user-profile")
    @ResponseBody
    public Response<UserSimplifiedDto> getUserProfileOfSubject(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "access_token") String accessToken,
            @RequestParam(name = "subject", required = false) String subject) throws Exception {
        this.openApiAuthService.authenticate(clientId, accessToken);

        if (ObjectUtils.isEmpty(subject)) {
            return Response.buildSuccess();
        } else {
            return Response.buildSuccess(
                    this.openApiAuthService.getUserSimple(clientId, subject));
        }
    }

    @Operation(summary = "Sign out")
    @PostMapping("/authn/sign-out")
    @ResponseBody
    public Response signOut() throws Exception {
        return Response.buildSuccess();
    }

    /**
     * Sign in, 适配老版本
     *
     * @param signInDto
     * @return
     * @throws Exception
     */
    @Deprecated
    @Operation(summary = "创建 Access token")
    @GetMapping("/oauth/access_token")
    @ResponseBody
    public Response<OpenApiSignedInDto> createAccessToken(
            @RequestBody OpenApiSignInDto signInDto) throws Exception {
        if (ObjectUtils.isEmpty(signInDto.getClientId())) {
            throw new AbcIllegalParameterException("client_id is required");
        }
        if (ObjectUtils.isEmpty(signInDto.getGrantType())) {
            throw new AbcIllegalParameterException("grant_type is required");
        }
        if (!AbcApiUtils.OAUTH_GRANT_TYPE_CLIENT_CREDENTIALS.equalsIgnoreCase(signInDto.getGrantType())) {
            throw new AbcIllegalParameterException("grant_type should be "
                    + AbcApiUtils.OAUTH_GRANT_TYPE_CLIENT_CREDENTIALS);
        }
        if (ObjectUtils.isEmpty(signInDto.getSignature())) {
            throw new AbcIllegalParameterException("signature is required");
        }

        return Response.buildSuccess(
                this.openApiAuthService.signIn(signInDto));
    }

    /**
     * Refresh, 适配老版本
     *
     * @param refreshAccessTokenDto
     * @return
     * @throws Exception
     */
    @Deprecated
    @ApiOperation("刷新 Access token")
    @PutMapping("/oauth/access_token")
    @ResponseBody
    public Response<RefreshedAccessTokenDto> refreshAccessToken(
            @RequestBody RefreshAccessTokenDto refreshAccessTokenDto) throws Exception {
        if (Strings.isNullOrEmpty(refreshAccessTokenDto.getClientId())) {
            throw new AbcIllegalParameterException("client_id shout is required");
        }
        if (Strings.isNullOrEmpty(refreshAccessTokenDto.getRefreshToken())) {
            throw new AbcIllegalParameterException("refresh_token required");
        }
        if (Strings.isNullOrEmpty(refreshAccessTokenDto.getGrantType())) {
            throw new AbcIllegalParameterException("grant_type required");
        }
        if (!AbcApiUtils.OAUTH_GRANT_TYPE_REFRESH_TOKEN.equalsIgnoreCase(refreshAccessTokenDto.getGrantType())) {
            throw new AbcIllegalParameterException("grant_type should be "
                    + AbcApiUtils.OAUTH_GRANT_TYPE_REFRESH_TOKEN);
        }
        if (Strings.isNullOrEmpty(refreshAccessTokenDto.getSignature())) {
            throw new AbcIllegalParameterException("signature is required");
        }

        return Response.buildSuccess(
                this.openApiAuthService.refreshAccessToken(refreshAccessTokenDto));
    }
}
