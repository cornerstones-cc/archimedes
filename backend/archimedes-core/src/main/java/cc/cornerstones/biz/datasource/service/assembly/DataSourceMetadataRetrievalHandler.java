package cc.cornerstones.biz.datasource.service.assembly;

import cc.cornerstones.almond.constants.InfrastructureConstants;
import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.exceptions.AbcCapacityLimitException;
import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcDateUtils;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.administration.serviceconnection.entity.UserSynchronizationExecutionInstanceDo;
import cc.cornerstones.biz.administration.serviceconnection.service.assembly.UserSynchronizationHandler;
import cc.cornerstones.biz.datasource.entity.DataSourceDo;
import cc.cornerstones.biz.datasource.entity.DataSourceMetadataRetrievalInstanceDo;
import cc.cornerstones.biz.datasource.persistence.DataSourceRepository;
import cc.cornerstones.biz.datasource.persistence.DataSourceMetadataRetrievalInstanceRepository;
import cc.cornerstones.biz.datasource.service.assembly.database.DataColumnMetadata;
import cc.cornerstones.biz.datasource.service.assembly.database.DataIndexMetadata;
import cc.cornerstones.biz.datasource.service.assembly.database.DataTableMetadata;
import cc.cornerstones.biz.datasource.service.assembly.database.DdlHandler;
import cc.cornerstones.biz.distributedjob.dto.CreateDistributedJobDto;
import cc.cornerstones.biz.distributedjob.dto.DistributedJobDto;
import cc.cornerstones.biz.distributedjob.share.constants.JobExecutorRoutingAlgorithmEnum;
import cc.cornerstones.biz.distributedjob.share.types.JobHandler;
import cc.cornerstones.biz.share.event.DataSourceMetadataRefreshedEvent;
import cc.cornerstones.biz.share.event.EventBusManager;
import com.alibaba.fastjson.JSONObject;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
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
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;

@Component
public class DataSourceMetadataRetrievalHandler implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceMetadataRetrievalHandler.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private DataSourceMetadataRetrievalInstanceRepository dataSourceMetadataRetrievalInstanceRepository;

    private static final String JOB_HANDLER_DATA_SOURCE_CHANGE_METADATA_CAPTURE = "data_source_change_metadata_capture";

    /**
     * retrieve metadata of data source 是一个重型活，要控制并发
     */
    private final ThreadFactory NAMED_THREAD_FACTORY = new ThreadFactoryBuilder()
            .setNameFormat("RMDS-%d").build();
    private final ExecutorService executorService = new ThreadPoolExecutor(
            1,
            3,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(3),
            NAMED_THREAD_FACTORY,
            new ThreadPoolExecutor.AbortPolicy());

    public Long retrieveMetadata(
            DataSourceDo dataSourceDo,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        // 检查是否存在还未结束的，针对同一 data source 的 metadata retrieval 实例，
        // 如果有，则同一时间只能有 1 个。
        boolean existsUnendingRefreshInstance = checkIfExistsUnendingMetadataRetrievalInstance(dataSourceDo.getUid());
        if (existsUnendingRefreshInstance) {
            LOGGER.warn("already exists unending metadata retrieval instance of data source {}" +
                    " - " +
                    "{}," +
                    " ignore this request", dataSourceDo.getUid(), dataSourceDo.getName());
            throw new AbcCapacityLimitException("only one metadata retrieval instance is allowed at the same time for" +
                    " each data source");
        }

        DdlHandler objectiveDdlHandler = null;
        Map<String, DdlHandler> map = this.applicationContext.getBeansOfType(DdlHandler.class);
        if (!CollectionUtils.isEmpty(map)) {
            for (Map.Entry<String, DdlHandler> entry : map.entrySet()) {
                DdlHandler ddlHandler = entry.getValue();
                if (ddlHandler.type().equals(dataSourceDo.getType())) {
                    objectiveDdlHandler = ddlHandler;
                    break;
                }
            }
        }
        if (objectiveDdlHandler == null) {
            throw new AbcResourceConflictException(
                    String.format("cannot find DDL handler of data source type:%s",
                            dataSourceDo.getType()));
        }

        final DdlHandler finalObjectiveDdlHandler = objectiveDdlHandler;

        //
        // Step 2, core-processing
        //

        // create an instance, status is CREATED
        final Long finalDataSourceMetadataRetrievalInstanceUid =
                this.idHelper.getNextDistributedId(DataSourceMetadataRetrievalInstanceDo.RESOURCE_NAME);
        DataSourceMetadataRetrievalInstanceDo dataSourceMetadataRetrievalInstanceDo = new DataSourceMetadataRetrievalInstanceDo();
        dataSourceMetadataRetrievalInstanceDo.setUid(finalDataSourceMetadataRetrievalInstanceUid);
        dataSourceMetadataRetrievalInstanceDo.setStatus(JobStatusEnum.CREATED);
        dataSourceMetadataRetrievalInstanceDo.setDataSourceUid(dataSourceDo.getUid());
        BaseDo.create(dataSourceMetadataRetrievalInstanceDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dataSourceMetadataRetrievalInstanceRepository.save(dataSourceMetadataRetrievalInstanceDo);

        try {

            this.executorService.execute(new Runnable() {
                @Override
                public void run() {
                    //
                    // instance processing
                    //
                    updateMetadataRetrievalInstanceDo(finalDataSourceMetadataRetrievalInstanceUid,
                            JobStatusEnum.RUNNING, null,
                            JobStatusEnum.CREATED);

                    //
                    // value creation processing
                    //
                    long beginTime = System.currentTimeMillis();

                    try {
                        LOGGER.info("instance:{}, begin to retrieve metadata of data source {} - {}",
                                finalDataSourceMetadataRetrievalInstanceUid, dataSourceDo.getUid(), dataSourceDo.getName());

                        // data table metadata
                        List<DataTableMetadata> dataTableMetadataList =
                                finalObjectiveDdlHandler.loadDataTableMetadata(
                                        dataSourceDo.getConnectionProfile());

                        if (!CollectionUtils.isEmpty(dataTableMetadataList)) {
                            List<DataSourceMetadataRefreshedEvent.DataTable> dataTableList = new LinkedList<>();

                            for (int i = 0; i < dataTableMetadataList.size(); i++) {
                                DataTableMetadata dataTableMetadata = dataTableMetadataList.get(i);

                                DataSourceMetadataRefreshedEvent.DataTable dataTable = new DataSourceMetadataRefreshedEvent.DataTable();
                                dataTable.setDataTableMetadata(dataTableMetadata);

                                // data column metadata of a data table
                                // 已经出现过数据库视图依赖了无效的表，类似如下错误，忽略就好，不影响其它表
                                // SQL Error [1356] [HY000]: View 'locust.v_export_other_for_import' references invalid table(s) or
                                // column(s) or function(s) or definer/invoker of view lack rights to use them
                                List<DataColumnMetadata> dataColumnMetadataList = null;
                                try {
                                    dataColumnMetadataList =
                                            finalObjectiveDdlHandler.loadDataColumnMetadataOfDataTable(
                                                    dataSourceDo.getConnectionProfile(), dataTableMetadata);
                                } catch (Exception e) {
                                    LOGGER.error("failed to load data column metadata of data table {}", dataTableMetadata, e);
                                }
                                if (CollectionUtils.isEmpty(dataColumnMetadataList)) {
                                    LOGGER.error("cannot find any data column of data table {}", dataTableMetadata);
                                    continue;
                                }
                                dataTable.setDataColumnMetadataList(dataColumnMetadataList);

                                // data index metadata of a data table
                                List<DataIndexMetadata> dataIndexMetadataList = null;
                                try {
                                    dataIndexMetadataList =
                                            finalObjectiveDdlHandler.loadDataIndexMetadataOfDataTable(
                                                    dataSourceDo.getConnectionProfile(), dataTableMetadata);
                                } catch (Exception e) {
                                    LOGGER.error("failed to load data index metadata of data table {}",
                                            dataTableMetadata, e);
                                }
                                dataTable.setDataIndexMetadataList(dataIndexMetadataList);

                                dataTableList.add(dataTable);
                            }

                            // post event
                            DataSourceMetadataRefreshedEvent dataSourceMetadataRefreshedEvent =
                                    new DataSourceMetadataRefreshedEvent();
                            dataSourceMetadataRefreshedEvent.setDataSourceDo(dataSourceDo);
                            dataSourceMetadataRefreshedEvent.setDataTableList(dataTableList);
                            dataSourceMetadataRefreshedEvent.setOperatingUserProfile(operatingUserProfile);
                            eventBusManager.send(dataSourceMetadataRefreshedEvent);
                        }

                        LOGGER.info("instance:{}, end to retrieve metadata of data source {} - {}, " +
                                        "duration:{}",
                                finalDataSourceMetadataRetrievalInstanceUid, dataSourceDo.getUid(), dataSourceDo.getName(),
                                AbcDateUtils.format(System.currentTimeMillis() - beginTime));

                        //
                        // instance processing
                        //
                        updateMetadataRetrievalInstanceDo(finalDataSourceMetadataRetrievalInstanceUid,
                                JobStatusEnum.FINISHED, null,
                                JobStatusEnum.RUNNING);
                    } catch (Exception e) {
                        LOGGER.error("instance:{}, fail to retrieve metadata of data source {} - " +
                                        "{}, duration:{}",
                                finalDataSourceMetadataRetrievalInstanceUid, dataSourceDo.getUid(), dataSourceDo.getName(),
                                AbcDateUtils.format(System.currentTimeMillis() - beginTime), e);

                        //
                        // instance processing
                        //
                        String remark = null;
                        if (!ObjectUtils.isEmpty(e.getMessage())) {
                            remark = e.getMessage();
                        }
                        updateMetadataRetrievalInstanceDo(finalDataSourceMetadataRetrievalInstanceUid,
                                JobStatusEnum.FAILED, remark,
                                JobStatusEnum.RUNNING);
                    }
                }
            });
        } catch (RejectedExecutionException e) {
            LOGGER.warn("reach queue capacity limit", e);

            //
            // instance processing
            //
            updateMetadataRetrievalInstanceDo(finalDataSourceMetadataRetrievalInstanceUid,
                    JobStatusEnum.FAILED, "rejected execution",
                    JobStatusEnum.CREATED);

            throw new AbcCapacityLimitException("metadata retrieval task is busy, please try again later");
        }

        //
        // Step 3, post-processing
        //

        return finalDataSourceMetadataRetrievalInstanceUid;
    }

    private boolean checkIfExistsUnendingMetadataRetrievalInstance(Long dataSourceUid) {
        List<JobStatusEnum> statuses = new LinkedList<>();
        statuses.add(JobStatusEnum.INITIALIZING);
        statuses.add(JobStatusEnum.CREATED);
        statuses.add(JobStatusEnum.RUNNING);

        Specification<DataSourceMetadataRetrievalInstanceDo> specification =
                new Specification<DataSourceMetadataRetrievalInstanceDo>() {
                    @Override
                    public Predicate toPredicate(Root<DataSourceMetadataRetrievalInstanceDo> root, CriteriaQuery<?> query,
                                                 CriteriaBuilder criteriaBuilder) {
                        List<Predicate> predicateList = new ArrayList<>();
                        if (dataSourceUid != null) {
                            predicateList.add(criteriaBuilder.equal(root.get("dataSourceUid"), dataSourceUid));
                        }
                        if (!CollectionUtils.isEmpty(statuses)) {
                            CriteriaBuilder.In<JobStatusEnum> in = criteriaBuilder.in(root.get("status"));
                            statuses.forEach(status -> {
                                in.value(status);
                            });
                            predicateList.add(in);
                        }
                        return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
                    }
                };

        Sort sort = Sort.by(Sort.Order.asc(BaseDo.CREATED_TIMESTAMP_FIELD_NAME));

        List<DataSourceMetadataRetrievalInstanceDo> dataSourceMetadataRetrievalInstanceDoList =
                this.dataSourceMetadataRetrievalInstanceRepository.findAll(specification, sort);

        if (!CollectionUtils.isEmpty(dataSourceMetadataRetrievalInstanceDoList)) {
            // 利用这个机会进行超时检查和处理
            // 检查规则：跟此刻时间相比，创建时间已超过3个小时，最后修改时间已超过1小时
            long currentMillis = System.currentTimeMillis();
            final long THREE_HOURS_IN_MILLIS = 3 * 60 * 60 * 1000;
            final long ONE_HOUR_IN_MILLIS = 1 * 60 * 60 * 1000;

            //
            List<DataSourceMetadataRetrievalInstanceDo> timeoutList = new LinkedList<>();

            dataSourceMetadataRetrievalInstanceDoList.forEach(dataSourceMetadataRetrievalInstanceDo -> {
                long differInCreationTime =
                        currentMillis - dataSourceMetadataRetrievalInstanceDo.getCreatedTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                long differInModificationTime =
                        currentMillis - dataSourceMetadataRetrievalInstanceDo.getLastModifiedTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                if (differInCreationTime > THREE_HOURS_IN_MILLIS
                        || differInModificationTime > ONE_HOUR_IN_MILLIS) {
                    dataSourceMetadataRetrievalInstanceDo.setStatus(JobStatusEnum.FAILED);
                    dataSourceMetadataRetrievalInstanceDo.setRemark("timeout");
                    BaseDo.update(dataSourceMetadataRetrievalInstanceDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
                    LOGGER.warn("a timeout is detected and the instance:{} is set to failed",
                            dataSourceMetadataRetrievalInstanceDo.getUid());

                    timeoutList.add(dataSourceMetadataRetrievalInstanceDo);
                }
            });

            if (!timeoutList.isEmpty()) {
                this.dataSourceMetadataRetrievalInstanceRepository.saveAll(timeoutList);
            }

            if (dataSourceMetadataRetrievalInstanceDoList.size() > timeoutList.size()) {
                return true;
            }
        }

        return false;
    }

    private void updateMetadataRetrievalInstanceDo(
            Long uid,
            JobStatusEnum newStatus,
            String remark,
            JobStatusEnum... allowedOldStatuses) {
        DataSourceMetadataRetrievalInstanceDo dataSourceMetadataRetrievalInstanceDo =
                this.dataSourceMetadataRetrievalInstanceRepository.findByUid(uid);
        if (dataSourceMetadataRetrievalInstanceDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DataSourceMetadataRetrievalInstanceDo.RESOURCE_SYMBOL, uid));
        }

        if (newStatus.equals(dataSourceMetadataRetrievalInstanceDo.getStatus())) {
            return;
        }

        if (allowedOldStatuses != null && allowedOldStatuses.length > 0) {
            boolean allowed = false;
            StringBuilder allowedAsString = new StringBuilder();
            for (JobStatusEnum allowedOldStatus : allowedOldStatuses) {
                if (allowedAsString.length() > 0) {
                    allowedAsString.append(",").append(allowedOldStatus);
                }

                if (allowedOldStatus.equals(dataSourceMetadataRetrievalInstanceDo.getStatus())) {
                    allowed = true;
                }
            }

            if (!allowed) {
                throw new AbcResourceConflictException(String.format("expected status is %d, but " +
                                "found:%s, instance:%d", allowedAsString,
                        dataSourceMetadataRetrievalInstanceDo.getStatus(),
                        uid));
            }
        }

        dataSourceMetadataRetrievalInstanceDo.setStatus(newStatus);
        dataSourceMetadataRetrievalInstanceDo.setRemark(remark);
        BaseDo.update(dataSourceMetadataRetrievalInstanceDo, InfrastructureConstants.ROOT_USER_UID,
                LocalDateTime.now());
        dataSourceMetadataRetrievalInstanceRepository.save(dataSourceMetadataRetrievalInstanceDo);
    }

    @JobHandler(name = JOB_HANDLER_DATA_SOURCE_CHANGE_METADATA_CAPTURE)
    public void dataSourceChangeMetadataCapture(
            JSONObject params) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        UserProfile operatingUserProfile = new UserProfile();
        operatingUserProfile.setUid(InfrastructureConstants.ROOT_USER_UID);
        operatingUserProfile.setDisplayName(InfrastructureConstants.ROOT_USER_DISPLAY_NAME);

        //
        // Step 2, core-processing
        //
        this.dataSourceRepository.findAll().forEach(dataSourceDo -> {
            retrieveMetadata(dataSourceDo, operatingUserProfile);
        });

        //
        // Step 3, post-processing
        //
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        
    }
}
