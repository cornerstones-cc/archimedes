package cc.cornerstones.biz.administration.usermanagement.service.impl;

import cc.cornerstones.almond.exceptions.AbcResourceDuplicateException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserBriefInformation;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.administration.serviceconnection.dto.AuthenticationServiceAgentDto;
import cc.cornerstones.biz.administration.serviceconnection.dto.AuthenticationServiceComponentDto;
import cc.cornerstones.biz.administration.usermanagement.dto.AccountTypeDto;
import cc.cornerstones.biz.administration.usermanagement.dto.CreateAccountTypeDto;
import cc.cornerstones.biz.administration.usermanagement.dto.UpdateAccountTypeDto;
import cc.cornerstones.biz.administration.usermanagement.entity.AccountTypeDo;
import cc.cornerstones.biz.administration.usermanagement.persistence.AccountTypeRepository;
import cc.cornerstones.biz.administration.usermanagement.service.inf.AccountTypeService;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.share.assembly.ResourceReferenceManager;
import cc.cornerstones.biz.share.event.AccountTypeDeletedEvent;
import cc.cornerstones.biz.share.event.EventBusManager;
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

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AccountTypeServiceImpl implements AccountTypeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountTypeServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private AccountTypeRepository accountTypeRepository;

    @Autowired
    private ResourceReferenceManager resourceReferenceManager;

    private java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private java.time.format.DateTimeFormatter dateTimeFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    private UserService userService;

    @Override
    public AccountTypeDto createAccountType(
            CreateAccountTypeDto createAccountTypeDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        boolean existsDuplicate = this.accountTypeRepository.existsByName(createAccountTypeDto.getName());
        if (existsDuplicate) {
            throw new AbcResourceDuplicateException(String.format("%s::name=%s", AccountTypeDo.RESOURCE_SYMBOL,
                    createAccountTypeDto.getName()));
        }

        //
        // Step 2, core-processing
        //
        AccountTypeDo accountTypeDo = new AccountTypeDo();
        accountTypeDo.setUid(this.idHelper.getNextDistributedId(AccountTypeDo.RESOURCE_NAME));
        accountTypeDo.setName(createAccountTypeDto.getName());
        accountTypeDo.setObjectName(
                createAccountTypeDto.getName()
                        .replaceAll("_", "__")
                        .replaceAll("\\s", "_")
                        .toLowerCase());
        accountTypeDo.setDescription(createAccountTypeDto.getDescription());
        accountTypeDo.setSequence(createAccountTypeDto.getSequence());
        BaseDo.create(accountTypeDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.accountTypeRepository.save(accountTypeDo);

        //
        // Step 3, post-processing
        //
        AccountTypeDto accountTypeDto = new AccountTypeDto();
        BeanUtils.copyProperties(accountTypeDo, accountTypeDto);
        return accountTypeDto;
    }

    @Override
    public void updateAccountType(
            Long uid,
            UpdateAccountTypeDto updateAccountTypeDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        AccountTypeDo accountTypeDo = this.accountTypeRepository.findByUid(uid);
        if (accountTypeDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", AccountTypeDo.RESOURCE_SYMBOL, uid));
        }
        if (!ObjectUtils.isEmpty(updateAccountTypeDto.getName())
                && !updateAccountTypeDto.getName().equalsIgnoreCase(accountTypeDo.getName())) {
            boolean existsDuplicate = this.accountTypeRepository.existsByName(updateAccountTypeDto.getName());
            if (existsDuplicate) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s", AccountTypeDo.RESOURCE_SYMBOL,
                        updateAccountTypeDto.getName()));
            }
        }

        //
        // Step 2, core-processing
        //
        boolean requiredToUpdate = false;
        if (!ObjectUtils.isEmpty(updateAccountTypeDto.getName())) {
            accountTypeDo.setName(updateAccountTypeDto.getName());
            accountTypeDo.setObjectName(
                    updateAccountTypeDto.getName()
                            .replaceAll("_", "__")
                            .replaceAll("\\s", "_")
                            .toLowerCase());
            requiredToUpdate = true;
        }
        if (updateAccountTypeDto.getDescription() != null
                && !updateAccountTypeDto.getDescription().equals(accountTypeDo.getDescription())) {
            accountTypeDo.setDescription(updateAccountTypeDto.getDescription());
            requiredToUpdate = true;
        }
        if (updateAccountTypeDto.getSequence() != null
                && !updateAccountTypeDto.getSequence().equals(accountTypeDo.getSequence())) {
            accountTypeDo.setSequence(updateAccountTypeDto.getSequence());
            requiredToUpdate = true;
        }
        if (requiredToUpdate) {
            BaseDo.update(accountTypeDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.accountTypeRepository.save(accountTypeDo);
        }

        //
        // Step 3, post-processing
        //
    }

    @Override
    public List<String> listAllReferencesToAccountType(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        AccountTypeDo accountTypeDo = this.accountTypeRepository.findByUid(uid);
        if (accountTypeDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", AccountTypeDo.RESOURCE_SYMBOL,
                    uid));
        }

        //
        // Step 2, core-processing
        //
        return this.resourceReferenceManager.check(
                ResourceReferenceManager.ResourceCategoryEnum.ACCOUNT_TYPE,
                accountTypeDo.getUid(),
                accountTypeDo.getName());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteAccountType(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        AccountTypeDo accountTypeDo = this.accountTypeRepository.findByUid(uid);
        if (accountTypeDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", AccountTypeDo.RESOURCE_SYMBOL, uid));
        }

        //
        // Step 2, core-processing
        //
        deleteAccountType(accountTypeDo, operatingUserProfile);

        //
        // Step 3, post-processing
        //
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteAccountType(
            AccountTypeDo accountTypeDo,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        accountTypeDo.setDeleted(Boolean.TRUE);
        BaseDo.update(accountTypeDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.accountTypeRepository.save(accountTypeDo);

        // event post
        AccountTypeDeletedEvent event = new AccountTypeDeletedEvent();
        event.setAccountTypeDo(accountTypeDo);
        event.setOperatingUserProfile(operatingUserProfile);
        this.eventBusManager.post(event);
    }

    @Override
    public AccountTypeDto getAccountType(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        AccountTypeDo accountTypeDo = this.accountTypeRepository.findByUid(uid);
        if (accountTypeDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", AccountTypeDo.RESOURCE_SYMBOL, uid));
        }

        //
        // Step 2, core-processing
        //
        AccountTypeDto accountTypeDto = new AccountTypeDto();
        BeanUtils.copyProperties(accountTypeDo, accountTypeDto);
        return accountTypeDto;

        //
        // Step 3, post-processing
        //
    }

    @Override
    public List<AccountTypeDto> listingQueryAccountTypes(
            Long uid,
            String name,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Specification<AccountTypeDo> specification = new Specification<AccountTypeDo>() {
            @Override
            public Predicate toPredicate(Root<AccountTypeDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (uid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("uid"), uid));
                }
                if (!ObjectUtils.isEmpty(name)) {
                    predicateList.add(criteriaBuilder.like(root.get("name"), "%" + name + "%"));
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        List<AccountTypeDo> itemDoList = this.accountTypeRepository.findAll(specification, sort);
        if (CollectionUtils.isEmpty(itemDoList)) {
            return null;
        }
        List<AccountTypeDto> content = new ArrayList<>(itemDoList.size());
        itemDoList.forEach(itemDo -> {
            AccountTypeDto itemDto = new AccountTypeDto();
            BeanUtils.copyProperties(itemDo, itemDto);
            content.add(itemDto);
        });
        return content;
    }

    @Override
    public Page<AccountTypeDto> pagingQueryAccountTypes(
            Long uid,
            String name,
            String description,
            List<Long> userUidListOfLastModifiedBy,
            List<String> lastModifiedTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // step 1, pre-processing
        //

        //
        // step 2, core-processing
        //
        Specification<AccountTypeDo> specification = new Specification<AccountTypeDo>() {
            @Override
            public Predicate toPredicate(Root<AccountTypeDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (uid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("uid"), uid));
                }
                if (!ObjectUtils.isEmpty(name)) {
                    predicateList.add(criteriaBuilder.like(root.get("name"), "%" + name + "%"));
                }
                if (!ObjectUtils.isEmpty(description)) {
                    predicateList.add(criteriaBuilder.like(root.get("description"), "%" + description + "%"));
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
                        CriteriaBuilder.In<LocalDateTime> in =
                                criteriaBuilder.in(root.get(BaseDo.LAST_MODIFIED_TIMESTAMP_FIELD_NAME));
                        lastModifiedTimestampAsStringList.forEach(createdTimestampAsString -> {
                            LocalDateTime dateTime0 = LocalDateTime.parse(createdTimestampAsString, dateTimeFormatter);
                            in.value(dateTime0);
                        });
                        predicateList.add(in);
                    }
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        Page<AccountTypeDo> itemDoPage = this.accountTypeRepository.findAll(specification, pageable);


        //
        // step 3, post-processing
        //

        //
        // step 3.1, 为 created by, last modified by 补充 user brief information
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
            List<UserBriefInformation> userBriefInformationList =
                    this.userService.listingUserBriefInformation(userUidList, operatingUserProfile);
            if (!CollectionUtils.isEmpty(userBriefInformationList)) {
                userBriefInformationList.forEach(userBriefInformation -> {
                    userBriefInformationMap.put(userBriefInformation.getUid(), userBriefInformation);
                });
            }
        }

        //
        // step 3.2, 构造返回内容
        //
        List<AccountTypeDto> content = new ArrayList<>(itemDoPage.getSize());
        itemDoPage.forEach(itemDo -> {
            AccountTypeDto itemDto = new AccountTypeDto();
            BeanUtils.copyProperties(itemDo, itemDto);

            // 为 created by, last modified by 补充 user brief information
            itemDto.setCreatedByUser(userBriefInformationMap.get(itemDo.getCreatedBy()));
            itemDto.setLastModifiedByUser(userBriefInformationMap.get(itemDo.getLastModifiedBy()));

            content.add(itemDto);
        });
        Page<AccountTypeDto> itemDtoPage = new PageImpl<>(content, pageable, itemDoPage.getTotalElements());
        return itemDtoPage;
    }
}
