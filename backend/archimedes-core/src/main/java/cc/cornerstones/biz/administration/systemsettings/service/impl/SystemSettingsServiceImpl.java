package cc.cornerstones.biz.administration.systemsettings.service.impl;

import cc.cornerstones.almond.constants.InfrastructureConstants;
import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.TreeNode;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.administration.serviceconnection.dto.AuthenticationServiceAgentDto;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.AuthenticationServiceAgentService;
import cc.cornerstones.biz.administration.systemsettings.dto.*;
import cc.cornerstones.biz.administration.systemsettings.entity.SettingsDo;
import cc.cornerstones.biz.administration.systemsettings.entity.SystemReleaseDo;
import cc.cornerstones.biz.administration.systemsettings.persistence.SettingsRepository;
import cc.cornerstones.biz.administration.systemsettings.persistence.SystemReleaseRepository;
import cc.cornerstones.biz.administration.systemsettings.service.assembly.Cleaner;
import cc.cornerstones.biz.administration.systemsettings.service.inf.SystemSettingsService;
import cc.cornerstones.biz.administration.usermanagement.dto.UserDetailedDto;
import cc.cornerstones.biz.administration.usermanagement.service.inf.PermissionsService;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.administration.usermanagement.share.types.NavigationMenu;
import cc.cornerstones.biz.administration.usermanagement.share.types.Permissions;
import cc.cornerstones.biz.distributedjob.dto.CreateDistributedJobDto;
import cc.cornerstones.biz.distributedjob.dto.DistributedJobDto;
import cc.cornerstones.biz.distributedjob.dto.UpdateDistributedJobDto;
import cc.cornerstones.biz.distributedjob.service.inf.DistributedJobService;
import cc.cornerstones.biz.distributedjob.share.constants.JobExecutorRoutingAlgorithmEnum;
import cc.cornerstones.biz.settings.dto.SignInOptionDto;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class SystemSettingsServiceImpl implements SystemSettingsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SystemSettingsServiceImpl.class);

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private SystemReleaseRepository systemReleaseRepository;

    @Autowired
    private SettingsRepository settingsRepository;

    @Autowired
    private AuthenticationServiceAgentService authenticationServiceService;

    @Autowired
    private UserService userService;

    @Autowired
    private PermissionsService permissionsService;

    public static final String CLEANUP_JOB_UID =
            "cleanup.job-uid";

    public static final String ENABLED_CLEANUP =
            "cleanup.enabled-cleanup";

    public static final String START_TIME_CRON_EXPRESSION =
            "cleanup.start-time-cron-expression";

    public static final String MAXIMUM_DURATION_IN_MINUTES =
            "cleanup.maximum-duration-in-minutes";

    public static final String MAXIMUM_RETAIN_DAYS_FOR_LOGICAL_DELETE =
            "cleanup.maximum-retain-days-for-logical-delete";
    public static final String MAXIMUM_RETAIN_DAYS_FOR_ACCESS_LOGS =
            "cleanup.maximum-retain-days-for-access-logs";
    public static final String MAXIMUM_RETAIN_DAYS_FOR_EXPORT_LOGS =
            "cleanup.maximum-retain-days-for-export-logs";
    public static final String MAXIMUM_RETAIN_DAYS_FOR_JOB_EXECUTION_LOGS =
            "cleanup.maximum-retain-days-for-job-execution-logs";
    public static final String MAXIMUM_RETAIN_DAYS_FOR_TASK_EXECUTION_LOGS =
            "cleanup.maximum-retain-days-for-task-execution-logs";
    public static final String MAXIMUM_RETAIN_DAYS_FOR_DICTIONARY_BUILD_LOGS =
            "cleanup.maximum-retain-days-for-dictionary-build-logs";

    public static final String MAXIMUM_NUMBER_OF_IMAGES_EXPORTED_BY_ONE_EXPORT_TASK =
            "capacity.maximum-number-of-images-exported-by-one-export-task";
    public static final String MAXIMUM_NUMBER_OF_FILES_EXPORTED_BY_ONE_EXPORT_TASK =
            "capacity.maximum-number-of-files-exported-by-one-export-task";

    @Autowired
    private DistributedJobService distributedJobService;

    @Override
    public SystemReleaseDto createSystemRelease(
            CreateSystemReleaseDto createSystemReleaseDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        //
        // Step 2, core-processing
        //
        SystemReleaseDo systemReleaseDo = new SystemReleaseDo();
        systemReleaseDo.setUid(this.idHelper.getNextDistributedId(SystemReleaseDo.RESOURCE_NAME));
        systemReleaseDo.setSystemName(createSystemReleaseDto.getSystemName());
        systemReleaseDo.setSystemLogoUrl(createSystemReleaseDto.getSystemLogoUrl());
        systemReleaseDo.setBigPictureUrl(createSystemReleaseDto.getBigPictureUrl());
        systemReleaseDo.setReleaseVersion(createSystemReleaseDto.getReleaseVersion());
        systemReleaseDo.setTermsOfServiceUrl(createSystemReleaseDto.getTermsOfServiceUrl());
        systemReleaseDo.setPrivacyPolicyUrl(createSystemReleaseDto.getPrivacyPolicyUrl());
        systemReleaseDo.setVendorName(createSystemReleaseDto.getVendorName());

        BaseDo.create(systemReleaseDo, operatingUserProfile.getUid(), LocalDateTime.now());

        this.systemReleaseRepository.save(systemReleaseDo);

        //
        // Step 3, post-processing
        //
        SystemReleaseDto systemReleaseDto = new SystemReleaseDto();
        BeanUtils.copyProperties(systemReleaseDo, systemReleaseDto);
        return systemReleaseDto;
    }

    @Override
    public SystemReleaseDto getSystemRelease(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        SystemReleaseDo systemReleaseDo = this.systemReleaseRepository.findByUid(uid);
        if (systemReleaseDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", SystemReleaseDo.RESOURCE_SYMBOL, uid));
        }

        //
        // Step 2, core-processing
        //


        //
        // Step 3, post-processing
        //
        SystemReleaseDto systemReleaseDto = new SystemReleaseDto();
        BeanUtils.copyProperties(systemReleaseDo, systemReleaseDto);
        return systemReleaseDto;
    }

    @Override
    public SystemReleaseDto getLatestSystemRelease(
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        //
        // Step 2, core-processing
        //
        PageRequest pageRequest = PageRequest.of(0, 1, Sort.by(Sort.Order.desc(BaseDo.ID_FIELD_NAME)));
        Page<SystemReleaseDo> page = this.systemReleaseRepository.findAll(pageRequest);
        if (page.isEmpty()) {
            return null;
        }
        SystemReleaseDo systemReleaseDo = page.getContent().get(0);

        //
        // Step 3, post-processing
        //
        SystemReleaseDto systemReleaseDto = new SystemReleaseDto();
        BeanUtils.copyProperties(systemReleaseDo, systemReleaseDto);
        return systemReleaseDto;
    }

    @Override
    public ArchiveCleanupSettingsDto createOrReplaceArchiveCleanupSettings(
            CreateOrReplaceArchiveCleanupSettingsDto createOrReplaceArchiveCleanupSettingsDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        if (Boolean.TRUE.equals(createOrReplaceArchiveCleanupSettingsDto.getEnabledCleanup())) {
            if (ObjectUtils.isEmpty(createOrReplaceArchiveCleanupSettingsDto.getStartTimeCronExpression())) {
                throw new AbcIllegalParameterException("start_time_cron_expression should not be null or empty if " +
                        "enabled_cleanup is true");
            }
        }

        if (!ObjectUtils.isEmpty(createOrReplaceArchiveCleanupSettingsDto.getStartTimeCronExpression())) {
            if (!CronExpression.isValidExpression(createOrReplaceArchiveCleanupSettingsDto.getStartTimeCronExpression())) {
                throw new AbcIllegalParameterException("start_time_cron_expression should be legal cron expression");
            }
        }

        if (Boolean.FALSE.equals(createOrReplaceArchiveCleanupSettingsDto.getEnabledCleanup())) {
            if (createOrReplaceArchiveCleanupSettingsDto.getStartTimeCronExpression() != null) {
                createOrReplaceArchiveCleanupSettingsDto.setStartTimeCronExpression(null);
            }

            if (createOrReplaceArchiveCleanupSettingsDto.getMaximumDurationInMinutes() != null) {
                createOrReplaceArchiveCleanupSettingsDto.setMaximumDurationInMinutes(null);
            }
        }

        if (Boolean.TRUE.equals(createOrReplaceArchiveCleanupSettingsDto.getEnabledCleanup())
                && createOrReplaceArchiveCleanupSettingsDto.getMaximumDurationInMinutes() == null) {
            createOrReplaceArchiveCleanupSettingsDto.setMaximumDurationInMinutes(4 * 60);
        }

        //
        // Step 2, core-processing
        //
        SettingsDo enabledCleanupSettingsDo =
                this.settingsRepository.findByName(ENABLED_CLEANUP);
        if (enabledCleanupSettingsDo == null) {
            enabledCleanupSettingsDo = new SettingsDo();
            enabledCleanupSettingsDo.setName(ENABLED_CLEANUP);
            enabledCleanupSettingsDo.setValue(
                    String.valueOf(createOrReplaceArchiveCleanupSettingsDto.getEnabledCleanup()));
            BaseDo.create(enabledCleanupSettingsDo, operatingUserProfile.getUid(), LocalDateTime.now());
        } else {
            enabledCleanupSettingsDo.setValue(
                    String.valueOf(createOrReplaceArchiveCleanupSettingsDto.getEnabledCleanup()));
            BaseDo.update(enabledCleanupSettingsDo, operatingUserProfile.getUid(), LocalDateTime.now());
        }
        this.settingsRepository.save(enabledCleanupSettingsDo);

        //
        SettingsDo startTimeCronExpressionSettingsDo =
                this.settingsRepository.findByName(START_TIME_CRON_EXPRESSION);
        if (ObjectUtils.isEmpty(createOrReplaceArchiveCleanupSettingsDto.getStartTimeCronExpression())) {
            if (startTimeCronExpressionSettingsDo != null) {
                this.settingsRepository.delete(startTimeCronExpressionSettingsDo);
            }
        } else {
            if (startTimeCronExpressionSettingsDo == null) {
                startTimeCronExpressionSettingsDo = new SettingsDo();
                startTimeCronExpressionSettingsDo.setName(START_TIME_CRON_EXPRESSION);
                startTimeCronExpressionSettingsDo.setValue(
                        String.valueOf(createOrReplaceArchiveCleanupSettingsDto.getStartTimeCronExpression()));
                BaseDo.create(startTimeCronExpressionSettingsDo, operatingUserProfile.getUid(), LocalDateTime.now());
            } else {
                startTimeCronExpressionSettingsDo.setValue(
                        String.valueOf(createOrReplaceArchiveCleanupSettingsDto.getStartTimeCronExpression()));
                BaseDo.update(startTimeCronExpressionSettingsDo, operatingUserProfile.getUid(), LocalDateTime.now());
            }
            this.settingsRepository.save(startTimeCronExpressionSettingsDo);
        }

        //
        SettingsDo maximumDurationInMinutesSettingsDo =
                this.settingsRepository.findByName(MAXIMUM_DURATION_IN_MINUTES);
        if (createOrReplaceArchiveCleanupSettingsDto.getMaximumDurationInMinutes() == null) {
            if (maximumDurationInMinutesSettingsDo != null) {
                this.settingsRepository.delete(maximumDurationInMinutesSettingsDo);
            }
        } else {
            if (maximumDurationInMinutesSettingsDo == null) {
                maximumDurationInMinutesSettingsDo = new SettingsDo();
                maximumDurationInMinutesSettingsDo.setName(MAXIMUM_DURATION_IN_MINUTES);
                maximumDurationInMinutesSettingsDo.setValue(
                        String.valueOf(createOrReplaceArchiveCleanupSettingsDto.getMaximumDurationInMinutes()));
                BaseDo.create(maximumDurationInMinutesSettingsDo, operatingUserProfile.getUid(), LocalDateTime.now());
            } else {
                maximumDurationInMinutesSettingsDo.setValue(
                        String.valueOf(createOrReplaceArchiveCleanupSettingsDto.getMaximumDurationInMinutes()));
                BaseDo.update(maximumDurationInMinutesSettingsDo, operatingUserProfile.getUid(), LocalDateTime.now());
            }
            this.settingsRepository.save(maximumDurationInMinutesSettingsDo);
        }

        //
        SettingsDo maximumRetainDaysForLogicalDeleteSettingsDo =
                this.settingsRepository.findByName(MAXIMUM_RETAIN_DAYS_FOR_LOGICAL_DELETE);
        if (createOrReplaceArchiveCleanupSettingsDto.getMaximumRetainDaysForLogicalDelete() == null) {
            if (maximumRetainDaysForLogicalDeleteSettingsDo != null) {
                this.settingsRepository.delete(maximumRetainDaysForLogicalDeleteSettingsDo);
            }
        } else {
            if (maximumRetainDaysForLogicalDeleteSettingsDo == null) {
                maximumRetainDaysForLogicalDeleteSettingsDo = new SettingsDo();
                maximumRetainDaysForLogicalDeleteSettingsDo.setName(MAXIMUM_RETAIN_DAYS_FOR_LOGICAL_DELETE);
                maximumRetainDaysForLogicalDeleteSettingsDo.setValue(
                        String.valueOf(createOrReplaceArchiveCleanupSettingsDto.getMaximumRetainDaysForLogicalDelete()));
                BaseDo.create(maximumRetainDaysForLogicalDeleteSettingsDo, operatingUserProfile.getUid(), LocalDateTime.now());
            } else {
                maximumRetainDaysForLogicalDeleteSettingsDo.setValue(
                        String.valueOf(createOrReplaceArchiveCleanupSettingsDto.getMaximumRetainDaysForLogicalDelete()));
                BaseDo.update(maximumRetainDaysForLogicalDeleteSettingsDo, operatingUserProfile.getUid(), LocalDateTime.now());
            }
            this.settingsRepository.save(maximumRetainDaysForLogicalDeleteSettingsDo);
        }

        //
        SettingsDo maximumRetainDaysForAccessLogsSettingsDo =
                this.settingsRepository.findByName(MAXIMUM_RETAIN_DAYS_FOR_ACCESS_LOGS);
        if (createOrReplaceArchiveCleanupSettingsDto.getMaximumRetainDaysForAccessLogs() == null) {
            if (maximumRetainDaysForAccessLogsSettingsDo != null) {
                this.settingsRepository.delete(maximumRetainDaysForAccessLogsSettingsDo);
            }
        } else {
            if (maximumRetainDaysForAccessLogsSettingsDo == null) {
                maximumRetainDaysForAccessLogsSettingsDo = new SettingsDo();
                maximumRetainDaysForAccessLogsSettingsDo.setName(MAXIMUM_RETAIN_DAYS_FOR_ACCESS_LOGS);
                maximumRetainDaysForAccessLogsSettingsDo.setValue(
                        String.valueOf(createOrReplaceArchiveCleanupSettingsDto.getMaximumRetainDaysForAccessLogs()));
                BaseDo.create(maximumRetainDaysForAccessLogsSettingsDo, operatingUserProfile.getUid(), LocalDateTime.now());
            } else {
                maximumRetainDaysForAccessLogsSettingsDo.setValue(
                        String.valueOf(createOrReplaceArchiveCleanupSettingsDto.getMaximumRetainDaysForAccessLogs()));
                BaseDo.update(maximumRetainDaysForAccessLogsSettingsDo, operatingUserProfile.getUid(), LocalDateTime.now());
            }
            this.settingsRepository.save(maximumRetainDaysForAccessLogsSettingsDo);
        }

        //
        SettingsDo maximumRetainDaysForExportLogsSettingsDo =
                this.settingsRepository.findByName(MAXIMUM_RETAIN_DAYS_FOR_EXPORT_LOGS);
        if (createOrReplaceArchiveCleanupSettingsDto.getMaximumRetainDaysForExportLogs() == null) {
            if (maximumRetainDaysForExportLogsSettingsDo != null) {
                this.settingsRepository.delete(maximumRetainDaysForExportLogsSettingsDo);
            }
        } else {
            if (maximumRetainDaysForExportLogsSettingsDo == null) {
                maximumRetainDaysForExportLogsSettingsDo = new SettingsDo();
                maximumRetainDaysForExportLogsSettingsDo.setName(MAXIMUM_RETAIN_DAYS_FOR_EXPORT_LOGS);
                maximumRetainDaysForExportLogsSettingsDo.setValue(
                        String.valueOf(createOrReplaceArchiveCleanupSettingsDto.getMaximumRetainDaysForExportLogs()));
                BaseDo.create(maximumRetainDaysForExportLogsSettingsDo, operatingUserProfile.getUid(), LocalDateTime.now());
            } else {
                maximumRetainDaysForExportLogsSettingsDo.setValue(
                        String.valueOf(createOrReplaceArchiveCleanupSettingsDto.getMaximumRetainDaysForExportLogs()));
                BaseDo.update(maximumRetainDaysForExportLogsSettingsDo, operatingUserProfile.getUid(), LocalDateTime.now());
            }
            this.settingsRepository.save(maximumRetainDaysForExportLogsSettingsDo);
        }

        //
        SettingsDo maximumRetainDaysForJobExecutionLogsSettingsDo =
                this.settingsRepository.findByName(MAXIMUM_RETAIN_DAYS_FOR_JOB_EXECUTION_LOGS);
        if (createOrReplaceArchiveCleanupSettingsDto.getMaximumRetainDaysForJobExecutionLogs() == null) {
            if (maximumRetainDaysForJobExecutionLogsSettingsDo != null) {
                this.settingsRepository.delete(maximumRetainDaysForJobExecutionLogsSettingsDo);
            }
        } else {
            if (maximumRetainDaysForJobExecutionLogsSettingsDo == null) {
                maximumRetainDaysForJobExecutionLogsSettingsDo = new SettingsDo();
                maximumRetainDaysForJobExecutionLogsSettingsDo.setName(MAXIMUM_RETAIN_DAYS_FOR_JOB_EXECUTION_LOGS);
                maximumRetainDaysForJobExecutionLogsSettingsDo.setValue(
                        String.valueOf(createOrReplaceArchiveCleanupSettingsDto.getMaximumRetainDaysForJobExecutionLogs()));
                BaseDo.create(maximumRetainDaysForJobExecutionLogsSettingsDo, operatingUserProfile.getUid(), LocalDateTime.now());
            } else {
                maximumRetainDaysForJobExecutionLogsSettingsDo.setValue(
                        String.valueOf(createOrReplaceArchiveCleanupSettingsDto.getMaximumRetainDaysForJobExecutionLogs()));
                BaseDo.update(maximumRetainDaysForJobExecutionLogsSettingsDo, operatingUserProfile.getUid(), LocalDateTime.now());
            }
            this.settingsRepository.save(maximumRetainDaysForJobExecutionLogsSettingsDo);
        }

        //
        SettingsDo maximumRetainDaysForTaskExecutionLogsSettingsDo =
                this.settingsRepository.findByName(MAXIMUM_RETAIN_DAYS_FOR_TASK_EXECUTION_LOGS);
        if (createOrReplaceArchiveCleanupSettingsDto.getMaximumRetainDaysForTaskExecutionLogs() == null) {
            if (maximumRetainDaysForTaskExecutionLogsSettingsDo != null) {
                this.settingsRepository.delete(maximumRetainDaysForTaskExecutionLogsSettingsDo);
            }
        } else {
            if (maximumRetainDaysForTaskExecutionLogsSettingsDo == null) {
                maximumRetainDaysForTaskExecutionLogsSettingsDo = new SettingsDo();
                maximumRetainDaysForTaskExecutionLogsSettingsDo.setName(MAXIMUM_RETAIN_DAYS_FOR_TASK_EXECUTION_LOGS);
                maximumRetainDaysForTaskExecutionLogsSettingsDo.setValue(
                        String.valueOf(createOrReplaceArchiveCleanupSettingsDto.getMaximumRetainDaysForTaskExecutionLogs()));
                BaseDo.create(maximumRetainDaysForTaskExecutionLogsSettingsDo, operatingUserProfile.getUid(), LocalDateTime.now());
            } else {
                maximumRetainDaysForTaskExecutionLogsSettingsDo.setValue(
                        String.valueOf(createOrReplaceArchiveCleanupSettingsDto.getMaximumRetainDaysForTaskExecutionLogs()));
                BaseDo.update(maximumRetainDaysForTaskExecutionLogsSettingsDo, operatingUserProfile.getUid(), LocalDateTime.now());
            }
            this.settingsRepository.save(maximumRetainDaysForTaskExecutionLogsSettingsDo);
        }

        //
        SettingsDo maximumRetainDaysForDictionaryBuildLogsSettingsDo =
                this.settingsRepository.findByName(MAXIMUM_RETAIN_DAYS_FOR_DICTIONARY_BUILD_LOGS);
        if (createOrReplaceArchiveCleanupSettingsDto.getMaximumRetainDaysForDictionaryBuildLogs() == null) {
            if (maximumRetainDaysForDictionaryBuildLogsSettingsDo != null) {
                this.settingsRepository.delete(maximumRetainDaysForDictionaryBuildLogsSettingsDo);
            }
        } else {
            if (maximumRetainDaysForDictionaryBuildLogsSettingsDo == null) {
                maximumRetainDaysForDictionaryBuildLogsSettingsDo = new SettingsDo();
                maximumRetainDaysForDictionaryBuildLogsSettingsDo.setName(MAXIMUM_RETAIN_DAYS_FOR_DICTIONARY_BUILD_LOGS);
                maximumRetainDaysForDictionaryBuildLogsSettingsDo.setValue(
                        String.valueOf(createOrReplaceArchiveCleanupSettingsDto.getMaximumRetainDaysForDictionaryBuildLogs()));
                BaseDo.create(maximumRetainDaysForDictionaryBuildLogsSettingsDo, operatingUserProfile.getUid(), LocalDateTime.now());
            } else {
                maximumRetainDaysForDictionaryBuildLogsSettingsDo.setValue(
                        String.valueOf(createOrReplaceArchiveCleanupSettingsDto.getMaximumRetainDaysForDictionaryBuildLogs()));
                BaseDo.update(maximumRetainDaysForDictionaryBuildLogsSettingsDo, operatingUserProfile.getUid(), LocalDateTime.now());
            }
            this.settingsRepository.save(maximumRetainDaysForDictionaryBuildLogsSettingsDo);
        }

        createOrUpdateCleanupJob(
                createOrReplaceArchiveCleanupSettingsDto.getEnabledCleanup(),
                createOrReplaceArchiveCleanupSettingsDto.getStartTimeCronExpression(),
                createOrReplaceArchiveCleanupSettingsDto.getMaximumDurationInMinutes(),
                operatingUserProfile);

        ArchiveCleanupSettingsDto archiveCleanupSettingsDto = new ArchiveCleanupSettingsDto();
        BeanUtils.copyProperties(createOrReplaceArchiveCleanupSettingsDto, archiveCleanupSettingsDto);
        return archiveCleanupSettingsDto;
    }

    private void createOrUpdateCleanupJob(
            Boolean enabledCleanup,
            String startTimeCronExpression,
            Integer maximumDurationInMinutes,
            UserProfile operatingUserProfile) {
        // 调度 job
        Long cleanupJobUid = null;
        SettingsDo cleanupJobUidSettingsDo =
                this.settingsRepository.findByName(CLEANUP_JOB_UID);
        if (cleanupJobUidSettingsDo != null
                && !ObjectUtils.isEmpty(cleanupJobUidSettingsDo.getValue())) {
            cleanupJobUid = Long.parseLong(cleanupJobUidSettingsDo.getValue());
        }

        if (Boolean.TRUE.equals(enabledCleanup)) {
            if (cleanupJobUid == null) {
                CreateDistributedJobDto createDistributedJobDto = new CreateDistributedJobDto();
                createDistributedJobDto.setName("cleanup");
                createDistributedJobDto.setDescription("cleanup");
                createDistributedJobDto.setCronExpression(startTimeCronExpression);
                createDistributedJobDto.setEnabled(Boolean.TRUE);
                createDistributedJobDto.setFailedRetires(0);
                createDistributedJobDto.setHandlerName(Cleaner.JOB_HANDLER_CLEANUP);

                JSONObject parameters = new JSONObject();
                parameters.put("maximum_duration_in_minutes", maximumDurationInMinutes);
                createDistributedJobDto.setParameters(parameters);
                createDistributedJobDto.setRoutingAlgorithm(JobExecutorRoutingAlgorithmEnum.ROUND_ROBIN);
                createDistributedJobDto.setTimeoutDurationInSecs(3600L);

                DistributedJobDto distributedJobDto = this.distributedJobService.createJob(createDistributedJobDto, operatingUserProfile);

                cleanupJobUid = distributedJobDto.getUid();

                if (cleanupJobUidSettingsDo == null) {
                    cleanupJobUidSettingsDo = new SettingsDo();
                    cleanupJobUidSettingsDo.setName(CLEANUP_JOB_UID);
                    cleanupJobUidSettingsDo.setValue(String.valueOf(cleanupJobUid));
                    BaseDo.create(cleanupJobUidSettingsDo, operatingUserProfile.getUid(), LocalDateTime.now());
                } else {
                    cleanupJobUidSettingsDo.setValue(String.valueOf(cleanupJobUid));
                    BaseDo.update(cleanupJobUidSettingsDo, operatingUserProfile.getUid(), LocalDateTime.now());
                }
                this.settingsRepository.save(cleanupJobUidSettingsDo);
            } else {
                UpdateDistributedJobDto updateDistributedJobDto = new UpdateDistributedJobDto();
                updateDistributedJobDto.setEnabled(Boolean.TRUE);
                updateDistributedJobDto.setName("cleanup");
                updateDistributedJobDto.setDescription("cleanup");
                updateDistributedJobDto.setCronExpression(startTimeCronExpression);

                JSONObject parameters = new JSONObject();
                parameters.put("maximum_duration_in_minutes", maximumDurationInMinutes);
                updateDistributedJobDto.setParameters(parameters);
                updateDistributedJobDto.setRoutingAlgorithm(JobExecutorRoutingAlgorithmEnum.ROUND_ROBIN);
                updateDistributedJobDto.setTimeoutDurationInSecs(3600L);

                this.distributedJobService.updateJob(cleanupJobUid, updateDistributedJobDto, operatingUserProfile);
            }
        } else {
            if (cleanupJobUid != null) {
                UpdateDistributedJobDto updateDistributedJobDto = new UpdateDistributedJobDto();
                updateDistributedJobDto.setEnabled(Boolean.FALSE);

                this.distributedJobService.updateJob(cleanupJobUid, updateDistributedJobDto, operatingUserProfile);
            }
        }
    }

    @Override
    public ArchiveCleanupSettingsDto getArchiveCleanupSettings(
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        ArchiveCleanupSettingsDto archiveCleanupSettingsDto = new ArchiveCleanupSettingsDto();

        SettingsDo enabledCleanupSettingsDo =
                this.settingsRepository.findByName(ENABLED_CLEANUP);
        if (enabledCleanupSettingsDo != null
                && !ObjectUtils.isEmpty(enabledCleanupSettingsDo.getValue())) {
            archiveCleanupSettingsDto.setEnabledCleanup(
                    Boolean.parseBoolean(enabledCleanupSettingsDo.getValue()));
        }

        SettingsDo startTimeCronExpressionSettingsDo =
                this.settingsRepository.findByName(START_TIME_CRON_EXPRESSION);
        if (startTimeCronExpressionSettingsDo != null
                && !ObjectUtils.isEmpty(startTimeCronExpressionSettingsDo.getValue())) {
            archiveCleanupSettingsDto.setStartTimeCronExpression(
                    startTimeCronExpressionSettingsDo.getValue());
        }

        SettingsDo maximumDurationInMinutesSettingsDo =
                this.settingsRepository.findByName(MAXIMUM_DURATION_IN_MINUTES);
        if (maximumDurationInMinutesSettingsDo != null
                && !ObjectUtils.isEmpty(maximumDurationInMinutesSettingsDo.getValue())) {
            archiveCleanupSettingsDto.setMaximumDurationInMinutes(
                    Integer.parseInt(maximumDurationInMinutesSettingsDo.getValue()));
        }

        SettingsDo maximumRetainDaysForLogicalDeleteSettingsDo =
                this.settingsRepository.findByName(MAXIMUM_RETAIN_DAYS_FOR_LOGICAL_DELETE);
        if (maximumRetainDaysForLogicalDeleteSettingsDo != null
                && !ObjectUtils.isEmpty(maximumRetainDaysForLogicalDeleteSettingsDo.getValue())) {
            archiveCleanupSettingsDto.setMaximumRetainDaysForLogicalDelete(
                    Integer.parseInt(maximumRetainDaysForLogicalDeleteSettingsDo.getValue()));
        }

        SettingsDo maximumRetainDaysForAccessLogsSettingsDo =
                this.settingsRepository.findByName(MAXIMUM_RETAIN_DAYS_FOR_ACCESS_LOGS);
        if (maximumRetainDaysForAccessLogsSettingsDo != null
                && !ObjectUtils.isEmpty(maximumRetainDaysForAccessLogsSettingsDo.getValue())) {
            archiveCleanupSettingsDto.setMaximumRetainDaysForAccessLogs(
                    Integer.parseInt(maximumRetainDaysForAccessLogsSettingsDo.getValue()));
        }

        SettingsDo maximumRetainDaysForExportLogsSettingsDo =
                this.settingsRepository.findByName(MAXIMUM_RETAIN_DAYS_FOR_EXPORT_LOGS);
        if (maximumRetainDaysForExportLogsSettingsDo != null
                && !ObjectUtils.isEmpty(maximumRetainDaysForExportLogsSettingsDo.getValue())) {
            archiveCleanupSettingsDto.setMaximumRetainDaysForExportLogs(
                    Integer.parseInt(maximumRetainDaysForExportLogsSettingsDo.getValue()));
        }

        SettingsDo maximumRetainDaysForJobExecutionLogsSettingsDo =
                this.settingsRepository.findByName(MAXIMUM_RETAIN_DAYS_FOR_JOB_EXECUTION_LOGS);
        if (maximumRetainDaysForJobExecutionLogsSettingsDo != null
                && !ObjectUtils.isEmpty(maximumRetainDaysForJobExecutionLogsSettingsDo.getValue())) {
            archiveCleanupSettingsDto.setMaximumRetainDaysForJobExecutionLogs(
                    Integer.parseInt(maximumRetainDaysForJobExecutionLogsSettingsDo.getValue()));
        }

        SettingsDo maximumRetainDaysForTaskExecutionLogsSettingsDo =
                this.settingsRepository.findByName(MAXIMUM_RETAIN_DAYS_FOR_TASK_EXECUTION_LOGS);
        if (maximumRetainDaysForTaskExecutionLogsSettingsDo != null
                && !ObjectUtils.isEmpty(maximumRetainDaysForTaskExecutionLogsSettingsDo.getValue())) {
            archiveCleanupSettingsDto.setMaximumRetainDaysForTaskExecutionLogs(
                    Integer.parseInt(maximumRetainDaysForTaskExecutionLogsSettingsDo.getValue()));
        }

        SettingsDo maximumRetainDaysForDictionaryBuildLogsSettingsDo =
                this.settingsRepository.findByName(MAXIMUM_RETAIN_DAYS_FOR_DICTIONARY_BUILD_LOGS);
        if (maximumRetainDaysForDictionaryBuildLogsSettingsDo != null
                && !ObjectUtils.isEmpty(maximumRetainDaysForDictionaryBuildLogsSettingsDo.getValue())) {
            archiveCleanupSettingsDto.setMaximumRetainDaysForDictionaryBuildLogs(
                    Integer.parseInt(maximumRetainDaysForDictionaryBuildLogsSettingsDo.getValue()));
        }

        return archiveCleanupSettingsDto;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public CapacityControlSettingsDto createOrReplaceCapacityControlSettings(
            CreateOrReplaceCapacityControlSettingsDto createOrReplaceCapacityControlSettingsDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        SettingsDo maximumNumberOfImagesExportedByOneExportTask =
                this.settingsRepository.findByName(MAXIMUM_NUMBER_OF_IMAGES_EXPORTED_BY_ONE_EXPORT_TASK);
        if (createOrReplaceCapacityControlSettingsDto.getMaximumNumberOfImagesExportedByOneExportTask() == null) {
            if (maximumNumberOfImagesExportedByOneExportTask != null) {
                this.settingsRepository.delete(maximumNumberOfImagesExportedByOneExportTask);
            }
        } else {
            if (maximumNumberOfImagesExportedByOneExportTask == null) {
                maximumNumberOfImagesExportedByOneExportTask = new SettingsDo();
                maximumNumberOfImagesExportedByOneExportTask.setName(MAXIMUM_NUMBER_OF_IMAGES_EXPORTED_BY_ONE_EXPORT_TASK);
                maximumNumberOfImagesExportedByOneExportTask.setValue(
                        String.valueOf(createOrReplaceCapacityControlSettingsDto.getMaximumNumberOfImagesExportedByOneExportTask()));
                BaseDo.create(maximumNumberOfImagesExportedByOneExportTask, operatingUserProfile.getUid(), LocalDateTime.now());
            } else {
                maximumNumberOfImagesExportedByOneExportTask.setValue(
                        String.valueOf(createOrReplaceCapacityControlSettingsDto.getMaximumNumberOfImagesExportedByOneExportTask()));
                BaseDo.update(maximumNumberOfImagesExportedByOneExportTask, operatingUserProfile.getUid(), LocalDateTime.now());
            }
            this.settingsRepository.save(maximumNumberOfImagesExportedByOneExportTask);
        }

        //
        SettingsDo maximumNumberOfFilesExportedByOneExportTask =
                this.settingsRepository.findByName(MAXIMUM_NUMBER_OF_FILES_EXPORTED_BY_ONE_EXPORT_TASK);
        if (createOrReplaceCapacityControlSettingsDto.getMaximumNumberOfFilesExportedByOneExportTask() == null) {
            if (maximumNumberOfFilesExportedByOneExportTask != null) {
                this.settingsRepository.delete(maximumNumberOfFilesExportedByOneExportTask);
            }
        } else {
            if (maximumNumberOfFilesExportedByOneExportTask == null) {
                maximumNumberOfFilesExportedByOneExportTask = new SettingsDo();
                maximumNumberOfFilesExportedByOneExportTask.setName(MAXIMUM_NUMBER_OF_FILES_EXPORTED_BY_ONE_EXPORT_TASK);
                maximumNumberOfFilesExportedByOneExportTask.setValue(
                        String.valueOf(createOrReplaceCapacityControlSettingsDto.getMaximumNumberOfFilesExportedByOneExportTask()));
                BaseDo.create(maximumNumberOfFilesExportedByOneExportTask, operatingUserProfile.getUid(), LocalDateTime.now());
            } else {
                maximumNumberOfFilesExportedByOneExportTask.setValue(
                        String.valueOf(createOrReplaceCapacityControlSettingsDto.getMaximumNumberOfFilesExportedByOneExportTask()));
                BaseDo.update(maximumNumberOfFilesExportedByOneExportTask, operatingUserProfile.getUid(), LocalDateTime.now());
            }
            this.settingsRepository.save(maximumNumberOfFilesExportedByOneExportTask);
        }

        CapacityControlSettingsDto capacityControlSettingsDto = new CapacityControlSettingsDto();
        BeanUtils.copyProperties(createOrReplaceCapacityControlSettingsDto, capacityControlSettingsDto);
        return capacityControlSettingsDto;
    }

    @Override
    public CapacityControlSettingsDto getCapacityControlSettings(
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        SettingsDo maximumNumberOfImagesExportedByOneExportTaskSettingsDo =
                this.settingsRepository.findByName(MAXIMUM_NUMBER_OF_IMAGES_EXPORTED_BY_ONE_EXPORT_TASK);

        SettingsDo maximumNumberOfFilesExportedByOneExportTaskSettingsDo =
                this.settingsRepository.findByName(MAXIMUM_NUMBER_OF_FILES_EXPORTED_BY_ONE_EXPORT_TASK);

        CapacityControlSettingsDto capacityControlSettingsDto = new CapacityControlSettingsDto();

        if (maximumNumberOfImagesExportedByOneExportTaskSettingsDo != null
                && !ObjectUtils.isEmpty(maximumNumberOfImagesExportedByOneExportTaskSettingsDo.getValue())) {
            capacityControlSettingsDto.setMaximumNumberOfImagesExportedByOneExportTask(
                    Integer.parseInt(maximumNumberOfImagesExportedByOneExportTaskSettingsDo.getValue()));
        }
        if (maximumNumberOfFilesExportedByOneExportTaskSettingsDo != null
                && !ObjectUtils.isEmpty(maximumNumberOfFilesExportedByOneExportTaskSettingsDo.getValue())) {
            capacityControlSettingsDto.setMaximumNumberOfFilesExportedByOneExportTask(
                    Integer.parseInt(maximumNumberOfFilesExportedByOneExportTaskSettingsDo.getValue()));
        }

        return capacityControlSettingsDto;
    }

    @Override
    public List<SignInOptionDto> listingQuerySignInOptions() throws AbcUndefinedException {
        List<AuthenticationServiceAgentDto> authenticationServiceAgentDtoList =
                this.authenticationServiceService.listingQueryAuthenticationServiceAgents(
                        null, null, Sort.by(Sort.Order.asc("sequence")), null);
        if (CollectionUtils.isEmpty(authenticationServiceAgentDtoList)) {
            return null;
        }

        List<SignInOptionDto> result = new LinkedList<>();
        authenticationServiceAgentDtoList.forEach(authenticationServiceAgentDto -> {
            if (Boolean.TRUE.equals(authenticationServiceAgentDto.getEnabled())) {
                // 补充信息
                authenticationServiceAgentDto =
                        this.authenticationServiceService.getAuthenticationServiceAgent(
                                authenticationServiceAgentDto.getUid(), null);

                SignInOptionDto signInOptionDto = new SignInOptionDto();
                signInOptionDto.setUid(authenticationServiceAgentDto.getUid());
                signInOptionDto.setName(authenticationServiceAgentDto.getName());
                signInOptionDto.setObjectName(authenticationServiceAgentDto.getObjectName());
                signInOptionDto.setDescription(authenticationServiceAgentDto.getDescription());
                signInOptionDto.setPreferred(authenticationServiceAgentDto.getPreferred());
                signInOptionDto.setType(authenticationServiceAgentDto.getServiceComponent().getType());
                signInOptionDto.setFrontEndComponent(authenticationServiceAgentDto.getServiceComponent().getFrontEndComponent());
                result.add(signInOptionDto);
            }
        });

        return result;
    }

    @Override
    public List<TreeNode> listingQueryNavigationMenus(
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        if (InfrastructureConstants.ROOT_USER_UID.equals(operatingUserProfile.getUid())) {
            return privilegedListingQueryNavigationMenus(operatingUserProfile);
        }

        UserDetailedDto userDetailedDto = this.userService.getUserDetailed(operatingUserProfile.getUid(),
                operatingUserProfile);
        Permissions permissions = userDetailedDto.getPermissions();
        if (permissions == null || CollectionUtils.isEmpty(permissions.getNavigationMenuList())) {
            LOGGER.warn("cannot find any navigation menu for user: {} ({})", operatingUserProfile.getUid(),
                    operatingUserProfile.getDisplayName());
            return null;
        }

        List<NavigationMenu> directAllowedNavigationMenuList = permissions.getNavigationMenuList();
        List<Long> allowedNavigationMenuUidList = new ArrayList<>(directAllowedNavigationMenuList.size());
        directAllowedNavigationMenuList.forEach(navigationMenu -> {
            allowedNavigationMenuUidList.add(navigationMenu.getUid());
        });

        List<TreeNode> allTreeNodeList =
                this.permissionsService.treeListingAllNodesOfNavigationMenuHierarchy(operatingUserProfile);

        // 记录每个 tree node 的 parent uid
        Map<Long, TreeNode> allTreeNodeMap = new HashMap<>();
        Map<Long, Long> allParentTreeNodeUidMap = new HashMap<>();
        recursivelyBuildAllTreeNodeMapAndParentRelationship(
                null, allTreeNodeList, allTreeNodeMap, allParentTreeNodeUidMap);

        recursivelyTaggingAllowedNavigationMenus(
                allTreeNodeList, allTreeNodeMap, allParentTreeNodeUidMap,
                allowedNavigationMenuUidList);

        return allTreeNodeList;
    }

    private void recursivelyBuildAllTreeNodeMapAndParentRelationship(
            Long parentTreeNodeUid,
            List<TreeNode> treeNodeList,
            Map<Long, TreeNode> allTreeNodeMap,
            Map<Long, Long> allParentTreeNodeUidMap) {
        for (TreeNode treeNode : treeNodeList) {
            allTreeNodeMap.put(treeNode.getUid(), treeNode);

            if (parentTreeNodeUid != null) {
                allParentTreeNodeUidMap.put(treeNode.getUid(), parentTreeNodeUid);
            }

            if (!CollectionUtils.isEmpty(treeNode.getChildren())) {
                recursivelyBuildAllTreeNodeMapAndParentRelationship(
                        treeNode.getUid(), treeNode.getChildren(), allTreeNodeMap, allParentTreeNodeUidMap);
            }
        }
    }

    private void recursivelyTaggingAllowedNavigationMenus(
            List<TreeNode> treeNodeList,
            Map<Long, TreeNode> allTreeNodeMap,
            Map<Long, Long> allParentTreeNodeUidMap,
            List<Long> allowedNavigationMenuUidList) {
        for (TreeNode treeNode : treeNodeList) {
            // direct allowed
            if (allowedNavigationMenuUidList.contains(treeNode.getUid())) {
                if (treeNode.getTags() == null) {
                    treeNode.setTags(new HashMap<>());
                }
                treeNode.getTags().put("allowed", true);

                // indirect allowed
                recursivelyTaggingIndirectAllowedNavigationMenus(treeNode, allTreeNodeMap, allParentTreeNodeUidMap);
            }

            if (!CollectionUtils.isEmpty(treeNode.getChildren())) {
                recursivelyTaggingAllowedNavigationMenus(treeNode.getChildren(),
                        allTreeNodeMap, allParentTreeNodeUidMap, allowedNavigationMenuUidList);
            }
        }
    }

    private void recursivelyTaggingIndirectAllowedNavigationMenus(
            TreeNode treeNode,
            Map<Long, TreeNode> allTreeNodeMap,
            Map<Long, Long> allParentTreeNodeUidMap) {
        Long parentTreeNodeUid = allParentTreeNodeUidMap.get(treeNode.getUid());
        if (parentTreeNodeUid == null) {
            return;
        }

        TreeNode parentTreeNode = allTreeNodeMap.get(parentTreeNodeUid);
        if (parentTreeNode == null) {
            return;
        }

        // tree node allowed, 其 parent tree node 也应该 allowed (也就是 Hierarchy 的半勾选）
        if (parentTreeNode.getTags() == null) {
            parentTreeNode.setTags(new HashMap<>());
            parentTreeNode.getTags().put("allowed", true);
        } else {
            Boolean allowedParentTreeNode = (Boolean) parentTreeNode.getTags().get("allowed");
            if (!Boolean.TRUE.equals(allowedParentTreeNode)) {
                parentTreeNode.getTags().put("allowed", true);
            }
        }

        recursivelyTaggingIndirectAllowedNavigationMenus(parentTreeNode, allTreeNodeMap, allParentTreeNodeUidMap);
    }

    private List<TreeNode> privilegedListingQueryNavigationMenus(UserProfile operatingUserProfile) {
        List<TreeNode> allTreeNodeList =
                this.permissionsService.treeListingAllNodesOfNavigationMenuHierarchy(operatingUserProfile);
        if (!CollectionUtils.isEmpty(allTreeNodeList)) {
            recursivelyTaggingAllowedNavigationMenus(allTreeNodeList);
        }

        return allTreeNodeList;
    }

    private void recursivelyTaggingAllowedNavigationMenus(
            List<TreeNode> treeNodeList) {
        for (TreeNode treeNode : treeNodeList) {
            if (treeNode.getTags() == null) {
                treeNode.setTags(new HashMap<>());
            }
            treeNode.getTags().put("allowed", true);

            if (!CollectionUtils.isEmpty(treeNode.getChildren())) {
                recursivelyTaggingAllowedNavigationMenus(treeNode.getChildren());
            }
        }
    }
}

