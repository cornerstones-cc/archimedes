package cc.cornerstones.biz.datasource.service.assembly.database;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.biz.datasource.share.constants.DatabaseServerTypeEnum;
import com.alibaba.fastjson.JSONObject;

import java.util.List;

public interface DdlHandler {
    /**
     * Database server type
     *
     * @return
     */
    DatabaseServerTypeEnum type();

    List<DataTableMetadata> loadDataTableMetadata(
            JSONObject connectionProfile) throws AbcUndefinedException;

    List<DataTableMetadata> loadDataTableMetadata(
            DataSourceConnection connection) throws AbcUndefinedException;

    List<DataColumnMetadata> loadDataColumnMetadataOfDataTable(
            JSONObject connectionProfile,
            DataTableMetadata dataTableMetadata) throws AbcUndefinedException;

    List<DataColumnMetadata> loadDataColumnMetadataOfDataTable(
            DataSourceConnection connection,
            DataTableMetadata dataTableMetadata) throws AbcUndefinedException;

    List<DataIndexMetadata> loadDataIndexMetadataOfDataTable(
            JSONObject connectionProfile,
            DataTableMetadata dataTableMetadata) throws AbcUndefinedException;

    List<DataIndexMetadata> loadDataIndexMetadataOfDataTable(
            DataSourceConnection connection,
            DataTableMetadata dataTableMetadata) throws AbcUndefinedException;

    DataTableMetadata loadDataTableMetadata(
            String tableName,
            List<String> tableContextPath,
            JSONObject connectionProfile) throws AbcUndefinedException;

    DataTableMetadata loadDataTableMetadata(
            String tableName,
            List<String> tableContextPath,
            DataSourceConnection connection) throws AbcUndefinedException;

    List<DataTableMetadata> loadDataTableMetadata(
            List<String> contextPath,
            JSONObject connectionProfile) throws AbcUndefinedException;

    List<DataTableMetadata> loadDataTableMetadata(
            List<String> contextPath,
            DataSourceConnection connection) throws AbcUndefinedException;
}
