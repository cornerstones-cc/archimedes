package cc.cornerstones.biz.datatable.service.assembly;

import cc.cornerstones.almond.types.AbcTuple2;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcStringUtils;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.datasource.entity.DataSourceDo;
import cc.cornerstones.biz.datasource.persistence.DataSourceRepository;
import cc.cornerstones.biz.datasource.service.assembly.database.DataColumnMetadata;
import cc.cornerstones.biz.datasource.service.assembly.database.DataIndexMetadata;
import cc.cornerstones.biz.datasource.service.assembly.database.DataTableMetadata;
import cc.cornerstones.biz.datasource.share.constants.DataTableTypeEnum;
import cc.cornerstones.biz.datatable.entity.DataColumnDo;
import cc.cornerstones.biz.datatable.entity.DataIndexDo;
import cc.cornerstones.biz.datatable.entity.DataTableDo;
import cc.cornerstones.biz.datatable.persistence.DataColumnRepository;
import cc.cornerstones.biz.datatable.persistence.DataIndexRepository;
import cc.cornerstones.biz.datatable.persistence.DataTableRepository;
import cc.cornerstones.biz.share.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;

@Component
public class DirectDataTableHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DirectDataTableHandler.class);

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
    private DataIndexRepository dataIndexRepository;

    @Transactional(rollbackFor = Exception.class)
    public void handleDataSourceMetadataRefreshed(
            DataSourceDo dataSourceDo,
            List<DataSourceMetadataRefreshedEvent.DataTable> dataTableList,
            UserProfile operatingUserProfile) {
        if (dataSourceDo == null) {
            LOGGER.error("null or empty data source, ignore saving");
            return;
        }
        if (CollectionUtils.isEmpty(dataTableList)) {
            LOGGER.warn("null or empty data table list, ignore saving");
            return;
        }

        //
        // Step 1, 找出 existing items，并转换成 map
        //
        List<DataTableTypeEnum> types = new LinkedList<>();
        types.add(DataTableTypeEnum.DATABASE_TABLE);
        types.add(DataTableTypeEnum.DATABASE_VIEW);
        List<DataTableDo> dataTableDoList =
                this.dataTableRepository.findByDataSourceUidAndTypeIn(dataSourceDo.getUid(), types);
        // Key --- Context Path / Name
        Map<String, DataTableDo> existingItemMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(dataTableDoList)) {
            dataTableDoList.forEach(dataTableDo -> {
                StringBuilder key = new StringBuilder();
                if (CollectionUtils.isEmpty(dataTableDo.getContextPath())) {
                    key.append(dataTableDo.getName());
                } else {
                    key.append(AbcStringUtils.toString(dataTableDo.getContextPath(), "/"))
                            .append("/").append(dataTableDo.getName());
                }

                existingItemMap.put(key.toString(), dataTableDo);
            });
        }

        //
        // Step 2, 转换 input items 成 map
        //
        // Key --- Context Path / Name
        Map<String, DataTableMetadata> inputItemMap = new HashMap<>();
        Map<String, List<DataColumnMetadata>> inputItemExtension1Map = new HashMap<>();
        Map<String, List<DataIndexMetadata>> inputItemExtension2Map = new HashMap<>();
        for (int i = 0; i < dataTableList.size(); i++) {
            DataSourceMetadataRefreshedEvent.DataTable dataTable = dataTableList.get(i);

            DataTableMetadata dataTableMetadata = dataTable.getDataTableMetadata();
            List<DataColumnMetadata> dataColumnMetadataList = dataTable.getDataColumnMetadataList();
            List<DataIndexMetadata> dataIndexMetadataList = dataTable.getDataIndexMetadataList();

            StringBuilder key = new StringBuilder();
            if (CollectionUtils.isEmpty(dataTableMetadata.getContextPath())) {
                key.append(dataTableMetadata.getName());
            } else {
                key.append(AbcStringUtils.toString(dataTableMetadata.getContextPath(), "/"))
                        .append("/").append(dataTableMetadata.getName());
            }

            inputItemMap.put(key.toString(), dataTableMetadata);
            inputItemExtension1Map.put(key.toString(), dataColumnMetadataList);
            inputItemExtension2Map.put(key.toString(), dataIndexMetadataList);
        }


        //
        // Step 3, input items vs existing items, 找出新增/修改/删除 items
        //
        List<DataTableDo> toCreateDataTableList = new LinkedList<>();
        List<DataTableDo> toUpdateDataTableBasicList = new LinkedList<>();
        List<DataTableDo> toDeleteDataTableList = new LinkedList<>();

        //
        List<DataTableDo> continueToHoldDataTableDoList = new LinkedList<>();
        // key -- data table uid
        Map<Long, List<DataColumnDo>> continueToHoldDataTableStructureMap = new HashMap<>();

        LocalDateTime now = LocalDateTime.now();

        //
        existingItemMap.forEach((key, existingItem) -> {
            if (inputItemMap.containsKey(key)) {
                // 在 existing, 也在 input，
                // 继续检查是否修改场景

                DataTableMetadata inputItem = inputItemMap.get(key);

                boolean requiredToUpdateDataTableBasic = false;
                if (inputItem.getDescription() != null) {
                    if (!inputItem.getDescription().equalsIgnoreCase(existingItem.getDescription())) {
                        existingItem.setDescription(inputItem.getDescription());
                        requiredToUpdateDataTableBasic = true;
                    }
                } else {
                    if (existingItem.getDescription() != null) {
                        existingItem.setDescription(null);
                        requiredToUpdateDataTableBasic = true;
                    }
                }

                if (inputItem.getType() != null) {
                    if (!inputItem.getType().equals(existingItem.getType())) {
                        existingItem.setType(inputItem.getType());
                        requiredToUpdateDataTableBasic = true;
                    }
                } else {
                    if (existingItem.getType() != null) {
                        existingItem.setType(null);
                        requiredToUpdateDataTableBasic = true;
                    }
                }

                //
                // save available data columns
                //
                List<DataColumnMetadata> inputItemExtension1 = inputItemExtension1Map.get(key);
                AbcTuple2<Boolean, List<DataColumnDo>> dataTableStructureStatus = saveDataColumns(existingItem,
                        inputItemExtension1,
                        operatingUserProfile);
                if (Boolean.TRUE.equals(dataTableStructureStatus.f)) {
                    requiredToUpdateDataTableBasic = true;
                }

                //
                // save available data indexes
                //
                List<DataIndexMetadata> inputItemExtension2 = inputItemExtension2Map.get(key);
                saveDataIndexes(existingItem, inputItemExtension2, operatingUserProfile);

                // 原先存在的 data table，最新情况仍然还有，这部分 data table 要收集它们的最新的 data table structure
                continueToHoldDataTableDoList.add(existingItem);
                continueToHoldDataTableStructureMap.put(existingItem.getUid(), dataTableStructureStatus.s);

                if (requiredToUpdateDataTableBasic) {
                    existingItem.setBuildNumber(System.currentTimeMillis());
                    BaseDo.update(existingItem, operatingUserProfile.getUid(), now);
                    toUpdateDataTableBasicList.add(existingItem);
                }
            } else {
                // 在 existing, 不在 input
                // 删除场景

                existingItem.setDeleted(Boolean.TRUE);
                BaseDo.update(existingItem, operatingUserProfile.getUid(), now);
                toDeleteDataTableList.add(existingItem);
            }
        });

        inputItemMap.forEach((key, inputItem) -> {
            if (!existingItemMap.containsKey(key)) {
                // 在 input，不在 existing
                // 新增场景

                DataTableDo dataTableDo = new DataTableDo();
                dataTableDo.setUid(idHelper.getNextDistributedId(DataTableDo.RESOURCE_NAME));
                dataTableDo.setName(inputItem.getName());
                dataTableDo.setObjectName(inputItem.getName()
                        .replaceAll("_", "__")
                        .replaceAll("\\s", "_")
                        .toLowerCase());
                dataTableDo.setDescription(inputItem.getDescription());
                dataTableDo.setType(inputItem.getType());
                dataTableDo.setContextPath(inputItem.getContextPath());
                dataTableDo.setContextPathStr(AbcStringUtils.toString(inputItem.getContextPath(), "/"));
                dataTableDo.setBuildNumber(System.currentTimeMillis());
                dataTableDo.setDataSourceUid(dataSourceDo.getUid());
                BaseDo.create(dataTableDo, operatingUserProfile.getUid(), now);
                toCreateDataTableList.add(dataTableDo);

                //
                // save available data columns
                //
                List<DataColumnMetadata> inputItemExtension1 = inputItemExtension1Map.get(key);
                saveDataColumns(dataTableDo, inputItemExtension1, operatingUserProfile);

                //
                // save available data indexes
                //
                List<DataIndexMetadata> inputItemExtension2 = inputItemExtension2Map.get(key);
                saveDataIndexes(dataTableDo, inputItemExtension2, operatingUserProfile);
            }
        });

        if (!CollectionUtils.isEmpty(toCreateDataTableList)) {
            this.dataTableRepository.saveAll(toCreateDataTableList);
        }
        if (!CollectionUtils.isEmpty(toUpdateDataTableBasicList)) {
            this.dataTableRepository.saveAll(toUpdateDataTableBasicList);
        }
        if (!CollectionUtils.isEmpty(toDeleteDataTableList)) {
            this.dataTableRepository.saveAll(toDeleteDataTableList);

            // event post
            toDeleteDataTableList.forEach(dataTableDo -> {
                DirectDataTableDeletedEvent directDataTableDeletedEvent = new DirectDataTableDeletedEvent();
                directDataTableDeletedEvent.setDataTableDo(dataTableDo);
                directDataTableDeletedEvent.setOperatingUserProfile(operatingUserProfile);
                this.eventBusManager.send(directDataTableDeletedEvent);
            });
        }

        // 原先存在的 data table，最新情况仍然还有，这部分 data table 要收集它们的最新的 data table structure
        // 并对外发布，以便引用方有机会关注到影响
        if (!CollectionUtils.isEmpty(continueToHoldDataTableDoList)) {
            for (DataTableDo dataTableDo : continueToHoldDataTableDoList) {
                DirectDataTableStructureRefreshedEvent directDataTableStructureRefreshedEvent =
                        new DirectDataTableStructureRefreshedEvent();
                directDataTableStructureRefreshedEvent.setDataTableDo(dataTableDo);
                directDataTableStructureRefreshedEvent.setDataColumnDoList(
                        continueToHoldDataTableStructureMap.get(dataTableDo.getUid()));
                directDataTableStructureRefreshedEvent.setOperatingUserProfile(operatingUserProfile);
                this.eventBusManager.send(directDataTableStructureRefreshedEvent);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleDataTableMetadataRefreshed(
            DataTableDo dataTableDo,
            DataTableMetadata dataTableMetadata,
            List<DataColumnMetadata> dataColumnMetadataList,
            List<DataIndexMetadata> dataIndexMetadataList,
            UserProfile operatingUserProfile) {
        //
        // data table 的基本信息
        //
        boolean toUpdateDataTableBasic = false;

        if (!dataTableDo.getType().equals(dataTableMetadata.getType())) {
            dataTableDo.setType(dataTableMetadata.getType());
            toUpdateDataTableBasic = true;
        }
        if (dataTableMetadata.getDescription() != null
                && !dataTableMetadata.getDescription().equals(dataTableDo.getDescription())) {
            dataTableDo.setDescription(dataTableMetadata.getDescription());
        } else if (dataTableMetadata.getDescription() == null
                && dataTableDo.getDescription() != null) {
            dataTableDo.setDescription(null);
            toUpdateDataTableBasic = true;
        }


        //
        // data table 的 data column(s)
        //
        AbcTuple2<Boolean, List<DataColumnDo>> dataTableStructureStatus =
                saveDataColumns(dataTableDo, dataColumnMetadataList, operatingUserProfile);

        //
        // data table 的 data index(es), 索引变化不影响 data table structure
        //
        saveDataIndexes(dataTableDo, dataIndexMetadataList, operatingUserProfile);

        if (Boolean.TRUE.equals(dataTableStructureStatus.f)) {
            toUpdateDataTableBasic = true;
        }

        if (toUpdateDataTableBasic) {
            dataTableDo.setBuildNumber(System.currentTimeMillis());
            BaseDo.update(dataTableDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.dataTableRepository.save(dataTableDo);
        }

        // 对应待更新的 data table，要收集它们的最新的 data table structure
        // 并对外发布，以便引用方有机会关注到影响
        DirectDataTableStructureRefreshedEvent directDataTableStructureRefreshedEvent =
                new DirectDataTableStructureRefreshedEvent();
        directDataTableStructureRefreshedEvent.setDataTableDo(dataTableDo);
        directDataTableStructureRefreshedEvent.setDataColumnDoList(dataTableStructureStatus.s);
        directDataTableStructureRefreshedEvent.setOperatingUserProfile(operatingUserProfile);
        this.eventBusManager.send(directDataTableStructureRefreshedEvent);
    }

    public void handleDataTableMetadataCreated(
            DataSourceDo dataSourceDo,
            DataTableMetadata dataTableMetadata,
            List<DataColumnMetadata> dataColumnMetadataList,
            List<DataIndexMetadata> dataIndexMetadataList,
            UserProfile operatingUserProfile) {
        DataTableDo dataTableDo = new DataTableDo();
        dataTableDo.setUid(this.idHelper.getNextDistributedId(DataTableDo.RESOURCE_NAME));
        dataTableDo.setName(dataTableMetadata.getName());
        dataTableDo.setObjectName(dataTableMetadata.getName()
                .replaceAll("_", "__")
                .replaceAll("\\s", "_")
                .toLowerCase());
        dataTableDo.setDescription(dataTableMetadata.getDescription());
        dataTableDo.setContextPath(dataTableMetadata.getContextPath());
        dataTableDo.setContextPathStr(AbcStringUtils.toString(dataTableMetadata.getContextPath(), "/"));
        dataTableDo.setType(dataTableMetadata.getType());
        dataTableDo.setBuildNumber(System.currentTimeMillis());
        dataTableDo.setDataSourceUid(dataSourceDo.getUid());
        BaseDo.create(dataTableDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dataTableRepository.save(dataTableDo);

        saveDataColumns(dataTableDo, dataColumnMetadataList, operatingUserProfile);

        saveDataIndexes(dataTableDo, dataIndexMetadataList, operatingUserProfile);
    }

    @Transactional(rollbackFor = Exception.class,
            isolation = Isolation.READ_COMMITTED,
            propagation = Propagation.REQUIRES_NEW)
    private AbcTuple2<Boolean, List<DataColumnDo>> saveDataColumns(
            DataTableDo dataTableDo,
            List<DataColumnMetadata> dataColumnMetadataList,
            UserProfile operatingUserProfile) {
        if (CollectionUtils.isEmpty(dataColumnMetadataList)) {
            LOGGER.warn("null or empty input items, ignore saving");
            return new AbcTuple2<>(false, null);
        }

        //
        // Step 1, 找出 existing items，并转换成 map
        //
        List<DataColumnDo> dataColumnDoList = this.dataColumnRepository.findByDataTableUid(dataTableDo.getUid());
        // Key --- Context Path / Name
        Map<String, DataColumnDo> existingItemMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(dataColumnDoList)) {
            dataColumnDoList.forEach(dataColumnDo -> {
                existingItemMap.put(dataColumnDo.getName(), dataColumnDo);
            });
        }

        //
        // Step 2, 转换 input items 成 map
        //
        // Key --- Column Name
        Map<String, DataColumnMetadata> inputItemMap = new HashMap<>();
        dataColumnMetadataList.forEach(dataColumnMetadata -> {
            inputItemMap.put(dataColumnMetadata.getName(), dataColumnMetadata);
        });


        //
        // Step 3, input items vs existing items, 找出新增/修改/删除 items
        //
        List<DataColumnDo> latestFullList = new LinkedList<>();
        List<DataColumnDo> toCreateList = new LinkedList<>();
        List<DataColumnDo> toUpdateList = new LinkedList<>();
        List<DataColumnDo> toDeleteList = new LinkedList<>();

        LocalDateTime now = LocalDateTime.now();

        //
        existingItemMap.forEach((key, existingItem) -> {
            if (inputItemMap.containsKey(key)) {
                // 在 existing, 也在 input，
                // 继续检查是否修改场景

                DataColumnMetadata inputItem = inputItemMap.get(key);

                boolean requiredToUpdate = false;

                if (inputItem.getDescription() != null) {
                    if (!inputItem.getDescription().equalsIgnoreCase(existingItem.getDescription())) {
                        existingItem.setDescription(inputItem.getDescription());
                        requiredToUpdate = true;
                    }
                } else {
                    if (existingItem.getDescription() != null) {
                        existingItem.setDescription(null);
                        requiredToUpdate = true;
                    }
                }

                if (inputItem.getType() != null) {
                    if (!inputItem.getType().equals(existingItem.getType())) {
                        existingItem.setType(inputItem.getType());
                        requiredToUpdate = true;
                    }
                } else {
                    if (existingItem.getType() != null) {
                        existingItem.setType(null);
                        requiredToUpdate = true;
                    }
                }

                if (inputItem.getOrdinalPosition() != null) {
                    if (!inputItem.getOrdinalPosition().equals(existingItem.getOrdinalPosition())) {
                        existingItem.setOrdinalPosition(inputItem.getOrdinalPosition());
                        requiredToUpdate = true;
                    }
                } else {
                    if (existingItem.getOrdinalPosition() != null) {
                        existingItem.setOrdinalPosition(null);
                        requiredToUpdate = true;
                    }
                }

                if (requiredToUpdate) {
                    BaseDo.update(existingItem, operatingUserProfile.getUid(), now);
                    toUpdateList.add(existingItem);
                }

                latestFullList.add(existingItem);
            } else {
                // 在 existing, 不在 input
                // 删除场景

                existingItem.setDeleted(Boolean.TRUE);
                BaseDo.update(existingItem, operatingUserProfile.getUid(), now);
                toDeleteList.add(existingItem);
            }
        });

        inputItemMap.forEach((key, inputItem) -> {
            if (!existingItemMap.containsKey(key)) {
                // 在 input，不在 existing
                // 新增场景

                DataColumnDo dataColumnDo = new DataColumnDo();
                dataColumnDo.setUid(idHelper.getNextDistributedId(DataColumnDo.RESOURCE_NAME));
                dataColumnDo.setName(inputItem.getName());
                dataColumnDo.setObjectName(inputItem.getName()
                        .replaceAll("_", "__")
                        .replaceAll("\\s", "_")
                        .toLowerCase());
                dataColumnDo.setDescription(inputItem.getDescription());
                dataColumnDo.setType(inputItem.getType());
                dataColumnDo.setOrdinalPosition(inputItem.getOrdinalPosition());
                dataColumnDo.setDataTableUid(dataTableDo.getUid());
                dataColumnDo.setDataSourceUid(dataTableDo.getDataSourceUid());
                BaseDo.create(dataColumnDo, operatingUserProfile.getUid(), now);
                toCreateList.add(dataColumnDo);

                latestFullList.add(dataColumnDo);
            }
        });


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

        return new AbcTuple2<>(anythingChanged, latestFullList);
    }

    @Transactional(rollbackFor = Exception.class,
            isolation = Isolation.READ_COMMITTED,
            propagation = Propagation.REQUIRES_NEW)
    private void saveDataIndexes(
            DataTableDo dataTableDo,
            List<DataIndexMetadata> dataIndexMetadataList,
            UserProfile operatingUserProfile) {
        if (CollectionUtils.isEmpty(dataIndexMetadataList)) {
            LOGGER.warn("null or empty input items, ignore saving");
            return;
        }

        //
        // Step 1, 找出 existing items，并转换成 map
        //
        List<DataIndexDo> dataIndexDoList = this.dataIndexRepository.findByDataTableUid(dataTableDo.getUid());
        // Key --- Context Path / Name
        Map<String, DataIndexDo> existingItemMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(dataIndexDoList)) {
            dataIndexDoList.forEach(dataIndexDo -> {
                existingItemMap.put(dataIndexDo.getName(), dataIndexDo);
            });
        }

        //
        // Step 2, 转换 input items 成 map
        //
        // Key --- Context Path / Name
        Map<String, DataIndexMetadata> inputItemMap = new HashMap<>();
        dataIndexMetadataList.forEach(dataIndexMetadata -> {
            inputItemMap.put(dataIndexMetadata.getName(), dataIndexMetadata);
        });


        //
        // Step 3, input items vs existing items, 找出新增/修改/删除 items
        //
        List<DataIndexDo> toCreateList = new LinkedList<>();
        List<DataIndexDo> toUpdateList = new LinkedList<>();
        List<DataIndexDo> toDeleteList = new LinkedList<>();

        LocalDateTime now = LocalDateTime.now();

        //
        existingItemMap.forEach((key, existingItem) -> {
            if (inputItemMap.containsKey(key)) {
                // 在 existing, 也在 input，
                // 继续检查是否修改场景

                DataIndexMetadata inputItem = inputItemMap.get(key);

                boolean requiredToUpdate = false;

                if (inputItem.getUnique() != null) {
                    if (!inputItem.getUnique().equals(existingItem.getUnique())) {
                        existingItem.setUnique(inputItem.getUnique());
                        requiredToUpdate = true;
                    }
                } else {
                    if (existingItem.getUnique() != null) {
                        existingItem.setUnique(null);
                        requiredToUpdate = true;
                    }
                }

                if (!CollectionUtils.isEmpty(inputItem.getColumns())) {
                    if (!CollectionUtils.isEmpty(existingItem.getColumns())) {
                        if (inputItem.getColumns().size() != existingItem.getColumns().size()) {
                            requiredToUpdate = true;
                            existingItem.setColumns(inputItem.getColumns());
                        } else {
                            for (int i = 0; i < inputItem.getColumns().size(); i++) {
                                if (!inputItem.getColumns().get(i).equals(existingItem.getColumns().get(i))) {
                                    requiredToUpdate = true;
                                    existingItem.setColumns(inputItem.getColumns());
                                    break;
                                }
                            }
                        }
                    }
                }

                if (requiredToUpdate) {
                    BaseDo.update(existingItem, operatingUserProfile.getUid(), now);
                    toUpdateList.add(existingItem);
                }

            } else {
                // 在 existing, 不在 input
                // 删除场景

                existingItem.setDeleted(Boolean.TRUE);
                BaseDo.update(existingItem, operatingUserProfile.getUid(), now);
                toDeleteList.add(existingItem);
            }
        });

        inputItemMap.forEach((key, inputItem) -> {
            if (!existingItemMap.containsKey(key)) {
                // 在 input，不在 existing
                // 新增场景

                DataIndexDo dataIndexDo = new DataIndexDo();
                dataIndexDo.setUid(idHelper.getNextDistributedId(DataIndexDo.RESOURCE_NAME));
                dataIndexDo.setName(inputItem.getName());
                dataIndexDo.setObjectName(inputItem.getName()
                        .replaceAll("_", "__")
                        .replaceAll("\\s", "_")
                        .toLowerCase());
                dataIndexDo.setDescription(null);
                dataIndexDo.setUnique(inputItem.getUnique());
                dataIndexDo.setColumns(inputItem.getColumns());
                dataIndexDo.setDataTableUid(dataTableDo.getUid());
                dataIndexDo.setDataSourceUid(dataTableDo.getDataSourceUid());
                BaseDo.create(dataIndexDo, operatingUserProfile.getUid(), now);
                toCreateList.add(dataIndexDo);
            }
        });

        if (!CollectionUtils.isEmpty(toCreateList)) {
            this.dataIndexRepository.saveAll(toCreateList);
        }
        if (!CollectionUtils.isEmpty(toUpdateList)) {
            this.dataIndexRepository.saveAll(toUpdateList);
        }
        if (!CollectionUtils.isEmpty(toDeleteList)) {
            this.dataIndexRepository.saveAll(toDeleteList);
        }
    }
}
