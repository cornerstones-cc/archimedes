package cc.cornerstones.biz.datatable.service.inf;

import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.TreeNode;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.datasource.service.assembly.database.QueryResult;
import cc.cornerstones.biz.datatable.dto.CreateIndirectDataTableDto;
import cc.cornerstones.biz.datatable.dto.DataTableDto;
import cc.cornerstones.biz.datatable.dto.UpdateIndirectDataTableDto;

import java.util.List;

public interface DataTableService {

    /**
     * 为指定 Data Source 创建 Data Table (Indirect)
     *
     * @param dataSourceUid
     * @param createIndirectDataTableDto
     * @param operatingUserProfile
     * @return
     * @throws AbcUndefinedException
     */
    DataTableDto createIndirectDataTable(
            Long dataSourceUid,
            CreateIndirectDataTableDto createIndirectDataTableDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    /**
     * 更新指定 Data Table (Indirect)
     *
     * @param dataTableUid
     * @param updateIndirectDataTableDto
     * @param operatingUserProfile
     * @throws AbcUndefinedException
     */
    void updateIndirectDataTable(
            Long dataTableUid,
            UpdateIndirectDataTableDto updateIndirectDataTableDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    DataTableDto getDataTable(
            Long dataTableUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    /**
     * 列出针对指定 Data Table (Indirect) 的所有引用
     *
     * @param dataTableUid
     * @param operatingUserProfile
     * @return
     * @throws AbcUndefinedException
     */
    List<String> listAllReferencesToDataTable(
            Long dataTableUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    /**
     * 删除指定 Data Table (Indirect)
     *
     * @param dataTableUid
     * @param operatingUserProfile
     * @throws AbcUndefinedException
     */
    void deleteDataTable(
            Long dataTableUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    /**
     * 树形列出指定 Data Source 的所有 Data Tables
     *
     * @param dataSourceUid
     * @param operatingUserProfile
     * @return
     * @throws AbcUndefinedException
     */
    List<TreeNode> treeListingAllDataTables(
            Long dataSourceUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    /**
     * 树形列出所有 Data Sources 的所有 Data Tables
     *
     * @param operatingUserProfile
     * @return
     * @throws AbcUndefinedException
     */
    List<TreeNode> treeListingAllDataTables(
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    /**
     * 重新获取指定 Data table 的 Metadata，包括 columns and indexes
     * @param dataTableUid
     * @param operatingUserProfile
     */
    Long asyncRetrieveMetadataOfDataTable(
            Long dataTableUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    JobStatusEnum getTaskStatusOfAsyncRetrieveMetadataOfDataTable(
            Long taskUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    QueryResult getSampleDataOfDataTable(
            Long dataTableUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    /**
     * 重新获取指定 Context path 的 Metadata，包括 Data tables 及每个 Data table 的 columns and indexes
     *
     * @param dataSourceUid
     * @param contextPath
     * @param operatingUserProfile
     * @throws AbcUndefinedException
     */
    Long asyncRetrieveMetadataOfContextPath(
            Long dataSourceUid,
            String contextPath,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    JobStatusEnum getTaskStatusOfAsyncRetrieveMetadataOfContextPath(
            Long taskUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

}
