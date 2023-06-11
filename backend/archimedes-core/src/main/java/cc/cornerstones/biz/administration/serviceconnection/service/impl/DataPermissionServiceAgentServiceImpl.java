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
import cc.cornerstones.biz.administration.serviceconnection.entity.DataPermissionServiceAgentDo;
import cc.cornerstones.biz.administration.serviceconnection.entity.DataPermissionServiceComponentDo;
import cc.cornerstones.biz.administration.serviceconnection.persistence.DataPermissionServiceComponentRepository;
import cc.cornerstones.biz.administration.serviceconnection.persistence.DataPermissionServiceAgentRepository;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DataPermissionServiceComponentService;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DataPermissionServiceAgentService;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DfsServiceAgentService;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.share.assembly.ResourceReferenceManager;
import cc.cornerstones.biz.share.event.*;
import cc.cornerstones.biz.share.types.ResourceReferenceHandler;
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
public class DataPermissionServiceAgentServiceImpl implements DataPermissionServiceAgentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataPermissionServiceAgentServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private DataPermissionServiceComponentRepository dataPermissionServiceComponentRepository;

    @Autowired
    private DataPermissionServiceComponentService dataPermissionServiceComponentService;

    @Autowired
    private DataPermissionServiceAgentRepository dataPermissionServiceAgentRepository;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    private PluginHelper pluginHelper;

    @Autowired
    private DfsServiceAgentService dfsServiceAgentService;

    @Autowired
    private UserService userService;

    @Autowired
    private ResourceReferenceManager resourceReferenceManager;


    @Override
    public DataPermissionServiceAgentDto createDataPermissionServiceAgent(
            CreateDataPermissionServiceAgentDto createDataPermissionServiceAgentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        boolean existsDuplicate =
                this.dataPermissionServiceAgentRepository.existsByName(createDataPermissionServiceAgentDto.getName());
        if (existsDuplicate) {
            throw new AbcResourceDuplicateException(String.format("%s::name=%s", DataPermissionServiceAgentDo.RESOURCE_SYMBOL,
                    createDataPermissionServiceAgentDto.getName()));
        }

        boolean existsDataPermissionServiceComponent = this.dataPermissionServiceComponentRepository.existsByUid(
                createDataPermissionServiceAgentDto.getServiceComponentUid());
        if (!existsDataPermissionServiceComponent) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DataPermissionServiceComponentDo.RESOURCE_SYMBOL,
                    createDataPermissionServiceAgentDto.getServiceComponentUid()));
        }

        //
        // Step 2, core-processing
        //
        DataPermissionServiceAgentDo dataPermissionServiceAgentDo = new DataPermissionServiceAgentDo();
        dataPermissionServiceAgentDo.setUid(this.idHelper.getNextDistributedId(DataPermissionServiceAgentDo.RESOURCE_NAME));
        dataPermissionServiceAgentDo.setName(createDataPermissionServiceAgentDto.getName());
        dataPermissionServiceAgentDo.setObjectName(
                createDataPermissionServiceAgentDto.getName()
                        .replaceAll("_", "__")
                        .replaceAll("\\s", "_")
                        .toLowerCase());
        dataPermissionServiceAgentDo.setDescription(createDataPermissionServiceAgentDto.getDescription());
        dataPermissionServiceAgentDo.setSequence(createDataPermissionServiceAgentDto.getSequence());
        dataPermissionServiceAgentDo.setEnabled(createDataPermissionServiceAgentDto.getEnabled());
        dataPermissionServiceAgentDo.setServiceComponentUid(createDataPermissionServiceAgentDto.getServiceComponentUid());

        dataPermissionServiceAgentDo.setConfiguration(createDataPermissionServiceAgentDto.getConfiguration());
        dataPermissionServiceAgentDo.setAccountTypeUid(createDataPermissionServiceAgentDto.getAccountTypeUid());

        BaseDo.create(dataPermissionServiceAgentDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dataPermissionServiceAgentRepository.save(dataPermissionServiceAgentDo);

        //
        // Step 3, post-processing
        //
        DataPermissionServiceAgentDto dataPermissionServiceAgentDto = new DataPermissionServiceAgentDto();
        BeanUtils.copyProperties(dataPermissionServiceAgentDo, dataPermissionServiceAgentDto);

        DataPermissionServiceComponentDto dataPermissionServiceComponentDto =
                this.dataPermissionServiceComponentService.getDataPermissionServiceComponent(
                        dataPermissionServiceAgentDo.getServiceComponentUid(), operatingUserProfile);
        dataPermissionServiceAgentDto.setServiceComponent(dataPermissionServiceComponentDto);

        return dataPermissionServiceAgentDto;
    }

    @Override
    public void updateDataPermissionServiceAgent(
            Long uid,
            UpdateDataPermissionServiceAgentDto updateDataPermissionServiceAgentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DataPermissionServiceAgentDo dataPermissionServiceAgentDo = this.dataPermissionServiceAgentRepository.findByUid(uid);
        if (dataPermissionServiceAgentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataPermissionServiceAgentDo.RESOURCE_SYMBOL,
                    uid));
        }

        if (!ObjectUtils.isEmpty(updateDataPermissionServiceAgentDto.getName())
                && !updateDataPermissionServiceAgentDto.getName().equalsIgnoreCase(dataPermissionServiceAgentDo.getName())) {
            boolean existsDuplicate =
                    this.dataPermissionServiceAgentRepository.existsByName(updateDataPermissionServiceAgentDto.getName());
            if (existsDuplicate) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s", DataPermissionServiceAgentDo.RESOURCE_SYMBOL,
                        updateDataPermissionServiceAgentDto.getName()));
            }
        }

        if (updateDataPermissionServiceAgentDto.getServiceComponentUid() != null
                && !updateDataPermissionServiceAgentDto.getServiceComponentUid().equals(dataPermissionServiceAgentDo.getServiceComponentUid())) {
            boolean existsDataPermissionServiceComponent = this.dataPermissionServiceComponentRepository.existsByUid(
                    updateDataPermissionServiceAgentDto.getServiceComponentUid());
            if (!existsDataPermissionServiceComponent) {
                throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                        DataPermissionServiceComponentDo.RESOURCE_SYMBOL,
                        updateDataPermissionServiceAgentDto.getServiceComponentUid()));
            }
        }

        //
        // Step 2, core-processing
        //
        boolean requiredToUpdate = false;

        if (!ObjectUtils.isEmpty(updateDataPermissionServiceAgentDto.getName())
                && !updateDataPermissionServiceAgentDto.getName().equalsIgnoreCase(dataPermissionServiceAgentDo.getName())) {
            dataPermissionServiceAgentDo.setName(updateDataPermissionServiceAgentDto.getName());
            dataPermissionServiceAgentDo.setObjectName(
                    updateDataPermissionServiceAgentDto.getName()
                            .replaceAll("_", "__")
                            .replaceAll("\\s", "_")
                            .toLowerCase());
            requiredToUpdate = true;
        }
        if (updateDataPermissionServiceAgentDto.getDescription() != null
                && !updateDataPermissionServiceAgentDto.getDescription().equalsIgnoreCase(dataPermissionServiceAgentDo.getDescription())) {
            dataPermissionServiceAgentDo.setDescription(updateDataPermissionServiceAgentDto.getDescription());
            requiredToUpdate = true;
        }
        if (updateDataPermissionServiceAgentDto.getSequence() != null
                && !updateDataPermissionServiceAgentDto.getSequence().equals(dataPermissionServiceAgentDo.getSequence())) {
            dataPermissionServiceAgentDo.setSequence(updateDataPermissionServiceAgentDto.getSequence());
            requiredToUpdate = true;
        }
        if (updateDataPermissionServiceAgentDto.getEnabled() != null
                && !updateDataPermissionServiceAgentDto.getEnabled().equals(dataPermissionServiceAgentDo.getEnabled())) {
            dataPermissionServiceAgentDo.setEnabled(updateDataPermissionServiceAgentDto.getEnabled());
            requiredToUpdate = true;
        }
        if (!ObjectUtils.isEmpty(updateDataPermissionServiceAgentDto.getConfiguration())) {
            dataPermissionServiceAgentDo.setConfiguration(updateDataPermissionServiceAgentDto.getConfiguration());
            requiredToUpdate = true;
        }
        if (updateDataPermissionServiceAgentDto.getAccountTypeUid() != null
                && !updateDataPermissionServiceAgentDto.getAccountTypeUid().equals(dataPermissionServiceAgentDo.getAccountTypeUid())) {
            dataPermissionServiceAgentDo.setAccountTypeUid(updateDataPermissionServiceAgentDto.getAccountTypeUid());
            requiredToUpdate = true;
        }
        if (updateDataPermissionServiceAgentDto.getServiceComponentUid() != null
                && !updateDataPermissionServiceAgentDto.getServiceComponentUid().equals(dataPermissionServiceAgentDo.getServiceComponentUid())) {
            dataPermissionServiceAgentDo.setServiceComponentUid(updateDataPermissionServiceAgentDto.getServiceComponentUid());
            requiredToUpdate = true;
        }

        if (requiredToUpdate) {
            BaseDo.update(dataPermissionServiceAgentDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.dataPermissionServiceAgentRepository.save(dataPermissionServiceAgentDo);
        }
    }

    @Override
    public List<String> listAllReferencesToDataPermissionServiceAgent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DataPermissionServiceAgentDo dataPermissionServiceAgentDo = this.dataPermissionServiceAgentRepository.findByUid(uid);
        if (dataPermissionServiceAgentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataPermissionServiceAgentDo.RESOURCE_SYMBOL,
                    uid));
        }

        //
        // Step 2, core-processing
        //
        return this.resourceReferenceManager.check(
                ResourceReferenceManager.ResourceCategoryEnum.DATA_PERMISSION_SERVICE_AGENT,
                dataPermissionServiceAgentDo.getUid(),
                dataPermissionServiceAgentDo.getName());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteDataPermissionServiceAgent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        DataPermissionServiceAgentDo dataPermissionServiceAgentDo = this.dataPermissionServiceAgentRepository.findByUid(uid);
        if (dataPermissionServiceAgentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataPermissionServiceAgentDo.RESOURCE_SYMBOL,
                    uid));
        }

        deleteDataPermissionServiceAgent(dataPermissionServiceAgentDo, operatingUserProfile);
    }

    public void deleteDataPermissionServiceAgent(
            DataPermissionServiceAgentDo dataPermissionServiceAgentDo,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        dataPermissionServiceAgentDo.setDeleted(Boolean.TRUE);
        BaseDo.update(dataPermissionServiceAgentDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dataPermissionServiceAgentRepository.save(dataPermissionServiceAgentDo);

        // event post
        DataPermissionServiceAgentDeletedEvent dataPermissionServiceAgentDeletedEvent = new DataPermissionServiceAgentDeletedEvent();
        dataPermissionServiceAgentDeletedEvent.setDataPermissionServiceAgentDo(dataPermissionServiceAgentDo);
        dataPermissionServiceAgentDeletedEvent.setOperatingUserProfile(operatingUserProfile);
        this.eventBusManager.post(dataPermissionServiceAgentDeletedEvent);
    }

    @Override
    public DataPermissionServiceAgentDto getDataPermissionServiceAgent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        DataPermissionServiceAgentDo dataPermissionServiceAgentDo = this.dataPermissionServiceAgentRepository.findByUid(uid);
        if (dataPermissionServiceAgentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataPermissionServiceAgentDo.RESOURCE_SYMBOL,
                    uid));
        }

        DataPermissionServiceAgentDto dataPermissionServiceAgentDto = new DataPermissionServiceAgentDto();
        BeanUtils.copyProperties(dataPermissionServiceAgentDo, dataPermissionServiceAgentDto);

        DataPermissionServiceComponentDto dataPermissionServiceComponentDto =
                this.dataPermissionServiceComponentService.getDataPermissionServiceComponent(
                        dataPermissionServiceAgentDo.getServiceComponentUid(), operatingUserProfile);
        dataPermissionServiceAgentDto.setServiceComponent(dataPermissionServiceComponentDto);

        return dataPermissionServiceAgentDto;
    }

    @Override
    public List<DataPermissionServiceAgentDto> listingQueryDataPermissionServiceAgents(
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
        Specification<DataPermissionServiceAgentDo> specification = new Specification<DataPermissionServiceAgentDo>() {
            @Override
            public Predicate toPredicate(Root<DataPermissionServiceAgentDo> root, CriteriaQuery<?> query,
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

        List<DataPermissionServiceAgentDo> itemDoList = this.dataPermissionServiceAgentRepository.findAll(specification, sort);

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
        List<DataPermissionServiceAgentDto> content = new LinkedList<>();
        itemDoList.forEach(itemDo -> {
            DataPermissionServiceAgentDto itemDto = new DataPermissionServiceAgentDto();
            BeanUtils.copyProperties(itemDo, itemDto);

            DataPermissionServiceComponentDto dataPermissionServiceComponentDto =
                    this.dataPermissionServiceComponentService.getDataPermissionServiceComponent(
                            itemDo.getServiceComponentUid(), operatingUserProfile);
            itemDto.setServiceComponent(dataPermissionServiceComponentDto);

            // 为 created by, last modified by 补充 user brief information
            itemDto.setCreatedByUser(userBriefInformationMap.get(itemDo.getCreatedBy()));
            itemDto.setLastModifiedByUser(userBriefInformationMap.get(itemDo.getLastModifiedBy()));

            content.add(itemDto);
        });
        return content;
    }

    @Override
    public Page<DataPermissionServiceAgentDto> pagingQueryDataPermissionServiceAgents(
            Long uid,
            String name,
            String description,
            Boolean enabled,
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
        Specification<DataPermissionServiceAgentDo> specification = new Specification<DataPermissionServiceAgentDo>() {
            @Override
            public Predicate toPredicate(Root<DataPermissionServiceAgentDo> root, CriteriaQuery<?> query,
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

        Page<DataPermissionServiceAgentDo> itemDoPage = this.dataPermissionServiceAgentRepository.findAll(specification, pageable);

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
        List<DataPermissionServiceAgentDto> content = new ArrayList<>(itemDoPage.getSize());
        itemDoPage.forEach(itemDo -> {
            DataPermissionServiceAgentDto itemDto = new DataPermissionServiceAgentDto();
            BeanUtils.copyProperties(itemDo, itemDto);

            DataPermissionServiceComponentDto dataPermissionServiceComponentDto =
                    this.dataPermissionServiceComponentService.getDataPermissionServiceComponent(
                            itemDo.getServiceComponentUid(),
                            operatingUserProfile);
            itemDto.setServiceComponent(dataPermissionServiceComponentDto);

            // 为 created by, last modified by 补充 user brief information
            itemDto.setCreatedByUser(userBriefInformationMap.get(itemDo.getCreatedBy()));
            itemDto.setLastModifiedByUser(userBriefInformationMap.get(itemDo.getLastModifiedBy()));

            content.add(itemDto);
        });
        Page<DataPermissionServiceAgentDto> itemDtoPage = new PageImpl<>(content, pageable, itemDoPage.getTotalElements());
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
    public void handleDataPermissionServiceComponentDeletedEvent(DataPermissionServiceComponentDeletedEvent event) {
        LOGGER.info("rcv event:{}", event);

        List<DataPermissionServiceAgentDo> dataPermissionServiceAgentDoList =
                this.dataPermissionServiceAgentRepository.findByServiceComponentUid(event.getDataPermissionServiceComponentDo().getUid());
        if (!CollectionUtils.isEmpty(dataPermissionServiceAgentDoList)) {
            dataPermissionServiceAgentDoList.forEach(dataPermissionServiceAgentDo -> {
                deleteDataPermissionServiceAgent(dataPermissionServiceAgentDo, event.getOperatingUserProfile());
            });
        }
    }

    @ResourceReferenceHandler(name = "data permission service agent")
    public List<String> checkResourceReference(
            ResourceReferenceManager.ResourceCategoryEnum resourceCategory,
            Long resourceUid,
            String resourceName) throws Exception {
        switch (resourceCategory) {
            case DATA_PERMISSION_SERVICE_COMPONENT: {
                Long serviceComponentUid = resourceUid;

                List<DataPermissionServiceAgentDo> serviceAgentDoList =
                        this.dataPermissionServiceAgentRepository.findByServiceComponentUid(serviceComponentUid);
                if (!CollectionUtils.isEmpty(serviceAgentDoList)) {
                    List<String> result = new LinkedList<>();
                    serviceAgentDoList.forEach(serviceAgentDo -> {
                        result.add(String.format(
                                "[%s] %s (%d)",
                                DataPermissionServiceAgentDo.RESOURCE_SYMBOL,
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
