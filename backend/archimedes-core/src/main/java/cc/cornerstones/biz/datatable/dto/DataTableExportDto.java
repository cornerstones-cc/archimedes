package cc.cornerstones.biz.datatable.dto;

import cc.cornerstones.almond.types.AbcSort;
import cc.cornerstones.biz.share.constants.ExportOptionEnum;
import cc.cornerstones.biz.share.types.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.File;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class DataTableExportDto {
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
     * Export option
     */
    private ExportOptionEnum exportOption;

    /**
     * if exportOption = EXPORT_AS_TEMPLATE，需指定 export extended template file
     */
    private File exportExtendedTemplateFile;

    /**
     * if exportOption = EXPORT_CSV_W_ATTACHMENTS，需要指定哪些字段，什么源头，导出预处理，导出文件命名等规定
     */
    private List<ExportAttachment> imageAttachmentList;

    /**
     * if exportOption = EXPORT_EXCEL_W_ATTACHMENTS，需要指定哪些字段，什么源头，导出预处理，导出文件命名等规定
     */
    private List<ExportAttachment> fileAttachmentList;

    private Long dataFacetUid;

    private String dataFacetName;
}
