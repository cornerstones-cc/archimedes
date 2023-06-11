package cc.cornerstones.biz.app.service.impl;

import cc.cornerstones.almond.exceptions.AbcResourceDuplicateException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
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
import cc.cornerstones.biz.app.entity.AppMemberDo;
import cc.cornerstones.biz.app.entity.AppMemberInviteStrategyDo;
import cc.cornerstones.biz.app.persistence.AppMemberInviteStrategyRepository;
import cc.cornerstones.biz.app.persistence.AppMemberRepository;
import cc.cornerstones.biz.app.service.assembly.AppAccessHandler;
import cc.cornerstones.biz.app.service.inf.AppMemberService;
import cc.cornerstones.biz.app.share.constants.AppMembershipEnum;
import cc.cornerstones.biz.app.share.types.InviteStrategy;
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
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AppMemberServiceImpl implements AppMemberService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppMemberServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private AppMemberInviteStrategyRepository appMemberInviteStrategyRepository;

    @Autowired
    private AppMemberRepository appMemberRepository;

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
    private UserService userService;

    @Autowired
    private AppAccessHandler appAccessHandler;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public AppMemberInviteStrategyDto getInviteStrategyOfApp(
            Long appUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        AppMemberInviteStrategyDo appMemberInviteStrategyDo =
                this.appMemberInviteStrategyRepository.findByAppUid(appUid);
        if (appMemberInviteStrategyDo == null) {
            return null;
        }

        //
        // Step 2, core-processing
        //


        //
        // Step 3, post-processing
        //
        AppMemberInviteStrategyDto appMemberInviteStrategyDto = new AppMemberInviteStrategyDto();
        BeanUtils.copyProperties(appMemberInviteStrategyDo, appMemberInviteStrategyDto);
        appMemberInviteStrategyDto.setEnabledRoles(appMemberInviteStrategyDo.getInviteStrategy().getEnabledRoles());
        appMemberInviteStrategyDto.setRoleUidList(appMemberInviteStrategyDo.getInviteStrategy().getRoleUidList());
        appMemberInviteStrategyDto.setEnabledGroups(appMemberInviteStrategyDo.getInviteStrategy().getEnabledGroups());
        appMemberInviteStrategyDto.setGroupUidList(appMemberInviteStrategyDo.getInviteStrategy().getGroupUidList());
        appMemberInviteStrategyDto.setMembership(appMemberInviteStrategyDo.getInviteStrategy().getMembership());
        return appMemberInviteStrategyDto;
    }

    @Override
    public AppMemberInviteStrategyDto createOrReplaceInviteStrategyForApp(
            Long appUid,
            CreateOrReplaceAppInviteStrategyDto createOrReplaceAppInviteStrategyDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        // verify authorization
        this.appAccessHandler.verifyWriteAuthorization(appUid, operatingUserProfile);


        //
        // Step 2, core-processing
        //

        //
        // Step 2.1,
        //
        AppMemberInviteStrategyDo appMemberInviteStrategyDo =
                this.appMemberInviteStrategyRepository.findByAppUid(appUid);
        if (appMemberInviteStrategyDo == null) {
            appMemberInviteStrategyDo = new AppMemberInviteStrategyDo();
            appMemberInviteStrategyDo.setAppUid(appUid);

            InviteStrategy inviteStrategy = new InviteStrategy();
            inviteStrategy.setEnabledRoles(createOrReplaceAppInviteStrategyDto.getEnabledRoles());
            if (Boolean.TRUE.equals(createOrReplaceAppInviteStrategyDto.getEnabledRoles())) {
                inviteStrategy.setRoleUidList(createOrReplaceAppInviteStrategyDto.getRoleUidList());
            }
            inviteStrategy.setEnabledGroups(createOrReplaceAppInviteStrategyDto.getEnabledGroups());
            if (Boolean.TRUE.equals(createOrReplaceAppInviteStrategyDto.getEnabledGroups())) {
                inviteStrategy.setGroupUidList(createOrReplaceAppInviteStrategyDto.getGroupUidList());
            }
            inviteStrategy.setMembership(createOrReplaceAppInviteStrategyDto.getMembership());
            appMemberInviteStrategyDo.setInviteStrategy(inviteStrategy);

            BaseDo.create(appMemberInviteStrategyDo, operatingUserProfile.getUid(), LocalDateTime.now());
        } else {
            InviteStrategy inviteStrategy = new InviteStrategy();
            inviteStrategy.setEnabledRoles(createOrReplaceAppInviteStrategyDto.getEnabledRoles());
            if (Boolean.TRUE.equals(createOrReplaceAppInviteStrategyDto.getEnabledRoles())) {
                inviteStrategy.setRoleUidList(createOrReplaceAppInviteStrategyDto.getRoleUidList());
            }
            inviteStrategy.setEnabledGroups(createOrReplaceAppInviteStrategyDto.getEnabledGroups());
            if (Boolean.TRUE.equals(createOrReplaceAppInviteStrategyDto.getEnabledGroups())) {
                inviteStrategy.setGroupUidList(createOrReplaceAppInviteStrategyDto.getGroupUidList());
            }
            inviteStrategy.setMembership(createOrReplaceAppInviteStrategyDto.getMembership());
            appMemberInviteStrategyDo.setInviteStrategy(inviteStrategy);

            BaseDo.update(appMemberInviteStrategyDo, operatingUserProfile.getUid(), LocalDateTime.now());
        }

        this.appMemberInviteStrategyRepository.save(appMemberInviteStrategyDo);

        //
        // Step 2.2, initially invite users who belong to roles, groups
        //
        inviteUsers(appUid,
                createOrReplaceAppInviteStrategyDto.getRoleUidList(),
                createOrReplaceAppInviteStrategyDto.getGroupUidList(),
                createOrReplaceAppInviteStrategyDto.getMembership(),
                operatingUserProfile);

        //
        // Step 3, post-processing
        //
        AppMemberInviteStrategyDto appMemberInviteStrategyDto = new AppMemberInviteStrategyDto();
        BeanUtils.copyProperties(appMemberInviteStrategyDo, appMemberInviteStrategyDto);
        return appMemberInviteStrategyDto;
    }

    private void inviteUsers(
            Long appUid,
            List<Long> roleUidList,
            List<Long> groupUidList,
            AppMembershipEnum membership,
            UserProfile operatingUserProfile) {
        List<Long> existingUserUidList = new LinkedList<>();
        List<AppMemberDo> existingAppMemberDoList = this.appMemberRepository.findByAppUid(appUid);
        if (!CollectionUtils.isEmpty(existingAppMemberDoList)) {
            existingAppMemberDoList.forEach(appMemberDo -> {
                if (!existingUserUidList.contains(appMemberDo.getUserUid())) {
                    existingUserUidList.add(appMemberDo.getUserUid());
                }
            });
        }

        List<Long> toAddUserUidList = new LinkedList<>();

        if (!CollectionUtils.isEmpty(roleUidList)) {
            List<UserRoleDo> userRoleDoList = this.userRoleRepository.findByRoleUidIn(roleUidList);
            if (!CollectionUtils.isEmpty(userRoleDoList)) {
                userRoleDoList.forEach(userRoleDo -> {
                    if (!existingUserUidList.contains(userRoleDo.getUserUid())
                            && !toAddUserUidList.contains(userRoleDo.getUserUid())) {
                        toAddUserUidList.add(userRoleDo.getUserUid());
                    }
                });
            }
        }

        if (!CollectionUtils.isEmpty(groupUidList)) {
            List<UserGroupDo> userGroupDoList = this.userGroupRepository.findByGroupUidIn(groupUidList);
            if (!CollectionUtils.isEmpty(userGroupDoList)) {
                userGroupDoList.forEach(userGroupDo -> {
                    if (!existingUserUidList.contains(userGroupDo.getUserUid())
                            && !toAddUserUidList.contains(userGroupDo.getUserUid())) {
                        toAddUserUidList.add(userGroupDo.getUserUid());
                    }
                });
            }
        }

        List<AppMemberDo> toAddAppMemberDoList = new LinkedList<>();
        toAddUserUidList.forEach(userUid -> {
            AppMemberDo appMemberDo = new AppMemberDo();
            appMemberDo.setUserUid(userUid);
            appMemberDo.setAppUid(appUid);

            if (membership != null) {
                appMemberDo.setMembership(membership);
            } else {
                appMemberDo.setMembership(AppMembershipEnum.MEMBER);
            }

            BaseDo.create(appMemberDo, operatingUserProfile.getUid(), LocalDateTime.now());
            toAddAppMemberDoList.add(appMemberDo);
        });

        if (!CollectionUtils.isEmpty(toAddAppMemberDoList)) {
            this.appMemberRepository.saveAll(toAddAppMemberDoList);

            // post event
            toAddAppMemberDoList.forEach(appMemberDo -> {
                AppMemberCreatedEvent appMemberCreatedEvent = new AppMemberCreatedEvent();
                appMemberCreatedEvent.setAppUid(appMemberDo.getAppUid());
                appMemberCreatedEvent.setUserUid(appMemberDo.getUserUid());
                appMemberCreatedEvent.setOperatingUserProfile(operatingUserProfile);
                this.eventBusManager.send(appMemberCreatedEvent);
            });
        }
    }

    @Override
    public AppMemberDto createMemberForApp(
            Long appUid,
            CreateAppMemberDto createAppMemberDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        AppMemberDo appMemberDo = this.appMemberRepository.findByAppUidAndUserUid(appUid,
                createAppMemberDto.getUserUid());
        if (appMemberDo != null) {
            throw new AbcResourceDuplicateException(String.format("%s::app_uid=%d, user_uid=%d", appUid,
                    createAppMemberDto.getUserUid()));
        }

        // verify authorization
        this.appAccessHandler.verifyWriteAuthorization(appUid, operatingUserProfile);

        //
        // Step 2, core-processing
        //
        appMemberDo = new AppMemberDo();
        appMemberDo.setAppUid(appUid);
        appMemberDo.setMembership(createAppMemberDto.getMembership());
        appMemberDo.setUserUid(createAppMemberDto.getUserUid());
        BaseDo.create(appMemberDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.appMemberRepository.save(appMemberDo);

        //
        // Step 3, post-processing
        //

        // post event
        AppMemberCreatedEvent appMemberCreatedEvent = new AppMemberCreatedEvent();
        appMemberCreatedEvent.setAppUid(appMemberDo.getAppUid());
        appMemberCreatedEvent.setUserUid(appMemberDo.getUserUid());
        appMemberCreatedEvent.setOperatingUserProfile(operatingUserProfile);
        this.eventBusManager.send(appMemberCreatedEvent);

        AppMemberDto appMemberDto = new AppMemberDto();
        BeanUtils.copyProperties(appMemberDo, appMemberDto);
        return appMemberDto;
    }

    @Override
    public List<AppMemberDto> batchCreateMembersForApp(
            Long appUid,
            BatchCreateAppMemberDto batchCreateAppMemberDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        List<AppMemberDo> existingAppMemberDoList = this.appMemberRepository.findByAppUidAndUserUidIn(appUid,
                batchCreateAppMemberDto.getUserUidList());
        List<Long> existingUserUidList = new LinkedList<>();
        if (!CollectionUtils.isEmpty(existingAppMemberDoList)) {
            existingAppMemberDoList.forEach(appMemberDo -> {
                if (!existingUserUidList.contains(appMemberDo.getUserUid())) {
                    existingUserUidList.add(appMemberDo.getUserUid());
                }
            });
        }

        // verify authorization
        this.appAccessHandler.verifyWriteAuthorization(appUid, operatingUserProfile);


        //
        // Step 2, core-processing
        //
        List<AppMemberDo> toAddAppMemberDoList = new LinkedList<>();
        batchCreateAppMemberDto.getUserUidList().forEach(userUid -> {
            if (existingUserUidList.contains(userUid)) {
                return;
            }

            AppMemberDo appMemberDo = new AppMemberDo();
            appMemberDo.setAppUid(appUid);
            appMemberDo.setUserUid(userUid);
            if (batchCreateAppMemberDto.getMembership() != null) {
                appMemberDo.setMembership(batchCreateAppMemberDto.getMembership());
            } else {
                appMemberDo.setMembership(AppMembershipEnum.MEMBER);
            }
            BaseDo.create(appMemberDo, operatingUserProfile.getUid(), LocalDateTime.now());
            toAddAppMemberDoList.add(appMemberDo);
        });

        this.appMemberRepository.saveAll(toAddAppMemberDoList);

        //
        // Step 3, post-processing
        //

        // post event
        toAddAppMemberDoList.forEach(appMemberDo -> {
            AppMemberCreatedEvent appMemberCreatedEvent = new AppMemberCreatedEvent();
            appMemberCreatedEvent.setAppUid(appMemberDo.getAppUid());
            appMemberCreatedEvent.setUserUid(appMemberDo.getUserUid());
            appMemberCreatedEvent.setOperatingUserProfile(operatingUserProfile);
            this.eventBusManager.send(appMemberCreatedEvent);
        });

        List<AppMemberDto> appMemberDtoList = new LinkedList<>();
        toAddAppMemberDoList.forEach(appMemberDo -> {
            AppMemberDto appMemberDto = new AppMemberDto();
            BeanUtils.copyProperties(appMemberDo, appMemberDto);
            appMemberDtoList.add(appMemberDto);
        });
        return appMemberDtoList;
    }

    @Override
    public void updateMemberOfApp(
            Long appUid,
            Long userUid,
            UpdateAppMemberDto updateAppMemberDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        AppMemberDo appMemberDo = this.appMemberRepository.findByAppUidAndUserUid(appUid, userUid);
        if (appMemberDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::app_uid=%d, user_uid=%d",
                    AppMemberDo.RESOURCE_SYMBOL, appUid,
                    userUid));
        }

        // verify authorization
        this.appAccessHandler.verifyWriteAuthorization(appUid, operatingUserProfile);


        //
        // Step 2, core-processing
        //
        boolean requiredToUpdate = false;

        if (updateAppMemberDto.getMembership() != null
                && !updateAppMemberDto.getMembership().equals(appMemberDo.getMembership())) {
            appMemberDo.setMembership(updateAppMemberDto.getMembership());
            requiredToUpdate = true;
        }

        if (requiredToUpdate) {
            this.appMemberRepository.save(appMemberDo);
        }

        //
        // Step 3, post-processing
        //
    }

    @Override
    public List<String> listAllReferencesToMemberOfApp(
            Long appUid,
            Long userUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return null;
    }

    @Override
    public void deleteMemberOfApp(
            Long appUid,
            Long userUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        AppMemberDo appMemberDo = this.appMemberRepository.findByAppUidAndUserUid(appUid, userUid);
        if (appMemberDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::app_uid=%d, user_uid=%d",
                    AppMemberDo.RESOURCE_SYMBOL, appUid,
                    userUid));
        }

        // verify authorization
        this.appAccessHandler.verifyWriteAuthorization(appUid, operatingUserProfile);


        //
        // Step 2, core-processing
        //
        appMemberDo.setDeleted(Boolean.TRUE);
        BaseDo.update(appMemberDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.appMemberRepository.save(appMemberDo);


        //
        // Step 3, post-processing
        //

        // post event
        AppMemberDeletedEvent appMemberDeletedEvent = new AppMemberDeletedEvent();
        appMemberDeletedEvent.setAppUid(appMemberDo.getAppUid());
        appMemberDeletedEvent.setUserUid(appMemberDo.getUserUid());
        appMemberDeletedEvent.setOperatingUserProfile(operatingUserProfile);
        this.eventBusManager.send(appMemberDeletedEvent);
    }

    @Override
    public void bulkDeleteMembersOfApp(
            Long appUid,
            BulkRemoveAppMembersDto bulkRemoveAppMembersDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        // verify authorization
        this.appAccessHandler.verifyWriteAuthorization(appUid, operatingUserProfile);

        if (CollectionUtils.isEmpty(bulkRemoveAppMembersDto.getUserUidList())) {
            return;
        }

        bulkRemoveAppMembersDto.getUserUidList().forEach(userUid -> {
            deleteMemberOfApp(appUid, userUid, operatingUserProfile);
        });
    }

    @Override
    public List<AppMemberUserDto> listingQueryMembersOfApp(
            Long appUid,
            List<AppMembershipEnum> membershipList,
            List<Long> userUidListOfUser,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

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
                if (!CollectionUtils.isEmpty(membershipList)) {
                    CriteriaBuilder.In<AppMembershipEnum> in =
                            criteriaBuilder.in(root.get("membership"));
                    membershipList.forEach(membership -> {
                        in.value(membership);
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

        List<AppMemberDo> itemDoList = this.appMemberRepository.findAll(specification, sort);

        //
        // Step 3, post-processing
        //

        if (CollectionUtils.isEmpty(itemDoList)) {
            return null;
        }

        //
        // Step 3.1, 为 created by, last modified by, uid (user) 补充 user brief information
        //
        List<Long> userUidList = new LinkedList<>();
        itemDoList.forEach(itemDo -> {
            if (itemDo.getCreatedBy() != null && !userUidList.contains(itemDo.getCreatedBy())) {
                userUidList.add(itemDo.getCreatedBy());
            }
            if (itemDo.getLastModifiedBy() != null && !userUidList.contains(itemDo.getLastModifiedBy())) {
                userUidList.add(itemDo.getLastModifiedBy());
            }

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
        if (CollectionUtils.isEmpty(itemDoList)) {
            return null;
        } else {
            List<Long> queryUserUidList = new ArrayList<>(itemDoList.size());
            List<AppMemberUserDto> itemDtoList = new ArrayList<>(itemDoList.size());
            itemDoList.forEach(itemDo -> {
                AppMemberUserDto itemDto = new AppMemberUserDto();
                BeanUtils.copyProperties(itemDo, itemDto);

                // 为 created by, last modified by 补充 user brief information
                itemDto.setCreatedByUser(userBriefInformationMap.get(itemDo.getCreatedBy()));
                itemDto.setLastModifiedByUser(userBriefInformationMap.get(itemDo.getLastModifiedBy()));
                // 为 uid (user) 补充 user brief information
                itemDto.setUser(userBriefInformationMap.get(itemDo.getUserUid()));

                itemDtoList.add(itemDto);

                queryUserUidList.add(itemDo.getUserUid());
            });

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
            itemDtoList.forEach(itemDto -> {
                if (itemDto.getUser() != null) {
                    itemDto.setRoleList(userUidAndRoleListMap.get(itemDto.getUser().getUid()));
                    itemDto.setGroupList(userUidAndGroupListMap.get(itemDto.getUser().getUid()));
                }
            });

            // 返回
            return itemDtoList;
        }
    }

    @Override
    public Page<AppMemberUserDto> pagingQueryMembersOfApp(
            Long appUid,
            List<AppMembershipEnum> membershipList,
            List<Long> userUidListOfMembershipLastModifiedBy,
            List<String> membershipLastModifiedTimestampAsStringList,
            List<Long> userUidListOfUser,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

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
                if (!CollectionUtils.isEmpty(membershipList)) {
                    CriteriaBuilder.In<AppMembershipEnum> in =
                            criteriaBuilder.in(root.get("membership"));
                    membershipList.forEach(membership -> {
                        in.value(membership);
                    });
                    predicateList.add(in);
                }
                if (!CollectionUtils.isEmpty(userUidListOfMembershipLastModifiedBy)) {
                    CriteriaBuilder.In<Long> in = criteriaBuilder.in(root.get(BaseDo.LAST_MODIFIED_BY_FIELD_NAME));
                    userUidListOfMembershipLastModifiedBy.forEach(item -> {
                        in.value(item);
                    });
                    predicateList.add(in);
                }
                if (!CollectionUtils.isEmpty(membershipLastModifiedTimestampAsStringList)) {
                    if (membershipLastModifiedTimestampAsStringList.size() == 2) {
                        LocalDateTime dateTime0 = LocalDateTime.parse(membershipLastModifiedTimestampAsStringList.get(0),
                                dateTimeFormatter);
                        LocalDateTime dateTime1 = LocalDateTime.parse(membershipLastModifiedTimestampAsStringList.get(1), dateTimeFormatter);
                        if (dateTime0.isAfter(dateTime1)) {
                            predicateList.add(criteriaBuilder.between(root.get(BaseDo.LAST_MODIFIED_TIMESTAMP_FIELD_NAME),
                                    dateTime1, dateTime0));
                        } else {
                            predicateList.add(criteriaBuilder.between(root.get(BaseDo.LAST_MODIFIED_TIMESTAMP_FIELD_NAME),
                                    dateTime0, dateTime1));
                        }
                    } else if (membershipLastModifiedTimestampAsStringList.size() == 1) {
                        LocalDateTime dateTime0 = LocalDateTime.parse(membershipLastModifiedTimestampAsStringList.get(0), dateTimeFormatter);
                        predicateList.add(criteriaBuilder.equal(root.get(BaseDo.LAST_MODIFIED_TIMESTAMP_FIELD_NAME),
                                dateTime0));
                    } else {
                        CriteriaBuilder.In<LocalDateTime> in =
                                criteriaBuilder.in(root.get(BaseDo.LAST_MODIFIED_TIMESTAMP_FIELD_NAME));
                        membershipLastModifiedTimestampAsStringList.forEach(createdTimestampAsString -> {
                            LocalDateTime dateTime0 = LocalDateTime.parse(createdTimestampAsString, dateTimeFormatter);
                            in.value(dateTime0);
                        });
                        predicateList.add(in);
                    }
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

        Page<AppMemberDo> itemDoPage = this.appMemberRepository.findAll(specification, pageable);

        //
        // Step 3, post-processing
        //

        //
        // Step 3.1, 为 created by, last modified by, uid (user) 补充 user brief information
        //
        List<Long> userUidList = new LinkedList<>();
        itemDoPage.forEach(itemDo -> {
            if (itemDo.getCreatedBy() != null && !userUidList.contains(itemDo.getCreatedBy())) {
                userUidList.add(itemDo.getCreatedBy());
            }
            if (itemDo.getLastModifiedBy() != null && !userUidList.contains(itemDo.getLastModifiedBy())) {
                userUidList.add(itemDo.getLastModifiedBy());
            }

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
        if (itemDoPage.isEmpty()) {
            Page<AppMemberUserDto> itemDtoPage = new PageImpl<AppMemberUserDto>(
                    new ArrayList<>(), pageable, itemDoPage.getTotalElements());
            return itemDtoPage;
        } else {
            List<Long> queryUserUidList = new ArrayList<>(itemDoPage.getContent().size());
            List<AppMemberUserDto> content = new ArrayList<>(itemDoPage.getContent().size());
            itemDoPage.forEach(itemDo -> {
                AppMemberUserDto itemDto = new AppMemberUserDto();
                BeanUtils.copyProperties(itemDo, itemDto);

                // 为 created by, last modified by 补充 user brief information
                itemDto.setCreatedByUser(userBriefInformationMap.get(itemDo.getCreatedBy()));
                itemDto.setLastModifiedByUser(userBriefInformationMap.get(itemDo.getLastModifiedBy()));
                // 为 uid (user) 补充 user brief information
                itemDto.setUser(userBriefInformationMap.get(itemDo.getUserUid()));

                content.add(itemDto);

                queryUserUidList.add(itemDo.getUserUid());
            });
            Page<AppMemberUserDto> itemDtoPage = new PageImpl<AppMemberUserDto>(
                    content, pageable, itemDoPage.getTotalElements());

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
            content.forEach(itemDto -> {
                if (itemDto.getUser() != null) {
                    itemDto.setRoleList(userUidAndRoleListMap.get(itemDto.getUser().getUid()));
                    itemDto.setGroupList(userUidAndGroupListMap.get(itemDto.getUser().getUid()));
                }
            });

            // 返回
            return itemDtoPage;
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
    public void handleRoleDeletedEvent(RoleDeletedEvent event) {
        LOGGER.info("rcv event:{}", event);

        Long roleUid = event.getUid();
        UserProfile operatingUserProfile = event.getOperatingUserProfile();

        this.appMemberInviteStrategyRepository.findAll().forEach(appMemberInviteStrategyDo -> {
            InviteStrategy inviteStrategy = appMemberInviteStrategyDo.getInviteStrategy();

            if (Boolean.TRUE.equals(inviteStrategy.getEnabledRoles())) {
                if (CollectionUtils.isEmpty(inviteStrategy.getRoleUidList())) {
                    return;
                }

                if (inviteStrategy.getRoleUidList().contains(roleUid)) {
                    inviteStrategy.getRoleUidList().remove(roleUid);

                    BaseDo.update(appMemberInviteStrategyDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.appMemberInviteStrategyRepository.save(appMemberInviteStrategyDo);
                }
            }
        });
    }

    /**
     * event handler
     *
     * @param event
     */
    @Subscribe
    public void handleGroupDeletedEvent(GroupDeletedEvent event) {
        LOGGER.info("rcv event:{}", event);

        Long groupUid = event.getUid();
        UserProfile operatingUserProfile = event.getOperatingUserProfile();

        this.appMemberInviteStrategyRepository.findAll().forEach(appMemberInviteStrategyDo -> {
            InviteStrategy inviteStrategy = appMemberInviteStrategyDo.getInviteStrategy();

            if (Boolean.TRUE.equals(inviteStrategy.getEnabledGroups())) {
                if (CollectionUtils.isEmpty(inviteStrategy.getGroupUidList())) {
                    return;
                }

                if (inviteStrategy.getGroupUidList().contains(groupUid)) {
                    inviteStrategy.getGroupUidList().remove(groupUid);

                    BaseDo.update(appMemberInviteStrategyDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.appMemberInviteStrategyRepository.save(appMemberInviteStrategyDo);
                }
            }
        });
    }

    /**
     * event handler
     *
     * @param event
     */
    @Subscribe
    public void handleUserDeletedEvent(UserDeletedEvent event) {
        LOGGER.info("rcv event:{}", event);

        Long userUid = event.getUid();
        UserProfile operatingUserProfile = event.getOperatingUserProfile();

        List<AppMemberDo> appMemberDoList = this.appMemberRepository.findByUserUid(userUid);
        if (!CollectionUtils.isEmpty(appMemberDoList)) {
            this.appMemberRepository.deleteAll(appMemberDoList);

            appMemberDoList.forEach(appMemberDo -> {
                // post event
                AppMemberDeletedEvent appMemberDeletedEvent = new AppMemberDeletedEvent();
                appMemberDeletedEvent.setAppUid(appMemberDo.getAppUid());
                appMemberDeletedEvent.setUserUid(appMemberDo.getUserUid());
                appMemberDeletedEvent.setOperatingUserProfile(operatingUserProfile);
                this.eventBusManager.send(appMemberDeletedEvent);
            });

        }

    }

    /**
     * event handler
     *
     * @param event
     */
    @Subscribe
    public void handleUserRoleChangedEvent(UserRoleChangedEvent event) {
        LOGGER.info("rcv event:{}", event);

        Long userUid = event.getUserUid();
        List<Long> newRoleUidList = event.getNewRoleUidList();
        UserProfile operatingUserProfile = event.getOperatingUserProfile();

        if (CollectionUtils.isEmpty(newRoleUidList)) {
            return;
        }

        this.appMemberInviteStrategyRepository.findAll().forEach(appMemberInviteStrategyDo -> {
            Long appUid = appMemberInviteStrategyDo.getAppUid();

            AppMemberDo appMemberDo = this.appMemberRepository.findByAppUidAndUserUid(appUid, userUid);
            if (appMemberDo != null) {
                return;
            }

            InviteStrategy inviteStrategy = appMemberInviteStrategyDo.getInviteStrategy();

            if (Boolean.TRUE.equals(inviteStrategy.getEnabledRoles())) {
                List<Long> objectiveRoleUidList = inviteStrategy.getRoleUidList();
                if (CollectionUtils.isEmpty(objectiveRoleUidList)) {
                    return;
                }

                for (Long newRoleUid : newRoleUidList) {
                    if (objectiveRoleUidList.contains(newRoleUid)) {
                        // 该 user 的该 role 属于该 app 的邀请范围，确保该 user 是该 app 的 member
                        appMemberDo = new AppMemberDo();
                        appMemberDo.setAppUid(appUid);
                        appMemberDo.setUserUid(userUid);

                        if (inviteStrategy.getMembership() != null) {
                            appMemberDo.setMembership(inviteStrategy.getMembership());
                        } else {
                            appMemberDo.setMembership(AppMembershipEnum.MEMBER);
                        }

                        BaseDo.create(appMemberDo, operatingUserProfile.getUid(), LocalDateTime.now());
                        this.appMemberRepository.save(appMemberDo);

                        // post event
                        AppMemberCreatedEvent appMemberCreatedEvent = new AppMemberCreatedEvent();
                        appMemberCreatedEvent.setAppUid(appMemberDo.getAppUid());
                        appMemberCreatedEvent.setUserUid(appMemberDo.getUserUid());
                        appMemberCreatedEvent.setOperatingUserProfile(operatingUserProfile);
                        this.eventBusManager.send(appMemberCreatedEvent);

                        return;
                    }
                }
            }
        });

    }

    /**
     * event handler
     *
     * @param event
     */
    @Subscribe
    public void handleUserGroupChangedEvent(UserGroupChangedEvent event) {
        LOGGER.info("rcv event:{}", event);

        Long userUid = event.getUserUid();
        List<Long> newGroupUidList = event.getNewGroupUidList();
        UserProfile operatingUserProfile = event.getOperatingUserProfile();

        if (CollectionUtils.isEmpty(newGroupUidList)) {
            return;
        }

        this.appMemberInviteStrategyRepository.findAll().forEach(appMemberInviteStrategyDo -> {
            Long appUid = appMemberInviteStrategyDo.getAppUid();

            AppMemberDo appMemberDo = this.appMemberRepository.findByAppUidAndUserUid(appUid, userUid);
            if (appMemberDo != null) {
                return;
            }

            InviteStrategy inviteStrategy = appMemberInviteStrategyDo.getInviteStrategy();

            if (Boolean.TRUE.equals(inviteStrategy.getEnabledGroups())) {
                List<Long> objectiveGroupUidList = inviteStrategy.getGroupUidList();
                if (CollectionUtils.isEmpty(objectiveGroupUidList)) {
                    return;
                }

                for (Long newGroupUid : newGroupUidList) {
                    if (objectiveGroupUidList.contains(newGroupUid)) {
                        // 该 user 的该 group 属于该 app 的邀请范围，确保该 user 是该 app 的 member
                        appMemberDo = new AppMemberDo();
                        appMemberDo.setAppUid(appUid);
                        appMemberDo.setUserUid(userUid);

                        if (inviteStrategy.getMembership() != null) {
                            appMemberDo.setMembership(inviteStrategy.getMembership());
                        } else {
                            appMemberDo.setMembership(AppMembershipEnum.MEMBER);
                        }

                        BaseDo.create(appMemberDo, operatingUserProfile.getUid(), LocalDateTime.now());
                        this.appMemberRepository.save(appMemberDo);

                        // post event
                        AppMemberCreatedEvent appMemberCreatedEvent = new AppMemberCreatedEvent();
                        appMemberCreatedEvent.setAppUid(appMemberDo.getAppUid());
                        appMemberCreatedEvent.setUserUid(appMemberDo.getUserUid());
                        appMemberCreatedEvent.setOperatingUserProfile(operatingUserProfile);
                        this.eventBusManager.send(appMemberCreatedEvent);

                        return;
                    }
                }
            }
        });

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

        List<AppMemberDo> appMemberDoList = this.appMemberRepository.findByAppUid(appUid);
        if (!CollectionUtils.isEmpty(appMemberDoList)) {
            this.appMemberRepository.deleteAll(appMemberDoList);
        }

        AppMemberInviteStrategyDo appMemberInviteStrategyDo =
                this.appMemberInviteStrategyRepository.findByAppUid(appUid);
        if (appMemberInviteStrategyDo != null) {
            this.appMemberInviteStrategyRepository.delete(appMemberInviteStrategyDo);
        }
    }

}
