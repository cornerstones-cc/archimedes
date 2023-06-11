package cc.cornerstones.biz.administration.serviceconnection.service.impl;

import cc.cornerstones.almond.exceptions.*;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserBriefInformation;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.arbutus.pf4j.service.assembly.PluginHelper;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.administration.serviceconnection.dto.*;
import cc.cornerstones.biz.administration.serviceconnection.entity.AuthenticationServiceAgentDo;
import cc.cornerstones.biz.administration.serviceconnection.entity.AuthenticationServiceComponentDo;
import cc.cornerstones.biz.administration.serviceconnection.persistence.AuthenticationServiceComponentRepository;
import cc.cornerstones.biz.administration.serviceconnection.persistence.AuthenticationServiceAgentRepository;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.AuthenticationServiceComponentService;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.AuthenticationServiceAgentService;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DfsServiceComponentService;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.datadictionary.entity.DictionaryCategoryDo;
import cc.cornerstones.biz.datadictionary.service.assembly.DictionaryBuildSqlLogic;
import cc.cornerstones.biz.share.assembly.ResourceReferenceManager;
import cc.cornerstones.biz.share.event.AuthenticationServiceAgentDeletedEvent;
import cc.cornerstones.biz.share.event.AuthenticationServiceComponentDeletedEvent;
import cc.cornerstones.biz.share.event.DataSourceDeletedEvent;
import cc.cornerstones.biz.share.event.EventBusManager;
import cc.cornerstones.biz.share.types.ResourceReferenceHandler;
import com.alibaba.fastjson.JSONObject;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.PostConstruct;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AuthenticationServiceAgentServiceImpl implements AuthenticationServiceAgentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationServiceAgentServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private AuthenticationServiceAgentRepository authenticationServiceAgentRepository;

    @Autowired
    private AuthenticationServiceComponentRepository authenticationServiceComponentRepository;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    private PluginHelper pluginHelper;

    @Autowired
    private DfsServiceComponentService dfsServiceComponentService;

    @Autowired
    private AuthenticationServiceComponentService authenticationServiceComponentService;

    @Autowired
    private UserService userService;

    @Autowired
    private ResourceReferenceManager resourceReferenceManager;


    @Transactional(rollbackFor = Exception.class)
    @Override
    public AuthenticationServiceAgentDto createAuthenticationServiceAgent(
            CreateAuthenticationServiceAgentDto createAuthenticationServiceAgentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        boolean existsDuplicate =
                this.authenticationServiceAgentRepository.existsByName(createAuthenticationServiceAgentDto.getName());
        if (existsDuplicate) {
            throw new AbcResourceDuplicateException(String.format("%s::name=%s", AuthenticationServiceAgentDo.RESOURCE_SYMBOL,
                    createAuthenticationServiceAgentDto.getName()));
        }

        boolean existsAuthenticationServiceComponent = this.authenticationServiceComponentRepository.existsByUid(
                createAuthenticationServiceAgentDto.getServiceComponentUid());
        if (!existsAuthenticationServiceComponent) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    AuthenticationServiceComponentDo.RESOURCE_SYMBOL,
                    createAuthenticationServiceAgentDto.getServiceComponentUid()));
        }

        //
        // Step 2, core-processing
        //
        AuthenticationServiceAgentDo authenticationServiceAgentDo = new AuthenticationServiceAgentDo();
        authenticationServiceAgentDo.setUid(this.idHelper.getNextDistributedId(AuthenticationServiceComponentDo.RESOURCE_NAME));
        authenticationServiceAgentDo.setName(createAuthenticationServiceAgentDto.getName());
        authenticationServiceAgentDo.setObjectName(
                createAuthenticationServiceAgentDto.getName()
                        .replaceAll("_", "__")
                        .replaceAll("\\s", "_")
                        .toLowerCase());
        authenticationServiceAgentDo.setDescription(createAuthenticationServiceAgentDto.getDescription());
        authenticationServiceAgentDo.setSequence(createAuthenticationServiceAgentDto.getSequence());
        authenticationServiceAgentDo.setEnabled(createAuthenticationServiceAgentDto.getEnabled());
        authenticationServiceAgentDo.setPreferred(createAuthenticationServiceAgentDto.getPreferred());
        authenticationServiceAgentDo.setServiceComponentUid(createAuthenticationServiceAgentDto.getServiceComponentUid());
        authenticationServiceAgentDo.setConfiguration(createAuthenticationServiceAgentDto.getConfiguration());
        authenticationServiceAgentDo.setAccountTypeUidList(createAuthenticationServiceAgentDto.getAccountTypeUidList());
        authenticationServiceAgentDo.setProperties(createAuthenticationServiceAgentDto.getProperties());

        BaseDo.create(authenticationServiceAgentDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.authenticationServiceAgentRepository.save(authenticationServiceAgentDo);

        //
        // Step 3, post-processing
        //

        // preferred 是排它的
        if (Boolean.TRUE.equals(authenticationServiceAgentDo.getPreferred())) {
            this.authenticationServiceAgentRepository.findAll().forEach(candidateAuthenticationServiceAgentDo -> {
                if (authenticationServiceAgentDo.getUid().equals(candidateAuthenticationServiceAgentDo.getUid())) {
                    return;
                }

                if (Boolean.TRUE.equals(candidateAuthenticationServiceAgentDo.getPreferred())) {
                    candidateAuthenticationServiceAgentDo.setPreferred(Boolean.FALSE);
                    BaseDo.update(candidateAuthenticationServiceAgentDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.authenticationServiceAgentRepository.save(candidateAuthenticationServiceAgentDo);
                }
            });
        }

        AuthenticationServiceAgentDto authenticationServiceAgentDto = new AuthenticationServiceAgentDto();
        BeanUtils.copyProperties(authenticationServiceAgentDo, authenticationServiceAgentDto);

        AuthenticationServiceComponentDto authenticationServiceComponentDto =
                this.authenticationServiceComponentService.getAuthenticationServiceComponent(
                        authenticationServiceAgentDo.getServiceComponentUid(), operatingUserProfile);
        authenticationServiceAgentDto.setServiceComponent(authenticationServiceComponentDto);

        return authenticationServiceAgentDto;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateAuthenticationServiceAgent(
            Long uid,
            UpdateAuthenticationServiceAgentDto updateAuthenticationServiceAgentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        AuthenticationServiceAgentDo authenticationServiceAgentDo = this.authenticationServiceAgentRepository.findByUid(uid);
        if (authenticationServiceAgentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", AuthenticationServiceAgentDo.RESOURCE_SYMBOL,
                    uid));
        }

        if (!ObjectUtils.isEmpty(updateAuthenticationServiceAgentDto.getName())
                && !updateAuthenticationServiceAgentDto.getName().equalsIgnoreCase(authenticationServiceAgentDo.getName())) {
            boolean existsDuplicate =
                    this.authenticationServiceAgentRepository.existsByName(updateAuthenticationServiceAgentDto.getName());
            if (existsDuplicate) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s", AuthenticationServiceAgentDo.RESOURCE_SYMBOL,
                        updateAuthenticationServiceAgentDto.getName()));
            }
        }

        if (updateAuthenticationServiceAgentDto.getServiceComponentUid() != null
                && !updateAuthenticationServiceAgentDto.getServiceComponentUid().equals(authenticationServiceAgentDo.getServiceComponentUid())) {
            boolean existsAuthenticationServiceComponent = this.authenticationServiceComponentRepository.existsByUid(
                    updateAuthenticationServiceAgentDto.getServiceComponentUid());
            if (!existsAuthenticationServiceComponent) {
                throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                        AuthenticationServiceComponentDo.RESOURCE_SYMBOL,
                        updateAuthenticationServiceAgentDto.getServiceComponentUid()));
            }
        }

        //
        // Step 2, core-processing
        //

        boolean requiredToUpdate = false;

        if (!ObjectUtils.isEmpty(updateAuthenticationServiceAgentDto.getName())
                && !updateAuthenticationServiceAgentDto.getName().equalsIgnoreCase(authenticationServiceAgentDo.getName())) {
            authenticationServiceAgentDo.setName(updateAuthenticationServiceAgentDto.getName());

            authenticationServiceAgentDo.setObjectName(
                    updateAuthenticationServiceAgentDto.getName()
                            .replaceAll("_", "__")
                            .replaceAll("\\s", "_")
                            .toLowerCase());

            requiredToUpdate = true;
        }
        if (updateAuthenticationServiceAgentDto.getDescription() != null
                && !updateAuthenticationServiceAgentDto.getDescription().equals(authenticationServiceAgentDo.getDescription())) {
            authenticationServiceAgentDo.setDescription(updateAuthenticationServiceAgentDto.getDescription());
            requiredToUpdate = true;
        }
        if (updateAuthenticationServiceAgentDto.getSequence() != null
                && !updateAuthenticationServiceAgentDto.getSequence().equals(authenticationServiceAgentDo.getSequence())) {
            authenticationServiceAgentDo.setSequence(updateAuthenticationServiceAgentDto.getSequence());
            requiredToUpdate = true;
        }
        if (updateAuthenticationServiceAgentDto.getEnabled() != null
                && !updateAuthenticationServiceAgentDto.getEnabled().equals(authenticationServiceAgentDo.getEnabled())) {
            authenticationServiceAgentDo.setEnabled(updateAuthenticationServiceAgentDto.getEnabled());
            requiredToUpdate = true;
        }
        if (updateAuthenticationServiceAgentDto.getPreferred() != null
                && !updateAuthenticationServiceAgentDto.getPreferred().equals(authenticationServiceAgentDo.getPreferred())) {
            authenticationServiceAgentDo.setPreferred(updateAuthenticationServiceAgentDto.getPreferred());
            requiredToUpdate = true;
        }
        if (!ObjectUtils.isEmpty(updateAuthenticationServiceAgentDto.getConfiguration())) {
            authenticationServiceAgentDo.setConfiguration(updateAuthenticationServiceAgentDto.getConfiguration());
            requiredToUpdate = true;
        }
        if (!CollectionUtils.isEmpty(updateAuthenticationServiceAgentDto.getAccountTypeUidList())) {
            authenticationServiceAgentDo.setAccountTypeUidList(updateAuthenticationServiceAgentDto.getAccountTypeUidList());
            requiredToUpdate = true;
        }
        if (!CollectionUtils.isEmpty(updateAuthenticationServiceAgentDto.getProperties())) {
            authenticationServiceAgentDo.setProperties(updateAuthenticationServiceAgentDto.getProperties());
            requiredToUpdate = true;
        }
        if (updateAuthenticationServiceAgentDto.getServiceComponentUid() != null
                && !updateAuthenticationServiceAgentDto.getServiceComponentUid().equals(authenticationServiceAgentDo.getServiceComponentUid())) {
            authenticationServiceAgentDo.setServiceComponentUid(updateAuthenticationServiceAgentDto.getServiceComponentUid());
            requiredToUpdate = true;
        }

        if (requiredToUpdate) {
            BaseDo.update(authenticationServiceAgentDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.authenticationServiceAgentRepository.save(authenticationServiceAgentDo);
        }

        //
        // Step 3, post-processing
        //
        // preferred 是排它的
        if (Boolean.TRUE.equals(authenticationServiceAgentDo.getPreferred())) {
            this.authenticationServiceAgentRepository.findAll().forEach(candidateAuthenticationServiceAgentDo -> {
                if (authenticationServiceAgentDo.getUid().equals(candidateAuthenticationServiceAgentDo.getUid())) {
                    return;
                }

                if (Boolean.TRUE.equals(candidateAuthenticationServiceAgentDo.getPreferred())) {
                    candidateAuthenticationServiceAgentDo.setPreferred(Boolean.FALSE);
                    BaseDo.update(candidateAuthenticationServiceAgentDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.authenticationServiceAgentRepository.save(candidateAuthenticationServiceAgentDo);
                }
            });
        }
    }

    @Override
    public List<String> listAllReferencesToAuthenticationServiceAgent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        AuthenticationServiceAgentDo authenticationServiceAgentDo = this.authenticationServiceAgentRepository.findByUid(uid);
        if (authenticationServiceAgentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", AuthenticationServiceAgentDo.RESOURCE_SYMBOL,
                    uid));
        }

        //
        // Step 2, core-processing
        //
        return this.resourceReferenceManager.check(
                ResourceReferenceManager.ResourceCategoryEnum.AUTHENTICATION_SERVICE_AGENT,
                authenticationServiceAgentDo.getUid(),
                authenticationServiceAgentDo.getName());
    }

    @Override
    public void deleteAuthenticationServiceAgent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        AuthenticationServiceAgentDo authenticationServiceAgentDo = this.authenticationServiceAgentRepository.findByUid(uid);
        if (authenticationServiceAgentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", AuthenticationServiceComponentDo.RESOURCE_SYMBOL,
                    uid));
        }

        deleteAuthenticationServiceAgent(authenticationServiceAgentDo, operatingUserProfile);
    }

    private void deleteAuthenticationServiceAgent(
            AuthenticationServiceAgentDo authenticationServiceAgentDo,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        authenticationServiceAgentDo.setDeleted(Boolean.TRUE);
        BaseDo.update(authenticationServiceAgentDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.authenticationServiceAgentRepository.save(authenticationServiceAgentDo);

        // event post
        AuthenticationServiceAgentDeletedEvent event = new AuthenticationServiceAgentDeletedEvent();
        event.setAuthenticationServiceAgentDo(authenticationServiceAgentDo);
        event.setOperatingUserProfile(operatingUserProfile);
        this.eventBusManager.post(event);
    }

    @Override
    public AuthenticationServiceAgentDto getAuthenticationServiceAgent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        AuthenticationServiceAgentDo authenticationServiceAgentDo = this.authenticationServiceAgentRepository.findByUid(uid);
        if (authenticationServiceAgentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", AuthenticationServiceAgentDo.RESOURCE_SYMBOL,
                    uid));
        }

        AuthenticationServiceAgentDto authenticationServiceAgentDto = new AuthenticationServiceAgentDto();
        BeanUtils.copyProperties(authenticationServiceAgentDo, authenticationServiceAgentDto);

        AuthenticationServiceComponentDto authenticationServiceComponentDto =
                this.authenticationServiceComponentService.getAuthenticationServiceComponent(
                        authenticationServiceAgentDo.getServiceComponentUid(), operatingUserProfile);
        authenticationServiceAgentDto.setServiceComponent(authenticationServiceComponentDto);

        return authenticationServiceAgentDto;
    }

    @Override
    public List<AuthenticationServiceAgentDto> listingQueryAuthenticationServiceAgents(
            Long uid,
            String name,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // step 1, pre-processing
        //

        //
        // step 2, core-processing
        //
        Specification<AuthenticationServiceAgentDo> specification = new Specification<AuthenticationServiceAgentDo>() {
            @Override
            public Predicate toPredicate(Root<AuthenticationServiceAgentDo> root, CriteriaQuery<?> query,
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

        if (sort == null || sort.isUnsorted()) {
            sort = Sort.by(Sort.Order.asc("sequence"));
        }

        List<AuthenticationServiceAgentDo> itemDoList = this.authenticationServiceAgentRepository.findAll(specification, sort);

        if (CollectionUtils.isEmpty(itemDoList)) {
            return null;
        }

        //
        // step 3, post-processing
        //

        //
        // step 3.1, 为 created by, last modified by 补充 user brief information
        //
        List<Long> userUidList = new LinkedList<>();
        itemDoList.forEach(itemDo -> {
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
        List<AuthenticationServiceAgentDto> content = new LinkedList<>();
        itemDoList.forEach(itemDo -> {
            AuthenticationServiceAgentDto itemDto = new AuthenticationServiceAgentDto();
            BeanUtils.copyProperties(itemDo, itemDto);

            AuthenticationServiceComponentDto authenticationServiceComponentDto =
                    this.authenticationServiceComponentService.getAuthenticationServiceComponent(
                            itemDo.getServiceComponentUid(), operatingUserProfile);
            itemDto.setServiceComponent(authenticationServiceComponentDto);

            // 为 created by, last modified by 补充 user brief information
            itemDto.setCreatedByUser(userBriefInformationMap.get(itemDo.getCreatedBy()));
            itemDto.setLastModifiedByUser(userBriefInformationMap.get(itemDo.getLastModifiedBy()));

            content.add(itemDto);
        });
        return content;
    }

    @Override
    public Page<AuthenticationServiceAgentDto> pagingQueryAuthenticationServiceAgents(
            Long uid,
            String name,
            String description,
            Boolean enabled,
            Boolean preferred,
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
        Specification<AuthenticationServiceAgentDo> specification = new Specification<AuthenticationServiceAgentDo>() {
            @Override
            public Predicate toPredicate(Root<AuthenticationServiceAgentDo> root, CriteriaQuery<?> query,
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
                if (enabled != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("enabled"), enabled));
                }
                if (preferred != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("preferred"), preferred));
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

        if (pageable == null) {
            pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.asc(
                    "sequence")));
        } else if (pageable.getSort() == null || pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Order.asc(
                    "sequence")));
        }


        Page<AuthenticationServiceAgentDo> itemDoPage = this.authenticationServiceAgentRepository.findAll(specification, pageable);

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
        List<AuthenticationServiceAgentDto> content = new ArrayList<>(itemDoPage.getSize());
        itemDoPage.forEach(itemDo -> {
            AuthenticationServiceAgentDto itemDto = new AuthenticationServiceAgentDto();
            BeanUtils.copyProperties(itemDo, itemDto);

            AuthenticationServiceComponentDto authenticationServiceComponentDto =
                    this.authenticationServiceComponentService.getAuthenticationServiceComponent(
                            itemDo.getServiceComponentUid(), operatingUserProfile);
            itemDto.setServiceComponent(authenticationServiceComponentDto);

            // 为 created by, last modified by 补充 user brief information
            itemDto.setCreatedByUser(userBriefInformationMap.get(itemDo.getCreatedBy()));
            itemDto.setLastModifiedByUser(userBriefInformationMap.get(itemDo.getLastModifiedBy()));

            content.add(itemDto);
        });
        Page<AuthenticationServiceAgentDto> itemDtoPage = new PageImpl<>(content, pageable, itemDoPage.getTotalElements());
        return itemDtoPage;
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
    public void handleAuthenticationServiceComponentDeletedEvent(
            AuthenticationServiceComponentDeletedEvent event) {
        LOGGER.info("rcv event:{}", event);

        List<AuthenticationServiceAgentDo> authenticationServiceAgentDoList =
                this.authenticationServiceAgentRepository.findByServiceComponentUid(
                        event.getAuthenticationServiceComponentDo().getUid());
        if (!CollectionUtils.isEmpty(authenticationServiceAgentDoList)) {
            authenticationServiceAgentDoList.forEach(authenticationServiceAgentDo -> {
                deleteAuthenticationServiceAgent(authenticationServiceAgentDo, event.getOperatingUserProfile());
            });
        }
    }

    @ResourceReferenceHandler(name = "authentication service agent")
    public List<String> checkResourceReference(
            ResourceReferenceManager.ResourceCategoryEnum resourceCategory,
            Long resourceUid,
            String resourceName) throws Exception {
        switch (resourceCategory) {
            case AUTHENTICATION_SERVICE_COMPONENT: {
                Long serviceComponentUid = resourceUid;

                List<AuthenticationServiceAgentDo> serviceAgentDoList =
                        this.authenticationServiceAgentRepository.findByServiceComponentUid(serviceComponentUid);
                if (!CollectionUtils.isEmpty(serviceAgentDoList)) {
                    List<String> result = new LinkedList<>();
                    serviceAgentDoList.forEach(serviceAgentDo -> {
                        result.add(String.format(
                                "[%s] %s (%d)",
                                AuthenticationServiceAgentDo.RESOURCE_SYMBOL,
                                serviceAgentDo.getName(),
                                serviceAgentDo.getUid()));
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
