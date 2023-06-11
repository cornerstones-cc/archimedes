package cc.cornerstones.biz.administration.serviceconnection.service.impl;

import cc.cornerstones.almond.constants.DatabaseConstants;
import cc.cornerstones.almond.exceptions.*;
import cc.cornerstones.almond.types.AbcTuple3;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserBriefInformation;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.arbutus.pf4j.service.assembly.PluginHelper;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.administration.serviceconnection.dto.*;
import cc.cornerstones.biz.administration.serviceconnection.entity.AuthenticationServiceComponentDo;
import cc.cornerstones.biz.administration.serviceconnection.entity.DataPermissionServiceAgentDo;
import cc.cornerstones.biz.administration.serviceconnection.entity.DfsServiceAgentDo;
import cc.cornerstones.biz.administration.serviceconnection.entity.DfsServiceComponentDo;
import cc.cornerstones.biz.administration.serviceconnection.persistence.DfsServiceComponentRepository;
import cc.cornerstones.biz.administration.serviceconnection.persistence.DfsServiceAgentRepository;
import cc.cornerstones.biz.administration.serviceconnection.service.assembly.DfsServiceHandler;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DfsServiceComponentService;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DfsServiceAgentService;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.share.assembly.ResourceReferenceManager;
import cc.cornerstones.biz.share.event.DataPermissionServiceComponentDeletedEvent;
import cc.cornerstones.biz.share.event.DfsServiceAgentDeletedEvent;
import cc.cornerstones.biz.share.event.DfsServiceComponentDeletedEvent;
import cc.cornerstones.biz.share.event.EventBusManager;
import cc.cornerstones.biz.share.types.ResourceReferenceHandler;
import com.google.common.eventbus.Subscribe;
import javafx.beans.property.ReadOnlyListProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
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
import java.io.*;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DfsServiceAgentServiceImpl implements DfsServiceAgentService, ApplicationListener<ApplicationReadyEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DfsServiceAgentServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private DfsServiceComponentRepository dfsServiceComponentRepository;

    @Autowired
    private DfsServiceComponentService dfsServiceComponentService;

    @Autowired
    private DfsServiceAgentRepository dfsServiceAgentRepository;


    @Autowired
    private ApplicationContext applicationContext;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    private PluginHelper pluginHelper;

    @Autowired
    private DfsServiceHandler dfsServiceHandler;

    @Autowired
    private UserService userService;

    @Autowired
    private ResourceReferenceManager resourceReferenceManager;


    /**
     * Handle an application event.
     *
     * @param event the event to respond to
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public DfsServiceAgentDto createDfsServiceAgent(
            CreateDfsServiceAgentDto createDfsServiceAgentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        boolean existsDuplicate =
                this.dfsServiceAgentRepository.existsByName(createDfsServiceAgentDto.getName());
        if (existsDuplicate) {
            throw new AbcResourceDuplicateException(String.format("%s::name=%s", DfsServiceAgentDo.RESOURCE_SYMBOL,
                    createDfsServiceAgentDto.getName()));
        }

        boolean existsDfsServiceComponent = this.dfsServiceComponentRepository.existsByUid(
                createDfsServiceAgentDto.getServiceComponentUid());
        if (!existsDfsServiceComponent) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DfsServiceComponentDo.RESOURCE_SYMBOL,
                    createDfsServiceAgentDto.getServiceComponentUid()));
        }

        //
        // Step 2, core-processing
        //
        DfsServiceAgentDo dfsServiceAgentDo = new DfsServiceAgentDo();
        dfsServiceAgentDo.setUid(this.idHelper.getNextDistributedId(DfsServiceAgentDo.RESOURCE_NAME));
        dfsServiceAgentDo.setName(createDfsServiceAgentDto.getName());
        dfsServiceAgentDo.setObjectName(
                createDfsServiceAgentDto.getName()
                        .replaceAll("_", "__")
                        .replaceAll("\\s", "_")
                        .toLowerCase());
        dfsServiceAgentDo.setDescription(createDfsServiceAgentDto.getDescription());
        dfsServiceAgentDo.setSequence(createDfsServiceAgentDto.getSequence());
        dfsServiceAgentDo.setEnabled(createDfsServiceAgentDto.getEnabled());
        dfsServiceAgentDo.setPreferred(createDfsServiceAgentDto.getPreferred());
        dfsServiceAgentDo.setServiceComponentUid(createDfsServiceAgentDto.getServiceComponentUid());

        dfsServiceAgentDo.setConfiguration(createDfsServiceAgentDto.getConfiguration());

        BaseDo.create(dfsServiceAgentDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dfsServiceAgentRepository.save(dfsServiceAgentDo);

        //
        // Step 3, post-processing
        //

        // Preferred 最多只能有1个，具有排它性
        if (Boolean.TRUE.equals(dfsServiceAgentDo.getPreferred())) {
            List<DfsServiceAgentDo> toUpdateItemDoList = new LinkedList<>();
            this.dfsServiceAgentRepository.findAll().forEach(candidateDfsServiceAgentDo -> {
                if (Boolean.TRUE.equals(candidateDfsServiceAgentDo.getPreferred())
                        && !candidateDfsServiceAgentDo.getUid().equals(dfsServiceAgentDo.getUid())) {
                    candidateDfsServiceAgentDo.setPreferred(Boolean.FALSE);
                    BaseDo.update(candidateDfsServiceAgentDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    toUpdateItemDoList.add(candidateDfsServiceAgentDo);
                }
            });
            if (!CollectionUtils.isEmpty(toUpdateItemDoList)) {
                this.dfsServiceAgentRepository.saveAll(toUpdateItemDoList);
            }
        }

        DfsServiceAgentDto dfsServiceAgentDto = new DfsServiceAgentDto();
        BeanUtils.copyProperties(dfsServiceAgentDo, dfsServiceAgentDto);

        DfsServiceComponentDto dfsServiceComponentDto =
                this.dfsServiceComponentService.getDfsServiceComponent(
                        dfsServiceAgentDo.getServiceComponentUid(),
                        operatingUserProfile);
        dfsServiceAgentDto.setServiceComponent(dfsServiceComponentDto);

        return dfsServiceAgentDto;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateDfsServiceAgent(
            Long uid,
            UpdateDfsServiceAgentDto updateDfsServiceAgentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DfsServiceAgentDo dfsServiceAgentDo = this.dfsServiceAgentRepository.findByUid(uid);
        if (dfsServiceAgentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DfsServiceAgentDo.RESOURCE_SYMBOL,
                    uid));
        }

        if (!ObjectUtils.isEmpty(updateDfsServiceAgentDto.getName())
                && !updateDfsServiceAgentDto.getName().equalsIgnoreCase(dfsServiceAgentDo.getName())) {
            boolean existsDuplicate =
                    this.dfsServiceAgentRepository.existsByName(updateDfsServiceAgentDto.getName());
            if (existsDuplicate) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s", DfsServiceAgentDo.RESOURCE_SYMBOL,
                        updateDfsServiceAgentDto.getName()));
            }
        }

        if (updateDfsServiceAgentDto.getServiceComponentUid() != null
                && !updateDfsServiceAgentDto.getServiceComponentUid().equals(dfsServiceAgentDo.getServiceComponentUid())) {
            boolean existsDfsServiceComponent = this.dfsServiceComponentRepository.existsByUid(
                    updateDfsServiceAgentDto.getServiceComponentUid());
            if (!existsDfsServiceComponent) {
                throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                        DfsServiceComponentDo.RESOURCE_SYMBOL,
                        updateDfsServiceAgentDto.getServiceComponentUid()));
            }
        }

        //
        // Step 2, core-processing
        //
        boolean requiredToUpdate = false;

        if (!ObjectUtils.isEmpty(updateDfsServiceAgentDto.getName())
                && !updateDfsServiceAgentDto.getName().equalsIgnoreCase(dfsServiceAgentDo.getName())) {
            dfsServiceAgentDo.setName(updateDfsServiceAgentDto.getName());

            dfsServiceAgentDo.setObjectName(
                    updateDfsServiceAgentDto.getName()
                            .replaceAll("_", "__")
                            .replaceAll("\\s", "_")
                            .toLowerCase());

            requiredToUpdate = true;
        }
        if (updateDfsServiceAgentDto.getDescription() != null
                && !updateDfsServiceAgentDto.getDescription().equalsIgnoreCase(dfsServiceAgentDo.getDescription())) {
            dfsServiceAgentDo.setDescription(updateDfsServiceAgentDto.getDescription());
            requiredToUpdate = true;
        }
        if (updateDfsServiceAgentDto.getSequence() != null
                && !updateDfsServiceAgentDto.getSequence().equals(dfsServiceAgentDo.getSequence())) {
            dfsServiceAgentDo.setSequence(updateDfsServiceAgentDto.getSequence());
            requiredToUpdate = true;
        }
        if (updateDfsServiceAgentDto.getEnabled() != null
                && !updateDfsServiceAgentDto.getEnabled().equals(dfsServiceAgentDo.getEnabled())) {
            dfsServiceAgentDo.setEnabled(updateDfsServiceAgentDto.getEnabled());
            requiredToUpdate = true;
        }
        if (updateDfsServiceAgentDto.getPreferred() != null
                && !updateDfsServiceAgentDto.getPreferred().equals(dfsServiceAgentDo.getPreferred())) {
            dfsServiceAgentDo.setPreferred(updateDfsServiceAgentDto.getPreferred());
            requiredToUpdate = true;
        }
        if (!ObjectUtils.isEmpty(updateDfsServiceAgentDto.getConfiguration())) {
            dfsServiceAgentDo.setConfiguration(updateDfsServiceAgentDto.getConfiguration());
            requiredToUpdate = true;
        }
        if (updateDfsServiceAgentDto.getServiceComponentUid() != null
                && !updateDfsServiceAgentDto.getServiceComponentUid().equals(dfsServiceAgentDo.getServiceComponentUid())) {
            dfsServiceAgentDo.setServiceComponentUid(updateDfsServiceAgentDto.getServiceComponentUid());
            requiredToUpdate = true;
        }

        if (requiredToUpdate) {
            BaseDo.update(dfsServiceAgentDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.dfsServiceAgentRepository.save(dfsServiceAgentDo);
        }

        //
        // Step 3, post-processing
        //

        // Preferred 最多只能有1个，具有排它性
        if (Boolean.TRUE.equals(dfsServiceAgentDo.getPreferred())) {
            List<DfsServiceAgentDo> toUpdateItemDoList = new LinkedList<>();
            this.dfsServiceAgentRepository.findAll().forEach(candidateDfsServiceAgentDo -> {
                if (Boolean.TRUE.equals(candidateDfsServiceAgentDo.getPreferred())
                        && !candidateDfsServiceAgentDo.getUid().equals(dfsServiceAgentDo.getUid())) {
                    candidateDfsServiceAgentDo.setPreferred(Boolean.FALSE);
                    BaseDo.update(candidateDfsServiceAgentDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    toUpdateItemDoList.add(candidateDfsServiceAgentDo);
                }
            });
            if (!CollectionUtils.isEmpty(toUpdateItemDoList)) {
                this.dfsServiceAgentRepository.saveAll(toUpdateItemDoList);
            }
        }
    }

    @Override
    public List<String> listAllReferencesToDfsServiceAgent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DfsServiceAgentDo dfsServiceAgentDo = this.dfsServiceAgentRepository.findByUid(uid);
        if (dfsServiceAgentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DfsServiceAgentDo.RESOURCE_SYMBOL,
                    uid));
        }

        //
        // Step 2, core-processing
        //
        return this.resourceReferenceManager.check(
                ResourceReferenceManager.ResourceCategoryEnum.DFS_SERVICE_AGENT,
                dfsServiceAgentDo.getUid(),
                dfsServiceAgentDo.getName());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteDfsServiceAgent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        DfsServiceAgentDo dfsServiceAgentDo = this.dfsServiceAgentRepository.findByUid(uid);
        if (dfsServiceAgentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DfsServiceAgentDo.RESOURCE_SYMBOL,
                    uid));
        }

        deleteDfsServiceAgent(dfsServiceAgentDo, operatingUserProfile);
    }

    private void deleteDfsServiceAgent(
            DfsServiceAgentDo dfsServiceAgentDo,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        dfsServiceAgentDo.setDeleted(Boolean.TRUE);
        BaseDo.update(dfsServiceAgentDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dfsServiceAgentRepository.save(dfsServiceAgentDo);

        // event post
        DfsServiceAgentDeletedEvent dfsServiceAgentDeletedEvent = new DfsServiceAgentDeletedEvent();
        dfsServiceAgentDeletedEvent.setDfsServiceAgentDo(dfsServiceAgentDo);
        dfsServiceAgentDeletedEvent.setOperatingUserProfile(operatingUserProfile);
        this.eventBusManager.post(dfsServiceAgentDeletedEvent);
    }

    @Override
    public DfsServiceAgentDto getDfsServiceAgent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        DfsServiceAgentDo dfsServiceAgentDo = this.dfsServiceAgentRepository.findByUid(uid);
        if (dfsServiceAgentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DfsServiceAgentDo.RESOURCE_SYMBOL,
                    uid));
        }

        DfsServiceAgentDto dfsServiceAgentDto = new DfsServiceAgentDto();
        BeanUtils.copyProperties(dfsServiceAgentDo, dfsServiceAgentDto);

        DfsServiceComponentDto dfsServiceComponentDto =
                this.dfsServiceComponentService.getDfsServiceComponent(
                        dfsServiceAgentDo.getServiceComponentUid(),
                        operatingUserProfile);
        dfsServiceAgentDto.setServiceComponent(dfsServiceComponentDto);

        return dfsServiceAgentDto;
    }

    @Override
    public DfsServiceAgentDto getPreferredDfsServiceAgent(
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        List<DfsServiceAgentDo> dfsServiceAgentDoList =
                this.dfsServiceAgentRepository.findByEnabledAndPreferred(Boolean.TRUE, Boolean.TRUE);
        if (CollectionUtils.isEmpty(dfsServiceAgentDoList)) {
            throw new AbcResourceNotFoundException("cannot find preferred dfs service");
        }
        if (dfsServiceAgentDoList.size() > 1) {
            throw new AbcResourceConflictException("found out more than 1 preferred dfs services");
        }
        DfsServiceAgentDo dfsServiceAgentDo = dfsServiceAgentDoList.get(0);

        DfsServiceAgentDto dfsServiceAgentDto = new DfsServiceAgentDto();
        BeanUtils.copyProperties(dfsServiceAgentDo, dfsServiceAgentDto);

        DfsServiceComponentDto dfsServiceComponentDto =
                this.dfsServiceComponentService.getDfsServiceComponent(
                        dfsServiceAgentDo.getServiceComponentUid(),
                        operatingUserProfile);
        dfsServiceAgentDto.setServiceComponent(dfsServiceComponentDto);

        return dfsServiceAgentDto;
    }

    @Override
    public List<DfsServiceAgentDto> listingQueryDfsServiceAgents(
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
        Specification<DfsServiceAgentDo> specification = new Specification<DfsServiceAgentDo>() {
            @Override
            public Predicate toPredicate(Root<DfsServiceAgentDo> root, CriteriaQuery<?> query,
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

        List<DfsServiceAgentDo> itemDoList = this.dfsServiceAgentRepository.findAll(specification, sort);

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
        List<DfsServiceAgentDto> content = new LinkedList<>();
        itemDoList.forEach(itemDo -> {
            DfsServiceAgentDto itemDto = new DfsServiceAgentDto();
            BeanUtils.copyProperties(itemDo, itemDto);

            DfsServiceComponentDto dfsServiceComponentDto =
                    this.dfsServiceComponentService.getDfsServiceComponent(
                            itemDo.getServiceComponentUid(),
                            operatingUserProfile);
            itemDto.setServiceComponent(dfsServiceComponentDto);

            // 为 created by, last modified by 补充 user brief information
            itemDto.setCreatedByUser(userBriefInformationMap.get(itemDo.getCreatedBy()));
            itemDto.setLastModifiedByUser(userBriefInformationMap.get(itemDo.getLastModifiedBy()));

            content.add(itemDto);
        });
        return content;
    }

    @Override
    public Page<DfsServiceAgentDto> pagingQueryDfsServiceAgents(
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
        Specification<DfsServiceAgentDo> specification = new Specification<DfsServiceAgentDo>() {
            @Override
            public Predicate toPredicate(Root<DfsServiceAgentDo> root, CriteriaQuery<?> query,
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

        Page<DfsServiceAgentDo> itemDoPage = this.dfsServiceAgentRepository.findAll(specification, pageable);

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
        List<DfsServiceAgentDto> content = new ArrayList<>(itemDoPage.getSize());
        itemDoPage.forEach(itemDo -> {
            DfsServiceAgentDto itemDto = new DfsServiceAgentDto();
            BeanUtils.copyProperties(itemDo, itemDto);

            DfsServiceComponentDto dfsServiceComponentDto =
                    this.dfsServiceComponentService.getDfsServiceComponent(
                            itemDo.getServiceComponentUid(),
                            operatingUserProfile);
            itemDto.setServiceComponent(dfsServiceComponentDto);

            // 为 created by, last modified by 补充 user brief information
            itemDto.setCreatedByUser(userBriefInformationMap.get(itemDo.getCreatedBy()));
            itemDto.setLastModifiedByUser(userBriefInformationMap.get(itemDo.getLastModifiedBy()));

            content.add(itemDto);
        });
        Page<DfsServiceAgentDto> itemDtoPage = new PageImpl<>(content, pageable, itemDoPage.getTotalElements());
        return itemDtoPage;
    }

    @Override
    public String uploadFile(
            File file,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return this.dfsServiceHandler.uploadFile(file, operatingUserProfile);
    }

    @Override
    public String uploadFile(
            Long dfsServiceAgentUid,
            File file,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return this.dfsServiceHandler.uploadFile(dfsServiceAgentUid, file, operatingUserProfile);
    }

    private String uploadFile(
            DfsServiceAgentDo dfsServiceAgentDo,
            File file,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return this.dfsServiceHandler.uploadFile(dfsServiceAgentDo, file, operatingUserProfile);
    }

    @Override
    public File downloadFile(
            String fileId,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return this.dfsServiceHandler.downloadFile(fileId, operatingUserProfile);
    }

    @Override
    public File downloadFile(
            Long uid,
            String fileId,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return this.dfsServiceHandler.downloadFile(uid, fileId, operatingUserProfile);
    }

    @Override
    public void downloadFiles(
            Long uid,
            List<AbcTuple3<String, String, String>> inputList,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        this.dfsServiceHandler.downloadFiles(uid, inputList, operatingUserProfile);
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
    public void handleDfsServiceComponentDeletedEvent(DfsServiceComponentDeletedEvent event) {
        LOGGER.info("rcv event:{}", event);

        List<DfsServiceAgentDo> dfsServiceAgentDoList =
                this.dfsServiceAgentRepository.findByServiceComponentUid(event.getDfsServiceComponentDo().getUid());
        if (!CollectionUtils.isEmpty(dfsServiceAgentDoList)) {
            dfsServiceAgentDoList.forEach(dfsServiceAgentDo -> {
                deleteDfsServiceAgent(dfsServiceAgentDo, event.getOperatingUserProfile());
            });
        }
    }

    @ResourceReferenceHandler(name = "dfs service agent")
    public List<String> checkResourceReference(
            ResourceReferenceManager.ResourceCategoryEnum resourceCategory,
            Long resourceUid,
            String resourceName) throws Exception {
        switch (resourceCategory) {
            case DFS_SERVICE_COMPONENT: {
                Long serviceComponentUid = resourceUid;

                List<DfsServiceAgentDo> serviceAgentDoList =
                        this.dfsServiceAgentRepository.findByServiceComponentUid(serviceComponentUid);
                if (!CollectionUtils.isEmpty(serviceAgentDoList)) {
                    List<String> result = new LinkedList<>();
                    serviceAgentDoList.forEach(serviceAgentDo -> {
                        result.add(String.format(
                                "[%s] %s (%d)",
                                DfsServiceAgentDo.RESOURCE_SYMBOL,
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
