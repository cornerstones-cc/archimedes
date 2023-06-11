package cc.cornerstones.biz.datasource.service.assembly.database.mssql;

import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.AbcPagination;
import cc.cornerstones.almond.types.AbcSort;
import cc.cornerstones.almond.utils.AbcMssqlUtils;
import cc.cornerstones.biz.datasource.service.assembly.database.*;
import cc.cornerstones.biz.datasource.share.constants.DatabaseServerTypeEnum;
import cc.cornerstones.biz.datasource.share.types.RowHandler;
import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.sqlserver.visitor.SQLServerSchemaStatVisitor;
import com.alibaba.druid.stat.TableStat;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class MssqlDmlHandler implements DmlHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MssqlDmlHandler.class);

    @Autowired
    private MssqlConnectivityHandler connectivityHandler;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");


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
    public QueryResult loadSampleDataOfDataTable(
            JSONObject connectionProfile,
            DataTableMetadata dataTableMetadata,
            AbcSort sort) throws AbcUndefinedException {
        DataSourceConnection connection = this.connectivityHandler.createConnection(connectionProfile);
        try {
            return loadSampleDataOfDataTable(connection, dataTableMetadata, sort);
        } finally {
            this.connectivityHandler.closeConnection(connection);
        }
    }

    @Override
    public QueryResult loadSampleDataOfDataTable(
            DataSourceConnection connection,
            DataTableMetadata dataTableMetadata,
            AbcSort sort) throws AbcUndefinedException {
        StringBuilder sql = new StringBuilder();

        if (CollectionUtils.isEmpty(dataTableMetadata.getContextPath())
                || dataTableMetadata.getContextPath().size() != 2) {
            throw new AbcResourceConflictException("context path should contain database name and schema name");
        }

        if (sort == null || CollectionUtils.isEmpty(sort.getOrders())) {
            throw new AbcResourceConflictException("should provide sort parameter");
        }

        String databaseName = dataTableMetadata.getContextPath().get(0);
        String schemaName = dataTableMetadata.getContextPath().get(1);

        sql.append("SELECT * FROM ")
                .append(AbcMssqlUtils.buildEscapedName(databaseName))
                .append(".").append(AbcMssqlUtils.buildEscapedName(schemaName))
                .append(".").append(AbcMssqlUtils.buildEscapedName(dataTableMetadata.getName()));

        StringBuilder orderByClause = new StringBuilder();
        sort.getOrders().forEach(order -> {
            if (orderByClause.length() > 0) {
                orderByClause.append(",");
            }
            orderByClause.append(order.getProperty()).append(" ").append(order.getDirection().isAscending() ? "ASC" : "DESC");
        });

        sql.append(" ORDER BY ").append(orderByClause);

        sql.append(" OFFSET " + "0")
                .append(" ROWS FETCH NEXT " + "100")
                .append(" ROWS ONLY");

        return executeQuery(connection, sql.toString());
    }


    @Override
    public QueryResult loadSampleDataOfDataTable(
            JSONObject connectionProfile,
            String statement) throws AbcUndefinedException {
        if (ObjectUtils.isEmpty(statement)) {
            return null;
        }

        StringBuilder sql = new StringBuilder();

        sql.append("SELECT * FROM (")
                .append(statement)
                .append(") AS ").append("tt").append(System.currentTimeMillis())
                .append(" LIMIT 0, 100");

        DataSourceConnection connection = this.connectivityHandler.createConnection(connectionProfile);
        try {
            return executeQuery(connection, sql.toString());
        } finally {
            this.connectivityHandler.closeConnection(connection);
        }
    }

    @Override
    public ParseResult parseQuery(
            JSONObject connectionProfile,
            String queryStatement) throws AbcUndefinedException {
        DataSourceConnection connection = this.connectivityHandler.createConnection(connectionProfile);
        try {
            return parseQuery(connection, queryStatement);
        } finally {
            this.connectivityHandler.closeConnection(connection);
        }
    }

    @Override
    public ParseResult parseQuery(
            DataSourceConnection connection,
            String queryStatement) throws AbcUndefinedException {
        SQLStatement sqlStatement = SQLUtils.parseSingleStatement(queryStatement, DbType.sqlserver);
        if (ObjectUtils.isEmpty(sqlStatement)) {
            return null;
        }

        ParseResult parseResult = new ParseResult();
        parseResult.setTables(new LinkedList<>());
        parseResult.setColumns(new LinkedList<>());

        SQLServerSchemaStatVisitor visitor = new SQLServerSchemaStatVisitor();

        sqlStatement.accept(visitor);

        Map<TableStat.Name, TableStat> tables = visitor.getTables();
        if (CollectionUtils.isEmpty(tables)) {
            throw new AbcIllegalParameterException("illegal query statement, no table found");
        }
        for (Map.Entry<TableStat.Name, TableStat> entry : tables.entrySet()) {
            String name = entry.getKey().getName();
            String[] slices = name.split("\\.");
            if (slices.length == 2) {
                List<String> contextPath = new ArrayList<>(1);
                contextPath.add(slices[0]);
                String tableName = slices[1];

                ParseResult.Table table = new ParseResult.Table();
                table.setName(tableName);
                table.setContextPath(contextPath);
                parseResult.getTables().add(table);
            } else {
                LOGGER.error("illegal table name:{}", name);
                throw new AbcIllegalParameterException(String.format("illegal query statement, illegal table name:%s," +
                        " should contain database name and table name", name));
            }
        }

        return parseResult;

    }

    @Override
    public QueryResult testQuery(
            JSONObject connectionProfile,
            String queryStatement,
            Integer limit) throws AbcUndefinedException {
        DataSourceConnection connection = this.connectivityHandler.createConnection(connectionProfile);
        try {
            return testQuery(connection, queryStatement, limit);
        } finally {
            this.connectivityHandler.closeConnection(connection);
        }
    }

    @Override
    public QueryResult testQuery(
            DataSourceConnection connection,
            String queryStatement,
            Integer limit) throws AbcUndefinedException {
        StringBuilder wrapperSql = new StringBuilder();
        // test query 控制查询结果数量
        wrapperSql.append("SELECT * FROM (")
                .append(queryStatement)
                .append(") ta")
                .append(" OFFSET " + "0")
                .append(" ROWS FETCH NEXT " + limit)
                .append(" ROWS ONLY");
        return executeQuery(connection, wrapperSql.toString());
    }

    @Override
    public QueryResult executeQuery(
            JSONObject connectionProfile,
            String queryStatement) throws AbcUndefinedException {
        DataSourceConnection connection = this.connectivityHandler.createConnection(connectionProfile);
        try {
            return executeQuery(connection, queryStatement);
        } finally {
            this.connectivityHandler.closeConnection(connection);
        }
    }

    @Override
    public QueryResult executeQuery(
            DataSourceConnection connection,
            String queryStatement) throws AbcUndefinedException {
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

        QueryResult queryResult = new QueryResult();
        List<String> columnNames = new ArrayList<>();
        List<Map<String, Object>> rows = new LinkedList<>();
        queryResult.setColumnNames(columnNames);
        queryResult.setRows(rows);

        ResultSet resultSet = null;
        int columnCount = 0;

        LOGGER.info("sql:{}", queryStatement);

        try (PreparedStatement statement =
                     objectiveConnection.getConnection().prepareStatement(queryStatement,
                             ResultSet.TYPE_FORWARD_ONLY
                             , ResultSet.CONCUR_READ_ONLY)) {
            // fixed value
            statement.setFetchSize(10000);

            resultSet = statement.executeQuery();

            while (resultSet.next()) {

                // 利用这个机会收集列头（包含了列名和列序）
                if (columnNames.isEmpty()) {
                    ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
                    columnCount = resultSetMetaData.getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        // column name 取 column label值
                        columnNames.add(resultSetMetaData.getColumnLabel(i));
                    }
                }

                Map<String, Object> row = new HashMap<>();
                rows.add(row);
                for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                    row.put(columnNames.get(columnIndex - 1), resultSet.getObject(columnIndex));
                }
            }

            return queryResult;
        } catch (SQLException e) {
            LOGGER.error("fail to execute sql:{}", queryStatement, e);
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
    public QueryResult executeQuery(
            JSONObject connectionProfile,
            String queryStatement,
            AbcPagination pagination) throws AbcUndefinedException {
        DataSourceConnection connection = this.connectivityHandler.createConnection(connectionProfile);
        try {
            return executeQuery(connection, queryStatement, pagination);
        } finally {
            this.connectivityHandler.closeConnection(connection);
        }
    }

    @Override
    public QueryResult executeQuery(
            DataSourceConnection connection,
            String queryStatement,
            AbcPagination pagination) throws AbcUndefinedException {
        StringBuilder wrappedQueryStatement = new StringBuilder();
        if (pagination != null) {
            String tableAlias = "ta_" + System.currentTimeMillis();
            wrappedQueryStatement
                    .append(" SELECT * FROM ")
                    .append("(").append(queryStatement).append(")").append(" ").append(tableAlias)
                    .append(" OFFSET " + pagination.getPage() * pagination.getSize())
                    .append(" ROWS FETCH NEXT " + pagination.getSize())
                    .append(" ROWS ONLY");
            return executeQuery(connection, wrappedQueryStatement.toString());
        } else {
            return executeQuery(connection, queryStatement);
        }
    }

    @Override
    public void executeQuery(
            JSONObject connectionProfile,
            String queryStatement, RowHandler rowHandler) throws AbcUndefinedException {
        DataSourceConnection connection = this.connectivityHandler.createConnection(connectionProfile);
        try {
            executeQuery(connection, queryStatement, rowHandler);
        } finally {
            this.connectivityHandler.closeConnection(connection);
        }
    }

    @Override
    public void executeQuery(
            DataSourceConnection connection,
            String queryStatement,
            RowHandler<Integer> rowHandler) throws AbcUndefinedException {
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

        ResultSet resultSet = null;

        LOGGER.info("sql:{}", queryStatement);

        final int batchSize = 10000;

        List<String> columnLabels = new LinkedList<>();
        List<List<Object>> rows = new LinkedList<>();

        try (PreparedStatement statement =
                     objectiveConnection.getConnection().prepareStatement(queryStatement,
                             ResultSet.TYPE_FORWARD_ONLY
                             , ResultSet.CONCUR_READ_ONLY)) {
            // fixed value
            statement.setFetchSize(10000);

            resultSet = statement.executeQuery();

            boolean end = false;

            while (resultSet.next()) {
                if (columnLabels.isEmpty()) {
                    for (int columnIndex = 1; columnIndex <= resultSet.getMetaData().getColumnCount(); columnIndex++) {
                        columnLabels.add(resultSet.getMetaData().getColumnLabel(columnIndex));
                    }
                }

                List<Object> row = new LinkedList<>();
                for (int columnIndex = 1; columnIndex <= resultSet.getMetaData().getColumnCount(); columnIndex++) {
                    Object columnValue = resultSet.getObject(columnIndex);

                    if (ObjectUtils.isEmpty(columnValue)) {
                        row.add(resultSet.getObject(columnIndex));
                    } else if (columnValue instanceof java.util.Date
                            || columnValue instanceof java.sql.Date
                            || columnValue instanceof java.sql.Timestamp
                            || columnValue instanceof java.time.LocalDateTime
                            || columnValue instanceof java.time.LocalDate
                            || columnValue instanceof java.time.LocalTime
                            || columnValue instanceof String) {
                        row.add(rowHandler.transformColumnValue(columnValue, columnLabels.get(columnIndex - 1)));
                    } else if (columnValue instanceof BigInteger) {
                        BigInteger bigIntegerObject = (BigInteger) columnValue;
                        long longValue = bigIntegerObject.longValue();
                        row.add(Long.valueOf(longValue));
                    } else {
                        row.add(resultSet.getObject(columnIndex));
                    }
                }

                rows.add(row);

                if (rows.size() >= batchSize) {
                    Integer ret = rowHandler.process(rows, columnLabels);

                    rows.clear();

                    // 遇到返回 -1 终止
                    if (ret != null && ret.intValue() == -1) {
                        end = true;
                        break;
                    }
                }
            }

            // end
            if (!end) {
                if (rows.size() > 0) {
                    rowHandler.process(rows, columnLabels);

                    rows.clear();
                }
            }
        } catch (Exception e) {
            LOGGER.error("fail to execute sql:{}", queryStatement, e);
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
