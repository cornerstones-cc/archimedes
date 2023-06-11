package cc.cornerstones.biz.export.service.assembly;

import cc.cornerstones.almond.constants.DatabaseConstants;
import cc.cornerstones.almond.exceptions.*;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcDateUtils;
import cc.cornerstones.almond.utils.AbcFileUtils;
import cc.cornerstones.arbutus.structuredlogging.service.inf.LogService;
import cc.cornerstones.biz.datafacet.share.constants.DataFieldTypeEnum;
import cc.cornerstones.biz.export.share.constants.ExportTaskStatusEnum;
import cc.cornerstones.biz.export.entity.ExportTaskDo;
import cc.cornerstones.biz.export.persistence.ExportTaskRepository;
import cc.cornerstones.biz.share.types.DataPermissionFilter;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mysql.cj.x.protobuf.MysqlxDatatypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class ExportTaskHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExportTaskHandler.class);

    @Autowired
    private ExportTaskRepository exportTaskRepository;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    private LogService logService;

    private static String JOB_CATEGORY = "export";

    protected void handleFailed(
            Long taskUid,
            String message,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        ExportTaskDo exportTaskDo = this.exportTaskRepository.findByTaskUid(taskUid);
        if (exportTaskDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::task_uid=%d", ExportTaskDo.RESOURCE_SYMBOL, taskUid));
        }

        exportTaskDo.setTaskStatus(ExportTaskStatusEnum.FAILED);
        exportTaskDo.setEndTimestamp(LocalDateTime.now());

        long totalDurationInMillis =
                exportTaskDo.getEndTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        - exportTaskDo.getBeginTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        if (totalDurationInMillis < 0) {
            totalDurationInMillis = 0L;
        }
        long totalDurationInSecs = totalDurationInMillis / 1000;
        exportTaskDo.setTotalDurationInSecs(totalDurationInSecs);
        exportTaskDo.setTotalDurationRemark(AbcDateUtils.format(totalDurationInMillis));

        if (!ObjectUtils.isEmpty(message)) {
            this.logService.createLog(JOB_CATEGORY, taskUid, message);

            if (message.length() > DatabaseConstants.DEFAULT_DESCRIPTION_LENGTH) {
                exportTaskDo.setRemark(message.substring(0, DatabaseConstants.DEFAULT_DESCRIPTION_LENGTH));
            } else {
                exportTaskDo.setRemark(message);
            }
        }

        BaseDo.update(exportTaskDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.exportTaskRepository.save(exportTaskDo);
    }

    protected void handleFinished(
            Long taskUid,
            String message,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        ExportTaskDo exportTaskDo = this.exportTaskRepository.findByTaskUid(taskUid);
        if (exportTaskDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::task_uid=%d", ExportTaskDo.RESOURCE_SYMBOL, taskUid));
        }

        exportTaskDo.setTaskStatus(ExportTaskStatusEnum.FINISHED);
        exportTaskDo.setEndTimestamp(LocalDateTime.now());

        long totalDurationInMillis =
                exportTaskDo.getEndTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        - exportTaskDo.getBeginTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        if (totalDurationInMillis < 0) {
            totalDurationInMillis = 0L;
        }
        long totalDurationInSecs = totalDurationInMillis / 1000;
        exportTaskDo.setTotalDurationInSecs(totalDurationInSecs);
        exportTaskDo.setTotalDurationRemark(AbcDateUtils.format(totalDurationInMillis));

        if (!ObjectUtils.isEmpty(message)) {
            if (message.length() > DatabaseConstants.DEFAULT_DESCRIPTION_LENGTH) {
                exportTaskDo.setRemark(message.substring(0, DatabaseConstants.DEFAULT_DESCRIPTION_LENGTH));
            } else {
                exportTaskDo.setRemark(message);
            }
        }

        BaseDo.update(exportTaskDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.exportTaskRepository.save(exportTaskDo);
    }

    protected void handlePrepareEnd(
            Long taskUid,
            String countStatement,
            String queryStatement,
            List<DataPermissionFilter> dataPermissionFilters,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        ExportTaskDo exportTaskDo = this.exportTaskRepository.findByTaskUid(taskUid);
        if (exportTaskDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::task_uid=%d", ExportTaskDo.RESOURCE_SYMBOL, taskUid));
        }

        exportTaskDo.setTaskStatus(ExportTaskStatusEnum.COUNTING);
        exportTaskDo.setCountBeginTimestamp(LocalDateTime.now());
        exportTaskDo.setCountStatement(countStatement);
        exportTaskDo.setQueryStatement(queryStatement);

        //
        if (!CollectionUtils.isEmpty(dataPermissionFilters)) {
            Object object = JSONObject.toJSON(dataPermissionFilters);
            if (object instanceof JSONObject) {
                JSONObject jsonObject = (JSONObject) object;
                exportTaskDo.setIntermediateResult(jsonObject);
            } else if (object instanceof JSONArray) {
                JSONArray jsonArray = (JSONArray) object;
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("data", jsonArray);
                exportTaskDo.setIntermediateResult(jsonObject);
            }
        }

        BaseDo.update(exportTaskDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.exportTaskRepository.save(exportTaskDo);
    }

    protected void handleCountEnd(
            Long taskUid,
            Long totalRowsInSource,
            Integer totalColumnsInSource,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        ExportTaskDo exportTaskDo = this.exportTaskRepository.findByTaskUid(taskUid);
        if (exportTaskDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::task_uid=%d", ExportTaskDo.RESOURCE_SYMBOL, taskUid));
        }

        exportTaskDo.setTaskStatus(ExportTaskStatusEnum.QUERYING);
        exportTaskDo.setCountEndTimestamp(LocalDateTime.now());
        exportTaskDo.setTotalRowsInSource(totalRowsInSource);
        exportTaskDo.setTotalColumnsInSource(totalColumnsInSource);

        long totalDurationInSecs =
                exportTaskDo.getCountEndTimestamp().toEpochSecond(ZoneOffset.UTC)
                        - exportTaskDo.getCountBeginTimestamp().toEpochSecond(ZoneOffset.UTC);
        if (totalDurationInSecs < 0) {
            totalDurationInSecs = 0L;
        }
        exportTaskDo.setTotalCountDurationInSecs(totalDurationInSecs);

        BaseDo.update(exportTaskDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.exportTaskRepository.save(exportTaskDo);
    }

    protected void handleQueryEnd(
            Long taskUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        ExportTaskDo exportTaskDo = this.exportTaskRepository.findByTaskUid(taskUid);
        if (exportTaskDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::task_uid=%d", ExportTaskDo.RESOURCE_SYMBOL, taskUid));
        }

        exportTaskDo.setTaskStatus(ExportTaskStatusEnum.FETCHING);
        exportTaskDo.setQueryEndTimestamp(LocalDateTime.now());

        long totalDurationInSecs =
                exportTaskDo.getQueryEndTimestamp().toEpochSecond(ZoneOffset.UTC)
                        - exportTaskDo.getCountEndTimestamp().toEpochSecond(ZoneOffset.UTC);
        if (totalDurationInSecs < 0) {
            totalDurationInSecs = 0L;
        }
        exportTaskDo.setTotalQueryDurationInSecs(totalDurationInSecs);

        BaseDo.update(exportTaskDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.exportTaskRepository.save(exportTaskDo);
    }

    protected void handleFetchEnd(
            Long taskUid,
            Long totalWriteDurationInSecs,
            Long totalRowsInFile,
            Long fileLengthInBytes,
            String fileName,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        ExportTaskDo exportTaskDo = this.exportTaskRepository.findByTaskUid(taskUid);
        if (exportTaskDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::task_uid=%d", ExportTaskDo.RESOURCE_SYMBOL, taskUid));
        }

        exportTaskDo.setTaskStatus(ExportTaskStatusEnum.TRANSFERRING);
        exportTaskDo.setFetchEndTimestamp(LocalDateTime.now());

        long totalFetchDurationInSecs =
                exportTaskDo.getFetchEndTimestamp().toEpochSecond(ZoneOffset.UTC)
                        - exportTaskDo.getQueryEndTimestamp().toEpochSecond(ZoneOffset.UTC);
        if (totalFetchDurationInSecs < 0) {
            totalFetchDurationInSecs = 0L;
        }

        exportTaskDo.setTotalReadDurationInSecs(totalFetchDurationInSecs - totalWriteDurationInSecs);
        exportTaskDo.setTotalWriteDurationInSecs(totalWriteDurationInSecs);
        exportTaskDo.setTotalRowsInFile(totalRowsInFile);
        exportTaskDo.setFileLengthInBytes(fileLengthInBytes);
        exportTaskDo.setFileLengthRemark(AbcFileUtils.formatFileLength(fileLengthInBytes));
        exportTaskDo.setFileName(fileName);

        BaseDo.update(exportTaskDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.exportTaskRepository.save(exportTaskDo);
    }

    protected void handleTransferEnd(
            Long taskUid,
            String fileId,
            Long dfsServiceAgentUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        ExportTaskDo exportTaskDo = this.exportTaskRepository.findByTaskUid(taskUid);
        if (exportTaskDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::task_uid=%d", ExportTaskDo.RESOURCE_SYMBOL, taskUid));
        }

        exportTaskDo.setTaskStatus(ExportTaskStatusEnum.FINISHED);
        exportTaskDo.setTransferEndTimestamp(LocalDateTime.now());

        long transferDurationInSecs =
                exportTaskDo.getTransferEndTimestamp().toEpochSecond(ZoneOffset.UTC)
                        - exportTaskDo.getFetchEndTimestamp().toEpochSecond(ZoneOffset.UTC);
        if (transferDurationInSecs < 0) {
            transferDurationInSecs = 0L;
        }
        exportTaskDo.setTotalTransferDurationInSecs(transferDurationInSecs);

        exportTaskDo.setEndTimestamp(LocalDateTime.now());

        exportTaskDo.setFileId(fileId);

        exportTaskDo.setDfsServiceAgentUid(dfsServiceAgentUid);

        long totalDurationInMillis =
                exportTaskDo.getEndTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        - exportTaskDo.getBeginTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        if (totalDurationInMillis < 0) {
            totalDurationInMillis = 0L;
        }
        long totalDurationInSecs = totalDurationInMillis / 1000;
        exportTaskDo.setTotalDurationInSecs(totalDurationInSecs);
        exportTaskDo.setTotalDurationRemark(AbcDateUtils.format(totalDurationInMillis));

        BaseDo.update(exportTaskDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.exportTaskRepository.save(exportTaskDo);
    }

    protected void handleFetchProgressUpdate(
            Long taskUid,
            String fetchProgressPercentage,
            String fetchProgressRemark,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        ExportTaskDo exportTaskDo = this.exportTaskRepository.findByTaskUid(taskUid);
        if (exportTaskDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::task_uid=%d", ExportTaskDo.RESOURCE_SYMBOL, taskUid));
        }

        exportTaskDo.setFetchProgressPercentage(fetchProgressPercentage);
        exportTaskDo.setFetchProgressRemark(fetchProgressRemark);
        BaseDo.update(exportTaskDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.exportTaskRepository.save(exportTaskDo);
    }

    protected Map<String, Object> parseRow(ResultSet resultSet) throws SQLException {
        Map<String, Object> row = new HashMap<>(10);

        for (int columnIndex = 1; columnIndex <= resultSet.getMetaData().getColumnCount(); columnIndex++) {
            Object object = resultSet.getObject(columnIndex);

            // 特殊处理个别 MYSQL 数据类型，转换成相应的 Java 数据类型
            if (object instanceof java.util.Date) {
                java.util.Date transformedObject = (java.util.Date) object;
                LocalDateTime transformed =
                        Instant.ofEpochMilli(transformedObject.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
                String transformedString = transformed.format(this.dateTimeFormatter);
                row.put(resultSet.getMetaData().getColumnLabel(columnIndex), transformedString);
            } else if (object instanceof java.sql.Date) {
                java.sql.Date sqlDate = (java.sql.Date) object;
                java.util.Date transformedObject = new java.util.Date(sqlDate.getTime());
                LocalDateTime transformed =
                        Instant.ofEpochMilli(transformedObject.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
                String transformedString = transformed.format(this.dateFormatter);
                row.put(resultSet.getMetaData().getColumnLabel(columnIndex), transformedString);
            } else if (object instanceof java.sql.Timestamp) {
                java.util.Date transformedObject = new java.util.Date(((java.sql.Timestamp) object).getTime());
                LocalDateTime transformed =
                        Instant.ofEpochMilli(transformedObject.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
                String transformedString = transformed.format(this.dateTimeFormatter);
                row.put(resultSet.getMetaData().getColumnLabel(columnIndex), transformedString);
            } else if (object instanceof java.sql.Time) {
                java.util.Date transformedObject = new java.util.Date(((java.sql.Time) object).getTime());
                LocalDateTime transformed =
                        Instant.ofEpochMilli(transformedObject.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
                String transformedString = transformed.format(this.timeFormatter);
                row.put(resultSet.getMetaData().getColumnLabel(columnIndex), transformedString);
            } else if (object instanceof java.time.LocalDateTime) {
                LocalDateTime transformedObject = (LocalDateTime) object;
                String transformedString = transformedObject.format(this.dateTimeFormatter);
                row.put(resultSet.getMetaData().getColumnLabel(columnIndex), transformedString);
            } else if (object instanceof java.time.LocalDate) {
                LocalDate transformedObject = (LocalDate) object;
                String transformedString = transformedObject.format(this.dateFormatter);
                row.put(resultSet.getMetaData().getColumnLabel(columnIndex), transformedString);
            } else if (object instanceof java.time.LocalTime) {
                LocalTime transformedObject = (LocalTime) object;
                String transformedString = transformedObject.format(this.timeFormatter);
                row.put(resultSet.getMetaData().getColumnLabel(columnIndex), transformedString);
            } else if (object instanceof BigInteger) {
                BigInteger bigIntegerObject = (BigInteger) object;
                long longObject = bigIntegerObject.longValue();
                row.put(resultSet.getMetaData().getColumnLabel(columnIndex), longObject);
            } else {
                row.put(resultSet.getMetaData().getColumnLabel(columnIndex), object);
            }
        }

        return row;
    }

    protected Object transform(
            Object columnValue,
            String columnLabel,
            Map<String, DataFieldTypeEnum> fieldTypeMap) {
        if (columnValue == null) {
            return columnValue;
        }

        DataFieldTypeEnum fieldType = fieldTypeMap.get(columnLabel);

        if (columnValue instanceof java.util.Date) {
            Date transformedObject = (Date) columnValue;
            LocalDateTime transformed =
                    Instant.ofEpochMilli(transformedObject.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();

            if (fieldType != null && fieldType.equals(DataFieldTypeEnum.DATETIME)) {
                String transformedString = transformed.format(dateTimeFormatter);
                return transformedString;
            } else if (fieldType != null && fieldType.equals(DataFieldTypeEnum.DATE)) {
                String transformedString = transformed.format(dateFormatter);
                return transformedString;
            } else {
                String transformedString = transformed.format(dateTimeFormatter);
                return transformedString;
            }
        } else if (columnValue instanceof java.sql.Timestamp) {
            Date transformedObject = new Date(((java.sql.Timestamp) columnValue).getTime());
            LocalDateTime transformed =
                    Instant.ofEpochMilli(transformedObject.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();

            if (fieldType != null && fieldType.equals(DataFieldTypeEnum.DATETIME)) {
                String transformedString = transformed.format(dateTimeFormatter);
                return transformedString;
            } else if (fieldType != null && fieldType.equals(DataFieldTypeEnum.DATE)) {
                String transformedString = transformed.format(dateFormatter);
                return transformedString;
            } else {
                String transformedString = transformed.format(dateTimeFormatter);
                return transformedString;
            }
        } else if (columnValue instanceof java.sql.Date) {
            java.sql.Date sqlDate = (java.sql.Date) columnValue;
            Date transformedObject = new Date(sqlDate.getTime());
            LocalDateTime transformed =
                    Instant.ofEpochMilli(transformedObject.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();

            if (fieldType != null && fieldType.equals(DataFieldTypeEnum.DATETIME)) {
                String transformedString = transformed.format(dateTimeFormatter);
                return transformedString;
            } else if (fieldType != null && fieldType.equals(DataFieldTypeEnum.DATE)) {
                String transformedString = transformed.format(dateFormatter);
                return transformedString;
            } else {
                String transformedString = transformed.format(dateTimeFormatter);
                return transformedString;
            }
        } else if (columnValue instanceof java.sql.Time) {
            java.sql.Time sqlTime = (java.sql.Time) columnValue;
            return sqlTime.toString();
        } else if (columnValue instanceof java.time.LocalDateTime) {
            LocalDateTime transformedObject = (LocalDateTime) columnValue;

            if (fieldType != null && fieldType.equals(DataFieldTypeEnum.DATETIME)) {
                String transformedString = transformedObject.format(dateTimeFormatter);
                return transformedString;
            } else if (fieldType != null && fieldType.equals(DataFieldTypeEnum.DATE)) {
                String transformedString = transformedObject.format(dateFormatter);
                return transformedString;
            } else {
                String transformedString = transformedObject.format(dateTimeFormatter);
                return transformedString;
            }
        } else if (columnValue instanceof java.time.LocalDate) {
            LocalDate transformedObject = (LocalDate) columnValue;

            if (fieldType != null && fieldType.equals(DataFieldTypeEnum.DATETIME)) {
                String transformedString = transformedObject.format(dateTimeFormatter);
                return transformedString;
            } else if (fieldType != null && fieldType.equals(DataFieldTypeEnum.DATE)) {
                String transformedString = transformedObject.format(dateFormatter);
                return transformedString;
            } else {
                String transformedString = transformedObject.format(dateFormatter);
                return transformedString;
            }
        } else if (columnValue instanceof java.time.LocalTime) {
            LocalTime transformedObject = (LocalTime) columnValue;
            String transformedString = transformedObject.format(timeFormatter);
            return transformedString;
        } else if (columnValue instanceof java.lang.String) {
            if (fieldType != null) {
                switch (fieldType) {
                    case INTEGER: {
                        String transformedObject = (String) columnValue;
                        try {
                            Integer transformedInteger = Integer.parseInt(transformedObject);
                            return transformedInteger;
                        } catch (NumberFormatException e) {
                            LOGGER.error("failed to transform {} to Integer", transformedObject, e);
                        }
                        return columnValue;
                    }
                    case LONG: {
                        String transformedObject = (String) columnValue;
                        try {
                            Long transformedLong = Long.parseLong(transformedObject);
                            return transformedLong;
                        } catch (NumberFormatException e) {
                            LOGGER.error("failed to transform {} to Long", transformedObject, e);
                        }
                        return columnValue;
                    }
                    case DECIMAL: {
                        String transformedObject = (String) columnValue;
                        try {
                            BigDecimal transformedBigDecimal = new BigDecimal(transformedObject);
                            return transformedBigDecimal;
                        } catch (NumberFormatException e) {
                            LOGGER.error("failed to transform {} to BigDecimal", transformedObject, e);
                        }
                        return columnValue;
                    }
                }
            }
        }

        return columnValue;
    }

    protected boolean shouldCancel(
            Long taskUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        ExportTaskDo exportTaskDo = this.exportTaskRepository.findByTaskUid(taskUid);
        if (exportTaskDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::task_uid=%d", ExportTaskDo.RESOURCE_SYMBOL, taskUid));
        }

        if (ExportTaskStatusEnum.CANCELLING.equals(exportTaskDo.getTaskStatus())) {
            exportTaskDo.setTaskStatus(ExportTaskStatusEnum.CANCELED);
            exportTaskDo.setEndTimestamp(LocalDateTime.now());

            long totalDurationInMillis =
                    exportTaskDo.getEndTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            - exportTaskDo.getBeginTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            if (totalDurationInMillis < 0) {
                totalDurationInMillis = 0L;
            }
            long totalDurationInSecs = totalDurationInMillis / 1000;
            exportTaskDo.setTotalDurationInSecs(totalDurationInSecs);
            exportTaskDo.setTotalDurationRemark(AbcDateUtils.format(totalDurationInMillis));

            BaseDo.update(exportTaskDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.exportTaskRepository.save(exportTaskDo);

            return true;
        }

        return false;
    }
}
