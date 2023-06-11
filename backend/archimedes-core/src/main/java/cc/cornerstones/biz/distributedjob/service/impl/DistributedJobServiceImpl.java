package cc.cornerstones.biz.distributedjob.service.impl;

import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.exceptions.AbcResourceDuplicateException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcDateUtils;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.distributedjob.dto.*;
import cc.cornerstones.biz.distributedjob.entity.DistributedJobDo;
import cc.cornerstones.biz.distributedjob.entity.DistributedJobExecutionDo;
import cc.cornerstones.biz.distributedjob.persistence.DistributedJobExecutionRepository;
import cc.cornerstones.biz.distributedjob.persistence.DistributedJobRepository;
import cc.cornerstones.biz.distributedjob.service.assembly.DistributedJobExecutor;
import cc.cornerstones.biz.distributedjob.service.inf.DistributedJobService;
import cc.cornerstones.biz.share.event.EventBusManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class DistributedJobServiceImpl implements DistributedJobService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedJobServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private DistributedJobRepository distributedJobRepository;

    @Autowired
    private DistributedJobExecutionRepository distributedJobExecutionRepository;

    @Autowired
    private DistributedJobExecutor distributedJobExecutor;

    private java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private java.time.format.DateTimeFormatter dateTimeFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public DistributedJobDto createJob(
            CreateDistributedJobDto createDistributedJobDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        StringBuilder toBeHashedString = new StringBuilder();
        toBeHashedString.append(createDistributedJobDto.getName());
        toBeHashedString.append(".").append(createDistributedJobDto.getHandlerName());
        if (createDistributedJobDto.getParameters() != null && !createDistributedJobDto.getParameters().isEmpty()) {
            toBeHashedString.append(".").append(createDistributedJobDto.getParameters().toJSONString());
        }
        String hashedString = Base64Utils.encodeToString(toBeHashedString.toString().getBytes(StandardCharsets.UTF_8));

        boolean existsDuplicate = this.distributedJobRepository.existsByHashedString(hashedString);
        if (existsDuplicate) {
            throw new AbcResourceDuplicateException(String.format("%s::uid=%d", DistributedJobDo.RESOURCE_SYMBOL,
                    createDistributedJobDto.getName()));
        }

        //
        // Step 2, core-processing
        //
        DistributedJobDo distributedJobDo = new DistributedJobDo();
        distributedJobDo.setUid(this.idHelper.getNextDistributedId(DistributedJobDo.RESOURCE_NAME));
        distributedJobDo.setName(createDistributedJobDto.getName());
        distributedJobDo.setObjectName(createDistributedJobDto.getName()
                .replaceAll("_", "__")
                .replaceAll("\\s", "_").toLowerCase());
        distributedJobDo.setDescription(createDistributedJobDto.getDescription());
        distributedJobDo.setCronExpression(createDistributedJobDto.getCronExpression());
        distributedJobDo.setEnabled(createDistributedJobDto.getEnabled());
        distributedJobDo.setHandlerName(createDistributedJobDto.getHandlerName());
        distributedJobDo.setParameters(createDistributedJobDto.getParameters());
        distributedJobDo.setFailedRetires(createDistributedJobDto.getFailedRetires());
        distributedJobDo.setRoutingAlgorithm(createDistributedJobDto.getRoutingAlgorithm());
        distributedJobDo.setTimeoutDurationInSecs(createDistributedJobDto.getTimeoutDurationInSecs());
        distributedJobDo.setHashedString(hashedString);

        CronExpression cronExpression = null;
        try {
            cronExpression = CronExpression.parse(distributedJobDo.getCronExpression());
        } catch (Exception e) {
            throw new AbcIllegalParameterException("illegal cron expression");
        }
        LocalDateTime nextLocalDateTime = cronExpression.next(LocalDateTime.now());
        distributedJobDo.setNextExecutionTimestamp(nextLocalDateTime);
        distributedJobDo.setLastExecutionTimestamp(null);
        distributedJobDo.setLastExecutorHostname(null);
        distributedJobDo.setLastExecutorIpAddress(null);
        BaseDo.create(distributedJobDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.distributedJobRepository.save(distributedJobDo);

        //
        // Step 3, post-processing
        //
        DistributedJobDto distributedJobDto = new DistributedJobDto();
        BeanUtils.copyProperties(distributedJobDo, distributedJobDto);
        return distributedJobDto;
    }

    @Override
    public void updateJob(
            Long jobUid,
            UpdateDistributedJobDto updateDistributedJobDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DistributedJobDo distributedJobDo = this.distributedJobRepository.findByUid(jobUid);
        if (distributedJobDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DistributedJobDo.RESOURCE_SYMBOL,
                    jobUid));
        }

        boolean hashedStringUpdated = false;
        StringBuilder toBeHashedString = new StringBuilder();
        if (!ObjectUtils.isEmpty(updateDistributedJobDto.getName())
                && !updateDistributedJobDto.getName().equals(distributedJobDo.getName())) {
            toBeHashedString.append(updateDistributedJobDto.getName());
            hashedStringUpdated = true;
        } else {
            toBeHashedString.append(distributedJobDo.getName());
        }
        if (!ObjectUtils.isEmpty(updateDistributedJobDto.getHandlerName())
                && !updateDistributedJobDto.getHandlerName().equals(distributedJobDo.getHandlerName())) {
            toBeHashedString.append(".").append(updateDistributedJobDto.getHandlerName());
            hashedStringUpdated = true;
        } else {
            toBeHashedString.append(".").append(distributedJobDo.getHandlerName());
        }
        if (updateDistributedJobDto.getParameters() != null) {
            toBeHashedString.append(".").append(updateDistributedJobDto.getParameters().toJSONString());
            hashedStringUpdated = true;
        } else {
            if (distributedJobDo.getHandlerName() != null && !distributedJobDo.getHandlerName().isEmpty()) {
                toBeHashedString.append(".").append(distributedJobDo.getHandlerName());
            }
        }
        String hashedString = Base64Utils.encodeToString(toBeHashedString.toString().getBytes(StandardCharsets.UTF_8));
        if (hashedStringUpdated) {
            boolean existsDuplicate = this.distributedJobRepository.existsByHashedString(hashedString);
            if (existsDuplicate) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s", DistributedJobDo.RESOURCE_SYMBOL,
                        updateDistributedJobDto.getName()));
            }
        }

        //
        // Step 2, core-processing
        //
        boolean requiredToUpdate = false;
        if (!ObjectUtils.isEmpty(updateDistributedJobDto.getName())
                && !updateDistributedJobDto.getName().equals(distributedJobDo.getName())) {
            distributedJobDo.setName(updateDistributedJobDto.getName());
            distributedJobDo.setObjectName(updateDistributedJobDto.getName()
                    .replaceAll("_", "__")
                    .replaceAll("\\s", "_").toLowerCase());
            requiredToUpdate = true;
        }
        if (updateDistributedJobDto.getDescription() != null
                && !updateDistributedJobDto.getDescription().equals(distributedJobDo.getDescription())) {
            distributedJobDo.setDescription(updateDistributedJobDto.getDescription());
            requiredToUpdate = true;
        }
        if (updateDistributedJobDto.getEnabled() != null
                && !updateDistributedJobDto.getEnabled().equals(distributedJobDo.getEnabled())) {
            distributedJobDo.setEnabled(updateDistributedJobDto.getEnabled());
            requiredToUpdate = true;
        }
        if (!ObjectUtils.isEmpty(updateDistributedJobDto.getCronExpression())
                && !updateDistributedJobDto.getCronExpression().equals(distributedJobDo.getCronExpression())) {
            CronExpression cronExpression = null;
            try {
                cronExpression = CronExpression.parse(distributedJobDo.getCronExpression());
            } catch (Exception e) {
                throw new AbcIllegalParameterException("illegal cron expression");
            }

            LocalDateTime nextLocalDateTime = cronExpression.next(LocalDateTime.now());
            distributedJobDo.setNextExecutionTimestamp(nextLocalDateTime);

            distributedJobDo.setCronExpression(updateDistributedJobDto.getCronExpression());
            requiredToUpdate = true;
        }
        if (updateDistributedJobDto.getFailedRetires() != null
                && !updateDistributedJobDto.getFailedRetires().equals(distributedJobDo.getFailedRetires())) {
            distributedJobDo.setFailedRetires(updateDistributedJobDto.getFailedRetires());
            requiredToUpdate = true;
        }
        if (!ObjectUtils.isEmpty(updateDistributedJobDto.getHandlerName())
                && !updateDistributedJobDto.getHandlerName().equals(distributedJobDo.getHandlerName())) {
            distributedJobDo.setHandlerName(updateDistributedJobDto.getHandlerName());
            requiredToUpdate = true;
        }
        if (updateDistributedJobDto.getParameters() != null) {
            distributedJobDo.setParameters(updateDistributedJobDto.getParameters());
            requiredToUpdate = true;
        }
        if (updateDistributedJobDto.getRoutingAlgorithm() != null
                && !updateDistributedJobDto.getRoutingAlgorithm().equals(distributedJobDo.getRoutingAlgorithm())) {
            distributedJobDo.setRoutingAlgorithm(updateDistributedJobDto.getRoutingAlgorithm());
            requiredToUpdate = true;
        }
        if (updateDistributedJobDto.getTimeoutDurationInSecs() != null
                && !updateDistributedJobDto.getTimeoutDurationInSecs().equals(distributedJobDo.getTimeoutDurationInSecs())) {
            distributedJobDo.setTimeoutDurationInSecs(updateDistributedJobDto.getTimeoutDurationInSecs());
            requiredToUpdate = true;
        }
        if (hashedStringUpdated) {
            distributedJobDo.setHashedString(hashedString);
            requiredToUpdate = true;
        }

        if (requiredToUpdate) {
            BaseDo.update(distributedJobDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.distributedJobRepository.save(distributedJobDo);
        }

        //
        // Step 3, post-processing
        //
    }

    @Override
    public DistributedJobDto getJob(
            Long jobUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        DistributedJobDo distributedJobDo = this.distributedJobRepository.findByUid(jobUid);
        if (distributedJobDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DistributedJobDo.RESOURCE_SYMBOL,
                    jobUid));
        }

        DistributedJobDto distributedJobDto = new DistributedJobDto();
        BeanUtils.copyProperties(distributedJobDo, distributedJobDto);
        return distributedJobDto;
    }

    @Override
    public List<String> listAllReferencesToJob(
            Long jobUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return null;
    }

    @Override
    public void deleteJob(
            Long jobUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        DistributedJobDo distributedJobDo = this.distributedJobRepository.findByUid(jobUid);
        if (distributedJobDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DistributedJobDo.RESOURCE_SYMBOL,
                    jobUid));
        }

        distributedJobDo.setDeleted(Boolean.TRUE);
        BaseDo.update(distributedJobDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.distributedJobRepository.save(distributedJobDo);
    }

    @Override
    public Page<DistributedJobDto> pagingQueryJobs(
            Long jobUid,
            String jobName,
            Boolean enabled,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Specification<DistributedJobDo> specification = new Specification<DistributedJobDo>() {
            @Override
            public Predicate toPredicate(Root<DistributedJobDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (jobUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("uid"), jobUid));
                }
                if (!ObjectUtils.isEmpty(jobName)) {
                    predicateList.add(criteriaBuilder.like(root.get("name"), "%" + jobName + "%"));
                }
                if (enabled != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("enabled"), enabled));
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        if (pageable == null) {
            pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc(BaseDo.ID_FIELD_NAME)));
        } else if (pageable.getSort() == null || pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Order.desc(BaseDo.ID_FIELD_NAME)));
        }

        Page<DistributedJobDo> itemDoPage = this.distributedJobRepository.findAll(specification, pageable);
        List<DistributedJobDto> content = new ArrayList<>(itemDoPage.getSize());
        itemDoPage.forEach(itemDo -> {
            DistributedJobDto itemDto = new DistributedJobDto();
            BeanUtils.copyProperties(itemDo, itemDto);
            content.add(itemDto);
        });
        Page<DistributedJobDto> itemDtoPage = new PageImpl<>(content, pageable, itemDoPage.getTotalElements());
        return itemDtoPage;
    }

    @Override
    public DistributedJobExecutionDto startJobExecution(
            Long jobUid) throws AbcUndefinedException {
        DistributedJobDo distributedJobDo = this.distributedJobRepository.findByUid(jobUid);
        if (distributedJobDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DistributedJobDo.RESOURCE_SYMBOL, jobUid));
        }

        return this.distributedJobExecutor.execute(distributedJobDo);
    }

    @Override
    public DistributedJobExecutionDto stopJobExecution(
            Long jobExecutionUid) throws AbcUndefinedException {
        return this.distributedJobExecutor.stop(jobExecutionUid);
    }

    @Override
    public Page<DistributedJobExecutionDto> pagingQueryJobExecutions(
            Long jobUid,
            Long uid,
            List<JobStatusEnum> statusList,
            List<String> createdTimestampAsStringList,
            List<String> lastModifiedTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Specification<DistributedJobExecutionDo> specification = new Specification<DistributedJobExecutionDo>() {
            @Override
            public Predicate toPredicate(Root<DistributedJobExecutionDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (jobUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("jobUid"), jobUid));
                }
                if (uid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("uid"), uid));
                }
                if (!CollectionUtils.isEmpty(statusList)) {
                    CriteriaBuilder.In<JobStatusEnum> in = criteriaBuilder.in(root.get("status"));
                    statusList.forEach(item -> {
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
                        lastModifiedTimestampAsStringList.forEach(lastModifiedTimestampAsString -> {
                            LocalDateTime dateTime0 = LocalDateTime.parse(lastModifiedTimestampAsString, dateTimeFormatter);
                            in.value(dateTime0);
                        });
                        predicateList.add(in);
                    }
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        if (pageable == null) {
            pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc(BaseDo.ID_FIELD_NAME)));
        } else if (pageable.getSort() == null || pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Order.desc(BaseDo.ID_FIELD_NAME)));
        }

        Page<DistributedJobExecutionDo> itemDoPage = this.distributedJobExecutionRepository.findAll(specification, pageable);
        List<DistributedJobExecutionDto> content = new ArrayList<>(itemDoPage.getSize());
        itemDoPage.forEach(itemDo -> {
            DistributedJobExecutionDto itemDto = new DistributedJobExecutionDto();
            BeanUtils.copyProperties(itemDo, itemDto);

            switch (itemDo.getStatus()) {
                case CANCELED:
                case FAILED:
                case FINISHED:
                    itemDto.setExecutionDuration(AbcDateUtils.format(Duration.between(itemDo.getBeginTimestamp(),
                            itemDo.getEndTimestamp()).toMillis()));
                    break;
                default:
                    break;
            }

            itemDto.setTotalDuration(AbcDateUtils.format(Duration.between(itemDo.getCreatedTimestamp(),
                    itemDo.getLastModifiedTimestamp()).toMillis()));

            content.add(itemDto);
        });
        Page<DistributedJobExecutionDto> itemDtoPage = new PageImpl<>(content, pageable, itemDoPage.getTotalElements());
        return itemDtoPage;
    }
}
