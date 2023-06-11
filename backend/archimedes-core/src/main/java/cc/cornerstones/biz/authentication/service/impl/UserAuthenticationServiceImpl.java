package cc.cornerstones.biz.authentication.service.impl;

import cc.cornerstones.almond.constants.InfrastructureConstants;
import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.exceptions.AbcAuthenticationException;
import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcJsonUtils;
import cc.cornerstones.arbutus.pf4j.service.assembly.PluginHelper;
import cc.cornerstones.arbutus.pf4j.share.types.PluginProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.archimedes.extensions.AuthenticationServiceProvider;
import cc.cornerstones.archimedes.extensions.types.SignedInfo;
import cc.cornerstones.biz.administration.serviceconnection.entity.AuthenticationServiceAgentDo;
import cc.cornerstones.biz.administration.serviceconnection.entity.AuthenticationServiceComponentDo;
import cc.cornerstones.biz.administration.serviceconnection.persistence.AuthenticationServiceComponentRepository;
import cc.cornerstones.biz.administration.serviceconnection.persistence.AuthenticationServiceAgentRepository;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DfsServiceAgentService;
import cc.cornerstones.biz.administration.usermanagement.dto.UserAccountDto;
import cc.cornerstones.biz.administration.usermanagement.entity.UserAccountDo;
import cc.cornerstones.biz.administration.usermanagement.entity.UserBasicDo;
import cc.cornerstones.biz.administration.usermanagement.entity.UserExtendedPropertyDo;
import cc.cornerstones.biz.administration.usermanagement.persistence.UserAccountRepository;
import cc.cornerstones.biz.administration.usermanagement.persistence.UserBasicRepository;
import cc.cornerstones.biz.administration.usermanagement.persistence.UserExtendedPropertyRepository;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserSchemaService;
import cc.cornerstones.biz.authentication.dto.UserAuthenticationInstanceDto;
import cc.cornerstones.biz.authentication.entity.UserAuthenticationInstanceDo;
import cc.cornerstones.biz.authentication.persistence.UserAuthenticationInstanceRepository;
import cc.cornerstones.biz.authentication.service.assembly.AuthenticationHandler;
import cc.cornerstones.biz.authentication.service.inf.UserAuthenticationService;
import com.alibaba.fastjson.JSONObject;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserAuthenticationServiceImpl implements UserAuthenticationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserAuthenticationServiceImpl.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private UserAuthenticationInstanceRepository userAuthenticationInstanceRepository;

    @Autowired
    private UserBasicRepository userBasicRepository;

    @Autowired
    private AuthenticationHandler authenticationHandler;

    /**
     * Access Token 的过期时长（按小时计量）
     */
    @Value("${private.auth.jwt.expires-in-hours}")
    private Integer expiresInHours;

    @Autowired
    private AuthenticationServiceAgentRepository authenticationServiceAgentRepository;

    @Autowired
    private AuthenticationServiceComponentRepository authenticationServiceComponentRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PluginHelper pluginHelper;

    @Autowired
    private UserExtendedPropertyRepository userExtendedPropertyRepository;

    @Autowired
    private UserSchemaService userSchemaService;

    @Autowired
    private DfsServiceAgentService dfsServiceAgentService;

    @Override
    public UserAccountDto signIn(
            Long authenticationServiceAgentUid,
            JSONObject signInDto) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        AuthenticationServiceAgentDo authenticationServiceAgentDo =
                this.authenticationServiceAgentRepository.findByUid(authenticationServiceAgentUid);
        if (authenticationServiceAgentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    AuthenticationServiceAgentDo.RESOURCE_SYMBOL, authenticationServiceAgentUid));
        }
        Long authenticationServiceComponentUid = authenticationServiceAgentDo.getServiceComponentUid();
        AuthenticationServiceComponentDo authenticationServiceComponentDo =
                this.authenticationServiceComponentRepository.findByUid(authenticationServiceComponentUid);
        if (authenticationServiceComponentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    AuthenticationServiceComponentDo.RESOURCE_SYMBOL, authenticationServiceComponentUid));
        }
        String configuration = authenticationServiceAgentDo.getConfiguration();

        //
        // Step 2, core-processing
        //

        switch (authenticationServiceComponentDo.getType()) {
            case BUILTIN: {
                String entryClassName = authenticationServiceComponentDo.getEntryClassName();
                AuthenticationServiceProvider authenticationServiceProvider = null;
                Map<String, AuthenticationServiceProvider> candidateAuthenticationServiceProviderMap =
                        this.applicationContext.getBeansOfType(AuthenticationServiceProvider.class);
                if (!CollectionUtils.isEmpty(candidateAuthenticationServiceProviderMap)) {
                    for (AuthenticationServiceProvider candidateAuthenticationServiceProvider : candidateAuthenticationServiceProviderMap.values()) {
                        if (candidateAuthenticationServiceProvider.getClass().getName().equals(entryClassName)) {
                            authenticationServiceProvider = candidateAuthenticationServiceProvider;
                            break;
                        }
                    }
                }
                if (authenticationServiceProvider == null) {
                    throw new AbcResourceConflictException(String.format("cannot find authentication service " +
                                    "provider:%s",
                            authenticationServiceComponentDo.getName()));
                }

                try {
                    SignedInfo signedInfo = authenticationServiceProvider.signIn(signInDto, configuration);

                    // 找出 user account
                    UserAccountDo userAccountDo = null;
                    if (signedInfo.getAccountTypeUid().equals(InfrastructureConstants.ROOT_USER_ACCOUNT_TYPE_UID)) {
                        userAccountDo = new UserAccountDo();
                        userAccountDo.setUserUid(InfrastructureConstants.ROOT_USER_UID);
                        userAccountDo.setAccountTypeUid(InfrastructureConstants.ROOT_USER_ACCOUNT_TYPE_UID);
                        userAccountDo.setName(InfrastructureConstants.ROOT_USER_DISPLAY_NAME);
                    } else {
                        userAccountDo = this.userAccountRepository.findByAccountTypeUidAndName(
                                signedInfo.getAccountTypeUid(),
                                signedInfo.getAccountName());
                    }
                    if (userAccountDo == null) {
                        throw new AbcResourceNotFoundException("The account name does not exist or the password is incorrect");
                    }
                    UserAccountDto userAccountDto = new UserAccountDto();
                    BeanUtils.copyProperties(userAccountDo, userAccountDto);
                    return userAccountDto;
                } catch (Exception e) {
                    LOGGER.error("failed to sign in {} with authentication service provider {}", signInDto,
                            authenticationServiceComponentDo.getName(), e);
                    throw new AbcResourceConflictException("the account name does not exist or the" +
                            " password is incorrect");
                }
            }
            case PLUGIN: {
                PluginProfile pluginProfile = authenticationServiceComponentDo.getBackEndComponentMetadata();
                if (pluginProfile == null || ObjectUtils.isEmpty(pluginProfile.getPluginId())) {
                    throw new AbcResourceConflictException("illegal plugin");
                }
                try {
                    this.pluginHelper.ensureStartPluginIdentifiedByPluginId(pluginProfile.getPluginId());
                } catch (Exception e) {
                    File pluginFile = this.dfsServiceAgentService.downloadFile(
                            authenticationServiceComponentDo.getDfsServiceAgentUidOfBackEndComponentFileId(),
                            authenticationServiceComponentDo.getBackEndComponentFileId(),
                            null);
                    try {
                        this.pluginHelper.ensureStartPluginIdentifiedByPath(pluginFile.getAbsolutePath());
                    } catch (Exception e3) {
                        LOGGER.error("failed to ensure start plugin:{}", pluginProfile.getPluginId(), e3);
                        throw new AbcResourceConflictException("failed to load plugin");
                    }
                }

                List<AuthenticationServiceProvider> listOfProcessors =
                        this.pluginHelper.getPluginManager().getExtensions(
                                AuthenticationServiceProvider.class,
                                pluginProfile.getPluginId());
                if (listOfProcessors == null || listOfProcessors.isEmpty()) {
                    this.pluginHelper.getPluginManager().unloadPlugin(pluginProfile.getPluginId());
                    throw new AbcUndefinedException("cannot find AuthenticationServiceProvider");
                }

                if (listOfProcessors.size() > 1) {
                    this.pluginHelper.getPluginManager().unloadPlugin(pluginProfile.getPluginId());
                    throw new AbcUndefinedException("found " + listOfProcessors.size() + " AuthenticationServiceProvider");
                }

                AuthenticationServiceProvider authenticationServiceProvider = listOfProcessors.get(0);
                try {
                    SignedInfo signedInfo = authenticationServiceProvider.signIn(signInDto, configuration);

                    // 找出 user account
                    UserAccountDo userAccountDo = null;

                    for (Long accountTypeUid : authenticationServiceAgentDo.getAccountTypeUidList()) {
                        userAccountDo = this.userAccountRepository.findByAccountTypeUidAndName(
                                accountTypeUid,
                                signedInfo.getAccountName());
                        if (userAccountDo != null) {
                            break;
                        }
                    }

                    if (userAccountDo == null) {
                        LOGGER.error("cannot find user by account::account_type_uid:{}, account_name:{}",
                                authenticationServiceAgentDo.getAccountTypeUidList(), signedInfo.getAccountName());
                        throw new AbcResourceNotFoundException("The account does not exist");
                    }

                    // 补充 user info
                    if (signedInfo.getUserInfo() != null) {
                        if (!CollectionUtils.isEmpty(authenticationServiceAgentDo.getProperties())) {
                            List<UserExtendedPropertyDo> userExtendedPropertyDoList =
                                    this.userExtendedPropertyRepository.findByUserUid(userAccountDo.getUserUid());
                            if (!CollectionUtils.isEmpty(userExtendedPropertyDoList)) {
                                Map<Long, UserExtendedPropertyDo> userExtendedPropertyDoMap = new HashMap<>();
                                userExtendedPropertyDoList.forEach(userExtendedPropertyDo -> {
                                    userExtendedPropertyDoMap.put(userExtendedPropertyDo.getExtendedPropertyUid(),
                                            userExtendedPropertyDo);
                                });

                                List<UserExtendedPropertyDo> toAddItemDoList = new LinkedList<>();
                                List<UserExtendedPropertyDo> toUpdateItemDoList = new LinkedList<>();

                                authenticationServiceAgentDo.getProperties().forEach(property -> {
                                    Long extendedPropertyUid = property.f;
                                    String objectiveFieldPath = property.s;

                                    String[] slices = objectiveFieldPath.split(".");
                                    Object objectiveFieldValue = AbcJsonUtils.recurseFindField(signedInfo.getUserInfo(),
                                            slices);
                                    if (objectiveFieldValue != null) {
                                        String newExtendedPropertyValue =
                                                this.userSchemaService.transformExtendedPropertyValueFromObjectToString(objectiveFieldValue, extendedPropertyUid);

                                        if (userExtendedPropertyDoMap.containsKey(extendedPropertyUid)) {
                                            UserExtendedPropertyDo existingUserExtendedPropertyDo =
                                                    userExtendedPropertyDoMap.get(extendedPropertyUid);
                                            String existingExtendedPropertyValue =
                                                    existingUserExtendedPropertyDo.getExtendedPropertyValue();

                                            if (!newExtendedPropertyValue.equals(existingExtendedPropertyValue)) {
                                                // extended property value updated
                                                existingUserExtendedPropertyDo.setExtendedPropertyValue(newExtendedPropertyValue);
                                                BaseDo.update(existingUserExtendedPropertyDo,
                                                        InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
                                                toUpdateItemDoList.add(existingUserExtendedPropertyDo);
                                            }
                                        } else {
                                            // new user extended property
                                            UserExtendedPropertyDo newUserExtendedPropertyDo =
                                                    new UserExtendedPropertyDo();
                                            newUserExtendedPropertyDo.setExtendedPropertyUid(extendedPropertyUid);
                                            newUserExtendedPropertyDo.setExtendedPropertyValue(newExtendedPropertyValue);
                                            BaseDo.create(newUserExtendedPropertyDo,
                                                    InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
                                            toAddItemDoList.add(newUserExtendedPropertyDo);
                                        }
                                    }
                                });

                                if (!CollectionUtils.isEmpty(toAddItemDoList)) {
                                    this.userExtendedPropertyRepository.saveAll(toAddItemDoList);
                                }

                                if (!CollectionUtils.isEmpty(toUpdateItemDoList)) {
                                    this.userExtendedPropertyRepository.saveAll(toUpdateItemDoList);
                                }
                            }
                        }
                    }

                    UserAccountDto userAccountDto = new UserAccountDto();
                    BeanUtils.copyProperties(userAccountDo, userAccountDto);
                    return userAccountDto;
                } catch (Exception e) {
                    LOGGER.error("failed to sign in {} with authentication service provider {}", signInDto,
                            authenticationServiceComponentDo.getName(), e);
                    throw new AbcResourceConflictException("the account name does not exist or the password is " +
                            "incorrect");
                }
            }
            default:
                throw new AbcResourceConflictException(String.format("unsupported service provider type:%s",
                        authenticationServiceComponentDo.getType()));
        }

        //
        // Step 3, post-processing
        //

    }

    @Override
    public void signOut(
            Long authenticationServiceAgentUid,
            Long userUid) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        AuthenticationServiceAgentDo authenticationServiceAgentDo =
                this.authenticationServiceAgentRepository.findByUid(authenticationServiceAgentUid);
        if (authenticationServiceAgentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    AuthenticationServiceAgentDo.RESOURCE_SYMBOL, authenticationServiceAgentUid));
        }
        Long serviceComponentUid = authenticationServiceAgentDo.getServiceComponentUid();
        AuthenticationServiceComponentDo authenticationServiceComponentDo =
                this.authenticationServiceComponentRepository.findByUid(serviceComponentUid);
        if (authenticationServiceComponentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    AuthenticationServiceComponentDo.RESOURCE_SYMBOL, serviceComponentUid));
        }
        String configuration = authenticationServiceAgentDo.getConfiguration();

        //
        // Step 2, core-processing
        //

        switch (authenticationServiceComponentDo.getType()) {
            case BUILTIN: {
                String entryClassName = authenticationServiceComponentDo.getEntryClassName();
                AuthenticationServiceProvider authenticationServiceProvider = null;
                Map<String, AuthenticationServiceProvider> candidateAuthenticationServiceProviderMap =
                        this.applicationContext.getBeansOfType(AuthenticationServiceProvider.class);
                if (!CollectionUtils.isEmpty(candidateAuthenticationServiceProviderMap)) {
                    for (AuthenticationServiceProvider candidateAuthenticationServiceProvider : candidateAuthenticationServiceProviderMap.values()) {
                        if (candidateAuthenticationServiceProvider.getClass().getName().equals(entryClassName)) {
                            authenticationServiceProvider = candidateAuthenticationServiceProvider;
                            break;
                        }
                    }
                }
                if (authenticationServiceProvider == null) {
                    throw new AbcResourceConflictException(String.format("cannot find authentication service " +
                                    "provider:%s",
                            authenticationServiceComponentDo.getName()));
                }

                try {
                    authenticationServiceProvider.signOut(null, configuration);
                } catch (Exception e) {
                    LOGGER.error("failed to sign out {} with authentication service provider {}", null,
                            authenticationServiceComponentDo.getName(), e);
                    throw new AbcResourceConflictException("failed to sign out");
                }
            }
            break;
            case PLUGIN: {
                PluginProfile pluginProfile = authenticationServiceComponentDo.getBackEndComponentMetadata();
                if (pluginProfile == null || ObjectUtils.isEmpty(pluginProfile.getPluginId())) {
                    throw new AbcResourceConflictException("illegal plugin");
                }
                try {
                    this.pluginHelper.ensureStartPluginIdentifiedByPluginId(pluginProfile.getPluginId());
                } catch (Exception e) {
                    File pluginFile = this.dfsServiceAgentService.downloadFile(
                            authenticationServiceComponentDo.getDfsServiceAgentUidOfBackEndComponentFileId(),
                            authenticationServiceComponentDo.getBackEndComponentFileId(),
                            null);
                    try {
                        this.pluginHelper.ensureStartPluginIdentifiedByPath(pluginFile.getAbsolutePath());
                    } catch (Exception e3) {
                        LOGGER.error("failed to ensure start plugin:{}", pluginProfile.getPluginId(), e3);
                        throw new AbcResourceConflictException("failed to load plugin");
                    }
                }

                List<AuthenticationServiceProvider> listOfProcessors =
                        this.pluginHelper.getPluginManager().getExtensions(
                                AuthenticationServiceProvider.class,
                                pluginProfile.getPluginId());
                if (listOfProcessors == null || listOfProcessors.isEmpty()) {
                    this.pluginHelper.getPluginManager().unloadPlugin(pluginProfile.getPluginId());
                    throw new AbcUndefinedException("cannot find AuthenticationServiceProvider");
                }

                if (listOfProcessors.size() > 1) {
                    this.pluginHelper.getPluginManager().unloadPlugin(pluginProfile.getPluginId());
                    throw new AbcUndefinedException("found " + listOfProcessors.size() + " AuthenticationServiceProvider");
                }

                AuthenticationServiceProvider authenticationServiceProvider = listOfProcessors.get(0);
                try {
                    authenticationServiceProvider.signOut(null, configuration);
                } catch (Exception e) {
                    LOGGER.error("failed to sign out {} with authentication service provider {}", null,
                            authenticationServiceComponentDo.getName(), e);
                    throw new AbcResourceConflictException("failed to sign out");
                }
            }
            break;
            default:
                throw new AbcResourceConflictException(String.format("unsupported service provider type:%s",
                        authenticationServiceComponentDo.getType()));
        }

        //
        // Step 3, post-processing
        //

    }

    @Override
    public UserAuthenticationInstanceDto createUserAuthenticationInstance(
            UserAccountDto userAccountDto) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        UserBasicDo userBasicDo = this.userBasicRepository.findByUid(userAccountDto.getUserUid());
        if (userBasicDo == null) {
            throw new AbcResourceNotFoundException(String.format("the user %d does not exist",
                    userAccountDto.getUserUid()));
        }

        if (!Boolean.TRUE.equals(userBasicDo.getEnabled())) {
            throw new AbcResourceConflictException(String.format("the user %d (%s) is disabled", userBasicDo.getUid(),
                    userBasicDo.getDisplayName()));
        }

        //
        // Step 2, core-processing
        //

        //
        // Step 2.1, 将该 user 名下还未过期的 access token 置为过期
        //
        List<UserAuthenticationInstanceDo> userAuthenticationInstanceDoList =
                this.userAuthenticationInstanceRepository.findUnrevokedByUserUid(userBasicDo.getUid());
        if (!CollectionUtils.isEmpty(userAuthenticationInstanceDoList)) {
            userAuthenticationInstanceDoList.forEach(userAuthenticationInstanceDo -> {
                userAuthenticationInstanceDo.setRevoked(Boolean.TRUE);
                BaseDo.update(userAuthenticationInstanceDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
            });
            this.userAuthenticationInstanceRepository.saveAll(userAuthenticationInstanceDoList);
        }

        //
        // Step 2.2, 生成 Access Token & Refresh Token
        //
        String accessToken = null;
        LocalDateTime nowDateTime = LocalDateTime.now();
        LocalDateTime expiresAtDateTime = nowDateTime.plusHours(this.expiresInHours);
        try {
            accessToken = this.authenticationHandler.encodeTokenByJwt(
                    String.valueOf(userBasicDo.getUid()),
                    nowDateTime,
                    expiresAtDateTime);
        } catch (Exception e) {
            LOGGER.error("failed to generate access token", e);
            throw new AbcResourceConflictException(String.format("failed to generate access token for user %d (%s)",
                    userBasicDo.getUid(), userBasicDo.getDisplayName()));
        }

        String refreshToken = Base64.getEncoder().encodeToString(
                UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));

        //
        // Step 2.3, 创建新的user鉴权记录
        //
        UserAuthenticationInstanceDo userAuthenticationInstanceDo = new UserAuthenticationInstanceDo();
        userAuthenticationInstanceDo.setAccessToken(accessToken);
        userAuthenticationInstanceDo.setTokenType(NetworkingConstants.BEARER_TOKEN_TYPE);
        userAuthenticationInstanceDo.setRevoked(false);
        userAuthenticationInstanceDo.setExpiresAtTimestamp(expiresAtDateTime);
        userAuthenticationInstanceDo.setExpiresInSeconds(this.expiresInHours * 60 * 60);
        userAuthenticationInstanceDo.setRefreshToken(refreshToken);
        userAuthenticationInstanceDo.setUid(this.idHelper.getNextDistributedId(UserAuthenticationInstanceDo.RESOURCE_NAME));
        userAuthenticationInstanceDo.setUserUid(userBasicDo.getUid());
        userAuthenticationInstanceDo.setUserDisplayName(userBasicDo.getDisplayName());
        userAuthenticationInstanceDo.setUserAccountTypeUid(userAccountDto.getAccountTypeUid());
        userAuthenticationInstanceDo.setUserAccountName(userAccountDto.getName());
        BaseDo.create(userAuthenticationInstanceDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
        this.userAuthenticationInstanceRepository.save(userAuthenticationInstanceDo);

        //
        // Step 3, post-processing
        //
        UserAuthenticationInstanceDto userAuthenticationInstanceDto = new UserAuthenticationInstanceDto();
        BeanUtils.copyProperties(userAuthenticationInstanceDo, userAuthenticationInstanceDto);

        return userAuthenticationInstanceDto;
    }

    @Override
    public void revokeUserAuthenticationInstance(Long userUid) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        //
        // Step 2, core-processing
        //

        // 将该 user 名下还未过期的 access token 置为过期
        List<UserAuthenticationInstanceDo> userAuthenticationInstanceDoList =
                this.userAuthenticationInstanceRepository.findUnrevokedByUserUid(userUid);
        if (!CollectionUtils.isEmpty(userAuthenticationInstanceDoList)) {
            userAuthenticationInstanceDoList.forEach(userAuthenticationInstanceDo -> {
                userAuthenticationInstanceDo.setRevoked(Boolean.TRUE);
                BaseDo.update(userAuthenticationInstanceDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
            });
            this.userAuthenticationInstanceRepository.saveAll(userAuthenticationInstanceDoList);
        }

        //
        // Step 3, post-processing
        //
    }

    @Override
    public void validateAccessToken(String accessToken) throws AbcUndefinedException {
        try {
            this.authenticationHandler.decodeTokenByJwt(accessToken);
        } catch (Exception e) {
            LOGGER.error("failed to decode access token {}", accessToken, e);
            throw new AbcAuthenticationException("illegal access token");
        }
    }

    @Override
    public void validateAccessToken(String accessToken, Long userUid) throws AbcUndefinedException {
        DecodedJWT decodedJwt;
        try {
            decodedJwt = this.authenticationHandler.decodeTokenByJwt(accessToken);
        } catch (Exception e) {
            LOGGER.error("failed to decode access token {}", accessToken, e);
            throw new AbcAuthenticationException("illegal access token");
        }
        String userUidAsString = String.valueOf(userUid);
        if (!userUidAsString.equals(decodedJwt.getSubject())) {
            LOGGER.error("the subject in the access token {} is {}, but requires {}", accessToken,
                    decodedJwt.getSubject(), userUidAsString);
            throw new AbcAuthenticationException("illegal access token");
        }
    }

    @Override
    public Long validateAccessTokenAndExtractUserUid(String accessToken) throws AbcUndefinedException {
        DecodedJWT decodedJwt;
        try {
            decodedJwt = this.authenticationHandler.decodeTokenByJwt(accessToken);
        } catch (Exception e) {
            LOGGER.error("failed to decode access token {}", accessToken, e);
            throw new AbcAuthenticationException("illegal access token");
        }

        return Long.valueOf(decodedJwt.getSubject());
    }

    @Override
    public UserProfile validateAccessTokenAndRetrieveUserProfile(String accessToken) throws AbcUndefinedException {
        Long userUid = validateAccessTokenAndExtractUserUid(accessToken);

        UserBasicDo userBasicDo = this.userBasicRepository.findByUid(userUid);
        if (userBasicDo == null) {
            throw new AbcResourceNotFoundException(String.format("the user %d does not exist",
                    userUid));
        }

        UserProfile userProfile = new UserProfile();
        userProfile.setUid(userUid);
        userProfile.setDisplayName(userBasicDo.getDisplayName());
        userProfile.setCreatedTimestamp(userBasicDo.getCreatedTimestamp());
        userProfile.setLastModifiedTimestamp(userBasicDo.getLastModifiedTimestamp());

        return userProfile;
    }
}
