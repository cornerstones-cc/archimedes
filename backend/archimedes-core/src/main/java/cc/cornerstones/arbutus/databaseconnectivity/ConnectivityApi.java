package cc.cornerstones.arbutus.databaseconnectivity;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import com.clickhouse.jdbc.ClickHouseDataSource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.sql.*;
import java.util.Properties;

/**
 * @author bbottong
 */
@Tag(name = "[Arbutus] Utilities / Server connectivity")
@RestController
@RequestMapping(value = "/utilities/server-connectivity")
public class ConnectivityApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectivityApi.class);

    @Operation(summary = "MSSQL test connection")
    @PostMapping("/mssql")
    @ResponseBody
    public Response performConnectivityTestForMssql(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long userUid,
            @Valid @RequestBody MssqlConnectivityTest connectivityTest) throws Exception {
        StringBuilder connectionString = new StringBuilder();
        connectionString.append("jdbc:sqlserver://")
                .append(connectivityTest.getHost())
                .append(":").append(connectivityTest.getPort());

        Properties properties = new Properties();
        properties.setProperty("user", connectivityTest.getUser());
        properties.setProperty("password", connectivityTest.getPassword());

        if (!ObjectUtils.isEmpty(connectivityTest.getDatabase())) {
            properties.setProperty("databaseName", connectivityTest.getDatabase());
        }
        if (!CollectionUtils.isEmpty(connectivityTest.getProperties())) {
            connectivityTest.getProperties().forEach(optionalProperties -> {
                properties.setProperty(optionalProperties.f, optionalProperties.s);
            });
        }

        java.sql.Connection connection = null;
        try {
            connection = DriverManager.getConnection(connectionString.toString(), properties);

            LOGGER.info(String.format("[CONNECTIVITY] connected to mssql with configuration: %s",
                    connectivityTest));
        } catch (SQLException e) {
            LOGGER.error(String.format("fail to connect to mssql with configuration: %s",
                    connectivityTest), e);

            if (!ObjectUtils.isEmpty(e.getMessage())) {
                throw new Exception(e.getMessage());
            } else {
                throw new Exception("failed to connect to mssql");
            }
        } finally {
            if (connection != null) {
                connection.close();
            }
        }

        return Response.buildSuccess();
    }

    @Operation(summary = "MySQL test connection")
    @PostMapping("/mysql")
    @ResponseBody
    public Response performConnectivityTestForMysql(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long userUid,
            @Valid @RequestBody MysqlConnectivityTest connectivityTest) throws Exception {
        StringBuilder connectionString = new StringBuilder();
        connectionString.append("jdbc:mysql://")
                .append(connectivityTest.getHost())
                .append(":").append(connectivityTest.getPort());

        if (!ObjectUtils.isEmpty(connectivityTest.getDatabase())) {
            connectionString.append("/").append(connectivityTest.getDatabase());
        }

        Properties properties = new Properties();
        properties.setProperty("user", connectivityTest.getUser());
        properties.setProperty("password", connectivityTest.getPassword());
        if (!CollectionUtils.isEmpty(connectivityTest.getProperties())) {
            connectivityTest.getProperties().forEach(optionalProperties -> {
                properties.setProperty(optionalProperties.f, optionalProperties.s);
            });
        }

        java.sql.Connection connection = null;
        try {
            connection = DriverManager.getConnection(connectionString.toString(), properties);

            LOGGER.info(String.format("[CONNECTIVITY] connected to mysql with configuration: %s",
                    connectivityTest));

            return Response.buildSuccess();
        } catch (SQLException e) {
            LOGGER.error(String.format("fail to connect to mysql with configuration: %s",
                    connectivityTest.toString()), e);

            if (!ObjectUtils.isEmpty(e.getMessage())) {
                throw new Exception(e.getMessage());
            } else {
                throw new Exception("failed to connect to mysql");
            }
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    @Operation(summary = "PostgreSQL test connection")
    @PostMapping("/postgresql")
    @ResponseBody
    public Response performConnectivityTestForPostgresql(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long userUid,
            @Valid @RequestBody PostgresqlConnectivityTest connectivityTest) throws Exception {
        StringBuilder connectionString = new StringBuilder();
        connectionString.append("jdbc:postgresql://")
                .append(connectivityTest.getHost())
                .append(":").append(connectivityTest.getPort());

        if (!ObjectUtils.isEmpty(connectivityTest.getDatabase())) {
            connectionString.append("/").append(connectivityTest.getDatabase());
        }

        Properties properties = new Properties();
        properties.setProperty("user", connectivityTest.getUser());
        properties.setProperty("password", connectivityTest.getPassword());
        if (!CollectionUtils.isEmpty(connectivityTest.getProperties())) {
            connectivityTest.getProperties().forEach(optionalProperties -> {
                properties.setProperty(optionalProperties.f, optionalProperties.s);
            });
        }

        java.sql.Connection connection = null;
        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection(connectionString.toString(), properties);

            LOGGER.info(String.format("[CONNECTIVITY] connected to postgresql with configuration: %s",
                    connectivityTest));
        } catch (SQLException e) {
            LOGGER.error(String.format("fail to connect to postgresql with configuration: %s",
                    connectivityTest), e);

            if (!ObjectUtils.isEmpty(e.getMessage())) {
                throw new Exception(e.getMessage());
            } else {
                throw new Exception("failed to connect to postgresql");
            }
        } finally {
            if (connection != null) {
                connection.close();
            }
        }

        return Response.buildSuccess();
    }

    @Operation(summary = "Oracle test connection")
    @PostMapping("/oracle")
    @ResponseBody
    public Response performConnectivityTestForOracle(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long userUid,
            @Valid @RequestBody OracleConnectivityTest connectivityTest) throws Exception {
        final String URL_PATTERN_SYSTEM_NAME = "SERVICE_NAME";
        final String URL_PATTERN_SID = "SID";
        final String JDBC_DRIVER_PREFIX = "jdbc:oracle:thin:@";
        final String USER_SYMBOL = "user";
        final String PASSWORD_SYMBOL = "password";
        final String INTERNAL_LOGON_SYMBOL = "internal_logon";

        StringBuilder connectionString = new StringBuilder();

        switch(connectivityTest.getUrlPattern()) {
            case SERVICE_NAME:
                connectionString.append(JDBC_DRIVER_PREFIX);
                connectionString.append("//" + connectivityTest.getHost());
                connectionString.append(":" + connectivityTest.getPort());
                connectionString.append("/" + connectivityTest.getEndpoint());
                break;
            case SID:
                connectionString.append(JDBC_DRIVER_PREFIX);
                connectionString.append(connectivityTest.getHost());
                connectionString.append(":" + connectivityTest.getPort());
                connectionString.append(":" + connectivityTest.getEndpoint());
                break;
            default:
                String errorMessage =
                        "Not supported Oracle DB URL Pattern: " + connectivityTest.getUrlPattern();
                throw new Exception(errorMessage);
        }

        Properties properties = new Properties();
        properties.put(USER_SYMBOL, connectivityTest.getUser());
        properties.put(PASSWORD_SYMBOL, connectivityTest.getPassword());
        properties.put(INTERNAL_LOGON_SYMBOL, connectivityTest.getInternalLogon());

        java.sql.Connection connection = null;
        try {
            connection = DriverManager.getConnection(connectionString.toString(), properties);

            LOGGER.info(String.format("[CONNECTIVITY] connected to oracle with configuration: %s",
                    connectivityTest));
        } catch (SQLException e) {
            LOGGER.error(String.format("fail to connect to oracle with configuration: %s",
                    connectivityTest), e);

            if (!ObjectUtils.isEmpty(e.getMessage())) {
                throw new Exception(e.getMessage());
            } else {
                throw new Exception("failed to connect to oracle");
            }
        } finally {
            if (connection != null) {
                connection.close();
            }
        }

        return Response.buildSuccess();
    }

    @Operation(summary = "ClickHouse test connection")
    @PostMapping("/clickhouse")
    @ResponseBody
    public Response performConnectivityTestForClickhouse(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long userUid,
            @Valid @RequestBody ClickhouseConnectivityTest connectivityTest) throws Exception {
        StringBuilder connectionString = new StringBuilder();
        connectionString.append("jdbc:clickhouse://")
                .append(connectivityTest.getHost())
                .append(":").append(connectivityTest.getPort());

        Properties properties = new Properties();
        properties.setProperty("user", connectivityTest.getUser());
        properties.setProperty("password", connectivityTest.getPassword());
        // optional properties
        if (!CollectionUtils.isEmpty(connectivityTest.getProperties())) {
            connectivityTest.getProperties().forEach(optionalProperties -> {
                properties.setProperty(optionalProperties.f, optionalProperties.s);
            });
        }

        ClickHouseDataSource dataSource = new ClickHouseDataSource(connectionString.toString(), properties);
        java.sql.Connection connection = null;
        try {
            // Class.forName("ru.yandex.clickhouse.ClickHouseDriver");
            connection = dataSource.getConnection();

            LOGGER.info(String.format("[CONNECTIVITY] connected to clickhouse with configuration: %s",
                    connectivityTest));
        } catch (SQLException e) {
            LOGGER.error(String.format("fail to connect to clickhouse with configuration: %s",
                    connectivityTest), e);

            if (!ObjectUtils.isEmpty(e.getMessage())) {
                throw new Exception(e.getMessage());
            } else {
                throw new Exception("failed to connect to clickhouse");
            }
        } finally {
            if (connection != null) {
                connection.close();
            }
        }

        return Response.buildSuccess();
    }
}
