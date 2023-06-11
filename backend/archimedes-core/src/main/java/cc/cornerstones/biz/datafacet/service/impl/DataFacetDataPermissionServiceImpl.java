package cc.cornerstones.biz.datafacet.service.impl;

import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.administration.serviceconnection.dto.DataPermissionServiceAgentDto;
import cc.cornerstones.biz.administration.serviceconnection.dto.DataPermissionServiceComponentDto;
import cc.cornerstones.biz.administration.serviceconnection.entity.DataPermissionServiceAgentDo;
import cc.cornerstones.biz.administration.serviceconnection.entity.DataPermissionServiceComponentDo;
import cc.cornerstones.biz.administration.serviceconnection.persistence.DataPermissionServiceAgentRepository;
import cc.cornerstones.biz.administration.serviceconnection.persistence.DataPermissionServiceComponentRepository;
import cc.cornerstones.biz.datafacet.dto.*;
import cc.cornerstones.biz.datafacet.entity.DataFieldDo;
import cc.cornerstones.biz.datafacet.entity.DataPermissionDo;
import cc.cornerstones.biz.datafacet.persistence.DataPermissionRepository;
import cc.cornerstones.biz.datafacet.service.inf.DataFacetDataPermissionService;
import cc.cornerstones.biz.datafacet.share.types.FieldTypeExtensionFile;
import cc.cornerstones.biz.resourceownership.service.inf.ResourceOwnershipService;
import cc.cornerstones.biz.share.event.*;
import com.alibaba.fastjson.JSONObject;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DataFacetDataPermissionServiceImpl implements DataFacetDataPermissionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataFacetDataPermissionServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private DataPermissionRepository dataPermissionRepository;

    @Autowired
    private DataPermissionServiceAgentRepository dataPermissionServiceAgentRepository;

    @Autowired
    private ResourceOwnershipService resourceOwnershipService;

    @Override
    public DataPermissionDto createDataPermissionForDataFacet(
            Long dataFacetUid,
            CreateDataPermissionDto createDataPermissionDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        //
        // Step 2, core-processing
        //
        DataPermissionDo dataPermissionDo = new DataPermissionDo();
        dataPermissionDo.setUid(this.idHelper.getNextDistributedId(DataPermissionDo.RESOURCE_NAME));

        DataPermissionContentDto dataPermissionContentDto = new DataPermissionContentDto();
        dataPermissionContentDto.setFieldNameList(createDataPermissionDto.getFieldNameList());
        dataPermissionContentDto.setDataPermissionServiceAgentUid(createDataPermissionDto.getDataPermissionServiceAgentUid());
        dataPermissionContentDto.setResourceCategoryUid(createDataPermissionDto.getResourceCategoryUid());
        dataPermissionContentDto.setResourceCategoryName(createDataPermissionDto.getResourceCategoryName());
        dataPermissionContentDto.setResourceStructureLevelMapping(createDataPermissionDto.getResourceStructureLevelMapping());
        dataPermissionDo.setContent(dataPermissionContentDto);

        dataPermissionDo.setEnabled(createDataPermissionDto.getEnabled());
        dataPermissionDo.setDataFacetUid(dataFacetUid);

        BaseDo.create(dataPermissionDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dataPermissionRepository.save(dataPermissionDo);

        //
        // Step 3, post-processing
        //
        DataPermissionDto dataPermissionDto = new DataPermissionDto();
        dataPermissionDto.setUid(dataPermissionDo.getUid());
        dataPermissionDto.setEnabled(dataPermissionDo.getEnabled());
        dataPermissionDto.setDataFacetUid(dataPermissionDo.getDataFacetUid());

        dataPermissionDto.setFieldNameList(createDataPermissionDto.getFieldNameList());
        dataPermissionDto.setServiceAgent(new DataPermissionServiceAgentDto());
        dataPermissionDto.getServiceAgent().setUid(createDataPermissionDto.getDataPermissionServiceAgentUid());
        dataPermissionDto.setResourceCategoryUid(createDataPermissionDto.getResourceCategoryUid());
        dataPermissionDto.setResourceCategoryName(createDataPermissionDto.getResourceCategoryName());
        dataPermissionDto.setResourceStructureLevelMapping(createDataPermissionDto.getResourceStructureLevelMapping());

        return dataPermissionDto;
    }

    @Override
    public void replaceDataPermission(
            Long uid,
            CreateDataPermissionDto createDataPermissionDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DataPermissionDo dataPermissionDo = this.dataPermissionRepository.findByUid(uid);
        if (dataPermissionDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataPermissionDo.RESOURCE_SYMBOL, uid));
        }

        //
        // Step 2, core-processing
        //

        DataPermissionContentDto dataPermissionContentDto = new DataPermissionContentDto();
        dataPermissionContentDto.setFieldNameList(createDataPermissionDto.getFieldNameList());
        dataPermissionContentDto.setDataPermissionServiceAgentUid(createDataPermissionDto.getDataPermissionServiceAgentUid());
        dataPermissionContentDto.setResourceCategoryUid(createDataPermissionDto.getResourceCategoryUid());
        dataPermissionContentDto.setResourceCategoryName(createDataPermissionDto.getResourceCategoryName());
        dataPermissionContentDto.setResourceStructureLevelMapping(createDataPermissionDto.getResourceStructureLevelMapping());
        dataPermissionDo.setContent(dataPermissionContentDto);

        dataPermissionDo.setContent(dataPermissionContentDto);

        dataPermissionDo.setEnabled(createDataPermissionDto.getEnabled());

        BaseDo.update(dataPermissionDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dataPermissionRepository.save(dataPermissionDo);

        //
        // Step 3, post-processing
        //
    }

    @Override
    public List<String> listAllReferencesToDataPermission(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return null;
    }

    @Override
    public void deleteDataPermission(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DataPermissionDo dataPermissionDo = this.dataPermissionRepository.findByUid(uid);
        if (dataPermissionDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataPermissionDo.RESOURCE_SYMBOL, uid));
        }

        //
        // Step 2, core-processing
        //


        //
        // Step 3, post-processing
        //
        dataPermissionDo.setDeleted(Boolean.TRUE);
        BaseDo.update(dataPermissionDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dataPermissionRepository.save(dataPermissionDo);
    }

    @Override
    public List<DataPermissionDto> listingQueryDataPermissionsOfDataFacet(
            Long dataFacetUid,
            Boolean enabled,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Specification<DataPermissionDo> specification = new Specification<DataPermissionDo>() {
            @Override
            public Predicate toPredicate(Root<DataPermissionDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (dataFacetUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("dataFacetUid"), dataFacetUid));
                }
                if (enabled != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("enabled"), enabled));
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        List<DataPermissionDo> itemDoList = this.dataPermissionRepository.findAll(specification, sort);
        if (CollectionUtils.isEmpty(itemDoList)) {
            return null;
        }

        Map<Long, DataPermissionServiceAgentDto> dataPermissionServiceAgentDtoMap = new HashMap<>();
        Map<Long, cc.cornerstones.archimedes.extensions.types.TreeNode> resourceStructureTreeNodeMap = new HashMap();

        List<DataPermissionDto> content = new LinkedList<>();
        itemDoList.forEach(itemDo -> {
            DataPermissionDto itemDto = new DataPermissionDto();
            BeanUtils.copyProperties(itemDo, itemDto);

            DataPermissionContentDto dataPermissionContentDto = itemDo.getContent();
            itemDto.setFieldNameList(dataPermissionContentDto.getFieldNameList());
            itemDto.setResourceCategoryUid(dataPermissionContentDto.getResourceCategoryUid());
            itemDto.setResourceCategoryName(dataPermissionContentDto.getResourceCategoryName());
            itemDto.setResourceStructureLevelMapping(dataPermissionContentDto.getResourceStructureLevelMapping());

            List<cc.cornerstones.archimedes.extensions.types.TreeNode> resourceCategoryTreeNodeList =
                    this.resourceOwnershipService.treeListingAllNodesOfResourceCategoryHierarchy(
                            dataPermissionContentDto.getDataPermissionServiceAgentUid(),
                            operatingUserProfile);
            if (!CollectionUtils.isEmpty(resourceCategoryTreeNodeList)) {
                resourceCategoryTreeNodeList.forEach(resourceCategoryTreeNode -> {
                    if (resourceCategoryTreeNode.getUid() != null
                            && resourceCategoryTreeNode.getUid().equals(dataPermissionContentDto.getResourceCategoryUid())) {
                        itemDto.setResourceCategoryName(resourceCategoryTreeNode.getName());
                    }
                });
            }

            if (resourceStructureTreeNodeMap.containsKey(dataPermissionContentDto.getResourceCategoryUid())) {
                itemDto.setResourceStructure(resourceStructureTreeNodeMap.get(dataPermissionContentDto.getResourceCategoryUid()));
            } else {
                cc.cornerstones.archimedes.extensions.types.TreeNode resourceStructureTreeNode =
                        this.resourceOwnershipService.treeListingAllNodesOfResourceStructureHierarchy(
                                dataPermissionContentDto.getDataPermissionServiceAgentUid(),
                                dataPermissionContentDto.getResourceCategoryUid(),
                                operatingUserProfile);
                if (resourceStructureTreeNode != null) {
                    resourceStructureTreeNodeMap.put(dataPermissionContentDto.getResourceCategoryUid(), resourceStructureTreeNode);
                    itemDto.setResourceStructure(resourceStructureTreeNode);
                }
            }

            if (dataPermissionServiceAgentDtoMap.containsKey(
                    dataPermissionContentDto.getDataPermissionServiceAgentUid())) {
                itemDto.setServiceAgent(
                        dataPermissionServiceAgentDtoMap.get(dataPermissionContentDto.getDataPermissionServiceAgentUid()));
            } else {
                DataPermissionServiceAgentDto dataPermissionServiceAgentDto =
                        new DataPermissionServiceAgentDto();
                DataPermissionServiceAgentDo dataPermissionServiceAgentDo =
                        this.dataPermissionServiceAgentRepository.findByUid(dataPermissionContentDto.getDataPermissionServiceAgentUid());
                if (dataPermissionServiceAgentDo == null) {
                    LOGGER.error("cannot find data permission service agent {}, but it is referenced by data " +
                                    "permission of data facet {}",
                            dataPermissionContentDto.getDataPermissionServiceAgentUid(), dataFacetUid);
                    dataPermissionServiceAgentDto.setUid(dataPermissionServiceAgentDo.getUid());
                } else {
                    dataPermissionServiceAgentDto.setUid(dataPermissionServiceAgentDo.getUid());
                    dataPermissionServiceAgentDto.setName(dataPermissionServiceAgentDo.getName());
                }
                dataPermissionServiceAgentDtoMap.put(dataPermissionServiceAgentDo.getUid(),
                        dataPermissionServiceAgentDto);
                itemDto.setServiceAgent(dataPermissionServiceAgentDto);
            }

            content.add(itemDto);
        });
        return content;
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

        //
        // Step 2, core-processing
        //


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
        this.dataPermissionRepository.deleteByDataFacetUid(event.getDataFacetDo().getUid());

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

        //
        // Step 2, core-processing
        //


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
    public void handleDataPermissionServiceAgentDeletedEvent(DataPermissionServiceAgentDeletedEvent event) {
        LOGGER.info("rcv event:{}", event);

        Long dataPermissionServiceAgentUid = event.getDataPermissionServiceAgentDo().getUid();

        List<DataPermissionDo> toDeleteList = new LinkedList<>();

        this.dataPermissionRepository.findAll().forEach(dataPermissionDo -> {
            if (dataPermissionDo.getContent() == null) {
                return;
            }

            if (dataPermissionServiceAgentUid.equals(dataPermissionDo.getContent().getDataPermissionServiceAgentUid())) {
                dataPermissionDo.setDeleted(Boolean.TRUE);
                BaseDo.update(dataPermissionDo, event.getOperatingUserProfile().getUid(), LocalDateTime.now());
                toDeleteList.add(dataPermissionDo);
            }
        });


        if (!CollectionUtils.isEmpty(toDeleteList)) {
            this.dataPermissionRepository.saveAll(toDeleteList);
        }
    }
}
