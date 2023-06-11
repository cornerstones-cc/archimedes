package cc.cornerstones.biz.datatable.service.assembly;

import cc.cornerstones.almond.constants.InfrastructureConstants;
import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.exceptions.*;
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
import cc.cornerstones.biz.datatable.entity.ContextPathMetadataRetrievalInstanceDo;
import cc.cornerstones.biz.datatable.persistence.ContextPathMetadataRetrievalInstanceRepository;
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
public class ContextPathMetadataRetrievalHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContextPathMetadataRetrievalHandler.class);

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
    private ContextPathMetadataRetrievalInstanceRepository contextPathMetadataRetrievalInstanceRepository;

    /**
     * retrieve metadata of context path 是一个重型活，要控制并发
     */
    private final ThreadFactory NAMED_THREAD_FACTORY = new ThreadFactoryBuilder()
            .setNameFormat("RMCT-%d").build();
    private final ExecutorService executorService = new ThreadPoolExecutor(
            1,
            3,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(3),
            NAMED_THREAD_FACTORY,
            new ThreadPoolExecutor.AbortPolicy());

    public Long retrieveMetadata(
            Long dataSourceUid,
            String contextPath,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DataSourceDo dataSourceDo = this.dataSourceRepository.findByUid(dataSourceUid);
        if (dataSourceDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataSourceDo.RESOURCE_SYMBOL,
                    dataSourceUid));
        }

        //
        String[] slicesOfContextPath = contextPath.split("/");
        List<String> contextPathList = new ArrayList<>(slicesOfContextPath.length);
        for (String slice : slicesOfContextPath) {
            contextPathList.add(slice);
        }

        // 检查是否存在还未结束的，针对同一 context path 的 metadata retrieval 实例，
        // 如果有，则同一时间只能有 1 个。
        boolean existsUnendingRefreshInstance = checkIfExistsUnendingMetadataRetrievalInstance(contextPath, dataSourceUid);
        if (existsUnendingRefreshInstance) {
            LOGGER.warn("already exists unending metadata retrieval instance of context path {}" +
                    " of data source {} ({}), ignore this request",
                    contextPath,
                    dataSourceDo.getUid(), dataSourceDo.getName());
            throw new AbcCapacityLimitException("only one metadata retrieval instance is allowed at the same time for" +
                    " each context path");
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
        final Long finalContextPathMetadataRetrievalInstanceUid =
                this.idHelper.getNextDistributedId(ContextPathMetadataRetrievalInstanceDo.RESOURCE_NAME);
        ContextPathMetadataRetrievalInstanceDo metadataRetrievalInstanceDo =
                new ContextPathMetadataRetrievalInstanceDo();
        metadataRetrievalInstanceDo.setUid(finalContextPathMetadataRetrievalInstanceUid);
        metadataRetrievalInstanceDo.setStatus(JobStatusEnum.CREATED);
        metadataRetrievalInstanceDo.setContextPath(contextPathList);
        metadataRetrievalInstanceDo.setContextPathStr(contextPath);
        metadataRetrievalInstanceDo.setDataSourceUid(dataSourceUid);
        BaseDo.create(metadataRetrievalInstanceDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.contextPathMetadataRetrievalInstanceRepository.save(metadataRetrievalInstanceDo);

        try {

            this.executorService.execute(new Runnable() {
                @Override
                public void run() {
                    //
                    // instance processing
                    //
                    updateMetadataRetrievalInstanceDo(finalContextPathMetadataRetrievalInstanceUid,
                            JobStatusEnum.RUNNING, null,
                            JobStatusEnum.CREATED);

                    //
                    // value creation processing
                    //
                    long beginTime = System.currentTimeMillis();

                    try {
                        LOGGER.info("instance:{}, begin to retrieve metadata of context path {} of data source {} - {}",
                                finalContextPathMetadataRetrievalInstanceUid, contextPath, dataSourceDo.getUid(),
                                dataSourceDo.getName());

                        // data table metadata
                        List<DataTableMetadata> dataTableMetadataList =
                                finalObjectiveDdlHandler.loadDataTableMetadata(contextPathList, dataSourceDo.getConnectionProfile());

                        if (!CollectionUtils.isEmpty(dataTableMetadataList)) {
                            dataTableMetadataList.forEach(dataTableMetadata -> {
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
                                    return;
                                }
                                // 为了跟 data table, data column, data index 对齐，空的也要记下来
                                if (CollectionUtils.isEmpty(dataColumnMetadataList)) {
                                    LOGGER.error("cannot find any data column metadata of data table {}",
                                            dataTableMetadata);
                                    return;
                                }

                                // data index metadata of a data table
                                List<DataIndexMetadata> dataIndexMetadataList = null;
                                try {
                                    dataIndexMetadataList =
                                            finalObjectiveDdlHandler.loadDataIndexMetadataOfDataTable(
                                                    dataSourceDo.getConnectionProfile(), dataTableMetadata);
                                } catch (Exception e) {
                                    LOGGER.warn("failed to load data index metadata of data table {}", dataTableMetadata
                                            , e);
                                }

                                // post event
                                DataTableMetadataRefreshedEvent dataTableMetadataRefreshedEvent =
                                        new DataTableMetadataRefreshedEvent();
                                dataTableMetadataRefreshedEvent.setDataSourceDo(dataSourceDo);
                                // 刷新的是 context path，暂时不知道 data table uid，event handler 需要自己处理
                                dataTableMetadataRefreshedEvent.setDataTableDo(null);
                                dataTableMetadataRefreshedEvent.setDataTableMetadata(dataTableMetadata);
                                dataTableMetadataRefreshedEvent.setDataColumnMetadataList(dataColumnMetadataList);
                                dataTableMetadataRefreshedEvent.setDataIndexMetadataList(dataIndexMetadataList);
                                dataTableMetadataRefreshedEvent.setOperatingUserProfile(operatingUserProfile);
                                eventBusManager.send(dataTableMetadataRefreshedEvent);
                            });
                        }

                        LOGGER.info("instance:{}, end to retrieve metadata of context path {} of data source {} - {}," +
                                        " " +
                                        "duration:{}",
                                finalContextPathMetadataRetrievalInstanceUid, contextPath, dataSourceDo.getUid(),
                                dataSourceDo.getName(),
                                AbcDateUtils.format(System.currentTimeMillis() - beginTime));

                        //
                        // instance processing
                        //
                        updateMetadataRetrievalInstanceDo(finalContextPathMetadataRetrievalInstanceUid,
                                JobStatusEnum.FINISHED, null,
                                JobStatusEnum.RUNNING);
                    } catch (Exception e) {
                        LOGGER.error("instance:{}, fail to retrieve metadata of context path {} of data source {} - " +
                                        "{}, duration:{}",
                                finalContextPathMetadataRetrievalInstanceUid, contextPath, dataSourceDo.getUid(),
                                dataSourceDo.getName(),
                                AbcDateUtils.format(System.currentTimeMillis() - beginTime), e);

                        //
                        // instance processing
                        //
                        String remark = null;
                        if (!ObjectUtils.isEmpty(e.getMessage())) {
                            remark = e.getMessage();
                        }
                        updateMetadataRetrievalInstanceDo(finalContextPathMetadataRetrievalInstanceUid,
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
            updateMetadataRetrievalInstanceDo(finalContextPathMetadataRetrievalInstanceUid,
                    JobStatusEnum.FAILED, "rejected execution",
                    JobStatusEnum.CREATED);

            throw new AbcCapacityLimitException("metadata retrieval task is busy, please try again later");
        }

        return finalContextPathMetadataRetrievalInstanceUid;
    }

    private boolean checkIfExistsUnendingMetadataRetrievalInstance(
            String contextPathStr,
            Long dataSourceUid) {
        if (ObjectUtils.isEmpty(contextPathStr)) {
            throw new AbcIllegalParameterException("context path should not be null or empty");
        }
        if (dataSourceUid == null) {
            throw new AbcIllegalParameterException("data_source_uid should not be null");
        }

        List<JobStatusEnum> statuses = new LinkedList<>();
        statuses.add(JobStatusEnum.INITIALIZING);
        statuses.add(JobStatusEnum.CREATED);
        statuses.add(JobStatusEnum.RUNNING);

        Specification<ContextPathMetadataRetrievalInstanceDo> specification =
                new Specification<ContextPathMetadataRetrievalInstanceDo>() {
                    @Override
                    public Predicate toPredicate(Root<ContextPathMetadataRetrievalInstanceDo> root, CriteriaQuery<?> query,
                                                 CriteriaBuilder criteriaBuilder) {
                        List<Predicate> predicateList = new ArrayList<>();

                        predicateList.add(criteriaBuilder.equal(root.get("contextPathStr"), contextPathStr));

                        predicateList.add(criteriaBuilder.equal(root.get("dataSourceUid"), dataSourceUid));

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

        List<ContextPathMetadataRetrievalInstanceDo> contextPathMetadataRetrievalInstanceDoList =
                this.contextPathMetadataRetrievalInstanceRepository.findAll(specification, sort);

        if (!CollectionUtils.isEmpty(contextPathMetadataRetrievalInstanceDoList)) {
            // 利用这个机会进行超时检查和处理
            // 检查规则：跟此刻时间相比，创建时间已超过3个小时，最后修改时间已超过1小时
            long currentMillis = System.currentTimeMillis();
            final long THREE_HOURS_IN_MILLIS = 3 * 60 * 60 * 1000;
            final long ONE_HOUR_IN_MILLIS = 1 * 60 * 60 * 1000;

            //
            List<ContextPathMetadataRetrievalInstanceDo> timeoutList = new LinkedList<>();

            contextPathMetadataRetrievalInstanceDoList.forEach(metadataRetrievalInstanceDo -> {
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
                this.contextPathMetadataRetrievalInstanceRepository.saveAll(timeoutList);
            }

            if (contextPathMetadataRetrievalInstanceDoList.size() > timeoutList.size()) {
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
        ContextPathMetadataRetrievalInstanceDo metadataRetrievalInstanceDo =
                this.contextPathMetadataRetrievalInstanceRepository.findByUid(uid);
        if (metadataRetrievalInstanceDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    ContextPathMetadataRetrievalInstanceDo.RESOURCE_SYMBOL, uid));
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
        this.contextPathMetadataRetrievalInstanceRepository.save(metadataRetrievalInstanceDo);
    }
}
