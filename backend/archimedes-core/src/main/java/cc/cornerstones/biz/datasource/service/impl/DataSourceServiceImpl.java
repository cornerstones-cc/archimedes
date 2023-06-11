package cc.cornerstones.biz.datasource.service.impl;

import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceDuplicateException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.administration.serviceconnection.entity.UserSynchronizationServiceComponentDo;
import cc.cornerstones.biz.datasource.entity.DataSourceDo;
import cc.cornerstones.biz.datasource.dto.CreateDataSourceDto;
import cc.cornerstones.biz.datasource.dto.DataSourceDto;
import cc.cornerstones.biz.datasource.dto.UpdateDataSourceDto;
import cc.cornerstones.biz.datasource.entity.DataSourceMetadataRetrievalInstanceDo;
import cc.cornerstones.biz.datasource.entity.DatabaseServerDo;
import cc.cornerstones.biz.datasource.persistence.DataSourceMetadataRetrievalInstanceRepository;
import cc.cornerstones.biz.datasource.persistence.DataSourceRepository;
import cc.cornerstones.biz.datasource.persistence.DatabaseServerRepository;
import cc.cornerstones.biz.datasource.service.assembly.database.ConnectivityHandler;
import cc.cornerstones.biz.datasource.service.assembly.database.DmlHandler;
import cc.cornerstones.biz.datasource.service.assembly.database.QueryBuilder;
import cc.cornerstones.biz.datasource.service.assembly.database.QueryResult;
import cc.cornerstones.biz.datasource.service.assembly.DataSourceMetadataRetrievalHandler;
import cc.cornerstones.biz.datasource.service.inf.DataSourceService;
import cc.cornerstones.biz.datasource.share.constants.DatabaseServerTypeEnum;
import cc.cornerstones.biz.datatable.dto.TestQueryStatementDto;
import cc.cornerstones.biz.share.assembly.ResourceReferenceManager;
import cc.cornerstones.biz.share.event.DataSourceDeletedEvent;
import cc.cornerstones.biz.share.event.EventBusManager;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.ObjectUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author bbottong
 */
@Service
public class DataSourceServiceImpl implements DataSourceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DatabaseServerRepository databaseServerRepository;

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private DataSourceMetadataRetrievalHandler dataSourceMetadataRetrievalHandler;

    @Autowired
    private DataSourceMetadataRetrievalInstanceRepository dataSourceMetadataRetrievalInstanceRepository;

    @Autowired
    private ResourceReferenceManager resourceReferenceManager;

    /**
     * 创建 Data Source
     *
     * @param createDataSourceDto
     * @param operatingUserProfile
     * @return
     * @throws AbcUndefinedException
     */
    @Override
    public DataSourceDto createDataSource(
            CreateDataSourceDto createDataSourceDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        //
        // step 1.1, 不能有 name 相同的 Data source
        //
        boolean existsDuplicate = this.dataSourceRepository.existsByName(createDataSourceDto.getName());
        if (existsDuplicate) {
            throw new AbcResourceDuplicateException(String.format("%s::name:%s", DataSourceDo.RESOURCE_SYMBOL,
                    createDataSourceDto.getName()));
        }

        //
        // step 1.2, 也不能有 connection profile 相同的 Data source
        //
        String connectionProfileHashedString =
                Base64Utils.encodeToString(createDataSourceDto.getConnectionProfile().toJSONString().getBytes(StandardCharsets.UTF_8));
        existsDuplicate = this.dataSourceRepository.existsByConnectionProfileHashedString(connectionProfileHashedString);
        if (existsDuplicate) {
            throw new AbcResourceDuplicateException(String.format("%s::connection profile",
                    DataSourceDo.RESOURCE_SYMBOL));
        }

        //
        // step 1.3, test connectivity
        //
        ConnectivityHandler objectiveConnectivityHandler = null;
        Map<String, ConnectivityHandler> map = this.applicationContext.getBeansOfType(ConnectivityHandler.class);
        if (!CollectionUtils.isEmpty(map)) {
            for (Map.Entry<String, ConnectivityHandler> entry : map.entrySet()) {
                ConnectivityHandler connectivityHandler = entry.getValue();
                if (connectivityHandler.type().equals(createDataSourceDto.getType())) {
                    objectiveConnectivityHandler = connectivityHandler;
                    break;
                }
            }
        }
        if (objectiveConnectivityHandler == null) {
            throw new AbcResourceConflictException(
                    String.format("cannot find connectivity handler of data source type:%s",
                            createDataSourceDto.getType()));
        }
        // perform connectivity test
        objectiveConnectivityHandler.testConnectivity(createDataSourceDto.getConnectionProfile());


        //
        // Step 2, core-processing
        //

        //
        // Step 2.1, 找出或创建 database server
        //
        JSONObject hostProfileOfDatabaseServer =
                objectiveConnectivityHandler.extractHostProfile(createDataSourceDto.getConnectionProfile());
        String hashedHostProfileOfDatabaseServer =
                DigestUtils.md5DigestAsHex(hostProfileOfDatabaseServer.toJSONString().getBytes(StandardCharsets.UTF_8));
        DatabaseServerDo databaseServerDo =
                this.databaseServerRepository.findByTypeAndHashedHostProfile(createDataSourceDto.getType(),
                        hashedHostProfileOfDatabaseServer);
        if (databaseServerDo == null) {
            databaseServerDo = new DatabaseServerDo();
            databaseServerDo.setUid(this.idHelper.getNextDistributedId(DatabaseServerDo.RESOURCE_NAME));
            databaseServerDo.setHashedHostProfile(hashedHostProfileOfDatabaseServer);
            databaseServerDo.setHostProfile(hostProfileOfDatabaseServer);
            databaseServerDo.setType(createDataSourceDto.getType());
            BaseDo.create(databaseServerDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.databaseServerRepository.save(databaseServerDo);
        }

        //
        // Step 2.2, 创建 data source
        //
        DataSourceDo dataSourceDo = new DataSourceDo();
        dataSourceDo.setUid(this.idHelper.getNextDistributedId(DataSourceDo.RESOURCE_NAME));
        dataSourceDo.setName(createDataSourceDto.getName());
        dataSourceDo.setObjectName(createDataSourceDto.getName()
                .replaceAll("_", "__")
                .replaceAll("\\s", "_")
                .toLowerCase());
        dataSourceDo.setDescription(createDataSourceDto.getDescription());
        dataSourceDo.setType(createDataSourceDto.getType());
        dataSourceDo.setConnectionProfile(createDataSourceDto.getConnectionProfile());
        dataSourceDo.setConnectionProfileHashedString(connectionProfileHashedString);
        dataSourceDo.setDatabaseServerUid(databaseServerDo.getUid());
        BaseDo.create(dataSourceDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dataSourceRepository.save(dataSourceDo);

        //
        // Step 3, post-processing
        //
        asyncRetrieveMetadataOfDataSource(dataSourceDo.getUid(), operatingUserProfile);

        DataSourceDto dataSourceDto = new DataSourceDto();
        BeanUtils.copyProperties(dataSourceDo, dataSourceDto);
        return dataSourceDto;
    }

    /**
     * 获取指定 Data Source
     *
     * @param dataSourceUid
     * @param operatingUserProfile
     * @return
     * @throws AbcUndefinedException
     */
    @Override
    public DataSourceDto getDataSource(
            Long dataSourceUid,
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
        // step 2, core-processing
        //
        ConnectivityHandler objectiveConnectivityHandler = null;
        Map<String, ConnectivityHandler> map = this.applicationContext.getBeansOfType(ConnectivityHandler.class);
        if (!CollectionUtils.isEmpty(map)) {
            for (Map.Entry<String, ConnectivityHandler> entry : map.entrySet()) {
                ConnectivityHandler connectivityHandler = entry.getValue();
                if (connectivityHandler.type().equals(dataSourceDo.getType())) {
                    objectiveConnectivityHandler = connectivityHandler;
                    break;
                }
            }
        }
        if (objectiveConnectivityHandler == null) {
            throw new AbcResourceConflictException(
                    String.format("cannot find connectivity handler of data source type:%s",
                            dataSourceDo.getType()));
        }
        // exclude confidential information in the connection profile
        JSONObject transformedConnectionProfile =
                objectiveConnectivityHandler.excludeConfidentialInformation(dataSourceDo.getConnectionProfile());

        //
        // Step 3, post-processing
        //
        DataSourceDto dataSourceDto = new DataSourceDto();
        BeanUtils.copyProperties(dataSourceDo, dataSourceDto);
        dataSourceDto.setConnectionProfile(transformedConnectionProfile);
        return dataSourceDto;
    }

    /**
     * 更新指定 Data Source
     *
     * @param dataSourceUid
     * @param updateDataSourceDto
     * @param operatingUserProfile
     * @throws AbcUndefinedException
     */
    @Override
    public void updateDataSource(
            Long dataSourceUid,
            UpdateDataSourceDto updateDataSourceDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, preprocessing
        //
        DataSourceDo dataSourceDo = this.dataSourceRepository.findByUid(dataSourceUid);
        if (dataSourceDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataSourceDo.RESOURCE_SYMBOL,
                    dataSourceUid));
        }

        boolean requiredToUpdate = false;

        //
        // step 1.1, 不能有 name 相同的 Data Source
        //
        if (!ObjectUtils.isEmpty(updateDataSourceDto.getName())
                && !updateDataSourceDto.getName().equalsIgnoreCase(dataSourceDo.getName())) {
            boolean existsDuplicate = this.dataSourceRepository.existsByName(updateDataSourceDto.getName());
            if (existsDuplicate) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s", DataSourceDo.RESOURCE_SYMBOL,
                        updateDataSourceDto.getName()));
            }

            //
            dataSourceDo.setName(updateDataSourceDto.getName());
            dataSourceDo.setObjectName(updateDataSourceDto.getName()
                    .replaceAll("_", "__")
                    .replaceAll("\\s", "_")
                    .toLowerCase());
            requiredToUpdate = true;
        }

        //
        // Step 1.2, 也不能有 connection profile 相同的 Data source
        //

        boolean connectionProfileUpdated = false;
        if (updateDataSourceDto.getConnectionProfile() != null) {
            String connectionProfileHashedString =
                    Base64Utils.encodeToString(updateDataSourceDto.getConnectionProfile().toJSONString().getBytes(StandardCharsets.UTF_8));
            if (!connectionProfileHashedString.equals(dataSourceDo.getConnectionProfileHashedString())) {
                boolean existsDuplicate =
                        this.dataSourceRepository.existsByConnectionProfileHashedString(connectionProfileHashedString);
                if (existsDuplicate) {
                    throw new AbcResourceDuplicateException(String.format("%s::connection profile", DataSourceDo.RESOURCE_SYMBOL));
                }

                connectionProfileUpdated = true;

                //
                dataSourceDo.setConnectionProfile(updateDataSourceDto.getConnectionProfile());
                dataSourceDo.setConnectionProfileHashedString(connectionProfileHashedString);
                requiredToUpdate = true;
            }
        }

        if (connectionProfileUpdated) {
            //
            // step 1.3, test connectivity
            //
            ConnectivityHandler objectiveConnectivityHandler = null;
            Map<String, ConnectivityHandler> map = this.applicationContext.getBeansOfType(ConnectivityHandler.class);
            if (!CollectionUtils.isEmpty(map)) {
                for (Map.Entry<String, ConnectivityHandler> entry : map.entrySet()) {
                    ConnectivityHandler connectivityHandler = entry.getValue();
                    if (connectivityHandler.type().equals(dataSourceDo.getType())) {
                        objectiveConnectivityHandler = connectivityHandler;
                        break;
                    }
                }
            }
            if (objectiveConnectivityHandler == null) {
                throw new AbcResourceConflictException(
                        String.format("cannot find connectivity handler of data source type:%s",
                                dataSourceDo.getType()));
            }
            // perform connectivity test
            objectiveConnectivityHandler.testConnectivity(updateDataSourceDto.getConnectionProfile());

            //
            // Step 1.4, 找出或创建 database server
            //
            JSONObject hostProfileOfDatabaseServer =
                    objectiveConnectivityHandler.extractHostProfile(updateDataSourceDto.getConnectionProfile());
            String hashedHostProfileOfDatabaseServer =
                    DigestUtils.md5DigestAsHex(hostProfileOfDatabaseServer.toJSONString().getBytes(StandardCharsets.UTF_8));
            DatabaseServerDo databaseServerDo =
                    this.databaseServerRepository.findByTypeAndHashedHostProfile(dataSourceDo.getType(),
                            hashedHostProfileOfDatabaseServer);
            if (databaseServerDo == null) {
                databaseServerDo = new DatabaseServerDo();
                databaseServerDo.setUid(this.idHelper.getNextDistributedId(DatabaseServerDo.RESOURCE_NAME));
                databaseServerDo.setHashedHostProfile(hashedHostProfileOfDatabaseServer);
                databaseServerDo.setHostProfile(hostProfileOfDatabaseServer);
                databaseServerDo.setType(dataSourceDo.getType());
                BaseDo.create(databaseServerDo, operatingUserProfile.getUid(), LocalDateTime.now());
                this.databaseServerRepository.save(databaseServerDo);
            }

            if (!dataSourceDo.getDatabaseServerUid().equals(databaseServerDo.getUid())) {
                dataSourceDo.setDatabaseServerUid(databaseServerDo.getUid());
                requiredToUpdate = true;
            }
        }

        //
        // Step 1.5, 其它变化
        //
        if (updateDataSourceDto.getDescription() != null
                && !updateDataSourceDto.getDescription().equalsIgnoreCase(dataSourceDo.getDescription())) {
            dataSourceDo.setDescription(updateDataSourceDto.getDescription());
            requiredToUpdate = true;
        }

        if (requiredToUpdate) {
            BaseDo.update(dataSourceDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.dataSourceRepository.save(dataSourceDo);
        }
    }

    /**
     * 列出针对指定 Data Source 的所有引用
     *
     * @param dataSourceUid
     * @param operatingUserProfile
     * @return
     * @throws AbcUndefinedException
     */
    @Override
    public List<String> listAllReferencesToDataSource(
            Long dataSourceUid,
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
        // Step 2, core-processing
        //
        return this.resourceReferenceManager.check(
                ResourceReferenceManager.ResourceCategoryEnum.DATA_SOURCE,
                dataSourceDo.getUid(),
                dataSourceDo.getName());
    }

    /**
     * 删除指定 Data Source
     *
     * @param dataSourceUid
     * @param operatingUserProfile
     * @throws AbcUndefinedException
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteDataSource(
            Long dataSourceUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DataSourceDo dataSourceDo = this.dataSourceRepository.findByUid(dataSourceUid);
        if (dataSourceDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataSourceDo.RESOURCE_SYMBOL, dataSourceUid));
        }

        //
        // Step 2, core-processing
        //

        // 发布通知，所有引用方需要响应通知
        DataSourceDeletedEvent dataSourceDeletedEvent = new DataSourceDeletedEvent();
        dataSourceDeletedEvent.setDataSourceDo(dataSourceDo);
        dataSourceDeletedEvent.setOperatingUserProfile(operatingUserProfile);
        this.eventBusManager.send(dataSourceDeletedEvent);

        // 如果 data source 所属的 database server 已经没有任何 data source，则也删除 database server
        if (dataSourceDo.getDatabaseServerUid() != null) {
            Integer count = this.dataSourceRepository.countByDatabaseServerUid(dataSourceDo.getDatabaseServerUid());
            if (count != null && count <= 1) {
                DatabaseServerDo databaseServerDo =
                        this.databaseServerRepository.findByUid(dataSourceDo.getDatabaseServerUid());
                if (databaseServerDo != null) {
                    databaseServerDo.setDeleted(Boolean.TRUE);
                    BaseDo.update(databaseServerDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.databaseServerRepository.save(databaseServerDo);
                }
            }
        }

        //
        dataSourceDo.setDeleted(Boolean.TRUE);
        BaseDo.update(dataSourceDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dataSourceRepository.save(dataSourceDo);

        //
        // Step 3, post-processing
        //
    }

    /**
     * 分页查询 Data Sources
     *
     * @param dataSourceUid
     * @param dataSourceName
     * @param dataSourceType
     * @param pageable
     * @param operatingUserProfile
     * @return
     * @throws AbcUndefinedException
     */
    @Override
    public Page<DataSourceDto> pagingQueryDataSources(
            Long dataSourceUid,
            String dataSourceName,
            DatabaseServerTypeEnum dataSourceType,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Specification<DataSourceDo> specification = new Specification<DataSourceDo>() {
            @Override
            public Predicate toPredicate(Root<DataSourceDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (dataSourceUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("uid"), dataSourceUid));
                }
                if (!ObjectUtils.isEmpty(dataSourceName)) {
                    predicateList.add(criteriaBuilder.like(root.get("name"), "%" + dataSourceName + "%"));
                }
                if (dataSourceType != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("type"), dataSourceType));
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        Page<DataSourceDo> itemDoPage = this.dataSourceRepository.findAll(specification, pageable);
        List<DataSourceDto> content = new ArrayList<>(itemDoPage.getSize());
        itemDoPage.forEach(itemDo -> {
            DataSourceDto itemDto = new DataSourceDto();
            BeanUtils.copyProperties(itemDo, itemDto);

            // 隐藏敏感信息
            itemDto.setConnectionProfile(null);

            content.add(itemDto);
        });
        Page<DataSourceDto> itemDtoPage = new PageImpl<>(content, pageable, itemDoPage.getTotalElements());
        return itemDtoPage;
    }

    /**
     * 列表查询 Data Sources
     *
     * @param dataSourceUid
     * @param dataSourceName
     * @param dataSourceType
     * @param sort
     * @param operatingUserProfile
     * @return
     * @throws AbcUndefinedException
     */
    @Override
    public List<DataSourceDto> listingQueryDataSources(
            Long dataSourceUid,
            String dataSourceName,
            DatabaseServerTypeEnum dataSourceType,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Specification<DataSourceDo> specification = new Specification<DataSourceDo>() {
            @Override
            public Predicate toPredicate(Root<DataSourceDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (dataSourceUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("uid"), dataSourceUid));
                }
                if (!ObjectUtils.isEmpty(dataSourceName)) {
                    predicateList.add(criteriaBuilder.like(root.get("name"), "%" + dataSourceName + "%"));
                }
                if (dataSourceType != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("type"), dataSourceType));
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        List<DataSourceDo> itemDoList = this.dataSourceRepository.findAll(specification, sort);
        if (CollectionUtils.isEmpty(itemDoList)) {
            return null;
        }
        List<DataSourceDto> content = new ArrayList<>(itemDoList.size());
        itemDoList.forEach(itemDo -> {
            DataSourceDto itemDto = new DataSourceDto();
            BeanUtils.copyProperties(itemDo, itemDto);

            // 隐藏敏感信息
            itemDto.setConnectionProfile(null);

            content.add(itemDto);
        });
        return content;
    }

    /**
     * 重新获取指定 Data Source 的 Metadata，包括 tables, views, columns, and indexes
     *
     * @param dataSourceUid
     * @param operatingUserProfile
     * @throws AbcUndefinedException
     */
    @Override
    public Long asyncRetrieveMetadataOfDataSource(
            Long dataSourceUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        DataSourceDo dataSourceDo = this.dataSourceRepository.findByUid(dataSourceUid);
        if (dataSourceDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataSourceDo.RESOURCE_SYMBOL, dataSourceUid));
        }
        return this.dataSourceMetadataRetrievalHandler.retrieveMetadata(
                dataSourceDo, operatingUserProfile);
    }

    @Override
    public JobStatusEnum getTaskStatusOfAsyncRetrieveMetadataOfDataSource(
            Long taskUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        DataSourceMetadataRetrievalInstanceDo dataSourceMetadataRetrievalInstanceDo =
                this.dataSourceMetadataRetrievalInstanceRepository.findByUid(taskUid);
        if (dataSourceMetadataRetrievalInstanceDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DataSourceMetadataRetrievalInstanceDo.RESOURCE_SYMBOL, taskUid));
        }

        return dataSourceMetadataRetrievalInstanceDo.getStatus();
    }

    /**
     * 针对指定 Data Source 测试 Query Statement
     *
     * @param dataSourceUid
     * @param testQueryStatementDto
     * @param operatingUserProfile
     * @return
     */
    @Override
    public QueryResult testQueryStatement(
            Long dataSourceUid,
            TestQueryStatementDto testQueryStatementDto,
            UserProfile operatingUserProfile) {
        //
        // step 1, pre-processing
        //
        DataSourceDo dataSourceDo = this.dataSourceRepository.findByUid(dataSourceUid);
        if (dataSourceDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataSourceDo.RESOURCE_SYMBOL,
                    dataSourceUid));
        }

        //
        // step 2, core-processing
        //

        // verify statement
        QueryBuilder objectiveQueryBuilder = null;
        Map<String, QueryBuilder> queryBuilderMap = this.applicationContext.getBeansOfType(QueryBuilder.class);
        if (!CollectionUtils.isEmpty(queryBuilderMap)) {
            for (Map.Entry<String, QueryBuilder> entry : queryBuilderMap.entrySet()) {
                QueryBuilder queryBuilder = entry.getValue();
                if (queryBuilder.type().equals(dataSourceDo.getType())) {
                    objectiveQueryBuilder = queryBuilder;
                    break;
                }
            }
        }
        if (objectiveQueryBuilder == null) {
            throw new AbcResourceConflictException(
                    String.format("cannot find query builder of data source type:%s",
                            dataSourceDo.getType()));
        }
        objectiveQueryBuilder.verifyStatement(testQueryStatementDto.getQueryStatement());

        // perform query
        DmlHandler objectiveDmlHandler = null;
        Map<String, DmlHandler> dmlHandlerMap = this.applicationContext.getBeansOfType(DmlHandler.class);
        if (!CollectionUtils.isEmpty(dmlHandlerMap)) {
            for (Map.Entry<String, DmlHandler> entry : dmlHandlerMap.entrySet()) {
                DmlHandler dmlHandler = entry.getValue();
                if (dmlHandler.type().equals(dataSourceDo.getType())) {
                    objectiveDmlHandler = dmlHandler;
                    break;
                }
            }
        }
        if (objectiveDmlHandler == null) {
            throw new AbcResourceConflictException(
                    String.format("cannot find dml handler of data source type:%s",
                            dataSourceDo.getType()));
        }
        QueryResult queryResult = objectiveDmlHandler.testQuery(dataSourceDo.getConnectionProfile(),
                testQueryStatementDto.getQueryStatement(), testQueryStatementDto.getLimit());

        //
        // step 3, post-processing
        //
        return queryResult;
    }

    /**
     * 针对指定 Data Source 测试 Query Statement
     *
     * @param dataSourceUid
     * @param queryStatement
     * @param operatingUserProfile
     * @return
     */
    @Override
    public void validateQueryStatement(
            Long dataSourceUid,
            String queryStatement,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // step 1, pre-processing
        //
        DataSourceDo dataSourceDo = this.dataSourceRepository.findByUid(dataSourceUid);
        if (dataSourceDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataSourceDo.RESOURCE_SYMBOL,
                    dataSourceUid));
        }

        //
        // step 2, core-processing
        //

        // verify statement
        QueryBuilder objectiveQueryBuilder = null;
        Map<String, QueryBuilder> queryBuilderMap = this.applicationContext.getBeansOfType(QueryBuilder.class);
        if (!CollectionUtils.isEmpty(queryBuilderMap)) {
            for (Map.Entry<String, QueryBuilder> entry : queryBuilderMap.entrySet()) {
                QueryBuilder queryBuilder = entry.getValue();
                if (queryBuilder.type().equals(dataSourceDo.getType())) {
                    objectiveQueryBuilder = queryBuilder;
                    break;
                }
            }
        }
        if (objectiveQueryBuilder == null) {
            throw new AbcResourceConflictException(
                    String.format("cannot find query builder of data source type:%s",
                            dataSourceDo.getType()));
        }
        objectiveQueryBuilder.verifyStatement(queryStatement);
    }
}
