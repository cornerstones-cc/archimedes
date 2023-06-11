package cc.cornerstones.biz.datafacet.service.impl;

import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceIntegrityException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.AbcTuple3;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcDateUtils;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.archimedes.extensions.types.TreeNode;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DfsServiceAgentService;
import cc.cornerstones.biz.datafacet.dto.DataFacetExportDto;
import cc.cornerstones.biz.datafacet.dto.DataFacetQueryDto;
import cc.cornerstones.biz.datafacet.dto.DataPermissionContentDto;
import cc.cornerstones.biz.datafacet.entity.*;
import cc.cornerstones.biz.datafacet.persistence.*;
import cc.cornerstones.biz.datafacet.service.inf.ExecuteDataFacetService;
import cc.cornerstones.biz.datafacet.share.constants.DataFieldTypeEnum;
import cc.cornerstones.biz.datafacet.share.constants.TemplateColumnHeaderSourceEnum;
import cc.cornerstones.biz.datafacet.share.types.FieldTypeExtensionFile;
import cc.cornerstones.biz.datafacet.share.types.ListingFieldTypeExtensionFile;
import cc.cornerstones.biz.datafacet.share.types.ListingFieldTypeExtensionImage;
import cc.cornerstones.biz.datatable.dto.DataTableExportDto;
import cc.cornerstones.biz.datatable.dto.DataTableQueryDto;
import cc.cornerstones.biz.datatable.service.inf.ExecuteDataTableService;
import cc.cornerstones.biz.operations.accesslogging.dto.CreateOrUpdateQueryLogDto;
import cc.cornerstones.biz.operations.accesslogging.service.inf.AccessLoggingService;
import cc.cornerstones.biz.resourceownership.service.inf.ResourceOwnershipService;
import cc.cornerstones.biz.share.constants.ExpressionTypeEnum;
import cc.cornerstones.biz.share.constants.SelectionFieldTypeEnum;
import cc.cornerstones.biz.share.types.*;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.metadata.CellData;
import com.alibaba.excel.metadata.CellExtra;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ExecuteDataFacetServiceImpl implements ExecuteDataFacetService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteDataFacetServiceImpl.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataFacetRepository dataFacetRepository;

    @Autowired
    private DataFieldRepository dataFieldRepository;

    @Autowired
    private ListingDataFieldRepository listingDataFieldRepository;

    @Autowired
    private ExportExtendedTemplateRepository exportExtendedTemplateRepository;

    @Autowired
    private ExecuteDataTableService executeDataTableService;

    @Autowired
    private DfsServiceAgentService dfsServiceAgentService;

    @Autowired
    private DataPermissionRepository dataPermissionRepository;

    @Autowired
    private ResourceOwnershipService resourceOwnershipService;

    @Autowired
    private AccessLoggingService accessLoggingService;

    @Autowired
    private IdHelper idHelper;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    private void recursiveExtractLevelUidAndLevelIndex(
            TreeNode treeNode,
            Integer parentLevelIndex,
            Map<Long, Integer> levelUidAndLevelIndexMap) {
        Integer levelIndex = 0;
        if (parentLevelIndex != null) {
            levelIndex = parentLevelIndex + 1;
            parentLevelIndex = levelIndex;
        }

        levelUidAndLevelIndexMap.put(treeNode.getUid(), levelIndex);

        if (CollectionUtils.isEmpty(treeNode.getChildren())) {
            return;
        }

        parentLevelIndex = levelIndex;

        for (TreeNode childTreeNode : treeNode.getChildren()) {
            recursiveExtractLevelUidAndLevelIndex(childTreeNode, parentLevelIndex, levelUidAndLevelIndexMap);
        }
    }

    private void transformHierarchyToFlat(
            List<List<Object>> rows,
            List<Object> parentColumns,
            List<cc.cornerstones.archimedes.extensions.types.TreeNode> treeNodeList) {
        treeNodeList.forEach(treeNode -> {
            if (CollectionUtils.isEmpty(treeNode.getChildren())) {
                // 叶子节点，一行就绪
                List<Object> row = new LinkedList<>();
                row.addAll(parentColumns);
                row.add(treeNode.getName());
                rows.add(row);
            } else {
                parentColumns.add(treeNode.getName());
                transformHierarchyToFlat(rows, parentColumns, treeNode.getChildren());
            }
        });

        if (!parentColumns.isEmpty()) {
            parentColumns.remove(parentColumns.size() - 1);
        }
    }

    private List<DataPermissionFilter> buildDataPermissionFilters(
            Long dataFacetUid,
            List<DataPermissionDo> dataPermissionDoList,
            Map<String, DataFieldDo> dataFieldNameAndDataFieldDoMap,
            UserProfile operatingUserProfile) {
        List<DataPermissionFilter> result = new LinkedList<>();

        if (!CollectionUtils.isEmpty(dataPermissionDoList)) {
            dataPermissionDoList.forEach(dataPermissionDo -> {
                DataPermissionContentDto dataPermissionContentDto = dataPermissionDo.getContent();
                Long dataPermissionServiceAgentUid = dataPermissionContentDto.getDataPermissionServiceAgentUid();
                Long resourceCategoryUid = dataPermissionContentDto.getResourceCategoryUid();
                Map<Long, String> levelUidAndFieldNameMap = dataPermissionContentDto.getResourceStructureLevelMapping();
                cc.cornerstones.archimedes.extensions.types.TreeNode resourceStructureTreeNode =
                        this.resourceOwnershipService.treeListingAllNodesOfResourceStructureHierarchy(
                                dataPermissionServiceAgentUid,
                                resourceCategoryUid,
                                operatingUserProfile);

                Map<Long, Integer> fullLevelUidAndLevelIndexMap = new HashMap<>();
                recursiveExtractLevelUidAndLevelIndex(resourceStructureTreeNode, null,
                        fullLevelUidAndLevelIndexMap);
                Map<Integer, String> requiredLevelIndexAndFieldNameMap = new HashMap<>();
                levelUidAndFieldNameMap.forEach((levelUid, fieldName) -> {
                    requiredLevelIndexAndFieldNameMap.put(fullLevelUidAndLevelIndexMap.get(levelUid), fieldName);
                });

                List<cc.cornerstones.archimedes.extensions.types.TreeNode> resourceContentTreeNodeList =
                        this.resourceOwnershipService.treeListingAllNodesOfResourceContentHierarchy(
                                dataPermissionServiceAgentUid,
                                resourceCategoryUid,
                                operatingUserProfile);

                if (CollectionUtils.isEmpty(resourceContentTreeNodeList)) {
                    // TODO 不允许访问
                    return;
                }

                List<Integer> levelIndexList = new ArrayList<>(fullLevelUidAndLevelIndexMap.values());
                List<List<Object>> rows = new LinkedList<>();
                List<Object> parentColumns = new LinkedList<>();
                transformHierarchyToFlat(rows, parentColumns, resourceContentTreeNodeList);

                DataPermissionFilter dataPermissionFilter = new DataPermissionFilter();
                result.add(dataPermissionFilter);
                List<List<AbcTuple3<String, String, DataFieldTypeEnum>>> l0 = new LinkedList<>();
                dataPermissionFilter.setContent(l0);
                for (List<Object> row : rows) {
                    List<AbcTuple3<String, String, DataFieldTypeEnum>> l1 = new LinkedList<>();

                    for (int levelIndex = 0; levelIndex < row.size(); levelIndex++) {
                        if (requiredLevelIndexAndFieldNameMap.containsKey(levelIndex)) {
                            LOGGER.info("********levelIndex:{}, fieldName:{}", levelIndex,
                                    requiredLevelIndexAndFieldNameMap.get(levelIndex));

                            String fieldName = requiredLevelIndexAndFieldNameMap.get(levelIndex);
                            Object fieldValue = row.get(levelIndex);
                            AbcTuple3<String, String, DataFieldTypeEnum> tuple =
                                    new AbcTuple3<String, String, DataFieldTypeEnum>(fieldName,
                                            String.valueOf(fieldValue),
                                            dataFieldNameAndDataFieldDoMap.get(fieldName).getType());
                            l1.add(tuple);
                        }
                    }

                    if (!CollectionUtils.isEmpty(l1)) {
                        l0.add(l1);
                    }
                }
            });
        }

        return result;
    }

    private DataTableQueryDto buildDataTableQuery(
            DataFacetDo dataFacetDo,
            DataFacetQueryDto dataFacetQueryDto) {
        //
        // Step 1, pre-processing
        //
        Long dataFacetUid = dataFacetDo.getUid();
        String dataFacetName = dataFacetDo.getName();

        List<DataFieldDo> dataFieldDoList = this.dataFieldRepository.findByDataFacetUid(dataFacetUid);
        Map<String, DataFieldDo> dataFieldNameAndDataFieldDoMap = new HashMap<>(dataFieldDoList.size());
        if (CollectionUtils.isEmpty(dataFieldDoList)) {
            throw new AbcResourceIntegrityException(String.format("cannot find any data field of data facet:%d",
                    dataFacetUid));
        }
        dataFieldDoList.forEach(dataFieldDo -> {
            dataFieldNameAndDataFieldDoMap.put(dataFieldDo.getName(), dataFieldDo);
        });

        //
        // Step 2, core-processing
        //

        DataTableQueryDto dataTableQueryDto = new DataTableQueryDto();

        //
        // Step 2.1, 补充 selection fields 信息
        //
        if (!CollectionUtils.isEmpty(dataFacetQueryDto.getSelectionFields())) {
            dataFacetQueryDto.getSelectionFields().forEach(selectionField -> {
                switch (selectionField.getType()) {
                    case PLAIN: {
                        PlainSelectionField plainSelectionField = JSONObject.toJavaObject(selectionField.getContent()
                                , PlainSelectionField.class);

                        DataFieldDo dataFieldDo =
                                dataFieldNameAndDataFieldDoMap.get(plainSelectionField.getFieldName());
                        if (dataFieldDo == null) {
                            throw new AbcResourceIntegrityException(String.format("cannot find data field of name:%s," +
                                    " data facet:%d", plainSelectionField.getFieldName(), dataFacetUid));
                        }

                        // 补充字段信息
                        plainSelectionField.setFieldLabel(dataFieldDo.getLabel());
                        plainSelectionField.setFieldType(dataFieldDo.getType());
                        //
                        selectionField.setContent((JSONObject) JSONObject.toJSON(plainSelectionField));
                    }
                    break;
                    case EXPRESSION: {
                        ExpressionSelectionField expressionSelectionField =
                                JSONObject.toJavaObject(selectionField.getContent(), ExpressionSelectionField.class);
                        switch (expressionSelectionField.getType()) {
                            case AGGREGATE_FUNCTION:
                                break;
                            case COMPOUND:
                                break;
                            case OPERATOR:
                                break;
                            case NON_AGGREGATE_FUNCTION:
                                break;
                            case PLACEHOLDER:
                                break;
                            default:
                                throw new AbcResourceConflictException(String.format("unsupported expression selection" +
                                                " field type:%s",
                                        expressionSelectionField.getType()));
                        }
                    }
                    break;
                    default:
                        throw new AbcResourceConflictException(String.format("unsupported selection field type:%s",
                                selectionField.getType()));
                }
            });
        }

        //
        // Step 2.2, 补充 data permission filters
        //
        List<DataPermissionDo> dataPermissionDoList =
                this.dataPermissionRepository.findByDataFacetUidAndEnabled(dataFacetUid, Boolean.TRUE);
        if (CollectionUtils.isEmpty(dataPermissionDoList)) {
            dataTableQueryDto.setRequireDataPermissionFilters(Boolean.FALSE);
        } else {
            List<DataPermissionFilter> dataPermissionFilters = buildDataPermissionFilters(
                    dataFacetUid,
                    dataPermissionDoList,
                    dataFieldNameAndDataFieldDoMap,
                    dataFacetQueryDto.getOperatingUserProfile());

            if (CollectionUtils.isEmpty(dataPermissionFilters)) {
                // 要求有授权，但是一个都没发现，不允许继续
                LOGGER.warn("no data permission granted, data facet {} ({}), operating user {} ({})",
                        dataFacetUid,
                        dataFacetName,
                        dataFacetQueryDto.getOperatingUserProfile().getUid(),
                        dataFacetQueryDto.getOperatingUserProfile().getDisplayName());
            }

            dataTableQueryDto.setRequireDataPermissionFilters(Boolean.TRUE);
            dataTableQueryDto.setDataPermissionFilters(dataPermissionFilters);
        }

        //
        // Step 2.3, 构造结果
        //
        dataTableQueryDto.setSelectionFields(dataFacetQueryDto.getSelectionFields());
        dataTableQueryDto.setPlainFilters(dataFacetQueryDto.getPlainFilters());
        dataTableQueryDto.setStatementFilter(dataFacetQueryDto.getStatementFilter());
        dataTableQueryDto.setGroupByFields(dataFacetQueryDto.getGroupByFields());
        dataTableQueryDto.setGroupFilters(dataFacetQueryDto.getGroupFilters());
        dataTableQueryDto.setCascadingFilters(dataFacetQueryDto.getCascadingFilters());
        dataTableQueryDto.setSort(dataFacetQueryDto.getSort());
        dataTableQueryDto.setPagination(dataFacetQueryDto.getPagination());

        return dataTableQueryDto;
    }

    private DataTableExportDto buildDataTableExport(
            DataFacetDo dataFacetDo,
            DataFacetExportDto dataFacetExportDto) {
        //
        // Step 1, pre-processing
        //
        Long dataFacetUid = dataFacetDo.getUid();
        String dataFacetName = dataFacetDo.getName();

        List<DataFieldDo> dataFieldDoList = this.dataFieldRepository.findByDataFacetUid(dataFacetUid);
        Map<String, DataFieldDo> dataFieldNameAndDataFieldDoMap = new HashMap<>(dataFieldDoList.size());
        Map<String, DataFieldDo> dataFieldLabelAndDataFieldDoMap = new HashMap<>(dataFieldDoList.size());
        if (CollectionUtils.isEmpty(dataFieldDoList)) {
            throw new AbcResourceIntegrityException(String.format("cannot find any data field of data facet:%d",
                    dataFacetUid));
        }
        dataFieldDoList.forEach(dataFieldDo -> {
            dataFieldNameAndDataFieldDoMap.put(dataFieldDo.getName(), dataFieldDo);
            dataFieldLabelAndDataFieldDoMap.put(dataFieldDo.getLabel(), dataFieldDo);
        });
        List<ListingDataFieldDo> listingDataFieldDoList =
                this.listingDataFieldRepository.findByDataFacetUid(dataFacetUid, Sort.by(Sort.Order.asc("listingSequence")));
        if (CollectionUtils.isEmpty(listingDataFieldDoList)) {
            throw new AbcResourceIntegrityException(String.format("cannot find any listing data field of data facet:%d",
                    dataFacetUid));
        }
        Map<String, ListingDataFieldDo> listingDataFieldDoMap = new HashMap<>();
        listingDataFieldDoList.forEach(listingDataFieldDo -> {
            listingDataFieldDoMap.put(listingDataFieldDo.getFieldName(), listingDataFieldDo);
        });

        //
        // Step 2, core-processing
        //

        DataTableExportDto dataTableExportDto = new DataTableExportDto();

        //
        // Step 2.1, 补充 selection fields 信息，同时看这些 selection fields 是否涉及 image / file 导出
        //
        List<ExportAttachment> fileAttachmentList = new LinkedList<>();
        List<ExportAttachment> imageAttachmentList = new LinkedList<>();

        if (!CollectionUtils.isEmpty(dataFacetExportDto.getSelectionFields())) {
            dataFacetExportDto.getSelectionFields().forEach(selectionField -> {
                switch (selectionField.getType()) {
                    case PLAIN: {
                        PlainSelectionField plainSelectionField = JSONObject.toJavaObject(selectionField.getContent()
                                , PlainSelectionField.class);

                        DataFieldDo dataFieldDo =
                                dataFieldNameAndDataFieldDoMap.get(plainSelectionField.getFieldName());
                        if (dataFieldDo == null) {
                            throw new AbcResourceIntegrityException(String.format("cannot find data field of name:%s," +
                                    " data facet:%d", plainSelectionField.getFieldName(), dataFacetUid));
                        }

                        // 补充字段信息
                        plainSelectionField.setFieldLabel(dataFieldDo.getLabel());
                        plainSelectionField.setFieldType(dataFieldDo.getType());
                        //
                        selectionField.setContent((JSONObject) JSONObject.toJSON(plainSelectionField));

                        //
                        switch (dataFieldDo.getType()) {
                            case FILE: {
                                if (dataFieldDo.getTypeExtension() != null
                                        && !dataFieldDo.getTypeExtension().isEmpty()) {
                                    ExportAttachment attachment = new ExportAttachment();
                                    attachment.setColumnName(dataFieldDo.getName());
                                    FieldTypeExtensionFile fieldTypeExtensionFile =
                                            JSONObject.toJavaObject(dataFieldDo.getTypeExtension(),
                                                    FieldTypeExtensionFile.class);
                                    attachment.setSource(fieldTypeExtensionFile);

                                    ListingDataFieldDo listingDataFieldDo =
                                            listingDataFieldDoMap.get(dataFieldDo.getName());
                                    if (listingDataFieldDo != null) {
                                        if (listingDataFieldDo.getExtension() != null
                                                && !listingDataFieldDo.getExtension().isEmpty()) {
                                            ListingFieldTypeExtensionFile listingFieldTypeExtensionFile =
                                                    JSONObject.toJavaObject(listingDataFieldDo.getExtension(),
                                                            ListingFieldTypeExtensionFile.class);
                                            attachment.setNamingPolicy(listingFieldTypeExtensionFile.getNamingPolicy());
                                            attachment.setNamingPolicyExtCombine(listingFieldTypeExtensionFile.getNamingPolicyExtCombine());
                                        }
                                    }

                                    fileAttachmentList.add(attachment);
                                }
                            }
                            break;
                            case IMAGE: {
                                if (dataFieldDo.getTypeExtension() != null
                                        && !dataFieldDo.getTypeExtension().isEmpty()) {
                                    ExportAttachment attachment = new ExportAttachment();
                                    attachment.setColumnName(dataFieldDo.getName());
                                    FieldTypeExtensionFile fieldTypeExtensionFile =
                                            JSONObject.toJavaObject(dataFieldDo.getTypeExtension(),
                                                    FieldTypeExtensionFile.class);
                                    attachment.setSource(fieldTypeExtensionFile);

                                    ListingDataFieldDo listingDataFieldDo =
                                            listingDataFieldDoMap.get(dataFieldDo.getName());
                                    if (listingDataFieldDo != null) {
                                        if (listingDataFieldDo.getExtension() != null
                                                && !listingDataFieldDo.getExtension().isEmpty()) {
                                            ListingFieldTypeExtensionImage listingFieldTypeExtensionImage =
                                                    JSONObject.toJavaObject(listingDataFieldDo.getExtension(),
                                                            ListingFieldTypeExtensionImage.class);
                                            attachment.setNamingPolicy(listingFieldTypeExtensionImage.getNamingPolicy());
                                            attachment.setNamingPolicyExtCombine(listingFieldTypeExtensionImage.getNamingPolicyExtCombine());
                                        }
                                    }

                                    imageAttachmentList.add(attachment);
                                }
                            }
                            break;
                        }
                    }
                    break;
                    case EXPRESSION: {
                        ExpressionSelectionField expressionSelectionField =
                                JSONObject.toJavaObject(selectionField.getContent(), ExpressionSelectionField.class);
                        switch (expressionSelectionField.getType()) {
                            case AGGREGATE_FUNCTION:
                                break;
                            case COMPOUND:
                                break;
                            case OPERATOR:
                                break;
                            case NON_AGGREGATE_FUNCTION:
                                break;
                            case PLACEHOLDER:
                                break;
                            default:
                                throw new AbcResourceConflictException(String.format("unsupported expression selection" +
                                                " field type:%s",
                                        expressionSelectionField.getType()));
                        }
                    }
                    break;
                    default:
                        throw new AbcResourceConflictException(String.format("unsupported selection field type:%s",
                                selectionField.getType()));
                }
            });
        }

        //
        // Step 2.2, 补充 data permission filters
        //
        List<DataPermissionDo> dataPermissionDoList =
                this.dataPermissionRepository.findByDataFacetUidAndEnabled(dataFacetUid, Boolean.TRUE);
        if (CollectionUtils.isEmpty(dataPermissionDoList)) {
            dataTableExportDto.setRequireDataPermissionFilters(Boolean.FALSE);
        } else {
            List<DataPermissionFilter> dataPermissionFilters = buildDataPermissionFilters(
                    dataFacetUid,
                    dataPermissionDoList,
                    dataFieldNameAndDataFieldDoMap,
                    dataFacetExportDto.getOperatingUserProfile());

            if (CollectionUtils.isEmpty(dataPermissionFilters)) {
                // 要求有授权，但是一个都没发现，不允许继续
                LOGGER.warn("no data permission granted, data facet {} ({}), operating user {} ({})",
                        dataFacetUid,
                        dataFacetName,
                        dataFacetExportDto.getOperatingUserProfile().getUid(),
                        dataFacetExportDto.getOperatingUserProfile().getDisplayName());
            }

            dataTableExportDto.setRequireDataPermissionFilters(Boolean.TRUE);
            dataTableExportDto.setDataPermissionFilters(dataPermissionFilters);
        }

        //
        // Step 2.3, 构造结果
        //
        dataTableExportDto.setDataFacetUid(dataFacetUid);
        dataTableExportDto.setDataFacetName(dataFacetName);
        dataTableExportDto.setVisibleSelectionFields(dataFacetExportDto.getVisibleSelectionFields());
        dataTableExportDto.setSelectionFields(dataFacetExportDto.getSelectionFields());
        dataTableExportDto.setPlainFilters(dataFacetExportDto.getPlainFilters());
        dataTableExportDto.setStatementFilter(dataFacetExportDto.getStatementFilter());
        dataTableExportDto.setGroupByFields(dataFacetExportDto.getGroupByFields());
        dataTableExportDto.setGroupFilters(dataFacetExportDto.getGroupFilters());
        dataTableExportDto.setCascadingFilters(dataFacetExportDto.getCascadingFilters());
        dataTableExportDto.setSort(dataFacetExportDto.getSort());

        dataTableExportDto.setFileAttachmentList(fileAttachmentList);
        dataTableExportDto.setImageAttachmentList(imageAttachmentList);
        dataTableExportDto.setExportOption(dataFacetExportDto.getExportOption());

        //
        // 补充 export extended template，同时要根据 template 的要求字段覆盖 selection fields
        //
        switch (dataFacetExportDto.getExportOption()) {
            case EXPORT_AS_TEMPLATE: {
                Long exportExtendedTemplateUid = dataFacetExportDto.getExportExtendedTemplateUid();
                ExportExtendedTemplateDo exportExtendedTemplateDo =
                        this.exportExtendedTemplateRepository.findByUid(exportExtendedTemplateUid);
                if (exportExtendedTemplateDo == null) {
                    throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                            ExportExtendedTemplateDo.RESOURCE_SYMBOL, exportExtendedTemplateUid));
                }
                File exportExtendedTemplateFile =
                        this.dfsServiceAgentService.downloadFile(exportExtendedTemplateDo.getDfsServiceAgentUid(),
                                exportExtendedTemplateDo.getFileId(), dataFacetExportDto.getOperatingUserProfile());
                dataTableExportDto.setExportExtendedTemplateFile(exportExtendedTemplateFile);

                Map<Integer, String> head = new HashMap<>();
                EasyExcel.read(exportExtendedTemplateFile, new ReadListener<Map<String, Object>>() {

                    /**
                     * All listeners receive this method when any one Listener does an error report. If an exception is thrown here, the
                     * entire read will terminate.
                     *
                     * @param exception
                     * @param context
                     * @throws Exception
                     */
                    @Override
                    public void onException(Exception exception, AnalysisContext context) throws Exception {
                        LOGGER.error("failed to read export extended template {} - {}",
                                exportExtendedTemplateDo.getFileId(),
                                exportExtendedTemplateFile.getAbsolutePath(), exception);
                    }

                    /**
                     * When analysis one head row trigger invoke function.
                     *
                     * @param headMap
                     * @param context
                     */
                    @Override
                    public void invokeHead(Map<Integer, CellData> headMap, AnalysisContext context) {
                        headMap.forEach((cellNo, cellData) -> {
                            head.put(cellNo, cellData.getStringValue());
                        });
                    }

                    /**
                     * When analysis one row trigger invoke function.
                     *
                     * @param data    one row value. Is is same as {@link AnalysisContext#readRowHolder()}
                     * @param context
                     */
                    @Override
                    public void invoke(Map<String, Object> data, AnalysisContext context) {

                    }

                    /**
                     * The current method is called when extra information is returned
                     *
                     * @param extra   extra information
                     * @param context
                     */
                    @Override
                    public void extra(CellExtra extra, AnalysisContext context) {

                    }

                    /**
                     * if have something to do after all analysis
                     *
                     * @param context
                     */
                    @Override
                    public void doAfterAllAnalysed(AnalysisContext context) {

                    }

                    /**
                     * Verify that there is another piece of data.You can stop the read by returning false
                     *
                     * @param context
                     * @return
                     */
                    @Override
                    public boolean hasNext(AnalysisContext context) {
                        if (!head.isEmpty()) {
                            return false;
                        } else {
                            return true;
                        }
                    }
                }).sheet(0).doRead();

                List<SelectionField> selectionFields = new LinkedList<>();

                // temp code
                if (exportExtendedTemplateDo.getColumnHeaderSource() == null) {
                    exportExtendedTemplateDo.setColumnHeaderSource(TemplateColumnHeaderSourceEnum.FIELD_NAME);
                }
                switch (exportExtendedTemplateDo.getColumnHeaderSource()) {
                    case FIELD_LABEL: {
                        Integer[] arrayOfColumnNo = new Integer[head.keySet().size()];
                        head.keySet().toArray(arrayOfColumnNo);
                        Arrays.sort(arrayOfColumnNo);
                        for (Integer columnNo : arrayOfColumnNo) {
                            String columnName = head.get(columnNo);
                            if (dataFieldLabelAndDataFieldDoMap.containsKey(columnName)) {
                                SelectionField selectionField = new SelectionField();
                                selectionField.setType(SelectionFieldTypeEnum.PLAIN);

                                PlainSelectionField plainSelectionField = new PlainSelectionField();
                                plainSelectionField.setFieldName(dataFieldLabelAndDataFieldDoMap.get(columnName).getName());
                                plainSelectionField.setFieldLabel(dataFieldLabelAndDataFieldDoMap.get(columnName).getLabel());
                                plainSelectionField.setFieldType(dataFieldLabelAndDataFieldDoMap.get(columnName).getType());

                                selectionField.setContent((JSONObject) JSONObject.toJSON(plainSelectionField));

                                selectionFields.add(selectionField);
                            } else {
                                SelectionField selectionField = new SelectionField();
                                selectionField.setType(SelectionFieldTypeEnum.EXPRESSION);

                                ExpressionSelectionField expressionSelectionField = new ExpressionSelectionField();
                                expressionSelectionField.setType(ExpressionTypeEnum.PLACEHOLDER);

                                ExpressionPlaceholderSelectionField expressionPlaceholderSelectionField =
                                        new ExpressionPlaceholderSelectionField();
                                expressionPlaceholderSelectionField.setFieldName(columnName);
                                expressionSelectionField.setContent((JSONObject) JSONObject.toJSON(expressionPlaceholderSelectionField));

                                selectionField.setContent((JSONObject) JSONObject.toJSON(expressionSelectionField));

                                selectionFields.add(selectionField);
                            }
                        }
                    }
                    break;
                    case FIELD_NAME: {
                        Integer[] arrayOfColumnNo = new Integer[head.keySet().size()];
                        head.keySet().toArray(arrayOfColumnNo);
                        Arrays.sort(arrayOfColumnNo);
                        for (Integer columnNo : arrayOfColumnNo) {
                            String columnName = head.get(columnNo);
                            if (dataFieldNameAndDataFieldDoMap.containsKey(columnName)) {
                                SelectionField selectionField = new SelectionField();
                                selectionField.setType(SelectionFieldTypeEnum.PLAIN);

                                PlainSelectionField plainSelectionField = new PlainSelectionField();
                                plainSelectionField.setFieldName(dataFieldNameAndDataFieldDoMap.get(columnName).getName());
                                plainSelectionField.setFieldLabel(dataFieldNameAndDataFieldDoMap.get(columnName).getLabel());
                                plainSelectionField.setFieldType(dataFieldNameAndDataFieldDoMap.get(columnName).getType());

                                selectionField.setContent((JSONObject) JSONObject.toJSON(plainSelectionField));

                                selectionFields.add(selectionField);
                            } else {
                                SelectionField selectionField = new SelectionField();
                                selectionField.setType(SelectionFieldTypeEnum.EXPRESSION);

                                ExpressionSelectionField expressionSelectionField = new ExpressionSelectionField();
                                expressionSelectionField.setType(ExpressionTypeEnum.PLACEHOLDER);

                                ExpressionPlaceholderSelectionField expressionPlaceholderSelectionField =
                                        new ExpressionPlaceholderSelectionField();
                                expressionPlaceholderSelectionField.setFieldName(columnName);
                                expressionSelectionField.setContent((JSONObject) JSONObject.toJSON(expressionPlaceholderSelectionField));

                                selectionField.setContent((JSONObject) JSONObject.toJSON(expressionSelectionField));

                                selectionFields.add(selectionField);
                            }
                        }
                    }
                    break;
                }

                dataTableExportDto.setSelectionFields(selectionFields);
            }
            break;
            default:
                break;
        }

        return dataTableExportDto;
    }

    @Override
    public QueryContentResult queryContent(
            Long dataFacetUid,
            DataFacetQueryDto dataFacetQueryDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DataFacetDo dataFacetDo = this.dataFacetRepository.findByUid(dataFacetUid);
        if (dataFacetDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataFacetDo.RESOURCE_SYMBOL,
                    dataFacetUid));
        }

        //
        // Step 2, core-processing
        //
        DataTableQueryDto dataTableQueryDto = buildDataTableQuery(dataFacetDo, dataFacetQueryDto);
        if (dataTableQueryDto == null) {
            throw new AbcResourceConflictException(String.format("failed to build query, data facet %d",
                    dataFacetUid));
        }

        // tracking
        // 需要记录 data facet 信息
        CreateOrUpdateQueryLogDto updateQueryLogDto = new CreateOrUpdateQueryLogDto();
        updateQueryLogDto.setTrackingSerialNumber(operatingUserProfile.getTrackingSerialNumber());
        updateQueryLogDto.setDataFacetUid(dataFacetUid);
        updateQueryLogDto.setDataFacetName(dataFacetDo.getName());

        // data facet 层面，pre-check data permission granted
        if (Boolean.TRUE.equals(dataTableQueryDto.getRequireDataPermissionFilters())
                && CollectionUtils.isEmpty(dataTableQueryDto.getDataPermissionFilters())) {
            updateQueryLogDto.setSuccessful(Boolean.TRUE);
            updateQueryLogDto.setBeginTimestamp(LocalDateTime.now());
            updateQueryLogDto.setEndTimestamp(LocalDateTime.now());
            updateQueryLogDto.setTotalRowsInSource(0L);
            updateQueryLogDto.setTotalDurationInMillis(0L);
            updateQueryLogDto.setTotalDurationRemark(AbcDateUtils.format(0L));
            updateQueryLogDto.setRemark("no data permission granted");

            this.accessLoggingService.updateQueryLog(updateQueryLogDto, operatingUserProfile);

            QueryContentResult result = new QueryContentResult();
            result.setNumberOfElements(0);
            if (dataFacetQueryDto.getPagination() != null) {
                result.setEnabledPagination(Boolean.TRUE);
                result.setPageNumber(dataFacetQueryDto.getPagination().getPage());
                result.setPageSize(dataFacetQueryDto.getPagination().getSize());
                result.setTotalPages(0);
                result.setTotalElements(0L);
            }

            return result;
        }

        this.accessLoggingService.updateQueryLog(updateQueryLogDto, operatingUserProfile);

        // execute query
        QueryContentResult result = this.executeDataTableService.queryContent(
                dataFacetDo.getDataTableUid(),
                dataTableQueryDto,
                operatingUserProfile);

        //
        // Step 3, post-processing
        //
        List<SelectionField> selectionFieldList = dataFacetQueryDto.getSelectionFields();
        if (!CollectionUtils.isEmpty(selectionFieldList)
                && !CollectionUtils.isEmpty(result.getContent())) {
            Map<String, DataFieldTypeEnum> selectionFieldTypeMap = new HashMap<>();
            selectionFieldList.forEach(selectionField -> {
                switch (selectionField.getType()) {
                    case PLAIN: {
                        PlainSelectionField plainSelectionField = JSONObject.toJavaObject(selectionField.getContent()
                                , PlainSelectionField.class);
                        selectionFieldTypeMap.put(plainSelectionField.getFieldName(),
                                plainSelectionField.getFieldType());
                    }
                    break;
                    case EXPRESSION:
                        break;
                    default:
                        break;
                }
            });

            if (!CollectionUtils.isEmpty(selectionFieldTypeMap)) {
                for (int i = 0; i < result.getColumnNames().size(); i++) {
                    String fieldName = result.getColumnNames().get(i);
                    DataFieldTypeEnum expectedFieldType = selectionFieldTypeMap.get(fieldName);

                    switch (expectedFieldType) {
                        case DATE:
                        case DATETIME: {
                            for (Map<String, Object> row : result.getContent()) {
                                Object rawFieldValue = row.get(fieldName);
                                if (rawFieldValue != null) {
                                    Object transformedFieldValue =
                                            transformFieldValueAccordingToExpectedFieldType(rawFieldValue, expectedFieldType);
                                    row.put(fieldName, transformedFieldValue);
                                }
                            }
                        }
                        break;
                        default:
                            break;
                    }


                }
            }
        }

        return result;
    }

    @Override
    public Long exportContent(
            Long dataFacetUid,
            DataFacetExportDto dataFacetExportDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DataFacetDo dataFacetDo = this.dataFacetRepository.findByUid(dataFacetUid);
        if (dataFacetDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataFacetDo.RESOURCE_SYMBOL,
                    dataFacetUid));
        }

        //
        // Step 2, core-processing
        //
        DataTableExportDto dataTableExportDto = buildDataTableExport(dataFacetDo, dataFacetExportDto);
        if (dataTableExportDto == null) {
            throw new AbcResourceConflictException(String.format("failed to build export, data facet %d",
                    dataFacetUid));
        }

        return this.executeDataTableService.exportContent(
                dataFacetDo.getName(),
                dataFacetDo.getDataTableUid(),
                dataTableExportDto,
                operatingUserProfile);
    }


    public Object transformFieldValueAccordingToExpectedFieldType(
            Object fieldValue,
            DataFieldTypeEnum fieldType) {
        if (fieldValue instanceof java.util.Date) {
            Date transformedObject = (Date) fieldValue;
            LocalDateTime transformed =
                    Instant.ofEpochMilli(transformedObject.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();

            if (fieldType != null && fieldType.equals(DataFieldTypeEnum.DATETIME)) {
                String transformedString = transformed.format(dateTimeFormatter);
                return transformedString;
            } else if (fieldType != null && fieldType.equals(DataFieldTypeEnum.DATE)) {
                String transformedString = transformed.format(dateFormatter);
                return transformedString;
            }
        } else if (fieldValue instanceof java.sql.Timestamp) {
            Date transformedObject = new Date(((java.sql.Timestamp) fieldValue).getTime());
            LocalDateTime transformed =
                    Instant.ofEpochMilli(transformedObject.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();

            if (fieldType != null && fieldType.equals(DataFieldTypeEnum.DATETIME)) {
                String transformedString = transformed.format(dateTimeFormatter);
                return transformedString;
            } else if (fieldType != null && fieldType.equals(DataFieldTypeEnum.DATE)) {
                String transformedString = transformed.format(dateFormatter);
                return transformedString;
            }
        } else if (fieldValue instanceof java.sql.Date) {
            java.sql.Date sqlDate = (java.sql.Date) fieldValue;
            Date transformedObject = new Date(sqlDate.getTime());
            LocalDateTime transformed =
                    Instant.ofEpochMilli(transformedObject.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();

            if (fieldType != null && fieldType.equals(DataFieldTypeEnum.DATETIME)) {
                String transformedString = transformed.format(dateTimeFormatter);
                return transformedString;
            } else if (fieldType != null && fieldType.equals(DataFieldTypeEnum.DATE)) {
                String transformedString = transformed.format(dateFormatter);
                return transformedString;
            }
        } else if (fieldValue instanceof java.sql.Time) {
            java.sql.Time sqlTime = (java.sql.Time) fieldValue;
            return sqlTime.toString();
        } else if (fieldValue instanceof java.time.LocalDateTime) {
            LocalDateTime transformedObject = (LocalDateTime) fieldValue;

            if (fieldType != null && fieldType.equals(DataFieldTypeEnum.DATETIME)) {
                String transformedString = transformedObject.format(dateTimeFormatter);
                return transformedString;
            } else if (fieldType != null && fieldType.equals(DataFieldTypeEnum.DATE)) {
                String transformedString = transformedObject.format(dateFormatter);
                return transformedString;
            }
        } else if (fieldValue instanceof java.time.LocalDate) {
            LocalDate transformedObject = (LocalDate) fieldValue;

            if (fieldType != null && fieldType.equals(DataFieldTypeEnum.DATETIME)) {
                String transformedString = transformedObject.format(dateTimeFormatter);
                return transformedString;
            } else if (fieldType != null && fieldType.equals(DataFieldTypeEnum.DATE)) {
                String transformedString = transformedObject.format(dateFormatter);
                return transformedString;
            }
        } else if (fieldValue instanceof java.time.LocalTime) {
            LocalTime transformedObject = (LocalTime) fieldValue;
            String transformedString = transformedObject.format(timeFormatter);
            return transformedString;
        }

        return fieldValue;
    }
}
