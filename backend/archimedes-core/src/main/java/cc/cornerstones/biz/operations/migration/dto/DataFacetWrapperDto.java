package cc.cornerstones.biz.operations.migration.dto;

import cc.cornerstones.biz.datafacet.entity.*;
import cc.cornerstones.biz.datatable.entity.DataTableDo;
import cc.cornerstones.biz.datawidget.entity.DataWidgetDo;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DataFacetWrapperDto {
    private AdvancedFeatureDo advancedFeatureDo;
    private DataFacetDo dataFacetDo;
    private List<DataFieldDo> dataFieldDoList;
    private List<DataPermissionDo> dataPermissionDoList;
    private ExportBasicDo exportBasicDo;
    private List<ExportExtendedTemplateDo> exportExtendedTemplateDoList;
    private List<FilteringDataFieldDo> filteringDataFieldDoList;
    private FilteringExtendedDo filteringExtendedDo;
    private List<ListingDataFieldDo> listingDataFieldDoList;
    private ListingExtendedDo listingExtendedDo;
    private List<SortingDataFieldDo> sortingDataFieldDoList;

    private DataTableDo dataTableDo;
    private List<DataWidgetDo> dataWidgetDoList;
}
