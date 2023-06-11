package cc.cornerstones.biz.authentication.service.impl;

import cc.cornerstones.almond.constants.InfrastructureConstants;
import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.administration.usermanagement.dto.UserDto;
import cc.cornerstones.biz.administration.usermanagement.dto.UserSimplifiedDto;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.app.dto.AppOpenApiAccountTypeDto;
import cc.cornerstones.biz.app.entity.AppOpenApiCredentialDo;
import cc.cornerstones.biz.app.persistence.AppOpenApiCredentialRepository;
import cc.cornerstones.biz.app.persistence.AppOpenApiSettingsRepository;
import cc.cornerstones.biz.app.service.inf.AppOpenApiService;
import cc.cornerstones.biz.authentication.dto.*;
import cc.cornerstones.biz.authentication.entity.OpenApiAuthenticationInstanceDo;
import cc.cornerstones.biz.authentication.entity.UserAuthenticationInstanceDo;
import cc.cornerstones.biz.authentication.persistence.OpenApiAuthenticationInstanceRepository;
import cc.cornerstones.biz.authentication.service.assembly.AuthenticationHandler;
import cc.cornerstones.biz.authentication.service.inf.OpenApiAuthService;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class OpenApiAuthServiceImpl implements OpenApiAuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserAuthenticationServiceImpl.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private AppOpenApiCredentialRepository appOpenApiCredentialRepository;

    @Autowired
    private AppOpenApiSettingsRepository appOpenApiSettingsRepository;

    @Autowired
    private OpenApiAuthenticationInstanceRepository openApiAuthenticationInstanceRepository;

    @Autowired
    private AuthenticationHandler authenticationHandler;

    /**
     * Access Token 的过期时长（按小时计量）
     */
    @Value("${private.auth.jwt.expires-in-hours}")
    private Integer expiresInHours;

    @Autowired
    private AppOpenApiService appOpenApiService;

    @Autowired
    private UserService userService;

    @Override
    public OpenApiSignedInDto signIn(OpenApiSignInDto signInDto) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        AppOpenApiCredentialDo appOpenApiCredentialDo =
                this.appOpenApiCredentialRepository.findByAppKey(signInDto.getClientId());
        if (appOpenApiCredentialDo == null) {
            throw new AbcResourceNotFoundException(String.format("client id:%s", signInDto.getClientId()));
        }

        Long appUid = appOpenApiCredentialDo.getAppUid();
        String appKey = appOpenApiCredentialDo.getAppKey();
        String appSecret = appOpenApiCredentialDo.getAppSecret();

        //
        // Step 2, core-processing
        //

        //
        // Step 2.1, 验证签名
        //
        verifySignature(appKey, appSecret, signInDto.getSignature());

        //
        // Step 2.2, 将该 app 名下还未过期的 access token 删除
        //
        List<OpenApiAuthenticationInstanceDo> openApiAuthenticationInstanceDoList =
                this.openApiAuthenticationInstanceRepository.findUnrevokedByAppUid(appUid);
        if (!CollectionUtils.isEmpty(openApiAuthenticationInstanceDoList)) {
            openApiAuthenticationInstanceDoList.forEach(openApiAuthenticationInstanceDo -> {
                openApiAuthenticationInstanceDo.setRevoked(true);
                BaseDo.update(openApiAuthenticationInstanceDo, InfrastructureConstants.ROOT_USER_UID,
                        LocalDateTime.now());
            });
            this.openApiAuthenticationInstanceRepository.saveAll(openApiAuthenticationInstanceDoList);
        }

        //
        // Step 2.3, 生成 Access Token & Refresh Token
        //
        String accessToken = null;
        LocalDateTime nowDateTime = LocalDateTime.now();
        LocalDateTime expiresAtDateTime = nowDateTime.plusHours(this.expiresInHours);
        try {
            accessToken = this.authenticationHandler.encodeTokenByJwt(
                    appKey,
                    nowDateTime,
                    expiresAtDateTime);
        } catch (Exception e) {
            LOGGER.error("failed to generate access token for app {} ()", appUid, appKey, e);
            throw new AbcResourceConflictException("failed to sign in");
        }

        String refreshToken = Base64.getEncoder().encodeToString(
                UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));

        //
        // Step 2.3, 创建新的user鉴权记录
        //
        OpenApiAuthenticationInstanceDo openApiAuthenticationInstanceDo = new OpenApiAuthenticationInstanceDo();
        openApiAuthenticationInstanceDo.setAccessToken(accessToken);
        openApiAuthenticationInstanceDo.setTokenType(NetworkingConstants.BEARER_TOKEN_TYPE);
        openApiAuthenticationInstanceDo.setRevoked(false);
        openApiAuthenticationInstanceDo.setExpiresAtTimestamp(expiresAtDateTime);
        openApiAuthenticationInstanceDo.setExpiresInSeconds(this.expiresInHours * 60 * 60);
        openApiAuthenticationInstanceDo.setRefreshToken(refreshToken);
        openApiAuthenticationInstanceDo.setUid(this.idHelper.getNextDistributedId(UserAuthenticationInstanceDo.RESOURCE_NAME));
        openApiAuthenticationInstanceDo.setAppUid(appUid);
        openApiAuthenticationInstanceDo.setAppKey(appKey);
        BaseDo.create(openApiAuthenticationInstanceDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
        this.openApiAuthenticationInstanceRepository.save(openApiAuthenticationInstanceDo);

        //
        // Step 3, post-processing
        //
        OpenApiSignedInDto openApiSignedInDto = new OpenApiSignedInDto();
        openApiSignedInDto.setAccessToken(openApiAuthenticationInstanceDo.getAccessToken());
        openApiSignedInDto.setRefreshToken(openApiAuthenticationInstanceDo.getRefreshToken());
        openApiSignedInDto.setTokenType(openApiAuthenticationInstanceDo.getTokenType());
        openApiSignedInDto.setExpiresAtTimestamp(openApiAuthenticationInstanceDo.getExpiresAtTimestamp());
        openApiSignedInDto.setExpiresInSeconds(openApiAuthenticationInstanceDo.getExpiresInSeconds());
        openApiSignedInDto.setExpiresIn(openApiAuthenticationInstanceDo.getExpiresInSeconds());
        openApiSignedInDto.setCreatedTimestamp(openApiAuthenticationInstanceDo.getCreatedTimestamp());
        return openApiSignedInDto;
    }

    @Override
    public RefreshedAccessTokenDto refreshAccessToken(
            RefreshAccessTokenDto refreshAccessTokenDto) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        AppOpenApiCredentialDo appOpenApiCredentialDo =
                this.appOpenApiCredentialRepository.findByAppKey(refreshAccessTokenDto.getClientId());
        if (appOpenApiCredentialDo == null) {
            throw new AbcResourceNotFoundException(String.format("client id:%s", refreshAccessTokenDto.getClientId()));
        }

        Long appUid = appOpenApiCredentialDo.getAppUid();
        String appKey = appOpenApiCredentialDo.getAppKey();
        String appSecret = appOpenApiCredentialDo.getAppSecret();

        //
        // Step 2, core-processing
        //

        //
        // Step 2.1, 验证签名
        //
        verifySignature(appKey, appSecret, refreshAccessTokenDto.getSignature());

        //
        // Step 2.2, 将该 app 名下还未过期的 access token 删除
        //
        OpenApiAuthenticationInstanceDo openApiAuthenticationInstanceDo =
                this.openApiAuthenticationInstanceRepository.findByAppUidAndRefreshToken(
                        appUid, refreshAccessTokenDto.getRefreshToken());
        if (openApiAuthenticationInstanceDo == null) {
            throw new AbcResourceConflictException("failed to refresh access token");
        }

        if (Boolean.TRUE.equals(openApiAuthenticationInstanceDo.getRevoked())) {
            throw new AbcResourceConflictException("failed to refresh access token");
        }

        //
        // Step 2.3, 生成 Access Token & Refresh Token
        //
        String accessToken = null;
        LocalDateTime nowDateTime = LocalDateTime.now();
        LocalDateTime expiresAtDateTime = nowDateTime.plusHours(this.expiresInHours);
        try {
            accessToken = this.authenticationHandler.encodeTokenByJwt(
                    appKey,
                    nowDateTime,
                    expiresAtDateTime);
        } catch (Exception e) {
            LOGGER.error("failed to generate access token for app {} ()", appUid, appKey, e);
            throw new AbcResourceConflictException("failed to sign in");
        }

        //
        // Step 2.3, 创建新的user鉴权记录
        //
        openApiAuthenticationInstanceDo.setAccessToken(accessToken);
        openApiAuthenticationInstanceDo.setExpiresAtTimestamp(expiresAtDateTime);
        openApiAuthenticationInstanceDo.setExpiresInSeconds(this.expiresInHours * 60 * 60);
        BaseDo.update(openApiAuthenticationInstanceDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
        this.openApiAuthenticationInstanceRepository.save(openApiAuthenticationInstanceDo);

        //
        // Step 3, post-processing
        //
        RefreshedAccessTokenDto refreshedAccessTokenDto = new RefreshedAccessTokenDto();
        refreshedAccessTokenDto.setAccessToken(accessToken);
        refreshedAccessTokenDto.setExpiresAtTimestamp(openApiAuthenticationInstanceDo.getExpiresAtTimestamp());
        refreshedAccessTokenDto.setExpiresInSeconds(openApiAuthenticationInstanceDo.getExpiresInSeconds());
        refreshedAccessTokenDto.setExpiresIn(openApiAuthenticationInstanceDo.getExpiresInSeconds());

        return refreshedAccessTokenDto;
    }

    @Override
    public void authenticate(
            String clientId,
            String accessToken) throws AbcUndefinedException {
        DecodedJWT decodedJwt;
        try {
            decodedJwt = this.authenticationHandler.decodeTokenByJwt(accessToken);
        } catch (Exception e) {
            LOGGER.error("failed to decode token by JWT for access token {}", accessToken, e);
            throw new AbcResourceConflictException("illegal access token");
        }
        if (!clientId.equalsIgnoreCase(decodedJwt.getSubject())) {
            LOGGER.error("the decoded token by JWT for {} is NOT for subject {}", accessToken, clientId);
            throw new AbcResourceConflictException("illegal access token");
        }
    }

    @Override
    public UserProfile getUserProfile(
            String clientId,
            String subject) throws AbcUndefinedException {
        UserProfile operatingUserProfile = new UserProfile();
        operatingUserProfile.setUid(InfrastructureConstants.ROOT_USER_UID);
        operatingUserProfile.setDisplayName(InfrastructureConstants.ROOT_USER_DISPLAY_NAME);

        AppOpenApiCredentialDo appOpenApiCredentialDo = this.appOpenApiCredentialRepository.findByAppKey(clientId);
        if (appOpenApiCredentialDo == null) {
            throw new AbcResourceNotFoundException("open api access to app");
        }
        Long appUid = appOpenApiCredentialDo.getAppUid();

        UserProfile result = new UserProfile();

        result.setOpenApi(Boolean.TRUE);
        result.setAppUid(appUid);
        result.setTrackingSerialNumber(UUID.randomUUID().toString());
        result.setCreatedTimestamp(LocalDateTime.now());

        // 先设置 user (organization) uid
        result.setUid(appOpenApiCredentialDo.getUserUid());
        result.setAppUserUid(appOpenApiCredentialDo.getUserUid());

        // 再根据该 app 有没有在本系统登记帐号类型，以及 subject 有没有对应的 user (personal) account
        // 找到就替换 uid
        if (!ObjectUtils.isEmpty(subject)) {
            AppOpenApiAccountTypeDto appOpenApiAccountTypeDto =
                    this.appOpenApiService.getAccountTypeOfApp(clientId, operatingUserProfile);
            if (appOpenApiAccountTypeDto != null) {
                boolean foundUser = false;

                List<Long> accountTypeUidList = appOpenApiAccountTypeDto.getAccountTypeUidList();
                if (!CollectionUtils.isEmpty(accountTypeUidList)) {
                    for (Long accountTypeUid : accountTypeUidList) {
                        if (accountTypeUid == null) {
                            continue;
                        }

                        UserProfile userProfileOfSubject = this.userService.getUserProfile(accountTypeUid, subject);

                        if (userProfileOfSubject != null && userProfileOfSubject.getUid() != null) {
                            result.setUid(userProfileOfSubject.getUid());
                            result.setDisplayName(userProfileOfSubject.getDisplayName());

                            foundUser = true;

                            break;
                        }
                    }
                }

                if (!foundUser) {
                    LOGGER.error("cannot find user by subject {} and app uid {}", subject, appUid);
                }
            }
        }

        return result;
    }

    private void verifySignature(
            String appKey,
            String appSecret,
            String signature) throws AbcUndefinedException {
        String toHashText = appKey + appSecret;
        String hashedText = org.apache.commons.codec.digest.DigestUtils.sha256Hex(toHashText);
        if (!signature.equals(hashedText)) {
            LOGGER.warn("illegal signature {}, should be {}", signature, hashedText);
            throw new AbcResourceConflictException("illegal signature");
        }
    }

    @Override
    public void authorizeDataFacet(
            Long dataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {

    }

    @Override
    public UserDto getUser(String clientId, String subject) throws AbcUndefinedException {
        UserProfile operatingUserProfile = new UserProfile();
        operatingUserProfile.setUid(InfrastructureConstants.ROOT_USER_UID);
        operatingUserProfile.setDisplayName(InfrastructureConstants.ROOT_USER_DISPLAY_NAME);

        AppOpenApiAccountTypeDto appOpenApiAccountTypeDto =
                this.appOpenApiService.getAccountTypeOfApp(clientId, operatingUserProfile);
        if (appOpenApiAccountTypeDto != null) {
            List<Long> accountTypeUidList = appOpenApiAccountTypeDto.getAccountTypeUidList();
            if (!CollectionUtils.isEmpty(accountTypeUidList)) {
                for (Long accountTypeUid : accountTypeUidList) {
                    if (accountTypeUid == null) {
                        continue;
                    }

                    UserDto userDto = this.userService.getUser(accountTypeUid, subject);
                    if (userDto != null) {
                        return userDto;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public UserSimplifiedDto getUserSimple(
            String clientId,
            String subject) throws AbcUndefinedException {
        UserProfile operatingUserProfile = new UserProfile();
        operatingUserProfile.setUid(InfrastructureConstants.ROOT_USER_UID);
        operatingUserProfile.setDisplayName(InfrastructureConstants.ROOT_USER_DISPLAY_NAME);

        AppOpenApiAccountTypeDto appOpenApiAccountTypeDto =
                this.appOpenApiService.getAccountTypeOfApp(clientId, operatingUserProfile);
        if (appOpenApiAccountTypeDto != null) {
            List<Long> accountTypeUidList = appOpenApiAccountTypeDto.getAccountTypeUidList();
            if (!CollectionUtils.isEmpty(accountTypeUidList)) {
                for (Long accountTypeUid : accountTypeUidList) {
                    if (accountTypeUid == null) {
                        continue;
                    }

                    UserSimplifiedDto userSimplifiedDto = this.userService.getUserSimplified(accountTypeUid, subject);
                    if (userSimplifiedDto != null) {
                        return userSimplifiedDto;
                    }
                }
            }
        }

        return null;
    }

    public static void main(String[] args) {
        generateSignature("zgywmgfmzjutmtm2ms00ogjlltg3mmmtmtblzde5ytfmotzh", "y2jiowezyjutmmu5zs00nzu1lwe5mjitnddjywnim2fjzgqw");
    }

    public static void generateSignature(
            String appKey,
            String appSecret) throws AbcUndefinedException {
        String toHashText = appKey + appSecret;
        String hashedText = org.apache.commons.codec.digest.DigestUtils.sha256Hex(toHashText);
        System.out.println(hashedText);
    }
}
