package cc.cornerstones.biz.app.service.impl;

import cc.cornerstones.almond.exceptions.AbcAuthorizationException;
import cc.cornerstones.almond.exceptions.AbcResourceDuplicateException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserBriefInformation;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.app.dto.AppDto;
import cc.cornerstones.biz.app.dto.CreateAppDto;
import cc.cornerstones.biz.app.dto.UpdateAppDto;
import cc.cornerstones.biz.app.entity.AppDo;
import cc.cornerstones.biz.app.persistence.AppRepository;
import cc.cornerstones.biz.app.service.assembly.AppAccessHandler;
import cc.cornerstones.biz.app.service.inf.AppService;
import cc.cornerstones.biz.share.assembly.ResourceReferenceManager;
import cc.cornerstones.biz.share.event.AppCreatedEvent;
import cc.cornerstones.biz.share.event.AppDeletedEvent;
import cc.cornerstones.biz.share.event.EventBusManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AppServiceImpl implements AppService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private AppRepository appRepository;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    private UserService userService;

    @Autowired
    private ResourceReferenceManager resourceReferenceManager;

    @Autowired
    private AppAccessHandler appAccessHandler;

    @Override
    public AppDto createApp(
            CreateAppDto createAppDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // step 1, pre-processing
        //
        boolean existsDuplicate = this.appRepository.existsByName(createAppDto.getName());
        if (existsDuplicate) {
            throw new AbcResourceDuplicateException(String.format("%s::name:%s", AppDo.RESOURCE_SYMBOL,
                    createAppDto.getName()));
        }

        //
        // step 2, core-processing
        //
        AppDo appDo = new AppDo();
        appDo.setUid(this.idHelper.getNextDistributedId(AppDo.RESOURCE_NAME));
        appDo.setName(createAppDto.getName());
        appDo.setObjectName(createAppDto.getName()
                .replaceAll("_", "__")
                .replaceAll("\\s", "_").toLowerCase());
        appDo.setDescription(createAppDto.getDescription());
        appDo.setEnabled(createAppDto.getEnabled());
        appDo.setSequence(createAppDto.getSequence());
        BaseDo.create(appDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.appRepository.save(appDo);

        //
        // step 3, post-processing
        //

        // event post
        AppCreatedEvent appCreatedEvent = new AppCreatedEvent();
        appCreatedEvent.setUid(appDo.getUid());
        appCreatedEvent.setOperatingUserProfile(operatingUserProfile);
        this.eventBusManager.send(appCreatedEvent);

        AppDto appDto = new AppDto();
        BeanUtils.copyProperties(appDo, appDto);
        return appDto;
    }

    @Override
    public void updateApp(
            Long uid,
            UpdateAppDto updateAppDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // step 1, pre-processing
        //
        AppDo appDo = this.appRepository.findByUid(uid);
        if (appDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", AppDo.RESOURCE_SYMBOL, uid));
        }

        if (!ObjectUtils.isEmpty(updateAppDto.getName())
                && !updateAppDto.getName().equalsIgnoreCase(appDo.getName())) {
            boolean existsDuplicate = this.appRepository.existsByName(updateAppDto.getName());
            if (existsDuplicate) {
                throw new AbcResourceDuplicateException(String.format("%s::name:%s", AppDo.RESOURCE_SYMBOL,
                        updateAppDto.getName()));
            }
        }

        // verify authorization
        this.appAccessHandler.verifyWriteAuthorization(appDo, operatingUserProfile);


        //
        // step 2, core-processing
        //
        boolean requiredToUpdate = false;

        if (!ObjectUtils.isEmpty(updateAppDto.getName())
                && !updateAppDto.getName().equalsIgnoreCase(appDo.getName())) {
            appDo.setName(updateAppDto.getName());
            appDo.setObjectName(updateAppDto.getName()
                    .replaceAll("_", "__")
                    .replaceAll("\\s", "_").toLowerCase());
            requiredToUpdate = true;
        }

        if (updateAppDto.getDescription() != null
                && !updateAppDto.getDescription().equalsIgnoreCase(appDo.getDescription())) {
            appDo.setDescription(updateAppDto.getDescription());
            requiredToUpdate = true;
        }

        if (updateAppDto.getEnabled() != null
                && !updateAppDto.getEnabled().equals(appDo.getEnabled())) {
            appDo.setEnabled(updateAppDto.getEnabled());
            requiredToUpdate = true;
        }

        if (updateAppDto.getSequence() != null
                && !updateAppDto.getSequence().equals(appDo.getSequence())) {
            appDo.setSequence(updateAppDto.getSequence());
            requiredToUpdate = true;
        }

        if (requiredToUpdate) {
            BaseDo.update(appDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.appRepository.save(appDo);
        }

        //
        // step 3, post-processing
        //
    }

    @Override
    public List<String> listAllReferencesToApp(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        AppDo appDo = this.appRepository.findByUid(uid);
        if (appDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", AppDo.RESOURCE_SYMBOL, uid));
        }

        //
        // Step 2, core-processing
        //
        return this.resourceReferenceManager.check(
                ResourceReferenceManager.ResourceCategoryEnum.APP,
                appDo.getUid(),
                appDo.getName());
    }

    @Override
    public void deleteApp(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        AppDo appDo = this.appRepository.findByUid(uid);
        if (appDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", AppDo.RESOURCE_SYMBOL, uid));
        }

        // verify authorization
        this.appAccessHandler.verifyAdminAuthorization(appDo, operatingUserProfile);

        //
        // Step 2, core-processing
        //
        appDo.setDeleted(Boolean.TRUE);
        BaseDo.update(appDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.appRepository.save(appDo);

        //
        // Step 3, post-processing
        //
        AppDeletedEvent appDeletedEvent = new AppDeletedEvent();
        appDeletedEvent.setUid(appDo.getUid());
        appDeletedEvent.setOperatingUserProfile(operatingUserProfile);
        this.eventBusManager.send(appDeletedEvent);
    }

    @Override
    public AppDto getApp(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        AppDo appDo = this.appRepository.findByUid(uid);
        if (appDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", AppDo.RESOURCE_SYMBOL, uid));
        }

        // verify authorization
        this.appAccessHandler.verifyReadAuthorization(appDo, operatingUserProfile);


        //
        // Step 2, core-processing
        //

        //
        // Step 3, post-processing
        //
        AppDto appDto = new AppDto();
        BeanUtils.copyProperties(appDo, appDto);
        return appDto;
    }

    @Override
    public List<AppDto> listingQueryApps(
            Long uid,
            String name,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        // verify authorization, part 1/2
        List<Long> authorizedReadAppUidList = new LinkedList<>();
        boolean restricted = this.appAccessHandler.collectAppsThatAreAuthorizedToRead(
                authorizedReadAppUidList,
                operatingUserProfile);
        if (uid != null & restricted) {
            if (!authorizedReadAppUidList.contains(uid)) {
                throw new AbcAuthorizationException("you are not allowed to perform this operation");
            }
        }

        //
        // Step 2, core-processing
        //
        Specification<AppDo> specification = new Specification<AppDo>() {
            @Override
            public Predicate toPredicate(Root<AppDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (uid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("uid"), uid));
                } else {
                    // verify authorization, part 2/2
                    if (restricted) {
                        CriteriaBuilder.In<Long> in = criteriaBuilder.in(root.get("uid"));
                        authorizedReadAppUidList.forEach(authorizedReadAppUid -> {
                            in.value(authorizedReadAppUid);
                        });

                        predicateList.add(in);
                    }
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

        List<AppDo> itemDoList = this.appRepository.findAll(specification, sort);
        List<AppDto> content = new LinkedList<>();
        itemDoList.forEach(itemDo -> {
            AppDto itemDto = new AppDto();
            BeanUtils.copyProperties(itemDo, itemDto);
            content.add(itemDto);
        });
        return content;
    }

    @Override
    public Page<AppDto> pagingQueryApps(
            Long uid,
            String name,
            String description,
            Boolean enabled,
            List<Long> userUidListOfLastModifiedBy,
            List<String> lastModifiedTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        // verify authorization, part 1/2
        List<Long> authorizedReadAppUidList = new LinkedList<>();
        boolean restricted = this.appAccessHandler.collectAppsThatAreAuthorizedToRead(
                authorizedReadAppUidList,
                operatingUserProfile);
        if (uid != null & restricted) {
            if (!authorizedReadAppUidList.contains(uid)) {
                throw new AbcAuthorizationException("you are not allowed to perform this operation");
            }
        }

        //
        // Step 2, core-processing
        //
        Specification<AppDo> specification = new Specification<AppDo>() {
            @Override
            public Predicate toPredicate(Root<AppDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (uid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("uid"), uid));
                } else {
                    // verify authorization, part 2/2
                    if (restricted) {
                        CriteriaBuilder.In<Long> in = criteriaBuilder.in(root.get("uid"));
                        authorizedReadAppUidList.forEach(authorizedReadAppUid -> {
                            in.value(authorizedReadAppUid);
                        });

                        predicateList.add(in);
                    }
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

        Page<AppDo> itemDoPage = this.appRepository.findAll(specification, pageable);
        List<AppDto> content = new ArrayList<>(itemDoPage.getSize());

        //
        // 为 created by, last modified by 补充 user brief information
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
        // 最后的结果封装
        //
        itemDoPage.forEach(itemDo -> {
            AppDto itemDto = new AppDto();
            BeanUtils.copyProperties(itemDo, itemDto);

            itemDto.setCreatedByUser(userBriefInformationMap.get(itemDo.getCreatedBy()));
            itemDto.setLastModifiedByUser(userBriefInformationMap.get(itemDo.getLastModifiedBy()));

            content.add(itemDto);
        });
        Page<AppDto> itemDtoPage = new PageImpl<>(content, pageable, itemDoPage.getTotalElements());
        return itemDtoPage;
    }
}
