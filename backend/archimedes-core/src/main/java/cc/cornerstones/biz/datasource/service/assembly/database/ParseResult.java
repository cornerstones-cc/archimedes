package cc.cornerstones.biz.datasource.service.assembly.database;

import lombok.Data;

import java.util.List;

@Data
public class ParseResult {
    /**
     * 引用的原始表集合
     */
    private List<Table> tables;

    /**
     * 引用的原始表的列集合
     */
    private List<Column> columns;

    @Data
    public static class Table {
        private String name;
        private List<String> contextPath;
    }

    @Data
    public static class Column {
        private String tableName;
        private String name;
        private String dataType;
        private String fullName;
    }
}
