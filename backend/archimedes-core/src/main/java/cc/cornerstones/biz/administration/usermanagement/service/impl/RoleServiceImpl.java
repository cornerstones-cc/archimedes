package cc.cornerstones.biz.administration.usermanagement.service.impl;

import cc.cornerstones.almond.exceptions.AbcResourceDuplicateException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.*;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.administration.usermanagement.dto.*;
import cc.cornerstones.biz.administration.usermanagement.entity.*;
import cc.cornerstones.biz.administration.usermanagement.persistence.RolePermissionRepository;
import cc.cornerstones.biz.administration.usermanagement.persistence.RoleRepository;
import cc.cornerstones.biz.administration.usermanagement.service.inf.RoleService;
import cc.cornerstones.biz.administration.usermanagement.share.constants.PermissionTypeEnum;
import cc.cornerstones.biz.administration.usermanagement.share.types.SimplePermissions;
import cc.cornerstones.biz.share.event.RoleDeletedEvent;
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
public class RoleServiceImpl implements RoleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Override
    public List<TreeNode> treeListingAllNodesOfRoleHierarchy(
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Map<Long, TreeNode> itemUidAndTreeNodeMap = new HashMap<>();
        List<TreeNode> rootTreeNodeList = new LinkedList<>();
        this.roleRepository.findAll().forEach(itemDo -> {
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
    public TreeNode createEntityNodeForRoleHierarchy(
            Long parentUid,
            CreateEntityTreeNode createEntityTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        if (parentUid == null) {
            Integer existsDuplicate = this.roleRepository.existsByNameWithoutParent(
                    createEntityTreeNode.getName());
            if (existsDuplicate > 0) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s", RoleDo.RESOURCE_SYMBOL,
                        createEntityTreeNode.getName()));
            }
        } else {
            Integer existsDuplicate = this.roleRepository.existsByNameWithinParent(
                    createEntityTreeNode.getName(), parentUid);
            if (existsDuplicate > 0) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s", RoleDo.RESOURCE_SYMBOL,
                        createEntityTreeNode.getName()));
            }

            boolean exists = this.roleRepository.existsByUid(parentUid);
            if (!exists) {
                throw new AbcResourceNotFoundException(String.format("%s::uid=%s", RoleDo.RESOURCE_SYMBOL,
                        parentUid));
            }
        }

        //
        // Step 2, core-processing
        //
        RoleDo roleDo = new RoleDo();
        roleDo.setUid(this.idHelper.getNextDistributedId(RoleDo.RESOURCE_NAME));
        roleDo.setName(createEntityTreeNode.getName());
        roleDo.setObjectName(
                createEntityTreeNode.getName()
                        .replaceAll("_", "__")
                        .replaceAll("\\s", "_")
                        .toLowerCase());
        roleDo.setDescription(createEntityTreeNode.getDescription());
        roleDo.setDirectory(Boolean.FALSE);
        roleDo.setParentUid(parentUid);

        // entity node translation
        // N/A

        // sequence
        Float maxSequence = null;
        if (parentUid == null) {
            maxSequence = this.roleRepository.findMaxSequenceWithoutParent();
        } else {
            maxSequence = this.roleRepository.findMaxSequenceWithinParent(parentUid);
        }
        if (maxSequence == null) {
            roleDo.setSequence(0.001f);
        } else {
            roleDo.setSequence(maxSequence + 0.001f);
        }

        BaseDo.create(roleDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.roleRepository.save(roleDo);

        //
        // Step 3, post-processing
        //
        TreeNode treeNode = new TreeNode();
        treeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
        treeNode.setUid(roleDo.getUid());
        treeNode.setName(roleDo.getName());
        treeNode.setDescription(roleDo.getDescription());
        treeNode.setType(TreeNode.GENERAL_TYPE_ENTITY);

        return treeNode;
    }

    @Override
    public void updateEntityNodeOfRoleHierarchy(
            Long uid,
            UpdateEntityTreeNode updateEntityTreeNode,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        RoleDo roleDo = this.roleRepository.findByUid(uid);
        if (roleDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", RoleDo.RESOURCE_SYMBOL, uid));
        }
        if (!ObjectUtils.isEmpty(updateEntityTreeNode.getName())
                && !updateEntityTreeNode.getName().equalsIgnoreCase(roleDo.getName())) {
            // name 要改
            if (roleDo.getParentUid() == null) {
                // 没有 parent 的情况
                Integer existsDuplicate = this.roleRepository.existsByNameWithoutParent(
                        updateEntityTreeNode.getName());
                if (existsDuplicate > 0) {
                    throw new AbcResourceDuplicateException(String.format("%s::name=%s", RoleDo.RESOURCE_SYMBOL,
                            updateEntityTreeNode.getName()));
                }
            } else {
                // 有 parent 的情况
                Integer existsDuplicate = this.roleRepository.existsByNameWithinParent(
                        updateEntityTreeNode.getName(), roleDo.getParentUid());
                if (existsDuplicate > 0) {
                    throw new AbcResourceDuplicateException(String.format("%s::name=%s", RoleDo.RESOURCE_SYMBOL,
                            updateEntityTreeNode.getName()));
                }
            }
        }

        //
        // Step 2, core-processing
        //
        boolean requiredToUpdate = false;

        if (!ObjectUtils.isEmpty(updateEntityTreeNode.getName())
                && !updateEntityTreeNode.getName().equalsIgnoreCase(roleDo.getName())) {
            roleDo.setName(updateEntityTreeNode.getName());
            roleDo.setObjectName(
                    updateEntityTreeNode.getName()
                            .replaceAll("_", "__")
                            .replaceAll("\\s", "_")
                            .toLowerCase());
            requiredToUpdate = true;
        }
        if (updateEntityTreeNode.getDescription() != null
                && !updateEntityTreeNode.getDescription().equalsIgnoreCase(roleDo.getDescription())) {
            roleDo.setDescription(updateEntityTreeNode.getDescription());
            requiredToUpdate = true;
        }

        if (requiredToUpdate) {
            BaseDo.update(roleDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.roleRepository.save(roleDo);
        }

        //
        // Step 3, post-processing
        //
    }

    @Override
    public void replaceNodeRelationshipOfRoleHierarchy(
            Long uid,
            ReplaceTreeNodeRelationship replaceTreeNodeRelationship,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        RoleDo itemDo = this.roleRepository.findByUid(uid);
        if (itemDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", RoleDo.RESOURCE_SYMBOL,
                    uid));
        }

        if (replaceTreeNodeRelationship.getReferenceTreeNodeUid() == null) {
            // 调整到根节点下，作为其最末尾子节点
            Specification<RoleDo> specification = new Specification<RoleDo>() {
                @Override
                public Predicate toPredicate(Root<RoleDo> root, CriteriaQuery<?> query,
                                             CriteriaBuilder criteriaBuilder) {
                    List<Predicate> predicateList = new ArrayList<>();
                    predicateList.add(criteriaBuilder.isNull(root.get("parentUid")));
                    return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
                }
            };

            Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("sequence")));
            Page<RoleDo> candidateItemDoPage = this.roleRepository.findAll(specification, pageable);
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
            this.roleRepository.save(itemDo);

            return;
        }

        RoleDo referenceItemDo =
                this.roleRepository.findByUid(replaceTreeNodeRelationship.getReferenceTreeNodeUid());
        if (referenceItemDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", RoleDo.RESOURCE_SYMBOL,
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
                    this.roleRepository.save(itemDo);

                    referenceItemDo.setSequence(2.0f);
                    BaseDo.update(referenceItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.roleRepository.save(referenceItemDo);
                } else {
                    Specification<RoleDo> specification = new Specification<RoleDo>() {
                        @Override
                        public Predicate toPredicate(Root<RoleDo> root, CriteriaQuery<?> query,
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
                    Page<RoleDo> candidateItemDoPage = this.roleRepository.findAll(specification, pageable);
                    if (candidateItemDoPage.isEmpty()) {
                        itemDo.setSequence(referenceItemDo.getSequence() * 0.5f);
                    } else {
                        float gap =
                                referenceItemDo.getSequence() - candidateItemDoPage.getContent().get(0).getSequence();
                        itemDo.setSequence(referenceItemDo.getSequence() - gap / 2);
                    }

                    BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.roleRepository.save(itemDo);
                }
            }
            break;
            case CENTER: {
                itemDo.setParentUid(referenceItemDo.getUid());

                Specification<RoleDo> specification = new Specification<RoleDo>() {
                    @Override
                    public Predicate toPredicate(Root<RoleDo> root, CriteriaQuery<?> query,
                                                 CriteriaBuilder criteriaBuilder) {
                        List<Predicate> predicateList = new ArrayList<>();

                        predicateList.add(criteriaBuilder.equal(root.get("parentUid"), referenceItemDo.getUid()));
                        return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
                    }
                };

                Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("sequence")));
                Page<RoleDo> candidateItemDoPage = this.roleRepository.findAll(specification, pageable);
                if (candidateItemDoPage.isEmpty()) {
                    itemDo.setSequence(1.0f);
                } else {
                    itemDo.setSequence(candidateItemDoPage.getContent().get(0).getSequence() + 1.0f);
                }

                BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                this.roleRepository.save(itemDo);
            }
            break;
            case REAR: {
                itemDo.setParentUid(referenceItemDo.getParentUid());

                if (referenceItemDo.getSequence() == null) {
                    itemDo.setSequence(2.0f);
                    BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.roleRepository.save(itemDo);

                    referenceItemDo.setSequence(1.0f);
                    BaseDo.update(referenceItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.roleRepository.save(referenceItemDo);
                } else {
                    Specification<RoleDo> specification = new Specification<RoleDo>() {
                        @Override
                        public Predicate toPredicate(Root<RoleDo> root, CriteriaQuery<?> query,
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
                    Page<RoleDo> candidateItemDoPage = this.roleRepository.findAll(specification, pageable);
                    if (candidateItemDoPage.isEmpty()) {
                        itemDo.setSequence(referenceItemDo.getSequence() + 1.0f);
                    } else {
                        float gap =
                                candidateItemDoPage.getContent().get(0).getSequence() - referenceItemDo.getSequence();
                        itemDo.setSequence(referenceItemDo.getSequence() + gap / 2);
                    }

                    BaseDo.update(itemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.roleRepository.save(itemDo);
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
    public List<String> listAllReferencesToNodeOfRoleHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return null;
    }

    @Override
    public void deleteNodeOfRoleHierarchy(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        RoleDo roleDo = this.roleRepository.findByUid(uid);
        if (roleDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", RoleDo.RESOURCE_SYMBOL, uid));
        }

        //
        // Step 2, core-processing
        //
        recursiveDeleteNodeOfRoleHierarchy(roleDo, operatingUserProfile);
    }

    private void recursiveDeleteNodeOfRoleHierarchy(
            RoleDo roleDo,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        // 找出所有的子节点，删除
        List<RoleDo> children =
                this.roleRepository.findByParentUid(roleDo.getUid());
        if (!CollectionUtils.isEmpty(children)) {
            children.forEach(child -> {
                recursiveDeleteNodeOfRoleHierarchy(child, operatingUserProfile);
            });
        }

        // 删除自己
        this.roleRepository.delete(roleDo);

        //
        // Step 3, post-processing
        //
        RoleDeletedEvent roleDeletedEvent = new RoleDeletedEvent();
        roleDeletedEvent.setUid(roleDo.getUid());
        roleDeletedEvent.setOperatingUserProfile(operatingUserProfile);
        this.eventBusManager.send(roleDeletedEvent);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createOrReplaceCommonPermissionsForAllRoles(
            CreateOrReplacePermissionsDto createOrReplacePermissionsDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        LocalDateTime now = LocalDateTime.now();

        // for all roles

        // delete first
        this.rolePermissionRepository.deleteAllWithoutRole();

        // insert second
        List<RolePermissionDo> rolePermissionDoList = new LinkedList<>();
        if (!CollectionUtils.isEmpty(createOrReplacePermissionsDto.getFunctionUidList())) {
            createOrReplacePermissionsDto.getFunctionUidList().forEach(functionUid -> {

                RolePermissionDo rolePermissionDo = new RolePermissionDo();
                rolePermissionDo.setRoleUid(null);
                rolePermissionDo.setPermissionType(PermissionTypeEnum.FUNCTION);
                rolePermissionDo.setPermissionUid(functionUid);
                BaseDo.create(rolePermissionDo, operatingUserProfile.getUid(), now);

                rolePermissionDoList.add(rolePermissionDo);
            });
        }
        if (!CollectionUtils.isEmpty(createOrReplacePermissionsDto.getNavigationMenuUidList())) {
            createOrReplacePermissionsDto.getNavigationMenuUidList().forEach(navigationMenuUid -> {

                RolePermissionDo rolePermissionDo = new RolePermissionDo();
                rolePermissionDo.setRoleUid(null);
                rolePermissionDo.setPermissionType(PermissionTypeEnum.NAVIGATION_MENU);
                rolePermissionDo.setPermissionUid(navigationMenuUid);
                BaseDo.create(rolePermissionDo, operatingUserProfile.getUid(), now);

                rolePermissionDoList.add(rolePermissionDo);
            });
        }

        if (!CollectionUtils.isEmpty(rolePermissionDoList)) {
            this.rolePermissionRepository.saveAll(rolePermissionDoList);
        }
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createOrReplacePermissionsForGivenRole(
            Long uid,
            CreateOrReplacePermissionsDto createOrReplacePermissionsDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        LocalDateTime now = LocalDateTime.now();

        // for the specific role

        // delete first
        this.rolePermissionRepository.deleteByRoleUid(uid);

        // insert second
        List<RolePermissionDo> rolePermissionDoList = new LinkedList<>();
        if (!CollectionUtils.isEmpty(createOrReplacePermissionsDto.getFunctionUidList())) {
            createOrReplacePermissionsDto.getFunctionUidList().forEach(functionUid -> {
                RolePermissionDo rolePermissionDo = new RolePermissionDo();
                rolePermissionDo.setRoleUid(uid);
                rolePermissionDo.setPermissionType(PermissionTypeEnum.FUNCTION);
                rolePermissionDo.setPermissionUid(functionUid);
                BaseDo.create(rolePermissionDo, operatingUserProfile.getUid(), now);

                rolePermissionDoList.add(rolePermissionDo);
            });
        }
        if (!CollectionUtils.isEmpty(createOrReplacePermissionsDto.getNavigationMenuUidList())) {
            createOrReplacePermissionsDto.getNavigationMenuUidList().forEach(navigationMenuUid -> {
                RolePermissionDo rolePermissionDo = new RolePermissionDo();
                rolePermissionDo.setRoleUid(uid);
                rolePermissionDo.setPermissionType(PermissionTypeEnum.NAVIGATION_MENU);
                rolePermissionDo.setPermissionUid(navigationMenuUid);
                BaseDo.create(rolePermissionDo, operatingUserProfile.getUid(), now);

                rolePermissionDoList.add(rolePermissionDo);
            });
        }

        if (!CollectionUtils.isEmpty(rolePermissionDoList)) {
            this.rolePermissionRepository.saveAll(rolePermissionDoList);
        }
    }

    @Override
    public SimplePermissions getCommonPermissionsOfAllRoles(
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        List<RolePermissionDo> rolePermissionDoList =  this.rolePermissionRepository.findAllWithoutRoleUid();

        if (CollectionUtils.isEmpty(rolePermissionDoList)) {
            return null;
        }

        SimplePermissions permissions = new SimplePermissions();
        rolePermissionDoList.forEach(rolePermissionDo -> {
            switch (rolePermissionDo.getPermissionType()) {
                case FUNCTION: {
                    if (permissions.getFunctionUidList() == null) {
                        permissions.setFunctionUidList(new LinkedList<>());
                    }
                    permissions.getFunctionUidList().add(rolePermissionDo.getPermissionUid());
                }
                break;
                case NAVIGATION_MENU: {
                    if (permissions.getNavigationMenuUidList() == null) {
                        permissions.setNavigationMenuUidList(new LinkedList<>());
                    }
                    permissions.getNavigationMenuUidList().add(rolePermissionDo.getPermissionUid());
                }
                break;
                default:
                    break;
            }
        });
        return permissions;
    }


    @Override
    public SimplePermissions getPermissionsOfGivenRole(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        List<RolePermissionDo> rolePermissionDoList = this.rolePermissionRepository.findByRoleUid(uid);

        if (CollectionUtils.isEmpty(rolePermissionDoList)) {
            return null;
        }

        SimplePermissions permissions = new SimplePermissions();
        rolePermissionDoList.forEach(rolePermissionDo -> {
            switch (rolePermissionDo.getPermissionType()) {
                case FUNCTION: {
                    if (permissions.getFunctionUidList() == null) {
                        permissions.setFunctionUidList(new LinkedList<>());
                    }
                    permissions.getFunctionUidList().add(rolePermissionDo.getPermissionUid());
                }
                break;
                case NAVIGATION_MENU: {
                    if (permissions.getNavigationMenuUidList() == null) {
                        permissions.setNavigationMenuUidList(new LinkedList<>());
                    }
                    permissions.getNavigationMenuUidList().add(rolePermissionDo.getPermissionUid());
                }
                break;
                default:
                    break;
            }
        });
        return permissions;
    }
}
