package cc.cornerstones.biz.export.service.assembly;

import cc.cornerstones.almond.constants.InfrastructureConstants;
import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.AbcTuple2;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcDateUtils;
import cc.cornerstones.almond.utils.AbcExcelUtils;
import cc.cornerstones.almond.utils.AbcObjectUtils;
import cc.cornerstones.biz.administration.usermanagement.dto.UserDto;
import cc.cornerstones.biz.administration.usermanagement.dto.UserOutlineDto;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.datafacet.share.constants.DataFieldTypeEnum;
import cc.cornerstones.biz.datasource.dto.DataSourceExportDto;
import cc.cornerstones.biz.datasource.dto.DataSourceQueryDto;
import cc.cornerstones.biz.datasource.entity.DataSourceDo;
import cc.cornerstones.biz.datasource.persistence.DataSourceRepository;
import cc.cornerstones.biz.datasource.service.assembly.database.DmlHandler;
import cc.cornerstones.biz.datasource.service.assembly.database.QueryBuilder;
import cc.cornerstones.biz.datasource.service.assembly.database.QueryResult;
import cc.cornerstones.biz.datasource.share.types.RowHandler;
import cc.cornerstones.biz.distributedtask.share.types.TaskHandler;
import cc.cornerstones.biz.export.entity.ExportTaskDo;
import cc.cornerstones.biz.export.persistence.ExportConfRepository;
import cc.cornerstones.biz.export.persistence.ExportTaskRepository;
import cc.cornerstones.biz.export.share.constants.ExportTaskStatusEnum;
import cc.cornerstones.biz.share.constants.ExportOptionEnum;
import cc.cornerstones.biz.share.types.ExpressionAggregateSelectionField;
import cc.cornerstones.biz.share.types.ExpressionSelectionField;
import cc.cornerstones.biz.share.types.PlainSelectionField;
import cc.cornerstones.biz.share.types.SelectionField;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class ExportExcelTaskHandler extends ExportTaskHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExportExcelTaskHandler.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private ExportTaskRepository exportTaskRepository;

    @Autowired
    private ExportConfRepository exportConfRepository;

    private static final String PROPERTY_NAME_EXCEL_WRITE_BATCH_SIZE = "excel.write.batch-size";

    public static final String TASK_TYPE_EXPORT = "export_excel";
    public static final String TASK_HANDLER_NAME_EXPORT = "export_excel";

    @Value("${private.dir.general.project.export}")
    private String projectExportPath;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    private Uploader uploader;

    @Autowired
    private UserService userService;

    @TaskHandler(type = TASK_TYPE_EXPORT, name = TASK_HANDLER_NAME_EXPORT)
    public void execute(
            Long taskUid,
            String taskName,
            JSONObject payload) throws AbcUndefinedException {
        ExportExcelPayload input = JSONObject.toJavaObject(payload, ExportExcelPayload.class);
        Long dataSourceUid = input.getDataSourceUid();
        DataSourceExportDto dataSourceExportDto = input.getDataSourceExport();
        UserProfile operatingUserProfile = input.getOperatingUserProfile();

        // tracking
        ExportTaskDo exportTaskDo = this.exportTaskRepository.findByTaskUid(taskUid);
        if (exportTaskDo != null) {
            LOGGER.info("reuse existing export task {}", exportTaskDo);
        } else {
            exportTaskDo = new ExportTaskDo();
            exportTaskDo.setTaskUid(taskUid);
            exportTaskDo.setTaskName(taskName);
            exportTaskDo.setCreatedDate(LocalDate.now());
            exportTaskDo.setDataFacetUid(dataSourceExportDto.getDataFacetUid());
            exportTaskDo.setDataFacetName(dataSourceExportDto.getDataFacetName());
        }

        exportTaskDo.setExportOption(ExportOptionEnum.EXPORT_EXCEL);
        exportTaskDo.setBeginTimestamp(LocalDateTime.now());

        if (operatingUserProfile == null) {
            exportTaskDo.setTaskStatus(ExportTaskStatusEnum.CANCELED);
            exportTaskDo.setRemark("no operating user profile");
            BaseDo.create(exportTaskDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
            this.exportTaskRepository.save(exportTaskDo);

            throw new AbcResourceConflictException("no operating user profile");
        } else {
            // 补充操作用户信息
            UserDto userDto = this.userService.getUser(operatingUserProfile.getUid());
            if (userDto != null) {
                exportTaskDo.setUserOutline(new UserOutlineDto());
                exportTaskDo.getUserOutline().setUid(userDto.getUid());
                exportTaskDo.getUserOutline().setDisplayName(userDto.getDisplayName());
                exportTaskDo.getUserOutline().setExtendedPropertyList(userDto.getExtendedPropertyList());
                exportTaskDo.getUserOutline().setAccountList(userDto.getAccountList());
                exportTaskDo.getUserOutline().setRoleList(userDto.getRoleList());
                exportTaskDo.getUserOutline().setGroupList(userDto.getGroupList());
            }

            // pre-check data permission granted
            if (Boolean.TRUE.equals(dataSourceExportDto.getRequireDataPermissionFilters())
                    && CollectionUtils.isEmpty(dataSourceExportDto.getDataPermissionFilters())) {
                exportTaskDo.setTaskStatus(ExportTaskStatusEnum.FINISHED);
                exportTaskDo.setEndTimestamp(LocalDateTime.now());
                exportTaskDo.setTotalDurationInSecs(0L);
                exportTaskDo.setTotalDurationRemark(AbcDateUtils.format(0L));
                exportTaskDo.setRemark("no data permission granted");

                BaseDo.create(exportTaskDo, operatingUserProfile.getUid(), LocalDateTime.now());
                this.exportTaskRepository.save(exportTaskDo);

                return;
            }

            exportTaskDo.setTaskStatus(ExportTaskStatusEnum.CREATED);

            BaseDo.create(exportTaskDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.exportTaskRepository.save(exportTaskDo);
        }

        try {
            //
            // Step 1, pre-processing
            //
            if (dataSourceUid == null) {
                throw new AbcIllegalParameterException("data_source_uid is required");
            }

            DataSourceDo dataSourceDo = this.dataSourceRepository.findByUid(dataSourceUid);
            if (dataSourceDo == null) {
                throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataSourceDo.RESOURCE_SYMBOL, dataSourceUid));
            }

            DataSourceQueryDto dataSourceQueryDto = new DataSourceQueryDto();
            BeanUtils.copyProperties(dataSourceExportDto, dataSourceQueryDto);

            //
            // Step 2, core-processing
            //

            //
            // Step 2.1, build count statement
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
            String countStatement = objectiveQueryBuilder.buildCountStatement(dataSourceQueryDto);

            //
            // Step 2.2, build query statement
            //
            String queryStatement = objectiveQueryBuilder.buildQueryStatement(dataSourceQueryDto);

            //
            // Step 2.3, tracking
            //
            handlePrepareEnd(taskUid, countStatement, queryStatement,
                    dataSourceQueryDto.getDataPermissionFilters(),
                    operatingUserProfile);

            //
            // Step 2.4, execute query statement
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

            export(objectiveDmlHandler, dataSourceDo, countStatement, queryStatement,
                    dataSourceQueryDto.getSelectionFields(), taskUid, taskName, operatingUserProfile);

            //
            // Step 3, post-processing
            //
        } catch (Exception e) {
            LOGGER.error("failed to execute export task ({}) {}", taskUid, taskName, e);

            handleFailed(taskUid, e.getMessage(), operatingUserProfile);
        }

    }

    private void export(
            DmlHandler dmlHandler,
            DataSourceDo dataSourceDo,
            String countStatement,
            String queryStatement,
            List<SelectionField> selectionFields,
            Long taskUid,
            String taskName,
            UserProfile operatingUserProfile) {
        //
        // Step 1, pre-processing
        //

        //
        // Step 2, core-processing
        //

        //
        // Step 2.1, count
        //
        int totalColumnsInSource = selectionFields.size();
        long totalRowsInSource = 0;
        try {
            QueryResult countResult = dmlHandler.executeQuery(dataSourceDo.getConnectionProfile(), countStatement);
            if (countResult != null
                    && !CollectionUtils.isEmpty(countResult.getColumnNames())
                    && !CollectionUtils.isEmpty(countResult.getRows())) {
                Map<String, Object> row0 = countResult.getRows().get(0);
                String column0 = countResult.getColumnNames().get(0);
                Object column0Value = row0.get(column0);
                totalRowsInSource = AbcObjectUtils.toInteger(column0Value);

                handleCountEnd(taskUid, totalRowsInSource, totalColumnsInSource, operatingUserProfile);
            }
            if (totalRowsInSource == 0) {
                handleFinished(taskUid, "0 rows", operatingUserProfile);
                return;
            }
        } catch (Exception e) {
            LOGGER.error("failed to count", e);
            handleFailed(taskUid, "failed to count", operatingUserProfile);
            return;
        }

        //
        // 阶段性检查是否需要取消任务
        //
        boolean shouldCancel = shouldCancel(taskUid, operatingUserProfile);
        if (shouldCancel) {
            return;
        }

        //
        // Step 2.2, query
        //
        try {
            export(dmlHandler, dataSourceDo, queryStatement, selectionFields, taskUid, taskName,
                    totalRowsInSource, operatingUserProfile);
        } catch (Exception e) {
            LOGGER.error("failed to export", e);
            handleFailed(taskUid, "failed to export", operatingUserProfile);
            return;
        }

        //
        // Step 3, post-processing
        //
    }

    private void export(
            DmlHandler dmlHandler,
            DataSourceDo dataSourceDo,
            String queryStatement,
            List<SelectionField> selectionFields,
            Long taskUid,
            String taskName,
            long totalRowsInSource,
            UserProfile operatingUserProfile) {
        //
        // Step 1, pre-processing
        //

        //
        // Step 1.1, 取 export to excel 的参数配置
        //
        int batchSizeOfFetch = 10000;
        String batchSizeOfFetchAsString =
                this.exportConfRepository.findByPropertyName(PROPERTY_NAME_EXCEL_WRITE_BATCH_SIZE);
        if (!ObjectUtils.isEmpty(batchSizeOfFetchAsString)) {
            try {
                int parsed = Integer.parseInt(batchSizeOfFetchAsString);
                if (parsed > 0) {
                    batchSizeOfFetch = parsed;
                }
            } catch (Exception e) {
                LOGGER.warn("unexpected {}, {}", PROPERTY_NAME_EXCEL_WRITE_BATCH_SIZE, batchSizeOfFetchAsString, e);
            }
        }
        // 从 source 中取数的最大限制，受 Excel 单个 Sheet 最大行数限制。目前不考虑 export to multiple sheets of a excel
        long maximumSizeOfFetch = totalRowsInSource;
        if (totalRowsInSource > AbcExcelUtils.EXCEL_SHEET_MAX_ROWS_COUNT) {
            maximumSizeOfFetch = AbcExcelUtils.EXCEL_SHEET_MAX_ROWS_COUNT;
        }
        // 为了控制写入文件 I/O 的次数，按 batch 写入文件，因此，计算所需 batch 的总数量
        long totalNumberOfBatches =
                maximumSizeOfFetch / batchSizeOfFetch + (maximumSizeOfFetch % batchSizeOfFetch == 0 ? 0 : 1);
        // 虽然是一次查询，但在写入文件时，仍然拆分成一个个 batch,
        List<List<Object>> batchOfRows = new LinkedList<>();

        //
        // Step 1.2, 本地文件名，本地文件路径
        //
        String fileName = String.format("%s.xlsx", taskName);
        Path filePath = Paths.get(this.projectExportPath, fileName);

        //
        // Step 1.3, 准备和 inner function 的数据交换桥梁
        //
        final Map<String, Boolean> booleanBridge = new HashMap<>();
        booleanBridge.put("query_end", false);
        booleanBridge.put("fetch_success", true);

        final Map<String, Integer> integerBridge = new HashMap<>();
        integerBridge.put("number_of_rows_read", 0);
        integerBridge.put("batch_index", 0);

        final Map<String, Long> longBridge = new HashMap<>();
        longBridge.put("total_write_duration_in_millis", 0L);

        //
        // Step 1.4, 利用 Alibaba EasyExcel 来实现导出数据到 Excel
        //
        ExcelWriter excelWriter = EasyExcel.write(filePath.toFile()).build();
        WriteSheet writeSheet = EasyExcel.writerSheet("sheet1").build();

        //
        // Step 1.5, 将标题行写入文件
        //
        Map<String, DataFieldTypeEnum> fieldTypeMap = new HashMap<>();
        List<String> headerRow = new LinkedList<>();
        // easy excel 支持 multiple rows of header，但此地没有这种需求
        List<List<String>> headerRows = new LinkedList<>();
        headerRows.add(headerRow);
        selectionFields.forEach(selectionField -> {
            switch (selectionField.getType()) {
                case PLAIN: {
                    PlainSelectionField plainSelectionField = JSONObject.toJavaObject(selectionField.getContent()
                            , PlainSelectionField.class);

                    // 补充字段信息
                    headerRow.add(plainSelectionField.getFieldLabel());
                    fieldTypeMap.put(plainSelectionField.getFieldName(), plainSelectionField.getFieldType());
                }
                break;
                case EXPRESSION: {
                    ExpressionSelectionField expressionSelectionField =
                            JSONObject.toJavaObject(selectionField.getContent(), ExpressionSelectionField.class);
                    switch (expressionSelectionField.getType()) {
                        case AGGREGATE_FUNCTION: {
                            ExpressionAggregateSelectionField expressionAggregateSelectionField =
                                    JSONObject.toJavaObject(expressionSelectionField.getContent(),
                                            ExpressionAggregateSelectionField.class);
                            headerRow.add(expressionAggregateSelectionField.getTargetFieldName());
                            // TODO
                        }
                        break;
                        case COMPOUND:
                            break;
                        case OPERATOR:
                            break;
                        case NON_AGGREGATE_FUNCTION:
                            break;
                        case PLACEHOLDER: {

                        }
                        break;
                        default:
                            throw new AbcResourceConflictException(String.format("unsupported expression selection" +
                                            " field type:%s",
                                    expressionSelectionField.getType()));
                    }
                }
                break;
                default:
                    throw new AbcResourceConflictException(String.format("unsupported selection field type:%s",
                            selectionField.getType()));
            }
        });

        try {
            excelWriter.write(headerRows, writeSheet);
        } catch (Exception e) {
            LOGGER.error("failed to export data to excel", e);

            handleFailed(taskUid, "failed to write header row", operatingUserProfile);

            if (excelWriter != null) {
                excelWriter.finish();
            }

            if (filePath.toFile().exists()) {
                filePath.toFile().delete();
            }
        }

        int totalColumnsInSource = headerRow.size();

        //
        // 阶段性检查是否需要取消任务
        //
        boolean shouldCancel = shouldCancel(taskUid, operatingUserProfile);
        if (shouldCancel) {
            if (excelWriter != null) {
                excelWriter.finish();
            }

            if (filePath.toFile().exists()) {
                filePath.toFile().delete();
            }

            return;
        }

        //
        // Step 2, core-processing
        //

        try {
            //
            // Step 2.1, execute query
            //
            ExcelWriter finalExcelWriter = excelWriter;
            int finalBatchSizeOfFetch = batchSizeOfFetch;
            long finalMaximumSizeOfFetch = maximumSizeOfFetch;
            dmlHandler.executeQuery(dataSourceDo.getConnectionProfile(), queryStatement, new RowHandler<Integer>() {
                @Override
                public Object transformColumnValue(Object columnValue, String columnLabel) {
                    return transform(columnValue, columnLabel, fieldTypeMap);
                }

                @Override
                public Integer process(List<List<Object>> rows, List<String> columnLabels) throws AbcUndefinedException {
                    //
                    // Step 2.2, 记录 query end
                    //
                    Boolean queryEnd = booleanBridge.get("query_end");
                    if (!Boolean.TRUE.equals(queryEnd)) {
                        handleQueryEnd(taskUid, operatingUserProfile);
                        booleanBridge.put("query_end", true);
                    }

                    //
                    // Step 2.3,  提取内容行
                    //
                    // 将 row 加入 batch
                    batchOfRows.addAll(rows);

                    // 已读行数 +
                    integerBridge.put("number_of_rows_read",
                            integerBridge.get("number_of_rows_read") + rows.size());

                    //
                    // Step 2.4, 检查一个写入 Batch 是否已满，如果已满，则将这个 batch 写入文件
                    //
                    if (batchOfRows.size() >= finalBatchSizeOfFetch
                            || integerBridge.get("number_of_rows_read").intValue() >= finalMaximumSizeOfFetch) {

                        long writeBeginTime = System.currentTimeMillis();

                        try {
                            finalExcelWriter.write(batchOfRows, writeSheet);
                        } catch (Exception e) {
                            LOGGER.error("failed to write to file:{}", filePath, e);
                            // 标记 fetch 失败
                            booleanBridge.put("fetch_success", false);
                            // 中断，不再 fetch
                            return -1;
                        }

                        long durationInMillis = System.currentTimeMillis() - writeBeginTime;
                        longBridge.put("total_write_duration_in_millis", longBridge.get(
                                "total_write_duration_in_millis") + durationInMillis);

                        batchOfRows.clear();

                        //
                        integerBridge.put("batch_index", integerBridge.get("batch_index") + 1);

                        //
                        Double fetchProgressPercentage = integerBridge.get("number_of_rows_read") * 100.0 / finalMaximumSizeOfFetch;

                        // fetch progress update
                        handleFetchProgressUpdate(taskUid, String.format("%d%%", fetchProgressPercentage.intValue()),
                                String.format("%d / %d", integerBridge.get("batch_index"), totalNumberOfBatches), operatingUserProfile);
                    }

                    //
                    // Step 2.5, 已经超出了最大取数大小限制，不再继续 fetch
                    //
                    if (integerBridge.get("number_of_rows_read").intValue() >= finalMaximumSizeOfFetch) {
                        LOGGER.warn("reach maximum fetch size limit {}, stop fetching", finalMaximumSizeOfFetch);
                        // 中断，不再 fetch
                        return -1;
                    }

                    //
                    // 阶段性检查是否需要取消任务
                    //
                    boolean shouldCancel = shouldCancel(taskUid, operatingUserProfile);
                    if (shouldCancel) {
                        return -1;
                    }

                    // 继续 fetch
                    return 0;
                }
            });

            // 根据 fetch 成功与否继续后续步骤
            Boolean fetchSuccess = booleanBridge.get("fetch_success");
            if (Boolean.TRUE.equals(fetchSuccess)) {
                //
                // Step 2.6, 完成 Fetch
                //
                excelWriter.finish();
                excelWriter = null;

                handleFetchEnd(taskUid, longBridge.get("total_write_duration_in_millis") / 1000,
                        maximumSizeOfFetch, filePath.toFile().length(), fileName, operatingUserProfile);

                //
                // 阶段性检查是否需要取消任务
                //
                shouldCancel = shouldCancel(taskUid, operatingUserProfile);
                if (shouldCancel) {
                    return;
                }

                //
                // Step 2.7, 开始 Transfer
                //
                AbcTuple2<Long, String> transferResult = this.uploader.upload(filePath.toFile(),
                        operatingUserProfile);
                handleTransferEnd(taskUid, transferResult.s, transferResult.f, operatingUserProfile);
            } else {
                excelWriter.finish();
                excelWriter = null;

                handleFailed(taskUid, "fetch failed", operatingUserProfile);
            }
        } catch (Exception e) {
            LOGGER.error("failed to export data to excel", e);

            handleFailed(taskUid, "failed to write body rows", operatingUserProfile);
        } finally {
            if (excelWriter != null) {
                excelWriter.finish();
            }

            if (filePath.toFile() != null) {
                filePath.toFile().delete();
            }
        }
    }
}
