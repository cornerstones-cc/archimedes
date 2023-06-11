package cc.cornerstones.biz.administration.usermanagement.service.impl;

import cc.cornerstones.almond.constants.DatabaseFieldTypeEnum;
import cc.cornerstones.almond.exceptions.AbcResourceDuplicateException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserBriefInformation;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.administration.usermanagement.dto.CreateUserSchemaExtendedPropertyDto;
import cc.cornerstones.biz.administration.usermanagement.dto.UpdateUserSchemaExtendedPropertyDto;
import cc.cornerstones.biz.administration.usermanagement.dto.UserSchemaExtendedPropertyDto;
import cc.cornerstones.biz.administration.usermanagement.entity.UserSchemaExtendedPropertyDo;
import cc.cornerstones.biz.administration.usermanagement.persistence.UserSchemaExtendedPropertyRepository;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserSchemaService;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
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
import java.util.*;

@Service
public class UserSchemaServiceImpl implements UserSchemaService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserSchemaServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private UserSchemaExtendedPropertyRepository userSchemaExtendedPropertyRepository;

    private java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private java.time.format.DateTimeFormatter dateTimeFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    private UserService userService;

    @Override
    public UserSchemaExtendedPropertyDto createExtendedProperty(
            CreateUserSchemaExtendedPropertyDto createUserSchemaExtendedPropertyDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        boolean existsDuplicate = this.userSchemaExtendedPropertyRepository.existsByName(createUserSchemaExtendedPropertyDto.getName());
        if (existsDuplicate) {
            throw new AbcResourceDuplicateException(String.format("%s::name=%s",
                    UserSchemaExtendedPropertyDo.RESOURCE_SYMBOL,
                    createUserSchemaExtendedPropertyDto.getName()));
        }

        //
        // Step 2, core-processing
        //
        UserSchemaExtendedPropertyDo userSchemaExtendedPropertyDo = new UserSchemaExtendedPropertyDo();
        userSchemaExtendedPropertyDo.setUid(this.idHelper.getNextDistributedId(UserSchemaExtendedPropertyDo.RESOURCE_NAME));
        userSchemaExtendedPropertyDo.setName(createUserSchemaExtendedPropertyDto.getName());
        userSchemaExtendedPropertyDo.setObjectName(createUserSchemaExtendedPropertyDto.getName().replaceAll("_", "__")
                .replaceAll("\\s", "_")
                .toLowerCase());
        userSchemaExtendedPropertyDo.setDescription(createUserSchemaExtendedPropertyDto.getDescription());
        userSchemaExtendedPropertyDo.setSequence(createUserSchemaExtendedPropertyDto.getSequence());
        userSchemaExtendedPropertyDo.setType(createUserSchemaExtendedPropertyDto.getType());
        userSchemaExtendedPropertyDo.setLength(createUserSchemaExtendedPropertyDto.getLength());
        userSchemaExtendedPropertyDo.setInputValidationRegex(createUserSchemaExtendedPropertyDto.getInputValidationRegex());
        userSchemaExtendedPropertyDo.setNullable(createUserSchemaExtendedPropertyDto.getNullable());
        userSchemaExtendedPropertyDo.setShowInFilter(createUserSchemaExtendedPropertyDto.getShowInFilter());
        userSchemaExtendedPropertyDo.setShowInDetailedInformation(createUserSchemaExtendedPropertyDto.getShowInDetailedInformation());
        userSchemaExtendedPropertyDo.setShowInBriefInformation(createUserSchemaExtendedPropertyDto.getShowInBriefInformation());
        BaseDo.create(userSchemaExtendedPropertyDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.userSchemaExtendedPropertyRepository.save(userSchemaExtendedPropertyDo);

        //
        // Step 3, post-processing
        //
        UserSchemaExtendedPropertyDto userSchemaExtendedPropertyDto = new UserSchemaExtendedPropertyDto();
        BeanUtils.copyProperties(userSchemaExtendedPropertyDo, userSchemaExtendedPropertyDto);
        return userSchemaExtendedPropertyDto;
    }

    @Override
    public void updateExtendedProperty(
            Long uid,
            UpdateUserSchemaExtendedPropertyDto updateUserSchemaExtendedPropertyDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        UserSchemaExtendedPropertyDo userSchemaExtendedPropertyDo =
                this.userSchemaExtendedPropertyRepository.findByUid(uid);
        if (userSchemaExtendedPropertyDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", UserSchemaExtendedPropertyDo.RESOURCE_SYMBOL, uid));
        }
        if (!ObjectUtils.isEmpty(updateUserSchemaExtendedPropertyDto.getName())
                && !updateUserSchemaExtendedPropertyDto.getName().equalsIgnoreCase(userSchemaExtendedPropertyDo.getName())) {
            boolean existsDuplicate = this.userSchemaExtendedPropertyRepository.existsByName(updateUserSchemaExtendedPropertyDto.getName());
            if (existsDuplicate) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s", UserSchemaExtendedPropertyDo.RESOURCE_SYMBOL,
                        updateUserSchemaExtendedPropertyDto.getName()));
            }
        }

        //
        // Step 2, core-processing
        //
        boolean requiredToUpdate = false;
        if (!ObjectUtils.isEmpty(updateUserSchemaExtendedPropertyDto.getName())) {
            userSchemaExtendedPropertyDo.setName(updateUserSchemaExtendedPropertyDto.getName());
            userSchemaExtendedPropertyDo.setObjectName(updateUserSchemaExtendedPropertyDto.getName().replaceAll("_", "__")
                    .replaceAll("\\s", "_")
                    .toLowerCase());
            requiredToUpdate = true;
        }
        if (updateUserSchemaExtendedPropertyDto.getDescription() != null
                && !updateUserSchemaExtendedPropertyDto.getDescription().equals(userSchemaExtendedPropertyDo.getDescription())) {
            userSchemaExtendedPropertyDo.setDescription(updateUserSchemaExtendedPropertyDto.getDescription());
            requiredToUpdate = true;
        }
        if (updateUserSchemaExtendedPropertyDto.getSequence() != null
                && !updateUserSchemaExtendedPropertyDto.getSequence().equals(userSchemaExtendedPropertyDo.getSequence())) {
            userSchemaExtendedPropertyDo.setSequence(updateUserSchemaExtendedPropertyDto.getSequence());
            requiredToUpdate = true;
        }
        if (updateUserSchemaExtendedPropertyDto.getType() != null
                && !updateUserSchemaExtendedPropertyDto.getType().equals(userSchemaExtendedPropertyDo.getType())) {
            userSchemaExtendedPropertyDo.setType(updateUserSchemaExtendedPropertyDto.getType());
            requiredToUpdate = true;
        }
        if (updateUserSchemaExtendedPropertyDto.getLength() != null
                && !updateUserSchemaExtendedPropertyDto.getLength().equals(userSchemaExtendedPropertyDo.getLength())) {
            userSchemaExtendedPropertyDo.setLength(updateUserSchemaExtendedPropertyDto.getLength());
            requiredToUpdate = true;
        }
        if (userSchemaExtendedPropertyDo.getType() != null) {
            if (!userSchemaExtendedPropertyDo.getType().equals(DatabaseFieldTypeEnum.CHAR)
                    && !userSchemaExtendedPropertyDo.getType().equals(DatabaseFieldTypeEnum.VARCHAR)
                    && !userSchemaExtendedPropertyDo.getType().equals(DatabaseFieldTypeEnum.DECIMAL)) {
                if (userSchemaExtendedPropertyDo.getLength() != null) {
                    userSchemaExtendedPropertyDo.setLength(null);
                    requiredToUpdate = true;
                }
            }
        }
        if (updateUserSchemaExtendedPropertyDto.getInputValidationRegex() != null
                && !updateUserSchemaExtendedPropertyDto.getInputValidationRegex().equals(userSchemaExtendedPropertyDo.getInputValidationRegex())) {
            userSchemaExtendedPropertyDo.setInputValidationRegex(updateUserSchemaExtendedPropertyDto.getInputValidationRegex());
            requiredToUpdate = true;
        }
        if (updateUserSchemaExtendedPropertyDto.getNullable() != null
                && !updateUserSchemaExtendedPropertyDto.getNullable().equals(userSchemaExtendedPropertyDo.getNullable())) {
            userSchemaExtendedPropertyDo.setNullable(updateUserSchemaExtendedPropertyDto.getNullable());
            requiredToUpdate = true;
        }
        if (updateUserSchemaExtendedPropertyDto.getShowInFilter() != null
                && !updateUserSchemaExtendedPropertyDto.getShowInFilter().equals(userSchemaExtendedPropertyDo.getShowInFilter())) {
            userSchemaExtendedPropertyDo.setShowInFilter(updateUserSchemaExtendedPropertyDto.getShowInFilter());
            requiredToUpdate = true;
        }
        if (updateUserSchemaExtendedPropertyDto.getShowInDetailedInformation() != null
                && !updateUserSchemaExtendedPropertyDto.getShowInDetailedInformation().equals(
                userSchemaExtendedPropertyDo.getShowInDetailedInformation())) {
            userSchemaExtendedPropertyDo.setShowInDetailedInformation(
                    updateUserSchemaExtendedPropertyDto.getShowInDetailedInformation());
            requiredToUpdate = true;
        }
        if (updateUserSchemaExtendedPropertyDto.getShowInBriefInformation() != null
                && !updateUserSchemaExtendedPropertyDto.getShowInBriefInformation().equals(
                userSchemaExtendedPropertyDo.getShowInBriefInformation())) {
            userSchemaExtendedPropertyDo.setShowInBriefInformation(
                    updateUserSchemaExtendedPropertyDto.getShowInBriefInformation());
            requiredToUpdate = true;
        }

        if (requiredToUpdate) {
            BaseDo.update(userSchemaExtendedPropertyDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.userSchemaExtendedPropertyRepository.save(userSchemaExtendedPropertyDo);
        }

        //
        // Step 3, post-processing
        //
    }

    @Override
    public List<String> listAllReferencesToExtendedProperty(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return null;
    }

    @Override
    public void deleteExtendedProperty(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        UserSchemaExtendedPropertyDo userSchemaExtendedPropertyDo =
                this.userSchemaExtendedPropertyRepository.findByUid(uid);
        if (userSchemaExtendedPropertyDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", UserSchemaExtendedPropertyDo.RESOURCE_SYMBOL, uid));
        }

        //
        // Step 2, core-processing
        //
        userSchemaExtendedPropertyDo.setDeleted(Boolean.TRUE);
        BaseDo.update(userSchemaExtendedPropertyDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.userSchemaExtendedPropertyRepository.save(userSchemaExtendedPropertyDo);

        //
        // Step 3, post-processing
        //
    }

    @Override
    public UserSchemaExtendedPropertyDto getExtendedProperty(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        UserSchemaExtendedPropertyDo userSchemaExtendedPropertyDo =
                this.userSchemaExtendedPropertyRepository.findByUid(uid);
        if (userSchemaExtendedPropertyDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", UserSchemaExtendedPropertyDo.RESOURCE_SYMBOL, uid));
        }

        //
        // Step 2, core-processing
        //
        UserSchemaExtendedPropertyDto userSchemaExtendedPropertyDto = new UserSchemaExtendedPropertyDto();
        BeanUtils.copyProperties(userSchemaExtendedPropertyDo, userSchemaExtendedPropertyDto);
        return userSchemaExtendedPropertyDto;

        //
        // Step 3, post-processing
        //
    }

    @Override
    public List<UserSchemaExtendedPropertyDto> listingQueryExtendedProperties(
            Long uid,
            String name,
            Boolean showInFilter,
            Boolean showInDetailedInformation,
            Boolean showInBriefInformation,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // step 1, pre-processing
        //

        //
        // step 2, core-processing
        //
        Specification<UserSchemaExtendedPropertyDo> specification = new Specification<UserSchemaExtendedPropertyDo>() {
            @Override
            public Predicate toPredicate(Root<UserSchemaExtendedPropertyDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (uid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("uid"), uid));
                }
                if (!ObjectUtils.isEmpty(name)) {
                    predicateList.add(criteriaBuilder.like(root.get("name"), "%" + name + "%"));
                }
                if (showInFilter != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("showInFilter"), showInFilter));
                }
                if (showInDetailedInformation != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("showInDetailedInformation"), showInDetailedInformation));
                }
                if (showInBriefInformation != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("showInBriefInformation"), showInBriefInformation));
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        if (sort == null || sort.isUnsorted()) {
            sort = Sort.by(Sort.Order.asc("sequence"));
        }

        List<UserSchemaExtendedPropertyDo> itemDoList = this.userSchemaExtendedPropertyRepository.findAll(specification, sort);

        //
        // step 3, post-processing
        //
        if (CollectionUtils.isEmpty(itemDoList)) {
            return null;
        }
        List<UserSchemaExtendedPropertyDto> content = new ArrayList<>(itemDoList.size());
        itemDoList.forEach(itemDo -> {
            UserSchemaExtendedPropertyDto itemDto = new UserSchemaExtendedPropertyDto();
            BeanUtils.copyProperties(itemDo, itemDto);
            content.add(itemDto);
        });
        return content;
    }

    @Override
    public Page<UserSchemaExtendedPropertyDto> pagingQueryExtendedProperties(
            Long uid,
            String name,
            String description,
            List<Long> userUidListOfLastModifiedBy,
            List<String> lastModifiedTimestampAsStringList,
            Boolean showInFilter,
            Boolean showInDetailedInformation,
            Boolean showInBriefInformation,
            Pageable pageable,
            UserProfile operatingUserProfile) {
        //
        // step 1, pre-processing
        //

        //
        // step 2, core-processing
        //
        Specification<UserSchemaExtendedPropertyDo> specification = new Specification<UserSchemaExtendedPropertyDo>() {
            @Override
            public Predicate toPredicate(Root<UserSchemaExtendedPropertyDo> root, CriteriaQuery<?> query,
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
                if (showInFilter != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("showInFilter"), showInFilter));
                }
                if (showInDetailedInformation != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("showInDetailedInformation"), showInDetailedInformation));
                }
                if (showInBriefInformation != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("showInBriefInformation"), showInBriefInformation));
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        if (pageable == null) {
            pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.asc("sequence")));
        } else if (pageable.getSort() == null || pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Order.asc("sequence")));
        }

        Page<UserSchemaExtendedPropertyDo> itemDoPage = this.userSchemaExtendedPropertyRepository.findAll(specification, pageable);

        //
        // step 3, post-processing
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
            List<UserBriefInformation> userBriefInformationList =
                    this.userService.listingUserBriefInformation(userUidList,
                            operatingUserProfile);
            if (!CollectionUtils.isEmpty(userBriefInformationList)) {
                userBriefInformationList.forEach(userBriefInformation -> {
                    userBriefInformationMap.put(userBriefInformation.getUid(), userBriefInformation);
                });
            }
        }

        //
        // Step 3.2, 构造返回内容
        //
        List<UserSchemaExtendedPropertyDto> content = new ArrayList<>(itemDoPage.getSize());
        itemDoPage.forEach(itemDo -> {
            UserSchemaExtendedPropertyDto itemDto = new UserSchemaExtendedPropertyDto();
            BeanUtils.copyProperties(itemDo, itemDto);

            // 为 created by, last modified by 补充 user brief information
            itemDto.setCreatedByUser(userBriefInformationMap.get(itemDo.getCreatedBy()));
            itemDto.setLastModifiedByUser(userBriefInformationMap.get(itemDo.getLastModifiedBy()));

            content.add(itemDto);
        });
        Page<UserSchemaExtendedPropertyDto> itemDtoPage = new PageImpl<>(content, pageable, itemDoPage.getTotalElements());
        return itemDtoPage;
    }

    @Override
    public String transformExtendedPropertyValueFromObjectToString(
            Object extendedPropertyValue,
            Long extendedPropertyUid) throws AbcUndefinedException {
        UserSchemaExtendedPropertyDo userSchemaExtendedPropertyDo =
                this.userSchemaExtendedPropertyRepository.findByUid(extendedPropertyUid);
        if (userSchemaExtendedPropertyDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    UserSchemaExtendedPropertyDo.RESOURCE_SYMBOL, extendedPropertyUid));
        }

        String expectedExtendedPropertyValue = null;
        switch (userSchemaExtendedPropertyDo.getType()) {
            case BOOLEAN: {
                if (extendedPropertyValue instanceof Boolean) {
                    Boolean transformed = (Boolean) extendedPropertyValue;
                    if (Boolean.TRUE.equals(transformed)) {
                        expectedExtendedPropertyValue = "1";
                    } else {
                        expectedExtendedPropertyValue = "0";
                    }
                } else if (extendedPropertyValue instanceof Boolean) {
                    String transformed = (String) extendedPropertyValue;
                    if (transformed.equalsIgnoreCase("true")) {
                        expectedExtendedPropertyValue = "1";
                    } else if (transformed.equalsIgnoreCase("false")) {
                        expectedExtendedPropertyValue = "0";
                    }
                } else if (extendedPropertyValue instanceof Integer) {
                    String transformed = (String) extendedPropertyValue;
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
                expectedExtendedPropertyValue = String.valueOf(extendedPropertyValue);
            }
            break;
            case DATE: {
                if (extendedPropertyValue instanceof String) {
                    String transformed = (String) extendedPropertyValue;
                    expectedExtendedPropertyValue = transformed;
                } else if (extendedPropertyValue instanceof java.time.LocalDate) {
                    java.time.LocalDate transformed = (java.time.LocalDate) extendedPropertyValue;
                    expectedExtendedPropertyValue = transformed.format(this.dateFormatter);
                } else if (extendedPropertyValue instanceof java.time.LocalDateTime) {
                    java.time.LocalDateTime transformed = (java.time.LocalDateTime) extendedPropertyValue;
                    expectedExtendedPropertyValue = transformed.format(this.dateFormatter);
                }
            }
            break;
            case DATETIME:
            case TIMESTAMP: {
                if (extendedPropertyValue instanceof String) {
                    String transformed = (String) extendedPropertyValue;
                    expectedExtendedPropertyValue = transformed;
                } else if (extendedPropertyValue instanceof java.time.LocalDate) {
                    java.time.LocalDate transformed = (java.time.LocalDate) extendedPropertyValue;
                    expectedExtendedPropertyValue = transformed.format(this.dateTimeFormatter);
                } else if (extendedPropertyValue instanceof java.time.LocalDateTime) {
                    java.time.LocalDateTime transformed = (java.time.LocalDateTime) extendedPropertyValue;
                    expectedExtendedPropertyValue = transformed.format(this.dateTimeFormatter);
                }
            }
            break;
            case TIME: {
                if (extendedPropertyValue instanceof String) {
                    String transformed = (String) extendedPropertyValue;
                    expectedExtendedPropertyValue = transformed;
                } else if (extendedPropertyValue instanceof java.time.LocalTime) {
                    java.time.LocalTime transformed = (java.time.LocalTime) extendedPropertyValue;
                    expectedExtendedPropertyValue = transformed.format(this.timeFormatter);
                }
            }
            break;
            default:
                expectedExtendedPropertyValue = String.valueOf(extendedPropertyValue);
                break;
        }

        return expectedExtendedPropertyValue;
    }
}
