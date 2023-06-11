package cc.cornerstones.biz.datasource.service.assembly.database.oracle;

import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.AbcTuple2;
import cc.cornerstones.almond.utils.AbcOracleUtils;
import cc.cornerstones.almond.utils.AbcStringUtils;
import cc.cornerstones.biz.datasource.service.assembly.database.*;
import cc.cornerstones.biz.datasource.share.constants.DataColumnTypeEnum;
import cc.cornerstones.biz.datasource.share.constants.DataTableTypeEnum;
import cc.cornerstones.biz.datasource.share.constants.DatabaseServerTypeEnum;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Component
public class OracleDdlHandler implements DdlHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(OracleDdlHandler.class);

    @Autowired
    private OracleConnectivityHandler connectivityHandler;

    /**
     * Database server type
     *
     * @return
     */
    @Override
    public DatabaseServerTypeEnum type() {
        return DatabaseServerTypeEnum.ORACLE;
    }

    @Override
    public List<DataTableMetadata> loadDataTableMetadata(
            JSONObject connectionProfile) throws AbcUndefinedException {
        DataSourceConnection connection = this.connectivityHandler.createConnection(connectionProfile);
        try {
            return loadDataTableMetadata(connection);
        } finally {
            this.connectivityHandler.closeConnection(connection);
        }
    }

    @Override
    public List<DataTableMetadata> loadDataTableMetadata(
            DataSourceConnection connection) throws AbcUndefinedException {
        if (connection == null) {
            throw new AbcResourceConflictException("null connection");
        }

        if (!(connection instanceof OracleConnection)) {
            throw new AbcResourceConflictException("unexpected oracle connection");
        }

        OracleConnection objectiveConnection = (OracleConnection) connection;

        if (objectiveConnection.getConnection() == null) {
            throw new AbcResourceConflictException("null oracle connection");
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT (SELECT USER FROM DUAL) AS TABLE_SCHEMA, TABLE_NAME, COMMENTS AS TABLE_COMMENT, TABLE_TYPE FROM user_tab_comments");
        LOGGER.info("sql:{}", sql);

        List<DataTableMetadata> dataTableMetadataList = new LinkedList<>();
        ResultSet resultSet = null;
        try (PreparedStatement statement = objectiveConnection.getConnection().prepareStatement(sql.toString());) {
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String tableSchema = resultSet.getString(1);
                String tableName = resultSet.getString(2);
                String tableComment = resultSet.getString(3);
                String tableType = resultSet.getString(4);

                DataTableMetadata dataTableMetadata = new DataTableMetadata();
                dataTableMetadata.setName(tableName);
                dataTableMetadata.setDescription(tableComment);
                if (ObjectUtils.isEmpty(tableType)) {
                    // 默认
                    dataTableMetadata.setType(DataTableTypeEnum.DATABASE_TABLE);
                } else {
                    switch (tableType) {
                        case "TABLE":
                            dataTableMetadata.setType(DataTableTypeEnum.DATABASE_TABLE);
                            break;
                        case "VIEW":
                            dataTableMetadata.setType(DataTableTypeEnum.DATABASE_VIEW);
                            break;
                        default:
                            LOGGER.warn("unsupported oracle table type:{}", tableType);
                            // 默认
                            dataTableMetadata.setType(DataTableTypeEnum.DATABASE_TABLE);
                            break;
                    }
                }
                dataTableMetadata.setContextPath(new ArrayList<>(1));
                dataTableMetadata.getContextPath().add(tableSchema);

                OracleDataTableMetadataExtension extension = new OracleDataTableMetadataExtension();
                extension.setDatabaseName(tableSchema);
                extension.setTableName(tableName);
                extension.setTableComment(tableComment);
                extension.setTableType(tableType);

                dataTableMetadata.setExtension((JSONObject) JSONObject.toJSON(extension));

                dataTableMetadataList.add(dataTableMetadata);
            }

            return dataTableMetadataList;
        } catch (SQLException e) {
            LOGGER.error("fail to execute sql:{}", sql, e);
            throw new AbcResourceConflictException("fail to execute sql");
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    LOGGER.error("fail to close result set", e);
                }
            }
        }
    }

    @Override
    public List<DataColumnMetadata> loadDataColumnMetadataOfDataTable(
            JSONObject connectionProfile,
            DataTableMetadata dataTableMetadata) throws AbcUndefinedException {
        DataSourceConnection connection = this.connectivityHandler.createConnection(connectionProfile);
        try {
            return loadDataColumnMetadataOfDataTable(connection, dataTableMetadata);
        } finally {
            this.connectivityHandler.closeConnection(connection);
        }
    }

    @Override
    public List<DataColumnMetadata> loadDataColumnMetadataOfDataTable(
            DataSourceConnection connection,
            DataTableMetadata dataTableMetadata) throws AbcUndefinedException {
        if (connection == null) {
            throw new AbcIllegalParameterException("null connection");
        }

        if (!(connection instanceof OracleConnection)) {
            throw new AbcIllegalParameterException("unexpected oracle connection");
        }

        OracleConnection objectiveConnection = (OracleConnection) connection;

        if (objectiveConnection.getConnection() == null) {
            throw new AbcIllegalParameterException("null oracle connection");
        }

        if (dataTableMetadata == null) {
            throw new AbcIllegalParameterException("null data table metadata");
        }

        JSONObject extension = dataTableMetadata.getExtension();
        OracleDataTableMetadataExtension objectiveDataTableMetadataExtension = JSONObject.toJavaObject(extension,
                OracleDataTableMetadataExtension.class);

        StringBuilder sql = new StringBuilder();
        sql.append("USE " + AbcOracleUtils.buildEscapedName(objectiveDataTableMetadataExtension.getDatabaseName()) + ";")
                .append("SELECT c.COLUMN_NAME, c.COMMENTS, t.DATA_TYPE, t.CHAR_LENGTH, t.DATA_PRECISION, t.DATA_SCALE")
                .append(" FROM user_col_comments c JOIN user_tab_columns t")
                .append(" ON c.TABLE_NAME = t.TABLE_NAME AND c.COLUMN_NAME = t.COLUMN_NAME")
                .append(" WHERE c.TABLE_NAME = ")
                .append(AbcOracleUtils.buildEscapedName(objectiveDataTableMetadataExtension.getTableName()));
        LOGGER.info("sql:{}", sql);

        List<DataColumnMetadata> dataColumnMetadataList = new LinkedList<>();
        ResultSet resultSet = null;
        try (PreparedStatement statement = objectiveConnection.getConnection().prepareStatement(sql.toString());) {
            resultSet = statement.executeQuery();

            int ordinalPosition = 0;
            while (resultSet.next()) {
                String fieldName = resultSet.getString("COLUMN_NAME");
                String fieldType = resultSet.getString("DATA_TYPE");
                String fieldDescription = resultSet.getString("COMMENTS");
                BigDecimal charLength = resultSet.getBigDecimal("CHAR_LENGTH");
                BigDecimal dataPrecision = resultSet.getBigDecimal("DATA_PRECISION");
                BigDecimal dataScale = resultSet.getBigDecimal("DATA_SCALE");

                AbcTuple2<DataColumnTypeEnum, String> transformedDataFieldTypeAndLength =
                        transformDataColumnTypeAndLength(fieldType, charLength, dataPrecision, dataScale);

                DataColumnMetadata dataColumnMetadata = new DataColumnMetadata();
                dataColumnMetadata.setName(fieldName);
                dataColumnMetadata.setDescription(fieldDescription);
                dataColumnMetadata.setType(transformedDataFieldTypeAndLength.f);
                dataColumnMetadata.setLength(transformedDataFieldTypeAndLength.s);
                dataColumnMetadata.setOrdinalPosition((ordinalPosition++) * 1.0f);

                dataColumnMetadataList.add(dataColumnMetadata);
            }

            return dataColumnMetadataList;
        } catch (SQLException e) {
            LOGGER.error("fail to execute sql:{}", sql, e);
            throw new AbcResourceConflictException("fail to execute sql");
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    LOGGER.warn("fail to close result set", e);
                }
            }
        }
    }

    private AbcTuple2<DataColumnTypeEnum, String> transformDataColumnTypeAndLength(
            String originalDataColumnTypeAndLength, BigDecimal charLength, BigDecimal dataPrecision,
            BigDecimal dataScale) {
        DataColumnTypeEnum dataColumnType = null;
        String dataColumnLength = null;

        if (originalDataColumnTypeAndLength.startsWith("NUMBER")) {
            if (dataScale == null || dataPrecision == null) {
                dataColumnType = DataColumnTypeEnum.DECIMAL;
            } else if (dataScale.intValue() == 0) {
                if (dataPrecision.intValue() == 1) {
                    // 约定在应用中把 NUMBER(1, 0) 作为布尔类型使用
                    dataColumnType = DataColumnTypeEnum.BOOLEAN;
                } else if (dataPrecision.intValue() < 20) {
                    dataColumnType = DataColumnTypeEnum.INT;
                } else {
                    dataColumnType = DataColumnTypeEnum.LONG;
                }
            } else if (dataScale.intValue() == -127) {
                if (dataPrecision.intValue() == 0) {
                    // ROWNUM 列
                    dataColumnType = DataColumnTypeEnum.LONG;
                } else {
                    dataColumnType = DataColumnTypeEnum.DECIMAL;
                }
            } else {
                dataColumnType = DataColumnTypeEnum.DECIMAL;
            }
        } else if (originalDataColumnTypeAndLength.startsWith("LONG")) {
            dataColumnType = DataColumnTypeEnum.LONG;
        } else if (originalDataColumnTypeAndLength.startsWith("DATE")) {
            dataColumnType = DataColumnTypeEnum.TIMESTAMP;
        } else if (originalDataColumnTypeAndLength.startsWith("TIMESTAMP")) {
            dataColumnType = DataColumnTypeEnum.TIMESTAMP;
        } else if (originalDataColumnTypeAndLength.startsWith("FLOAT")) {
            dataColumnType = DataColumnTypeEnum.DECIMAL;
        } else if (originalDataColumnTypeAndLength.startsWith("VARCHAR2")) {
            dataColumnType = DataColumnTypeEnum.TEXT;
        } else if (originalDataColumnTypeAndLength.startsWith("CHAR")) {
            dataColumnType = DataColumnTypeEnum.TEXT;
            dataColumnLength = String.valueOf(charLength.intValue());
        } else {
            //
            LOGGER.warn("unsupported data column type:{}, default to TEXT", originalDataColumnTypeAndLength);
            // 保护性处理，赋予类型
            dataColumnType = DataColumnTypeEnum.TEXT;
        }

        return new AbcTuple2(dataColumnType, dataColumnLength);
    }

    @Override
    public List<DataIndexMetadata> loadDataIndexMetadataOfDataTable(
            JSONObject connectionProfile,
            DataTableMetadata dataTableMetadata) throws AbcUndefinedException {
        DataSourceConnection connection = this.connectivityHandler.createConnection(connectionProfile);
        try {
            return loadDataIndexMetadataOfDataTable(connection, dataTableMetadata);
        } finally {
            this.connectivityHandler.closeConnection(connection);
        }
    }

    @Override
    public List<DataIndexMetadata> loadDataIndexMetadataOfDataTable(
            DataSourceConnection connection,
            DataTableMetadata dataTableMetadata) throws AbcUndefinedException {
        if (connection == null) {
            throw new AbcIllegalParameterException("null connection");
        }

        if (!(connection instanceof OracleConnection)) {
            throw new AbcIllegalParameterException("unexpected oracle connection");
        }

        OracleConnection objectiveConnection = (OracleConnection) connection;

        if (objectiveConnection.getConnection() == null) {
            throw new AbcIllegalParameterException("null oracle connection");
        }

        if (dataTableMetadata == null) {
            throw new AbcIllegalParameterException("null data table metadata");
        }

        JSONObject extension = dataTableMetadata.getExtension();
        OracleDataTableMetadataExtension objectiveDataTableMetadataExtension = JSONObject.toJavaObject(extension,
                OracleDataTableMetadataExtension.class);

        StringBuilder sql = new StringBuilder();
        sql.append("USE " + AbcOracleUtils.buildEscapedName(objectiveDataTableMetadataExtension.getDatabaseName()) + ";")
                .append("SELECT a.INDEX_NAME, a.UNIQUENESS, c.COLUMN_NAME, c.COLUMN_POSITION from all_indexes a JOIN user_ind_columns c")
                .append(" ON a.TABLE_NAME = c.TABLE_NAME AND a.INDEX_NAME = c.INDEX_NAME")
                .append(" WHERE a.TABLE_NAME = ")
                .append(AbcOracleUtils.buildEscapedName(objectiveDataTableMetadataExtension.getTableName()));
        LOGGER.info("sql:{}", sql);

        ResultSet resultSet = null;
        try (PreparedStatement statement = objectiveConnection.getConnection().prepareStatement(sql.toString());) {
            resultSet = statement.executeQuery();

            Map<String, DataIndexMetadata> indexMap = new HashMap<>();
            Map<String, List<AbcTuple2<String, Integer>>> indexAndListOfColumnNameAndSequenceInIndexMap =
                    new HashMap<>();
            while (resultSet.next()) {
                String indexName = resultSet.getString("INDEX_NAME");
                String uniqueness = resultSet.getString("UNIQUENESS");
                Boolean uniqueIndex = false;
                if ("UNIQUE".equals(uniqueness)) {
                    uniqueIndex = true;
                }

                DataIndexMetadata dataIndexMetadata = indexMap.get(indexName);
                // 如果还没有创建 data index，先创建
                if (dataIndexMetadata == null) {
                    dataIndexMetadata = new DataIndexMetadata();
                    dataIndexMetadata.setName(indexName);
                    dataIndexMetadata.setUnique(uniqueIndex);
                    indexMap.put(indexName, dataIndexMetadata);
                }

                // columns in index 涉及顺序
                BigDecimal columnSequenceInIndex = resultSet.getBigDecimal("COLUMN_POSITION");
                String columnName = resultSet.getString("COLUMN_NAME");

                List<AbcTuple2<String, Integer>> listOfColumnNameAndSequenceInIndex =
                        indexAndListOfColumnNameAndSequenceInIndexMap.get(indexName);
                if (listOfColumnNameAndSequenceInIndex == null) {
                    listOfColumnNameAndSequenceInIndex = new LinkedList<>();
                    indexAndListOfColumnNameAndSequenceInIndexMap.put(indexName, listOfColumnNameAndSequenceInIndex);
                }
                listOfColumnNameAndSequenceInIndex.add(new AbcTuple2<>(columnName, columnSequenceInIndex.intValue()));
            }

            // 按照 columns in index 顺序整理 data index metadata
            indexMap.forEach((indexName, dataIndexMetadata) -> {
                List<AbcTuple2<String, Integer>> listOfColumnNameAndSequenceInIndex =
                        indexAndListOfColumnNameAndSequenceInIndexMap.get(indexName);
                if (CollectionUtils.isEmpty(listOfColumnNameAndSequenceInIndex)) {
                    return;
                }

                listOfColumnNameAndSequenceInIndex.sort(new Comparator<AbcTuple2<String, Integer>>() {
                    @Override
                    public int compare(AbcTuple2<String, Integer> o1, AbcTuple2<String, Integer> o2) {
                        if (o1.s < o2.s) {
                            return -1;
                        } else if (o1.s > o2.s) {
                            return 1;
                        }
                        return 0;
                    }
                });

                dataIndexMetadata.setColumns(new ArrayList<>(listOfColumnNameAndSequenceInIndex.size()));
                listOfColumnNameAndSequenceInIndex.forEach(tuple -> {
                    dataIndexMetadata.getColumns().add(tuple.f);
                });
            });

            return new ArrayList<>(indexMap.values());
        } catch (SQLException e) {
            LOGGER.error("fail to execute sql:{}", sql, e);
            throw new AbcResourceConflictException("fail to execute sql");
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    LOGGER.error("fail to close result set", e);
                }
            }
        }
    }

    @Override
    public DataTableMetadata loadDataTableMetadata(
            String tableName,
            List<String> tableContextPath,
            JSONObject connectionProfile) throws AbcUndefinedException {
        DataSourceConnection connection = this.connectivityHandler.createConnection(connectionProfile);
        try {
            return loadDataTableMetadata(tableName, tableContextPath, connection);
        } finally {
            this.connectivityHandler.closeConnection(connection);
        }
    }

    @Override
    public DataTableMetadata loadDataTableMetadata(
            String tableName,
            List<String> tableContextPath,
            DataSourceConnection connection) throws AbcUndefinedException {
        if (connection == null) {
            throw new AbcResourceConflictException("null connection");
        }

        if (!(connection instanceof OracleConnection)) {
            throw new AbcResourceConflictException("unexpected oracle connection");
        }

        OracleConnection objectiveConnection = (OracleConnection) connection;

        if (objectiveConnection.getConnection() == null) {
            throw new AbcResourceConflictException("null oracle connection");
        }

        if (tableContextPath.size() != 1) {
            throw new AbcResourceConflictException(String.format("unexpected context path::%s",
                    AbcStringUtils.toString(tableContextPath, ",")));
        }

        String databaseName = tableContextPath.get(0);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT (SELECT USER FROM DUAL) AS TABLE_SCHEMA, TABLE_NAME, COMMENTS AS TABLE_COMMENT, TABLE_TYPE FROM user_tab_comments")
                .append(" WHERE TABLE_SCHEMA = ").append(AbcOracleUtils.buildEscapedName(databaseName))
                .append(" AND TABLE_NAME = ").append(AbcOracleUtils.buildEscapedName(tableName));
        LOGGER.info("sql:{}", sql);

        ResultSet resultSet = null;
        try (PreparedStatement statement = objectiveConnection.getConnection().prepareStatement(sql.toString());) {
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                String tableSchema = resultSet.getString(1);
                // String tableName = resultSet.getString(2);
                String tableComment = resultSet.getString(3);
                String tableType = resultSet.getString(4);

                DataTableMetadata dataTableMetadata = new DataTableMetadata();
                dataTableMetadata.setName(tableName);
                dataTableMetadata.setDescription(tableComment);
                if (ObjectUtils.isEmpty(tableType)) {
                    // 默认
                    dataTableMetadata.setType(DataTableTypeEnum.DATABASE_TABLE);
                } else {
                    switch (tableType) {
                        case "TABLE":
                            dataTableMetadata.setType(DataTableTypeEnum.DATABASE_TABLE);
                            break;
                        case "VIEW":
                            dataTableMetadata.setType(DataTableTypeEnum.DATABASE_VIEW);
                            break;
                        default:
                            LOGGER.warn("unsupported oracle table type:{}", tableType);
                            // 默认
                            dataTableMetadata.setType(DataTableTypeEnum.DATABASE_TABLE);
                            break;
                    }
                }
                dataTableMetadata.setContextPath(new ArrayList<>(1));
                dataTableMetadata.getContextPath().add(tableSchema);

                OracleDataTableMetadataExtension extension = new OracleDataTableMetadataExtension();
                extension.setDatabaseName(tableSchema);
                extension.setTableName(tableName);
                extension.setTableComment(tableComment);
                extension.setTableType(tableType);

                dataTableMetadata.setExtension((JSONObject) JSONObject.toJSON(extension));

                return dataTableMetadata;
            } else {
                throw new AbcResourceNotFoundException("cannot find the table");
            }
        } catch (SQLException e) {
            LOGGER.error("fail to execute sql:{}", sql, e);
            throw new AbcResourceConflictException("failed to load table metadata");
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    LOGGER.error("fail to close result set", e);
                }
            }
        }

    }

    @Override
    public List<DataTableMetadata> loadDataTableMetadata(
            List<String> contextPath,
            JSONObject connectionProfile) throws AbcUndefinedException {
        return null;
    }

    @Override
    public List<DataTableMetadata> loadDataTableMetadata(
            List<String> contextPath,
            DataSourceConnection connection) throws AbcUndefinedException {
        return null;
    }
}
