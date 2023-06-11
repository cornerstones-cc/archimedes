package cc.cornerstones.biz.serve.service.assembly.datawidget.table;

import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.*;
import cc.cornerstones.almond.utils.AbcTreeNodeUtils;
import cc.cornerstones.biz.datafacet.dto.DataFacetExportDto;
import cc.cornerstones.biz.datafacet.dto.DataFacetQueryDto;
import cc.cornerstones.biz.datafacet.entity.*;
import cc.cornerstones.biz.datafacet.persistence.*;
import cc.cornerstones.biz.datafacet.share.constants.DataFieldTypeEnum;
import cc.cornerstones.biz.datafacet.service.inf.ExecuteDataFacetService;
import cc.cornerstones.biz.datafacet.share.types.*;
import cc.cornerstones.biz.datadictionary.persistence.DictionaryCategoryRepository;
import cc.cornerstones.biz.datadictionary.persistence.DictionaryStructureNodeRepository;
import cc.cornerstones.biz.datadictionary.service.inf.DictionaryService;
import cc.cornerstones.biz.datawidget.service.assembly.table.TableDataWidgetExport;
import cc.cornerstones.biz.datawidget.service.assembly.table.TableDataWidgetQuery;
import cc.cornerstones.biz.datawidget.share.constants.DataWidgetTypeEnum;
import cc.cornerstones.biz.serve.service.assembly.datawidget.ExecuteDataWidgetHandler;
import cc.cornerstones.biz.share.constants.ExportOptionEnum;
import cc.cornerstones.biz.share.constants.FilteringTypeEnum;
import cc.cornerstones.biz.share.constants.NamingPolicyEnum;
import cc.cornerstones.biz.share.constants.SelectionFieldTypeEnum;
import cc.cornerstones.biz.share.types.*;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class ExecuteTableDataWidgetHandler implements ExecuteDataWidgetHandler {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ExecuteTableDataWidgetHandler.class);

    @Autowired
    private ExecuteDataFacetService executeDataFacetService;

    @Autowired
    private DataFacetRepository dataFacetRepository;

    @Autowired
    private DataFieldRepository dataFieldRepository;

    @Autowired
    private FilteringDataFieldRepository filteringDataFieldRepository;

    @Autowired
    private FilteringExtendedRepository filteringExtendedRepository;

    @Autowired
    private ListingDataFieldRepository listingDataFieldRepository;

    @Autowired
    private ListingExtendedRepository listingExtendedRepository;

    @Autowired
    private SortingDataFieldRepository sortingDataFieldRepository;

    @Autowired
    private ExportBasicRepository exportBasicRepository;

    @Autowired
    private AdvancedFeatureRepository advancedFeatureRepository;

    @Autowired
    private DictionaryCategoryRepository dictionaryCategoryRepository;

    @Autowired
    private DictionaryStructureNodeRepository dictionaryStructureNodeRepository;

    @Autowired
    private DictionaryService dictionaryService;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Data widget type
     *
     * @return
     */
    @Override
    public DataWidgetTypeEnum type() {
        return DataWidgetTypeEnum.TABLE;
    }

    private DataFacetQueryDto buildDataFacetQuery(
            Long dataFacetUid,
            JSONObject dataWidgetCharacteristics,
            JSONObject request,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, 获取 (table) data widget characteristics
        //
        DataFacetDo dataFacetDo = this.dataFacetRepository.findByUid(dataFacetUid);
        if (dataFacetDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataFacetDo.RESOURCE_SYMBOL,
                    dataFacetUid));
        }
        List<DataFieldDo> dataFieldDoList = this.dataFieldRepository.findByDataFacetUid(dataFacetUid);
        if (CollectionUtils.isEmpty(dataFieldDoList)) {
            throw new AbcResourceNotFoundException(String.format("%s::data_facet_uid=%d, no data field",
                    DataFieldDo.RESOURCE_SYMBOL,
                    dataFacetUid));
        }
        Map<String, DataFieldDo> dataFieldDoMap = new HashMap<>();
        dataFieldDoList.forEach(dataFieldDo -> {
            dataFieldDoMap.put(dataFieldDo.getName(), dataFieldDo);
        });

        // 取 data facet appearance settings
        List<FilteringDataFieldDo> filteringDataFieldDoList =
                this.filteringDataFieldRepository.findByDataFacetUid(
                        dataFacetUid, Sort.by(Sort.Order.asc("filteringSequence")));

        List<ListingDataFieldDo> listingDataFieldDoList =
                this.listingDataFieldRepository.findByDataFacetUid(
                        dataFacetUid, Sort.by(Sort.Order.asc("listingSequence")));

        ListingExtendedDo listingExtendedDo = this.listingExtendedRepository.findByDataFacetUid(dataFacetUid);

        List<SortingDataFieldDo> sortingDataFieldDoList =
                this.sortingDataFieldRepository.findByDataFacetUid(
                        dataFacetUid, Sort.by(Sort.Order.asc("sortingSequence")));

        // 构造 available filtering field names & filtering type
        Map<String, FilteringTypeEnum> availableFilteringFieldNameAndFilteringTypeMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(filteringDataFieldDoList)) {
            filteringDataFieldDoList.forEach(filteringField -> {
                availableFilteringFieldNameAndFilteringTypeMap.put(
                        filteringField.getFieldName(), filteringField.getFilteringType());
            });
        }

        // 构造 available listing field names
        List<String> availableListingFieldNamesInOrder = new LinkedList<>();
        if (!CollectionUtils.isEmpty(listingDataFieldDoList)) {
            listingDataFieldDoList.forEach(listingField -> {
                availableListingFieldNamesInOrder.add(listingField.getFieldName());
            });
        }

        // 构造 available sorting field names
        // 构造 sort
        List<String> availableSortingFieldNames = new LinkedList<>();
        List<AbcOrder> orders = new LinkedList<>();
        if (!CollectionUtils.isEmpty(sortingDataFieldDoList)) {
            sortingDataFieldDoList.forEach(sortingField -> {
                if (ObjectUtils.isEmpty(sortingField.getFieldName())
                        || sortingField.getDirection() == null) {
                    return;
                }

                availableSortingFieldNames.add(sortingField.getFieldName());

                switch (sortingField.getDirection()) {
                    case ASC:
                        orders.add(new AbcOrder(sortingField.getFieldName(), Sort.Direction.ASC));
                        break;
                    case DESC:
                        orders.add(new AbcOrder(sortingField.getFieldName(), Sort.Direction.DESC));
                        break;
                    default:
                        break;
                }
            });
        }
        AbcSort sort = null;
        if (!CollectionUtils.isEmpty(orders)) {
            sort = new AbcSort(orders);
        }

        // 构造 pagination
        int pageSize = 20;
        if (listingExtendedDo != null) {
            if (listingExtendedDo.getDefaultPageSize() != null) {
                pageSize = listingExtendedDo.getDefaultPageSize();
            }
        }

        //
        // Step 2, 构造 data facet query
        //
        final DataFacetQueryDto dataFacetQueryDto = new DataFacetQueryDto();
        dataFacetQueryDto.setOperatingUserProfile(operatingUserProfile);

        if (request == null) {
            // default query

            //
            // selection fields
            //
            dataFacetQueryDto.setSelectionFields(new LinkedList<>());
            availableListingFieldNamesInOrder.forEach(fieldName -> {
                SelectionField selectionField = new SelectionField();
                selectionField.setType(SelectionFieldTypeEnum.PLAIN);

                PlainSelectionField plainSelectionField = new PlainSelectionField();
                plainSelectionField.setFieldName(fieldName);
                plainSelectionField.setFieldLabel(dataFieldDoMap.get(fieldName).getLabel());
                plainSelectionField.setFieldType(dataFieldDoMap.get(fieldName).getType());

                selectionField.setContent((JSONObject) JSONObject.toJSON(plainSelectionField));

                dataFacetQueryDto.getSelectionFields().add(selectionField);
            });

            //
            // sort
            //
            dataFacetQueryDto.setSort(sort);

            //
            // pagination
            //
            dataFacetQueryDto.setPagination(new AbcPagination(0, pageSize));
        } else {
            // custom query
            TableDataWidgetQuery tableDataWidgetQuery = null;
            try {
                tableDataWidgetQuery = JSONObject.toJavaObject(request, TableDataWidgetQuery.class);
            } catch (Exception e) {
                LOGGER.error("fail to parse query object:{}, for data widget type:%s", request, type(), e);
                throw new AbcIllegalParameterException(String.format("illegal request for data widget " +
                        "type:%s", type()));
            }
            if (tableDataWidgetQuery == null) {
                throw new AbcIllegalParameterException(String.format("illegal request for data widget " +
                        "type:%s", type()));
            }

            //
            // selection fields
            //
            dataFacetQueryDto.setSelectionFields(new LinkedList<>());
            if (CollectionUtils.isEmpty(tableDataWidgetQuery.getSelectionFieldNames())) {
                availableListingFieldNamesInOrder.forEach(fieldName -> {
                    SelectionField selectionField = new SelectionField();
                    selectionField.setType(SelectionFieldTypeEnum.PLAIN);

                    PlainSelectionField plainSelectionField = new PlainSelectionField();
                    plainSelectionField.setFieldName(fieldName);
                    plainSelectionField.setFieldLabel(dataFieldDoMap.get(fieldName).getLabel());
                    plainSelectionField.setFieldType(dataFieldDoMap.get(fieldName).getType());

                    selectionField.setContent((JSONObject) JSONObject.toJSON(plainSelectionField));

                    dataFacetQueryDto.getSelectionFields().add(selectionField);
                });
            } else {
                List<String> selectionFieldNames = tableDataWidgetQuery.getSelectionFieldNames();
                availableListingFieldNamesInOrder.forEach(fieldName -> {
                    if (selectionFieldNames.contains(fieldName)) {
                        SelectionField selectionField = new SelectionField();
                        selectionField.setType(SelectionFieldTypeEnum.PLAIN);

                        PlainSelectionField plainSelectionField = new PlainSelectionField();
                        plainSelectionField.setFieldName(fieldName);
                        plainSelectionField.setFieldLabel(dataFieldDoMap.get(fieldName).getLabel());
                        plainSelectionField.setFieldType(dataFieldDoMap.get(fieldName).getType());

                        selectionField.setContent((JSONObject) JSONObject.toJSON(plainSelectionField));

                        dataFacetQueryDto.getSelectionFields().add(selectionField);
                    }
                });

                if (CollectionUtils.isEmpty(dataFacetQueryDto.getSelectionFields())) {
                    // 交集结果为空，即，请求的 result field names 都不存在或者不被允许访问
                    throw new AbcIllegalParameterException(String.format("illegal expected selection field names"));
                }

            }

            //
            // plain filters
            //
            if (!CollectionUtils.isEmpty(tableDataWidgetQuery.getFilters())) {
                List<AbcTuple4<String, String[], FilteringTypeEnum, DataFieldTypeEnum>> transformedFilters = new LinkedList<>();
                tableDataWidgetQuery.getFilters().forEach((fieldName, arrayOfParameterValue) -> {
                    FilteringTypeEnum filterType = availableFilteringFieldNameAndFilteringTypeMap.get(fieldName);
                    if (filterType != null) {
                        AbcTuple4<String, String[], FilteringTypeEnum, DataFieldTypeEnum> tuple =
                                new AbcTuple4<>(fieldName,
                                        arrayOfParameterValue,
                                        filterType, dataFieldDoMap.get(fieldName).getType());
                        transformedFilters.add(tuple);
                    }
                });
                if (!CollectionUtils.isEmpty(transformedFilters)) {
                    dataFacetQueryDto.setPlainFilters(new LinkedList<>());
                    transformedFilters.forEach(filter -> {
                        PlainFilter plainFilter = new PlainFilter();
                        plainFilter.setContent(filter);
                        dataFacetQueryDto.getPlainFilters().add(plainFilter);
                    });
                }
            }

            //
            // cascading filters
            //
            // 对于每个级联关系（含N个字段），只会有 1 个 Cascading filter 字段对应
            if (!CollectionUtils.isEmpty(tableDataWidgetQuery.getCascadingFilters())) {
                // 收集 cascading filter
                Map<String, FilteringFieldCascadingSettings> cascadingFilterMap = new HashMap<>();
                filteringDataFieldDoList.forEach(filteringDataFieldDo -> {
                    switch (filteringDataFieldDo.getFilteringType()) {
                        case CASCADING_DROP_DOWN_LIST_SINGLE:
                        case CASCADING_DROP_DOWN_LIST_MULTIPLE: {
                            if (filteringDataFieldDo.getFilteringTypeExtension() != null
                                    && !filteringDataFieldDo.getFilteringTypeExtension().isEmpty()) {
                                FilteringFieldCascadingSettings filteringFieldCascadingSettings =
                                        JSONObject.toJavaObject(
                                                filteringDataFieldDo.getFilteringTypeExtension(),
                                                FilteringFieldCascadingSettings.class);
                                if (!cascadingFilterMap.containsKey(filteringFieldCascadingSettings.getFilterName())) {
                                    cascadingFilterMap.put(filteringFieldCascadingSettings.getFilterName(),
                                            filteringFieldCascadingSettings);
                                }
                            }
                        }
                        break;
                        default:
                            break;
                    }
                });

                List<CascadingFilter> cascadingFilters = new LinkedList<>();
                tableDataWidgetQuery.getCascadingFilters().forEach((cascadingFilterName,
                                                                    arrayOfParameterValue) -> {
                    FilteringFieldCascadingSettings filteringFieldCascadingSettings =
                            cascadingFilterMap.get(cascadingFilterName);
                    if (filteringFieldCascadingSettings == null) {
                        LOGGER.error("cannot find cascading filter {} in the data facet {}", cascadingFilterName,
                                dataFacetUid);
                        return;
                    }

                    if (arrayOfParameterValue == null || arrayOfParameterValue.length == 0) {
                        LOGGER.error("null or empty parameter value found in the cascading filter {} in the data " +
                                "facet {}", cascadingFilterName, dataFacetUid);
                        return;
                    }

                    CascadingFilter cascadingFilter = new CascadingFilter();
                    cascadingFilters.add(cascadingFilter);
                    cascadingFilter.setContent(new LinkedList<>());
                    // Cascading filter 的组成字段，按列表顺序从低到高依次是 level 0, level 1, level ..., level N
                    List<String> fieldNames = filteringFieldCascadingSettings.getFields();

                    for (int i = 0; i < arrayOfParameterValue.length; i++) {
                        String parameterValue = arrayOfParameterValue[i];
                        if (ObjectUtils.isEmpty(parameterValue)) {
                            LOGGER.error("null or empty parameter value found in the cascading filter {} in the data " +
                                    "facet {}", cascadingFilterName, dataFacetUid);
                            return;
                        }

                        // cascading filter 的单个 parameter value 约定按照 level 0 value > level 1 value > ... > level N
                        // value 样式组成字符串
                        String[] slicesOfParameterValue = parameterValue.split(">");

                        List<AbcTuple3<String, String[], DataFieldTypeEnum>> tuples = new LinkedList<>();
                        cascadingFilter.getContent().add(tuples);

                        for (int j = 0; j < slicesOfParameterValue.length; j++) {
                            String slice = slicesOfParameterValue[j].trim();
                            String[] arrayOfFieldValue = slice.split(",");
                            if (fieldNames.size() >= slicesOfParameterValue.length) {
                                String fieldName = fieldNames.get(j);
                                AbcTuple3<String, String[], DataFieldTypeEnum> tuple = new AbcTuple3<>(fieldName,
                                        arrayOfFieldValue, dataFieldDoMap.get(fieldName).getType());
                                tuples.add(tuple);
                            } else {
                                LOGGER.error("mismatched data '{}' found in cascading filter {} in the data facet {}",
                                        parameterValue, cascadingFilterName, dataFacetUid);
                            }
                        }
                    }
                });

                if (!CollectionUtils.isEmpty(cascadingFilters)) {
                    dataFacetQueryDto.setCascadingFilters(cascadingFilters);
                }
            }

            //
            // sort
            //
            if (tableDataWidgetQuery.getSort() == null
                    || CollectionUtils.isEmpty(tableDataWidgetQuery.getSort().getOrders())) {
                dataFacetQueryDto.setSort(sort);
            } else {
                // 验证每个字段是否被允许作为 Order，并且按 Order 字段顺序重新排列
                List<AbcOrder> transformedOrders = new LinkedList<>();

                // 先找出被允许作为 Order 的
                Map<String, AbcOrder> expectedOrders = new HashMap<>();
                tableDataWidgetQuery.getSort().getOrders().forEach(order -> {
                    if (availableSortingFieldNames.contains(order.getProperty())) {
                        expectedOrders.put(order.getProperty(), order);
                    }
                });
                // 再按照 Order 字段顺序重新排列
                availableSortingFieldNames.forEach(fieldName -> {
                    if (expectedOrders.containsKey(fieldName)) {
                        transformedOrders.add(expectedOrders.get(fieldName));
                    }
                });
                dataFacetQueryDto.setSort(new AbcSort(transformedOrders));
            }

            //
            // pagination
            //
            if (tableDataWidgetQuery.getPagination() != null
                    && tableDataWidgetQuery.getPagination().getPage() >= 0
                    && tableDataWidgetQuery.getPagination().getSize() > 0) {
                if (tableDataWidgetQuery.getPagination().getSize() > 1000) {
                    tableDataWidgetQuery.getPagination().setSize(1000);
                }
                dataFacetQueryDto.setPagination(tableDataWidgetQuery.getPagination());
            } else {
                dataFacetQueryDto.setPagination(new AbcPagination(0, pageSize));
            }
        }

        return dataFacetQueryDto;
    }

    private DataFacetExportDto buildDataFacetExport(
            Long dataFacetUid,
            JSONObject dataWidgetCharacteristics,
            JSONObject request,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, 获取 (table) data widget characteristics
        //
        DataFacetDo dataFacetDo = this.dataFacetRepository.findByUid(dataFacetUid);
        if (dataFacetDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataFacetDo.RESOURCE_SYMBOL,
                    dataFacetUid));
        }
        List<DataFieldDo> dataFieldDoList = this.dataFieldRepository.findByDataFacetUid(dataFacetUid);
        if (CollectionUtils.isEmpty(dataFieldDoList)) {
            throw new AbcResourceNotFoundException(String.format("%s::data_facet_uid=%d, no data field",
                    DataFieldDo.RESOURCE_SYMBOL,
                    dataFacetUid));
        }
        Map<String, DataFieldDo> dataFieldDoMap = new HashMap<>();
        dataFieldDoList.forEach(dataFieldDo -> {
            dataFieldDoMap.put(dataFieldDo.getName(), dataFieldDo);
        });

        // 取 data facet appearance settings
        List<FilteringDataFieldDo> filteringDataFieldDoList =
                this.filteringDataFieldRepository.findByDataFacetUid(
                        dataFacetUid, Sort.by(Sort.Order.asc("filteringSequence")));

        FilteringExtendedDo filteringExtendedDo = this.filteringExtendedRepository.findByDataFacetUid(dataFacetUid);

        List<ListingDataFieldDo> listingDataFieldDoList =
                this.listingDataFieldRepository.findByDataFacetUid(
                        dataFacetUid, Sort.by(Sort.Order.asc("listingSequence")));

        List<SortingDataFieldDo> sortingDataFieldDoList =
                this.sortingDataFieldRepository.findByDataFacetUid(
                        dataFacetUid, Sort.by(Sort.Order.asc("sortingSequence")));

        // 构造 available filtering field names & filtering type
        Map<String, FilteringTypeEnum> availableFilteringFieldNameAndFilteringTypeMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(filteringDataFieldDoList)) {
            filteringDataFieldDoList.forEach(filteringField -> {
                availableFilteringFieldNameAndFilteringTypeMap.put(
                        filteringField.getFieldName(), filteringField.getFilteringType());
            });
        }

        // 构造 available listing field names
        List<String> availableListingFieldNamesInOrder = new LinkedList<>();
        if (!CollectionUtils.isEmpty(listingDataFieldDoList)) {
            listingDataFieldDoList.forEach(listingField -> {
                availableListingFieldNamesInOrder.add(listingField.getFieldName());
            });
        }

        // 构造 available sorting field names
        // 构造 sort
        List<String> availableSortingFieldNames = new LinkedList<>();
        List<AbcOrder> orders = new LinkedList<>();
        if (!CollectionUtils.isEmpty(sortingDataFieldDoList)) {
            sortingDataFieldDoList.forEach(sortingField -> {
                if (sortingField.getDirection() == null) {
                    return;
                }
                availableSortingFieldNames.add(sortingField.getFieldName());

                switch (sortingField.getDirection()) {
                    case ASC:
                        orders.add(new AbcOrder(sortingField.getFieldName(), Sort.Direction.ASC));
                        break;
                    case DESC:
                        orders.add(new AbcOrder(sortingField.getFieldName(), Sort.Direction.DESC));
                        break;
                    default:
                        break;
                }
            });
        }
        AbcSort sort = new AbcSort(orders);

        //
        // Step 2, 构造 data facet export
        //
        final DataFacetExportDto dataFacetExportDto = new DataFacetExportDto();
        dataFacetExportDto.setOperatingUserProfile(operatingUserProfile);

        if (request == null) {
            // default query

            //
            // selection fields
            //
            dataFacetExportDto.setSelectionFields(new LinkedList<>());
            availableListingFieldNamesInOrder.forEach(fieldName -> {
                SelectionField selectionField = new SelectionField();
                selectionField.setType(SelectionFieldTypeEnum.PLAIN);

                PlainSelectionField plainSelectionField = new PlainSelectionField();
                plainSelectionField.setFieldName(fieldName);
                plainSelectionField.setFieldLabel(dataFieldDoMap.get(fieldName).getLabel());
                plainSelectionField.setFieldType(dataFieldDoMap.get(fieldName).getType());

                selectionField.setContent((JSONObject) JSONObject.toJSON(plainSelectionField));

                dataFacetExportDto.getSelectionFields().add(selectionField);
            });

            //
            // sort
            //
            dataFacetExportDto.setSort(sort);

            //
            // export specific
            //
            dataFacetExportDto.setExportOption(ExportOptionEnum.EXPORT_CSV);

        } else {
            // custom query
            TableDataWidgetExport tableDataWidgetExport = null;
            try {
                tableDataWidgetExport = JSONObject.toJavaObject(request, TableDataWidgetExport.class);
            } catch (Exception e) {
                LOGGER.error("fail to parse query object:{}, for data widget type:%s", request, type(), e);
                throw new AbcIllegalParameterException(String.format("illegal request for data widget " +
                        "type:%s", type()));
            }
            if (tableDataWidgetExport == null) {
                throw new AbcIllegalParameterException(String.format("illegal request for data widget " +
                        "type:%s", type()));
            }

            //
            // selection fields
            //
            List<String> objectiveSelectionFieldNameList = new LinkedList<>();

            dataFacetExportDto.setVisibleSelectionFields(new LinkedList<>());
            dataFacetExportDto.setSelectionFields(new LinkedList<>());

            if (CollectionUtils.isEmpty(tableDataWidgetExport.getSelectionFieldNames())) {
                availableListingFieldNamesInOrder.forEach(fieldName -> {
                    SelectionField selectionField = new SelectionField();
                    selectionField.setType(SelectionFieldTypeEnum.PLAIN);

                    PlainSelectionField plainSelectionField = new PlainSelectionField();
                    plainSelectionField.setFieldName(fieldName);
                    plainSelectionField.setFieldLabel(dataFieldDoMap.get(fieldName).getLabel());
                    plainSelectionField.setFieldType(dataFieldDoMap.get(fieldName).getType());

                    selectionField.setContent((JSONObject) JSONObject.toJSON(plainSelectionField));

                    dataFacetExportDto.getVisibleSelectionFields().add(selectionField);
                    dataFacetExportDto.getSelectionFields().add(selectionField);

                    objectiveSelectionFieldNameList.add(fieldName);
                });
            } else {
                List<String> selectionFieldNames = tableDataWidgetExport.getSelectionFieldNames();
                availableListingFieldNamesInOrder.forEach(fieldName -> {
                    if (selectionFieldNames.contains(fieldName)) {
                        SelectionField selectionField = new SelectionField();
                        selectionField.setType(SelectionFieldTypeEnum.PLAIN);

                        PlainSelectionField plainSelectionField = new PlainSelectionField();
                        plainSelectionField.setFieldName(fieldName);
                        plainSelectionField.setFieldLabel(dataFieldDoMap.get(fieldName).getLabel());
                        plainSelectionField.setFieldType(dataFieldDoMap.get(fieldName).getType());

                        selectionField.setContent((JSONObject) JSONObject.toJSON(plainSelectionField));

                        dataFacetExportDto.getVisibleSelectionFields().add(selectionField);
                        dataFacetExportDto.getSelectionFields().add(selectionField);

                        objectiveSelectionFieldNameList.add(fieldName);
                    }
                });

                if (CollectionUtils.isEmpty(dataFacetExportDto.getVisibleSelectionFields())) {
                    // 交集结果为空，即，请求的 result field names 都不存在或者不被允许访问
                    throw new AbcIllegalParameterException(String.format("illegal expected selection field names"));
                }
            }

            //
            // plain filters
            //
            if (!CollectionUtils.isEmpty(tableDataWidgetExport.getFilters())) {
                List<AbcTuple4<String, String[], FilteringTypeEnum, DataFieldTypeEnum>> transformedFilters = new LinkedList<>();
                tableDataWidgetExport.getFilters().forEach((fieldName, arrayOfParameterValue) -> {
                    FilteringTypeEnum filterType = availableFilteringFieldNameAndFilteringTypeMap.get(fieldName);
                    if (filterType != null) {
                        AbcTuple4<String, String[], FilteringTypeEnum, DataFieldTypeEnum> tuple =
                                new AbcTuple4<>(fieldName,
                                        arrayOfParameterValue,
                                        filterType,
                                        dataFieldDoMap.get(fieldName).getType());
                        transformedFilters.add(tuple);
                    }
                });
                if (!CollectionUtils.isEmpty(transformedFilters)) {
                    dataFacetExportDto.setPlainFilters(new LinkedList<>());
                    transformedFilters.forEach(filter -> {
                        PlainFilter plainFilter = new PlainFilter();
                        plainFilter.setContent(filter);
                        dataFacetExportDto.getPlainFilters().add(plainFilter);
                    });
                }
            }

            //
            // cascading filters
            //
            // 对于每个级联关系（含N个字段），只会有 1 个 Cascading filter 字段对应
            if (!CollectionUtils.isEmpty(tableDataWidgetExport.getCascadingFilters())) {
                // 收集 cascading filter
                Map<String, FilteringFieldCascadingSettings> cascadingFilterMap = new HashMap<>();
                filteringDataFieldDoList.forEach(filteringDataFieldDo -> {
                    switch (filteringDataFieldDo.getFilteringType()) {
                        case CASCADING_DROP_DOWN_LIST_SINGLE:
                        case CASCADING_DROP_DOWN_LIST_MULTIPLE: {
                            if (filteringDataFieldDo.getFilteringTypeExtension() != null
                                    && !filteringDataFieldDo.getFilteringTypeExtension().isEmpty()) {
                                FilteringFieldCascadingSettings filteringFieldCascadingSettings =
                                        JSONObject.toJavaObject(
                                                filteringDataFieldDo.getFilteringTypeExtension(),
                                                FilteringFieldCascadingSettings.class);
                                if (!cascadingFilterMap.containsKey(filteringFieldCascadingSettings.getFilterName())) {
                                    cascadingFilterMap.put(filteringFieldCascadingSettings.getFilterName(),
                                            filteringFieldCascadingSettings);
                                }
                            }
                        }
                        break;
                        default:
                            break;
                    }
                });

                List<CascadingFilter> cascadingFilters = new LinkedList<>();
                tableDataWidgetExport.getCascadingFilters().forEach((cascadingFilterName,
                                                                     arrayOfParameterValue) -> {
                    FilteringFieldCascadingSettings filteringFieldCascadingSettings =
                            cascadingFilterMap.get(cascadingFilterName);
                    if (filteringFieldCascadingSettings == null) {
                        LOGGER.error("cannot find cascading filter {} in the data facet {}", cascadingFilterName,
                                dataFacetUid);
                        return;
                    }

                    if (arrayOfParameterValue == null || arrayOfParameterValue.length == 0) {
                        LOGGER.error("null or empty parameter value found in the cascading filter {} in the data " +
                                "facet {}", cascadingFilterName, dataFacetUid);
                        return;
                    }

                    CascadingFilter cascadingFilter = new CascadingFilter();
                    cascadingFilters.add(cascadingFilter);
                    cascadingFilter.setContent(new LinkedList<>());
                    // Cascading filter 的组成字段，按列表顺序从低到高依次是 level 0, level 1, level ..., level N
                    List<String> fieldNames = filteringFieldCascadingSettings.getFields();

                    for (int i = 0; i < arrayOfParameterValue.length; i++) {
                        String parameterValue = arrayOfParameterValue[i];
                        if (ObjectUtils.isEmpty(parameterValue)) {
                            LOGGER.error("null or empty parameter value found in the cascading filter {} in the data " +
                                    "facet {}", cascadingFilterName, dataFacetUid);
                            return;
                        }

                        // cascading filter 的单个 parameter value 约定按照 level 0 value > level 1 value > ... > level N
                        // value 样式组成字符串
                        String[] slicesOfParameterValue = parameterValue.split(">");

                        List<AbcTuple3<String, String[], DataFieldTypeEnum>> tuples = new LinkedList<>();
                        cascadingFilter.getContent().add(tuples);

                        for (int j = 0; j < slicesOfParameterValue.length; j++) {
                            String slice = slicesOfParameterValue[j].trim();
                            String[] arrayOfFieldValue = slice.split(",");
                            if (fieldNames.size() >= slicesOfParameterValue.length) {
                                String fieldName = fieldNames.get(j);
                                AbcTuple3<String, String[], DataFieldTypeEnum> tuple =
                                        new AbcTuple3<>(fieldName,
                                                arrayOfFieldValue,
                                                dataFieldDoMap.get(fieldName).getType());
                                tuples.add(tuple);
                            } else {
                                LOGGER.error("mismatched data '{}' found in cascading filter {} in the data facet {}",
                                        parameterValue, cascadingFilterName, dataFacetUid);
                            }
                        }
                    }
                });

                if (!CollectionUtils.isEmpty(cascadingFilters)) {
                    dataFacetExportDto.setCascadingFilters(cascadingFilters);
                }
            }

            //
            // sort
            //
            if (tableDataWidgetExport.getSort() == null
                    || CollectionUtils.isEmpty(tableDataWidgetExport.getSort().getOrders())) {
                dataFacetExportDto.setSort(sort);
            } else {
                // 验证每个字段是否被允许作为 Order，并且按 Order 字段顺序重新排列
                List<AbcOrder> transformedOrders = new LinkedList<>();

                // 先找出被允许作为 Order 的
                Map<String, AbcOrder> expectedOrders = new HashMap<>();
                tableDataWidgetExport.getSort().getOrders().forEach(order -> {
                    if (availableSortingFieldNames.contains(order.getProperty())) {
                        expectedOrders.put(order.getProperty(), order);
                    }
                });
                // 再按照 Order 字段顺序重新排列
                availableSortingFieldNames.forEach(fieldName -> {
                    if (expectedOrders.containsKey(fieldName)) {
                        transformedOrders.add(expectedOrders.get(fieldName));
                    }
                });
                dataFacetExportDto.setSort(new AbcSort(transformedOrders));
            }

            // export specific
            if (tableDataWidgetExport.getExportOption() == null) {
                dataFacetExportDto.setExportOption(ExportOptionEnum.EXPORT_CSV);
            } else {
                dataFacetExportDto.setExportOption(tableDataWidgetExport.getExportOption());

                if (tableDataWidgetExport.getExportOption().equals(ExportOptionEnum.EXPORT_AS_TEMPLATE)) {
                    if (tableDataWidgetExport.getExportExtendedTemplateUid() == null) {
                        throw new AbcIllegalParameterException("export_extended_template_uid is required while trying" +
                                " to export as template");
                    }
                    dataFacetExportDto.setExportExtendedTemplateUid(tableDataWidgetExport.getExportExtendedTemplateUid());
                }

                // export csv w/ attachments & export excel w/ attachments 的 naming policy 可能涉及超出 selection fields 的
                // field，需要加入到 selection fields
                switch (tableDataWidgetExport.getExportOption()) {
                    case EXPORT_CSV_W_ATTACHMENTS:
                    case EXPORT_EXCEL_W_ATTACHMENTS: {
                        if (!CollectionUtils.isEmpty(listingDataFieldDoList)) {
                            listingDataFieldDoList.forEach(listingField -> {
                                if (listingField.getExtension() == null || listingField.getExtension().isEmpty()) {
                                    return;
                                }

                                DataFieldDo dataFieldDo = dataFieldDoMap.get(listingField.getFieldName());
                                if (dataFieldDo == null && dataFieldDo.getType() == null) {
                                    return;
                                }

                                switch (dataFieldDo.getType()) {
                                    case IMAGE: {
                                        ListingFieldTypeExtensionImage listingFieldTypeExtensionImage =
                                                JSONObject.toJavaObject(listingField.getExtension(),
                                                        ListingFieldTypeExtensionImage.class);
                                        if (NamingPolicyEnum.COMBINE.equals(listingFieldTypeExtensionImage.getNamingPolicy())
                                                && listingFieldTypeExtensionImage.getNamingPolicyExtCombine() != null
                                                && !CollectionUtils.isEmpty(listingFieldTypeExtensionImage.getNamingPolicyExtCombine().getFields())) {
                                            List<NamingPolicyExtCombineField> fields =
                                                    listingFieldTypeExtensionImage.getNamingPolicyExtCombine().getFields();
                                            fields.forEach(field -> {
                                                if (Boolean.TRUE.equals(field.getFixed())) {
                                                    return;
                                                }

                                                if (!objectiveSelectionFieldNameList.contains(field.getFieldName())) {
                                                    SelectionField selectionField = new SelectionField();
                                                    selectionField.setType(SelectionFieldTypeEnum.PLAIN);

                                                    PlainSelectionField plainSelectionField = new PlainSelectionField();
                                                    plainSelectionField.setFieldName(field.getFieldName());

                                                    selectionField.setContent((JSONObject) JSONObject.toJSON(plainSelectionField));

                                                    dataFacetExportDto.getSelectionFields().add(selectionField);
                                                }
                                            });
                                        }
                                    }
                                    break;
                                    case FILE: {
                                        ListingFieldTypeExtensionFile listingFieldTypeExtensionFile =
                                                JSONObject.toJavaObject(listingField.getExtension(),
                                                        ListingFieldTypeExtensionFile.class);
                                        if (NamingPolicyEnum.COMBINE.equals(listingFieldTypeExtensionFile.getNamingPolicy())
                                                && listingFieldTypeExtensionFile.getNamingPolicyExtCombine() != null
                                                && !CollectionUtils.isEmpty(listingFieldTypeExtensionFile.getNamingPolicyExtCombine().getFields())) {
                                            List<NamingPolicyExtCombineField> fields =
                                                    listingFieldTypeExtensionFile.getNamingPolicyExtCombine().getFields();
                                            fields.forEach(field -> {
                                                if (Boolean.TRUE.equals(field.getFixed())) {
                                                    return;
                                                }

                                                if (!objectiveSelectionFieldNameList.contains(field.getFieldName())) {
                                                    SelectionField selectionField = new SelectionField();
                                                    selectionField.setType(SelectionFieldTypeEnum.PLAIN);

                                                    PlainSelectionField plainSelectionField = new PlainSelectionField();
                                                    plainSelectionField.setFieldName(field.getFieldName());

                                                    selectionField.setContent((JSONObject) JSONObject.toJSON(plainSelectionField));

                                                    dataFacetExportDto.getSelectionFields().add(selectionField);
                                                }
                                            });
                                        }
                                    }
                                    break;
                                    default:
                                        break;
                                }
                            });
                        }
                    }
                    break;
                    default:
                        break;
                }
            }
        }

        return dataFacetExportDto;
    }

    /**
     * Query content of the specified Data Facet
     *
     * @param dataFacetUid
     * @param dataWidgetCharacteristics
     * @param request
     * @param operatingUserProfile
     * @return
     * @throws AbcUndefinedException
     */
    @Override
    public QueryContentResult queryContent(
            Long dataFacetUid,
            JSONObject dataWidgetCharacteristics,
            JSONObject request,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DataFacetQueryDto dataFacetQueryDto = buildDataFacetQuery(
                dataFacetUid,
                dataWidgetCharacteristics,
                request,
                operatingUserProfile);

        //
        // Step 2, core-processing
        //
        QueryContentResult queryContentResult = this.executeDataFacetService.queryContent(
                dataFacetUid,
                dataFacetQueryDto,
                operatingUserProfile);

        //
        // Step 3, post-processing
        //
        return queryContentResult;
    }

    /**
     * Export content of the specified Data Facet
     *
     * @param dataFacetUid
     * @param dataWidgetCharacteristics
     * @param request
     * @param operatingUserProfile
     * @return
     * @throws AbcUndefinedException
     */
    @Override
    public Long exportContent(
            Long dataFacetUid,
            JSONObject dataWidgetCharacteristics,
            JSONObject request,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DataFacetExportDto dataFacetExportDto = buildDataFacetExport(
                dataFacetUid,
                dataWidgetCharacteristics,
                request,
                operatingUserProfile);

        //
        // Step 2, core-processing
        //
        Long taskUid = this.executeDataFacetService.exportContent(
                dataFacetUid,
                dataFacetExportDto,
                operatingUserProfile);

        //
        // Step 3, post-processing
        //
        return taskUid;
    }

    @Override
    public Object generateServeCharacteristics(
            Long dataFacetUid) throws AbcUndefinedException {
        //
        // step 1, pre-processing
        //
        DataFacetDo dataFacetDo = this.dataFacetRepository.findByUid(dataFacetUid);
        if (dataFacetDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataFacetDo.RESOURCE_SYMBOL,
                    dataFacetUid));
        }
        List<DataFieldDo> dataFieldDoList = this.dataFieldRepository.findByDataFacetUid(dataFacetUid);
        if (CollectionUtils.isEmpty(dataFieldDoList)) {
            throw new AbcResourceNotFoundException(String.format("%s::data_facet_uid=%d, no data field",
                    DataFieldDo.RESOURCE_SYMBOL,
                    dataFacetUid));
        }
        Map<String, DataFieldDo> dataFieldDoMap = new HashMap<>();
        dataFieldDoList.forEach(dataFieldDo -> {
            dataFieldDoMap.put(dataFieldDo.getName(), dataFieldDo);
        });

        // data facet appearance settings
        List<FilteringDataFieldDo> filteringDataFieldDoList =
                this.filteringDataFieldRepository.findByDataFacetUid(dataFacetUid, Sort.by(Sort.Order.asc("filteringSequence")));
        FilteringExtendedDo filteringExtendedDo = this.filteringExtendedRepository.findByDataFacetUid(dataFacetUid);
        List<ListingDataFieldDo> listingDataFieldDoList =
                this.listingDataFieldRepository.findByDataFacetUid(dataFacetUid,
                        Sort.by(Sort.Order.asc("listingSequence")));
        ListingExtendedDo listingExtendedDo = this.listingExtendedRepository.findByDataFacetUid(dataFacetUid);
        List<SortingDataFieldDo> sortingDataFieldDoList =
                this.sortingDataFieldRepository.findByDataFacetUid(dataFacetUid, Sort.by(Sort.Order.asc("sortingSequence")));
        ExportBasicDo exportBasicDo = this.exportBasicRepository.findByDataFacetUid(dataFacetUid);
        AdvancedFeatureDo advancedFeatureDo = this.advancedFeatureRepository.findByDataFacetUid(dataFacetUid);


        //
        // step 2, core-processing
        //

        TableDataWidgetServeCharacteristics serveCharacteristics = new TableDataWidgetServeCharacteristics();

        //
        // service status
        //
        TableDataWidgetServeCharacteristics.DataFacetServiceStatus serviceStatus =
                new TableDataWidgetServeCharacteristics.DataFacetServiceStatus();
        serviceStatus.setActive(Boolean.TRUE);
        serviceStatus.setMessage(null);
        serveCharacteristics.setServiceStatus(serviceStatus);

        if (advancedFeatureDo != null
                && advancedFeatureDo.getContent() != null
                && Boolean.TRUE.equals(advancedFeatureDo.getContent().getEnabledMaintenanceWindow())
                && !CollectionUtils.isEmpty(advancedFeatureDo.getContent().getMaintenanceWindowList())) {
            advancedFeatureDo.getContent().getMaintenanceWindowList().forEach(maintenanceWindow -> {
                if (ObjectUtils.isEmpty(maintenanceWindow.getCronExpression())) {
                    return;
                }

                CronExpression cronExpression = null;
                try {
                    cronExpression = CronExpression.parse(maintenanceWindow.getCronExpression());
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime next1 = cronExpression.next(now);
                    LocalDateTime next2 = cronExpression.next(next1.plusSeconds(1));
                    long gapInSeconds = ChronoUnit.SECONDS.between(next1, next2);
                    LocalDateTime previous = next1.minusSeconds(gapInSeconds);
                    LocalDateTime begin = previous;
                    LocalDateTime end = previous.plusMinutes(maintenanceWindow.getDurationInMinutes());
                    if (now.isAfter(begin) && now.isBefore(end)) {
                        serviceStatus.setActive(Boolean.FALSE);
                        serviceStatus.setMessage(maintenanceWindow.getMessage()
                                + " Maintenance window: [" + begin.format(dateTimeFormatter) + " - " + end.format(dateTimeFormatter) + "]");
                    }
                } catch (Exception e) {
                    throw new AbcIllegalParameterException("illegal cron expression");
                }
            });
        }

        //
        // header
        //
        if (advancedFeatureDo != null
                && advancedFeatureDo.getContent() != null
                && Boolean.TRUE.equals(advancedFeatureDo.getContent().getEnabledHeader())
                && !ObjectUtils.isEmpty(dataFacetDo.getDescription())) {
            serveCharacteristics.setHeader(dataFacetDo.getDescription());
        }

        //
        // filtering
        //
        TableDataWidgetServeCharacteristics.Filtering filtering = new TableDataWidgetServeCharacteristics.Filtering();
        if (!CollectionUtils.isEmpty(filteringDataFieldDoList)) {
            filtering.setFields(new LinkedList<>());

            //
            // 准备工作，处理 cascading filter 关系，每个 cascading filter 关系（字段组合）拼凑成一个过滤元素（字段）
            //
            // 每个参与 cascading filter 关系（字段组合）的第1级 (level 0) 字段
            List<String> cascadingLevel0FieldNameList = new LinkedList<>();
            // 每个参与 cascading 关系（字段组合）的字段 和 所属 cascading filter 关系的对应
            Map<String, TableDataWidgetServeCharacteristics.CascadingFilter> cascadingFilterMap = new HashMap();
            filteringDataFieldDoList.forEach(filteringDataFieldDo -> {
                switch (filteringDataFieldDo.getFilteringType()) {
                    case CASCADING_DROP_DOWN_LIST_SINGLE:
                    case CASCADING_DROP_DOWN_LIST_MULTIPLE: {
                        // 参与同一个 cascading 关系的所有字段，携带的有关 cascading 关系配置是一样的
                        if (cascadingFilterMap.containsKey(filteringDataFieldDo.getFieldName())) {
                            return;
                        }
                        if (filteringDataFieldDo.getFilteringTypeExtension() != null
                                && !filteringDataFieldDo.getFilteringTypeExtension().isEmpty()) {
                            FilteringFieldCascadingSettings filteringFieldCascadingSettings =
                                    JSONObject.toJavaObject(filteringDataFieldDo.getFilteringTypeExtension(),
                                            FilteringFieldCascadingSettings.class);
                            if (!ObjectUtils.isEmpty(filteringFieldCascadingSettings.getFilterName())
                                    && !ObjectUtils.isEmpty(filteringFieldCascadingSettings.getFilterLabel())
                                    && !CollectionUtils.isEmpty(filteringFieldCascadingSettings.getFields())
                                    && filteringFieldCascadingSettings.getDictionaryCategoryUid() != null) {
                                // 有效的配置，继续
                                TableDataWidgetServeCharacteristics.CascadingFilter cascadingFilter =
                                        new TableDataWidgetServeCharacteristics.CascadingFilter();

                                cascadingFilter.setFilterName(filteringFieldCascadingSettings.getFilterName());
                                cascadingFilter.setFilterLabel(filteringFieldCascadingSettings.getFilterLabel());
                                cascadingFilter.setFilterDescription(filteringFieldCascadingSettings.getFilterDescription());
                                cascadingFilter.setDictionaryCategoryUid(filteringFieldCascadingSettings.getDictionaryCategoryUid());

                                TableDataWidgetServeCharacteristics.CascadingField level0CascadingField = null;
                                TableDataWidgetServeCharacteristics.CascadingField parentCascadingField = null;
                                // 把 cascading 关系中的字段组合按从第1级到最末尾1级的顺序组织
                                for (String fieldName : filteringFieldCascadingSettings.getFields()) {
                                    if (!dataFieldDoMap.containsKey(fieldName)) {
                                        LOGGER.error("mismatched data found, do not exist field name {}, cascading " +
                                                        "filter {} in the data facet {}",
                                                fieldName, filteringFieldCascadingSettings.getFilterName(), dataFacetUid);
                                        throw new AbcResourceConflictException("mismatched data found");
                                    }

                                    TableDataWidgetServeCharacteristics.CascadingField cascadingField =
                                            new TableDataWidgetServeCharacteristics.CascadingField();
                                    cascadingField.setFieldName(fieldName);
                                    cascadingField.setFieldLabel(dataFieldDoMap.get(fieldName).getLabel());
                                    cascadingField.setFieldDescription(dataFieldDoMap.get(fieldName).getDescription());

                                    if (level0CascadingField == null) {
                                        level0CascadingField = cascadingField;
                                        cascadingFilter.setCascadingField(level0CascadingField);
                                        cascadingLevel0FieldNameList.add(fieldName);
                                    }

                                    if (parentCascadingField == null) {
                                        parentCascadingField = cascadingField;
                                    } else {
                                        parentCascadingField.setChild(cascadingField);
                                        parentCascadingField = cascadingField;
                                    }

                                    // 每个字段都找到了所属 cascading 根节点
                                    cascadingFilterMap.put(fieldName, cascadingFilter);
                                }
                            }
                        }
                    }
                    break;
                    default:
                        break;
                }
            });

            //
            // 构造 filtering fields
            //
            filteringDataFieldDoList.forEach(filteringDataFieldDo -> {
                switch (filteringDataFieldDo.getFilteringType()) {
                    case CASCADING_DROP_DOWN_LIST_SINGLE:
                    case CASCADING_DROP_DOWN_LIST_MULTIPLE: {
                        // 只看参与 cascading 级联关系的第1级字段，以它为代表，不搞重复了。
                        if (cascadingLevel0FieldNameList.contains(filteringDataFieldDo.getFieldName())) {
                            TableDataWidgetServeCharacteristics.CascadingFilter cascadingFilter =
                                    cascadingFilterMap.get(filteringDataFieldDo.getFieldName());

                            TableDataWidgetServeCharacteristics.FilteringField filteringField =
                                    new TableDataWidgetServeCharacteristics.FilteringField();

                            filteringField.setFieldName(cascadingFilter.getFilterName());
                            filteringField.setFieldLabel(cascadingFilter.getFilterLabel());
                            filteringField.setFieldDescription(cascadingFilter.getFilterDescription());
                            filteringField.setFilteringType(filteringDataFieldDo.getFilteringType());
                            filteringField.setFilteringSequence(filteringDataFieldDo.getFilteringSequence());

                            // default values
                            if (filteringDataFieldDo.getDefaultValueSettings() != null
                                    && !filteringDataFieldDo.getDefaultValueSettings().isEmpty()) {
                                FilteringFieldDefaultValueSettings filteringFieldDefaultValueSettings =
                                        JSONObject.toJavaObject(filteringDataFieldDo.getDefaultValueSettings(),
                                                FilteringFieldDefaultValueSettings.class);
                                if (filteringFieldDefaultValueSettings.getDictionaryCategoryUid() != null) {
                                    List<TreeNode> defaultValueTreeNodeList =
                                            this.dictionaryService.treeListingAllNodesOfDictionaryContentHierarchy(
                                                    filteringFieldDefaultValueSettings.getDictionaryCategoryUid(), null);
                                    if (!CollectionUtils.isEmpty(defaultValueTreeNodeList)) {
                                        List<ValueLabelPair> defaultValueList = new LinkedList<>();

                                        // transform value in the hierarchical tree node to flat
                                        List<List<Object>> rows = new LinkedList<>();
                                        List<Object> parentColumns = new LinkedList<>();
                                        AbcTreeNodeUtils.transformTagValueFromHierarchyToFlat(rows, parentColumns, defaultValueTreeNodeList);
                                        for (List<Object> row : rows) {
                                            StringBuilder sb = new StringBuilder();
                                            for (Object column : row) {
                                                if (sb.length() > 0) {
                                                    sb.append(">");
                                                }
                                                if (column == null) {
                                                    sb.append("NULL");
                                                } else {
                                                    sb.append(column);
                                                }
                                            }

                                            ValueLabelPair defaultValue = new ValueLabelPair();
                                            defaultValue.setValue(sb.toString());
                                            defaultValue.setLabel(String.valueOf(row.get(row.size() - 1)));

                                            defaultValueList.add(defaultValue);
                                        }

                                        filteringField.setFieldDefaultValues(defaultValueList);
                                    }
                                }
                            }

                            // optional values
                            filteringField.setCascadingDictionaryCategoryUid(cascadingFilter.getDictionaryCategoryUid());

                            // cascading
                            filteringField.setCascadingField(cascadingFilter.getCascadingField());

                            filtering.getFields().add(filteringField);
                        }
                    }
                    break;
                    case DROP_DOWN_LIST_SINGLE:
                    case DROP_DOWN_LIST_MULTIPLE:
                    case ASSOCIATING_SINGLE:
                    case ASSOCIATING_MULTIPLE: {
                        TableDataWidgetServeCharacteristics.FilteringField filteringField =
                                new TableDataWidgetServeCharacteristics.FilteringField();

                        filteringField.setFieldName(filteringDataFieldDo.getFieldName());
                        filteringField.setFieldLabel(dataFieldDoMap.get(filteringDataFieldDo.getFieldName()).getLabel());
                        filteringField.setFieldDescription(dataFieldDoMap.get(filteringDataFieldDo.getFieldName()).getDescription());
                        filteringField.setFilteringType(filteringDataFieldDo.getFilteringType());
                        filteringField.setFilteringSequence(filteringDataFieldDo.getFilteringSequence());

                        // default values
                        if (filteringDataFieldDo.getDefaultValueSettings() != null
                                && !filteringDataFieldDo.getDefaultValueSettings().isEmpty()) {
                            FilteringFieldDefaultValueSettings filteringFieldDefaultValueSettings =
                                    JSONObject.toJavaObject(filteringDataFieldDo.getDefaultValueSettings(),
                                            FilteringFieldDefaultValueSettings.class);
                            if (filteringFieldDefaultValueSettings.getDictionaryCategoryUid() != null) {
                                List<TreeNode> defaultValueTreeNodeList =
                                        this.dictionaryService.treeListingAllNodesOfDictionaryContentHierarchy(
                                                filteringFieldDefaultValueSettings.getDictionaryCategoryUid(), null);
                                if (!CollectionUtils.isEmpty(defaultValueTreeNodeList)) {
                                    List<ValueLabelPair> defaultValueList = new LinkedList<>();
                                    defaultValueTreeNodeList.forEach(treeNode -> {
                                        ValueLabelPair defaultValue = new ValueLabelPair();
                                        defaultValue.setValue(treeNode.getTags().get("value"));
                                        defaultValue.setLabel((String) treeNode.getTags().get("label"));
                                        defaultValueList.add(defaultValue);
                                    });
                                    filteringField.setFieldDefaultValues(defaultValueList);
                                }
                            }
                        }

                        // optional values
                        if (filteringDataFieldDo.getFilteringTypeExtension() != null
                                && !filteringDataFieldDo.getFilteringTypeExtension().isEmpty()) {
                            FilteringFieldOptionalValueSettings filteringFieldOptionalValueSettings =
                                    JSONObject.toJavaObject(filteringDataFieldDo.getFilteringTypeExtension(),
                                            FilteringFieldOptionalValueSettings.class);
                            List<TreeNode> optionalValueTreeNodeList =
                                    this.dictionaryService.treeListingAllNodesOfDictionaryContentHierarchy(filteringFieldOptionalValueSettings.getDictionaryCategoryUid(), null);
                            if (!CollectionUtils.isEmpty(optionalValueTreeNodeList)) {
                                List<ValueLabelPair> optionalValues = new LinkedList<>();
                                optionalValueTreeNodeList.forEach(treeNode -> {
                                    ValueLabelPair optionalValue = new ValueLabelPair();
                                    optionalValue.setValue(treeNode.getTags().get("value"));
                                    optionalValue.setLabel((String) treeNode.getTags().get("label"));
                                    optionalValues.add(optionalValue);
                                });
                                filteringField.setFieldOptionalValues(optionalValues);
                            }
                        }

                        filtering.getFields().add(filteringField);
                    }
                    break;
                    default: {
                        TableDataWidgetServeCharacteristics.FilteringField filteringField =
                                new TableDataWidgetServeCharacteristics.FilteringField();

                        filteringField.setFieldName(filteringDataFieldDo.getFieldName());
                        filteringField.setFieldLabel(dataFieldDoMap.get(filteringDataFieldDo.getFieldName()).getLabel());
                        filteringField.setFieldDescription(dataFieldDoMap.get(filteringDataFieldDo.getFieldName()).getDescription());
                        filteringField.setFilteringType(filteringDataFieldDo.getFilteringType());
                        filteringField.setFilteringSequence(filteringDataFieldDo.getFilteringSequence());

                        // default values
                        if (filteringDataFieldDo.getDefaultValueSettings() != null
                                && !filteringDataFieldDo.getDefaultValueSettings().isEmpty()) {
                            FilteringFieldDefaultValueSettings filteringFieldDefaultValueSettings =
                                    JSONObject.toJavaObject(filteringDataFieldDo.getDefaultValueSettings(),
                                            FilteringFieldDefaultValueSettings.class);
                            if (filteringFieldDefaultValueSettings.getDictionaryCategoryUid() != null) {
                                List<TreeNode> defaultValueTreeNodeList =
                                        this.dictionaryService.treeListingAllNodesOfDictionaryContentHierarchy(
                                                filteringFieldDefaultValueSettings.getDictionaryCategoryUid(), null);
                                if (!CollectionUtils.isEmpty(defaultValueTreeNodeList)) {
                                    List<ValueLabelPair> defaultValueList = new LinkedList<>();
                                    defaultValueTreeNodeList.forEach(treeNode -> {
                                        ValueLabelPair defaultValue = new ValueLabelPair();
                                        defaultValue.setValue(treeNode.getTags().get("value"));
                                        defaultValue.setLabel((String) treeNode.getTags().get("label"));
                                        defaultValueList.add(defaultValue);
                                    });
                                    filteringField.setFieldDefaultValues(defaultValueList);
                                }
                            }
                        }

                        filtering.getFields().add(filteringField);
                    }
                    break;
                }

            });
        }

        filtering.setExtended(new TableDataWidgetServeCharacteristics.FilteringExtended());
        if (filteringExtendedDo != null) {
            filtering.getExtended().setEnabledDefaultQuery(filteringExtendedDo.getEnabledDefaultQuery());
            filtering.getExtended().setEnabledFilterFolding(filteringExtendedDo.getEnabledFilterFolding());
        }

        serveCharacteristics.setFiltering(filtering);

        //
        // listing
        //
        TableDataWidgetServeCharacteristics.Listing listing = new TableDataWidgetServeCharacteristics.Listing();
        if (!CollectionUtils.isEmpty(listingDataFieldDoList)) {
            listing.setFields(new LinkedList<>());
            listingDataFieldDoList.forEach(listingDataFieldDo -> {
                TableDataWidgetServeCharacteristics.ListingField listingField =
                        new TableDataWidgetServeCharacteristics.ListingField();
                listing.getFields().add(listingField);

                listingField.setFieldName(listingDataFieldDo.getFieldName());
                listingField.setFieldLabel(dataFieldDoMap.get(listingDataFieldDo.getFieldName()).getLabel());
                listingField.setFieldDescription(dataFieldDoMap.get(listingDataFieldDo.getFieldName()).getDescription());
                listingField.setListingSequence(listingDataFieldDo.getListingSequence());
                listingField.setWidth(listingDataFieldDo.getWidth());

                // image & file handling
                switch (dataFieldDoMap.get(listingDataFieldDo.getFieldName()).getType()) {
                    case IMAGE: {
                        if (listingDataFieldDo.getExtension() != null
                                && !listingDataFieldDo.getExtension().isEmpty()) {
                            ListingFieldTypeExtensionImage listingFieldTypeExtensionImage =
                                    JSONObject.toJavaObject(listingDataFieldDo.getExtension(),
                                            ListingFieldTypeExtensionImage.class);

                            if (Boolean.TRUE.equals(listingFieldTypeExtensionImage.getEnabledImagePreview())) {
                                TableDataWidgetServeCharacteristics.ImagePreview imagePreview =
                                        new TableDataWidgetServeCharacteristics.ImagePreview();
                                listingField.setImagePreview(imagePreview);

                                JSONObject typeExtension =
                                        dataFieldDoMap.get(listingDataFieldDo.getFieldName()).getTypeExtension();
                                if (typeExtension != null
                                        && !typeExtension.isEmpty()) {
                                    FieldTypeExtensionFile fieldTypeExtensionFile =
                                            JSONObject.toJavaObject(typeExtension, FieldTypeExtensionFile.class);
                                    BeanUtils.copyProperties(fieldTypeExtensionFile, imagePreview);
                                }

                                imagePreview.setEnabled(listingFieldTypeExtensionImage.getEnabledImagePreview());
                            }
                        }
                    }
                    break;
                    case FILE: {
                        if (listingDataFieldDo.getExtension() != null
                                && !listingDataFieldDo.getExtension().isEmpty()) {
                            ListingFieldTypeExtensionFile listingFieldTypeExtensionFile =
                                    JSONObject.toJavaObject(listingDataFieldDo.getExtension(),
                                            ListingFieldTypeExtensionFile.class);

                            if (Boolean.TRUE.equals(listingFieldTypeExtensionFile.getEnabledFileDownload())) {
                                TableDataWidgetServeCharacteristics.FileDownload fileDownload =
                                        new TableDataWidgetServeCharacteristics.FileDownload();
                                listingField.setFileDownload(fileDownload);

                                JSONObject typeExtension =
                                        dataFieldDoMap.get(listingDataFieldDo.getFieldName()).getTypeExtension();
                                if (typeExtension != null
                                        && !typeExtension.isEmpty()) {
                                    FieldTypeExtensionFile fieldTypeExtensionFile =
                                            JSONObject.toJavaObject(typeExtension, FieldTypeExtensionFile.class);
                                    BeanUtils.copyProperties(fieldTypeExtensionFile, fileDownload);
                                }

                                fileDownload.setEnabled(listingFieldTypeExtensionFile.getEnabledFileDownload());
                            }
                        }
                    }
                    break;
                    default:
                        break;
                }
            });
        }

        listing.setExtended(new TableDataWidgetServeCharacteristics.ListingExtended());
        if (listingExtendedDo != null) {
            listing.getExtended().setEnabledPagination(true);
            listing.getExtended().setDefaultPageSize(listingExtendedDo.getDefaultPageSize());
            listing.getExtended().setEnabledColumnNo(listingExtendedDo.getEnabledColumnNo());
            listing.getExtended().setEnabledVerticalScrolling(listingExtendedDo.getEnabledVerticalScrolling());
            listing.getExtended().setVerticalScrollingHeightThreshold(listingExtendedDo.getVerticalScrollingHeightThreshold());
            listing.getExtended().setEnabledFreezeTopRows(listingExtendedDo.getEnabledFreezeTopRows());
            listing.getExtended().setInclusiveTopRows(listingExtendedDo.getInclusiveTopRows());
            listing.getExtended().setEnabledFreezeLeftColumns(listingExtendedDo.getEnabledFreezeLeftColumns());
            listing.getExtended().setInclusiveLeftColumns(listingExtendedDo.getInclusiveLeftColumns());
            listing.getExtended().setEnabledFreezeRightColumns(listingExtendedDo.getEnabledFreezeRightColumns());
            listing.getExtended().setInclusiveRightColumns(listingExtendedDo.getInclusiveRightColumns());

        }

        serveCharacteristics.setListing(listing);

        //
        // sorting
        //
        TableDataWidgetServeCharacteristics.Sorting sorting = new TableDataWidgetServeCharacteristics.Sorting();
        if (!CollectionUtils.isEmpty(sortingDataFieldDoList)) {
            sorting.setFields(new LinkedList<>());
            sortingDataFieldDoList.forEach(sortingDataFieldDo -> {
                TableDataWidgetServeCharacteristics.SortingField sortingField =
                        new TableDataWidgetServeCharacteristics.SortingField();
                sortingField.setFieldName(sortingDataFieldDo.getFieldName());
                sortingField.setFieldLabel(dataFieldDoMap.get(sortingDataFieldDo.getFieldName()).getLabel());
                sortingField.setFieldDescription(dataFieldDoMap.get(sortingDataFieldDo.getFieldName()).getDescription());
                sortingField.setSortingSequence(sortingDataFieldDo.getSortingSequence());
                sortingField.setDirection(sortingDataFieldDo.getDirection());
                sorting.getFields().add(sortingField);
            });
        }
        serveCharacteristics.setSorting(sorting);

        //
        // exporting
        //
        TableDataWidgetServeCharacteristics.Exporting exporting = new TableDataWidgetServeCharacteristics.Exporting();
        if (exportBasicDo != null) {
            exporting.setEnabledExportCsv(exportBasicDo.getEnabledExportCsv());
            exporting.setEnabledExportExcel(exportBasicDo.getEnabledExportExcel());

            boolean containDataFieldDoWithAttachment = false;
            for (DataFieldDo dataFieldDo : dataFieldDoList) {
                if (DataFieldTypeEnum.IMAGE.equals(dataFieldDo.getType())
                        || DataFieldTypeEnum.FILE.equals(dataFieldDo.getType())) {
                    JSONObject typeExtension = dataFieldDo.getTypeExtension();
                    if (typeExtension != null && !typeExtension.isEmpty()) {
                        containDataFieldDoWithAttachment = true;
                        break;
                    }
                }
            }
            if (Boolean.TRUE.equals(exportBasicDo.getEnabledExportCsv())
                    && containDataFieldDoWithAttachment
                    && (Boolean.TRUE.equals(exportBasicDo.getEnabledExportDataAndFiles())
                    || Boolean.TRUE.equals(exportBasicDo.getEnabledExportDataAndImages()))) {
                exporting.setEnabledExportCsvWithAttachments(Boolean.TRUE);
            }

            if (Boolean.TRUE.equals(exportBasicDo.getEnabledExportExcel())
                    && containDataFieldDoWithAttachment
                    && (Boolean.TRUE.equals(exportBasicDo.getEnabledExportDataAndFiles())
                    || Boolean.TRUE.equals(exportBasicDo.getEnabledExportDataAndImages()))) {
                exporting.setEnabledExportExcelWithAttachments(Boolean.TRUE);
            }

            exporting.setEnabledExportAsTemplates(exportBasicDo.getEnabledExportAsTemplates());
        }
        serveCharacteristics.setExporting(exporting);

        return serveCharacteristics;
    }
}
