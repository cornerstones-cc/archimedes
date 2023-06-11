package cc.cornerstones.biz.datasource.service.impl;

import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcDateUtils;
import cc.cornerstones.biz.datafacet.share.constants.DataFieldTypeEnum;
import cc.cornerstones.biz.datasource.dto.DataSourceExportDto;
import cc.cornerstones.biz.datasource.entity.DataSourceDo;
import cc.cornerstones.biz.datasource.persistence.DataSourceRepository;
import cc.cornerstones.biz.datasource.service.assembly.database.DmlHandler;
import cc.cornerstones.biz.datasource.service.assembly.database.QueryBuilder;
import cc.cornerstones.biz.datasource.service.assembly.database.QueryResult;
import cc.cornerstones.biz.datasource.dto.DataSourceQueryDto;
import cc.cornerstones.biz.datasource.service.inf.ExecuteDataSourceService;
import cc.cornerstones.biz.distributedtask.dto.CreateDistributedTaskDto;
import cc.cornerstones.biz.distributedtask.dto.DistributedTaskDto;
import cc.cornerstones.biz.distributedtask.service.inf.DistributedTaskService;
import cc.cornerstones.biz.export.service.assembly.*;
import cc.cornerstones.biz.operations.accesslogging.dto.CreateOrUpdateQueryLogDto;
import cc.cornerstones.biz.operations.accesslogging.service.inf.AccessLoggingService;
import cc.cornerstones.biz.share.types.QueryContentResult;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

@Service
public class ExecuteDataSourceServiceImpl implements ExecuteDataSourceService {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ExecuteDataSourceServiceImpl.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private DistributedTaskService taskService;

    private final DateTimeFormatter narrowDateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final DateTimeFormatter narrowTimeFormatter = DateTimeFormatter.ofPattern("HHmmssSSS");

    @Autowired
    private AccessLoggingService accessLoggingService;

    @Override
    public QueryContentResult queryContent(
            Long dataSourceUid,
            DataSourceQueryDto dataSourceQueryDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DataSourceDo dataSourceDo = this.dataSourceRepository.findByUid(dataSourceUid);
        if (dataSourceDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataSourceDo.RESOURCE_SYMBOL,
                    dataSourceUid));
        }

        // tracking
        CreateOrUpdateQueryLogDto updateQueryLogDto = new CreateOrUpdateQueryLogDto();
        updateQueryLogDto.setTrackingSerialNumber(operatingUserProfile.getTrackingSerialNumber());
        updateQueryLogDto.setBeginTimestamp(LocalDateTime.now());
        if (!CollectionUtils.isEmpty(dataSourceQueryDto.getDataPermissionFilters())) {
            Object object = JSONObject.toJSON(dataSourceQueryDto.getDataPermissionFilters());
            if (object instanceof JSONObject) {
                JSONObject jsonObject = (JSONObject) object;
                updateQueryLogDto.setIntermediateResult(jsonObject);
            } else if (object instanceof JSONArray) {
                JSONArray jsonArray = (JSONArray) object;
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("data", jsonArray);
                updateQueryLogDto.setIntermediateResult(jsonObject);
            }

        }


        //
        // Step 2, core-processing
        //

        try {
            //
            // Step 2.1, build query statement
            //
            QueryBuilder objectiveQueryBuilder = null;
            Map<String, QueryBuilder> queryBuilderMap = this.applicationContext.getBeansOfType(QueryBuilder.class);
            if (!CollectionUtils.isEmpty(queryBuilderMap)) {
                for (Map.Entry<String, QueryBuilder> entry : queryBuilderMap.entrySet()) {
                    QueryBuilder queryBuilder = entry.getValue();
                    if (queryBuilder.type().equals(dataSourceDo.getType())) {
                        objectiveQueryBuilder = queryBuilder;
                        break;
                    }
                }
            }
            if (objectiveQueryBuilder == null) {
                throw new AbcResourceConflictException(
                        String.format("cannot find query builder of data source type:%s",
                                dataSourceDo.getType()));
            }

            String queryStatement = objectiveQueryBuilder.buildQueryStatement(dataSourceQueryDto);

            updateQueryLogDto.setQueryStatement(queryStatement);

            //
            // Step 2.2, execute query statement
            //
            DmlHandler objectiveDmlHandler = null;
            Map<String, DmlHandler> dmlHandlerMap = this.applicationContext.getBeansOfType(DmlHandler.class);
            if (!CollectionUtils.isEmpty(dmlHandlerMap)) {
                for (Map.Entry<String, DmlHandler> entry : dmlHandlerMap.entrySet()) {
                    DmlHandler dmlHandler = entry.getValue();
                    if (dmlHandler.type().equals(dataSourceDo.getType())) {
                        objectiveDmlHandler = dmlHandler;
                        break;
                    }
                }
            }
            if (objectiveDmlHandler == null) {
                throw new AbcResourceConflictException(
                        String.format("cannot find dml handler of data source type:%s",
                                dataSourceDo.getType()));
            }

            // tracking
            LocalDateTime beginQueryLocalDateTime = LocalDateTime.now();

            QueryResult queryResult = objectiveDmlHandler.executeQuery(dataSourceDo.getConnectionProfile(), queryStatement);

            // tracking
            LocalDateTime endQueryLocalDateTime = LocalDateTime.now();
            Duration queryDuration = Duration.between(beginQueryLocalDateTime, endQueryLocalDateTime);
            updateQueryLogDto.setQueryDurationInMillis(queryDuration.toMillis());
            updateQueryLogDto.setQueryDurationRemark(AbcDateUtils.format(queryDuration.toMillis()));

            //
            // Step 3, post-processing
            //
            if (dataSourceQueryDto.getPagination() != null) {
                // 分页查询
                QueryContentResult finalResult = new QueryContentResult();
                finalResult.setEnabledPagination(Boolean.TRUE);
                finalResult.setPageNumber(dataSourceQueryDto.getPagination().getPage());
                finalResult.setPageSize(dataSourceQueryDto.getPagination().getSize());

                if (queryResult != null) {
                    if (CollectionUtils.isEmpty(queryResult.getRows())) {
                        finalResult.setNumberOfElements(0);
                    } else {
                        finalResult.setNumberOfElements(queryResult.getRows().size());
                    }
                    finalResult.setContent(queryResult.getRows());
                    finalResult.setColumnNames(queryResult.getColumnNames());
                }

                // 1st page w/ total pages & total elements
                if (dataSourceQueryDto.getPagination().getPage() == 0) {
                    String countStatement = objectiveQueryBuilder.buildCountStatement(dataSourceQueryDto);

                    // tracking
                    updateQueryLogDto.setCountStatement(countStatement);
                    LocalDateTime beginCountLocalDateTime = LocalDateTime.now();

                    QueryResult countResult = objectiveDmlHandler.executeQuery(dataSourceDo.getConnectionProfile(),
                            countStatement);

                    // tracking
                    LocalDateTime endCountLocalDateTime = LocalDateTime.now();
                    Duration countDuration = Duration.between(beginCountLocalDateTime, endCountLocalDateTime);
                    updateQueryLogDto.setCountDurationInMillis(countDuration.toMillis());
                    updateQueryLogDto.setCountDurationRemark(AbcDateUtils.format(countDuration.toMillis()));

                    if (countResult != null
                            && !CollectionUtils.isEmpty(countResult.getRows())) {
                        Object cnt = countResult.getRows().get(0).get(QueryBuilder.COUNT_COLUMN_NAME);
                        if (cnt != null) {
                            if (cnt instanceof Integer) {
                                Integer cntAsInteger = (Integer) cnt;

                                finalResult.setTotalElements(cntAsInteger.longValue());
                                if (cntAsInteger % dataSourceQueryDto.getPagination().getSize() == 0) {
                                    finalResult.setTotalPages(
                                            cntAsInteger / dataSourceQueryDto.getPagination().getSize());
                                } else {
                                    finalResult.setTotalPages(cntAsInteger / dataSourceQueryDto.getPagination().getSize() + 1);
                                }
                            } else if (cnt instanceof Long) {
                                Long cntAsLong = (Long) cnt;

                                finalResult.setTotalElements(cntAsLong);
                                if (cntAsLong % dataSourceQueryDto.getPagination().getSize() == 0) {
                                    Long totalPagesAsLong = cntAsLong / dataSourceQueryDto.getPagination().getSize();
                                    finalResult.setTotalPages(totalPagesAsLong.intValue());
                                } else {
                                    Long totalPagesAsLong = cntAsLong / dataSourceQueryDto.getPagination().getSize() + 1;
                                    finalResult.setTotalPages(totalPagesAsLong.intValue());
                                }
                            } else {
                                LOGGER.error("unsupported data type, data value:{}", cnt);
                            }
                        }
                    } else {
                        finalResult.setTotalElements(0L);
                        finalResult.setTotalPages(0);
                    }
                }

                // tracking
                updateQueryLogDto.setSuccessful(Boolean.TRUE);
                if (finalResult.getColumnNames() == null) {
                    updateQueryLogDto.setTotalColumnsInSource(0);
                } else {
                    updateQueryLogDto.setTotalColumnsInSource(finalResult.getColumnNames().size());
                }
                updateQueryLogDto.setTotalRowsInSource(finalResult.getTotalElements());
                updateQueryLogDto.setResponse((JSONObject) JSONObject.toJSON(finalResult));

                return finalResult;
            } else {
                // 非分页查询
                QueryContentResult finalResult = new QueryContentResult();
                finalResult.setEnabledPagination(Boolean.FALSE);

                if (queryResult != null) {
                    finalResult.setContent(queryResult.getRows());
                    finalResult.setColumnNames(queryResult.getColumnNames());
                    if (CollectionUtils.isEmpty(queryResult.getRows())) {
                        finalResult.setNumberOfElements(0);
                    } else {
                        finalResult.setNumberOfElements(queryResult.getRows().size());
                    }
                }

                // tracking
                updateQueryLogDto.setSuccessful(Boolean.TRUE);
                if (finalResult.getColumnNames() == null) {
                    updateQueryLogDto.setTotalColumnsInSource(0);
                } else {
                    updateQueryLogDto.setTotalColumnsInSource(finalResult.getColumnNames().size());
                }
                if (finalResult.getNumberOfElements() != null) {
                    updateQueryLogDto.setTotalRowsInSource(Long.valueOf(finalResult.getNumberOfElements()));
                }
                updateQueryLogDto.setResponse((JSONObject) JSONObject.toJSON(finalResult));

                return finalResult;
            }
        } catch (Exception e) {
            updateQueryLogDto.setSuccessful(Boolean.FALSE);

            throw e;
        } finally {
            updateQueryLogDto.setEndTimestamp(LocalDateTime.now());
            updateQueryLogDto.setTotalDurationInMillis(Duration.between(updateQueryLogDto.getBeginTimestamp(),
                    updateQueryLogDto.getEndTimestamp()).toMillis());
            updateQueryLogDto.setTotalDurationRemark(AbcDateUtils.format(updateQueryLogDto.getTotalDurationInMillis()));
            this.accessLoggingService.updateQueryLog(updateQueryLogDto, operatingUserProfile);
        }
    }

    @Override
    public Long exportContent(
            String name,
            Long dataSourceUid,
            DataSourceExportDto dataSourceExportDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        //
        // Step 2, core-processing
        //
        CreateDistributedTaskDto createDistributedTaskDto = new CreateDistributedTaskDto();

        createDistributedTaskDto.setName(String.format("%s_%s_%s",
                name.replaceAll("_", "__")
                        .replaceAll("\\s", "_")
                        .toLowerCase(),
                LocalDateTime.now().format(this.narrowDateFormatter),
                LocalDateTime.now().format(this.narrowTimeFormatter)));

        switch (dataSourceExportDto.getExportOption()) {
            case EXPORT_CSV: {
                ExportCsvPayload payload = new ExportCsvPayload();
                payload.setDataSourceUid(dataSourceUid);
                payload.setDataSourceExport(dataSourceExportDto);
                payload.setOperatingUserProfile(operatingUserProfile);
                createDistributedTaskDto.setPayload((JSONObject) JSONObject.toJSON(payload));

                createDistributedTaskDto.setType(ExportCsvTaskHandler.TASK_TYPE_EXPORT);
                createDistributedTaskDto.setHandlerName(ExportCsvTaskHandler.TASK_HANDLER_NAME_EXPORT);
            }
            break;
            case EXPORT_EXCEL: {
                ExportExcelPayload payload = new ExportExcelPayload();
                payload.setDataSourceUid(dataSourceUid);
                payload.setDataSourceExport(dataSourceExportDto);
                payload.setOperatingUserProfile(operatingUserProfile);
                createDistributedTaskDto.setPayload((JSONObject) JSONObject.toJSON(payload));

                createDistributedTaskDto.setType(ExportExcelTaskHandler.TASK_TYPE_EXPORT);
                createDistributedTaskDto.setHandlerName(ExportExcelTaskHandler.TASK_HANDLER_NAME_EXPORT);
            }
            break;
            case EXPORT_AS_TEMPLATE: {
                ExportAsTemplatePayload payload = new ExportAsTemplatePayload();
                payload.setDataSourceUid(dataSourceUid);
                payload.setDataSourceExport(dataSourceExportDto);
                payload.setOperatingUserProfile(operatingUserProfile);
                payload.setExportExtendedTemplateFile(dataSourceExportDto.getExportExtendedTemplateFile());
                createDistributedTaskDto.setPayload((JSONObject) JSONObject.toJSON(payload));

                createDistributedTaskDto.setType(ExportAsTemplateTaskHandler.TASK_TYPE_EXPORT);
                createDistributedTaskDto.setHandlerName(ExportAsTemplateTaskHandler.TASK_HANDLER_NAME_EXPORT);
            }
            break;
            case EXPORT_CSV_W_ATTACHMENTS: {
                ExportCsvWithAttachmentsPayload payload = new ExportCsvWithAttachmentsPayload();
                payload.setDataSourceUid(dataSourceUid);
                payload.setDataSourceExport(dataSourceExportDto);
                payload.setOperatingUserProfile(operatingUserProfile);
                payload.setFileAttachmentList(dataSourceExportDto.getFileAttachmentList());
                payload.setImageAttachmentList(dataSourceExportDto.getImageAttachmentList());
                createDistributedTaskDto.setPayload((JSONObject) JSONObject.toJSON(payload));

                createDistributedTaskDto.setType(ExportCsvWithAttachmentsTaskHandler.TASK_TYPE_EXPORT);
                createDistributedTaskDto.setHandlerName(ExportCsvWithAttachmentsTaskHandler.TASK_HANDLER_NAME_EXPORT);
            }
            break;
            case EXPORT_EXCEL_W_ATTACHMENTS: {
                ExportExcelWithAttachmentsPayload payload = new ExportExcelWithAttachmentsPayload();
                payload.setDataSourceUid(dataSourceUid);
                payload.setDataSourceExport(dataSourceExportDto);
                payload.setOperatingUserProfile(operatingUserProfile);
                payload.setFileAttachmentList(dataSourceExportDto.getFileAttachmentList());
                payload.setImageAttachmentList(dataSourceExportDto.getImageAttachmentList());
                createDistributedTaskDto.setPayload((JSONObject) JSONObject.toJSON(payload));

                createDistributedTaskDto.setType(ExportExcelWithAttachmentsTaskHandler.TASK_TYPE_EXPORT);
                createDistributedTaskDto.setHandlerName(ExportExcelWithAttachmentsTaskHandler.TASK_HANDLER_NAME_EXPORT);
            }
            break;
            default:
                break;
        }

        DistributedTaskDto distributedTaskDto =
                this.taskService.createTask(createDistributedTaskDto, operatingUserProfile);

        //
        // Step 3, post-processing
        //

        return distributedTaskDto.getUid();
    }
}
