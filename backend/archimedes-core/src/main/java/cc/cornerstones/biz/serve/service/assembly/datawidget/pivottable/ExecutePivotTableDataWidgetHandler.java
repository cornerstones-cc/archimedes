package cc.cornerstones.biz.serve.service.assembly.datawidget.pivottable;

import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.AbcTuple4;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.datafacet.dto.DataFacetQueryDto;
import cc.cornerstones.biz.datafacet.service.inf.ExecuteDataFacetService;
import cc.cornerstones.biz.datawidget.service.assembly.pivottable.PivotTableDataWidgetBuildCharacteristics;
import cc.cornerstones.biz.datawidget.service.assembly.pivottable.PivotTableDataWidgetQuery;
import cc.cornerstones.biz.datawidget.share.constants.DataWidgetTypeEnum;
import cc.cornerstones.biz.serve.service.assembly.datawidget.ExecuteDataWidgetHandler;
import cc.cornerstones.biz.share.constants.ExpressionTypeEnum;
import cc.cornerstones.biz.share.constants.FilteringTypeEnum;
import cc.cornerstones.biz.share.constants.SelectionFieldTypeEnum;
import cc.cornerstones.biz.share.types.*;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Component
public class ExecutePivotTableDataWidgetHandler implements ExecuteDataWidgetHandler {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ExecutePivotTableDataWidgetHandler.class);

    @Autowired
    private ExecuteDataFacetService executeDataFacetService;

    /**
     * Data widget type
     *
     * @return
     */
    @Override
    public DataWidgetTypeEnum type() {
        return DataWidgetTypeEnum.PIVOT_TABLE;
    }

    /**
     * Query content of the specified Data Facet
     *
     * @param dataFacetUid
     * @param dataWidgetCharacteristics
     * @param request
     * @param operatingUserProfile
     * @return
     * @throws AbcUndefinedException
     */
    @Override
    public QueryContentResult queryContent(
            Long dataFacetUid,
            JSONObject dataWidgetCharacteristics,
            JSONObject request,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, 获取 (pivot table) data visualization characteristics
        //
        PivotTableDataWidgetBuildCharacteristics pivotTableDataVisualizationCharacteristics = null;
        if (dataWidgetCharacteristics == null) {
            throw new AbcIllegalParameterException(String.format("null or empty " +
                    "characteristics for data visualization type:%s", type()));
        } else {
            try {
                pivotTableDataVisualizationCharacteristics =
                        JSONObject.toJavaObject(dataWidgetCharacteristics,
                                PivotTableDataWidgetBuildCharacteristics.class);
            } catch (Exception e) {
                LOGGER.error("fail to parse characteristics:{}, for data visualization type:%s",
                        dataWidgetCharacteristics, type(), e);
                throw new AbcIllegalParameterException(String.format("illegal characteristics for " +
                        "data visualization type:%s", type()));
            }
            if (pivotTableDataVisualizationCharacteristics == null) {
                throw new AbcIllegalParameterException(String.format("null or empty characteristics for data " +
                        "visualization type:%s", type()));
            }
        }
        if (pivotTableDataVisualizationCharacteristics.getBasic() == null
                || pivotTableDataVisualizationCharacteristics.getAdvanced() == null) {
            throw new AbcIllegalParameterException(String.format("null or empty " +
                            "characteristics for data visualization type:%s, both basic and advanced cannot be null or empty"
                    , type()));
        }

        List<PivotTableDataWidgetBuildCharacteristics.FilterField> filterFields =
                pivotTableDataVisualizationCharacteristics.getBasic().getFilterFields();
        PivotTableDataWidgetBuildCharacteristics.AdvancedConf advancedConf =
                pivotTableDataVisualizationCharacteristics.getAdvanced();

        // 构造 available filter field names & filter type
        Map<String, FilteringTypeEnum> availableFilterFieldNameAndFilterTypeMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(filterFields)) {
            filterFields.forEach(filterField -> {
                availableFilterFieldNameAndFilterTypeMap.put(filterField.getFieldName(), filterField.getType());
            });
        }

        //
        // Step 2, 构造 data facet query layout
        //
        final DataFacetQueryDto dataFacetQueryDto = new DataFacetQueryDto();
        if (request == null) {
            // default query

            // TODO default query
        } else {
            // custom query

            PivotTableDataWidgetQuery pivotTableDataWidgetQuery = null;
            try {
                pivotTableDataWidgetQuery = JSONObject.toJavaObject(request, PivotTableDataWidgetQuery.class);
            } catch (Exception e) {
                LOGGER.error("fail to parse query object:{}, for data visualization type:%s", request, type(), e);
                throw new AbcIllegalParameterException(String.format("illegal request for data visualization " +
                        "type:%s", type()));
            }
            if (pivotTableDataWidgetQuery == null) {
                throw new AbcIllegalParameterException(String.format("illegal request for data visualization " +
                        "type:%s", type()));
            }

            // selection fields
            dataFacetQueryDto.setSelectionFields(new LinkedList<>());
            pivotTableDataWidgetQuery.getRows().forEach(row -> {
                SelectionField selectionField = new SelectionField();
                selectionField.setType(SelectionFieldTypeEnum.PLAIN);
                PlainSelectionField plainSelectionField =
                        new PlainSelectionField();
                plainSelectionField.setFieldName(row.getName());
                selectionField.setContent((JSONObject) JSONObject.toJSON(plainSelectionField));
                dataFacetQueryDto.getSelectionFields().add(selectionField);
            });
            pivotTableDataWidgetQuery.getColumns().forEach(column -> {
                SelectionField selectionField = new SelectionField();
                selectionField.setType(SelectionFieldTypeEnum.PLAIN);
                PlainSelectionField plainSelectionField =
                        new PlainSelectionField();
                plainSelectionField.setFieldName(column.getName());
                selectionField.setContent((JSONObject) JSONObject.toJSON(plainSelectionField));
                dataFacetQueryDto.getSelectionFields().add(selectionField);
            });
            pivotTableDataWidgetQuery.getValues().forEach(value -> {
                SelectionField selectionField = new SelectionField();
                selectionField.setType(SelectionFieldTypeEnum.EXPRESSION);

                ExpressionSelectionField expressionSelectionField =
                        new ExpressionSelectionField();
                expressionSelectionField.setType(ExpressionTypeEnum.AGGREGATE_FUNCTION);

                ExpressionAggregateSelectionField expressionAggregateSelectionField =
                        new ExpressionAggregateSelectionField();
                expressionAggregateSelectionField.setSourceFieldName(value.getSourceFieldName());
                expressionAggregateSelectionField.setTargetFieldName(value.getTargetFieldName());
                expressionAggregateSelectionField.setAggregateFunction(value.getSummarizeBy());

                expressionSelectionField.setContent((JSONObject) JSONObject.toJSON(expressionAggregateSelectionField));

                selectionField.setContent((JSONObject) JSONObject.toJSON(expressionSelectionField));

                dataFacetQueryDto.getSelectionFields().add(selectionField);
            });

            // plain filters
            dataFacetQueryDto.setPlainFilters(new LinkedList<>());
            if (!CollectionUtils.isEmpty(pivotTableDataWidgetQuery.getFilters())) {
                pivotTableDataWidgetQuery.getFilters().forEach((fieldName, fieldValues) -> {
                    PlainFilter plainFilter = new PlainFilter();
                    plainFilter.setContent(new AbcTuple4<>(fieldName, fieldValues,
                            availableFilterFieldNameAndFilterTypeMap.get(fieldName), null));
                    dataFacetQueryDto.getPlainFilters().add(plainFilter);
                });
            }

            // group by
            dataFacetQueryDto.setGroupByFields(new LinkedList<>());
            pivotTableDataWidgetQuery.getRows().forEach(row -> {
                GroupByField groupByField = new GroupByField();
                groupByField.setFieldName(row.getName());
                dataFacetQueryDto.getGroupByFields().add(groupByField);
            });
            pivotTableDataWidgetQuery.getColumns().forEach(column -> {
                GroupByField groupByField = new GroupByField();
                groupByField.setFieldName(column.getName());
                dataFacetQueryDto.getGroupByFields().add(groupByField);
            });

            // group filters
            dataFacetQueryDto.setGroupFilters(pivotTableDataWidgetQuery.getGroupFilters());

            // sort
            if (pivotTableDataWidgetQuery.getSort() != null
                    && !CollectionUtils.isEmpty(pivotTableDataWidgetQuery.getSort().getOrders())) {
                dataFacetQueryDto.setSort(pivotTableDataWidgetQuery.getSort());
            }

            // pagination
            if (pivotTableDataWidgetQuery.getPagination() != null
                    && pivotTableDataWidgetQuery.getPagination().getPage() != null
                    && pivotTableDataWidgetQuery.getPagination().getSize() != null) {
                dataFacetQueryDto.setPagination(pivotTableDataWidgetQuery.getPagination());
            }
        }

        return this.executeDataFacetService.queryContent(dataFacetUid, dataFacetQueryDto, operatingUserProfile);
    }

    /**
     * Export content of the specified Data Facet
     *
     * @param dataFacetUid
     * @param dataWidgetCharacteristics
     * @param request
     * @param operatingUserProfile
     * @return
     * @throws AbcUndefinedException
     */
    @Override
    public Long exportContent(
            Long dataFacetUid,
            JSONObject dataWidgetCharacteristics,
            JSONObject request,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return null;
    }

    @Override
    public Object generateServeCharacteristics(
            Long dataFacetUid) throws AbcUndefinedException {
        return null;
    }
}
