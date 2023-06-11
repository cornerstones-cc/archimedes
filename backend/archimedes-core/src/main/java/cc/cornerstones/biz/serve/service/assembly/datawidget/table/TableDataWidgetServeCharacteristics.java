package cc.cornerstones.biz.serve.service.assembly.datawidget.table;

import cc.cornerstones.almond.types.ValueLabelPair;
import cc.cornerstones.biz.datafacet.share.types.FieldTypeExtensionFile;
import cc.cornerstones.biz.datawidget.service.assembly.DataWidgetServeCharacteristics;
import cc.cornerstones.biz.share.constants.FileSourceSettingsModeEnum;
import cc.cornerstones.biz.share.constants.FilteringTypeEnum;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.PropertyNamingStrategy;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

/**
 * @author bbottong
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class TableDataWidgetServeCharacteristics extends DataWidgetServeCharacteristics {
    /**
     * Service status
     */
    private DataFacetServiceStatus serviceStatus;

    /**
     * Header
     */
    private String header;

    /**
     * Filtering
     */
    private Filtering filtering;

    /**
     * Listing
     */
    private Listing listing;

    /**
     * Sorting
     */
    private Sorting sorting;

    /**
     * Exporting
     */
    private Exporting exporting;

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    public static class DataFacetServiceStatus {
        /**
         * true --- in service, false --- out of service
         */
        private Boolean active;

        /**
         * message while out of service
         */
        private String message;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    public static class Filtering {
        private List<FilteringField> fields;
        private FilteringExtended extended;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    public static class FilteringField {
        /**
         * Field name
         */
        private String fieldName;

        /**
         * Field description
         */
        private String fieldDescription;

        /**
         * Field label
         */
        private String fieldLabel;

        /**
         * Filtering type
         */
        private FilteringTypeEnum filteringType;

        /**
         * Filtering sequence
         * <p>
         * 在所有 Filtering 字段中的序号（从0开始计数）
         */
        private Float filteringSequence;

        /**
         * Default value(s) for all filtering types
         * (specific value(s) come from dictionary content of the specific dictionary category)
         */
        private List<ValueLabelPair> fieldDefaultValues;

        /**
         * Optional value(s) for the specific filtering types (
         * DROP_DOWN_LIST_SINGLE,
         * DROP_DOWN_LIST_MULTIPLE,
         * ASSOCIATING_SINGLE,
         * ASSOCIATING_MULTIPLE,
         */
        private List<ValueLabelPair> fieldOptionalValues;

        /**
         * Optional value(s) for the specific filtering types (
         * CASCADING_DROP_DOWN_LIST_SINGLE,
         * CASCADING_DROP_DOWN_LIST_MULTIPLE
         * Available field optional values, come from dictionary category uid
         */
        private Long cascadingDictionaryCategoryUid;

        /**
         * Specific settings for the specific filtering types (
         * CASCADING_DROP_DOWN_LIST_SINGLE,
         * CASCADING_DROP_DOWN_LIST_MULTIPLE
         * )
         * 级联关系中的从第1级到最末尾1级字段的基本信息
         */
        private CascadingField cascadingField;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    public static class CascadingFilter {
        /**
         * Filter 名称
         */
        private String filterName;

        /**
         * Filter 描述
         */
        private String filterDescription;

        /**
         * Filter 别名
         */
        private String filterLabel;

        /**
         * Dictionary category uid
         */
        private Long dictionaryCategoryUid;

        /**
         * 级联关系中的第1级，以及嵌套的下一级别
         */
        private CascadingField cascadingField;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    public static class CascadingField {
        /**
         * 字段名称
         */
        private String fieldName;

        /**
         * 字段描述
         */
        private String fieldDescription;

        /**
         * 字段别名
         */
        private String fieldLabel;

        /**
         * 级联关系中的下一级
         */
        private CascadingField child;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    public static class FilteringExtended {

        /**
         * Enabled filter folding
         */
        private Boolean enabledFilterFolding;

        /**
         * Enabled default query
         */
        private Boolean enabledDefaultQuery;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    public static class Listing {
        private List<ListingField> fields;
        private ListingExtended extended;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    public static class ListingField {
        /**
         * Field name
         */
        private String fieldName;

        /**
         * Field description
         */
        private String fieldDescription;

        /**
         * Field label
         */
        private String fieldLabel;

        /**
         * Width
         */
        private Integer width;

        /**
         * Listing sequence
         * <p>
         * 在所有 Listing 字段中的序号（从0开始计数）
         */
        private Float listingSequence;

        /**
         * if image field type, image preview settings
         */
        private ImagePreview imagePreview;

        /**
         * if file field type, file download settings
         */
        private FileDownload fileDownload;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    public static class ImagePreview extends FieldTypeExtensionFile {
        /**
         * Enabled image preview for image field type
         */
        private Boolean enabled;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    public static class FileDownload extends FieldTypeExtensionFile {
        /**
         * Enabled file download for file field type
         */
        private Boolean enabled;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    public static class ListingExtended {

        /**
         * 是否启用分页
         */
        private Boolean enabledPagination;

        /**
         * 如果启用分页，默认分页大小
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
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    public static class Sorting {
        private List<SortingField> fields;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    public static class SortingField {
        /**
         * Field name
         */
        private String fieldName;

        /**
         * Field description
         */
        private String fieldDescription;

        /**
         * Field label
         */
        private String fieldLabel;

        /**
         * Sorting sequence
         * <p>
         * 在所有 Sorting 字段中的序号（从0开始计数）
         */
        private Float sortingSequence;

        /**
         * Sorting direction
         */
        private Sort.Direction direction;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    public static class Exporting {
        /**
         * Enabled export csv
         */
        private Boolean enabledExportCsv;

        /**
         * Enabled export excel
         */
        private Boolean enabledExportExcel;

        /**
         * Enabled export csv with attachments
         */
        private Boolean enabledExportCsvWithAttachments;

        /**
         * Enabled export excel with attachments
         */
        private Boolean enabledExportExcelWithAttachments;

        /**
         * Enabled export as templates
         */
        private Boolean enabledExportAsTemplates;
    }

    public static void main(String[] args) {
        TableDataWidgetServeCharacteristics result = new TableDataWidgetServeCharacteristics();

        // service status
        DataFacetServiceStatus serviceStatus = new DataFacetServiceStatus();
        serviceStatus.setActive(Boolean.TRUE);
        serviceStatus.setMessage("message only show if active = false");
        result.setServiceStatus(serviceStatus);

        // header
        result.setHeader("welcome to data facet xxx");

        // filtering
        Filtering filtering = new Filtering();
        filtering.setFields(new LinkedList<>());

        List<ValueLabelPair> defaultValuesForText = new LinkedList<>();
        ValueLabelPair defaultValueForTextObject = new ValueLabelPair();
        defaultValueForTextObject.setValue("华南大区");
        defaultValueForTextObject.setLabel("华南大区");
        defaultValuesForText.add(defaultValueForTextObject);

        List<ValueLabelPair> defaultValuesForDateTime = new LinkedList<>();
        ValueLabelPair defaultValueForDateTimeObject = new ValueLabelPair();
        defaultValueForDateTimeObject.setValue(LocalDateTime.now());
        defaultValueForDateTimeObject.setLabel(LocalDateTime.now().toString());
        defaultValuesForDateTime.add(defaultValueForDateTimeObject);

        List<ValueLabelPair> defaultValuesForDropDownSingle = new LinkedList<>();
        ValueLabelPair defaultValueForDropDownSingleObject = new ValueLabelPair();
        defaultValueForDropDownSingleObject.setValue("华南大区");
        defaultValueForDropDownSingleObject.setLabel("华南大区");
        defaultValuesForDropDownSingle.add(defaultValueForDropDownSingleObject);

        List<ValueLabelPair> defaultValuesForDropDownMultiple = new LinkedList<>();
        ValueLabelPair defaultValuesForDropDownMultipleObject1 = new ValueLabelPair();
        defaultValuesForDropDownMultipleObject1.setValue("华南大区");
        defaultValuesForDropDownMultipleObject1.setLabel("华南大区");
        ValueLabelPair defaultValuesForDropDownMultipleObject2 = new ValueLabelPair();
        defaultValuesForDropDownMultipleObject2.setValue("华南大区");
        defaultValuesForDropDownMultipleObject2.setLabel("华南大区");
        defaultValuesForDropDownMultiple.add(defaultValuesForDropDownMultipleObject1);
        defaultValuesForDropDownMultiple.add(defaultValuesForDropDownMultipleObject2);

        List<ValueLabelPair> defaultValuesForCascadingSingle = new LinkedList<>();
        ValueLabelPair defaultValueForCascadingSingleObject = new ValueLabelPair();
        defaultValueForCascadingSingleObject.setValue("华南大区>广东>广州>清远");
        defaultValueForCascadingSingleObject.setLabel("清远");
        defaultValuesForCascadingSingle.add(defaultValueForCascadingSingleObject);

        List<ValueLabelPair> defaultValuesForCascadingMultiple = new LinkedList<>();
        ValueLabelPair defaultValueForCascadingMultipleObject1 = new ValueLabelPair();
        defaultValueForCascadingMultipleObject1.setValue("华南大区>广东>广州>清远");
        defaultValueForCascadingMultipleObject1.setLabel("清远");
        ValueLabelPair defaultValueForCascadingMultipleObject2 = new ValueLabelPair();
        defaultValueForCascadingMultipleObject2.setValue("华南大区>广东");
        defaultValueForCascadingMultipleObject2.setLabel("广东");
        defaultValuesForCascadingMultiple.add(defaultValueForCascadingMultipleObject1);
        defaultValuesForCascadingMultiple.add(defaultValueForCascadingMultipleObject2);

        CascadingField cascadingFieldLevel0 = new CascadingField();
        cascadingFieldLevel0.setFieldName("region");
        cascadingFieldLevel0.setFieldLabel("大区");
        cascadingFieldLevel0.setFieldDescription("大区");

        CascadingField cascadingFieldLevel1 = new CascadingField();
        cascadingFieldLevel1.setFieldName("province");
        cascadingFieldLevel1.setFieldLabel("省份");
        cascadingFieldLevel1.setFieldDescription("省份");

        CascadingField cascadingFieldLevel2 = new CascadingField();
        cascadingFieldLevel2.setFieldName("city_group");
        cascadingFieldLevel2.setFieldLabel("城市群");
        cascadingFieldLevel2.setFieldDescription("城市群");

        CascadingField cascadingFieldLevel3 = new CascadingField();
        cascadingFieldLevel3.setFieldName("city");
        cascadingFieldLevel3.setFieldLabel("城市");
        cascadingFieldLevel3.setFieldDescription("城市");

        cascadingFieldLevel0.setChild(cascadingFieldLevel1);
        cascadingFieldLevel1.setChild(cascadingFieldLevel2);
        cascadingFieldLevel2.setChild(cascadingFieldLevel3);


        List<ValueLabelPair> defaultValuesForIsNotNull = new LinkedList<>();
        ValueLabelPair defaultValueForIsNotNullObject = new ValueLabelPair();
        defaultValueForIsNotNullObject.setValue(Boolean.TRUE);
        defaultValueForIsNotNullObject.setLabel("Yes");
        defaultValuesForIsNotNull.add(defaultValueForIsNotNullObject);


        List<ValueLabelPair> defaultValuesForIsNull = new LinkedList<>();
        ValueLabelPair defaultValueForIsNullObject = new ValueLabelPair();
        defaultValueForIsNullObject.setValue(Boolean.FALSE);
        defaultValueForIsNullObject.setLabel("No");
        defaultValuesForIsNull.add(defaultValueForIsNullObject);

        filtering.getFields().add(
                buildFilteringField(
                        "f1", "F1", "F1 DESC", FilteringTypeEnum.EQUALS_TEXT, 1.1f, defaultValuesForText, null, null));
        filtering.getFields().add(
                buildFilteringField(
                        "f2", "F2", "F2 DESC", FilteringTypeEnum.CONTAINS_TEXT, 1.2f, null, null, null));
        filtering.getFields().add(
                buildFilteringField(
                        "f3", "F3", "F3 DESC", FilteringTypeEnum.ENDS_WITH_TEXT, 1.3f, null, null, null));
        filtering.getFields().add(
                buildFilteringField(
                        "f4", "F4", "F4 DESC", FilteringTypeEnum.BEGINS_WITH_TEXT, 1.4f, null, null, null));
        filtering.getFields().add(
                buildFilteringField(
                        "f5", "F5", "F5 DESC", FilteringTypeEnum.DATE_RANGE, 1.5f, null, null, null));
        filtering.getFields().add(
                buildFilteringField(
                        "f6", "F6", "F6 DESC", FilteringTypeEnum.TIME_RANGE, 1.6f, null, null, null));
        filtering.getFields().add(
                buildFilteringField(
                        "f7", "F7", "F7 DESC", FilteringTypeEnum.DATETIME_RANGE, 1.7f, defaultValuesForDateTime, null, null));
        filtering.getFields().add(
                buildFilteringField(
                        "f8", "F8", "F8 DESC", FilteringTypeEnum.NUMBER_RANGE, 1.8f, null, null, null));
        filtering.getFields().add(
                buildFilteringField(
                        "f9", "F9", "F9 DESC", FilteringTypeEnum.DROP_DOWN_LIST_SINGLE, 1.9f, defaultValuesForDropDownSingle, 12L, null));
        filtering.getFields().add(
                buildFilteringField(
                        "f10", "F10", "F10 DESC", FilteringTypeEnum.DROP_DOWN_LIST_MULTIPLE, 2.0f, defaultValuesForDropDownMultiple, 12L, null));
        filtering.getFields().add(
                buildFilteringField(
                        "f11", "F11", "F11 DESC", FilteringTypeEnum.ASSOCIATING_SINGLE, 2.1f, null, 12L, null));
        filtering.getFields().add(
                buildFilteringField(
                        "f12", "F12", "F12 DESC", FilteringTypeEnum.ASSOCIATING_MULTIPLE, 2.2f, null, 12L, null));
        filtering.getFields().add(
                buildFilteringField(
                        "f13", "F13", "F13 DESC", FilteringTypeEnum.CASCADING_DROP_DOWN_LIST_SINGLE, 2.3f,
                        defaultValuesForCascadingSingle, 12L, cascadingFieldLevel0));
        filtering.getFields().add(
                buildFilteringField(
                        "f14", "F14", "F14 DESC", FilteringTypeEnum.CASCADING_DROP_DOWN_LIST_MULTIPLE, 2.4f,
                        defaultValuesForCascadingMultiple, 12L, cascadingFieldLevel0));
        filtering.getFields().add(
                buildFilteringField(
                        "f15", "F15", "F15 DESC", FilteringTypeEnum.IS_NOT_NULL, 2.5f, defaultValuesForIsNotNull, null, null));
        filtering.getFields().add(
                buildFilteringField(
                        "f16", "F16", "F16 DESC", FilteringTypeEnum.IS_NULL, 2.6f, defaultValuesForIsNull, null, null));

        filtering.setExtended(new FilteringExtended());
        filtering.getExtended().setEnabledDefaultQuery(Boolean.TRUE);
        filtering.getExtended().setEnabledFilterFolding(Boolean.TRUE);

        result.setFiltering(filtering);

        // listing
        Listing listing = new Listing();
        listing.setFields(new LinkedList<>());

        ImagePreview imagePreview = new ImagePreview();
        imagePreview.setEnabled(Boolean.TRUE);
        imagePreview.setSettingsMode(FileSourceSettingsModeEnum.DFS_FILE);
        imagePreview.setDfsServiceAgentUid(10L);
        imagePreview.setMayContainMultipleItemsInOneField(Boolean.TRUE);
        imagePreview.setDelimiter(",");

        FileDownload fileDownload = new FileDownload();
        fileDownload.setEnabled(Boolean.TRUE);
        fileDownload.setSettingsMode(FileSourceSettingsModeEnum.DFS_FILE);
        fileDownload.setDfsServiceAgentUid(10L);
        fileDownload.setMayContainMultipleItemsInOneField(Boolean.TRUE);
        fileDownload.setDelimiter(",");

        listing.getFields().add(buildListingField("f1", "F1", "F1 DESC", 100, 1.0F, imagePreview, null));
        listing.getFields().add(buildListingField("f2", "F2", "F2 DESC", 110, 1.1F, null, fileDownload));
        listing.getFields().add(buildListingField("f3", "F3", "F3 DESC", 120, 1.2F, imagePreview, fileDownload));
        listing.getFields().add(buildListingField("f4", "F4", "F4 DESC", 130, 1.3F, null, null));
        listing.getFields().add(buildListingField("f5", "F5", "F5 DESC", 140, 1.4F, null, null));
        listing.getFields().add(buildListingField("f6", "F6", "F6 DESC", 150, 1.5F, null, null));
        listing.getFields().add(buildListingField("f7", "F7", "F7 DESC", 160, 1.6F, null, null));
        listing.getFields().add(buildListingField("f8", "F8", "F8 DESC", 170, 1.7F, null, null));
        listing.getFields().add(buildListingField("f9", "F9", "F9 DESC", 180, 1.8F, null, null));

        listing.setExtended(new ListingExtended());
        listing.getExtended().setEnabledPagination(Boolean.TRUE);
        listing.getExtended().setDefaultPageSize(20);
        listing.getExtended().setEnabledVerticalScrolling(Boolean.TRUE);
        listing.getExtended().setVerticalScrollingHeightThreshold(800);
        listing.getExtended().setEnabledColumnNo(Boolean.TRUE);
        listing.getExtended().setEnabledFreezeTopRows(Boolean.TRUE);
        listing.getExtended().setInclusiveTopRows(1);
        listing.getExtended().setEnabledFreezeLeftColumns(Boolean.TRUE);
        listing.getExtended().setInclusiveLeftColumns(1);
        listing.getExtended().setEnabledFreezeRightColumns(Boolean.TRUE);
        listing.getExtended().setInclusiveRightColumns(1);
        result.setListing(listing);

        // sorting
        Sorting sorting = new Sorting();
        sorting.setFields(new LinkedList<>());
        sorting.getFields().add(buildSortingField("f1", "F1", "F1 DESC", Sort.Direction.ASC, 1.0f));
        sorting.getFields().add(buildSortingField("f3", "F3", "F3 DESC", Sort.Direction.ASC, 1.1f));
        sorting.getFields().add(buildSortingField("f5", "F5", "F5 DESC", Sort.Direction.ASC, 1.2f));
        result.setSorting(sorting);

        // exporting
        Exporting exporting = new Exporting();
        exporting.setEnabledExportCsv(Boolean.TRUE);
        exporting.setEnabledExportExcel(Boolean.TRUE);
        exporting.setEnabledExportCsvWithAttachments(Boolean.TRUE);
        exporting.setEnabledExportExcelWithAttachments(Boolean.TRUE);
        exporting.setEnabledExportAsTemplates(Boolean.TRUE);

        result.setExporting(exporting);

        SerializeConfig serializeConfig = new SerializeConfig();
        serializeConfig.propertyNamingStrategy = PropertyNamingStrategy.SnakeCase;

        String str = JSONObject.toJSONString(result, serializeConfig, SerializerFeature.DisableCircularReferenceDetect);

        System.out.println(str);
    }

    private static FilteringField buildFilteringField(String fieldName, String fieldLabel, String fieldDescription,
                                                      FilteringTypeEnum filteringType, Float filteringSequence,
                                                      List<ValueLabelPair> defaultValues,
                                                      Long dictionaryCategoryUid,
                                                      CascadingField cascadingField) {
        FilteringField filteringField = new FilteringField();
        filteringField.setFieldName(fieldName);
        filteringField.setFieldLabel(fieldLabel);
        filteringField.setFieldDescription(fieldDescription);
        filteringField.setFilteringType(filteringType);
        filteringField.setFilteringSequence(filteringSequence);
        filteringField.setFieldDefaultValues(defaultValues);
        filteringField.setCascadingDictionaryCategoryUid(dictionaryCategoryUid);
        filteringField.setCascadingField(cascadingField);

        return filteringField;
    }

    private static ListingField buildListingField(String fieldName, String fieldLabel, String fieldDescription,
                                                  Integer width, Float listingSequence,
                                                  ImagePreview imagePreview,
                                                  FileDownload fileDownload) {
        ListingField listingField = new ListingField();
        listingField.setFieldName(fieldName);
        listingField.setFieldLabel(fieldLabel);
        listingField.setFieldDescription(fieldDescription);
        listingField.setWidth(width);
        listingField.setListingSequence(listingSequence);
        listingField.setImagePreview(imagePreview);
        listingField.setFileDownload(fileDownload);

        return listingField;
    }

    private static SortingField buildSortingField(String fieldName, String fieldLabel, String fieldDescription,
                                                  Sort.Direction direction, Float sortingSequence) {
        SortingField sortingField = new SortingField();
        sortingField.setFieldName(fieldName);
        sortingField.setFieldLabel(fieldLabel);
        sortingField.setFieldDescription(fieldDescription);
        sortingField.setDirection(direction);
        sortingField.setSortingSequence(sortingSequence);

        return sortingField;
    }
}
