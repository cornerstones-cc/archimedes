package cc.cornerstones.biz.datawidget.service.assembly.table;

import cc.cornerstones.almond.types.AbcOrder;
import cc.cornerstones.almond.types.AbcPagination;
import cc.cornerstones.almond.types.AbcSort;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.PropertyNamingStrategy;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.Data;
import org.springframework.data.domain.Sort;

import java.util.*;

@Data
public class TableDataWidgetQuery {
    /**
     * 要求的 Result 字段名称，不填则表示默认情况
     */
    private List<String> selectionFieldNames;

    /**
     * Filters
     */
    private Map<String, String[]> filters;

    /**
     * Cascading filters
     */
    private Map<String, String[]> cascadingFilters;

    /**
     * Sort
     */
    private AbcSort sort;

    /**
     * Pagination
     */
    private AbcPagination pagination;

    public static void main(String[] args) {
        TableDataWidgetQuery query = new TableDataWidgetQuery();

        query.setSelectionFieldNames(new LinkedList<>());
        query.getSelectionFieldNames().add("f1");
        query.getSelectionFieldNames().add("f2");
        query.getSelectionFieldNames().add("f3");
        query.getSelectionFieldNames().add("f4");
        query.getSelectionFieldNames().add("f5");
        query.getSelectionFieldNames().add("f6");
        query.getSelectionFieldNames().add("f7");

        query.setFilters(new HashMap<>());
        String[] f1Value = new String[2];
        f1Value[0] = "1";
        f1Value[1] = "2";
        query.getFilters().put("f1", f1Value);

        String[] f2Value = new String[1];
        f2Value[0] = "ak47";
        query.getFilters().put("f2", f2Value);

        query.setCascadingFilters(new HashMap<>());

        String[] f13Value = new String[2];
        f13Value[0] = "华南大区>广东>广州>清远";
        f13Value[1] = "华中大区>湖北>武汉>黄石";
        query.getCascadingFilters().put("f13", f13Value);

        AbcSort sort = new AbcSort();
        sort.setOrders(new LinkedList<>());
        AbcOrder order1 = new AbcOrder();
        order1.setProperty("f1");
        order1.setDirection(Sort.Direction.ASC);
        AbcOrder order2 = new AbcOrder();
        order2.setProperty("f2");
        order2.setDirection(Sort.Direction.DESC);
        sort.getOrders().add(order2);
        query.setSort(sort);

        AbcPagination pagination = new AbcPagination();
        pagination.setPage(0);
        pagination.setSize(5);
        query.setPagination(pagination);

        SerializeConfig serializeConfig = new SerializeConfig();
        serializeConfig.propertyNamingStrategy = PropertyNamingStrategy.SnakeCase;

        String str = JSONObject.toJSONString(query, serializeConfig, SerializerFeature.DisableCircularReferenceDetect);

        System.out.println(str);
    }
}
