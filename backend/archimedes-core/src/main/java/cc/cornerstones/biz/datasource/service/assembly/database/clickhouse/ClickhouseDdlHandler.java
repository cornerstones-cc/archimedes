package cc.cornerstones.biz.datasource.service.assembly.database.clickhouse;

import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.AbcTuple2;
import cc.cornerstones.almond.utils.AbcClickhouseUtils;
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
import org.springframework.util.ObjectUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Component
public class ClickhouseDdlHandler implements DdlHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClickhouseDdlHandler.class);

    @Autowired
    private ClickhouseConnectivityHandler connectivityHandler;

    /**
     * Database server type
     *
     * @return
     */
    @Override
    public DatabaseServerTypeEnum type() {
        return DatabaseServerTypeEnum.CLICKHOUSE;
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

        if (!(connection instanceof ClickhouseConnection)) {
            throw new AbcResourceConflictException("unexpected clickhouse connection");
        }

        ClickhouseConnection objectiveConnection = (ClickhouseConnection) connection;

        if (objectiveConnection.getConnection() == null) {
            throw new AbcResourceConflictException("null clickhouse connection");
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT database AS TABLE_SCHEMA, name AS TABLE_NAME, comment AS TABLE_COMMENT, engine " +
                "AS TABLE_TYPE FROM system.tables");
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
                        case "View":
                            dataTableMetadata.setType(DataTableTypeEnum.DATABASE_VIEW);
                            break;
                        default:
                            // 默认
                            dataTableMetadata.setType(DataTableTypeEnum.DATABASE_TABLE);
                            break;
                    }
                }
                dataTableMetadata.setContextPath(new ArrayList<>(1));
                dataTableMetadata.getContextPath().add(tableSchema);

                ClickhouseDataTableMetadataExtension extension = new ClickhouseDataTableMetadataExtension();
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

        if (!(connection instanceof ClickhouseConnection)) {
            throw new AbcIllegalParameterException("unexpected clickhouse connection");
        }

        ClickhouseConnection objectiveConnection = (ClickhouseConnection) connection;

        if (objectiveConnection.getConnection() == null) {
            throw new AbcIllegalParameterException("null clickhouse connection");
        }

        if (dataTableMetadata == null) {
            throw new AbcIllegalParameterException("null data table metadata");
        }

        JSONObject extension = dataTableMetadata.getExtension();
        ClickhouseDataTableMetadataExtension objectiveDataTableMetadataExtension = JSONObject.toJavaObject(extension,
                ClickhouseDataTableMetadataExtension.class);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT *")
                .append(" FROM system.columns")
                .append(" WHERE ")
                .append(" database = ")
                .append(AbcClickhouseUtils.surroundBySingleQuotes(objectiveDataTableMetadataExtension.getDatabaseName()))
                .append(" AND table = ")
                .append(AbcClickhouseUtils.surroundBySingleQuotes(objectiveDataTableMetadataExtension.getTableName()));
        LOGGER.info("sql:{}", sql);

        List<DataColumnMetadata> dataColumnMetadataList = new LinkedList<>();
        ResultSet resultSet = null;
        try (PreparedStatement statement = objectiveConnection.getConnection().prepareStatement(sql.toString());) {
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String fieldName = resultSet.getString("name");
                String fieldTypeAndLength = resultSet.getString("type");
                String fieldDescription = resultSet.getString("comment");
                Integer ordinalPosition = resultSet.getInt("position");

                AbcTuple2<DataColumnTypeEnum, String> transformedDataFieldTypeAndLength =
                        transformDataColumnTypeAndLength(fieldTypeAndLength);

                DataColumnMetadata dataColumnMetadata = new DataColumnMetadata();
                dataColumnMetadata.setName(fieldName);
                dataColumnMetadata.setDescription(fieldDescription);
                dataColumnMetadata.setType(transformedDataFieldTypeAndLength.f);
                dataColumnMetadata.setLength(transformedDataFieldTypeAndLength.s);
                dataColumnMetadata.setOrdinalPosition(ordinalPosition * 1.0f);

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
            String originalDataColumnTypeAndLength) {
        DataColumnTypeEnum dataColumnType = null;
        String dataColumnLength = null;

        if (originalDataColumnTypeAndLength.startsWith("String")) {
            dataColumnType = DataColumnTypeEnum.TEXT;
            dataColumnLength = "45";
        } else if (originalDataColumnTypeAndLength.startsWith("DateTime64")) {
            dataColumnType = DataColumnTypeEnum.TIMESTAMP;
        } else if (originalDataColumnTypeAndLength.startsWith("DateTime")) {
            dataColumnType = DataColumnTypeEnum.DATETIME;
        } else if (originalDataColumnTypeAndLength.startsWith("Date")) {
            dataColumnType = DataColumnTypeEnum.DATE;
        } else if (originalDataColumnTypeAndLength.startsWith("UInt8")
                || originalDataColumnTypeAndLength.startsWith("Int8")) {
            dataColumnType = DataColumnTypeEnum.SMALLINT;
        } else if (originalDataColumnTypeAndLength.startsWith("UInt16")
                || originalDataColumnTypeAndLength.startsWith("Int16")) {
            dataColumnType = DataColumnTypeEnum.MEDIUMINT;
        } else if (originalDataColumnTypeAndLength.startsWith("UInt32")
                || originalDataColumnTypeAndLength.startsWith(
                "Int32")) {
            dataColumnType = DataColumnTypeEnum.INT;
        } else if (originalDataColumnTypeAndLength.startsWith("UInt64")
                || originalDataColumnTypeAndLength.startsWith("Int64")) {
            dataColumnType = DataColumnTypeEnum.LONG;
        } else if (originalDataColumnTypeAndLength.startsWith("Float64")) {
            dataColumnType = DataColumnTypeEnum.DECIMAL;
        } else if (originalDataColumnTypeAndLength.startsWith("Nullable(Float64)")) {
            dataColumnType = DataColumnTypeEnum.DECIMAL;
        } else if (originalDataColumnTypeAndLength.startsWith("Nullable(String)")) {
            dataColumnType = DataColumnTypeEnum.TEXT;
        } else if (originalDataColumnTypeAndLength.startsWith("Decimal")) {
            dataColumnType = DataColumnTypeEnum.DECIMAL;

            if (originalDataColumnTypeAndLength.length() > "Decimal".length()) {
                dataColumnLength = originalDataColumnTypeAndLength.substring("Decimal".length() + 1,
                        originalDataColumnTypeAndLength.length() - 1);
            }
        } else {
            //
            LOGGER.warn("unsupported data column type:{}", originalDataColumnTypeAndLength);
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

        if (!(connection instanceof ClickhouseConnection)) {
            throw new AbcIllegalParameterException("unexpected clickhouse connection");
        }

        ClickhouseConnection objectiveConnection = (ClickhouseConnection) connection;

        if (objectiveConnection.getConnection() == null) {
            throw new AbcIllegalParameterException("null clickhouse connection");
        }

        if (dataTableMetadata == null) {
            throw new AbcIllegalParameterException("null data table metadata");
        }

        JSONObject extension = dataTableMetadata.getExtension();
        ClickhouseDataTableMetadataExtension objectiveDataTableMetadataExtension = JSONObject.toJavaObject(extension,
                ClickhouseDataTableMetadataExtension.class);
        return null;
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

        if (!(connection instanceof ClickhouseConnection)) {
            throw new AbcResourceConflictException("unexpected clickhouse connection");
        }

        ClickhouseConnection objectiveConnection = (ClickhouseConnection) connection;

        if (objectiveConnection.getConnection() == null) {
            throw new AbcResourceConflictException("null clickhouse connection");
        }

        if (tableContextPath.size() != 1) {
            throw new AbcResourceConflictException(String.format("unexpected context path::%s",
                    AbcStringUtils.toString(tableContextPath, ",")));
        }

        String databaseName = tableContextPath.get(0);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT database AS TABLE_SCHEMA, name AS TABLE_NAME, comment AS TABLE_COMMENT, engine " +
                        "AS TABLE_TYPE FROM system.tables")
                .append(" WHERE ")
                .append("database = ").append(AbcClickhouseUtils.surroundBySingleQuotes(databaseName))
                .append(" AND ").append("name = ").append(AbcClickhouseUtils.surroundBySingleQuotes(tableName));
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
                        case "View":
                            dataTableMetadata.setType(DataTableTypeEnum.DATABASE_VIEW);
                            break;
                        default:
                            // 默认
                            dataTableMetadata.setType(DataTableTypeEnum.DATABASE_TABLE);
                            break;
                    }
                }
                dataTableMetadata.setContextPath(new ArrayList<>(1));
                dataTableMetadata.getContextPath().add(tableSchema);

                ClickhouseDataTableMetadataExtension extension = new ClickhouseDataTableMetadataExtension();
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
