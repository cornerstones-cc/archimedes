package cc.cornerstones.biz.datafacet.service.assembly;

import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.datafacet.entity.DataFacetDo;
import cc.cornerstones.biz.datafacet.entity.DataFieldDo;
import cc.cornerstones.biz.datafacet.persistence.DataFacetRepository;
import cc.cornerstones.biz.datafacet.persistence.DataFieldRepository;
import cc.cornerstones.biz.datafacet.share.constants.DataFieldTypeEnum;
import cc.cornerstones.biz.datasource.service.assembly.database.DataColumnMetadata;
import cc.cornerstones.biz.datasource.share.constants.DataColumnTypeEnum;
import cc.cornerstones.biz.datatable.entity.DataColumnDo;
import cc.cornerstones.biz.datatable.persistence.DataColumnRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.*;

@Component
public class DataFieldHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataFieldHandler.class);

    @Autowired
    private IdHelper idHelper;


    @Autowired
    private DataFacetRepository dataFacetRepository;

    @Autowired
    private DataColumnRepository dataColumnRepository;

    @Autowired
    private DataFieldRepository dataFieldRepository;

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED,
            propagation = Propagation.REQUIRES_NEW)
    public void initDataFieldsOfDataFacet(
            Long dataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        DataFacetDo dataFacetDo = this.dataFacetRepository.findByUid(dataFacetUid);
        if (dataFacetDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataFacetDo.RESOURCE_SYMBOL,
                    dataFacetUid));
        }

        List<DataColumnDo> dataColumnDoList =
                this.dataColumnRepository.findByDataTableUid(dataFacetDo.getDataTableUid());
        if (CollectionUtils.isEmpty(dataColumnDoList)) {
            throw new AbcResourceConflictException(String.format("cannot find any data column of data table:%d",
                    dataFacetDo.getDataTableUid()));
        }
        List<DataFieldDo> dataFieldDoList = new ArrayList<>(dataColumnDoList.size());
        dataColumnDoList.forEach(dataColumnDo -> {
            DataFieldDo dataFieldDo = transform(dataFacetUid, dataColumnDo, operatingUserProfile);
            dataFieldDoList.add(dataFieldDo);
        });
        this.dataFieldRepository.saveAll(dataFieldDoList);
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED,
            propagation = Propagation.REQUIRES_NEW)
    public void reinitDataFieldsOfDataFacet(
            Long dataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        DataFacetDo dataFacetDo = this.dataFacetRepository.findByUid(dataFacetUid);
        if (dataFacetDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataFacetDo.RESOURCE_SYMBOL,
                    dataFacetUid));
        }

        List<DataColumnDo> dataColumnDoList =
                this.dataColumnRepository.findByDataTableUid(dataFacetDo.getDataTableUid());
        if (dataColumnDoList == null) {
            dataColumnDoList = new LinkedList<>();
        }
        Map<String, DataColumnDo> inputMap = new HashMap<>();
        dataColumnDoList.forEach(dataColumnDo -> {
            inputMap.put(dataColumnDo.getName(), dataColumnDo);
        });

        List<DataFieldDo> dataFieldDoList = this.dataFieldRepository.findByDataFacetUid(dataFacetDo.getUid());
        if (dataFieldDoList == null) {
            dataFieldDoList = new LinkedList<>();
        }
        Map<String, DataFieldDo> existingMap = new HashMap<>();
        dataFieldDoList.forEach(dataFieldDo -> {
            existingMap.put(dataFieldDo.getName(), dataFieldDo);
        });


        List<DataFieldDo> toCreateList = new LinkedList<>();
        List<DataFieldDo> toUpdateList = new LinkedList<>();
        List<DataFieldDo> toDeleteList = new LinkedList<>();

        existingMap.forEach((key, existingItem) -> {
            if (inputMap.containsKey(key)) {
                // existing 有, input 有
                // 继续判断是否修改场景
                DataColumnDo inputItem = inputMap.get(key);

                boolean requiredToUpdate = false;

                DataFieldTypeEnum dataFieldTypeOfInputItem = transform(inputItem.getType());
                if (!existingItem.getTypePhysical().equals(dataFieldTypeOfInputItem)) {
                    existingItem.setTypePhysical(dataFieldTypeOfInputItem);
                    if (existingItem.getTypeLogical() == null) {
                        existingItem.setType(existingItem.getTypePhysical());
                    }
                    existingItem.setTypeExtension(null);
                    requiredToUpdate = true;
                }

                if (ObjectUtils.isEmpty(inputItem.getDescription())) {
                    if (!ObjectUtils.isEmpty(existingItem.getDescriptionPhysical())) {
                        existingItem.setDescriptionPhysical(inputItem.getDescription());
                    }
                    if (ObjectUtils.isEmpty(existingItem.getDescriptionLogical())) {
                        existingItem.setDescription(existingItem.getDescriptionPhysical());
                    }

                    existingItem.setLabelPhysical(inputItem.getName());
                    if (ObjectUtils.isEmpty(existingItem.getLabelLogical())) {
                        existingItem.setLabel(existingItem.getLabelPhysical());
                    }

                    requiredToUpdate = true;
                } else {
                    if (!inputItem.getDescription().equals(existingItem.getDescriptionPhysical())) {
                        existingItem.setDescriptionPhysical(inputItem.getDescription());
                        if (ObjectUtils.isEmpty(existingItem.getDescriptionLogical())) {
                            existingItem.setDescription(existingItem.getDescriptionPhysical());
                        }

                        existingItem.setLabelPhysical(inputItem.getDescription());
                        if (ObjectUtils.isEmpty(existingItem.getLabelLogical())) {
                            existingItem.setLabel(existingItem.getLabelPhysical());
                        }

                        requiredToUpdate = true;
                    }
                }

                if (inputItem.getOrdinalPosition() != null) {
                    if (!inputItem.getOrdinalPosition().equals(existingItem.getSequencePhysical())) {
                        existingItem.setSequencePhysical(inputItem.getOrdinalPosition());
                        if (existingItem.getSequenceLogical() == null) {
                            existingItem.setSequence(existingItem.getSequencePhysical());
                        }
                        requiredToUpdate = true;
                    }
                }

                if (requiredToUpdate) {
                    BaseDo.update(existingItem, operatingUserProfile.getUid(), LocalDateTime.now());
                    toUpdateList.add(existingItem);
                }
            } else {
                // existing 有，input 没有
                // 删除场景
                existingItem.setDeleted(Boolean.TRUE);
                BaseDo.update(existingItem, operatingUserProfile.getUid(), LocalDateTime.now());
                toDeleteList.add(existingItem);
            }
        });

        inputMap.forEach((key, inputItem) -> {
            if (!existingMap.containsKey(key)) {
                // input 有, existing 有
                // 新增场景
                DataFieldDo toCreateItem = transform(dataFacetUid, inputItem, operatingUserProfile);
                toCreateList.add(toCreateItem);
            }
        });

        if (!CollectionUtils.isEmpty(toCreateList)) {
            this.dataFieldRepository.saveAll(toCreateList);
        }

        if (!CollectionUtils.isEmpty(toUpdateList)) {
            this.dataFieldRepository.saveAll(toUpdateList);
        }

        if (!CollectionUtils.isEmpty(toDeleteList)) {
            this.dataFieldRepository.saveAll(toDeleteList);
        }
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED,
            propagation = Propagation.REQUIRES_NEW)
    public void reinitDataFieldsOfDataFacetWithDataColumnDoList(
            Long dataFacetUid,
            List<DataColumnDo> dataColumnDoList,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        DataFacetDo dataFacetDo = this.dataFacetRepository.findByUid(dataFacetUid);
        if (dataFacetDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataFacetDo.RESOURCE_SYMBOL,
                    dataFacetUid));
        }

        Map<String, DataColumnDo> inputMap = new HashMap<>();
        dataColumnDoList.forEach(dataColumnDo -> {
            inputMap.put(dataColumnDo.getName(), dataColumnDo);
        });

        List<DataFieldDo> dataFieldDoList = this.dataFieldRepository.findByDataFacetUid(dataFacetDo.getUid());
        if (dataFieldDoList == null) {
            dataFieldDoList = new LinkedList<>();
        }
        Map<String, DataFieldDo> existingMap = new HashMap<>();
        dataFieldDoList.forEach(dataFieldDo -> {
            existingMap.put(dataFieldDo.getName(), dataFieldDo);
        });


        List<DataFieldDo> toCreateList = new LinkedList<>();
        List<DataFieldDo> toUpdateList = new LinkedList<>();
        List<DataFieldDo> toDeleteList = new LinkedList<>();

        existingMap.forEach((key, existingItem) -> {
            if (inputMap.containsKey(key)) {
                // existing 有, input 有
                // 继续判断是否修改场景
                DataColumnDo inputItem = inputMap.get(key);

                boolean requiredToUpdate = false;

                DataFieldTypeEnum dataFieldTypeOfInputItem = transform(inputItem.getType());
                if (!existingItem.getTypePhysical().equals(dataFieldTypeOfInputItem)) {
                    existingItem.setTypePhysical(dataFieldTypeOfInputItem);
                    if (existingItem.getTypeLogical() == null) {
                        existingItem.setType(existingItem.getTypePhysical());
                    }
                    existingItem.setTypeExtension(null);
                    requiredToUpdate = true;
                }

                if (ObjectUtils.isEmpty(inputItem.getDescription())) {
                    if (!ObjectUtils.isEmpty(existingItem.getDescriptionPhysical())) {
                        existingItem.setDescriptionPhysical(inputItem.getDescription());
                    }
                    if (ObjectUtils.isEmpty(existingItem.getDescriptionLogical())) {
                        existingItem.setDescription(existingItem.getDescriptionPhysical());
                    }

                    existingItem.setLabelPhysical(inputItem.getName());
                    if (ObjectUtils.isEmpty(existingItem.getLabelLogical())) {
                        existingItem.setLabel(existingItem.getLabelPhysical());
                    }

                    requiredToUpdate = true;
                } else {
                    if (!inputItem.getDescription().equals(existingItem.getDescriptionPhysical())) {
                        existingItem.setDescriptionPhysical(inputItem.getDescription());
                        if (ObjectUtils.isEmpty(existingItem.getDescriptionLogical())) {
                            existingItem.setDescription(existingItem.getDescriptionPhysical());
                        }

                        existingItem.setLabelPhysical(inputItem.getDescription());
                        if (ObjectUtils.isEmpty(existingItem.getLabelLogical())) {
                            existingItem.setLabel(existingItem.getLabelPhysical());
                        }

                        requiredToUpdate = true;
                    }
                }

                if (inputItem.getOrdinalPosition() != null) {
                    if (!inputItem.getOrdinalPosition().equals(existingItem.getSequencePhysical())) {
                        existingItem.setSequencePhysical(inputItem.getOrdinalPosition());
                        if (existingItem.getSequenceLogical() == null) {
                            existingItem.setSequence(existingItem.getSequencePhysical());
                        }
                        requiredToUpdate = true;
                    }
                }

                if (requiredToUpdate) {
                    BaseDo.update(existingItem, operatingUserProfile.getUid(), LocalDateTime.now());
                    toUpdateList.add(existingItem);
                }
            } else {
                // existing 有，input 没有
                // 删除场景
                existingItem.setDeleted(Boolean.TRUE);
                BaseDo.update(existingItem, operatingUserProfile.getUid(), LocalDateTime.now());
                toDeleteList.add(existingItem);
            }
        });

        inputMap.forEach((key, inputItem) -> {
            if (!existingMap.containsKey(key)) {
                // input 有, existing 有
                // 新增场景
                DataFieldDo toCreateItem = transform(dataFacetUid, inputItem, operatingUserProfile);
                toCreateList.add(toCreateItem);
            }
        });

        if (!CollectionUtils.isEmpty(toCreateList)) {
            this.dataFieldRepository.saveAll(toCreateList);
        }

        if (!CollectionUtils.isEmpty(toUpdateList)) {
            this.dataFieldRepository.saveAll(toUpdateList);
        }

        if (!CollectionUtils.isEmpty(toDeleteList)) {
            this.dataFieldRepository.saveAll(toDeleteList);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDataFieldsOfDataFacet(
            Long dataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        List<DataFieldDo> dataFieldDoList = this.dataFieldRepository.findByDataFacetUid(dataFacetUid);
        if (!CollectionUtils.isEmpty(dataFieldDoList)) {
            LocalDateTime now = LocalDateTime.now();
            dataFieldDoList.forEach(dataFieldDo -> {
                dataFieldDo.setDeleted(Boolean.TRUE);
                BaseDo.update(dataFieldDo, operatingUserProfile.getUid(), now);
            });
            this.dataFieldRepository.saveAll(dataFieldDoList);
        }
    }

    private DataFieldDo transform(
            Long dataFacetUid,
            DataColumnDo dataColumnDo,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        DataFieldDo dataFieldDo = new DataFieldDo();
        dataFieldDo.setUid(this.idHelper.getNextDistributedId(DataFieldDo.RESOURCE_NAME));
        dataFieldDo.setName(dataColumnDo.getName());
        dataFieldDo.setObjectName(dataColumnDo.getObjectName());

        if (ObjectUtils.isEmpty(dataColumnDo.getDescription())) {
            dataFieldDo.setLabelPhysical(dataColumnDo.getName());
        } else {
            dataFieldDo.setLabelPhysical(dataColumnDo.getDescription());
        }
        dataFieldDo.setLabel(dataFieldDo.getLabelPhysical());

        dataFieldDo.setDescriptionPhysical(dataColumnDo.getDescription());
        dataFieldDo.setDescription(dataFieldDo.getDescriptionPhysical());

        dataFieldDo.setTypePhysical(transform(dataColumnDo.getType()));
        dataFieldDo.setType(dataFieldDo.getTypePhysical());

        dataFieldDo.setSequencePhysical(dataColumnDo.getOrdinalPosition());
        dataFieldDo.setSequence(dataFieldDo.getSequencePhysical());

        dataFieldDo.setDataFacetUid(dataFacetUid);
        BaseDo.create(dataFieldDo, operatingUserProfile.getUid(), LocalDateTime.now());

        return dataFieldDo;
    }

    private DataFieldDo transform(
            Long dataFacetUid,
            DataColumnMetadata dataColumnMetadata,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        DataFieldDo dataFieldDo = new DataFieldDo();
        dataFieldDo.setUid(this.idHelper.getNextDistributedId(DataFieldDo.RESOURCE_NAME));
        dataFieldDo.setName(dataColumnMetadata.getName());
        dataFieldDo.setObjectName(dataColumnMetadata.getName()
                .replaceAll("_", "__")
                .replaceAll("\\s", "_")
                .toLowerCase());

        if (ObjectUtils.isEmpty(dataColumnMetadata.getDescription())) {
            dataFieldDo.setLabelPhysical(dataColumnMetadata.getName());
        } else {
            dataFieldDo.setLabelPhysical(dataColumnMetadata.getDescription());
        }
        dataFieldDo.setLabel(dataFieldDo.getLabelPhysical());

        dataFieldDo.setDescriptionPhysical(dataColumnMetadata.getDescription());
        dataFieldDo.setDescription(dataFieldDo.getDescriptionPhysical());

        dataFieldDo.setTypePhysical(transform(dataColumnMetadata.getType()));
        dataFieldDo.setType(dataFieldDo.getTypePhysical());

        dataFieldDo.setSequencePhysical(dataColumnMetadata.getOrdinalPosition());
        dataFieldDo.setSequence(dataFieldDo.getSequencePhysical());

        dataFieldDo.setDataFacetUid(dataFacetUid);
        BaseDo.create(dataFieldDo, operatingUserProfile.getUid(), LocalDateTime.now());

        return dataFieldDo;
    }

    private DataFieldTypeEnum transform(DataColumnTypeEnum dataColumnType) {
        if (dataColumnType == null) {
            return DataFieldTypeEnum.STRING;
        }

        switch (dataColumnType) {
            case BOOLEAN:
                return DataFieldTypeEnum.BOOLEAN;
            case TINYINT:
            case SMALLINT:
            case MEDIUMINT:
            case INT:
            case YEAR:
                return DataFieldTypeEnum.INTEGER;
            case LONG:
                return DataFieldTypeEnum.LONG;
            case DECIMAL:
                return DataFieldTypeEnum.DECIMAL;
            case DATE:
                return DataFieldTypeEnum.DATE;
            case DATETIME:
            case TIMESTAMP:
                return DataFieldTypeEnum.DATETIME;
            case TIME:
            case CHAR:
            case VARCHAR:
            case TEXT:
            case BLOB:
                return DataFieldTypeEnum.STRING;
            case JSON:
                return DataFieldTypeEnum.JSON;
            default:
                throw new AbcResourceConflictException(String.format("unsupported data column type:%s",
                        dataColumnType));
        }
    }
}
