package cc.cornerstones.biz.operations.migration.service.impl;

import cc.cornerstones.almond.constants.TaskStatusEnum;
import cc.cornerstones.almond.exceptions.*;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcDateUtils;
import cc.cornerstones.almond.utils.AbcFileUtils;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.administration.serviceconnection.dto.DfsServiceAgentDto;
import cc.cornerstones.biz.administration.serviceconnection.entity.DataPermissionServiceAgentDo;
import cc.cornerstones.biz.administration.serviceconnection.entity.DfsServiceAgentDo;
import cc.cornerstones.biz.administration.serviceconnection.persistence.DataPermissionServiceAgentRepository;
import cc.cornerstones.biz.administration.serviceconnection.persistence.DfsServiceAgentRepository;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DfsServiceAgentService;
import cc.cornerstones.biz.datadictionary.entity.DictionaryCategoryDo;
import cc.cornerstones.biz.datadictionary.persistence.DictionaryCategoryRepository;
import cc.cornerstones.biz.datafacet.dto.DataPermissionContentDto;
import cc.cornerstones.biz.datafacet.entity.*;
import cc.cornerstones.biz.datafacet.persistence.*;
import cc.cornerstones.biz.datafacet.share.types.*;
import cc.cornerstones.biz.datasource.entity.DataSourceDo;
import cc.cornerstones.biz.datasource.persistence.DataSourceRepository;
import cc.cornerstones.biz.datatable.entity.DataTableDo;
import cc.cornerstones.biz.datatable.persistence.DataTableRepository;
import cc.cornerstones.biz.datawidget.entity.DataWidgetDo;
import cc.cornerstones.biz.datawidget.persistence.DataWidgetRepository;
import cc.cornerstones.biz.distributedtask.dto.CreateDistributedTaskDto;
import cc.cornerstones.biz.distributedtask.dto.DistributedTaskDto;
import cc.cornerstones.biz.distributedtask.service.inf.DistributedTaskService;
import cc.cornerstones.biz.distributedtask.share.types.TaskHandler;
import cc.cornerstones.biz.operations.migration.dto.*;
import cc.cornerstones.biz.operations.migration.service.inf.MigrationService;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.PropertyNamingStrategy;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class MigrationServiceImpl implements MigrationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MigrationServiceImpl.class);

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private AdvancedFeatureRepository advancedFeatureRepository;

    @Autowired
    private DataFacetRepository dataFacetRepository;

    @Autowired
    private DataFieldRepository dataFieldRepository;

    @Autowired
    private DataPermissionRepository dataPermissionRepository;

    @Autowired
    private ExportBasicRepository exportBasicRepository;

    @Autowired
    private ExportExtendedTemplateRepository exportExtendedTemplateRepository;

    @Autowired
    private FilteringDataFieldRepository filteringDataFieldRepository;

    @Autowired
    private FilteringExtendedRepository filteringExtendedRepository;

    @Autowired
    private ListingDataFieldRepository listingDataFieldRepository;

    @Autowired
    private ListingExtendedRepository listingExtendedRepository;

    @Autowired
    private SortingDataFieldRepository sortingDataFieldRepository;

    @Autowired
    private DataWidgetRepository dataWidgetRepository;

    @Autowired
    private DataTableRepository dataTableRepository;

    @Autowired
    private DfsServiceAgentService dfsServiceAgentService;

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private DictionaryCategoryRepository dictionaryCategoryRepository;

    @Autowired
    private DfsServiceAgentRepository dfsServiceAgentRepository;

    @Autowired
    private DataPermissionServiceAgentRepository dataPermissionServiceAgentRepository;

    @Value("${private.dir.general.project.download}")
    private String projectDownloadPath;

    public static final String TASK_TYPE_MIGRATE_IN = "migrate_in";
    public static final String TASK_HANDLER_NAME_MIGRATE_IN = "migrate_in";

    private final DateTimeFormatter narrowDateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final DateTimeFormatter narrowTimeFormatter = DateTimeFormatter.ofPattern("HHmmssSSS");

    @Autowired
    private DistributedTaskService taskService;

    @Override
    public PreparedMigrateOutDto prepareMigrateOut(
            PrepareMigrateOutDto prepareMigrateOutDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        if (CollectionUtils.isEmpty(prepareMigrateOutDto.getSourceDataFacetUidList())) {
            throw new AbcIllegalParameterException("source_data_facet_uid_list should not be null or empty");
        }

        List<DataFacetDo> sourceDataFacetDoList =
                this.dataFacetRepository.findByUidIn(prepareMigrateOutDto.getSourceDataFacetUidList());
        if (CollectionUtils.isEmpty(sourceDataFacetDoList)
                || sourceDataFacetDoList.size() != prepareMigrateOutDto.getSourceDataFacetUidList().size()) {
            throw new AbcIllegalParameterException("data mismatch, at least one source data facet cannot be found");
        }

        Map<Long, String> dataSourceMap = new HashMap<>();
        Map<Long, String> dictionaryCategoryMap = new HashMap<>();
        Map<Long, String> dfsServiceAgentMap = new HashMap<>();
        Map<Long, String> dataPermissionServiceAgentMap = new HashMap<>();

        for (DataFacetDo sourceDataFacetDo : sourceDataFacetDoList) {
            Long sourceDataFacetUid = sourceDataFacetDo.getUid();
            Long sourceDataTableUid = sourceDataFacetDo.getDataTableUid();
            Long sourceDataSourceUid = sourceDataFacetDo.getDataSourceUid();

            if (!dataSourceMap.containsKey(sourceDataSourceUid)) {
                DataSourceDo dataSourceDo = this.dataSourceRepository.findByUid(sourceDataSourceUid);
                if (dataSourceDo == null) {
                    throw new AbcResourceIntegrityException("cannot find data source " + sourceDataSourceUid);
                }
                dataSourceMap.put(sourceDataSourceUid, dataSourceDo.getName());
            }

            // data field
            List<DataFieldDo> sourceDataFieldDoList = this.dataFieldRepository.findByDataFacetUid(sourceDataFacetUid);
            if (!CollectionUtils.isEmpty(sourceDataFieldDoList)) {
                for (DataFieldDo sourceDataFieldDo : sourceDataFieldDoList) {
                    if (sourceDataFieldDo.getTypeExtension() != null
                            && !sourceDataFieldDo.getTypeExtension().isEmpty()) {
                        FieldTypeExtensionFile fieldTypeExtensionFile =
                                JSONObject.toJavaObject(sourceDataFieldDo.getTypeExtension(),
                                        FieldTypeExtensionFile.class);
                        Long sourceDfsServiceAgentUid = fieldTypeExtensionFile.getDfsServiceAgentUid();
                        if (sourceDfsServiceAgentUid != null) {
                            if (!dfsServiceAgentMap.containsKey(sourceDfsServiceAgentUid)) {
                                DfsServiceAgentDo dfsServiceAgentDo =
                                        this.dfsServiceAgentRepository.findByUid(sourceDfsServiceAgentUid);
                                if (dfsServiceAgentDo == null) {
                                    throw new AbcResourceIntegrityException("cannot find dfs service agent " + sourceDfsServiceAgentUid);
                                }
                                dfsServiceAgentMap.put(sourceDfsServiceAgentUid, dfsServiceAgentDo.getName());
                            }
                        }
                    }
                }
            }

            // data permission
            List<DataPermissionDo> sourceDataPermissionDoList =
                    this.dataPermissionRepository.findByDataFacetUid(sourceDataFacetUid);
            if (!CollectionUtils.isEmpty(sourceDataPermissionDoList)) {
                for (DataPermissionDo sourceDataPermissionDo : sourceDataPermissionDoList) {
                    DataPermissionContentDto dataPermissionContentDto = sourceDataPermissionDo.getContent();
                    Long sourceDataPermissionServiceAgentUid =
                            dataPermissionContentDto.getDataPermissionServiceAgentUid();

                    if (sourceDataPermissionServiceAgentUid != null) {
                        if (!dataPermissionServiceAgentMap.containsKey(sourceDataPermissionServiceAgentUid)) {
                            DataPermissionServiceAgentDo dataPermissionServiceAgentDo =
                                    this.dataPermissionServiceAgentRepository.findByUid(sourceDataPermissionServiceAgentUid);
                            if (dataPermissionServiceAgentDo == null) {
                                throw new AbcResourceIntegrityException("cannot find data permission service agent " + sourceDataPermissionServiceAgentUid);
                            }
                            dataPermissionServiceAgentMap.put(sourceDataPermissionServiceAgentUid,
                                    dataPermissionServiceAgentDo.getName());
                        }
                    }
                }
            }

            // filtering data field
            List<FilteringDataFieldDo> sourceFilteringDataFieldDoList =
                    this.filteringDataFieldRepository.findByDataFacetUid(sourceDataFacetUid,
                            Sort.by(Sort.Order.asc("id")));
            if (!CollectionUtils.isEmpty(sourceFilteringDataFieldDoList)) {
                for (FilteringDataFieldDo sourceFilteringDataFieldDo : sourceFilteringDataFieldDoList) {
                    // default value settings
                    if (sourceFilteringDataFieldDo.getDefaultValueSettings() != null &&
                            !sourceFilteringDataFieldDo.getDefaultValueSettings().isEmpty()) {
                        FilteringFieldDefaultValueSettings filteringFieldDefaultValueSettings =
                                JSONObject.toJavaObject(sourceFilteringDataFieldDo.getDefaultValueSettings(),
                                        FilteringFieldDefaultValueSettings.class);
                        Long sourceDictionaryCategoryUid =
                                filteringFieldDefaultValueSettings.getDictionaryCategoryUid();
                        if (sourceDictionaryCategoryUid != null) {
                            if (!dictionaryCategoryMap.containsKey(sourceDictionaryCategoryUid)) {
                                DictionaryCategoryDo dictionaryCategoryDo =
                                        this.dictionaryCategoryRepository.findByUid(sourceDictionaryCategoryUid);
                                if (dictionaryCategoryDo == null) {
                                    throw new AbcResourceIntegrityException("cannot find dictionary category " + sourceDictionaryCategoryUid);
                                }
                                dictionaryCategoryMap.put(sourceDictionaryCategoryUid, dictionaryCategoryDo.getName());
                            }
                        }
                    }

                    // cascading filtering settings
                    // optional value settings
                    if (sourceFilteringDataFieldDo.getFilteringTypeExtension() != null
                            && !sourceFilteringDataFieldDo.getFilteringTypeExtension().isEmpty()) {
                        switch (sourceFilteringDataFieldDo.getFilteringType()) {
                            case DROP_DOWN_LIST_SINGLE:
                            case DROP_DOWN_LIST_MULTIPLE:
                            case ASSOCIATING_SINGLE:
                            case ASSOCIATING_MULTIPLE: {
                                FilteringFieldOptionalValueSettings filteringFieldOptionalValueSettings =
                                        JSONObject.toJavaObject(sourceFilteringDataFieldDo.getFilteringTypeExtension(),
                                                FilteringFieldOptionalValueSettings.class);
                                Long sourceDictionaryCategoryUid =
                                        filteringFieldOptionalValueSettings.getDictionaryCategoryUid();
                                if (sourceDictionaryCategoryUid != null) {
                                    if (!dictionaryCategoryMap.containsKey(sourceDictionaryCategoryUid)) {
                                        DictionaryCategoryDo dictionaryCategoryDo =
                                                this.dictionaryCategoryRepository.findByUid(sourceDictionaryCategoryUid);
                                        if (dictionaryCategoryDo == null) {
                                            throw new AbcResourceIntegrityException("cannot find dictionary category " + sourceDictionaryCategoryUid);
                                        }
                                        dictionaryCategoryMap.put(sourceDictionaryCategoryUid, dictionaryCategoryDo.getName());
                                    }
                                }
                            }
                            break;
                            case CASCADING_DROP_DOWN_LIST_SINGLE:
                            case CASCADING_DROP_DOWN_LIST_MULTIPLE: {
                                FilteringFieldCascadingSettings cascadingFilteringSettings =
                                        JSONObject.toJavaObject(sourceFilteringDataFieldDo.getFilteringTypeExtension(),
                                                FilteringFieldCascadingSettings.class);
                                Long sourceDictionaryCategoryUid =
                                        cascadingFilteringSettings.getDictionaryCategoryUid();
                                if (sourceDictionaryCategoryUid != null) {
                                    if (!dictionaryCategoryMap.containsKey(sourceDictionaryCategoryUid)) {
                                        DictionaryCategoryDo dictionaryCategoryDo =
                                                this.dictionaryCategoryRepository.findByUid(sourceDictionaryCategoryUid);
                                        if (dictionaryCategoryDo == null) {
                                            throw new AbcResourceIntegrityException("cannot find dictionary category " + sourceDictionaryCategoryUid);
                                        }
                                        dictionaryCategoryMap.put(sourceDictionaryCategoryUid, dictionaryCategoryDo.getName());
                                    }
                                }
                            }
                            break;
                            default:
                                break;
                        }
                    }
                }
            }
        }

        PreparedMigrateOutDto preparedMigrateOutDto = new PreparedMigrateOutDto();
        preparedMigrateOutDto.setDataSourceMap(dataSourceMap);
        preparedMigrateOutDto.setDictionaryCategoryMap(dictionaryCategoryMap);
        preparedMigrateOutDto.setDfsServiceAgentMap(dfsServiceAgentMap);
        preparedMigrateOutDto.setDataPermissionServiceAgentMap(dataPermissionServiceAgentMap);

        return preparedMigrateOutDto;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Path startMigrateOut(
            StartMigrateOutDto startMigrateOutDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        if (CollectionUtils.isEmpty(startMigrateOutDto.getSourceDataFacetUidList())) {
            throw new AbcIllegalParameterException("source_data_facet_uid_list should not be null or empty");
        }

        List<DataFacetDo> sourceDataFacetDoList =
                this.dataFacetRepository.findByUidIn(startMigrateOutDto.getSourceDataFacetUidList());
        if (CollectionUtils.isEmpty(sourceDataFacetDoList)
                || sourceDataFacetDoList.size() != startMigrateOutDto.getSourceDataFacetUidList().size()) {
            throw new AbcIllegalParameterException("data mismatch, at least one source data facet cannot be found");
        }

        List<String> dataSourceUidMapping = startMigrateOutDto.getDataSourceUidMapping();
        Map<Long, Long> dataSourceUidMappingMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(dataSourceUidMapping)) {
            dataSourceUidMapping.forEach(tuple -> {
                String[] slices = tuple.split(",");
                dataSourceUidMappingMap.put(Long.valueOf(slices[0]), Long.valueOf(slices[1]));
            });
        }

        List<String> dictionaryCategoryUidMapping = startMigrateOutDto.getDictionaryCategoryUidMapping();
        Map<Long, Long> dictionaryCategoryUidMappingMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(dictionaryCategoryUidMapping)) {
            dictionaryCategoryUidMapping.forEach(tuple -> {
                String[] slices = tuple.split(",");
                dictionaryCategoryUidMappingMap.put(Long.valueOf(slices[0]), Long.valueOf(slices[1]));
            });
        }

        List<String> dfsServiceProviderUidMapping = startMigrateOutDto.getDfsServiceAgentUidMapping();
        Map<Long, Long> dfsServiceAgentUidMappingMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(dfsServiceProviderUidMapping)) {
            dfsServiceProviderUidMapping.forEach(tuple -> {
                String[] slices = tuple.split(",");
                dfsServiceAgentUidMappingMap.put(Long.valueOf(slices[0]), Long.valueOf(slices[1]));
            });
        }

        List<String> dataPermissionServiceAgentUidMapping = startMigrateOutDto.getDataPermissionServiceAgentUidMapping();
        Map<Long, Long> dataPermissionServiceAgentUidMappingMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(dataPermissionServiceAgentUidMapping)) {
            dataPermissionServiceAgentUidMapping.forEach(tuple -> {
                String[] slices = tuple.split(",");
                dataPermissionServiceAgentUidMappingMap.put(Long.valueOf(slices[0]), Long.valueOf(slices[1]));
            });
        }

        //
        //
        //
        Path directoryPath = Paths.get(
                this.projectDownloadPath, UUID.randomUUID().toString());
        directoryPath.toFile().mkdir();
        Path exportTemplatesDirectoryPath = Paths.get(directoryPath.toString(), "export_templates");
        exportTemplatesDirectoryPath.toFile().mkdir();

        MigrationDeploymentDto migrationDeploymentDto = new MigrationDeploymentDto();
        migrationDeploymentDto.setDataFacetWrapperList(new LinkedList<>());
        migrationDeploymentDto.setDataSourceUidMapping(dataSourceUidMappingMap);
        migrationDeploymentDto.setDictionaryCategoryUidMapping(dictionaryCategoryUidMappingMap);
        migrationDeploymentDto.setDfsServiceAgentUidMapping(dfsServiceAgentUidMappingMap);
        migrationDeploymentDto.setDataPermissionServiceAgentUidMapping(dataPermissionServiceAgentUidMappingMap);

        for (DataFacetDo sourceDataFacetDo : sourceDataFacetDoList) {
            // 目标
            DataFacetWrapperDto targetDataFacetWrapperDto = new DataFacetWrapperDto();
            migrationDeploymentDto.getDataFacetWrapperList().add(targetDataFacetWrapperDto);

            Long sourceDataFacetUid = sourceDataFacetDo.getUid();
            Long sourceDataTableUid = sourceDataFacetDo.getDataTableUid();
            Long sourceDataSourceUid = sourceDataFacetDo.getDataSourceUid();

            if (!dataSourceUidMappingMap.containsKey(sourceDataSourceUid)) {
                throw new AbcIllegalParameterException("cannot find data source uid mapping:" + sourceDataSourceUid);
            }

            // data table
            DataTableDo sourceDataTableDo = this.dataTableRepository.findByUid(sourceDataTableUid);
            targetDataFacetWrapperDto.setDataTableDo(sourceDataTableDo);

            // data widget
            List<DataWidgetDo> sourceDataWidgetDoList =
                    this.dataWidgetRepository.findByDataFacetUid(sourceDataFacetUid);
            targetDataFacetWrapperDto.setDataWidgetDoList(sourceDataWidgetDoList);

            // advanced feature
            AdvancedFeatureDo sourceAdvancedFeatureDo =
                    this.advancedFeatureRepository.findByDataFacetUid(sourceDataFacetUid);
            targetDataFacetWrapperDto.setAdvancedFeatureDo(sourceAdvancedFeatureDo);

            // data facet
            targetDataFacetWrapperDto.setDataFacetDo(sourceDataFacetDo);

            // data field
            List<DataFieldDo> sourceDataFieldDoList = this.dataFieldRepository.findByDataFacetUid(sourceDataFacetUid);
            if (!CollectionUtils.isEmpty(sourceDataFieldDoList)) {
                for (DataFieldDo sourceDataFieldDo : sourceDataFieldDoList) {
                    if (sourceDataFieldDo.getTypeExtension() != null
                            && !sourceDataFieldDo.getTypeExtension().isEmpty()) {
                        FieldTypeExtensionFile fieldTypeExtensionFile =
                                JSONObject.toJavaObject(sourceDataFieldDo.getTypeExtension(),
                                        FieldTypeExtensionFile.class);
                        Long sourceDfsServiceAgentUid = fieldTypeExtensionFile.getDfsServiceAgentUid();
                        if (sourceDfsServiceAgentUid != null) {
                            if (!dfsServiceAgentUidMappingMap.containsKey(sourceDfsServiceAgentUid)) {
                                throw new AbcIllegalParameterException("cannot find dfs service agent uid " +
                                        "mapping:" + sourceDfsServiceAgentUid);
                            }
                        }
                    }
                }
            }
            targetDataFacetWrapperDto.setDataFieldDoList(sourceDataFieldDoList);

            // data permission
            List<DataPermissionDo> sourceDataPermissionDoList =
                    this.dataPermissionRepository.findByDataFacetUid(sourceDataFacetUid);
            if (!CollectionUtils.isEmpty(sourceDataPermissionDoList)) {
                for (DataPermissionDo sourceDataPermissionDo : sourceDataPermissionDoList) {
                    DataPermissionContentDto dataPermissionContentDto = sourceDataPermissionDo.getContent();
                    Long sourceDataPermissionServiceAgentUid =
                            dataPermissionContentDto.getDataPermissionServiceAgentUid();

                    if (sourceDataPermissionServiceAgentUid != null) {
                        if (!dataPermissionServiceAgentUidMappingMap.containsKey(sourceDataPermissionServiceAgentUid)) {
                            throw new AbcIllegalParameterException("cannot find data permission service agent uid " +
                                    "mapping:" + sourceDataPermissionServiceAgentUid);
                        }
                    }
                }
            }
            targetDataFacetWrapperDto.setDataPermissionDoList(sourceDataPermissionDoList);

            // export basic
            ExportBasicDo sourceExportBasicDo = this.exportBasicRepository.findByDataFacetUid(sourceDataFacetUid);
            targetDataFacetWrapperDto.setExportBasicDo(sourceExportBasicDo);

            // export extended
            List<ExportExtendedTemplateDo> exportExtendedTemplateDoList =
                    this.exportExtendedTemplateRepository.findByDataFacetUid(sourceDataFacetUid);
            if (!CollectionUtils.isEmpty(exportExtendedTemplateDoList)) {
                targetDataFacetWrapperDto.setExportExtendedTemplateDoList(exportExtendedTemplateDoList);

                exportExtendedTemplateDoList.forEach(exportExtendedTemplateDo -> {
                    Long dfsServiceAgentUid = exportExtendedTemplateDo.getDfsServiceAgentUid();
                    String fileId = exportExtendedTemplateDo.getFileId();
                    if (!ObjectUtils.isEmpty(fileId)) {
                        File file = this.dfsServiceAgentService.downloadFile(dfsServiceAgentUid, fileId,
                                operatingUserProfile);

                        Path exportTemplateDirectoryPath = Paths.get(
                                exportTemplatesDirectoryPath.toString(), String.valueOf(exportExtendedTemplateDo.getUid()));
                        exportTemplateDirectoryPath.toFile().mkdir();
                        Path exportTemplateFilePath = Paths.get(exportTemplateDirectoryPath.toString(), file.getName());
                        try {
                            Files.move(file, exportTemplateFilePath.toFile());
                        } catch (IOException e) {
                            throw new AbcResourceConflictException("failed to migrate export extended template");
                        }
                    }

                });
            }

            // filtering data field
            List<FilteringDataFieldDo> sourceFilteringDataFieldDoList =
                    this.filteringDataFieldRepository.findByDataFacetUid(sourceDataFacetUid,
                            Sort.by(Sort.Order.asc("id")));
            if (!CollectionUtils.isEmpty(sourceFilteringDataFieldDoList)) {
                for (FilteringDataFieldDo sourceFilteringDataFieldDo : sourceFilteringDataFieldDoList) {
                    // default value settings
                    if (sourceFilteringDataFieldDo.getDefaultValueSettings() != null &&
                            !sourceFilteringDataFieldDo.getDefaultValueSettings().isEmpty()) {
                        FilteringFieldDefaultValueSettings filteringFieldDefaultValueSettings =
                                JSONObject.toJavaObject(sourceFilteringDataFieldDo.getDefaultValueSettings(),
                                        FilteringFieldDefaultValueSettings.class);
                        Long sourceDictionaryCategoryUid =
                                filteringFieldDefaultValueSettings.getDictionaryCategoryUid();
                        if (sourceDictionaryCategoryUid != null) {
                            if (!dictionaryCategoryUidMappingMap.containsKey(sourceDictionaryCategoryUid)) {
                                throw new AbcIllegalParameterException("cannot find dictionary category uid " +
                                        "mapping:" + sourceDictionaryCategoryUid);
                            }
                        }
                    }

                    // cascading filtering settings
                    // optional value settings
                    if (sourceFilteringDataFieldDo.getFilteringTypeExtension() != null
                            && !sourceFilteringDataFieldDo.getFilteringTypeExtension().isEmpty()) {
                        switch (sourceFilteringDataFieldDo.getFilteringType()) {
                            case DROP_DOWN_LIST_SINGLE:
                            case DROP_DOWN_LIST_MULTIPLE:
                            case ASSOCIATING_SINGLE:
                            case ASSOCIATING_MULTIPLE: {
                                FilteringFieldOptionalValueSettings filteringFieldOptionalValueSettings =
                                        JSONObject.toJavaObject(sourceFilteringDataFieldDo.getFilteringTypeExtension(),
                                                FilteringFieldOptionalValueSettings.class);
                                Long sourceDictionaryCategoryUid =
                                        filteringFieldOptionalValueSettings.getDictionaryCategoryUid();
                                if (sourceDictionaryCategoryUid != null) {
                                    if (!dictionaryCategoryUidMappingMap.containsKey(sourceDictionaryCategoryUid)) {
                                        throw new AbcIllegalParameterException("cannot find dictionary category uid " +
                                                "mapping:" + sourceDictionaryCategoryUid);
                                    }
                                }
                            }
                            break;
                            case CASCADING_DROP_DOWN_LIST_SINGLE:
                            case CASCADING_DROP_DOWN_LIST_MULTIPLE: {
                                FilteringFieldCascadingSettings cascadingFilteringSettings =
                                        JSONObject.toJavaObject(sourceFilteringDataFieldDo.getFilteringTypeExtension(),
                                                FilteringFieldCascadingSettings.class);
                                Long sourceDictionaryCategoryUid =
                                        cascadingFilteringSettings.getDictionaryCategoryUid();
                                if (sourceDictionaryCategoryUid != null) {
                                    if (!dictionaryCategoryUidMappingMap.containsKey(sourceDictionaryCategoryUid)) {
                                        throw new AbcIllegalParameterException("cannot find dictionary category uid " +
                                                "mapping:" + sourceDictionaryCategoryUid);
                                    }
                                }
                            }
                            break;
                            default:
                                break;
                        }
                    }
                }
            }
            targetDataFacetWrapperDto.setFilteringDataFieldDoList(sourceFilteringDataFieldDoList);

            // filtering extended
            FilteringExtendedDo sourceFilteringExtendedDo =
                    this.filteringExtendedRepository.findByDataFacetUid(sourceDataFacetUid);
            targetDataFacetWrapperDto.setFilteringExtendedDo(sourceFilteringExtendedDo);


            // listing data field
            List<ListingDataFieldDo> sourceListingDataFieldDoList =
                    this.listingDataFieldRepository.findByDataFacetUid(sourceDataFacetUid,
                            Sort.by(Sort.Order.asc("id")));
            targetDataFacetWrapperDto.setListingDataFieldDoList(sourceListingDataFieldDoList);

            // listing extended
            ListingExtendedDo sourceListingExtendedDo =
                    this.listingExtendedRepository.findByDataFacetUid(sourceDataFacetUid);
            targetDataFacetWrapperDto.setListingExtendedDo(sourceListingExtendedDo);


            // sorting data field
            List<SortingDataFieldDo> sourceSortingDataFieldDoList =
                    this.sortingDataFieldRepository.findByDataFacetUid(sourceDataFacetUid,
                            Sort.by(Sort.Order.asc("id")));
            targetDataFacetWrapperDto.setSortingDataFieldDoList(sourceSortingDataFieldDoList);

        }

        // generate file to download
        SerializeConfig serializeConfig = new SerializeConfig();
        serializeConfig.propertyNamingStrategy = PropertyNamingStrategy.SnakeCase;
        String deploymentAsJson = JSONObject.toJSONString(migrationDeploymentDto, serializeConfig,
                SerializerFeature.DisableCircularReferenceDetect);
        Path deploymentFilePath = Paths.get(directoryPath.toString(), "deployment.json");
        try {
            FileUtils.write(deploymentFilePath.toFile(), deploymentAsJson, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AbcResourceConflictException("failed to write deployment.json");
        }

        Path zipFilePath = Paths.get(this.projectDownloadPath, "migration.zip");
        AbcFileUtils.recursivelyZipDirectory(directoryPath.toString(), zipFilePath.toString());

        return zipFilePath;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long startMigrateIn(
            MigrationDeploymentDto migrationDeploymentDto,
            Path exportTemplatesDirectoryPath,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // step 1, pre-processing
        //

        //
        // step 2, core-processing
        //
        CreateDistributedTaskDto createDistributedTaskDto = new CreateDistributedTaskDto();

        createDistributedTaskDto.setName(String.format("%s_%s_%s",
                "migrate_in",
                LocalDateTime.now().format(this.narrowDateFormatter),
                LocalDateTime.now().format(this.narrowTimeFormatter)));

        JSONObject payload = new JSONObject();
        SerializeConfig serializeConfig = new SerializeConfig();
        serializeConfig.propertyNamingStrategy = PropertyNamingStrategy.SnakeCase;
        payload.put("deployment", JSONObject.toJSONString(migrationDeploymentDto, serializeConfig,
                SerializerFeature.DisableCircularReferenceDetect));
        payload.put("export_templates_directory_path", exportTemplatesDirectoryPath.toString());
        payload.put("operating_user_profile", JSONObject.toJSONString(operatingUserProfile, serializeConfig,
                SerializerFeature.DisableCircularReferenceDetect));
        createDistributedTaskDto.setPayload(payload);

        createDistributedTaskDto.setType(TASK_TYPE_MIGRATE_IN);
        createDistributedTaskDto.setHandlerName(TASK_HANDLER_NAME_MIGRATE_IN);

        DistributedTaskDto distributedTaskDto =
                this.taskService.createTask(createDistributedTaskDto, operatingUserProfile);

        //
        // step 3, post-processing
        //

        return distributedTaskDto.getUid();
    }

    @TaskHandler(type = TASK_TYPE_MIGRATE_IN, name = TASK_HANDLER_NAME_MIGRATE_IN)
    public void execute(
            Long taskUid,
            String taskName,
            JSONObject payload) throws AbcUndefinedException {
        //
        // step 1, pre-processing
        //

        long beginTime = System.currentTimeMillis();
        LOGGER.info("start migrate in:{}", payload.toJSONString());

        String migrationDeploymentDtoAsJSONString = payload.getString("deployment");
        MigrationDeploymentDto migrationDeploymentDto = JSONObject.parseObject(migrationDeploymentDtoAsJSONString,
                MigrationDeploymentDto.class);

        String exportTemplatesDirectoryPathAsString = payload.getString("export_templates_directory_path");
        Path exportTemplatesDirectoryPath = Paths.get(exportTemplatesDirectoryPathAsString);

        String operatingUserProfileAsJSONString = payload.getString("operating_user_profile");
        UserProfile operatingUserProfile = JSONObject.parseObject(operatingUserProfileAsJSONString, UserProfile.class);

        if (CollectionUtils.isEmpty(migrationDeploymentDto.getDataFacetWrapperList())) {
            throw new AbcIllegalParameterException("source data facet wrapper list is null or empty");
        }

        //
        // step 2, core-processing
        //
        Map<Long, DataSourceDo> targetDataSourceDoMap = new HashMap<>();
        Map<Long, DictionaryCategoryDo> targetDictionaryCategoryDoMap = new HashMap<>();
        Map<Long, DfsServiceAgentDo> targetDfsServiceAgentDoMap = new HashMap<>();
        Map<Long, DataPermissionServiceAgentDo> targetDataPermissionServiceAgentDoMap = new HashMap<>();

        if (!CollectionUtils.isEmpty(migrationDeploymentDto.getDataSourceUidMapping())) {
            for (Map.Entry<Long, Long> entry : migrationDeploymentDto.getDataSourceUidMapping().entrySet()) {
                Long targetDataSourceUid = entry.getValue();
                DataSourceDo targetDataSourceDo = this.dataSourceRepository.findByUid(targetDataSourceUid);
                if (targetDataSourceDo == null) {
                    throw new AbcIllegalParameterException("cannot find target data source " + targetDataSourceUid);
                }
                targetDataSourceDoMap.put(targetDataSourceUid, targetDataSourceDo);
            }
        }

        if (!CollectionUtils.isEmpty(migrationDeploymentDto.getDictionaryCategoryUidMapping())) {
            for (Map.Entry<Long, Long> entry : migrationDeploymentDto.getDictionaryCategoryUidMapping().entrySet()) {
                Long targetDictionaryCategoryUid = entry.getValue();
                DictionaryCategoryDo targetDictionaryCategoryDo =
                        this.dictionaryCategoryRepository.findByUid(targetDictionaryCategoryUid);
                if (targetDictionaryCategoryDo == null) {
                    throw new AbcIllegalParameterException("cannot find target dictionary category " + targetDictionaryCategoryUid);
                }
                targetDictionaryCategoryDoMap.put(targetDictionaryCategoryUid, targetDictionaryCategoryDo);
            }
        }

        if (!CollectionUtils.isEmpty(migrationDeploymentDto.getDfsServiceAgentUidMapping())) {
            for (Map.Entry<Long, Long> entry : migrationDeploymentDto.getDfsServiceAgentUidMapping().entrySet()) {
                Long targetDfsServiceAgentUid = entry.getValue();
                DfsServiceAgentDo targetDfsServiceAgentDo =
                        this.dfsServiceAgentRepository.findByUid(targetDfsServiceAgentUid);
                if (targetDfsServiceAgentDo == null) {
                    throw new AbcIllegalParameterException("cannot find target dfs service agent " + targetDfsServiceAgentUid);
                }
                targetDfsServiceAgentDoMap.put(targetDfsServiceAgentUid, targetDfsServiceAgentDo);
            }
        }

        if (!CollectionUtils.isEmpty(migrationDeploymentDto.getDataPermissionServiceAgentUidMapping())) {
            for (Map.Entry<Long, Long> entry : migrationDeploymentDto.getDataPermissionServiceAgentUidMapping().entrySet()) {
                Long targetDataPermissionServiceAgentUid = entry.getValue();
                DataPermissionServiceAgentDo targetDataPermissionServiceProviderDo =
                        this.dataPermissionServiceAgentRepository.findByUid(targetDataPermissionServiceAgentUid);
                if (targetDataPermissionServiceProviderDo == null) {
                    throw new AbcIllegalParameterException("cannot find target data permission service agent " + targetDataPermissionServiceAgentUid);
                }
                targetDataPermissionServiceAgentDoMap.put(targetDataPermissionServiceAgentUid, targetDataPermissionServiceProviderDo);
            }
        }

        for (DataFacetWrapperDto sourceDataFacetWrapperDto : migrationDeploymentDto.getDataFacetWrapperList()) {
            LOGGER.info("begin to copy source data facet: {} ({})",
                    sourceDataFacetWrapperDto.getDataFacetDo().getUid(),
                    sourceDataFacetWrapperDto.getDataFacetDo().getName());

            DataTableDo sourceDataTableDo = sourceDataFacetWrapperDto.getDataTableDo();
            DataFacetDo sourceDataFacetDo = sourceDataFacetWrapperDto.getDataFacetDo();

            Long sourceDataSourceUid = sourceDataFacetDo.getDataSourceUid();
            Long targetDataSourceUid = migrationDeploymentDto.getDataSourceUidMapping().get(sourceDataSourceUid);

            // data table
            DataTableDo targetDataTableDo = null;
            List<DataTableDo> targetDataTableDoList =
                    this.dataTableRepository.findByDataSourceUidAndName(targetDataSourceUid,
                            sourceDataTableDo.getName());
            if (CollectionUtils.isEmpty(targetDataTableDoList)) {
                throw new AbcIllegalParameterException(String.format("cannot find target data table from target data " +
                        "source" +
                        " %d and " +
                        "name %s", targetDataSourceUid, sourceDataTableDo.getName()));
            }
            for (DataTableDo candidateTargetDataTableDo : targetDataTableDoList) {
                if (CollectionUtils.isEmpty(candidateTargetDataTableDo.getContextPath())
                        && CollectionUtils.isEmpty(sourceDataTableDo.getContextPath())) {
                    targetDataTableDo = candidateTargetDataTableDo;
                    break;
                } else if (!CollectionUtils.isEmpty(candidateTargetDataTableDo.getContextPath())
                        && !CollectionUtils.isEmpty(sourceDataTableDo.getContextPath())
                        && candidateTargetDataTableDo.getContextPath().size() == sourceDataTableDo.getContextPath().size()) {
                    boolean matched = true;
                    for (int i = 0; i < candidateTargetDataTableDo.getContextPath().size(); i++) {
                        if (!candidateTargetDataTableDo.getContextPath().get(i).equalsIgnoreCase(sourceDataTableDo.getContextPath().get(i))) {
                            matched = false;
                            break;
                        }
                    }
                    if (matched) {
                        targetDataTableDo = candidateTargetDataTableDo;
                        break;
                    }
                }
            }
            if (targetDataTableDo == null) {
                throw new AbcIllegalParameterException(String.format("cannot find target data table from target data " +
                        "source" +
                        " %d and " +
                        "name %s", targetDataSourceUid, sourceDataTableDo.getName()));
            }
            Long targetDataTableUid = targetDataTableDo.getUid();


            // data facet
            DataFacetDo targetDataFacetDo = new DataFacetDo();
            BeanUtils.copyProperties(sourceDataFacetDo, targetDataFacetDo);
            targetDataFacetDo.setId(null);
            targetDataFacetDo.setUid(this.idHelper.getNextDistributedId(DataFacetDo.RESOURCE_NAME));
            targetDataFacetDo.setDataTableUid(targetDataTableUid);
            targetDataFacetDo.setDataSourceUid(targetDataSourceUid);
            //
            // targetDataFacetDo.setCreatedBy();
            targetDataFacetDo.setCreatedTimestamp(LocalDateTime.now());
            targetDataFacetDo.setLastModifiedTimestamp(LocalDateTime.now());
            //
            // targetDataFacetDo.setLastModifiedBy();
            this.dataFacetRepository.save(targetDataFacetDo);

            LOGGER.info("copied from source data facet: {} ({}) to target data facet: {} ({})",
                    sourceDataFacetWrapperDto.getDataFacetDo().getUid(),
                    sourceDataFacetWrapperDto.getDataFacetDo().getName(),
                    targetDataFacetDo.getUid(),
                    targetDataFacetDo.getName());

            // data widget
            if (!CollectionUtils.isEmpty(sourceDataFacetWrapperDto.getDataWidgetDoList())) {
                List<DataWidgetDo> targetDataWidgetDoList = new ArrayList<>(sourceDataFacetWrapperDto.getDataWidgetDoList().size());
                for (DataWidgetDo sourceDataWidgetDo : sourceDataFacetWrapperDto.getDataWidgetDoList()) {
                    DataWidgetDo targetDataWidgetDo = new DataWidgetDo();
                    BeanUtils.copyProperties(sourceDataWidgetDo, targetDataWidgetDo);
                    targetDataWidgetDo.setId(null);
                    targetDataWidgetDo.setUid(this.idHelper.getNextDistributedId(DataWidgetDo.RESOURCE_NAME));
                    targetDataWidgetDo.setDataFacetUid(targetDataFacetDo.getUid());
                    // targetDataWidgetDo.setCreatedBy();
                    targetDataWidgetDo.setCreatedTimestamp(LocalDateTime.now());
                    targetDataWidgetDo.setLastModifiedTimestamp(LocalDateTime.now());
                    // targetDataWidgetDo.setLastModifiedBy();

                    targetDataWidgetDoList.add(targetDataWidgetDo);
                }
                this.dataWidgetRepository.saveAll(targetDataWidgetDoList);
            }

            // advanced feature
            AdvancedFeatureDo targetAdvancedFeatureDo = new AdvancedFeatureDo();
            if (sourceDataFacetWrapperDto.getAdvancedFeatureDo() != null) {
                BeanUtils.copyProperties(sourceDataFacetWrapperDto.getAdvancedFeatureDo(), targetAdvancedFeatureDo);
                targetAdvancedFeatureDo.setId(null);
                targetAdvancedFeatureDo.setDataFacetUid(targetDataFacetDo.getUid());
                // targetAdvancedFeatureDo.setCreatedBy();
                targetAdvancedFeatureDo.setCreatedTimestamp(LocalDateTime.now());
                targetAdvancedFeatureDo.setLastModifiedTimestamp(LocalDateTime.now());
                // targetAdvancedFeatureDo.setLastModifiedBy();
                this.advancedFeatureRepository.save(targetAdvancedFeatureDo);
            }

            // data field
            if (!CollectionUtils.isEmpty(sourceDataFacetWrapperDto.getDataFieldDoList())) {
                List<DataFieldDo> targetDataFieldDoList =
                        new ArrayList<>(sourceDataFacetWrapperDto.getDataFieldDoList());
                sourceDataFacetWrapperDto.getDataFieldDoList().forEach(sourceDataFieldDo -> {
                    DataFieldDo targetDataFieldDo = new DataFieldDo();
                    BeanUtils.copyProperties(sourceDataFieldDo, targetDataFieldDo);
                    targetDataFieldDo.setId(null);
                    targetDataFieldDo.setUid(this.idHelper.getNextDistributedId(DataFieldDo.RESOURCE_NAME));
                    targetDataFieldDo.setDataFacetUid(targetDataFacetDo.getUid());
                    // targetDataFieldDo.setCreatedBy();
                    targetDataFieldDo.setCreatedTimestamp(LocalDateTime.now());
                    targetDataFieldDo.setLastModifiedTimestamp(LocalDateTime.now());
                    // targetDataFieldDo.setLastModifiedBy();

                    if (sourceDataFieldDo.getTypeExtension() != null &&
                            !sourceDataFieldDo.getTypeExtension().isEmpty()) {
                        FieldTypeExtensionFile fieldTypeExtensionFile =
                                JSONObject.toJavaObject(sourceDataFieldDo.getTypeExtension(),
                                        FieldTypeExtensionFile.class);
                        Long sourceDfsServiceAgentUid = fieldTypeExtensionFile.getDfsServiceAgentUid();
                        if (sourceDfsServiceAgentUid != null) {
                            Long targetDfsServiceProviderUid =
                                    migrationDeploymentDto.getDfsServiceAgentUidMapping().get(sourceDfsServiceAgentUid);
                            fieldTypeExtensionFile.setDfsServiceAgentUid(targetDfsServiceProviderUid);

                            targetDataFieldDo.setTypeExtension((JSONObject) JSONObject.toJSON(fieldTypeExtensionFile));
                        }
                    }

                    targetDataFieldDoList.add(targetDataFieldDo);
                });

                this.dataFieldRepository.saveAll(targetDataFieldDoList);
            }

            // data permission
            if (!CollectionUtils.isEmpty(sourceDataFacetWrapperDto.getDataPermissionDoList())) {
                List<DataPermissionDo> targetDataPermissionDoList =
                        new ArrayList<>(sourceDataFacetWrapperDto.getDataPermissionDoList().size());
                sourceDataFacetWrapperDto.getDataPermissionDoList().forEach(sourceDataPermissionDo -> {
                    DataPermissionDo targetDataPermissionDo = new DataPermissionDo();
                    BeanUtils.copyProperties(sourceDataPermissionDo, targetDataPermissionDo);
                    targetDataPermissionDo.setId(null);
                    targetDataPermissionDo.setUid(this.idHelper.getNextDistributedId(DataPermissionDo.RESOURCE_NAME));
                    targetDataPermissionDo.setDataFacetUid(targetDataFacetDo.getUid());
                    // targetDataPermissionDo.setCreatedBy();
                    targetDataPermissionDo.setCreatedTimestamp(LocalDateTime.now());
                    targetDataPermissionDo.setLastModifiedTimestamp(LocalDateTime.now());
                    // targetDataPermissionDo.setLastModifiedBy();

                    DataPermissionContentDto sourceDataPermissionContentDto = sourceDataPermissionDo.getContent();
                    Long sourceDataPermissionServiceAgentUid =
                            sourceDataPermissionContentDto.getDataPermissionServiceAgentUid();
                    if (sourceDataPermissionServiceAgentUid != null) {
                        Long targetDataPermissionServiceAgentUid =
                                migrationDeploymentDto.getDataPermissionServiceAgentUidMapping().get(sourceDataPermissionServiceAgentUid);

                        targetDataPermissionDo.getContent().setDataPermissionServiceAgentUid(targetDataPermissionServiceAgentUid);
                    }

                    targetDataPermissionDoList.add(targetDataPermissionDo);
                });

                this.dataPermissionRepository.saveAll(targetDataPermissionDoList);
            }

            // export basic
            if (sourceDataFacetWrapperDto.getExportBasicDo() != null) {
                ExportBasicDo targetExportBasicDo = new ExportBasicDo();
                BeanUtils.copyProperties(sourceDataFacetWrapperDto.getExportBasicDo(), targetExportBasicDo);
                targetExportBasicDo.setDataFacetUid(targetDataFacetDo.getUid());
                // targetExportBasicDo.setCreatedBy();
                targetExportBasicDo.setCreatedTimestamp(LocalDateTime.now());
                targetExportBasicDo.setLastModifiedTimestamp(LocalDateTime.now());
                // targetExportBasicDo.setLastModifiedBy();
                this.exportBasicRepository.save(targetExportBasicDo);
            }

            // export extended template
            if (!CollectionUtils.isEmpty(sourceDataFacetWrapperDto.getExportExtendedTemplateDoList())) {
                Map<String, File> exportTemplateFileMap = new HashMap<>();
                File[] exportTemplateDirectories = exportTemplatesDirectoryPath.toFile().listFiles();
                if (exportTemplateDirectories != null && exportTemplateDirectories.length > 0) {
                    for (File exportTemplateDirectory : exportTemplateDirectories) {
                        if (exportTemplateDirectory.isDirectory()) {
                            String name = exportTemplateDirectory.getName();
                            File[] exportTemplateFiles = exportTemplateDirectory.listFiles();
                            if (exportTemplateFiles != null && exportTemplateFiles.length == 1) {
                                exportTemplateFileMap.put(name, exportTemplateFiles[0]);
                            }
                        }
                    }
                }

                if (!exportTemplateFileMap.isEmpty()) {
                    DfsServiceAgentDto dfsServiceAgentDto =
                            this.dfsServiceAgentService.getPreferredDfsServiceAgent(operatingUserProfile);
                    if (dfsServiceAgentDto == null) {
                        throw new AbcResourceConflictException("cannot find preferred dfs service agent");
                    }

                    sourceDataFacetWrapperDto.getExportExtendedTemplateDoList().forEach(sourceExportExtendedTemplateDo -> {
                        ExportExtendedTemplateDo targetExportExtendedTemplateDo = new ExportExtendedTemplateDo();
                        BeanUtils.copyProperties(sourceExportExtendedTemplateDo, targetExportExtendedTemplateDo);

                        targetExportExtendedTemplateDo.setId(null);
                        targetExportExtendedTemplateDo.setUid(this.idHelper.getNextDistributedId(ExportExtendedTemplateDo.RESOURCE_NAME));
                        targetExportExtendedTemplateDo.setDataFacetUid(targetDataFacetDo.getUid());
                        // targetExportExtendedTemplateDo.setCreatedBy();
                        targetExportExtendedTemplateDo.setCreatedTimestamp(LocalDateTime.now());
                        targetExportExtendedTemplateDo.setLastModifiedTimestamp(LocalDateTime.now());
                        // targetExportExtendedTemplateDo.setLastModifiedBy();

                        File sourceExportTemplateFile =
                                exportTemplateFileMap.get(String.valueOf(sourceExportExtendedTemplateDo.getUid()));
                        if (sourceExportTemplateFile != null) {
                            String fileId = this.dfsServiceAgentService.uploadFile(dfsServiceAgentDto.getUid(),
                                    sourceExportTemplateFile,
                                    operatingUserProfile);
                            targetExportExtendedTemplateDo.setFileId(fileId);
                            targetExportExtendedTemplateDo.setDfsServiceAgentUid(dfsServiceAgentDto.getUid());
                        }
                    });
                }

                List<ExportExtendedTemplateDo> targetExportExtendedTemplateDoList =
                        new ArrayList<>(sourceDataFacetWrapperDto.getExportExtendedTemplateDoList());

            }


            // filtering data field
            if (!CollectionUtils.isEmpty(sourceDataFacetWrapperDto.getFilteringDataFieldDoList())) {
                List<FilteringDataFieldDo> targetFilteringDataFieldDoList =
                        new ArrayList<>(sourceDataFacetWrapperDto.getFilteringDataFieldDoList());
                sourceDataFacetWrapperDto.getFilteringDataFieldDoList().forEach(sourceFilteringDataFieldDo -> {
                    FilteringDataFieldDo targetFilteringDataFieldDo = new FilteringDataFieldDo();
                    BeanUtils.copyProperties(sourceFilteringDataFieldDo, targetFilteringDataFieldDo);
                    targetFilteringDataFieldDo.setId(null);
                    targetFilteringDataFieldDo.setDataFacetUid(targetDataFacetDo.getUid());
                    // targetFilteringDataFieldDo.setCreatedBy();
                    targetFilteringDataFieldDo.setCreatedTimestamp(LocalDateTime.now());
                    targetFilteringDataFieldDo.setLastModifiedTimestamp(LocalDateTime.now());
                    // targetFilteringDataFieldDo.setLastModifiedBy();

                    // default value settings
                    if (sourceFilteringDataFieldDo.getDefaultValueSettings() != null &&
                            !sourceFilteringDataFieldDo.getDefaultValueSettings().isEmpty()) {
                        FilteringFieldDefaultValueSettings filteringFieldDefaultValueSettings =
                                JSONObject.toJavaObject(sourceFilteringDataFieldDo.getDefaultValueSettings(),
                                        FilteringFieldDefaultValueSettings.class);
                        Long sourceDictionaryCategoryUid =
                                filteringFieldDefaultValueSettings.getDictionaryCategoryUid();
                        if (sourceDictionaryCategoryUid != null) {
                            Long targetDictionaryCategoryUid =
                                    migrationDeploymentDto.getDictionaryCategoryUidMapping().get(sourceDictionaryCategoryUid);
                            filteringFieldDefaultValueSettings.setDictionaryCategoryUid(targetDictionaryCategoryUid);

                            targetFilteringDataFieldDo.setDefaultValueSettings((JSONObject) JSONObject.toJSON(filteringFieldDefaultValueSettings));
                        }
                    }

                    // cascading filtering settings
                    // optional value settings
                    if (sourceFilteringDataFieldDo.getFilteringTypeExtension() != null
                            && !sourceFilteringDataFieldDo.getFilteringTypeExtension().isEmpty()) {
                        switch (sourceFilteringDataFieldDo.getFilteringType()) {
                            case DROP_DOWN_LIST_SINGLE:
                            case DROP_DOWN_LIST_MULTIPLE:
                            case ASSOCIATING_SINGLE:
                            case ASSOCIATING_MULTIPLE: {
                                FilteringFieldOptionalValueSettings filteringFieldOptionalValueSettings =
                                        JSONObject.toJavaObject(sourceFilteringDataFieldDo.getFilteringTypeExtension(),
                                                FilteringFieldOptionalValueSettings.class);
                                Long sourceDictionaryCategoryUid =
                                        filteringFieldOptionalValueSettings.getDictionaryCategoryUid();
                                if (sourceDictionaryCategoryUid != null) {
                                    Long targetDictionaryCategoryUid =
                                            migrationDeploymentDto.getDictionaryCategoryUidMapping().get(sourceDictionaryCategoryUid);
                                    filteringFieldOptionalValueSettings.setDictionaryCategoryUid(targetDictionaryCategoryUid);

                                    targetFilteringDataFieldDo.setFilteringTypeExtension((JSONObject) JSONObject.toJSON(filteringFieldOptionalValueSettings));
                                }
                            }
                            break;
                            case CASCADING_DROP_DOWN_LIST_SINGLE:
                            case CASCADING_DROP_DOWN_LIST_MULTIPLE: {
                                FilteringFieldCascadingSettings cascadingFilteringSettings =
                                        JSONObject.toJavaObject(sourceFilteringDataFieldDo.getFilteringTypeExtension(),
                                                FilteringFieldCascadingSettings.class);
                                Long sourceDictionaryCategoryUid =
                                        cascadingFilteringSettings.getDictionaryCategoryUid();
                                if (sourceDictionaryCategoryUid != null) {
                                    Long targetDictionaryCategoryUid =
                                            migrationDeploymentDto.getDictionaryCategoryUidMapping().get(sourceDictionaryCategoryUid);
                                    cascadingFilteringSettings.setDictionaryCategoryUid(targetDictionaryCategoryUid);

                                    targetFilteringDataFieldDo.setFilteringTypeExtension((JSONObject) JSONObject.toJSON(cascadingFilteringSettings));
                                }
                            }
                            break;
                            default:
                                break;
                        }
                    }

                    targetFilteringDataFieldDoList.add(targetFilteringDataFieldDo);
                });

                this.filteringDataFieldRepository.saveAll(targetFilteringDataFieldDoList);
            }

            // filtering extended
            if (sourceDataFacetWrapperDto.getFilteringExtendedDo() != null) {
                FilteringExtendedDo targetFilteringExtendedDo = new FilteringExtendedDo();
                BeanUtils.copyProperties(sourceDataFacetWrapperDto.getFilteringExtendedDo(), targetFilteringExtendedDo);
                targetFilteringExtendedDo.setDataFacetUid(targetDataFacetDo.getUid());
                // targetFilteringExtendedDo.setCreatedBy();
                targetFilteringExtendedDo.setCreatedTimestamp(LocalDateTime.now());
                targetFilteringExtendedDo.setLastModifiedTimestamp(LocalDateTime.now());
                // targetFilteringExtendedDo.setLastModifiedBy();

                this.filteringExtendedRepository.save(targetFilteringExtendedDo);
            }

            // listing data field
            if (!CollectionUtils.isEmpty(sourceDataFacetWrapperDto.getListingDataFieldDoList())) {
                List<ListingDataFieldDo> targetListingDataFieldDoList =
                        new ArrayList<>(sourceDataFacetWrapperDto.getListingDataFieldDoList());
                sourceDataFacetWrapperDto.getListingDataFieldDoList().forEach(sourceListingDataFieldDo -> {
                    ListingDataFieldDo targetListingDataFieldDo = new ListingDataFieldDo();
                    BeanUtils.copyProperties(sourceListingDataFieldDo, targetListingDataFieldDo);
                    targetListingDataFieldDo.setId(null);
                    targetListingDataFieldDo.setDataFacetUid(targetDataFacetDo.getUid());
                    // targetListingDataFieldDo.setCreatedBy();
                    targetListingDataFieldDo.setCreatedTimestamp(LocalDateTime.now());
                    targetListingDataFieldDo.setLastModifiedTimestamp(LocalDateTime.now());
                    // targetListingDataFieldDo.setLastModifiedBy();

                    targetListingDataFieldDoList.add(targetListingDataFieldDo);
                });

                this.listingDataFieldRepository.saveAll(targetListingDataFieldDoList);
            }

            // listing extended
            if (sourceDataFacetWrapperDto.getListingExtendedDo() != null) {
                ListingExtendedDo targetListingExtendedDo = new ListingExtendedDo();
                BeanUtils.copyProperties(sourceDataFacetWrapperDto.getListingExtendedDo(), targetListingExtendedDo);
                targetListingExtendedDo.setDataFacetUid(targetDataFacetDo.getUid());
                // targetListingExtendedDo.setCreatedBy();
                targetListingExtendedDo.setCreatedTimestamp(LocalDateTime.now());
                targetListingExtendedDo.setLastModifiedTimestamp(LocalDateTime.now());
                // targetListingExtendedDo.setLastModifiedBy();

                this.listingExtendedRepository.save(targetListingExtendedDo);
            }

            // sorting data field
            if (!CollectionUtils.isEmpty(sourceDataFacetWrapperDto.getSortingDataFieldDoList())) {
                List<SortingDataFieldDo> targetSortingDataFieldDoList =
                        new ArrayList<>(sourceDataFacetWrapperDto.getSortingDataFieldDoList());
                sourceDataFacetWrapperDto.getSortingDataFieldDoList().forEach(sourceSortingDataFieldDo -> {
                    SortingDataFieldDo targetSortingDataFieldDo = new SortingDataFieldDo();
                    BeanUtils.copyProperties(sourceSortingDataFieldDo, targetSortingDataFieldDo);
                    targetSortingDataFieldDo.setId(null);
                    targetSortingDataFieldDo.setDataFacetUid(targetDataFacetDo.getUid());
                    // targetSortingDataFieldDo.setCreatedBy();
                    targetSortingDataFieldDo.setCreatedTimestamp(LocalDateTime.now());
                    targetSortingDataFieldDo.setLastModifiedTimestamp(LocalDateTime.now());
                    // targetSortingDataFieldDo.setLastModifiedBy();

                    targetSortingDataFieldDoList.add(targetSortingDataFieldDo);
                });

                this.sortingDataFieldRepository.saveAll(targetSortingDataFieldDoList);
            }

            LOGGER.info("end to copy source data facet: {} ({})",
                    sourceDataFacetWrapperDto.getDataFacetDo().getUid(),
                    sourceDataFacetWrapperDto.getDataFacetDo().getName());
        }

        //
        // step 3, post-processing
        //
        LOGGER.info("end migrate in, duration:{}", AbcDateUtils.format(System.currentTimeMillis() - beginTime));
    }

    @Override
    public Long findTaskUidOfTheMigrateInTaskInProgress(UserProfile operatingUserProfile) throws AbcUndefinedException {
        DistributedTaskDto distributedTaskDto = findMigrateInTaskInProgress(operatingUserProfile);

        if (distributedTaskDto == null) {
            return null;
        } else {
            return distributedTaskDto.getUid();
        }
    }

    private DistributedTaskDto findMigrateInTaskInProgress(UserProfile operatingUserProfile) throws AbcUndefinedException {
        List<TaskStatusEnum> statuses = new LinkedList<>();
        statuses.add(TaskStatusEnum.CREATED);
        statuses.add(TaskStatusEnum.SCHEDULING);
        statuses.add(TaskStatusEnum.RUNNING);

        PageRequest pageable = PageRequest.of(0, 1, Sort.by(Sort.Order.desc(BaseDo.ID_FIELD_NAME)));
        Page<DistributedTaskDto> page = this.taskService.pagingQueryTasks(
                null,
                "migrate_in_",
                statuses,
                null,
                null,
                pageable,
                operatingUserProfile);

        if (page.isEmpty()) {
            return null;
        } else {
            return page.getContent().get(0);
        }
    }
}
