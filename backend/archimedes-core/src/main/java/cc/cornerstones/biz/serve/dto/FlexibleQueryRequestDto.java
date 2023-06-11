package cc.cornerstones.biz.serve.dto;

import cc.cornerstones.almond.types.AbcPagination;
import cc.cornerstones.almond.types.AbcSort;
import lombok.Data;

import java.util.List;

/**
 * 灵活查询请求对象
 */
@Data
public class FlexibleQueryRequestDto {
    /**
     * 要求的 Result 字段名称，不填则表示默认情况
     */
    private List<String> selectionFieldNames;

    /**
     * 过滤条件片段
     */
    private String filter;

    /**
     * Sort
     */
    private AbcSort sort;

    /**
     * Pagination
     */
    private AbcPagination pagination;
}
