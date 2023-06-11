package cc.cornerstones.biz.datawidget.service.assembly.pivottable;

import cc.cornerstones.almond.types.AbcNumberFormat;
import cc.cornerstones.almond.types.AbcPagination;
import cc.cornerstones.almond.types.AbcSort;
import cc.cornerstones.biz.share.constants.AggregateFunctionEnum;
import cc.cornerstones.biz.share.types.GroupFilter;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PivotTableDataWidgetQuery {

    /**
     * Filters
     */
    private Map<String, String[]> filters;

    /**
     * Rows
     */
    private List<Field> rows;

    /**
     * Columns
     */
    private List<Field> columns;

    /**
     * Values
     */
    private List<Value> values;

    /**
     * 分组查询条件
     */
    private List<GroupFilter> groupFilters;

    /**
     * Sort
     */
    private AbcSort sort;

    /**
     * Pagination
     */
    private AbcPagination pagination;

    @Data
    public static class Field {
        private String name;
        private Subtotals subtotals;
        private Layout layout;
    }

    @Data
    public static class Subtotals {
        private SubtotalsTypeEnum type;

        /**
         * 当 type = CUSTOM 时，要求1个或多个 aggregate function(s)
         */
        private List<AggregateFunctionEnum> aggregateFunctions;
    }

    @Data
    public static class Layout {
        private Boolean showItemsWithNoData;
    }

    @Data
    public static class Value {
        /**
         * Source Field's Name
         */
        private String sourceFieldName;

        /**
         * Target Field's Name
         */
        private String targetFieldName;

        /**
         * Summarize by
         */
        private AggregateFunctionEnum summarizeBy;

        /**
         * Show data as
         */
        private ShowDataAs showDataAs;

        /**
         * Number format
         */
        private AbcNumberFormat numberFormat;
    }

    @Data
    private static class ShowDataAs {
        private ShowDataAsTypeEnum type;

        /**
         * 当 type 等于以下值时，要求填写 baseFieldName
         * 1) DIFFERENCE_FROM;
         * 2) PERCENTAGE_OF;
         * 3) PERCENTAGE_DIFFERENCE_FROM;
         * 4) RUNNING_TOTAL_IN;
         * 5) PERCENTAGE_OF_PARENT_TOTAL;
         * 6) PERCENTAGE_RUNNING_TOTAL_IN;
         * 7) RANK_SMALLEST_TO_LARGEST;
         * 8) RANK_LARGEST_TO_SMALLEST;
         */
        private String baseFieldName;

        /**
         * 当 type 等于以下值时，要求填写 baseItemType
         * 1) DIFFERENCE_FROM;
         * 2) PERCENTAGE_OF;
         * 3) PERCENTAGE_DIFFERENCE_FROM;
         */
        private BaseItemTypeEnum baseItemType;

        /**
         * 当 baseItemType 等于以下值时，要求填写 baseItemValue
         * 1) SPECIFIC
         */
        private String baseItemValue;
    }
}
