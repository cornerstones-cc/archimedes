package cc.cornerstones.biz.datadictionary.service.assembly;

import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.AbcPagination;
import cc.cornerstones.biz.datasource.entity.DataSourceDo;
import cc.cornerstones.biz.datasource.persistence.DataSourceRepository;
import cc.cornerstones.biz.datasource.service.assembly.database.DmlHandler;
import cc.cornerstones.biz.datasource.service.assembly.database.QueryResult;
import cc.cornerstones.biz.datasource.service.inf.DataSourceService;
import cc.cornerstones.biz.datatable.dto.TestQueryStatementDto;
import cc.cornerstones.biz.datatable.share.constants.DictionaryBuildTypeEnum;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Component
public class DictionaryBuildSqlHandler implements DictionaryBuildHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryBuildSqlHandler.class);


    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private DataSourceService dataSourceService;

    /**
     * Type
     *
     * @return
     */
    @Override
    public DictionaryBuildTypeEnum type() {
        return DictionaryBuildTypeEnum.SQL;
    }

    @Override
    public void validate(JSONObject params) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        if (params == null || params.isEmpty()) {
            throw new AbcResourceConflictException("illegal logic");
        }

        DictionaryBuildSqlLogic logic = null;
        try {
            logic = JSONObject.toJavaObject(params, DictionaryBuildSqlLogic.class);
        } catch (Exception e) {
            LOGGER.error("failed to parse {}", params, e);
            throw new AbcResourceConflictException("illegal logic");
        }

        Long dataSourceUid = logic.getDataSourceUid();
        String queryStatement = logic.getQueryStatement();

        //
        // Step 2, core-processing
        //

        this.dataSourceService.validateQueryStatement(
                dataSourceUid, queryStatement, null);
    }

    @Override
    public QueryResult execute(
            JSONObject params,
            AbcPagination pagination) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        if (params == null || params.isEmpty()) {
            return null;
        }

        DictionaryBuildSqlLogic logic = null;
        try {
            logic = JSONObject.toJavaObject(params, DictionaryBuildSqlLogic.class);
        } catch (Exception e) {
            LOGGER.error("fail to parse params", e);
            return null;
        }

        Long dataSourceUid = logic.getDataSourceUid();
        String queryStatement = logic.getQueryStatement();

        DataSourceDo dataSourceDo = this.dataSourceRepository.findByUid(dataSourceUid);
        if (dataSourceDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataSourceDo.RESOURCE_SYMBOL,
                    dataSourceUid));
        }

        // 验证
        TestQueryStatementDto testQueryStatementDto = new TestQueryStatementDto();
        testQueryStatementDto.setQueryStatement(queryStatement);
        testQueryStatementDto.setLimit(10);
        this.dataSourceService.testQueryStatement(dataSourceUid, testQueryStatementDto, null);

        DmlHandler objectiveDmlHandler = null;
        Map<String, DmlHandler> dmlHandlerMap = this.applicationContext.getBeansOfType(DmlHandler.class);
        if (!CollectionUtils.isEmpty(dmlHandlerMap)) {
            for (Map.Entry<String, DmlHandler> entry : dmlHandlerMap.entrySet()) {
                DmlHandler dmlHandler = entry.getValue();
                if (dmlHandler.type().equals(dataSourceDo.getType())) {
                    objectiveDmlHandler = dmlHandler;
                    break;
                }
            }
        }
        if (objectiveDmlHandler == null) {
            throw new AbcResourceConflictException(
                    String.format("cannot find dml handler of data source type:%s",
                            dataSourceDo.getType()));
        }

        //
        // Step 2, core-processing
        //

        QueryResult queryResult = objectiveDmlHandler.executeQuery(
                dataSourceDo.getConnectionProfile(), queryStatement, pagination);
        return queryResult;
    }

}
