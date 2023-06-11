package cc.cornerstones.biz.datatable.service.impl;

import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.datasource.dto.DataSourceExportDto;
import cc.cornerstones.biz.datasource.share.constants.DataColumnTypeEnum;
import cc.cornerstones.biz.datatable.dto.DataTableExportDto;
import cc.cornerstones.biz.datatable.entity.DataColumnDo;
import cc.cornerstones.biz.datatable.entity.DataTableDo;
import cc.cornerstones.biz.datatable.persistence.DataColumnRepository;
import cc.cornerstones.biz.datatable.persistence.DataTableRepository;
import cc.cornerstones.biz.datasource.dto.DataSourceQueryDto;
import cc.cornerstones.biz.datasource.service.inf.ExecuteDataSourceService;
import cc.cornerstones.biz.datatable.dto.DataTableQueryDto;
import cc.cornerstones.biz.datatable.service.inf.ExecuteDataTableService;
import cc.cornerstones.biz.share.constants.ExpressionTypeEnum;
import cc.cornerstones.biz.share.constants.SelectionFieldTypeEnum;
import cc.cornerstones.biz.share.types.ExpressionAggregateSelectionField;
import cc.cornerstones.biz.share.types.ExpressionSelectionField;
import cc.cornerstones.biz.share.types.QueryContentResult;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExecuteDataTableServiceImpl implements ExecuteDataTableService {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ExecuteDataTableServiceImpl.class);

    @Autowired
    private DataTableRepository dataTableRepository;

    @Autowired
    private DataColumnRepository dataColumnRepository;

    @Autowired
    private ExecuteDataSourceService executeDataSourceService;

    private DataSourceQueryDto buildDataSourceQuery(
            DataTableQueryDto dataTableQueryDto,
            DataTableDo dataTableDo) {
        //
        // Step 1, pre-processing
        //
        List<DataColumnDo> dataColumnDoList = this.dataColumnRepository.findByDataTableUid(dataTableDo.getUid());
        if (CollectionUtils.isEmpty(dataColumnDoList)) {
            throw new AbcResourceNotFoundException(String.format("%s::data_table_uid:%d", DataColumnDo.RESOURCE_SYMBOL,
                    dataTableDo.getUid()));
        }
        Map<String, DataColumnTypeEnum> dataColumnNameAndDataColumnTypeMap = new HashMap<>();
        dataColumnDoList.forEach(dataColumnDo -> {
            dataColumnNameAndDataColumnTypeMap.put(dataColumnDo.getName(), dataColumnDo.getType());
        });

        //
        // Step 2, core-processing
        //
        DataSourceQueryDto dataSourceQueryDto = new DataSourceQueryDto();
        dataSourceQueryDto.setTableType(dataTableDo.getType());

        switch (dataTableDo.getType()) {
            case DATABASE_TABLE:
            case DATABASE_VIEW: {
                dataSourceQueryDto.setTableNameOfDirectTable(dataTableDo.getName());
                dataSourceQueryDto.setTableContextPathOfDirectTable(dataTableDo.getContextPath());
            }
            break;
            case INDIRECT_TABLE: {
                dataSourceQueryDto.setBuildingLogicOfIndirectTable(dataTableDo.getBuildingLogic());
            }
            break;
        }

        dataSourceQueryDto.setSelectionFields(dataTableQueryDto.getSelectionFields());
        dataSourceQueryDto.getSelectionFields().forEach(selectionField -> {
            if (SelectionFieldTypeEnum.EXPRESSION.equals(selectionField.getType())) {
                ExpressionSelectionField expressionSelectionField =
                        JSONObject.toJavaObject(selectionField.getContent(), ExpressionSelectionField.class);
                if (ExpressionTypeEnum.AGGREGATE_FUNCTION.equals(expressionSelectionField.getType())) {
                    ExpressionAggregateSelectionField expressionAggregateSelectionField =
                            JSONObject.toJavaObject(expressionSelectionField.getContent(),
                                    ExpressionAggregateSelectionField.class);
                    expressionAggregateSelectionField.setSourceFieldType(
                            dataColumnNameAndDataColumnTypeMap.get(expressionAggregateSelectionField.getSourceFieldName()));

                    expressionSelectionField.setContent((JSONObject) JSONObject.toJSON(expressionAggregateSelectionField));
                    selectionField.setContent((JSONObject) JSONObject.toJSON(expressionSelectionField));
                }
            }
        });

        dataSourceQueryDto.setPlainFilters(dataTableQueryDto.getPlainFilters());
        dataSourceQueryDto.setStatementFilter(dataTableQueryDto.getStatementFilter());
        dataSourceQueryDto.setGroupByFields(dataTableQueryDto.getGroupByFields());
        dataSourceQueryDto.setGroupFilters(dataTableQueryDto.getGroupFilters());
        dataSourceQueryDto.setCascadingFilters(dataTableQueryDto.getCascadingFilters());
        dataSourceQueryDto.setDataPermissionFilters(dataTableQueryDto.getDataPermissionFilters());
        dataSourceQueryDto.setSort(dataTableQueryDto.getSort());
        dataSourceQueryDto.setPagination(dataTableQueryDto.getPagination());

        return dataSourceQueryDto;
    }

    private DataSourceExportDto buildDataSourceExport(
            DataTableExportDto dataTableExportDto,
            DataTableDo dataTableDo) {
        //
        // Step 1, pre-processing
        //
        List<DataColumnDo> dataColumnDoList = this.dataColumnRepository.findByDataTableUid(dataTableDo.getUid());
        if (CollectionUtils.isEmpty(dataColumnDoList)) {
            throw new AbcResourceNotFoundException(String.format("%s::data_table_uid:%d", DataColumnDo.RESOURCE_SYMBOL,
                    dataTableDo.getUid()));
        }
        Map<String, DataColumnTypeEnum> dataColumnNameAndDataColumnTypeMap = new HashMap<>();
        dataColumnDoList.forEach(dataColumnDo -> {
            dataColumnNameAndDataColumnTypeMap.put(dataColumnDo.getName(), dataColumnDo.getType());
        });

        //
        // Step 2, core-processing
        //
        DataSourceExportDto dataSourceExportDto = new DataSourceExportDto();
        dataSourceExportDto.setDataFacetUid(dataTableExportDto.getDataFacetUid());
        dataSourceExportDto.setDataFacetName(dataTableExportDto.getDataFacetName());
        dataSourceExportDto.setTableType(dataTableDo.getType());

        switch (dataTableDo.getType()) {
            case DATABASE_TABLE:
            case DATABASE_VIEW: {
                dataSourceExportDto.setTableNameOfDirectTable(dataTableDo.getName());
                dataSourceExportDto.setTableContextPathOfDirectTable(dataTableDo.getContextPath());
            }
            break;
            case INDIRECT_TABLE: {
                dataSourceExportDto.setBuildingLogicOfIndirectTable(dataTableDo.getBuildingLogic());
            }
            break;
        }

        dataSourceExportDto.setVisibleSelectionFields(dataTableExportDto.getVisibleSelectionFields());
        dataSourceExportDto.setSelectionFields(dataTableExportDto.getSelectionFields());
        dataSourceExportDto.getSelectionFields().forEach(selectionField -> {
            if (SelectionFieldTypeEnum.EXPRESSION.equals(selectionField.getType())) {
                ExpressionSelectionField expressionSelectionField =
                        JSONObject.toJavaObject(selectionField.getContent(), ExpressionSelectionField.class);
                if (ExpressionTypeEnum.AGGREGATE_FUNCTION.equals(expressionSelectionField.getType())) {
                    ExpressionAggregateSelectionField expressionAggregateSelectionField =
                            JSONObject.toJavaObject(expressionSelectionField.getContent(),
                                    ExpressionAggregateSelectionField.class);
                    expressionAggregateSelectionField.setSourceFieldType(
                            dataColumnNameAndDataColumnTypeMap.get(expressionAggregateSelectionField.getSourceFieldName()));

                    expressionSelectionField.setContent((JSONObject) JSONObject.toJSON(expressionAggregateSelectionField));
                    selectionField.setContent((JSONObject) JSONObject.toJSON(expressionSelectionField));
                }
            }
        });

        dataSourceExportDto.setPlainFilters(dataTableExportDto.getPlainFilters());
        dataSourceExportDto.setStatementFilter(dataTableExportDto.getStatementFilter());
        dataSourceExportDto.setGroupByFields(dataTableExportDto.getGroupByFields());
        dataSourceExportDto.setGroupFilters(dataTableExportDto.getGroupFilters());
        dataSourceExportDto.setCascadingFilters(dataTableExportDto.getCascadingFilters());
        dataSourceExportDto.setDataPermissionFilters(dataTableExportDto.getDataPermissionFilters());
        dataSourceExportDto.setSort(dataTableExportDto.getSort());

        dataSourceExportDto.setExportOption(dataTableExportDto.getExportOption());
        dataSourceExportDto.setExportExtendedTemplateFile(dataTableExportDto.getExportExtendedTemplateFile());
        dataSourceExportDto.setFileAttachmentList(dataTableExportDto.getFileAttachmentList());
        dataSourceExportDto.setImageAttachmentList(dataTableExportDto.getImageAttachmentList());

        return dataSourceExportDto;
    }

    @Override
    public QueryContentResult queryContent(
            Long dataTableUid,
            DataTableQueryDto dataTableQueryDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DataTableDo dataTableDo = this.dataTableRepository.findByUid(dataTableUid);
        if (dataTableDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataTableDo.RESOURCE_SYMBOL,
                    dataTableUid));
        }

        //
        // Step 2, core-processing
        //
        DataSourceQueryDto dataSourceQueryDto = buildDataSourceQuery(dataTableQueryDto, dataTableDo);
        if (dataSourceQueryDto == null) {
            throw new AbcResourceConflictException(String.format("failed to build query, data table %d",
                    dataTableUid));
        }

        return this.executeDataSourceService.queryContent(
                dataTableDo.getDataSourceUid(),
                dataSourceQueryDto,
                operatingUserProfile);
    }

    @Override
    public Long exportContent(
            String name,
            Long dataTableUid,
            DataTableExportDto dataTableExportDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DataTableDo dataTableDo = this.dataTableRepository.findByUid(dataTableUid);
        if (dataTableDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataTableDo.RESOURCE_SYMBOL,
                    dataTableUid));
        }

        //
        // Step 2, core-processing
        //
        DataSourceExportDto dataSourceExportDto = buildDataSourceExport(dataTableExportDto, dataTableDo);
        if (dataSourceExportDto == null) {
            throw new AbcResourceConflictException(String.format("failed to build export, data table %d",
                    dataTableUid));
        }

        return this.executeDataSourceService.exportContent(
                name,
                dataTableDo.getDataSourceUid(),
                dataSourceExportDto,
                operatingUserProfile);
    }
}
