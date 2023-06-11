package cc.cornerstones.biz.datawidget.service.impl;

import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceDuplicateException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.datafacet.dto.AdvancedFeatureDto;
import cc.cornerstones.biz.datafacet.entity.DataFacetDo;
import cc.cornerstones.biz.datafacet.entity.DataFieldDo;
import cc.cornerstones.biz.datafacet.persistence.DataFacetRepository;
import cc.cornerstones.biz.datafacet.persistence.DataFieldRepository;
import cc.cornerstones.biz.datafacet.service.inf.DataFacetAdvancedFeatureService;
import cc.cornerstones.biz.datawidget.dto.CreateDataWidgetDto;
import cc.cornerstones.biz.datawidget.dto.DataWidgetDto;
import cc.cornerstones.biz.datawidget.dto.UpdateDataWidgetDto;
import cc.cornerstones.biz.datawidget.entity.DataWidgetDo;
import cc.cornerstones.biz.datawidget.persistence.DataWidgetRepository;
import cc.cornerstones.biz.datawidget.service.assembly.DataWidgetHandler;
import cc.cornerstones.biz.datawidget.service.inf.DataWidgetService;
import cc.cornerstones.biz.datawidget.share.constants.DataWidgetTypeEnum;
import cc.cornerstones.biz.share.event.*;
import com.alibaba.fastjson.JSONObject;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DataWidgetServiceImpl implements DataWidgetService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataWidgetServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataFacetRepository dataFacetRepository;

    @Autowired
    private DataFieldRepository dataFieldRepository;

    @Autowired
    private DataWidgetRepository dataWidgetRepository;

    @Autowired
    private DataFacetAdvancedFeatureService dataFacetAdvancedFeatureService;

    @Override
    public DataWidgetDto createDataWidget(
            Long dataFacetUid,
            CreateDataWidgetDto createDataWidgetDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // step 1, pre-processing
        //

        // find out data facet
        DataFacetDo dataFacetDo = this.dataFacetRepository.findByUid(dataFacetUid);
        if (dataFacetDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataFacetDo.RESOURCE_SYMBOL,
                    dataFacetUid));
        }

        // check exists duplicate
        boolean existsDuplicate =
                this.dataWidgetRepository.existsByNameAndDataFacetUid(createDataWidgetDto.getName(), dataFacetUid);
        if (existsDuplicate) {
            throw new AbcResourceDuplicateException(String.format("%s::name=%s,data_facet_uid=%d", DataWidgetDo.RESOURCE_SYMBOL,
                    createDataWidgetDto.getName(), dataFacetUid));
        }

        // validate build characteristics
        if (createDataWidgetDto.getBuildCharacteristics() != null) {
            DataWidgetHandler objectiveDataWidgetHandler = null;
            Map<String, DataWidgetHandler> map = this.applicationContext.getBeansOfType(DataWidgetHandler.class);
            if (!CollectionUtils.isEmpty(map)) {
                for (Map.Entry<String, DataWidgetHandler> entry : map.entrySet()) {
                    DataWidgetHandler dataWidgetHandler = entry.getValue();
                    if (dataWidgetHandler.type().equals(createDataWidgetDto.getType())) {
                        objectiveDataWidgetHandler = dataWidgetHandler;
                        break;
                    }
                }
            }
            if (objectiveDataWidgetHandler == null) {
                throw new AbcResourceConflictException(
                        String.format("cannot find data widget handler of data widget type:%s",
                                createDataWidgetDto.getType()));
            }
            objectiveDataWidgetHandler.validateBuildCharacteristicsAccordingToDataFacet(
                    createDataWidgetDto.getBuildCharacteristics(), dataFacetUid);
        }

        //
        // step 2, core-processing
        //
        DataWidgetDo dataWidgetDo = new DataWidgetDo();
        dataWidgetDo.setUid(this.idHelper.getNextDistributedId(DataWidgetDo.RESOURCE_NAME));
        dataWidgetDo.setName(createDataWidgetDto.getName());
        dataWidgetDo.setObjectName(createDataWidgetDto.getName()
                .replaceAll("_", "__")
                .replaceAll("\\s", "_")
                .toLowerCase());
        dataWidgetDo.setDescription(createDataWidgetDto.getDescription());
        dataWidgetDo.setType(createDataWidgetDto.getType());
        dataWidgetDo.setBuildCharacteristics(createDataWidgetDto.getBuildCharacteristics());
        dataWidgetDo.setDataFacetUid(dataFacetUid);
        BaseDo.create(dataWidgetDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dataWidgetRepository.save(dataWidgetDo);

        //
        // step 3, post-processing
        //
        DataWidgetDto dataWidgetDto = new DataWidgetDto();
        BeanUtils.copyProperties(dataWidgetDo, dataWidgetDto);
        return dataWidgetDto;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateDataWidget(
            Long uid,
            UpdateDataWidgetDto updateDataWidgetDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // step 1, pre-processing
        //

        DataWidgetDo dataWidgetDo = this.dataWidgetRepository.findByUid(uid);
        if (dataWidgetDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataWidgetDo.RESOURCE_SYMBOL,
                    uid));
        }

        if (updateDataWidgetDto.getName() != null
                && !updateDataWidgetDto.getName().equalsIgnoreCase(dataWidgetDo.getName())) {
            // check exists duplicate
            boolean existsDuplicate =
                    this.dataWidgetRepository.existsByNameAndDataFacetUid(updateDataWidgetDto.getName(),
                            dataWidgetDo.getDataFacetUid());
            if (existsDuplicate) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s,data_facet_uid=%d", DataWidgetDo.RESOURCE_SYMBOL,
                        updateDataWidgetDto.getName(), dataWidgetDo.getDataFacetUid()));
            }
        }

        //
        // step 2, core-processing
        //
        boolean requiredToUpdate = false;
        if (!ObjectUtils.isEmpty(updateDataWidgetDto.getName())
                && !updateDataWidgetDto.getName().equalsIgnoreCase(dataWidgetDo.getName())) {
            dataWidgetDo.setName(updateDataWidgetDto.getName());
            requiredToUpdate = true;
        }
        if (updateDataWidgetDto.getDescription() != null
                && !updateDataWidgetDto.getDescription().equalsIgnoreCase(dataWidgetDo.getDescription())) {
            dataWidgetDo.setDescription(updateDataWidgetDto.getDescription());
            requiredToUpdate = true;
        }
        if (updateDataWidgetDto.getCharacteristics() != null) {
            // validate characteristics
            DataWidgetHandler objectiveDataWidgetHandler = null;
            Map<String, DataWidgetHandler> map = this.applicationContext.getBeansOfType(DataWidgetHandler.class);
            if (!CollectionUtils.isEmpty(map)) {
                for (Map.Entry<String, DataWidgetHandler> entry : map.entrySet()) {
                    DataWidgetHandler dataWidgetHandler = entry.getValue();
                    if (dataWidgetHandler.type().equals(dataWidgetDo.getType())) {
                        objectiveDataWidgetHandler = dataWidgetHandler;
                        break;
                    }
                }
            }
            if (objectiveDataWidgetHandler == null) {
                throw new AbcResourceConflictException(
                        String.format("cannot find data widget handler of data widget type:%s",
                                dataWidgetDo.getType()));
            }

            // find out data fields
            List<DataFieldDo> dataFieldDoList =
                    this.dataFieldRepository.findByDataFacetUid(dataWidgetDo.getDataFacetUid());

            objectiveDataWidgetHandler.validateBuildCharacteristicsAccordingToDataFacet(
                    updateDataWidgetDto.getCharacteristics(), dataWidgetDo.getDataFacetUid());

            dataWidgetDo.setBuildCharacteristics(updateDataWidgetDto.getCharacteristics());
            requiredToUpdate = true;
        }
        if (requiredToUpdate) {
            BaseDo.update(dataWidgetDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.dataWidgetRepository.save(dataWidgetDo);

            // event post
            DataWidgetChangedEvent dataWidgetChangedEvent = new DataWidgetChangedEvent();
            dataWidgetChangedEvent.setUid(uid);
            dataWidgetChangedEvent.setOperatingUserProfile(operatingUserProfile);
            this.eventBusManager.send(dataWidgetChangedEvent);
        }

        //
        // step 3, post-processing
        //
    }

    @Override
    public DataWidgetDto getDataWidget(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        DataWidgetDo dataWidgetDo = this.dataWidgetRepository.findByUid(uid);
        if (dataWidgetDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataWidgetDo.RESOURCE_SYMBOL,
                    uid));
        }

        DataWidgetDto dataWidgetDto = new DataWidgetDto();
        BeanUtils.copyProperties(dataWidgetDo, dataWidgetDto);
        return dataWidgetDto;
    }

    @Override
    public List<String> listAllReferencesToDataWidget(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return null;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteDataWidget(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        DataWidgetDo dataWidgetDo = this.dataWidgetRepository.findByUid(uid);
        if (dataWidgetDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataWidgetDo.RESOURCE_SYMBOL,
                    uid));
        }

        deleteDataWidget(dataWidgetDo, operatingUserProfile);
    }

    public void deleteDataWidget(
            DataWidgetDo dataWidgetDo,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        dataWidgetDo.setDeleted(Boolean.TRUE);
        BaseDo.update(dataWidgetDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dataWidgetRepository.save(dataWidgetDo);

        // event post
        DataWidgetDeletedEvent dataWidgetDeletedEvent = new DataWidgetDeletedEvent();
        dataWidgetDeletedEvent.setUid(dataWidgetDo.getUid());
        dataWidgetDeletedEvent.setOperatingUserProfile(operatingUserProfile);
        this.eventBusManager.send(dataWidgetDeletedEvent);
    }

    @Override
    public Page<DataWidgetDto> pagingQueryDataWidgets(
            Long uid,
            String name,
            DataWidgetTypeEnum type,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Specification<DataWidgetDo> specification = new Specification<DataWidgetDo>() {
            @Override
            public Predicate toPredicate(Root<DataWidgetDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (uid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("uid"), uid));
                }
                if (!ObjectUtils.isEmpty(name)) {
                    predicateList.add(criteriaBuilder.like(root.get("name"), "%" + name + "%"));
                }
                if (type != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("type"), type));
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        Page<DataWidgetDo> itemDoPage = this.dataWidgetRepository.findAll(specification, pageable);
        List<DataWidgetDto> content = new ArrayList<>(itemDoPage.getSize());
        itemDoPage.forEach(itemDo -> {
            DataWidgetDto itemDto = new DataWidgetDto();
            BeanUtils.copyProperties(itemDo, itemDto);
            content.add(itemDto);
        });
        Page<DataWidgetDto> itemDtoPage = new PageImpl<>(content, pageable, itemDoPage.getTotalElements());
        return itemDtoPage;
    }

    @Override
    public List<DataWidgetDto> listingQueryDataWidgets(
            Long uid,
            String name,
            DataWidgetTypeEnum type,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Specification<DataWidgetDo> specification = new Specification<DataWidgetDo>() {
            @Override
            public Predicate toPredicate(Root<DataWidgetDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (uid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("uid"), uid));
                }
                if (!ObjectUtils.isEmpty(name)) {
                    predicateList.add(criteriaBuilder.like(root.get("name"), "%" + name + "%"));
                }
                if (type != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("type"), type));
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        List<DataWidgetDo> itemDoList = this.dataWidgetRepository.findAll(specification, sort);
        if (CollectionUtils.isEmpty(itemDoList)) {
            return null;
        }
        List<DataWidgetDto> itemDtoList = new ArrayList<>(itemDoList.size());
        itemDoList.forEach(itemDo -> {
            DataWidgetDto itemDto = new DataWidgetDto();
            BeanUtils.copyProperties(itemDo, itemDto);
            itemDtoList.add(itemDto);
        });
        return itemDtoList;
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
    public void handleDataFacetDeletedEvent(DataFacetDeletedEvent event) {
        LOGGER.info("rcv event:{}", event);

        List<DataWidgetDo> dataWidgetDoList =
                this.dataWidgetRepository.findByDataFacetUid(event.getDataFacetDo().getUid());
        if (CollectionUtils.isEmpty(dataWidgetDoList)) {
            return;
        }

        dataWidgetDoList.forEach(dataWidgetDo -> {
            deleteDataWidget(dataWidgetDo, event.getOperatingUserProfile());
        });
    }

    /**
     * event handler
     *
     * @param event
     */
    @Subscribe
    public void handleDataFacetChangedEvent(DataFacetChangedEvent event) {
        LOGGER.info("rcv event:{}", event);

        List<DataWidgetDo> dataWidgetDoList =
                this.dataWidgetRepository.findByDataFacetUid(event.getDataFacetDo().getUid());
        if (CollectionUtils.isEmpty(dataWidgetDoList)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // 处理 data widget 的 characteristics 是否符合最新的 data facet 实际
        dataWidgetDoList.forEach(dataWidgetDo -> {
            DataWidgetHandler objectiveDataWidgetHandler = null;
            Map<String, DataWidgetHandler> map = this.applicationContext.getBeansOfType(DataWidgetHandler.class);
            if (!CollectionUtils.isEmpty(map)) {
                for (Map.Entry<String, DataWidgetHandler> entry : map.entrySet()) {
                    DataWidgetHandler dataWidgetHandler = entry.getValue();
                    if (dataWidgetHandler.type().equals(dataWidgetDo.getType())) {
                        objectiveDataWidgetHandler = dataWidgetHandler;
                        break;
                    }
                }
            }
            if (objectiveDataWidgetHandler == null) {
                throw new AbcResourceConflictException(
                        String.format("cannot find data widget handler of data widget type:%s",
                                dataWidgetDo.getType()));
            }
            JSONObject adjustedCharacteristics =
                    objectiveDataWidgetHandler.adjustBuildCharacteristicsAccordingToDataFacet(
                            dataWidgetDo.getBuildCharacteristics(), dataWidgetDo.getDataFacetUid());
            if (adjustedCharacteristics != null) {
                dataWidgetDo.setBuildCharacteristics(adjustedCharacteristics);
                BaseDo.update(dataWidgetDo, event.getOperatingUserProfile().getUid(), now);
                this.dataWidgetRepository.save(dataWidgetDo);

                // event post
                DataWidgetChangedEvent dataWidgetChangedEvent = new DataWidgetChangedEvent();
                dataWidgetChangedEvent.setUid(dataWidgetDo.getUid());
                dataWidgetChangedEvent.setOperatingUserProfile(event.getOperatingUserProfile());
                eventBusManager.send(dataWidgetChangedEvent);
            }
        });
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
        this.dataWidgetRepository.deleteByDataFacetUid(event.getDataFacetDo().getUid());

        //
        // Step 2, core-processing
        //

        AdvancedFeatureDto advancedFeatureDto =
                this.dataFacetAdvancedFeatureService.getAdvancedFeatureOfDataFacet(
                        event.getDataFacetDo().getUid(), event.getOperatingUserProfile());


        // Step 2.1, 创建默认 table data widget, empty characteristics
        CreateDataWidgetDto createDataWidgetDtoOfTableType = new CreateDataWidgetDto();
        createDataWidgetDtoOfTableType.setName("Table");
        createDataWidgetDtoOfTableType.setType(DataWidgetTypeEnum.TABLE);
        createDataWidget(event.getDataFacetDo().getUid(), createDataWidgetDtoOfTableType, event.getOperatingUserProfile());

        if (advancedFeatureDto != null
                && advancedFeatureDto.getContent() != null
                && Boolean.TRUE.equals(advancedFeatureDto.getContent().getEnabledPivotTable())) {
            // Step 2.2, 创建默认 pivot table data widget, empty characteristics
            CreateDataWidgetDto createDataWidgetDtoOfPivotTableType = new CreateDataWidgetDto();
            createDataWidgetDtoOfPivotTableType.setName("Pivot table");
            createDataWidgetDtoOfPivotTableType.setType(DataWidgetTypeEnum.PIVOT_TABLE);
            createDataWidget(event.getDataFacetDo().getUid(), createDataWidgetDtoOfPivotTableType, event.getOperatingUserProfile());
        }

        if (advancedFeatureDto != null
                && advancedFeatureDto.getContent() != null
                && Boolean.TRUE.equals(advancedFeatureDto.getContent().getEnabledChart())) {
            // Step 2.3, 创建默认 chart data widget, empty characteristics
            CreateDataWidgetDto createDataWidgetDtoOfChart = new CreateDataWidgetDto();
            createDataWidgetDtoOfChart.setName("Chart");
            createDataWidgetDtoOfChart.setType(DataWidgetTypeEnum.CHART);
            createDataWidget(event.getDataFacetDo().getUid(), createDataWidgetDtoOfChart, event.getOperatingUserProfile());
        }

        //
        // Step 3, post-processing
        //
    }
}
