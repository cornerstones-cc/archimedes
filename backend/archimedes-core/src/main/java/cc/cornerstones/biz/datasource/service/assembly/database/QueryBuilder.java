package cc.cornerstones.biz.datasource.service.assembly.database;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.biz.datasource.share.constants.DatabaseServerTypeEnum;
import cc.cornerstones.biz.datasource.dto.DataSourceQueryDto;

public interface QueryBuilder {
    /**
     * Database server type
     *
     * @return
     */
    DatabaseServerTypeEnum type();

    String COUNT_COLUMN_NAME = "cnt";

    void verifyStatement(String statement) throws AbcUndefinedException;

    String buildCountStatement(DataSourceQueryDto dataSourceQueryDto) throws AbcUndefinedException;

    String buildQueryStatement(DataSourceQueryDto dataSourceQueryDto) throws AbcUndefinedException;
}
