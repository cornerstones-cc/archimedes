package cc.cornerstones.biz.datafacet.dto;

import cc.cornerstones.almond.types.AbcSort;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.share.constants.ExportOptionEnum;
import cc.cornerstones.biz.share.types.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class DataFacetExportDto {
    /**
     * 可见结果字段要求
     */
    private List<SelectionField> visibleSelectionFields;

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
     * 结果排序要求
     */
    private AbcSort sort;

    /**
     * Export option
     */
    private ExportOptionEnum exportOption;

    /**
     * if exportOption = EXPORT_AS_TEMPLATE，需指定 export extended template uid
     */
    private Long exportExtendedTemplateUid;

    /**
     * 操作用户
     */
    private UserProfile operatingUserProfile;

}
