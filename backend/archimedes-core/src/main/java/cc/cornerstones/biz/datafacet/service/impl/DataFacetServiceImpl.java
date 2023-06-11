package cc.cornerstones.biz.datafacet.service.impl;

import cc.cornerstones.almond.exceptions.*;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.TreeNode;
import cc.cornerstones.almond.types.UserBriefInformation;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.datafacet.dto.CreateDataFacetDto;
import cc.cornerstones.biz.datafacet.dto.DataFacetDto;
import cc.cornerstones.biz.datafacet.dto.UpdateDataFacetDto;
import cc.cornerstones.biz.datafacet.entity.DataFacetDo;
import cc.cornerstones.biz.datafacet.entity.FilteringDataFieldDo;
import cc.cornerstones.biz.datafacet.persistence.DataFacetRepository;
import cc.cornerstones.biz.datafacet.service.assembly.DataFieldHandler;
import cc.cornerstones.biz.datafacet.service.inf.DataFacetService;
import cc.cornerstones.biz.datafacet.service.inf.DataFieldService;
import cc.cornerstones.biz.datafacet.share.types.FilteringFieldCascadingSettings;
import cc.cornerstones.biz.datafacet.share.types.FilteringFieldDefaultValueSettings;
import cc.cornerstones.biz.datafacet.share.types.FilteringFieldOptionalValueSettings;
import cc.cornerstones.biz.datasource.dto.DataSourceSimpleDto;
import cc.cornerstones.biz.datasource.entity.DataSourceDo;
import cc.cornerstones.biz.datasource.persistence.DataSourceRepository;
import cc.cornerstones.biz.datasource.service.inf.DataSourceService;
import cc.cornerstones.biz.datatable.dto.DataTableSimpleDto;
import cc.cornerstones.biz.datatable.entity.DataTableDo;
import cc.cornerstones.biz.datatable.persistence.DataTableRepository;
import cc.cornerstones.biz.share.assembly.ResourceReferenceManager;
import cc.cornerstones.biz.share.event.*;
import cc.cornerstones.biz.share.types.ResourceReferenceHandler;
import com.alibaba.fastjson.JSONObject;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DataFacetServiceImpl implements DataFacetService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataFacetServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private DataFacetRepository dataFacetRepository;

    @Autowired
    private DataFieldHandler dataFieldHandler;

    @Autowired
    private DataTableRepository dataTableRepository;

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private DataSourceService dataSourceService;

    @Autowired
    private DataFieldService dataFieldService;

    @Autowired
    private UserService userService;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Transactional(rollbackFor = Exception.class)
    @Override
    public DataFacetDto createDataFacet(
            Long dataTableUid,
            CreateDataFacetDto createDataFacetDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // step 1, pre-processing
        //

        // step 1.1 check data table
        DataTableDo dataTableDo = this.dataTableRepository.findByUid(dataTableUid);
        if (dataTableDo == null) {
            throw new AbcIllegalParameterException(String.format("%s::uid=%d", DataTableDo.RESOURCE_SYMBOL,
                    dataTableUid));
        }

        // step 1.2 check data source
        DataSourceDo dataSourceDo = this.dataSourceRepository.findByUid(dataTableDo.getDataSourceUid());
        if (dataSourceDo == null) {
            throw new AbcIllegalParameterException(String.format("%s::uid=%d", DataSourceDo.RESOURCE_SYMBOL,
                    dataTableDo.getDataSourceUid()));
        }

        // step 1.3, check if duplicate
        boolean existsDuplicate = this.dataFacetRepository.existsByName(createDataFacetDto.getName());
        if (existsDuplicate) {
            throw new AbcResourceDuplicateException(String.format("%s::name:%s", DataFacetDo.RESOURCE_SYMBOL,
                    createDataFacetDto.getName()));
        }

        //
        // step 2, core-processing
        //

        // step 2.1, create data facet
        DataFacetDo dataFacetDo = new DataFacetDo();
        dataFacetDo.setUid(this.idHelper.getNextDistributedId(DataFacetDo.RESOURCE_NAME));
        dataFacetDo.setName(createDataFacetDto.getName());
        dataFacetDo.setObjectName(createDataFacetDto.getName()
                .replaceAll("_", "__")
                .replaceAll("\\s", "_").toLowerCase());
        dataFacetDo.setDescription(createDataFacetDto.getDescription());
        dataFacetDo.setBuildNumber(System.currentTimeMillis());
        dataFacetDo.setDataTableUid(dataTableDo.getUid());
        dataFacetDo.setDataSourceUid(dataTableDo.getDataSourceUid());
        BaseDo.create(dataFacetDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dataFacetRepository.save(dataFacetDo);

        // step 2.2, 初始化 data facet 的 data fields, according to data columns of the data table
        this.dataFieldService.initDataFieldsOfDataFacet(dataFacetDo.getUid(), operatingUserProfile);

        //
        // step 3, post-processing
        //

        // step 3.1, event post
        DataFacetCreatedEvent dataFacetCreatedEvent = new DataFacetCreatedEvent();
        dataFacetCreatedEvent.setDataFacetDo(dataFacetDo);
        dataFacetCreatedEvent.setOperatingUserProfile(operatingUserProfile);
        this.eventBusManager.send(dataFacetCreatedEvent);

        // Step 3.2, build return result
        DataFacetDto dataFacetDto = new DataFacetDto();
        BeanUtils.copyProperties(dataFacetDo, dataFacetDto);

        DataTableSimpleDto dataTableSimpleDto = new DataTableSimpleDto();
        dataTableSimpleDto.setUid(dataTableDo.getUid());
        dataTableSimpleDto.setName(dataTableDo.getName());
        dataTableSimpleDto.setObjectName(dataTableDo.getObjectName());
        dataTableSimpleDto.setContextPath(dataTableDo.getContextPath());
        dataFacetDto.setDataTable(dataTableSimpleDto);

        DataSourceSimpleDto dataSourceSimpleDto = new DataSourceSimpleDto();
        dataSourceSimpleDto.setUid(dataSourceDo.getUid());
        dataSourceSimpleDto.setName(dataSourceDo.getName());
        dataSourceSimpleDto.setObjectName(dataSourceDo.getObjectName());
        dataFacetDto.setDataSource(dataSourceSimpleDto);

        return dataFacetDto;
    }

    @Override
    public DataFacetDto getDataFacet(
            Long dataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        DataFacetDo dataFacetDo = this.dataFacetRepository.findByUid(dataFacetUid);
        if (dataFacetDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataFacetDo.RESOURCE_SYMBOL, dataFacetUid));
        }

        DataTableDo dataTableDo = this.dataTableRepository.findByUid(dataFacetDo.getDataTableUid());
        if (dataTableDo == null) {
            throw new AbcIllegalParameterException(String.format("%s::uid=%d", DataTableDo.RESOURCE_SYMBOL,
                    dataFacetDo.getDataTableUid()));
        }

        DataSourceDo dataSourceDo = this.dataSourceRepository.findByUid(dataFacetDo.getDataSourceUid());
        if (dataSourceDo == null) {
            throw new AbcIllegalParameterException(String.format("%s::uid=%d", DataSourceDo.RESOURCE_SYMBOL,
                    dataFacetDo.getDataSourceUid()));
        }

        DataFacetDto dataFacetDto = new DataFacetDto();
        BeanUtils.copyProperties(dataFacetDo, dataFacetDto);

        DataTableSimpleDto dataTableSimpleDto = new DataTableSimpleDto();
        dataTableSimpleDto.setUid(dataTableDo.getUid());
        dataTableSimpleDto.setName(dataTableDo.getName());
        dataTableSimpleDto.setContextPath(dataTableDo.getContextPath());
        dataFacetDto.setDataTable(dataTableSimpleDto);

        DataSourceSimpleDto dataSourceSimpleDto = new DataSourceSimpleDto();
        dataSourceSimpleDto.setUid(dataSourceDo.getUid());
        dataSourceSimpleDto.setName(dataSourceDo.getName());
        dataFacetDto.setDataSource(dataSourceSimpleDto);

        return dataFacetDto;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public DataFacetDto updateDataFacet(
            Long dataFacetUid,
            UpdateDataFacetDto updateDataFacetDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // step 1, pre-processing
        //
        // step 1.1, check if exists
        DataFacetDo dataFacetDo = this.dataFacetRepository.findByUid(dataFacetUid);
        if (dataFacetDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataFacetDo.RESOURCE_SYMBOL, dataFacetUid));
        }

        DataTableDo dataTableDo = this.dataTableRepository.findByUid(dataFacetDo.getDataTableUid());
        if (dataTableDo == null) {
            throw new AbcIllegalParameterException(String.format("%s::uid=%d", DataTableDo.RESOURCE_SYMBOL,
                    dataFacetDo.getDataTableUid()));
        }

        DataSourceDo dataSourceDo = this.dataSourceRepository.findByUid(dataFacetDo.getDataSourceUid());
        if (dataSourceDo == null) {
            throw new AbcIllegalParameterException(String.format("%s::uid=%d", DataSourceDo.RESOURCE_SYMBOL,
                    dataFacetDo.getDataSourceUid()));
        }

        // step 1.2, check if duplicate
        if (!ObjectUtils.isEmpty(updateDataFacetDto.getName())
                && !updateDataFacetDto.getName().equalsIgnoreCase(dataFacetDo.getName())) {
            boolean existsDuplicate = this.dataFacetRepository.existsByName(updateDataFacetDto.getName());
            if (existsDuplicate) {
                throw new AbcResourceDuplicateException(String.format("%s::name:%s", DataFacetDo.RESOURCE_SYMBOL,
                        updateDataFacetDto.getName()));
            }
        }

        //
        // step 2, core-processing
        //
        if (!ObjectUtils.isEmpty(updateDataFacetDto.getName())
                && !updateDataFacetDto.getName().equals(dataFacetDo.getName())) {
            dataFacetDo.setName(updateDataFacetDto.getName());
            dataFacetDo.setObjectName(updateDataFacetDto.getName()
                    .replaceAll("_", "__")
                    .replaceAll("\\s", "_")
                    .toLowerCase());
        }
        if (updateDataFacetDto.getDescription() != null) {
            if (!updateDataFacetDto.getDescription().equals(dataFacetDo.getDescription())) {
                dataFacetDo.setDescription(updateDataFacetDto.getDescription());
            }
        }

        dataFacetDo.setBuildNumber(System.currentTimeMillis());
        this.dataFacetRepository.save(dataFacetDo);

        //
        // step 3, post-processing
        //
        DataFacetDto dataFacetDto = new DataFacetDto();
        BeanUtils.copyProperties(dataFacetDo, dataFacetDto);

        DataTableSimpleDto dataTableSimpleDto = new DataTableSimpleDto();
        dataTableSimpleDto.setUid(dataTableDo.getUid());
        dataTableSimpleDto.setName(dataTableDo.getName());
        dataTableSimpleDto.setObjectName(dataTableDo.getObjectName());
        dataTableSimpleDto.setContextPath(dataTableDo.getContextPath());
        dataFacetDto.setDataTable(dataTableSimpleDto);

        DataSourceSimpleDto dataSourceSimpleDto = new DataSourceSimpleDto();
        dataSourceSimpleDto.setUid(dataSourceDo.getUid());
        dataSourceSimpleDto.setName(dataSourceDo.getName());
        dataSourceSimpleDto.setObjectName(dataSourceDo.getObjectName());
        dataFacetDto.setDataSource(dataSourceSimpleDto);

        return dataFacetDto;
    }

    @Override
    public List<String> listAllReferencesToDataFacet(
            Long dataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return null;
    }

    @Override
    public void deleteDataFacet(
            Long dataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        DataFacetDo dataFacetDo = this.dataFacetRepository.findByUid(dataFacetUid);
        if (dataFacetDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataFacetDo.RESOURCE_SYMBOL, dataFacetUid));
        }

        deleteDataFacet(dataFacetDo, operatingUserProfile);
    }

    @Transactional(rollbackFor = Exception.class)
    private void deleteDataFacet(
            DataFacetDo dataFacetDo,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        // logical delete data field(s)
        dataFieldService.deleteAllDataFieldsOfDataFacet(dataFacetDo.getUid(), operatingUserProfile);

        // logical delete data facet
        dataFacetDo.setDeleted(Boolean.TRUE);
        BaseDo.update(dataFacetDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dataFacetRepository.save(dataFacetDo);

        // event post
        DataFacetDeletedEvent dataFacetDeletedEvent = new DataFacetDeletedEvent();
        dataFacetDeletedEvent.setDataFacetDo(dataFacetDo);
        dataFacetDeletedEvent.setOperatingUserProfile(operatingUserProfile);
        this.eventBusManager.send(dataFacetDeletedEvent);
    }

    @Override
    public Page<DataFacetDto> pagingQueryDataFacets(
            Long uid,
            String name,
            String description,
            List<Long> userUidListOfLastModifiedBy,
            List<String> lastModifiedTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // step 1, pre-processing
        //

        //
        // step 2, core-processing
        //
        Specification<DataFacetDo> specification = new Specification<DataFacetDo>() {
            @Override
            public Predicate toPredicate(Root<DataFacetDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (uid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("uid"), uid));
                }
                if (!ObjectUtils.isEmpty(name)) {
                    predicateList.add(criteriaBuilder.like(root.get("name"), "%" + name + "%"));
                }
                if (!ObjectUtils.isEmpty(description)) {
                    predicateList.add(criteriaBuilder.like(root.get("description"), "%" + description + "%"));
                }
                if (!CollectionUtils.isEmpty(userUidListOfLastModifiedBy)) {
                    CriteriaBuilder.In<Long> in = criteriaBuilder.in(root.get("lastModifiedBy"));
                    userUidListOfLastModifiedBy.forEach(item -> {
                        in.value(item);
                    });
                    predicateList.add(in);
                }
                if (!CollectionUtils.isEmpty(lastModifiedTimestampAsStringList)) {
                    if (lastModifiedTimestampAsStringList.size() == 2) {
                        LocalDateTime dateTime0 = LocalDateTime.parse(lastModifiedTimestampAsStringList.get(0),
                                dateTimeFormatter);
                        LocalDateTime dateTime1 = LocalDateTime.parse(lastModifiedTimestampAsStringList.get(1), dateTimeFormatter);
                        if (dateTime0.isAfter(dateTime1)) {
                            predicateList.add(criteriaBuilder.between(root.get(BaseDo.LAST_MODIFIED_TIMESTAMP_FIELD_NAME),
                                    dateTime1, dateTime0));
                        } else {
                            predicateList.add(criteriaBuilder.between(root.get(BaseDo.LAST_MODIFIED_TIMESTAMP_FIELD_NAME),
                                    dateTime0, dateTime1));
                        }
                    } else if (lastModifiedTimestampAsStringList.size() == 1) {
                        LocalDateTime dateTime0 = LocalDateTime.parse(lastModifiedTimestampAsStringList.get(0), dateTimeFormatter);
                        predicateList.add(criteriaBuilder.equal(root.get(BaseDo.LAST_MODIFIED_TIMESTAMP_FIELD_NAME),
                                dateTime0));
                    } else {
                        CriteriaBuilder.In<LocalDateTime> in =
                                criteriaBuilder.in(root.get(BaseDo.LAST_MODIFIED_TIMESTAMP_FIELD_NAME));
                        lastModifiedTimestampAsStringList.forEach(createdTimestampAsString -> {
                            LocalDateTime dateTime0 = LocalDateTime.parse(createdTimestampAsString, dateTimeFormatter);
                            in.value(dateTime0);
                        });
                        predicateList.add(in);
                    }
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        Page<DataFacetDo> itemDoPage = this.dataFacetRepository.findAll(specification, pageable);

        //
        // step 3, post-processing
        //

        //
        // step 3.1, 为 created by, last modified by 补充 user brief information
        //
        List<Long> userUidList = new LinkedList<>();
        itemDoPage.forEach(itemDo -> {
            if (itemDo.getCreatedBy() != null && !userUidList.contains(itemDo.getCreatedBy())) {
                userUidList.add(itemDo.getCreatedBy());
            }
            if (itemDo.getLastModifiedBy() != null && !userUidList.contains(itemDo.getLastModifiedBy())) {
                userUidList.add(itemDo.getLastModifiedBy());
            }
        });

        Map<Long, UserBriefInformation> userBriefInformationMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(userUidList)) {
            List<UserBriefInformation> userBriefInformationList =
                    this.userService.listingUserBriefInformation(userUidList, operatingUserProfile);
            if (!CollectionUtils.isEmpty(userBriefInformationList)) {
                userBriefInformationList.forEach(userBriefInformation -> {
                    userBriefInformationMap.put(userBriefInformation.getUid(), userBriefInformation);
                });
            }
        }

        //
        // step 3.2, 构造返回内容
        //

        List<DataFacetDto> content = new ArrayList<>(itemDoPage.getSize());
        itemDoPage.forEach(itemDo -> {
            DataFacetDto itemDto = new DataFacetDto();
            BeanUtils.copyProperties(itemDo, itemDto);

            // 为 created by, last modified by 补充 user brief information
            itemDto.setCreatedByUser(userBriefInformationMap.get(itemDo.getCreatedBy()));
            itemDto.setLastModifiedByUser(userBriefInformationMap.get(itemDo.getLastModifiedBy()));

            content.add(itemDto);
        });
        Page<DataFacetDto> itemDtoPage = new PageImpl<>(content, pageable, itemDoPage.getTotalElements());
        return itemDtoPage;
    }

    @Override
    public List<DataFacetDto> listingQueryDataFacets(
            Long dataFacetUid,
            String dataFacetName,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Specification<DataFacetDo> specification = new Specification<DataFacetDo>() {
            @Override
            public Predicate toPredicate(Root<DataFacetDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (dataFacetUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("uid"), dataFacetUid));
                }
                if (!ObjectUtils.isEmpty(dataFacetName)) {
                    predicateList.add(criteriaBuilder.like(root.get("name"), "%" + dataFacetName + "%"));
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        List<DataFacetDo> itemDoList = this.dataFacetRepository.findAll(specification, sort);
        if (CollectionUtils.isEmpty(itemDoList)) {
            return null;
        }
        List<DataFacetDto> content = new LinkedList<>();
        itemDoList.forEach(itemDo -> {
            DataFacetDto itemDto = new DataFacetDto();
            BeanUtils.copyProperties(itemDo, itemDto);
            content.add(itemDto);
        });
        return content;
    }

    @Override
    public List<TreeNode> treeListingAllDataFacets(
            Long dataSourceUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        DataSourceDo dataSourceDo = this.dataSourceRepository.findByUid(dataSourceUid);
        if (dataSourceDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataSourceDo.RESOURCE_SYMBOL,
                    dataSourceUid));
        }
        List<DataFacetDo> dataFacetDoList = this.dataFacetRepository.findByDataSourceUid(dataSourceUid);
        if (CollectionUtils.isEmpty(dataFacetDoList)) {
            return null;
        }

        List<Long> dataTableUidList = new LinkedList<>();
        dataFacetDoList.forEach(dataFacetDo -> {
            if (!dataTableUidList.contains(dataFacetDo.getDataTableUid())) {
                dataTableUidList.add(dataFacetDo.getDataTableUid());
            }
        });
        if (CollectionUtils.isEmpty(dataTableUidList)) {
            return null;
        }
        List<DataTableDo> dataTableDoList = this.dataTableRepository.findByUidIn(dataTableUidList);
        if (CollectionUtils.isEmpty(dataTableDoList)) {
            return null;
        }

        List<TreeNode> result = new ArrayList<>(1);
        result.add(treeListing(dataSourceDo, dataTableDoList, dataFacetDoList));
        return result;
    }

    @Override
    public List<TreeNode> treeListingAllDataFacets(
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Map<Long, DataSourceDo> dataSourceUidAndDataSourceDoMap = new HashMap<>();
        this.dataSourceRepository.findAll().forEach(dataSourceDo -> {
            dataSourceUidAndDataSourceDoMap.put(dataSourceDo.getUid(), dataSourceDo);
        });
        Map<Long, List<DataFacetDo>> dataSourceUidAndDataFacetDoListMap = new HashMap<>();
        this.dataFacetRepository.findAll().forEach(dataFacetDo -> {
            if (!dataSourceUidAndDataFacetDoListMap.containsKey(dataFacetDo.getDataSourceUid())) {
                dataSourceUidAndDataFacetDoListMap.put(dataFacetDo.getDataSourceUid(), new LinkedList<>());
            }
            dataSourceUidAndDataFacetDoListMap.get(dataFacetDo.getDataSourceUid()).add(dataFacetDo);
        });

        List<TreeNode> result = new ArrayList<>(dataSourceUidAndDataSourceDoMap.size());
        dataSourceUidAndDataFacetDoListMap.forEach((dataSourceUid, dataFacetDoList) -> {
            DataSourceDo dataSourceDo = dataSourceUidAndDataSourceDoMap.get(dataSourceUid);
            if (dataSourceDo == null) {
                LOGGER.warn("cannot find data source:{}, but it is referenced in data tables", dataSourceUid);
                return;
            }

            List<Long> dataTableUidList = new LinkedList<>();
            dataFacetDoList.forEach(dataFacetDo -> {
                if (!dataTableUidList.contains(dataFacetDo.getDataTableUid())) {
                    dataTableUidList.add(dataFacetDo.getDataTableUid());
                }
            });
            if (CollectionUtils.isEmpty(dataTableUidList)) {
                return;
            }
            List<DataTableDo> dataTableDoList = this.dataTableRepository.findByUidIn(dataTableUidList);
            if (CollectionUtils.isEmpty(dataTableDoList)) {
                return;
            }

            result.add(treeListing(dataSourceDo, dataTableDoList, dataFacetDoList));
        });
        return result;
    }

    @Override
    public List<TreeNode> treeListingAllDataObjects(
            Long dataSourceUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        DataSourceDo dataSourceDo = this.dataSourceRepository.findByUid(dataSourceUid);
        if (dataSourceDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataSourceDo.RESOURCE_SYMBOL,
                    dataSourceUid));
        }
        List<DataFacetDo> dataFacetDoList = this.dataFacetRepository.findByDataSourceUid(dataSourceUid);
        if (CollectionUtils.isEmpty(dataFacetDoList)) {
            return null;
        }

        List<DataTableDo> dataTableDoList = this.dataTableRepository.findByDataSourceUid(dataSourceUid);
        if (CollectionUtils.isEmpty(dataTableDoList)) {
            return null;
        }

        List<TreeNode> result = new ArrayList<>(1);
        result.add(treeListing(dataSourceDo, dataTableDoList, dataFacetDoList));
        return result;
    }

    @Override
    public List<TreeNode> treeListingAllDataObjects(
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Map<Long, DataSourceDo> dataSourceUidAndDataSourceDoMap = new HashMap<>();
        this.dataSourceRepository.findAll().forEach(dataSourceDo -> {
            dataSourceUidAndDataSourceDoMap.put(dataSourceDo.getUid(), dataSourceDo);
        });

        //
        Map<Long, List<DataFacetDo>> dataSourceUidAndDataFacetDoListMap = new HashMap<>();
        this.dataFacetRepository.findAll().forEach(dataFacetDo -> {
            if (!dataSourceUidAndDataFacetDoListMap.containsKey(dataFacetDo.getDataSourceUid())) {
                dataSourceUidAndDataFacetDoListMap.put(dataFacetDo.getDataSourceUid(), new LinkedList<>());
            }
            dataSourceUidAndDataFacetDoListMap.get(dataFacetDo.getDataSourceUid()).add(dataFacetDo);
        });

        //
        Map<Long, List<DataTableDo>> dataSourceUidAndDataTableDoListMap = new HashMap<>();
        dataSourceUidAndDataSourceDoMap.keySet().forEach(dataSourceUid -> {
            List<DataTableDo> dataTableDoList = this.dataTableRepository.findByDataSourceUid(dataSourceUid);
            dataSourceUidAndDataTableDoListMap.put(dataSourceUid, dataTableDoList);
        });


        List<TreeNode> result = new ArrayList<>(dataSourceUidAndDataSourceDoMap.size());
        dataSourceUidAndDataSourceDoMap.forEach((dataSourceUid, dataSourceDo) -> {
            result.add(
                    treeListing(
                            dataSourceDo,
                            dataSourceUidAndDataTableDoListMap.get(dataSourceUid),
                            dataSourceUidAndDataFacetDoListMap.get(dataSourceUid)));
        });
        return result;
    }

    private TreeNode treeListing(
            DataSourceDo dataSourceDo,
            List<DataTableDo> dataTableDoList,
            List<DataFacetDo> dataFacetDoList) {
        //
        // Step 1, pre-processing
        //
        TreeNode rootTreeNode = new TreeNode();
        rootTreeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
        rootTreeNode.setUid(dataSourceDo.getUid());
        rootTreeNode.setName(dataSourceDo.getName());
        rootTreeNode.setDescription(dataSourceDo.getDescription());
        rootTreeNode.setType("data_source");
        rootTreeNode.setTags(new HashMap<>());
        rootTreeNode.getTags().put("type", dataSourceDo.getType());
        rootTreeNode.setChildren(new LinkedList<>());

        TreeNode directTreeNode = new TreeNode();
        directTreeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
        directTreeNode.setName("Direct");
        directTreeNode.setType("direct");
        directTreeNode.setChildren(new LinkedList<>());
        rootTreeNode.getChildren().add(directTreeNode);

        TreeNode indirectTreeNode = new TreeNode();
        indirectTreeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
        indirectTreeNode.setName("Indirect");
        indirectTreeNode.setType("indirect");
        indirectTreeNode.setChildren(new LinkedList<>());
        rootTreeNode.getChildren().add(indirectTreeNode);

        if (CollectionUtils.isEmpty(dataTableDoList)) {
            return rootTreeNode;
        }

        //
        // Step 2, core-processing
        //

        // 先把这些 data tables 构成 tree nodes

        Map<Long, TreeNode> allDataTableTreeNodeMap = new HashMap();
        Map<String, TreeNode> allContextPathTreeNodeMap = new HashMap<>();
        dataTableDoList.forEach(dataTableDo -> {
            TreeNode treeNodeOfDataTable = new TreeNode();
            treeNodeOfDataTable.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
            treeNodeOfDataTable.setUid(dataTableDo.getUid());
            treeNodeOfDataTable.setName(dataTableDo.getName());
            treeNodeOfDataTable.setDescription(dataTableDo.getDescription());
            treeNodeOfDataTable.setType("data_table");
            treeNodeOfDataTable.setTags(new HashMap<>());
            treeNodeOfDataTable.getTags().put("type", dataTableDo.getType());
            allDataTableTreeNodeMap.put(dataTableDo.getUid(), treeNodeOfDataTable);

            if (CollectionUtils.isEmpty(dataTableDo.getContextPath())) {
                // without context path, such as (indirect) data table

                switch (dataTableDo.getType()) {
                    case INDIRECT_TABLE:
                        indirectTreeNode.getChildren().add(treeNodeOfDataTable);
                        break;
                    default:
                        directTreeNode.getChildren().add(treeNodeOfDataTable);
                        break;
                }
            } else {
                // with context path, such as (direct) data table
                // 创建 context path tree node
                StringBuilder contextPath = new StringBuilder();
                for (int i = 0; i < dataTableDo.getContextPath().size(); i++) {
                    // 记住上一级 context path
                    String upperContextPath = null;
                    if (i > 0) {
                        upperContextPath = contextPath.toString();
                    }

                    // 设置最新的 context path
                    if (contextPath.length() > 0) {
                        contextPath.append("/").append(dataTableDo.getContextPath().get(i));
                    } else {
                        contextPath.append(dataTableDo.getContextPath().get(i));
                    }

                    if (!allContextPathTreeNodeMap.containsKey(contextPath.toString())) {
                        TreeNode treeNodeOfContextPath = new TreeNode();
                        treeNodeOfContextPath.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
                        treeNodeOfContextPath.setName(dataTableDo.getContextPath().get(i));
                        treeNodeOfContextPath.setType("context_path");
                        treeNodeOfContextPath.setTags(new HashMap<>());
                        treeNodeOfContextPath.getTags().put("context_path", contextPath);
                        allContextPathTreeNodeMap.put(contextPath.toString(), treeNodeOfContextPath);

                        if (upperContextPath == null) {
                            switch (dataTableDo.getType()) {
                                case INDIRECT_TABLE:
                                    indirectTreeNode.getChildren().add(treeNodeOfContextPath);
                                    break;
                                default:
                                    directTreeNode.getChildren().add(treeNodeOfContextPath);
                                    break;
                            }
                        } else {
                            TreeNode treeNodeOfUpperContextPath = allContextPathTreeNodeMap.get(upperContextPath);
                            if (treeNodeOfUpperContextPath.getChildren() == null) {
                                treeNodeOfUpperContextPath.setChildren(new LinkedList<>());
                            }
                            treeNodeOfUpperContextPath.getChildren().add(treeNodeOfContextPath);
                        }
                    }
                }

                // 关联 context path tree node w/ data table tree node
                TreeNode treeNodeOfContextPath = allContextPathTreeNodeMap.get(contextPath.toString());
                if (treeNodeOfContextPath.getChildren() == null) {
                    treeNodeOfContextPath.setChildren(new LinkedList<>());
                }
                treeNodeOfContextPath.getChildren().add(treeNodeOfDataTable);
            }
        });

        if (!CollectionUtils.isEmpty(dataFacetDoList)) {
            dataFacetDoList.forEach(dataFacetDo -> {
                TreeNode treeNodeOfDataFacet = new TreeNode();
                treeNodeOfDataFacet.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
                treeNodeOfDataFacet.setUid(dataFacetDo.getUid());
                treeNodeOfDataFacet.setName(dataFacetDo.getName());
                treeNodeOfDataFacet.setDescription(dataFacetDo.getDescription());
                treeNodeOfDataFacet.setType("data_facet");

                if (allDataTableTreeNodeMap.containsKey(dataFacetDo.getDataTableUid())) {
                    if (allDataTableTreeNodeMap.get(dataFacetDo.getDataTableUid()).getChildren() == null) {
                        allDataTableTreeNodeMap.get(dataFacetDo.getDataTableUid()).setChildren(new LinkedList<>());
                    }
                    allDataTableTreeNodeMap.get(dataFacetDo.getDataTableUid()).getChildren().add(treeNodeOfDataFacet);
                }
            });
        }

        return rootTreeNode;
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
    public void handleDirectDataTableDeletedEvent(DirectDataTableDeletedEvent event) {
        LOGGER.info("rcv event:{}", event);

        List<DataFacetDo> dataFacetDoList = this.dataFacetRepository.findByDataTableUid(event.getDataTableDo().getUid());
        if (CollectionUtils.isEmpty(dataFacetDoList)) {
            return;
        }

        dataFacetDoList.forEach(dataFacetDo -> {
            deleteDataFacet(dataFacetDo, event.getOperatingUserProfile());
        });
    }

    /**
     * event handler
     *
     * @param event
     */
    @Subscribe
    public void handleDirectDataTableStructureChangedEvent(DirectDataTableStructureChangedEvent event) {
        LOGGER.info("rcv event:{}", event);

        List<DataFacetDo> dataFacetDoList = this.dataFacetRepository.findByDataTableUid(event.getDataTableDo().getUid());
        if (CollectionUtils.isEmpty(dataFacetDoList)) {
            return;
        }

        dataFacetDoList.forEach(dataFacetDo -> {
            //
            dataFacetDo.setBuildNumber(System.currentTimeMillis());
            BaseDo.update(dataFacetDo, event.getOperatingUserProfile().getUid(), LocalDateTime.now());
            this.dataFacetRepository.save(dataFacetDo);

            //
            dataFieldService.reinitDataFieldsOfDataFacetWithDataColumnDoList(
                    dataFacetDo.getUid(),
                    event.getDataColumnDoList(),
                    event.getOperatingUserProfile());

            // event post
            DataFacetChangedEvent dataFacetChangedEvent = new DataFacetChangedEvent();
            dataFacetChangedEvent.setDataFacetDo(dataFacetDo);
            dataFacetChangedEvent.setOperatingUserProfile(event.getOperatingUserProfile());
            eventBusManager.send(dataFacetChangedEvent);
        });
    }

    /**
     * event handler
     *
     * @param event
     */
    @Subscribe
    public void handleDirectDataTableStructureRefreshedEvent(DirectDataTableStructureRefreshedEvent event) {
        LOGGER.info("rcv event:{}", event);

        List<DataFacetDo> dataFacetDoList = this.dataFacetRepository.findByDataTableUid(event.getDataTableDo().getUid());
        if (CollectionUtils.isEmpty(dataFacetDoList)) {
            return;
        }

        dataFacetDoList.forEach(dataFacetDo -> {
            //
            dataFacetDo.setBuildNumber(System.currentTimeMillis());
            BaseDo.update(dataFacetDo, event.getOperatingUserProfile().getUid(), LocalDateTime.now());
            this.dataFacetRepository.save(dataFacetDo);

            //
            dataFieldService.reinitDataFieldsOfDataFacetWithDataColumnDoList(
                    dataFacetDo.getUid(),
                    event.getDataColumnDoList(),
                    event.getOperatingUserProfile());

            // event post
            DataFacetChangedEvent dataFacetChangedEvent = new DataFacetChangedEvent();
            dataFacetChangedEvent.setDataFacetDo(dataFacetDo);
            dataFacetChangedEvent.setOperatingUserProfile(event.getOperatingUserProfile());
            eventBusManager.send(dataFacetChangedEvent);
        });
    }

    /**
     * event handler
     *
     * @param event
     */
    @Subscribe
    public void handleIndirectDataTableDeletedEvent(IndirectDataTableDeletedEvent event) {
        LOGGER.info("rcv event:{}", event);

        List<DataFacetDo> dataFacetDoList = this.dataFacetRepository.findByDataTableUid(event.getDataTableDo().getUid());
        if (CollectionUtils.isEmpty(dataFacetDoList)) {
            return;
        }

        dataFacetDoList.forEach(dataFacetDo -> {
            deleteDataFacet(dataFacetDo, event.getOperatingUserProfile());
        });
    }

    /**
     * event handler
     *
     * @param event
     */
    @Subscribe
    public void handleIndirectDataTableStructureChangedEvent(IndirectDataTableStructureChangedEvent event) {
        LOGGER.info("rcv event:{}", event);

        List<DataFacetDo> dataFacetDoList = this.dataFacetRepository.findByDataTableUid(event.getDataTableDo().getUid());
        if (CollectionUtils.isEmpty(dataFacetDoList)) {
            return;
        }

        dataFacetDoList.forEach(dataFacetDo -> {
            // data facet
            dataFacetDo.setBuildNumber(System.currentTimeMillis());
            BaseDo.update(dataFacetDo, event.getOperatingUserProfile().getUid(), LocalDateTime.now());
            this.dataFacetRepository.save(dataFacetDo);

            // data table
            dataFieldService.reinitDataFieldsOfDataFacetWithDataColumnDoList(
                    dataFacetDo.getUid(),
                    event.getDataColumnDoList(),
                    event.getOperatingUserProfile());

            // event post
            DataFacetChangedEvent dataFacetChangedEvent = new DataFacetChangedEvent();
            dataFacetChangedEvent.setDataFacetDo(dataFacetDo);
            dataFacetChangedEvent.setOperatingUserProfile(event.getOperatingUserProfile());
            eventBusManager.send(dataFacetChangedEvent);
        });
    }

    /**
     * event handler
     *
     * @param event
     */
    @Subscribe
    public void handleIndirectDataTableStructureRefreshedEvent(IndirectDataTableStructureRefreshedEvent event) {
        LOGGER.info("rcv event:{}", event);

        List<DataFacetDo> dataFacetDoList = this.dataFacetRepository.findByDataTableUid(event.getDataTableDo().getUid());
        if (CollectionUtils.isEmpty(dataFacetDoList)) {
            return;
        }

        dataFacetDoList.forEach(dataFacetDo -> {
            // data facet
            dataFacetDo.setBuildNumber(System.currentTimeMillis());
            BaseDo.update(dataFacetDo, event.getOperatingUserProfile().getUid(), LocalDateTime.now());
            this.dataFacetRepository.save(dataFacetDo);

            // data table
            dataFieldService.reinitDataFieldsOfDataFacetWithDataColumnDoList(
                    dataFacetDo.getUid(),
                    event.getDataColumnDoList(),
                    event.getOperatingUserProfile());

            // event post
            DataFacetChangedEvent dataFacetChangedEvent = new DataFacetChangedEvent();
            dataFacetChangedEvent.setDataFacetDo(dataFacetDo);
            dataFacetChangedEvent.setOperatingUserProfile(event.getOperatingUserProfile());
            eventBusManager.send(dataFacetChangedEvent);
        });
    }

    @ResourceReferenceHandler(name = "data facet")
    public List<String> checkResourceReference(
            ResourceReferenceManager.ResourceCategoryEnum resourceCategory,
            Long resourceUid,
            String resourceName) throws Exception {
        switch (resourceCategory) {
            case DATA_SOURCE: {
                Long dataSourceUid = resourceUid;

                Specification<DataFacetDo> specification = new Specification<DataFacetDo>() {
                    @Override
                    public Predicate toPredicate(Root<DataFacetDo> root, CriteriaQuery<?> query,
                                                 CriteriaBuilder criteriaBuilder) {
                        List<Predicate> predicateList = new ArrayList<>();

                        predicateList.add(criteriaBuilder.equal(root.get("dataSourceUid"), dataSourceUid));

                        return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
                    }
                };
                List<DataFacetDo> dataFacetDoList = this.dataFacetRepository.findAll(specification,
                        Sort.by(Sort.Order.asc(BaseDo.ID_FIELD_NAME)));
                if (!CollectionUtils.isEmpty(dataFacetDoList)) {
                    // resource reference found
                    List<String> result = new LinkedList<>();
                    dataFacetDoList.forEach(dataFacetDo -> {
                        result.add(String.format(
                                "[%s] %s (%d)",
                                DataFacetDo.RESOURCE_SYMBOL,
                                dataFacetDo.getName(),
                                dataFacetDo.getUid()));
                    });

                    return result;
                }
            }
            break;
            default:
                break;
        }

        return null;
    }
}
