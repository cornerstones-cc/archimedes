package cc.cornerstones.biz.datafacet.entity;

import cc.cornerstones.almond.types.BaseDo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Listing extended
 *
 * @author bbottong
 */
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = ListingExtendedDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class ListingExtendedDo extends BaseDo {
    public static final String RESOURCE_NAME = "a1_df_appearance_listing_extended";
    public static final String RESOURCE_SYMBOL = "Listing extended";

    /**
     * 查询必须分页，默认分页大小
     */
    @Column(name = "default_page_size")
    private Integer defaultPageSize;

    /**
     * 是否启用垂直滚动，如果启用，则在表格高度超过一个阈值之后显示垂直滚动条
     */
    @Column(name = "is_enabled_vertical_scrolling", columnDefinition = "boolean default false")
    private Boolean enabledVerticalScrolling;

    /**
     * 如果启用垂直滚动，超过多长（单位：vh, viewport height）则显示垂直滚动条
     */
    @Column(name = "vertical_scrolling_height_threshold")
    private Integer verticalScrollingHeightThreshold;

    /**
     * 是否启用"列序"列，如果启用，则在表格的第1列显示"No."列，内容即为表格中行的序号，从1到N。分页时是按延续排序处理，而不是重新排序。
     */
    @Column(name = "is_enabled_column_no", columnDefinition = "boolean default false")
    private Boolean enabledColumnNo;

    /**
     * 是否启用冻结从上至下计第1至N行
     */
    @Column(name = "is_enabled_freeze_top_rows", columnDefinition = "boolean default false")
    private Boolean enabledFreezeTopRows;

    /**
     * 如果启用冻结从上至下计第1至N行，N是多少
     */
    @Column(name = "inclusive_top_rows")
    private Integer inclusiveTopRows;

    /**
     * 是否启用冻结从左至右计第1至M列
     */
    @Column(name = "is_enabled_freeze_left_columns", columnDefinition = "boolean default false")
    private Boolean enabledFreezeLeftColumns;

    /**
     * 是否启用冻结从左至右计第1至M列，M是多少
     */
    @Column(name = "is_enabled_inclusive_left_columns")
    private Integer inclusiveLeftColumns;

    /**
     * 是否启用冻结从右至左计第1至P列
     */
    @Column(name = "is_enabled_freeze_right_columns", columnDefinition = "boolean default false")
    private Boolean enabledFreezeRightColumns;

    /**
     * 是否启用冻结从右至左计第1至P列，P是多少
     */
    @Column(name = "inclusive_right_columns")
    private Integer inclusiveRightColumns;

    /**
     * 所属 Data Facet 的 UID
     */
    @Column(name = "data_facet_uid")
    private Long dataFacetUid;
}