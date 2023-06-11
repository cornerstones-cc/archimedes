package cc.cornerstones.biz.administration.usermanagement.service.impl;

import cc.cornerstones.almond.exceptions.AbcResourceDuplicateException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.*;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.administration.usermanagement.dto.*;
import cc.cornerstones.biz.administration.usermanagement.entity.ApiDo;
import cc.cornerstones.biz.administration.usermanagement.entity.FunctionDo;
import cc.cornerstones.biz.administration.usermanagement.entity.NavigationMenuDo;
import cc.cornerstones.biz.administration.usermanagement.persistence.ApiRepository;
import cc.cornerstones.biz.administration.usermanagement.persistence.FunctionRepository;
import cc.cornerstones.biz.administration.usermanagement.persistence.NavigationMenuRepository;
import cc.cornerstones.biz.administration.usermanagement.service.inf.PermissionsService;
import cc.cornerstones.biz.administration.usermanagement.share.types.FunctionEntityNode;
import cc.cornerstones.biz.administration.usermanagement.share.types.NavigationMenuEntityNode;
import cc.cornerstones.biz.share.event.EventBusManager;
import cc.cornerstones.almond.types.ReplaceTreeNodeRelationship;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PermissionsServiceImpl implements PermissionsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionsServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private ApiRepository apiRepository;

    @Autowired
    private FunctionRepository functionRepository;

    @Autowired
    private NavigationMenuRepository navigationMenuRepository;

    @Override
    public List<ApiDto> listingQueryApis(
            String tag,
            String method,
            String uri,
            String summary,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Specification<ApiDo> specification = new Specification<ApiDo>() {
            @Override
            public Predicate toPredicate(Root<ApiDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (!ObjectUtils.isEmpty(tag)) {
                    predicateList.add(criteriaBuilder.like(root.get("tag"), "%" + tag + "%"));
                }
                if (!ObjectUtils.isEmpty(method)) {
                    predicateList.add(criteriaBuilder.like(root.get("method"), "%" + method + "%"));
                }
                if (!ObjectUtils.isEmpty(uri)) {
                    predicateList.add(criteriaBuilder.like(root.get("uri"), "%" + uri + "%"));
                }
                if (!ObjectUtils.isEmpty(summary)) {
                    predicateList.add(criteriaBuilder.like(root.get("summary"), "%" + summary + "%"));
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        List<ApiDo> itemDoList = this.apiRepository.findAll(specification, sort);
        if (CollectionUtils.isEmpty(itemDoList)) {
            return null;
        }
        List<ApiDto> itemDtoList = new LinkedList<>();
        itemDoList.forEach(itemDo -> {
            ApiDto itemDto = new ApiDto();
            BeanUtils.copyProperties(itemDo, itemDto);
            itemDtoList.add(itemDto);
        });
        return itemDtoList;
    }

    @Override
    public List<TreeNode> treeListingAllNodesOfApiHierarchy(
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Map<String, TreeNode> tagTreeNodeMap = new HashMap<>();
        this.apiRepository.findAll(Sort.by(Sort.Order.asc(BaseDo.ID_FIELD_NAME))).forEach(apiDo -> {
            TreeNode treeNode = new TreeNode();
            treeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
            treeNode.setUid(apiDo.getUid());
            treeNode.setName(apiDo.getSummary());
            treeNode.setDescription(apiDo.getMethod() + " " + apiDo.getUri());
            treeNode.setType(TreeNode.GENERAL_TYPE_ENTITY);
            treeNode.setTags(new HashMap<>());
            treeNode.getTags().put("uri", apiDo.getUri());
            treeNode.getTags().put("method", apiDo.getMethod());

            if (!ObjectUtils.isEmpty(apiDo.getTag())) {
                String tag = apiDo.getTag().trim().toLowerCase();
                TreeNode tagTreeNode = tagTreeNodeMap.get(tag);

                if (tagTreeNode == null) {
                    tagTreeNode = new TreeNode();
                    tagTreeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
                    tagTreeNode.setName(apiDo.getTag().trim());
                    tagTreeNode.setType(TreeNode.GENERAL_TYPE_DIRECTORY);
                    tagTreeNode.setChildren(new LinkedList<>());

                    tagTreeNodeMap.put(tag, tagTreeNode);
                }

                tagTreeNode.getChildren().add(treeNode);
            }
        });
        return new LinkedList<>(tagTreeNodeMap.values());
    }

    @Override
    public List<TreeNode> treeListingAllNodesOfFunctionHierarchy(
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Map<Long, TreeNode> itemUidAndTreeNodeMap = new HashMap<>();
        List<TreeNode> rootTreeNodeList = new LinkedList<>();
        this.functionRepository.findAll().forEach(itemDo -> {
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

        return rootTreeNodeList;
    }

    @Override
    public TreeNode createDirectoryNodeForFunctionHierarchy(
            Long parentUid,
            CreateDirectoryTreeNode createDirectoryTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        if (parentUid == null) {
            Integer existsDuplicate = this.functionRepository.existsByNameWithoutParent(
                    createDirectoryTreeNode.getName());
            if (existsDuplicate > 0) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s", FunctionDo.RESOURCE_SYMBOL,
                        createDirectoryTreeNode.getName()));
            }
        } else {
            Integer existsDuplicate = this.functionRepository.existsByNameWithinParent(
                    createDirectoryTreeNode.getName(), parentUid);
            if (existsDuplicate > 0) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s", FunctionDo.RESOURCE_SYMBOL,
                        createDirectoryTreeNode.getName()));
            }

            boolean exists = this.functionRepository.existsByUid(parentUid);
            if (!exists) {
                throw new AbcResourceNotFoundException(String.format("%s::uid=%s", FunctionDo.RESOURCE_SYMBOL,
                        parentUid));
            }
        }

        //
        // Step 2, core-processing
        //
        FunctionDo functionDo = new FunctionDo();
        functionDo.setUid(this.idHelper.getNextDistributedId(FunctionDo.RESOURCE_NAME));
        functionDo.setName(createDirectoryTreeNode.getName());
        functionDo.setObjectName(
                createDirectoryTreeNode.getName()
                        .replaceAll("_", "__")
                        .replaceAll("\\s", "_")
                        .toLowerCase());
        functionDo.setDescription(createDirectoryTreeNode.getDescription());
        functionDo.setDirectory(Boolean.TRUE);
        functionDo.setParentUid(parentUid);

        // sequence
        Float maxSequence = null;
        if (parentUid == null) {
            maxSequence = this.functionRepository.findMaxSequenceWithoutParent();
        } else {
            maxSequence = this.functionRepository.findMaxSequenceWithinParent(parentUid);
        }
        if (maxSequence == null) {
            functionDo.setSequence(0.001f);
        } else {
            functionDo.setSequence(maxSequence + 0.001f);
        }

        BaseDo.create(functionDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.functionRepository.save(functionDo);

        //
        // Step 3, post-processing
        //
        TreeNode treeNode = new TreeNode();
        treeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
        treeNode.setUid(functionDo.getUid());
        treeNode.setName(functionDo.getName());
        treeNode.setDescription(functionDo.getDescription());
        treeNode.setType(TreeNode.GENERAL_TYPE_DIRECTORY);

        return treeNode;
    }

    @Override
    public void updateDirectoryNodeOfFunctionHierarchy(
            Long uid,
            UpdateDirectoryTreeNode updateDirectoryTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        FunctionDo functionDo = this.functionRepository.findByUid(uid);
        if (functionDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", FunctionDo.RESOURCE_SYMBOL, uid));
        }
        if (!ObjectUtils.isEmpty(updateDirectoryTreeNode.getName())
                && !updateDirectoryTreeNode.getName().equalsIgnoreCase(functionDo.getName())) {
            // name 要改
            if (functionDo.getParentUid() == null) {
                // 没有 parent 的情况
                Integer existsDuplicate = this.functionRepository.existsByNameWithoutParent(
                        updateDirectoryTreeNode.getName());
                if (existsDuplicate > 0) {
                    throw new AbcResourceDuplicateException(String.format("%s::name=%s", FunctionDo.RESOURCE_SYMBOL,
                            updateDirectoryTreeNode.getName()));
                }
            } else {
                // 有 parent 的情况
                Integer existsDuplicate = this.functionRepository.existsByNameWithinParent(
                        updateDirectoryTreeNode.getName(), functionDo.getParentUid());
                if (existsDuplicate > 0) {
                    throw new AbcResourceDuplicateException(String.format("%s::name=%s", FunctionDo.RESOURCE_SYMBOL,
                            updateDirectoryTreeNode.getName()));
                }
            }
        }

        //
        // Step 2, core-processing
        //
        boolean requiredToUpdate = false;

        if (!ObjectUtils.isEmpty(updateDirectoryTreeNode.getName())
                && !updateDirectoryTreeNode.getName().equalsIgnoreCase(functionDo.getName())) {
            functionDo.setName(updateDirectoryTreeNode.getName());
            functionDo.setObjectName(
                    updateDirectoryTreeNode.getName()
                            .replaceAll("_", "__")
                            .replaceAll("\\s", "_")
                            .toLowerCase());
            requiredToUpdate = true;
        }
        if (updateDirectoryTreeNode.getDescription() != null
                && !updateDirectoryTreeNode.getDescription().equalsIgnoreCase(functionDo.getDescription())) {
            functionDo.setDescription(updateDirectoryTreeNode.getDescription());
            requiredToUpdate = true;
        }

        if (requiredToUpdate) {
            BaseDo.update(functionDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.functionRepository.save(functionDo);
        }

        //
        // Step 3, post-processing
        //
    }

    @Override
    public TreeNode createEntityNodeForFunctionHierarchy(
            Long parentUid,
            CreateEntityTreeNode createEntityTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        if (parentUid == null) {
            Integer existsDuplicate = this.functionRepository.existsByNameWithoutParent(
                    createEntityTreeNode.getName());
            if (existsDuplicate > 0) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s", FunctionDo.RESOURCE_SYMBOL,
                        createEntityTreeNode.getName()));
            }
        } else {
            Integer existsDuplicate = this.functionRepository.existsByNameWithinParent(
                    createEntityTreeNode.getName(), parentUid);
            if (existsDuplicate > 0) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s", FunctionDo.RESOURCE_SYMBOL,
                        createEntityTreeNode.getName()));
            }

            boolean exists = this.functionRepository.existsByUid(parentUid);
            if (!exists) {
                throw new AbcResourceNotFoundException(String.format("%s::uid=%s", FunctionDo.RESOURCE_SYMBOL,
                        parentUid));
            }
        }

        //
        // Step 2, core-processing
        //
        FunctionDo functionDo = new FunctionDo();
        functionDo.setUid(this.idHelper.getNextDistributedId(FunctionDo.RESOURCE_NAME));
        functionDo.setDirectory(Boolean.FALSE);
        functionDo.setParentUid(parentUid);

        // entity node translation
        JSONObject payload = createEntityTreeNode.getPayload();
        FunctionEntityNode functionEntityNode = JSONObject.toJavaObject(payload, FunctionEntityNode.class);
        functionDo.setUri(functionEntityNode.getUri());
        functionDo.setMethod(functionEntityNode.getMethod());
        functionDo.setSummary(functionEntityNode.getSummary());

        // 特殊设置 name
        functionDo.setName(functionEntityNode.getMethod() + " " + functionEntityNode.getUri());
        functionDo.setObjectName(
                functionDo.getName()
                        .replaceAll("_", "__")
                        .replaceAll("\\s", "_")
                        .toLowerCase());
        // 特殊设置 description
        functionDo.setDescription(functionEntityNode.getSummary());

        // sequence
        Float maxSequence = null;
        if (parentUid == null) {
            maxSequence = this.functionRepository.findMaxSequenceWithoutParent();
        } else {
            maxSequence = this.functionRepository.findMaxSequenceWithinParent(parentUid);
        }
        if (maxSequence == null) {
            functionDo.setSequence(0.001f);
        } else {
            functionDo.setSequence(maxSequence + 0.001f);
        }

        BaseDo.create(functionDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.functionRepository.save(functionDo);

        //
        // Step 3, post-processing
        //
        TreeNode treeNode = new TreeNode();
        treeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
        treeNode.setUid(functionDo.getUid());
        treeNode.setName(functionDo.getName());
        treeNode.setDescription(functionDo.getDescription());
        treeNode.setType(TreeNode.GENERAL_TYPE_ENTITY);

        return treeNode;
    }

    @Override
    public List<TreeNode> batchCreateEntityNodesForFunctionHierarchy(
            Long parentUid,
            List<CreateEntityTreeNode> createEntityTreeNodeList,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        if (CollectionUtils.isEmpty(createEntityTreeNodeList)) {
            return null;
        }

        List<TreeNode> result = new LinkedList<>();
        createEntityTreeNodeList.forEach(createEntityNodeDto -> {
            TreeNode treeNode = createEntityNodeForFunctionHierarchy(parentUid, createEntityNodeDto,
                    operatingUserProfile);
            result.add(treeNode);
        });
        return result;
    }

    @Override
    public void replaceNodeRelationshipOfFunctionHierarchy(
            Long uid,
            ReplaceTreeNodeRelationship replaceTreeNodeRelationship,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        FunctionDo itemDo = this.functionRepository.findByUid(uid);
        if (itemDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", FunctionDo.RESOURCE_SYMBOL,
                    uid));
        }

        if (replaceTreeNodeRelationship.getReferenceTreeNodeUid() == null) {
            // 调整到根节点下，作为其最末尾子节点
            Specification<FunctionDo> specification = new Specification<FunctionDo>() {
                @Override
                public Predicate toPredicate(Root<FunctionDo> root, CriteriaQuery<?> query,
                                             CriteriaBuilder criteriaBuilder) {
                    List<Predicate> predicateList = new ArrayList<>();
                    predicateList.add(criteriaBuilder.isNull(root.get("parentUid")));
                    return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
                }
            };

            Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("sequence")));
            Page<FunctionDo> candidateItemDoPage = this.functionRepository.findAll(specification, pageable);
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
            this.functionRepository.save(itemDo);

            return;
        }

        FunctionDo referenceItemDo =
                this.functionRepository.findByUid(replaceTreeNodeRelationship.getReferenceTreeNodeUid());
        if (referenceItemDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", FunctionDo.RESOURCE_SYMBOL,
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
                    this.functionRepository.save(itemDo);

                    referenceItemDo.setSequence(2.0f);
                    BaseDo.update(referenceItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.functionRepository.save(referenceItemDo);
                } else {
                    Specification<FunctionDo> specification = new Specification<FunctionDo>() {
                        @Override
                        public Predicate toPredicate(Root<FunctionDo> root, CriteriaQuery<?> query,
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
                    Page<FunctionDo> candidateItemDoPage = this.functionRepository.findAll(specification, pageable);
                    if (candidateItemDoPage.isEmpty()) {
                        itemDo.setSequence(referenceItemDo.getSequence() * 0.5f);
                    } else {
                        float gap =
                                referenceItemDo.getSequence() - candidateItemDoPage.getContent().get(0).getSequence();
                        itemDo.setSequence(referenceItemDo.getSequence() - gap / 2);
                    }

                    BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.functionRepository.save(itemDo);
                }
            }
            break;
            case CENTER: {
                itemDo.setParentUid(referenceItemDo.getUid());

                Specification<FunctionDo> specification = new Specification<FunctionDo>() {
                    @Override
                    public Predicate toPredicate(Root<FunctionDo> root, CriteriaQuery<?> query,
                                                 CriteriaBuilder criteriaBuilder) {
                        List<Predicate> predicateList = new ArrayList<>();

                        predicateList.add(criteriaBuilder.equal(root.get("parentUid"), referenceItemDo.getUid()));
                        return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
                    }
                };

                Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("sequence")));
                Page<FunctionDo> candidateDoPage = this.functionRepository.findAll(specification, pageable);
                if (candidateDoPage.isEmpty()) {
                    itemDo.setSequence(1.0f);
                } else {
                    itemDo.setSequence(candidateDoPage.getContent().get(0).getSequence() + 1.0f);
                }

                BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                this.functionRepository.save(itemDo);
            }
            break;
            case REAR: {
                itemDo.setParentUid(referenceItemDo.getParentUid());

                if (referenceItemDo.getSequence() == null) {
                    itemDo.setSequence(2.0f);
                    BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.functionRepository.save(itemDo);

                    referenceItemDo.setSequence(1.0f);
                    BaseDo.update(referenceItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.functionRepository.save(referenceItemDo);
                } else {
                    Specification<FunctionDo> specification = new Specification<FunctionDo>() {
                        @Override
                        public Predicate toPredicate(Root<FunctionDo> root, CriteriaQuery<?> query,
                                                     CriteriaBuilder criteriaBuilder) {
                            List<Predicate> predicateList = new ArrayList<>();
                            if (referenceItemDo.getParentUid() != null) {
                                predicateList.add(criteriaBuilder.equal(root.get("parentUid"), referenceItemDo.getParentUid()));
                            } else {
                                predicateList.add(criteriaBuilder.isNull(root.get("parentUid")));
                            }
                            predicateList.add(criteriaBuilder.greaterThan(root.get("sequence"),
                                    referenceItemDo.getSequence()));
                            return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
                        }
                    };

                    Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Order.asc("sequence")));
                    Page<FunctionDo> candidateDoPage = this.functionRepository.findAll(specification, pageable);
                    if (candidateDoPage.isEmpty()) {
                        itemDo.setSequence(referenceItemDo.getSequence() + 1.0f);
                    } else {
                        float gap =
                                candidateDoPage.getContent().get(0).getSequence() - referenceItemDo.getSequence();
                        itemDo.setSequence(referenceItemDo.getSequence() + gap / 2);
                    }

                    BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.functionRepository.save(itemDo);
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
    public List<String> listAllReferencesToNodeOfFunctionHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return null;
    }

    @Override
    public void deleteNodeOfFunctionHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        FunctionDo functionDo = this.functionRepository.findByUid(uid);
        if (functionDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", FunctionDo.RESOURCE_SYMBOL, uid));
        }

        //
        // Step 2, core-processing
        //
        recursivelyDeleteNodeOfFunctionHierarchy(functionDo, operatingUserProfile);

        //
        // Step 3, post-processing
        //
    }

    private void recursivelyDeleteNodeOfFunctionHierarchy(
            FunctionDo functionDo,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        // 找出所有的子节点，删除
        List<FunctionDo> children =
                this.functionRepository.findByParentUid(functionDo.getUid());
        if (!CollectionUtils.isEmpty(children)) {
            children.forEach(child -> {
                recursivelyDeleteNodeOfFunctionHierarchy(child, operatingUserProfile);
            });
        }

        // 删除自己
        this.functionRepository.delete(functionDo);
    }

    @Override
    public List<TreeNode> treeListingAllNodesOfNavigationMenuHierarchy(
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Map<Long, TreeNode> itemUidAndTreeNodeMap = new HashMap<>();
        List<TreeNode> rootTreeNodeList = new LinkedList<>();
        this.navigationMenuRepository.findAll().forEach(itemDo -> {
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
            if (itemDo.getParentUid() != null) {
                treeNode.getTags().put("parent_uid", itemDo.getParentUid());
            }
            treeNode.getTags().put("uri", itemDo.getUri());
            treeNode.getTags().put("icon", itemDo.getIcon());
            treeNode.getTags().put("component_name", itemDo.getComponentName());
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
    public TreeNode createDirectoryNodeForNavigationMenuHierarchy(
            Long parentUid,
            CreateDirectoryTreeNode createDirectoryTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        if (parentUid == null) {
            Integer existsDuplicate = this.navigationMenuRepository.existsByNameWithoutParent(
                    createDirectoryTreeNode.getName());
            if (existsDuplicate > 0) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s", NavigationMenuDo.RESOURCE_SYMBOL,
                        createDirectoryTreeNode.getName()));
            }
        } else {
            Integer existsDuplicate = this.navigationMenuRepository.existsByNameWithinParent(
                    createDirectoryTreeNode.getName(), parentUid);
            if (existsDuplicate > 0) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s", NavigationMenuDo.RESOURCE_SYMBOL,
                        createDirectoryTreeNode.getName()));
            }

            boolean exists = this.navigationMenuRepository.existsByUid(parentUid);
            if (!exists) {
                throw new AbcResourceNotFoundException(String.format("%s::uid=%s", NavigationMenuDo.RESOURCE_SYMBOL,
                        parentUid));
            }
        }

        //
        // Step 2, core-processing
        //
        NavigationMenuDo navigationMenuDo = new NavigationMenuDo();
        navigationMenuDo.setUid(this.idHelper.getNextDistributedId(NavigationMenuDo.RESOURCE_NAME));
        navigationMenuDo.setName(createDirectoryTreeNode.getName());
        navigationMenuDo.setDescription(createDirectoryTreeNode.getDescription());
        navigationMenuDo.setDirectory(Boolean.TRUE);
        navigationMenuDo.setParentUid(parentUid);

        // sequence
        Float maxSequence = null;
        if (parentUid == null) {
            maxSequence = this.navigationMenuRepository.findMaxSequenceWithoutParent();
        } else {
            maxSequence = this.navigationMenuRepository.findMaxSequenceWithinParent(parentUid);
        }
        if (maxSequence == null) {
            navigationMenuDo.setSequence(0.001f);
        } else {
            navigationMenuDo.setSequence(maxSequence + 0.001f);
        }

        BaseDo.create(navigationMenuDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.navigationMenuRepository.save(navigationMenuDo);

        //
        // Step 3, post-processing
        //
        TreeNode treeNode = new TreeNode();
        treeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
        treeNode.setUid(navigationMenuDo.getUid());
        treeNode.setName(navigationMenuDo.getName());
        treeNode.setDescription(navigationMenuDo.getDescription());
        treeNode.setType(TreeNode.GENERAL_TYPE_DIRECTORY);

        return treeNode;
    }

    @Override
    public void updateDirectoryNodeOfNavigationMenuHierarchy(
            Long uid,
            UpdateDirectoryTreeNode updateDirectoryTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        NavigationMenuDo navigationMenuDo = this.navigationMenuRepository.findByUid(uid);
        if (navigationMenuDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", NavigationMenuDo.RESOURCE_SYMBOL, uid));
        }
        if (!ObjectUtils.isEmpty(updateDirectoryTreeNode.getName())
                && !updateDirectoryTreeNode.getName().equalsIgnoreCase(navigationMenuDo.getName())) {
            // name 要改
            if (navigationMenuDo.getParentUid() == null) {
                // 没有 parent 的情况
                Integer existsDuplicate = this.navigationMenuRepository.existsByNameWithoutParent(
                        updateDirectoryTreeNode.getName());
                if (existsDuplicate > 0) {
                    throw new AbcResourceDuplicateException(String.format("%s::name=%s", NavigationMenuDo.RESOURCE_SYMBOL,
                            updateDirectoryTreeNode.getName()));
                }
            } else {
                // 有 parent 的情况
                Integer existsDuplicate = this.navigationMenuRepository.existsByNameWithinParent(
                        updateDirectoryTreeNode.getName(), navigationMenuDo.getParentUid());
                if (existsDuplicate > 0) {
                    throw new AbcResourceDuplicateException(String.format("%s::name=%s", NavigationMenuDo.RESOURCE_SYMBOL,
                            updateDirectoryTreeNode.getName()));
                }
            }
        }

        //
        // Step 2, core-processing
        //
        boolean requiredToUpdate = false;

        if (!ObjectUtils.isEmpty(updateDirectoryTreeNode.getName())
                && !updateDirectoryTreeNode.getName().equalsIgnoreCase(navigationMenuDo.getName())) {
            navigationMenuDo.setName(updateDirectoryTreeNode.getName());
            requiredToUpdate = true;
        }
        if (updateDirectoryTreeNode.getDescription() != null
                && !updateDirectoryTreeNode.getDescription().equalsIgnoreCase(navigationMenuDo.getDescription())) {
            navigationMenuDo.setDescription(updateDirectoryTreeNode.getDescription());
            requiredToUpdate = true;
        }

        if (requiredToUpdate) {
            BaseDo.update(navigationMenuDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.navigationMenuRepository.save(navigationMenuDo);
        }

        //
        // Step 3, post-processing
        //
    }

    @Override
    public TreeNode createEntityNodeForNavigationMenuHierarchy(
            Long parentUid,
            CreateEntityTreeNode createEntityTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        if (parentUid == null) {
            Integer existsDuplicate = this.navigationMenuRepository.existsByNameWithoutParent(
                    createEntityTreeNode.getName());
            if (existsDuplicate > 0) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s", NavigationMenuDo.RESOURCE_SYMBOL,
                        createEntityTreeNode.getName()));
            }
        } else {
            Integer existsDuplicate = this.navigationMenuRepository.existsByNameWithinParent(
                    createEntityTreeNode.getName(), parentUid);
            if (existsDuplicate > 0) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s", NavigationMenuDo.RESOURCE_SYMBOL,
                        createEntityTreeNode.getName()));
            }

            boolean exists = this.navigationMenuRepository.existsByUid(parentUid);
            if (!exists) {
                throw new AbcResourceNotFoundException(String.format("%s::uid=%s", NavigationMenuDo.RESOURCE_SYMBOL,
                        parentUid));
            }
        }

        //
        // Step 2, core-processing
        //
        NavigationMenuDo navigationMenuDo = new NavigationMenuDo();
        navigationMenuDo.setUid(this.idHelper.getNextDistributedId(NavigationMenuDo.RESOURCE_NAME));
        navigationMenuDo.setName(createEntityTreeNode.getName());
        navigationMenuDo.setDescription(createEntityTreeNode.getDescription());
        navigationMenuDo.setDirectory(Boolean.FALSE);
        navigationMenuDo.setParentUid(parentUid);

        // entity node translation
        JSONObject payload = createEntityTreeNode.getPayload();
        NavigationMenuEntityNode navigationMenuEntityNode = JSONObject.toJavaObject(payload, NavigationMenuEntityNode.class);
        navigationMenuDo.setUri(navigationMenuEntityNode.getUri());
        navigationMenuDo.setIcon(navigationMenuEntityNode.getIcon());
        navigationMenuDo.setComponentName(navigationMenuEntityNode.getComponentName());

        // sequence
        Float maxSequence = null;
        if (parentUid == null) {
            maxSequence = this.navigationMenuRepository.findMaxSequenceWithoutParent();
        } else {
            maxSequence = this.navigationMenuRepository.findMaxSequenceWithinParent(parentUid);
        }
        if (maxSequence == null) {
            navigationMenuDo.setSequence(0.001f);
        } else {
            navigationMenuDo.setSequence(maxSequence + 0.001f);
        }

        BaseDo.create(navigationMenuDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.navigationMenuRepository.save(navigationMenuDo);

        //
        // Step 3, post-processing
        //
        TreeNode treeNode = new TreeNode();
        treeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
        treeNode.setUid(navigationMenuDo.getUid());
        treeNode.setName(navigationMenuDo.getName());
        treeNode.setDescription(navigationMenuDo.getDescription());
        treeNode.setType(TreeNode.GENERAL_TYPE_ENTITY);
        treeNode.setTags(new HashMap<>());
        treeNode.getTags().put("parent_uid", navigationMenuDo.getParentUid());
        treeNode.getTags().put("uri", navigationMenuDo.getUri());
        treeNode.getTags().put("icon", navigationMenuDo.getIcon());
        treeNode.getTags().put("component_name", navigationMenuDo.getComponentName());
        treeNode.getTags().put("sequence", navigationMenuDo.getSequence());

        return treeNode;
    }

    @Override
    public void updateEntityNodeOfNavigationMenuHierarchy(
            Long uid,
            UpdateEntityTreeNode updateEntityTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        NavigationMenuDo navigationMenuDo = this.navigationMenuRepository.findByUid(uid);
        if (navigationMenuDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", NavigationMenuDo.RESOURCE_SYMBOL, uid));
        }
        if (!ObjectUtils.isEmpty(updateEntityTreeNode.getName())
                && !updateEntityTreeNode.getName().equalsIgnoreCase(navigationMenuDo.getName())) {
            // name 要改
            if (navigationMenuDo.getParentUid() == null) {
                // 没有 parent 的情况
                Integer existsDuplicate = this.navigationMenuRepository.existsByNameWithoutParent(
                        updateEntityTreeNode.getName());
                if (existsDuplicate > 0) {
                    throw new AbcResourceDuplicateException(String.format("%s::name=%s", NavigationMenuDo.RESOURCE_SYMBOL,
                            updateEntityTreeNode.getName()));
                }
            } else {
                // 有 parent 的情况
                Integer existsDuplicate = this.navigationMenuRepository.existsByNameWithinParent(
                        updateEntityTreeNode.getName(), navigationMenuDo.getParentUid());
                if (existsDuplicate > 0) {
                    throw new AbcResourceDuplicateException(String.format("%s::name=%s", NavigationMenuDo.RESOURCE_SYMBOL,
                            updateEntityTreeNode.getName()));
                }
            }
        }

        //
        // Step 2, core-processing
        //
        boolean requiredToUpdate = false;

        if (!ObjectUtils.isEmpty(updateEntityTreeNode.getName())
                && !updateEntityTreeNode.getName().equalsIgnoreCase(navigationMenuDo.getName())) {
            navigationMenuDo.setName(updateEntityTreeNode.getName());
            requiredToUpdate = true;
        }
        if (updateEntityTreeNode.getDescription() != null
                && !updateEntityTreeNode.getDescription().equalsIgnoreCase(navigationMenuDo.getDescription())) {
            navigationMenuDo.setDescription(updateEntityTreeNode.getDescription());
            requiredToUpdate = true;
        }
        if (updateEntityTreeNode.getPayload() != null) {
            // entity node translation
            JSONObject payload = updateEntityTreeNode.getPayload();
            NavigationMenuEntityNode navigationMenuEntityNode = JSONObject.toJavaObject(payload, NavigationMenuEntityNode.class);
            if (navigationMenuEntityNode.getUri() != null
                    && !navigationMenuEntityNode.getUri().equalsIgnoreCase(navigationMenuDo.getUri())) {
                navigationMenuDo.setUri(navigationMenuEntityNode.getUri());
                requiredToUpdate = true;
            }
            if (navigationMenuEntityNode.getIcon() != null
                    && !navigationMenuEntityNode.getIcon().equalsIgnoreCase(navigationMenuDo.getIcon())) {
                navigationMenuDo.setIcon(navigationMenuEntityNode.getIcon());
                requiredToUpdate = true;
            }
            if (navigationMenuEntityNode.getComponentName() != null
                    && !navigationMenuEntityNode.getComponentName().equalsIgnoreCase(navigationMenuDo.getComponentName())) {
                navigationMenuDo.setComponentName(navigationMenuEntityNode.getComponentName());
                requiredToUpdate = true;
            }
        }

        if (requiredToUpdate) {
            BaseDo.update(navigationMenuDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.navigationMenuRepository.save(navigationMenuDo);
        }

        //
        // Step 3, post-processing
        //
    }

    @Override
    public void replaceNodeRelationshipOfNavigationMenuHierarchy(
            Long uid,
            ReplaceTreeNodeRelationship replaceTreeNodeRelationship,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        NavigationMenuDo itemDo = this.navigationMenuRepository.findByUid(uid);
        if (itemDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", NavigationMenuDo.RESOURCE_SYMBOL,
                    uid));
        }

        if (replaceTreeNodeRelationship.getReferenceTreeNodeUid() == null) {
            // 调整到根节点下，作为其最末尾子节点
            Specification<NavigationMenuDo> specification = new Specification<NavigationMenuDo>() {
                @Override
                public Predicate toPredicate(Root<NavigationMenuDo> root, CriteriaQuery<?> query,
                                             CriteriaBuilder criteriaBuilder) {
                    List<Predicate> predicateList = new ArrayList<>();
                    predicateList.add(criteriaBuilder.isNull(root.get("parentUid")));
                    return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
                }
            };

            Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("sequence")));
            Page<NavigationMenuDo> candidateItemDoPage = this.navigationMenuRepository.findAll(specification, pageable);
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
            this.navigationMenuRepository.save(itemDo);

            return;
        }

        NavigationMenuDo referenceItemDo =
                this.navigationMenuRepository.findByUid(replaceTreeNodeRelationship.getReferenceTreeNodeUid());
        if (referenceItemDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", NavigationMenuDo.RESOURCE_SYMBOL,
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
                    this.navigationMenuRepository.save(itemDo);

                    referenceItemDo.setSequence(2.0f);
                    BaseDo.update(referenceItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.navigationMenuRepository.save(referenceItemDo);
                } else {
                    Specification<NavigationMenuDo> specification = new Specification<NavigationMenuDo>() {
                        @Override
                        public Predicate toPredicate(Root<NavigationMenuDo> root, CriteriaQuery<?> query,
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
                    Page<NavigationMenuDo> candidateItemDoPage = this.navigationMenuRepository.findAll(specification, pageable);
                    if (candidateItemDoPage.isEmpty()) {
                        itemDo.setSequence(referenceItemDo.getSequence() * 0.5f);
                    } else {
                        float gap =
                                referenceItemDo.getSequence() - candidateItemDoPage.getContent().get(0).getSequence();
                        itemDo.setSequence(referenceItemDo.getSequence() - gap / 2);
                    }

                    BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.navigationMenuRepository.save(itemDo);
                }
            }
            break;
            case CENTER: {
                itemDo.setParentUid(referenceItemDo.getUid());

                Specification<NavigationMenuDo> specification = new Specification<NavigationMenuDo>() {
                    @Override
                    public Predicate toPredicate(Root<NavigationMenuDo> root, CriteriaQuery<?> query,
                                                 CriteriaBuilder criteriaBuilder) {
                        List<Predicate> predicateList = new ArrayList<>();

                        predicateList.add(criteriaBuilder.equal(root.get("parentUid"), referenceItemDo.getUid()));
                        return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
                    }
                };

                Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("sequence")));
                Page<NavigationMenuDo> candidateDoPage = this.navigationMenuRepository.findAll(specification, pageable);
                if (candidateDoPage.isEmpty()) {
                    itemDo.setSequence(1.0f);
                } else {
                    itemDo.setSequence(candidateDoPage.getContent().get(0).getSequence() + 1.0f);
                }

                BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                this.navigationMenuRepository.save(itemDo);
            }
            break;
            case REAR: {
                itemDo.setParentUid(referenceItemDo.getParentUid());

                if (referenceItemDo.getSequence() == null) {
                    itemDo.setSequence(2.0f);
                    BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.navigationMenuRepository.save(itemDo);

                    referenceItemDo.setSequence(1.0f);
                    BaseDo.update(referenceItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.navigationMenuRepository.save(referenceItemDo);
                } else {
                    Specification<NavigationMenuDo> specification = new Specification<NavigationMenuDo>() {
                        @Override
                        public Predicate toPredicate(Root<NavigationMenuDo> root, CriteriaQuery<?> query,
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
                    Page<NavigationMenuDo> candidateDoPage = this.navigationMenuRepository.findAll(specification, pageable);
                    if (candidateDoPage.isEmpty()) {
                        itemDo.setSequence(referenceItemDo.getSequence() + 1.0f);
                    } else {
                        float gap =
                                candidateDoPage.getContent().get(0).getSequence() - referenceItemDo.getSequence();
                        itemDo.setSequence(referenceItemDo.getSequence() + gap / 2);
                    }

                    BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.navigationMenuRepository.save(itemDo);
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
    public List<String> listAllReferencesToNodeOfNavigationMenuHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        //
        // Step 2, core-processing
        //

        //
        // Step 3, post-processing
        //
        return null;
    }

    @Override
    public void deleteNodeOfNavigationMenuHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        NavigationMenuDo navigationMenuDo = this.navigationMenuRepository.findByUid(uid);
        if (navigationMenuDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", NavigationMenuDo.RESOURCE_SYMBOL, uid));
        }

        //
        // Step 2, core-processing
        //
        recursiveDeleteNodeOfNavigationMenuHierarchy(navigationMenuDo, operatingUserProfile);

        //
        // Step 3, post-processing
        //
    }

    private void recursiveDeleteNodeOfNavigationMenuHierarchy(
            NavigationMenuDo navigationMenuDo,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        // 找出所有的子节点，删除
        List<NavigationMenuDo> children =
                this.navigationMenuRepository.findByParentUid(navigationMenuDo.getUid());
        if (!CollectionUtils.isEmpty(children)) {
            children.forEach(child -> {
                recursiveDeleteNodeOfNavigationMenuHierarchy(child, operatingUserProfile);
            });
        }

        // 删除自己
        this.navigationMenuRepository.delete(navigationMenuDo);
    }
}
