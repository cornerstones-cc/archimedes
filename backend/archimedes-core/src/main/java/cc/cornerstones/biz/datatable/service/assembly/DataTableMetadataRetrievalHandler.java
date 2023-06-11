package cc.cornerstones.biz.datatable.service.assembly;

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
import cc.cornerstones.biz.datasource.entity.DataSourceDo;
import cc.cornerstones.biz.datasource.persistence.DataSourceRepository;
import cc.cornerstones.biz.datasource.service.assembly.database.DataColumnMetadata;
import cc.cornerstones.biz.datasource.service.assembly.database.DataIndexMetadata;
import cc.cornerstones.biz.datasource.service.assembly.database.DataTableMetadata;
import cc.cornerstones.biz.datasource.service.assembly.database.DdlHandler;
import cc.cornerstones.biz.datatable.entity.DataTableDo;
import cc.cornerstones.biz.datatable.entity.DataTableMetadataRetrievalInstanceDo;
import cc.cornerstones.biz.datatable.persistence.DataTableMetadataRetrievalInstanceRepository;
import cc.cornerstones.biz.datatable.persistence.DataTableRepository;
import cc.cornerstones.biz.share.event.DataTableMetadataRefreshedEvent;
import cc.cornerstones.biz.share.event.EventBusManager;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Component
public class DataTableMetadataRetrievalHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataTableMetadataRetrievalHandler.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataTableRepository dataTableRepository;

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private DataTableMetadataRetrievalInstanceRepository dataTableMetadataRetrievalInstanceRepository;

    /**
     * retrieve metadata of data table 是一个重型活，要控制并发
     */
    private final ThreadFactory NAMED_THREAD_FACTORY = new ThreadFactoryBuilder()
            .setNameFormat("RMDT-%d").build();
    private final ExecutorService executorService = new ThreadPoolExecutor(
            1,
            3,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(3),
            NAMED_THREAD_FACTORY,
            new ThreadPoolExecutor.AbortPolicy());

    public Long retrieveMetadata(
            Long dataTableUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DataTableDo dataTableDo = this.dataTableRepository.findByUid(dataTableUid);
        if (dataTableDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataTableDo.RESOURCE_SYMBOL,
                    dataTableUid));
        }

        DataSourceDo dataSourceDo = this.dataSourceRepository.findByUid(dataTableDo.getDataSourceUid());
        if (dataSourceDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataSourceDo.RESOURCE_SYMBOL,
                    dataTableDo.getDataSourceUid()));
        }

        // 检查是否存在还未结束的，针对同一 data table 的 metadata retrieval 实例，
        // 如果有，则同一时间只能有 1 个。
        boolean existsUnendingRefreshInstance = checkIfExistsUnendingMetadataRetrievalInstance(dataTableUid);
        if (existsUnendingRefreshInstance) {
            LOGGER.warn("already exists unending metadata retrieval instance of data table {} ({})," +
                    " ignore this request", dataTableDo.getUid(), dataTableDo.getName());
            throw new AbcCapacityLimitException("only one metadata retrieval instance is allowed at the same time for" +
                    " each data table");
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
        final Long finalDataTableMetadataRetrievalInstanceUid =
                this.idHelper.getNextDistributedId(DataTableMetadataRetrievalInstanceDo.RESOURCE_NAME);
        DataTableMetadataRetrievalInstanceDo metadataRetrievalInstanceDo =
                new DataTableMetadataRetrievalInstanceDo();
        metadataRetrievalInstanceDo.setUid(finalDataTableMetadataRetrievalInstanceUid);
        metadataRetrievalInstanceDo.setStatus(JobStatusEnum.CREATED);
        metadataRetrievalInstanceDo.setDataTableUid(dataTableUid);
        metadataRetrievalInstanceDo.setDataSourceUid(dataTableDo.getDataSourceUid());
        BaseDo.create(metadataRetrievalInstanceDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dataTableMetadataRetrievalInstanceRepository.save(metadataRetrievalInstanceDo);

        try {

            this.executorService.execute(new Runnable() {
                @Override
                public void run() {
                    //
                    // instance processing
                    //
                    updateMetadataRetrievalInstanceDo(finalDataTableMetadataRetrievalInstanceUid,
                            JobStatusEnum.RUNNING, null,
                            JobStatusEnum.CREATED);

                    //
                    // value creation processing
                    //
                    long beginTime = System.currentTimeMillis();

                    try {
                        LOGGER.info("instance:{}, begin to retrieve metadata of data table {} ({}) of data source {} " +
                                        "({})",
                                finalDataTableMetadataRetrievalInstanceUid,
                                dataTableDo.getUid(), dataTableDo.getName(),
                                dataSourceDo.getUid(), dataSourceDo.getName());


                        List<DataTableDo> objectiveDataTableDoList = new LinkedList<>();

                        switch (dataTableDo.getType()) {
                            case INDIRECT_TABLE: {
                                List<Long> underlyingDataTableUidList = dataTableDo.getUnderlyingDataTableUidList();
                                if (!CollectionUtils.isEmpty(underlyingDataTableUidList)) {
                                    List<DataTableDo> underlyingDataTableDoList =
                                            dataTableRepository.findByUidIn(underlyingDataTableUidList);
                                    objectiveDataTableDoList.addAll(underlyingDataTableDoList);
                                }
                            }
                            break;
                            default: {
                                objectiveDataTableDoList.add(dataTableDo);
                            }
                            break;
                        }

                        for (int i = 0; i < objectiveDataTableDoList.size(); i++) {
                            DataTableDo objectiveDataTableDo = objectiveDataTableDoList.get(i);

                            // data table metadata
                            DataTableMetadata objectiveDataTableMetadata =
                                    finalObjectiveDdlHandler.loadDataTableMetadata(
                                            objectiveDataTableDo.getName(),
                                            objectiveDataTableDo.getContextPath(),
                                            dataSourceDo.getConnectionProfile());

                            // data column metadata of a data table
                            // 已经出现过数据库视图依赖了无效的表，类似如下错误，忽略就好，不影响其它表
                            // SQL Error [1356] [HY000]: View 'locust.v_export_other_for_import' references invalid table(s) or
                            // column(s) or function(s) or definer/invoker of view lack rights to use them
                            List<DataColumnMetadata> objectiveDataColumnMetadataList = null;
                            try {
                                objectiveDataColumnMetadataList =
                                        finalObjectiveDdlHandler.loadDataColumnMetadataOfDataTable(
                                                dataSourceDo.getConnectionProfile(), objectiveDataTableMetadata);
                            } catch (Exception e) {
                                LOGGER.error("failed to load data column metadata of data table {}", objectiveDataTableMetadata, e);
                                continue;
                            }
                            if (CollectionUtils.isEmpty(objectiveDataColumnMetadataList)) {
                                LOGGER.error("cannot find any data column metadata of data table {}",
                                        objectiveDataTableMetadata);
                                continue;
                            }

                            // data index metadata of a data table
                            List<DataIndexMetadata> objectiveDataIndexMetadataList = null;
                            try {
                                objectiveDataIndexMetadataList =
                                        finalObjectiveDdlHandler.loadDataIndexMetadataOfDataTable(
                                                dataSourceDo.getConnectionProfile(), objectiveDataTableMetadata);
                            } catch (Exception e) {
                                LOGGER.warn("failed to load data index metadata of data table {}", objectiveDataTableMetadata, e);
                            }

                            // post event
                            DataTableMetadataRefreshedEvent dataTableMetadataRefreshedEvent =
                                    new DataTableMetadataRefreshedEvent();
                            dataTableMetadataRefreshedEvent.setDataSourceDo(dataSourceDo);
                            dataTableMetadataRefreshedEvent.setDataTableDo(objectiveDataTableDo);
                            dataTableMetadataRefreshedEvent.setDataTableMetadata(objectiveDataTableMetadata);
                            dataTableMetadataRefreshedEvent.setDataColumnMetadataList(objectiveDataColumnMetadataList);
                            dataTableMetadataRefreshedEvent.setDataIndexMetadataList(objectiveDataIndexMetadataList);
                            dataTableMetadataRefreshedEvent.setOperatingUserProfile(operatingUserProfile);
                            eventBusManager.send(dataTableMetadataRefreshedEvent);
                        }

                        LOGGER.info("instance:{}, end to retrieve metadata of data table {} ({}) of data source {} (" +
                                        "{}), duration:{}",
                                finalDataTableMetadataRetrievalInstanceUid,
                                dataTableDo.getUid(), dataTableDo.getName(),
                                dataSourceDo.getUid(), dataSourceDo.getName(),
                                AbcDateUtils.format(System.currentTimeMillis() - beginTime));

                        //
                        // instance processing
                        //
                        updateMetadataRetrievalInstanceDo(finalDataTableMetadataRetrievalInstanceUid,
                                JobStatusEnum.FINISHED, null,
                                JobStatusEnum.RUNNING);
                    } catch (Exception e) {
                        LOGGER.error("instance:{}, fail to retrieve metadata of data table {} ({}) of data source {} (" +
                                        "{}), duration:{}",
                                finalDataTableMetadataRetrievalInstanceUid,
                                dataTableDo.getUid(), dataTableDo.getName(),
                                dataSourceDo.getUid(), dataSourceDo.getName(),
                                AbcDateUtils.format(System.currentTimeMillis() - beginTime), e);

                        //
                        // instance processing
                        //
                        String remark = null;
                        if (!ObjectUtils.isEmpty(e.getMessage())) {
                            remark = e.getMessage();
                        }
                        updateMetadataRetrievalInstanceDo(finalDataTableMetadataRetrievalInstanceUid,
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
            updateMetadataRetrievalInstanceDo(finalDataTableMetadataRetrievalInstanceUid,
                    JobStatusEnum.FAILED, "rejected execution",
                    JobStatusEnum.CREATED);

            throw new AbcCapacityLimitException("metadata retrieval task is busy, please try again later");
        }

        return finalDataTableMetadataRetrievalInstanceUid;
    }

    private boolean checkIfExistsUnendingMetadataRetrievalInstance(
            Long dataTableUid) {
        List<JobStatusEnum> statuses = new LinkedList<>();
        statuses.add(JobStatusEnum.INITIALIZING);
        statuses.add(JobStatusEnum.CREATED);
        statuses.add(JobStatusEnum.RUNNING);

        Specification<DataTableMetadataRetrievalInstanceDo> specification =
                new Specification<DataTableMetadataRetrievalInstanceDo>() {
                    @Override
                    public Predicate toPredicate(Root<DataTableMetadataRetrievalInstanceDo> root, CriteriaQuery<?> query,
                                                 CriteriaBuilder criteriaBuilder) {
                        List<Predicate> predicateList = new ArrayList<>();
                        if (dataTableUid != null) {
                            predicateList.add(criteriaBuilder.equal(root.get("dataTableUid"), dataTableUid));
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

        List<DataTableMetadataRetrievalInstanceDo> dataTableMetadataRetrievalInstanceDoList =
                this.dataTableMetadataRetrievalInstanceRepository.findAll(specification, sort);

        if (!CollectionUtils.isEmpty(dataTableMetadataRetrievalInstanceDoList)) {
            // 利用这个机会进行超时检查和处理
            // 检查规则：跟此刻时间相比，创建时间已超过3个小时，最后修改时间已超过1小时
            long currentMillis = System.currentTimeMillis();
            final long THREE_HOURS_IN_MILLIS = 3 * 60 * 60 * 1000;
            final long ONE_HOUR_IN_MILLIS = 1 * 60 * 60 * 1000;

            //
            List<DataTableMetadataRetrievalInstanceDo> timeoutList = new LinkedList<>();

            dataTableMetadataRetrievalInstanceDoList.forEach(metadataRetrievalInstanceDo -> {
                long differInCreationTime =
                        currentMillis - metadataRetrievalInstanceDo.getCreatedTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                long differInModificationTime =
                        currentMillis - metadataRetrievalInstanceDo.getLastModifiedTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                if (differInCreationTime > THREE_HOURS_IN_MILLIS
                        || differInModificationTime > ONE_HOUR_IN_MILLIS) {
                    metadataRetrievalInstanceDo.setStatus(JobStatusEnum.FAILED);
                    metadataRetrievalInstanceDo.setRemark("timeout");
                    BaseDo.update(metadataRetrievalInstanceDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
                    LOGGER.warn("a timeout is detected and the instance:{} is set to failed",
                            metadataRetrievalInstanceDo.getUid());

                    timeoutList.add(metadataRetrievalInstanceDo);
                }
            });

            if (!timeoutList.isEmpty()) {
                this.dataTableMetadataRetrievalInstanceRepository.saveAll(timeoutList);
            }

            if (dataTableMetadataRetrievalInstanceDoList.size() > timeoutList.size()) {
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
        DataTableMetadataRetrievalInstanceDo metadataRetrievalInstanceDo =
                this.dataTableMetadataRetrievalInstanceRepository.findByUid(uid);
        if (metadataRetrievalInstanceDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DataTableMetadataRetrievalInstanceDo.RESOURCE_SYMBOL, uid));
        }

        if (newStatus.equals(metadataRetrievalInstanceDo.getStatus())) {
            return;
        }

        if (allowedOldStatuses != null && allowedOldStatuses.length > 0) {
            boolean allowed = false;
            StringBuilder allowedAsString = new StringBuilder();
            for (JobStatusEnum allowedOldStatus : allowedOldStatuses) {
                if (allowedAsString.length() > 0) {
                    allowedAsString.append(",").append(allowedOldStatus);
                }

                if (allowedOldStatus.equals(metadataRetrievalInstanceDo.getStatus())) {
                    allowed = true;
                }
            }

            if (!allowed) {
                throw new AbcResourceConflictException(String.format("expected status is %d, but " +
                                "found:%s, instance:%d", allowedAsString,
                        metadataRetrievalInstanceDo.getStatus(),
                        uid));
            }
        }

        metadataRetrievalInstanceDo.setStatus(newStatus);
        metadataRetrievalInstanceDo.setRemark(remark);
        BaseDo.update(metadataRetrievalInstanceDo, InfrastructureConstants.ROOT_USER_UID,
                LocalDateTime.now());
        this.dataTableMetadataRetrievalInstanceRepository.save(metadataRetrievalInstanceDo);
    }
}
