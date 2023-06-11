package cc.cornerstones.biz.datasource.service.assembly.database.mysql;

import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.AbcTuple2;
import cc.cornerstones.almond.utils.AbcMysqlUtils;
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Component
public class MysqlDdlHandler implements DdlHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MysqlDdlHandler.class);

    @Autowired
    private MysqlConnectivityHandler connectivityHandler;

    /**
     * Database server type
     *
     * @return
     */
    @Override
    public DatabaseServerTypeEnum type() {
        return DatabaseServerTypeEnum.MYSQL;
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

        if (!(connection instanceof MysqlConnection)) {
            throw new AbcResourceConflictException("unexpected mysql connection");
        }

        MysqlConnection objectiveConnection = (MysqlConnection) connection;

        if (objectiveConnection.getConnection() == null) {
            throw new AbcResourceConflictException("null mysql connection");
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT TABLE_SCHEMA, TABLE_NAME, TABLE_COMMENT, TABLE_TYPE FROM information_schema.tables WHERE " +
                "TABLE_SCHEMA NOT IN \n" +
                "('information_schema', 'mysql', 'performance_schema', 'sys');");
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
                        case "BASE TABLE":
                            dataTableMetadata.setType(DataTableTypeEnum.DATABASE_TABLE);
                            break;
                        case "VIEW":
                            dataTableMetadata.setType(DataTableTypeEnum.DATABASE_VIEW);
                            break;
                        default:
                            LOGGER.warn("unsupported mysql table type:{}", tableType);
                            // 默认
                            dataTableMetadata.setType(DataTableTypeEnum.DATABASE_TABLE);
                            break;
                    }
                }
                dataTableMetadata.setContextPath(new ArrayList<>(1));
                dataTableMetadata.getContextPath().add(tableSchema);

                MysqlDataTableMetadataExtension extension = new MysqlDataTableMetadataExtension();
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

        if (!(connection instanceof MysqlConnection)) {
            throw new AbcIllegalParameterException("unexpected mysql connection");
        }

        MysqlConnection objectiveConnection = (MysqlConnection) connection;

        if (objectiveConnection.getConnection() == null) {
            throw new AbcIllegalParameterException("null mysql connection");
        }

        if (dataTableMetadata == null) {
            throw new AbcIllegalParameterException("null data table metadata");
        }

        JSONObject extension = dataTableMetadata.getExtension();
        MysqlDataTableMetadataExtension objectiveDataTableMetadataExtension = JSONObject.toJavaObject(extension,
                MysqlDataTableMetadataExtension.class);

        StringBuilder sql = new StringBuilder();
        sql.append("SHOW FULL COLUMNS FROM");
        sql.append(" ")
                .append(AbcMysqlUtils.buildEscapedName(objectiveDataTableMetadataExtension.getDatabaseName()))
                .append(".")
                .append(AbcMysqlUtils.buildEscapedName(objectiveDataTableMetadataExtension.getTableName()));
        LOGGER.info("sql:{}", sql);

        List<DataColumnMetadata> dataColumnMetadataList = new LinkedList<>();
        ResultSet resultSet = null;
        try (PreparedStatement statement = objectiveConnection.getConnection().prepareStatement(sql.toString());) {
            resultSet = statement.executeQuery();

            int ordinalPosition = 0;
            while (resultSet.next()) {
                String fieldName = resultSet.getString("Field");
                String fieldTypeAndLength = resultSet.getString("Type");
                String fieldDescription = resultSet.getString("Comment");

                AbcTuple2<DataColumnTypeEnum, String> transformedDataFieldTypeAndLength =
                        transformDataColumnTypeAndLength(fieldTypeAndLength);

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
            String originalDataColumnTypeAndLength) {
        DataColumnTypeEnum dataColumnType = null;
        String dataColumnLength = null;

        if (originalDataColumnTypeAndLength.startsWith("bit")) {
            dataColumnType = DataColumnTypeEnum.BOOLEAN;

            if (originalDataColumnTypeAndLength.length() > "bit".length()) {
                dataColumnLength = originalDataColumnTypeAndLength.substring("bit".length() + 1,
                        originalDataColumnTypeAndLength.length() - 1);
            }
        } else if (originalDataColumnTypeAndLength.startsWith("tinyint")) {
            dataColumnType = DataColumnTypeEnum.TINYINT;

            if (originalDataColumnTypeAndLength.length() > "tinyint".length()) {
                dataColumnLength = originalDataColumnTypeAndLength.substring("tinyint".length() + 1,
                        originalDataColumnTypeAndLength.length() - 1);
            }
        } else if (originalDataColumnTypeAndLength.startsWith("smallint")) {
            dataColumnType = DataColumnTypeEnum.SMALLINT;

            if (originalDataColumnTypeAndLength.length() > "smallint".length()) {
                dataColumnLength = originalDataColumnTypeAndLength.substring("smallint".length() + 1,
                        originalDataColumnTypeAndLength.length() - 1);
            }
        } else if (originalDataColumnTypeAndLength.startsWith("mediumint")) {
            dataColumnType = DataColumnTypeEnum.MEDIUMINT;

            if (originalDataColumnTypeAndLength.length() > "mediumint".length()) {
                dataColumnLength = originalDataColumnTypeAndLength.substring("mediumint".length() + 1,
                        originalDataColumnTypeAndLength.length() - 1);
            }
        } else if (originalDataColumnTypeAndLength.startsWith("int")) {
            dataColumnType = DataColumnTypeEnum.INT;

            if (originalDataColumnTypeAndLength.length() > "int".length()) {
                dataColumnLength = originalDataColumnTypeAndLength.substring("int".length() + 1,
                        originalDataColumnTypeAndLength.length() - 1);
            }
        } else if (originalDataColumnTypeAndLength.startsWith("bigint")) {
            dataColumnType = DataColumnTypeEnum.LONG;

            if (originalDataColumnTypeAndLength.length() > "bigint".length()) {
                dataColumnLength = originalDataColumnTypeAndLength.substring("bigint".length() + 1,
                        originalDataColumnTypeAndLength.length() - 1);
            }
        } else if (originalDataColumnTypeAndLength.startsWith("float")) {
            dataColumnType = DataColumnTypeEnum.DECIMAL;

            if (originalDataColumnTypeAndLength.length() > "float".length()) {
                String str = originalDataColumnTypeAndLength.substring("float".length() + 1,
                        originalDataColumnTypeAndLength.length() - 1);
                dataColumnLength = str;
            }
        } else if (originalDataColumnTypeAndLength.startsWith("double")) {
            dataColumnType = DataColumnTypeEnum.DECIMAL;

            if (originalDataColumnTypeAndLength.length() > "double".length()) {
                String str = originalDataColumnTypeAndLength.substring("double".length() + 1,
                        originalDataColumnTypeAndLength.length() - 1);
                dataColumnLength = str;
            }
        } else if (originalDataColumnTypeAndLength.startsWith("decimal")) {
            dataColumnType = DataColumnTypeEnum.DECIMAL;

            if (originalDataColumnTypeAndLength.length() > "decimal".length()) {
                String str = originalDataColumnTypeAndLength.substring("decimal".length() + 1,
                        originalDataColumnTypeAndLength.length() - 1);
                dataColumnLength = str;
            }
        } else if (originalDataColumnTypeAndLength.startsWith("datetime")) {
            // note: 先判断 datetime，再判断 date
            dataColumnType = DataColumnTypeEnum.DATETIME;

            if (originalDataColumnTypeAndLength.length() > "datetime".length()) {
                dataColumnLength = originalDataColumnTypeAndLength.substring("datetime".length() + 1,
                        originalDataColumnTypeAndLength.length() - 1);
            }
        } else if (originalDataColumnTypeAndLength.startsWith("date")) {
            dataColumnType = DataColumnTypeEnum.DATE;

            if (originalDataColumnTypeAndLength.length() > "date".length()) {
                String str = originalDataColumnTypeAndLength.substring("date".length() + 1,
                        originalDataColumnTypeAndLength.length() - 1);
                dataColumnLength = str;
            }
        } else if (originalDataColumnTypeAndLength.startsWith("timestamp")) {
            // note: 先判断 timestamp，再判断 time
            dataColumnType = DataColumnTypeEnum.TIMESTAMP;

            if (originalDataColumnTypeAndLength.length() > "timestamp".length()) {
                dataColumnLength = originalDataColumnTypeAndLength.substring("timestamp".length() + 1,
                        originalDataColumnTypeAndLength.length() - 1);
            }
        } else if (originalDataColumnTypeAndLength.startsWith("time")) {
            dataColumnType = DataColumnTypeEnum.TIME;

            if (originalDataColumnTypeAndLength.length() > "time".length()) {
                String str = originalDataColumnTypeAndLength.substring("time".length() + 1,
                        originalDataColumnTypeAndLength.length() - 1);
                dataColumnLength = str;
            }
        } else if (originalDataColumnTypeAndLength.startsWith("year")) {
            dataColumnType = DataColumnTypeEnum.YEAR;

            if (originalDataColumnTypeAndLength.length() > "year".length()) {
                String str = originalDataColumnTypeAndLength.substring("year".length() + 1,
                        originalDataColumnTypeAndLength.length() - 1);
                dataColumnLength = str;
            }
        } else if (originalDataColumnTypeAndLength.startsWith("char")) {
            dataColumnType = DataColumnTypeEnum.CHAR;

            if (originalDataColumnTypeAndLength.length() > "char".length()) {
                dataColumnLength = originalDataColumnTypeAndLength.substring("char".length() + 1,
                        originalDataColumnTypeAndLength.length() - 1);
            }
        } else if (originalDataColumnTypeAndLength.startsWith("varchar")) {
            dataColumnType = DataColumnTypeEnum.VARCHAR;

            if (originalDataColumnTypeAndLength.length() > "varchar".length()) {
                dataColumnLength = originalDataColumnTypeAndLength.substring("varchar".length() + 1,
                        originalDataColumnTypeAndLength.length() - 1);
            }
        } else if (originalDataColumnTypeAndLength.startsWith("text")) {
            dataColumnType = DataColumnTypeEnum.TEXT;

            if (originalDataColumnTypeAndLength.length() > "text".length()) {
                dataColumnLength = originalDataColumnTypeAndLength.substring("text".length() + 1,
                        originalDataColumnTypeAndLength.length() - 1);
            }
        } else if (originalDataColumnTypeAndLength.startsWith("mediumtext")) {
            dataColumnType = DataColumnTypeEnum.TEXT;

            if (originalDataColumnTypeAndLength.length() > "mediumtext".length()) {
                dataColumnLength = originalDataColumnTypeAndLength.substring("mediumtext".length() + 1,
                        originalDataColumnTypeAndLength.length() - 1);
            }
        } else if (originalDataColumnTypeAndLength.startsWith("longtext")) {
            dataColumnType = DataColumnTypeEnum.TEXT;

            if (originalDataColumnTypeAndLength.length() > "longtext".length()) {
                String str = originalDataColumnTypeAndLength.substring("longtext".length() + 1,
                        originalDataColumnTypeAndLength.length() - 1);
                dataColumnLength = str;
            }
        } else if (originalDataColumnTypeAndLength.startsWith("blob")) {
            dataColumnType = DataColumnTypeEnum.BLOB;

            if (originalDataColumnTypeAndLength.length() > "blob".length()) {
                String str = originalDataColumnTypeAndLength.substring("blob".length() + 1,
                        originalDataColumnTypeAndLength.length() - 1);
                dataColumnLength = str;
            }
        } else if (originalDataColumnTypeAndLength.startsWith("mediumblob")) {
            dataColumnType = DataColumnTypeEnum.BLOB;

            if (originalDataColumnTypeAndLength.length() > "mediumblob".length()) {
                String str = originalDataColumnTypeAndLength.substring("mediumblob".length() + 1,
                        originalDataColumnTypeAndLength.length() - 1);
                dataColumnLength = str;
            }
        }  else if (originalDataColumnTypeAndLength.startsWith("longblob")) {
            dataColumnType = DataColumnTypeEnum.BLOB;

            if (originalDataColumnTypeAndLength.length() > "longblob".length()) {
                String str = originalDataColumnTypeAndLength.substring("longblob".length() + 1,
                        originalDataColumnTypeAndLength.length() - 1);
                dataColumnLength = str;
            }
        } else if (originalDataColumnTypeAndLength.startsWith("json")) {
            dataColumnType = DataColumnTypeEnum.JSON;
        } else if (originalDataColumnTypeAndLength.startsWith("varbinary")) {
            dataColumnType = DataColumnTypeEnum.BLOB;

            if (originalDataColumnTypeAndLength.length() > "varbinary".length()) {
                String str = originalDataColumnTypeAndLength.substring("varbinary".length() + 1,
                        originalDataColumnTypeAndLength.length() - 1);
                dataColumnLength = str;
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

        if (!(connection instanceof MysqlConnection)) {
            throw new AbcIllegalParameterException("unexpected mysql connection");
        }

        MysqlConnection objectiveConnection = (MysqlConnection) connection;

        if (objectiveConnection.getConnection() == null) {
            throw new AbcIllegalParameterException("null mysql connection");
        }

        if (dataTableMetadata == null) {
            throw new AbcIllegalParameterException("null data table metadata");
        }

        JSONObject extension = dataTableMetadata.getExtension();
        MysqlDataTableMetadataExtension objectiveDataTableMetadataExtension = JSONObject.toJavaObject(extension,
                MysqlDataTableMetadataExtension.class);

        StringBuilder sql = new StringBuilder();
        sql.append("SHOW INDEX FROM");
        sql.append(" ")
                .append(AbcMysqlUtils.buildEscapedName(objectiveDataTableMetadataExtension.getDatabaseName()))
                .append(".")
                .append(AbcMysqlUtils.buildEscapedName(objectiveDataTableMetadataExtension.getTableName()));
        LOGGER.info("sql:{}", sql);

        ResultSet resultSet = null;
        try (PreparedStatement statement = objectiveConnection.getConnection().prepareStatement(sql.toString());) {
            resultSet = statement.executeQuery();

            Map<String, DataIndexMetadata> indexMap = new HashMap<>();
            Map<String, List<AbcTuple2<String, Integer>>> indexAndListOfColumnNameAndSequenceInIndexMap =
                    new HashMap<>();
            while (resultSet.next()) {
                String indexName = resultSet.getString("Key_name");
                Integer nonUniqueIndex = resultSet.getInt("Non_unique");
                Boolean uniqueIndex = false;
                if (nonUniqueIndex != 1) {
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
                Integer columnSequenceInIndex = resultSet.getInt("Seq_in_index");
                String columnName = resultSet.getString("Column_name");

                List<AbcTuple2<String, Integer>> listOfColumnNameAndSequenceInIndex =
                        indexAndListOfColumnNameAndSequenceInIndexMap.get(indexName);
                if (listOfColumnNameAndSequenceInIndex == null) {
                    listOfColumnNameAndSequenceInIndex = new LinkedList<>();
                    indexAndListOfColumnNameAndSequenceInIndexMap.put(indexName, listOfColumnNameAndSequenceInIndex);
                }
                listOfColumnNameAndSequenceInIndex.add(new AbcTuple2<>(columnName, columnSequenceInIndex));
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

        if (!(connection instanceof MysqlConnection)) {
            throw new AbcResourceConflictException("unexpected mysql connection");
        }

        MysqlConnection objectiveConnection = (MysqlConnection) connection;

        if (objectiveConnection.getConnection() == null) {
            throw new AbcResourceConflictException("null mysql connection");
        }

        if (tableContextPath.size() != 1) {
            throw new AbcResourceConflictException(String.format("unexpected context path::%s",
                    AbcStringUtils.toString(tableContextPath, ",")));
        }

        String databaseName = tableContextPath.get(0);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT TABLE_SCHEMA, TABLE_NAME, TABLE_COMMENT, TABLE_TYPE FROM information_schema.tables WHERE " +
                "TABLE_SCHEMA = ").append(AbcMysqlUtils.surroundBySingleQuotes(databaseName)).append(";");
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
                        case "BASE TABLE":
                            dataTableMetadata.setType(DataTableTypeEnum.DATABASE_TABLE);
                            break;
                        case "VIEW":
                            dataTableMetadata.setType(DataTableTypeEnum.DATABASE_VIEW);
                            break;
                        default:
                            LOGGER.warn("unsupported mysql table type:{}", tableType);
                            // 默认
                            dataTableMetadata.setType(DataTableTypeEnum.DATABASE_TABLE);
                            break;
                    }
                }
                dataTableMetadata.setContextPath(new ArrayList<>(1));
                dataTableMetadata.getContextPath().add(tableSchema);

                MysqlDataTableMetadataExtension extension = new MysqlDataTableMetadataExtension();
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

        if (!(connection instanceof MysqlConnection)) {
            throw new AbcResourceConflictException("unexpected mysql connection");
        }

        MysqlConnection objectiveConnection = (MysqlConnection) connection;

        if (objectiveConnection.getConnection() == null) {
            throw new AbcResourceConflictException("null mysql connection");
        }

        if (CollectionUtils.isEmpty(contextPath)) {
            throw new AbcResourceConflictException("null or empty context path");
        }

        String databaseName = contextPath.get(0);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT TABLE_SCHEMA, TABLE_NAME, TABLE_COMMENT, TABLE_TYPE FROM information_schema.tables WHERE " +
                "TABLE_SCHEMA").append(" = ").append(AbcMysqlUtils.surroundBySingleQuotes(databaseName));
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
                        case "BASE TABLE":
                            dataTableMetadata.setType(DataTableTypeEnum.DATABASE_TABLE);
                            break;
                        case "VIEW":
                            dataTableMetadata.setType(DataTableTypeEnum.DATABASE_VIEW);
                            break;
                        default:
                            LOGGER.warn("unsupported mysql table type:{}", tableType);
                            // 默认
                            dataTableMetadata.setType(DataTableTypeEnum.DATABASE_TABLE);
                            break;
                    }
                }
                dataTableMetadata.setContextPath(new ArrayList<>(1));
                dataTableMetadata.getContextPath().add(tableSchema);

                MysqlDataTableMetadataExtension extension = new MysqlDataTableMetadataExtension();
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
}
