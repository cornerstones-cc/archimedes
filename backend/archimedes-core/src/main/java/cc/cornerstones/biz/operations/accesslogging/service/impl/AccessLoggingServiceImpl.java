package cc.cornerstones.biz.operations.accesslogging.service.impl;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserBriefInformation;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.usermanagement.dto.UserDto;
import cc.cornerstones.biz.administration.usermanagement.dto.UserOutlineDto;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.operations.accesslogging.dto.CreateOrUpdateQueryLogDto;
import cc.cornerstones.biz.operations.accesslogging.dto.QueryLogDto;
import cc.cornerstones.biz.operations.accesslogging.dto.SimpleQueryLogDto;
import cc.cornerstones.biz.operations.accesslogging.entity.QueryLogDo;
import cc.cornerstones.biz.operations.accesslogging.persistence.QueryLogRepository;
import cc.cornerstones.biz.operations.accesslogging.service.inf.AccessLoggingService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AccessLoggingServiceImpl implements AccessLoggingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccessLoggingServiceImpl.class);

    @Autowired
    private QueryLogRepository queryLogRepository;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    private UserService userService;

    @Override
    public void createQueryLog(
            CreateOrUpdateQueryLogDto createQueryLogDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        QueryLogDo queryLogDo = new QueryLogDo();
        queryLogDo.setTrackingSerialNumber(createQueryLogDto.getTrackingSerialNumber());
        queryLogDo.setUserUid(createQueryLogDto.getUserUid());
        queryLogDo.setDisplayName(createQueryLogDto.getDisplayName());
        queryLogDo.setRequest(createQueryLogDto.getRequest());
        queryLogDo.setCreatedDate(LocalDate.now());

        //
        UserDto userDto = this.userService.getUser(createQueryLogDto.getUserUid());
        if (userDto != null) {
            queryLogDo.setUserOutline(new UserOutlineDto());
            queryLogDo.getUserOutline().setUid(userDto.getUid());
            queryLogDo.getUserOutline().setDisplayName(userDto.getDisplayName());
            queryLogDo.getUserOutline().setExtendedPropertyList(userDto.getExtendedPropertyList());
            queryLogDo.getUserOutline().setAccountList(userDto.getAccountList());
            queryLogDo.getUserOutline().setRoleList(userDto.getRoleList());
            queryLogDo.getUserOutline().setGroupList(userDto.getGroupList());
        }

        BaseDo.create(queryLogDo, operatingUserProfile.getUid(), LocalDateTime.now());

        this.queryLogRepository.save(queryLogDo);
    }

    @Override
    public void updateQueryLog(
            CreateOrUpdateQueryLogDto updateQueryLogDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        QueryLogDo queryLogDo =
                this.queryLogRepository.findByTrackingSerialNumber(updateQueryLogDto.getTrackingSerialNumber());
        if (queryLogDo == null) {
            LOGGER.error("cannot find query log {}", updateQueryLogDto.getTrackingSerialNumber());

            CreateOrUpdateQueryLogDto createQueryLogDto = new CreateOrUpdateQueryLogDto();
            queryLogDo.setTrackingSerialNumber(operatingUserProfile.getTrackingSerialNumber());
            queryLogDo.setUserUid(operatingUserProfile.getUid());
            queryLogDo.setDisplayName(operatingUserProfile.getDisplayName());
            queryLogDo.setRequest(updateQueryLogDto.getRequest());
            queryLogDo.setCreatedDate(LocalDate.now());
            BaseDo.create(queryLogDo, operatingUserProfile.getUid(), LocalDateTime.now());

            this.queryLogRepository.save(queryLogDo);
        }

        if (updateQueryLogDto.getDataFacetUid() != null) {
            queryLogDo.setDataFacetUid(updateQueryLogDto.getDataFacetUid());
        }

        if (updateQueryLogDto.getDataFacetName() != null) {
            queryLogDo.setDataFacetName(updateQueryLogDto.getDataFacetName());
        }

        if (updateQueryLogDto.getSuccessful() != null) {
            queryLogDo.setSuccessful(updateQueryLogDto.getSuccessful());
        }

        if (updateQueryLogDto.getBeginTimestamp() != null) {
            queryLogDo.setBeginTimestamp(updateQueryLogDto.getBeginTimestamp());
        }

        if (updateQueryLogDto.getEndTimestamp() != null) {
            queryLogDo.setEndTimestamp(updateQueryLogDto.getEndTimestamp());
        }

        if (updateQueryLogDto.getTotalDurationInMillis() != null) {
            queryLogDo.setTotalDurationInMillis(updateQueryLogDto.getTotalDurationInMillis());
        }

        if (updateQueryLogDto.getTotalDurationRemark() != null) {
            queryLogDo.setTotalDurationRemark(updateQueryLogDto.getTotalDurationRemark());
        }

        if (updateQueryLogDto.getTotalColumnsInSource() != null) {
            queryLogDo.setTotalColumnsInSource(updateQueryLogDto.getTotalColumnsInSource());
        }

        if (updateQueryLogDto.getTotalRowsInSource() != null) {
            queryLogDo.setTotalRowsInSource(updateQueryLogDto.getTotalRowsInSource());
        }

        if (updateQueryLogDto.getCountStatement() != null) {
            queryLogDo.setCountStatement(updateQueryLogDto.getCountStatement());
        }

        if (updateQueryLogDto.getCountDurationInMillis() != null) {
            queryLogDo.setCountDurationInMillis(updateQueryLogDto.getCountDurationInMillis());
        }

        if (updateQueryLogDto.getCountDurationRemark() != null) {
            queryLogDo.setCountDurationRemark(updateQueryLogDto.getCountDurationRemark());
        }

        if (updateQueryLogDto.getQueryStatement() != null) {
            queryLogDo.setQueryStatement(updateQueryLogDto.getQueryStatement());
        }

        if (updateQueryLogDto.getQueryDurationInMillis() != null) {
            queryLogDo.setQueryDurationInMillis(updateQueryLogDto.getQueryDurationInMillis());
        }

        if (updateQueryLogDto.getQueryDurationRemark() != null) {
            queryLogDo.setQueryDurationRemark(updateQueryLogDto.getQueryDurationRemark());
        }

        if (updateQueryLogDto.getRequest() != null) {
            queryLogDo.setRequest(updateQueryLogDto.getRequest());
        }

        if (updateQueryLogDto.getIntermediateResult() != null) {
            queryLogDo.setIntermediateResult(updateQueryLogDto.getIntermediateResult());
        }

        if (updateQueryLogDto.getResponse() != null) {
            // TODO 不再记录 response, 由于存入 MySQL 的 package 超过配置
            //  Packet for query is too large (80,073,725 > 52,428,800). You can change this value on the server by setting the 'max_allowed_packet' variable.
            // queryLogDo.setResponse(updateQueryLogDto.getResponse());
        }

        if (updateQueryLogDto.getRemark() != null) {
            queryLogDo.setRemark(updateQueryLogDto.getRemark());
        }

        BaseDo.update(queryLogDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.queryLogRepository.save(queryLogDo);
    }

    @Override
    public Page<SimpleQueryLogDto> pagingQueryQueryLogs(
            String trackingSerialNumber,
            Long dataFacetUid,
            String dataFacetName,
            List<Long> userUidListOfUser,
            List<String> createdTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        //
        // Step 2, core-processing
        //
        Specification<QueryLogDo> specification = new Specification<QueryLogDo>() {
            @Override
            public Predicate toPredicate(Root<QueryLogDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (!ObjectUtils.isEmpty(trackingSerialNumber)) {
                    predicateList.add(criteriaBuilder.equal(root.get("trackingSerialNumber"),
                            trackingSerialNumber));
                }
                if (dataFacetUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("dataFacetUid"), dataFacetUid));
                }
                if (!ObjectUtils.isEmpty(dataFacetName)) {
                    predicateList.add(criteriaBuilder.like(root.get("dataFacetName"), "%" + dataFacetName + "%"));
                }
                if (!CollectionUtils.isEmpty(userUidListOfUser)) {
                    CriteriaBuilder.In<Long> in = criteriaBuilder.in(root.get("userUid"));
                    userUidListOfUser.forEach(item -> {
                        in.value(item);
                    });
                    predicateList.add(in);
                }
                if (!CollectionUtils.isEmpty(createdTimestampAsStringList)) {
                    if (createdTimestampAsStringList.size() == 2) {
                        LocalDateTime dateTime0 = LocalDateTime.parse(createdTimestampAsStringList.get(0),
                                dateTimeFormatter);
                        LocalDateTime dateTime1 = LocalDateTime.parse(createdTimestampAsStringList.get(1), dateTimeFormatter);
                        if (dateTime0.isAfter(dateTime1)) {
                            predicateList.add(criteriaBuilder.between(root.get(BaseDo.CREATED_TIMESTAMP_FIELD_NAME),
                                    dateTime1, dateTime0));
                        } else {
                            predicateList.add(criteriaBuilder.between(root.get(BaseDo.CREATED_TIMESTAMP_FIELD_NAME),
                                    dateTime0, dateTime1));
                        }
                    } else if (createdTimestampAsStringList.size() == 1) {
                        LocalDateTime dateTime0 = LocalDateTime.parse(createdTimestampAsStringList.get(0), dateTimeFormatter);
                        predicateList.add(criteriaBuilder.equal(root.get(BaseDo.CREATED_TIMESTAMP_FIELD_NAME),
                                dateTime0));
                    } else {
                        CriteriaBuilder.In<LocalDateTime> in =
                                criteriaBuilder.in(root.get(BaseDo.CREATED_TIMESTAMP_FIELD_NAME));
                        createdTimestampAsStringList.forEach(createdTimestampAsString -> {
                            LocalDateTime dateTime0 = LocalDateTime.parse(createdTimestampAsString, dateTimeFormatter);
                            in.value(dateTime0);
                        });
                        predicateList.add(in);
                    }
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        if (pageable.getSort() == null || pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Order.desc(BaseDo.ID_FIELD_NAME)));
        }

        Page<QueryLogDo> itemDoPage = this.queryLogRepository.findAll(specification, pageable);

        //
        // Step 3, post-processing
        //

        //
        // Step 3.1, 为 user uid (user) 补充 user brief information
        //
        List<Long> userUidList = new LinkedList<>();
        itemDoPage.forEach(itemDo -> {
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
        List<SimpleQueryLogDto> content = new ArrayList<>(itemDoPage.getSize());
        itemDoPage.forEach(itemDo -> {
            SimpleQueryLogDto itemDto = new SimpleQueryLogDto();
            BeanUtils.copyProperties(itemDo, itemDto);

            // 为 uid (user) 补充 user brief information
            itemDto.setUser(userBriefInformationMap.get(itemDo.getUserUid()));

            content.add(itemDto);
        });
        Page<SimpleQueryLogDto> itemDtoPage = new PageImpl<>(content, pageable, itemDoPage.getTotalElements());
        return itemDtoPage;
    }

    @Override
    public QueryLogDto getQueryLog(
            String trackingSerialNumber,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        QueryLogDo queryLogDo = this.queryLogRepository.findByTrackingSerialNumber(trackingSerialNumber);
        if (queryLogDo == null) {
            return null;
        }

        QueryLogDto queryLogDto = new QueryLogDto();
        BeanUtils.copyProperties(queryLogDo, queryLogDto);

        List<Long> userUidList = new ArrayList<>(1);
        userUidList.add(queryLogDo.getUserUid());
        List<UserBriefInformation> userBriefInformationList =
                this.userService.listingUserBriefInformation(userUidList, operatingUserProfile);
        if (!CollectionUtils.isEmpty(userBriefInformationList)) {
            UserBriefInformation userBriefInformation = userBriefInformationList.get(0);
            // 为 uid (user) 补充 user brief information
            queryLogDto.setUser(userBriefInformation);
        }

        return queryLogDto;
    }

    @Override
    public Page<SimpleQueryLogDto> pagingQuerySlowQueryLogs(
            String trackingSerialNumber,
            Long dataFacetUid,
            String dataFacetName,
            List<Long> userUidListOfUser,
            List<String> createdTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        final long slowQueryDurationInMillisThreshold = 30 * 1000;

        //
        // Step 2, core-processing
        //

        Specification<QueryLogDo> specification = new Specification<QueryLogDo>() {
            @Override
            public Predicate toPredicate(Root<QueryLogDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();

                predicateList.add(criteriaBuilder.isNotNull(root.get("queryDurationInMillis")));

                predicateList.add(criteriaBuilder.greaterThan(root.get("queryDurationInMillis"),
                        slowQueryDurationInMillisThreshold));

                if (!ObjectUtils.isEmpty(trackingSerialNumber)) {
                    predicateList.add(criteriaBuilder.equal(root.get("trackingSerialNumber"),
                            trackingSerialNumber));
                }
                if (dataFacetUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("dataFacetUid"), dataFacetUid));
                }
                if (!ObjectUtils.isEmpty(dataFacetName)) {
                    predicateList.add(criteriaBuilder.like(root.get("dataFacetName"), "%" + dataFacetName + "%"));
                }
                if (!CollectionUtils.isEmpty(userUidListOfUser)) {
                    CriteriaBuilder.In<Long> in = criteriaBuilder.in(root.get("userUid"));
                    userUidListOfUser.forEach(item -> {
                        in.value(item);
                    });
                    predicateList.add(in);
                }
                if (!CollectionUtils.isEmpty(createdTimestampAsStringList)) {
                    if (createdTimestampAsStringList.size() == 2) {
                        LocalDateTime dateTime0 = LocalDateTime.parse(createdTimestampAsStringList.get(0),
                                dateTimeFormatter);
                        LocalDateTime dateTime1 = LocalDateTime.parse(createdTimestampAsStringList.get(1), dateTimeFormatter);
                        if (dateTime0.isAfter(dateTime1)) {
                            predicateList.add(criteriaBuilder.between(root.get(BaseDo.CREATED_TIMESTAMP_FIELD_NAME),
                                    dateTime1, dateTime0));
                        } else {
                            predicateList.add(criteriaBuilder.between(root.get(BaseDo.CREATED_TIMESTAMP_FIELD_NAME),
                                    dateTime0, dateTime1));
                        }
                    } else if (createdTimestampAsStringList.size() == 1) {
                        LocalDateTime dateTime0 = LocalDateTime.parse(createdTimestampAsStringList.get(0), dateTimeFormatter);
                        predicateList.add(criteriaBuilder.equal(root.get(BaseDo.CREATED_TIMESTAMP_FIELD_NAME),
                                dateTime0));
                    } else {
                        CriteriaBuilder.In<LocalDateTime> in =
                                criteriaBuilder.in(root.get(BaseDo.CREATED_TIMESTAMP_FIELD_NAME));
                        createdTimestampAsStringList.forEach(createdTimestampAsString -> {
                            LocalDateTime dateTime0 = LocalDateTime.parse(createdTimestampAsString, dateTimeFormatter);
                            in.value(dateTime0);
                        });
                        predicateList.add(in);
                    }
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        if (pageable.getSort() == null || pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Order.desc(
                    "queryDurationInMillis"), Sort.Order.desc(BaseDo.ID_FIELD_NAME)));
        }

        Page<QueryLogDo> itemDoPage = this.queryLogRepository.findAll(specification, pageable);

        //
        // Step 3, post-processing
        //

        //
        // Step 3.1, 为 user uid (user) 补充 user brief information
        //
        List<Long> userUidList = new LinkedList<>();
        itemDoPage.forEach(itemDo -> {
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
        List<SimpleQueryLogDto> content = new ArrayList<>(itemDoPage.getSize());
        itemDoPage.forEach(itemDo -> {
            SimpleQueryLogDto itemDto = new SimpleQueryLogDto();
            BeanUtils.copyProperties(itemDo, itemDto);

            // 为 uid (user) 补充 user brief information
            itemDto.setUser(userBriefInformationMap.get(itemDo.getUserUid()));

            content.add(itemDto);
        });
        Page<SimpleQueryLogDto> itemDtoPage = new PageImpl<>(content, pageable, itemDoPage.getTotalElements());
        return itemDtoPage;
    }
}
