package cc.cornerstones.biz.datasource.service.assembly.database;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.AbcPagination;
import cc.cornerstones.almond.types.AbcSort;
import cc.cornerstones.biz.datasource.share.constants.DatabaseServerTypeEnum;
import cc.cornerstones.biz.datasource.share.types.RowHandler;
import com.alibaba.fastjson.JSONObject;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;

public interface DmlHandler {
    /**
     * Database server type
     *
     * @return
     */
    DatabaseServerTypeEnum type();

    QueryResult loadSampleDataOfDataTable(
            JSONObject connectionProfile,
            DataTableMetadata dataTableMetadata,
            AbcSort sort) throws AbcUndefinedException;

    QueryResult loadSampleDataOfDataTable(
            DataSourceConnection connection,
            DataTableMetadata dataTableMetadata,
            AbcSort sort) throws AbcUndefinedException;

    QueryResult loadSampleDataOfDataTable(
            JSONObject connectionProfile,
            String statement) throws AbcUndefinedException;

    ParseResult parseQuery(
            JSONObject connectionProfile,
            String queryStatement) throws AbcUndefinedException;

    ParseResult parseQuery(
            DataSourceConnection connection,
            String queryStatement) throws AbcUndefinedException;

    QueryResult testQuery(
            JSONObject connectionProfile,
            String queryStatement,
            Integer limit) throws AbcUndefinedException;

    QueryResult testQuery(
            DataSourceConnection connection,
            String queryStatement,
            Integer limit) throws AbcUndefinedException;

    QueryResult executeQuery(
            JSONObject connectionProfile,
            String queryStatement) throws AbcUndefinedException;

    QueryResult executeQuery(
            DataSourceConnection connection,
            String queryStatement) throws AbcUndefinedException;

    QueryResult executeQuery(
            JSONObject connectionProfile,
            String queryStatement,
            AbcPagination pagination) throws AbcUndefinedException;

    QueryResult executeQuery(
            DataSourceConnection connection,
            String queryStatement,
            AbcPagination pagination) throws AbcUndefinedException;

    void executeQuery(
            JSONObject connectionProfile,
            String queryStatement,
            RowHandler<Integer> rowHandler) throws AbcUndefinedException;

    void executeQuery(
            DataSourceConnection connection,
            String queryStatement,
            RowHandler<Integer> rowHandler) throws AbcUndefinedException;

}
