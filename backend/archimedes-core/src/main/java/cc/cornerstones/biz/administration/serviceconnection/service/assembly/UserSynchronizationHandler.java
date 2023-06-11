package cc.cornerstones.biz.administration.serviceconnection.service.assembly;

import cc.cornerstones.almond.constants.DatabaseConstants;
import cc.cornerstones.almond.constants.InfrastructureConstants;
import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.arbutus.pf4j.service.assembly.PluginHelper;
import cc.cornerstones.arbutus.pf4j.share.types.PluginProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.archimedes.extensions.UserSynchronizationServiceProvider;
import cc.cornerstones.archimedes.extensions.types.UserInfo;
import cc.cornerstones.biz.administration.serviceconnection.dto.UserSynchronizationExecutionSummaryDto;
import cc.cornerstones.biz.administration.serviceconnection.entity.UserSynchronizationExecutionInstanceDo;
import cc.cornerstones.biz.administration.serviceconnection.entity.UserSynchronizationServiceAgentDo;
import cc.cornerstones.biz.administration.serviceconnection.entity.UserSynchronizationServiceComponentDo;
import cc.cornerstones.biz.administration.serviceconnection.persistence.UserSynchronizationExecutionInstanceRepository;
import cc.cornerstones.biz.administration.serviceconnection.persistence.UserSynchronizationServiceComponentRepository;
import cc.cornerstones.biz.administration.serviceconnection.persistence.UserSynchronizationServiceAgentRepository;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DfsServiceAgentService;
import cc.cornerstones.biz.administration.usermanagement.dto.CreateUserDto;
import cc.cornerstones.biz.administration.usermanagement.dto.ReplaceUserDto;
import cc.cornerstones.biz.administration.usermanagement.entity.*;
import cc.cornerstones.biz.administration.usermanagement.persistence.*;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.administration.usermanagement.share.types.Account;
import cc.cornerstones.biz.administration.usermanagement.share.types.ExtendedProperty;
import cc.cornerstones.biz.distributedjob.share.types.JobHandler;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class UserSynchronizationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserSynchronizationHandler.class);

    public static final String JOB_HANDLER_USER_SYNCHRONIZATION = "user_synchronization";

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private PluginHelper pluginHelper;

    @Autowired
    private UserSynchronizationServiceAgentRepository userSynchronizationServiceAgentRepository;

    @Autowired
    private UserSynchronizationServiceComponentRepository userSynchronizationServiceComponentRepository;

    @Autowired
    private UserSynchronizationExecutionInstanceRepository userSynchronizationExecutionInstanceRepository;

    @Autowired
    private UserBasicRepository userBasicRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private AccountTypeRepository accountTypeRepository;

    @Autowired
    private UserExtendedPropertyRepository userExtendedPropertyRepository;

    @Autowired
    private UserSchemaExtendedPropertyRepository userSchemaExtendedPropertyRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private DfsServiceAgentService dfsServiceAgentService;

    @JobHandler(name = JOB_HANDLER_USER_SYNCHRONIZATION)
    public UserSynchronizationExecutionInstanceDo executeUserSynchronization(
            JSONObject params) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        Long userSynchronizationServiceAgentUid = params.getLongValue("user_synchronization_service_agent_uid");
        if (userSynchronizationServiceAgentUid == null) {
            LOGGER.error("cannot find user_synchronization_service_agent_uid from the input parameters");
            return null;
        }

        UserSynchronizationServiceAgentDo userSynchronizationServiceAgentDo =
                this.userSynchronizationServiceAgentRepository.findByUid(userSynchronizationServiceAgentUid);
        if (userSynchronizationServiceAgentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", UserSynchronizationServiceAgentDo.RESOURCE_SYMBOL,
                    userSynchronizationServiceAgentUid));
        }
        Long userSynchronizationServiceComponentUid = userSynchronizationServiceAgentDo.getServiceComponentUid();
        UserSynchronizationServiceComponentDo userSynchronizationServiceComponentDo =
                this.userSynchronizationServiceComponentRepository.findByUid(userSynchronizationServiceComponentUid);
        if (userSynchronizationServiceComponentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", UserSynchronizationServiceComponentDo.RESOURCE_SYMBOL,
                    userSynchronizationServiceComponentUid));
        }

        // tracking
        UserSynchronizationExecutionInstanceDo userSynchronizationExecutionInstanceDo =
                new UserSynchronizationExecutionInstanceDo();
        userSynchronizationExecutionInstanceDo.setUid(this.idHelper.getNextDistributedId(UserSynchronizationExecutionInstanceDo.RESOURCE_NAME));
        userSynchronizationExecutionInstanceDo.setBeginTimestamp(LocalDateTime.now());
        userSynchronizationExecutionInstanceDo.setServiceAgentUid(userSynchronizationServiceAgentDo.getUid());
        userSynchronizationExecutionInstanceDo.setStatus(JobStatusEnum.CREATED);
        BaseDo.create(userSynchronizationExecutionInstanceDo, InfrastructureConstants.ROOT_USER_UID,
                LocalDateTime.now());
        this.userSynchronizationExecutionInstanceRepository.save(userSynchronizationExecutionInstanceDo);

        //
        // Step 2, core-processing
        //

        try {
            //
            // Step 2.1, find user synchronization service provider of the service component
            //
            UserSynchronizationServiceProvider userSynchronizationServiceProvider = null;
            switch (userSynchronizationServiceComponentDo.getType()) {
                case BUILTIN: {
                    String entryClassName = userSynchronizationServiceComponentDo.getEntryClassName();
                    Map<String, UserSynchronizationServiceProvider> candidateUserSynchronizationServiceProviderMap =
                            this.applicationContext.getBeansOfType(UserSynchronizationServiceProvider.class);
                    if (!CollectionUtils.isEmpty(candidateUserSynchronizationServiceProviderMap)) {
                        for (UserSynchronizationServiceProvider candidateUserSynchronizationServiceProvider :
                                candidateUserSynchronizationServiceProviderMap.values()) {
                            if (candidateUserSynchronizationServiceProvider.getClass().getName().equals(entryClassName)) {
                                userSynchronizationServiceProvider = candidateUserSynchronizationServiceProvider;
                                break;
                            }
                        }
                    }
                    if (userSynchronizationServiceProvider == null) {
                        throw new AbcResourceConflictException(String.format("cannot find user synchronization " +
                                        "service provider of service " +
                                        "component:%s",
                                userSynchronizationServiceComponentDo.getName()));
                    }
                }
                break;
                case PLUGIN: {
                    PluginProfile pluginProfile = userSynchronizationServiceComponentDo.getBackEndComponentMetadata();
                    if (pluginProfile == null || ObjectUtils.isEmpty(pluginProfile.getPluginId())) {
                        throw new AbcResourceConflictException("illegal plugin");
                    }
                    try {
                        this.pluginHelper.ensureStartPluginIdentifiedByPluginId(pluginProfile.getPluginId());
                    } catch (Exception e) {
                        File pluginFile = this.dfsServiceAgentService.downloadFile(
                                userSynchronizationServiceComponentDo.getDfsServiceAgentUidOfBackEndComponentFileId(),
                                userSynchronizationServiceComponentDo.getBackEndComponentFileId(),
                                null);
                        try {
                            this.pluginHelper.ensureStartPluginIdentifiedByPath(pluginFile.getAbsolutePath());
                        } catch (Exception e3) {
                            LOGGER.error("failed to ensure start plugin:{}", pluginProfile.getPluginId(), e3);
                            throw new AbcResourceConflictException("failed to load plugin");
                        }
                    }

                    List<UserSynchronizationServiceProvider> listOfProcessors =
                            this.pluginHelper.getPluginManager().getExtensions(
                                    UserSynchronizationServiceProvider.class,
                                    pluginProfile.getPluginId());
                    if (listOfProcessors == null || listOfProcessors.isEmpty()) {
                        this.pluginHelper.getPluginManager().unloadPlugin(pluginProfile.getPluginId());
                        throw new AbcUndefinedException("cannot find UserSynchronizationServiceProvider");
                    }

                    if (listOfProcessors.size() > 1) {
                        this.pluginHelper.getPluginManager().unloadPlugin(pluginProfile.getPluginId());
                        throw new AbcUndefinedException("found " + listOfProcessors.size() + " UserSynchronizationServiceProvider");
                    }

                    userSynchronizationServiceProvider = listOfProcessors.get(0);
                }
                break;
                default:
                    throw new AbcResourceConflictException(String.format("unsupported service provider type of service " +
                                    "component:%s",
                            userSynchronizationServiceComponentDo.getType()));
            }

            //
            // Step 2.2, execute user synchronization
            //
            List<UserInfo> result =
                    userSynchronizationServiceProvider.listingQueryAllUsers(
                            userSynchronizationServiceAgentDo.getConfiguration());

            save(result);

            // tracking
            UserSynchronizationExecutionSummaryDto userSynchronizationExecutionSummaryDto =
                    new UserSynchronizationExecutionSummaryDto();
            userSynchronizationExecutionSummaryDto.setNumberOfUsers(result.size());
            //
            userSynchronizationExecutionInstanceDo.setSummary(userSynchronizationExecutionSummaryDto);
            userSynchronizationExecutionInstanceDo.setStatus(JobStatusEnum.FINISHED);
        } catch (Exception e) {
            LOGGER.error("failed to execute user synchronization service provider of service component {}",
                    userSynchronizationServiceComponentDo.getName(), e);

            // tracking
            String remark = "failed to execute user synchronization";
            if (!ObjectUtils.isEmpty(e.getMessage())) {
                remark = e.getMessage();
                if (remark.length() > DatabaseConstants.DEFAULT_DESCRIPTION_LENGTH) {
                    remark = remark.substring(0, DatabaseConstants.DEFAULT_DESCRIPTION_LENGTH);
                }
            }
            userSynchronizationExecutionInstanceDo.setRemark(remark);
            userSynchronizationExecutionInstanceDo.setStatus(JobStatusEnum.FAILED);
        } finally {
            // tracking
            userSynchronizationExecutionInstanceDo.setEndTimestamp(LocalDateTime.now());
            BaseDo.update(userSynchronizationExecutionInstanceDo, InfrastructureConstants.ROOT_USER_UID,
                    LocalDateTime.now());
            this.userSynchronizationExecutionInstanceRepository.save(userSynchronizationExecutionInstanceDo);
        }

        //
        // Step 3, post-processing
        //

        return userSynchronizationExecutionInstanceDo;
    }

    private void save(List<UserInfo> inputItemList) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        Map<Long, AccountTypeDo> availableAccountTypeDoMap = new HashMap<>();
        this.accountTypeRepository.findAll().forEach(accountTypeDo -> {
            availableAccountTypeDoMap.put(accountTypeDo.getUid(), accountTypeDo);
        });
        Map<Long, UserSchemaExtendedPropertyDo> availableUserSchemaExtendedPropertyDoMap = new HashMap<>();
        this.userSchemaExtendedPropertyRepository.findAll().forEach(userSchemaExtendedPropertyDo -> {
            availableUserSchemaExtendedPropertyDoMap.put(userSchemaExtendedPropertyDo.getUid(), userSchemaExtendedPropertyDo);
        });
        List<Long> availableRoleUidList = new LinkedList<>();
        this.roleRepository.findAll().forEach(roleDo -> {
            availableRoleUidList.add(roleDo.getUid());
        });
        List<Long> availableGroupUidList = new LinkedList<>();
        this.groupRepository.findAll().forEach(groupDo -> {
            availableGroupUidList.add(groupDo.getUid());
        });

        Map<Long, List<UserAccountDo>> existingUserUidAndUserAccountDoList = new HashMap<>();
        Map<String, Long> existingAccountMap = new HashMap<>();
        this.userAccountRepository.findAll().forEach(userAccountDo -> {
            // 忽略大小写
            String key = userAccountDo.getAccountTypeUid() + "#" + userAccountDo.getName().toLowerCase();
            existingAccountMap.put(key, userAccountDo.getUserUid());

            if (!existingUserUidAndUserAccountDoList.containsKey(userAccountDo.getUserUid())) {
                existingUserUidAndUserAccountDoList.put(userAccountDo.getUserUid(), new LinkedList<>());
            }
            existingUserUidAndUserAccountDoList.get(userAccountDo.getUserUid()).add(userAccountDo);
        });

        Map<Long, UserBasicDo> existingUserBasicDoMap = new HashMap<>();
        this.userBasicRepository.findAll().forEach(userBasicDo -> {
            existingUserBasicDoMap.put(userBasicDo.getUid(), userBasicDo);
        });

        Map<Long, List<Long>> existingUserRoleUidListMap = new HashMap<>();
        this.userRoleRepository.findAll().forEach(userRoleDo -> {
            if (!existingUserRoleUidListMap.containsKey(userRoleDo.getUserUid())) {
                existingUserRoleUidListMap.put(userRoleDo.getUserUid(), new LinkedList<>());
            }
            existingUserRoleUidListMap.get(userRoleDo.getUserUid()).add(userRoleDo.getRoleUid());
        });

        Map<Long, List<Long>> existingUserGroupUidListMap = new HashMap<>();
        this.userGroupRepository.findAll().forEach(userGroupDo -> {
            if (!existingUserGroupUidListMap.containsKey(userGroupDo.getUserUid())) {
                existingUserGroupUidListMap.put(userGroupDo.getUserUid(), new LinkedList<>());
            }
            existingUserGroupUidListMap.get(userGroupDo.getUserUid()).add(userGroupDo.getGroupUid());
        });

        Map<Long, List<UserExtendedPropertyDo>> existingUserUidAndUserExtendedPropertyDoListMap = new HashMap<>();
        this.userExtendedPropertyRepository.findAll().forEach(userExtendedPropertyDo -> {
            if (!existingUserUidAndUserExtendedPropertyDoListMap.containsKey(userExtendedPropertyDo.getUserUid())) {
                existingUserUidAndUserExtendedPropertyDoListMap.put(userExtendedPropertyDo.getUserUid(),
                        new LinkedList<>());
            }
            existingUserUidAndUserExtendedPropertyDoListMap.get(userExtendedPropertyDo.getUserUid()).add(userExtendedPropertyDo);
        });

        //
        // Step 2, core-processing
        //

        List<CreateUserDto> createUserDtoList = new LinkedList<>();
        List<ReplaceUserDto> replaceUserDtoList = new LinkedList<>();

        inputItemList.forEach(inputItem -> {
            List<UserInfo.AccountInfo> accountInfoList = inputItem.getAccounts();
            if (CollectionUtils.isEmpty(accountInfoList)) {
                LOGGER.warn("illegal user info {}, found null or empty account", inputItem.getDisplayName());
                return;
            }

            Long foundUserUid = null;
            List<UserInfo.AccountInfo> newAccountInfoList = new LinkedList<>();
            for (UserInfo.AccountInfo accountInfo : accountInfoList) {
                if (!availableAccountTypeDoMap.containsKey(accountInfo.getAccountTypeUid())) {
                    LOGGER.warn("illegal user info {}, unknown account type uid {}, {}", inputItem.getDisplayName(),
                            accountInfo.getAccountTypeUid(), accountInfo.getAccountName());
                    return;
                }

                // 忽略大小写
                String key = accountInfo.getAccountTypeUid() + "#" + accountInfo.getAccountName().toLowerCase();
                if (existingAccountMap.containsKey(key)) {
                    // found existing user
                    Long newFoundUserUid = existingAccountMap.get(key);
                    if (foundUserUid != null && !foundUserUid.equals(newFoundUserUid)) {
                        LOGGER.warn("illegal user info {}, found more than 1 existing user uid", inputItem.getDisplayName());
                        return;
                    }
                    foundUserUid = newFoundUserUid;
                } else {
                    newAccountInfoList.add(accountInfo);
                }
            }

            if (foundUserUid == null) {
                // 新用户
                CreateUserDto createUserDto = new CreateUserDto();
                createUserDto.setEnabled(Boolean.TRUE);
                if (ObjectUtils.isEmpty(inputItem.getDisplayName())) {
                    createUserDto.setDisplayName("Unknown");
                } else {
                    if (inputItem.getDisplayName().equalsIgnoreCase(InfrastructureConstants.ROOT_USER_DISPLAY_NAME)) {
                        createUserDto.setDisplayName(inputItem.getDisplayName() + "" + System.currentTimeMillis());
                    } else {
                        createUserDto.setDisplayName(inputItem.getDisplayName());
                    }
                }
                createUserDto.setPassword(DigestUtils.sha1Hex("197010"));

                // account list
                createUserDto.setAccountList(new LinkedList<>());
                for (UserInfo.AccountInfo accountInfo : accountInfoList) {
                    Account account = new Account();
                    account.setAccountTypeUid(accountInfo.getAccountTypeUid());
                    account.setAccountName(accountInfo.getAccountName());
                    createUserDto.getAccountList().add(account);
                }

                // extended property list
                if (!CollectionUtils.isEmpty(inputItem.getExtendedProperties())) {
                    createUserDto.setExtendedPropertyList(new LinkedList<>());
                    for (UserInfo.ExtendedPropertyInfo extendedPropertyInfo : inputItem.getExtendedProperties()) {
                        if (!availableUserSchemaExtendedPropertyDoMap.containsKey(extendedPropertyInfo.getExtendedPropertyUid())) {
                            LOGGER.warn("illegal user info {}, unknown extended property uid {}, {}",
                                    inputItem.getDisplayName(), extendedPropertyInfo.getExtendedPropertyUid(),
                                    extendedPropertyInfo.getExtendedPropertyValue());
                            return;
                        }

                        ExtendedProperty extendedProperty = new ExtendedProperty();
                        extendedProperty.setExtendedPropertyUid(extendedPropertyInfo.getExtendedPropertyUid());
                        extendedProperty.setExtendedPropertyValue(extendedPropertyInfo.getExtendedPropertyValue());
                        createUserDto.getExtendedPropertyList().add(extendedProperty);
                    }
                }

                // role uid list
                if (!CollectionUtils.isEmpty(inputItem.getRoleUidList())) {
                    createUserDto.setRoleUidList(new LinkedList<>());
                    for (Long roleUid : inputItem.getRoleUidList()) {
                        if (!availableRoleUidList.contains(roleUid)) {
                            LOGGER.warn("illegal user info {}, unknown role uid {}, {}",
                                    inputItem.getDisplayName(), roleUid);
                            return;
                        }
                        createUserDto.getRoleUidList().add(roleUid);
                    }
                }

                // group uid list
                if (!CollectionUtils.isEmpty(inputItem.getGroupUidList())) {
                    createUserDto.setGroupUidList(new LinkedList<>());
                    for (Long groupUid : inputItem.getGroupUidList()) {
                        if (!availableGroupUidList.contains(groupUid)) {
                            LOGGER.warn("illegal user info {}, unknown group uid {}, {}",
                                    inputItem.getDisplayName(), groupUid);
                            return;
                        }
                        createUserDto.getGroupUidList().add(groupUid);
                    }
                }

                createUserDtoList.add(createUserDto);
            } else {
                // 老用户
                UserBasicDo userBasicDo = existingUserBasicDoMap.get(foundUserUid);
                if (userBasicDo == null) {
                    LOGGER.error("illegal user info {}, cannot find user {}", inputItem.getDisplayName(), foundUserUid);
                    return;
                }

                List<UserAccountDo> existingUserAccountDoList =
                        existingUserUidAndUserAccountDoList.get(foundUserUid);
                List<UserExtendedPropertyDo> existingUserExtendedPropertyDoList =
                        existingUserUidAndUserExtendedPropertyDoListMap.get(foundUserUid);

                ReplaceUserDto replaceUserDto = new ReplaceUserDto();
                replaceUserDto.setUid(foundUserUid);
                replaceUserDto.setEnabled(userBasicDo.getEnabled());
                if (ObjectUtils.isEmpty(inputItem.getDisplayName())) {
                    if (ObjectUtils.isEmpty(userBasicDo.getDisplayName())) {
                        replaceUserDto.setDisplayName("Unknown");
                    } else {
                        replaceUserDto.setDisplayName(userBasicDo.getDisplayName());
                    }
                } else {
                    if (inputItem.getDisplayName().equalsIgnoreCase(InfrastructureConstants.ROOT_USER_DISPLAY_NAME)) {
                        replaceUserDto.setDisplayName(inputItem.getDisplayName() + "" + System.currentTimeMillis());
                    } else {
                        replaceUserDto.setDisplayName(inputItem.getDisplayName());
                    }
                }

                // account list
                // 取并集，一个用户的一个 account type 最多只能有一个 account
                Map<Long, Account> mergedAccountMap = new HashMap<>();
                if (!CollectionUtils.isEmpty(existingUserAccountDoList)) {
                    existingUserAccountDoList.forEach(existingUserAccountDo -> {
                        Account account = new Account();
                        account.setAccountTypeUid(existingUserAccountDo.getAccountTypeUid());
                        account.setAccountName(existingUserAccountDo.getName());
                        mergedAccountMap.put(existingUserAccountDo.getAccountTypeUid(), account);
                    });
                }
                for (UserInfo.AccountInfo accountInfo : accountInfoList) {
                    Account account = new Account();
                    account.setAccountTypeUid(accountInfo.getAccountTypeUid());
                    account.setAccountName(accountInfo.getAccountName());
                    mergedAccountMap.put(accountInfo.getAccountTypeUid(), account);
                }
                if (!CollectionUtils.isEmpty(mergedAccountMap)) {
                    replaceUserDto.setAccountList(new ArrayList<>(mergedAccountMap.values()));
                }

                // extended property list
                // 取并集
                Map<Long, ExtendedProperty> mergedExtendedPropertyMap = new HashMap<>();
                if (!CollectionUtils.isEmpty(existingUserExtendedPropertyDoList)) {
                    existingUserExtendedPropertyDoList.forEach(existingUserExtendedPropertyDo -> {
                        ExtendedProperty extendedProperty = new ExtendedProperty();
                        extendedProperty.setExtendedPropertyUid(existingUserExtendedPropertyDo.getExtendedPropertyUid());
                        extendedProperty.setExtendedPropertyValue(existingUserExtendedPropertyDo.getExtendedPropertyValue());

                        mergedExtendedPropertyMap.put(existingUserExtendedPropertyDo.getExtendedPropertyUid(),
                                extendedProperty);
                    });
                }
                if (!CollectionUtils.isEmpty(inputItem.getExtendedProperties())) {
                    for (UserInfo.ExtendedPropertyInfo extendedPropertyInfo : inputItem.getExtendedProperties()) {
                        if (!availableUserSchemaExtendedPropertyDoMap.containsKey(extendedPropertyInfo.getExtendedPropertyUid())) {
                            LOGGER.warn("illegal user info {}, unknown extended property uid {}, {}",
                                    inputItem.getDisplayName(), extendedPropertyInfo.getExtendedPropertyUid(),
                                    extendedPropertyInfo.getExtendedPropertyValue());
                            return;
                        }

                        ExtendedProperty extendedProperty = new ExtendedProperty();
                        extendedProperty.setExtendedPropertyUid(extendedPropertyInfo.getExtendedPropertyUid());
                        extendedProperty.setExtendedPropertyValue(extendedPropertyInfo.getExtendedPropertyValue());

                        mergedExtendedPropertyMap.put(extendedPropertyInfo.getExtendedPropertyUid(), extendedProperty);
                    }
                }
                if (!CollectionUtils.isEmpty(mergedExtendedPropertyMap)) {
                    replaceUserDto.setExtendedPropertyList(new ArrayList<>(mergedExtendedPropertyMap.values()));
                }

                // role uid list
                // 取并集
                List<Long> mergedRoleUidList = new LinkedList<>();
                if (existingUserRoleUidListMap.containsKey(foundUserUid)) {
                    mergedRoleUidList.addAll(existingUserRoleUidListMap.get(foundUserUid));
                }
                if (!CollectionUtils.isEmpty(inputItem.getRoleUidList())) {
                    for (Long roleUid : inputItem.getRoleUidList()) {
                        if (!availableRoleUidList.contains(roleUid)) {
                            LOGGER.warn("illegal user info {}, unknown role uid {}, {}",
                                    inputItem.getDisplayName(), roleUid);
                            return;
                        }
                        if (!mergedRoleUidList.contains(roleUid)) {
                            mergedRoleUidList.add(roleUid);
                        }
                    }
                }
                if (!CollectionUtils.isEmpty(mergedRoleUidList)) {
                    replaceUserDto.setRoleUidList(mergedRoleUidList);
                }

                // group uid list
                // 取并集
                List<Long> mergedGroupUidList = new LinkedList<>();
                if (existingUserGroupUidListMap.containsKey(foundUserUid)) {
                    mergedGroupUidList.addAll(existingUserGroupUidListMap.get(foundUserUid));
                }
                if (!CollectionUtils.isEmpty(inputItem.getGroupUidList())) {
                    for (Long groupUid : inputItem.getGroupUidList()) {
                        if (!availableGroupUidList.contains(groupUid)) {
                            LOGGER.warn("illegal user info {}, unknown group uid {}, {}",
                                    inputItem.getDisplayName(), groupUid);
                            return;
                        }
                        if (!mergedGroupUidList.contains(groupUid)) {
                            mergedGroupUidList.add(groupUid);
                        }
                    }
                }
                if (!CollectionUtils.isEmpty(mergedGroupUidList)) {
                    replaceUserDto.setGroupUidList(mergedGroupUidList);
                }

                //
                replaceUserDtoList.add(replaceUserDto);
            }
        });

        UserProfile operatingUserProfile = new UserProfile();
        operatingUserProfile.setUid(InfrastructureConstants.ROOT_USER_UID);
        operatingUserProfile.setDisplayName(InfrastructureConstants.ROOT_USER_DISPLAY_NAME);

        if (!CollectionUtils.isEmpty(createUserDtoList)) {
            this.userService.batchCreateUsers(createUserDtoList, operatingUserProfile);
        }

        if (!CollectionUtils.isEmpty(replaceUserDtoList)) {
            this.userService.batchReplaceUsers(replaceUserDtoList, operatingUserProfile);
        }
    }

}
