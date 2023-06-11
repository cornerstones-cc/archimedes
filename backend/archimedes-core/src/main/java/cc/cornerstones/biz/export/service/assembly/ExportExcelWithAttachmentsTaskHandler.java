package cc.cornerstones.biz.export.service.assembly;

import cc.cornerstones.almond.constants.InfrastructureConstants;
import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.AbcTuple2;
import cc.cornerstones.almond.types.AbcTuple3;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcDateUtils;
import cc.cornerstones.almond.utils.AbcExcelUtils;
import cc.cornerstones.almond.utils.AbcFileUtils;
import cc.cornerstones.almond.utils.AbcObjectUtils;
import cc.cornerstones.biz.administration.systemsettings.entity.SettingsDo;
import cc.cornerstones.biz.administration.systemsettings.persistence.SettingsRepository;
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
import cc.cornerstones.biz.share.constants.NamingPolicyEnum;
import cc.cornerstones.biz.share.types.*;
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
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cc.cornerstones.biz.administration.systemsettings.service.impl.SystemSettingsServiceImpl.*;

@Component
public class ExportExcelWithAttachmentsTaskHandler extends ExportTaskHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExportExcelWithAttachmentsTaskHandler.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private ExportTaskRepository exportTaskRepository;

    @Autowired
    private ExportConfRepository exportConfRepository;

    private static final String PROPERTY_NAME_EXCEL_WRITE_BATCH_SIZE = "excel.write.batch-size";

    public static final String TASK_TYPE_EXPORT = "export_excel_w_attachments";
    public static final String TASK_HANDLER_NAME_EXPORT = "export_excel_w_attachments";

    @Value("${private.dir.general.project.export}")
    private String projectExportPath;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    private Downloader downloader;

    @Autowired
    private Uploader uploader;

    @Autowired
    private UserService userService;

    private final Pattern PATTERN = Pattern.compile("[\\s\\\\/:\\*\\?\\\"<>\\|]");

    @Autowired
    private SettingsRepository settingsRepository;

    @TaskHandler(type = TASK_TYPE_EXPORT, name = TASK_HANDLER_NAME_EXPORT)
    public void execute(
            Long taskUid,
            String taskName,
            JSONObject payload) throws AbcUndefinedException {
        ExportExcelWithAttachmentsPayload input = JSONObject.toJavaObject(payload,
                ExportExcelWithAttachmentsPayload.class);
        Long dataSourceUid = input.getDataSourceUid();
        DataSourceExportDto dataSourceExportDto = input.getDataSourceExport();
        UserProfile operatingUserProfile = input.getOperatingUserProfile();
        List<ExportAttachment> fileAttachmentList = input.getFileAttachmentList();
        List<ExportAttachment> imageAttachmentList = input.getImageAttachmentList();

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

        exportTaskDo.setExportOption(ExportOptionEnum.EXPORT_EXCEL_W_ATTACHMENTS);
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
                    dataSourceExportDto.getSelectionFields(), dataSourceExportDto.getVisibleSelectionFields(),
                    taskUid,
                    taskName, fileAttachmentList,
                    imageAttachmentList,
                    operatingUserProfile);

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
            List<SelectionField> visibleSelectionFields,
            Long taskUid,
            String taskName,
            List<ExportAttachment> fileAttachmentList,
            List<ExportAttachment> imageAttachmentList,
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
            export(dmlHandler, dataSourceDo, queryStatement, selectionFields, visibleSelectionFields,
                    taskUid, taskName,
                    totalRowsInSource, fileAttachmentList, imageAttachmentList, operatingUserProfile);
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
            List<SelectionField> visibleSelectionFields,
            Long taskUid,
            String taskName,
            long totalRowsInSource,
            List<ExportAttachment> fileAttachmentList,
            List<ExportAttachment> imageAttachmentList,
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
        String mainDirectoryName = taskName;
        Path mainDirectoryPath = Paths.get(this.projectExportPath, mainDirectoryName);
        if (!mainDirectoryPath.toFile().exists()) {
            mainDirectoryPath.toFile().mkdirs();
        }

        String dataFileName = String.format("%s.xlsx", taskName);
        Path dataFilePath = Paths.get(this.projectExportPath, mainDirectoryName, dataFileName);

        String fileAttachmentsDirectoryName = "files";
        Path fileAttachmentsDirectoryPath = Paths.get(this.projectExportPath, mainDirectoryName,
                fileAttachmentsDirectoryName);
        if (!fileAttachmentsDirectoryPath.toFile().exists()) {
            fileAttachmentsDirectoryPath.toFile().mkdirs();
        }

        String imageAttachmentsDirectoryName = "images";
        Path imageAttachmentsDirectoryPath = Paths.get(this.projectExportPath, mainDirectoryName, imageAttachmentsDirectoryName);
        if (!imageAttachmentsDirectoryPath.toFile().exists()) {
            imageAttachmentsDirectoryPath.toFile().mkdirs();
        }

        String zipFileName = String.format("%s.zip", taskName);
        Path zipFilePath = Paths.get(this.projectExportPath, zipFileName);


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
        ExcelWriter excelWriter = EasyExcel.write(dataFilePath.toFile()).build();
        WriteSheet writeSheet = EasyExcel.writerSheet("sheet1").build();
        File file = null;

        //
        // Step 1.5, 将标题行写入文件
        //
        List<String> visibleSelectionFieldNameList = new LinkedList<>();
        Map<String, DataFieldTypeEnum> fieldTypeMap = new HashMap<>();
        List<String> headerRow = new LinkedList<>();
        // easy excel 支持 multiple rows of header，但此地没有这种需求
        List<List<String>> headerRows = new LinkedList<>();
        headerRows.add(headerRow);
        visibleSelectionFields.forEach(selectionField -> {
            switch (selectionField.getType()) {
                case PLAIN: {
                    PlainSelectionField plainSelectionField = JSONObject.toJavaObject(selectionField.getContent()
                            , PlainSelectionField.class);

                    // 补充字段信息
                    headerRow.add(plainSelectionField.getFieldLabel());
                    fieldTypeMap.put(plainSelectionField.getFieldName(), plainSelectionField.getFieldType());
                    visibleSelectionFieldNameList.add(plainSelectionField.getFieldName());
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

            if (file != null) {
                file.delete();
            }
        }

        int totalColumnsInSource = headerRow.size();

        //
        // Step 1.6, 准备待导出的附件
        //
        Map<String, ExportAttachment> fileAttachmentMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(fileAttachmentList)) {
            fileAttachmentList.forEach(exportAttachment -> {
                fileAttachmentMap.put(exportAttachment.getColumnName(), exportAttachment);
            });
        }
        Map<String, ExportAttachment> imageAttachmentMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(imageAttachmentList)) {
            imageAttachmentList.forEach(exportAttachment -> {
                imageAttachmentMap.put(exportAttachment.getColumnName(), exportAttachment);
            });
        }

        //
        // 阶段性检查是否需要取消任务
        //
        boolean shouldCancel = shouldCancel(taskUid, operatingUserProfile);
        if (shouldCancel) {
            if (excelWriter != null) {
                excelWriter.finish();
            }

            if (file != null) {
                file.delete();
            }

            return;
        }

        //
        // Step 2, core-processing
        //

        // 收集待导出附件（文件）
        List<AbcTuple3<String, String, String>> fileUrlList = new LinkedList<>();
        List<AbcTuple3<String, String, String>> filePathList = new LinkedList<>();
        Map<Long, List<AbcTuple3<String, String, String>>> fileDfsServiceAgentUidAndFileIdListMap = new HashMap<>();

        // 收集待导出附件（图片）
        List<AbcTuple3<String, String, String>> imageUrlList = new LinkedList<>();
        List<AbcTuple3<String, String, String>> imagePathList = new LinkedList<>();
        Map<Long, List<AbcTuple3<String, String, String>>> imageDfsServiceAgentUidAndFileIdListMap = new HashMap<>();

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
                    List<List<Object>> visibleRows = new LinkedList<>();
                    for (int rowNo = 0; rowNo < rows.size(); rowNo++) {
                        List<Object> row = rows.get(rowNo);
                        Map<String, Object> rowAsMap = new HashMap<>();

                        List<Object> visibleRow = new LinkedList<>();
                        visibleRows.add(visibleRow);

                        for (int columnNo = 0; columnNo < columnLabels.size(); columnNo++) {
                            String columnLabel = columnLabels.get(columnNo);

                            if (visibleSelectionFieldNameList.contains(columnLabel)) {
                                visibleRow.add(row.get(columnNo));
                            }

                            rowAsMap.put(columnLabel, row.get(columnNo));
                        }

                        //
                        // prepare for attachment
                        for (int columnNo = 0; columnNo < columnLabels.size(); columnNo++) {
                            String columnLabel = columnLabels.get(columnNo);
                            Object columnValue = rowAsMap.get(columnLabel);

                            if (ObjectUtils.isEmpty(columnValue)) {
                                continue;
                            }

                            if (fileAttachmentMap.containsKey(columnLabel)) {
                                ExportAttachment exportAttachment = fileAttachmentMap.get(columnLabel);
                                switch (exportAttachment.getSource().getSettingsMode()) {
                                    case HTTP_FULL_URL: {
                                        String url = String.valueOf(columnValue);
                                        List<AbcTuple3<String, String, String>> tuples = prepareAttachment(url,
                                                rowAsMap,
                                                exportAttachment,
                                                fileAttachmentsDirectoryPath);
                                        if (!CollectionUtils.isEmpty(tuples)) {
                                            fileUrlList.addAll(tuples);
                                        }
                                    }
                                    break;
                                    case HTTP_RELATIVE_URL: {
                                        String url =
                                                exportAttachment.getSource().getPrefixForHttpRelativeUrl() + String.valueOf(columnValue);
                                        List<AbcTuple3<String, String, String>> tuples = prepareAttachment(url,
                                                rowAsMap,
                                                exportAttachment,
                                                fileAttachmentsDirectoryPath);
                                        if (!CollectionUtils.isEmpty(tuples)) {
                                            fileUrlList.addAll(tuples);
                                        }
                                    }
                                    break;
                                    case FILE_ABSOLUTE_LOCAL_PATH: {
                                        String filePath = String.valueOf(columnValue);
                                        List<AbcTuple3<String, String, String>> tuples = prepareAttachment(filePath,
                                                rowAsMap,
                                                exportAttachment,
                                                fileAttachmentsDirectoryPath);
                                        if (!CollectionUtils.isEmpty(tuples)) {
                                            filePathList.addAll(tuples);
                                        }
                                    }
                                    break;
                                    case FILE_RELATIVE_LOCAL_PATH: {
                                        String filePath = exportAttachment.getSource().getPrefixForFileRelativeLocalPath() + String.valueOf(columnValue);
                                        List<AbcTuple3<String, String, String>> tuples = prepareAttachment(filePath,
                                                rowAsMap,
                                                exportAttachment,
                                                fileAttachmentsDirectoryPath);
                                        if (!CollectionUtils.isEmpty(tuples)) {
                                            filePathList.addAll(tuples);
                                        }
                                    }
                                    break;
                                    case DFS_FILE: {
                                        String fileId = String.valueOf(columnValue);
                                        List<AbcTuple3<String, String, String>> tuples = prepareAttachment(fileId,
                                                rowAsMap,
                                                exportAttachment,
                                                fileAttachmentsDirectoryPath);
                                        if (!CollectionUtils.isEmpty(tuples)) {
                                            if (!fileDfsServiceAgentUidAndFileIdListMap.containsKey(exportAttachment.getSource().getDfsServiceAgentUid())) {
                                                fileDfsServiceAgentUidAndFileIdListMap.put(exportAttachment.getSource().getDfsServiceAgentUid(), new LinkedList<>());
                                            }
                                            fileDfsServiceAgentUidAndFileIdListMap.get(exportAttachment.getSource().getDfsServiceAgentUid()).addAll(tuples);
                                        }
                                    }
                                    break;
                                    default:
                                        break;
                                }
                            }
                            if (imageAttachmentMap.containsKey(columnLabel)) {
                                ExportAttachment exportAttachment = imageAttachmentMap.get(columnLabel);
                                switch (exportAttachment.getSource().getSettingsMode()) {
                                    case HTTP_FULL_URL: {
                                        String url = String.valueOf(columnValue);
                                        List<AbcTuple3<String, String, String>> tuples = prepareAttachment(url,
                                                rowAsMap,
                                                exportAttachment,
                                                imageAttachmentsDirectoryPath);
                                        if (!CollectionUtils.isEmpty(tuples)) {
                                            imageUrlList.addAll(tuples);
                                        }
                                    }
                                    break;
                                    case HTTP_RELATIVE_URL: {
                                        String url =
                                                exportAttachment.getSource().getPrefixForHttpRelativeUrl() + String.valueOf(columnValue);
                                        List<AbcTuple3<String, String, String>> tuples = prepareAttachment(url,
                                                rowAsMap,
                                                exportAttachment,
                                                imageAttachmentsDirectoryPath);
                                        if (!CollectionUtils.isEmpty(tuples)) {
                                            imageUrlList.addAll(tuples);
                                        }
                                    }
                                    break;
                                    case FILE_ABSOLUTE_LOCAL_PATH: {
                                        String filePath = String.valueOf(columnValue);
                                        List<AbcTuple3<String, String, String>> tuples = prepareAttachment(filePath,
                                                rowAsMap,
                                                exportAttachment,
                                                imageAttachmentsDirectoryPath);
                                        if (!CollectionUtils.isEmpty(tuples)) {
                                            imagePathList.addAll(tuples);
                                        }
                                    }
                                    break;
                                    case FILE_RELATIVE_LOCAL_PATH: {
                                        String filePath =
                                                Paths.get(exportAttachment.getSource().getPrefixForFileRelativeLocalPath(), String.valueOf(columnValue)).toString();
                                        List<AbcTuple3<String, String, String>> tuples = prepareAttachment(filePath,
                                                rowAsMap,
                                                exportAttachment,
                                                imageAttachmentsDirectoryPath);
                                        if (!CollectionUtils.isEmpty(tuples)) {
                                            imagePathList.addAll(tuples);
                                        }
                                    }
                                    break;
                                    case DFS_FILE: {
                                        String fileId = String.valueOf(columnValue);
                                        List<AbcTuple3<String, String, String>> tuples = prepareAttachment(fileId,
                                                rowAsMap,
                                                exportAttachment,
                                                imageAttachmentsDirectoryPath);
                                        if (!CollectionUtils.isEmpty(tuples)) {
                                            if (!imageDfsServiceAgentUidAndFileIdListMap.containsKey(exportAttachment.getSource().getDfsServiceAgentUid())) {
                                                imageDfsServiceAgentUidAndFileIdListMap.put(exportAttachment.getSource().getDfsServiceAgentUid(), new LinkedList<>());
                                            }
                                            imageDfsServiceAgentUidAndFileIdListMap.get(exportAttachment.getSource().getDfsServiceAgentUid()).addAll(tuples);
                                        }
                                    }
                                    break;
                                    default:
                                        break;
                                }
                            }
                        }
                    }

                    // 将 row 加入 batch
                    batchOfRows.addAll(visibleRows);

                    // 已读行数 +
                    integerBridge.put("number_of_rows_read",
                            integerBridge.get("number_of_rows_read") + visibleRows.size());

                    //
                    // Step 2.4, 检查一个写入 Batch 是否已满，如果已满，则将这个 batch 写入文件
                    //
                    if (batchOfRows.size() >= finalBatchSizeOfFetch
                            || integerBridge.get("number_of_rows_read").intValue() >= finalMaximumSizeOfFetch) {

                        long writeBeginTime = System.currentTimeMillis();

                        try {
                            finalExcelWriter.write(batchOfRows, writeSheet);
                        } catch (Exception e) {
                            LOGGER.error("failed to write to file:{}", dataFilePath, e);
                            // 标记 fetch 失败
                            booleanBridge.put("fetch_success", false);
                            // 中断，不再 fetch
                            return -1;
                        }

                        long durationInMillis = System.currentTimeMillis() - writeBeginTime;
                        longBridge.put("total_write_duration_in_millis", longBridge.get(
                                "total_write_duration_in_millis") + durationInMillis);

                        batchOfRows.clear();

                        visibleRows.clear();

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
                        maximumSizeOfFetch, dataFilePath.toFile().length(), dataFileName, operatingUserProfile);

                //
                // 阶段性检查是否需要取消任务
                //
                shouldCancel = shouldCancel(taskUid, operatingUserProfile);
                if (shouldCancel) {
                    return;
                }

                //
                // Step 2.7, 导出附件
                //
                // 考虑导出文件&图片数量限制
                Integer maximumNumberOfFilesExportedByOneExportTask =
                        getMaximumNumberOfFilesExportedByOneExportTask();
                int numberOfFiles = 0;
                if (!CollectionUtils.isEmpty(fileUrlList)) {
                    for (AbcTuple3<String, String, String> url : fileUrlList) {
                        if (numberOfFiles >= maximumNumberOfFilesExportedByOneExportTask) {
                            LOGGER.warn("reaches the maximum number limit of files exported by one export task {}, " +
                                            "stop downloading more files",
                                    maximumNumberOfFilesExportedByOneExportTask);
                            break;
                        }

                        this.downloader.downloadByUrl(url);

                        numberOfFiles++;
                    }
                }
                if (!CollectionUtils.isEmpty(filePathList)) {
                    for (AbcTuple3<String, String, String> path : filePathList) {
                        if (numberOfFiles >= maximumNumberOfFilesExportedByOneExportTask) {
                            LOGGER.warn("reaches the maximum number limit of files exported by one export task {}, " +
                                            "stop downloading more files",
                                    maximumNumberOfFilesExportedByOneExportTask);
                            break;
                        }

                        this.downloader.moveFile(path);

                        numberOfFiles++;
                    }
                }
                if (!CollectionUtils.isEmpty(fileDfsServiceAgentUidAndFileIdListMap)) {
                    for (Map.Entry<Long, List<AbcTuple3<String, String, String>>> entry :
                            fileDfsServiceAgentUidAndFileIdListMap.entrySet()) {
                        Long dfsServiceAgentUid = entry.getKey();
                        List<AbcTuple3<String, String, String>> fileIdList = entry.getValue();

                        if (maximumNumberOfFilesExportedByOneExportTask < numberOfFiles + fileIdList.size()) {
                            int quota = maximumNumberOfFilesExportedByOneExportTask - numberOfFiles;
                            List<AbcTuple3<String, String, String>> allowedFileIdList = fileIdList.subList(0, quota);

                            this.downloader.downloadByFileId(allowedFileIdList, dfsServiceAgentUid);

                            numberOfFiles += allowedFileIdList.size();

                            LOGGER.warn("reaches the maximum number limit of files exported by one export task {}, " +
                                            "stop downloading more files",
                                    maximumNumberOfFilesExportedByOneExportTask);

                            break;
                        } else if (maximumNumberOfFilesExportedByOneExportTask.equals(numberOfFiles + fileIdList.size())) {
                            this.downloader.downloadByFileId(fileIdList, dfsServiceAgentUid);

                            numberOfFiles += fileIdList.size();

                            LOGGER.warn("reaches the maximum number limit of files exported by one export task {}, " +
                                            "stop downloading more files",
                                    maximumNumberOfFilesExportedByOneExportTask);

                            break;
                        } else {
                            this.downloader.downloadByFileId(fileIdList, dfsServiceAgentUid);

                            numberOfFiles += fileIdList.size();
                        }
                    }
                }

                Integer maximumNumberOfImagesExportedByOneExportTask =
                        getMaximumNumberOfImagesExportedByOneExportTask();
                int numberOfImages = 0;
                if (!CollectionUtils.isEmpty(imageUrlList)) {
                    for (AbcTuple3<String, String, String> url : imageUrlList) {
                        if (numberOfImages >= maximumNumberOfImagesExportedByOneExportTask) {
                            LOGGER.warn("reaches the maximum number limit of images exported by one export task {}, " +
                                            "stop downloading more images",
                                    maximumNumberOfImagesExportedByOneExportTask);
                            break;
                        }

                        this.downloader.downloadByUrl(url);

                        numberOfImages++;
                    }
                }
                if (!CollectionUtils.isEmpty(imagePathList)) {
                    for (AbcTuple3<String, String, String> path : imagePathList) {
                        if (numberOfImages >= maximumNumberOfImagesExportedByOneExportTask) {
                            LOGGER.warn("reaches the maximum number limit of images exported by one export task {}, " +
                                            "stop downloading more images",
                                    maximumNumberOfImagesExportedByOneExportTask);
                            break;
                        }

                        this.downloader.moveFile(path);

                        numberOfImages++;
                    }
                }
                if (!CollectionUtils.isEmpty(imageDfsServiceAgentUidAndFileIdListMap)) {
                    for (Map.Entry<Long, List<AbcTuple3<String, String, String>>> entry :
                            imageDfsServiceAgentUidAndFileIdListMap.entrySet()) {
                        Long dfsServiceAgentUid = entry.getKey();
                        List<AbcTuple3<String, String, String>> fileIdList = entry.getValue();

                        if (maximumNumberOfImagesExportedByOneExportTask < numberOfImages + fileIdList.size()) {
                            int quota = maximumNumberOfImagesExportedByOneExportTask - numberOfImages;
                            List<AbcTuple3<String, String, String>> allowedFileIdList = fileIdList.subList(0, quota);

                            this.downloader.downloadByFileId(allowedFileIdList, dfsServiceAgentUid);

                            numberOfImages += allowedFileIdList.size();

                            LOGGER.warn("reaches the maximum number limit of images exported by one export task {}, " +
                                            "stop downloading more images",
                                    maximumNumberOfImagesExportedByOneExportTask);

                            break;
                        } else if (maximumNumberOfImagesExportedByOneExportTask.equals(numberOfImages + fileIdList.size())) {
                            this.downloader.downloadByFileId(fileIdList, dfsServiceAgentUid);

                            numberOfImages += fileIdList.size();

                            LOGGER.warn("reaches the maximum number limit of images exported by one export task {}, " +
                                            "stop downloading more images",
                                    maximumNumberOfImagesExportedByOneExportTask);

                            break;
                        } else {
                            this.downloader.downloadByFileId(fileIdList, dfsServiceAgentUid);

                            numberOfImages += fileIdList.size();
                        }
                    }
                }

                //
                // Step 2.8, 打包文件
                //
                AbcFileUtils.recursivelyZipDirectory(mainDirectoryPath.toString(), zipFilePath.toString());

                //
                // Step 2.9, 开始 Transfer
                //
                AbcTuple2<Long, String> transferResult = this.uploader.upload(zipFilePath.toFile(), operatingUserProfile);
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

            AbcFileUtils.recursivelyDeleteFile(mainDirectoryPath);

            if (zipFilePath.toFile().exists()) {
                zipFilePath.toFile().delete();
            }
        }
    }

    private List<AbcTuple3<String, String, String>> prepareAttachment(
            String columnValue,
            Map<String, Object> columnValueMap,
            ExportAttachment exportAttachment,
            Path directoryPath) {
        if (columnValue == null) {
            return null;
        }

        // 为了兼容
        if (exportAttachment.getNamingPolicy() == null) {
            exportAttachment.setNamingPolicy(NamingPolicyEnum.KEEP);
        }

        List<AbcTuple3<String, String, String>> result = new LinkedList<>();

        if (Boolean.TRUE.equals(exportAttachment.getSource().getMayContainMultipleItemsInOneField())) {
            String[] slices =
                    columnValue.split(exportAttachment.getSource().getDelimiter());
            for (String slice : slices) {
                if (!ObjectUtils.isEmpty(slice)) {
                    switch (exportAttachment.getNamingPolicy()) {
                        // 随机文件名
                        case RANDOM: {
                            AbcTuple3<String, String, String> tuple =
                                    new AbcTuple3<>(slice.trim(),
                                            directoryPath.toString(), UUID.randomUUID().toString());
                            result.add(tuple);
                        }
                        break;
                        // 保持现有文件名
                        case KEEP: {
                            AbcTuple3<String, String, String> tuple =
                                    new AbcTuple3<>(slice.trim(), directoryPath.toString(), null);
                            result.add(tuple);
                        }
                        break;
                        // 融合文件名
                        case COMBINE: {
                            NamingPolicyExtCombine namingPolicyExtCombine =
                                    exportAttachment.getNamingPolicyExtCombine();
                            StringBuilder targetName = new StringBuilder();
                            for (NamingPolicyExtCombineField field : namingPolicyExtCombine.getFields()) {
                                if (targetName.length() > 0) {
                                    targetName.append("-");
                                }
                                if (Boolean.TRUE.equals(field.getFixed())) {
                                    targetName.append(field.getFieldName());
                                } else {
                                    Object fieldValueAsObject =
                                            columnValueMap.get(field.getFieldName());
                                    if (!ObjectUtils.isEmpty(fieldValueAsObject)) {
                                        String fieldValueAsString =
                                                String.valueOf(fieldValueAsObject);
                                        Matcher matcher =
                                                PATTERN.matcher(fieldValueAsString);
                                        targetName.append(matcher.replaceAll(""));
                                    }
                                }
                            }
                            AbcTuple3<String, String, String> tuple =
                                    new AbcTuple3<>(slice.trim(), directoryPath.toString(), targetName.toString());
                            result.add(tuple);
                        }
                        break;
                        default:
                            throw new AbcUndefinedException(String.format("unsupported naming policy %s",
                                    exportAttachment.getNamingPolicy()));
                    }
                }
            }
        } else {
            switch (exportAttachment.getNamingPolicy()) {
                // 随机文件名
                case RANDOM: {
                    AbcTuple3<String, String, String> tuple =
                            new AbcTuple3<>(columnValue.trim(), directoryPath.toString(), UUID.randomUUID().toString());
                    result.add(tuple);
                }
                break;
                // 保持现有文件名
                case KEEP: {
                    AbcTuple3<String, String, String> tuple =
                            new AbcTuple3<>(columnValue.trim(), directoryPath.toString(), null);
                    result.add(tuple);
                }
                break;
                // 融合文件名
                case COMBINE: {
                    NamingPolicyExtCombine namingPolicyExtCombine =
                            exportAttachment.getNamingPolicyExtCombine();
                    StringBuilder targetName = new StringBuilder();
                    for (NamingPolicyExtCombineField field : namingPolicyExtCombine.getFields()) {
                        if (targetName.length() > 0) {
                            targetName.append("-");
                        }
                        if (Boolean.TRUE.equals(field.getFixed())) {
                            targetName.append(field.getFieldName());
                        } else {
                            Object fieldValueAsObject =
                                    columnValueMap.get(field.getFieldName());
                            if (!ObjectUtils.isEmpty(fieldValueAsObject)) {
                                String fieldValueAsString =
                                        String.valueOf(fieldValueAsObject);
                                Matcher matcher =
                                        PATTERN.matcher(fieldValueAsString);
                                targetName.append(matcher.replaceAll(""));
                            }
                        }
                    }
                    AbcTuple3<String, String, String> tuple =
                            new AbcTuple3<>(columnValue.trim(), directoryPath.toString(), targetName.toString());
                    result.add(tuple);
                }
                break;
                default:
                    throw new AbcUndefinedException(String.format("unsupported naming policy %s",
                            exportAttachment.getNamingPolicy()));
            }
        }

        if (CollectionUtils.isEmpty(result)) {
            return null;
        }

        return result;
    }

    private Integer getMaximumNumberOfImagesExportedByOneExportTask() {
        Integer maximumNumberOfImagesExportedByOneExportTask = 100;
        SettingsDo maximumNumberOfImagesExportedByOneExportTaskSettingsDo =
                this.settingsRepository.findByName(MAXIMUM_NUMBER_OF_IMAGES_EXPORTED_BY_ONE_EXPORT_TASK);
        if (maximumNumberOfImagesExportedByOneExportTaskSettingsDo != null
                && maximumNumberOfImagesExportedByOneExportTaskSettingsDo.getValue() != null) {
            maximumNumberOfImagesExportedByOneExportTask =
                    Integer.parseInt(maximumNumberOfImagesExportedByOneExportTaskSettingsDo.getValue());
        }

        return maximumNumberOfImagesExportedByOneExportTask;
    }

    private Integer getMaximumNumberOfFilesExportedByOneExportTask() {
        Integer maximumNumberOfFilesExportedByOneExportTask = 10;
        SettingsDo maximumNumberOfFilesExportedByOneExportTaskSettingsDo =
                this.settingsRepository.findByName(MAXIMUM_NUMBER_OF_FILES_EXPORTED_BY_ONE_EXPORT_TASK);
        if (maximumNumberOfFilesExportedByOneExportTaskSettingsDo != null
                && maximumNumberOfFilesExportedByOneExportTaskSettingsDo.getValue() != null) {
            maximumNumberOfFilesExportedByOneExportTask =
                    Integer.parseInt(maximumNumberOfFilesExportedByOneExportTaskSettingsDo.getValue());
        }

        return maximumNumberOfFilesExportedByOneExportTask;
    }
}
