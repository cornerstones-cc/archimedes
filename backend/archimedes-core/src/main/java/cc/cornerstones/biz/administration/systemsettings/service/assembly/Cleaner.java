package cc.cornerstones.biz.administration.systemsettings.service.assembly;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.utils.AbcDateUtils;
import cc.cornerstones.biz.administration.systemsettings.entity.SettingsDo;
import cc.cornerstones.biz.administration.systemsettings.persistence.SettingsRepository;
import cc.cornerstones.biz.datadictionary.entity.DictionaryBuildInstanceDo;
import cc.cornerstones.biz.datadictionary.persistence.DictionaryBuildInstanceRepository;
import cc.cornerstones.biz.distributedjob.entity.DistributedJobExecutionDo;
import cc.cornerstones.biz.distributedjob.persistence.DistributedJobExecutionRepository;
import cc.cornerstones.biz.distributedjob.share.types.JobHandler;
import cc.cornerstones.biz.distributedtask.entity.DistributedTaskDo;
import cc.cornerstones.biz.distributedtask.persistence.DistributedTaskRepository;
import cc.cornerstones.biz.export.entity.ExportTaskDo;
import cc.cornerstones.biz.export.persistence.ExportTaskRepository;
import cc.cornerstones.biz.operations.accesslogging.entity.QueryLogDo;
import cc.cornerstones.biz.operations.accesslogging.persistence.QueryLogRepository;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static cc.cornerstones.biz.administration.systemsettings.service.impl.SystemSettingsServiceImpl.*;

@Component
public class Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(Cleaner.class);

    @Autowired
    private SettingsRepository settingsRepository;

    @Autowired
    private DistributedJobExecutionRepository distributedJobExecutionRepository;

    @Autowired
    private DistributedTaskRepository distributedTaskRepository;

    @Autowired
    private DictionaryBuildInstanceRepository dictionaryBuildInstanceRepository;

    @Autowired
    private QueryLogRepository queryLogRepository;

    @Autowired
    private ExportTaskRepository exportTaskRepository;

    public static final String JOB_HANDLER_CLEANUP = "cleanup";

    @JobHandler(name = JOB_HANDLER_CLEANUP)
    public void cleanup(JSONObject params) throws AbcUndefinedException {
        LOGGER.info("[cleanup] begin to cleanup");

        //
        // Step 1, pre-processing
        //
        Integer maximumDurationInMinutes = params.getInteger("maximum_duration_in_minutes");
        if (maximumDurationInMinutes == null) {
            LOGGER.error("[cleanup] cannot find maximum_duration_in_minutes from the input parameters");
            return;
        }

        LocalDateTime shouldEndLocalDateTime = LocalDateTime.now().plusMinutes(maximumDurationInMinutes);

        //
        // Step 2, core-processing
        //

        long beginTime = System.currentTimeMillis();

        cleanupJobExecutionLogs(shouldEndLocalDateTime);

        cleanupTaskExecutionLogs(shouldEndLocalDateTime);

        cleanupDictionaryBuildLogs(shouldEndLocalDateTime);

        cleanupAccessLogs(shouldEndLocalDateTime);

        cleanupExportLogs(shouldEndLocalDateTime);

        LOGGER.info("[cleanup] end to cleanup, duration:{}", AbcDateUtils.format(System.currentTimeMillis() - beginTime));
    }

    public void cleanupJobExecutionLogs(LocalDateTime shouldEndLocalDateTime) throws AbcUndefinedException {
        if (LocalDateTime.now().isAfter(shouldEndLocalDateTime)) {
            return;
        }

        long beginTime = System.currentTimeMillis();
        LOGGER.info("[cleanup] begin to cleanup job execution logs");

        Integer maximumRetainDaysForJobExecutionLogs = 28;
        SettingsDo maximumRetainDaysForJobExecutionLogsSettingsDo =
                this.settingsRepository.findByName(MAXIMUM_RETAIN_DAYS_FOR_JOB_EXECUTION_LOGS);
        if (maximumRetainDaysForJobExecutionLogsSettingsDo != null
                && maximumRetainDaysForJobExecutionLogsSettingsDo.getValue() != null) {
            maximumRetainDaysForJobExecutionLogs =
                    Integer.parseInt(maximumRetainDaysForJobExecutionLogsSettingsDo.getValue());
        }

        LocalDateTime nowDateTime = LocalDateTime.now();
        LocalDateTime thresholdCreatedTimestamp = nowDateTime.minusDays(maximumRetainDaysForJobExecutionLogs);

        Specification<DistributedJobExecutionDo> specification = new Specification<DistributedJobExecutionDo>() {
            @Override
            public Predicate toPredicate(Root<DistributedJobExecutionDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();

                predicateList.add(criteriaBuilder.lessThan(root.get(BaseDo.CREATED_TIMESTAMP_FIELD_NAME), thresholdCreatedTimestamp));
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        int total = 0;
        Page<DistributedJobExecutionDo> itemDoPage = null;
        do {
            PageRequest pageable = PageRequest.of(0, 2000, Sort.by(Sort.Order.asc(BaseDo.ID_FIELD_NAME)));
            itemDoPage = this.distributedJobExecutionRepository.findAll(specification, pageable);

            this.distributedJobExecutionRepository.deleteAll(itemDoPage);

            total += itemDoPage.getNumberOfElements();

            LOGGER.info("[cleanup] performing cleanup job execution logs, subtotal:{}, duration:{}", itemDoPage.getNumberOfElements(),
                    AbcDateUtils.format(System.currentTimeMillis() - beginTime));

            if (LocalDateTime.now().isAfter(shouldEndLocalDateTime)) {
                LOGGER.warn("[cleanup] the allowed working time range is exceeded and the task is ended");
                return;
            }
        } while (itemDoPage != null && !itemDoPage.isEmpty());

        LOGGER.info("[cleanup] end to cleanup job execution logs, total:{}, duration:{}", total,
                AbcDateUtils.format(System.currentTimeMillis() - beginTime));
    }

    public void cleanupTaskExecutionLogs(LocalDateTime shouldEndLocalDateTime) throws AbcUndefinedException {
        if (LocalDateTime.now().isAfter(shouldEndLocalDateTime)) {
            return;
        }

        long beginTime = System.currentTimeMillis();
        LOGGER.info("[cleanup] begin to cleanup task execution logs");

        Integer maximumRetainDaysForTaskExecutionLogs = 28;
        SettingsDo maximumRetainDaysForTaskExecutionLogsSettingsDo =
                this.settingsRepository.findByName(MAXIMUM_RETAIN_DAYS_FOR_TASK_EXECUTION_LOGS);
        if (maximumRetainDaysForTaskExecutionLogsSettingsDo != null
                && maximumRetainDaysForTaskExecutionLogsSettingsDo.getValue() != null) {
            maximumRetainDaysForTaskExecutionLogs =
                    Integer.parseInt(maximumRetainDaysForTaskExecutionLogsSettingsDo.getValue());
        }

        LocalDateTime nowDateTime = LocalDateTime.now();
        LocalDateTime thresholdCreatedTimestamp = nowDateTime.minusDays(maximumRetainDaysForTaskExecutionLogs);

        Specification<DistributedTaskDo> specification = new Specification<DistributedTaskDo>() {
            @Override
            public Predicate toPredicate(Root<DistributedTaskDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();

                predicateList.add(criteriaBuilder.lessThan(root.get(BaseDo.CREATED_TIMESTAMP_FIELD_NAME), thresholdCreatedTimestamp));
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        int total = 0;
        Page<DistributedTaskDo> itemDoPage = null;
        do {
            PageRequest pageable = PageRequest.of(0, 2000, Sort.by(Sort.Order.asc(BaseDo.ID_FIELD_NAME)));
            itemDoPage = this.distributedTaskRepository.findAll(specification, pageable);

            this.distributedTaskRepository.deleteAll(itemDoPage);

            total += itemDoPage.getNumberOfElements();

            LOGGER.info("[cleanup] performing cleanup task execution logs, subtotal:{}, duration:{}",
                    itemDoPage.getNumberOfElements(),
                    AbcDateUtils.format(System.currentTimeMillis() - beginTime));

            if (LocalDateTime.now().isAfter(shouldEndLocalDateTime)) {
                LOGGER.warn("[cleanup] the allowed working time range is exceeded and the task is ended");
                return;
            }
        } while (itemDoPage != null && !itemDoPage.isEmpty());

        LOGGER.info("[cleanup] end to cleanup task execution logs, total:{}, duration:{}", total,
                AbcDateUtils.format(System.currentTimeMillis() - beginTime));
    }

    public void cleanupDictionaryBuildLogs(LocalDateTime shouldEndLocalDateTime) throws AbcUndefinedException {
        if (LocalDateTime.now().isAfter(shouldEndLocalDateTime)) {
            return;
        }

        long beginTime = System.currentTimeMillis();
        LOGGER.info("[cleanup] begin to cleanup dictionary build logs");

        Integer maximumRetainDaysForDictionaryBuildLogs = 28;
        SettingsDo maximumRetainDaysForDictionaryBuildLogsSettingsDo =
                this.settingsRepository.findByName(MAXIMUM_RETAIN_DAYS_FOR_DICTIONARY_BUILD_LOGS);
        if (maximumRetainDaysForDictionaryBuildLogsSettingsDo != null
                && maximumRetainDaysForDictionaryBuildLogsSettingsDo.getValue() != null) {
            maximumRetainDaysForDictionaryBuildLogs =
                    Integer.parseInt(maximumRetainDaysForDictionaryBuildLogsSettingsDo.getValue());
        }

        LocalDateTime nowDateTime = LocalDateTime.now();
        LocalDateTime thresholdCreatedTimestamp = nowDateTime.minusDays(maximumRetainDaysForDictionaryBuildLogs);

        Specification<DictionaryBuildInstanceDo> specification = new Specification<DictionaryBuildInstanceDo>() {
            @Override
            public Predicate toPredicate(Root<DictionaryBuildInstanceDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();

                predicateList.add(criteriaBuilder.lessThan(root.get(BaseDo.CREATED_TIMESTAMP_FIELD_NAME), thresholdCreatedTimestamp));
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        int total = 0;
        Page<DictionaryBuildInstanceDo> itemDoPage = null;
        do {
            PageRequest pageable = PageRequest.of(0, 2000, Sort.by(Sort.Order.asc(BaseDo.ID_FIELD_NAME)));
            itemDoPage = this.dictionaryBuildInstanceRepository.findAll(specification, pageable);

            this.dictionaryBuildInstanceRepository.deleteAll(itemDoPage);

            total += itemDoPage.getNumberOfElements();

            LOGGER.info("[cleanup] performing cleanup dictionary build logs, subtotal:{}, duration:{}",
                    itemDoPage.getNumberOfElements(),
                    AbcDateUtils.format(System.currentTimeMillis() - beginTime));

            if (LocalDateTime.now().isAfter(shouldEndLocalDateTime)) {
                LOGGER.warn("[cleanup] the allowed working time range is exceeded and the task is ended");
                return;
            }
        } while (itemDoPage != null && !itemDoPage.isEmpty());

        LOGGER.info("[cleanup] end to cleanup dictionary build logs, total:{}, duration:{}", total,
                AbcDateUtils.format(System.currentTimeMillis() - beginTime));
    }

    public void cleanupAccessLogs(LocalDateTime shouldEndLocalDateTime) throws AbcUndefinedException {
        if (LocalDateTime.now().isAfter(shouldEndLocalDateTime)) {
            return;
        }

        long beginTime = System.currentTimeMillis();
        LOGGER.info("[cleanup] begin to cleanup query logs");

        Integer maximumRetainDaysForAccessLogs = 84;
        SettingsDo maximumRetainDaysForAccessLogsSettingsDo =
                this.settingsRepository.findByName(MAXIMUM_RETAIN_DAYS_FOR_ACCESS_LOGS);
        if (maximumRetainDaysForAccessLogsSettingsDo != null
                && maximumRetainDaysForAccessLogsSettingsDo.getValue() != null) {
            maximumRetainDaysForAccessLogs =
                    Integer.parseInt(maximumRetainDaysForAccessLogsSettingsDo.getValue());
        }

        LocalDateTime nowDateTime = LocalDateTime.now();
        LocalDateTime thresholdCreatedTimestamp = nowDateTime.minusDays(maximumRetainDaysForAccessLogs);

        Specification<QueryLogDo> specification = new Specification<QueryLogDo>() {
            @Override
            public Predicate toPredicate(Root<QueryLogDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();

                predicateList.add(criteriaBuilder.lessThan(root.get(BaseDo.CREATED_TIMESTAMP_FIELD_NAME), thresholdCreatedTimestamp));
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        int total = 0;
        Page<QueryLogDo> itemDoPage = null;
        do {
            PageRequest pageable = PageRequest.of(0, 2000, Sort.by(Sort.Order.asc(BaseDo.ID_FIELD_NAME)));
            itemDoPage = this.queryLogRepository.findAll(specification, pageable);

            this.queryLogRepository.deleteAll(itemDoPage);

            total += itemDoPage.getNumberOfElements();

            LOGGER.info("[cleanup] performing cleanup query logs, subtotal:{}, duration:{}",
                    itemDoPage.getNumberOfElements(),
                    AbcDateUtils.format(System.currentTimeMillis() - beginTime));

            if (LocalDateTime.now().isAfter(shouldEndLocalDateTime)) {
                LOGGER.warn("[cleanup] the allowed working time range is exceeded and the task is ended");
                return;
            }
        } while (itemDoPage != null && !itemDoPage.isEmpty());

        LOGGER.info("[cleanup] end to cleanup query logs, total:{}, duration:{}", total,
                AbcDateUtils.format(System.currentTimeMillis() - beginTime));
    }

    public void cleanupExportLogs(LocalDateTime shouldEndLocalDateTime) throws AbcUndefinedException {
        if (LocalDateTime.now().isAfter(shouldEndLocalDateTime)) {
            return;
        }

        long beginTime = System.currentTimeMillis();
        LOGGER.info("[cleanup] begin to cleanup export logs");

        Integer maximumRetainDaysForExportLogs = 84;
        SettingsDo maximumRetainDaysForExportLogsSettingsDo =
                this.settingsRepository.findByName(MAXIMUM_RETAIN_DAYS_FOR_EXPORT_LOGS);
        if (maximumRetainDaysForExportLogsSettingsDo != null
                && maximumRetainDaysForExportLogsSettingsDo.getValue() != null) {
            maximumRetainDaysForExportLogs =
                    Integer.parseInt(maximumRetainDaysForExportLogsSettingsDo.getValue());
        }

        LocalDateTime nowDateTime = LocalDateTime.now();
        LocalDateTime thresholdCreatedTimestamp = nowDateTime.minusDays(maximumRetainDaysForExportLogs);

        Specification<ExportTaskDo> specification = new Specification<ExportTaskDo>() {
            @Override
            public Predicate toPredicate(Root<ExportTaskDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();

                predicateList.add(criteriaBuilder.lessThan(root.get(BaseDo.CREATED_TIMESTAMP_FIELD_NAME), thresholdCreatedTimestamp));
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        int total = 0;
        Page<ExportTaskDo> itemDoPage = null;
        do {
            PageRequest pageable = PageRequest.of(0, 2000, Sort.by(Sort.Order.asc(BaseDo.ID_FIELD_NAME)));
            itemDoPage = this.exportTaskRepository.findAll(specification, pageable);

            this.exportTaskRepository.deleteAll(itemDoPage);

            total += itemDoPage.getNumberOfElements();

            LOGGER.info("[cleanup] performing cleanup export logs, subtotal:{}, duration:{}",
                    itemDoPage.getNumberOfElements(),
                    AbcDateUtils.format(System.currentTimeMillis() - beginTime));

            if (LocalDateTime.now().isAfter(shouldEndLocalDateTime)) {
                LOGGER.warn("[cleanup] the allowed working time range is exceeded and the task is ended");
                return;
            }
        } while (itemDoPage != null && !itemDoPage.isEmpty());

        LOGGER.info("[cleanup] end to cleanup export logs, total:{}, duration:{}", total,
                AbcDateUtils.format(System.currentTimeMillis() - beginTime));
    }
}
