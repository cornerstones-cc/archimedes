package cc.cornerstones.biz.share.types;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class QueryContentResult {
    /**
     * 本次请求是否启动分页
     */
    private Boolean enabledPagination;

    /**
     * 如果本次请求启动了分页，则需要本次请求的分页序号（从0开始计数）
     */
    private Integer pageNumber;

    /**
     * 如果本次请求启动了分页，则需要本次请求的分页大小
     */
    private Integer pageSize;

    /**
     * 如果本次请求启动了分页，则需要按照本次请求的查询条件和分页大小，查询出完整结果集涉及的总分页数量
     */
    private Integer totalPages;

    /**
     * 如果本次请求启动了分页，则需要按照本次请求的查询条件，查询出完整结果集涉及的总行数
     */
    private Long totalElements;

    /**
     * 不管本次请求是否启动了分页，都需要本次结果集包含行数
     */
    private Integer numberOfElements;

    /**
     * 不管本次请求是否启动了分页，都需要本次结果集
     */
    private List<Map<String, Object>> content;

    /**
     * 不管本次请求是否启动了分页，都需要本次结果包含的列名及列序
     */
    private List<String> columnNames;
}
