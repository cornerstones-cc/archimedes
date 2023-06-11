package cc.cornerstones.biz.datasource.service.assembly.database.mysql;

import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.biz.datasource.service.assembly.database.ConnectivityHandler;
import cc.cornerstones.biz.datasource.service.assembly.database.DataSourceConnection;
import cc.cornerstones.biz.datasource.share.constants.DatabaseServerTypeEnum;
import com.alibaba.druid.DbType;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.ObjectUtils;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


@Component
public class MysqlConnectivityHandler implements ConnectivityHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MysqlConnectivityHandler.class);

    private static final String JDBC_DRIVER_PREFIX = "jdbc:mysql://";
    private static final String DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver";
    private static final String USER_SYMBOL = "user";
    private static final String PASSWORD_SYMBOL = "password";
    private static final String USE_CURSOR_FETCH = "useCursorFetch";

    private static Map<String, DruidDataSource> dataSourceMap = new HashMap<>();

    @Scheduled(cron = "0 59 23 * * ?")
    public void cleanup() throws AbcUndefinedException {
        // 防止 data source map 积累了太多 data source 对象
        dataSourceMap.values().forEach(dataSource -> {
            dataSource.close();
        });

        dataSourceMap.clear();
    }

    private static DruidDataSource getDataSource(MysqlConnectionProfile objectiveConnectionProfile) {
        String hashedKey =
                DigestUtils.md5DigestAsHex(JSONObject.toJSONString(objectiveConnectionProfile).getBytes(StandardCharsets.UTF_8));
        DruidDataSource dataSource = dataSourceMap.get(hashedKey);

        if (dataSource == null) {
            dataSource = new DruidDataSource();

            StringBuilder url = new StringBuilder();
            url.append(JDBC_DRIVER_PREFIX)
                    .append(objectiveConnectionProfile.getHost())
                    .append(":").append(objectiveConnectionProfile.getPort());
            if (!ObjectUtils.isEmpty(objectiveConnectionProfile.getDatabase())) {
                url.append("/").append(objectiveConnectionProfile.getDatabase());
            }

            // optional properties
            Properties properties = new Properties();
            properties.setProperty(USE_CURSOR_FETCH, "true");
            if (!CollectionUtils.isEmpty(objectiveConnectionProfile.getProperties())) {
                objectiveConnectionProfile.getProperties().forEach(optionalProperty -> {
                    if (optionalProperty.f.equalsIgnoreCase(USE_CURSOR_FETCH)) {
                        return;
                    }
                    properties.setProperty(optionalProperty.f, optionalProperty.s);
                });
            }

            dataSource.setUrl(url.toString());
            dataSource.setDriverClassName(DRIVER_CLASS_NAME);
            dataSource.setUsername(objectiveConnectionProfile.getUser());
            dataSource.setPassword(objectiveConnectionProfile.getPassword());
            dataSource.setConnectProperties(properties);
            dataSource.setDbType(DbType.mysql);

            dataSource.setInitialSize(5);
            dataSource.setMaxActive(30);
            dataSource.setMinIdle(5);
            dataSource.setMaxWait(30000);
            dataSource.setTestOnBorrow(true);
            dataSource.setTestOnReturn(true);
            dataSource.setTestWhileIdle(true);
            dataSource.setValidationQuery("select 1");
            dataSource.setValidationQueryTimeout(34000);
            dataSource.setMinEvictableIdleTimeMillis(30000);
            dataSource.setMaxEvictableIdleTimeMillis(55000);
            dataSource.setTimeBetweenEvictionRunsMillis(34000);

            dataSourceMap.put(hashedKey, dataSource);
        }

        return dataSource;
    }

    /**
     * Database Server Type
     *
     * @return
     */
    @Override
    public DatabaseServerTypeEnum type() {
        return DatabaseServerTypeEnum.MYSQL;
    }

    /**
     * 测试连通性
     *
     * @param connectionProfile
     * @throws AbcUndefinedException
     */
    @Override
    public void testConnectivity(
            JSONObject connectionProfile) throws AbcUndefinedException {
        DataSourceConnection connection = createConnection(connectionProfile);
        closeConnection(connection);
    }

    /**
     * 创建连接
     *
     * @param connectionProfile
     * @return
     * @throws AbcUndefinedException
     */
    @Override
    public DataSourceConnection createConnection(
            JSONObject connectionProfile) throws AbcUndefinedException {
        if (connectionProfile == null || connectionProfile.isEmpty()) {
            throw new AbcIllegalParameterException("null or empty connection profile");
        }

        MysqlConnectionProfile objectiveConnectionProfile = null;
        try {
            objectiveConnectionProfile = JSONObject.toJavaObject(connectionProfile,
                    MysqlConnectionProfile.class);
        } catch (Exception e) {
            LOGGER.error("fail to transform mysql connection profile:{}", connectionProfile, e);
            throw new AbcResourceConflictException("unexpected mysql connection profile");
        }

        if (objectiveConnectionProfile == null) {
            throw new AbcIllegalParameterException("null connection profile");
        }
        if (ObjectUtils.isEmpty(objectiveConnectionProfile.getHost())
                || objectiveConnectionProfile.getPort() == null
                || ObjectUtils.isEmpty(objectiveConnectionProfile.getUser())
                || ObjectUtils.isEmpty(objectiveConnectionProfile.getPassword())) {
            throw new AbcIllegalParameterException("illegal connection profile, host, port, user, password are all " +
                    "required");
        }

        Connection connection = null;
        try {
            connection = getDataSource(objectiveConnectionProfile).getConnection();

            MysqlConnection objectiveConnection = new MysqlConnection();
            objectiveConnection.setConnection(connection);
            return objectiveConnection;
        } catch (SQLException e) {
            LOGGER.error("fail to create mysql connection for {}", connectionProfile, e);
            throw new AbcResourceConflictException("fail to create mysql connection");
        } finally {
            // DO NOT close connection here
        }


        /*

        StringBuilder url = new StringBuilder();

        url.append(JDBC_DRIVER_PREFIX)
                .append(objectiveConnectionProfile.getHost())
                .append(":").append(objectiveConnectionProfile.getPort());

        if (!ObjectUtils.isEmpty(objectiveConnectionProfile.getDatabase())) {
            url.append("/").append(objectiveConnectionProfile.getDatabase());
        }

        Properties properties = new Properties();
        properties.setProperty(USER_SYMBOL, objectiveConnectionProfile.getUser());
        properties.setProperty(PASSWORD_SYMBOL, objectiveConnectionProfile.getPassword());
        // optional properties
        if (!CollectionUtils.isEmpty(objectiveConnectionProfile.getProperties())) {
            objectiveConnectionProfile.getProperties().forEach(optionalProperties -> {
                properties.setProperty(optionalProperties.f, optionalProperties.s);
            });
        }

        Connection connection = null;
        try {
            connection = DriverManager.getConnection(url.toString(), properties);

            MysqlConnection objectiveConnection = new MysqlConnection();
            objectiveConnection.setConnection(connection);
            return objectiveConnection;
        } catch (SQLException e) {
            LOGGER.error("fail to create mysql connection for {}", url, e);
            throw new AbcResourceConflictException("fail to create mysql connection");
        } finally {
            // DO NOT close connection here
        }
        */
    }

    /**
     * 关闭连接
     *
     * @param connection
     * @throws AbcUndefinedException
     */
    @Override
    public void closeConnection(
            DataSourceConnection connection) throws AbcUndefinedException {
        if (connection == null) {
            throw new AbcResourceConflictException("null connection");
        }

        if (!(connection instanceof MysqlConnection)) {
            throw new AbcResourceConflictException("unexpected mysql connection");
        }

        MysqlConnection objectiveConnection = (MysqlConnection) connection;

        if (objectiveConnection.getConnection() == null) {
            return;
        }

        try {
            if (objectiveConnection.getConnection().isClosed()) {
                return;
            }
        } catch (SQLException e) {
            LOGGER.warn("fail to check mysql connection status", e);
        }

        try {
            objectiveConnection.getConnection().close();
        } catch (SQLException e) {
            LOGGER.error("fail to close mysql connection", e);

            throw new AbcResourceConflictException("fail to close mysql connection");
        }
    }

    /**
     * 提取 Host
     *
     * @param connectionProfile
     * @return
     * @throws AbcUndefinedException
     */
    @Override
    public JSONObject extractHostProfile(
            JSONObject connectionProfile) throws AbcUndefinedException {
        if (connectionProfile == null || connectionProfile.isEmpty()) {
            throw new AbcIllegalParameterException("null or empty connection profile");
        }

        MysqlConnectionProfile objectiveConnectionProfile = null;
        try {
            objectiveConnectionProfile = JSONObject.toJavaObject(connectionProfile,
                    MysqlConnectionProfile.class);
        } catch (Exception e) {
            LOGGER.error("fail to transform mysql connection profile:{}", connectionProfile, e);
            throw new AbcResourceConflictException("unexpected mysql connection profile");
        }

        JSONObject result = new JSONObject();
        result.put("host", objectiveConnectionProfile.getHost());
        result.put("port", objectiveConnectionProfile.getPort());
        return result;
    }

    /**
     * 隐藏保密信息
     *
     * @param connectionProfile
     * @return
     * @throws AbcUndefinedException
     */
    @Override
    public JSONObject excludeConfidentialInformation(
            JSONObject connectionProfile) throws AbcUndefinedException {
        if (connectionProfile == null || connectionProfile.isEmpty()) {
            throw new AbcIllegalParameterException("null or empty connection profile");
        }

        MysqlConnectionProfile objectiveConnectionProfile = null;
        try {
            objectiveConnectionProfile = JSONObject.toJavaObject(connectionProfile,
                    MysqlConnectionProfile.class);
        } catch (Exception e) {
            LOGGER.error("fail to transform mysql connection profile:{}", connectionProfile, e);
            throw new AbcResourceConflictException("unexpected mysql connection profile");
        }

        objectiveConnectionProfile.setPassword(null);

        return (JSONObject) JSONObject.toJSON(objectiveConnectionProfile);
    }
}
