package cc.cornerstones.biz.datatable.dto;

import cc.cornerstones.almond.types.AbcPagination;
import cc.cornerstones.almond.types.AbcSort;
import cc.cornerstones.biz.share.types.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class DataTableQueryDto {
    /**
     * 结果字段要求
     */
    private List<SelectionField> selectionFields;

    /**
     * 简单查询条件
     */
    private List<PlainFilter> plainFilters;

    /**
     * 声明查询条件
     */
    private StatementFilter statementFilter;

    /**
     * 分组字段要求
     */
    private List<GroupByField> groupByFields;

    /**
     * 分组查询条件
     */
    private List<GroupFilter> groupFilters;

    /**
     * 级联查询条件
     */
    private List<CascadingFilter> cascadingFilters;

    /**
     * 是否需要数据权限
     */
    private Boolean requireDataPermissionFilters;

    /**
     * 数据权限查询条件
     */
    private List<DataPermissionFilter> dataPermissionFilters;

    /**
     * 结果排序要求
     */
    private AbcSort sort;

    /**
     * 结果分页要求
     */
    private AbcPagination pagination;
}
