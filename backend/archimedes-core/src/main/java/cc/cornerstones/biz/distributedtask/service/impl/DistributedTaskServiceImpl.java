package cc.cornerstones.biz.distributedtask.service.impl;

import cc.cornerstones.almond.constants.TaskStatusEnum;
import cc.cornerstones.almond.exceptions.*;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcDateUtils;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.distributedtask.dto.CreateDistributedTaskDto;
import cc.cornerstones.biz.distributedtask.dto.DistributedTaskDto;
import cc.cornerstones.biz.distributedtask.entity.DistributedTaskDo;
import cc.cornerstones.biz.distributedtask.persistence.DistributedTaskRepository;
import cc.cornerstones.biz.distributedtask.service.assembly.DistributedTaskExecutor;
import cc.cornerstones.biz.distributedtask.service.inf.DistributedTaskService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service
public class DistributedTaskServiceImpl implements DistributedTaskService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedTaskServiceImpl.class);

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private DistributedTaskRepository distributedTaskRepository;

    @Autowired
    private DistributedTaskExecutor distributedTaskExecutor;

    private java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private java.time.format.DateTimeFormatter dateTimeFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");


    /**
     * task 可大可小，要控制并发
     */
    private final ThreadFactory NAMED_THREAD_FACTORY = new ThreadFactoryBuilder()
            .setNameFormat("TASK-%d").build();
    private final ExecutorService executorService = new ThreadPoolExecutor(
            3,
            5,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(100),
            NAMED_THREAD_FACTORY,
            new ThreadPoolExecutor.AbortPolicy());

    @Override
    public DistributedTaskDto createTask(
            CreateDistributedTaskDto createDistributedTaskDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //


        //
        // Step 2, core-processing
        //
        DistributedTaskDo distributedTaskDo = new DistributedTaskDo();
        distributedTaskDo.setUid(this.idHelper.getNextDistributedId(DistributedTaskDo.RESOURCE_NAME));
        distributedTaskDo.setStatus(TaskStatusEnum.CREATED);
        distributedTaskDo.setName(createDistributedTaskDto.getName());
        distributedTaskDo.setPayload(createDistributedTaskDto.getPayload());
        distributedTaskDo.setType(createDistributedTaskDto.getType());
        distributedTaskDo.setHandlerName(createDistributedTaskDto.getHandlerName());
        BaseDo.create(distributedTaskDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.distributedTaskRepository.save(distributedTaskDo);

        //
        // Step 3, post-processing
        //
        DistributedTaskDto distributedTaskDto = new DistributedTaskDto();
        BeanUtils.copyProperties(distributedTaskDo, distributedTaskDto);
        return distributedTaskDto;
    }

    @Override
    public void startTask(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        DistributedTaskDo distributedTaskDo = this.distributedTaskRepository.findByUid(uid);
        if (distributedTaskDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DistributedTaskDo.RESOURCE_SYMBOL,
                    uid));
        }
        if (!TaskStatusEnum.CREATED.equals(distributedTaskDo.getStatus())) {
            throw new AbcResourceIntegrityException(String.format("expected status is CREATED, but get %s, task " +
                    "uid:%d", distributedTaskDo.getStatus(), distributedTaskDo.getUid()));
        }
        this.distributedTaskExecutor.execute(distributedTaskDo);
    }

    @Override
    public void stopTask(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        DistributedTaskDo distributedTaskDo = this.distributedTaskRepository.findByUid(uid);
        if (distributedTaskDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DistributedTaskDo.RESOURCE_SYMBOL,
                    uid));
        }
        if (!TaskStatusEnum.RUNNING.equals(distributedTaskDo.getStatus())) {
            throw new AbcResourceIntegrityException(String.format("expected status is RUNNING, but get %s, task " +
                    "uid:%d", distributedTaskDo.getStatus(), distributedTaskDo.getUid()));
        }

    }

    @Override
    public DistributedTaskDto getTask(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        DistributedTaskDo distributedTaskDo = this.distributedTaskRepository.findByUid(uid);
        if (distributedTaskDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DistributedTaskDo.RESOURCE_SYMBOL,
                    uid));
        }

        DistributedTaskDto distributedTaskDto = new DistributedTaskDto();
        BeanUtils.copyProperties(distributedTaskDo, distributedTaskDto);
        return distributedTaskDto;
    }

    @Override
    public Page<DistributedTaskDto> pagingQueryTasks(
            Long uid,
            String name,
            List<TaskStatusEnum> statusList,
            List<String> createdTimestampAsStringList,
            List<String> lastModifiedTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Specification<DistributedTaskDo> specification = new Specification<DistributedTaskDo>() {
            @Override
            public Predicate toPredicate(Root<DistributedTaskDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (uid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("uid"), uid));
                }
                if (!ObjectUtils.isEmpty(name)) {
                    predicateList.add(criteriaBuilder.like(root.get("name"), "%" + name + "%"));
                }
                if (!CollectionUtils.isEmpty(statusList)) {
                    CriteriaBuilder.In<TaskStatusEnum> in = criteriaBuilder.in(root.get("status"));
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

        Page<DistributedTaskDo> itemDoPage = this.distributedTaskRepository.findAll(specification, pageable);
        List<DistributedTaskDto> content = new ArrayList<>(itemDoPage.getSize());
        itemDoPage.forEach(itemDo -> {
            DistributedTaskDto itemDto = new DistributedTaskDto();
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
        Page<DistributedTaskDto> itemDtoPage = new PageImpl<>(content, pageable, itemDoPage.getTotalElements());
        return itemDtoPage;
    }
}
