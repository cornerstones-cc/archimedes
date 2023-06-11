package cc.cornerstones.biz.serve.service.impl;

import cc.cornerstones.almond.constants.InfrastructureConstants;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.TreeNode;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcStringUtils;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.app.entity.*;
import cc.cornerstones.biz.app.persistence.*;
import cc.cornerstones.biz.app.service.inf.AppDataFacetService;
import cc.cornerstones.biz.app.share.types.GrantStrategy;
import cc.cornerstones.biz.datafacet.entity.DataFacetDo;
import cc.cornerstones.biz.datafacet.persistence.DataFacetRepository;
import cc.cornerstones.biz.serve.dto.FlexibleQueryRequestDto;
import cc.cornerstones.biz.serve.service.assembly.FlexibleQueryHandler;
import cc.cornerstones.biz.serve.service.inf.ExploreDataFacetService;
import cc.cornerstones.biz.share.types.QueryContentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;

@Service
public class ExploreDataFacetServiceImpl implements ExploreDataFacetService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExploreDataFacetServiceImpl.class);

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private AppRepository appRepository;

    @Autowired
    private AppDataFacetRepository appDataFacetRepository;

    @Autowired
    private DataFacetRepository dataFacetRepository;

    @Autowired
    private AppMemberRepository appMemberRepository;

    @Autowired
    private AppMemberAccessGrantStrategyRepository appMemberAccessGrantStrategyRepository;

    @Autowired
    private AppMemberAccessGrantMemberRepository appMemberAccessGrantMemberRepository;

    @Autowired
    private FlexibleQueryHandler flexibleQueryHandler;

    @Autowired
    private AppDataFacetService appDataFacetService;

    @Override
    public List<TreeNode> treeListingAllNodesOfDataFacetHierarchyOfAllApps(
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        if (InfrastructureConstants.ROOT_USER_UID.equals(operatingUserProfile.getUid())) {
            return privilegedTreeListingAllNodesOfDataFacetHierarchyOfAllApps(operatingUserProfile);
        }

        //
        // Step 1, pre-processing
        //

        // Step 1.1, 找出当前用户是哪些 apps 的 members
        List<Long> grantedAppUidList = new LinkedList<>();
        List<AppMemberDo> appMemberDoList = this.appMemberRepository.findByUserUid(operatingUserProfile.getUid());
        if (CollectionUtils.isEmpty(appMemberDoList)) {
            return null;
        }
        appMemberDoList.forEach(appMemberDo -> {
            grantedAppUidList.add(appMemberDo.getAppUid());
        });

        // Step 1.2, 取出这些目标 (enabled & ordered) apps
        List<AppDo> grantedAppDoList = this.appRepository.findByEnabledAndUidIn(Boolean.TRUE, grantedAppUidList,
                Sort.by(Sort.Order.asc("sequence")));
        if (CollectionUtils.isEmpty(grantedAppDoList)) {
            LOGGER.error("cannot find any app by uid {}", AbcStringUtils.toString(grantedAppUidList, ","));
            return null;
        }
        Map<Long, GrantStrategy> appGrantStrategyMap = new HashMap<>();
        grantedAppDoList.forEach(appDo -> {
            AppMemberAccessGrantStrategyDo appMemberAccessGrantStrategyDo =
                    this.appMemberAccessGrantStrategyRepository.findByAppUid(appDo.getUid());
            if (appMemberAccessGrantStrategyDo == null
                    || appMemberAccessGrantStrategyDo.getGrantStrategy() == null) {
                GrantStrategy tempGrantStrategy = new GrantStrategy();
                tempGrantStrategy.setEnabledEntireGrant(Boolean.TRUE);
                tempGrantStrategy.setEnabledGranularGrant(Boolean.FALSE);
                appGrantStrategyMap.put(appDo.getUid(), tempGrantStrategy);
            } else {
                appGrantStrategyMap.put(appDo.getUid(),
                        appMemberAccessGrantStrategyDo.getGrantStrategy());
            }
        });

        // Step 1.3, 找出当前用户在每个目标 app 中被授权访问（此地不区分具体哪种访问模式）的 data facet hierarchy nodes
        Map<Long, List<Long>> grantedAppUidAndGrantedDataFacetHierarchyNodeUidListMap = new HashMap<>();

        appGrantStrategyMap.forEach((appUid, grantStrategy) -> {
            if (Boolean.TRUE.equals(grantStrategy.getEnabledEntireGrant())) {
                this.appDataFacetRepository.findAllByAppUid(appUid).forEach(appDataFacetDo -> {
                    if (!grantedAppUidAndGrantedDataFacetHierarchyNodeUidListMap.containsKey(appUid)) {
                        grantedAppUidAndGrantedDataFacetHierarchyNodeUidListMap.put(appUid,
                                new LinkedList<>());
                    }
                    grantedAppUidAndGrantedDataFacetHierarchyNodeUidListMap.get(appUid)
                            .add(appDataFacetDo.getUid());
                });
            } else if (Boolean.TRUE.equals(grantStrategy.getEnabledGranularGrant())) {
                List<AppMemberAccessGrantMemberDo> appMemberAccessGrantMemberDoList =
                        this.appMemberAccessGrantMemberRepository.findByAppUidAndUserUid(appUid,
                                operatingUserProfile.getUid());
                if (CollectionUtils.isEmpty(appMemberAccessGrantMemberDoList)) {
                    return;
                }

                appMemberAccessGrantMemberDoList.forEach(appMemberAccessGrantMemberDo -> {
                    if (!grantedAppUidAndGrantedDataFacetHierarchyNodeUidListMap.containsKey(appMemberAccessGrantMemberDo.getAppUid())) {
                        grantedAppUidAndGrantedDataFacetHierarchyNodeUidListMap.put(appMemberAccessGrantMemberDo.getAppUid(),
                                new LinkedList<>());
                    }
                    grantedAppUidAndGrantedDataFacetHierarchyNodeUidListMap.get(appMemberAccessGrantMemberDo.getAppUid())
                            .add(appMemberAccessGrantMemberDo.getDataFacetHierarchyNodeUid());
                });
            }
        });

        //
        // Step 2, core-processing
        //
        List<TreeNode> result = new LinkedList<>();
        grantedAppDoList.forEach(appDo -> {
            TreeNode treeNode = buildAppTreeNode(
                    appDo,
                    grantedAppUidAndGrantedDataFacetHierarchyNodeUidListMap.get(appDo.getUid()));
            result.add(treeNode);
        });

        //
        // Step 3, post-processing
        //
        return result;
    }

    private List<TreeNode> privilegedTreeListingAllNodesOfDataFacetHierarchyOfAllApps(
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        // Step 1.1, 取出 apps
        List<AppDo> appDoList = new LinkedList<>();
        this.appRepository.findAll(Sort.by(Sort.Order.asc("sequence"))).forEach(appDo -> {
            if (Boolean.TRUE.equals(appDo.getEnabled())) {
                appDoList.add(appDo);
            }
        });
        if (CollectionUtils.isEmpty(appDoList)) {
            LOGGER.warn("cannot find any app");
            return null;
        }

        //
        // Step 2, core-processing
        //
        List<TreeNode> result = new LinkedList<>();
        appDoList.forEach(appDo -> {
            TreeNode treeNode = new TreeNode();
            treeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
            treeNode.setUid(appDo.getUid());
            treeNode.setName(appDo.getName());
            treeNode.setDescription(appDo.getDescription());
            treeNode.setType(TreeNode.GENERAL_TYPE_DIRECTORY);

            List<TreeNode> children =
                    this.appDataFacetService.treeListingAllNodesOfDataFacetHierarchyOfOneApp(appDo.getUid(), operatingUserProfile);
            treeNode.setChildren(children);

            result.add(treeNode);
        });

        //
        // Step 3, post-processing
        //
        return result;
    }

    private TreeNode buildAppTreeNode(
            AppDo appDo,
            List<Long> dataFacetHierarchyNodeUidList) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        TreeNode rootTreeNode = new TreeNode();
        rootTreeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
        rootTreeNode.setUid(appDo.getUid());
        rootTreeNode.setName(appDo.getName());
        rootTreeNode.setDescription(appDo.getDescription());
        rootTreeNode.setType(TreeNode.GENERAL_TYPE_DIRECTORY);

        if (CollectionUtils.isEmpty(dataFacetHierarchyNodeUidList)) {
            return rootTreeNode;
        }

        // 直接相关的 data facet hierarchy node uid(s)，注意不是 data facet uid
        List<AppDataFacetDo> directRelatedAppDataFacetDoList = this.appDataFacetRepository.findByAppUidAndUidIn(
                appDo.getUid(),
                dataFacetHierarchyNodeUidList);
        if (CollectionUtils.isEmpty(directRelatedAppDataFacetDoList)) {
            LOGGER.error("found mismatched data, cannot find app data facet by uid {}",
                    AbcStringUtils.toString(dataFacetHierarchyNodeUidList, ","));
            return rootTreeNode;
        }

        // 收集所有直接相关和间接相关（e.g., 子节点）的 data facet uid，总称为 objective data facet uid，注意不是 data facet hierarchy node uid
        List<Long> objectiveDataFacetUidList = new LinkedList<>();

        // 收集直接相关的 data facet uid，注意不是 data facet hierarchy node uid
        directRelatedAppDataFacetDoList.forEach(appDataFacetDo -> {
            if (appDataFacetDo.getDataFacetUid() != null) {
                objectiveDataFacetUidList.add(appDataFacetDo.getDataFacetUid());
            }
        });

        Map<Long, DataFacetDo> objectiveDataFacetDoMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(objectiveDataFacetUidList)) {
            List<DataFacetDo> objectiveDataFacetDoList = this.dataFacetRepository.findByUidIn(objectiveDataFacetUidList);
            if (CollectionUtils.isEmpty(objectiveDataFacetDoList)) {
                LOGGER.error("found mismatched data, cannot find any data facet by data facet uid {}",
                        AbcStringUtils.toString(objectiveDataFacetUidList, ","));
                return rootTreeNode;
            }

            objectiveDataFacetDoList.forEach(dataFacetDo -> {
                objectiveDataFacetDoMap.put(dataFacetDo.getUid(), dataFacetDo);
            });
        }

        // 收集所有节点
        Map<Long, TreeNode> allTreeNodeMap = new HashMap<>();

        //
        // Step 2, core-processing
        //

        //
        // Step 2.1, 先为每个'直接'目标 app data facet 建立一个 tree node
        //
        directRelatedAppDataFacetDoList.forEach(appDataFacetDo -> {
            TreeNode treeNode = new TreeNode();
            treeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));

            if (Boolean.TRUE.equals(appDataFacetDo.getDirectory())) {
                treeNode.setType(TreeNode.GENERAL_TYPE_DIRECTORY);
                treeNode.setUid(appDataFacetDo.getUid());
                treeNode.setName(appDataFacetDo.getName());
                treeNode.setDescription(appDataFacetDo.getDescription());
            } else {
                treeNode.setType(TreeNode.GENERAL_TYPE_ENTITY);
                treeNode.setUid(appDataFacetDo.getUid());

                if (appDataFacetDo.getDataFacetUid() == null) {
                    LOGGER.warn("found mismatched data, entity app data facet {} without data facet uid",
                            appDataFacetDo.getUid());

                    treeNode.setName("EXCEPTIONAL-" + appDataFacetDo.getUid());
                    treeNode.setDescription(appDataFacetDo.getDescription());
                } else {
                    if (objectiveDataFacetDoMap.containsKey(appDataFacetDo.getDataFacetUid())) {
                        treeNode.setName(objectiveDataFacetDoMap.get(appDataFacetDo.getDataFacetUid()).getName());
                        treeNode.setDescription(objectiveDataFacetDoMap.get(appDataFacetDo.getDataFacetUid()).getDescription());
                    } else {
                        DataFacetDo dataFacetDo =
                                this.dataFacetRepository.findByUid(appDataFacetDo.getDataFacetUid());
                        if (dataFacetDo == null) {
                            LOGGER.warn("found mismatched data, cannot find data facet {}",
                                    appDataFacetDo.getDataFacetUid());

                            treeNode.setName("EXCEPTIONAL-" + appDataFacetDo.getUid());
                            treeNode.setDescription(appDataFacetDo.getDescription());
                        } else {
                            treeNode.setName(dataFacetDo.getName());
                            treeNode.setDescription(dataFacetDo.getDescription());

                            // additional data facet
                            objectiveDataFacetDoMap.put(dataFacetDo.getUid(), dataFacetDo);
                        }
                    }
                }
            }

            treeNode.setTags(new HashMap<>());
            treeNode.getTags().put("parent_uid", appDataFacetDo.getParentUid());
            treeNode.getTags().put("sequence", appDataFacetDo.getSequence());
            treeNode.getTags().put("directory", appDataFacetDo.getDirectory());
            treeNode.getTags().put("data_facet_uid", appDataFacetDo.getDataFacetUid());

            allTreeNodeMap.put(appDataFacetDo.getUid(), treeNode);
        });

        //
        // Step 2.2, 再为每个'直接'目标 app data facet 递归补齐 parent tree node，这是'间接'目标
        //
        directRelatedAppDataFacetDoList.forEach(appDataFacetDo -> {
            TreeNode treeNode = allTreeNodeMap.get(appDataFacetDo.getUid());
            recursivelyBuildParentTreeNode(treeNode, allTreeNodeMap, rootTreeNode, objectiveDataFacetDoMap);
        });

        //
        // Step 2.3, 再为每个'直接'目标 app data facet 递归补齐 child tree node，这是'间接'目标
        //
        directRelatedAppDataFacetDoList.forEach(appDataFacetDo -> {
            TreeNode treeNode = allTreeNodeMap.get(appDataFacetDo.getUid());
            recursivelyBuildChildTreeNode(treeNode, allTreeNodeMap, objectiveDataFacetDoMap);
        });

        //
        // Step 2.4, 再针对'直接'和'间接'目标排序
        //
        recursivelySortingTreeNodes(rootTreeNode.getChildren());

        return rootTreeNode;
    }

    private void recursivelyBuildParentTreeNode(
            TreeNode treeNode,
            Map<Long, TreeNode> allTreeNodeMap,
            TreeNode rootTreeNode,
            Map<Long, DataFacetDo> objectiveDataFacetDoMap) throws AbcUndefinedException {
        Object parentUidAsObject = treeNode.getTags().get("parent_uid");

        // 找不到父节点，自己是第一级节点
        if (parentUidAsObject == null) {
            if (rootTreeNode.getChildren() == null) {
                // 第一次遇到这个第一级节点
                rootTreeNode.setChildren(new LinkedList<>());
                rootTreeNode.getChildren().add(treeNode);
            } else {
                // 不一定是第一次遇到这个第一级节点
                // 需要找
                boolean exists = false;
                for (TreeNode childTreeNode : rootTreeNode.getChildren()) {
                    if (childTreeNode.getUid().equals(treeNode.getUid())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    rootTreeNode.getChildren().add(treeNode);
                }
            }
            return;
        }

        // 找到了父节点，处理自己与父节点关系
        Long parentUid = (Long) parentUidAsObject;
        if (allTreeNodeMap.containsKey(parentUid)) {
            TreeNode parentTreeNode = allTreeNodeMap.get(parentUid);
            if (parentTreeNode.getChildren() == null) {
                parentTreeNode.setChildren(new LinkedList<>());
                parentTreeNode.getChildren().add(treeNode);
            } else {
                boolean exists = false;
                for (TreeNode childTreeNode : parentTreeNode.getChildren()) {
                    if (childTreeNode.getUid().equals(treeNode.getUid())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    parentTreeNode.getChildren().add(treeNode);
                }
            }

            // recursive
            recursivelyBuildParentTreeNode(parentTreeNode, allTreeNodeMap, rootTreeNode, objectiveDataFacetDoMap);
        } else {
            AppDataFacetDo parentAppDataFacetDo = this.appDataFacetRepository.findByUid(parentUid);
            if (parentAppDataFacetDo == null) {
                LOGGER.error("found mismatched data, cannot find app data facet by uid {}", parentUid);
                return;
            }
            TreeNode parentTreeNode = new TreeNode();
            parentTreeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
            parentTreeNode.setUid(parentAppDataFacetDo.getUid());

            if (Boolean.TRUE.equals(parentAppDataFacetDo.getDirectory())) {
                parentTreeNode.setType(TreeNode.GENERAL_TYPE_DIRECTORY);

                parentTreeNode.setName(parentAppDataFacetDo.getName());
                parentTreeNode.setDescription(parentAppDataFacetDo.getDescription());
            } else {
                parentTreeNode.setType(TreeNode.GENERAL_TYPE_ENTITY);

                if (parentAppDataFacetDo.getDataFacetUid() == null) {
                    LOGGER.warn("found mismatched data, entity app data facet {} without data facet uid",
                            parentAppDataFacetDo.getUid());

                    parentTreeNode.setName("EXCEPTIONAL-" + parentAppDataFacetDo.getUid());
                    parentTreeNode.setDescription(parentAppDataFacetDo.getDescription());
                } else {
                    if (objectiveDataFacetDoMap.containsKey(parentAppDataFacetDo.getDataFacetUid())) {
                        parentTreeNode.setName(objectiveDataFacetDoMap.get(parentAppDataFacetDo.getDataFacetUid()).getName());
                        parentTreeNode.setDescription(objectiveDataFacetDoMap.get(parentAppDataFacetDo.getDataFacetUid()).getDescription());
                    } else {
                        DataFacetDo dataFacetDo =
                                this.dataFacetRepository.findByUid(parentAppDataFacetDo.getDataFacetUid());
                        if (dataFacetDo == null) {
                            LOGGER.warn("found mismatched data, cannot find data facet {}",
                                    parentAppDataFacetDo.getDataFacetUid());

                            parentTreeNode.setName("EXCEPTIONAL-" + parentAppDataFacetDo.getUid());
                            parentTreeNode.setDescription(parentAppDataFacetDo.getDescription());
                        } else {
                            parentTreeNode.setName(dataFacetDo.getName());
                            parentTreeNode.setDescription(dataFacetDo.getDescription());

                            // additional data facet
                            objectiveDataFacetDoMap.put(dataFacetDo.getUid(), dataFacetDo);
                        }
                    }
                }
            }
            parentTreeNode.setTags(new HashMap<>());
            parentTreeNode.getTags().put("parent_uid", parentAppDataFacetDo.getParentUid());
            parentTreeNode.getTags().put("sequence", parentAppDataFacetDo.getSequence());
            parentTreeNode.getTags().put("directory", parentAppDataFacetDo.getDirectory());
            parentTreeNode.getTags().put("data_facet_uid", parentAppDataFacetDo.getDataFacetUid());

            allTreeNodeMap.put(parentAppDataFacetDo.getUid(), parentTreeNode);

            parentTreeNode.setChildren(new LinkedList<>());
            parentTreeNode.getChildren().add(treeNode);

            // recursive
            recursivelyBuildParentTreeNode(parentTreeNode, allTreeNodeMap, rootTreeNode, objectiveDataFacetDoMap);
        }
    }

    private void recursivelyBuildChildTreeNode(
            TreeNode treeNode,
            Map<Long, TreeNode> allTreeNodeMap,
            Map<Long, DataFacetDo> objectiveDataFacetDoMap) throws AbcUndefinedException {
        Object directoryAsObject = treeNode.getTags().get("directory");
        if (directoryAsObject == null) {
            return;
        }

        List<AppDataFacetDo> childAppDataFacetDoList = this.appDataFacetRepository.findByParentUid(treeNode.getUid());
        if (CollectionUtils.isEmpty(childAppDataFacetDoList)) {
            return;
        }

        childAppDataFacetDoList.forEach(childAppDataFacetDo -> {
            TreeNode childTreeNode = null;
            if (!CollectionUtils.isEmpty(treeNode.getChildren())) {
                for (TreeNode existingChild : treeNode.getChildren()) {
                    if (existingChild.getUid().equals(childAppDataFacetDo.getUid())) {
                        childTreeNode = existingChild;
                        break;
                    }
                }
            }

            if (childTreeNode != null) {
                // recursive
                recursivelyBuildChildTreeNode(childTreeNode, allTreeNodeMap, objectiveDataFacetDoMap);
                return;
            }

            if (allTreeNodeMap.containsKey(childAppDataFacetDo.getUid())) {
                childTreeNode = allTreeNodeMap.get(childAppDataFacetDo.getUid());
                if (treeNode.getChildren() == null) {
                    treeNode.setChildren(new LinkedList<>());
                    treeNode.getChildren().add(childTreeNode);
                } else {
                    boolean exists = false;
                    for (TreeNode existingChild : treeNode.getChildren()) {
                        if (existingChild.getUid().equals(childTreeNode.getUid())) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        treeNode.getChildren().add(childTreeNode);
                    }
                }

                // recursive
                recursivelyBuildChildTreeNode(childTreeNode, allTreeNodeMap, objectiveDataFacetDoMap);
            } else {
                childTreeNode = new TreeNode();
                childTreeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
                childTreeNode.setUid(childAppDataFacetDo.getUid());

                if (Boolean.TRUE.equals(childAppDataFacetDo.getDirectory())) {
                    childTreeNode.setType(TreeNode.GENERAL_TYPE_DIRECTORY);

                    childTreeNode.setName(childAppDataFacetDo.getName());
                    childTreeNode.setDescription(childAppDataFacetDo.getDescription());
                } else {
                    childTreeNode.setType(TreeNode.GENERAL_TYPE_ENTITY);

                    if (childAppDataFacetDo.getDataFacetUid() == null) {
                        LOGGER.warn("found mismatched data, entity app data facet {} without data facet uid",
                                childAppDataFacetDo.getDataFacetUid());

                        childTreeNode.setName("EXCEPTIONAL-" + childAppDataFacetDo.getUid());
                        childTreeNode.setDescription(childAppDataFacetDo.getDescription());
                    } else {
                        if (objectiveDataFacetDoMap.containsKey(childAppDataFacetDo.getDataFacetUid())) {
                            childTreeNode.setName(objectiveDataFacetDoMap.get(childAppDataFacetDo.getDataFacetUid()).getName());
                            childTreeNode.setDescription(objectiveDataFacetDoMap.get(childAppDataFacetDo.getDataFacetUid()).getDescription());
                        } else {
                            DataFacetDo dataFacetDo =
                                    this.dataFacetRepository.findByUid(childAppDataFacetDo.getDataFacetUid());
                            if (dataFacetDo == null) {
                                LOGGER.warn("found mismatched data, cannot find data facet {}",
                                        childAppDataFacetDo.getDataFacetUid());

                                childTreeNode.setName("EXCEPTIONAL-" + childAppDataFacetDo.getUid());
                                childTreeNode.setDescription(childAppDataFacetDo.getDescription());
                            } else {
                                childTreeNode.setName(dataFacetDo.getName());
                                childTreeNode.setDescription(dataFacetDo.getDescription());

                                // additional data facet
                                objectiveDataFacetDoMap.put(dataFacetDo.getUid(), dataFacetDo);
                            }
                        }
                    }
                }
                childTreeNode.setTags(new HashMap<>());
                childTreeNode.getTags().put("parent_uid", childAppDataFacetDo.getParentUid());
                childTreeNode.getTags().put("sequence", childAppDataFacetDo.getSequence());
                childTreeNode.getTags().put("directory", childAppDataFacetDo.getDirectory());
                childTreeNode.getTags().put("data_facet_uid", childAppDataFacetDo.getDataFacetUid());

                allTreeNodeMap.put(childAppDataFacetDo.getUid(), childTreeNode);

                if (treeNode.getChildren() == null) {
                    treeNode.setChildren(new LinkedList<>());
                }
                treeNode.getChildren().add(childTreeNode);

                // recursive
                recursivelyBuildChildTreeNode(childTreeNode, allTreeNodeMap, objectiveDataFacetDoMap);
            }
        });
    }

    private void recursivelySortingTreeNodes(
            List<TreeNode> treeNodeList) {
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

        for (TreeNode treeNode : treeNodeList) {
            if (!CollectionUtils.isEmpty(treeNode.getChildren()) && treeNode.getChildren().size() > 1) {
                recursivelySortingTreeNodes(treeNode.getChildren());
            }
        }
    }

    @Override
    public QueryContentResult flexibleQuery(
            Long dataFacetUid,
            FlexibleQueryRequestDto flexibleQueryRequestDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return this.flexibleQueryHandler.execute(dataFacetUid, flexibleQueryRequestDto, operatingUserProfile);
    }

    @Override
    public List<TreeNode> treeListingAllNodesOfDataFacetHierarchyOfApp(
            Long appUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return null;
    }
}
