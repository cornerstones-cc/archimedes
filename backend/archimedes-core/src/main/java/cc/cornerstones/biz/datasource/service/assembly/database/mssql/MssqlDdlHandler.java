package cc.cornerstones.biz.datasource.service.assembly.database.mssql;

import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.AbcTuple2;
import cc.cornerstones.almond.utils.AbcMssqlUtils;
import cc.cornerstones.almond.utils.AbcStringUtils;
import cc.cornerstones.biz.datasource.service.assembly.database.*;
import cc.cornerstones.biz.datasource.share.constants.DataColumnTypeEnum;
import cc.cornerstones.biz.datasource.share.constants.DatabaseServerTypeEnum;
import cc.cornerstones.biz.datasource.share.constants.DataTableTypeEnum;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Component
public class MssqlDdlHandler implements DdlHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MssqlDdlHandler.class);

    @Autowired
    private MssqlConnectivityHandler connectivityHandler;

    /**
     * Database server type
     *
     * @return
     */
    @Override
    public DatabaseServerTypeEnum type() {
        return DatabaseServerTypeEnum.MSSQL;
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

    private List<String> loadDatabases(Connection connection) throws AbcUndefinedException {
        List<String> result = new ArrayList<>();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT name FROM master.sys.databases WHERE name NOT IN ('master', 'tempdb', 'model', 'msdb')");
        LOGGER.info("sql:{}", sql);

        ResultSet resultSet = null;
        try (PreparedStatement statement = connection.prepareStatement(sql.toString());) {
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String name = resultSet.getString(1);
                result.add(name);
            }

            return result;
        } catch (SQLException e) {
            LOGGER.error("fail to execute sql:{}", sql, e);
            throw new AbcUndefinedException("fail to execute sql");
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
            DataSourceConnection connection) throws AbcUndefinedException {
        if (connection == null) {
            throw new AbcResourceConflictException("null connection");
        }

        if (!(connection instanceof MssqlConnection)) {
            throw new AbcResourceConflictException("unexpected mssql connection");
        }

        MssqlConnection objectiveConnection = (MssqlConnection) connection;

        if (objectiveConnection.getConnection() == null) {
            throw new AbcResourceConflictException("null mssql connection");
        }

        List<String> databaseNameList = loadDatabases(objectiveConnection.getConnection());
        if (CollectionUtils.isEmpty(databaseNameList)) {
            return null;
        }

        List<DataTableMetadata> dataTableMetadataList = new LinkedList<>();
        databaseNameList.forEach(databaseName -> {
            StringBuilder sql = new StringBuilder();
            sql.append("USE " + AbcMssqlUtils.buildEscapedName(databaseName) + ";");
            sql.append("SELECT TABLE_SCHEMA, TABLE_NAME, TABLE_TYPE FROM INFORMATION_SCHEMA.TABLES");
            LOGGER.info("sql:{}", sql);

            ResultSet resultSet = null;
            try (PreparedStatement statement = objectiveConnection.getConnection().prepareStatement(sql.toString());) {
                resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    String schemaName = resultSet.getString(1);
                    String tableName = resultSet.getString(2);
                    String tableType = resultSet.getString(3);

                    DataTableMetadata dataTableMetadata = new DataTableMetadata();
                    dataTableMetadata.setName(tableName);
                    dataTableMetadata.setDescription(null);
                    if (ObjectUtils.isEmpty(tableType)) {
                        // 默认
                        dataTableMetadata.setType(DataTableTypeEnum.DATABASE_TABLE);
                    } else {
                        switch (tableType) {
                            case "BASE TABLE":
                                dataTableMetadata.setType(DataTableTypeEnum.DATABASE_TABLE);
                                break;
                            case "VIEW":
                                dataTableMetadata.setType(DataTableTypeEnum.DATABASE_VIEW);
                                break;
                            default:
                                LOGGER.warn("unsupported mssql table type:{}", tableType);
                                // 默认
                                dataTableMetadata.setType(DataTableTypeEnum.DATABASE_TABLE);
                                break;
                        }
                    }
                    dataTableMetadata.setContextPath(new ArrayList<>(1));
                    dataTableMetadata.getContextPath().add(databaseName);
                    dataTableMetadata.getContextPath().add(schemaName);

                    MssqlDataTableMetadataExtension extension = new MssqlDataTableMetadataExtension();
                    extension.setDatabaseName(databaseName);
                    extension.setSchemaName(schemaName);
                    extension.setTableName(tableName);
                    extension.setTableType(tableType);

                    dataTableMetadata.setExtension((JSONObject) JSONObject.toJSON(extension));

                    dataTableMetadataList.add(dataTableMetadata);
                }
            } catch (SQLException e) {
                LOGGER.error("fail to execute sql:{}", sql, e);
            } finally {
                if (resultSet != null) {
                    try {
                        resultSet.close();
                    } catch (SQLException e) {
                        LOGGER.error("fail to close result set", e);
                    }
                }
            }
        });

        return dataTableMetadataList;
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

        if (!(connection instanceof MssqlConnection)) {
            throw new AbcIllegalParameterException("unexpected mssql connection");
        }

        MssqlConnection objectiveConnection = (MssqlConnection) connection;

        if (objectiveConnection.getConnection() == null) {
            throw new AbcIllegalParameterException("null mssql connection");
        }

        if (dataTableMetadata == null) {
            throw new AbcIllegalParameterException("null data table metadata");
        }

        JSONObject extension = dataTableMetadata.getExtension();
        MssqlDataTableMetadataExtension objectiveDataTableMetadataExtension = JSONObject.toJavaObject(extension,
                MssqlDataTableMetadataExtension.class);

        StringBuilder sql = new StringBuilder();
        sql.append("USE " + AbcMssqlUtils.buildEscapedName(objectiveDataTableMetadataExtension.getDatabaseName()) + ";");
        sql.append("SELECT");
        sql.append("    COLUMN_NAME,");
        sql.append("    DATA_TYPE,");
        sql.append("    CHARACTER_MAXIMUM_LENGTH");
        sql.append("  FROM");
        sql.append("    INFORMATION_SCHEMA.COLUMNS");
        sql.append("  WHERE");
        sql.append("    TABLE_CATALOG = " + AbcMssqlUtils.surroundBySingleQuotes(objectiveDataTableMetadataExtension.getDatabaseName()));
        sql.append("    AND TABLE_SCHEMA = " + AbcMssqlUtils.surroundBySingleQuotes(objectiveDataTableMetadataExtension.getSchemaName()));
        sql.append("    AND TABLE_NAME = " + AbcMssqlUtils.surroundBySingleQuotes(objectiveDataTableMetadataExtension.getTableName()));
        sql.append("  ORDER BY");
        sql.append("    ORDINAL_POSITION ASC;");
        LOGGER.info("sql:{}", sql);

        List<DataColumnMetadata> dataColumnMetadataList = new LinkedList<>();
        ResultSet resultSet = null;
        try (PreparedStatement statement = objectiveConnection.getConnection().prepareStatement(sql.toString());) {
            resultSet = statement.executeQuery();

            int ordinalPosition = 0;

            while (resultSet.next()) {
                String fieldName = resultSet.getString(1);
                String fieldType = resultSet.getString(2);
                Integer characterMaximumLength = resultSet.getInt(3);

                AbcTuple2<DataColumnTypeEnum, String> transformedDataFieldTypeAndLength =
                        transformDataColumnTypeAndLength(fieldType, characterMaximumLength);

                DataColumnMetadata dataColumnMetadata = new DataColumnMetadata();
                dataColumnMetadata.setName(fieldName);
                dataColumnMetadata.setType(transformedDataFieldTypeAndLength.f);
                dataColumnMetadata.setLength(transformedDataFieldTypeAndLength.s);
                dataColumnMetadata.setOrdinalPosition((ordinalPosition++) * 1.0f);

                dataColumnMetadataList.add(dataColumnMetadata);
            }

            return dataColumnMetadataList;
        } catch (SQLException e) {
            LOGGER.error("fail to execute sql:{}", sql, e);
            return null;
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

    private AbcTuple2<DataColumnTypeEnum, String> transformDataColumnTypeAndLength(
            String originalDataColumnType, Integer characterMaximumLength) {
        DataColumnTypeEnum dataColumnType = null;
        String dataColumnLength = null;

        if (originalDataColumnType.startsWith("bit")) {
            dataColumnType = DataColumnTypeEnum.BOOLEAN;
        } else if (originalDataColumnType.startsWith("tinyint")) {
            dataColumnType = DataColumnTypeEnum.TINYINT;
        } else if (originalDataColumnType.startsWith("mediumint")) {
            dataColumnType = DataColumnTypeEnum.MEDIUMINT;
        } else if (originalDataColumnType.startsWith("int")) {
            dataColumnType = DataColumnTypeEnum.INT;
        } else if (originalDataColumnType.startsWith("bigint")) {
            dataColumnType = DataColumnTypeEnum.LONG;
        } else if (originalDataColumnType.startsWith("decimal")) {
            dataColumnType = DataColumnTypeEnum.DECIMAL;
        } else if (originalDataColumnType.startsWith("numeric")) {
            dataColumnType = DataColumnTypeEnum.DECIMAL;
        } else if (originalDataColumnType.startsWith("date")) {
            dataColumnType = DataColumnTypeEnum.DATE;
        } else if (originalDataColumnType.startsWith("datetime")) {
            dataColumnType = DataColumnTypeEnum.DATETIME;
        } else if (originalDataColumnType.startsWith("time")) {
            dataColumnType = DataColumnTypeEnum.TIME;
        } else if (originalDataColumnType.startsWith("varchar")) {
            dataColumnType = DataColumnTypeEnum.VARCHAR;
            dataColumnLength = String.valueOf(characterMaximumLength);
        } else if (originalDataColumnType.startsWith("nvarchar")) {
            dataColumnType = DataColumnTypeEnum.VARCHAR;
            dataColumnLength = String.valueOf(characterMaximumLength);
        } else if (originalDataColumnType.startsWith("longtext")) {
            dataColumnType = DataColumnTypeEnum.TEXT;
            dataColumnLength = String.valueOf(characterMaximumLength);
        } else {
            dataColumnType = DataColumnTypeEnum.TEXT;
            dataColumnLength = "45";
        }

        return new AbcTuple2(dataColumnType, dataColumnLength);
    }

    @Override
    public List<DataIndexMetadata> loadDataIndexMetadataOfDataTable(
            JSONObject connectionProfile,
            DataTableMetadata dataTableMetadata) throws AbcUndefinedException {
        return null;
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

        if (!(connection instanceof MssqlConnection)) {
            throw new AbcResourceConflictException("unexpected mssql connection");
        }

        MssqlConnection objectiveConnection = (MssqlConnection) connection;

        if (objectiveConnection.getConnection() == null) {
            throw new AbcResourceConflictException("null mssql connection");
        }

        if (tableContextPath.size() != 2) {
            throw new AbcResourceConflictException(String.format("unexpected context path::%s",
                    AbcStringUtils.toString(tableContextPath, ",")));
        }

        String databaseName = tableContextPath.get(0);
        String schemaName = tableContextPath.get(1);


        StringBuilder sql = new StringBuilder();
        sql.append("USE " + AbcMssqlUtils.buildEscapedName(databaseName) + ";");
        sql.append("SELECT TABLE_SCHEMA, TABLE_NAME, TABLE_TYPE FROM INFORMATION_SCHEMA.TABLES WHERE")
                .append(" TABLE_SCHEMA = ").append(schemaName)
                .append(" AND TABLE_NAME = ").append(tableName);
        LOGGER.info("sql:{}", sql);

        ResultSet resultSet = null;
        try (PreparedStatement statement = objectiveConnection.getConnection().prepareStatement(sql.toString());) {
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                String tableType = resultSet.getString(3);

                DataTableMetadata dataTableMetadata = new DataTableMetadata();
                dataTableMetadata.setName(tableName);
                dataTableMetadata.setDescription(null);
                if (ObjectUtils.isEmpty(tableType)) {
                    // 默认
                    dataTableMetadata.setType(DataTableTypeEnum.DATABASE_TABLE);
                } else {
                    switch (tableType) {
                        case "BASE TABLE":
                            dataTableMetadata.setType(DataTableTypeEnum.DATABASE_TABLE);
                            break;
                        case "VIEW":
                            dataTableMetadata.setType(DataTableTypeEnum.DATABASE_VIEW);
                            break;
                        default:
                            LOGGER.warn("unsupported mssql table type:{}", tableType);
                            // 默认
                            dataTableMetadata.setType(DataTableTypeEnum.DATABASE_TABLE);
                            break;
                    }
                }
                dataTableMetadata.setContextPath(new ArrayList<>(2));
                dataTableMetadata.getContextPath().add(databaseName);
                dataTableMetadata.getContextPath().add(schemaName);

                MssqlDataTableMetadataExtension extension = new MssqlDataTableMetadataExtension();
                extension.setDatabaseName(databaseName);
                extension.setSchemaName(schemaName);
                extension.setTableName(tableName);
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
