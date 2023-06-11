package cc.cornerstones.biz.datawidget.service.assembly.table;

import cc.cornerstones.almond.types.TreeNode;
import cc.cornerstones.biz.datawidget.service.assembly.DataWidgetBuildCharacteristics;
import cc.cornerstones.biz.share.constants.FilteringTypeEnum;
import lombok.Data;
import org.springframework.data.domain.Sort;

import java.util.List;

/**
 * @author bbottong
 */
@Data
public class TableDataWidgetBuildCharacteristics extends DataWidgetBuildCharacteristics {
    /**
     * Header
     */
    private String header;

    /**
     * Filtering
     */
    private Filtering filtering;

    /**
     * Listing
     */
    private Listing listing;

    /**
     * Sorting
     */
    private Sorting sorting;

    /**
     * Exporting
     */
    private Exporting exporting;

    @Data
    public static class Filtering {
        private List<FilteringField> fields;
        private FilteringExtended extended;
    }

    @Data
    public static class FilteringField {
        /**
         * Field name
         */
        private String fieldName;

        /**
         * Field description
         */
        private String fieldDescription;

        /**
         * Field label
         */
        private String fieldLabel;

        /**
         * Filtering type
         */
        private FilteringTypeEnum filteringType;

        /**
         * Filtering sequence
         * <p>
         * 在所有 Filtering 字段中的序号（从0开始计数）
         */
        private Float filteringSequence;

        /**
         * Default value(s)
         */
        private List<TreeNode> fieldDefaultValues;

        /**
         * Optional value(s)
         */
        private List<TreeNode> fieldOptionalValues;
    }

    @Data
    public static class FilteringExtended {

        /**
         * Enabled filter folding
         */
        private Boolean enabledFilterFolding;

        /**
         * Enabled default query
         */
        private Boolean enabledDefaultQuery;
    }

    @Data
    public static class Listing {
        private List<ListingField> fields;
        private ListingExtended extended;
    }

    @Data
    public static class ListingField {
        /**
         * Field name
         */
        private String fieldName;

        /**
         * Field description
         */
        private String fieldDescription;

        /**
         * Field label
         */
        private String fieldLabel;

        /**
         * Width
         */
        private Integer width;

        /**
         * Listing sequence
         * <p>
         * 在所有 Listing 字段中的序号（从0开始计数）
         */
        private Float listingSequence;

        /**
         * Enabled image preview for image field type
         */
        private Boolean enabledImagePreview;

        /**
         * Enabled file download for file field type
         */
        private Boolean enabledFileDownload;
    }

    @Data
    public static class ListingExtended {

        /**
         * 是否启用分页
         */
        private Boolean enabledPagination;

        /**
         * 如果启用分页，默认分页大小
         */
        private Integer defaultPageSize;

        /**
         * 是否启用垂直滚动，如果启用，则在表格高度超过一个阈值之后显示垂直滚动条
         */
        private Boolean enabledVerticalScrolling;

        /**
         * 如果启用垂直滚动，超过多长（单位：vh, viewport height）则显示垂直滚动条
         */
        private Integer verticalScrollingHeightThreshold;

        /**
         * 是否启用"列序"列，如果启用，则在表格的第1列显示"No."列，内容即为表格中行的序号，从1到N。分页时是按延续排序处理，而不是重新排序。
         */
        private Boolean enabledColumnNo;

        /**
         * 是否启用冻结从上至下计第1至N行
         */
        private Boolean enabledFreezeTopRows;

        /**
         * 如果启用冻结从上至下计第1至N行，N是多少
         */
        private Integer inclusiveTopRows;

        /**
         * 是否启用冻结从左至右计第1至M列
         */
        private Boolean enabledFreezeLeftColumns;

        /**
         * 是否启用冻结从左至右计第1至M列，M是多少
         */
        private Integer inclusiveLeftColumns;

        /**
         * 是否启用冻结从右至左计第1至P列
         */
        private Boolean enabledFreezeRightColumns;

        /**
         * 是否启用冻结从右至左计第1至P列，P是多少
         */
        private Integer inclusiveRightColumns;
    }

    @Data
    public static class Sorting {
        private List<SortingField> fields;
    }

    @Data
    public static class SortingField {
        /**
         * Field name
         */
        private String fieldName;

        /**
         * Field description
         */
        private String fieldDescription;

        /**
         * Field label
         */
        private String fieldLabel;

        /**
         * Sorting sequence
         * <p>
         * 在所有 Sorting 字段中的序号（从0开始计数）
         */
        private Float sortingSequence;

        /**
         * Sorting direction
         */
        private Sort.Direction direction;
    }

    @Data
    public static class Exporting {
        /**
         * Enabled export csv
         */
        private Boolean enabledExportCsv;

        /**
         * Enabled export excel
         */
        private Boolean enabledExportExcel;

        /**
         * Enabled export csv with attachments
         */
        private Boolean enabledExportCsvWithAttachments;

        /**
         * Enabled export excel with attachments
         */
        private Boolean enabledExportExcelWithAttachments;

        /**
         * Enabled export as template
         */
        private Boolean enabledExportAsTemplate;
    }
}
