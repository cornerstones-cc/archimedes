package cc.cornerstones.biz.app.service.impl;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserBriefInformation;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.administration.usermanagement.entity.*;
import cc.cornerstones.biz.administration.usermanagement.persistence.*;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.administration.usermanagement.share.types.Group;
import cc.cornerstones.biz.administration.usermanagement.share.types.Role;
import cc.cornerstones.biz.app.dto.*;
import cc.cornerstones.biz.app.entity.*;
import cc.cornerstones.biz.app.persistence.*;
import cc.cornerstones.biz.app.service.assembly.AppAccessHandler;
import cc.cornerstones.biz.app.service.inf.AppMemberAccessService;
import cc.cornerstones.biz.app.share.types.GrantStrategy;
import cc.cornerstones.biz.share.event.*;
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
import org.w3c.dom.html.HTMLLinkElement;

import javax.annotation.PostConstruct;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AppMemberAccessServiceImpl implements AppMemberAccessService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppMemberAccessServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private AppMemberAccessGrantStrategyRepository appMemberAccessGrantStrategyRepository;

    @Autowired
    private AppMemberAccessGrantMemberRepository appMemberAccessGrantMemberRepository;

    @Autowired
    private UserSchemaExtendedPropertyRepository userSchemaExtendedPropertyRepository;

    @Autowired
    private UserExtendedPropertyRepository userExtendedPropertyRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private AccountTypeRepository accountTypeRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private AppMemberRepository appMemberRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AppAccessHandler appAccessHandler;

    @Override
    public AppMemberAccessGrantStrategyDto getGrantStrategyOfApp(
            Long appUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        AppMemberAccessGrantStrategyDo appMemberAccessGrantStrategyDo =
                this.appMemberAccessGrantStrategyRepository.findByAppUid(appUid);
        if (appMemberAccessGrantStrategyDo == null) {
            return null;
        }

        //
        // Step 2, core-processing
        //

        //
        // Step 3, post-processing
        //
        AppMemberAccessGrantStrategyDto appMemberAccessGrantStrategyDto = new AppMemberAccessGrantStrategyDto();
        BeanUtils.copyProperties(appMemberAccessGrantStrategyDo, appMemberAccessGrantStrategyDto);
        appMemberAccessGrantStrategyDto.setEnabledEntireGrant(appMemberAccessGrantStrategyDo.getGrantStrategy().getEnabledEntireGrant());
        appMemberAccessGrantStrategyDto.setEnabledGranularGrant(appMemberAccessGrantStrategyDo.getGrantStrategy().getEnabledGranularGrant());
        return appMemberAccessGrantStrategyDto;
    }

    @Override
    public AppMemberAccessGrantStrategyDto createOrReplaceGrantStrategyForApp(
            Long appUid,
            CreateOrReplaceAppGrantStrategyDto createOrReplaceAppGrantStrategyDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        // verify authorization
        this.appAccessHandler.verifyWriteAuthorization(appUid, operatingUserProfile);

        //
        // Step 2, core-processing
        //

        AppMemberAccessGrantStrategyDo appMemberAccessGrantStrategyDo =
                this.appMemberAccessGrantStrategyRepository.findByAppUid(appUid);

        if (appMemberAccessGrantStrategyDo == null) {
            appMemberAccessGrantStrategyDo = new AppMemberAccessGrantStrategyDo();
            appMemberAccessGrantStrategyDo.setAppUid(appUid);

            GrantStrategy grantStrategy = new GrantStrategy();
            grantStrategy.setEnabledEntireGrant(createOrReplaceAppGrantStrategyDto.getEnabledEntireGrant());
            grantStrategy.setEnabledGranularGrant(createOrReplaceAppGrantStrategyDto.getEnabledGranularGrant());
            appMemberAccessGrantStrategyDo.setGrantStrategy(grantStrategy);

            BaseDo.create(appMemberAccessGrantStrategyDo, operatingUserProfile.getUid(), LocalDateTime.now());
        } else {
            GrantStrategy grantStrategy = new GrantStrategy();
            grantStrategy.setEnabledEntireGrant(createOrReplaceAppGrantStrategyDto.getEnabledEntireGrant());
            grantStrategy.setEnabledGranularGrant(createOrReplaceAppGrantStrategyDto.getEnabledGranularGrant());
            appMemberAccessGrantStrategyDo.setGrantStrategy(grantStrategy);

            BaseDo.update(appMemberAccessGrantStrategyDo, operatingUserProfile.getUid(), LocalDateTime.now());
        }

        this.appMemberAccessGrantStrategyRepository.save(appMemberAccessGrantStrategyDo);

        //
        // Step 3, post-processing
        //
        AppMemberAccessGrantStrategyDto appMemberAccessGrantStrategyDto = new AppMemberAccessGrantStrategyDto();
        BeanUtils.copyProperties(appMemberAccessGrantStrategyDo, appMemberAccessGrantStrategyDto);
        appMemberAccessGrantStrategyDto.setEnabledEntireGrant(appMemberAccessGrantStrategyDo.getGrantStrategy().getEnabledEntireGrant());
        appMemberAccessGrantStrategyDto.setEnabledGranularGrant(appMemberAccessGrantStrategyDo.getGrantStrategy().getEnabledGranularGrant());
        return appMemberAccessGrantStrategyDto;
    }

    public List<AppMemberDo> listingQueryMembersOfApp(
            Long appUid,
            List<Long> userRoleUidList,
            List<Long> userGroupUidList,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        //
        // Step 2, core-processing
        //

        //
        // Step 1, pre-processing
        //
        List<Long> userUidList = new LinkedList<>();
        if (!CollectionUtils.isEmpty(userRoleUidList)) {
            List<UserRoleDo> userRoleDoList = this.userRoleRepository.findByRoleUidIn(userRoleUidList);
            if (!CollectionUtils.isEmpty(userRoleDoList)) {
                userRoleDoList.forEach(userRoleDo -> {
                    if (!userUidList.contains(userRoleDo.getUserUid())) {
                        userUidList.add(userRoleDo.getUserUid());
                    }
                });
            }
        }
        if (!CollectionUtils.isEmpty(userGroupUidList)) {
            List<UserGroupDo> userGroupDoList = this.userGroupRepository.findByGroupUidIn(userGroupUidList);
            if (!CollectionUtils.isEmpty(userGroupDoList)) {
                userGroupDoList.forEach(userGroupDo -> {
                    if (!userUidList.contains(userGroupDo.getUserUid())) {
                        userUidList.add(userGroupDo.getUserUid());
                    }
                });
            }
        }


        //
        // Step 2, core-processing
        //
        Specification<AppMemberDo> specification = new Specification<AppMemberDo>() {
            @Override
            public Predicate toPredicate(Root<AppMemberDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (appUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("appUid"), appUid));
                }

                if (!CollectionUtils.isEmpty(userUidList)) {
                    CriteriaBuilder.In<Long> in = criteriaBuilder.in(root.get("userUid"));
                    userUidList.forEach(item -> {
                        in.value(item);
                    });
                    predicateList.add(in);
                }

                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        List<AppMemberDo> itemDoList = this.appMemberRepository.findAll(specification, sort);

        //
        // Step 3, post-processing
        //

        return itemDoList;
    }

    @Override
    public Page<AppMemberUserAccessGrantDto> pagingQueryMemberGrantsOfApp(
            Long appUid,
            List<Long> dataFacetHierarchyNodeUidList,
            List<Long> userUidListOfUser,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        //
        // Step 2, core-processing
        //
        Specification<AppMemberAccessGrantMemberDo> specification = new Specification<AppMemberAccessGrantMemberDo>() {
            @Override
            public Predicate toPredicate(Root<AppMemberAccessGrantMemberDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (appUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("appUid"), appUid));
                }
                if (!CollectionUtils.isEmpty(dataFacetHierarchyNodeUidList)) {
                    CriteriaBuilder.In<Long> in = criteriaBuilder.in(root.get("dataFacetHierarchyNodeUid"));
                    dataFacetHierarchyNodeUidList.forEach(item -> {
                        in.value(item);
                    });
                    predicateList.add(in);
                }
                if (!CollectionUtils.isEmpty(userUidListOfUser)) {
                    CriteriaBuilder.In<Long> in = criteriaBuilder.in(root.get("userUid"));
                    userUidListOfUser.forEach(item -> {
                        in.value(item);
                    });
                    predicateList.add(in);
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        Sort sort = Sort.by(Sort.Order.asc("userUid"));
        Iterable<AppMemberAccessGrantMemberDo> itemDoIterable =
                this.appMemberAccessGrantMemberRepository.findAll(specification, sort);

        //
        // Step 3, post-processing
        //

        //
        // Step 3.1, 为 uid (user) 补充 user brief information
        //
        List<Long> userUidList = new LinkedList<>();
        itemDoIterable.forEach(itemDo -> {
            if (itemDo.getUserUid() != null && !userUidList.contains(itemDo.getUserUid())) {
                userUidList.add(itemDo.getUserUid());
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
        // Step 3.2, 构造返回内容
        //
        if (!itemDoIterable.iterator().hasNext()) {
            Page<AppMemberUserAccessGrantDto> itemDtoPage = new PageImpl<AppMemberUserAccessGrantDto>(
                    new ArrayList<>(), pageable, 0);
            return itemDtoPage;
        }

        List<Long> queryUserUidList = new LinkedList<>();
        List<AppMemberUserAccessGrantDto> itemDtoList = new LinkedList<>();

        // by app by user 组织内容
        Map<Long, Map<Long, AppMemberUserAccessGrantDto>> aggregation = new HashMap<>();
        itemDoIterable.forEach(itemDo -> {
            // app
            if (!aggregation.containsKey(itemDo.getAppUid())) {
                aggregation.put(itemDo.getAppUid(), new HashMap<>());
            }

            // user
            AppMemberUserAccessGrantDto itemDto = aggregation.get(itemDo.getAppUid()).get(itemDo.getUserUid());
            if (itemDto == null) {
                itemDto = new AppMemberUserAccessGrantDto();
                itemDto.setUser(userBriefInformationMap.get(itemDo.getUserUid()));
                itemDto.setAppUid(itemDo.getAppUid());
                itemDto.setDataFacetHierarchyNodeUidList(new LinkedList<>());
                itemDto.getDataFacetHierarchyNodeUidList().add(itemDo.getDataFacetHierarchyNodeUid());

                aggregation.get(itemDo.getAppUid()).put(itemDo.getUserUid(), itemDto);

                itemDtoList.add(itemDto);
            } else {
                itemDto.getDataFacetHierarchyNodeUidList().add(itemDo.getDataFacetHierarchyNodeUid());
            }

            if (!queryUserUidList.contains(itemDo.getUserUid())) {
                queryUserUidList.add(itemDo.getUserUid());
            }
        });

        int pageNumber = pageable.getPageNumber();
        if (pageNumber < 0) {
            pageNumber = 0;
        }
        int pageSize = pageable.getPageSize();
        if (pageSize <= 0) {
            pageSize = 20;
        }
        int inclusiveFromIndex = pageNumber * pageSize;
        if (inclusiveFromIndex >= itemDtoList.size()) {
            // illegal
            Page<AppMemberUserAccessGrantDto> itemDtoPage = new PageImpl<AppMemberUserAccessGrantDto>(
                    new ArrayList<>(0),
                    pageable,
                    itemDtoList.size());
            return itemDtoPage;
        }
        int exclusiveToIndex = (pageNumber + 1) * pageSize;
        if (exclusiveToIndex > itemDtoList.size()) {
            exclusiveToIndex = itemDtoList.size();
        }
        List<AppMemberUserAccessGrantDto> objectiveItemDtoList =
                itemDtoList.subList(inclusiveFromIndex, exclusiveToIndex);
        Page<AppMemberUserAccessGrantDto> itemDtoPage = new PageImpl<AppMemberUserAccessGrantDto>(
                objectiveItemDtoList,
                pageable,
                itemDtoList.size());

        // 补齐 role
        Map<Long, RoleDo> roleDoMap = new HashMap<>();
        this.roleRepository.findAll().forEach(roleDo -> {
            roleDoMap.put(roleDo.getUid(), roleDo);
        });

        Map<Long, List<Role>> userUidAndRoleListMap = new HashMap<>();
        List<UserRoleDo> userRoleDoList = this.userRoleRepository.findByUserUidIn(queryUserUidList);
        if (!CollectionUtils.isEmpty(userRoleDoList)) {
            userRoleDoList.forEach(userRoleDo -> {
                if (!userUidAndRoleListMap.containsKey(userRoleDo.getUserUid())) {
                    userUidAndRoleListMap.put(userRoleDo.getUserUid(), new LinkedList<>());
                }

                if (roleDoMap.containsKey(userRoleDo.getRoleUid())) {
                    Role role = new Role();
                    role.setUid(userRoleDo.getRoleUid());
                    role.setName(roleDoMap.get(userRoleDo.getRoleUid()).getName());
                    // TODO 补全 full name 可以提升用户体验
                    role.setFullName(role.getName());
                    userUidAndRoleListMap.get(userRoleDo.getUserUid()).add(role);
                }
            });
        }

        // 补齐 group
        Map<Long, GroupDo> groupDoMap = new HashMap<>();
        this.groupRepository.findAll().forEach(groupDo -> {
            groupDoMap.put(groupDo.getUid(), groupDo);
        });

        Map<Long, List<Group>> userUidAndGroupListMap = new HashMap<>();
        List<UserGroupDo> userGroupDoList = this.userGroupRepository.findByUserUidIn(queryUserUidList);
        if (!CollectionUtils.isEmpty(userGroupDoList)) {
            userGroupDoList.forEach(userGroupDo -> {
                if (!userUidAndGroupListMap.containsKey(userGroupDo.getUserUid())) {
                    userUidAndGroupListMap.put(userGroupDo.getUserUid(), new LinkedList<>());
                }

                if (groupDoMap.containsKey(userGroupDo.getGroupUid())) {
                    Group group = new Group();
                    group.setUid(userGroupDo.getGroupUid());
                    group.setName(groupDoMap.get(userGroupDo.getGroupUid()).getName());
                    // TODO 补全 full name 可以提升用户体验
                    group.setFullName(group.getName());
                    userUidAndGroupListMap.get(userGroupDo.getUserUid()).add(group);
                }
            });
        }

        // 完善 user
        objectiveItemDtoList.forEach(itemDto -> {
            if (itemDto.getUser() != null) {
                itemDto.setRoleList(userUidAndRoleListMap.get(itemDto.getUser().getUid()));
                itemDto.setGroupList(userUidAndGroupListMap.get(itemDto.getUser().getUid()));
            }
        });

        // 返回
        return itemDtoPage;
    }

    @Override
    public AppGrantDto getMemberGrantOfApp(
            Long appUid,
            Long userUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        List<AppMemberAccessGrantMemberDo> appMemberAccessGrantMemberDoList =
                this.appMemberAccessGrantMemberRepository.findByAppUidAndUserUid(appUid,
                        userUid);
        if (CollectionUtils.isEmpty(appMemberAccessGrantMemberDoList)) {
            return null;
        }

        AppGrantDto appGrantDto = new AppGrantDto();
        appGrantDto.setDataFacetHierarchyNodeUidList(new LinkedList<>());
        appMemberAccessGrantMemberDoList.forEach(appMemberAccessGrantMemberDo -> {
            appGrantDto.getDataFacetHierarchyNodeUidList().add(appMemberAccessGrantMemberDo.getDataFacetHierarchyNodeUid());
        });

        return appGrantDto;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void bulkGrantsByMemberForApp(
            Long appUid,
            BulkGrantsByMemberDto bulkGrantsByMemberDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        // verify authorization
        this.appAccessHandler.verifyWriteAuthorization(appUid, operatingUserProfile);

        //
        // Step 2, core-processing
        //

        bulkGrantsByMemberDto.getUserUidList().forEach(userUid -> {
            this.appMemberAccessGrantMemberRepository.deleteByAppUidAndUserUid(appUid, userUid);

            List<Long> dataFacetHierarchyNodeUidList = bulkGrantsByMemberDto.getDataFacetHierarchyNodeUidList();
            if (CollectionUtils.isEmpty(dataFacetHierarchyNodeUidList)) {
                return;
            }

            List<AppMemberAccessGrantMemberDo> appMemberAccessGrantMemberDoList = new LinkedList();
            dataFacetHierarchyNodeUidList.forEach(dataFacetHierarchyNodeUid -> {
                AppMemberAccessGrantMemberDo appMemberAccessGrantMemberDo = new AppMemberAccessGrantMemberDo();
                appMemberAccessGrantMemberDo.setAppUid(appUid);
                appMemberAccessGrantMemberDo.setUserUid(userUid);
                appMemberAccessGrantMemberDo.setDataFacetHierarchyNodeUid(dataFacetHierarchyNodeUid);
                BaseDo.create(appMemberAccessGrantMemberDo, operatingUserProfile.getUid(), LocalDateTime.now());
                appMemberAccessGrantMemberDoList.add(appMemberAccessGrantMemberDo);
            });
            this.appMemberAccessGrantMemberRepository.saveAll(appMemberAccessGrantMemberDoList);
        });

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void bulkGrantsByRoleForApp(
            Long appUid,
            BulkGrantsByRoleDto bulkGrantsByRoleDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        // verify authorization
        this.appAccessHandler.verifyWriteAuthorization(appUid, operatingUserProfile);

        //
        // Step 2, core-processing
        //

        bulkGrantsByRoleDto.getRoleUidList().forEach(roleUid -> {
            //
            // transform mapping record among 'app uid' and 'role uid' and 'data facet hierarchy node uid' to
            // mapping record among 'app uid' and 'user uid' and 'data facet hierarchy node uid'
            //
            transformRoleGrantsToMemberGrants(
                    appUid,
                    roleUid,
                    bulkGrantsByRoleDto.getDataFacetHierarchyNodeUidList(),
                    operatingUserProfile);

            //
            // Step 3, post-processing
            //
        });

    }

    private void transformRoleGrantsToMemberGrants(
            Long appUid,
            Long roleUid,
            List<Long> dataFacetHierarchyNodeUidList,
            UserProfile operatingUserProfile) {
        List<Long> roleUidList = new LinkedList<>();
        roleUidList.add(roleUid);
        List<AppMemberDo> appMemberDoList = listingQueryMembersOfApp(
                appUid,
                roleUidList,
                null,
                Sort.by(Sort.Order.asc("userUid")),
                operatingUserProfile);
        if (CollectionUtils.isEmpty(appMemberDoList)) {
            return;
        }

        List<Long> userUidList = new LinkedList<>();
        appMemberDoList.forEach(appMemberDo -> {
            Long userUid = appMemberDo.getUserUid();
            if (!userUidList.contains(userUid)) {
                userUidList.add(userUid);
            }
        });
        if (CollectionUtils.isEmpty(userUidList)) {
            return;
        }

        if (CollectionUtils.isEmpty(dataFacetHierarchyNodeUidList)) {
            this.appMemberAccessGrantMemberRepository.deleteByAppUidAndUserUidIn(appUid, userUidList);
        }

        List<AppMemberAccessGrantMemberDo> existingAppMemberAccessGrantMemberDoList =
                this.appMemberAccessGrantMemberRepository.findByAppUidAndUserUidIn(appUid, userUidList);
        Map<Long, List<Long>> existingUserUidAndDataFacetHierarchyNodeUidListMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(existingAppMemberAccessGrantMemberDoList)) {
            existingAppMemberAccessGrantMemberDoList.forEach(appMemberAccessGrantMemberDo -> {
                if (!existingUserUidAndDataFacetHierarchyNodeUidListMap.containsKey(appMemberAccessGrantMemberDo.getUserUid())) {
                    existingUserUidAndDataFacetHierarchyNodeUidListMap.put(appMemberAccessGrantMemberDo.getUserUid(), new LinkedList<>());
                }
                existingUserUidAndDataFacetHierarchyNodeUidListMap.get(appMemberAccessGrantMemberDo.getUserUid())
                        .add(appMemberAccessGrantMemberDo.getDataFacetHierarchyNodeUid());
            });
        }

        List<AppMemberAccessGrantMemberDo> toAddAppMemberAccessGrantMemberDoList = new LinkedList<>();

        userUidList.forEach(userUid -> {
            List<Long> existingDataFacetHierarchyNodeUidList =
                    existingUserUidAndDataFacetHierarchyNodeUidListMap.get(userUid);
            if (CollectionUtils.isEmpty(existingDataFacetHierarchyNodeUidList)) {
                dataFacetHierarchyNodeUidList.forEach(dataFacetHierarchyNodeUid -> {
                    AppMemberAccessGrantMemberDo appMemberAccessGrantMemberDo = new AppMemberAccessGrantMemberDo();
                    appMemberAccessGrantMemberDo.setAppUid(appUid);
                    appMemberAccessGrantMemberDo.setUserUid(userUid);
                    appMemberAccessGrantMemberDo.setDataFacetHierarchyNodeUid(dataFacetHierarchyNodeUid);
                    BaseDo.create(appMemberAccessGrantMemberDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    toAddAppMemberAccessGrantMemberDoList.add(appMemberAccessGrantMemberDo);
                });
            } else {
                dataFacetHierarchyNodeUidList.forEach(dataFacetHierarchyNodeUid -> {
                    if (!existingDataFacetHierarchyNodeUidList.contains(dataFacetHierarchyNodeUid)) {
                        AppMemberAccessGrantMemberDo appMemberAccessGrantMemberDo = new AppMemberAccessGrantMemberDo();
                        appMemberAccessGrantMemberDo.setAppUid(appUid);
                        appMemberAccessGrantMemberDo.setUserUid(userUid);
                        appMemberAccessGrantMemberDo.setDataFacetHierarchyNodeUid(dataFacetHierarchyNodeUid);
                        BaseDo.create(appMemberAccessGrantMemberDo, operatingUserProfile.getUid(), LocalDateTime.now());
                        toAddAppMemberAccessGrantMemberDoList.add(appMemberAccessGrantMemberDo);
                    }
                });
            }
        });

        if (!CollectionUtils.isEmpty(toAddAppMemberAccessGrantMemberDoList)) {
            this.appMemberAccessGrantMemberRepository.saveAll(toAddAppMemberAccessGrantMemberDoList);
        }
    }

    @Override
    public void bulkGrantsByGroupForApp(
            Long appUid,
            BulkGrantsByGroupDto bulkGrantsByGroupDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        // verify authorization
        this.appAccessHandler.verifyWriteAuthorization(appUid, operatingUserProfile);

        //
        // Step 2, core-processing
        //

        bulkGrantsByGroupDto.getGroupUidList().forEach(groupUid -> {
            //
            // transform mapping record among 'app uid' and 'role uid' and 'data facet hierarchy node uid' to
            // mapping record among 'app uid' and 'user uid' and 'data facet hierarchy node uid'
            //
            transformGroupGrantsToMemberGrants(
                    appUid,
                    groupUid,
                    bulkGrantsByGroupDto.getDataFacetHierarchyNodeUidList(),
                    operatingUserProfile);
        });

    }

    private void transformGroupGrantsToMemberGrants(
            Long appUid,
            Long groupUid,
            List<Long> dataFacetHierarchyNodeUidList,
            UserProfile operatingUserProfile) {
        List<Long> groupUidList = new LinkedList<>();
        groupUidList.add(groupUid);
        List<AppMemberDo> appMemberDoList = listingQueryMembersOfApp(
                appUid,
                null,
                groupUidList,
                Sort.by(Sort.Order.asc("userUid")),
                operatingUserProfile);
        if (CollectionUtils.isEmpty(appMemberDoList)) {
            return;
        }

        List<Long> userUidList = new LinkedList<>();
        appMemberDoList.forEach(appMemberDo -> {
            Long userUid = appMemberDo.getUserUid();
            if (!userUidList.contains(userUid)) {
                userUidList.add(userUid);
            }
        });
        if (CollectionUtils.isEmpty(userUidList)) {
            return;
        }

        if (CollectionUtils.isEmpty(dataFacetHierarchyNodeUidList)) {
            return;
        }

        List<AppMemberAccessGrantMemberDo> existingAppMemberAccessGrantMemberDoList =
                this.appMemberAccessGrantMemberRepository.findByAppUidAndUserUidIn(appUid, userUidList);
        Map<Long, List<Long>> existingUserUidAndDataFacetHierarchyNodeUidListMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(existingAppMemberAccessGrantMemberDoList)) {
            existingAppMemberAccessGrantMemberDoList.forEach(appMemberAccessGrantMemberDo -> {
                if (!existingUserUidAndDataFacetHierarchyNodeUidListMap.containsKey(appMemberAccessGrantMemberDo.getUserUid())) {
                    existingUserUidAndDataFacetHierarchyNodeUidListMap.put(appMemberAccessGrantMemberDo.getUserUid(), new LinkedList<>());
                }
                existingUserUidAndDataFacetHierarchyNodeUidListMap.get(appMemberAccessGrantMemberDo.getUserUid())
                        .add(appMemberAccessGrantMemberDo.getDataFacetHierarchyNodeUid());
            });
        }

        List<AppMemberAccessGrantMemberDo> toAddAppMemberAccessGrantMemberDoList = new LinkedList<>();

        userUidList.forEach(userUid -> {
            List<Long> existingDataFacetHierarchyNodeUidList =
                    existingUserUidAndDataFacetHierarchyNodeUidListMap.get(userUid);
            if (CollectionUtils.isEmpty(existingDataFacetHierarchyNodeUidList)) {
                dataFacetHierarchyNodeUidList.forEach(dataFacetHierarchyNodeUid -> {
                    AppMemberAccessGrantMemberDo appMemberAccessGrantMemberDo = new AppMemberAccessGrantMemberDo();
                    appMemberAccessGrantMemberDo.setAppUid(appUid);
                    appMemberAccessGrantMemberDo.setUserUid(userUid);
                    appMemberAccessGrantMemberDo.setDataFacetHierarchyNodeUid(dataFacetHierarchyNodeUid);
                    BaseDo.create(appMemberAccessGrantMemberDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    toAddAppMemberAccessGrantMemberDoList.add(appMemberAccessGrantMemberDo);
                });
            } else {
                dataFacetHierarchyNodeUidList.forEach(dataFacetHierarchyNodeUid -> {
                    if (!existingDataFacetHierarchyNodeUidList.contains(dataFacetHierarchyNodeUid)) {
                        AppMemberAccessGrantMemberDo appMemberAccessGrantMemberDo = new AppMemberAccessGrantMemberDo();
                        appMemberAccessGrantMemberDo.setAppUid(appUid);
                        appMemberAccessGrantMemberDo.setUserUid(userUid);
                        appMemberAccessGrantMemberDo.setDataFacetHierarchyNodeUid(dataFacetHierarchyNodeUid);
                        BaseDo.create(appMemberAccessGrantMemberDo, operatingUserProfile.getUid(), LocalDateTime.now());
                        toAddAppMemberAccessGrantMemberDoList.add(appMemberAccessGrantMemberDo);
                    }
                });
            }
        });

        if (!CollectionUtils.isEmpty(toAddAppMemberAccessGrantMemberDoList)) {
            this.appMemberAccessGrantMemberRepository.saveAll(toAddAppMemberAccessGrantMemberDoList);
        }
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
    public void handleAppCreatedEvent(AppCreatedEvent event) {
        LOGGER.info("rcv event:{}", event);

        Long appUid = event.getUid();
        UserProfile operatingUserProfile = event.getOperatingUserProfile();

        CreateOrReplaceAppGrantStrategyDto createOrReplaceAppGrantStrategyDto =
                new CreateOrReplaceAppGrantStrategyDto();
        createOrReplaceAppGrantStrategyDto.setEnabledEntireGrant(Boolean.TRUE);
        createOrReplaceAppGrantStrategyDto.setEnabledGranularGrant(Boolean.FALSE);
        createOrReplaceGrantStrategyForApp(appUid, createOrReplaceAppGrantStrategyDto, operatingUserProfile);
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

        List<AppMemberAccessGrantMemberDo> appMemberAccessGrantMemberDoList = this.appMemberAccessGrantMemberRepository.findByAppUid(appUid);
        if (!CollectionUtils.isEmpty(appMemberAccessGrantMemberDoList)) {
            this.appMemberAccessGrantMemberRepository.deleteAll(appMemberAccessGrantMemberDoList);
        }

        AppMemberAccessGrantStrategyDo appMemberAccessGrantStrategyDo =
                this.appMemberAccessGrantStrategyRepository.findByAppUid(appUid);
        if (appMemberAccessGrantStrategyDo != null) {
            this.appMemberAccessGrantStrategyRepository.delete(appMemberAccessGrantStrategyDo);
        }
    }

    /**
     * event handler
     *
     * @param event
     */
    @Subscribe
    public void handleAppDataFacetDeleteEvent(AppDataFacetDeletedEvent event) {
        LOGGER.info("rcv event:{}", event);

        List<Long> dataFacetHierarchyNodeUidList = event.getUidList();
        UserProfile operatingUserProfile = event.getOperatingUserProfile();

        List<AppMemberAccessGrantMemberDo> appMemberAccessGrantMemberDoList =
                this.appMemberAccessGrantMemberRepository.findByDataFacetHierarchyNodeUidIn(dataFacetHierarchyNodeUidList);
        if (!CollectionUtils.isEmpty(appMemberAccessGrantMemberDoList)) {
            this.appMemberAccessGrantMemberRepository.deleteAll(appMemberAccessGrantMemberDoList);
        }
    }

    /**
     * event handler
     *
     * @param event
     */
    @Subscribe
    public void handleAppMemberDeletedEvent(AppMemberDeletedEvent event) {
        LOGGER.info("rcv event:{}", event);

        Long appUid = event.getAppUid();
        Long userUid = event.getUserUid();
        UserProfile operatingUserProfile = event.getOperatingUserProfile();

        this.appMemberAccessGrantMemberRepository.deleteByAppUidAndUserUid(appUid, userUid);
    }
}
