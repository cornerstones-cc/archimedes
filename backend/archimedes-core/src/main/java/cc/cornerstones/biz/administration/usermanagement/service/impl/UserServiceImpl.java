package cc.cornerstones.biz.administration.usermanagement.service.impl;

import cc.cornerstones.almond.constants.InfrastructureConstants;
import cc.cornerstones.almond.exceptions.*;
import cc.cornerstones.almond.types.AbcTuple3;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserBriefInformation;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.administration.usermanagement.dto.*;
import cc.cornerstones.biz.administration.usermanagement.entity.*;
import cc.cornerstones.biz.administration.usermanagement.persistence.*;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.administration.usermanagement.share.constants.UserTypeEnum;
import cc.cornerstones.biz.administration.usermanagement.share.types.*;
import cc.cornerstones.biz.settings.dto.UpdateUserCredentialDto;
import cc.cornerstones.biz.share.assembly.ResourceReferenceManager;
import cc.cornerstones.biz.share.event.*;
import cc.cornerstones.biz.share.types.ResourceReferenceHandler;
import com.google.common.eventbus.Subscribe;
import org.apache.commons.codec.digest.DigestUtils;
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

import javax.annotation.PostConstruct;
import javax.persistence.criteria.*;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private UserBasicRepository userBasicRepository;

    @Autowired
    private UserExtendedPropertyRepository userExtendedPropertyRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private AccountTypeRepository accountTypeRepository;

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private GroupRoleRepository groupRoleRepository;

    @Autowired
    private UserSchemaExtendedPropertyRepository userSchemaExtendedPropertyRepository;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public UserProfile getUserProfile(
            Long uid) throws AbcUndefinedException {
        if (uid == null) {
            throw new AbcResourceConflictException("uid should not be null");
        }

        // 一个约定
        if (uid.equals(0L)) {
            throw new AbcAuthenticationException("please sign in again");
        }

        if (uid.equals(InfrastructureConstants.ROOT_USER_UID)) {
            UserProfile userProfile = new UserProfile();
            userProfile.setUid(InfrastructureConstants.ROOT_USER_UID);
            userProfile.setDisplayName(InfrastructureConstants.ROOT_USER_DISPLAY_NAME);
            userProfile.setTrackingSerialNumber(UUID.randomUUID().toString());
            userProfile.setCreatedTimestamp(LocalDateTime.now());
            return userProfile;
        } else {
            UserBasicDo userBasicDo = this.userBasicRepository.findByUid(uid);
            if (userBasicDo == null) {
                throw new AbcResourceNotFoundException(String.format("%s::uid=%d", UserBasicDo.RESOURCE_SYMBOL, uid));
            }

            UserProfile userProfile = new UserProfile();
            userProfile.setUid(userBasicDo.getUid());
            userProfile.setDisplayName(userBasicDo.getDisplayName());
            userProfile.setTrackingSerialNumber(UUID.randomUUID().toString());
            userProfile.setCreatedTimestamp(LocalDateTime.now());
            return userProfile;
        }
    }

    @Override
    public UserProfile getUserProfile(
            Long accountTypeUid,
            String accountName) throws AbcUndefinedException {
        UserAccountDo userAccountDo = this.userAccountRepository.findByAccountTypeUidAndName(accountTypeUid,
                accountName);
        if (userAccountDo == null) {
            return null;
        }

        return getUserProfile(userAccountDo.getUserUid());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserDto createUser(
            CreateUserDto createUserDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        // 同一帐号类型帐号名不能重复
        for (Account account : createUserDto.getAccountList()) {
            boolean existsDuplicate =
                    this.userAccountRepository.existsByAccountTypeUidAndName(account.getAccountTypeUid(),
                            account.getAccountName());
            if (existsDuplicate) {
                throw new AbcResourceDuplicateException(String.format("%s::account_type_uid = %d, account_name = %s",
                        UserAccountDo.RESOURCE_SYMBOL,
                        account.getAccountTypeUid(), account.getAccountName()));
            }
        }

        //
        // Step 2, core-processing
        //

        //
        // Step 2.1, create user basic
        //
        UserBasicDo userBasicDo = new UserBasicDo();
        userBasicDo.setUid(this.idHelper.getNextDistributedId(UserBasicDo.RESOURCE_NAME));
        userBasicDo.setDisplayName(createUserDto.getDisplayName());
        userBasicDo.setEnabled(createUserDto.getEnabled());
        userBasicDo.setType(UserTypeEnum.PERSONAL);
        BaseDo.create(userBasicDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.userBasicRepository.save(userBasicDo);

        // Step 2.2, create user credential
        UserCredentialDo userCredentialDo = new UserCredentialDo();
        userCredentialDo.setUserUid(userBasicDo.getUid());
        userCredentialDo.setCredential(DigestUtils.sha256Hex(createUserDto.getPassword()));
        BaseDo.create(userCredentialDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.userCredentialRepository.save(userCredentialDo);

        //
        // Step 2.3, create user extended property(ies)
        //
        if (!CollectionUtils.isEmpty(createUserDto.getExtendedPropertyList())) {
            Map<Long, UserSchemaExtendedPropertyDo> uidAndUserSchemaExtendedPropertyDoMap = new HashMap<>();
            this.userSchemaExtendedPropertyRepository.findAll().forEach(userSchemaExtendedPropertyDo -> {
                uidAndUserSchemaExtendedPropertyDoMap.put(userSchemaExtendedPropertyDo.getUid(),
                        userSchemaExtendedPropertyDo);
            });

            // validate
            for (ExtendedProperty extendedProperty : createUserDto.getExtendedPropertyList()) {
                if (extendedProperty.getExtendedPropertyUid() == null
                        || ObjectUtils.isEmpty(extendedProperty.getExtendedPropertyValue())) {
                    throw new AbcIllegalParameterException("illegal extended property");
                }

                if (!uidAndUserSchemaExtendedPropertyDoMap.containsKey(extendedProperty.getExtendedPropertyUid())) {
                    throw new AbcIllegalParameterException("illegal extended property");
                }
            }

            List<UserExtendedPropertyDo> userExtendedPropertyDoList = new LinkedList<>();
            createUserDto.getExtendedPropertyList().forEach(extendedProperty -> {
                UserSchemaExtendedPropertyDo userSchemaExtendedPropertyDo =
                        uidAndUserSchemaExtendedPropertyDoMap.get(extendedProperty.getExtendedPropertyUid());

                String expectedExtendedPropertyValue =
                        transformExtendedPropertyValue(userSchemaExtendedPropertyDo, extendedProperty);

                if (expectedExtendedPropertyValue != null) {
                    UserExtendedPropertyDo userExtendedPropertyDo = new UserExtendedPropertyDo();
                    userExtendedPropertyDo.setUserUid(userBasicDo.getUid());
                    userExtendedPropertyDo.setExtendedPropertyUid(extendedProperty.getExtendedPropertyUid());
                    userExtendedPropertyDo.setExtendedPropertyValue(expectedExtendedPropertyValue);
                    BaseDo.create(userExtendedPropertyDo, operatingUserProfile.getUid(), LocalDateTime.now());

                    userExtendedPropertyDoList.add(userExtendedPropertyDo);
                }
            });

            if (!CollectionUtils.isEmpty(userExtendedPropertyDoList)) {
                this.userExtendedPropertyRepository.saveAll(userExtendedPropertyDoList);
            }
        }

        //
        // Step 2.4, create user account(s)
        //
        if (!CollectionUtils.isEmpty(createUserDto.getAccountList())) {
            Map<Long, AccountTypeDo> accountTypeDoMap = new HashMap<>();
            this.accountTypeRepository.findAll().forEach(accountTypeDo -> {
                accountTypeDoMap.put(accountTypeDo.getUid(), accountTypeDo);
            });

            // validate
            for (Account account : createUserDto.getAccountList()) {
                if (account.getAccountTypeUid() == null
                        || ObjectUtils.isEmpty(account.getAccountName())) {
                    throw new AbcIllegalParameterException("illegal account");
                }

                if (!accountTypeDoMap.containsKey(account.getAccountTypeUid())) {
                    throw new AbcIllegalParameterException("illegal account");
                }
            }

            List<UserAccountDo> userAccountDoList = new LinkedList<>();
            createUserDto.getAccountList().forEach(account -> {
                UserAccountDo userAccountDo = new UserAccountDo();
                userAccountDo.setUserUid(userBasicDo.getUid());
                userAccountDo.setAccountTypeUid(account.getAccountTypeUid());
                userAccountDo.setName(account.getAccountName());
                BaseDo.create(userAccountDo, operatingUserProfile.getUid(), LocalDateTime.now());
                userAccountDoList.add(userAccountDo);
            });

            if (!CollectionUtils.isEmpty(userAccountDoList)) {
                this.userAccountRepository.saveAll(userAccountDoList);
            }
        }

        //
        // Step 2.5, create user role(s)
        //
        if (!CollectionUtils.isEmpty(createUserDto.getRoleUidList())) {
            Map<Long, RoleDo> roleDoMap = new HashMap<>();
            this.roleRepository.findAll().forEach(roleDo -> {
                roleDoMap.put(roleDo.getUid(), roleDo);
            });

            // validate
            for (Long roleUid : createUserDto.getRoleUidList()) {
                if (roleUid == null) {
                    throw new AbcIllegalParameterException("illegal role");
                }

                if (!roleDoMap.containsKey(roleUid)) {
                    throw new AbcIllegalParameterException("illegal role");
                }
            }

            List<UserRoleDo> userRoleDoList = new LinkedList<>();

            List<Long> distinctUserRoleUidList =
                    createUserDto.getRoleUidList().stream().distinct().collect(Collectors.toList());
            distinctUserRoleUidList.forEach(roleUid -> {
                UserRoleDo userRoleDo = new UserRoleDo();
                userRoleDo.setUserUid(userBasicDo.getUid());
                userRoleDo.setRoleUid(roleUid);
                BaseDo.create(userRoleDo, operatingUserProfile.getUid(), LocalDateTime.now());
                userRoleDoList.add(userRoleDo);
            });

            if (!CollectionUtils.isEmpty(userRoleDoList)) {
                this.userRoleRepository.saveAll(userRoleDoList);
            }
        }

        //
        // Step 2.6, create user group(s)
        //

        if (!CollectionUtils.isEmpty(createUserDto.getGroupUidList())) {
            Map<Long, GroupDo> groupDoMap = new HashMap<>();
            this.groupRepository.findAll().forEach(groupDo -> {
                groupDoMap.put(groupDo.getUid(), groupDo);
            });

            // validate
            for (Long groupUid : createUserDto.getGroupUidList()) {
                if (groupUid == null) {
                    throw new AbcIllegalParameterException("illegal group");
                }

                if (!groupDoMap.containsKey(groupUid)) {
                    throw new AbcIllegalParameterException("illegal group");
                }
            }

            List<UserGroupDo> userGroupDoList = new LinkedList<>();

            List<Long> distinctUserGroupUidList =
                    createUserDto.getGroupUidList().stream().distinct().collect(Collectors.toList());
            distinctUserGroupUidList.forEach(groupUid -> {
                UserGroupDo userGroupDo = new UserGroupDo();
                userGroupDo.setUserUid(userBasicDo.getUid());
                userGroupDo.setGroupUid(groupUid);
                BaseDo.create(userGroupDo, operatingUserProfile.getUid(), LocalDateTime.now());
                userGroupDoList.add(userGroupDo);
            });

            if (!CollectionUtils.isEmpty(userGroupDoList)) {
                this.userGroupRepository.saveAll(userGroupDoList);
            }
        }

        //
        // Step 3, post-processing
        //

        if (!CollectionUtils.isEmpty(createUserDto.getRoleUidList())) {
            UserRoleChangedEvent userRoleChangedEvent = new UserRoleChangedEvent();
            userRoleChangedEvent.setUserUid(userBasicDo.getUid());
            userRoleChangedEvent.setNewRoleUidList(createUserDto.getRoleUidList());
            userRoleChangedEvent.setOperatingUserProfile(operatingUserProfile);
            this.eventBusManager.send(userRoleChangedEvent);
        }

        if (!CollectionUtils.isEmpty(createUserDto.getGroupUidList())) {
            UserGroupChangedEvent userGroupChangedEvent = new UserGroupChangedEvent();
            userGroupChangedEvent.setUserUid(userBasicDo.getUid());
            userGroupChangedEvent.setNewGroupUidList(createUserDto.getGroupUidList());
            userGroupChangedEvent.setOperatingUserProfile(operatingUserProfile);
            this.eventBusManager.send(userGroupChangedEvent);
        }

        return getUser(userBasicDo.getUid(), operatingUserProfile);
    }

    private String transformExtendedPropertyValue(
            UserSchemaExtendedPropertyDo userSchemaExtendedPropertyDo,
            ExtendedProperty extendedProperty) {
        String expectedExtendedPropertyValue = null;

        switch (userSchemaExtendedPropertyDo.getType()) {
            case BOOLEAN: {
                if (extendedProperty.getExtendedPropertyValue() instanceof Boolean) {
                    Boolean transformed = (Boolean) extendedProperty.getExtendedPropertyValue();
                    if (Boolean.TRUE.equals(transformed)) {
                        expectedExtendedPropertyValue = "1";
                    } else {
                        expectedExtendedPropertyValue = "0";
                    }
                } else if (extendedProperty.getExtendedPropertyValue() instanceof Boolean) {
                    String transformed = (String) extendedProperty.getExtendedPropertyValue();
                    if (transformed.equalsIgnoreCase("true")) {
                        expectedExtendedPropertyValue = "1";
                    } else if (transformed.equalsIgnoreCase("false")) {
                        expectedExtendedPropertyValue = "0";
                    }
                } else if (extendedProperty.getExtendedPropertyValue() instanceof Integer) {
                    String transformed = (String) extendedProperty.getExtendedPropertyValue();
                    if (transformed.equalsIgnoreCase("1")) {
                        expectedExtendedPropertyValue = "1";
                    } else if (transformed.equalsIgnoreCase("0")) {
                        expectedExtendedPropertyValue = "0";
                    }
                }
            }
            break;
            case TINYINT:
            case SMALLINT:
            case MEDIUMINT:
            case INT:
            case LONG:
            case DECIMAL: {
                expectedExtendedPropertyValue = String.valueOf(extendedProperty.getExtendedPropertyValue());
            }
            break;
            case DATE: {
                if (extendedProperty.getExtendedPropertyValue() instanceof String) {
                    String transformed = (String) extendedProperty.getExtendedPropertyValue();
                    expectedExtendedPropertyValue = transformed;
                } else if (extendedProperty.getExtendedPropertyValue() instanceof java.time.LocalDate) {
                    java.time.LocalDate transformed = (java.time.LocalDate) extendedProperty.getExtendedPropertyValue();
                    expectedExtendedPropertyValue = transformed.format(this.dateFormatter);
                } else if (extendedProperty.getExtendedPropertyValue() instanceof java.time.LocalDateTime) {
                    java.time.LocalDateTime transformed = (java.time.LocalDateTime) extendedProperty.getExtendedPropertyValue();
                    expectedExtendedPropertyValue = transformed.format(this.dateFormatter);
                }
            }
            break;
            case DATETIME:
            case TIMESTAMP: {
                if (extendedProperty.getExtendedPropertyValue() instanceof String) {
                    String transformed = (String) extendedProperty.getExtendedPropertyValue();
                    expectedExtendedPropertyValue = transformed;
                } else if (extendedProperty.getExtendedPropertyValue() instanceof java.time.LocalDate) {
                    java.time.LocalDate transformed = (java.time.LocalDate) extendedProperty.getExtendedPropertyValue();
                    expectedExtendedPropertyValue = transformed.format(this.dateTimeFormatter);
                } else if (extendedProperty.getExtendedPropertyValue() instanceof java.time.LocalDateTime) {
                    java.time.LocalDateTime transformed = (java.time.LocalDateTime) extendedProperty.getExtendedPropertyValue();
                    expectedExtendedPropertyValue = transformed.format(this.dateTimeFormatter);
                }
            }
            break;
            case TIME: {
                if (extendedProperty.getExtendedPropertyValue() instanceof String) {
                    String transformed = (String) extendedProperty.getExtendedPropertyValue();
                    expectedExtendedPropertyValue = transformed;
                } else if (extendedProperty.getExtendedPropertyValue() instanceof java.time.LocalTime) {
                    java.time.LocalTime transformed = (java.time.LocalTime) extendedProperty.getExtendedPropertyValue();
                    expectedExtendedPropertyValue = transformed.format(this.timeFormatter);
                }
            }
            break;
            default:
                expectedExtendedPropertyValue = String.valueOf(extendedProperty.getExtendedPropertyValue());
                break;
        }

        return expectedExtendedPropertyValue;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void replaceUser(
            Long uid,
            ReplaceUserDto replaceUserDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        UserBasicDo userBasicDo = this.userBasicRepository.findByUid(uid);
        if (userBasicDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", UserBasicDo.RESOURCE_SYMBOL, uid));
        }

        // 同一帐号类型帐号名不能重复
        List<UserAccountDo> userAccountDoList = this.userAccountRepository.findByUserUid(userBasicDo.getUid());
        Map<Long, String> existingAccountTypeUidAndAccountNameMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(userAccountDoList)) {
            userAccountDoList.forEach(userAccountDo -> {
                existingAccountTypeUidAndAccountNameMap.put(userAccountDo.getAccountTypeUid(), userAccountDo.getName());
            });
        }

        for (Account account : replaceUserDto.getAccountList()) {
            String existingAccountName = existingAccountTypeUidAndAccountNameMap.get(account.getAccountTypeUid());

            if (existingAccountName != null
                    && !existingAccountName.equalsIgnoreCase(account.getAccountName())) {
                boolean existsDuplicate =
                        this.userAccountRepository.existsByAccountTypeUidAndName(account.getAccountTypeUid(),
                                account.getAccountName());
                if (existsDuplicate) {
                    throw new AbcResourceDuplicateException(String.format("%s::account_type_uid = %d, account_name = %s",
                            UserAccountDo.RESOURCE_SYMBOL,
                            account.getAccountTypeUid(), account.getAccountName()));
                }
            }
        }

        //
        // Step 2, core-processing
        //

        boolean requiredToUpdateUser = false;

        //
        // Step 2.1, replace user extended property(ies)
        //
        {
            // all user schema extended property(ies)
            Map<Long, UserSchemaExtendedPropertyDo> uidAndUserSchemaExtendedPropertyDoMap = new HashMap<>();
            this.userSchemaExtendedPropertyRepository.findAll().forEach(userSchemaExtendedPropertyDo -> {
                uidAndUserSchemaExtendedPropertyDoMap.put(userSchemaExtendedPropertyDo.getUid(),
                        userSchemaExtendedPropertyDo);
            });

            if (!CollectionUtils.isEmpty(replaceUserDto.getExtendedPropertyList())) {
                // validate
                for (ExtendedProperty extendedProperty : replaceUserDto.getExtendedPropertyList()) {
                    if (extendedProperty.getExtendedPropertyUid() == null
                            || ObjectUtils.isEmpty(extendedProperty.getExtendedPropertyValue())) {
                        throw new AbcIllegalParameterException("illegal extended property");
                    }

                    if (!uidAndUserSchemaExtendedPropertyDoMap.containsKey(extendedProperty.getExtendedPropertyUid())) {
                        throw new AbcIllegalParameterException("illegal extended property");
                    }
                }
            }

            // existing user extended property(ies)
            List<UserExtendedPropertyDo> existingItemDoList =
                    this.userExtendedPropertyRepository.findByUserUid(userBasicDo.getUid());
            Map<Long, UserExtendedPropertyDo> existingItemDoMap = new HashMap<>();
            if (!CollectionUtils.isEmpty(existingItemDoList)) {
                existingItemDoList.forEach(existingItemDo -> {
                    existingItemDoMap.put(existingItemDo.getExtendedPropertyUid(), existingItemDo);
                });
            }

            // input extended property(ies)
            Map<Long, ExtendedProperty> inputItemMap = new HashMap<>();
            if (!CollectionUtils.isEmpty(replaceUserDto.getExtendedPropertyList())) {
                replaceUserDto.getExtendedPropertyList().forEach(inputItem -> {
                    inputItemMap.put(inputItem.getExtendedPropertyUid(),
                            inputItem);
                });
            }

            // compare
            List<UserExtendedPropertyDo> toAddItemDoList = new LinkedList<>();
            List<UserExtendedPropertyDo> toUpdateItemDoList = new LinkedList<>();
            List<UserExtendedPropertyDo> toDeleteItemDoList = new LinkedList<>();
            inputItemMap.forEach((key, inputItem) -> {
                UserSchemaExtendedPropertyDo userSchemaExtendedPropertyDo =
                        uidAndUserSchemaExtendedPropertyDoMap.get(key);

                String expectedExtendedPropertyValue = transformExtendedPropertyValue(userSchemaExtendedPropertyDo, inputItem);

                if (expectedExtendedPropertyValue != null) {
                    if (existingItemDoMap.containsKey(key)) {
                        // found in existing, found in input
                        UserExtendedPropertyDo existingItemDo =
                                existingItemDoMap.get(key);

                        boolean requiredToUpdate = false;
                        if (!existingItemDo.getExtendedPropertyValue().equals(expectedExtendedPropertyValue)) {
                            existingItemDo.setExtendedPropertyValue(expectedExtendedPropertyValue);
                            requiredToUpdate = true;
                        }
                        if (requiredToUpdate) {
                            BaseDo.update(existingItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                            toUpdateItemDoList.add(existingItemDo);
                        }
                    } else {
                        // not found in existing, found in input
                        UserExtendedPropertyDo newItemDo = new UserExtendedPropertyDo();
                        newItemDo.setUserUid(userBasicDo.getUid());
                        newItemDo.setExtendedPropertyUid(inputItem.getExtendedPropertyUid());
                        newItemDo.setExtendedPropertyValue(expectedExtendedPropertyValue);
                        BaseDo.create(newItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                        toAddItemDoList.add(newItemDo);
                    }

                }
            });

            existingItemDoMap.forEach((key, existingItemDo) -> {
                if (!inputItemMap.containsKey(key)) {
                    existingItemDo.setDeleted(Boolean.TRUE);
                    BaseDo.update(existingItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    toDeleteItemDoList.add(existingItemDo);
                }
            });

            if (!CollectionUtils.isEmpty(toAddItemDoList)) {
                this.userExtendedPropertyRepository.saveAll(toAddItemDoList);

                requiredToUpdateUser = true;
            }
            if (!CollectionUtils.isEmpty(toUpdateItemDoList)) {
                this.userExtendedPropertyRepository.saveAll(toUpdateItemDoList);

                requiredToUpdateUser = true;
            }
            if (!CollectionUtils.isEmpty(toDeleteItemDoList)) {
                this.userExtendedPropertyRepository.saveAll(toDeleteItemDoList);

                requiredToUpdateUser = true;
            }

        }

        //
        // Step 2.2, replace user account(s)
        //
        {
            Map<Long, AccountTypeDo> accountTypeDoMap = new HashMap<>();
            this.accountTypeRepository.findAll().forEach(accountTypeDo -> {
                accountTypeDoMap.put(accountTypeDo.getUid(), accountTypeDo);
            });

            if (!CollectionUtils.isEmpty(replaceUserDto.getAccountList())) {
                // validate
                for (Account account : replaceUserDto.getAccountList()) {
                    if (account.getAccountTypeUid() == null
                            || ObjectUtils.isEmpty(account.getAccountName())) {
                        throw new AbcIllegalParameterException("illegal account");
                    }

                    if (!accountTypeDoMap.containsKey(account.getAccountTypeUid())) {
                        throw new AbcIllegalParameterException("illegal account");
                    }
                }
            }

            // existing accounts
            List<UserAccountDo> existingItemDoList = this.userAccountRepository.findByUserUid(userBasicDo.getUid());
            Map<Long, UserAccountDo> existingItemDoMap = new HashMap<>();
            if (!CollectionUtils.isEmpty(existingItemDoList)) {
                existingItemDoList.forEach(existingItemDo -> {
                    existingItemDoMap.put(existingItemDo.getAccountTypeUid(), existingItemDo);
                });
            }

            // input accounts
            Map<Long, Account> inputItemMap = new HashMap<>();
            if (!CollectionUtils.isEmpty(replaceUserDto.getAccountList())) {
                replaceUserDto.getAccountList().forEach(account -> {
                    inputItemMap.put(account.getAccountTypeUid(), account);
                });
            }

            // compare
            List<UserAccountDo> toAddItemDoList = new LinkedList<>();
            List<UserAccountDo> toUpdateItemDoList = new LinkedList<>();
            List<UserAccountDo> toDeleteItemDoList = new LinkedList<>();

            inputItemMap.forEach((key, inputItem) -> {
                if (existingItemDoMap.containsKey(key)) {
                    // found in existing, found in input
                    UserAccountDo existingItemDo =
                            existingItemDoMap.get(key);

                    boolean requiredToUpdate = false;
                    if (!existingItemDo.getName().equalsIgnoreCase(inputItem.getAccountName())) {
                        existingItemDo.setName(inputItem.getAccountName());
                        requiredToUpdate = true;
                    }
                    if (requiredToUpdate) {
                        BaseDo.update(existingItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                        toUpdateItemDoList.add(existingItemDo);
                    }

                } else {
                    // not found in existing, found in input
                    UserAccountDo newItemDo = new UserAccountDo();
                    newItemDo.setUserUid(userBasicDo.getUid());
                    newItemDo.setAccountTypeUid(key);
                    newItemDo.setName(inputItem.getAccountName());
                    BaseDo.create(newItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    toAddItemDoList.add(newItemDo);
                }
            });

            existingItemDoMap.forEach((key, existingItemDo) -> {
                if (!inputItemMap.containsKey(key)) {
                    existingItemDo.setDeleted(Boolean.TRUE);
                    BaseDo.update(existingItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    toDeleteItemDoList.add(existingItemDo);
                }
            });

            if (!CollectionUtils.isEmpty(toAddItemDoList)) {
                this.userAccountRepository.saveAll(toAddItemDoList);

                requiredToUpdateUser = true;
            }
            if (!CollectionUtils.isEmpty(toUpdateItemDoList)) {
                this.userAccountRepository.saveAll(toUpdateItemDoList);

                requiredToUpdateUser = true;
            }
            if (!CollectionUtils.isEmpty(toDeleteItemDoList)) {
                this.userAccountRepository.saveAll(toDeleteItemDoList);

                requiredToUpdateUser = true;
            }
        }

        //
        // Step 2.3, replace user role(s)
        //
        boolean requiredToUpdateUserRole = false;
        {
            Map<Long, RoleDo> roleDoMap = new HashMap<>();
            this.roleRepository.findAll().forEach(roleDo -> {
                roleDoMap.put(roleDo.getUid(), roleDo);
            });

            if (!CollectionUtils.isEmpty(replaceUserDto.getRoleUidList())) {
                // validate
                for (Long roleUid : replaceUserDto.getRoleUidList()) {
                    if (roleUid == null) {
                        throw new AbcIllegalParameterException("illegal role");
                    }

                    if (!roleDoMap.containsKey(roleUid)) {
                        throw new AbcIllegalParameterException("illegal role");
                    }
                }
            }

            // input user role(s)
            List<Long> inputItemList = new LinkedList<>();

            if (!CollectionUtils.isEmpty(replaceUserDto.getRoleUidList())) {
                List<Long> distinctUserRoleUidList =
                        replaceUserDto.getRoleUidList().stream().distinct().collect(Collectors.toList());
                inputItemList.addAll(distinctUserRoleUidList);
            }

            // existing user role(s)
            List<UserRoleDo> existingItemDoList = this.userRoleRepository.findByUserUid(userBasicDo.getUid());
            Map<Long, UserRoleDo> existingItemDoMap = new HashMap<>();
            if (!CollectionUtils.isEmpty(existingItemDoList)) {
                existingItemDoList.forEach(existingItemDo -> {
                    existingItemDoMap.put(existingItemDo.getRoleUid(), existingItemDo);
                });
            }

            // compare
            List<UserRoleDo> toAddItemDoList = new LinkedList<>();
            List<UserRoleDo> toUpdateItemDoList = new LinkedList<>();
            List<UserRoleDo> toDeleteItemDoList = new LinkedList<>();
            inputItemList.forEach(inputItem -> {
                if (existingItemDoMap.containsKey(inputItem)) {
                    // found in existing, found in input
                    // Do nothing
                } else {
                    // not found in existing, found in input
                    UserRoleDo newItemDo = new UserRoleDo();
                    newItemDo.setUserUid(userBasicDo.getUid());
                    newItemDo.setRoleUid(inputItem);
                    BaseDo.create(newItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    toAddItemDoList.add(newItemDo);
                }
            });

            existingItemDoMap.forEach((key, existingItemDo) -> {
                if (!inputItemList.contains(key)) {
                    existingItemDo.setDeleted(Boolean.TRUE);
                    BaseDo.update(existingItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    toDeleteItemDoList.add(existingItemDo);
                }
            });

            if (!CollectionUtils.isEmpty(toAddItemDoList)) {
                this.userRoleRepository.saveAll(toAddItemDoList);

                requiredToUpdateUser = true;
                requiredToUpdateUserRole = true;
            }
            if (!CollectionUtils.isEmpty(toUpdateItemDoList)) {
                this.userRoleRepository.saveAll(toUpdateItemDoList);

                requiredToUpdateUser = true;
                requiredToUpdateUserRole = true;
            }
            if (!CollectionUtils.isEmpty(toDeleteItemDoList)) {
                this.userRoleRepository.saveAll(toDeleteItemDoList);

                requiredToUpdateUser = true;
                requiredToUpdateUserRole = true;
            }
        }

        //
        // Step 2.4, replace user group(s)
        //
        boolean requiredToUpdateUserGroup = false;
        {
            Map<Long, GroupDo> groupDoMap = new HashMap<>();
            this.groupRepository.findAll().forEach(groupDo -> {
                groupDoMap.put(groupDo.getUid(), groupDo);
            });

            if (!CollectionUtils.isEmpty(replaceUserDto.getGroupUidList())) {
                // validate
                for (Long groupUid : replaceUserDto.getGroupUidList()) {
                    if (groupUid == null) {
                        throw new AbcIllegalParameterException("illegal group");
                    }

                    if (!groupDoMap.containsKey(groupUid)) {
                        throw new AbcIllegalParameterException("illegal group");
                    }
                }
            }

            // input user group(s)
            List<Long> inputItemList = new LinkedList<>();

            if (!CollectionUtils.isEmpty(replaceUserDto.getGroupUidList())) {
                List<Long> distinctUserGroupUidList =
                        replaceUserDto.getGroupUidList().stream().distinct().collect(Collectors.toList());
                inputItemList.addAll(distinctUserGroupUidList);
            }

            // existing user group(s)
            List<UserGroupDo> existingItemDoList = this.userGroupRepository.findByUserUid(userBasicDo.getUid());
            Map<Long, UserGroupDo> existingItemDoMap = new HashMap<>();
            if (!CollectionUtils.isEmpty(existingItemDoList)) {
                existingItemDoList.forEach(existingItemDo -> {
                    existingItemDoMap.put(existingItemDo.getGroupUid(), existingItemDo);
                });
            }

            // compare
            List<UserGroupDo> toAddItemDoList = new LinkedList<>();
            List<UserGroupDo> toUpdateItemDoList = new LinkedList<>();
            List<UserGroupDo> toDeleteItemDoList = new LinkedList<>();

            inputItemList.forEach(inputItem -> {
                if (existingItemDoMap.containsKey(inputItem)) {
                    // found in existing, found in input
                    // Do nothing
                } else {
                    // not found in existing, found in input
                    UserGroupDo newItemDo = new UserGroupDo();
                    newItemDo.setUserUid(userBasicDo.getUid());
                    newItemDo.setGroupUid(inputItem);
                    BaseDo.create(newItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    toAddItemDoList.add(newItemDo);
                }
            });

            existingItemDoMap.forEach((key, existingItemDo) -> {
                if (!inputItemList.contains(key)) {
                    existingItemDo.setDeleted(Boolean.TRUE);
                    BaseDo.update(existingItemDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    toDeleteItemDoList.add(existingItemDo);
                }
            });

            if (!CollectionUtils.isEmpty(toAddItemDoList)) {
                this.userGroupRepository.saveAll(toAddItemDoList);

                requiredToUpdateUser = true;
                requiredToUpdateUserGroup = true;
            }
            if (!CollectionUtils.isEmpty(toUpdateItemDoList)) {
                this.userGroupRepository.saveAll(toUpdateItemDoList);

                requiredToUpdateUser = true;
                requiredToUpdateUserGroup = true;
            }
            if (!CollectionUtils.isEmpty(toDeleteItemDoList)) {
                this.userGroupRepository.saveAll(toDeleteItemDoList);

                requiredToUpdateUser = true;
                requiredToUpdateUserGroup = true;
            }
        }

        //
        // Step 2.5, replace user basic
        //
        if (!replaceUserDto.getDisplayName().equalsIgnoreCase(userBasicDo.getDisplayName())) {
            userBasicDo.setDisplayName(replaceUserDto.getDisplayName());
            requiredToUpdateUser = true;
        }
        if (!replaceUserDto.getEnabled().equals(userBasicDo.getEnabled())) {
            userBasicDo.setEnabled(replaceUserDto.getEnabled());
            requiredToUpdateUser = true;
        }

        if (requiredToUpdateUser) {
            BaseDo.update(userBasicDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.userBasicRepository.save(userBasicDo);
        }

        //
        // Step 3, post-processing
        //

        if (requiredToUpdateUserRole) {
            UserRoleChangedEvent userRoleChangedEvent = new UserRoleChangedEvent();
            userRoleChangedEvent.setUserUid(userBasicDo.getUid());
            userRoleChangedEvent.setNewRoleUidList(replaceUserDto.getRoleUidList());
            userRoleChangedEvent.setOperatingUserProfile(operatingUserProfile);
            this.eventBusManager.send(userRoleChangedEvent);
        }

        if (requiredToUpdateUserGroup) {
            UserGroupChangedEvent userGroupChangedEvent = new UserGroupChangedEvent();
            userGroupChangedEvent.setUserUid(userBasicDo.getUid());
            userGroupChangedEvent.setNewGroupUidList(replaceUserDto.getGroupUidList());
            userGroupChangedEvent.setOperatingUserProfile(operatingUserProfile);
            this.eventBusManager.send(userGroupChangedEvent);
        }
    }

    @Override
    public List<String> listAllReferencesToUser(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return null;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteUser(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        UserBasicDo userBasicDo = this.userBasicRepository.findByUid(uid);
        if (userBasicDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", UserBasicDo.RESOURCE_SYMBOL, uid));
        }

        //
        // Step 2, core-processing
        //
        this.userExtendedPropertyRepository.deleteByUserUid(uid);
        this.userAccountRepository.deleteByUserUid(uid);
        this.userRoleRepository.deleteByUserUid(uid);
        this.userGroupRepository.deleteByUserUid(uid);

        userBasicDo.setDeleted(Boolean.TRUE);
        BaseDo.update(userBasicDo, operatingUserProfile.getUid(), LocalDateTime.now());

        //
        // Step 3, post-processing
        //
        UserDeletedEvent userDeletedEvent = new UserDeletedEvent();
        userDeletedEvent.setUid(uid);
        userDeletedEvent.setOperatingUserProfile(operatingUserProfile);
        this.eventBusManager.send(userDeletedEvent);
    }

    @Override
    public UserDto getUser(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        UserBasicDo userBasicDo = this.userBasicRepository.findByUid(uid);
        if (userBasicDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", UserBasicDo.RESOURCE_SYMBOL, uid));
        }

        //
        // Step 2, core-processing
        //

        // user account(s)
        List<Account> accountList = new LinkedList<>();
        List<UserAccountDo> userAccountDoList = this.userAccountRepository.findByUserUid(uid);
        if (!CollectionUtils.isEmpty(userAccountDoList)) {
            Map<Long, AccountTypeDo> accountTypeDoMap = new HashMap<>();
            this.accountTypeRepository.findAll().forEach(accountTypeDo -> {
                accountTypeDoMap.put(accountTypeDo.getUid(),
                        accountTypeDo);
            });

            userAccountDoList.forEach(userAccountDo -> {
                if (accountTypeDoMap.containsKey(userAccountDo.getAccountTypeUid())) {
                    Account account = new Account();
                    account.setAccountTypeUid(userAccountDo.getAccountTypeUid());
                    account.setAccountTypeName(accountTypeDoMap.get(userAccountDo.getAccountTypeUid()).getName());
                    account.setAccountName(userAccountDo.getName());

                    accountList.add(account);
                }
            });
        }

        // user extended property(ies)
        List<ExtendedProperty> extendedPropertyList = new LinkedList<>();
        List<UserExtendedPropertyDo> userExtendedPropertyDoList =
                this.userExtendedPropertyRepository.findByUserUid(uid);
        if (!CollectionUtils.isEmpty(userExtendedPropertyDoList)) {
            Map<Long, UserSchemaExtendedPropertyDo> userSchemaExtendedPropertyDoMap = new HashMap<>();
            this.userSchemaExtendedPropertyRepository.findAll().forEach(userSchemaExtendedPropertyDo -> {
                userSchemaExtendedPropertyDoMap.put(userSchemaExtendedPropertyDo.getUid(),
                        userSchemaExtendedPropertyDo);
            });

            userExtendedPropertyDoList.forEach(userExtendedPropertyDo -> {
                if (userSchemaExtendedPropertyDoMap.containsKey(userExtendedPropertyDo.getExtendedPropertyUid())) {
                    ExtendedProperty extendedProperty = new ExtendedProperty();
                    extendedProperty.setExtendedPropertyUid(userExtendedPropertyDo.getExtendedPropertyUid());
                    extendedProperty.setExtendedPropertyName(
                            userSchemaExtendedPropertyDoMap.get(userExtendedPropertyDo.getExtendedPropertyUid()).getName());
                    extendedProperty.setExtendedPropertyValue(userExtendedPropertyDo.getExtendedPropertyValue());

                    extendedPropertyList.add(extendedProperty);
                }
            });
        }

        // user role(s)
        List<Role> roleList = new LinkedList<>();
        List<UserRoleDo> userRoleDoList = this.userRoleRepository.findByUserUid(uid);
        if (!CollectionUtils.isEmpty(userRoleDoList)) {
            Map<Long, RoleDo> roleDoMap = new HashMap<>();
            this.roleRepository.findAll().forEach(roleDo -> {
                roleDoMap.put(roleDo.getUid(),
                        roleDo);
            });

            userRoleDoList.forEach(userRoleDo -> {
                if (roleDoMap.containsKey(userRoleDo.getRoleUid())) {
                    Role role = new Role();
                    role.setUid(userRoleDo.getRoleUid());
                    role.setName(roleDoMap.get(userRoleDo.getRoleUid()).getName());

                    roleList.add(role);
                }
            });
        }

        // user group(s)
        List<Group> groupList = new LinkedList<>();
        List<UserGroupDo> userGroupDoList = this.userGroupRepository.findByUserUid(uid);
        if (!CollectionUtils.isEmpty(userGroupDoList)) {
            Map<Long, GroupDo> groupDoMap = new HashMap<>();
            this.groupRepository.findAll().forEach(groupDo -> {
                groupDoMap.put(groupDo.getUid(),
                        groupDo);
            });

            userGroupDoList.forEach(userGroupDo -> {
                if (groupDoMap.containsKey(userGroupDo.getGroupUid())) {
                    Group group = new Group();
                    group.setUid(userGroupDo.getGroupUid());
                    group.setName(groupDoMap.get(userGroupDo.getGroupUid()).getName());

                    groupList.add(group);
                }
            });
        }

        //
        // Step 3, post-processing
        //
        UserDto userDto = new UserDto();
        BeanUtils.copyProperties(userBasicDo, userDto);
        userDto.setExtendedPropertyList(extendedPropertyList);
        userDto.setAccountList(accountList);
        userDto.setRoleList(roleList);
        userDto.setGroupList(groupList);
        return userDto;
    }

    @Override
    public UserDto getUser(
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Long uid = operatingUserProfile.getUid();

        return getUser(uid, operatingUserProfile);
    }

    @Override
    public UserDto getUser(
            Long uid) throws AbcUndefinedException {
        UserProfile operatingUserProfile = new UserProfile();
        operatingUserProfile.setUid(InfrastructureConstants.ROOT_USER_UID);
        operatingUserProfile.setDisplayName(InfrastructureConstants.ROOT_USER_DISPLAY_NAME);
        return getUser(uid, operatingUserProfile);
    }

    @Override
    public UserDto getUser(
            Long accountTypeUid,
            String accountName) throws AbcUndefinedException {
        UserAccountDo userAccountDo = this.userAccountRepository.findByAccountTypeUidAndName(accountTypeUid,
                accountName);
        if (userAccountDo == null) {
            throw new AbcResourceNotFoundException("the account does not exist");
        }

        return getUser(userAccountDo.getUserUid());
    }

    @Override
    public UserSimplifiedDto getUserSimplified(
            Long uid) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        UserBasicDo userBasicDo = this.userBasicRepository.findByUid(uid);
        if (userBasicDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", UserBasicDo.RESOURCE_SYMBOL, uid));
        }

        //
        // Step 2, core-processing
        //

        // user extended property(ies)
        List<ExtendedProperty> extendedPropertyList = new LinkedList<>();
        List<UserExtendedPropertyDo> userExtendedPropertyDoList =
                this.userExtendedPropertyRepository.findByUserUid(uid);
        if (!CollectionUtils.isEmpty(userExtendedPropertyDoList)) {
            Map<Long, UserSchemaExtendedPropertyDo> userSchemaExtendedPropertyDoMap = new HashMap<>();
            this.userSchemaExtendedPropertyRepository.findAll().forEach(userSchemaExtendedPropertyDo -> {
                userSchemaExtendedPropertyDoMap.put(userSchemaExtendedPropertyDo.getUid(),
                        userSchemaExtendedPropertyDo);
            });

            userExtendedPropertyDoList.forEach(userExtendedPropertyDo -> {
                if (userSchemaExtendedPropertyDoMap.containsKey(userExtendedPropertyDo.getExtendedPropertyUid())) {
                    ExtendedProperty extendedProperty = new ExtendedProperty();
                    extendedProperty.setExtendedPropertyUid(userExtendedPropertyDo.getExtendedPropertyUid());
                    extendedProperty.setExtendedPropertyName(
                            userSchemaExtendedPropertyDoMap.get(userExtendedPropertyDo.getExtendedPropertyUid()).getName());
                    extendedProperty.setExtendedPropertyValue(userExtendedPropertyDo.getExtendedPropertyValue());

                    extendedPropertyList.add(extendedProperty);
                }
            });
        }

        // user account(s)
        List<Account> accountList = new LinkedList<>();
        List<UserAccountDo> userAccountDoList = this.userAccountRepository.findByUserUid(uid);
        if (!CollectionUtils.isEmpty(userAccountDoList)) {
            Map<Long, AccountTypeDo> accountTypeDoMap = new HashMap<>();
            this.accountTypeRepository.findAll().forEach(accountTypeDo -> {
                accountTypeDoMap.put(accountTypeDo.getUid(),
                        accountTypeDo);
            });

            userAccountDoList.forEach(userAccountDo -> {
                if (accountTypeDoMap.containsKey(userAccountDo.getAccountTypeUid())) {
                    Account account = new Account();
                    account.setAccountTypeUid(userAccountDo.getAccountTypeUid());
                    account.setAccountTypeName(accountTypeDoMap.get(userAccountDo.getAccountTypeUid()).getName());
                    account.setAccountName(userAccountDo.getName());

                    accountList.add(account);
                }
            });
        }

        //
        // Step 3, post-processing
        //
        UserSimplifiedDto userSimplifiedDto = new UserSimplifiedDto();
        BeanUtils.copyProperties(userBasicDo, userSimplifiedDto);
        userSimplifiedDto.setExtendedPropertyList(extendedPropertyList);
        userSimplifiedDto.setAccountList(accountList);
        return userSimplifiedDto;
    }

    @Override
    public UserSimplifiedDto getUserSimplified(
            Long accountTypeUid,
            String accountName) throws AbcUndefinedException {
        UserAccountDo userAccountDo = this.userAccountRepository.findByAccountTypeUidAndName(accountTypeUid,
                accountName);
        if (userAccountDo == null) {
            throw new AbcResourceNotFoundException("the account does not exist");
        }

        return getUserSimplified(userAccountDo.getUserUid());
    }

    @Override
    public UserDetailedDto getUserDetailed(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        UserDetailedDto userDetailedDto = new UserDetailedDto();

        UserDto userDto = getUser(uid, operatingUserProfile);
        BeanUtils.copyProperties(userDto, userDetailedDto);

        //
        // Step 2, core-processing
        //
        List<Long> roleUidList = new LinkedList<>();
        List<UserRoleDo> userRoleDoList = this.userRoleRepository.findByUserUid(uid);
        if (!CollectionUtils.isEmpty(userRoleDoList)) {
            userRoleDoList.forEach(userRoleDo -> {
                roleUidList.add(userRoleDo.getRoleUid());
            });
        }

        List<Long> groupUidList = new LinkedList<>();
        List<UserGroupDo> userGroupDoList = this.userGroupRepository.findByUserUid(uid);
        if (!CollectionUtils.isEmpty(userGroupDoList)) {
            userGroupDoList.forEach(userGroupDo -> {
                groupUidList.add(userGroupDo.getGroupUid());
            });
        }

        if (!CollectionUtils.isEmpty(groupUidList)) {
            List<GroupRoleDo> groupRoleDoList1 = this.groupRoleRepository.findByGroupUidIn(groupUidList);
            if (!CollectionUtils.isEmpty(groupRoleDoList1)) {
                groupRoleDoList1.forEach(groupRoleDo -> {
                    if (!roleUidList.contains(groupRoleDo.getRoleUid())) {
                        roleUidList.add(groupRoleDo.getRoleUid());
                    }
                });
            }
        }
        List<GroupRoleDo> groupRoleDoList2 = this.groupRoleRepository.findAllWithoutGroupUid();
        if (!CollectionUtils.isEmpty(groupRoleDoList2)) {
            groupRoleDoList2.forEach(groupRoleDo -> {
                if (!roleUidList.contains(groupRoleDo.getRoleUid())) {
                    roleUidList.add(groupRoleDo.getRoleUid());
                }
            });
        }

        Set<Long> navigationMenuPermissionUidSet = new HashSet<>();
        Set<Long> functionPermissionUidSet = new HashSet<>();

        if (!CollectionUtils.isEmpty(roleUidList)) {
            List<RolePermissionDo> rolePermissionDoList1 = this.rolePermissionRepository.findByRoleUidIn(roleUidList);
            if (!CollectionUtils.isEmpty(rolePermissionDoList1)) {
                rolePermissionDoList1.forEach(rolePermissionDo -> {
                    switch (rolePermissionDo.getPermissionType()) {
                        case FUNCTION:
                            functionPermissionUidSet.add(rolePermissionDo.getPermissionUid());
                            break;
                        case NAVIGATION_MENU:
                            navigationMenuPermissionUidSet.add(rolePermissionDo.getPermissionUid());
                            break;
                    }
                });
            }
        }
        List<RolePermissionDo> rolePermissionDoList2 = this.rolePermissionRepository.findAllWithoutRoleUid();
        if (!CollectionUtils.isEmpty(rolePermissionDoList2)) {
            rolePermissionDoList2.forEach(rolePermissionDo -> {
                switch (rolePermissionDo.getPermissionType()) {
                    case FUNCTION:
                        functionPermissionUidSet.add(rolePermissionDo.getPermissionUid());
                        break;
                    case NAVIGATION_MENU:
                        navigationMenuPermissionUidSet.add(rolePermissionDo.getPermissionUid());
                        break;
                }
            });
        }


        //
        // Step 3, post-processing
        //
        Permissions permissions = new Permissions();
        permissions.setFunctionList(new LinkedList<>());
        functionPermissionUidSet.forEach(permissionUid -> {
            Function function = new Function();
            function.setUid(permissionUid);
            permissions.getFunctionList().add(function);
        });
        permissions.setNavigationMenuList(new LinkedList<>());
        navigationMenuPermissionUidSet.forEach(permissionUid -> {
            NavigationMenu navigationMenu = new NavigationMenu();
            navigationMenu.setUid(permissionUid);
            permissions.getNavigationMenuList().add(navigationMenu);
        });

        userDetailedDto.setPermissions(permissions);
        return userDetailedDto;
    }

    @Override
    public List<UserDto> listingQueryUsers(
            Long uid,
            String displayName,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        //
        // Step 2, core-processing
        //
        Specification<UserBasicDo> specification = new Specification<UserBasicDo>() {
            @Override
            public Predicate toPredicate(Root<UserBasicDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();

                // ignore organization user
                predicateList.add(criteriaBuilder.notEqual(root.get("type"), UserTypeEnum.ORGANIZATION));

                if (uid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("uid"), uid));
                }
                if (!ObjectUtils.isEmpty(displayName)) {
                    predicateList.add(criteriaBuilder.like(root.get("displayName"), "%" + displayName + "%"));
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        List<UserBasicDo> itemDoList = this.userBasicRepository.findAll(specification, sort);

        //
        // Step 3, post-processing
        //
        if (CollectionUtils.isEmpty(itemDoList)) {
            return null;
        } else {
            List<Long> queryUserUidList = new ArrayList<>(itemDoList.size());
            List<UserDto> content = new ArrayList<>(itemDoList.size());
            itemDoList.forEach(itemDo -> {
                UserDto itemDto = new UserDto();
                BeanUtils.copyProperties(itemDo, itemDto);
                content.add(itemDto);

                queryUserUidList.add(itemDo.getUid());
            });

            // 补齐 extended property
            Map<Long, UserSchemaExtendedPropertyDo> userSchemaExtendedPropertyDoMap = new HashMap<>();
            this.userSchemaExtendedPropertyRepository.findAll().forEach(userSchemaExtendedPropertyDo -> {
                userSchemaExtendedPropertyDoMap.put(userSchemaExtendedPropertyDo.getUid(),
                        userSchemaExtendedPropertyDo);
            });

            Map<Long, List<ExtendedProperty>> userUidAndExtendedPropertyListMap = new HashMap<>();
            List<UserExtendedPropertyDo> userExtendedPropertyDoList =
                    this.userExtendedPropertyRepository.findByUserUidIn(queryUserUidList);
            if (!CollectionUtils.isEmpty(userExtendedPropertyDoList)) {
                userExtendedPropertyDoList.forEach(userExtendedPropertyDo -> {
                    if (!userUidAndExtendedPropertyListMap.containsKey(userExtendedPropertyDo.getUserUid())) {
                        userUidAndExtendedPropertyListMap.put(userExtendedPropertyDo.getUserUid(), new LinkedList<>());
                    }

                    if (userSchemaExtendedPropertyDoMap.containsKey(userExtendedPropertyDo.getExtendedPropertyUid())) {
                        ExtendedProperty extendedProperty = new ExtendedProperty();
                        extendedProperty.setExtendedPropertyUid(userExtendedPropertyDo.getExtendedPropertyUid());
                        extendedProperty.setExtendedPropertyName(
                                userSchemaExtendedPropertyDoMap.get(userExtendedPropertyDo.getExtendedPropertyUid()).getName());
                        extendedProperty.setExtendedPropertyValue(userExtendedPropertyDo.getExtendedPropertyValue());
                        userUidAndExtendedPropertyListMap.get(userExtendedPropertyDo.getUserUid()).add(extendedProperty);
                    }
                });
            }

            // 补齐 account
            Map<Long, AccountTypeDo> accountTypeDoMap = new HashMap<>();
            this.accountTypeRepository.findAll().forEach(accountTypeDo -> {
                accountTypeDoMap.put(accountTypeDo.getUid(), accountTypeDo);
            });

            Map<Long, List<Account>> userUidAndAccountListMap = new HashMap<>();
            List<UserAccountDo> userAccountDoList = this.userAccountRepository.findByUserUidIn(queryUserUidList);
            if (!CollectionUtils.isEmpty(userAccountDoList)) {
                userAccountDoList.forEach(userAccountDo -> {
                    if (!userUidAndAccountListMap.containsKey(userAccountDo.getUserUid())) {
                        userUidAndAccountListMap.put(userAccountDo.getUserUid(), new LinkedList<>());
                    }

                    if (accountTypeDoMap.containsKey(userAccountDo.getAccountTypeUid())) {
                        Account account = new Account();
                        account.setAccountTypeUid(userAccountDo.getAccountTypeUid());
                        account.setAccountTypeName(accountTypeDoMap.get(userAccountDo.getAccountTypeUid()).getName());
                        account.setAccountName(userAccountDo.getName());
                        account.setCreatedTimestamp(userAccountDo.getCreatedTimestamp());
                        userUidAndAccountListMap.get(userAccountDo.getUserUid()).add(account);
                    }
                });
            }

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
                itemDto.setExtendedPropertyList(userUidAndExtendedPropertyListMap.get(itemDto.getUid()));
                itemDto.setAccountList(userUidAndAccountListMap.get(itemDto.getUid()));
                itemDto.setRoleList(userUidAndRoleListMap.get(itemDto.getUid()));
                itemDto.setGroupList(userUidAndGroupListMap.get(itemDto.getUid()));
            });

            // 返回
            return content;
        }
    }

    @Override
    public Page<UserDto> pagingQueryUsers(
            Boolean enabled,
            List<Long> userUidListOfLastModifiedBy,
            List<String> lastModifiedTimestampAsStringList,
            Long uid,
            String displayName,
            List<String> extendedPropertyList,
            List<String> accountList,
            List<Long> roleUidList,
            List<Long> groupUidList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        Map<Long, UserSchemaExtendedPropertyDo> userSchemaExtendedPropertyDoMap = new HashMap<>();
        this.userSchemaExtendedPropertyRepository.findAll().forEach(userSchemaExtendedPropertyDo -> {
            userSchemaExtendedPropertyDoMap.put(userSchemaExtendedPropertyDo.getUid(),
                    userSchemaExtendedPropertyDo);
        });

        //
        // Step 2, core-processing
        //

        List<Long> candidateUserUidList = new LinkedList<>();
        if (uid != null) {
            candidateUserUidList.add(uid);
        }

        //
        // Step 2.1, query user uid list by extended property list
        //
        if (!CollectionUtils.isEmpty(extendedPropertyList)) {
            Specification<UserExtendedPropertyDo> specification = new Specification<UserExtendedPropertyDo>() {
                @Override
                public Predicate toPredicate(Root<UserExtendedPropertyDo> root, CriteriaQuery<?> query,
                                             CriteriaBuilder criteriaBuilder) {
                    List<Predicate> predicateList = new ArrayList<>();

                    extendedPropertyList.forEach(extendedProperty -> {
                        int signalIndex = extendedProperty.indexOf("_");
                        if (signalIndex <= 0) {
                            // ignore
                            LOGGER.warn("ignore illegal extended property param: {}", extendedProperty);
                            return;
                        }

                        String extendedPropertyUidAsString = extendedProperty.substring(0, signalIndex);
                        Long extendedPropertyUid = null;
                        try {
                            extendedPropertyUid = Long.valueOf(extendedPropertyUidAsString);
                        } catch (Exception e) {
                            LOGGER.warn("ignore illegal extended property uid {}", extendedPropertyUidAsString);
                            return;
                        }

                        String extendedPropertyValue = extendedProperty.substring(signalIndex + 1);

                        UserSchemaExtendedPropertyDo userSchemaExtendedPropertyDo =
                                userSchemaExtendedPropertyDoMap.get(extendedPropertyUid);
                        if (userSchemaExtendedPropertyDo == null) {
                            LOGGER.warn("ignore illegal extended property uid {}, cannot find it", extendedPropertyUid);
                            return;
                        }

                        switch (userSchemaExtendedPropertyDo.getType()) {
                            case BOOLEAN:
                            case TINYINT:
                            case SMALLINT:
                            case MEDIUMINT:
                            case INT:
                            case LONG:
                            case DECIMAL:
                            case YEAR:
                                // 精确查询
                                predicateList.add(criteriaBuilder.equal(
                                        root.get("extendedPropertyValue"), extendedPropertyValue));
                                break;
                            case VARCHAR:
                            case CHAR:
                                // 模糊查询
                                predicateList.add(criteriaBuilder.like(
                                        root.get("extendedPropertyValue"), "%" + extendedPropertyValue + "%"));
                                break;
                            case DATE:
                            case DATETIME:
                            case TIMESTAMP:
                            case TIME:
                                // 时间范围
                                // TODO 考虑 00:00:00, 23:59:59
                                String[] slices = extendedPropertyValue.split("TO");
                                if (slices.length == 2) {
                                    String from = slices[0].trim();
                                    String to = slices[1].trim();

                                    predicateList.add(criteriaBuilder.greaterThanOrEqualTo(
                                            root.get("extendedPropertyValue"), from));
                                    predicateList.add(criteriaBuilder.lessThanOrEqualTo(
                                            root.get("extendedPropertyValue"), to));
                                }
                                break;
                            default:
                                break;
                        }
                    });

                    return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
                }
            };

            Sort sort = Sort.by(Sort.Order.asc("extendedPropertyUid"));
            List<UserExtendedPropertyDo> itemDoList = this.userExtendedPropertyRepository.findAll(specification,
                    sort);

            if (CollectionUtils.isEmpty(itemDoList)) {
                Page<UserDto> itemDtoPage = new PageImpl<UserDto>(
                        new ArrayList<>(), pageable, 0L);
                return itemDtoPage;
            }

            List<Long> userUidListOfExtendedProperty = new LinkedList<>();
            itemDoList.forEach(itemDo -> {
                if (!userUidListOfExtendedProperty.contains(itemDo.getUserUid())) {
                    userUidListOfExtendedProperty.add(itemDo.getUserUid());
                }
            });

            if (candidateUserUidList.isEmpty()) {
                candidateUserUidList.addAll(userUidListOfExtendedProperty);
            } else {
                candidateUserUidList.retainAll(userUidListOfExtendedProperty);

                if (CollectionUtils.isEmpty(candidateUserUidList)) {
                    Page<UserDto> itemDtoPage = new PageImpl<UserDto>(
                            new ArrayList<>(), pageable, 0L);
                    return itemDtoPage;
                }
            }
        }

        //
        // Step 2.2, query user uid list by account list
        //
        if (!CollectionUtils.isEmpty(accountList)) {
            Specification<UserAccountDo> specification = new Specification<UserAccountDo>() {
                @Override
                public Predicate toPredicate(Root<UserAccountDo> root, CriteriaQuery<?> query,
                                             CriteriaBuilder criteriaBuilder) {
                    List<Predicate> predicateList = new ArrayList<>();

                    accountList.forEach(account -> {
                        int signalIndex = account.indexOf("_");
                        if (signalIndex >= 0) {
                            String accountName = account.substring(signalIndex + 1);
                            String accountTypeUidAsString = account.substring(0, signalIndex);
                            try {
                                Long accountTypeUid = Long.valueOf(accountTypeUidAsString);

                                predicateList.add(criteriaBuilder.equal(root.get("accountTypeUid"),
                                        accountTypeUid));
                                predicateList.add(criteriaBuilder.like(root.get("name"),
                                        "%" + accountName + "%"));
                            } catch (Exception e) {
                                LOGGER.warn("ignore illegal account query parameters:{}", account);
                                return;
                            }
                        } else {
                            // ignore
                            LOGGER.warn("ignore illegal account query parameters:{}", account);
                        }
                    });

                    return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
                }
            };

            Sort sort = Sort.by(Sort.Order.asc("userUid"));
            List<UserAccountDo> itemDoList = this.userAccountRepository.findAll(specification,
                    sort);

            if (CollectionUtils.isEmpty(itemDoList)) {
                Page<UserDto> itemDtoPage = new PageImpl<UserDto>(
                        new ArrayList<>(), pageable, 0L);
                return itemDtoPage;
            }

            List<Long> userUidListOfAccount = new LinkedList<>();
            itemDoList.forEach(itemDo -> {
                if (!userUidListOfAccount.contains(itemDo.getUserUid())) {
                    userUidListOfAccount.add(itemDo.getUserUid());
                }
            });

            if (candidateUserUidList.isEmpty()) {
                candidateUserUidList.addAll(userUidListOfAccount);
            } else {
                candidateUserUidList.retainAll(userUidListOfAccount);

                if (CollectionUtils.isEmpty(candidateUserUidList)) {
                    Page<UserDto> itemDtoPage = new PageImpl<UserDto>(
                            new ArrayList<>(), pageable, 0L);
                    return itemDtoPage;
                }
            }
        }

        //
        // Step 2.3, query user uid list by role uid list
        //
        if (!CollectionUtils.isEmpty(roleUidList)) {
            List<Long> userUidListOfRole = listingQueryUidOfUsersByRole(roleUidList, operatingUserProfile);
            if (CollectionUtils.isEmpty(userUidListOfRole)) {
                Page<UserDto> itemDtoPage = new PageImpl<UserDto>(
                        new ArrayList<>(), pageable, 0L);
                return itemDtoPage;
            }

            if (candidateUserUidList.isEmpty()) {
                candidateUserUidList.addAll(userUidListOfRole);
            } else {
                candidateUserUidList.retainAll(userUidListOfRole);

                if (CollectionUtils.isEmpty(candidateUserUidList)) {
                    Page<UserDto> itemDtoPage = new PageImpl<UserDto>(
                            new ArrayList<>(), pageable, 0L);
                    return itemDtoPage;
                }
            }
        }

        //
        // Step 2.4, query user uid list by group uid list
        //
        if (!CollectionUtils.isEmpty(groupUidList)) {
            List<Long> userUidListOfGroup = listingQueryUidOfUsersByGroup(groupUidList, operatingUserProfile);
            if (CollectionUtils.isEmpty(userUidListOfGroup)) {
                Page<UserDto> itemDtoPage = new PageImpl<UserDto>(
                        new ArrayList<>(), pageable, 0L);
                return itemDtoPage;
            }

            if (candidateUserUidList.isEmpty()) {
                candidateUserUidList.addAll(userUidListOfGroup);
            } else {
                candidateUserUidList.retainAll(userUidListOfGroup);

                if (CollectionUtils.isEmpty(candidateUserUidList)) {
                    Page<UserDto> itemDtoPage = new PageImpl<UserDto>(
                            new ArrayList<>(), pageable, 0L);
                    return itemDtoPage;
                }
            }
        }

        //
        // Step 2.5, final query
        //
        Specification<UserBasicDo> specification = new Specification<UserBasicDo>() {
            @Override
            public Predicate toPredicate(Root<UserBasicDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();

                // ignore root user
                predicateList.add(criteriaBuilder.notEqual(root.get("uid"), InfrastructureConstants.ROOT_USER_UID));

                // ignore organization user
                predicateList.add(criteriaBuilder.notEqual(root.get("type"), UserTypeEnum.ORGANIZATION));

                if (enabled != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("enabled"), enabled));
                }

                if (!CollectionUtils.isEmpty(userUidListOfLastModifiedBy)) {
                    CriteriaBuilder.In<Long> in = criteriaBuilder.in(root.get("lastModifiedBy"));
                    userUidListOfLastModifiedBy.forEach(item -> {
                        in.value(item);
                    });
                    predicateList.add(in);
                }

                if (!CollectionUtils.isEmpty(lastModifiedTimestampAsStringList)) {
                    if (lastModifiedTimestampAsStringList.size() == 2) {
                        LocalDateTime dateTime0 = LocalDateTime.parse(lastModifiedTimestampAsStringList.get(0),
                                dateTimeFormatter);
                        LocalDateTime dateTime1 = LocalDateTime.parse(lastModifiedTimestampAsStringList.get(1), dateTimeFormatter);
                        if (dateTime0.isAfter(dateTime1)) {
                            predicateList.add(criteriaBuilder.between(root.get(BaseDo.LAST_MODIFIED_TIMESTAMP_FIELD_NAME),
                                    dateTime1, dateTime0));
                        } else {
                            predicateList.add(criteriaBuilder.between(root.get(BaseDo.LAST_MODIFIED_TIMESTAMP_FIELD_NAME),
                                    dateTime0, dateTime1));
                        }
                    } else if (lastModifiedTimestampAsStringList.size() == 1) {
                        LocalDateTime dateTime0 = LocalDateTime.parse(lastModifiedTimestampAsStringList.get(0), dateTimeFormatter);
                        predicateList.add(criteriaBuilder.equal(root.get(BaseDo.LAST_MODIFIED_TIMESTAMP_FIELD_NAME),
                                dateTime0));
                    } else {
                        CriteriaBuilder.In<LocalDateTime> in = criteriaBuilder.in(root.get(BaseDo.LAST_MODIFIED_TIMESTAMP_FIELD_NAME));
                        lastModifiedTimestampAsStringList.forEach(timestampAsString -> {
                            LocalDateTime dateTime0 = LocalDateTime.parse(timestampAsString, dateTimeFormatter);
                            in.value(dateTime0);
                        });
                        predicateList.add(in);
                    }
                }

                if (!CollectionUtils.isEmpty(candidateUserUidList)) {
                    CriteriaBuilder.In<Long> in = criteriaBuilder.in(root.get("uid"));
                    candidateUserUidList.forEach(item -> {
                        in.value(item);
                    });
                    predicateList.add(in);
                }

                if (!ObjectUtils.isEmpty(displayName)) {
                    predicateList.add(criteriaBuilder.like(root.get("displayName"), "%" + displayName + "%"));
                }

                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        Page<UserBasicDo> itemDoPage = this.userBasicRepository.findAll(specification,
                pageable);

        //
        // Step 3, post-processing
        //

        //
        // Step 3.1, 为 created by, last modified by 补充 user brief information
        //
        List<Long> userUidList = new LinkedList<>();
        itemDoPage.forEach(itemDo -> {
            if (itemDo.getCreatedBy() != null && !userUidList.contains(itemDo.getCreatedBy())) {
                userUidList.add(itemDo.getCreatedBy());
            }
            if (itemDo.getLastModifiedBy() != null && !userUidList.contains(itemDo.getLastModifiedBy())) {
                userUidList.add(itemDo.getLastModifiedBy());
            }
        });

        Map<Long, UserBriefInformation> userBriefInformationMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(userUidList)) {
            List<UserBriefInformation> userBriefInformationList = listingUserBriefInformation(userUidList, operatingUserProfile);
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
            Page<UserDto> itemDtoPage = new PageImpl<UserDto>(
                    new ArrayList<>(), pageable, itemDoPage.getTotalElements());
            return itemDtoPage;
        } else {
            List<Long> queryUserUidList = new ArrayList<>(itemDoPage.getContent().size());
            List<UserDto> content = new ArrayList<>(itemDoPage.getContent().size());
            itemDoPage.forEach(itemDo -> {
                UserDto itemDto = new UserDto();
                BeanUtils.copyProperties(itemDo, itemDto);

                // 为 created by, last modified by 补充 user brief information
                itemDto.setCreatedByUser(userBriefInformationMap.get(itemDo.getCreatedBy()));
                itemDto.setLastModifiedByUser(userBriefInformationMap.get(itemDo.getLastModifiedBy()));

                content.add(itemDto);

                queryUserUidList.add(itemDo.getUid());
            });
            Page<UserDto> itemDtoPage = new PageImpl<UserDto>(
                    content, pageable, itemDoPage.getTotalElements());

            // 补齐 extended property(ies)
            Map<Long, List<ExtendedProperty>> userUidAndExtendedPropertyListMap = new HashMap<>();
            List<UserExtendedPropertyDo> userExtendedPropertyDoList =
                    this.userExtendedPropertyRepository.findByUserUidIn(queryUserUidList);
            if (!CollectionUtils.isEmpty(userExtendedPropertyDoList)) {
                userExtendedPropertyDoList.forEach(userExtendedPropertyDo -> {
                    if (!userUidAndExtendedPropertyListMap.containsKey(userExtendedPropertyDo.getUserUid())) {
                        userUidAndExtendedPropertyListMap.put(userExtendedPropertyDo.getUserUid(), new LinkedList<>());
                    }

                    if (userSchemaExtendedPropertyDoMap.containsKey(userExtendedPropertyDo.getExtendedPropertyUid())) {
                        ExtendedProperty extendedProperty = new ExtendedProperty();
                        extendedProperty.setExtendedPropertyUid(userExtendedPropertyDo.getExtendedPropertyUid());
                        extendedProperty.setExtendedPropertyName(
                                userSchemaExtendedPropertyDoMap.get(userExtendedPropertyDo.getExtendedPropertyUid()).getName());
                        extendedProperty.setExtendedPropertyValue(userExtendedPropertyDo.getExtendedPropertyValue());
                        userUidAndExtendedPropertyListMap.get(userExtendedPropertyDo.getUserUid()).add(extendedProperty);
                    }
                });
            }

            // 补齐 account(s)
            Map<Long, AccountTypeDo> accountTypeDoMap = new HashMap<>();
            this.accountTypeRepository.findAll().forEach(accountTypeDo -> {
                accountTypeDoMap.put(accountTypeDo.getUid(), accountTypeDo);
            });

            Map<Long, List<Account>> userUidAndAccountListMap = new HashMap<>();
            List<UserAccountDo> userAccountDoList = this.userAccountRepository.findByUserUidIn(queryUserUidList);
            if (!CollectionUtils.isEmpty(userAccountDoList)) {
                userAccountDoList.forEach(userAccountDo -> {
                    if (!userUidAndAccountListMap.containsKey(userAccountDo.getUserUid())) {
                        userUidAndAccountListMap.put(userAccountDo.getUserUid(), new LinkedList<>());
                    }

                    if (accountTypeDoMap.containsKey(userAccountDo.getAccountTypeUid())) {
                        Account account = new Account();
                        account.setAccountTypeUid(userAccountDo.getAccountTypeUid());
                        account.setAccountTypeName(accountTypeDoMap.get(userAccountDo.getAccountTypeUid()).getName());
                        account.setAccountName(userAccountDo.getName());
                        account.setCreatedTimestamp(userAccountDo.getCreatedTimestamp());
                        userUidAndAccountListMap.get(userAccountDo.getUserUid()).add(account);
                    }
                });
            }

            // 补齐 role(s)
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

            // 补齐 group(s)
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
                itemDto.setExtendedPropertyList(userUidAndExtendedPropertyListMap.get(itemDto.getUid()));
                itemDto.setAccountList(userUidAndAccountListMap.get(itemDto.getUid()));
                itemDto.setRoleList(userUidAndRoleListMap.get(itemDto.getUid()));
                itemDto.setGroupList(userUidAndGroupListMap.get(itemDto.getUid()));
            });

            // 返回
            return itemDtoPage;
        }
    }

    @Override
    public Page<UserOverviewDto> pagingQueryUserOverview(
            List<Long> userUidListOfUser,
            List<Long> roleUidList,
            List<Long> groupUidList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        //
        // Step 2, core-processing
        //

        List<Long> candidateUserUidList = new LinkedList<>();
        if (!CollectionUtils.isEmpty(userUidListOfUser)) {
            candidateUserUidList.addAll(userUidListOfUser);
        }

        //
        // Step 2.1, query user uid list by role uid list
        //
        if (!CollectionUtils.isEmpty(roleUidList)) {
            List<Long> userUidListOfRole = listingQueryUidOfUsersByRole(roleUidList, operatingUserProfile);
            if (CollectionUtils.isEmpty(userUidListOfRole)) {
                Page<UserOverviewDto> itemDtoPage = new PageImpl<UserOverviewDto>(
                        new ArrayList<>(), pageable, 0L);
                return itemDtoPage;
            }

            if (candidateUserUidList.isEmpty()) {
                candidateUserUidList.addAll(userUidListOfRole);
            } else {
                candidateUserUidList.retainAll(userUidListOfRole);

                if (CollectionUtils.isEmpty(candidateUserUidList)) {
                    Page<UserOverviewDto> itemDtoPage = new PageImpl<UserOverviewDto>(
                            new ArrayList<>(), pageable, 0L);
                    return itemDtoPage;
                }
            }
        }

        //
        // Step 2.2, query user uid list by group uid list
        //
        if (!CollectionUtils.isEmpty(groupUidList)) {
            List<Long> userUidListOfGroup = listingQueryUidOfUsersByGroup(groupUidList, operatingUserProfile);
            if (CollectionUtils.isEmpty(userUidListOfGroup)) {
                Page<UserOverviewDto> itemDtoPage = new PageImpl<UserOverviewDto>(
                        new ArrayList<>(), pageable, 0L);
                return itemDtoPage;
            }

            if (candidateUserUidList.isEmpty()) {
                candidateUserUidList.addAll(userUidListOfGroup);
            } else {
                candidateUserUidList.retainAll(userUidListOfGroup);

                if (CollectionUtils.isEmpty(candidateUserUidList)) {
                    Page<UserOverviewDto> itemDtoPage = new PageImpl<UserOverviewDto>(
                            new ArrayList<>(), pageable, 0L);
                    return itemDtoPage;
                }
            }
        }

        //
        // Step 2.3, final query
        //
        Specification<UserBasicDo> specification = new Specification<UserBasicDo>() {
            @Override
            public Predicate toPredicate(Root<UserBasicDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();

                // ignore root user
                predicateList.add(criteriaBuilder.notEqual(root.get("uid"), InfrastructureConstants.ROOT_USER_UID));

                // ignore organization user
                predicateList.add(criteriaBuilder.notEqual(root.get("type"), UserTypeEnum.ORGANIZATION));

                if (!CollectionUtils.isEmpty(candidateUserUidList)) {
                    CriteriaBuilder.In<Long> in = criteriaBuilder.in(root.get("uid"));
                    candidateUserUidList.forEach(item -> {
                        in.value(item);
                    });
                    predicateList.add(in);
                }

                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        Page<UserBasicDo> itemDoPage = this.userBasicRepository.findAll(specification,
                pageable);

        //
        // Step 3, post-processing
        //

        //
        // Step 3.1, 为 created by, last modified by, uid(user) 补充 user brief information
        //
        List<Long> userUidList = new LinkedList<>();
        itemDoPage.forEach(itemDo -> {
            if (itemDo.getCreatedBy() != null && !userUidList.contains(itemDo.getCreatedBy())) {
                userUidList.add(itemDo.getCreatedBy());
            }
            if (itemDo.getLastModifiedBy() != null && !userUidList.contains(itemDo.getLastModifiedBy())) {
                userUidList.add(itemDo.getLastModifiedBy());
            }
            if (itemDo.getUid() != null && !userUidList.contains(itemDo.getUid())) {
                userUidList.add(itemDo.getUid());
            }
        });

        Map<Long, UserBriefInformation> userBriefInformationMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(userUidList)) {
            List<UserBriefInformation> userBriefInformationList = listingUserBriefInformation(userUidList, operatingUserProfile);
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
            Page<UserOverviewDto> itemDtoPage = new PageImpl<UserOverviewDto>(
                    new ArrayList<>(), pageable, itemDoPage.getTotalElements());
            return itemDtoPage;
        } else {
            List<Long> queryUserUidList = new ArrayList<>(itemDoPage.getContent().size());
            List<UserOverviewDto> content = new ArrayList<>(itemDoPage.getContent().size());
            itemDoPage.forEach(itemDo -> {
                UserOverviewDto itemDto = new UserOverviewDto();

                // 为 created by, last modified by 补充 user brief information
                itemDto.setCreatedByUser(userBriefInformationMap.get(itemDo.getCreatedBy()));
                itemDto.setLastModifiedByUser(userBriefInformationMap.get(itemDo.getLastModifiedBy()));
                // 为 uid (user) 补充 user brief information
                itemDto.setUser(userBriefInformationMap.get(itemDo.getUid()));

                content.add(itemDto);

                queryUserUidList.add(itemDo.getUid());
            });
            Page<UserOverviewDto> itemDtoPage = new PageImpl<UserOverviewDto>(
                    content, pageable, itemDoPage.getTotalElements());

            // 补齐 role(s)
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

            // 补齐 group(s)
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

    @Override
    public List<Long> listingQueryUidOfUsers(
            Long uid,
            String displayName,
            List<String> extendedPropertyList,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        Map<Long, UserSchemaExtendedPropertyDo> userSchemaExtendedPropertyDoMap = new HashMap<>();
        this.userSchemaExtendedPropertyRepository.findAll().forEach(userSchemaExtendedPropertyDo -> {
            userSchemaExtendedPropertyDoMap.put(userSchemaExtendedPropertyDo.getUid(),
                    userSchemaExtendedPropertyDo);
        });

        //
        // Step 2, core-processing
        //
        List<Long> candidateUserUidList = new LinkedList<>();
        if (uid != null) {
            candidateUserUidList.add(uid);
        }

        //
        // Step 2.1, query user uid list by extended property list
        //
        if (!CollectionUtils.isEmpty(extendedPropertyList)) {
            Specification<UserExtendedPropertyDo> specification = new Specification<UserExtendedPropertyDo>() {
                @Override
                public Predicate toPredicate(Root<UserExtendedPropertyDo> root, CriteriaQuery<?> query,
                                             CriteriaBuilder criteriaBuilder) {
                    List<Predicate> predicateList = new ArrayList<>();

                    extendedPropertyList.forEach(extendedProperty -> {
                        int signalIndex = extendedProperty.indexOf("_");
                        if (signalIndex <= 0) {
                            // ignore
                            LOGGER.warn("ignore illegal extended property param: {}", extendedProperty);
                            return;
                        }

                        String extendedPropertyUidAsString = extendedProperty.substring(0, signalIndex);
                        Long extendedPropertyUid = null;
                        try {
                            extendedPropertyUid = Long.valueOf(extendedPropertyUidAsString);
                        } catch (Exception e) {
                            LOGGER.warn("ignore illegal extended property uid {}", extendedPropertyUidAsString);
                            return;
                        }

                        String extendedPropertyValue = extendedProperty.substring(signalIndex + 1);

                        UserSchemaExtendedPropertyDo userSchemaExtendedPropertyDo =
                                userSchemaExtendedPropertyDoMap.get(extendedPropertyUid);
                        if (userSchemaExtendedPropertyDo == null) {
                            LOGGER.warn("ignore illegal extended property uid {}, cannot find it", extendedPropertyUid);
                            return;
                        }

                        switch (userSchemaExtendedPropertyDo.getType()) {
                            case BOOLEAN:
                            case TINYINT:
                            case SMALLINT:
                            case MEDIUMINT:
                            case INT:
                            case LONG:
                            case DECIMAL:
                            case YEAR:
                                // 精确查询
                                predicateList.add(criteriaBuilder.equal(
                                        root.get("extendedPropertyValue"), extendedPropertyValue));
                                break;
                            case VARCHAR:
                            case CHAR:
                                // 模糊查询
                                predicateList.add(criteriaBuilder.like(
                                        root.get("extendedPropertyValue"), "%" + extendedPropertyValue + "%"));
                                break;
                            case DATE:
                            case DATETIME:
                            case TIMESTAMP:
                            case TIME:
                                // 时间范围
                                // TODO 考虑 00:00:00, 23:59:59
                                String[] slices = extendedPropertyValue.split("TO");
                                if (slices.length == 2) {
                                    String from = slices[0].trim();
                                    String to = slices[1].trim();

                                    predicateList.add(criteriaBuilder.greaterThanOrEqualTo(
                                            root.get("extendedPropertyValue"), from));
                                    predicateList.add(criteriaBuilder.lessThanOrEqualTo(
                                            root.get("extendedPropertyValue"), to));
                                }
                                break;
                            default:
                                break;
                        }
                    });

                    return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
                }
            };

            Sort sort = Sort.by(Sort.Order.asc("extendedPropertyUid"));
            List<UserExtendedPropertyDo> itemDoList = this.userExtendedPropertyRepository.findAll(specification,
                    sort);

            if (CollectionUtils.isEmpty(itemDoList)) {
                return null;
            }

            List<Long> userUidListOfExtendedProperty = new LinkedList<>();
            itemDoList.forEach(itemDo -> {
                if (!userUidListOfExtendedProperty.contains(itemDo.getUserUid())) {
                    userUidListOfExtendedProperty.add(itemDo.getUserUid());
                }
            });

            if (candidateUserUidList.isEmpty()) {
                candidateUserUidList.addAll(userUidListOfExtendedProperty);
            } else {
                candidateUserUidList.retainAll(userUidListOfExtendedProperty);

                if (CollectionUtils.isEmpty(candidateUserUidList)) {
                    return null;
                }
            }
        }

        //
        // Step 2, final query
        //
        Specification<UserBasicDo> specification = new Specification<UserBasicDo>() {
            @Override
            public Predicate toPredicate(Root<UserBasicDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();

                // ignore organization user
                predicateList.add(criteriaBuilder.notEqual(root.get("type"), UserTypeEnum.ORGANIZATION));

                if (!CollectionUtils.isEmpty(candidateUserUidList)) {
                    CriteriaBuilder.In<Long> in = criteriaBuilder.in(root.get("uid"));
                    candidateUserUidList.forEach(item -> {
                        in.value(item);
                    });
                    predicateList.add(in);
                }

                if (!ObjectUtils.isEmpty(displayName)) {
                    predicateList.add(criteriaBuilder.like(root.get("displayName"), "%" + displayName + "%"));
                }

                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        Sort sort = Sort.by(Sort.Order.asc(BaseDo.ID_FIELD_NAME));
        List<UserBasicDo> itemDoList = this.userBasicRepository.findAll(specification, sort);

        //
        // Step 3, post-processing
        //
        if (CollectionUtils.isEmpty(itemDoList)) {
            return null;
        } else {
            List<Long> userUidList = new LinkedList<>();
            itemDoList.forEach(itemDo -> {
                userUidList.add(itemDo.getUid());
            });

            // 返回
            return userUidList;
        }
    }

    @Override
    public List<Long> listingQueryUidOfUsers(
            String encodedInput,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        if (ObjectUtils.isEmpty(encodedInput)) {
            return null;
        }

        String decodedInput = null;
        try {
            decodedInput = URLDecoder.decode(encodedInput, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new AbcIllegalParameterException(encodedInput);
        }

        String[] innerParameters = decodedInput.split("!-!");
        if (innerParameters.length > 0) {
            Long uidOfUser = null;
            String displayNameOfUser = null;
            List<String> extendedPropertyListOfUser = null;

            for (String innerParameter : innerParameters) {
                int innerParameterNameIndex = innerParameter.indexOf("###");
                if (innerParameterNameIndex > 0) {
                    String innerParameterName = innerParameter.substring(0, innerParameterNameIndex);
                    String innerParameterValue = innerParameter.substring(innerParameterNameIndex + "###".length());

                    if (innerParameterName.equals("uid")) {
                        uidOfUser = Long.valueOf(innerParameterValue);
                    } else if (innerParameterName.equals("display_name")) {
                        displayNameOfUser = innerParameterValue;
                    } else if (innerParameterName.equals("extended_property")) {
                        // ${extended property uid}_${extended property value}
                        if (extendedPropertyListOfUser == null) {
                            extendedPropertyListOfUser = new LinkedList<>();
                        }
                        extendedPropertyListOfUser.add(innerParameterValue);
                    }
                }
            }

            return listingQueryUidOfUsers(uidOfUser,
                    displayNameOfUser,
                    extendedPropertyListOfUser,
                    operatingUserProfile);
        }

        return null;
    }

    @Override
    public List<Long> listingQueryUidOfUsersByRole(
            List<Long> roleUidList,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        if (CollectionUtils.isEmpty(roleUidList)) {
            return null;
        }

        //
        // Step 2, core-processing
        //
        Specification<UserRoleDo> specification = new Specification<UserRoleDo>() {
            @Override
            public Predicate toPredicate(Root<UserRoleDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();

                CriteriaBuilder.In<Long> in = criteriaBuilder.in(root.get("roleUid"));
                roleUidList.forEach(uid -> {
                    in.value(uid);
                });
                predicateList.add(in);

                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        Sort sort = Sort.by(Sort.Order.asc(BaseDo.ID_FIELD_NAME));
        List<UserRoleDo> itemDoList = this.userRoleRepository.findAll(specification, sort);

        //
        // Step 3, post-processing
        //
        if (CollectionUtils.isEmpty(itemDoList)) {
            return null;
        } else {
            List<Long> userUidList = new LinkedList<>();
            itemDoList.forEach(itemDo -> {
                if (!userUidList.contains(itemDo.getUserUid())) {
                    userUidList.add(itemDo.getUserUid());
                }
            });

            // 返回
            return userUidList;
        }
    }

    @Override
    public List<Long> listingQueryUidOfUsersByGroup(
            List<Long> groupUidList,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        if (CollectionUtils.isEmpty(groupUidList)) {
            return null;
        }

        //
        // Step 2, core-processing
        //
        Specification<UserGroupDo> specification = new Specification<UserGroupDo>() {
            @Override
            public Predicate toPredicate(Root<UserGroupDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();

                CriteriaBuilder.In<Long> in = criteriaBuilder.in(root.get("groupUid"));
                groupUidList.forEach(uid -> {
                    in.value(uid);
                });
                predicateList.add(in);

                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        Sort sort = Sort.by(Sort.Order.asc(BaseDo.ID_FIELD_NAME));
        List<UserGroupDo> itemDoList = this.userGroupRepository.findAll(specification, sort);

        //
        // Step 3, post-processing
        //
        if (CollectionUtils.isEmpty(itemDoList)) {
            return null;
        } else {
            List<Long> userUidList = new LinkedList<>();
            itemDoList.forEach(itemDo -> {
                if (!userUidList.contains(itemDo.getUserUid())) {
                    userUidList.add(itemDo.getUserUid());
                }
            });

            // 返回
            return userUidList;
        }
    }

    @Override
    public List<UserBriefInformation> listingUserBriefInformation(
            List<Long> uidList,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        if (CollectionUtils.isEmpty(uidList)) {
            throw new AbcIllegalParameterException("uid list should not be null or empty");
        }

        List<UserSchemaExtendedPropertyDo> extendedPropertyDoListShowInBriefInformation = new LinkedList<>();
        this.userSchemaExtendedPropertyRepository.findAll().forEach(userSchemaExtendedPropertyDo -> {
            if (Boolean.TRUE.equals(userSchemaExtendedPropertyDo.getShowInBriefInformation())) {
                extendedPropertyDoListShowInBriefInformation.add(userSchemaExtendedPropertyDo);
            }
        });

        //
        // Step 2, core-processing
        //
        Specification<UserBasicDo> specification = new Specification<UserBasicDo>() {
            @Override
            public Predicate toPredicate(Root<UserBasicDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();

                CriteriaBuilder.In<Long> in = criteriaBuilder.in(root.get("uid"));
                uidList.forEach(uid -> {
                    in.value(uid);
                });
                predicateList.add(in);

                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        Sort sort = Sort.by(Sort.Order.asc(BaseDo.ID_FIELD_NAME));
        List<UserBasicDo> itemDoList = this.userBasicRepository.findAll(specification, sort);


        //
        // Step 3, post-processing
        //
        if (CollectionUtils.isEmpty(itemDoList)) {
            return null;
        } else {
            Map<Long, String> extendedPropertyUidAndNameShowInBriefInformationMap = new HashMap<>();
            Map<Long, Map<Long, String>> userExtendedPropertyShowInBriefInformationMap = new HashMap<>();
            if (!CollectionUtils.isEmpty(extendedPropertyDoListShowInBriefInformation)) {
                List<Long> extendedPropertyUidListShowInBriefInformation = new LinkedList<>();
                extendedPropertyDoListShowInBriefInformation.forEach(extendedPropertyDo -> {
                    extendedPropertyUidListShowInBriefInformation.add(extendedPropertyDo.getUid());
                    extendedPropertyUidAndNameShowInBriefInformationMap.put(extendedPropertyDo.getUid(),
                            extendedPropertyDo.getName());
                });

                List<UserExtendedPropertyDo> userExtendedPropertyDoList =
                        this.userExtendedPropertyRepository.findByUserUidIn(uidList);
                if (!CollectionUtils.isEmpty(userExtendedPropertyDoList)) {
                    userExtendedPropertyDoList.forEach(userExtendedPropertyDo -> {
                        if (!extendedPropertyUidListShowInBriefInformation.contains(userExtendedPropertyDo.getExtendedPropertyUid())) {
                            return;
                        }

                        if (!userExtendedPropertyShowInBriefInformationMap.containsKey(userExtendedPropertyDo.getUserUid())) {
                            userExtendedPropertyShowInBriefInformationMap.put(userExtendedPropertyDo.getUserUid(), new HashMap<>());
                        }
                        userExtendedPropertyShowInBriefInformationMap.get(userExtendedPropertyDo.getUserUid())
                                .put(userExtendedPropertyDo.getExtendedPropertyUid(),
                                        userExtendedPropertyDo.getExtendedPropertyValue());
                    });
                }
            }

            List<UserBriefInformation> content = new LinkedList<>();
            itemDoList.forEach(itemDo -> {
                UserBriefInformation itemDto = new UserBriefInformation();
                itemDto.setUid(itemDo.getUid());
                itemDto.setDisplayName(itemDo.getDisplayName());

                // 补充作为 brief information 的 extended properties
                Map<Long, String> extendedPropertyUidAndValueMap = userExtendedPropertyShowInBriefInformationMap.get(itemDo.getUid());
                if (extendedPropertyUidAndValueMap != null) {
                    itemDto.setExtendedPropertyList(new LinkedList<>());

                    extendedPropertyUidAndValueMap.forEach((extendedPropertyUid, extendedPropertyValue) -> {
                        itemDto.getExtendedPropertyList().add(new AbcTuple3<>(extendedPropertyUid,
                                extendedPropertyUidAndNameShowInBriefInformationMap.get(extendedPropertyUid), extendedPropertyValue));
                    });
                }

                content.add(itemDto);
            });

            // 返回
            return content;
        }
    }

    @Override
    public void updateUserCredentials(
            UpdateUserCredentialDto updateUserCredentialDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Long userUid = operatingUserProfile.getUid();

        String hashedOldPassword = DigestUtils.sha256Hex(updateUserCredentialDto.getOldPassword());
        String hashedNewPassword = DigestUtils.sha256Hex(updateUserCredentialDto.getNewPassword());

        UserCredentialDo userCredentialDo = this.userCredentialRepository.findByUserUid(userUid);
        if (userCredentialDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", UserCredentialDo.RESOURCE_SYMBOL,
                    userUid));
        }

        if (!hashedOldPassword.equals(userCredentialDo.getCredential())) {
            throw new AbcResourceConflictException("wrong old password");
        }

        userCredentialDo.setCredential(hashedNewPassword);
        BaseDo.update(userCredentialDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.userCredentialRepository.save(userCredentialDo);
    }

    @Override
    public void updateUserCredentials(
            Long uid,
            UpdateUserCredentialDto updateUserCredentialDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        String hashedOldPassword = DigestUtils.sha256Hex(updateUserCredentialDto.getOldPassword());
        String hashedNewPassword = DigestUtils.sha256Hex(updateUserCredentialDto.getNewPassword());

        UserCredentialDo userCredentialDo = this.userCredentialRepository.findByUserUid(uid);
        if (userCredentialDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", UserCredentialDo.RESOURCE_SYMBOL,
                    uid));
        }

        if (!hashedOldPassword.equals(userCredentialDo.getCredential())) {
            throw new AbcResourceConflictException("wrong old password");
        }

        userCredentialDo.setCredential(hashedNewPassword);
        BaseDo.update(userCredentialDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.userCredentialRepository.save(userCredentialDo);
    }

    @Override
    public void batchCreateUsers(
            List<CreateUserDto> createUserDtoList,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        createUserDtoList.forEach(createUserDto -> {
            createUser(createUserDto, operatingUserProfile);
        });
    }

    @Override
    public void batchReplaceUsers(
            List<ReplaceUserDto> replaceUserDtoList,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        replaceUserDtoList.forEach(replaceUserDto -> {
            replaceUser(replaceUserDto.getUid(), replaceUserDto, operatingUserProfile);
        });
    }

    @Override
    public Long createOrganizationUser(
            String displayName,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        UserBasicDo userBasicDo = new UserBasicDo();
        userBasicDo.setUid(this.idHelper.getNextDistributedId(UserBasicDo.RESOURCE_NAME));
        userBasicDo.setDisplayName(displayName);
        userBasicDo.setEnabled(Boolean.TRUE);
        userBasicDo.setType(UserTypeEnum.ORGANIZATION);
        BaseDo.create(userBasicDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.userBasicRepository.save(userBasicDo);

        return userBasicDo.getUid();
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
    public void handleAccountTypeDeletedEvent(
            AccountTypeDeletedEvent event) {
        LOGGER.info("rcv event:{}", event);

        List<UserAccountDo> userAccountDoList =
                this.userAccountRepository.findByAccountTypeUid(event.getAccountTypeDo().getUid());
        if (!CollectionUtils.isEmpty(userAccountDoList)) {
            LocalDateTime now = LocalDateTime.now();

            List<Long> userUidList = new LinkedList<>();
            userAccountDoList.forEach(userAccountDo -> {
                userAccountDo.setDeleted(Boolean.TRUE);
                BaseDo.update(userAccountDo, event.getOperatingUserProfile().getUid(), now);

                if (!userUidList.contains(userAccountDo.getUserUid())) {
                    userUidList.add(userAccountDo.getUserUid());
                }
            });

            this.userAccountRepository.saveAll(userAccountDoList);

            // TODO update user

        }
    }


    @ResourceReferenceHandler(name = "user management")
    public List<String> checkResourceReference(
            ResourceReferenceManager.ResourceCategoryEnum resourceCategory,
            Long resourceUid,
            String resourceName) throws Exception {
        switch (resourceCategory) {
            case ACCOUNT_TYPE: {
                Long accountTypeUid = resourceUid;

                List<UserAccountDo> userAccountDoList =
                        this.userAccountRepository.findByAccountTypeUid(accountTypeUid);
                if (!CollectionUtils.isEmpty(userAccountDoList)) {
                    List<String> result = new LinkedList<>();
                    userAccountDoList.forEach(userAccountDo -> {
                        result.add(String.format(
                                "[%s] %s (%d)",
                                UserAccountDo.RESOURCE_SYMBOL,
                                userAccountDo.getName(),
                                userAccountDo.getUserUid()));
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
