package cc.cornerstones.biz.app.service.impl;

import cc.cornerstones.almond.exceptions.*;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.TreeNode;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.almond.types.CreateDirectoryTreeNode;
import cc.cornerstones.almond.types.CreateEntityTreeNode;
import cc.cornerstones.almond.types.UpdateDirectoryTreeNode;
import cc.cornerstones.almond.types.ReplaceTreeNodeRelationship;
import cc.cornerstones.biz.app.entity.AppDataFacetDo;
import cc.cornerstones.biz.app.persistence.AppDataFacetRepository;
import cc.cornerstones.biz.app.persistence.AppRepository;
import cc.cornerstones.biz.app.service.assembly.AppAccessHandler;
import cc.cornerstones.biz.app.service.inf.AppDataFacetService;
import cc.cornerstones.biz.app.share.types.AppDataFacetEntityNode;
import cc.cornerstones.biz.datadictionary.entity.DictionaryCategoryDo;
import cc.cornerstones.biz.datadictionary.service.assembly.DictionaryBuildSqlLogic;
import cc.cornerstones.biz.datafacet.entity.DataFacetDo;
import cc.cornerstones.biz.datafacet.persistence.DataFacetRepository;
import cc.cornerstones.biz.share.assembly.ResourceReferenceManager;
import cc.cornerstones.biz.share.event.*;
import cc.cornerstones.biz.share.types.ResourceReferenceHandler;
import com.alibaba.fastjson.JSONObject;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
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
public class AppDataFacetServiceImpl implements AppDataFacetService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppDataFacetServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private AppDataFacetRepository appDataFacetRepository;

    @Autowired
    private DataFacetRepository dataFacetRepository;

    @Autowired
    private AppRepository appRepository;

    @Autowired
    private AppAccessHandler appAccessHandler;

    @Override
    public TreeNode createDirectoryNodeForDataFacetHierarchy(
            Long appUid,
            Long parentUid,
            CreateDirectoryTreeNode createDirectoryTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        if (parentUid == null) {
            Integer existsDuplicate = this.appDataFacetRepository.existsByAppUidAndNameWithoutParent(
                    appUid, createDirectoryTreeNode.getName());
            if (existsDuplicate > 0) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s", AppDataFacetDo.RESOURCE_SYMBOL,
                        createDirectoryTreeNode.getName()));
            }
        } else {
            Integer existsDuplicate = this.appDataFacetRepository.existsByAppUidAndNameWithinParent(
                    appUid, createDirectoryTreeNode.getName(), parentUid);
            if (existsDuplicate > 0) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s", AppDataFacetDo.RESOURCE_SYMBOL,
                        createDirectoryTreeNode.getName()));
            }

            boolean exists = this.appDataFacetRepository.existsByUid(parentUid);
            if (!exists) {
                throw new AbcResourceNotFoundException(String.format("%s::uid=%s", AppDataFacetDo.RESOURCE_SYMBOL,
                        parentUid));
            }
        }

        // verify authorization
        this.appAccessHandler.verifyWriteAuthorization(appUid, operatingUserProfile);

        //
        // Step 2, core-processing
        //
        AppDataFacetDo appDataFacetDo = new AppDataFacetDo();
        appDataFacetDo.setUid(this.idHelper.getNextDistributedId(AppDataFacetDo.RESOURCE_NAME));
        appDataFacetDo.setName(createDirectoryTreeNode.getName());
        appDataFacetDo.setObjectName(
                createDirectoryTreeNode.getName()
                        .replaceAll("_", "__")
                        .replaceAll("\\s", "_")
                        .toLowerCase());
        appDataFacetDo.setDescription(createDirectoryTreeNode.getDescription());
        appDataFacetDo.setDirectory(Boolean.TRUE);
        appDataFacetDo.setParentUid(parentUid);
        appDataFacetDo.setAppUid(appUid);

        // sequence
        Float maxSequence = null;
        if (parentUid == null) {
            maxSequence = this.appDataFacetRepository.findMaxSequenceWithoutParent();
        } else {
            maxSequence = this.appDataFacetRepository.findMaxSequenceWithinParent(parentUid);
        }
        if (maxSequence == null) {
            appDataFacetDo.setSequence(0.001f);
        } else {
            appDataFacetDo.setSequence(maxSequence + 0.001f);
        }

        BaseDo.create(appDataFacetDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.appDataFacetRepository.save(appDataFacetDo);

        //
        // Step 3, post-processing
        //
        TreeNode treeNode = new TreeNode();
        treeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
        treeNode.setUid(appDataFacetDo.getUid());
        treeNode.setName(appDataFacetDo.getName());
        treeNode.setDescription(appDataFacetDo.getDescription());
        treeNode.setType(TreeNode.GENERAL_TYPE_DIRECTORY);

        return treeNode;
    }

    @Override
    public void updateDirectoryNodeOfDataFacetHierarchy(
            Long uid,
            UpdateDirectoryTreeNode updateDirectoryTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        AppDataFacetDo appDataFacetDo = this.appDataFacetRepository.findByUid(uid);
        if (appDataFacetDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", AppDataFacetDo.RESOURCE_SYMBOL, uid));
        }
        Long appUid = appDataFacetDo.getAppUid();

        if (!ObjectUtils.isEmpty(updateDirectoryTreeNode.getName())
                && !updateDirectoryTreeNode.getName().equalsIgnoreCase(appDataFacetDo.getName())) {
            // name 要改
            if (appDataFacetDo.getParentUid() == null) {
                // 没有 parent 的情况
                Integer existsDuplicate = this.appDataFacetRepository.existsByAppUidAndNameWithoutParent(
                        appUid, updateDirectoryTreeNode.getName());
                if (existsDuplicate > 0) {
                    throw new AbcResourceDuplicateException(String.format("%s::name=%s", AppDataFacetDo.RESOURCE_SYMBOL,
                            updateDirectoryTreeNode.getName()));
                }
            } else {
                // 有 parent 的情况
                Integer existsDuplicate = this.appDataFacetRepository.existsByAppUidAndNameWithinParent(
                        appUid, updateDirectoryTreeNode.getName(), appDataFacetDo.getParentUid());
                if (existsDuplicate > 0) {
                    throw new AbcResourceDuplicateException(String.format("%s::name=%s", AppDataFacetDo.RESOURCE_SYMBOL,
                            updateDirectoryTreeNode.getName()));
                }
            }
        }

        // verify authorization
        this.appAccessHandler.verifyWriteAuthorization(appUid, operatingUserProfile);


        //
        // Step 2, core-processing
        //
        boolean requiredToUpdate = false;

        if (!ObjectUtils.isEmpty(updateDirectoryTreeNode.getName())
                && !updateDirectoryTreeNode.getName().equalsIgnoreCase(appDataFacetDo.getName())) {
            appDataFacetDo.setName(updateDirectoryTreeNode.getName());
            requiredToUpdate = true;
        }
        if (updateDirectoryTreeNode.getDescription() != null
                && !updateDirectoryTreeNode.getDescription().equalsIgnoreCase(appDataFacetDo.getDescription())) {
            appDataFacetDo.setDescription(updateDirectoryTreeNode.getDescription());
            requiredToUpdate = true;
        }

        if (requiredToUpdate) {
            BaseDo.update(appDataFacetDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.appDataFacetRepository.save(appDataFacetDo);
        }

        //
        // Step 3, post-processing
        //
    }

    @Override
    public TreeNode createEntityNodeForDataFacetHierarchy(
            Long appUid,
            Long parentUid,
            CreateEntityTreeNode createEntityTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        // entity node translation
        JSONObject payload = createEntityTreeNode.getPayload();
        AppDataFacetEntityNode appDataFacetEntityNode = JSONObject.toJavaObject(payload, AppDataFacetEntityNode.class);
        Long dataFacetUid = appDataFacetEntityNode.getDataFacetUid();
        if (dataFacetUid == null) {
            throw new AbcIllegalParameterException("payload.data_facet_uid should not be null");
        }

        if (parentUid == null) {
            Integer existsDuplicate = this.appDataFacetRepository.existsByAppUidAndDataFacetUidWithoutParent(
                    appUid, dataFacetUid);
            if (existsDuplicate > 0) {
                return null;
            }
        } else {
            Integer existsDuplicate = this.appDataFacetRepository.existsByAppUidAndDataFacetUidWithinParent(
                    appUid, dataFacetUid, parentUid);
            if (existsDuplicate > 0) {
                return null;
            }

            boolean exists = this.appDataFacetRepository.existsByUid(parentUid);
            if (!exists) {
                throw new AbcResourceNotFoundException(String.format("%s::uid=%s", AppDataFacetDo.RESOURCE_SYMBOL,
                        parentUid));
            }
        }

        // verify authorization
        this.appAccessHandler.verifyWriteAuthorization(appUid, operatingUserProfile);


        //
        // Step 2, core-processing
        //
        AppDataFacetDo appDataFacetDo = new AppDataFacetDo();
        appDataFacetDo.setUid(this.idHelper.getNextDistributedId(AppDataFacetDo.RESOURCE_NAME));
        appDataFacetDo.setName(createEntityTreeNode.getName());
        appDataFacetDo.setDescription(createEntityTreeNode.getDescription());
        appDataFacetDo.setDirectory(Boolean.FALSE);
        appDataFacetDo.setParentUid(parentUid);
        appDataFacetDo.setAppUid(appUid);
        appDataFacetDo.setDataFacetUid(appDataFacetEntityNode.getDataFacetUid());

        // sequence
        Float maxSequence = null;
        if (parentUid == null) {
            maxSequence = this.appDataFacetRepository.findMaxSequenceWithoutParent();
        } else {
            maxSequence = this.appDataFacetRepository.findMaxSequenceWithinParent(parentUid);
        }
        if (maxSequence == null) {
            appDataFacetDo.setSequence(0.001f);
        } else {
            appDataFacetDo.setSequence(maxSequence + 0.001f);
        }

        BaseDo.create(appDataFacetDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.appDataFacetRepository.save(appDataFacetDo);

        //
        // Step 3, post-processing
        //
        TreeNode treeNode = new TreeNode();
        treeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
        treeNode.setUid(appDataFacetDo.getUid());
        treeNode.setName(appDataFacetDo.getName());
        treeNode.setDescription(appDataFacetDo.getDescription());
        treeNode.setType(TreeNode.GENERAL_TYPE_ENTITY);

        return treeNode;
    }

    @Override
    public List<TreeNode> batchCreateEntityNodesForDataFacetHierarchy(
            Long appUid,
            Long parentUid,
            List<CreateEntityTreeNode> createEntityTreeNodeList,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        if (CollectionUtils.isEmpty(createEntityTreeNodeList)) {
            return null;
        }

        // verify authorization
        this.appAccessHandler.verifyWriteAuthorization(appUid, operatingUserProfile);

        //
        // Step 2, core-processing
        //
        List<TreeNode> result = new LinkedList<>();
        createEntityTreeNodeList.forEach(createEntityNodeDto -> {
            TreeNode treeNode = createEntityNodeForDataFacetHierarchy(appUid, parentUid, createEntityNodeDto,
                    operatingUserProfile);
            result.add(treeNode);
        });
        return result;
    }


    @Override
    public void replaceNodeRelationshipOfDataFacetHierarchy(
            Long uid,
            ReplaceTreeNodeRelationship replaceTreeNodeRelationship,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        AppDataFacetDo itemDo = this.appDataFacetRepository.findByUid(uid);
        if (itemDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", AppDataFacetDo.RESOURCE_SYMBOL,
                    uid));
        }
        Long appUid = itemDo.getAppUid();

        if (replaceTreeNodeRelationship.getReferenceTreeNodeUid() == null) {
            // 调整到根节点下，作为其最末尾子节点
            Specification<AppDataFacetDo> specification = new Specification<AppDataFacetDo>() {
                @Override
                public Predicate toPredicate(Root<AppDataFacetDo> root, CriteriaQuery<?> query,
                                             CriteriaBuilder criteriaBuilder) {
                    List<Predicate> predicateList = new ArrayList<>();
                    predicateList.add(criteriaBuilder.isNull(root.get("parentUid")));
                    return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
                }
            };

            Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("sequence")));
            Page<AppDataFacetDo> candidateItemDoPage = this.appDataFacetRepository.findAll(specification, pageable);
            if (candidateItemDoPage.isEmpty()) {
                itemDo.setSequence(1.0f);
            } else {
                if (candidateItemDoPage.getContent().get(0).getSequence() == null) {
                    itemDo.setSequence(1.0f);
                } else {
                    itemDo.setSequence(candidateItemDoPage.getContent().get(0).getSequence() + 1.0f);
                }
            }

            BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.appDataFacetRepository.save(itemDo);

            return;
        }

        AppDataFacetDo referenceItemDo =
                this.appDataFacetRepository.findByUid(replaceTreeNodeRelationship.getReferenceTreeNodeUid());
        if (referenceItemDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", AppDataFacetDo.RESOURCE_SYMBOL,
                    replaceTreeNodeRelationship.getReferenceTreeNodeUid()));
        }

        if (!itemDo.getAppUid().equals(referenceItemDo.getAppUid())) {
            throw new AbcResourceConflictException(String.format("illegal request"));
        }

        // verify authorization
        this.appAccessHandler.verifyWriteAuthorization(appUid, operatingUserProfile);


        //
        // Step 2, core-processing
        //
        switch (replaceTreeNodeRelationship.getPosition()) {
            case FRONT: {
                itemDo.setParentUid(referenceItemDo.getParentUid());

                if (referenceItemDo.getSequence() == null) {
                    itemDo.setSequence(1.0f);
                    BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.appDataFacetRepository.save(itemDo);

                    referenceItemDo.setSequence(2.0f);
                    BaseDo.update(referenceItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.appDataFacetRepository.save(referenceItemDo);
                } else {
                    Specification<AppDataFacetDo> specification = new Specification<AppDataFacetDo>() {
                        @Override
                        public Predicate toPredicate(Root<AppDataFacetDo> root, CriteriaQuery<?> query,
                                                     CriteriaBuilder criteriaBuilder) {
                            List<Predicate> predicateList = new ArrayList<>();
                            if (referenceItemDo.getParentUid() != null) {
                                predicateList.add(criteriaBuilder.equal(root.get("parentUid"),
                                        referenceItemDo.getParentUid()));
                            } else {
                                predicateList.add(criteriaBuilder.isNull(root.get("parentUid")));
                            }
                            predicateList.add(criteriaBuilder.lessThanOrEqualTo(root.get("sequence"),
                                    referenceItemDo.getSequence()));
                            predicateList.add(criteriaBuilder.notEqual(root.get("uid"), referenceItemDo.getUid()));
                            predicateList.add(criteriaBuilder.notEqual(root.get("uid"), itemDo.getUid()));
                            return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
                        }
                    };

                    Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("sequence")));
                    Page<AppDataFacetDo> candidateItemDoPage = this.appDataFacetRepository.findAll(specification, pageable);
                    if (candidateItemDoPage.isEmpty()) {
                        itemDo.setSequence(referenceItemDo.getSequence() * 0.5f);
                    } else {
                        float gap =
                                referenceItemDo.getSequence() - candidateItemDoPage.getContent().get(0).getSequence();
                        itemDo.setSequence(referenceItemDo.getSequence() - gap / 2);
                    }

                    BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.appDataFacetRepository.save(itemDo);
                }
            }
            break;
            case CENTER: {
                itemDo.setParentUid(referenceItemDo.getUid());

                Specification<AppDataFacetDo> specification = new Specification<AppDataFacetDo>() {
                    @Override
                    public Predicate toPredicate(Root<AppDataFacetDo> root, CriteriaQuery<?> query,
                                                 CriteriaBuilder criteriaBuilder) {
                        List<Predicate> predicateList = new ArrayList<>();

                        predicateList.add(criteriaBuilder.equal(root.get("parentUid"), referenceItemDo.getUid()));
                        return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
                    }
                };

                Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("sequence")));
                Page<AppDataFacetDo> candidateDoPage = this.appDataFacetRepository.findAll(specification, pageable);
                if (candidateDoPage.isEmpty()) {
                    itemDo.setSequence(1.0f);
                } else {
                    itemDo.setSequence(candidateDoPage.getContent().get(0).getSequence() + 1.0f);
                }

                BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                this.appDataFacetRepository.save(itemDo);
            }
            break;
            case REAR: {
                itemDo.setParentUid(referenceItemDo.getParentUid());

                if (referenceItemDo.getSequence() == null) {
                    itemDo.setSequence(2.0f);
                    BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.appDataFacetRepository.save(itemDo);

                    referenceItemDo.setSequence(1.0f);
                    BaseDo.update(referenceItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.appDataFacetRepository.save(referenceItemDo);
                } else {
                    Specification<AppDataFacetDo> specification = new Specification<AppDataFacetDo>() {
                        @Override
                        public Predicate toPredicate(Root<AppDataFacetDo> root, CriteriaQuery<?> query,
                                                     CriteriaBuilder criteriaBuilder) {
                            List<Predicate> predicateList = new ArrayList<>();
                            if (referenceItemDo.getParentUid() != null) {
                                predicateList.add(criteriaBuilder.equal(root.get("parentUid"), referenceItemDo.getParentUid()));
                            } else {
                                predicateList.add(criteriaBuilder.isNull(root.get("parentUid")));
                            }
                            predicateList.add(criteriaBuilder.greaterThanOrEqualTo(root.get("sequence"),
                                    referenceItemDo.getSequence()));
                            predicateList.add(criteriaBuilder.notEqual(root.get("uid"), referenceItemDo.getUid()));
                            predicateList.add(criteriaBuilder.notEqual(root.get("uid"), itemDo.getUid()));
                            return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
                        }
                    };

                    Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Order.asc("sequence")));
                    Page<AppDataFacetDo> candidateDoPage = this.appDataFacetRepository.findAll(specification, pageable);
                    if (candidateDoPage.isEmpty()) {
                        itemDo.setSequence(referenceItemDo.getSequence() + 1.0f);
                    } else {
                        float gap =
                                candidateDoPage.getContent().get(0).getSequence() - referenceItemDo.getSequence();
                        itemDo.setSequence(referenceItemDo.getSequence() + gap / 2);
                    }

                    BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.appDataFacetRepository.save(itemDo);
                }
            }
            break;
            default:
                break;
        }

        //
        // Step 3, post-processing
        //
    }

    @Override
    public List<String> listAllReferencesToNodeOfDataFacetHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return null;
    }

    @Override
    public void deleteNodeOfDataFacetHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        AppDataFacetDo appDataFacetDo = this.appDataFacetRepository.findByUid(uid);
        if (appDataFacetDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", AppDataFacetDo.RESOURCE_SYMBOL, uid));
        }
        Long appUid = appDataFacetDo.getAppUid();

        // verify authorization
        this.appAccessHandler.verifyWriteAuthorization(appUid, operatingUserProfile);


        //
        // Step 2, core-processing
        //
        recursivelyDeleteNodeOfDataFacetHierarchy(appDataFacetDo, operatingUserProfile);

        //
        // Step 3, post-processing
        //
    }

    private void recursivelyDeleteNodeOfDataFacetHierarchy(
            AppDataFacetDo appDataFacetDo,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        // 找出所有的子节点，删除
        List<AppDataFacetDo> children =
                this.appDataFacetRepository.findByParentUid(appDataFacetDo.getUid());
        if (!CollectionUtils.isEmpty(children)) {
            children.forEach(child -> {
                recursivelyDeleteNodeOfDataFacetHierarchy(child, operatingUserProfile);
            });
        }

        // 删除自己
        this.appDataFacetRepository.delete(appDataFacetDo);

        AppDataFacetDeletedEvent appDataFacetDeletedEvent = new AppDataFacetDeletedEvent();
        List<Long> dataFacetHierarchyNodeUidList = new LinkedList<>();
        dataFacetHierarchyNodeUidList.add(appDataFacetDo.getUid());
        appDataFacetDeletedEvent.setUidList(dataFacetHierarchyNodeUidList);
        appDataFacetDeletedEvent.setOperatingUserProfile(operatingUserProfile);
        this.eventBusManager.send(appDataFacetDeletedEvent);
    }

    @Override
    public List<TreeNode> treeListingAllNodesOfDataFacetHierarchyOfOneApp(
            Long appUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        // verify authorization
        this.appAccessHandler.verifyWriteAuthorization(appUid, operatingUserProfile);

        //
        // Step 2, core-processing
        //
        List<Long> dataFacetUidList = new LinkedList<>();
        List<AppDataFacetDo> itemDoList = new LinkedList<>();
        this.appDataFacetRepository.findAllByAppUid(appUid).forEach(itemDo -> {
            if (itemDo.getDataFacetUid() != null &&
                    !dataFacetUidList.contains(itemDo.getDataFacetUid())) {
                dataFacetUidList.add(itemDo.getDataFacetUid());
            }

            itemDoList.add(itemDo);
        });
        if (CollectionUtils.isEmpty(itemDoList)) {
            return null;
        }

        Map<Long, DataFacetDo> dataFacetDoMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(dataFacetUidList)) {
            List<DataFacetDo> dataFacetDoList = this.dataFacetRepository.findByUidIn(dataFacetUidList);
            if (!CollectionUtils.isEmpty(dataFacetDoList)) {
                dataFacetDoList.forEach(dataFacetDo -> {
                    dataFacetDoMap.put(dataFacetDo.getUid(), dataFacetDo);
                });
            }
        }

        Map<Long, TreeNode> itemUidAndTreeNodeMap = new HashMap<>();
        List<TreeNode> rootTreeNodeList = new LinkedList<>();
        itemDoList.forEach(itemDo -> {
            TreeNode treeNode = new TreeNode();
            treeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
            treeNode.setUid(itemDo.getUid());

            if (Boolean.TRUE.equals(itemDo.getDirectory())) {
                treeNode.setType(TreeNode.GENERAL_TYPE_DIRECTORY);

                treeNode.setName(itemDo.getName());
                treeNode.setDescription(itemDo.getDescription());
            } else {
                treeNode.setType(TreeNode.GENERAL_TYPE_ENTITY);

                if (itemDo.getDataFacetUid() != null
                        && dataFacetDoMap.containsKey(itemDo.getDataFacetUid())) {
                    treeNode.setName(dataFacetDoMap.get(itemDo.getDataFacetUid()).getName());
                    treeNode.setDescription(dataFacetDoMap.get(itemDo.getDataFacetUid()).getDescription());
                } else {
                    treeNode.setName(itemDo.getName());
                    treeNode.setDescription(itemDo.getDescription());
                }
            }
            treeNode.setTags(new HashMap<>());
            if (itemDo.getParentUid() != null) {
                treeNode.getTags().put("parent_uid", itemDo.getParentUid());
            }
            treeNode.getTags().put("data_facet_uid", itemDo.getDataFacetUid());
            treeNode.getTags().put("sequence", itemDo.getSequence());

            if (itemDo.getParentUid() == null) {
                rootTreeNodeList.add(treeNode);
            }

            itemUidAndTreeNodeMap.put(itemDo.getUid(), treeNode);
        });

        itemUidAndTreeNodeMap.forEach((itemUid, treeNode) -> {
            if (treeNode.getTags().get("parent_uid") != null) {
                Long parentUid = (Long) treeNode.getTags().get("parent_uid");
                TreeNode parentTreeNode = itemUidAndTreeNodeMap.get(parentUid);
                if (parentTreeNode == null) {
                    LOGGER.error("cannot find item {} 's parent_uid {}", itemUid, parentUid);
                } else {
                    if (parentTreeNode.getChildren() == null) {
                        parentTreeNode.setChildren(new LinkedList<>());
                    }
                    parentTreeNode.getChildren().add(treeNode);
                }
            }
        });

        // 排序
        recursivelySortingTreeNodes(rootTreeNodeList);

        return rootTreeNodeList;
    }

    private void recursivelySortingTreeNodes(List<TreeNode> treeNodeList) {
        if (CollectionUtils.isEmpty(treeNodeList)) {
            return;
        }

        for (TreeNode treeNode : treeNodeList) {
            recursivelySortingTreeNodes(treeNode.getChildren());
        }

        if (treeNodeList.size() > 1) {
            treeNodeList.sort(new Comparator<TreeNode>() {
                @Override
                public int compare(TreeNode o1, TreeNode o2) {
                    Object sequenceObjectOfO1 = o1.getTags().get("sequence");
                    Float sequenceOfO1 = 0f;
                    if (sequenceObjectOfO1 != null) {
                        sequenceOfO1 = (Float) sequenceObjectOfO1;
                    }

                    Object sequenceObjectOfO2 = o2.getTags().get("sequence");
                    Float sequenceOfO2 = 0f;
                    if (sequenceObjectOfO2 != null) {
                        sequenceOfO2 = (Float) sequenceObjectOfO2;
                    }

                    int result = Float.compare(sequenceOfO1, sequenceOfO2);

                    if (result < 0) {
                        return -1;
                    } else if (result > 0) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            });
        }
    }

    @Override
    public List<TreeNode> treeListingAllNodesOfDataFacetHierarchyOfAllApps(
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        List<TreeNode> result = new LinkedList<>();
        this.appRepository.findAll(Sort.by(Sort.Order.asc("sequence"))).forEach(appDo -> {
            TreeNode treeNode = new TreeNode();
            treeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
            treeNode.setUid(appDo.getUid());
            treeNode.setName(appDo.getName());
            treeNode.setDescription(appDo.getDescription());
            treeNode.setType(TreeNode.GENERAL_TYPE_DIRECTORY);

            List<TreeNode> childTreeNodeList = treeListingAllNodesOfDataFacetHierarchyOfOneApp(appDo.getUid(),
                    operatingUserProfile);
            treeNode.setChildren(childTreeNodeList);

            result.add(treeNode);
        });
        return result;
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

        List<AppDataFacetDo> appDataFacetDoList = this.appDataFacetRepository.findByDataFacetUid(event.getDataFacetDo().getUid());
        if (!CollectionUtils.isEmpty(appDataFacetDoList)) {
            List<Long> dataFacetHierarchyNodeUidList = new LinkedList<>();
            appDataFacetDoList.forEach(appDataFacetDo -> {
                if (!dataFacetHierarchyNodeUidList.contains(appDataFacetDo.getUid())) {
                    dataFacetHierarchyNodeUidList.add(appDataFacetDo.getUid());
                }
            });
            this.appDataFacetRepository.deleteAll(appDataFacetDoList);


            AppDataFacetDeletedEvent appDataFacetDeletedEvent = new AppDataFacetDeletedEvent();
            appDataFacetDeletedEvent.setUidList(dataFacetHierarchyNodeUidList);
            appDataFacetDeletedEvent.setOperatingUserProfile(event.getOperatingUserProfile());
            this.eventBusManager.send(appDataFacetDeletedEvent);
        }
    }

    /**
     * event handler
     *
     * @param event
     */
    @Subscribe
    public void handleAppDeletedEvent(AppDeletedEvent event) {
        LOGGER.info("rcv event:{}", event);

        Long appUid = event.getUid();
        UserProfile operatingUserProfile = event.getOperatingUserProfile();

        List<AppDataFacetDo> appDataFacetDoList = this.appDataFacetRepository.findByAppUid(appUid);
        if (!CollectionUtils.isEmpty(appDataFacetDoList)) {
            this.appDataFacetRepository.deleteAll(appDataFacetDoList);
        }

    }

    @ResourceReferenceHandler(name = "app data facet")
    public List<String> checkResourceReference(
            ResourceReferenceManager.ResourceCategoryEnum resourceCategory,
            Long resourceUid,
            String resourceName) throws Exception {
        switch (resourceCategory) {
            case APP: {
                Long appUid = resourceUid;

                List<AppDataFacetDo> appDataFacetDoList = this.appDataFacetRepository.findByAppUid(appUid);
                if (!CollectionUtils.isEmpty(appDataFacetDoList)) {
                    List<String> result = new LinkedList<>();

                    appDataFacetDoList.forEach(appDataFacetDo -> {
                        if (ObjectUtils.isEmpty(appDataFacetDo.getName())) {
                            result.add(String.format(
                                    "[%s] %s (%d)",
                                    AppDataFacetDo.RESOURCE_SYMBOL,
                                    appDataFacetDo.getDataFacetUid(),
                                    appDataFacetDo.getUid()));
                        } else {
                            result.add(String.format(
                                    "[%s] %s (%d)",
                                    AppDataFacetDo.RESOURCE_SYMBOL,
                                    appDataFacetDo.getName(),
                                    appDataFacetDo.getUid()));
                        }
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
