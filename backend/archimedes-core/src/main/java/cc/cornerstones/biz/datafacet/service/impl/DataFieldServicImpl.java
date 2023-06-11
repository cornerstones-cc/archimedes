package cc.cornerstones.biz.datafacet.service.impl;

import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.datafacet.dto.CreateDataFieldDto;
import cc.cornerstones.biz.datafacet.dto.DataFieldAnotherDto;
import cc.cornerstones.biz.datafacet.dto.DataFieldDto;
import cc.cornerstones.biz.datafacet.dto.UpdateDataFieldDto;
import cc.cornerstones.biz.datafacet.entity.DataFacetDo;
import cc.cornerstones.biz.datafacet.entity.DataFieldDo;
import cc.cornerstones.biz.datafacet.persistence.DataFieldRepository;
import cc.cornerstones.biz.datafacet.service.assembly.DataFieldHandler;
import cc.cornerstones.biz.datafacet.service.inf.DataFieldService;
import cc.cornerstones.biz.datafacet.share.constants.DataFieldTypeEnum;
import cc.cornerstones.biz.datafacet.share.types.FieldTypeExtensionCalculated;
import cc.cornerstones.biz.datafacet.share.types.FieldTypeExtensionFile;
import cc.cornerstones.biz.datasource.service.assembly.database.DataColumnMetadata;
import cc.cornerstones.biz.datatable.entity.DataColumnDo;
import cc.cornerstones.biz.share.event.*;
import com.alibaba.fastjson.JSONObject;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.PostConstruct;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DataFieldServicImpl implements DataFieldService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataFieldServicImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private DataFieldHandler dataFieldHandler;

    @Autowired
    private DataFieldRepository dataFieldRepository;

    @Override
    public DataFieldDto createDataField(
            CreateDataFieldDto createDataFieldDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return null;
    }

    @Override
    public DataFieldDto getDataField(
            Long dataFieldUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return null;
    }

    @Override
    public void updateDataField(
            Long dataFieldUid,
            UpdateDataFieldDto updateDataFieldDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {

    }

    @Override
    public List<String> listAllReferencesToDataField(
            Long dataFieldUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return null;
    }

    @Override
    public void deleteDataField(
            Long dataFieldUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void replaceAllDataFieldsOfDataFacet(
            Long dataFacetUid,
            List<DataFieldDto> dataFieldDtoList,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        if (CollectionUtils.isEmpty(dataFieldDtoList)) {
            return;
        }
        for (DataFieldDto dataFieldDto : dataFieldDtoList) {
            if (ObjectUtils.isEmpty(dataFieldDto.getName())) {
                throw new AbcIllegalParameterException(String.format("field %s 's name is empty",
                        dataFieldDto.getName()));
            }

            if (ObjectUtils.isEmpty(dataFieldDto.getLabel())) {
                throw new AbcIllegalParameterException(String.format("field %s 's label is empty",
                        dataFieldDto.getName()));
            }

            if (dataFieldDto.getType() == null) {
                throw new AbcIllegalParameterException(String.format("field %s 's type is null", dataFieldDto.getName()));
            }

            switch (dataFieldDto.getType()) {
                case FILE:
                case IMAGE: {
                    if (dataFieldDto.getTypeExtension() != null
                            && !dataFieldDto.getTypeExtension().isEmpty()) {
                        FieldTypeExtensionFile fieldTypeExtensionFile =
                                JSONObject.toJavaObject(dataFieldDto.getTypeExtension(), FieldTypeExtensionFile.class);
                        if (fieldTypeExtensionFile.getSettingsMode() == null) {
                            throw new AbcIllegalParameterException(String.format("field %s 's type_extension" +
                                    ".settings_mode should not be null", dataFieldDto.getName()));
                        }
                        switch (fieldTypeExtensionFile.getSettingsMode()) {
                            case HTTP_RELATIVE_URL:
                                if (ObjectUtils.isEmpty(fieldTypeExtensionFile.getPrefixForHttpRelativeUrl())) {
                                    throw new AbcIllegalParameterException(String.format("field %s 's " +
                                            "type_extension.prefix_for_http_relative_url" +
                                            " should not be null or empty if settings mode is prefix for http " +
                                            "relative url", dataFieldDto.getName()));
                                }
                                break;
                            case FILE_RELATIVE_LOCAL_PATH:
                                if (ObjectUtils.isEmpty(fieldTypeExtensionFile.getPrefixForFileRelativeLocalPath())) {
                                    throw new AbcIllegalParameterException(String.format("field %s 's " +
                                                    "type_extension.prefix_for_file_relative_local_path" +
                                                    " should not be null or empty if settings mode is prefix for file " +
                                                    "relative local path",
                                            dataFieldDto.getName()));
                                }
                                break;
                            case DFS_FILE:
                                if (fieldTypeExtensionFile.getDfsServiceAgentUid() == null) {
                                    throw new AbcIllegalParameterException(String.format("field %s 's " +
                                                    "type_extension.dfs_service_agent_uid" +
                                                    " should not be null if settings mode is DFS file",
                                            dataFieldDto.getName()));
                                }
                                break;
                            default:
                                break;
                        }
                        if (Boolean.TRUE.equals(fieldTypeExtensionFile.getMayContainMultipleItemsInOneField())) {
                            if (ObjectUtils.isEmpty(fieldTypeExtensionFile.getDelimiter())) {
                                throw new AbcIllegalParameterException(String.format("field %s 's " +
                                                "type_extension.delimiter" +
                                                " should not be null if may contain multiple items in one field",
                                        dataFieldDto.getName()));
                            }
                        }
                    }
                }
                break;
                case CALCULATED: {
                    if (dataFieldDto.getTypeExtension() != null
                            && !dataFieldDto.getTypeExtension().isEmpty()) {
                        FieldTypeExtensionCalculated fieldTypeExtensionCalculated =
                                JSONObject.toJavaObject(dataFieldDto.getTypeExtension(), FieldTypeExtensionCalculated.class);
                        if (ObjectUtils.isEmpty(fieldTypeExtensionCalculated.getBuildingLogic())) {
                            throw new AbcIllegalParameterException(String.format("field %s 's " +
                                            "type_extension.building_logic" +
                                            " should not be null",
                                    dataFieldDto.getName()));
                        }
                    }
                }
                break;
                default:
                    break;
            }
        }

        List<DataFieldDo> existingItemDoList = this.dataFieldRepository.findByDataFacetUid(dataFacetUid);
        Map<String, DataFieldDo> existingItemDoMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(existingItemDoList)) {
            existingItemDoList.forEach(existingItemDo -> {
                existingItemDoMap.put(existingItemDo.getName(), existingItemDo);
            });
        }

        List<DataFieldDto> inputItemList = dataFieldDtoList;
        Map<String, DataFieldDto> inputItemMap = new HashMap();
        Map<String, Float> inputItemSequenceMap = new HashMap();
        if (!CollectionUtils.isEmpty(inputItemList)) {
            for (int i = 0; i < inputItemList.size(); i++) {
                DataFieldDto inputItem = inputItemList.get(i);
                inputItemMap.put(inputItem.getName(), inputItem);
                inputItemSequenceMap.put(inputItem.getName(), i * 1.0f);
            }
        }

        List<DataFieldDo> toAddItemDoList = new LinkedList<>();
        List<DataFieldDo> toUpdateItemDoList = new LinkedList<>();
        List<DataFieldDo> toDeleteItemDoList = new LinkedList<>();

        //
        // Step 2, core-processing
        //
        existingItemDoMap.forEach((key, existingItemDo) -> {
            if (inputItemMap.containsKey(key)) {
                // existing 有，input 有
                // 可能是更新

                DataFieldDto inputItem = inputItemMap.get(key);

                boolean requiredUpdate = false;

                if (!ObjectUtils.isEmpty(inputItem.getLabel())
                        && !inputItem.getLabel().equals(existingItemDo.getLabel())) {
                    existingItemDo.setLabel(inputItem.getLabel());
                    existingItemDo.setLabelLogical(inputItem.getLabel());
                    requiredUpdate = true;
                }

                if (!ObjectUtils.isEmpty(inputItem.getDescription())
                        && !inputItem.getDescription().equals(existingItemDo.getDescription())) {
                    existingItemDo.setDescription(inputItem.getDescription());
                    existingItemDo.setDescriptionLogical(inputItem.getDescription());
                    requiredUpdate = true;
                }

                if (inputItem.getType() != null
                        && !inputItem.getType().equals(existingItemDo.getType())) {
                    existingItemDo.setType(inputItem.getType());
                    existingItemDo.setTypeLogical(inputItem.getType());
                    requiredUpdate = true;
                }

                if (inputItem.getTypeExtension() != null
                        && !inputItem.getTypeExtension().equals(existingItemDo.getTypeExtension())) {
                    existingItemDo.setTypeExtension(inputItem.getTypeExtension());
                    requiredUpdate = true;
                }

                if (inputItem.getMeasurementRole() != null
                        && !inputItem.getMeasurementRole().equals(existingItemDo.getMeasurementRole())) {
                    existingItemDo.setMeasurementRole(inputItem.getMeasurementRole());
                    requiredUpdate = true;
                }

                if (!inputItemSequenceMap.get(key).equals(existingItemDo.getSequence())) {
                    existingItemDo.setSequence(inputItemSequenceMap.get(key));
                    existingItemDo.setSequenceLogical(inputItemSequenceMap.get(key));
                    requiredUpdate = true;
                }

                if (requiredUpdate) {
                    BaseDo.update(existingItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    toUpdateItemDoList.add(existingItemDo);
                }

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
                DataFieldDo newItemDo = new DataFieldDo();
                newItemDo.setUid(this.idHelper.getNextDistributedId(DataFieldDo.RESOURCE_NAME));
                newItemDo.setName(inputItem.getName());
                newItemDo.setObjectName(inputItem.getName()
                        .replaceAll("_", "__")
                        .replaceAll("\\s", "_").toLowerCase());
                newItemDo.setLabel(inputItem.getLabel());
                newItemDo.setLabelLogical(inputItem.getLabel());
                newItemDo.setDescription(inputItem.getDescription());
                newItemDo.setDescriptionLogical(inputItem.getDescription());
                newItemDo.setType(inputItem.getType());
                newItemDo.setTypeLogical(inputItem.getType());
                newItemDo.setTypeExtension(inputItem.getTypeExtension());
                newItemDo.setMeasurementRole(inputItem.getMeasurementRole());
                newItemDo.setSequence(inputItemSequenceMap.get(key));
                newItemDo.setSequenceLogical(inputItemSequenceMap.get(key));
                newItemDo.setDataFacetUid(dataFacetUid);
                BaseDo.create(newItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                toAddItemDoList.add(newItemDo);
            }
        });

        if (!CollectionUtils.isEmpty(toAddItemDoList)) {
            this.dataFieldRepository.saveAll(toAddItemDoList);
        }
        if (!CollectionUtils.isEmpty(toUpdateItemDoList)) {
            this.dataFieldRepository.saveAll(toUpdateItemDoList);
        }
        if (!CollectionUtils.isEmpty(toDeleteItemDoList)) {
            this.dataFieldRepository.saveAll(toDeleteItemDoList);
        }

        //
        // Step 3, post-processing
        //


    }

    @Override
    public Page<DataFieldAnotherDto> pagingQueryDataFields(
            Long dataFacetUid,
            Long dataFieldUid,
            String dataFieldName,
            List<DataFieldTypeEnum> dataFieldTypeList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Specification<DataFieldDo> specification = new Specification<DataFieldDo>() {
            @Override
            public Predicate toPredicate(Root<DataFieldDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (dataFacetUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("dataFacetUid"), dataFacetUid));
                }
                if (dataFieldUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("uid"), dataFieldUid));
                }
                if (!ObjectUtils.isEmpty(dataFieldName)) {
                    predicateList.add(criteriaBuilder.like(root.get("name"), "%" + dataFieldName + "%"));
                }
                if (!CollectionUtils.isEmpty(dataFieldTypeList)) {
                    CriteriaBuilder.In<DataFieldTypeEnum> in =
                            criteriaBuilder.in(root.get("type"));
                    dataFieldTypeList.forEach(dataFieldType -> {
                        in.value(dataFieldType);
                    });
                    predicateList.add(in);
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        if (pageable.getSort() == null || pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Order.asc(
                    "sequence")));
        }

        Page<DataFieldDo> itemDoPage = this.dataFieldRepository.findAll(specification, pageable);
        List<DataFieldAnotherDto> content = new ArrayList<>(itemDoPage.getSize());
        itemDoPage.forEach(itemDo -> {
            DataFieldAnotherDto itemDto = new DataFieldAnotherDto();
            BeanUtils.copyProperties(itemDo, itemDto);

            //
            // 避开 JSONObject 作为对象字段时不能配置 SNAKE_CASE 格式输出问题
            //

            content.add(itemDto);
        });
        Page<DataFieldAnotherDto> itemDtoPage = new PageImpl<>(content, pageable, itemDoPage.getTotalElements());
        return itemDtoPage;
    }

    @Override
    public List<DataFieldAnotherDto> listingQueryDataFields(
            Long dataFacetUid,
            Long dataFieldUid,
            String dataFieldName,
            List<DataFieldTypeEnum> dataFieldTypeList,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Specification<DataFieldDo> specification = new Specification<DataFieldDo>() {
            @Override
            public Predicate toPredicate(Root<DataFieldDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (dataFacetUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("dataFacetUid"), dataFacetUid));
                }
                if (dataFieldUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("uid"), dataFieldUid));
                }
                if (!ObjectUtils.isEmpty(dataFieldName)) {
                    predicateList.add(criteriaBuilder.like(root.get("name"), "%" + dataFieldName + "%"));
                }
                if (!CollectionUtils.isEmpty(dataFieldTypeList)) {
                    CriteriaBuilder.In<DataFieldTypeEnum> in =
                            criteriaBuilder.in(root.get("type"));
                    dataFieldTypeList.forEach(dataFieldType -> {
                        in.value(dataFieldType);
                    });
                    predicateList.add(in);
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        if (sort == null || sort.isUnsorted()) {
            sort = Sort.by(Sort.Order.asc("sequence"));
        }
        List<DataFieldDo> itemDoList = this.dataFieldRepository.findAll(specification, sort);
        if (CollectionUtils.isEmpty(itemDoList)) {
            return null;
        }
        List<DataFieldAnotherDto> content = new LinkedList<>();
        itemDoList.forEach(itemDo -> {
            DataFieldAnotherDto itemDto = new DataFieldAnotherDto();
            BeanUtils.copyProperties(itemDo, itemDto);

            //
            // 避开 JSONObject 作为对象字段时不能配置 SNAKE_CASE 格式输出问题
            //
            if (itemDo.getTypeExtension() != null) {
                switch (itemDo.getType()) {
                    case IMAGE:
                    case FILE: {
                        FieldTypeExtensionFile fieldTypeExtensionFile =
                                JSONObject.toJavaObject(itemDo.getTypeExtension(), FieldTypeExtensionFile.class);
                        itemDto.setTypeExtension(fieldTypeExtensionFile);
                    }
                    break;
                    case CALCULATED: {
                        FieldTypeExtensionCalculated fieldTypeExtensionCalculated =
                                JSONObject.toJavaObject(itemDo.getTypeExtension(), FieldTypeExtensionCalculated.class);
                        itemDto.setTypeExtension(fieldTypeExtensionCalculated);
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
    public void initDataFieldsOfDataFacet(
            Long dataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        this.dataFieldHandler.initDataFieldsOfDataFacet(dataFacetUid, operatingUserProfile);
    }

    @Override
    public void reinitDataFieldsOfDataFacet(
            Long dataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        this.dataFieldHandler.reinitDataFieldsOfDataFacet(dataFacetUid, operatingUserProfile);
    }

    @Override
    public void reinitDataFieldsOfDataFacetWithDataColumnDoList(
            Long dataFacetUid,
            List<DataColumnDo> dataColumnDoList,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        this.dataFieldHandler.reinitDataFieldsOfDataFacetWithDataColumnDoList(
                dataFacetUid, dataColumnDoList, operatingUserProfile);
    }

    @Override
    public void deleteAllDataFieldsOfDataFacet(
            Long dataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        this.dataFieldHandler.deleteDataFieldsOfDataFacet(dataFacetUid, operatingUserProfile);
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
    @Subscribe
    public void handleDfsServiceAgentDeletedEvent(DfsServiceAgentDeletedEvent event) {
        LOGGER.info("rcv event:{}", event);

        Long dfsServiceAgentUid = event.getDfsServiceAgentDo().getUid();

        Specification<DataFieldDo> specification = new Specification<DataFieldDo>() {
            @Override
            public Predicate toPredicate(Root<DataFieldDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();

                predicateList.add(criteriaBuilder.isNotNull(root.get("typeExtension")));

                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        List<DataFieldDo> itemDoList = this.dataFieldRepository.findAll(specification, Sort.by(Sort.Order.asc("sequence")));
        if (CollectionUtils.isEmpty(itemDoList)) {
            return;
        }

        List<DataFieldDo> toDeleteList = new LinkedList<>();
        itemDoList.forEach(itemDo -> {
            FieldTypeExtensionFile fieldTypeExtensionFile =
                    JSONObject.toJavaObject(itemDo.getTypeExtension(), FieldTypeExtensionFile.class);
            if (dfsServiceAgentUid.equals(fieldTypeExtensionFile.getDfsServiceAgentUid())) {
                itemDo.setDeleted(Boolean.TRUE);
                BaseDo.update(itemDo, event.getOperatingUserProfile().getUid(), LocalDateTime.now());
                toDeleteList.add(itemDo);
            }
        });

        if (!CollectionUtils.isEmpty(toDeleteList)) {
            this.dataFieldRepository.saveAll(toDeleteList);
        }
    }
}
