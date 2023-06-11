package cc.cornerstones.biz.datatable.service.assembly;

import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.datasource.entity.DataSourceDo;
import cc.cornerstones.biz.datasource.persistence.DataSourceRepository;
import cc.cornerstones.biz.datasource.service.assembly.database.*;
import cc.cornerstones.biz.datatable.entity.DataTableDo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Map;

@Component
public class IndirectDataTableHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(IndirectDataTableHandler.class);

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSourceRepository dataSourceRepository;

    public QueryResult testQuery(DataSourceDo dataSourceDo, String queryStatement, Integer limit) {
        //
        // step 1, pre-processing
        //

        //
        // step 2, core-processing
        //
        DmlHandler objectiveDmlHandler = null;
        Map<String, DmlHandler> map = this.applicationContext.getBeansOfType(DmlHandler.class);
        if (!CollectionUtils.isEmpty(map)) {
            for (Map.Entry<String, DmlHandler> entry : map.entrySet()) {
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

        return objectiveDmlHandler.testQuery(dataSourceDo.getConnectionProfile(), queryStatement, limit);
    }

    public ParseResult parseQuery(DataSourceDo dataSourceDo, String queryStatement) {
        //
        // step 1, pre-processing
        //

        //
        // step 2, core-processing
        //
        DmlHandler objectiveDmlHandler = null;
        Map<String, DmlHandler> map = this.applicationContext.getBeansOfType(DmlHandler.class);
        if (!CollectionUtils.isEmpty(map)) {
            for (Map.Entry<String, DmlHandler> entry : map.entrySet()) {
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

        return objectiveDmlHandler.parseQuery(dataSourceDo.getConnectionProfile(), queryStatement);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleDirectDataTableChanged(
            DataTableDo dataTableDo,
            UserProfile operatingUserProfile) {

    }
}
