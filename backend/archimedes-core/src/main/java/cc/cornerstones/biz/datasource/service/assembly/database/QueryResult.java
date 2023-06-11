package cc.cornerstones.biz.datasource.service.assembly.database;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class QueryResult {
    /**
     * 列名及列序
     */
    private List<String> columnNames;
    /**
     * 行集合
     */
    private List<Map<String, Object>> rows;
}
