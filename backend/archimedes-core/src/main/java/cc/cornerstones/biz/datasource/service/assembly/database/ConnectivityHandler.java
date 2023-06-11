package cc.cornerstones.biz.datasource.service.assembly.database;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.biz.datasource.share.constants.DatabaseServerTypeEnum;
import com.alibaba.fastjson.JSONObject;

public interface ConnectivityHandler {
    /**
     * Type
     *
     * @return
     */
    DatabaseServerTypeEnum type();

    /**
     * 测试连通性
     *
     * @param connectionProfile
     * @throws AbcUndefinedException
     */
    void testConnectivity(JSONObject connectionProfile) throws AbcUndefinedException;

    /**
     * 创建连接
     *
     * @param connectionProfile
     * @return
     * @throws AbcUndefinedException
     */
    DataSourceConnection createConnection(JSONObject connectionProfile) throws AbcUndefinedException;

    /**
     * 关闭连接
     *
     * @param connection
     * @throws AbcUndefinedException
     */
    void closeConnection(DataSourceConnection connection) throws AbcUndefinedException;

    /**
     * 提取 Host
     *
     * @param connectionProfile
     * @return
     * @throws AbcUndefinedException
     */
    JSONObject extractHostProfile(JSONObject connectionProfile) throws AbcUndefinedException;

    /**
     * 隐藏保密信息
     *
     * @param connectionProfile
     * @return
     * @throws AbcUndefinedException
     */
    JSONObject excludeConfidentialInformation(JSONObject connectionProfile) throws AbcUndefinedException;
}
