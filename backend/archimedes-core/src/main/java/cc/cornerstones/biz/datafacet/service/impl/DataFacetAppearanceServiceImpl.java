package cc.cornerstones.biz.datafacet.service.impl;

import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.datafacet.dto.*;
import cc.cornerstones.biz.datafacet.entity.*;
import cc.cornerstones.biz.datafacet.persistence.*;
import cc.cornerstones.biz.datafacet.service.inf.DataFacetAppearanceService;
import cc.cornerstones.biz.datafacet.share.constants.DataFieldTypeEnum;
import cc.cornerstones.biz.datafacet.share.types.*;
import cc.cornerstones.biz.datatable.persistence.DataIndexRepository;
import cc.cornerstones.biz.share.assembly.ResourceReferenceManager;
import cc.cornerstones.biz.share.constants.FilteringTypeEnum;
import cc.cornerstones.biz.share.constants.NamingPolicyEnum;
import cc.cornerstones.biz.share.event.*;
import cc.cornerstones.biz.share.types.NamingPolicyExtCombine;
import cc.cornerstones.biz.share.types.NamingPolicyExtCombineField;
import cc.cornerstones.biz.share.types.ResourceReferenceHandler;
import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSONObject;
import com.google.common.eventbus.Subscribe;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DataFacetAppearanceServiceImpl implements DataFacetAppearanceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataFacetAppearanceServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

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
    private DataFieldRepository dataFieldRepository;

    @Autowired
    private DataIndexRepository dataIndexRepository;

    @Autowired
    private DataFacetRepository dataFacetRepository;

    @Value("${private.dir.general.project.download}")
    private String projectDownloadPath;

    @Value("${private.dir.general.project.upload}")
    private String projectUploadPath;

    @Override
    public List<FilteringDataFieldAnotherDto> listingQueryFilteringDataFieldsOfDataFacet(
            Long dataFacetUid,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Specification<FilteringDataFieldDo> specification = new Specification<FilteringDataFieldDo>() {
            @Override
            public Predicate toPredicate(Root<FilteringDataFieldDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (dataFacetUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("dataFacetUid"), dataFacetUid));
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        if (sort == null || sort.isUnsorted()) {
            sort = Sort.by(Sort.Order.asc("filteringSequence"));
        }
        List<FilteringDataFieldDo> itemDoList = this.filteringDataFieldRepository.findAll(specification, sort);
        if (CollectionUtils.isEmpty(itemDoList)) {
            return null;
        }
        List<FilteringDataFieldAnotherDto> content = new LinkedList<>();
        itemDoList.forEach(itemDo -> {
            FilteringDataFieldAnotherDto itemDto = new FilteringDataFieldAnotherDto();
            BeanUtils.copyProperties(itemDo, itemDto);

            //
            // 避开 JSONObject 作为对象字段时不能配置 SNAKE_CASE 格式输出问题
            //
            if (itemDo.getFilteringTypeExtension() != null) {
                switch (itemDo.getFilteringType()) {
                    case DROP_DOWN_LIST_SINGLE:
                    case DROP_DOWN_LIST_MULTIPLE:
                    case ASSOCIATING_SINGLE:
                    case ASSOCIATING_MULTIPLE: {
                        FilteringFieldOptionalValueSettings filteringFieldOptionalValueSettings =
                                JSONObject.toJavaObject(itemDo.getFilteringTypeExtension(),
                                        FilteringFieldOptionalValueSettings.class);
                        itemDto.setFilteringTypeExtension(filteringFieldOptionalValueSettings);
                    }
                    break;
                    case CASCADING_DROP_DOWN_LIST_SINGLE:
                    case CASCADING_DROP_DOWN_LIST_MULTIPLE: {
                        FilteringFieldCascadingSettings cascadingFilteringSettings =
                                JSONObject.toJavaObject(itemDo.getFilteringTypeExtension(),
                                        FilteringFieldCascadingSettings.class);
                        itemDto.setFilteringTypeExtension(cascadingFilteringSettings);
                    }
                    break;
                    default:
                        break;
                }
            }

            if (itemDo.getDefaultValueSettings() != null) {
                FilteringFieldDefaultValueSettings filteringFieldDefaultValueSettings =
                        JSONObject.toJavaObject(itemDo.getDefaultValueSettings(),
                                FilteringFieldDefaultValueSettings.class);
                itemDto.setDefaultValueSettings(filteringFieldDefaultValueSettings);
            }

            content.add(itemDto);
        });
        return content;
    }

    @Override
    public FilteringExtendedDto getFilteringExtendedOfDataFacet(
            Long dataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        FilteringExtendedDo filteringExtendedDo = this.filteringExtendedRepository.findByDataFacetUid(dataFacetUid);
        if (filteringExtendedDo == null) {
            return null;
        }

        FilteringExtendedDto filteringExtendedDto = new FilteringExtendedDto();
        BeanUtils.copyProperties(filteringExtendedDo, filteringExtendedDto);
        return filteringExtendedDto;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void replaceAllFilteringDataFieldsOfDataFacet(
            Long dataFacetUid,
            List<FilteringDataFieldDto> filteringDataFieldDtoList,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        if (CollectionUtils.isEmpty(filteringDataFieldDtoList)) {
            return;
        }

        DataFacetDo dataFacetDo = this.dataFacetRepository.findByUid(dataFacetUid);
        if (dataFacetDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataFacetDo.RESOURCE_SYMBOL,
                    dataFacetUid));
        }

        Map<String, DataFieldDo> availableDataFieldDoMap = new HashMap<>();
        List<DataFieldDo> availableDataFieldDoList = this.dataFieldRepository.findByDataFacetUid(dataFacetUid);
        if (CollectionUtils.isEmpty(availableDataFieldDoList)) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataFacetDo.RESOURCE_SYMBOL,
                    dataFacetUid));
        }
        availableDataFieldDoList.forEach(dataFieldDo -> {
            availableDataFieldDoMap.put(dataFieldDo.getName(), dataFieldDo);
        });

        // 一方面校验，另一方面分配 sequence
        float beginSequence = 1.0f;
        for (FilteringDataFieldDto filteringDataFieldDto : filteringDataFieldDtoList) {
            if (filteringDataFieldDto == null) {
                throw new AbcIllegalParameterException("at least one filtering data field is null");
            }
            if (ObjectUtils.isEmpty(filteringDataFieldDto.getFieldName())) {
                throw new AbcIllegalParameterException("at least one field name is null or empty");
            }
            if (!availableDataFieldDoMap.containsKey(filteringDataFieldDto.getFieldName())) {
                throw new AbcIllegalParameterException("at least one field name does not exist");
            }
            if (filteringDataFieldDto.getFilteringType() == null) {
                throw new AbcIllegalParameterException(String.format("field %s 's type should not be null",
                        filteringDataFieldDto.getFieldName()));
            }
            switch (filteringDataFieldDto.getFilteringType()) {
                case DROP_DOWN_LIST_SINGLE:
                case DROP_DOWN_LIST_MULTIPLE:
                case ASSOCIATING_SINGLE:
                case ASSOCIATING_MULTIPLE: {
                    if (filteringDataFieldDto.getFilteringTypeExtension() == null
                            || filteringDataFieldDto.getFilteringTypeExtension().isEmpty()) {
                        throw new AbcIllegalParameterException(String.format("field %s 's " +
                                        "filtering_type_extension should not be null if " +
                                        "filtering_type is %s", filteringDataFieldDto.getFieldName(),
                                filteringDataFieldDto.getFilteringType()));
                    }
                    FilteringFieldOptionalValueSettings filteringFieldOptionalValueSettings =
                            JSONObject.toJavaObject(filteringDataFieldDto.getFilteringTypeExtension(),
                                    FilteringFieldOptionalValueSettings.class);
                    if (filteringFieldOptionalValueSettings.getDictionaryCategoryUid() == null) {
                        throw new AbcIllegalParameterException(String.format("field %s 's " +
                                        "filtering_type_extension.dictionary_category_uid should not be null if " +
                                        "filtering_type is %s", filteringDataFieldDto.getFieldName(),
                                filteringDataFieldDto.getFilteringType()));
                    }
                }
                break;
                case CASCADING_DROP_DOWN_LIST_SINGLE:
                case CASCADING_DROP_DOWN_LIST_MULTIPLE: {
                    if (filteringDataFieldDto.getFilteringTypeExtension() == null
                            || filteringDataFieldDto.getFilteringTypeExtension().isEmpty()) {
                        throw new AbcIllegalParameterException(String.format("field %s 's " +
                                        "filtering_type_extension should not be null if " +
                                        "filtering_type is %s", filteringDataFieldDto.getFieldName(),
                                filteringDataFieldDto.getFilteringType()));
                    }
                    FilteringFieldCascadingSettings cascadingFilteringSettings =
                            JSONObject.toJavaObject(filteringDataFieldDto.getFilteringTypeExtension(),
                                    FilteringFieldCascadingSettings.class);
                    if (cascadingFilteringSettings.getDictionaryCategoryUid() == null) {
                        throw new AbcIllegalParameterException(String.format("field %s 's " +
                                        "filtering_type_extension.dictionary_category_uid should not be null if " +
                                        "filtering_type is %s", filteringDataFieldDto.getFieldName(),
                                filteringDataFieldDto.getFilteringType()));
                    }
                    if (CollectionUtils.isEmpty(cascadingFilteringSettings.getFields())) {
                        throw new AbcIllegalParameterException(String.format("field %s 's " +
                                        "filtering_type_extension.fields should not be null or empty if " +
                                        "filtering_type is %s", filteringDataFieldDto.getFieldName(),
                                filteringDataFieldDto.getFilteringType()));
                    }
                    if (ObjectUtils.isEmpty(cascadingFilteringSettings.getFilterName())) {
                        throw new AbcIllegalParameterException(String.format("field %s 's " +
                                        "filtering_type_extension.filter_name should not be null or empty if " +
                                        "filtering_type is %s", filteringDataFieldDto.getFieldName(),
                                filteringDataFieldDto.getFilteringType()));
                    }
                    if (ObjectUtils.isEmpty(cascadingFilteringSettings.getFilterLabel())) {
                        throw new AbcIllegalParameterException(String.format("field %s 's " +
                                        "filtering_type_extension.filter_label should not be null or empty if " +
                                        "filtering_type is %s", filteringDataFieldDto.getFieldName(),
                                filteringDataFieldDto.getFilteringType()));
                    }
                }
                break;
                default:
                    break;
            }

            filteringDataFieldDto.setFilteringSequence(beginSequence++);
        }

        List<FilteringDataFieldDo> existingItemDoList =
                this.filteringDataFieldRepository.findByDataFacetUid(dataFacetUid, Sort.by(Sort.Order.asc(BaseDo.ID_FIELD_NAME)));
        Map<String, FilteringDataFieldDo> existingItemDoMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(existingItemDoList)) {
            existingItemDoList.forEach(existingItemDo -> {
                existingItemDoMap.put(existingItemDo.getFieldName(), existingItemDo);
            });
        }

        List<FilteringDataFieldDto> inputItemList = filteringDataFieldDtoList;
        Map<String, FilteringDataFieldDto> inputItemMap = new HashMap();
        if (!CollectionUtils.isEmpty(inputItemList)) {
            for (int i = 0; i < inputItemList.size(); i++) {
                FilteringDataFieldDto inputItem = inputItemList.get(i);
                inputItemMap.put(inputItem.getFieldName(), inputItem);
            }
        }

        List<FilteringDataFieldDo> toAddItemDoList = new LinkedList<>();
        List<FilteringDataFieldDo> toUpdateItemDoList = new LinkedList<>();
        List<FilteringDataFieldDo> toDeleteItemDoList = new LinkedList<>();

        //
        // Step 2, core-processing
        //
        existingItemDoMap.forEach((key, existingItemDo) -> {
            if (inputItemMap.containsKey(key)) {
                // existing 有，input 有
                // 覆盖

                FilteringDataFieldDto inputItem = inputItemMap.get(key);
                existingItemDo.setFilteringType(inputItem.getFilteringType());
                existingItemDo.setFilteringTypeExtension(inputItem.getFilteringTypeExtension());
                existingItemDo.setDefaultValueSettings(inputItem.getDefaultValueSettings());
                existingItemDo.setFilteringSequence(inputItem.getFilteringSequence());

                BaseDo.update(existingItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                toUpdateItemDoList.add(existingItemDo);
            } else {
                // existing 有，input 没有
                // 删除
                existingItemDo.setDeleted(Boolean.TRUE);
                BaseDo.update(existingItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                toDeleteItemDoList.add(existingItemDo);
            }
        });

        inputItemMap.forEach((key, inputItem) -> {
            if (!existingItemDoMap.containsKey(key)) {
                // input 有，existing 没有
                // 新增
                FilteringDataFieldDo newItemDo = new FilteringDataFieldDo();

                newItemDo.setFieldName(inputItem.getFieldName());
                newItemDo.setFilteringType(inputItem.getFilteringType());
                newItemDo.setFilteringTypeExtension(inputItem.getFilteringTypeExtension());
                newItemDo.setDefaultValueSettings(inputItem.getDefaultValueSettings());
                newItemDo.setFilteringSequence(inputItem.getFilteringSequence());
                newItemDo.setDataFacetUid(dataFacetUid);
                BaseDo.create(newItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                toAddItemDoList.add(newItemDo);
            }
        });

        if (!CollectionUtils.isEmpty(toAddItemDoList)) {
            this.filteringDataFieldRepository.saveAll(toAddItemDoList);
        }
        if (!CollectionUtils.isEmpty(toUpdateItemDoList)) {
            this.filteringDataFieldRepository.saveAll(toUpdateItemDoList);
        }
        if (!CollectionUtils.isEmpty(toDeleteItemDoList)) {
            this.filteringDataFieldRepository.saveAll(toDeleteItemDoList);
        }

        //
        // Step 3, post-processing
        //

        // step 3.1, event post
        DataFacetChangedEvent dataFacetChangedEvent = new DataFacetChangedEvent();
        dataFacetChangedEvent.setDataFacetDo(dataFacetDo);
        dataFacetChangedEvent.setOperatingUserProfile(operatingUserProfile);
        this.eventBusManager.send(dataFacetChangedEvent);
    }

    @Override
    public void replaceFilteringExtendedOfDataFacet(
            Long dataFacetUid,
            FilteringExtendedDto filteringExtendedDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DataFacetDo dataFacetDo = this.dataFacetRepository.findByUid(dataFacetUid);
        if (dataFacetDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataFacetDo.RESOURCE_SYMBOL,
                    dataFacetUid));
        }

        FilteringExtendedDo filteringExtendedDo = this.filteringExtendedRepository.findByDataFacetUid(dataFacetUid);

        //
        // Step 2, core-processing
        //
        if (filteringExtendedDo == null) {
            filteringExtendedDo = new FilteringExtendedDo();
            filteringExtendedDo.setDataFacetUid(dataFacetUid);
            BaseDo.create(filteringExtendedDo, operatingUserProfile.getUid(), LocalDateTime.now());
        } else {
            BaseDo.update(filteringExtendedDo, operatingUserProfile.getUid(), LocalDateTime.now());
        }

        filteringExtendedDo.setEnabledDefaultQuery(filteringExtendedDto.getEnabledDefaultQuery());
        filteringExtendedDo.setEnabledFilterFolding(filteringExtendedDto.getEnabledFilterFolding());

        this.filteringExtendedRepository.save(filteringExtendedDo);

        //
        // Step 3, post-processing
        //

        // step 3.1, event post
        DataFacetChangedEvent dataFacetChangedEvent = new DataFacetChangedEvent();
        dataFacetChangedEvent.setDataFacetDo(dataFacetDo);
        dataFacetChangedEvent.setOperatingUserProfile(operatingUserProfile);
        this.eventBusManager.send(dataFacetChangedEvent);
    }

    @Override
    public List<ListingDataFieldAnotherDto> listingQueryListingDataFieldsOfDataFacet(
            Long dataFacetUid,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        List<DataFieldDo> dataFieldDoList = this.dataFieldRepository.findByDataFacetUid(dataFacetUid);
        if (CollectionUtils.isEmpty(dataFieldDoList)) {
            return null;
        }
        Map<String, DataFieldDo> dataFieldDoMap = new HashMap<>();
        dataFieldDoList.forEach(dataFieldDo -> {
            dataFieldDoMap.put(dataFieldDo.getName(), dataFieldDo);
        });

        Specification<ListingDataFieldDo> specification = new Specification<ListingDataFieldDo>() {
            @Override
            public Predicate toPredicate(Root<ListingDataFieldDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (dataFacetUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("dataFacetUid"), dataFacetUid));
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        if (sort == null || sort.isUnsorted()) {
            sort = Sort.by(Sort.Order.asc("listingSequence"));
        }
        List<ListingDataFieldDo> itemDoList = this.listingDataFieldRepository.findAll(specification, sort);
        if (CollectionUtils.isEmpty(itemDoList)) {
            return null;
        }
        List<ListingDataFieldAnotherDto> content = new LinkedList<>();
        itemDoList.forEach(itemDo -> {
            ListingDataFieldAnotherDto itemDto = new ListingDataFieldAnotherDto();
            BeanUtils.copyProperties(itemDo, itemDto);

            //
            // 避开 JSONObject 作为对象字段时不能配置 SNAKE_CASE 格式输出问题
            //
            DataFieldTypeEnum dataFieldType = dataFieldDoMap.get(itemDo.getFieldName()).getType();
            if (dataFieldType != null) {
                switch (dataFieldType) {
                    case FILE: {
                        ListingFieldTypeExtensionFile listingFieldTypeExtensionFile =
                                JSONObject.toJavaObject(itemDo.getExtension(),
                                        ListingFieldTypeExtensionFile.class);
                        itemDto.setExtension(listingFieldTypeExtensionFile);
                    }
                    break;
                    case IMAGE: {
                        ListingFieldTypeExtensionImage listingFieldTypeExtensionImage =
                                JSONObject.toJavaObject(itemDo.getExtension(),
                                        ListingFieldTypeExtensionImage.class);
                        itemDto.setExtension(listingFieldTypeExtensionImage);
                    }
                    break;
                    default:
                        break;
                }
            }

            content.add(itemDto);
        });
        return content;
    }

    @Override
    public ListingExtendedDto getListingExtendedOfDataFacet(
            Long dataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        ListingExtendedDo listingExtendedDo = this.listingExtendedRepository.findByDataFacetUid(dataFacetUid);
        if (listingExtendedDo == null) {
            return null;
        }

        ListingExtendedDto listingExtendedDto = new ListingExtendedDto();
        BeanUtils.copyProperties(listingExtendedDo, listingExtendedDto);
        return listingExtendedDto;
    }

    @Override
    public void replaceAllListingDataFieldsOfDataFacet(
            Long dataFacetUid,
            List<ListingDataFieldDto> listingDataFieldDtoList,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        if (CollectionUtils.isEmpty(listingDataFieldDtoList)) {
            return;
        }

        DataFacetDo dataFacetDo = this.dataFacetRepository.findByUid(dataFacetUid);
        if (dataFacetDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataFacetDo.RESOURCE_SYMBOL,
                    dataFacetUid));
        }

        List<DataFieldDo> dataFieldDoList = this.dataFieldRepository.findByDataFacetUid(dataFacetUid);
        if (CollectionUtils.isEmpty(dataFieldDoList)) {
            return;
        }
        Map<String, DataFieldDo> dataFieldDoMap = new HashMap<>();
        dataFieldDoList.forEach(dataFieldDo -> {
            dataFieldDoMap.put(dataFieldDo.getName(), dataFieldDo);
        });

        // 一方面校验，另一方面分配 sequence
        float beginSequence = 1.0f;
        for (ListingDataFieldDto listingDataFieldDto : listingDataFieldDtoList) {
            if (listingDataFieldDto == null) {
                throw new AbcIllegalParameterException("at least one listing data field is null");
            }
            if (ObjectUtils.isEmpty(listingDataFieldDto.getFieldName())) {
                throw new AbcIllegalParameterException("at least one field name is null or empty");
            }
            if (!dataFieldDoMap.containsKey(listingDataFieldDto.getFieldName())) {
                throw new AbcIllegalParameterException("at least one field name does not exist");
            }

            DataFieldTypeEnum dataFieldType = dataFieldDoMap.get(listingDataFieldDto.getFieldName()).getType();
            if (dataFieldType != null) {
                switch (dataFieldType) {
                    case FILE: {
                        if (listingDataFieldDto.getExtension() != null
                                && !listingDataFieldDto.getExtension().isEmpty()) {
                            ListingFieldTypeExtensionFile listingFieldTypeExtensionFile =
                                    JSONObject.toJavaObject(listingDataFieldDto.getExtension(),
                                            ListingFieldTypeExtensionFile.class);
                            // 设置默认值
                            if (listingFieldTypeExtensionFile.getEnabledFileDownload() == null) {
                                listingFieldTypeExtensionFile.setEnabledFileDownload(Boolean.FALSE);
                            }
                            // 设置默认值
                            if (listingFieldTypeExtensionFile.getNamingPolicy() == null) {
                                listingFieldTypeExtensionFile.setNamingPolicy(NamingPolicyEnum.KEEP);
                            }
                            switch (listingFieldTypeExtensionFile.getNamingPolicy()) {
                                case COMBINE: {
                                    NamingPolicyExtCombine namingPolicyExtCombine =
                                            listingFieldTypeExtensionFile.getNamingPolicyExtCombine();
                                    if (namingPolicyExtCombine == null) {
                                        throw new AbcIllegalParameterException(String.format("field %s 's " +
                                                        "extension.naming_policy_ext_combine should not be null if " +
                                                        "naming policy is %s", listingDataFieldDto.getFieldName(),
                                                listingFieldTypeExtensionFile.getNamingPolicy()));
                                    }
                                    if (CollectionUtils.isEmpty(namingPolicyExtCombine.getFields())) {
                                        throw new AbcIllegalParameterException(String.format("field %s 's " +
                                                        "extension.naming_policy_ext_combine.fields should not be" +
                                                        " null or empty if " +
                                                        "naming policy is %s", listingDataFieldDto.getFieldName(),
                                                listingFieldTypeExtensionFile.getNamingPolicy()));
                                    }
                                    for (NamingPolicyExtCombineField field :
                                            namingPolicyExtCombine.getFields()) {
                                        if (ObjectUtils.isEmpty(field.getFieldName())) {
                                            throw new AbcIllegalParameterException(String.format("field %s 's " +
                                                            "extension.naming_policy_ext_combine.fields " +
                                                            "should " +
                                                            "not contain" +
                                                            " null or empty field name if " +
                                                            "naming policy is %s", listingDataFieldDto.getFieldName(),
                                                    listingFieldTypeExtensionFile.getNamingPolicy()));
                                        }
                                    }
                                }
                                break;
                                default:
                                    break;
                            }
                        }
                    }
                    break;
                    case IMAGE: {
                        if (listingDataFieldDto.getExtension() != null
                                && !listingDataFieldDto.getExtension().isEmpty()) {
                            ListingFieldTypeExtensionImage listingFieldTypeExtensionImage =
                                    JSONObject.toJavaObject(listingDataFieldDto.getExtension(),
                                            ListingFieldTypeExtensionImage.class);
                            // 设置默认值
                            if (listingFieldTypeExtensionImage.getEnabledImagePreview() == null) {
                                listingFieldTypeExtensionImage.setEnabledImagePreview(Boolean.FALSE);
                            }
                            // 设置默认值
                            if (listingFieldTypeExtensionImage.getNamingPolicy() == null) {
                                listingFieldTypeExtensionImage.setNamingPolicy(NamingPolicyEnum.KEEP);
                            }
                            switch (listingFieldTypeExtensionImage.getNamingPolicy()) {
                                case COMBINE: {
                                    NamingPolicyExtCombine namingPolicyExtCombine =
                                            listingFieldTypeExtensionImage.getNamingPolicyExtCombine();
                                    if (namingPolicyExtCombine == null) {
                                        throw new AbcIllegalParameterException(String.format("field %s 's " +
                                                        "extension.naming_policy_ext_combine should not be null if " +
                                                        "naming policy is %s", listingDataFieldDto.getFieldName(),
                                                listingFieldTypeExtensionImage.getNamingPolicy()));
                                    }
                                    if (CollectionUtils.isEmpty(namingPolicyExtCombine.getFields())) {
                                        throw new AbcIllegalParameterException(String.format("field %s 's " +
                                                        "extension.naming_policy_ext_combine.fields should not be" +
                                                        " null or empty if " +
                                                        "naming policy is %s", listingDataFieldDto.getFieldName(),
                                                listingFieldTypeExtensionImage.getNamingPolicy()));
                                    }
                                    for (NamingPolicyExtCombineField field :
                                            namingPolicyExtCombine.getFields()) {
                                        if (ObjectUtils.isEmpty(field.getFieldName())) {
                                            throw new AbcIllegalParameterException(String.format("field %s 's " +
                                                            "extension.naming_policy_ext_combine.fields should " +
                                                            "not contain" +
                                                            " null or empty field name if " +
                                                            "naming policy is %s", listingDataFieldDto.getFieldName(),
                                                    listingFieldTypeExtensionImage.getNamingPolicy()));
                                        }
                                    }
                                }
                                break;
                                default:
                                    break;
                            }
                        }
                    }
                    break;
                    default:
                        break;
                }
            }

            listingDataFieldDto.setListingSequence(beginSequence++);
        }

        List<ListingDataFieldDo> existingItemDoList =
                this.listingDataFieldRepository.findByDataFacetUid(
                        dataFacetUid, Sort.by(Sort.Order.asc("listingSequence")));
        Map<String, ListingDataFieldDo> existingItemDoMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(existingItemDoList)) {
            existingItemDoList.forEach(existingItemDo -> {
                existingItemDoMap.put(existingItemDo.getFieldName(), existingItemDo);
            });
        }

        List<ListingDataFieldDto> inputItemList = listingDataFieldDtoList;
        Map<String, ListingDataFieldDto> inputItemMap = new HashMap();
        if (!CollectionUtils.isEmpty(inputItemList)) {
            for (int i = 0; i < inputItemList.size(); i++) {
                ListingDataFieldDto inputItem = inputItemList.get(i);
                inputItemMap.put(inputItem.getFieldName(), inputItem);
            }
        }

        List<ListingDataFieldDo> toAddItemDoList = new LinkedList<>();
        List<ListingDataFieldDo> toUpdateItemDoList = new LinkedList<>();
        List<ListingDataFieldDo> toDeleteItemDoList = new LinkedList<>();

        //
        // Step 2, core-processing
        //
        existingItemDoMap.forEach((key, existingItemDo) -> {
            if (inputItemMap.containsKey(key)) {
                // existing 有，input 有
                // 可能是更新

                ListingDataFieldDto inputItem = inputItemMap.get(key);
                existingItemDo.setWidth(inputItem.getWidth());
                existingItemDo.setExtension(inputItem.getExtension());
                existingItemDo.setListingSequence(inputItem.getListingSequence());

                BaseDo.update(existingItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                toUpdateItemDoList.add(existingItemDo);
            } else {
                // existing 有，input 没有
                // 删除
                existingItemDo.setDeleted(Boolean.TRUE);
                BaseDo.update(existingItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                toDeleteItemDoList.add(existingItemDo);
            }
        });

        inputItemMap.forEach((key, inputItem) -> {
            if (!existingItemDoMap.containsKey(key)) {
                // input 有，existing 没有
                // 新增
                ListingDataFieldDo newItemDo = new ListingDataFieldDo();

                newItemDo.setFieldName(inputItem.getFieldName());
                newItemDo.setWidth(inputItem.getWidth());
                newItemDo.setExtension(inputItem.getExtension());
                newItemDo.setListingSequence(inputItem.getListingSequence());
                newItemDo.setDataFacetUid(dataFacetUid);
                BaseDo.create(newItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                toAddItemDoList.add(newItemDo);
            }
        });

        if (!CollectionUtils.isEmpty(toAddItemDoList)) {
            this.listingDataFieldRepository.saveAll(toAddItemDoList);
        }
        if (!CollectionUtils.isEmpty(toUpdateItemDoList)) {
            this.listingDataFieldRepository.saveAll(toUpdateItemDoList);
        }
        if (!CollectionUtils.isEmpty(toDeleteItemDoList)) {
            this.listingDataFieldRepository.saveAll(toDeleteItemDoList);
        }

        //
        // Step 3, post-processing
        //

        // step 3.1, event post
        DataFacetChangedEvent dataFacetChangedEvent = new DataFacetChangedEvent();
        dataFacetChangedEvent.setDataFacetDo(dataFacetDo);
        dataFacetChangedEvent.setOperatingUserProfile(operatingUserProfile);
        this.eventBusManager.send(dataFacetChangedEvent);
    }

    @Override
    public void replaceListingExtendedOfDataFacet(
            Long dataFacetUid,
            ListingExtendedDto listingExtendedDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DataFacetDo dataFacetDo = this.dataFacetRepository.findByUid(dataFacetUid);
        if (dataFacetDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataFacetDo.RESOURCE_SYMBOL,
                    dataFacetUid));
        }

        ListingExtendedDo listingExtendedDo = this.listingExtendedRepository.findByDataFacetUid(dataFacetUid);

        //
        // Step 2, core-processing
        //
        if (listingExtendedDo == null) {
            listingExtendedDo = new ListingExtendedDo();
            listingExtendedDo.setDataFacetUid(dataFacetUid);
            BaseDo.create(listingExtendedDo, operatingUserProfile.getUid(), LocalDateTime.now());
        } else {
            BaseDo.update(listingExtendedDo, operatingUserProfile.getUid(), LocalDateTime.now());
        }

        listingExtendedDo.setDefaultPageSize(listingExtendedDto.getDefaultPageSize());
        listingExtendedDo.setEnabledVerticalScrolling(listingExtendedDto.getEnabledVerticalScrolling());
        listingExtendedDo.setVerticalScrollingHeightThreshold(listingExtendedDto.getVerticalScrollingHeightThreshold());
        listingExtendedDo.setEnabledColumnNo(listingExtendedDto.getEnabledColumnNo());
        listingExtendedDo.setEnabledFreezeTopRows(listingExtendedDto.getEnabledFreezeTopRows());
        listingExtendedDo.setInclusiveTopRows(listingExtendedDto.getInclusiveTopRows());
        listingExtendedDo.setEnabledFreezeLeftColumns(listingExtendedDto.getEnabledFreezeLeftColumns());
        listingExtendedDo.setInclusiveLeftColumns(listingExtendedDto.getInclusiveLeftColumns());
        listingExtendedDo.setEnabledFreezeRightColumns(listingExtendedDto.getEnabledFreezeRightColumns());
        listingExtendedDo.setInclusiveRightColumns(listingExtendedDto.getInclusiveRightColumns());

        this.listingExtendedRepository.save(listingExtendedDo);

        //
        // Step 3, post-processing
        //

        // step 3.1, event post
        DataFacetChangedEvent dataFacetChangedEvent = new DataFacetChangedEvent();
        dataFacetChangedEvent.setDataFacetDo(dataFacetDo);
        dataFacetChangedEvent.setOperatingUserProfile(operatingUserProfile);
        this.eventBusManager.send(dataFacetChangedEvent);
    }

    @Override
    public File exportSequenceOfAllListingDataFieldsOfDataFacet(
            Long dataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Specification<ListingDataFieldDo> specification = new Specification<ListingDataFieldDo>() {
            @Override
            public Predicate toPredicate(Root<ListingDataFieldDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (dataFacetUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("dataFacetUid"), dataFacetUid));
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        List<ListingDataFieldDo> itemDoList = this.listingDataFieldRepository.findAll(
                specification, Sort.by(Sort.Order.asc("listingSequence")));
        if (CollectionUtils.isEmpty(itemDoList)) {
            return null;
        }

        //
        // write to excel
        //
        Path path = Paths.get(this.projectUploadPath, "listing_" + dataFacetUid + ".xlsx");
        File file = path.toFile();

        List<List<String>> head = new ArrayList<>(1);
        List<String> headLine = new ArrayList<>(1);
        headLine.add("Field Name");
        head.add(headLine);

        List<List<Object>> body = new LinkedList<>();
        itemDoList.forEach(itemDo -> {
            List<Object> bodyLine = new ArrayList<>(1);
            bodyLine.add(itemDo.getFieldName());
            body.add(bodyLine);
        });

        EasyExcel.write(file.getAbsolutePath()).head(head).sheet(0).doWrite(body);

        return file;
    }

    @Override
    public void importSequenceOfAllListingDataFieldsOfDataFacet(
            Long dataFacetUid,
            MultipartFile file,
            UserProfile operatingUserProfile) {
        //
        // step 1, pre-processing
        //
        List<ListingDataFieldDo> itemDoList = this.listingDataFieldRepository.findByDataFacetUid(dataFacetUid,
                Sort.by(Sort.Order.asc("listingSequence")));
        if (CollectionUtils.isEmpty(itemDoList)) {
            throw new AbcIllegalParameterException("no listing data field found");
        }
        List<String> existingListingDataFieldNameList = new ArrayList<>(itemDoList.size());
        itemDoList.forEach(itemDo -> {
            existingListingDataFieldNameList.add(itemDo.getFieldName());
        });

        //
        // step 2, core-processing
        //
        String uuid = UUID.randomUUID().toString();
        String fileId = Base64.encodeBase64String(uuid.getBytes(StandardCharsets.UTF_8)).toLowerCase();

        Path targetPath = Paths.get(this.projectUploadPath, fileId, file.getOriginalFilename());
        if (!targetPath.toFile().exists()) {
            targetPath.toFile().getParentFile().mkdirs();
        }

        try {
            // Copy file to the target location (Replacing existing file with the same name)
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("failed to store file: {}", targetPath, e);
            throw new AbcResourceConflictException("Could not store file " + file.getOriginalFilename() + ". Please " +
                    "try again.");
        }

        List<Map<Integer, String>> result = EasyExcel.read(targetPath.toFile()).sheet(0).headRowNumber(1).doReadSync();
        if (CollectionUtils.isEmpty(result) || result.size() == 1) {
            throw new AbcIllegalParameterException("empty content");
        }

        // 导入的 listing data field 可能只是部分 listing data field
        Map<String, Float> newSequenceMap = new HashMap<>();
        Float sequence = 1.0f;
        // 1行 head row 已经略过，这里都是 boy rows
        for (int rowIndex = 0; rowIndex < result.size(); rowIndex++) {
            Map<Integer, String> row = result.get(rowIndex);
            // 只有1列
            if (CollectionUtils.isEmpty(row)) {
                throw new AbcIllegalParameterException("The 1st column should be the sorted listing data field name");
            }

            String column0 = row.get(0);
            if (ObjectUtils.isEmpty(column0)) {
                throw new AbcIllegalParameterException("The 1st column should be the sorted listing data field name");
            }

            if (!existingListingDataFieldNameList.contains(column0)) {
                throw new AbcIllegalParameterException("The 1st column should be the sorted listing data field name");
            }

            newSequenceMap.put(column0, sequence);
            sequence += 1.0f;
        }

        final float baselineSequence = sequence + 1.0f;
        itemDoList.forEach(itemDo -> {
            if (newSequenceMap.containsKey(itemDo.getFieldName())) {
                itemDo.setListingSequence(newSequenceMap.get(itemDo.getFieldName()));
            } else {
                if (itemDo.getListingSequence() == null) {
                    itemDo.setListingSequence(baselineSequence);
                } else {
                    itemDo.setListingSequence(itemDo.getListingSequence() + baselineSequence);
                }
            }
        });

        this.listingDataFieldRepository.saveAll(itemDoList);
    }

    @Override
    public List<SortingDataFieldDto> listingQuerySortingDataFieldsOfDataFacet(
            Long dataFacetUid,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Specification<SortingDataFieldDo> specification = new Specification<SortingDataFieldDo>() {
            @Override
            public Predicate toPredicate(Root<SortingDataFieldDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (dataFacetUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("dataFacetUid"), dataFacetUid));
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        if (sort == null || sort.isUnsorted()) {
            sort = Sort.by(Sort.Order.asc("sortingSequence"));
        }
        List<SortingDataFieldDo> itemDoList = this.sortingDataFieldRepository.findAll(specification, sort);
        if (CollectionUtils.isEmpty(itemDoList)) {
            return null;
        }
        List<SortingDataFieldDto> content = new LinkedList<>();
        itemDoList.forEach(itemDo -> {
            SortingDataFieldDto itemDto = new SortingDataFieldDto();
            BeanUtils.copyProperties(itemDo, itemDto);
            content.add(itemDto);
        });
        return content;
    }

    @Override
    public void replaceAllSortingDataFieldsOfDataFacet(
            Long dataFacetUid,
            List<SortingDataFieldDto> sortingDataFieldDtoList,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        if (CollectionUtils.isEmpty(sortingDataFieldDtoList)) {
            return;
        }

        DataFacetDo dataFacetDo = this.dataFacetRepository.findByUid(dataFacetUid);
        if (dataFacetDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataFacetDo.RESOURCE_SYMBOL,
                    dataFacetUid));
        }

        Map<String, DataFieldDo> availableDataFieldDoMap = new HashMap<>();
        List<DataFieldDo> availableDataFieldDoList = this.dataFieldRepository.findByDataFacetUid(dataFacetUid);
        if (CollectionUtils.isEmpty(availableDataFieldDoList)) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataFacetDo.RESOURCE_SYMBOL,
                    dataFacetUid));
        }
        availableDataFieldDoList.forEach(dataFieldDo -> {
            availableDataFieldDoMap.put(dataFieldDo.getName(), dataFieldDo);
        });

        // 一方面校验，另一方面分配 sequence
        float beginSequence = 1.0f;
        for (SortingDataFieldDto sortingDataFieldDto : sortingDataFieldDtoList) {
            if (sortingDataFieldDto == null) {
                throw new AbcIllegalParameterException("at least one sorting data field is null");
            }
            if (ObjectUtils.isEmpty(sortingDataFieldDto.getFieldName())) {
                throw new AbcIllegalParameterException("at least one field name is null or empty");
            }
            if (ObjectUtils.isEmpty(sortingDataFieldDto.getDirection() == null)) {
                throw new AbcIllegalParameterException("at least one field's direction is null or empty");
            }
            if (!availableDataFieldDoMap.containsKey(sortingDataFieldDto.getFieldName())) {
                throw new AbcIllegalParameterException("at least one field name does not exist");
            }

            sortingDataFieldDto.setSortingSequence(beginSequence++);
        }

        List<SortingDataFieldDo> existingItemDoList =
                this.sortingDataFieldRepository.findByDataFacetUid(
                        dataFacetUid, Sort.by(Sort.Order.asc(BaseDo.ID_FIELD_NAME)));
        Map<String, SortingDataFieldDo> existingItemDoMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(existingItemDoList)) {
            existingItemDoList.forEach(existingItemDo -> {
                existingItemDoMap.put(existingItemDo.getFieldName(), existingItemDo);
            });
        }

        List<SortingDataFieldDto> inputItemList = sortingDataFieldDtoList;
        Map<String, SortingDataFieldDto> inputItemMap = new HashMap();
        if (!CollectionUtils.isEmpty(inputItemList)) {
            for (int i = 0; i < inputItemList.size(); i++) {
                SortingDataFieldDto inputItem = inputItemList.get(i);
                inputItemMap.put(inputItem.getFieldName(), inputItem);
            }
        }

        List<SortingDataFieldDo> toAddItemDoList = new LinkedList<>();
        List<SortingDataFieldDo> toUpdateItemDoList = new LinkedList<>();
        List<SortingDataFieldDo> toDeleteItemDoList = new LinkedList<>();

        //
        // Step 2, core-processing
        //
        existingItemDoMap.forEach((key, existingItemDo) -> {
            if (inputItemMap.containsKey(key)) {
                // existing 有，input 有
                // 覆盖

                SortingDataFieldDto inputItem = inputItemMap.get(key);
                existingItemDo.setDirection(inputItem.getDirection());
                existingItemDo.setSortingSequence(inputItem.getSortingSequence());

                BaseDo.update(existingItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                toUpdateItemDoList.add(existingItemDo);
            } else {
                // existing 有，input 没有
                // 删除
                existingItemDo.setDeleted(Boolean.TRUE);
                BaseDo.update(existingItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                toDeleteItemDoList.add(existingItemDo);
            }
        });

        inputItemMap.forEach((key, inputItem) -> {
            if (!existingItemDoMap.containsKey(key)) {
                // input 有，existing 没有
                // 新增
                SortingDataFieldDo newItemDo = new SortingDataFieldDo();

                newItemDo.setFieldName(inputItem.getFieldName());
                newItemDo.setDirection(inputItem.getDirection());
                newItemDo.setSortingSequence(inputItem.getSortingSequence());
                newItemDo.setDataFacetUid(dataFacetUid);
                BaseDo.create(newItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                toAddItemDoList.add(newItemDo);
            }
        });

        if (!CollectionUtils.isEmpty(toAddItemDoList)) {
            this.sortingDataFieldRepository.saveAll(toAddItemDoList);
        }
        if (!CollectionUtils.isEmpty(toUpdateItemDoList)) {
            this.sortingDataFieldRepository.saveAll(toUpdateItemDoList);
        }
        if (!CollectionUtils.isEmpty(toDeleteItemDoList)) {
            this.sortingDataFieldRepository.saveAll(toDeleteItemDoList);
        }

        //
        // Step 3, post-processing
        //

        // step 3.1, event post
        DataFacetChangedEvent dataFacetChangedEvent = new DataFacetChangedEvent();
        dataFacetChangedEvent.setDataFacetDo(dataFacetDo);
        dataFacetChangedEvent.setOperatingUserProfile(operatingUserProfile);
        this.eventBusManager.send(dataFacetChangedEvent);
    }

    /**
     * 在 event bus 中注册成为 subscriber
     */
    @PostConstruct
    public void init() {
        this.eventBusManager.registerSubscriber(this);
    }

    /**
     * event handler
     *
     * @param event
     */
    @Transactional(rollbackFor = Exception.class)
    @Subscribe
    public void handleDataFacetCreatedEvent(DataFacetCreatedEvent event) {
        LOGGER.info("rcv event:{}", event);

        //
        // Step 1, pre-processing
        //
        Specification<DataFieldDo> specification = new Specification<DataFieldDo>() {
            @Override
            public Predicate toPredicate(Root<DataFieldDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                predicateList.add(criteriaBuilder.equal(root.get("dataFacetUid"), event.getDataFacetDo().getUid()));
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        List<DataFieldDo> dataFieldDoList = this.dataFieldRepository.findAll(specification,
                Sort.by(Sort.Order.asc("sequence")));
        if (CollectionUtils.isEmpty(dataFieldDoList)) {
            return;
        }

        //
        // Step 2, core-processing
        //

        // 设置 filtering data field 的最小和最大数量
        int maximumNumberOfFilteringDataField;
        int maximumNumberOfSortingDataField;
        if (dataFieldDoList.size() > 300) {
            maximumNumberOfFilteringDataField = dataFieldDoList.size() / 30;
            maximumNumberOfSortingDataField = dataFieldDoList.size() / 60;
        } else if (dataFieldDoList.size() > 200) {
            maximumNumberOfFilteringDataField = dataFieldDoList.size() / 20;
            maximumNumberOfSortingDataField = dataFieldDoList.size() / 40;
        } else if (dataFieldDoList.size() > 100) {
            maximumNumberOfFilteringDataField = dataFieldDoList.size() / 10;
            maximumNumberOfSortingDataField = dataFieldDoList.size() / 20;
        } else if (dataFieldDoList.size() > 50) {
            maximumNumberOfFilteringDataField = dataFieldDoList.size() / 5;
            maximumNumberOfSortingDataField = dataFieldDoList.size() / 10;
        } else if (dataFieldDoList.size() > 10) {
            maximumNumberOfFilteringDataField = dataFieldDoList.size() / 3;
            maximumNumberOfSortingDataField = dataFieldDoList.size() / 6;
        } else {
            maximumNumberOfFilteringDataField = dataFieldDoList.size();
            maximumNumberOfSortingDataField = dataFieldDoList.size();
        }
        List<FilteringDataFieldDto> filteringDataFieldDtoList = new LinkedList<>();
        List<ListingDataFieldDto> listingDataFieldDtoList = new LinkedList<>();
        List<SortingDataFieldDto> sortingDataFieldDtoList = new LinkedList<>();

        dataFieldDoList.forEach(dataFieldDo -> {
            // filtering data field
            switch (dataFieldDo.getType()) {
                case DATETIME: {
                    if (filteringDataFieldDtoList.size() < maximumNumberOfFilteringDataField) {
                        FilteringDataFieldDto filteringDataFieldDto = new FilteringDataFieldDto();
                        filteringDataFieldDto.setFieldName(dataFieldDo.getName());
                        filteringDataFieldDto.setFilteringSequence(dataFieldDo.getSequence());
                        filteringDataFieldDto.setFilteringType(FilteringTypeEnum.DATETIME_RANGE);
                        filteringDataFieldDtoList.add(filteringDataFieldDto);
                    }
                }
                case DATE: {
                    if (filteringDataFieldDtoList.size() < maximumNumberOfFilteringDataField) {
                        FilteringDataFieldDto filteringDataFieldDto = new FilteringDataFieldDto();
                        filteringDataFieldDto.setFieldName(dataFieldDo.getName());
                        filteringDataFieldDto.setFilteringSequence(dataFieldDo.getSequence());
                        filteringDataFieldDto.setFilteringType(FilteringTypeEnum.DATE_RANGE);
                        filteringDataFieldDtoList.add(filteringDataFieldDto);
                    }
                }
                case STRING: {
                    if (filteringDataFieldDtoList.size() < maximumNumberOfFilteringDataField) {
                        FilteringDataFieldDto filteringDataFieldDto = new FilteringDataFieldDto();
                        filteringDataFieldDto.setFieldName(dataFieldDo.getName());
                        filteringDataFieldDto.setFilteringSequence(dataFieldDo.getSequence());
                        filteringDataFieldDto.setFilteringType(FilteringTypeEnum.CONTAINS_TEXT);
                        filteringDataFieldDtoList.add(filteringDataFieldDto);
                    }
                }
                break;
            }

            // listing data field
            ListingDataFieldDto listingDataFieldDto = new ListingDataFieldDto();
            listingDataFieldDto.setFieldName(dataFieldDo.getName());
            listingDataFieldDto.setListingSequence(dataFieldDo.getSequence());
            listingDataFieldDto.setWidth(100);
            listingDataFieldDtoList.add(listingDataFieldDto);

            // sorting data field
            switch (dataFieldDo.getType()) {
                case DATETIME:
                case DATE: {
                    if (sortingDataFieldDtoList.size() < maximumNumberOfSortingDataField) {
                        SortingDataFieldDto sortingDataFieldDto = new SortingDataFieldDto();
                        sortingDataFieldDto.setFieldName(dataFieldDo.getName());
                        sortingDataFieldDto.setSortingSequence(dataFieldDo.getSequence());
                        sortingDataFieldDto.setDirection(Sort.Direction.ASC);
                        sortingDataFieldDtoList.add(sortingDataFieldDto);
                    }
                }
                break;
            }
        });

        if (CollectionUtils.isEmpty(sortingDataFieldDtoList)) {
            // should contain at least 1 sorting field, otherwise, end-user cannot use paginated queries
            SortingDataFieldDto sortingDataFieldDto = new SortingDataFieldDto();
            sortingDataFieldDto.setFieldName(dataFieldDoList.get(0).getName());
            sortingDataFieldDto.setSortingSequence(dataFieldDoList.get(0).getSequence());
            sortingDataFieldDto.setDirection(Sort.Direction.ASC);
            sortingDataFieldDtoList.add(sortingDataFieldDto);
        }

        FilteringExtendedDto filteringExtendedDto = new FilteringExtendedDto();
        filteringExtendedDto.setEnabledDefaultQuery(Boolean.TRUE);
        filteringExtendedDto.setEnabledFilterFolding(Boolean.TRUE);

        ListingExtendedDto listingExtendedDto = new ListingExtendedDto();
        listingExtendedDto.setDefaultPageSize(20);
        listingExtendedDto.setEnabledColumnNo(Boolean.TRUE);
        listingExtendedDto.setEnabledVerticalScrolling(Boolean.TRUE);
        listingExtendedDto.setVerticalScrollingHeightThreshold(800);
        listingExtendedDto.setEnabledFreezeTopRows(Boolean.TRUE);
        listingExtendedDto.setInclusiveTopRows(1);
        listingExtendedDto.setEnabledFreezeLeftColumns(Boolean.TRUE);
        listingExtendedDto.setInclusiveLeftColumns(1);
        listingExtendedDto.setEnabledFreezeRightColumns(Boolean.TRUE);
        listingExtendedDto.setInclusiveRightColumns(1);

        replaceAllFilteringDataFieldsOfDataFacet(event.getDataFacetDo().getUid(), filteringDataFieldDtoList,
                event.getOperatingUserProfile());
        replaceAllListingDataFieldsOfDataFacet(event.getDataFacetDo().getUid(), listingDataFieldDtoList, event.getOperatingUserProfile());
        replaceAllSortingDataFieldsOfDataFacet(event.getDataFacetDo().getUid(), sortingDataFieldDtoList, event.getOperatingUserProfile());
        replaceFilteringExtendedOfDataFacet(event.getDataFacetDo().getUid(), filteringExtendedDto, event.getOperatingUserProfile());
        replaceListingExtendedOfDataFacet(event.getDataFacetDo().getUid(), listingExtendedDto, event.getOperatingUserProfile());

        //
        // Step 3, post-processing
        //
    }

    /**
     * event handler
     *
     * @param event
     */
    @Subscribe
    public void handleDataFacetDeletedEvent(DataFacetDeletedEvent event) {
        LOGGER.info("rcv event:{}", event);

        //
        // Step 1, pre-processing
        //

        //
        // Step 2, core-processing
        //
        this.filteringDataFieldRepository.deleteByDataFacetUid(event.getDataFacetDo().getUid());
        this.listingDataFieldRepository.deleteByDataFacetUid(event.getDataFacetDo().getUid());
        this.sortingDataFieldRepository.deleteByDataFacetUid(event.getDataFacetDo().getUid());
        this.filteringExtendedRepository.deleteByDataFacetUid(event.getDataFacetDo().getUid());
        this.listingExtendedRepository.deleteByDataFacetUid(event.getDataFacetDo().getUid());

        //
        // Step 3, post-processing
        //
    }

    /**
     * event handler
     *
     * @param event
     */
    @Subscribe
    public void handleDataFacetChangedEvent(DataFacetChangedEvent event) {
        LOGGER.info("rcv event:{}", event);

        //
        // Step 1, pre-processing
        //
        List<DataFieldDo> dataFieldDoList = this.dataFieldRepository.findByDataFacetUid(event.getDataFacetDo().getUid());
        if (CollectionUtils.isEmpty(dataFieldDoList)) {
            return;
        }
        Map<String, DataFieldDo> dataFieldDoMap = new HashMap<>();
        dataFieldDoList.forEach(dataFieldDo -> {
            dataFieldDoMap.put(dataFieldDo.getName(), dataFieldDo);
        });

        //
        // Step 2, core-processing
        //

        // filtering data field
        List<FilteringDataFieldDo> filteringDataFieldDoList =
                this.filteringDataFieldRepository.findByDataFacetUid(event.getDataFacetDo().getUid(),
                        Sort.by(Sort.Order.asc("filteringSequence")));
        if (!CollectionUtils.isEmpty(filteringDataFieldDoList)) {
            List<FilteringDataFieldDo> toDeleteList = new LinkedList<>();
            filteringDataFieldDoList.forEach(filteringDataFieldDo -> {
                if (!dataFieldDoMap.containsKey(filteringDataFieldDo.getFieldName())) {
                    filteringDataFieldDo.setDeleted(Boolean.TRUE);
                    BaseDo.update(filteringDataFieldDo, event.getOperatingUserProfile().getUid(), LocalDateTime.now());
                    toDeleteList.add(filteringDataFieldDo);
                }
            });
            if (!CollectionUtils.isEmpty(toDeleteList)) {
                this.filteringDataFieldRepository.saveAll(toDeleteList);
            }
        }

        // listing data field
        List<ListingDataFieldDo> listingDataFieldDoList =
                this.listingDataFieldRepository.findByDataFacetUid(event.getDataFacetDo().getUid(),
                        Sort.by(Sort.Order.asc("listingSequence")));
        if (!CollectionUtils.isEmpty(listingDataFieldDoList)) {
            List<ListingDataFieldDo> toDeleteList = new LinkedList<>();
            listingDataFieldDoList.forEach(listingDataFieldDo -> {
                if (!dataFieldDoMap.containsKey(listingDataFieldDo.getFieldName())) {
                    listingDataFieldDo.setDeleted(Boolean.TRUE);
                    BaseDo.update(listingDataFieldDo, event.getOperatingUserProfile().getUid(), LocalDateTime.now());
                    toDeleteList.add(listingDataFieldDo);
                }
            });
            if (!CollectionUtils.isEmpty(toDeleteList)) {
                this.listingDataFieldRepository.saveAll(toDeleteList);
            }
        }

        // sorting data field
        List<SortingDataFieldDo> sortingDataFieldDoList =
                this.sortingDataFieldRepository.findByDataFacetUid(event.getDataFacetDo().getUid(),
                        Sort.by(Sort.Order.asc("sortingSequence")));
        if (!CollectionUtils.isEmpty(sortingDataFieldDoList)) {
            List<SortingDataFieldDo> toDeleteList = new LinkedList<>();
            sortingDataFieldDoList.forEach(sortingDataFieldDo -> {
                if (!dataFieldDoMap.containsKey(sortingDataFieldDo.getFieldName())) {
                    sortingDataFieldDo.setDeleted(Boolean.TRUE);
                    BaseDo.update(sortingDataFieldDo, event.getOperatingUserProfile().getUid(), LocalDateTime.now());
                    toDeleteList.add(sortingDataFieldDo);
                }
            });
            if (!CollectionUtils.isEmpty(toDeleteList)) {
                this.sortingDataFieldRepository.saveAll(toDeleteList);
            }
        }

        //
        // Step 3, post-processing
        //
    }

    /**
     * event handler
     *
     * @param event
     */
    @Subscribe
    public void handleDataDictionaryDeletedEvent(DataDictionaryDeletedEvent event) {
        LOGGER.info("rcv event:{}", event);

        //
        // Step 1, pre-processing
        //
        Long dictionaryCategoryUid = event.getUid();
        UserProfile operatingUserProfile = event.getOperatingUserProfile();

        //
        // Step 2, core-processing
        //
        Specification<FilteringDataFieldDo> specification = new Specification<FilteringDataFieldDo>() {
            @Override
            public Predicate toPredicate(Root<FilteringDataFieldDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();

                predicateList.add(criteriaBuilder.or(
                        criteriaBuilder.isNotNull(root.get("filteringTypeExtension")),
                        criteriaBuilder.isNotNull(root.get("defaultValueSettings"))));

                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };
        List<FilteringDataFieldDo> filteringDataFieldDoList = this.filteringDataFieldRepository.findAll(specification,
                Sort.by(Sort.Order.asc(BaseDo.ID_FIELD_NAME)));
        if (!CollectionUtils.isEmpty(filteringDataFieldDoList)) {
            List<FilteringDataFieldDo> toUpdateFilteringDataFieldDoList = new LinkedList<>();
            filteringDataFieldDoList.forEach(filteringDataFieldDo -> {
                boolean requiredToUpdate = false;

                if (filteringDataFieldDo.getFilteringTypeExtension() != null
                        && !filteringDataFieldDo.getFilteringTypeExtension().isEmpty()) {
                    switch (filteringDataFieldDo.getFilteringType()) {
                        case DROP_DOWN_LIST_SINGLE:
                        case DROP_DOWN_LIST_MULTIPLE:
                        case ASSOCIATING_SINGLE:
                        case ASSOCIATING_MULTIPLE: {
                            FilteringFieldOptionalValueSettings filteringFieldOptionalValueSettings =
                                    JSONObject.toJavaObject(filteringDataFieldDo.getFilteringTypeExtension(),
                                            FilteringFieldOptionalValueSettings.class);
                            if (dictionaryCategoryUid.equals(filteringFieldOptionalValueSettings.getDictionaryCategoryUid())) {
                                filteringDataFieldDo.setFilteringTypeExtension(null);
                                requiredToUpdate = true;
                            }
                        }
                        break;
                        case CASCADING_DROP_DOWN_LIST_SINGLE:
                        case CASCADING_DROP_DOWN_LIST_MULTIPLE: {
                            FilteringFieldCascadingSettings filteringFieldCascadingSettings =
                                    JSONObject.toJavaObject(filteringDataFieldDo.getFilteringTypeExtension(),
                                            FilteringFieldCascadingSettings.class);
                            if (dictionaryCategoryUid.equals(filteringFieldCascadingSettings.getDictionaryCategoryUid())) {
                                filteringDataFieldDo.setFilteringTypeExtension(null);
                                requiredToUpdate = true;
                            }
                        }
                        break;
                        default:
                            break;
                    }
                }

                if (filteringDataFieldDo.getDefaultValueSettings() != null
                        && !filteringDataFieldDo.getDefaultValueSettings().isEmpty()) {
                    FilteringFieldDefaultValueSettings filteringFieldDefaultValueSettings =
                            JSONObject.toJavaObject(filteringDataFieldDo.getDefaultValueSettings(),
                                    FilteringFieldDefaultValueSettings.class);
                    if (dictionaryCategoryUid.equals(filteringFieldDefaultValueSettings.getDictionaryCategoryUid())) {
                        filteringDataFieldDo.setDefaultValueSettings(null);
                        requiredToUpdate = true;
                    }
                }

                if (requiredToUpdate) {
                    BaseDo.update(filteringDataFieldDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    toUpdateFilteringDataFieldDoList.add(filteringDataFieldDo);
                }
            });
            if (!CollectionUtils.isEmpty(toUpdateFilteringDataFieldDoList)) {
                this.filteringDataFieldRepository.saveAll(toUpdateFilteringDataFieldDoList);
            }
        }

        //
        // Step 3, post-processing
        //
    }

    @ResourceReferenceHandler(name = "data facet appearance")
    public List<String> checkResourceReference(
            ResourceReferenceManager.ResourceCategoryEnum resourceCategory,
            Long resourceUid,
            String resourceName) throws Exception {
        switch (resourceCategory) {
            case DATA_DICTIONARY: {
                Long dictionaryCategoryUid = resourceUid;

                Specification<FilteringDataFieldDo> specification = new Specification<FilteringDataFieldDo>() {
                    @Override
                    public Predicate toPredicate(Root<FilteringDataFieldDo> root, CriteriaQuery<?> query,
                                                 CriteriaBuilder criteriaBuilder) {
                        List<Predicate> predicateList = new ArrayList<>();

                        predicateList.add(criteriaBuilder.or(
                                criteriaBuilder.isNotNull(root.get("filteringTypeExtension")),
                                criteriaBuilder.isNotNull(root.get("defaultValueSettings"))));

                        return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
                    }
                };
                List<FilteringDataFieldDo> filteringDataFieldDoList = this.filteringDataFieldRepository.findAll(specification,
                        Sort.by(Sort.Order.asc(BaseDo.ID_FIELD_NAME)));
                if (!CollectionUtils.isEmpty(filteringDataFieldDoList)) {
                    // resource reference found
                    Map<Long, String> foundResourceReferenceByDataFacet = new HashMap<>();

                    for (FilteringDataFieldDo filteringDataFieldDo : filteringDataFieldDoList) {
                        if (filteringDataFieldDo.getFilteringTypeExtension() != null
                                && !filteringDataFieldDo.getFilteringTypeExtension().isEmpty()) {
                            switch (filteringDataFieldDo.getFilteringType()) {
                                case DROP_DOWN_LIST_SINGLE:
                                case DROP_DOWN_LIST_MULTIPLE:
                                case ASSOCIATING_SINGLE:
                                case ASSOCIATING_MULTIPLE: {
                                    FilteringFieldOptionalValueSettings filteringFieldOptionalValueSettings =
                                            JSONObject.toJavaObject(filteringDataFieldDo.getFilteringTypeExtension(),
                                                    FilteringFieldOptionalValueSettings.class);
                                    if (dictionaryCategoryUid.equals(filteringFieldOptionalValueSettings.getDictionaryCategoryUid())) {
                                        if (!foundResourceReferenceByDataFacet.containsKey(filteringDataFieldDo.getDataFacetUid())) {
                                            DataFacetDo dataFacetDo =
                                                    this.dataFacetRepository.findByUid(filteringDataFieldDo.getDataFacetUid());

                                            if (dataFacetDo != null) {
                                                foundResourceReferenceByDataFacet.put(
                                                        dataFacetDo.getUid(),
                                                        String.format(
                                                                "[%s] %s (%d)",
                                                                DataFacetDo.RESOURCE_SYMBOL,
                                                                dataFacetDo.getName(),
                                                                dataFacetDo.getUid()));
                                            }
                                        }
                                    }
                                }
                                break;
                                case CASCADING_DROP_DOWN_LIST_SINGLE:
                                case CASCADING_DROP_DOWN_LIST_MULTIPLE: {
                                    FilteringFieldCascadingSettings filteringFieldCascadingSettings =
                                            JSONObject.toJavaObject(filteringDataFieldDo.getFilteringTypeExtension(),
                                                    FilteringFieldCascadingSettings.class);
                                    if (dictionaryCategoryUid.equals(filteringFieldCascadingSettings.getDictionaryCategoryUid())) {
                                        if (!foundResourceReferenceByDataFacet.containsKey(filteringDataFieldDo.getDataFacetUid())) {
                                            DataFacetDo dataFacetDo =
                                                    this.dataFacetRepository.findByUid(filteringDataFieldDo.getDataFacetUid());

                                            if (dataFacetDo != null) {
                                                foundResourceReferenceByDataFacet.put(
                                                        dataFacetDo.getUid(),
                                                        String.format(
                                                                "[%s] %s (%d)",
                                                                DataFacetDo.RESOURCE_SYMBOL,
                                                                dataFacetDo.getName(),
                                                                dataFacetDo.getUid()));
                                            }
                                        }
                                    }
                                }
                                break;
                                default:
                                    break;
                            }
                        }

                        if (filteringDataFieldDo.getDefaultValueSettings() != null
                                && !filteringDataFieldDo.getDefaultValueSettings().isEmpty()) {
                            FilteringFieldDefaultValueSettings filteringFieldDefaultValueSettings =
                                    JSONObject.toJavaObject(filteringDataFieldDo.getDefaultValueSettings(),
                                            FilteringFieldDefaultValueSettings.class);
                            if (dictionaryCategoryUid.equals(filteringFieldDefaultValueSettings.getDictionaryCategoryUid())) {
                                if (!foundResourceReferenceByDataFacet.containsKey(filteringDataFieldDo.getDataFacetUid())) {
                                    DataFacetDo dataFacetDo =
                                            this.dataFacetRepository.findByUid(filteringDataFieldDo.getDataFacetUid());

                                    if (dataFacetDo != null) {
                                        foundResourceReferenceByDataFacet.put(
                                                dataFacetDo.getUid(),
                                                String.format(
                                                        "[%s] %s (%d)",
                                                        DataFacetDo.RESOURCE_SYMBOL,
                                                        dataFacetDo.getName(),
                                                        dataFacetDo.getUid()));
                                    }
                                }
                            }
                        }

                    }

                    if (!CollectionUtils.isEmpty(foundResourceReferenceByDataFacet)) {
                        return new ArrayList<>(foundResourceReferenceByDataFacet.values());
                    }
                }
            }
            break;
            default:
                break;
        }

        return null;
    }
}
