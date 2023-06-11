package cc.cornerstones.biz.datasource.service.assembly.database;

import lombok.Data;

import java.util.List;

@Data
public class DataIndexMetadata {
    /**
     * 索引名称
     */
    private String name;

    /**
     * 是否唯一索引
     */
    private Boolean unique;

    /**
     * 索引字段集合（按顺序）
     */
    private List<String> columns;
}
