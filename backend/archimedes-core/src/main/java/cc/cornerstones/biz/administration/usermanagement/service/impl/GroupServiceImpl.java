package cc.cornerstones.biz.administration.usermanagement.service.impl;

import cc.cornerstones.almond.exceptions.AbcResourceDuplicateException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.*;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.administration.usermanagement.dto.*;
import cc.cornerstones.biz.administration.usermanagement.entity.GroupDo;
import cc.cornerstones.biz.administration.usermanagement.entity.GroupRoleDo;
import cc.cornerstones.biz.administration.usermanagement.persistence.GroupRoleRepository;
import cc.cornerstones.biz.administration.usermanagement.persistence.GroupRepository;
import cc.cornerstones.biz.administration.usermanagement.service.inf.GroupService;
import cc.cornerstones.biz.share.event.GroupDeletedEvent;
import cc.cornerstones.biz.share.event.EventBusManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class GroupServiceImpl implements GroupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupRoleRepository groupRoleRepository;

    @Override
    public List<TreeNode> treeListingAllNodesOfGroupHierarchy(
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Map<Long, TreeNode> itemUidAndTreeNodeMap = new HashMap<>();
        List<TreeNode> rootTreeNodeList = new LinkedList<>();
        this.groupRepository.findAll().forEach(itemDo -> {
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
    public TreeNode createEntityNodeForGroupHierarchy(
            Long parentUid,
            CreateEntityTreeNode createEntityTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        if (parentUid == null) {
            Integer existsDuplicate = this.groupRepository.existsByNameWithoutParent(
                    createEntityTreeNode.getName());
            if (existsDuplicate > 0) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s", GroupDo.RESOURCE_SYMBOL,
                        createEntityTreeNode.getName()));
            }
        } else {
            Integer existsDuplicate = this.groupRepository.existsByNameWithinParent(
                    createEntityTreeNode.getName(), parentUid);
            if (existsDuplicate > 0) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s", GroupDo.RESOURCE_SYMBOL,
                        createEntityTreeNode.getName()));
            }

            boolean exists = this.groupRepository.existsByUid(parentUid);
            if (!exists) {
                throw new AbcResourceNotFoundException(String.format("%s::uid=%s", GroupDo.RESOURCE_SYMBOL,
                        parentUid));
            }
        }

        //
        // Step 2, core-processing
        //
        GroupDo groupDo = new GroupDo();
        groupDo.setUid(this.idHelper.getNextDistributedId(GroupDo.RESOURCE_NAME));
        groupDo.setName(createEntityTreeNode.getName());
        groupDo.setObjectName(
                createEntityTreeNode.getName()
                        .replaceAll("_", "__")
                        .replaceAll("\\s", "_")
                        .toLowerCase());
        groupDo.setDescription(createEntityTreeNode.getDescription());
        groupDo.setDirectory(Boolean.FALSE);
        groupDo.setParentUid(parentUid);

        // entity node translation
        // N/A

        // sequence
        Float maxSequence = null;
        if (parentUid == null) {
            maxSequence = this.groupRepository.findMaxSequenceWithoutParent();
        } else {
            maxSequence = this.groupRepository.findMaxSequenceWithinParent(parentUid);
        }
        if (maxSequence == null) {
            groupDo.setSequence(0.001f);
        } else {
            groupDo.setSequence(maxSequence + 0.001f);
        }

        BaseDo.create(groupDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.groupRepository.save(groupDo);

        //
        // Step 3, post-processing
        //
        TreeNode treeNode = new TreeNode();
        treeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
        treeNode.setUid(groupDo.getUid());
        treeNode.setName(groupDo.getName());
        treeNode.setDescription(groupDo.getDescription());
        treeNode.setType(TreeNode.GENERAL_TYPE_ENTITY);

        return treeNode;
    }

    @Override
    public void updateEntityNodeOfGroupHierarchy(
            Long uid,
            UpdateEntityTreeNode updateEntityTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        GroupDo groupDo = this.groupRepository.findByUid(uid);
        if (groupDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", GroupDo.RESOURCE_SYMBOL, uid));
        }
        if (!ObjectUtils.isEmpty(updateEntityTreeNode.getName())
                && !updateEntityTreeNode.getName().equalsIgnoreCase(groupDo.getName())) {
            // name 要改
            if (groupDo.getParentUid() == null) {
                // 没有 parent 的情况
                Integer existsDuplicate = this.groupRepository.existsByNameWithoutParent(
                        updateEntityTreeNode.getName());
                if (existsDuplicate > 0) {
                    throw new AbcResourceDuplicateException(String.format("%s::name=%s", GroupDo.RESOURCE_SYMBOL,
                            updateEntityTreeNode.getName()));
                }
            } else {
                // 有 parent 的情况
                Integer existsDuplicate = this.groupRepository.existsByNameWithinParent(
                        updateEntityTreeNode.getName(), groupDo.getParentUid());
                if (existsDuplicate > 0) {
                    throw new AbcResourceDuplicateException(String.format("%s::name=%s", GroupDo.RESOURCE_SYMBOL,
                            updateEntityTreeNode.getName()));
                }
            }
        }

        //
        // Step 2, core-processing
        //
        boolean requiredToUpdate = false;

        if (!ObjectUtils.isEmpty(updateEntityTreeNode.getName())
                && !updateEntityTreeNode.getName().equalsIgnoreCase(groupDo.getName())) {
            groupDo.setName(updateEntityTreeNode.getName());
            groupDo.setObjectName(
                    updateEntityTreeNode.getName()
                            .replaceAll("_", "__")
                            .replaceAll("\\s", "_")
                            .toLowerCase());
            requiredToUpdate = true;
        }
        if (updateEntityTreeNode.getDescription() != null
                && !updateEntityTreeNode.getDescription().equalsIgnoreCase(groupDo.getDescription())) {
            groupDo.setDescription(updateEntityTreeNode.getDescription());
            requiredToUpdate = true;
        }

        if (requiredToUpdate) {
            BaseDo.update(groupDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.groupRepository.save(groupDo);
        }

        //
        // Step 3, post-processing
        //

    }

    @Override
    public void replaceNodeRelationshipOfFunctionalHierarchy(
            Long uid,
            ReplaceTreeNodeRelationship replaceTreeNodeRelationship,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        GroupDo itemDo = this.groupRepository.findByUid(uid);
        if (itemDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", GroupDo.RESOURCE_SYMBOL,
                    uid));
        }

        if (replaceTreeNodeRelationship.getReferenceTreeNodeUid() == null) {
            // 调整到根节点下，作为其最末尾子节点
            Specification<GroupDo> specification = new Specification<GroupDo>() {
                @Override
                public Predicate toPredicate(Root<GroupDo> root, CriteriaQuery<?> query,
                                             CriteriaBuilder criteriaBuilder) {
                    List<Predicate> predicateList = new ArrayList<>();
                    predicateList.add(criteriaBuilder.isNull(root.get("parentUid")));
                    return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
                }
            };

            Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("sequence")));
            Page<GroupDo> candidateItemDoPage = this.groupRepository.findAll(specification, pageable);
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
            this.groupRepository.save(itemDo);

            return;
        }

        GroupDo referenceItemDo =
                this.groupRepository.findByUid(replaceTreeNodeRelationship.getReferenceTreeNodeUid());
        if (referenceItemDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", GroupDo.RESOURCE_SYMBOL,
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
                    this.groupRepository.save(itemDo);

                    referenceItemDo.setSequence(2.0f);
                    BaseDo.update(referenceItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.groupRepository.save(referenceItemDo);
                } else {
                    Specification<GroupDo> specification = new Specification<GroupDo>() {
                        @Override
                        public Predicate toPredicate(Root<GroupDo> root, CriteriaQuery<?> query,
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
                    Page<GroupDo> candidateItemDoPage = this.groupRepository.findAll(specification, pageable);
                    if (candidateItemDoPage.isEmpty()) {
                        itemDo.setSequence(referenceItemDo.getSequence() * 0.5f);
                    } else {
                        float gap =
                                referenceItemDo.getSequence() - candidateItemDoPage.getContent().get(0).getSequence();
                        itemDo.setSequence(referenceItemDo.getSequence() - gap / 2);
                    }

                    BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.groupRepository.save(itemDo);
                }
            }
            break;
            case CENTER: {
                itemDo.setParentUid(referenceItemDo.getUid());

                Specification<GroupDo> specification = new Specification<GroupDo>() {
                    @Override
                    public Predicate toPredicate(Root<GroupDo> root, CriteriaQuery<?> query,
                                                 CriteriaBuilder criteriaBuilder) {
                        List<Predicate> predicateList = new ArrayList<>();

                        predicateList.add(criteriaBuilder.equal(root.get("parentUid"), referenceItemDo.getUid()));
                        return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
                    }
                };

                Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("sequence")));
                Page<GroupDo> candidateItemDoPage = this.groupRepository.findAll(specification, pageable);
                if (candidateItemDoPage.isEmpty()) {
                    itemDo.setSequence(1.0f);
                } else {
                    itemDo.setSequence(candidateItemDoPage.getContent().get(0).getSequence() + 1.0f);
                }

                BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                this.groupRepository.save(itemDo);
            }
            break;
            case REAR: {
                itemDo.setParentUid(referenceItemDo.getParentUid());

                if (referenceItemDo.getSequence() == null) {
                    itemDo.setSequence(2.0f);
                    BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.groupRepository.save(itemDo);

                    referenceItemDo.setSequence(1.0f);
                    BaseDo.update(referenceItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.groupRepository.save(referenceItemDo);
                } else {
                    Specification<GroupDo> specification = new Specification<GroupDo>() {
                        @Override
                        public Predicate toPredicate(Root<GroupDo> root, CriteriaQuery<?> query,
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
                    Page<GroupDo> candidateItemDoPage = this.groupRepository.findAll(specification, pageable);
                    if (candidateItemDoPage.isEmpty()) {
                        itemDo.setSequence(referenceItemDo.getSequence() + 1.0f);
                    } else {
                        float gap =
                                candidateItemDoPage.getContent().get(0).getSequence() - referenceItemDo.getSequence();
                        itemDo.setSequence(referenceItemDo.getSequence() + gap / 2);
                    }

                    BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.groupRepository.save(itemDo);
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
    public List<String> listAllReferencesToNodeOfGroupHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return null;
    }

    @Override
    public void deleteNodeOfGroupHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        GroupDo groupDo = this.groupRepository.findByUid(uid);
        if (groupDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", GroupDo.RESOURCE_SYMBOL, uid));
        }

        //
        // Step 2, core-processing
        //
        recursiveDeleteNodeOfGroupHierarchy(groupDo, operatingUserProfile);

        //
        // Step 3, post-processing
        //
    }

    private void recursiveDeleteNodeOfGroupHierarchy(
            GroupDo groupDo,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        // 找出所有的子节点，删除
        List<GroupDo> children =
                this.groupRepository.findByParentUid(groupDo.getUid());
        if (!CollectionUtils.isEmpty(children)) {
            children.forEach(child -> {
                recursiveDeleteNodeOfGroupHierarchy(child, operatingUserProfile);
            });
        }

        // 删除自己
        this.groupRepository.delete(groupDo);

        GroupDeletedEvent groupDeletedEvent = new GroupDeletedEvent();
        groupDeletedEvent.setUid(groupDo.getUid());
        groupDeletedEvent.setOperatingUserProfile(operatingUserProfile);
        this.eventBusManager.send(groupDeletedEvent);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createOrReplaceCommonRolesForAllGroups(
            CreateOrReplaceRolesDto createOrReplaceRolesDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        LocalDateTime now = LocalDateTime.now();

        // for all groups

        // delete first
        this.groupRoleRepository.deleteAllWithoutGroup();

        // insert second
        List<GroupRoleDo> groupRoleDoList = new LinkedList<>();
        if (!CollectionUtils.isEmpty(createOrReplaceRolesDto.getRoleUidList())) {
            createOrReplaceRolesDto.getRoleUidList().forEach(roleUid -> {

                GroupRoleDo groupRoleDo = new GroupRoleDo();
                groupRoleDo.setGroupUid(null);
                groupRoleDo.setRoleUid(roleUid);
                BaseDo.create(groupRoleDo, operatingUserProfile.getUid(), now);

                groupRoleDoList.add(groupRoleDo);
            });
        }

        if (!CollectionUtils.isEmpty(groupRoleDoList)) {
            this.groupRoleRepository.saveAll(groupRoleDoList);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createOrReplaceRolesForGivenGroup(
            Long uid,
            CreateOrReplaceRolesDto createOrReplaceRolesDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        LocalDateTime now = LocalDateTime.now();

        // for the specific group

        // delete first
        this.groupRoleRepository.deleteByGroupUid(uid);

        // insert second
        List<GroupRoleDo> groupRoleDoList = new LinkedList<>();
        if (!CollectionUtils.isEmpty(createOrReplaceRolesDto.getRoleUidList())) {
            createOrReplaceRolesDto.getRoleUidList().forEach(roleUid -> {
                    GroupRoleDo groupRoleDo = new GroupRoleDo();
                    groupRoleDo.setGroupUid(uid);
                    groupRoleDo.setRoleUid(roleUid);
                    BaseDo.create(groupRoleDo, operatingUserProfile.getUid(), now);

                    groupRoleDoList.add(groupRoleDo);
            });
        }

        if (!CollectionUtils.isEmpty(groupRoleDoList)) {
            this.groupRoleRepository.saveAll(groupRoleDoList);
        }
    }

    @Override
    public List<Long> getCommonRolesOfAllGroups(
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        List<GroupRoleDo> groupRoleDoList = this.groupRoleRepository.findAllWithoutGroupUid();
        if (CollectionUtils.isEmpty(groupRoleDoList)) {
            return null;
        }

        List<Long> roleUidList = new LinkedList<>();
        groupRoleDoList.forEach(groupRoleDo -> {
            roleUidList.add(groupRoleDo.getRoleUid());
        });
        return roleUidList;
    }


    @Override
    public List<Long> getRolesOfGivenGroup(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        List<GroupRoleDo> groupRoleDoList = this.groupRoleRepository.findByGroupUid(uid);
        if (CollectionUtils.isEmpty(groupRoleDoList)) {
            return null;
        }

        List<Long> roleUidList = new LinkedList<>();
        groupRoleDoList.forEach(groupRoleDo -> {
            roleUidList.add(groupRoleDo.getRoleUid());
        });
        return roleUidList;
    }
}
