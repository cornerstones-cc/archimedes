package cc.cornerstones.biz.datafacet.service.impl;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.datafacet.entity.*;
import cc.cornerstones.biz.datafacet.persistence.*;
import cc.cornerstones.biz.datafacet.service.inf.*;
import cc.cornerstones.biz.share.event.EventBusManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class DataFacetReplicationServiceImpl implements DataFacetReplicationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataFacetReplicationServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private DataFieldRepository dataFieldRepository;

    @Autowired
    private FilteringDataFieldRepository filteringDataFieldRepository;

    @Autowired
    private FilteringExtendedRepository filteringExtendedRepository;

    @Autowired
    private ListingDataFieldRepository listingDataFieldRepository;

    @Autowired
    private ListingExtendedRepository listingExtendedRepository;

    @Autowired
    private SortingDataFieldRepository sortingDataFieldRepository;

    @Autowired
    private ExportBasicRepository exportBasicRepository;

    @Autowired
    private AdvancedFeatureRepository advancedFeatureRepository;

    @Autowired
    private DataPermissionRepository dataPermissionRepository;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void copyDataFacet(
            Long sourceDataFacetUid,
            Long targetDataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        List<DataFieldDo> sourceDataFieldDoList = this.dataFieldRepository.findByDataFacetUid(sourceDataFacetUid);
        Map<String, DataFieldDo> sourceDataFieldDoMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(sourceDataFieldDoList)) {
            sourceDataFieldDoList.forEach(dataFieldDo -> {
                sourceDataFieldDoMap.put(dataFieldDo.getName(), dataFieldDo);
            });
        }
        List<DataFieldDo> targetDataFieldDoList = this.dataFieldRepository.findByDataFacetUid(targetDataFacetUid);
        Map<String, DataFieldDo> targetDataFieldDoMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(targetDataFieldDoList)) {
            targetDataFieldDoList.forEach(dataFieldDo -> {
                targetDataFieldDoMap.put(dataFieldDo.getName(), dataFieldDo);
            });
        }

        List<String> sameFieldNameList = new LinkedList<>();
        sourceDataFieldDoMap.forEach((fieldName, sourceDataFieldDo) -> {
            if (targetDataFieldDoMap.containsKey(fieldName)) {
                sameFieldNameList.add(fieldName);
            }
        });

        //
        // Step 2, core-processing
        //
        copyDataFields(sameFieldNameList, sourceDataFieldDoMap, targetDataFieldDoMap, operatingUserProfile);
        copyFilteringDataFields(sameFieldNameList, sourceDataFacetUid, targetDataFacetUid, operatingUserProfile);
        copyFilteringExtended(sourceDataFacetUid, targetDataFacetUid, operatingUserProfile);
        copyListingDataFields(sameFieldNameList, sourceDataFacetUid, targetDataFacetUid, operatingUserProfile);
        copyListingExtended(sourceDataFacetUid, targetDataFacetUid, operatingUserProfile);
        copySortingDataFields(sameFieldNameList, sourceDataFacetUid, targetDataFacetUid, operatingUserProfile);
        copyExportBasic(sourceDataFacetUid, targetDataFacetUid, operatingUserProfile);
        copyDataPermissions(sameFieldNameList, sourceDataFacetUid, targetDataFacetUid, operatingUserProfile);
        copyAdvancedFeature(sourceDataFacetUid, targetDataFacetUid, operatingUserProfile);
    }

    private void copyDataFields(
            List<String> sameFieldNameList,
            Map<String, DataFieldDo> sourceDataFieldDoMap,
            Map<String, DataFieldDo> targetDataFieldDoMap,
            UserProfile operatingUserProfile) {
        if (CollectionUtils.isEmpty(sameFieldNameList)) {
            return;
        }

        List<DataFieldDo> toUpdateItemDoList = new LinkedList<>();
        sameFieldNameList.forEach(fieldName -> {
            DataFieldDo sourceDataFieldDo = sourceDataFieldDoMap.get(fieldName);
            DataFieldDo targetDataFieldDo = targetDataFieldDoMap.get(fieldName);

            targetDataFieldDo.setSequenceLogical(sourceDataFieldDo.getSequenceLogical());
            targetDataFieldDo.setSequence(sourceDataFieldDo.getSequence());
            targetDataFieldDo.setDescriptionLogical(sourceDataFieldDo.getDescriptionLogical());
            targetDataFieldDo.setDescription(sourceDataFieldDo.getDescription());
            targetDataFieldDo.setLabelLogical(sourceDataFieldDo.getLabelLogical());
            targetDataFieldDo.setLabel(sourceDataFieldDo.getLabel());
            targetDataFieldDo.setTypeLogical(sourceDataFieldDo.getTypeLogical());
            targetDataFieldDo.setType(sourceDataFieldDo.getType());
            targetDataFieldDo.setMeasurementRole(sourceDataFieldDo.getMeasurementRole());
            targetDataFieldDo.setTypeExtension(sourceDataFieldDo.getTypeExtension());
            BaseDo.update(targetDataFieldDo, operatingUserProfile.getUid(), LocalDateTime.now());
            toUpdateItemDoList.add(targetDataFieldDo);
        });

        this.dataFieldRepository.saveAll(toUpdateItemDoList);
    }

    private void copyFilteringDataFields(
            List<String> sameFieldNameList,
            Long sourceDataFacetUid,
            Long targetDataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        List<FilteringDataFieldDo> sourceFilteringDataFieldDoList =
                this.filteringDataFieldRepository.findByDataFacetUidAndFieldNameIn(
                        sourceDataFacetUid, sameFieldNameList);
        if (CollectionUtils.isEmpty(sourceFilteringDataFieldDoList)) {
            return;
        }
        Map<String, FilteringDataFieldDo> sourceFilteringDataFieldDoMap = new HashMap<>();
        sourceFilteringDataFieldDoList.forEach(filteringDataFieldDo -> {
            sourceFilteringDataFieldDoMap.put(filteringDataFieldDo.getFieldName(), filteringDataFieldDo);
        });

        List<FilteringDataFieldDo> targetFilteringDataFieldDoList =
                this.filteringDataFieldRepository.findByDataFacetUidAndFieldNameIn(
                        targetDataFacetUid, sameFieldNameList);
        Map<String, FilteringDataFieldDo> targetFilteringDataFieldDoMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(targetFilteringDataFieldDoList)) {
            targetFilteringDataFieldDoList.forEach(filteringDataFieldDo -> {
                targetFilteringDataFieldDoMap.put(filteringDataFieldDo.getFieldName(), filteringDataFieldDo);
            });
        }

        List<FilteringDataFieldDo> toAddItemDoList = new LinkedList<>();
        List<FilteringDataFieldDo> toUpdateItemDoList = new LinkedList<>();
        List<FilteringDataFieldDo> toDeleteItemDoList = new LinkedList<>();

        sourceFilteringDataFieldDoList.forEach(sourceFilteringDataFieldDo -> {
            FilteringDataFieldDo targetFilteringDataFieldDo = targetFilteringDataFieldDoMap.get(sourceFilteringDataFieldDo.getFieldName());
            if (targetFilteringDataFieldDo == null) {
                targetFilteringDataFieldDo = new FilteringDataFieldDo();
                targetFilteringDataFieldDo.setFieldName(sourceFilteringDataFieldDo.getFieldName());
                targetFilteringDataFieldDo.setFilteringType(sourceFilteringDataFieldDo.getFilteringType());
                targetFilteringDataFieldDo.setFilteringTypeExtension(sourceFilteringDataFieldDo.getFilteringTypeExtension());
                targetFilteringDataFieldDo.setDefaultValueSettings(sourceFilteringDataFieldDo.getDefaultValueSettings());
                targetFilteringDataFieldDo.setFilteringSequence(sourceFilteringDataFieldDo.getFilteringSequence());
                targetFilteringDataFieldDo.setDataFacetUid(targetDataFacetUid);
                BaseDo.create(targetFilteringDataFieldDo, operatingUserProfile.getUid(), LocalDateTime.now());
                toAddItemDoList.add(targetFilteringDataFieldDo);

                targetFilteringDataFieldDoMap.put(targetFilteringDataFieldDo.getFieldName(),
                        targetFilteringDataFieldDo);
            } else {
                targetFilteringDataFieldDo.setFilteringType(sourceFilteringDataFieldDo.getFilteringType());
                targetFilteringDataFieldDo.setFilteringTypeExtension(sourceFilteringDataFieldDo.getFilteringTypeExtension());
                targetFilteringDataFieldDo.setDefaultValueSettings(sourceFilteringDataFieldDo.getDefaultValueSettings());
                targetFilteringDataFieldDo.setFilteringSequence(sourceFilteringDataFieldDo.getFilteringSequence());
                BaseDo.update(targetFilteringDataFieldDo, operatingUserProfile.getUid(), LocalDateTime.now());
                toUpdateItemDoList.add(targetFilteringDataFieldDo);
            }
        });

        // 还需要将 target filtering data field 存在，但在 source filtering data filed 不存在的，删除
        targetFilteringDataFieldDoMap.forEach((fieldName, targetFilteringDataFieldDo) -> {
            if (!sourceFilteringDataFieldDoMap.containsKey(fieldName)) {
                targetFilteringDataFieldDo.setDeleted(Boolean.TRUE);
                BaseDo.update(targetFilteringDataFieldDo, operatingUserProfile.getUid(), LocalDateTime.now());
                toDeleteItemDoList.add(targetFilteringDataFieldDo);
            }
        });

        if (!CollectionUtils.isEmpty(toAddItemDoList)) {
            this.filteringDataFieldRepository.saveAll(toAddItemDoList);
        }
        if (!CollectionUtils.isEmpty(toUpdateItemDoList)) {
            this.filteringDataFieldRepository.saveAll(toUpdateItemDoList);
        }
        if (!CollectionUtils.isEmpty(toDeleteItemDoList)) {
            this.filteringDataFieldRepository.saveAll(toDeleteItemDoList);
        }
    }

    private void copyFilteringExtended(
            Long sourceDataFacetUid,
            Long targetDataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        FilteringExtendedDo sourceFilteringExtendedDo =
                this.filteringExtendedRepository.findByDataFacetUid(sourceDataFacetUid);
        if (sourceFilteringExtendedDo == null) {
            return;
        }
        FilteringExtendedDo targetFilteringExtendedDo =
                this.filteringExtendedRepository.findByDataFacetUid(targetDataFacetUid);
        if (targetFilteringExtendedDo == null) {
            targetFilteringExtendedDo = new FilteringExtendedDo();
            targetFilteringExtendedDo.setEnabledDefaultQuery(sourceFilteringExtendedDo.getEnabledDefaultQuery());
            targetFilteringExtendedDo.setEnabledFilterFolding(sourceFilteringExtendedDo.getEnabledFilterFolding());
            targetFilteringExtendedDo.setDataFacetUid(targetDataFacetUid);
            BaseDo.create(targetFilteringExtendedDo, operatingUserProfile.getUid(), LocalDateTime.now());
        } else {
            targetFilteringExtendedDo.setEnabledDefaultQuery(sourceFilteringExtendedDo.getEnabledDefaultQuery());
            targetFilteringExtendedDo.setEnabledFilterFolding(sourceFilteringExtendedDo.getEnabledFilterFolding());
            BaseDo.update(targetFilteringExtendedDo, operatingUserProfile.getUid(), LocalDateTime.now());
        }
        this.filteringExtendedRepository.save(targetFilteringExtendedDo);
    }

    private void copyListingDataFields(
            List<String> sameFieldNameList,
            Long sourceDataFacetUid,
            Long targetDataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        List<ListingDataFieldDo> sourceListingDataFieldDoList =
                this.listingDataFieldRepository.findByDataFacetUidAndFieldNameIn(
                        sourceDataFacetUid, sameFieldNameList);
        if (CollectionUtils.isEmpty(sourceListingDataFieldDoList)) {
            return;
        }
        Map<String, ListingDataFieldDo> sourceListingDataFieldDoMap = new HashMap<>();
        sourceListingDataFieldDoList.forEach(listingDataFieldDo -> {
            sourceListingDataFieldDoMap.put(listingDataFieldDo.getFieldName(), listingDataFieldDo);
        });

        List<ListingDataFieldDo> targetListingDataFieldDoList =
                this.listingDataFieldRepository.findByDataFacetUidAndFieldNameIn(
                        targetDataFacetUid, sameFieldNameList);
        Map<String, ListingDataFieldDo> targetListingDataFieldDoMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(targetListingDataFieldDoList)) {
            targetListingDataFieldDoList.forEach(listingDataFieldDo -> {
                targetListingDataFieldDoMap.put(listingDataFieldDo.getFieldName(), listingDataFieldDo);
            });
        }

        List<ListingDataFieldDo> toAddItemDoList = new LinkedList<>();
        List<ListingDataFieldDo> toUpdateItemDoList = new LinkedList<>();
        List<ListingDataFieldDo> toDeleteItemDoList = new LinkedList<>();

        sourceListingDataFieldDoList.forEach(sourceListingDataFieldDo -> {
            ListingDataFieldDo targetListingDataFieldDo = targetListingDataFieldDoMap.get(sourceListingDataFieldDo.getFieldName());
            if (targetListingDataFieldDo == null) {
                targetListingDataFieldDo = new ListingDataFieldDo();
                targetListingDataFieldDo.setFieldName(sourceListingDataFieldDo.getFieldName());
                targetListingDataFieldDo.setWidth(sourceListingDataFieldDo.getWidth());
                targetListingDataFieldDo.setExtension(sourceListingDataFieldDo.getExtension());
                targetListingDataFieldDo.setListingSequence(sourceListingDataFieldDo.getListingSequence());
                targetListingDataFieldDo.setDataFacetUid(targetDataFacetUid);
                BaseDo.create(targetListingDataFieldDo, operatingUserProfile.getUid(), LocalDateTime.now());
                toAddItemDoList.add(targetListingDataFieldDo);

                targetListingDataFieldDoMap.put(targetListingDataFieldDo.getFieldName(),
                        targetListingDataFieldDo);
            } else {
                targetListingDataFieldDo.setWidth(sourceListingDataFieldDo.getWidth());
                targetListingDataFieldDo.setExtension(sourceListingDataFieldDo.getExtension());
                targetListingDataFieldDo.setListingSequence(sourceListingDataFieldDo.getListingSequence());
                BaseDo.update(targetListingDataFieldDo, operatingUserProfile.getUid(), LocalDateTime.now());
                toUpdateItemDoList.add(targetListingDataFieldDo);
            }
        });

        // 还需要将 target listing data field 存在，但在 source listing data filed 不存在的，删除
        targetListingDataFieldDoMap.forEach((fieldName, targetListingDataFieldDo) -> {
            if (!sourceListingDataFieldDoMap.containsKey(fieldName)) {
                targetListingDataFieldDo.setDeleted(Boolean.TRUE);
                BaseDo.update(targetListingDataFieldDo, operatingUserProfile.getUid(), LocalDateTime.now());
                toDeleteItemDoList.add(targetListingDataFieldDo);
            }
        });

        if (!CollectionUtils.isEmpty(toAddItemDoList)) {
            this.listingDataFieldRepository.saveAll(toAddItemDoList);
        }
        if (!CollectionUtils.isEmpty(toUpdateItemDoList)) {
            this.listingDataFieldRepository.saveAll(toUpdateItemDoList);
        }
        if (!CollectionUtils.isEmpty(toDeleteItemDoList)) {
            this.listingDataFieldRepository.saveAll(toDeleteItemDoList);
        }
    }

    private void copyListingExtended(
            Long sourceDataFacetUid,
            Long targetDataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        ListingExtendedDo sourceListingExtendedDo =
                this.listingExtendedRepository.findByDataFacetUid(sourceDataFacetUid);
        if (sourceListingExtendedDo == null) {
            return;
        }
        ListingExtendedDo targetListingExtendedDo =
                this.listingExtendedRepository.findByDataFacetUid(targetDataFacetUid);
        if (targetListingExtendedDo == null) {
            targetListingExtendedDo = new ListingExtendedDo();
            targetListingExtendedDo.setDefaultPageSize(sourceListingExtendedDo.getDefaultPageSize());
            targetListingExtendedDo.setEnabledVerticalScrolling(sourceListingExtendedDo.getEnabledVerticalScrolling());
            targetListingExtendedDo.setVerticalScrollingHeightThreshold(sourceListingExtendedDo.getVerticalScrollingHeightThreshold());
            targetListingExtendedDo.setEnabledColumnNo(sourceListingExtendedDo.getEnabledColumnNo());
            targetListingExtendedDo.setEnabledFreezeTopRows(sourceListingExtendedDo.getEnabledFreezeTopRows());
            targetListingExtendedDo.setInclusiveTopRows(sourceListingExtendedDo.getInclusiveTopRows());
            targetListingExtendedDo.setEnabledFreezeLeftColumns(sourceListingExtendedDo.getEnabledFreezeLeftColumns());
            targetListingExtendedDo.setInclusiveLeftColumns(sourceListingExtendedDo.getInclusiveLeftColumns());
            targetListingExtendedDo.setEnabledFreezeRightColumns(sourceListingExtendedDo.getEnabledFreezeRightColumns());
            targetListingExtendedDo.setInclusiveRightColumns(sourceListingExtendedDo.getInclusiveRightColumns());
            targetListingExtendedDo.setDataFacetUid(targetDataFacetUid);
            BaseDo.create(targetListingExtendedDo, operatingUserProfile.getUid(), LocalDateTime.now());
        } else {
            targetListingExtendedDo.setDefaultPageSize(sourceListingExtendedDo.getDefaultPageSize());
            targetListingExtendedDo.setEnabledVerticalScrolling(sourceListingExtendedDo.getEnabledVerticalScrolling());
            targetListingExtendedDo.setVerticalScrollingHeightThreshold(sourceListingExtendedDo.getVerticalScrollingHeightThreshold());
            targetListingExtendedDo.setEnabledColumnNo(sourceListingExtendedDo.getEnabledColumnNo());
            targetListingExtendedDo.setEnabledFreezeTopRows(sourceListingExtendedDo.getEnabledFreezeTopRows());
            targetListingExtendedDo.setInclusiveTopRows(sourceListingExtendedDo.getInclusiveTopRows());
            targetListingExtendedDo.setEnabledFreezeLeftColumns(sourceListingExtendedDo.getEnabledFreezeLeftColumns());
            targetListingExtendedDo.setInclusiveLeftColumns(sourceListingExtendedDo.getInclusiveLeftColumns());
            targetListingExtendedDo.setEnabledFreezeRightColumns(sourceListingExtendedDo.getEnabledFreezeRightColumns());
            targetListingExtendedDo.setInclusiveRightColumns(sourceListingExtendedDo.getInclusiveRightColumns());
            BaseDo.update(targetListingExtendedDo, operatingUserProfile.getUid(), LocalDateTime.now());
        }
        this.listingExtendedRepository.save(targetListingExtendedDo);
    }

    private void copySortingDataFields(
            List<String> sameFieldNameList,
            Long sourceDataFacetUid,
            Long targetDataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        List<SortingDataFieldDo> sourceSortingDataFieldDoList =
                this.sortingDataFieldRepository.findByDataFacetUidAndFieldNameIn(
                        sourceDataFacetUid, sameFieldNameList);
        if (CollectionUtils.isEmpty(sourceSortingDataFieldDoList)) {
            return;
        }
        Map<String, SortingDataFieldDo> sourceSortingDataFieldDoMap = new HashMap<>();
        sourceSortingDataFieldDoList.forEach(sortingDataFieldDo -> {
            sourceSortingDataFieldDoMap.put(sortingDataFieldDo.getFieldName(), sortingDataFieldDo);
        });

        List<SortingDataFieldDo> targetSortingDataFieldDoList =
                this.sortingDataFieldRepository.findByDataFacetUidAndFieldNameIn(
                        targetDataFacetUid, sameFieldNameList);
        Map<String, SortingDataFieldDo> targetSortingDataFieldDoMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(targetSortingDataFieldDoList)) {
            targetSortingDataFieldDoList.forEach(sortingDataFieldDo -> {
                targetSortingDataFieldDoMap.put(sortingDataFieldDo.getFieldName(), sortingDataFieldDo);
            });
        }

        List<SortingDataFieldDo> toAddItemDoList = new LinkedList<>();
        List<SortingDataFieldDo> toUpdateItemDoList = new LinkedList<>();
        List<SortingDataFieldDo> toDeleteItemDoList = new LinkedList<>();

        sourceSortingDataFieldDoList.forEach(sourceSortingDataFieldDo -> {
            SortingDataFieldDo targetSortingDataFieldDo = targetSortingDataFieldDoMap.get(sourceSortingDataFieldDo.getFieldName());
            if (targetSortingDataFieldDo == null) {
                targetSortingDataFieldDo = new SortingDataFieldDo();
                targetSortingDataFieldDo.setFieldName(sourceSortingDataFieldDo.getFieldName());
                targetSortingDataFieldDo.setDirection(sourceSortingDataFieldDo.getDirection());
                targetSortingDataFieldDo.setSortingSequence(sourceSortingDataFieldDo.getSortingSequence());
                targetSortingDataFieldDo.setDataFacetUid(targetDataFacetUid);
                BaseDo.create(targetSortingDataFieldDo, operatingUserProfile.getUid(), LocalDateTime.now());
                toAddItemDoList.add(targetSortingDataFieldDo);

                targetSortingDataFieldDoMap.put(targetSortingDataFieldDo.getFieldName(),
                        targetSortingDataFieldDo);
            } else {
                targetSortingDataFieldDo.setDirection(sourceSortingDataFieldDo.getDirection());
                targetSortingDataFieldDo.setSortingSequence(sourceSortingDataFieldDo.getSortingSequence());
                BaseDo.update(targetSortingDataFieldDo, operatingUserProfile.getUid(), LocalDateTime.now());
                toUpdateItemDoList.add(targetSortingDataFieldDo);
            }
        });

        // 还需要将 target sorting data field 存在，但在 source sorting data filed 不存在的，删除
        targetSortingDataFieldDoMap.forEach((fieldName, targetSortingDataFieldDo) -> {
            if (!sourceSortingDataFieldDoMap.containsKey(fieldName)) {
                targetSortingDataFieldDo.setDeleted(Boolean.TRUE);
                BaseDo.update(targetSortingDataFieldDo, operatingUserProfile.getUid(), LocalDateTime.now());
                toDeleteItemDoList.add(targetSortingDataFieldDo);
            }
        });

        if (!CollectionUtils.isEmpty(toAddItemDoList)) {
            this.sortingDataFieldRepository.saveAll(toAddItemDoList);
        }
        if (!CollectionUtils.isEmpty(toUpdateItemDoList)) {
            this.sortingDataFieldRepository.saveAll(toUpdateItemDoList);
        }
        if (!CollectionUtils.isEmpty(toDeleteItemDoList)) {
            this.sortingDataFieldRepository.saveAll(toDeleteItemDoList);
        }
    }

    private void copyExportBasic(
            Long sourceDataFacetUid,
            Long targetDataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        ExportBasicDo sourceExportBasicDo =
                this.exportBasicRepository.findByDataFacetUid(sourceDataFacetUid);
        if (sourceExportBasicDo == null) {
            return;
        }
        ExportBasicDo targetExportBasicDo =
                this.exportBasicRepository.findByDataFacetUid(targetDataFacetUid);
        if (targetExportBasicDo == null) {
            targetExportBasicDo = new ExportBasicDo();
            targetExportBasicDo.setEnabledExportCsv(sourceExportBasicDo.getEnabledExportCsv());
            targetExportBasicDo.setExportCsvStrategy(sourceExportBasicDo.getExportCsvStrategy());
            targetExportBasicDo.setEnabledExportExcel(sourceExportBasicDo.getEnabledExportExcel());
            targetExportBasicDo.setEnabledExportDataAndImages(sourceExportBasicDo.getEnabledExportDataAndImages());
            targetExportBasicDo.setEnabledExportDataAndFiles(sourceExportBasicDo.getEnabledExportDataAndFiles());
            targetExportBasicDo.setDataFacetUid(targetDataFacetUid);
            BaseDo.create(targetExportBasicDo, operatingUserProfile.getUid(), LocalDateTime.now());
        } else {
            targetExportBasicDo.setEnabledExportCsv(sourceExportBasicDo.getEnabledExportCsv());
            targetExportBasicDo.setExportCsvStrategy(sourceExportBasicDo.getExportCsvStrategy());
            targetExportBasicDo.setEnabledExportExcel(sourceExportBasicDo.getEnabledExportExcel());
            targetExportBasicDo.setEnabledExportDataAndImages(sourceExportBasicDo.getEnabledExportDataAndImages());
            targetExportBasicDo.setEnabledExportDataAndFiles(sourceExportBasicDo.getEnabledExportDataAndFiles());
            BaseDo.update(targetExportBasicDo, operatingUserProfile.getUid(), LocalDateTime.now());
        }
        this.exportBasicRepository.save(targetExportBasicDo);
    }

    private void copyDataPermissions(
            List<String> sameFieldNameList,
            Long sourceDataFacetUid,
            Long targetDataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        List<DataPermissionDo> sourceDataPermissionDoList =
                this.dataPermissionRepository.findByDataFacetUid(sourceDataFacetUid);
        if (CollectionUtils.isEmpty(sourceDataPermissionDoList)) {
            return;
        }

        Map<String, DataPermissionDo> availableToCopyDataPermissionDoMap = new HashMap<>();
        sourceDataPermissionDoList.forEach(dataPermissionDo -> {
            List<String> fieldNameList = dataPermissionDo.getContent().getFieldNameList();

            if (CollectionUtils.isEmpty(fieldNameList)) {
                return;
            }

            StringBuilder mapKey = new StringBuilder();
            boolean matched = true;
            for (String fieldName : fieldNameList) {
                if (!sameFieldNameList.contains(fieldName)) {
                    matched = false;
                    break;
                }

                if (mapKey.length() > 0) {
                    mapKey.append("###");
                }

                mapKey.append(fieldName);
            }

            if (matched) {
                availableToCopyDataPermissionDoMap.put(mapKey.toString(), dataPermissionDo);
            }
        });

        if (CollectionUtils.isEmpty(availableToCopyDataPermissionDoMap)) {
            return;
        }

        List<DataPermissionDo> toAddDataPermissionDoList = new LinkedList<>();
        List<DataPermissionDo> toUpdateDataPermissionDoList = new LinkedList<>();
        List<DataPermissionDo> targetDataPermissionDoList =
                this.dataPermissionRepository.findByDataFacetUid(targetDataFacetUid);
        if (CollectionUtils.isEmpty(targetDataPermissionDoList)) {
            availableToCopyDataPermissionDoMap.values().forEach(sourceDataPermissionDo -> {
                DataPermissionDo targetDataPermissionDo = new DataPermissionDo();
                targetDataPermissionDo.setUid(this.idHelper.getNextDistributedId(DataPermissionDo.RESOURCE_NAME));
                targetDataPermissionDo.setContent(sourceDataPermissionDo.getContent());
                targetDataPermissionDo.setEnabled(sourceDataPermissionDo.getEnabled());
                targetDataPermissionDo.setDataFacetUid(targetDataFacetUid);
                BaseDo.create(targetDataPermissionDo, operatingUserProfile.getUid(), LocalDateTime.now());
                toAddDataPermissionDoList.add(targetDataPermissionDo);
            });
        } else {
            List<String> toUpdateMapKeyList = new LinkedList<>();
            targetDataPermissionDoList.forEach(targetDataPermissionDo -> {
                if (targetDataPermissionDo.getContent() != null
                        && !CollectionUtils.isEmpty(targetDataPermissionDo.getContent().getFieldNameList())) {
                    StringBuilder mapKey = new StringBuilder();
                    for (String fieldName : targetDataPermissionDo.getContent().getFieldNameList()) {
                        if (mapKey.length() > 0) {
                            mapKey.append("###");
                        }

                        mapKey.append(fieldName);
                    }

                    if (availableToCopyDataPermissionDoMap.containsKey(mapKey.toString())) {
                        DataPermissionDo sourceDataPermissionDo =
                                availableToCopyDataPermissionDoMap.get(mapKey.toString());
                        targetDataPermissionDo.setContent(sourceDataPermissionDo.getContent());
                        targetDataPermissionDo.setEnabled(sourceDataPermissionDo.getEnabled());
                        BaseDo.update(targetDataPermissionDo, operatingUserProfile.getUid(), LocalDateTime.now());
                        toUpdateDataPermissionDoList.add(targetDataPermissionDo);

                        toUpdateMapKeyList.add(mapKey.toString());
                    }
                }
            });

            availableToCopyDataPermissionDoMap.forEach((mapKey, sourceDataPermissionDo) -> {
                if (toUpdateMapKeyList.contains(mapKey)) {
                    return;
                }

                DataPermissionDo targetDataPermissionDo = new DataPermissionDo();
                targetDataPermissionDo.setUid(this.idHelper.getNextDistributedId(DataPermissionDo.RESOURCE_NAME));
                targetDataPermissionDo.setContent(sourceDataPermissionDo.getContent());
                targetDataPermissionDo.setEnabled(sourceDataPermissionDo.getEnabled());
                targetDataPermissionDo.setDataFacetUid(targetDataFacetUid);
                BaseDo.create(targetDataPermissionDo, operatingUserProfile.getUid(), LocalDateTime.now());
                toAddDataPermissionDoList.add(targetDataPermissionDo);
            });
        }

        if (!CollectionUtils.isEmpty(toAddDataPermissionDoList)) {
            this.dataPermissionRepository.saveAll(toAddDataPermissionDoList);
        }

        if (!CollectionUtils.isEmpty(toUpdateDataPermissionDoList)) {
            this.dataPermissionRepository.saveAll(toUpdateDataPermissionDoList);
        }
    }

    private void copyAdvancedFeature(
            Long sourceDataFacetUid,
            Long targetDataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        AdvancedFeatureDo sourceAdvancedFeatureDo =
                this.advancedFeatureRepository.findByDataFacetUid(sourceDataFacetUid);
        if (sourceAdvancedFeatureDo == null) {
            return;
        }
        AdvancedFeatureDo targetAdvancedFeatureDo =
                this.advancedFeatureRepository.findByDataFacetUid(targetDataFacetUid);
        if (targetAdvancedFeatureDo == null) {
            targetAdvancedFeatureDo = new AdvancedFeatureDo();
            targetAdvancedFeatureDo.setContent(sourceAdvancedFeatureDo.getContent());
            targetAdvancedFeatureDo.setDataFacetUid(targetDataFacetUid);
            BaseDo.create(targetAdvancedFeatureDo, operatingUserProfile.getUid(), LocalDateTime.now());
        } else {
            targetAdvancedFeatureDo.setContent(sourceAdvancedFeatureDo.getContent());
            BaseDo.update(targetAdvancedFeatureDo, operatingUserProfile.getUid(), LocalDateTime.now());
        }
        this.advancedFeatureRepository.save(targetAdvancedFeatureDo);
    }
}
