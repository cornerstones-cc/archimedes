package cc.cornerstones.biz.operations.performancelogging.service.impl;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.operations.accesslogging.dto.SimpleQueryLogDto;
import cc.cornerstones.biz.operations.accesslogging.entity.QueryLogDo;
import cc.cornerstones.biz.operations.accesslogging.persistence.QueryLogRepository;
import cc.cornerstones.biz.operations.performancelogging.service.inf.PerformanceLoggingService;
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
import java.util.ArrayList;
import java.util.List;

@Service
public class PerformanceLoggingServiceImpl implements PerformanceLoggingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceLoggingServiceImpl.class);

    @Autowired
    private QueryLogRepository queryLogRepository;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public Page<SimpleQueryLogDto> pagingQuerySlowQueryLogs(
            String trackingSerialNumber,
            Long dataFacetUid,
            String dataFacetName,
            Long userUid,
            String displayName,
            List<String> createdTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        final long slowQueryDurationInMillisThreshold = 30 * 1000;
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
                if (userUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("userUid"), userUid));
                }
                if (!ObjectUtils.isEmpty(displayName)) {
                    predicateList.add(criteriaBuilder.like(root.get("displayName"), "%" + displayName + "%"));
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
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Order.desc(
                    "queryDurationInMillis")));
        }

        Page<QueryLogDo> itemDoPage = this.queryLogRepository.findAll(specification, pageable);
        List<SimpleQueryLogDto> content = new ArrayList<>(itemDoPage.getSize());
        itemDoPage.forEach(itemDo -> {
            SimpleQueryLogDto itemDto = new SimpleQueryLogDto();
            BeanUtils.copyProperties(itemDo, itemDto);
            content.add(itemDto);
        });
        Page<SimpleQueryLogDto> itemDtoPage = new PageImpl<>(content, pageable, itemDoPage.getTotalElements());
        return itemDtoPage;
    }
}
