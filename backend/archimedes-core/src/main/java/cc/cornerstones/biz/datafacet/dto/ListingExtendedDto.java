package cc.cornerstones.biz.datafacet.dto;

import cc.cornerstones.almond.types.BaseDto;
import lombok.Data;


@Data
public class ListingExtendedDto extends BaseDto {
    /**
     * 查询必须分页，默认分页大小
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

    /**
     * 所属 Data Facet 的 UID
     */
    private Long dataFacetUid;
}
