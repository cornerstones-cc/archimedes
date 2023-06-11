package cc.cornerstones.biz.serve.service.impl;

import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.serve.dto.CreateSessionDto;
import cc.cornerstones.biz.serve.dto.SessionDto;
import cc.cornerstones.biz.serve.entity.SessionDo;
import cc.cornerstones.biz.serve.persistence.SessionRepository;
import cc.cornerstones.biz.serve.service.inf.SessionService;
import cc.cornerstones.biz.serve.share.constants.SessionStatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
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
import java.util.LinkedList;
import java.util.List;

@Service
public class SessionServiceImpl implements SessionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionServiceImpl.class);

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private SessionRepository sessionRepository;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");


    @Override
    public SessionDto createSessionForDataWidget(
            Long dataWidgetUid,
            CreateSessionDto createSessionDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        //
        // Step 2, core-processing
        //
        SessionDo sessionDo = new SessionDo();
        sessionDo.setUid(this.idHelper.getNextDistributedId(SessionDo.RESOURCE_NAME));
        sessionDo.setName(createSessionDto.getName());
        sessionDo.setContent(createSessionDto.getContent());
        sessionDo.setStatus(SessionStatusEnum.EFFECTIVE);
        sessionDo.setDataWidgetUid(dataWidgetUid);
        BaseDo.create(sessionDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.sessionRepository.save(sessionDo);

        //
        // Step 3, post-processing
        //
        SessionDto sessionDto = new SessionDto();
        BeanUtils.copyProperties(sessionDo, sessionDto);
        return sessionDto;
    }

    @Override
    public void deleteMySessionOfDataWidget(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        SessionDo sessionDo = this.sessionRepository.findByUid(uid);
        if (sessionDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", SessionDo.RESOURCE_SYMBOL, uid));
        }

        //
        // Step 2, core-processing
        //
        if (!operatingUserProfile.getUid().equals(sessionDo.getOwner())) {
            LOGGER.warn("user {} trying to delete another user {}'s session {}, be rejected",
                    operatingUserProfile.getUid(),
                    sessionDo.getOwner(), uid);
            throw new AbcResourceConflictException("deleting another user's session is not allowed");
        }

        //
        // Step 3, post-processing
        //
        sessionDo.setDeleted(Boolean.TRUE);
        BaseDo.update(sessionDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.sessionRepository.save(sessionDo);
    }

    @Override
    public List<SessionDto> listingQuerySessionsOfDataWidget(
            Long userUid,
            Long dataWidgetUid,
            Long uid,
            String name,
            List<String> lastModifiedTimestampAsStringList,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Specification<SessionDo> specification = new Specification<SessionDo>() {
            @Override
            public Predicate toPredicate(Root<SessionDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();

                if (userUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get(BaseDo.OWNER_FIELD_NAME), userUid));
                }
                if (dataWidgetUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("dataWidgetUid"), dataWidgetUid));
                }
                if (uid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("uid"), uid));
                }
                if (!ObjectUtils.isEmpty(name)) {
                    predicateList.add(criteriaBuilder.like(root.get("name"), "%" + name + "%"));
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

        if (sort == null) {
            sort = Sort.by(Sort.Order.desc(BaseDo.ID_FIELD_NAME));
        } else if (sort.isUnsorted()) {
            sort = Sort.by(Sort.Order.desc(BaseDo.ID_FIELD_NAME));
        }

        List<SessionDo> itemDoList = this.sessionRepository.findAll(specification, sort);
        List<SessionDto> itemDtoList = new LinkedList<>();
        itemDoList.forEach(itemDo -> {
            SessionDto itemDto = new SessionDto();
            BeanUtils.copyProperties(itemDo, itemDto);
            itemDtoList.add(itemDto);
        });
        return itemDtoList;
    }
}
