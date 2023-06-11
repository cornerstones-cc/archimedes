package cc.cornerstones.biz.serve.service.assembly;

import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.*;
import cc.cornerstones.biz.datafacet.dto.DataFacetQueryDto;
import cc.cornerstones.biz.datafacet.entity.FilteringDataFieldDo;
import cc.cornerstones.biz.datafacet.entity.ListingDataFieldDo;
import cc.cornerstones.biz.datafacet.entity.ListingExtendedDo;
import cc.cornerstones.biz.datafacet.entity.SortingDataFieldDo;
import cc.cornerstones.biz.datafacet.persistence.FilteringDataFieldRepository;
import cc.cornerstones.biz.datafacet.persistence.ListingDataFieldRepository;
import cc.cornerstones.biz.datafacet.persistence.ListingExtendedRepository;
import cc.cornerstones.biz.datafacet.persistence.SortingDataFieldRepository;
import cc.cornerstones.biz.datafacet.service.inf.ExecuteDataFacetService;
import cc.cornerstones.biz.serve.dto.FlexibleQueryRequestDto;
import cc.cornerstones.biz.share.constants.FilteringTypeEnum;
import cc.cornerstones.biz.share.constants.SelectionFieldTypeEnum;
import cc.cornerstones.biz.share.types.PlainSelectionField;
import cc.cornerstones.biz.share.types.QueryContentResult;
import cc.cornerstones.biz.share.types.SelectionField;
import cc.cornerstones.biz.share.types.StatementFilter;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Component
public class FlexibleQueryHandler {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(FlexibleQueryHandler.class);

    @Autowired
    private ExecuteDataFacetService executeDataFacetService;

    @Autowired
    private FilteringDataFieldRepository filteringDataFieldRepository;

    @Autowired
    private ListingDataFieldRepository listingDataFieldRepository;

    @Autowired
    private SortingDataFieldRepository sortingDataFieldRepository;

    @Autowired
    private ListingExtendedRepository listingExtendedRepository;

    /**
     * Query content of the specified Data Facet
     *
     * @param dataFacetUid
     * @param flexibleQueryRequestDto
     * @param operatingUserProfile
     * @return
     * @throws AbcUndefinedException
     */
    public QueryContentResult execute(
            Long dataFacetUid,
            FlexibleQueryRequestDto flexibleQueryRequestDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, 获取 data facet 的 appearance (filtering & listing & sorting) 配置
        //
        List<FilteringDataFieldDo> filteringDataFieldDoList =
                this.filteringDataFieldRepository.findByDataFacetUid(dataFacetUid, Sort.by(Sort.Order.asc(BaseDo.ID_FIELD_NAME)));
        List<ListingDataFieldDo> listingDataFieldDoList =
                this.listingDataFieldRepository.findByDataFacetUid(dataFacetUid, Sort.by(Sort.Order.asc("listingSequence")));
        List<SortingDataFieldDo> sortingDataFieldDoList =
                this.sortingDataFieldRepository.findByDataFacetUid(dataFacetUid, Sort.by(Sort.Order.asc(BaseDo.ID_FIELD_NAME)));
        ListingExtendedDo listingExtendedDo = this.listingExtendedRepository.findByDataFacetUid(dataFacetUid);

        // 构造 available filtering field names & filtering type
        Map<String, FilteringTypeEnum> availableFilteringFieldNameAndFilteringTypeMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(filteringDataFieldDoList)) {
            filteringDataFieldDoList.forEach(filteringField -> {
                availableFilteringFieldNameAndFilteringTypeMap.put(filteringField.getFieldName(), filteringField.getFilteringType());
            });
        }

        // 构造 available listing field names
        List<String> availableListingFieldNames = new LinkedList<>();
        if (!CollectionUtils.isEmpty(listingDataFieldDoList)) {
            listingDataFieldDoList.forEach(listingField -> {
                availableListingFieldNames.add(listingField.getFieldName());
            });
        }

        // 构造 available sorting field names & sort direction
        List<String> availableSortingFieldNames = new LinkedList<>();
        List<AbcOrder> orders = new LinkedList<>();
        if (!CollectionUtils.isEmpty(sortingDataFieldDoList)) {
            sortingDataFieldDoList.forEach(sortingField -> {
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

        // 构造 更多配置
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
        if (flexibleQueryRequestDto == null) {
            // default query

            // selection fields
            dataFacetQueryDto.setSelectionFields(new LinkedList<>());
            availableListingFieldNames.forEach(fieldName -> {
                SelectionField selectionField = new SelectionField();
                selectionField.setType(SelectionFieldTypeEnum.PLAIN);

                PlainSelectionField plainSelectionField = new PlainSelectionField();
                plainSelectionField.setFieldName(fieldName);

                selectionField.setContent((JSONObject) JSONObject.toJSON(plainSelectionField));

                dataFacetQueryDto.getSelectionFields().add(selectionField);
            });

            // sort fields
            dataFacetQueryDto.setSort(sort);

            // pagination fields
            dataFacetQueryDto.setPagination(new AbcPagination(0, pageSize));
        } else {
            // custom query

            // selection fields
            dataFacetQueryDto.setSelectionFields(new LinkedList<>());
            if (CollectionUtils.isEmpty(flexibleQueryRequestDto.getSelectionFieldNames())) {
                availableListingFieldNames.forEach(fieldName -> {
                    SelectionField selectionField = new SelectionField();
                    selectionField.setType(SelectionFieldTypeEnum.PLAIN);

                    PlainSelectionField plainSelectionField = new PlainSelectionField();
                    plainSelectionField.setFieldName(fieldName);

                    selectionField.setContent((JSONObject) JSONObject.toJSON(plainSelectionField));

                    dataFacetQueryDto.getSelectionFields().add(selectionField);
                });
            } else {
                // 请求的 field names 和 available fields names 取交集
                flexibleQueryRequestDto.getSelectionFieldNames().retainAll(availableListingFieldNames);
                if (CollectionUtils.isEmpty(flexibleQueryRequestDto.getSelectionFieldNames())) {
                    // 交集结果为空，即，请求的 result field names 都不存在或者不被允许访问
                    throw new AbcIllegalParameterException(String.format("illegal expected result field names"));
                }

                flexibleQueryRequestDto.getSelectionFieldNames().forEach(fieldName -> {
                    SelectionField selectionField = new SelectionField();
                    selectionField.setType(SelectionFieldTypeEnum.PLAIN);

                    PlainSelectionField plainSelectionField = new PlainSelectionField();
                    plainSelectionField.setFieldName(fieldName);

                    selectionField.setContent((JSONObject) JSONObject.toJSON(plainSelectionField));

                    dataFacetQueryDto.getSelectionFields().add(selectionField);
                });
            }

            // statement filters
            dataFacetQueryDto.setStatementFilter(new StatementFilter());
            dataFacetQueryDto.getStatementFilter().setStatement(flexibleQueryRequestDto.getFilter());

            // sort
            if (flexibleQueryRequestDto.getSort() == null
                    || CollectionUtils.isEmpty(flexibleQueryRequestDto.getSort().getOrders())) {
                dataFacetQueryDto.setSort(sort);
            } else {
                // 验证每个字段是否被允许作为 Order，并且按 Order 字段顺序重新排列
                List<AbcOrder> transformedOrders = new LinkedList<>();

                // 先找出被允许作为 Order 的
                Map<String, AbcOrder> expectedOrders = new HashMap<>();
                flexibleQueryRequestDto.getSort().getOrders().forEach(order -> {
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
            if (flexibleQueryRequestDto.getPagination() != null
                    && flexibleQueryRequestDto.getPagination().getPage() >= 0
                    && flexibleQueryRequestDto.getPagination().getSize() > 0) {
                if (flexibleQueryRequestDto.getPagination().getSize() > 1000) {
                    flexibleQueryRequestDto.getPagination().setSize(1000);
                }
                dataFacetQueryDto.setPagination(flexibleQueryRequestDto.getPagination());
            } else {
                dataFacetQueryDto.setPagination(new AbcPagination(0, pageSize));
            }

        }

        return this.executeDataFacetService.queryContent(dataFacetUid, dataFacetQueryDto, operatingUserProfile);
    }
}
