package cc.cornerstones.biz.datadictionary.service.impl;

import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.exceptions.*;
import cc.cornerstones.almond.types.*;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.datadictionary.dto.CreateOrReplaceDictionaryBuildDto;
import cc.cornerstones.biz.datadictionary.dto.DictionaryBuildDto;
import cc.cornerstones.biz.datadictionary.dto.DictionaryBuildInstanceDto;
import cc.cornerstones.biz.datadictionary.dto.TestDictionaryBuildDto;
import cc.cornerstones.biz.datadictionary.entity.*;
import cc.cornerstones.biz.datadictionary.persistence.*;
import cc.cornerstones.biz.datadictionary.service.assembly.DictionaryBuildSqlLogic;
import cc.cornerstones.biz.datadictionary.service.assembly.DictionaryHandler;
import cc.cornerstones.biz.datadictionary.service.inf.DictionaryService;
import cc.cornerstones.biz.datafacet.entity.DataFacetDo;
import cc.cornerstones.biz.datatable.share.constants.DictionaryBuildTypeEnum;
import cc.cornerstones.biz.datatable.share.types.DictionaryContentEntityNode;
import cc.cornerstones.biz.distributedjob.dto.CreateDistributedJobDto;
import cc.cornerstones.biz.distributedjob.dto.DistributedJobDto;
import cc.cornerstones.biz.distributedjob.dto.UpdateDistributedJobDto;
import cc.cornerstones.biz.distributedjob.service.inf.DistributedJobService;
import cc.cornerstones.biz.distributedjob.share.constants.JobExecutorRoutingAlgorithmEnum;
import cc.cornerstones.biz.share.assembly.ResourceReferenceManager;
import cc.cornerstones.biz.share.event.DataDictionaryDeletedEvent;
import cc.cornerstones.biz.share.event.EventBusManager;
import cc.cornerstones.almond.types.ReplaceTreeNodeRelationship;
import cc.cornerstones.biz.share.types.ResourceReferenceHandler;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.persistence.criteria.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DictionaryServiceImpl implements DictionaryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private DictionaryCategoryRepository dictionaryCategoryRepository;

    @Autowired
    private DictionaryStructureNodeRepository dictionaryStructureNodeRepository;

    @Autowired
    private DictionaryContentNodeRepository dictionaryContentNodeRepository;

    @Autowired
    private DictionaryBuildRepository dictionaryBuildRepository;

    @Autowired
    private DictionaryBuildInstanceRepository dictionaryBuildInstanceRepository;

    @Autowired
    private DictionaryHandler dictionaryHandler;

    @Autowired
    private DistributedJobService distributedJobService;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private ResourceReferenceManager resourceReferenceManager;

    @Override
    public TreeNode createDirectoryNodeForDictionaryCategoryHierarchy(
            Long parentUid,
            CreateDirectoryTreeNode createDirectoryTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        if (parentUid == null) {
            Integer existsDuplicate = this.dictionaryCategoryRepository.existsByNameWithoutParent(
                    createDirectoryTreeNode.getName());
            if (existsDuplicate > 0) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s",
                        DictionaryCategoryDo.RESOURCE_SYMBOL,
                        createDirectoryTreeNode.getName()));
            }
        } else {
            Integer existsDuplicate = this.dictionaryCategoryRepository.existsByNameWithinParent(
                    createDirectoryTreeNode.getName(), parentUid);
            if (existsDuplicate > 0) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s",
                        DictionaryCategoryDo.RESOURCE_SYMBOL,
                        createDirectoryTreeNode.getName()));
            }

            boolean exists = this.dictionaryCategoryRepository.existsByUid(parentUid);
            if (!exists) {
                throw new AbcResourceNotFoundException(String.format("%s::uid=%s",
                        DictionaryCategoryDo.RESOURCE_SYMBOL,
                        parentUid));
            }
        }

        //
        // Step 2, core-processing
        //
        DictionaryCategoryDo dictionaryCategoryDo = new DictionaryCategoryDo();
        dictionaryCategoryDo.setUid(this.idHelper.getNextDistributedId(DictionaryCategoryDo.RESOURCE_NAME));
        dictionaryCategoryDo.setName(createDirectoryTreeNode.getName());
        dictionaryCategoryDo.setObjectName(createDirectoryTreeNode.getName()
                .replaceAll("_", "__")
                .replaceAll("\\s", "_").toLowerCase());
        dictionaryCategoryDo.setDescription(createDirectoryTreeNode.getDescription());
        dictionaryCategoryDo.setDirectory(Boolean.TRUE);
        dictionaryCategoryDo.setParentUid(parentUid);

        // sequence
        Float maxSequence = null;
        if (parentUid == null) {
            maxSequence = this.dictionaryCategoryRepository.findMaxSequenceWithoutParent();
        } else {
            maxSequence = this.dictionaryCategoryRepository.findMaxSequenceWithinParent(parentUid);
        }
        if (maxSequence == null) {
            dictionaryCategoryDo.setSequence(0.001f);
        } else {
            dictionaryCategoryDo.setSequence(maxSequence + 0.001f);
        }

        BaseDo.create(dictionaryCategoryDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dictionaryCategoryRepository.save(dictionaryCategoryDo);

        //
        // Step 3, post-processing
        //
        TreeNode treeNode = new TreeNode();
        treeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
        treeNode.setUid(dictionaryCategoryDo.getUid());
        treeNode.setName(dictionaryCategoryDo.getName());
        treeNode.setDescription(dictionaryCategoryDo.getDescription());
        treeNode.setType(TreeNode.GENERAL_TYPE_DIRECTORY);

        return treeNode;
    }

    @Override
    public void updateDirectoryNodeOfDictionaryCategoryHierarchy(
            Long uid,
            UpdateDirectoryTreeNode updateDirectoryTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DictionaryCategoryDo dictionaryCategoryDo = this.dictionaryCategoryRepository.findByUid(uid);
        if (dictionaryCategoryDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DictionaryCategoryDo.RESOURCE_SYMBOL,
                    uid));
        }
        if (!ObjectUtils.isEmpty(updateDirectoryTreeNode.getName())
                && !updateDirectoryTreeNode.getName().equalsIgnoreCase(dictionaryCategoryDo.getName())) {
            // name 要改
            if (dictionaryCategoryDo.getParentUid() == null) {
                // 没有 parent 的情况
                Integer existsDuplicate = this.dictionaryCategoryRepository.existsByNameWithoutParent(
                        updateDirectoryTreeNode.getName());
                if (existsDuplicate > 0) {
                    throw new AbcResourceDuplicateException(String.format("%s::name=%s",
                            DictionaryCategoryDo.RESOURCE_SYMBOL,
                            updateDirectoryTreeNode.getName()));
                }
            } else {
                // 有 parent 的情况
                Integer existsDuplicate = this.dictionaryCategoryRepository.existsByNameWithinParent(
                        updateDirectoryTreeNode.getName(), dictionaryCategoryDo.getParentUid());
                if (existsDuplicate > 0) {
                    throw new AbcResourceDuplicateException(String.format("%s::name=%s",
                            DictionaryCategoryDo.RESOURCE_SYMBOL,
                            updateDirectoryTreeNode.getName()));
                }
            }
        }

        //
        // Step 2, core-processing
        //
        boolean requiredToUpdate = false;

        if (!ObjectUtils.isEmpty(updateDirectoryTreeNode.getName())
                && !updateDirectoryTreeNode.getName().equalsIgnoreCase(dictionaryCategoryDo.getName())) {
            dictionaryCategoryDo.setName(updateDirectoryTreeNode.getName());
            dictionaryCategoryDo.setObjectName(updateDirectoryTreeNode.getName()
                    .replaceAll("_", "__")
                    .replaceAll("\\s", "_").toLowerCase());
            requiredToUpdate = true;
        }
        if (updateDirectoryTreeNode.getDescription() != null
                && !updateDirectoryTreeNode.getDescription().equalsIgnoreCase(dictionaryCategoryDo.getDescription())) {
            dictionaryCategoryDo.setDescription(updateDirectoryTreeNode.getDescription());
            requiredToUpdate = true;
        }

        if (requiredToUpdate) {
            BaseDo.update(dictionaryCategoryDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.dictionaryCategoryRepository.save(dictionaryCategoryDo);
        }

        //
        // Step 3, post-processing
        //
    }

    @Override
    public TreeNode createEntityNodeForDictionaryCategoryHierarchy(
            Long parentUid,
            CreateEntityTreeNode createEntityTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        if (parentUid == null) {
            Integer existsDuplicate = this.dictionaryCategoryRepository.existsByNameWithoutParent(
                    createEntityTreeNode.getName());
            if (existsDuplicate > 0) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s",
                        DictionaryCategoryDo.RESOURCE_SYMBOL,
                        createEntityTreeNode.getName()));
            }
        } else {
            Integer existsDuplicate = this.dictionaryCategoryRepository.existsByNameWithinParent(
                    createEntityTreeNode.getName(), parentUid);
            if (existsDuplicate > 0) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s",
                        DictionaryCategoryDo.RESOURCE_SYMBOL,
                        createEntityTreeNode.getName()));
            }

            boolean exists = this.dictionaryCategoryRepository.existsByUid(parentUid);
            if (!exists) {
                throw new AbcResourceNotFoundException(String.format("%s::uid=%s",
                        DictionaryCategoryDo.RESOURCE_SYMBOL,
                        parentUid));
            }
        }

        //
        // Step 2, core-processing
        //
        DictionaryCategoryDo dictionaryCategoryDo = new DictionaryCategoryDo();
        dictionaryCategoryDo.setUid(this.idHelper.getNextDistributedId(DictionaryCategoryDo.RESOURCE_NAME));
        dictionaryCategoryDo.setName(createEntityTreeNode.getName());
        dictionaryCategoryDo.setObjectName(createEntityTreeNode.getName()
                .replaceAll("_", "__")
                .replaceAll("\\s", "_").toLowerCase());
        dictionaryCategoryDo.setDescription(createEntityTreeNode.getDescription());
        dictionaryCategoryDo.setDirectory(Boolean.FALSE);
        dictionaryCategoryDo.setParentUid(parentUid);
        dictionaryCategoryDo.setVersion(1L);

        // entity node translation
        // N/A

        // sequence
        Float maxSequence = null;
        if (parentUid == null) {
            maxSequence = this.dictionaryCategoryRepository.findMaxSequenceWithoutParent();
        } else {
            maxSequence = this.dictionaryCategoryRepository.findMaxSequenceWithinParent(parentUid);
        }
        if (maxSequence == null) {
            dictionaryCategoryDo.setSequence(0.001f);
        } else {
            dictionaryCategoryDo.setSequence(maxSequence + 0.001f);
        }

        BaseDo.create(dictionaryCategoryDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dictionaryCategoryRepository.save(dictionaryCategoryDo);

        //
        // Step 3, post-processing
        //
        TreeNode treeNode = new TreeNode();
        treeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
        treeNode.setUid(dictionaryCategoryDo.getUid());
        treeNode.setName(dictionaryCategoryDo.getName());
        treeNode.setDescription(dictionaryCategoryDo.getDescription());
        treeNode.setType(TreeNode.GENERAL_TYPE_ENTITY);

        return treeNode;
    }

    @Override
    public void updateEntityNodeOfDictionaryCategoryHierarchy(
            Long uid,
            UpdateEntityTreeNode updateEntityTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DictionaryCategoryDo dictionaryCategoryDo = this.dictionaryCategoryRepository.findByUid(uid);
        if (dictionaryCategoryDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DictionaryCategoryDo.RESOURCE_SYMBOL,
                    uid));
        }
        if (!ObjectUtils.isEmpty(updateEntityTreeNode.getName())
                && !updateEntityTreeNode.getName().equalsIgnoreCase(dictionaryCategoryDo.getName())) {
            // name 要改
            if (dictionaryCategoryDo.getParentUid() == null) {
                // 没有 parent 的情况
                Integer existsDuplicate = this.dictionaryCategoryRepository.existsByNameWithoutParent(
                        updateEntityTreeNode.getName());
                if (existsDuplicate > 0) {
                    throw new AbcResourceDuplicateException(String.format("%s::name=%s",
                            DictionaryCategoryDo.RESOURCE_SYMBOL,
                            updateEntityTreeNode.getName()));
                }
            } else {
                // 有 parent 的情况
                Integer existsDuplicate = this.dictionaryCategoryRepository.existsByNameWithinParent(
                        updateEntityTreeNode.getName(), dictionaryCategoryDo.getParentUid());
                if (existsDuplicate > 0) {
                    throw new AbcResourceDuplicateException(String.format("%s::name=%s",
                            DictionaryCategoryDo.RESOURCE_SYMBOL,
                            updateEntityTreeNode.getName()));
                }
            }
        }

        //
        // Step 2, core-processing
        //
        boolean requiredToUpdate = false;

        if (!ObjectUtils.isEmpty(updateEntityTreeNode.getName())
                && !updateEntityTreeNode.getName().equalsIgnoreCase(dictionaryCategoryDo.getName())) {
            dictionaryCategoryDo.setName(updateEntityTreeNode.getName());
            dictionaryCategoryDo.setObjectName(updateEntityTreeNode.getName()
                    .replaceAll("_", "__")
                    .replaceAll("\\s", "_").toLowerCase());
            requiredToUpdate = true;
        }
        if (updateEntityTreeNode.getDescription() != null
                && !updateEntityTreeNode.getDescription().equalsIgnoreCase(dictionaryCategoryDo.getDescription())) {
            dictionaryCategoryDo.setDescription(updateEntityTreeNode.getDescription());
            requiredToUpdate = true;
        }

        if (requiredToUpdate) {
            BaseDo.update(dictionaryCategoryDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.dictionaryCategoryRepository.save(dictionaryCategoryDo);
        }

        //
        // Step 3, post-processing
        //
    }

    @Override
    public void replaceNodeRelationshipOfDictionaryCategoryHierarchy(
            Long uid,
            ReplaceTreeNodeRelationship replaceTreeNodeRelationship,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DictionaryCategoryDo itemDo = this.dictionaryCategoryRepository.findByUid(uid);
        if (itemDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DictionaryCategoryDo.RESOURCE_SYMBOL,
                    uid));
        }

        if (replaceTreeNodeRelationship.getReferenceTreeNodeUid() == null) {
            // 调整到根节点下，作为其最末尾子节点
            Specification<DictionaryCategoryDo> specification = new Specification<DictionaryCategoryDo>() {
                @Override
                public Predicate toPredicate(Root<DictionaryCategoryDo> root, CriteriaQuery<?> query,
                                             CriteriaBuilder criteriaBuilder) {
                    List<Predicate> predicateList = new ArrayList<>();
                    predicateList.add(criteriaBuilder.isNull(root.get("parentUid")));
                    return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
                }
            };

            Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("sequence")));
            Page<DictionaryCategoryDo> candidateItemDoPage = this.dictionaryCategoryRepository.findAll(specification, pageable);
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
            this.dictionaryCategoryRepository.save(itemDo);

            return;
        }

        DictionaryCategoryDo referenceItemDo =
                this.dictionaryCategoryRepository.findByUid(replaceTreeNodeRelationship.getReferenceTreeNodeUid());
        if (referenceItemDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DictionaryCategoryDo.RESOURCE_SYMBOL,
                    replaceTreeNodeRelationship.getReferenceTreeNodeUid()));
        }

        //
        // Step 2, core-processing
        //
        switch (replaceTreeNodeRelationship.getPosition()) {
            case FRONT: {
                itemDo.setParentUid(referenceItemDo.getParentUid());

                if (referenceItemDo.getSequence() == null) {
                    itemDo.setSequence(1.0f);
                    BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.dictionaryCategoryRepository.save(itemDo);

                    referenceItemDo.setSequence(2.0f);
                    BaseDo.update(referenceItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.dictionaryCategoryRepository.save(referenceItemDo);
                } else {
                    Specification<DictionaryCategoryDo> specification = new Specification<DictionaryCategoryDo>() {
                        @Override
                        public Predicate toPredicate(Root<DictionaryCategoryDo> root, CriteriaQuery<?> query,
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
                    Page<DictionaryCategoryDo> candidateItemDoPage = this.dictionaryCategoryRepository.findAll(specification, pageable);
                    if (candidateItemDoPage.isEmpty()) {
                        itemDo.setSequence(referenceItemDo.getSequence() * 0.5f);
                    } else {
                        float gap =
                                referenceItemDo.getSequence() - candidateItemDoPage.getContent().get(0).getSequence();
                        itemDo.setSequence(referenceItemDo.getSequence() - gap / 2);
                    }

                    BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.dictionaryCategoryRepository.save(itemDo);
                }
            }
            break;
            case CENTER: {
                itemDo.setParentUid(referenceItemDo.getUid());

                Specification<DictionaryCategoryDo> specification = new Specification<DictionaryCategoryDo>() {
                    @Override
                    public Predicate toPredicate(Root<DictionaryCategoryDo> root, CriteriaQuery<?> query,
                                                 CriteriaBuilder criteriaBuilder) {
                        List<Predicate> predicateList = new ArrayList<>();

                        predicateList.add(criteriaBuilder.equal(root.get("parentUid"), referenceItemDo.getUid()));
                        return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
                    }
                };

                Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("sequence")));
                Page<DictionaryCategoryDo> candidateDoPage = this.dictionaryCategoryRepository.findAll(specification, pageable);
                if (candidateDoPage.isEmpty()) {
                    itemDo.setSequence(1.0f);
                } else {
                    itemDo.setSequence(candidateDoPage.getContent().get(0).getSequence() + 1.0f);
                }

                BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                this.dictionaryCategoryRepository.save(itemDo);
            }
            break;
            case REAR: {
                itemDo.setParentUid(referenceItemDo.getParentUid());

                if (referenceItemDo.getSequence() == null) {
                    itemDo.setSequence(2.0f);
                    BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.dictionaryCategoryRepository.save(itemDo);

                    referenceItemDo.setSequence(1.0f);
                    BaseDo.update(referenceItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.dictionaryCategoryRepository.save(referenceItemDo);
                } else {
                    Specification<DictionaryCategoryDo> specification = new Specification<DictionaryCategoryDo>() {
                        @Override
                        public Predicate toPredicate(Root<DictionaryCategoryDo> root, CriteriaQuery<?> query,
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
                    Page<DictionaryCategoryDo> candidateDoPage = this.dictionaryCategoryRepository.findAll(specification, pageable);
                    if (candidateDoPage.isEmpty()) {
                        itemDo.setSequence(referenceItemDo.getSequence() + 1.0f);
                    } else {
                        float gap =
                                candidateDoPage.getContent().get(0).getSequence() - referenceItemDo.getSequence();
                        itemDo.setSequence(referenceItemDo.getSequence() + gap / 2);
                    }

                    BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.dictionaryCategoryRepository.save(itemDo);
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
    public List<String> listAllReferencesToNodeOfDictionaryCategoryHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DictionaryCategoryDo dictionaryCategoryDo = this.dictionaryCategoryRepository.findByUid(uid);
        if (dictionaryCategoryDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DictionaryCategoryDo.RESOURCE_SYMBOL,
                    uid));
        }

        //
        // Step 2, core-processing
        //
        return this.resourceReferenceManager.check(
                ResourceReferenceManager.ResourceCategoryEnum.DATA_DICTIONARY,
                dictionaryCategoryDo.getUid(),
                dictionaryCategoryDo.getName());
    }

    @Override
    public void deleteNodeOfDictionaryCategoryHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DictionaryCategoryDo dictionaryCategoryDo = this.dictionaryCategoryRepository.findByUid(uid);
        if (dictionaryCategoryDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DictionaryCategoryDo.RESOURCE_SYMBOL,
                    uid));
        }

        //
        // Step 2, core-processing
        //

        //
        // Step 3, post-processing
        //
        recursiveDeleteNodeOfDictionaryCategoryHierarchy(dictionaryCategoryDo, operatingUserProfile);
    }

    private void recursiveDeleteNodeOfDictionaryCategoryHierarchy(
            DictionaryCategoryDo dictionaryCategoryDo,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        // 找出所有的子节点，删除
        List<DictionaryCategoryDo> children =
                this.dictionaryCategoryRepository.findByParentUid(dictionaryCategoryDo.getUid());
        if (!CollectionUtils.isEmpty(children)) {
            children.forEach(child -> {
                recursiveDeleteNodeOfDictionaryCategoryHierarchy(child, operatingUserProfile);
            });
        }

        // 删除自己
        this.dictionaryCategoryRepository.delete(dictionaryCategoryDo);

        //
        DictionaryBuildDo dictionaryBuildDo =
                this.dictionaryBuildRepository.findByDictionaryCategoryUid(dictionaryCategoryDo.getUid());
        if (dictionaryBuildDo != null) {
            deleteDictionaryBuild(dictionaryBuildDo, operatingUserProfile);
        }

        // post event
        DataDictionaryDeletedEvent dataDictionaryDeletedEvent = new DataDictionaryDeletedEvent();
        dataDictionaryDeletedEvent.setUid(dictionaryCategoryDo.getUid());
        dataDictionaryDeletedEvent.setOperatingUserProfile(operatingUserProfile);
        this.eventBusManager.send(dataDictionaryDeletedEvent);
    }

    @Override
    public List<TreeNode> treeListingAllNodesOfDictionaryCategoryHierarchy(
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Map<Long, TreeNode> itemUidAndTreeNodeMap = new HashMap<>();
        List<TreeNode> rootTreeNodeList = new LinkedList<>();
        this.dictionaryCategoryRepository.findAll().forEach(itemDo -> {
            TreeNode treeNode = new TreeNode();
            treeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
            treeNode.setUid(itemDo.getUid());
            treeNode.setName(itemDo.getName());
            treeNode.setDescription(itemDo.getDescription());

            if (Boolean.TRUE.equals(itemDo.getDirectory())) {
                treeNode.setType(TreeNode.GENERAL_TYPE_DIRECTORY);
            } else {
                treeNode.setType(TreeNode.GENERAL_TYPE_ENTITY);
            }

            treeNode.setTags(new HashMap<>());

            treeNode.getTags().put("sequence", itemDo.getSequence());

            if (itemDo.getParentUid() != null) {
                treeNode.getTags().put("parent_uid", itemDo.getParentUid());
            }

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
    public TreeNode createEntityNodeForDictionaryStructureHierarchy(
            Long dictionaryCategoryUid,
            Long parentUid,
            CreateEntityTreeNode createEntityTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        // Step 1.1, dictionary structure level 同一级有且只能有1个节点
        if (parentUid == null) {
            DictionaryStructureNodeDo dictionaryStructureNodeDo =
                    this.dictionaryStructureNodeRepository.findByDictionaryCategoryUidWithNullParentUid(
                            dictionaryCategoryUid);
            if (dictionaryStructureNodeDo != null) {
                throw new AbcResourceDuplicateException(String.format("%s::each level can only have one node",
                        DictionaryStructureNodeDo.RESOURCE_SYMBOL));
            }
        } else {
            DictionaryStructureNodeDo dictionaryStructureNodeDo =
                    this.dictionaryStructureNodeRepository.findByDictionaryCategoryUidWithParentUid(
                            dictionaryCategoryUid, parentUid);
            if (dictionaryStructureNodeDo != null) {
                throw new AbcResourceDuplicateException(String.format("%s::each level can only have one node",
                        DictionaryStructureNodeDo.RESOURCE_SYMBOL));
            }
        }

        // Step 1.2, 同一 dictionary category 的 dictionary structure 中不同级 dictionary structure level 的名称不能相同
        boolean existsDuplicateName =
                this.dictionaryStructureNodeRepository.existsByDictionaryCategoryUidAndName(dictionaryCategoryUid,
                        createEntityTreeNode.getName());
        if (existsDuplicateName) {
            throw new AbcResourceDuplicateException(String.format("%s::the names of different levels cannot be the same",
                    DictionaryStructureNodeDo.RESOURCE_SYMBOL));
        }


        //
        // Step 2, core-processing
        //
        DictionaryStructureNodeDo dictionaryStructureNodeDo = new DictionaryStructureNodeDo();
        dictionaryStructureNodeDo.setUid(this.idHelper.getNextDistributedId(DictionaryStructureNodeDo.RESOURCE_NAME));
        dictionaryStructureNodeDo.setName(createEntityTreeNode.getName());
        dictionaryStructureNodeDo.setObjectName(createEntityTreeNode.getName()
                .replaceAll("_", "__")
                .replaceAll("\\s", "_").toLowerCase());
        dictionaryStructureNodeDo.setDescription(createEntityTreeNode.getDescription());
        dictionaryStructureNodeDo.setParentUid(parentUid);
        dictionaryStructureNodeDo.setDictionaryCategoryUid(dictionaryCategoryUid);
        BaseDo.create(dictionaryStructureNodeDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dictionaryStructureNodeRepository.save(dictionaryStructureNodeDo);

        //
        // Step 3, post-processing
        //
        TreeNode treeNode = new TreeNode();
        treeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
        treeNode.setUid(dictionaryStructureNodeDo.getUid());
        treeNode.setName(dictionaryStructureNodeDo.getName());
        treeNode.setDescription(dictionaryStructureNodeDo.getDescription());
        treeNode.setType(TreeNode.GENERAL_TYPE_ENTITY);
        return treeNode;
    }

    @Override
    public void updateEntityNodeOfDictionaryStructureHierarchy(
            Long uid,
            UpdateEntityTreeNode updateEntityTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DictionaryStructureNodeDo dictionaryStructureNodeDo =
                this.dictionaryStructureNodeRepository.findByUid(uid);
        if (dictionaryStructureNodeDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DictionaryStructureNodeDo.RESOURCE_SYMBOL,
                    uid));
        }

        if (!ObjectUtils.isEmpty(updateEntityTreeNode.getName())
                && !updateEntityTreeNode.getName().equals(dictionaryStructureNodeDo.getName())) {
            boolean existsDuplicateName =
                    this.dictionaryStructureNodeRepository.existsByDictionaryCategoryUidAndName(
                            dictionaryStructureNodeDo.getDictionaryCategoryUid(),
                            updateEntityTreeNode.getName());
            if (existsDuplicateName) {
                throw new AbcResourceDuplicateException("the names of different levels cannot be the same");
            }
        }

        //
        // Step 2, core-processing
        //
        boolean requiredToUpdate = false;
        if (!ObjectUtils.isEmpty(updateEntityTreeNode.getName())
                && !updateEntityTreeNode.getName().equals(dictionaryStructureNodeDo.getName())) {
            dictionaryStructureNodeDo.setName(updateEntityTreeNode.getName());
            dictionaryStructureNodeDo.setObjectName(updateEntityTreeNode.getName()
                    .replaceAll("_", "__")
                    .replaceAll("\\s", "_").toLowerCase());
            requiredToUpdate = true;
        }
        if (updateEntityTreeNode.getDescription() != null
                && !updateEntityTreeNode.getDescription().equals(dictionaryStructureNodeDo.getDescription())) {
            dictionaryStructureNodeDo.setDescription(updateEntityTreeNode.getDescription());
            requiredToUpdate = true;
        }
        if (requiredToUpdate) {
            BaseDo.update(dictionaryStructureNodeDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.dictionaryStructureNodeRepository.save(dictionaryStructureNodeDo);
        }

        //
        // Step 3, post-processing
        //
    }

    @Override
    public void replaceNodeRelationshipOfDictionaryStructureHierarchy(
            Long uid,
            ReplaceTreeNodeRelationship replaceTreeNodeRelationship,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        // N/A
    }

    @Override
    public List<String> listAllReferencesToNodeOfDictionaryStructureHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return null;
    }

    @Override
    public void deleteNodeOfDictionaryStructureHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DictionaryStructureNodeDo dictionaryStructureNodeDo =
                this.dictionaryStructureNodeRepository.findByUid(uid);
        if (dictionaryStructureNodeDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DictionaryStructureNodeDo.RESOURCE_SYMBOL,
                    uid));
        }

        boolean existsChild = this.dictionaryStructureNodeRepository.existsByParentUid(uid);
        if (existsChild) {
            throw new AbcResourceInUseException(String.format("%s::uid=%d",
                    DictionaryStructureNodeDo.RESOURCE_SYMBOL,
                    uid));
        }

        //
        // Step 2, core-processing
        //
        dictionaryStructureNodeDo.setDeleted(Boolean.TRUE);
        BaseDo.update(dictionaryStructureNodeDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dictionaryStructureNodeRepository.save(dictionaryStructureNodeDo);

        //
        // Step 3, post-processing
        //
    }

    @Override
    public TreeNode treeListingAllNodesOfDictionaryStructureHierarchy(
            Long dictionaryCategoryUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DictionaryCategoryDo dictionaryCategoryDo = this.dictionaryCategoryRepository.findByUid(dictionaryCategoryUid);
        if (dictionaryCategoryDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DictionaryCategoryDo.RESOURCE_SYMBOL,
                    dictionaryCategoryUid));
        }

        //
        // Step 2, core-processing
        //
        TreeNode dictionaryStructureTreeNode =
                buildDictionaryStructureTreeNodeOfOneDictionaryCategory(dictionaryCategoryDo,
                        operatingUserProfile);

        return dictionaryStructureTreeNode;
    }

    public TreeNode treeListingDictionaryStructureNodesOfOneDictionaryCategory(
            Long dictionaryCategoryUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DictionaryCategoryDo dictionaryCategoryDo = this.dictionaryCategoryRepository.findByUid(dictionaryCategoryUid);
        if (dictionaryCategoryDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DictionaryCategoryDo.RESOURCE_SYMBOL,
                    dictionaryCategoryUid));
        }

        //
        // Step 2, core-processing
        //

        TreeNode dictionaryStructureTreeNode =
                buildDictionaryStructureTreeNodeOfOneDictionaryCategory(dictionaryCategoryDo,
                        operatingUserProfile);

        TreeNode result = new TreeNode();
        result.setUid(this.idHelper.getNextStandaloneId(TreeNode.class.getName()));
        result.setName(dictionaryCategoryDo.getName());
        result.setDescription(dictionaryCategoryDo.getDescription());
        result.setType(DictionaryCategoryDo.TYPE);
        result.setTags(new HashMap<>());
        result.getTags().put("uid", dictionaryCategoryDo.getUid());

        if (dictionaryStructureTreeNode != null) {
            result.setChildren(new ArrayList<>(1));
            result.getChildren().add(dictionaryStructureTreeNode);
        }

        return result;
    }

    private TreeNode buildDictionaryStructureTreeNodeOfOneDictionaryCategory(
            DictionaryCategoryDo dictionaryCategoryDo,
            UserProfile operatingUserProfile) {
        //
        // Step 1, pre-processing
        //
        List<DictionaryStructureNodeDo> dictionaryStructureNodeDoList =
                this.dictionaryStructureNodeRepository.findByDictionaryCategoryUid(dictionaryCategoryDo.getUid());
        if (CollectionUtils.isEmpty(dictionaryStructureNodeDoList)) {
            return null;
        }

        //
        // Step 2, core-processing
        //

        // 没有 parent uid 的 dictionary structure node
        TreeNode rootTreeNode = null;

        Map<Long, DictionaryStructureNodeDo> dictionaryStructureNodeUidAndDictionaryStructureNodeDoMap = new HashMap<>();
        Map<Long, TreeNode> dictionaryStructureNodeUidAndTreeNodeMap = new HashMap<>();
        dictionaryStructureNodeDoList.forEach(dictionaryStructureNodeDo -> {
            TreeNode treeNode = new TreeNode();
            treeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
            treeNode.setUid(dictionaryStructureNodeDo.getUid());
            treeNode.setName(dictionaryStructureNodeDo.getName());
            treeNode.setDescription(dictionaryStructureNodeDo.getDescription());
            treeNode.setType(DictionaryStructureNodeDo.TYPE);
            dictionaryStructureNodeUidAndTreeNodeMap.put(dictionaryStructureNodeDo.getUid(), treeNode);
            dictionaryStructureNodeUidAndDictionaryStructureNodeDoMap.put(dictionaryStructureNodeDo.getUid(),
                    dictionaryStructureNodeDo);
        });

        for (Map.Entry<Long, TreeNode> entry : dictionaryStructureNodeUidAndTreeNodeMap.entrySet()) {
            Long dictionaryStructureNodeUid = entry.getKey();
            TreeNode treeNode = entry.getValue();

            DictionaryStructureNodeDo dictionaryStructureNodeDo =
                    dictionaryStructureNodeUidAndDictionaryStructureNodeDoMap.get(dictionaryStructureNodeUid);
            if (dictionaryStructureNodeDo.getParentUid() == null) {
                if (rootTreeNode == null) {
                    rootTreeNode = treeNode;
                } else {
                    LOGGER.warn("found more than 1 dictionary structure levels without parent uid, they are:{} and {}",
                            dictionaryStructureNodeDo.getUid(), rootTreeNode.getTags().get("uid"));
                }
            } else {
                TreeNode parentTreeNode =
                        dictionaryStructureNodeUidAndTreeNodeMap.get(dictionaryStructureNodeDo.getParentUid());
                if (parentTreeNode == null) {
                    LOGGER.warn("found parent uid that does not exist, dictionary structure level's parent uid:{}",
                            dictionaryStructureNodeDo.getParentUid());
                } else {
                    if (parentTreeNode.getChildren() == null) {
                        parentTreeNode.setChildren(new LinkedList<>());
                    }
                    parentTreeNode.getChildren().add(treeNode);
                }
            }
        }

        return rootTreeNode;
    }

    public List<TreeNode> treeListingDictionaryStructureNodesOfAllDictionaryCategories(
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        //
        // Step 2, core-processing
        //
        List<TreeNode> result = new LinkedList<>();

        // 从 DB 分页取出 dictionary category，并为每个 dictionary category 构建 tree node，
        // 包括该 dictionary category 的 dictionary structure node(s)
        int page = 0;
        int size = 100;
        Page<DictionaryCategoryDo> dictionaryCategoryDoPage = null;
        do {
            PageRequest pageRequest = PageRequest.of(page, size);
            dictionaryCategoryDoPage =
                    this.dictionaryCategoryRepository.findAll(pageRequest);
            if (dictionaryCategoryDoPage.isEmpty()) {
                break;
            }

            dictionaryCategoryDoPage.forEach(dictionaryCategoryDo -> {
                TreeNode dictionaryStructureTreeNode =
                        buildDictionaryStructureTreeNodeOfOneDictionaryCategory(dictionaryCategoryDo,
                                operatingUserProfile);

                TreeNode dictionaryCategoryTreeNode = new TreeNode();
                dictionaryCategoryTreeNode.setUid(this.idHelper.getNextStandaloneId(TreeNode.class.getName()));
                dictionaryCategoryTreeNode.setName(dictionaryCategoryDo.getName());
                dictionaryCategoryTreeNode.setDescription(dictionaryCategoryDo.getDescription());
                dictionaryCategoryTreeNode.setType(DictionaryCategoryDo.TYPE);
                dictionaryCategoryTreeNode.setTags(new HashMap<>());
                dictionaryCategoryTreeNode.getTags().put("uid", dictionaryCategoryDo.getUid());
                dictionaryCategoryTreeNode.getTags().put("sequence", dictionaryCategoryDo.getSequence());

                if (dictionaryStructureTreeNode != null) {
                    dictionaryCategoryTreeNode.setChildren(new ArrayList<>(1));
                    dictionaryCategoryTreeNode.getChildren().add(dictionaryStructureTreeNode);
                }

                result.add(dictionaryCategoryTreeNode);
            });

            page++;
        } while (true);

        //
        // Step 3, post-processing
        //

        return result;
    }

    @Override
    public TreeNode createEntityNodeForDictionaryContentHierarchy(
            Long dictionaryCategoryUid,
            Long parentUid,
            CreateEntityTreeNode createEntityTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DictionaryCategoryDo dictionaryCategoryDo = this.dictionaryCategoryRepository.findByUid(dictionaryCategoryUid);
        if (dictionaryCategoryDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DictionaryCategoryDo.RESOURCE_SYMBOL,
                    dictionaryCategoryUid));
        }

        JSONObject payload = createEntityTreeNode.getPayload();
        DictionaryContentEntityNode transformedPayload = JSONObject.toJavaObject(payload,
                DictionaryContentEntityNode.class);

        if (parentUid == null) {
            List<DictionaryContentNodeDo> dictionaryContentNodeDoList = null;

            // value 允许为 null
            if (transformedPayload.getValue() == null) {
                dictionaryContentNodeDoList =
                        this.dictionaryContentNodeRepository.findByDictionaryCategoryUidAndVersionAndNullParentUidAndNullValue(
                                dictionaryCategoryDo.getUid(), dictionaryCategoryDo.getVersion());
            } else {
                dictionaryContentNodeDoList =
                        this.dictionaryContentNodeRepository.findByDictionaryCategoryUidAndVersionAndNullParentUidAndValue(
                                dictionaryCategoryDo.getUid(),
                                dictionaryCategoryDo.getVersion(),
                                transformedPayload.getValue());
            }

            if (!CollectionUtils.isEmpty(dictionaryContentNodeDoList)) {
                throw new AbcResourceDuplicateException(String.format("%s::dictionary_category_uid=%d, value=%s, " +
                                "parent_uid=null",
                        DictionaryContentNodeDo.RESOURCE_SYMBOL,
                        dictionaryCategoryUid,
                        transformedPayload.getValue()));
            }

            // label 不允许为 null
            dictionaryContentNodeDoList =
                    this.dictionaryContentNodeRepository.findByDictionaryCategoryUidAndVersionAndNullParentUidAndLabel(
                            dictionaryCategoryDo.getUid(),
                            dictionaryCategoryDo.getVersion(),
                            transformedPayload.getLabel());
            if (!CollectionUtils.isEmpty(dictionaryContentNodeDoList)) {
                throw new AbcResourceDuplicateException(String.format("%s::dictionary_category_uid=%d, label=%s, " +
                                "parent_uid=null",
                        DictionaryContentNodeDo.RESOURCE_SYMBOL,
                        dictionaryCategoryUid,
                        transformedPayload.getLabel()));
            }
        } else {
            boolean existsParent =
                    this.dictionaryContentNodeRepository.existsByUid(parentUid);
            if (!existsParent) {
                throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                        DictionaryContentNodeDo.RESOURCE_SYMBOL,
                        parentUid));
            }

            List<DictionaryContentNodeDo> dictionaryContentNodeDoList = null;

            // value 允许为 null
            if (transformedPayload.getValue() == null) {
                dictionaryContentNodeDoList =
                        this.dictionaryContentNodeRepository.findByDictionaryCategoryUidAndVersionAndParentUidAndNullValue(
                                dictionaryCategoryDo.getUid(),
                                dictionaryCategoryDo.getVersion(),
                                parentUid);
            } else {
                dictionaryContentNodeDoList =
                        this.dictionaryContentNodeRepository.findByDictionaryCategoryUidAndVersionAndParentUidAndValue(
                                dictionaryCategoryDo.getUid(),
                                dictionaryCategoryDo.getVersion(),
                                parentUid,
                                transformedPayload.getValue());
            }
            if (!CollectionUtils.isEmpty(dictionaryContentNodeDoList)) {
                throw new AbcResourceDuplicateException(String.format("%s::dictionary_category_uid=%d, parent_uid=%d," +
                                " value=%s",
                        DictionaryContentNodeDo.RESOURCE_SYMBOL,
                        dictionaryCategoryUid,
                        parentUid,
                        transformedPayload.getValue()));
            }

            // label 不允许为 null
            dictionaryContentNodeDoList =
                    this.dictionaryContentNodeRepository.findByDictionaryCategoryUidAndVersionAndParentUidAndLabel(
                            dictionaryCategoryDo.getUid(),
                            dictionaryCategoryDo.getVersion(),
                            parentUid,
                            transformedPayload.getLabel());
            if (!CollectionUtils.isEmpty(dictionaryContentNodeDoList)) {
                throw new AbcResourceDuplicateException(String.format("%s::dictionary_category_uid=%d, parent_uid=%d," +
                                " label=%s",
                        DictionaryContentNodeDo.RESOURCE_SYMBOL,
                        dictionaryCategoryUid,
                        parentUid,
                        transformedPayload.getLabel()));
            }
        }

        //
        // Step 2, core-processing
        //
        DictionaryContentNodeDo dictionaryContentNodeDo = new DictionaryContentNodeDo();
        dictionaryContentNodeDo.setUid(this.idHelper.getNextDistributedId(DictionaryContentNodeDo.RESOURCE_NAME));
        dictionaryContentNodeDo.setValue(transformedPayload.getValue());
        dictionaryContentNodeDo.setLabelLogical(transformedPayload.getLabel());
        dictionaryContentNodeDo.setLabel(transformedPayload.getLabel());
        dictionaryContentNodeDo.setParentUid(parentUid);
        dictionaryContentNodeDo.setDictionaryCategoryUid(dictionaryCategoryUid);
        dictionaryContentNodeDo.setVersion(dictionaryCategoryDo.getVersion());

        Float maxSequence = null;
        if (parentUid == null) {
            maxSequence =
                    this.dictionaryContentNodeRepository.findMaxSequenceWithoutParent(dictionaryCategoryUid);
        } else {
            maxSequence =
                    this.dictionaryContentNodeRepository.findMaxSequenceWithinParent(dictionaryCategoryUid, parentUid);
        }
        Float sequence = 1.0f;
        if (maxSequence != null) {
            sequence = maxSequence + 1.0f;
        }
        dictionaryContentNodeDo.setSequence(sequence);

        BaseDo.create(dictionaryContentNodeDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dictionaryContentNodeRepository.save(dictionaryContentNodeDo);

        //
        // Step 3, post-processing
        //
        TreeNode treeNode = new TreeNode();
        treeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
        treeNode.setUid(dictionaryContentNodeDo.getUid());
        treeNode.setName(dictionaryContentNodeDo.getLabel());
        treeNode.setDescription(dictionaryContentNodeDo.getValue());
        treeNode.setType(TreeNode.GENERAL_TYPE_ENTITY);
        treeNode.setTags(new HashMap<>());
        treeNode.getTags().put("value", dictionaryContentNodeDo.getValue());
        treeNode.getTags().put("label", dictionaryContentNodeDo.getLabel());
        treeNode.getTags().put("sequence", dictionaryContentNodeDo.getSequence());
        return treeNode;
    }

    @Override
    public void updateEntityNodeOfDictionaryContentHierarchy(
            Long uid,
            UpdateEntityTreeNode updateEntityTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DictionaryContentNodeDo dictionaryContentNodeDo =
                this.dictionaryContentNodeRepository.findByUid(uid);
        if (dictionaryContentNodeDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DictionaryContentNodeDo.RESOURCE_SYMBOL,
                    uid));
        }

        Long dictionaryCategoryUid = dictionaryContentNodeDo.getDictionaryCategoryUid();
        DictionaryCategoryDo dictionaryCategoryDo = this.dictionaryCategoryRepository.findByUid(dictionaryCategoryUid);
        if (dictionaryCategoryDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DictionaryContentNodeDo.RESOURCE_SYMBOL,
                    dictionaryCategoryUid));
        }

        JSONObject payload = updateEntityTreeNode.getPayload();
        if (payload == null) {
            return;
        }
        DictionaryContentEntityNode transformedPayload = JSONObject.toJavaObject(payload,
                DictionaryContentEntityNode.class);
        boolean requiredToUpdate = false;
        if (transformedPayload.getValue() == null) {
            if (dictionaryContentNodeDo.getValue() != null) {
                requiredToUpdate = true;
            }
        } else {
            if (!transformedPayload.getValue().equals(dictionaryContentNodeDo.getValue())) {
                requiredToUpdate = true;
            }
        }
        if (transformedPayload.getLabel() == null) {
            if (dictionaryContentNodeDo.getLabel() != null) {
                requiredToUpdate = true;
            }
        } else {
            if (!transformedPayload.getLabel().equals(dictionaryContentNodeDo.getLabel())) {
                requiredToUpdate = true;
            }
        }
        if (!requiredToUpdate) {
            return;
        }

        Long parentUid = dictionaryContentNodeDo.getParentUid();

        if (parentUid == null) {
            List<DictionaryContentNodeDo> dictionaryContentNodeDoList = null;

            // value 允许为 null
            if (transformedPayload.getValue() == null) {
                dictionaryContentNodeDoList =
                        this.dictionaryContentNodeRepository.findByDictionaryCategoryUidAndVersionAndNullParentUidAndNullValue(
                                dictionaryCategoryDo.getUid(),
                                dictionaryCategoryDo.getVersion());
            } else {
                dictionaryContentNodeDoList =
                        this.dictionaryContentNodeRepository.findByDictionaryCategoryUidAndVersionAndNullParentUidAndValue(
                                dictionaryCategoryDo.getUid(),
                                dictionaryCategoryDo.getVersion(),
                                transformedPayload.getValue());
            }

            if (!CollectionUtils.isEmpty(dictionaryContentNodeDoList)) {
                throw new AbcResourceDuplicateException(String.format("%s::dictionary_category_uid=%d, value=%s, " +
                                "parent_uid=null",
                        DictionaryContentNodeDo.RESOURCE_SYMBOL,
                        dictionaryCategoryUid,
                        transformedPayload.getValue()));
            }

            // label 不允许为 null
            dictionaryContentNodeDoList =
                    this.dictionaryContentNodeRepository.findByDictionaryCategoryUidAndVersionAndNullParentUidAndLabel(
                            dictionaryCategoryDo.getUid(),
                            dictionaryCategoryDo.getVersion(),
                            transformedPayload.getLabel());
            if (!CollectionUtils.isEmpty(dictionaryContentNodeDoList)) {
                throw new AbcResourceDuplicateException(String.format("%s::dictionary_category_uid=%d, label=%s, " +
                                "parent_uid=null",
                        DictionaryContentNodeDo.RESOURCE_SYMBOL,
                        dictionaryCategoryUid,
                        transformedPayload.getLabel()));
            }
        } else {
            boolean existsParent =
                    this.dictionaryContentNodeRepository.existsByUid(parentUid);
            if (!existsParent) {
                throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                        DictionaryContentNodeDo.RESOURCE_SYMBOL,
                        parentUid));
            }

            List<DictionaryContentNodeDo> dictionaryContentNodeDoList = null;

            // value 允许为 null
            if (transformedPayload.getValue() == null) {
                dictionaryContentNodeDoList =
                        this.dictionaryContentNodeRepository.findByDictionaryCategoryUidAndVersionAndParentUidAndNullValue(
                                dictionaryCategoryDo.getUid(),
                                dictionaryCategoryDo.getVersion(),
                                parentUid);
            } else {
                dictionaryContentNodeDoList =
                        this.dictionaryContentNodeRepository.findByDictionaryCategoryUidAndVersionAndParentUidAndValue(
                                dictionaryCategoryDo.getUid(),
                                dictionaryCategoryDo.getVersion(),
                                parentUid,
                                transformedPayload.getValue());
            }
            if (!CollectionUtils.isEmpty(dictionaryContentNodeDoList)) {
                throw new AbcResourceDuplicateException(String.format("%s::dictionary_category_uid=%d, parent_uid=%d," +
                                " value=%s",
                        DictionaryContentNodeDo.RESOURCE_SYMBOL,
                        dictionaryCategoryUid,
                        parentUid,
                        transformedPayload.getValue()));
            }

            // label 不允许为 null
            dictionaryContentNodeDoList =
                    this.dictionaryContentNodeRepository.findByDictionaryCategoryUidAndVersionAndParentUidAndLabel(
                            dictionaryCategoryDo.getUid(),
                            dictionaryCategoryDo.getVersion(),
                            parentUid,
                            transformedPayload.getLabel());
            if (!CollectionUtils.isEmpty(dictionaryContentNodeDoList)) {
                throw new AbcResourceDuplicateException(String.format("%s::dictionary_category_uid=%d, parent_uid=%d," +
                                " label=%s",
                        DictionaryContentNodeDo.RESOURCE_SYMBOL,
                        dictionaryCategoryUid,
                        parentUid,
                        transformedPayload.getLabel()));
            }
        }


        //
        // Step 2, core-processing
        //
        dictionaryContentNodeDo.setValue(transformedPayload.getValue());
        dictionaryContentNodeDo.setLabelLogical(transformedPayload.getLabel());
        dictionaryContentNodeDo.setLabel(transformedPayload.getLabel());
        BaseDo.update(dictionaryContentNodeDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dictionaryContentNodeRepository.save(dictionaryContentNodeDo);

        //
        // Step 3, post-processing
        //
    }

    @Override
    public void replaceNodeRelationshipOfDictionaryContentHierarchy(
            Long uid,
            ReplaceTreeNodeRelationship replaceTreeNodeRelationship,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DictionaryContentNodeDo itemDo = this.dictionaryContentNodeRepository.findByUid(uid);
        if (itemDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DictionaryContentNodeDo.RESOURCE_SYMBOL,
                    uid));
        }

        if (replaceTreeNodeRelationship.getReferenceTreeNodeUid() == null) {
            // 调整到根节点下，作为其最末尾子节点
            Specification<DictionaryContentNodeDo> specification = new Specification<DictionaryContentNodeDo>() {
                @Override
                public Predicate toPredicate(Root<DictionaryContentNodeDo> root, CriteriaQuery<?> query,
                                             CriteriaBuilder criteriaBuilder) {
                    List<Predicate> predicateList = new ArrayList<>();
                    predicateList.add(criteriaBuilder.isNull(root.get("parentUid")));
                    return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
                }
            };

            Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("sequence")));
            Page<DictionaryContentNodeDo> candidateItemDoPage = this.dictionaryContentNodeRepository.findAll(specification, pageable);
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
            this.dictionaryContentNodeRepository.save(itemDo);

            return;
        }

        DictionaryContentNodeDo referenceItemDo =
                this.dictionaryContentNodeRepository.findByUid(replaceTreeNodeRelationship.getReferenceTreeNodeUid());
        if (referenceItemDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DictionaryContentNodeDo.RESOURCE_SYMBOL,
                    replaceTreeNodeRelationship.getReferenceTreeNodeUid()));
        }

        //
        // Step 2, core-processing
        //
        switch (replaceTreeNodeRelationship.getPosition()) {
            case FRONT: {
                itemDo.setParentUid(referenceItemDo.getParentUid());

                if (referenceItemDo.getSequence() == null) {
                    itemDo.setSequence(1.0f);
                    BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.dictionaryContentNodeRepository.save(itemDo);

                    referenceItemDo.setSequence(2.0f);
                    BaseDo.update(referenceItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.dictionaryContentNodeRepository.save(referenceItemDo);
                } else {
                    Specification<DictionaryContentNodeDo> specification = new Specification<DictionaryContentNodeDo>() {
                        @Override
                        public Predicate toPredicate(Root<DictionaryContentNodeDo> root, CriteriaQuery<?> query,
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
                    Page<DictionaryContentNodeDo> candidateItemDoPage = this.dictionaryContentNodeRepository.findAll(specification, pageable);
                    if (candidateItemDoPage.isEmpty()) {
                        itemDo.setSequence(referenceItemDo.getSequence() * 0.5f);
                    } else {
                        float gap =
                                referenceItemDo.getSequence() - candidateItemDoPage.getContent().get(0).getSequence();
                        itemDo.setSequence(referenceItemDo.getSequence() - gap / 2);
                    }

                    BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.dictionaryContentNodeRepository.save(itemDo);
                }
            }
            break;
            case CENTER: {
                itemDo.setParentUid(referenceItemDo.getUid());

                Specification<DictionaryContentNodeDo> specification = new Specification<DictionaryContentNodeDo>() {
                    @Override
                    public Predicate toPredicate(Root<DictionaryContentNodeDo> root, CriteriaQuery<?> query,
                                                 CriteriaBuilder criteriaBuilder) {
                        List<Predicate> predicateList = new ArrayList<>();

                        predicateList.add(criteriaBuilder.equal(root.get("parentUid"), referenceItemDo.getUid()));
                        return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
                    }
                };

                Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("sequence")));
                Page<DictionaryContentNodeDo> candidateDoPage = this.dictionaryContentNodeRepository.findAll(specification, pageable);
                if (candidateDoPage.isEmpty()) {
                    itemDo.setSequence(1.0f);
                } else {
                    itemDo.setSequence(candidateDoPage.getContent().get(0).getSequence() + 1.0f);
                }

                BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                this.dictionaryContentNodeRepository.save(itemDo);
            }
            break;
            case REAR: {
                itemDo.setParentUid(referenceItemDo.getParentUid());

                if (referenceItemDo.getSequence() == null) {
                    itemDo.setSequence(2.0f);
                    BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.dictionaryContentNodeRepository.save(itemDo);

                    referenceItemDo.setSequence(1.0f);
                    BaseDo.update(referenceItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.dictionaryContentNodeRepository.save(referenceItemDo);
                } else {
                    Specification<DictionaryContentNodeDo> specification = new Specification<DictionaryContentNodeDo>() {
                        @Override
                        public Predicate toPredicate(Root<DictionaryContentNodeDo> root, CriteriaQuery<?> query,
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
                    Page<DictionaryContentNodeDo> candidateDoPage = this.dictionaryContentNodeRepository.findAll(specification, pageable);
                    if (candidateDoPage.isEmpty()) {
                        itemDo.setSequence(referenceItemDo.getSequence() + 1.0f);
                    } else {
                        float gap =
                                candidateDoPage.getContent().get(0).getSequence() - referenceItemDo.getSequence();
                        itemDo.setSequence(referenceItemDo.getSequence() + gap / 2);
                    }

                    BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.dictionaryContentNodeRepository.save(itemDo);
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
    public List<String> listAllReferencesToNodeOfDictionaryContentHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return null;
    }

    @Override
    public void deleteNodeOfDictionaryContentHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DictionaryContentNodeDo dictionaryContentNodeDo =
                this.dictionaryContentNodeRepository.findByUid(uid);
        if (dictionaryContentNodeDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DictionaryContentNodeDo.RESOURCE_SYMBOL,
                    uid));
        }

        //
        // Step 2, core-processing
        //
        recursivelyDeleteNodeOfDictionaryContentHierarchy(dictionaryContentNodeDo, operatingUserProfile);

        //
        // Step 3, post-processing
        //
    }

    private void recursivelyDeleteNodeOfDictionaryContentHierarchy(
            DictionaryContentNodeDo dictionaryContentNodeDo,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        List<DictionaryContentNodeDo> childDictionaryContentNodeDoList =
                this.dictionaryContentNodeRepository.findByParentUid(dictionaryContentNodeDo.getUid());
        if (!CollectionUtils.isEmpty(childDictionaryContentNodeDoList)) {
            childDictionaryContentNodeDoList.forEach(childDictionaryContentNodeDo -> {
                recursivelyDeleteNodeOfDictionaryContentHierarchy(childDictionaryContentNodeDo, operatingUserProfile);
            });
        }

        this.dictionaryContentNodeRepository.delete(dictionaryContentNodeDo);
    }

    @Override
    public List<TreeNode> treeListingAllNodesOfDictionaryContentHierarchy(
            Long dictionaryCategoryUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DictionaryCategoryDo dictionaryCategoryDo = this.dictionaryCategoryRepository.findByUid(dictionaryCategoryUid);
        if (dictionaryCategoryDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DictionaryCategoryDo.RESOURCE_SYMBOL,
                    dictionaryCategoryUid));
        }

        //
        // Step 2, core-processing
        //
        List<TreeNode> dictionaryContentTreeNodeList =
                buildDictionaryContentTreeNodesOfOneDictionaryCategory(dictionaryCategoryDo,
                        operatingUserProfile);

        // 排序
        recursivelySortingTreeNodes(dictionaryContentTreeNodeList);

        return dictionaryContentTreeNodeList;
    }

    public TreeNode treeListingDictionaryContentNodesOfOneDictionaryCategory(
            Long dictionaryCategoryUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DictionaryCategoryDo dictionaryCategoryDo = this.dictionaryCategoryRepository.findByUid(dictionaryCategoryUid);
        if (dictionaryCategoryDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DictionaryCategoryDo.RESOURCE_SYMBOL,
                    dictionaryCategoryUid));
        }

        //
        // Step 2, core-processing
        //
        List<TreeNode> dictionaryContentTreeNodeList =
                buildDictionaryContentTreeNodesOfOneDictionaryCategory(dictionaryCategoryDo,
                        operatingUserProfile);

        TreeNode result = new TreeNode();
        result.setUid(this.idHelper.getNextStandaloneId(TreeNode.class.getName()));
        result.setName(dictionaryCategoryDo.getName());
        result.setDescription(dictionaryCategoryDo.getDescription());
        result.setType(DictionaryCategoryDo.TYPE);
        result.setTags(new HashMap<>());
        result.getTags().put("uid", dictionaryCategoryDo.getUid());

        if (!CollectionUtils.isEmpty(dictionaryContentTreeNodeList)) {
            // 排序
            recursivelySortingTreeNodes(dictionaryContentTreeNodeList);

            result.setChildren(dictionaryContentTreeNodeList);
        }

        return result;
    }

    private List<TreeNode> buildDictionaryContentTreeNodesOfOneDictionaryCategory(
            DictionaryCategoryDo dictionaryCategoryDo,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        //
        // Step 2, core-processing
        //

        List<TreeNode> result = new LinkedList<>();
        Map<Long, TreeNode> dictionaryContentNodeUidAndTreeNodeMap = new HashMap<>();
        Map<Long, Long> dictionaryContentNodeUidAndParentUidMap = new HashMap<>();

        // Step 2.1, 从 DB 分页取出 dictionary content node, 并为每个 dictionary content node 构建 tree node
        int page = 0;
        int size = 1000;
        Page<DictionaryContentNodeDo> dictionaryContentNodeDoPage = null;
        do {
            PageRequest pageRequest = PageRequest.of(page, size);
            dictionaryContentNodeDoPage =
                    this.dictionaryContentNodeRepository.findByDictionaryCategoryUidAndVersion(
                            dictionaryCategoryDo.getUid(),
                            dictionaryCategoryDo.getVersion(),
                            pageRequest);
            if (dictionaryContentNodeDoPage.isEmpty()) {
                break;
            }

            dictionaryContentNodeDoPage.forEach(dictionaryContentNodeDo -> {
                TreeNode treeNode = new TreeNode();
                treeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
                treeNode.setUid(dictionaryContentNodeDo.getUid());
                treeNode.setName(dictionaryContentNodeDo.getLabel());
                treeNode.setDescription(dictionaryContentNodeDo.getValue());
                treeNode.setType(DictionaryContentNodeDo.TYPE);
                treeNode.setTags(new HashMap<>());
                treeNode.getTags().put("label", dictionaryContentNodeDo.getLabel());
                treeNode.getTags().put("value", dictionaryContentNodeDo.getValue());
                treeNode.getTags().put("sequence", dictionaryContentNodeDo.getSequence());
                dictionaryContentNodeUidAndTreeNodeMap.put(dictionaryContentNodeDo.getUid(), treeNode);
                if (dictionaryContentNodeDo.getParentUid() != null) {
                    dictionaryContentNodeUidAndParentUidMap.put(dictionaryContentNodeDo.getUid(),
                            dictionaryContentNodeDo.getParentUid());
                }
            });

            page++;
        } while (true);

        // Step 2.2, 根据 dictionary content node 的 parent，为这些 tree node 构建 parent/child 关系
        dictionaryContentNodeUidAndTreeNodeMap.forEach((dictionaryContentNodeUid, treeNode) -> {
            Long parentDictionaryContentNodeUid =
                    dictionaryContentNodeUidAndParentUidMap.get(dictionaryContentNodeUid);
            if (parentDictionaryContentNodeUid == null) {
                result.add(treeNode);
            } else {
                TreeNode parentTreeNode =
                        dictionaryContentNodeUidAndTreeNodeMap.get(parentDictionaryContentNodeUid);
                if (parentTreeNode == null) {
                    LOGGER.warn("found parent uid that does not exist, dictionary content node's parent uid:{}",
                            parentDictionaryContentNodeUid);
                } else {
                    if (parentTreeNode.getChildren() == null) {
                        parentTreeNode.setChildren(new LinkedList<>());
                    }
                    parentTreeNode.getChildren().add(treeNode);
                }
            }
        });

        //
        // Step 3, post-processing
        //
        return result;
    }

    public List<TreeNode> treeListingDictionaryContentNodesOfAllDictionaryCategories(
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        //
        // Step 2, core-processing
        //

        List<TreeNode> result = new LinkedList<>();

        // Step 2.1, 从 DB 分页取出 dictionary category，为每个 dictionary category 构建 tree node，也为每个 dictionary category 的每个
        // dictionary content node 构建 tree node
        int page = 0;
        int size = 100;
        Page<DictionaryCategoryDo> dictionaryCategoryDoPage = null;
        do {
            PageRequest pageRequest = PageRequest.of(page, size);
            dictionaryCategoryDoPage =
                    this.dictionaryCategoryRepository.findAll(pageRequest);
            if (dictionaryCategoryDoPage.isEmpty()) {
                break;
            }

            dictionaryCategoryDoPage.forEach(dictionaryCategoryDo -> {
                List<TreeNode> dictionaryContentTreeNodeList =
                        buildDictionaryContentTreeNodesOfOneDictionaryCategory(dictionaryCategoryDo,
                                operatingUserProfile);

                TreeNode dictionaryCategoryTreeNode = new TreeNode();
                dictionaryCategoryTreeNode.setUid(this.idHelper.getNextStandaloneId(TreeNode.class.getName()));
                dictionaryCategoryTreeNode.setName(dictionaryCategoryDo.getName());
                dictionaryCategoryTreeNode.setDescription(dictionaryCategoryDo.getDescription());
                dictionaryCategoryTreeNode.setType(DictionaryCategoryDo.TYPE);
                dictionaryCategoryTreeNode.setTags(new HashMap<>());
                dictionaryCategoryTreeNode.getTags().put("uid", dictionaryCategoryDo.getUid());
                dictionaryCategoryTreeNode.getTags().put("sequence", dictionaryCategoryDo.getSequence());

                if (!CollectionUtils.isEmpty(dictionaryContentTreeNodeList)) {
                    dictionaryCategoryTreeNode.setChildren(dictionaryContentTreeNodeList);
                }

                result.add(dictionaryCategoryTreeNode);
            });

            page++;
        } while (true);

        //
        // Step 3, post-processing
        //
        // 排序
        recursivelySortingTreeNodes(result);

        return result;
    }

    @Override
    public List<TreeNode> treeListingFirstLevelOfDictionaryContentHierarchy(
            Long dictionaryCategoryUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DictionaryCategoryDo dictionaryCategoryDo = this.dictionaryCategoryRepository.findByUid(dictionaryCategoryUid);
        if (dictionaryCategoryDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DictionaryCategoryDo.RESOURCE_SYMBOL,
                    dictionaryCategoryUid));
        }

        //
        // Step 2, core-processing
        //

        // 从 DB 中取出指定 dictionary category 的没有 parent 的 dictionary content node(s)
        List<DictionaryContentNodeDo> dictionaryContentNodeDoList =
                this.dictionaryContentNodeRepository.findByDictionaryCategoryUidAndVersionAndNullParentUid(
                        dictionaryCategoryDo.getUid(), dictionaryCategoryDo.getVersion());
        if (CollectionUtils.isEmpty(dictionaryContentNodeDoList)) {
            return null;
        }

        List<TreeNode> result = new ArrayList<>(dictionaryContentNodeDoList.size());
        dictionaryContentNodeDoList.forEach(dictionaryContentNodeDo -> {
            TreeNode treeNode = new TreeNode();
            treeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
            treeNode.setUid(dictionaryContentNodeDo.getUid());
            treeNode.setName(dictionaryContentNodeDo.getLabel());
            treeNode.setDescription(dictionaryContentNodeDo.getValue());
            treeNode.setType(DictionaryContentNodeDo.TYPE);
            treeNode.setTags(new HashMap<>());
            treeNode.getTags().put("label", dictionaryContentNodeDo.getLabel());
            treeNode.getTags().put("value", dictionaryContentNodeDo.getValue());
            treeNode.getTags().put("sequence", dictionaryContentNodeDo.getSequence());
            result.add(treeNode);
        });

        //
        // Step 3, post-processing
        //
        // 排序
        recursivelySortingTreeNodes(result);

        return result;
    }

    @Override
    public List<TreeNode> treeListingNextLevelOfDictionaryContentHierarchy(
            Long dictionaryContentUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        //
        // Step 2, core-processing
        //

        List<DictionaryContentNodeDo> dictionaryContentNodeDoList =
                this.dictionaryContentNodeRepository.findByParentUid(dictionaryContentUid);
        if (CollectionUtils.isEmpty(dictionaryContentNodeDoList)) {
            return null;
        }

        List<TreeNode> result = new ArrayList<>(dictionaryContentNodeDoList.size());
        dictionaryContentNodeDoList.forEach(dictionaryContentNodeDo -> {
            TreeNode treeNode = new TreeNode();
            treeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
            treeNode.setUid(dictionaryContentNodeDo.getUid());
            treeNode.setName(dictionaryContentNodeDo.getLabel());
            treeNode.setDescription(dictionaryContentNodeDo.getValue());
            treeNode.setType(DictionaryContentNodeDo.TYPE);
            treeNode.setTags(new HashMap<>());
            treeNode.getTags().put("label", dictionaryContentNodeDo.getLabel());
            treeNode.getTags().put("value", dictionaryContentNodeDo.getValue());
            treeNode.getTags().put("sequence", dictionaryContentNodeDo.getSequence());
            result.add(treeNode);
        });

        //
        // Step 3, post-processing
        //
        // 排序
        recursivelySortingTreeNodes(result);

        return result;
    }

    @Override
    public List<TreeNode> treeQueryingNodesOfDictionaryContentHierarchy(
            Long dictionaryCategoryUid,
            String value,
            String label,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        //
        // Step 2, core-processing
        //

        List<TreeNode> result = new LinkedList<>();

        //
        // Step 2.1, 先搜索 DB，找出直接符合搜索条件的 dictionary content node(s)
        //
        Specification<DictionaryContentNodeDo> specification = new Specification<DictionaryContentNodeDo>() {
            @Override
            public Predicate toPredicate(Root<DictionaryContentNodeDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (dictionaryCategoryUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("dictionaryCategoryUid"), dictionaryCategoryUid));
                }
                if (!ObjectUtils.isEmpty(value)) {
                    predicateList.add(criteriaBuilder.like(root.get("value"), "%" + value + "%"));
                }
                if (!ObjectUtils.isEmpty(label)) {
                    predicateList.add(criteriaBuilder.like(root.get("label"), "%" + label + "%"));
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        // 包含了跟搜索条件直接相关的 dictionary content node(s)，为每一个都构建 tree node
        //
        // L1 key --- dictionary content node uid, L1 value --- dictionary content node (tree node)
        Map<Long, TreeNode> directlyRelatedContentNodeUidAndTreeNodeMap = new HashMap<>();
        // L1 key --- dictionary content node uid, L1 value --- dictionary content node (uid)'s parent node (uid)
        Map<Long, Long> directlyRelatedContentNodeUidAndParentUidMap = new HashMap<>();

        int page = 0;
        int size = 1000;
        Page<DictionaryContentNodeDo> itemDoPage = null;
        do {
            PageRequest pageRequest = PageRequest.of(page++, size);
            itemDoPage = this.dictionaryContentNodeRepository.findAll(specification, pageRequest);
            if (itemDoPage.isEmpty()) {
                break;
            }

            buildTreeNodeForEachDictionaryContentNode(
                    directlyRelatedContentNodeUidAndTreeNodeMap,
                    directlyRelatedContentNodeUidAndParentUidMap,
                    itemDoPage.getContent());
        } while (true);

        //
        // Step 2.3, 为了构造 tree node(s), 只有跟搜索条件直接相关的 dictionary content node(s) 作为 tree node(s) 还不够，
        // 还需要递归为每个 dictionary content node (tree node) 找到 parent
        //

        // 包含了跟搜索条件直接相关的 dictionary content node (tree node)，以及它们各自的 parent node (tree node)
        //
        // L1 key --- dictionary content node uid
        Map<Long, TreeNode> allContentNodeUidAndTreeNodeMap = new HashMap<>();
        // L1 key --- dictionary content node uid
        Map<Long, Long> allContentNodeUidAndParentUidMap = new HashMap<>();

        recursivelyBuildTreeNodeForEachParentDictionaryContentNode(
                directlyRelatedContentNodeUidAndTreeNodeMap,
                directlyRelatedContentNodeUidAndParentUidMap,
                allContentNodeUidAndTreeNodeMap,
                allContentNodeUidAndParentUidMap);

        //
        // Step 2.4, 梳理 tree node 之间的 parent/child 关系
        //
        allContentNodeUidAndTreeNodeMap.forEach(
                (dictionaryContentUid,
                 dictionaryContentTreeNode) -> {
                    Long parentDictionaryContentNodeUid =
                            allContentNodeUidAndParentUidMap.get(dictionaryContentUid);
                    if (parentDictionaryContentNodeUid == null) {
                        // 第一级 dictionary content node
                        result.add(dictionaryContentTreeNode);
                    } else {
                        // 非第一级 dictionary content node
                        TreeNode parentTreeNode =
                                allContentNodeUidAndTreeNodeMap.get(parentDictionaryContentNodeUid);
                        if (parentTreeNode == null) {
                            LOGGER.warn("found parent uid that does not exist, dictionary content node's parent uid:{}",
                                    parentDictionaryContentNodeUid);
                        } else {
                            if (parentTreeNode.getChildren() == null) {
                                parentTreeNode.setChildren(new LinkedList<>());
                            }
                            parentTreeNode.getChildren().add(dictionaryContentTreeNode);
                        }
                    }
                });

        //
        // Step 3, post-processing
        //
        // 排序
        recursivelySortingTreeNodes(result);

        return result;
    }

    @Override
    public List<TreeNode> treeListingFirstLevelOfDictionaryContentHierarchyWithTaggingSelected(
            Long dictionaryCategoryUid,
            List<Long> uidList,
            List<String> valueList,
            List<String> labelList,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        /**
         * Step 1, pre-processing
         */
        if (CollectionUtils.isEmpty(uidList)
                && CollectionUtils.isEmpty(valueList)
                && CollectionUtils.isEmpty(labelList)) {
            return treeListingFirstLevelOfDictionaryContentHierarchy(dictionaryCategoryUid, operatingUserProfile);
        }

        Specification<DictionaryContentNodeDo> specification = new Specification<DictionaryContentNodeDo>() {
            @Override
            public Predicate toPredicate(Root<DictionaryContentNodeDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (dictionaryCategoryUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("dictionaryCategoryUid"), dictionaryCategoryUid));
                }
                if (!CollectionUtils.isEmpty(uidList)) {
                    CriteriaBuilder.In<Long> in = criteriaBuilder.in(root.get("uid"));
                    uidList.forEach(uid -> {
                        in.value(uid);
                    });
                    predicateList.add(in);
                }
                if (!CollectionUtils.isEmpty(valueList)) {
                    CriteriaBuilder.In<String> in = criteriaBuilder.in(root.get("value"));
                    valueList.forEach(value -> {
                        in.value(value);
                    });
                    predicateList.add(in);
                }
                if (!CollectionUtils.isEmpty(labelList)) {
                    CriteriaBuilder.In<String> in = criteriaBuilder.in(root.get("label"));
                    labelList.forEach(label -> {
                        in.value(label);
                    });
                    predicateList.add(in);
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        List<DictionaryContentNodeDo> selectedDictionaryContentNodeDoList =
                this.dictionaryContentNodeRepository.findAll(specification, Sort.by(Sort.Order.asc(BaseDo.ID_FIELD_NAME)));

        if (CollectionUtils.isEmpty(selectedDictionaryContentNodeDoList)) {
            return treeListingFirstLevelOfDictionaryContentHierarchy(dictionaryCategoryUid, operatingUserProfile);
        }

        List<Long> selectedDictionaryContentUidList = new LinkedList<>();
        selectedDictionaryContentNodeDoList.forEach(dictionaryContentNodeDo -> {
            selectedDictionaryContentUidList.add(dictionaryContentNodeDo.getUid());
        });

        //
        // Step 2, core-processing
        //
        DictionaryCategoryDo dictionaryCategoryDo = this.dictionaryCategoryRepository.findByUid(dictionaryCategoryUid);
        if (dictionaryCategoryDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DictionaryCategoryDo.RESOURCE_SYMBOL,
                    dictionaryCategoryUid));
        }

        List<TreeNode> result = new LinkedList<>();

        List<DictionaryContentNodeDo> objectiveDictionaryContentNodeDoList = new LinkedList<>();

        List<DictionaryContentNodeDo> rootDictionaryContentNodeDoList =
                this.dictionaryContentNodeRepository.findByDictionaryCategoryUidAndVersionAndNullParentUid(
                        dictionaryCategoryDo.getUid(), dictionaryCategoryDo.getVersion());

        if (CollectionUtils.isEmpty(rootDictionaryContentNodeDoList)) {
            return null;
        }

        objectiveDictionaryContentNodeDoList.addAll(rootDictionaryContentNodeDoList);

        objectiveDictionaryContentNodeDoList.addAll(selectedDictionaryContentNodeDoList);

        // 包含了跟搜索条件直接相关的 dictionary content node(s)，为每一个都构建 tree node
        //
        // L1 key --- dictionary content node uid, L1 value --- dictionary content node (tree node)
        Map<Long, TreeNode> directlyRelatedContentNodeUidAndTreeNodeMap = new HashMap<>();
        // L1 key --- dictionary content node uid, L1 value --- dictionary content node (uid)'s parent node (uid)
        Map<Long, Long> directlyRelatedContentNodeUidAndParentUidMap = new HashMap<>();

        buildTreeNodeForEachDictionaryContentNode(
                directlyRelatedContentNodeUidAndTreeNodeMap,
                directlyRelatedContentNodeUidAndParentUidMap,
                objectiveDictionaryContentNodeDoList);

        //
        // Step 2.3, 为了构造 tree node(s), 只有跟搜索条件直接相关的 dictionary content node(s) 作为 tree node(s) 还不够，
        // 还需要递归为每个 dictionary content node (tree node) 找到 parent
        //

        // 包含了跟搜索条件直接相关的 dictionary content node (tree node)，以及它们各自的 parent node (tree node)
        //
        // L1 key --- dictionary content node uid
        Map<Long, TreeNode> allContentNodeUidAndTreeNodeMap = new HashMap<>();
        // L1 key --- dictionary content node uid
        Map<Long, Long> allContentNodeUidAndParentUidMap = new HashMap<>();

        recursivelyBuildTreeNodeForEachParentDictionaryContentNode(
                directlyRelatedContentNodeUidAndTreeNodeMap,
                directlyRelatedContentNodeUidAndParentUidMap,
                allContentNodeUidAndTreeNodeMap,
                allContentNodeUidAndParentUidMap);

        //
        // Step 2.4, 梳理 tree node 之间的 parent/child 关系
        //
        allContentNodeUidAndTreeNodeMap.forEach(
                (dictionaryContentUid,
                 dictionaryContentTreeNode) -> {
                    // tagging selected
                    if (selectedDictionaryContentUidList.contains(dictionaryContentUid)) {
                        if (dictionaryContentTreeNode.getTags() == null) {
                            dictionaryContentTreeNode.setTags(new HashMap<>());
                        }
                        dictionaryContentTreeNode.getTags().put(TreeNode.GENERAL_TAG_SELECTED, true);
                    }

                    Long parentDictionaryContentNodeUid =
                            allContentNodeUidAndParentUidMap.get(dictionaryContentUid);
                    if (parentDictionaryContentNodeUid == null) {
                        // 第一级 dictionary content node
                        result.add(dictionaryContentTreeNode);
                    } else {
                        // 非第一级 dictionary content node
                        TreeNode parentTreeNode =
                                allContentNodeUidAndTreeNodeMap.get(parentDictionaryContentNodeUid);
                        if (parentTreeNode == null) {
                            LOGGER.warn("found parent uid that does not exist, dictionary content node's parent uid:{}",
                                    parentDictionaryContentNodeUid);
                        } else {
                            if (parentTreeNode.getChildren() == null) {
                                parentTreeNode.setChildren(new LinkedList<>());
                            }
                            parentTreeNode.getChildren().add(dictionaryContentTreeNode);
                        }
                    }
                });

        //
        // Step 3, post-processing
        //
        // 排序
        recursivelySortingTreeNodes(result);

        return result;
    }

    /**
     * 为每个 dictionary content node 构建 tree node
     *
     * @param dictionaryContentNodeUidAndTreeNodeMap,  tree node 结构收集，L1 key --- dictionary content node uid, L1
     *                                                 value --- tree node for dictionary
     *                                                 content node
     * @param dictionaryContentNodeUidAndParentUidMap, dictionary content node 的 parent uid
     *                                                 收集，L1
     *                                                 key --- dictionary content node uid, L1
     *                                                 value --- parent's uid of dictionary
     *                                                 content node
     * @param dictionaryContentNodeDoList,             collection of dictionary content node
     */
    private void buildTreeNodeForEachDictionaryContentNode(
            Map<Long, TreeNode> dictionaryContentNodeUidAndTreeNodeMap,
            Map<Long, Long> dictionaryContentNodeUidAndParentUidMap,
            List<DictionaryContentNodeDo> dictionaryContentNodeDoList) {
        //
        // Step 1, pre-processing
        //
        if (CollectionUtils.isEmpty(dictionaryContentNodeDoList)) {
            return;
        }

        //
        // Step 2, core-processing
        //
        dictionaryContentNodeDoList.forEach(dictionaryContentNodeDo -> {
            TreeNode treeNode = new TreeNode();
            treeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
            treeNode.setUid(dictionaryContentNodeDo.getUid());
            treeNode.setName(dictionaryContentNodeDo.getLabel());
            treeNode.setDescription(dictionaryContentNodeDo.getValue());
            treeNode.setType(DictionaryContentNodeDo.TYPE);
            treeNode.setTags(new HashMap<>());
            treeNode.getTags().put("label", dictionaryContentNodeDo.getLabel());
            treeNode.getTags().put("value", dictionaryContentNodeDo.getValue());
            treeNode.getTags().put("sequence", dictionaryContentNodeDo.getSequence());

            dictionaryContentNodeUidAndTreeNodeMap
                    .put(dictionaryContentNodeDo.getUid(), treeNode);

            if (dictionaryContentNodeDo.getParentUid() != null) {
                dictionaryContentNodeUidAndParentUidMap
                        .put(dictionaryContentNodeDo.getUid(), dictionaryContentNodeDo.getParentUid());
            }
        });

        //
        // Step 3, post-processing
        //
    }

    /**
     * 递归找出每个 dictionary content node 的 parent，并为其构建 tree node
     *
     * @param todoDictionaryContentNodeUidAndTreeNodeMap,  待处理 tree node 集合，L1 key --- dictionary content node uid, L1
     *                                                     value --- tree node for dictionary
     *                                                     content node
     * @param todoDictionaryContentNodeUidAndParentUidMap, 待处理 dictionary content node 的
     *                                                     parent uid 集合，L1
     *                                                     key --- dictionary content node
     *                                                     uid, L1
     *                                                     value --- parent's uid of dictionary
     *                                                     content node
     * @param allDictionaryContentNodeUidAndTreeNodeMap,   完整 tree node 结果收集，L1 --- dictionary content node uid, L1
     *                                                     value --- tree node for dictionary
     *                                                     content node
     * @param allDictionaryContentNodeUidAndParentUidMap,  完整 dictionary content node 的 parent uid
     *                                                     收集，L1
     *                                                     key --- dictionary content node
     *                                                     uid, L1
     *                                                     value --- parent's uid of dictionary
     *                                                     content node
     */
    private void recursivelyBuildTreeNodeForEachParentDictionaryContentNode(
            Map<Long, TreeNode> todoDictionaryContentNodeUidAndTreeNodeMap,
            Map<Long, Long> todoDictionaryContentNodeUidAndParentUidMap,
            Map<Long, TreeNode> allDictionaryContentNodeUidAndTreeNodeMap,
            Map<Long, Long> allDictionaryContentNodeUidAndParentUidMap) {
        //
        // Step 1, pre-processing
        //
        allDictionaryContentNodeUidAndTreeNodeMap
                .putAll(todoDictionaryContentNodeUidAndTreeNodeMap);
        allDictionaryContentNodeUidAndParentUidMap
                .putAll(todoDictionaryContentNodeUidAndParentUidMap);

        //
        // Step 2, core-processing
        //

        // 找出待构建 tree node 的 dictionary content node(s) 的 uid(s)
        List<Long> toFetchDictionaryContentUidList = new LinkedList<>();
        todoDictionaryContentNodeUidAndParentUidMap.forEach(
                (dictionaryContentUid,
                 dictionaryContentUidAndParentUid) -> {
                    if (!allDictionaryContentNodeUidAndTreeNodeMap.containsKey(dictionaryContentUidAndParentUid)) {
                        toFetchDictionaryContentUidList.add(dictionaryContentUidAndParentUid);
                    }
                });
        if (CollectionUtils.isEmpty(toFetchDictionaryContentUidList)) {
            return;
        }

        // 分页从 DB 取出这些 dictionary content node(s)，并为每个 dictionary content node 构建 tree node
        Map<Long, TreeNode> newTodoDictionaryContentNodeUidAndTreeNodeMap =
                new HashMap<>();
        Map<Long, Long> newTodoDictionaryContentNodeUidAndParentUidMap = new HashMap<>();
        int slices =
                toFetchDictionaryContentUidList.size() / 1000 + (toFetchDictionaryContentUidList.size() % 1000 > 0 ?
                        1 : 0);
        for (int slice = 0; slice < slices; slice++) {
            int end;
            if (slice < slices - 1) {
                end = (slice + 1) * 1000;
            } else {
                end = toFetchDictionaryContentUidList.size();
            }
            List<Long> subList = toFetchDictionaryContentUidList.subList(slice * 1000, end);
            List<DictionaryContentNodeDo> dictionaryContentNodeDoList =
                    this.dictionaryContentNodeRepository.findByUidIn(subList);

            buildTreeNodeForEachDictionaryContentNode(
                    newTodoDictionaryContentNodeUidAndTreeNodeMap,
                    newTodoDictionaryContentNodeUidAndParentUidMap,
                    dictionaryContentNodeDoList);
        }

        //
        // Step 3, post-processing
        //

        // 递归继续处理本次找出的 dictionary content node(s) 的 parent node(s)
        recursivelyBuildTreeNodeForEachParentDictionaryContentNode(
                newTodoDictionaryContentNodeUidAndTreeNodeMap,
                newTodoDictionaryContentNodeUidAndParentUidMap,
                allDictionaryContentNodeUidAndTreeNodeMap,
                allDictionaryContentNodeUidAndParentUidMap);
    }

    @Override
    public DictionaryBuildDto createOrReplaceDictionaryBuild(
            Long dictionaryCategoryUid,
            CreateOrReplaceDictionaryBuildDto createOrReplaceDictionaryBuildDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DictionaryBuildDo dictionaryBuildDo =
                this.dictionaryBuildRepository.findByDictionaryCategoryUid(dictionaryCategoryUid);

        //
        // Step 2, core-processing
        //
        if (dictionaryBuildDo == null) {
            // create
            return createDictionaryBuild(dictionaryCategoryUid, createOrReplaceDictionaryBuildDto,
                    operatingUserProfile);
        } else {
            // replace
            return replaceDictionaryBuild(dictionaryBuildDo, createOrReplaceDictionaryBuildDto,
                    operatingUserProfile);
        }
    }

    private DictionaryBuildDto createDictionaryBuild(
            Long dictionaryCategoryUid,
            CreateOrReplaceDictionaryBuildDto createOrReplaceDictionaryBuildDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DictionaryCategoryDo dictionaryCategoryDo = this.dictionaryCategoryRepository.findByUid(dictionaryCategoryUid);
        if (dictionaryCategoryDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DictionaryCategoryDo.RESOURCE_SYMBOL,
                    dictionaryCategoryUid));
        }
        boolean existsDuplicate =
                this.dictionaryBuildRepository.existsByDictionaryCategoryUid(dictionaryCategoryUid);
        if (existsDuplicate) {
            throw new AbcResourceDuplicateException(String.format("%s::dictionary_category_uid=%d",
                    DictionaryBuildDo.RESOURCE_SYMBOL,
                    dictionaryCategoryUid));
        }

        testDictionaryBuild(
                createOrReplaceDictionaryBuildDto.getType(),
                createOrReplaceDictionaryBuildDto.getLogic(),
                operatingUserProfile);

        //
        // Step 2, core-processing
        //
        DictionaryBuildDo dictionaryBuildDo = new DictionaryBuildDo();
        dictionaryBuildDo.setEnabled(createOrReplaceDictionaryBuildDto.getEnabled());
        dictionaryBuildDo.setCronExpression(createOrReplaceDictionaryBuildDto.getCronExpression());
        dictionaryBuildDo.setType(createOrReplaceDictionaryBuildDto.getType());
        dictionaryBuildDo.setLogic(createOrReplaceDictionaryBuildDto.getLogic());
        dictionaryBuildDo.setDictionaryCategoryUid(dictionaryCategoryUid);

        if (Boolean.TRUE.equals(dictionaryBuildDo.getEnabled())) {
            CreateDistributedJobDto createDistributedJobDto = new CreateDistributedJobDto();
            createDistributedJobDto.setName(dictionaryCategoryDo.getName());
            createDistributedJobDto.setDescription(dictionaryCategoryDo.getDescription());
            createDistributedJobDto.setCronExpression(dictionaryBuildDo.getCronExpression());
            createDistributedJobDto.setEnabled(Boolean.TRUE);
            createDistributedJobDto.setFailedRetires(0);
            createDistributedJobDto.setHandlerName(DictionaryHandler.JOB_HANDLER_DICTIONARY_BUILD);

            JSONObject parameters = new JSONObject();
            parameters.put("dictionary_category_uid", dictionaryBuildDo.getDictionaryCategoryUid());
            createDistributedJobDto.setParameters(parameters);
            createDistributedJobDto.setRoutingAlgorithm(JobExecutorRoutingAlgorithmEnum.ROUND_ROBIN);
            createDistributedJobDto.setTimeoutDurationInSecs(3600L);
            DistributedJobDto distributedJobDto = this.distributedJobService.createJob(createDistributedJobDto, operatingUserProfile);

            dictionaryBuildDo.setJobUid(distributedJobDto.getUid());
        }

        BaseDo.create(dictionaryBuildDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dictionaryBuildRepository.save(dictionaryBuildDo);

        //
        // Step 3, post-processing
        //
        DictionaryBuildDto dictionaryBuildDto = new DictionaryBuildDto();
        BeanUtils.copyProperties(dictionaryBuildDo, dictionaryBuildDto);
        return dictionaryBuildDto;
    }

    public DictionaryBuildDto replaceDictionaryBuild(
            DictionaryBuildDo dictionaryBuildDo,
            CreateOrReplaceDictionaryBuildDto createOrReplaceDictionaryBuildDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DictionaryCategoryDo dictionaryCategoryDo =
                this.dictionaryCategoryRepository.findByUid(dictionaryBuildDo.getDictionaryCategoryUid());
        if (dictionaryCategoryDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DictionaryCategoryDo.RESOURCE_SYMBOL,
                    dictionaryBuildDo.getDictionaryCategoryUid()));
        }

        //
        // Step 2, core-processing
        //
        boolean requiredToUpdate = false;
        if (!ObjectUtils.isEmpty(createOrReplaceDictionaryBuildDto.getCronExpression())
                && !createOrReplaceDictionaryBuildDto.getCronExpression().equals(dictionaryBuildDo.getCronExpression())) {
            dictionaryBuildDo.setCronExpression(createOrReplaceDictionaryBuildDto.getCronExpression());
            requiredToUpdate = true;
        }
        if (createOrReplaceDictionaryBuildDto.getType() != null
                && !createOrReplaceDictionaryBuildDto.getType().equals(dictionaryBuildDo.getType())) {
            dictionaryBuildDo.setType(createOrReplaceDictionaryBuildDto.getType());
            requiredToUpdate = true;
        }
        if (createOrReplaceDictionaryBuildDto.getLogic() != null
                && !createOrReplaceDictionaryBuildDto.getLogic().equals(dictionaryBuildDo.getLogic())) {
            dictionaryBuildDo.setLogic(createOrReplaceDictionaryBuildDto.getLogic());
            requiredToUpdate = true;
        }
        if (createOrReplaceDictionaryBuildDto.getEnabled() != null
                && !createOrReplaceDictionaryBuildDto.getEnabled().equals(dictionaryBuildDo.getEnabled())) {
            dictionaryBuildDo.setEnabled(createOrReplaceDictionaryBuildDto.getEnabled());
            requiredToUpdate = true;
        }

        this.dictionaryHandler.validateDictionaryBuild(
                dictionaryBuildDo.getType(),
                dictionaryBuildDo.getLogic());

        if (dictionaryBuildDo.getJobUid() == null) {
            CreateDistributedJobDto createDistributedJobDto = new CreateDistributedJobDto();
            createDistributedJobDto.setName(dictionaryCategoryDo.getName());
            createDistributedJobDto.setDescription(dictionaryCategoryDo.getDescription());
            createDistributedJobDto.setCronExpression(dictionaryBuildDo.getCronExpression());
            createDistributedJobDto.setEnabled(dictionaryBuildDo.getEnabled());
            createDistributedJobDto.setFailedRetires(0);
            createDistributedJobDto.setHandlerName(DictionaryHandler.JOB_HANDLER_DICTIONARY_BUILD);

            JSONObject parameters = new JSONObject();
            parameters.put("dictionary_category_uid", dictionaryBuildDo.getDictionaryCategoryUid());
            createDistributedJobDto.setParameters(parameters);
            createDistributedJobDto.setRoutingAlgorithm(JobExecutorRoutingAlgorithmEnum.ROUND_ROBIN);
            createDistributedJobDto.setTimeoutDurationInSecs(3600L);
            DistributedJobDto distributedJobDto = this.distributedJobService.createJob(createDistributedJobDto, operatingUserProfile);

            dictionaryBuildDo.setJobUid(distributedJobDto.getUid());
        } else {
            UpdateDistributedJobDto updateDistributedJobDto = new UpdateDistributedJobDto();
            updateDistributedJobDto.setEnabled(dictionaryBuildDo.getEnabled());
            updateDistributedJobDto.setName(dictionaryCategoryDo.getName());
            updateDistributedJobDto.setDescription(dictionaryCategoryDo.getDescription());
            updateDistributedJobDto.setCronExpression(dictionaryBuildDo.getCronExpression());
            this.distributedJobService.updateJob(dictionaryBuildDo.getJobUid(), updateDistributedJobDto, operatingUserProfile);
        }

        if (requiredToUpdate) {
            BaseDo.update(dictionaryBuildDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.dictionaryBuildRepository.save(dictionaryBuildDo);
        }

        //
        // Step 3, post-processing
        //
        DictionaryBuildDto dictionaryBuildDto = new DictionaryBuildDto();
        BeanUtils.copyProperties(dictionaryBuildDo, dictionaryBuildDto);
        return dictionaryBuildDto;
    }

    public void deleteDictionaryBuild(
            DictionaryBuildDo dictionaryBuildDo,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //


        //
        // Step 2, core-processing
        //

        if (dictionaryBuildDo.getJobUid() != null) {
            this.distributedJobService.deleteJob(dictionaryBuildDo.getJobUid(),
                    operatingUserProfile);
        }

        dictionaryBuildDo.setDeleted(Boolean.TRUE);
        BaseDo.update(dictionaryBuildDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dictionaryBuildRepository.save(dictionaryBuildDo);

        //
        // Step 3, post-processing
        //

    }

    @Override
    public DictionaryBuildDto findDictionaryBuildByDataDictionaryUid(
            Long dictionaryCategoryUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        //
        // Step 2, core-processing
        //
        DictionaryBuildDo dictionaryBuildDo = this.dictionaryBuildRepository.findByDictionaryCategoryUid(dictionaryCategoryUid);
        if (dictionaryBuildDo == null) {
            return null;
        }

        //
        // Step 3, post-processing
        //
        DictionaryBuildDto dictionaryBuildDto = new DictionaryBuildDto();
        BeanUtils.copyProperties(dictionaryBuildDo, dictionaryBuildDto);
        return dictionaryBuildDto;
    }

    @Override
    public List<TreeNode> testDictionaryBuild(
            TestDictionaryBuildDto testDictionaryBuildDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return testDictionaryBuild(
                testDictionaryBuildDto.getType(),
                testDictionaryBuildDto.getLogic(),
                operatingUserProfile);
    }

    private List<TreeNode> testDictionaryBuild(
            DictionaryBuildTypeEnum type,
            JSONObject logic,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return this.dictionaryHandler.testDictionaryBuild(
                type,
                logic);
    }

    @Override
    public void executeOnceDictionaryBuild(
            Long dictionaryCategoryUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        //
        // Step 2, core-processing
        //
        JSONObject params = new JSONObject();
        params.put("dictionary_category_uid", dictionaryCategoryUid);
        this.dictionaryHandler.executeDictionaryBuild(params);

        //
        // Step 3, post-processing
        //
    }

    @Override
    public Page<DictionaryBuildInstanceDto> pagingQueryDictionaryBuildInstances(
            Long dictionaryCategoryUid,
            Long uid,
            List<JobStatusEnum> statuses,
            List<String> createdTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        //
        // Step 2, core-processing
        //
        Specification<DictionaryBuildInstanceDo> specification = new Specification<DictionaryBuildInstanceDo>() {
            @Override
            public Predicate toPredicate(Root<DictionaryBuildInstanceDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (dictionaryCategoryUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("dictionaryCategoryUid"), dictionaryCategoryUid));
                }
                if (uid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("uid"), uid));
                }
                if (!CollectionUtils.isEmpty(statuses)) {
                    CriteriaBuilder.In<JobStatusEnum> in = criteriaBuilder.in(root.get("status"));
                    statuses.forEach(status -> {
                        in.value(status);
                    });
                    predicateList.add(in);
                }
                if (!CollectionUtils.isEmpty(createdTimestampAsStringList)) {
                    if (createdTimestampAsStringList.size() == 2) {
                        LocalDateTime dateTime0 = LocalDateTime.parse(createdTimestampAsStringList.get(0), dateTimeFormatter);
                        LocalDateTime dateTime1 = LocalDateTime.parse(createdTimestampAsStringList.get(1), dateTimeFormatter);
                        if (dateTime0.isAfter(dateTime1)) {
                            predicateList.add(criteriaBuilder.between(root.get(BaseDo.CREATED_TIMESTAMP_FIELD_NAME),
                                    dateTime1, dateTime0));
                        } else {
                            predicateList.add(criteriaBuilder.between(root.get(BaseDo.CREATED_TIMESTAMP_FIELD_NAME),
                                    dateTime0, dateTime1));
                        }
                    } else if (createdTimestampAsStringList.size() == 1) {
                        LocalDateTime dateTime0 = LocalDateTime.parse(createdTimestampAsStringList.get(0), dateTimeFormatter);
                        predicateList.add(criteriaBuilder.equal(root.get(BaseDo.CREATED_TIMESTAMP_FIELD_NAME),
                                dateTime0));
                    } else {
                        CriteriaBuilder.In<LocalDateTime> in = criteriaBuilder.in(root.get(BaseDo.CREATED_TIMESTAMP_FIELD_NAME));
                        createdTimestampAsStringList.forEach(createdTimestampAsString -> {
                            LocalDateTime dateTime0 = LocalDateTime.parse(createdTimestampAsString, dateTimeFormatter);
                            in.value(dateTime0);
                        });
                        predicateList.add(in);
                    }
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        if (pageable.getSort() == null || pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Order.desc(BaseDo.ID_FIELD_NAME)));
        }
        Page<DictionaryBuildInstanceDo> itemDoPage = this.dictionaryBuildInstanceRepository.findAll(specification,
                pageable);

        //
        // Step 3, post-processing
        //
        if (itemDoPage.isEmpty()) {
            Page<DictionaryBuildInstanceDto> itemDtoPage = new PageImpl<DictionaryBuildInstanceDto>(
                    new ArrayList<>(), pageable, itemDoPage.getTotalElements());
            return itemDtoPage;
        } else {
            List<DictionaryBuildInstanceDto> content = new ArrayList<>(itemDoPage.getContent().size());
            itemDoPage.forEach(itemDo -> {
                DictionaryBuildInstanceDto itemDto = new DictionaryBuildInstanceDto();
                BeanUtils.copyProperties(itemDo, itemDto);
                content.add(itemDto);
            });
            Page<DictionaryBuildInstanceDto> itemDtoPage = new PageImpl<DictionaryBuildInstanceDto>(
                    content, pageable, itemDoPage.getTotalElements());
            return itemDtoPage;
        }
    }

    @ResourceReferenceHandler(name = "data dictionary")
    public List<String> checkResourceReference(
            ResourceReferenceManager.ResourceCategoryEnum resourceCategory,
            Long resourceUid,
            String resourceName) throws Exception {
        switch (resourceCategory) {
            case DATA_SOURCE: {
                Long dataSourceUid = resourceUid;

                List<Long> foundReferencedDictionaryCategoryUidList = new LinkedList<>();
                this.dictionaryBuildRepository.findAll().forEach(dictionaryBuildDo -> {
                    JSONObject logic = dictionaryBuildDo.getLogic();

                    switch (dictionaryBuildDo.getType()) {
                        case SQL: {
                            DictionaryBuildSqlLogic dictionaryBuildSqlLogic = null;
                            try {
                                dictionaryBuildSqlLogic = JSONObject.toJavaObject(logic, DictionaryBuildSqlLogic.class);
                                if (dataSourceUid.equals(dictionaryBuildSqlLogic.getDataSourceUid())) {
                                    if (!foundReferencedDictionaryCategoryUidList.contains(dictionaryBuildDo.getDictionaryCategoryUid())) {
                                        foundReferencedDictionaryCategoryUidList.add(dictionaryBuildDo.getDictionaryCategoryUid());
                                    }
                                }
                            } catch (Exception e) {
                                LOGGER.error("failed to parse dictionary build sql logic {}", logic, e);
                                throw new AbcResourceConflictException("illegal logic");
                            }

                        }
                            break;
                        default:
                            break;
                    }
                });

                if (!CollectionUtils.isEmpty(foundReferencedDictionaryCategoryUidList)) {
                   List<DictionaryCategoryDo> dictionaryCategoryDoList =
                           this.dictionaryCategoryRepository.findByUidIn(foundReferencedDictionaryCategoryUidList);
                   if (!CollectionUtils.isEmpty(dictionaryCategoryDoList)) {
                       List<String> result = new LinkedList<>();
                       dictionaryCategoryDoList.forEach(dictionaryCategoryDo -> {
                           result.add(String.format(
                                   "[%s] %s (%d)",
                                   DictionaryCategoryDo.RESOURCE_SYMBOL,
                                   dictionaryCategoryDo.getName(),
                                   dictionaryCategoryDo.getUid()));
                       });

                       return result;
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
