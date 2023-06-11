package cc.cornerstones.biz.app.service.impl;

import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.app.dto.AppOpenApiAccountTypeDto;
import cc.cornerstones.biz.app.dto.AppOpenApiCredentialDto;
import cc.cornerstones.biz.app.dto.CreateOrReplaceAppAccountTypeDto;
import cc.cornerstones.biz.app.entity.AppDo;
import cc.cornerstones.biz.app.entity.AppOpenApiCredentialDo;
import cc.cornerstones.biz.app.entity.AppOpenApiSettingsDo;
import cc.cornerstones.biz.app.persistence.AppOpenApiCredentialRepository;
import cc.cornerstones.biz.app.persistence.AppOpenApiSettingsRepository;
import cc.cornerstones.biz.app.persistence.AppRepository;
import cc.cornerstones.biz.app.service.assembly.AppAccessHandler;
import cc.cornerstones.biz.app.service.inf.AppOpenApiService;
import cc.cornerstones.biz.datafacet.dto.AppOpenApiSettingsContentDto;
import cc.cornerstones.biz.share.event.AppDeletedEvent;
import cc.cornerstones.biz.share.event.EventBusManager;
import com.google.common.eventbus.Subscribe;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AppOpenApiServiceImpl implements AppOpenApiService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppOpenApiServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private AppRepository appRepository;

    @Autowired
    private AppOpenApiCredentialRepository appOpenApiCredentialRepository;

    @Autowired
    private AppOpenApiSettingsRepository appOpenApiSettingsRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AppAccessHandler appAccessHandler;

    @Override
    public AppOpenApiCredentialDto getOpenApiCredentialOfApp(
            Long appUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        AppOpenApiCredentialDo appOpenApiCredentialDo = this.appOpenApiCredentialRepository.findByAppUid(appUid);
        if (appOpenApiCredentialDo == null) {
            return null;
        }
        AppOpenApiCredentialDto appOpenApiCredentialDto = new AppOpenApiCredentialDto();
        BeanUtils.copyProperties(appOpenApiCredentialDo, appOpenApiCredentialDto);
        return appOpenApiCredentialDto;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public AppOpenApiCredentialDto createOrReplaceOpenApiCredentialForApp(
            Long appUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        AppDo appDo = this.appRepository.findByUid(appUid);
        if  (appDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", AppDo.RESOURCE_SYMBOL, appUid));
        }

        // verify authorization
        this.appAccessHandler.verifyAdminAuthorization(appUid, operatingUserProfile);

        //
        // Step 2, core-processing
        //
        AppOpenApiCredentialDo appOpenApiCredentialDo = this.appOpenApiCredentialRepository.findByAppUid(appUid);
        if (appOpenApiCredentialDo == null) {
            // create a user (organization type)
            Long userUid = this.userService.createOrganizationUser(appDo.getName(), operatingUserProfile);

            appOpenApiCredentialDo = new AppOpenApiCredentialDo();
            appOpenApiCredentialDo.setAppUid(appUid);
            appOpenApiCredentialDo.setAppKey(Base64.encodeBase64String(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)).toLowerCase());
            appOpenApiCredentialDo.setAppSecret(Base64.encodeBase64String(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)).toLowerCase());
            appOpenApiCredentialDo.setUserUid(userUid);
            BaseDo.create(appOpenApiCredentialDo, operatingUserProfile.getUid(), LocalDateTime.now());
        } else {
            if (appOpenApiCredentialDo.getUserUid() == null) {
                // create a user (organization type)
                Long userUid = this.userService.createOrganizationUser(appDo.getName(), operatingUserProfile);
                appOpenApiCredentialDo.setUserUid(userUid);
            }
            appOpenApiCredentialDo.setAppKey(Base64.encodeBase64String(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)).toLowerCase());
            appOpenApiCredentialDo.setAppSecret(Base64.encodeBase64String(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)).toLowerCase());
            BaseDo.update(appOpenApiCredentialDo, operatingUserProfile.getUid(), LocalDateTime.now());
        }

        this.appOpenApiCredentialRepository.save(appOpenApiCredentialDo);

        //
        // Step 3, post-processing
        //
        AppOpenApiCredentialDto appOpenApiCredentialDto = new AppOpenApiCredentialDto();
        BeanUtils.copyProperties(appOpenApiCredentialDo, appOpenApiCredentialDto);
        return appOpenApiCredentialDto;
    }

    @Override
    public AppOpenApiAccountTypeDto getAccountTypeOfApp(
            Long appUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        AppOpenApiSettingsDo appOpenApiSettingsDo = this.appOpenApiSettingsRepository.findByAppUid(appUid);
        if (appOpenApiSettingsDo == null) {
            return null;
        }

        AppOpenApiAccountTypeDto appOpenApiAccountTypeDto = new AppOpenApiAccountTypeDto();
        appOpenApiAccountTypeDto.setAccountTypeUidList(appOpenApiSettingsDo.getContent().getAccountTypeUidList());
        appOpenApiAccountTypeDto.setAppUid(appUid);
        return appOpenApiAccountTypeDto;
    }

    @Override
    public AppOpenApiAccountTypeDto createOrReplaceAccountTypeForApp(
            Long appUid,
            CreateOrReplaceAppAccountTypeDto createOrReplaceAppAccountTypeDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        // verify authorization
        this.appAccessHandler.verifyAdminAuthorization(appUid, operatingUserProfile);

        AppOpenApiSettingsDo appOpenApiSettingsDo = this.appOpenApiSettingsRepository.findByAppUid(appUid);
        if (appOpenApiSettingsDo == null) {
            appOpenApiSettingsDo = new AppOpenApiSettingsDo();
            appOpenApiSettingsDo.setAppUid(appUid);

            AppOpenApiSettingsContentDto appOpenApiSettingsContentDto = new AppOpenApiSettingsContentDto();
            appOpenApiSettingsContentDto.setAccountTypeUidList(createOrReplaceAppAccountTypeDto.getAccountTypeUidList());
            appOpenApiSettingsDo.setContent(appOpenApiSettingsContentDto);

            BaseDo.create(appOpenApiSettingsDo, operatingUserProfile.getUid(), LocalDateTime.now());
        } else {
            AppOpenApiSettingsContentDto appOpenApiSettingsContentDto = new AppOpenApiSettingsContentDto();
            appOpenApiSettingsContentDto.setAccountTypeUidList(createOrReplaceAppAccountTypeDto.getAccountTypeUidList());
            appOpenApiSettingsDo.setContent(appOpenApiSettingsContentDto);

            BaseDo.update(appOpenApiSettingsDo, operatingUserProfile.getUid(), LocalDateTime.now());
        }

        this.appOpenApiSettingsRepository.save(appOpenApiSettingsDo);

        AppOpenApiAccountTypeDto appOpenApiAccountTypeDto = new AppOpenApiAccountTypeDto();
        appOpenApiAccountTypeDto.setAccountTypeUidList(appOpenApiSettingsDo.getContent().getAccountTypeUidList());
        appOpenApiAccountTypeDto.setAppUid(appUid);
        return appOpenApiAccountTypeDto;
    }

    @Override
    public AppOpenApiAccountTypeDto getAccountTypeOfApp(
            String appKey,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        AppOpenApiCredentialDo appOpenApiCredentialDo = this.appOpenApiCredentialRepository.findByAppKey(appKey);
        if (appOpenApiCredentialDo == null) {
            throw new AbcResourceNotFoundException("app");
        }

        return getAccountTypeOfApp(appOpenApiCredentialDo.getAppUid(), operatingUserProfile);
    }

    /**
     * 在 event bus 中注册成为 subscriber
     */
    @PostConstruct
    public void init() {
        this.eventBusManager.registerSubscriber(this);
    }

    /**
     * event handler
     *
     * @param event
     */
    @Subscribe
    public void handleAppDeletedEvent(AppDeletedEvent event) {
        LOGGER.info("rcv event:{}", event);

        Long appUid = event.getUid();
        UserProfile operatingUserProfile = event.getOperatingUserProfile();

        AppOpenApiSettingsDo appOpenApiSettingsDo = this.appOpenApiSettingsRepository.findByAppUid(appUid);
        if (appOpenApiSettingsDo != null) {
            this.appOpenApiSettingsRepository.delete(appOpenApiSettingsDo);
        }

        AppOpenApiCredentialDo appOpenApiCredentialDo = this.appOpenApiCredentialRepository.findByAppUid(appUid);
        if (appOpenApiCredentialDo != null) {
            this.appOpenApiCredentialRepository.delete(appOpenApiCredentialDo);
        }

    }
}
