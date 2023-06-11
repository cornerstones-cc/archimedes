package cc.cornerstones.biz.datasource.service.assembly.database.postgresql;

import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.AbcTuple2;
import cc.cornerstones.almond.utils.AbcMysqlUtils;
import cc.cornerstones.almond.utils.AbcPostgresqlUtils;
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Component
public class PostgresqlDdlHandler implements DdlHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresqlDdlHandler.class);

    @Autowired
    private PostgresqlConnectivityHandler connectivityHandler;

    /**
     * Database server type
     *
     * @return
     */
    @Override
    public DatabaseServerTypeEnum type() {
        return DatabaseServerTypeEnum.POSTGRESQL;
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

        if (!(connection instanceof PostgresqlConnection)) {
            throw new AbcResourceConflictException("unexpected postgresql connection");
        }

        PostgresqlConnection objectiveConnection = (PostgresqlConnection) connection;

        if (objectiveConnection.getConnection() == null) {
            throw new AbcResourceConflictException("null postgresql connection");
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT schemaname, tablename FROM pg_tables WHERE " +
                "schemaname NOT IN \n" +
                "('information_schema');");
        LOGGER.info("sql:{}", sql);

        List<DataTableMetadata> dataTableMetadataList = new LinkedList<>();
        ResultSet resultSet = null;
        try (PreparedStatement statement = objectiveConnection.getConnection().prepareStatement(sql.toString());) {
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String tableSchema = resultSet.getString(1);
                String tableName = resultSet.getString(2);

                DataTableMetadata dataTableMetadata = new DataTableMetadata();
                dataTableMetadata.setName(tableName);
                dataTableMetadata.setDescription(null);
                dataTableMetadata.setType(DataTableTypeEnum.DATABASE_TABLE);
                dataTableMetadata.setContextPath(new ArrayList<>(1));
                dataTableMetadata.getContextPath().add(tableSchema);

                PostgresqlDataTableMetadataExtension extension = new PostgresqlDataTableMetadataExtension();
                extension.setDatabaseName(tableSchema);
                extension.setTableName(tableName);
                extension.setTableComment(null);
                extension.setTableType(null);

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

        if (!(connection instanceof PostgresqlConnection)) {
            throw new AbcIllegalParameterException("unexpected postgresql connection");
        }

        PostgresqlConnection objectiveConnection = (PostgresqlConnection) connection;

        if (objectiveConnection.getConnection() == null) {
            throw new AbcIllegalParameterException("null postgresql connection");
        }

        if (dataTableMetadata == null) {
            throw new AbcIllegalParameterException("null data table metadata");
        }

        JSONObject extension = dataTableMetadata.getExtension();
        PostgresqlDataTableMetadataExtension objectiveDataTableMetadataExtension = JSONObject.toJavaObject(extension,
                PostgresqlDataTableMetadataExtension.class);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT column_name, ordinal_position, data_type")
                .append(" FROM information_schema.columns")
                .append(" WHERE")
                .append(" table_schema = ")
                .append(AbcPostgresqlUtils.surroundBySingleQuotes(objectiveDataTableMetadataExtension.getDatabaseName()))
                .append(" AND table_name = ")
                .append(AbcPostgresqlUtils.surroundBySingleQuotes(objectiveDataTableMetadataExtension.getTableName()));
        LOGGER.info("sql:{}", sql);

        List<DataColumnMetadata> dataColumnMetadataList = new LinkedList<>();
        ResultSet resultSet = null;
        try (PreparedStatement statement = objectiveConnection.getConnection().prepareStatement(sql.toString());) {
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String fieldName = resultSet.getString("column_name");
                String fieldType = resultSet.getString("data_type");
                Integer fieldOrdinalPosition = resultSet.getInt("ordinal_position");

                AbcTuple2<DataColumnTypeEnum, String> transformedDataFieldTypeAndLength =
                        transformDataColumnTypeAndLength(fieldType);

                DataColumnMetadata dataColumnMetadata = new DataColumnMetadata();
                dataColumnMetadata.setName(fieldName);
                dataColumnMetadata.setDescription(null);
                dataColumnMetadata.setType(transformedDataFieldTypeAndLength.f);
                dataColumnMetadata.setLength(transformedDataFieldTypeAndLength.s);
                dataColumnMetadata.setOrdinalPosition(fieldOrdinalPosition * 1.0f);

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

        if (originalDataColumnTypeAndLength.startsWith("boolean")) {
            dataColumnType = DataColumnTypeEnum.BOOLEAN;
        } else if (originalDataColumnTypeAndLength.startsWith("bit")) {
            dataColumnType = DataColumnTypeEnum.BOOLEAN;
        } else if (originalDataColumnTypeAndLength.startsWith("tinyint")) {
            dataColumnType = DataColumnTypeEnum.TINYINT;
        } else if (originalDataColumnTypeAndLength.startsWith("smallint")) {
            dataColumnType = DataColumnTypeEnum.SMALLINT;
        } else if (originalDataColumnTypeAndLength.startsWith("mediumint")) {
            dataColumnType = DataColumnTypeEnum.MEDIUMINT;
        } else if (originalDataColumnTypeAndLength.startsWith("integer")) {
            dataColumnType = DataColumnTypeEnum.INT;
        } else if (originalDataColumnTypeAndLength.startsWith("bigint")) {
            dataColumnType = DataColumnTypeEnum.LONG;
        } else if (originalDataColumnTypeAndLength.startsWith("float")) {
            dataColumnType = DataColumnTypeEnum.DECIMAL;
        } else if (originalDataColumnTypeAndLength.startsWith("double")) {
            dataColumnType = DataColumnTypeEnum.DECIMAL;
        } else if (originalDataColumnTypeAndLength.startsWith("decimal")) {
            dataColumnType = DataColumnTypeEnum.DECIMAL;
        } else if (originalDataColumnTypeAndLength.startsWith("date")) {
            dataColumnType = DataColumnTypeEnum.DATE;
        } else if (originalDataColumnTypeAndLength.startsWith("datetime")) {
            dataColumnType = DataColumnTypeEnum.DATETIME;
        } else if (originalDataColumnTypeAndLength.startsWith("timestamp")) {
            dataColumnType = DataColumnTypeEnum.TIMESTAMP;
        } else if (originalDataColumnTypeAndLength.startsWith("time")) {
            dataColumnType = DataColumnTypeEnum.TIME;
        } else if (originalDataColumnTypeAndLength.startsWith("year")) {
            dataColumnType = DataColumnTypeEnum.YEAR;
        } else if (originalDataColumnTypeAndLength.contains("char")) {
            dataColumnType = DataColumnTypeEnum.CHAR;
        } else if (originalDataColumnTypeAndLength.startsWith("varchar")) {
            dataColumnType = DataColumnTypeEnum.VARCHAR;
        } else if (originalDataColumnTypeAndLength.startsWith("text")) {
            dataColumnType = DataColumnTypeEnum.TEXT;
        } else if (originalDataColumnTypeAndLength.startsWith("mediumtext")) {
            dataColumnType = DataColumnTypeEnum.TEXT;
        } else if (originalDataColumnTypeAndLength.startsWith("longtext")) {
            dataColumnType = DataColumnTypeEnum.TEXT;
        } else if (originalDataColumnTypeAndLength.startsWith("blob")) {
            dataColumnType = DataColumnTypeEnum.BLOB;
        } else if (originalDataColumnTypeAndLength.startsWith("mediumblob")) {
            dataColumnType = DataColumnTypeEnum.BLOB;
        } else if (originalDataColumnTypeAndLength.startsWith("longblob")) {
            dataColumnType = DataColumnTypeEnum.BLOB;
        } else if (originalDataColumnTypeAndLength.startsWith("json")) {
            dataColumnType = DataColumnTypeEnum.JSON;
        } else if (originalDataColumnTypeAndLength.startsWith("varbinary")) {
            dataColumnType = DataColumnTypeEnum.BLOB;
        } else if (originalDataColumnTypeAndLength.startsWith("name")
                || originalDataColumnTypeAndLength.startsWith("oid")
                || originalDataColumnTypeAndLength.startsWith("regproc")
                || originalDataColumnTypeAndLength.startsWith("pg_node_tree")
                || originalDataColumnTypeAndLength.startsWith("ARRAY")
                || originalDataColumnTypeAndLength.startsWith("abstime")
                || originalDataColumnTypeAndLength.startsWith("anyarray")
                || originalDataColumnTypeAndLength.startsWith("bytea")
                || originalDataColumnTypeAndLength.contains("character varying")
                || originalDataColumnTypeAndLength.startsWith("inet")
                || originalDataColumnTypeAndLength.startsWith("pg_dependencies")
                || originalDataColumnTypeAndLength.startsWith("pg_lsn")
                || originalDataColumnTypeAndLength.startsWith("pg_ndistinct")
                || originalDataColumnTypeAndLength.startsWith("real")
                || originalDataColumnTypeAndLength.startsWith("xid")) {
            dataColumnType = DataColumnTypeEnum.TEXT;
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

        if (!(connection instanceof PostgresqlConnection)) {
            throw new AbcResourceConflictException("unexpected postgresql connection");
        }

        PostgresqlConnection objectiveConnection = (PostgresqlConnection) connection;

        if (objectiveConnection.getConnection() == null) {
            throw new AbcResourceConflictException("null postgresql connection");
        }

        if (tableContextPath.size() != 1) {
            throw new AbcResourceConflictException(String.format("unexpected context path::%s",
                    AbcStringUtils.toString(tableContextPath, ",")));
        }

        String databaseName = tableContextPath.get(0);

        DataTableMetadata dataTableMetadata = new DataTableMetadata();
        dataTableMetadata.setName(tableName);
        dataTableMetadata.setDescription(null);
        dataTableMetadata.setType(DataTableTypeEnum.DATABASE_TABLE);
        dataTableMetadata.setContextPath(new ArrayList<>(1));
        dataTableMetadata.getContextPath().add(databaseName);

        PostgresqlDataTableMetadataExtension extension = new PostgresqlDataTableMetadataExtension();
        extension.setDatabaseName(databaseName);
        extension.setTableName(tableName);
        extension.setTableComment(null);
        extension.setTableType(null);

        dataTableMetadata.setExtension((JSONObject) JSONObject.toJSON(extension));

        return dataTableMetadata;
    }

    @Override
    public List<DataTableMetadata> loadDataTableMetadata(
            List<String> contextPath,
            JSONObject connectionProfile) throws AbcUndefinedException {
        DataSourceConnection connection = this.connectivityHandler.createConnection(connectionProfile);
        try {
            return loadDataTableMetadata(contextPath, connection);
        } finally {
            this.connectivityHandler.closeConnection(connection);
        }
    }

    @Override
    public List<DataTableMetadata> loadDataTableMetadata(
            List<String> contextPath,
            DataSourceConnection connection) throws AbcUndefinedException {
        if (connection == null) {
            throw new AbcResourceConflictException("null connection");
        }

        if (!(connection instanceof PostgresqlConnection)) {
            throw new AbcResourceConflictException("unexpected postgresql connection");
        }

        PostgresqlConnection objectiveConnection = (PostgresqlConnection) connection;

        if (objectiveConnection.getConnection() == null) {
            throw new AbcResourceConflictException("null postgresql connection");
        }

        if (CollectionUtils.isEmpty(contextPath)) {
            throw new AbcResourceConflictException("null or empty context path");
        }

        String databaseName = contextPath.get(0);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT schemaname, tablename FROM pg_tables WHERE " +
                "schemaname").append(" = ").append(AbcMysqlUtils.surroundBySingleQuotes(databaseName));
        LOGGER.info("sql:{}", sql);

        List<DataTableMetadata> dataTableMetadataList = new LinkedList<>();
        ResultSet resultSet = null;
        try (PreparedStatement statement = objectiveConnection.getConnection().prepareStatement(sql.toString());) {
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String tableSchema = resultSet.getString(1);
                String tableName = resultSet.getString(2);

                DataTableMetadata dataTableMetadata = new DataTableMetadata();
                dataTableMetadata.setName(tableName);
                dataTableMetadata.setDescription(null);
                dataTableMetadata.setType(DataTableTypeEnum.DATABASE_TABLE);
                dataTableMetadata.setContextPath(new ArrayList<>(1));
                dataTableMetadata.getContextPath().add(tableSchema);

                PostgresqlDataTableMetadataExtension extension = new PostgresqlDataTableMetadataExtension();
                extension.setDatabaseName(tableSchema);
                extension.setTableName(tableName);
                extension.setTableComment(null);
                extension.setTableType(null);

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
}
