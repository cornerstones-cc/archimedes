package cc.cornerstones.biz.supplement.service.assembly;

import cc.cornerstones.almond.constants.InfrastructureConstants;
import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcDateUtils;
import cc.cornerstones.biz.administration.usermanagement.dto.UserDto;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.administration.usermanagement.share.types.ExtendedProperty;
import cc.cornerstones.biz.administration.usermanagement.share.types.Role;
import cc.cornerstones.biz.authentication.entity.UserAuthenticationInstanceDo;
import cc.cornerstones.biz.authentication.persistence.OpenApiAuthenticationInstanceRepository;
import cc.cornerstones.biz.authentication.persistence.UserAuthenticationInstanceRepository;
import cc.cornerstones.biz.datafacet.dto.DataFacetDto;
import cc.cornerstones.biz.datafacet.service.inf.DataFacetService;
import cc.cornerstones.biz.distributedjob.dto.CreateDistributedJobDto;
import cc.cornerstones.biz.distributedjob.dto.DistributedJobDto;
import cc.cornerstones.biz.distributedjob.dto.UpdateDistributedJobDto;
import cc.cornerstones.biz.distributedjob.service.inf.DistributedJobService;
import cc.cornerstones.biz.distributedjob.share.constants.JobExecutorRoutingAlgorithmEnum;
import cc.cornerstones.biz.distributedjob.share.types.JobHandler;
import cc.cornerstones.biz.export.entity.ExportTaskDo;
import cc.cornerstones.biz.export.persistence.ExportTaskRepository;
import cc.cornerstones.biz.operations.accesslogging.entity.QueryLogDo;
import cc.cornerstones.biz.operations.accesslogging.persistence.QueryLogRepository;
import cc.cornerstones.biz.supplement.entity.SupplementDo;
import cc.cornerstones.biz.supplement.entity.SupplementFaDo;
import cc.cornerstones.biz.supplement.persistence.SupplementFaRepository;
import cc.cornerstones.biz.supplement.persistence.SupplementRepository;
import cc.cornerstones.biz.supplement.service.impl.SupplementServiceImpl;
import cc.cornerstones.biz.supplement.share.types.Supplement;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class SupplementFa implements Supplement {
    private static final Logger LOGGER = LoggerFactory.getLogger(SupplementServiceImpl.class);

    public static final String SUPPLEMENT_NAME = "supplement_fa";

    public static final String JOB_HANDLER = "supplement_fa";

    public static final String JOB_CRON_EXPRESSION = "0 0/15 * * * ?";

    @Autowired
    private SupplementRepository supplementRepository;

    @Autowired
    private SupplementFaRepository supplementFaRepository;

    @Autowired
    private UserAuthenticationInstanceRepository userAuthenticationInstanceRepository;

    @Autowired
    private OpenApiAuthenticationInstanceRepository openApiAuthenticationInstanceRepository;

    @Autowired
    private QueryLogRepository queryLogRepository;

    @Autowired
    private ExportTaskRepository exportTaskRepository;

    @Autowired
    private DistributedJobService distributedJobService;

    @Autowired
    private UserService userService;

    @Autowired
    private DataFacetService dataFacetService;

    private Map<Long, UserDto> userCache = new HashMap<>();

    private Map<Long, String> dataFacetCache = new HashMap<>();

    @Override
    public String name() {
        return SUPPLEMENT_NAME;
    }

    @Override
    public void validate(JSONObject configuration) throws Exception {

    }

    @Override
    public void onEnabled(SupplementDo supplementDo, UserProfile operatingUserProfile) throws Exception {
        //
        // Step 1, pre-processing
        //
        JSONObject configuration = supplementDo.getConfiguration();
        Configuration objectiveConfiguration = null;
        boolean requiredCreateJob = false;

        if (configuration == null) {
            //
            objectiveConfiguration = new Configuration();
            requiredCreateJob = true;
        } else {
            objectiveConfiguration = JSONObject.toJavaObject(configuration, Configuration.class);
            if (objectiveConfiguration.getJobUid() == null) {
                requiredCreateJob = true;
            }
        }

        //
        // Step 2, core-processing
        //
        if (requiredCreateJob) {
            CreateDistributedJobDto createDistributedJobDto = new CreateDistributedJobDto();
            createDistributedJobDto.setName(supplementDo.getName());
            createDistributedJobDto.setDescription(null);
            createDistributedJobDto.setCronExpression(JOB_CRON_EXPRESSION);
            createDistributedJobDto.setEnabled(Boolean.TRUE);
            createDistributedJobDto.setFailedRetires(0);
            createDistributedJobDto.setHandlerName(JOB_HANDLER);
            createDistributedJobDto.setParameters(null);
            createDistributedJobDto.setRoutingAlgorithm(JobExecutorRoutingAlgorithmEnum.ROUND_ROBIN);
            createDistributedJobDto.setTimeoutDurationInSecs(3600L);
            DistributedJobDto distributedJobDto =
                    this.distributedJobService.createJob(createDistributedJobDto, operatingUserProfile);

            //
            objectiveConfiguration.setJobUid(distributedJobDto.getUid());
            supplementDo.setConfiguration((JSONObject) JSONObject.toJSON(objectiveConfiguration));
            BaseDo.update(supplementDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.supplementRepository.save(supplementDo);
        }
    }

    @Override
    public void onDisabled(SupplementDo supplementDo, UserProfile operatingUserProfile) throws Exception {
        //
        // Step 1, pre-processing
        //
        JSONObject configuration = supplementDo.getConfiguration();
        Configuration objectiveConfiguration = null;
        boolean requiredUpdateJob = false;

        if (configuration != null) {
            objectiveConfiguration = JSONObject.toJavaObject(configuration, Configuration.class);
            if (objectiveConfiguration.getJobUid() != null) {
                requiredUpdateJob = true;
            }
        }

        //
        // Step 2, core-processing
        //
        if (requiredUpdateJob) {
            UpdateDistributedJobDto updateDistributedJobDto = new UpdateDistributedJobDto();
            updateDistributedJobDto.setEnabled(Boolean.FALSE);
            this.distributedJobService.updateJob(
                    objectiveConfiguration.getJobUid(),
                    updateDistributedJobDto,
                    operatingUserProfile);
        }
    }

    @JobHandler(name = JOB_HANDLER)
    public void execute(JSONObject params) throws AbcUndefinedException {
        // at the beginning, clear
        userCache.clear();
        dataFacetCache.clear();
        try {
            coreExecute(params);
        } finally {
            // at the end, clear again
            userCache.clear();
            dataFacetCache.clear();
        }
    }

    private void coreExecute(JSONObject params) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        SupplementDo supplementDo = this.supplementRepository.findByName(SUPPLEMENT_NAME);
        if (supplementDo == null) {
            throw new AbcResourceConflictException("ignore job execution, cannot find supplement fa");
        }

        if (!Boolean.TRUE.equals(supplementDo.getEnabled())) {
            throw new AbcResourceConflictException("ignore job execution, supplement fa is disabled");
        }

        JSONObject configuration = supplementDo.getConfiguration();
        if (configuration == null) {
            throw new AbcResourceConflictException("ignore job execution, configuration is null, supplement fa");
        }
        Configuration objectiveConfiguration = JSONObject.toJavaObject(configuration, Configuration.class);
        if (objectiveConfiguration.getJobUid() == null) {
            throw new AbcResourceConflictException("ignore job execution, no job uid in the configuration, supplement" +
                    " fa");
        }

        Context objectiveContext = null;
        JSONObject context = supplementDo.getContext();
        if (context == null) {
            objectiveContext = new Context();
        } else {
            objectiveContext = JSONObject.toJavaObject(context, Context.class);
        }

        //
        // Step 2, core-processing
        //
        UserProfile operatingUserProfile = new UserProfile();
        operatingUserProfile.setUid(InfrastructureConstants.ROOT_USER_UID);
        operatingUserProfile.setDisplayName(InfrastructureConstants.ROOT_USER_DISPLAY_NAME);

        LocalDateTime exclusiveEndDateTime = LocalDateTime.now();

        // Step 2.1, user login
        {
            if (objectiveContext.getUserLoginWatermark() == null) {
                collectUserLoginLog(
                        objectiveConfiguration.getExtendedPropertyUidAsUserCode(),
                        objectiveConfiguration.getExtendedPropertyUidAsTester(),
                        null,
                        exclusiveEndDateTime,
                        operatingUserProfile);
            } else {
                collectUserLoginLog(
                        objectiveConfiguration.getExtendedPropertyUidAsUserCode(),
                        objectiveConfiguration.getExtendedPropertyUidAsTester(),
                        objectiveContext.getUserLoginWatermark(),
                        exclusiveEndDateTime,
                        operatingUserProfile);
            }
            objectiveContext.setUserLoginWatermark(exclusiveEndDateTime);
        }

        // Step 2.2, query
        {
            if (objectiveContext.getQueryWatermark() == null) {
                collectQueryLog(
                        objectiveConfiguration.getExtendedPropertyUidAsUserCode(),
                        objectiveConfiguration.getExtendedPropertyUidAsTester(),
                        objectiveConfiguration.getReservedSymbolList(),
                        objectiveConfiguration.getExcludedDataFacetUidList(),
                        null,
                        exclusiveEndDateTime,
                        operatingUserProfile);
            } else {
                collectQueryLog(
                        objectiveConfiguration.getExtendedPropertyUidAsUserCode(),
                        objectiveConfiguration.getExtendedPropertyUidAsTester(),
                        objectiveConfiguration.getReservedSymbolList(),
                        objectiveConfiguration.getExcludedDataFacetUidList(),
                        objectiveContext.getQueryWatermark(),
                        exclusiveEndDateTime,
                        operatingUserProfile);
            }
            objectiveContext.setQueryWatermark(exclusiveEndDateTime);
        }

        // Step 2.3, export
        {
            if (objectiveContext.getExportWatermark() == null) {
                collectExportLog(
                        objectiveConfiguration.getExtendedPropertyUidAsUserCode(),
                        objectiveConfiguration.getExtendedPropertyUidAsTester(),
                        objectiveConfiguration.getReservedSymbolList(),
                        objectiveConfiguration.getExcludedDataFacetUidList(),
                        null,
                        exclusiveEndDateTime,
                        operatingUserProfile);
            } else {
                collectExportLog(
                        objectiveConfiguration.getExtendedPropertyUidAsUserCode(),
                        objectiveConfiguration.getExtendedPropertyUidAsTester(),
                        objectiveConfiguration.getReservedSymbolList(),
                        objectiveConfiguration.getExcludedDataFacetUidList(),
                        objectiveContext.getExportWatermark(),
                        exclusiveEndDateTime,
                        operatingUserProfile);
            }
            objectiveContext.setExportWatermark(exclusiveEndDateTime);
        }

        //
        // Step 3, post-processing
        //
        context = (JSONObject) JSONObject.toJSON(objectiveContext);
        supplementDo.setContext(context);
        BaseDo.update(supplementDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.supplementRepository.save(supplementDo);
    }

    private void collectUserLoginLog(
            Long extendedPropertyUidAsUserCode,
            Long extendedPropertyUidAsTester,
            LocalDateTime inclusiveBeginDataTime,
            LocalDateTime exclusiveEndDateTime,
            UserProfile operatingUserProfile) {

        Specification<UserAuthenticationInstanceDo> specification = new Specification<UserAuthenticationInstanceDo>() {
            @Override
            public Predicate toPredicate(Root<UserAuthenticationInstanceDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();

                if (inclusiveBeginDataTime != null) {
                    predicateList.add(criteriaBuilder.greaterThanOrEqualTo(
                            root.get(BaseDo.CREATED_TIMESTAMP_FIELD_NAME), inclusiveBeginDataTime));
                }
                predicateList.add(criteriaBuilder.lessThan(
                        root.get(BaseDo.CREATED_TIMESTAMP_FIELD_NAME), exclusiveEndDateTime));

                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        long beginTime = System.currentTimeMillis();
        int pageNumber = 0;
        int pageSize = 2000;
        Page<UserAuthenticationInstanceDo> itemDoPage = null;
        do {
            PageRequest pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Order.asc(BaseDo.ID_FIELD_NAME)));
            itemDoPage = this.userAuthenticationInstanceRepository.findAll(specification, pageable);

            LOGGER.info("[{}] collect user login logs, subtotal:{}, duration:{}",
                    SUPPLEMENT_NAME,
                    itemDoPage.getNumberOfElements(),
                    AbcDateUtils.format(System.currentTimeMillis() - beginTime));

            // transform
            if (!itemDoPage.isEmpty()) {
                List<SupplementFaDo> supplementFaDoList = new LinkedList<>();
                itemDoPage.forEach(itemDo -> {
                    Long userUid = itemDo.getUserUid();
                    String userDisplayName = itemDo.getUserDisplayName();
                    LocalDateTime createdTimestamp = itemDo.getCreatedTimestamp();

                    SupplementFaDo supplementFaDo = new SupplementFaDo();
                    supplementFaDo.setOperationName("登录");
                    supplementFaDo.setOperationCreatedTimestamp(createdTimestamp);
                    supplementFaDo.setOperationCreatedDate(createdTimestamp.toLocalDate());

                    // user uid
                    supplementFaDo.setUserUid(userUid);

                    // user code

                    // 最新的用户信息
                    UserDto userDto = null;

                    if (extendedPropertyUidAsUserCode != null) {
                        List<ExtendedProperty> extendedPropertyList = null;

                        if (itemDo.getUserOutline() != null) {
                            extendedPropertyList = itemDo.getUserOutline().getExtendedPropertyList();
                        } else {
                            // for 历史数据，取最新的补充信息
                            if (userDto == null) {
                                if (this.userCache.containsKey(userUid)) {
                                    userDto = this.userCache.get(userUid);
                                } else {
                                    try {
                                        userDto = this.userService.getUser(userUid);
                                        if (userDto != null) {
                                            this.userCache.put(userUid, userDto);
                                        }
                                    } catch (Exception e) {
                                        LOGGER.warn("cannot find user {}", userUid);
                                    }
                                }
                            }
                            if (userDto != null) {
                                extendedPropertyList = userDto.getExtendedPropertyList();
                            }
                        }

                        if (!CollectionUtils.isEmpty(extendedPropertyList)) {
                            for (ExtendedProperty extendedProperty : extendedPropertyList) {
                                if (extendedProperty.getExtendedPropertyUid().equals(extendedPropertyUidAsUserCode)) {
                                    if (extendedProperty.getExtendedPropertyValue() instanceof String) {
                                        supplementFaDo.setUserCode((String) extendedProperty.getExtendedPropertyValue());
                                    } else {
                                        supplementFaDo.setUserCode(String.valueOf(extendedProperty.getExtendedPropertyValue()));
                                    }

                                    break;
                                }
                            }
                        }
                    }

                    // user name
                    supplementFaDo.setUserName(userDisplayName);

                    // user role(s)
                    List<Role> roleList = null;
                    if (itemDo.getUserOutline() != null) {
                        roleList = itemDo.getUserOutline().getRoleList();
                    } else {
                        // for 历史数据，取最新的补充信息
                        if (userDto == null) {
                            if (this.userCache.containsKey(userUid)) {
                                userDto = this.userCache.get(userUid);
                            } else {
                                try {
                                    userDto = this.userService.getUser(userUid);
                                    if (userDto != null) {
                                        this.userCache.put(userUid, userDto);
                                    }
                                } catch (Exception e) {
                                    LOGGER.warn("cannot find user {}", userUid);
                                }
                            }
                        }
                        if (userDto != null) {
                            roleList = userDto.getRoleList();
                        }
                    }

                    if (!CollectionUtils.isEmpty(roleList)) {
                        List<String> roleUidList = new LinkedList<>();
                        List<String> roleNameList = new LinkedList<>();
                        for (Role role : roleList) {
                            roleUidList.add(String.valueOf(role.getUid()));
                            roleNameList.add(role.getName());
                        }
                        supplementFaDo.setUserRoleUidList(String.join(",", roleUidList));
                        supplementFaDo.setUserRoleNameList(String.join(",", roleNameList));
                    }

                    // tester
                    if (extendedPropertyUidAsTester != null) {
                        List<ExtendedProperty> extendedPropertyList = null;

                        if (itemDo.getUserOutline() != null) {
                            extendedPropertyList = itemDo.getUserOutline().getExtendedPropertyList();
                        } else {
                            // for 历史数据，取最新的补充信息
                            if (userDto == null) {
                                if (this.userCache.containsKey(userUid)) {
                                    userDto = this.userCache.get(userUid);
                                } else {
                                    try {
                                        userDto = this.userService.getUser(userUid);
                                        if (userDto != null) {
                                            this.userCache.put(userUid, userDto);
                                        }
                                    } catch (Exception e) {
                                        LOGGER.warn("cannot find user {}", userUid);
                                    }
                                }
                            }
                            if (userDto != null) {
                                extendedPropertyList = userDto.getExtendedPropertyList();
                            }
                        }

                        if (!CollectionUtils.isEmpty(extendedPropertyList)) {
                            for (ExtendedProperty extendedProperty : extendedPropertyList) {
                                if (extendedProperty.getExtendedPropertyUid().equals(extendedPropertyUidAsTester)) {
                                    if (extendedProperty.getExtendedPropertyValue() != null) {
                                        if (extendedProperty.getExtendedPropertyValue() instanceof Boolean) {
                                            supplementFaDo.setTester((Boolean) extendedProperty.getExtendedPropertyValue());
                                        } else if (extendedProperty.getExtendedPropertyValue() instanceof String) {
                                            String extendedPropertyValueAsString =
                                                    (String) extendedProperty.getExtendedPropertyValue();
                                            if (extendedPropertyValueAsString.equalsIgnoreCase("1")
                                                    || extendedPropertyValueAsString.equalsIgnoreCase("true")) {
                                                supplementFaDo.setTester(Boolean.TRUE);
                                            } else if (extendedPropertyValueAsString.equalsIgnoreCase("0")
                                                    || extendedPropertyValueAsString.equalsIgnoreCase("false")) {
                                                supplementFaDo.setTester(Boolean.FALSE);
                                            }
                                        } else {
                                            String extendedPropertyValueAsString =
                                                    String.valueOf(extendedProperty.getExtendedPropertyValue());
                                            if (extendedPropertyValueAsString.equalsIgnoreCase("1")
                                                    || extendedPropertyValueAsString.equalsIgnoreCase("true")) {
                                                supplementFaDo.setTester(Boolean.TRUE);
                                            } else if (extendedPropertyValueAsString.equalsIgnoreCase("0")
                                                    || extendedPropertyValueAsString.equalsIgnoreCase("false")) {
                                                supplementFaDo.setTester(Boolean.FALSE);
                                            }
                                        }
                                    }

                                    break;
                                }
                            }
                        }
                    }

                    BaseDo.create(supplementFaDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    supplementFaDoList.add(supplementFaDo);
                });

                this.supplementFaRepository.saveAll(supplementFaDoList);
            }

            beginTime = System.currentTimeMillis();
            pageNumber++;
        } while (itemDoPage != null && !itemDoPage.isEmpty());
    }

    private void collectQueryLog(
            Long extendedPropertyUidAsUserCode,
            Long extendedPropertyUidAsTester,
            List<String> reservedSymbolList,
            List<Long> excludedDataFacetUidList,
            LocalDateTime inclusiveBeginDataTime,
            LocalDateTime exclusiveEndDateTime,
            UserProfile operatingUserProfile) {
        Specification<QueryLogDo> specification = new Specification<QueryLogDo>() {
            @Override
            public Predicate toPredicate(Root<QueryLogDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();

                if (inclusiveBeginDataTime != null) {
                    predicateList.add(criteriaBuilder.greaterThanOrEqualTo(
                            root.get(BaseDo.CREATED_TIMESTAMP_FIELD_NAME), inclusiveBeginDataTime));
                }
                predicateList.add(criteriaBuilder.lessThan(
                        root.get(BaseDo.CREATED_TIMESTAMP_FIELD_NAME), exclusiveEndDateTime));

                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        long beginTime = System.currentTimeMillis();
        int pageNumber = 0;
        int pageSize = 2000;
        Page<QueryLogDo> itemDoPage = null;
        do {
            PageRequest pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Order.asc(BaseDo.ID_FIELD_NAME)));
            itemDoPage = this.queryLogRepository.findAll(specification, pageable);

            LOGGER.info("[{}] collect query logs, subtotal:{}, duration:{}",
                    SUPPLEMENT_NAME,
                    itemDoPage.getNumberOfElements(),
                    AbcDateUtils.format(System.currentTimeMillis() - beginTime));

            // transform
            if (!itemDoPage.isEmpty()) {
                List<SupplementFaDo> supplementFaDoList = new LinkedList<>();
                itemDoPage.forEach(itemDo -> {
                    if (itemDo.getDataFacetUid() == null) {
                        // ignore
                        return;
                    }
                    if (!CollectionUtils.isEmpty(excludedDataFacetUidList)
                            && excludedDataFacetUidList.contains(itemDo.getDataFacetUid())) {
                        // ignore
                        return;
                    }

                    Long userUid = itemDo.getUserUid();
                    String userDisplayName = itemDo.getDisplayName();
                    LocalDateTime createdTimestamp = itemDo.getCreatedTimestamp();

                    SupplementFaDo supplementFaDo = new SupplementFaDo();
                    supplementFaDo.setTrackingSerialNumber(itemDo.getTrackingSerialNumber());
                    supplementFaDo.setRemark(itemDo.getRemark());
                    supplementFaDo.setOperationName("查询");
                    supplementFaDo.setOperationCreatedTimestamp(createdTimestamp);
                    if (createdTimestamp != null) {
                        supplementFaDo.setOperationCreatedDate(createdTimestamp.toLocalDate());
                    }
                    if (itemDo.getTotalDurationInMillis() != null) {
                        supplementFaDo.setOperationDurationInSeconds(Double.valueOf(itemDo.getTotalDurationInMillis() * 1.0 / 1000).floatValue());
                    }
                    supplementFaDo.setOperationObjectUid(itemDo.getDataFacetUid());
                    supplementFaDo.setOperationObjectCode(String.valueOf(itemDo.getDataFacetUid()));

                    if (ObjectUtils.isEmpty(itemDo.getDataFacetName())) {
                        // for 历史数据，取最新的补充信息
                        if (dataFacetCache.containsKey(itemDo.getDataFacetUid())) {
                            supplementFaDo.setOperationObjectName(dataFacetCache.get(itemDo.getDataFacetUid()));
                        } else {
                            try {
                                DataFacetDto dataFacetDto = this.dataFacetService.getDataFacet(itemDo.getDataFacetUid(),
                                        operatingUserProfile);
                                if (dataFacetDto != null) {
                                    dataFacetCache.put(itemDo.getDataFacetUid(), dataFacetDto.getName());

                                    supplementFaDo.setOperationObjectName(dataFacetDto.getName());
                                }
                            } catch (Exception e) {
                                LOGGER.warn("cannot find data facet {}", itemDo.getDataFacetUid());
                            }
                        }
                    } else {
                        supplementFaDo.setOperationObjectName(itemDo.getDataFacetName());
                    }

                    if (Boolean.TRUE.equals(itemDo.getSuccessful())) {
                        supplementFaDo.setOperationStatus("FINISHED");
                        if (itemDo.getTotalRowsInSource() == null) {
                            supplementFaDo.setNumberOfOperationResultRecords(0L);
                        } else {
                            supplementFaDo.setNumberOfOperationResultRecords(itemDo.getTotalRowsInSource());
                        }
                    } else if (Boolean.FALSE.equals(itemDo.getSuccessful())) {
                        supplementFaDo.setOperationStatus("FAILED");
                    } else {
                        supplementFaDo.setOperationStatus("IN PROGRESS");
                    }

                    // user uid
                    supplementFaDo.setUserUid(userUid);

                    // user code

                    // 最新的用户信息
                    UserDto userDto = null;

                    if (extendedPropertyUidAsUserCode != null) {
                        List<ExtendedProperty> extendedPropertyList = null;

                        if (itemDo.getUserOutline() != null) {
                            extendedPropertyList = itemDo.getUserOutline().getExtendedPropertyList();
                        } else {
                            // for 历史数据，取最新的补充信息
                            if (userDto == null) {
                                if (this.userCache.containsKey(userUid)) {
                                    userDto = this.userCache.get(userUid);
                                } else {
                                    try {
                                        userDto = this.userService.getUser(userUid);
                                        if (userDto != null) {
                                            this.userCache.put(userUid, userDto);
                                        }
                                    } catch (Exception e) {
                                        LOGGER.warn("cannot find user {}", userUid);
                                    }
                                }
                            }
                            if (userDto != null) {
                                extendedPropertyList = userDto.getExtendedPropertyList();
                            }
                        }

                        if (!CollectionUtils.isEmpty(extendedPropertyList)) {
                            for (ExtendedProperty extendedProperty : extendedPropertyList) {
                                if (extendedProperty.getExtendedPropertyUid().equals(extendedPropertyUidAsUserCode)) {
                                    if (extendedProperty.getExtendedPropertyValue() instanceof String) {
                                        supplementFaDo.setUserCode((String) extendedProperty.getExtendedPropertyValue());
                                    } else {
                                        supplementFaDo.setUserCode(String.valueOf(extendedProperty.getExtendedPropertyValue()));
                                    }

                                    break;
                                }
                            }
                        }
                    }

                    // user name
                    supplementFaDo.setUserName(userDisplayName);

                    // user role(s)
                    List<Role> roleList = null;
                    if (itemDo.getUserOutline() != null) {
                        roleList = itemDo.getUserOutline().getRoleList();
                    } else {
                        // for 历史数据，取最新的补充信息
                        if (userDto == null) {
                            if (this.userCache.containsKey(userUid)) {
                                userDto = this.userCache.get(userUid);
                            } else {
                                try {
                                    userDto = this.userService.getUser(userUid);
                                    if (userDto != null) {
                                        this.userCache.put(userUid, userDto);
                                    }
                                } catch (Exception e) {
                                    LOGGER.warn("cannot find user {}", userUid);
                                }
                            }
                        }
                        if (userDto != null) {
                            roleList = userDto.getRoleList();
                        }
                    }

                    if (!CollectionUtils.isEmpty(roleList)) {
                        List<String> roleUidList = new LinkedList<>();
                        List<String> roleNameList = new LinkedList<>();
                        for (Role role : roleList) {
                            roleUidList.add(String.valueOf(role.getUid()));
                            roleNameList.add(role.getName());
                        }
                        supplementFaDo.setUserRoleUidList(String.join(",", roleUidList));
                        supplementFaDo.setUserRoleNameList(String.join(",", roleNameList));
                    }

                    // tester
                    if (extendedPropertyUidAsTester != null) {
                        List<ExtendedProperty> extendedPropertyList = null;

                        if (itemDo.getUserOutline() != null) {
                            extendedPropertyList = itemDo.getUserOutline().getExtendedPropertyList();
                        } else {
                            // for 历史数据，取最新的补充信息
                            if (userDto == null) {
                                if (this.userCache.containsKey(userUid)) {
                                    userDto = this.userCache.get(userUid);
                                } else {
                                    try {
                                        userDto = this.userService.getUser(userUid);
                                        if (userDto != null) {
                                            this.userCache.put(userUid, userDto);
                                        }
                                    } catch (Exception e) {
                                        LOGGER.warn("cannot find user {}", userUid);
                                    }
                                }
                            }
                            if (userDto != null) {
                                extendedPropertyList = userDto.getExtendedPropertyList();
                            }
                        }

                        if (!CollectionUtils.isEmpty(extendedPropertyList)) {
                            for (ExtendedProperty extendedProperty : extendedPropertyList) {
                                if (extendedProperty.getExtendedPropertyUid().equals(extendedPropertyUidAsTester)) {
                                    if (extendedProperty.getExtendedPropertyValue() != null) {
                                        if (extendedProperty.getExtendedPropertyValue() instanceof Boolean) {
                                            supplementFaDo.setTester((Boolean) extendedProperty.getExtendedPropertyValue());
                                        } else if (extendedProperty.getExtendedPropertyValue() instanceof String) {
                                            String extendedPropertyValueAsString =
                                                    (String) extendedProperty.getExtendedPropertyValue();
                                            if (extendedPropertyValueAsString.equalsIgnoreCase("1")
                                                    || extendedPropertyValueAsString.equalsIgnoreCase("true")) {
                                                supplementFaDo.setTester(Boolean.TRUE);
                                            } else if (extendedPropertyValueAsString.equalsIgnoreCase("0")
                                                    || extendedPropertyValueAsString.equalsIgnoreCase("false")) {
                                                supplementFaDo.setTester(Boolean.FALSE);
                                            }
                                        } else {
                                            String extendedPropertyValueAsString =
                                                    String.valueOf(extendedProperty.getExtendedPropertyValue());
                                            if (extendedPropertyValueAsString.equalsIgnoreCase("1")
                                                    || extendedPropertyValueAsString.equalsIgnoreCase("true")) {
                                                supplementFaDo.setTester(Boolean.TRUE);
                                            } else if (extendedPropertyValueAsString.equalsIgnoreCase("0")
                                                    || extendedPropertyValueAsString.equalsIgnoreCase("false")) {
                                                supplementFaDo.setTester(Boolean.FALSE);
                                            }
                                        }
                                    }

                                    break;
                                }
                            }
                        }
                    }

                    // reserved
                    if (!CollectionUtils.isEmpty(reservedSymbolList)) {
                        Reserved reserved = transformReserved(reservedSymbolList, itemDo.getIntermediateResult());
                        if (reserved != null && !CollectionUtils.isEmpty(reserved.getValues())) {

                            for (int i = 0; i < reservedSymbolList.size(); i++) {
                                String reservedSymbol = reservedSymbolList.get(i);

                                List<String> reservedValues = reserved.getValues().get(reservedSymbol);
                                if (!CollectionUtils.isEmpty(reservedValues)) {
                                    if (i == 0) {
                                        supplementFaDo.setReserved0(String.join(",", reservedValues));
                                    } else if (i == 1) {
                                        supplementFaDo.setReserved1(String.join(",", reservedValues));
                                    } else if (i == 2) {
                                        supplementFaDo.setReserved2(String.join(",", reservedValues));
                                    } else if (i == 3) {
                                        supplementFaDo.setReserved3(String.join(",", reservedValues));
                                    }
                                }
                            }
                        }
                    }

                    BaseDo.create(supplementFaDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    supplementFaDoList.add(supplementFaDo);
                });

                this.supplementFaRepository.saveAll(supplementFaDoList);
            }

            beginTime = System.currentTimeMillis();
            pageNumber++;
        } while (itemDoPage != null && !itemDoPage.isEmpty());
    }

    private void collectExportLog(
            Long extendedPropertyUidAsUserCode,
            Long extendedPropertyUidAsTester,
            List<String> reservedSymbolList,
            List<Long> excludedDataFacetUidList,
            LocalDateTime inclusiveBeginDataTime,
            LocalDateTime exclusiveEndDateTime,
            UserProfile operatingUserProfile) {
        Specification<ExportTaskDo> specification = new Specification<ExportTaskDo>() {
            @Override
            public Predicate toPredicate(Root<ExportTaskDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();

                if (inclusiveBeginDataTime != null) {
                    predicateList.add(criteriaBuilder.greaterThanOrEqualTo(
                            root.get(BaseDo.CREATED_TIMESTAMP_FIELD_NAME), inclusiveBeginDataTime));
                }
                predicateList.add(criteriaBuilder.lessThan(
                        root.get(BaseDo.CREATED_TIMESTAMP_FIELD_NAME), exclusiveEndDateTime));

                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        long beginTime = System.currentTimeMillis();
        int pageNumber = 0;
        int pageSize = 2000;
        Page<ExportTaskDo> itemDoPage = null;
        do {
            PageRequest pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Order.asc(BaseDo.ID_FIELD_NAME)));
            itemDoPage = this.exportTaskRepository.findAll(specification, pageable);

            LOGGER.info("[{}] collect export logs, subtotal:{}, duration:{}",
                    SUPPLEMENT_NAME,
                    itemDoPage.getNumberOfElements(),
                    AbcDateUtils.format(System.currentTimeMillis() - beginTime));

            // transform
            if (!itemDoPage.isEmpty()) {
                List<SupplementFaDo> supplementFaDoList = new LinkedList<>();
                itemDoPage.forEach(itemDo -> {
                    if (itemDo.getDataFacetUid() == null) {
                        // ignore
                        return;
                    }
                    if (!CollectionUtils.isEmpty(excludedDataFacetUidList)
                            && excludedDataFacetUidList.contains(itemDo.getDataFacetUid())) {
                        // ignore
                        return;
                    }

                    Long userUid = itemDo.getCreatedBy();
                    LocalDateTime createdTimestamp = itemDo.getCreatedTimestamp();

                    SupplementFaDo supplementFaDo = new SupplementFaDo();
                    supplementFaDo.setTrackingSerialNumber(String.valueOf(itemDo.getTaskUid()));
                    supplementFaDo.setRemark(itemDo.getRemark());
                    supplementFaDo.setOperationName("导出");
                    supplementFaDo.setOperationCreatedTimestamp(createdTimestamp);
                    if (createdTimestamp != null) {
                        supplementFaDo.setOperationCreatedDate(createdTimestamp.toLocalDate());
                    }
                    if (itemDo.getTotalDurationInSecs() != null) {
                        supplementFaDo.setOperationDurationInSeconds(Double.valueOf(itemDo.getTotalDurationInSecs()).floatValue());
                    }
                    supplementFaDo.setOperationObjectUid(itemDo.getDataFacetUid());
                    supplementFaDo.setOperationObjectCode(String.valueOf(itemDo.getDataFacetUid()));

                    if (ObjectUtils.isEmpty(itemDo.getDataFacetName())) {
                        // for 历史数据，取最新的补充信息
                        if (dataFacetCache.containsKey(itemDo.getDataFacetUid())) {
                            supplementFaDo.setOperationObjectName(dataFacetCache.get(itemDo.getDataFacetUid()));
                        } else {
                            try {
                                DataFacetDto dataFacetDto = this.dataFacetService.getDataFacet(itemDo.getDataFacetUid(),
                                        operatingUserProfile);
                                if (dataFacetDto != null) {
                                    dataFacetCache.put(itemDo.getDataFacetUid(), dataFacetDto.getName());

                                    supplementFaDo.setOperationObjectName(dataFacetDto.getName());
                                }
                            } catch (Exception e) {
                                LOGGER.warn("cannot find data facet {}", itemDo.getDataFacetUid());
                            }
                        }
                    } else {
                        supplementFaDo.setOperationObjectName(itemDo.getDataFacetName());
                    }

                    switch (itemDo.getTaskStatus()) {
                        case FINISHED:
                            supplementFaDo.setOperationStatus("FINISHED");
                            if (itemDo.getTotalRowsInFile() != null) {
                                supplementFaDo.setNumberOfOperationResultRecords(itemDo.getTotalRowsInFile());
                            } else if (itemDo.getTotalRowsInSource() != null){
                                supplementFaDo.setNumberOfOperationResultRecords(itemDo.getTotalRowsInSource());
                            } else {
                                supplementFaDo.setNumberOfOperationResultRecords(0L);
                            }
                            break;
                        case FAILED:
                        case CANCELED:
                            supplementFaDo.setOperationStatus("FAILED");
                            break;
                        default:
                            supplementFaDo.setOperationStatus("IN PROGRESS");
                            break;
                    }

                    // user uid
                    supplementFaDo.setUserUid(userUid);

                    // user code

                    // 最新的用户信息
                    UserDto userDto = null;

                    if (extendedPropertyUidAsUserCode != null) {
                        List<ExtendedProperty> extendedPropertyList = null;

                        if (itemDo.getUserOutline() != null) {
                            extendedPropertyList = itemDo.getUserOutline().getExtendedPropertyList();
                        } else {
                            // for 历史数据，取最新的补充信息
                            if (userDto == null) {
                                if (this.userCache.containsKey(userUid)) {
                                    userDto = this.userCache.get(userUid);
                                } else {
                                    try {
                                        userDto = this.userService.getUser(userUid);
                                        if (userDto != null) {
                                            this.userCache.put(userUid, userDto);
                                        }
                                    } catch (Exception e) {
                                        LOGGER.warn("cannot find user {}", userUid);
                                    }
                                }
                            }
                            if (userDto != null) {
                                extendedPropertyList = userDto.getExtendedPropertyList();
                            }
                        }

                        if (!CollectionUtils.isEmpty(extendedPropertyList)) {
                            for (ExtendedProperty extendedProperty : extendedPropertyList) {
                                if (extendedProperty.getExtendedPropertyUid().equals(extendedPropertyUidAsUserCode)) {
                                    if (extendedProperty.getExtendedPropertyValue() instanceof String) {
                                        supplementFaDo.setUserCode((String) extendedProperty.getExtendedPropertyValue());
                                    } else {
                                        supplementFaDo.setUserCode(String.valueOf(extendedProperty.getExtendedPropertyValue()));
                                    }

                                    break;
                                }
                            }
                        }
                    }

                    // user name
                    if (itemDo.getUserOutline() != null) {
                        supplementFaDo.setUserName(itemDo.getUserOutline().getDisplayName());
                    } else {
                        // for 历史数据，取最新的补充信息
                        if (userDto == null) {
                            if (this.userCache.containsKey(userUid)) {
                                userDto = this.userCache.get(userUid);
                            } else {
                                try {
                                    userDto = this.userService.getUser(userUid);
                                    if (userDto != null) {
                                        this.userCache.put(userUid, userDto);
                                    }
                                } catch (Exception e) {
                                    LOGGER.warn("cannot find user {}", userUid);
                                }
                            }
                        }
                        if (userDto != null) {
                            supplementFaDo.setUserName(userDto.getDisplayName());
                        }
                    }

                    // user role(s)
                    List<Role> roleList = null;
                    if (itemDo.getUserOutline() != null) {
                        roleList = itemDo.getUserOutline().getRoleList();
                    } else {
                        // for 历史数据，取最新的补充信息
                        if (userDto == null) {
                            if (this.userCache.containsKey(userUid)) {
                                userDto = this.userCache.get(userUid);
                            } else {
                                try {
                                    userDto = this.userService.getUser(userUid);
                                    if (userDto != null) {
                                        this.userCache.put(userUid, userDto);
                                    }
                                } catch (Exception e) {
                                    LOGGER.warn("cannot find user {}", userUid);
                                }
                            }
                        }
                        if (userDto != null) {
                            roleList = userDto.getRoleList();
                        }
                    }

                    if (!CollectionUtils.isEmpty(roleList)) {
                        List<String> roleUidList = new LinkedList<>();
                        List<String> roleNameList = new LinkedList<>();
                        for (Role role : roleList) {
                            roleUidList.add(String.valueOf(role.getUid()));
                            roleNameList.add(role.getName());
                        }
                        supplementFaDo.setUserRoleUidList(String.join(",", roleUidList));
                        supplementFaDo.setUserRoleNameList(String.join(",", roleNameList));
                    }

                    // tester
                    if (extendedPropertyUidAsTester != null) {
                        List<ExtendedProperty> extendedPropertyList = null;

                        if (itemDo.getUserOutline() != null) {
                            extendedPropertyList = itemDo.getUserOutline().getExtendedPropertyList();
                        } else {
                            // for 历史数据，取最新的补充信息
                            if (userDto == null) {
                                if (this.userCache.containsKey(userUid)) {
                                    userDto = this.userCache.get(userUid);
                                } else {
                                    try {
                                        userDto = this.userService.getUser(userUid);
                                        if (userDto != null) {
                                            this.userCache.put(userUid, userDto);
                                        }
                                    } catch (Exception e) {
                                        LOGGER.warn("cannot find user {}", userUid);
                                    }
                                }
                            }
                            if (userDto != null) {
                                extendedPropertyList = userDto.getExtendedPropertyList();
                            }
                        }

                        if (!CollectionUtils.isEmpty(extendedPropertyList)) {
                            for (ExtendedProperty extendedProperty : extendedPropertyList) {
                                if (extendedProperty.getExtendedPropertyUid().equals(extendedPropertyUidAsTester)) {
                                    if (extendedProperty.getExtendedPropertyValue() != null) {
                                        if (extendedProperty.getExtendedPropertyValue() instanceof Boolean) {
                                            supplementFaDo.setTester((Boolean) extendedProperty.getExtendedPropertyValue());
                                        } else if (extendedProperty.getExtendedPropertyValue() instanceof String) {
                                            String extendedPropertyValueAsString =
                                                    (String) extendedProperty.getExtendedPropertyValue();
                                            if (extendedPropertyValueAsString.equalsIgnoreCase("1")
                                                    || extendedPropertyValueAsString.equalsIgnoreCase("true")) {
                                                supplementFaDo.setTester(Boolean.TRUE);
                                            } else if (extendedPropertyValueAsString.equalsIgnoreCase("0")
                                                    || extendedPropertyValueAsString.equalsIgnoreCase("false")) {
                                                supplementFaDo.setTester(Boolean.FALSE);
                                            }
                                        } else {
                                            String extendedPropertyValueAsString =
                                                    String.valueOf(extendedProperty.getExtendedPropertyValue());
                                            if (extendedPropertyValueAsString.equalsIgnoreCase("1")
                                                    || extendedPropertyValueAsString.equalsIgnoreCase("true")) {
                                                supplementFaDo.setTester(Boolean.TRUE);
                                            } else if (extendedPropertyValueAsString.equalsIgnoreCase("0")
                                                    || extendedPropertyValueAsString.equalsIgnoreCase("false")) {
                                                supplementFaDo.setTester(Boolean.FALSE);
                                            }
                                        }
                                    }

                                    break;
                                }
                            }
                        }
                    }

                    // reserved
                    if (!CollectionUtils.isEmpty(reservedSymbolList)) {
                        Reserved reserved = transformReserved(reservedSymbolList, itemDo.getIntermediateResult());
                        if (reserved != null && !CollectionUtils.isEmpty(reserved.getValues())) {

                            for (int i = 0; i < reservedSymbolList.size(); i++) {
                                String reservedSymbol = reservedSymbolList.get(i);

                                List<String> reservedValues = reserved.getValues().get(reservedSymbol);
                                if (!CollectionUtils.isEmpty(reservedValues)) {
                                    if (i == 0) {
                                        supplementFaDo.setReserved0(String.join(",", reservedValues));
                                    } else if (i == 1) {
                                        supplementFaDo.setReserved1(String.join(",", reservedValues));
                                    } else if (i == 2) {
                                        supplementFaDo.setReserved2(String.join(",", reservedValues));
                                    } else if (i == 3) {
                                        supplementFaDo.setReserved3(String.join(",", reservedValues));
                                    }
                                }
                            }
                        }
                    }

                    BaseDo.create(supplementFaDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    supplementFaDoList.add(supplementFaDo);
                });

                this.supplementFaRepository.saveAll(supplementFaDoList);
            }

            beginTime = System.currentTimeMillis();
            pageNumber++;
        } while (itemDoPage != null && !itemDoPage.isEmpty());
    }

    /**
     * @param reservedSymbolList
     * @param input
     * @return
     */
    private Reserved transformReserved(List<String> reservedSymbolList, JSONObject input) {
        /**
         * {
         * "data": [
         * {
         * "content": [
         * [...], 3 items
         * [
         * {
         * "f":
         * "mars_region_name",
         *
         * "s":
         * "业务南区",
         *
         * "t":
         * "STRING"
         *
         * },
         *
         * {
         * "f":
         * "mars_province_name",
         *
         * "s":
         * "粤桂",
         *
         * "t":
         * "STRING"
         *
         * },
         *
         * {
         * "f":
         * "mars_city_cluster_name",
         *
         * "s":
         * "粤桂办公室",
         *
         * "t":
         * "STRING"
         *
         * }
         *
         * ],
         *
         * [...], 3 items
         * [...], 3 items
         * [...], 3 items
         * [...], 3 items
         * [...], 3 items
         * [...] 3 items
         * ]
         *
         * }
         *
         * ]
         *
         * }
         */

        if (input == null) {
            return null;
        }
        if (CollectionUtils.isEmpty(reservedSymbolList)) {
            return null;
        }

        // 最外层，是一个 data 列表
        ReservedLevelFour reservedLevelFour = JSONObject.toJavaObject(input, ReservedLevelFour.class);
        if (CollectionUtils.isEmpty(reservedLevelFour.getData())) {
            return null;
        }

        Reserved reserved = new Reserved();
        reserved.setValues(new HashMap<>());

        // data 列表 item 是一个对象，对象只有一个属性 context 列表
        for (ReservedLevelThree reservedLevelThree : reservedLevelFour.getData()) {
            if (CollectionUtils.isEmpty(reservedLevelThree.getContent())) {
                continue;
            }

            // context 列表 item 又是一个列表，将其命名为 kk
            for (List<ReservedLevelOne> reservedLevelTwo : reservedLevelThree.getContent()) {
                if (CollectionUtils.isEmpty(reservedLevelTwo)) {
                    continue;
                }

                if (reservedSymbolList.size() < reservedLevelTwo.size()) {
                    continue;
                }

                // kk 列表 item 是一个对象，包含 f, s, t 三个属性
                // 继续根据 f 属性的取值判断是否期望的 reserved 信息
                boolean matched = true;
                for (int i = 0; i < reservedLevelTwo.size(); i++) {
                    ReservedLevelOne reservedLevelOne = reservedLevelTwo.get(i);

                    if (!reservedSymbolList.get(i).equalsIgnoreCase(reservedLevelOne.f)) {
                        matched = false;
                        break;
                    }
                }

                if (!matched) {
                    continue;
                }

                for (int i = 0; i < reservedLevelTwo.size(); i++) {
                    ReservedLevelOne reservedLevelOne = reservedLevelTwo.get(i);

                    if (!reserved.getValues().containsKey(reservedLevelOne.f)) {
                        reserved.getValues().put(reservedLevelOne.f, new LinkedList<>());
                        reserved.getValues().get(reservedLevelOne.f).add(reservedLevelOne.s);
                    } else {
                        if (!reserved.getValues().get(reservedLevelOne.f).contains(reservedLevelOne.s)) {
                            reserved.getValues().get(reservedLevelOne.f).add(reservedLevelOne.s);
                        }
                    }
                }
            }
        }

        return reserved;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    public static class Configuration {
        /**
         * extended property uid as user code
         */
        private Long extendedPropertyUidAsUserCode;

        /**
         * reserved symbol list
         */
        private List<String> reservedSymbolList;

        /**
         * extended property uid as tester
         */
        private Long extendedPropertyUidAsTester;

        /**
         * d-job uid
         */
        private Long jobUid;

        /**
         * excluded data facet uid list
         */
        private List<Long> excludedDataFacetUidList;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    public static class Context {
        private LocalDateTime userLoginWatermark;
        private LocalDateTime openApiLoginWatermark;
        private LocalDateTime queryWatermark;
        private LocalDateTime exportWatermark;
    }

    @Data
    public static class ReservedLevelOne {
        private String f;
        private String s;
    }

    @Data
    public static class ReservedLevelThree {
        private List<List<ReservedLevelOne>> content;
    }

    @Data
    public static class ReservedLevelFour {
        private List<ReservedLevelThree> data;
    }

    @Data
    public static class Reserved {
        private Map<String, List<String>> values;
    }
}
