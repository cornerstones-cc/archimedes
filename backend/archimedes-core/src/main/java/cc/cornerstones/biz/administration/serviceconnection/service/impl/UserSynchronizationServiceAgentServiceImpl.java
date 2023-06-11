package cc.cornerstones.biz.administration.serviceconnection.service.impl;

import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.exceptions.*;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserBriefInformation;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcDateUtils;
import cc.cornerstones.arbutus.pf4j.service.assembly.PluginHelper;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.administration.serviceconnection.dto.*;
import cc.cornerstones.biz.administration.serviceconnection.entity.*;
import cc.cornerstones.biz.administration.serviceconnection.persistence.UserSynchronizationExecutionInstanceRepository;
import cc.cornerstones.biz.administration.serviceconnection.persistence.UserSynchronizationServiceComponentRepository;
import cc.cornerstones.biz.administration.serviceconnection.persistence.UserSynchronizationServiceAgentRepository;
import cc.cornerstones.biz.administration.serviceconnection.service.assembly.UserSynchronizationHandler;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DfsServiceAgentService;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.UserSynchronizationServiceComponentService;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.UserSynchronizationServiceAgentService;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.distributedjob.dto.CreateDistributedJobDto;
import cc.cornerstones.biz.distributedjob.dto.DistributedJobDto;
import cc.cornerstones.biz.distributedjob.dto.UpdateDistributedJobDto;
import cc.cornerstones.biz.distributedjob.service.inf.DistributedJobService;
import cc.cornerstones.biz.distributedjob.share.constants.JobExecutorRoutingAlgorithmEnum;
import cc.cornerstones.biz.share.assembly.ResourceReferenceManager;
import cc.cornerstones.biz.share.event.DataPermissionServiceComponentDeletedEvent;
import cc.cornerstones.biz.share.event.EventBusManager;
import cc.cornerstones.biz.share.event.UserSynchronizationServiceAgentDeletedEvent;
import cc.cornerstones.biz.share.event.UserSynchronizationServiceComponentDeletedEvent;
import cc.cornerstones.biz.share.types.ResourceReferenceHandler;
import com.alibaba.fastjson.JSONObject;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.PostConstruct;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class UserSynchronizationServiceAgentServiceImpl implements UserSynchronizationServiceAgentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserSynchronizationServiceAgentServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private UserSynchronizationServiceComponentRepository userSynchronizationServiceComponentRepository;

    @Autowired
    private UserSynchronizationServiceComponentService userSynchronizationServiceComponentService;

    @Autowired
    private UserSynchronizationServiceAgentRepository userSynchronizationServiceAgentRepository;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    private PluginHelper pluginHelper;

    @Autowired
    private DfsServiceAgentService dfsServiceAgentService;

    @Autowired
    private DistributedJobService distributedJobService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserSynchronizationExecutionInstanceRepository userSynchronizationExecutionInstanceRepository;

    @Autowired
    private UserSynchronizationHandler userSynchronizationHandler;

    @Autowired
    private ResourceReferenceManager resourceReferenceManager;


    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserSynchronizationServiceAgentDto createUserSynchronizationServiceAgent(
            CreateUserSynchronizationServiceAgentDto createUserSynchronizationServiceAgentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        boolean existsDuplicate =
                this.userSynchronizationServiceAgentRepository.existsByName(createUserSynchronizationServiceAgentDto.getName());
        if (existsDuplicate) {
            throw new AbcResourceDuplicateException(String.format("%s::name=%s", UserSynchronizationServiceAgentDo.RESOURCE_SYMBOL,
                    createUserSynchronizationServiceAgentDto.getName()));
        }

        boolean existsUserSynchronizationServiceComponent = this.userSynchronizationServiceComponentRepository.existsByUid(
                createUserSynchronizationServiceAgentDto.getServiceComponentUid());
        if (!existsUserSynchronizationServiceComponent) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    UserSynchronizationServiceComponentDo.RESOURCE_SYMBOL,
                    createUserSynchronizationServiceAgentDto.getServiceComponentUid()));
        }

        //
        // Step 2, core-processing
        //
        UserSynchronizationServiceAgentDo userSynchronizationServiceAgentDo = new UserSynchronizationServiceAgentDo();
        userSynchronizationServiceAgentDo.setUid(this.idHelper.getNextDistributedId(UserSynchronizationServiceAgentDo.RESOURCE_NAME));
        userSynchronizationServiceAgentDo.setName(createUserSynchronizationServiceAgentDto.getName());
        userSynchronizationServiceAgentDo.setObjectName(
                createUserSynchronizationServiceAgentDto.getName()
                        .replaceAll("_", "__")
                        .replaceAll("\\s", "_")
                        .toLowerCase());
        userSynchronizationServiceAgentDo.setDescription(createUserSynchronizationServiceAgentDto.getDescription());
        userSynchronizationServiceAgentDo.setSequence(createUserSynchronizationServiceAgentDto.getSequence());
        userSynchronizationServiceAgentDo.setEnabled(createUserSynchronizationServiceAgentDto.getEnabled());
        userSynchronizationServiceAgentDo.setServiceComponentUid(createUserSynchronizationServiceAgentDto.getServiceComponentUid());
        userSynchronizationServiceAgentDo.setConfiguration(
                createUserSynchronizationServiceAgentDto.getConfiguration());

        if (!ObjectUtils.isEmpty(createUserSynchronizationServiceAgentDto.getCronExpression())) {
            CronExpression cronExpression = null;
            try {
                cronExpression = CronExpression.parse(createUserSynchronizationServiceAgentDto.getCronExpression());
            } catch (Exception e) {
                throw new AbcIllegalParameterException("illegal cron expression");
            }

            userSynchronizationServiceAgentDo.setCronExpression(createUserSynchronizationServiceAgentDto.getCronExpression());
        }

        // 调度 job
        if (Boolean.TRUE.equals(userSynchronizationServiceAgentDo.getEnabled())
                && !ObjectUtils.isEmpty(userSynchronizationServiceAgentDo.getCronExpression())) {
            CreateDistributedJobDto createDistributedJobDto = new CreateDistributedJobDto();
            createDistributedJobDto.setName(userSynchronizationServiceAgentDo.getName());
            createDistributedJobDto.setDescription(userSynchronizationServiceAgentDo.getDescription());
            createDistributedJobDto.setCronExpression(userSynchronizationServiceAgentDo.getCronExpression());
            createDistributedJobDto.setEnabled(Boolean.TRUE);
            createDistributedJobDto.setFailedRetires(0);
            createDistributedJobDto.setHandlerName(UserSynchronizationHandler.JOB_HANDLER_USER_SYNCHRONIZATION);

            JSONObject parameters = new JSONObject();
            parameters.put("user_synchronization_service_agent_uid", userSynchronizationServiceAgentDo.getUid());
            createDistributedJobDto.setParameters(parameters);
            createDistributedJobDto.setRoutingAlgorithm(JobExecutorRoutingAlgorithmEnum.ROUND_ROBIN);
            createDistributedJobDto.setTimeoutDurationInSecs(3600L);
            DistributedJobDto distributedJobDto = this.distributedJobService.createJob(createDistributedJobDto, operatingUserProfile);

            userSynchronizationServiceAgentDo.setJobUid(distributedJobDto.getUid());
        }

        BaseDo.create(userSynchronizationServiceAgentDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.userSynchronizationServiceAgentRepository.save(userSynchronizationServiceAgentDo);

        //
        // Step 3, post-processing
        //
        UserSynchronizationServiceAgentDto userSynchronizationServiceAgentDto = new UserSynchronizationServiceAgentDto();
        BeanUtils.copyProperties(userSynchronizationServiceAgentDo, userSynchronizationServiceAgentDto);

        UserSynchronizationServiceComponentDto userSynchronizationServiceComponentDto =
                this.userSynchronizationServiceComponentService.getUserSynchronizationServiceComponent(
                        userSynchronizationServiceAgentDo.getServiceComponentUid(),
                        operatingUserProfile);
        userSynchronizationServiceAgentDto.setServiceComponent(userSynchronizationServiceComponentDto);

        return userSynchronizationServiceAgentDto;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateUserSynchronizationServiceAgent(
            Long uid,
            UpdateUserSynchronizationServiceAgentDto updateUserSynchronizationServiceAgentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        UserSynchronizationServiceAgentDo userSynchronizationServiceAgentDo = this.userSynchronizationServiceAgentRepository.findByUid(uid);
        if (userSynchronizationServiceAgentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", UserSynchronizationServiceAgentDo.RESOURCE_SYMBOL,
                    uid));
        }

        if (!ObjectUtils.isEmpty(updateUserSynchronizationServiceAgentDto.getName())
                && !updateUserSynchronizationServiceAgentDto.getName().equalsIgnoreCase(userSynchronizationServiceAgentDo.getName())) {
            boolean existsDuplicate =
                    this.userSynchronizationServiceAgentRepository.existsByName(updateUserSynchronizationServiceAgentDto.getName());
            if (existsDuplicate) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s", UserSynchronizationServiceAgentDo.RESOURCE_SYMBOL,
                        updateUserSynchronizationServiceAgentDto.getName()));
            }
        }

        if (!ObjectUtils.isEmpty(updateUserSynchronizationServiceAgentDto.getCronExpression())
                && !updateUserSynchronizationServiceAgentDto.getCronExpression().equals(userSynchronizationServiceAgentDo.getCronExpression())) {
            CronExpression cronExpression = null;
            try {
                cronExpression = CronExpression.parse(updateUserSynchronizationServiceAgentDto.getCronExpression());
            } catch (Exception e) {
                throw new AbcIllegalParameterException("illegal cron expression");
            }
        }

        if (updateUserSynchronizationServiceAgentDto.getServiceComponentUid() != null
                && !updateUserSynchronizationServiceAgentDto.getServiceComponentUid().equals(userSynchronizationServiceAgentDo.getServiceComponentUid())) {
            boolean existsUserSynchronizationServiceComponent = this.userSynchronizationServiceComponentRepository.existsByUid(
                    updateUserSynchronizationServiceAgentDto.getServiceComponentUid());
            if (!existsUserSynchronizationServiceComponent) {
                throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                        UserSynchronizationServiceComponentDo.RESOURCE_SYMBOL,
                        updateUserSynchronizationServiceAgentDto.getServiceComponentUid()));
            }
        }

        //
        // Step 2, core-processing
        //
        boolean requiredToUpdate = false;

        if (!ObjectUtils.isEmpty(updateUserSynchronizationServiceAgentDto.getName())
                && !updateUserSynchronizationServiceAgentDto.getName().equalsIgnoreCase(userSynchronizationServiceAgentDo.getName())) {
            userSynchronizationServiceAgentDo.setName(updateUserSynchronizationServiceAgentDto.getName());
            userSynchronizationServiceAgentDo.setObjectName(
                    updateUserSynchronizationServiceAgentDto.getName()
                            .replaceAll("_", "__")
                            .replaceAll("\\s", "_")
                            .toLowerCase());
            requiredToUpdate = true;
        }
        if (updateUserSynchronizationServiceAgentDto.getDescription() != null
                && !updateUserSynchronizationServiceAgentDto.getDescription().equalsIgnoreCase(userSynchronizationServiceAgentDo.getDescription())) {
            userSynchronizationServiceAgentDo.setDescription(updateUserSynchronizationServiceAgentDto.getDescription());
            requiredToUpdate = true;
        }
        if (updateUserSynchronizationServiceAgentDto.getSequence() != null
                && !updateUserSynchronizationServiceAgentDto.getSequence().equals(userSynchronizationServiceAgentDo.getSequence())) {
            userSynchronizationServiceAgentDo.setSequence(updateUserSynchronizationServiceAgentDto.getSequence());
            requiredToUpdate = true;
        }
        if (updateUserSynchronizationServiceAgentDto.getEnabled() != null
                && !updateUserSynchronizationServiceAgentDto.getEnabled().equals(userSynchronizationServiceAgentDo.getEnabled())) {
            userSynchronizationServiceAgentDo.setEnabled(updateUserSynchronizationServiceAgentDto.getEnabled());
            requiredToUpdate = true;
        }
        if (!ObjectUtils.isEmpty(updateUserSynchronizationServiceAgentDto.getConfiguration())) {
            userSynchronizationServiceAgentDo.setConfiguration(updateUserSynchronizationServiceAgentDto.getConfiguration());
            requiredToUpdate = true;
        }
        if (!ObjectUtils.isEmpty(updateUserSynchronizationServiceAgentDto.getCronExpression())
                && !updateUserSynchronizationServiceAgentDto.getCronExpression().equals(userSynchronizationServiceAgentDo.getCronExpression())) {
            userSynchronizationServiceAgentDo.setCronExpression(updateUserSynchronizationServiceAgentDto.getCronExpression());
            requiredToUpdate = true;
        }
        if (updateUserSynchronizationServiceAgentDto.getServiceComponentUid() != null
                && !updateUserSynchronizationServiceAgentDto.getServiceComponentUid().equals(userSynchronizationServiceAgentDo.getServiceComponentUid())) {
            userSynchronizationServiceAgentDo.setServiceComponentUid(updateUserSynchronizationServiceAgentDto.getServiceComponentUid());
            requiredToUpdate = true;
        }

        if (requiredToUpdate) {
            BaseDo.update(userSynchronizationServiceAgentDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.userSynchronizationServiceAgentRepository.save(userSynchronizationServiceAgentDo);
        }

        //
        // Step 3, post-processing
        //
        if (userSynchronizationServiceAgentDo.getJobUid() == null) {
            if (Boolean.TRUE.equals(userSynchronizationServiceAgentDo.getEnabled())
                    && !ObjectUtils.isEmpty(userSynchronizationServiceAgentDo.getCronExpression())) {
                CreateDistributedJobDto createDistributedJobDto = new CreateDistributedJobDto();
                createDistributedJobDto.setName(userSynchronizationServiceAgentDo.getName());
                createDistributedJobDto.setDescription(userSynchronizationServiceAgentDo.getDescription());
                createDistributedJobDto.setCronExpression(userSynchronizationServiceAgentDo.getCronExpression());
                createDistributedJobDto.setEnabled(Boolean.TRUE);
                createDistributedJobDto.setFailedRetires(0);
                createDistributedJobDto.setHandlerName(UserSynchronizationHandler.JOB_HANDLER_USER_SYNCHRONIZATION);

                JSONObject parameters = new JSONObject();
                parameters.put("user_synchronization_service_agent_uid", userSynchronizationServiceAgentDo.getUid());
                createDistributedJobDto.setParameters(parameters);
                createDistributedJobDto.setRoutingAlgorithm(JobExecutorRoutingAlgorithmEnum.ROUND_ROBIN);
                createDistributedJobDto.setTimeoutDurationInSecs(3600L);
                DistributedJobDto distributedJobDto = this.distributedJobService.createJob(createDistributedJobDto, operatingUserProfile);

                userSynchronizationServiceAgentDo.setJobUid(distributedJobDto.getUid());
            }
        } else {
            if (Boolean.TRUE.equals(userSynchronizationServiceAgentDo.getEnabled())
                    && !ObjectUtils.isEmpty(userSynchronizationServiceAgentDo.getCronExpression())) {
                UpdateDistributedJobDto updateDistributedJobDto = new UpdateDistributedJobDto();
                updateDistributedJobDto.setEnabled(Boolean.TRUE);
                updateDistributedJobDto.setName(userSynchronizationServiceAgentDo.getName());
                updateDistributedJobDto.setDescription(userSynchronizationServiceAgentDo.getDescription());
                updateDistributedJobDto.setCronExpression(userSynchronizationServiceAgentDo.getCronExpression());
                this.distributedJobService.updateJob(userSynchronizationServiceAgentDo.getJobUid(), updateDistributedJobDto, operatingUserProfile);
            } else {
                this.distributedJobService.deleteJob(userSynchronizationServiceAgentDo.getJobUid(), operatingUserProfile);
            }
        }
    }

    @Override
    public List<String> listAllReferencesToUserSynchronizationServiceAgent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        UserSynchronizationServiceAgentDo userSynchronizationServiceAgentDo = this.userSynchronizationServiceAgentRepository.findByUid(uid);
        if (userSynchronizationServiceAgentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", UserSynchronizationServiceAgentDo.RESOURCE_SYMBOL,
                    uid));
        }

        //
        // Step 2, core-processing
        //
        return this.resourceReferenceManager.check(
                ResourceReferenceManager.ResourceCategoryEnum.USER_SYNCHRONIZATION_SERVICE_AGENT,
                userSynchronizationServiceAgentDo.getUid(),
                userSynchronizationServiceAgentDo.getName());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteUserSynchronizationServiceAgent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        UserSynchronizationServiceAgentDo userSynchronizationServiceAgentDo = this.userSynchronizationServiceAgentRepository.findByUid(uid);
        if (userSynchronizationServiceAgentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", UserSynchronizationServiceAgentDo.RESOURCE_SYMBOL,
                    uid));
        }

        deleteUserSynchronizationServiceAgent(userSynchronizationServiceAgentDo, operatingUserProfile);
    }

    private void deleteUserSynchronizationServiceAgent(
            UserSynchronizationServiceAgentDo userSynchronizationServiceAgentDo,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        userSynchronizationServiceAgentDo.setDeleted(Boolean.TRUE);
        BaseDo.update(userSynchronizationServiceAgentDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.userSynchronizationServiceAgentRepository.save(userSynchronizationServiceAgentDo);

        if (userSynchronizationServiceAgentDo.getJobUid() != null) {
            this.distributedJobService.deleteJob(userSynchronizationServiceAgentDo.getJobUid(), operatingUserProfile);
        }

        // event post
        UserSynchronizationServiceAgentDeletedEvent event = new UserSynchronizationServiceAgentDeletedEvent();
        event.setUserSynchronizationServiceAgentDo(userSynchronizationServiceAgentDo);
        event.setOperatingUserProfile(operatingUserProfile);
        this.eventBusManager.post(event);
    }

    @Override
    public UserSynchronizationServiceAgentDto getUserSynchronizationServiceAgent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        UserSynchronizationServiceAgentDo userSynchronizationServiceAgentDo = this.userSynchronizationServiceAgentRepository.findByUid(uid);
        if (userSynchronizationServiceAgentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", UserSynchronizationServiceAgentDo.RESOURCE_SYMBOL,
                    uid));
        }

        UserSynchronizationServiceAgentDto userSynchronizationServiceAgentDto = new UserSynchronizationServiceAgentDto();
        BeanUtils.copyProperties(userSynchronizationServiceAgentDo, userSynchronizationServiceAgentDto);

        UserSynchronizationServiceComponentDto userSynchronizationServiceComponentDto =
                this.userSynchronizationServiceComponentService.getUserSynchronizationServiceComponent(
                        userSynchronizationServiceAgentDo.getServiceComponentUid(),
                        operatingUserProfile);
        userSynchronizationServiceAgentDto.setServiceComponent(userSynchronizationServiceComponentDto);

        return userSynchronizationServiceAgentDto;
    }

    @Override
    public List<UserSynchronizationServiceAgentDto> listingQueryUserSynchronizationServiceAgents(
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
        Specification<UserSynchronizationServiceAgentDo> specification = new Specification<UserSynchronizationServiceAgentDo>() {
            @Override
            public Predicate toPredicate(Root<UserSynchronizationServiceAgentDo> root, CriteriaQuery<?> query,
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

        List<UserSynchronizationServiceAgentDo> itemDoList = this.userSynchronizationServiceAgentRepository.findAll(specification, sort);

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
        List<UserSynchronizationServiceAgentDto> content = new LinkedList<>();
        itemDoList.forEach(itemDo -> {
            UserSynchronizationServiceAgentDto itemDto = new UserSynchronizationServiceAgentDto();
            BeanUtils.copyProperties(itemDo, itemDto);

            UserSynchronizationServiceComponentDto userSynchronizationServiceComponentDto =
                    this.userSynchronizationServiceComponentService.getUserSynchronizationServiceComponent(
                            itemDo.getServiceComponentUid(),
                            operatingUserProfile);
            itemDto.setServiceComponent(userSynchronizationServiceComponentDto);

            // 为 created by, last modified by 补充 user brief information
            itemDto.setCreatedByUser(userBriefInformationMap.get(itemDo.getCreatedBy()));
            itemDto.setLastModifiedByUser(userBriefInformationMap.get(itemDo.getLastModifiedBy()));

            content.add(itemDto);
        });
        return content;
    }

    @Override
    public Page<UserSynchronizationServiceAgentDto> pagingQueryUserSynchronizationServiceAgents(
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
        Specification<UserSynchronizationServiceAgentDo> specification = new Specification<UserSynchronizationServiceAgentDo>() {
            @Override
            public Predicate toPredicate(Root<UserSynchronizationServiceAgentDo> root, CriteriaQuery<?> query,
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

        Page<UserSynchronizationServiceAgentDo> itemDoPage = this.userSynchronizationServiceAgentRepository.findAll(specification, pageable);

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
        List<UserSynchronizationServiceAgentDto> content = new ArrayList<>(itemDoPage.getSize());
        itemDoPage.forEach(itemDo -> {
            UserSynchronizationServiceAgentDto itemDto = new UserSynchronizationServiceAgentDto();
            BeanUtils.copyProperties(itemDo, itemDto);

            UserSynchronizationServiceComponentDto userSynchronizationServiceComponentDto =
                    this.userSynchronizationServiceComponentService.getUserSynchronizationServiceComponent(
                            itemDo.getServiceComponentUid(),
                            operatingUserProfile);
            itemDto.setServiceComponent(userSynchronizationServiceComponentDto);

            // 为 created by, last modified by 补充 user brief information
            itemDto.setCreatedByUser(userBriefInformationMap.get(itemDo.getCreatedBy()));
            itemDto.setLastModifiedByUser(userBriefInformationMap.get(itemDo.getLastModifiedBy()));

            content.add(itemDto);
        });
        Page<UserSynchronizationServiceAgentDto> itemDtoPage = new PageImpl<>(content, pageable, itemDoPage.getTotalElements());
        return itemDtoPage;
    }

    @Override
    public Page<UserSynchronizationExecutionInstanceDto> pagingQueryUserSynchronizationExecutionInstances(
            Long userSynchronizationServiceAgentUid,
            Long uid,
            List<JobStatusEnum> jobStatusList,
            List<String> createdTimestampAsStringList,
            List<String> lastModifiedTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // step 1, pre-processing
        //

        //
        // step 2, core-processing
        //
        Specification<UserSynchronizationExecutionInstanceDo> specification = new Specification<UserSynchronizationExecutionInstanceDo>() {
            @Override
            public Predicate toPredicate(Root<UserSynchronizationExecutionInstanceDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (userSynchronizationServiceAgentUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("serviceAgentUid"), userSynchronizationServiceAgentUid));
                }
                if (uid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("uid"), uid));
                }
                if (!CollectionUtils.isEmpty(jobStatusList)) {
                    CriteriaBuilder.In<JobStatusEnum> in = criteriaBuilder.in(root.get("status"));
                    jobStatusList.forEach(item -> {
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
            pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc(
                    BaseDo.ID_FIELD_NAME)));
        } else if (pageable.getSort() == null || pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Order.desc(
                    BaseDo.ID_FIELD_NAME)));
        }

        Page<UserSynchronizationExecutionInstanceDo> itemDoPage =
                this.userSynchronizationExecutionInstanceRepository.findAll(specification, pageable);

        //
        // step 3, post-processing
        //

        //
        // step 3.2, 构造返回内容
        //
        List<UserSynchronizationExecutionInstanceDto> content = new ArrayList<>(itemDoPage.getSize());
        itemDoPage.forEach(itemDo -> {
            UserSynchronizationExecutionInstanceDto itemDto = new UserSynchronizationExecutionInstanceDto();
            BeanUtils.copyProperties(itemDo, itemDto);

            switch (itemDo.getStatus()) {
                case CANCELED:
                case FINISHED:
                case FAILED:
                    itemDto.setTotalDuration(AbcDateUtils.format(Duration.between(itemDo.getCreatedTimestamp(),
                            itemDo.getLastModifiedTimestamp()).toMillis()));

                    itemDto.setExecutionDuration(AbcDateUtils.format(Duration.between(itemDo.getBeginTimestamp(),
                            itemDo.getEndTimestamp()).toMillis()));
                    break;
            }

            content.add(itemDto);
        });
        Page<UserSynchronizationExecutionInstanceDto> itemDtoPage = new PageImpl<>(content, pageable, itemDoPage.getTotalElements());
        return itemDtoPage;
    }

    @Override
    public UserSynchronizationExecutionInstanceDto createUserSynchronizationExecutionInstance(
            Long userSynchronizationServiceAgentUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        JSONObject params = new JSONObject();
        params.put("user_synchronization_service_agent_uid", userSynchronizationServiceAgentUid);
        UserSynchronizationExecutionInstanceDo userSynchronizationExecutionInstanceDo =
                this.userSynchronizationHandler.executeUserSynchronization(params);
        if (userSynchronizationExecutionInstanceDo == null) {
            return null;
        } else {
            UserSynchronizationExecutionInstanceDto userSynchronizationExecutionInstanceDto =
                    new UserSynchronizationExecutionInstanceDto();
            BeanUtils.copyProperties(userSynchronizationExecutionInstanceDo, userSynchronizationExecutionInstanceDto);
            return userSynchronizationExecutionInstanceDto;
        }
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
    public void handleUserSynchronizationServiceComponentDeletedEvent(UserSynchronizationServiceComponentDeletedEvent event) {
        LOGGER.info("rcv event:{}", event);

        List<UserSynchronizationServiceAgentDo> userSynchronizationServiceAgentDoList =
                this.userSynchronizationServiceAgentRepository.findByServiceComponentUid(
                        event.getUserSynchronizationServiceComponentDo().getUid());
        if (!CollectionUtils.isEmpty(userSynchronizationServiceAgentDoList)) {
            userSynchronizationServiceAgentDoList.forEach(userSynchronizationServiceAgentDo -> {
                deleteUserSynchronizationServiceAgent(userSynchronizationServiceAgentDo,
                        event.getOperatingUserProfile());
            });
        }
    }

    @ResourceReferenceHandler(name = "user synchronization service agent")
    public List<String> checkResourceReference(
            ResourceReferenceManager.ResourceCategoryEnum resourceCategory,
            Long resourceUid,
            String resourceName) throws Exception {
        switch (resourceCategory) {
            case USER_SYNCHRONIZATION_SERVICE_COMPONENT: {
                Long serviceComponentUid = resourceUid;

                List<UserSynchronizationServiceAgentDo> serviceAgentDoList =
                        this.userSynchronizationServiceAgentRepository.findByServiceComponentUid(serviceComponentUid);
                if (!CollectionUtils.isEmpty(serviceAgentDoList)) {
                    List<String> result = new LinkedList<>();
                    serviceAgentDoList.forEach(serviceAgentDo -> {
                        result.add(String.format(
                                "[%s] %s (%d)",
                                UserSynchronizationServiceAgentDo.RESOURCE_SYMBOL,
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
