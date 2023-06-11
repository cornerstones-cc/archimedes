package cc.cornerstones.biz.datawidget.service.assembly.pivottable;

import cc.cornerstones.biz.datawidget.service.assembly.DataWidgetBuildCharacteristics;
import cc.cornerstones.biz.share.constants.FilteringTypeEnum;
import lombok.Data;

import java.util.List;

@Data
public class PivotTableDataWidgetBuildCharacteristics extends DataWidgetBuildCharacteristics {
    private BasicConf basic;
    private AdvancedConf advanced;

    @Data
    public static class BasicConf {
        private List<FilterField> filterFields;
    }

    @Data
    public static class FilterField {
        /**
         * field name
         */
        private String fieldName;

        /**
         * 该 Filter 字段的表单元素类型
         */
        private FilteringTypeEnum type;
    }

    @Data
    public static class AdvancedConf {
        /**
         * 是否启用分页
         */
        private Boolean enabledPagination;

        /**
         * 如果启用分页，默认分页大小
         */
        private Integer defaultPageSize;
    }
}
