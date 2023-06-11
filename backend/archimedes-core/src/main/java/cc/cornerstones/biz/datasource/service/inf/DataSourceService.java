package cc.cornerstones.biz.datasource.service.inf;

import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.datasource.dto.CreateDataSourceDto;
import cc.cornerstones.biz.datasource.dto.DataSourceDto;
import cc.cornerstones.biz.datasource.dto.UpdateDataSourceDto;
import cc.cornerstones.biz.datasource.service.assembly.database.QueryResult;
import cc.cornerstones.biz.datasource.share.constants.DatabaseServerTypeEnum;
import cc.cornerstones.biz.datatable.dto.TestQueryStatementDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

/**
 * @author bbottong
 */
public interface DataSourceService {
    /**
     * 创建 Data Source
     *
     * @param createDataSourceDto
     * @param operatingUserProfile
     * @return
     * @throws AbcUndefinedException
     */
    DataSourceDto createDataSource(
            CreateDataSourceDto createDataSourceDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    /**
     * 获取指定 Data Source
     *
     * @param dataSourceUid
     * @param operatingUserProfile
     * @return
     * @throws AbcUndefinedException
     */
    DataSourceDto getDataSource(
            Long dataSourceUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    /**
     * 更新指定 Data Source
     *
     * @param dataSourceUid
     * @param updateDataSourceDto
     * @param operatingUserProfile
     * @throws AbcUndefinedException
     */
    void updateDataSource(
            Long dataSourceUid,
            UpdateDataSourceDto updateDataSourceDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    /**
     * 列出针对指定 Data Source 的所有引用
     *
     * @param dataSourceUid
     * @param operatingUserProfile
     * @return
     * @throws AbcUndefinedException
     */
    List<String> listAllReferencesToDataSource(
            Long dataSourceUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    /**
     * 删除指定 Data Source
     *
     * @param dataSourceUid
     * @param operatingUserProfile
     * @throws AbcUndefinedException
     */
    void deleteDataSource(
            Long dataSourceUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    /**
     * 分页查询 Data Sources
     *
     * @param dataSourceUid
     * @param dataSourceName
     * @param dataSourceType
     * @param pageable
     * @param operatingUserProfile
     * @return
     * @throws AbcUndefinedException
     */
    Page<DataSourceDto> pagingQueryDataSources(
            Long dataSourceUid,
            String dataSourceName,
            DatabaseServerTypeEnum dataSourceType,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    /**
     * 列表查询 Data Sources
     *
     * @param dataSourceUid
     * @param dataSourceName
     * @param dataSourceType
     * @param sort
     * @param operatingUserProfile
     * @return
     * @throws AbcUndefinedException
     */
    List<DataSourceDto> listingQueryDataSources(
            Long dataSourceUid,
            String dataSourceName,
            DatabaseServerTypeEnum dataSourceType,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    /**
     * 重新获取指定 Data Source 的 Metadata，包括 tables, views, columns, and indexes
     *
     * @param dataSourceUid
     * @param operatingUserProfile
     * @throws AbcUndefinedException
     */
    Long asyncRetrieveMetadataOfDataSource(
            Long dataSourceUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    JobStatusEnum getTaskStatusOfAsyncRetrieveMetadataOfDataSource(
            Long taskUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    /**
     * 针对指定 Data Source 测试 Query Statement
     *
     * @param dataSourceUid
     * @param testQueryStatementDto
     * @param operatingUserProfile
     * @return
     */
    QueryResult testQueryStatement(
            Long dataSourceUid,
            TestQueryStatementDto testQueryStatementDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    /**
     * 针对指定 Data Source 测试 Query Statement
     *
     * @param dataSourceUid
     * @param queryStatement
     * @param operatingUserProfile
     * @return
     */
    void validateQueryStatement(
            Long dataSourceUid,
            String queryStatement,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
