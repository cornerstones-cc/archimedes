package cc.cornerstones.biz.datatable.service.impl;

import cc.cornerstones.almond.constants.InfrastructureConstants;
import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.exceptions.*;
import cc.cornerstones.almond.types.*;
import cc.cornerstones.almond.utils.AbcStringUtils;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.datasource.entity.DataSourceDo;
import cc.cornerstones.biz.datasource.persistence.DataSourceRepository;
import cc.cornerstones.biz.datasource.service.assembly.database.DataTableMetadata;
import cc.cornerstones.biz.datasource.service.assembly.database.DmlHandler;
import cc.cornerstones.biz.datasource.service.assembly.database.ParseResult;
import cc.cornerstones.biz.datasource.service.assembly.database.QueryResult;
import cc.cornerstones.biz.datasource.share.constants.DataColumnTypeEnum;
import cc.cornerstones.biz.datasource.share.constants.DataTableTypeEnum;
import cc.cornerstones.biz.datatable.dto.CreateIndirectDataTableDto;
import cc.cornerstones.biz.datatable.dto.DataTableDto;
import cc.cornerstones.biz.datatable.dto.UpdateIndirectDataTableDto;
import cc.cornerstones.biz.datatable.entity.ContextPathMetadataRetrievalInstanceDo;
import cc.cornerstones.biz.datatable.entity.DataColumnDo;
import cc.cornerstones.biz.datatable.entity.DataTableDo;
import cc.cornerstones.biz.datatable.entity.DataTableMetadataRetrievalInstanceDo;
import cc.cornerstones.biz.datatable.persistence.ContextPathMetadataRetrievalInstanceRepository;
import cc.cornerstones.biz.datatable.persistence.DataColumnRepository;
import cc.cornerstones.biz.datatable.persistence.DataTableMetadataRetrievalInstanceRepository;
import cc.cornerstones.biz.datatable.persistence.DataTableRepository;
import cc.cornerstones.biz.datatable.service.assembly.DataTableMetadataRetrievalHandler;
import cc.cornerstones.biz.datatable.service.assembly.ContextPathMetadataRetrievalHandler;
import cc.cornerstones.biz.datatable.service.assembly.DirectDataTableHandler;
import cc.cornerstones.biz.datatable.service.assembly.IndirectDataTableHandler;
import cc.cornerstones.biz.datatable.service.inf.DataColumnService;
import cc.cornerstones.biz.datatable.service.inf.DataTableService;
import cc.cornerstones.biz.share.event.*;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.PostConstruct;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class DataTableServiceImpl implements DataTableService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataTableServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private DataTableRepository dataTableRepository;

    @Autowired
    private DataColumnRepository dataColumnRepository;

    @Autowired
    private DirectDataTableHandler directDataTableHandler;

    @Autowired
    private IndirectDataTableHandler indirectDataTableHandler;

    @Autowired
    private DataColumnService dataColumnService;

    @Autowired
    private DataTableMetadataRetrievalHandler directDataTableMetadataRetrievalHandler;

    @Autowired
    private DataTableMetadataRetrievalInstanceRepository dataTableMetadataRetrievalInstanceRepository;

    @Autowired
    private ContextPathMetadataRetrievalHandler contextPathMetadataRetrievalHandler;

    @Autowired
    private ContextPathMetadataRetrievalInstanceRepository contextPathMetadataRetrievalInstanceRepository;

    /**
     * 为指定 Data Source 创建 Indirect Data Table
     *
     * @param dataSourceUid
     * @param createIndirectDataTableDto
     * @param operatingUserProfile
     * @return
     * @throws AbcUndefinedException
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public DataTableDto createIndirectDataTable(
            Long dataSourceUid,
            CreateIndirectDataTableDto createIndirectDataTableDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // step 1, pre-processing
        //
        DataSourceDo dataSourceDo = this.dataSourceRepository.findByUid(dataSourceUid);
        if (dataSourceDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataSourceDo.RESOURCE_SYMBOL,
                    dataSourceUid));
        }

        // 同一 data source 内，同样的 context path + name 不能有重复的
        Integer duplicate =
                this.dataTableRepository.countByDataSourceUidAndNameWithNullContextPath(
                        dataSourceUid, createIndirectDataTableDto.getName());
        if (duplicate != null && duplicate.intValue() > 0) {
            throw new AbcResourceDuplicateException(String.format("%s::name=%s, data_source_uid=%d, null context_path", DataTableDo.RESOURCE_SYMBOL,
                    createIndirectDataTableDto.getName(), dataSourceUid));
        }

        // parse building logic
        AbcTuple3<List<Long>, List<String>, Map<String, DataColumnTypeEnum>> buildingLogicParsedResult =
                parseBuildingLogic(dataSourceDo, createIndirectDataTableDto.getBuildingLogic());
        List<Long> underlyingDataTableUidList = buildingLogicParsedResult.f;
        List<String> columnNames = buildingLogicParsedResult.s;
        Map<String, DataColumnTypeEnum> columnNameAndDataColumnTypeMap = buildingLogicParsedResult.t;

        //
        // step 2, core-processing
        //

        // step 2.1, create data table (indirect)
        DataTableDo dataTableDo = new DataTableDo();
        dataTableDo.setUid(this.idHelper.getNextDistributedId(DataTableDo.RESOURCE_NAME));
        dataTableDo.setName(createIndirectDataTableDto.getName());
        dataTableDo.setObjectName(createIndirectDataTableDto.getName()
                .replaceAll("_", "__")
                .replaceAll("\\s", "_")
                .toLowerCase());
        dataTableDo.setDescription(createIndirectDataTableDto.getDescription());
        dataTableDo.setType(DataTableTypeEnum.INDIRECT_TABLE);
        dataTableDo.setBuildingLogic(createIndirectDataTableDto.getBuildingLogic());
        dataTableDo.setUnderlyingDataTableUidList(underlyingDataTableUidList);
        dataTableDo.setBuildNumber(System.currentTimeMillis());
        dataTableDo.setDataSourceUid(dataSourceUid);
        BaseDo.create(dataTableDo, operatingUserProfile.getUid(), dataSourceDo.getOwner(), LocalDateTime.now());
        this.dataTableRepository.save(dataTableDo);

        // step 2.2, create data columns
        LocalDateTime now = LocalDateTime.now();
        List<DataColumnDo> dataColumnDoList = new ArrayList<>(columnNames.size());
        for (int i = 0; i < columnNames.size(); i++) {
            String columnName = columnNames.get(i);

            DataColumnDo dataColumnDo = new DataColumnDo();
            dataColumnDo.setUid(this.idHelper.getNextDistributedId(DataColumnDo.RESOURCE_NAME));
            dataColumnDo.setName(columnName);
            dataColumnDo.setObjectName(columnName
                    .replaceAll("_", "__")
                    .replaceAll("\\s", "_")
                    .toLowerCase());
            dataColumnDo.setType(columnNameAndDataColumnTypeMap.get(columnName));
            if (dataColumnDo.getType() == null) {
                dataColumnDo.setType(DataColumnTypeEnum.TEXT);
            }
            dataColumnDo.setOrdinalPosition(i * 1.0f);
            dataColumnDo.setDataTableUid(dataTableDo.getUid());
            dataColumnDo.setDataSourceUid(dataTableDo.getDataSourceUid());
            BaseDo.create(dataColumnDo, operatingUserProfile.getUid(), now);
            dataColumnDoList.add(dataColumnDo);
        }
        this.dataColumnRepository.saveAll(dataColumnDoList);

        //
        // step 3, post-processing
        //
        DataTableDto dataTableDto = new DataTableDto();
        BeanUtils.copyProperties(dataTableDo, dataTableDto);
        return dataTableDto;
    }

    private AbcTuple3<List<Long>, List<String>, Map<String, DataColumnTypeEnum>> parseBuildingLogic(
            DataSourceDo dataSourceDo,
            String buildingLogic) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //


        //
        // Step 2, core-processing
        //

        //
        // Step 2.1, parse query
        //
        ParseResult parseResult = this.indirectDataTableHandler.parseQuery(dataSourceDo,
                buildingLogic);
        if (CollectionUtils.isEmpty(parseResult.getTables())) {
            throw new AbcIllegalParameterException("cannot parse underlying data table(s)");
        }
        List<Long> underlyingDataTableUidList = new LinkedList<>();
        for (ParseResult.Table table : parseResult.getTables()) {
            String objectiveTableName = table.getName();
            List<String> objectiveContextPath = table.getContextPath();
            List<DataTableDo> availableDataTableDoList =
                    this.dataTableRepository.findByDataSourceUidAndName(dataSourceDo.getUid(),
                            objectiveTableName);
            if (CollectionUtils.isEmpty(availableDataTableDoList)) {
                throw new AbcIllegalParameterException(String.format("cannot find data table:%s", objectiveTableName));
            }
            DataTableDo objectiveDataTableDo = null;
            for (DataTableDo availableDataTableDo : availableDataTableDoList) {
                List<String> availableContextPath = availableDataTableDo.getContextPath();

                if (objectiveContextPath.size() != availableContextPath.size()) {
                    continue;
                }

                boolean equal = true;
                for (int i = 0; i < objectiveContextPath.size(); i++) {
                    if (!objectiveContextPath.get(i).equals(availableContextPath.get(i))) {
                        equal = false;
                        break;
                    }
                }

                if (equal) {
                    objectiveDataTableDo = availableDataTableDo;
                    break;
                }
            }
            if (objectiveDataTableDo == null) {
                throw new AbcIllegalParameterException(String.format("cannot find data table:%s, %s",
                        objectiveTableName, AbcStringUtils.toString(objectiveContextPath, ",")));
            }
            underlyingDataTableUidList.add(objectiveDataTableDo.getUid());
        }

        //
        // Step 2.2, test query
        //
        QueryResult queryResult = this.indirectDataTableHandler.testQuery(dataSourceDo,
                buildingLogic, 100);
        if (queryResult == null || CollectionUtils.isEmpty(queryResult.getColumnNames())) {
            throw new AbcIllegalParameterException("failed to build indirect table, no result data found");
        }
        // infer the data column type from the sample data
        Map<String, DataColumnTypeEnum> columnNameAndDataColumnTypeMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(queryResult.getRows())) {
            Map<String, List<DataColumnTypeEnum>> statistics = new HashMap<>();
            queryResult.getRows().forEach(row -> {
                row.forEach((key, value) -> {
                    if (value == null) {
                        return;
                    }
                    if (!statistics.containsKey(key)) {
                        statistics.put(key, new LinkedList<>());
                    }
                    if (value instanceof String) {
                        statistics.get(key).add(DataColumnTypeEnum.TEXT);
                    } else if (value instanceof Boolean) {
                        statistics.get(key).add(DataColumnTypeEnum.BOOLEAN);
                    } else if (value instanceof Byte) {
                        statistics.get(key).add(DataColumnTypeEnum.CHAR);
                    } else if (value instanceof Short) {
                        statistics.get(key).add(DataColumnTypeEnum.SMALLINT);
                    } else if (value instanceof Integer) {
                        statistics.get(key).add(DataColumnTypeEnum.INT);
                    } else if (value instanceof Long) {
                        statistics.get(key).add(DataColumnTypeEnum.LONG);
                    } else if (value instanceof BigDecimal) {
                        statistics.get(key).add(DataColumnTypeEnum.DECIMAL);
                    } else if (value instanceof Float) {
                        statistics.get(key).add(DataColumnTypeEnum.DECIMAL);
                    } else if (value instanceof Double) {
                        statistics.get(key).add(DataColumnTypeEnum.DECIMAL);
                    } else if (value instanceof java.util.Date) {
                        statistics.get(key).add(DataColumnTypeEnum.DATETIME);
                    } else if (value instanceof java.sql.Date) {
                        statistics.get(key).add(DataColumnTypeEnum.DATE);
                    } else if (value instanceof java.sql.Timestamp) {
                        statistics.get(key).add(DataColumnTypeEnum.TIMESTAMP);
                    } else if (value instanceof LocalDate) {
                        statistics.get(key).add(DataColumnTypeEnum.DATE);
                    } else if (value instanceof LocalDateTime) {
                        statistics.get(key).add(DataColumnTypeEnum.DATETIME);
                    } else if (value instanceof LocalTime) {
                        statistics.get(key).add(DataColumnTypeEnum.TIME);
                    } else if (value instanceof Character) {
                        statistics.get(key).add(DataColumnTypeEnum.CHAR);
                    } else {
                        LOGGER.warn("unsupported data type, data value is:{}", value);
                        statistics.get(key).add(DataColumnTypeEnum.TEXT);
                    }
                });
            });

            statistics.forEach((columnName, types) -> {
                Map<DataColumnTypeEnum, Integer> count = new HashMap<>();
                types.forEach(type -> {
                    if (!count.containsKey(type)) {
                        count.put(type, 1);
                    } else {
                        count.put(type, count.get(type) + 1);
                    }
                });

                DataColumnTypeEnum maxDataColumnType = null;
                Integer maxCount = null;
                for (Map.Entry<DataColumnTypeEnum, Integer> entry : count.entrySet()) {
                    if (maxCount == null) {
                        maxDataColumnType = entry.getKey();
                        maxCount = entry.getValue();
                    } else {
                        if (entry.getValue() > maxCount) {
                            maxDataColumnType = entry.getKey();
                            maxCount = entry.getValue();
                        }
                    }
                }

                columnNameAndDataColumnTypeMap.put(columnName, maxDataColumnType);
            });
        }

        return new AbcTuple3<>(underlyingDataTableUidList, queryResult.getColumnNames(),
                columnNameAndDataColumnTypeMap);
    }

    /**
     * 重建 Indirect Data Table
     *
     * @param dataTableDo
     * @param operatingUserProfile
     */
    @Transactional(rollbackFor = Exception.class)
    private void rebuildIndirectDataTable(
            DataTableDo dataTableDo,
            UserProfile operatingUserProfile) {
        //
        // step 1, pre-processing
        //
        Long dataSourceUid = dataTableDo.getDataSourceUid();
        DataSourceDo dataSourceDo = this.dataSourceRepository.findByUid(dataSourceUid);
        if (dataSourceDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataSourceDo.RESOURCE_SYMBOL,
                    dataSourceUid));
        }

        // parse building logic
        AbcTuple3<List<Long>, List<String>, Map<String, DataColumnTypeEnum>> buildingLogicParsedResult =
                parseBuildingLogic(dataSourceDo, dataTableDo.getBuildingLogic());
        List<Long> underlyingDataTableUidList = buildingLogicParsedResult.f;
        List<String> columnNames = buildingLogicParsedResult.s;
        Map<String, DataColumnTypeEnum> columnNameAndDataColumnTypeMap = buildingLogicParsedResult.t;

        //
        // step 2, core-processing
        //

        // step 2.1, update data table (indirect)

        // step 2.2, create/update/delete data columns
        List<DataColumnDo> existingDataColumnDoList =
                this.dataColumnRepository.findByDataTableUid(dataTableDo.getUid());
        if (existingDataColumnDoList == null) {
            existingDataColumnDoList = new LinkedList<>();
        }
        Map<String, DataColumnDo> existingMap = new HashMap<>();
        existingDataColumnDoList.forEach(dataColumnDo -> {
            existingMap.put(dataColumnDo.getName(), dataColumnDo);
        });

        List<DataColumnDo> latestFullList = new LinkedList<>();
        List<DataColumnDo> toCreateList = new LinkedList<>();
        List<DataColumnDo> toUpdateList = new LinkedList<>();
        List<DataColumnDo> toDeleteList = new LinkedList<>();
        LocalDateTime now = LocalDateTime.now();
        existingMap.forEach((key, existingItem) -> {
            if (columnNames.contains(key)) {
                // existing 有，input 有
                // 继续检查，看是否有修改场景

                boolean requiredToUpdate = false;
                if (existingItem.getType() != null) {
                    if (!existingItem.getType().equals(columnNameAndDataColumnTypeMap.get(key))) {
                        existingItem.setType(columnNameAndDataColumnTypeMap.get(key));
                        requiredToUpdate = true;
                    }
                }
                int ordinalPosition = columnNames.indexOf(key);
                if (existingItem.getOrdinalPosition() != null) {
                    if (existingItem.getOrdinalPosition().intValue() != ordinalPosition) {
                        existingItem.setOrdinalPosition(ordinalPosition * 1.0f);
                        requiredToUpdate = true;
                    }
                }
                if (requiredToUpdate) {
                    BaseDo.update(existingItem, operatingUserProfile.getUid(), now);
                    toUpdateList.add(existingItem);
                }

                latestFullList.add(existingItem);
            } else {
                // existing 有，input 没有
                // 删除场景
                existingItem.setDeleted(Boolean.TRUE);
                BaseDo.update(existingItem, operatingUserProfile.getUid(), now);
                toDeleteList.add(existingItem);
            }
        });
        for (int i = 0; i < columnNames.size(); i++) {
            String columnName = columnNames.get(i);
            if (!existingMap.containsKey(columnName)) {
                // input 有，existing 没有
                // 新增场景
                DataColumnDo dataColumnDo = new DataColumnDo();
                dataColumnDo.setUid(this.idHelper.getNextDistributedId(DataColumnDo.RESOURCE_NAME));
                dataColumnDo.setName(columnName);
                dataColumnDo.setObjectName(columnName.replaceAll("_", "__")
                        .replaceAll("\\s", "_")
                        .toLowerCase());
                dataColumnDo.setType(columnNameAndDataColumnTypeMap.get(columnName));
                dataColumnDo.setOrdinalPosition(i * 1.0f);
                dataColumnDo.setDataTableUid(dataTableDo.getUid());
                dataColumnDo.setDataSourceUid(dataTableDo.getDataSourceUid());
                BaseDo.create(dataColumnDo, operatingUserProfile.getUid(), now);
                toCreateList.add(dataColumnDo);

                latestFullList.add(dataColumnDo);
            }
        }

        boolean anythingChanged = false;
        if (!CollectionUtils.isEmpty(toCreateList)) {
            this.dataColumnRepository.saveAll(toCreateList);
            anythingChanged = true;
        }
        if (!CollectionUtils.isEmpty(toUpdateList)) {
            this.dataColumnRepository.saveAll(toUpdateList);
            anythingChanged = true;
        }
        if (!CollectionUtils.isEmpty(toDeleteList)) {
            this.dataColumnRepository.saveAll(toDeleteList);
            anythingChanged = true;
        }
        if (anythingChanged) {
            //
            dataTableDo.setBuildNumber(System.currentTimeMillis());
            BaseDo.update(dataTableDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
            this.dataTableRepository.save(dataTableDo);
        }

        // event post
        IndirectDataTableStructureRefreshedEvent indirectDataTableStructureRefreshedEvent =
                new IndirectDataTableStructureRefreshedEvent();
        indirectDataTableStructureRefreshedEvent.setDataTableDo(dataTableDo);
        indirectDataTableStructureRefreshedEvent.setDataColumnDoList(latestFullList);
        indirectDataTableStructureRefreshedEvent.setOperatingUserProfile(operatingUserProfile);
        this.eventBusManager.send(indirectDataTableStructureRefreshedEvent);

        //
        // step 3, post-processing
        //

    }

    /**
     * 更新指定 Data Table (Indirect)
     *
     * @param dataTableUid
     * @param updateIndirectDataTableDto
     * @param operatingUserProfile
     * @throws AbcUndefinedException
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateIndirectDataTable(
            Long dataTableUid,
            UpdateIndirectDataTableDto updateIndirectDataTableDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // step 1, pre-processing
        //
        DataTableDo dataTableDo = this.dataTableRepository.findByUid(dataTableUid);
        if (dataTableDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataTableDo.RESOURCE_SYMBOL,
                    dataTableUid));
        }

        if (!ObjectUtils.isEmpty(updateIndirectDataTableDto.getName())
                && !updateIndirectDataTableDto.getName().equals(dataTableDo.getName())) {
            // 同一 data source 内，同样的 context path + name 不能有重复的
            Integer duplicate =
                    this.dataTableRepository.countByDataSourceUidAndNameWithNullContextPath(
                            dataTableDo.getDataSourceUid(), updateIndirectDataTableDto.getName());
            if (duplicate != null && duplicate > 0) {
                throw new AbcResourceDuplicateException(String.format("%s::name:%s, data_source_uid:%d, null context_path", DataTableDo.RESOURCE_SYMBOL,
                        updateIndirectDataTableDto.getName(), dataTableDo.getDataSourceUid()));
            }
        }

        DataSourceDo dataSourceDo = this.dataSourceRepository.findByUid(dataTableDo.getDataSourceUid());
        if (dataSourceDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataSourceDo.RESOURCE_SYMBOL,
                    dataTableDo.getDataSourceUid()));
        }


        //
        // step 2, core-processing
        //

        boolean requiredToUpdateDataTable = false;

        if (!ObjectUtils.isEmpty(updateIndirectDataTableDto.getName())
                && !updateIndirectDataTableDto.getName().equals(dataTableDo.getName())) {
            dataTableDo.setName(updateIndirectDataTableDto.getName());
            dataTableDo.setObjectName(updateIndirectDataTableDto.getName()
                    .replaceAll("_", "__")
                    .replaceAll("\\s", "_")
                    .toLowerCase());
            requiredToUpdateDataTable = true;
        }

        if (updateIndirectDataTableDto.getDescription() != null
                && !updateIndirectDataTableDto.getDescription().equals(dataTableDo.getDescription())) {
            dataTableDo.setDescription(updateIndirectDataTableDto.getDescription());
            requiredToUpdateDataTable = true;
        }

        AbcTuple3<List<Long>, List<String>, Map<String, DataColumnTypeEnum>> buildingLogicParsedResult = null;
        if (!ObjectUtils.isEmpty(updateIndirectDataTableDto.getBuildingLogic())
                && !updateIndirectDataTableDto.getBuildingLogic().equals(dataTableDo.getBuildingLogic())) {
            // parse building logic
            buildingLogicParsedResult =
                    parseBuildingLogic(dataSourceDo, updateIndirectDataTableDto.getBuildingLogic());

            List<Long> underlyingDataTableUidList = buildingLogicParsedResult.f;

            dataTableDo.setBuildingLogic(updateIndirectDataTableDto.getBuildingLogic());
            dataTableDo.setUnderlyingDataTableUidList(underlyingDataTableUidList);

            requiredToUpdateDataTable = true;
        }

        if (requiredToUpdateDataTable) {
            dataTableDo.setBuildNumber(System.currentTimeMillis());
            BaseDo.update(dataTableDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.dataTableRepository.save(dataTableDo);
        }

        if (buildingLogicParsedResult != null) {
            List<Long> underlyingDataTableUidList = buildingLogicParsedResult.f;
            List<String> columnNames = buildingLogicParsedResult.s;
            Map<String, DataColumnTypeEnum> columnNameAndDataColumnTypeMap = buildingLogicParsedResult.t;

            // create/update/delete data columns
            List<DataColumnDo> existingDataColumnDoList =
                    this.dataColumnRepository.findByDataTableUid(dataTableDo.getUid());
            if (existingDataColumnDoList == null) {
                existingDataColumnDoList = new LinkedList<>();
            }
            Map<String, DataColumnDo> existingMap = new HashMap<>();
            existingDataColumnDoList.forEach(dataColumnDo -> {
                existingMap.put(dataColumnDo.getName(), dataColumnDo);
            });

            List<DataColumnDo> latestFullList = new LinkedList<>();
            List<DataColumnDo> toCreateList = new LinkedList<>();
            List<DataColumnDo> toUpdateList = new LinkedList<>();
            List<DataColumnDo> toDeleteList = new LinkedList<>();
            LocalDateTime now = LocalDateTime.now();
            existingMap.forEach((key, existingItem) -> {
                if (columnNames.contains(key)) {
                    // existing 有，input 有
                    // 继续检查，看是否有修改场景

                    boolean requiredToUpdateDataColumn = false;
                    if (existingItem.getType() != null) {
                        if (!existingItem.getType().equals(columnNameAndDataColumnTypeMap.get(key))) {
                            existingItem.setType(columnNameAndDataColumnTypeMap.get(key));
                            requiredToUpdateDataColumn = true;
                        }
                    }
                    int ordinalPosition = columnNames.indexOf(key);
                    if (existingItem.getOrdinalPosition() != null) {
                        if (existingItem.getOrdinalPosition().intValue() != ordinalPosition) {
                            existingItem.setOrdinalPosition(ordinalPosition * 1.0f);
                            requiredToUpdateDataColumn = true;
                        }
                    }
                    if (requiredToUpdateDataColumn) {
                        BaseDo.update(existingItem, operatingUserProfile.getUid(), now);
                        toUpdateList.add(existingItem);
                    }

                    latestFullList.add(existingItem);
                } else {
                    // existing 有，input 没有
                    // 删除场景
                    existingItem.setDeleted(Boolean.TRUE);
                    BaseDo.update(existingItem, operatingUserProfile.getUid(), now);
                    toDeleteList.add(existingItem);
                }
            });
            for (int i = 0; i < columnNames.size(); i++) {
                String columnName = columnNames.get(i);
                if (!existingMap.containsKey(columnName)) {
                    // input 有，existing 没有
                    // 新增场景
                    DataColumnDo dataColumnDo = new DataColumnDo();
                    dataColumnDo.setUid(this.idHelper.getNextDistributedId(DataColumnDo.RESOURCE_NAME));
                    dataColumnDo.setName(columnName);
                    dataColumnDo.setObjectName(columnName
                            .replaceAll("_", "__")
                            .replaceAll("\\s", "_")
                            .toLowerCase());
                    dataColumnDo.setType(columnNameAndDataColumnTypeMap.get(columnName));
                    dataColumnDo.setOrdinalPosition(i * 1.0f);
                    dataColumnDo.setDataTableUid(dataTableDo.getUid());
                    dataColumnDo.setDataSourceUid(dataTableDo.getDataSourceUid());
                    BaseDo.create(dataColumnDo, operatingUserProfile.getUid(), now);
                    toCreateList.add(dataColumnDo);

                    latestFullList.add(dataColumnDo);
                }
            }

            boolean anythingChanged = false;
            if (!CollectionUtils.isEmpty(toCreateList)) {
                this.dataColumnRepository.saveAll(toCreateList);
                anythingChanged = true;
            }
            if (!CollectionUtils.isEmpty(toUpdateList)) {
                this.dataColumnRepository.saveAll(toUpdateList);
                anythingChanged = true;
            }
            if (!CollectionUtils.isEmpty(toDeleteList)) {
                this.dataColumnRepository.saveAll(toDeleteList);
                anythingChanged = true;
            }

            // event post
            IndirectDataTableStructureRefreshedEvent indirectDataTableStructureRefreshedEvent =
                    new IndirectDataTableStructureRefreshedEvent();
            indirectDataTableStructureRefreshedEvent.setDataTableDo(dataTableDo);
            indirectDataTableStructureRefreshedEvent.setDataColumnDoList(latestFullList);
            indirectDataTableStructureRefreshedEvent.setOperatingUserProfile(operatingUserProfile);
            this.eventBusManager.send(indirectDataTableStructureRefreshedEvent);
        }

        //
        // step 3, post-processing
        //
    }

    @Override
    public DataTableDto getDataTable(
            Long dataTableUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // step 1, pre-processing
        //
        DataTableDo dataTableDo = this.dataTableRepository.findByUid(dataTableUid);
        if (dataTableDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataTableDo.RESOURCE_SYMBOL,
                    dataTableUid));
        }

        //
        // step 2, core-processing
        //
        DataTableDto dataTableDto = new DataTableDto();
        BeanUtils.copyProperties(dataTableDo, dataTableDto);
        return dataTableDto;
    }

    /**
     * 列出针对指定 Data Table (Indirect) 的所有引用
     *
     * @param dataTableUid
     * @param operatingUserProfile
     * @return
     * @throws AbcUndefinedException
     */
    @Override
    public List<String> listAllReferencesToDataTable(
            Long dataTableUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return null;
    }

    /**
     * 删除指定 Data Table (Indirect)
     *
     * @param dataTableUid
     * @param operatingUserProfile
     * @throws AbcUndefinedException
     */
    @Override
    public void deleteDataTable(
            Long dataTableUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // step 1, pre-processing
        //
        DataTableDo dataTableDo = this.dataTableRepository.findByUid(dataTableUid);
        if (dataTableDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataTableDo.RESOURCE_SYMBOL,
                    dataTableUid));
        }

        if (!DataTableTypeEnum.INDIRECT_TABLE.equals(dataTableDo.getType())) {
            throw new AbcResourceConflictException("only supports delete data table (indirect)");
        }

        //
        // step 2, core-processing
        //
        deleteDataTable(dataTableDo, operatingUserProfile);

        //
        // step 3, post-processing
        //
    }

    private void deleteDataTable(
            DataTableDo dataTableDo,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        dataTableDo.setDeleted(Boolean.TRUE);
        BaseDo.update(dataTableDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dataTableRepository.save(dataTableDo);

        // event post
        IndirectDataTableDeletedEvent indirectDataTableDeletedEvent = new IndirectDataTableDeletedEvent();
        indirectDataTableDeletedEvent.setDataTableDo(dataTableDo);
        indirectDataTableDeletedEvent.setOperatingUserProfile(operatingUserProfile);
        this.eventBusManager.send(indirectDataTableDeletedEvent);
    }

    /**
     * 树形列出指定 Data Source 的 Data Tables
     *
     * @param dataSourceUid
     * @param operatingUserProfile
     * @return
     * @throws AbcUndefinedException
     */
    @Override
    public List<TreeNode> treeListingAllDataTables(
            Long dataSourceUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        DataSourceDo dataSourceDo = this.dataSourceRepository.findByUid(dataSourceUid);
        if (dataSourceDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataSourceDo.RESOURCE_SYMBOL,
                    dataSourceUid));
        }
        List<DataTableDo> dataTableDoList = this.dataTableRepository.findByDataSourceUid(dataSourceUid);
        List<TreeNode> result = new ArrayList<>(1);
        result.add(treeListing(dataSourceDo, dataTableDoList));
        return result;
    }

    /**
     * 树形列出所有 Data Sources 的 Data Tables
     *
     * @param operatingUserProfile
     * @return
     * @throws AbcUndefinedException
     */
    @Override
    public List<TreeNode> treeListingAllDataTables(
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Map<Long, DataSourceDo> dataSourceUidAndDataSourceDoMap = new HashMap<>();
        this.dataSourceRepository.findAll().forEach(dataSourceDo -> {
            dataSourceUidAndDataSourceDoMap.put(dataSourceDo.getUid(), dataSourceDo);
        });
        Map<Long, List<DataTableDo>> dataSourceUidAndDataTableDoListMap = new HashMap<>();
        this.dataTableRepository.findAll().forEach(dataTableDo -> {
            if (!dataSourceUidAndDataTableDoListMap.containsKey(dataTableDo.getDataSourceUid())) {
                dataSourceUidAndDataTableDoListMap.put(dataTableDo.getDataSourceUid(), new LinkedList<>());
            }
            dataSourceUidAndDataTableDoListMap.get(dataTableDo.getDataSourceUid()).add(dataTableDo);
        });

        List<TreeNode> result = new ArrayList<>(dataSourceUidAndDataSourceDoMap.size());
        dataSourceUidAndDataTableDoListMap.forEach((dataSourceUid, dataTableDoList) -> {
            DataSourceDo dataSourceDo = dataSourceUidAndDataSourceDoMap.get(dataSourceUid);
            if (dataSourceDo == null) {
                LOGGER.warn("cannot find data source:{}, but it is referenced in data tables", dataSourceUid);
                return;
            }
            result.add(treeListing(dataSourceDo, dataTableDoList));
        });
        return result;
    }

    private TreeNode treeListing(
            DataSourceDo dataSourceDo,
            List<DataTableDo> dataTableDoList) {
        //
        // Step 1, pre-processing
        //
        TreeNode rootTreeNode = new TreeNode();
        rootTreeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
        rootTreeNode.setUid(dataSourceDo.getUid());
        rootTreeNode.setName(dataSourceDo.getName());
        rootTreeNode.setDescription(dataSourceDo.getDescription());
        rootTreeNode.setType("data_source");
        rootTreeNode.setTags(new HashMap<>());
        rootTreeNode.getTags().put("type", dataSourceDo.getType());
        rootTreeNode.setChildren(new LinkedList<>());

        TreeNode directTreeNode = new TreeNode();
        directTreeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
        directTreeNode.setName("Direct");
        directTreeNode.setType("dummy");
        directTreeNode.setChildren(new LinkedList<>());
        rootTreeNode.getChildren().add(directTreeNode);

        TreeNode indirectTreeNode = new TreeNode();
        indirectTreeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
        indirectTreeNode.setName("Indirect");
        indirectTreeNode.setType("dummy");
        indirectTreeNode.setChildren(new LinkedList<>());
        rootTreeNode.getChildren().add(indirectTreeNode);

        if (CollectionUtils.isEmpty(dataTableDoList)) {
            return rootTreeNode;
        }

        //
        // Step 2, core-processing
        //
        Map<String, TreeNode> allTreeNodeMap = new HashMap<>();
        dataTableDoList.forEach(dataTableDo -> {
            TreeNode treeNodeOfDataTable = new TreeNode();
            treeNodeOfDataTable.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
            treeNodeOfDataTable.setUid(dataTableDo.getUid());
            treeNodeOfDataTable.setName(dataTableDo.getName());
            treeNodeOfDataTable.setDescription(dataTableDo.getDescription());
            treeNodeOfDataTable.setType("data_table");
            treeNodeOfDataTable.setTags(new HashMap<>());
            treeNodeOfDataTable.getTags().put("type", dataTableDo.getType());

            if (CollectionUtils.isEmpty(dataTableDo.getContextPath())) {
                // without context path, such as (indirect) data table

                switch (dataTableDo.getType()) {
                    case INDIRECT_TABLE:
                        indirectTreeNode.getChildren().add(treeNodeOfDataTable);
                        break;
                    default:
                        directTreeNode.getChildren().add(treeNodeOfDataTable);
                        break;
                }
            } else {
                // with context path, such as (direct) data table
                // 创建 context path tree node
                StringBuilder contextPath = new StringBuilder();
                for (int i = 0; i < dataTableDo.getContextPath().size(); i++) {
                    // 记住上一级 context path
                    String upperContextPath = null;
                    if (i > 0) {
                        upperContextPath = contextPath.toString();
                    }

                    // 设置最新的 context path
                    if (contextPath.length() > 0) {
                        contextPath.append("/").append(dataTableDo.getContextPath().get(i));
                    } else {
                        contextPath.append(dataTableDo.getContextPath().get(i));
                    }

                    if (!allTreeNodeMap.containsKey(contextPath.toString())) {
                        TreeNode treeNodeOfContextPath = new TreeNode();
                        treeNodeOfContextPath.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
                        treeNodeOfContextPath.setName(dataTableDo.getContextPath().get(i));
                        treeNodeOfContextPath.setType("context_path");
                        treeNodeOfContextPath.setTags(new HashMap<>());
                        treeNodeOfContextPath.getTags().put("context_path", contextPath);
                        allTreeNodeMap.put(contextPath.toString(), treeNodeOfContextPath);

                        if (upperContextPath == null) {
                            switch (dataTableDo.getType()) {
                                case INDIRECT_TABLE:
                                    indirectTreeNode.getChildren().add(treeNodeOfContextPath);
                                    break;
                                default:
                                    directTreeNode.getChildren().add(treeNodeOfContextPath);
                                    break;
                            }
                        } else {
                            TreeNode treeNodeOfUpperContextPath = allTreeNodeMap.get(upperContextPath);
                            if (treeNodeOfUpperContextPath.getChildren() == null) {
                                treeNodeOfUpperContextPath.setChildren(new LinkedList<>());
                            }
                            treeNodeOfUpperContextPath.getChildren().add(treeNodeOfContextPath);
                        }
                    }
                }

                // 关联 context path tree node w/ data table tree node
                TreeNode treeNodeOfContextPath = allTreeNodeMap.get(contextPath.toString());
                if (treeNodeOfContextPath.getChildren() == null) {
                    treeNodeOfContextPath.setChildren(new LinkedList<>());
                }
                treeNodeOfContextPath.getChildren().add(treeNodeOfDataTable);
            }
        });

        return rootTreeNode;
    }

    /**
     * 重新获取指定 Data table 的 Metadata，包括 columns and indexes
     *
     * @param dataTableUid
     * @param operatingUserProfile
     */
    @Override
    public Long asyncRetrieveMetadataOfDataTable(
            Long dataTableUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return this.directDataTableMetadataRetrievalHandler.retrieveMetadata(
                dataTableUid, operatingUserProfile);
    }

    @Override
    public JobStatusEnum getTaskStatusOfAsyncRetrieveMetadataOfDataTable(
            Long taskUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        DataTableMetadataRetrievalInstanceDo dataTableMetadataRetrievalInstanceDo =
                this.dataTableMetadataRetrievalInstanceRepository.findByUid(taskUid);
        if (dataTableMetadataRetrievalInstanceDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DataTableMetadataRetrievalInstanceDo.RESOURCE_SYMBOL, taskUid));
        }

        return dataTableMetadataRetrievalInstanceDo.getStatus();
    }

    @Override
    public QueryResult getSampleDataOfDataTable(
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

        DmlHandler objectiveDmlHandler = null;
        Map<String, DmlHandler> map = this.applicationContext.getBeansOfType(DmlHandler.class);
        if (!CollectionUtils.isEmpty(map)) {
            for (Map.Entry<String, DmlHandler> entry : map.entrySet()) {
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

        //
        // Step 2, core-processing
        //
        switch (dataTableDo.getType()) {
            case DATABASE_TABLE:
            case DATABASE_VIEW: {
                // prepare 1 data column for sorting (MSSQL requires at least 1 sorting order if enabled pagination)
                Specification<DataColumnDo> specification = new Specification<DataColumnDo>() {
                    @Override
                    public Predicate toPredicate(Root<DataColumnDo> root, CriteriaQuery<?> query,
                                                 CriteriaBuilder criteriaBuilder) {
                        List<Predicate> predicateList = new ArrayList<>();
                        predicateList.add(criteriaBuilder.equal(root.get("dataTableUid"), dataTableUid));
                        return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
                    }
                };
                Page<DataColumnDo> dataColumnDoPage = this.dataColumnRepository.findAll(
                        specification, PageRequest.of(0, 1));
                if (dataColumnDoPage.isEmpty()) {
                    throw new AbcResourceNotFoundException(String.format("%s::data_table_uid=%d", DataColumnDo.RESOURCE_SYMBOL,
                            dataTableUid));
                }
                DataColumnDo dataColumnDoForSorting = dataColumnDoPage.getContent().get(0);


                DataTableMetadata dataTableMetadata = new DataTableMetadata();
                dataTableMetadata.setName(dataTableDo.getName());
                dataTableMetadata.setContextPath(dataTableDo.getContextPath());

                // prepare for sorting (MSSQL requires at least 1 sorting order if enabled pagination)
                AbcSort sort = new AbcSort();
                sort.setOrders(new LinkedList<>());
                AbcOrder order = new AbcOrder();
                order.setProperty(dataColumnDoForSorting.getName());
                order.setDirection(Sort.Direction.ASC);
                sort.getOrders().add(order);

                return objectiveDmlHandler.loadSampleDataOfDataTable(dataSourceDo.getConnectionProfile(),
                        dataTableMetadata, sort);
            }
            case INDIRECT_TABLE: {
                return objectiveDmlHandler.loadSampleDataOfDataTable(dataSourceDo.getConnectionProfile(),
                        dataTableDo.getBuildingLogic());
            }
            default:
                return null;
        }
    }

    /**
     * 重新获取指定 Context path 的 Metadata，包括 Data tables 及每个 Data table 的 columns and indexes
     *
     * @param dataSourceUid
     * @param contextPath
     * @param operatingUserProfile
     * @throws AbcUndefinedException
     */
    @Override
    public Long asyncRetrieveMetadataOfContextPath(
            Long dataSourceUid,
            String contextPath,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return this.contextPathMetadataRetrievalHandler.retrieveMetadata(
                dataSourceUid, contextPath,
                operatingUserProfile);
    }

    @Override
    public JobStatusEnum getTaskStatusOfAsyncRetrieveMetadataOfContextPath(
            Long taskUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        ContextPathMetadataRetrievalInstanceDo contextPathMetadataRetrievalInstanceDo =
                this.contextPathMetadataRetrievalInstanceRepository.findByUid(taskUid);
        if (contextPathMetadataRetrievalInstanceDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    ContextPathMetadataRetrievalInstanceDo.RESOURCE_SYMBOL, taskUid));
        }

        return contextPathMetadataRetrievalInstanceDo.getStatus();
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
    public void handleDataSourceDeletedEvent(DataSourceDeletedEvent event) {
        LOGGER.info("rcv event:{}", event);

        this.dataTableRepository.logicalDeleteByDataSourceUid(event.getDataSourceDo().getUid(),
                event.getOperatingUserProfile().getUid(), LocalDateTime.now());
    }

    /**
     * event handler
     *
     * @param event
     */
    @Subscribe
    public void handleDataSourceMetadataRefreshedEvent(DataSourceMetadataRefreshedEvent event) {
        LOGGER.info("rcv event:{}", event);

        this.directDataTableHandler.handleDataSourceMetadataRefreshed(
                event.getDataSourceDo(),
                event.getDataTableList(),
                event.getOperatingUserProfile());
    }

    /**
     * event handler
     *
     * @param event
     */
    @Subscribe
    public void handleDataTableMetadataRefreshedEvent(DataTableMetadataRefreshedEvent event) {
        LOGGER.info("rcv event:{}", event);

        // 不能为空
        DataSourceDo dataSourceDo = event.getDataSourceDo();
        // 可以为空 (刷新的可以是 context path，也可以是 data table)
        DataTableDo dataTableDo = event.getDataTableDo();

        if (dataTableDo == null) {
            //
            List<DataTableDo> candidateDataTableDoList =
                    this.dataTableRepository.findByDataSourceUidAndName(dataSourceDo.getUid(),
                            event.getDataTableMetadata().getName());
            if (!CollectionUtils.isEmpty(candidateDataTableDoList)) {
                for (DataTableDo candidateDataTableDo : candidateDataTableDoList) {
                    if (CollectionUtils.isEmpty(candidateDataTableDo.getContextPath())
                            && CollectionUtils.isEmpty(event.getDataTableMetadata().getContextPath())) {
                        dataTableDo = candidateDataTableDo;
                        break;
                    } else if (!CollectionUtils.isEmpty(candidateDataTableDo.getContextPath())
                            && !CollectionUtils.isEmpty(event.getDataTableMetadata().getContextPath())
                            && candidateDataTableDo.getContextPath().size() == event.getDataTableMetadata().getContextPath().size()) {
                        boolean matched = true;
                        for (int i = 0; i < candidateDataTableDo.getContextPath().size(); i++) {
                            String slice = candidateDataTableDo.getContextPath().get(i);
                            if (!slice.equalsIgnoreCase(event.getDataTableMetadata().getContextPath().get(i))) {
                                matched = false;
                                break;
                            }
                        }
                        if (matched) {
                            dataTableDo = candidateDataTableDo;
                            break;
                        }
                    }

                }
            }
        }

        if (dataTableDo == null) {
            this.directDataTableHandler.handleDataTableMetadataCreated(
                    dataSourceDo,
                    event.getDataTableMetadata(),
                    event.getDataColumnMetadataList(),
                    event.getDataIndexMetadataList(),
                    event.getOperatingUserProfile());
        } else {
            this.directDataTableHandler.handleDataTableMetadataRefreshed(
                    dataTableDo,
                    event.getDataTableMetadata(),
                    event.getDataColumnMetadataList(),
                    event.getDataIndexMetadataList(),
                    event.getOperatingUserProfile());
        }
    }

    /**
     * event handler
     *
     * @param event
     */
    @Subscribe
    public void handleDirectDataTableDeletedEvent(DirectDataTableDeletedEvent event) {
        LOGGER.info("rcv event:{}", event);

        //
        // Step 1, pre-processing
        //
        DataTableDo directDataTableDo = event.getDataTableDo();

        //
        // Step 2, core-processing
        //

        // 找出依赖该 direct data table 的 indirect data table(s)
        List<DataTableDo> indirectDataTableDoList =
                this.dataTableRepository.findByDataSourceUidAndType(directDataTableDo.getDataSourceUid(),
                        DataTableTypeEnum.INDIRECT_TABLE);
        if (CollectionUtils.isEmpty(indirectDataTableDoList)) {
            return;
        }

        indirectDataTableDoList.forEach(indirectDataTableDo -> {
            if (CollectionUtils.isEmpty(indirectDataTableDo.getUnderlyingDataTableUidList())) {
                return;
            }
            if (!indirectDataTableDo.getUnderlyingDataTableUidList().contains(directDataTableDo.getUid())) {
                return;
            }

            // find out one indirect data table, which depends on the objective data table

            indirectDataTableDo.setDeleted(Boolean.TRUE);
            BaseDo.update(indirectDataTableDo, event.getOperatingUserProfile().getUid(), LocalDateTime.now());
            this.dataTableRepository.save(indirectDataTableDo);

            // event post
            IndirectDataTableDeletedEvent indirectDataTableDeletedEvent = new IndirectDataTableDeletedEvent();
            indirectDataTableDeletedEvent.setDataTableDo(indirectDataTableDo);
            indirectDataTableDeletedEvent.setOperatingUserProfile(event.getOperatingUserProfile());
            this.eventBusManager.send(indirectDataTableDeletedEvent);
        });
    }

    /**
     * event handler
     *
     * @param event
     */
    @Subscribe
    public void handleDirectDataTableStructureChangedEvent(DirectDataTableStructureChangedEvent event) {
        LOGGER.info("rcv event:{}", event);

        //
        // Step 1, pre-processing
        //
        DataTableDo directDataTableDo = event.getDataTableDo();

        //
        // Step 2, core-processing
        //

        // 找出依赖该 direct data table 的 indirect data table(s)
        List<DataTableDo> indirectDataTableDoList =
                this.dataTableRepository.findByDataSourceUidAndType(directDataTableDo.getDataSourceUid(),
                        DataTableTypeEnum.INDIRECT_TABLE);
        if (CollectionUtils.isEmpty(indirectDataTableDoList)) {
            return;
        }

        indirectDataTableDoList.forEach(indirectDataTableDo -> {
            if (CollectionUtils.isEmpty(indirectDataTableDo.getUnderlyingDataTableUidList())) {
                return;
            }
            if (!indirectDataTableDo.getUnderlyingDataTableUidList().contains(directDataTableDo.getUid())) {
                return;
            }

            // find out one indirect data table, which depends on the objective data table

            // rebuild this indirect data table
            rebuildIndirectDataTable(indirectDataTableDo, event.getOperatingUserProfile());
        });
    }

}
