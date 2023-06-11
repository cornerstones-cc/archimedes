package cc.cornerstones.biz.datadictionary.service.assembly;

import cc.cornerstones.almond.constants.DatabaseConstants;
import cc.cornerstones.almond.constants.InfrastructureConstants;
import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.AbcPagination;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.TreeNode;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.datasource.service.assembly.database.QueryResult;
import cc.cornerstones.biz.datadictionary.entity.DictionaryBuildDo;
import cc.cornerstones.biz.datadictionary.entity.DictionaryBuildInstanceDo;
import cc.cornerstones.biz.datadictionary.entity.DictionaryCategoryDo;
import cc.cornerstones.biz.datadictionary.entity.DictionaryContentNodeDo;
import cc.cornerstones.biz.datadictionary.persistence.DictionaryBuildInstanceRepository;
import cc.cornerstones.biz.datadictionary.persistence.DictionaryBuildRepository;
import cc.cornerstones.biz.datadictionary.persistence.DictionaryCategoryRepository;
import cc.cornerstones.biz.datadictionary.persistence.DictionaryContentNodeRepository;
import cc.cornerstones.biz.datatable.share.constants.DictionaryBuildTypeEnum;
import cc.cornerstones.biz.distributedjob.share.types.JobHandler;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class DictionaryHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryHandler.class);

    public static final String JOB_HANDLER_DICTIONARY_BUILD = "dictionary_build";

    @Autowired
    private DictionaryBuildRepository dictionaryBuildRepository;

    @Autowired
    private DictionaryCategoryRepository dictionaryCategoryRepository;

    @Autowired
    private DictionaryContentNodeRepository dictionaryContentNodeRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private DictionaryBuildInstanceRepository dictionaryBuildInstanceRepository;

    private DictionaryBuildHandler findDictionaryBuildHandler(DictionaryBuildTypeEnum type) {
        DictionaryBuildHandler objectiveDictionaryBuildHandler = null;
        Map<String, DictionaryBuildHandler> map = this.applicationContext.getBeansOfType(DictionaryBuildHandler.class);
        if (!CollectionUtils.isEmpty(map)) {
            for (Map.Entry<String, DictionaryBuildHandler> entry : map.entrySet()) {
                DictionaryBuildHandler dictionaryBuildHandler = entry.getValue();
                if (dictionaryBuildHandler.type().equals(type)) {
                    objectiveDictionaryBuildHandler = dictionaryBuildHandler;
                    break;
                }
            }
        }

        return objectiveDictionaryBuildHandler;
    }

    @JobHandler(name = JOB_HANDLER_DICTIONARY_BUILD)
    public void executeDictionaryBuild(JSONObject params) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        DictionaryBuildInstanceDo dictionaryBuildInstanceDo = new DictionaryBuildInstanceDo();
        dictionaryBuildInstanceDo.setUid(this.idHelper.getNextDistributedId(DictionaryBuildInstanceDo.RESOURCE_NAME));

        Long dictionaryCategoryUid = params.getLongValue("dictionary_category_uid");
        if (dictionaryCategoryUid == null) {
            LOGGER.error("cannot find dictionary_category_uid from the input parameters");

            dictionaryBuildInstanceDo.setStatus(JobStatusEnum.CANCELED);
            dictionaryBuildInstanceDo.setRemark("dictionary_category_uid is null");
            BaseDo.create(dictionaryBuildInstanceDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
            this.dictionaryBuildInstanceRepository.save(dictionaryBuildInstanceDo);

            return;
        }

        dictionaryBuildInstanceDo.setDictionaryCategoryUid(dictionaryCategoryUid);
        dictionaryBuildInstanceDo.setStatus(JobStatusEnum.RUNNING);
        dictionaryBuildInstanceDo.setStartedTimestamp(LocalDateTime.now());
        BaseDo.create(dictionaryBuildInstanceDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
        this.dictionaryBuildInstanceRepository.save(dictionaryBuildInstanceDo);

        try {
            execute(dictionaryCategoryUid);

            dictionaryBuildInstanceDo.setStatus(JobStatusEnum.FINISHED);
            dictionaryBuildInstanceDo.setFinishedTimestamp(LocalDateTime.now());
            BaseDo.update(dictionaryBuildInstanceDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
            this.dictionaryBuildInstanceRepository.save(dictionaryBuildInstanceDo);
        } catch (Exception e) {
            LOGGER.error("failed to handle job {}", params, e);

            dictionaryBuildInstanceDo.setStatus(JobStatusEnum.FAILED);
            dictionaryBuildInstanceDo.setFailedTimestamp(LocalDateTime.now());
            if (e.getMessage() != null) {
                String remark = e.getMessage();
                if (remark.length() > DatabaseConstants.DEFAULT_DESCRIPTION_LENGTH) {
                    remark = remark.substring(0, DatabaseConstants.DEFAULT_DESCRIPTION_LENGTH);
                }
                dictionaryBuildInstanceDo.setRemark(remark);
            }
            BaseDo.update(dictionaryBuildInstanceDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
            this.dictionaryBuildInstanceRepository.save(dictionaryBuildInstanceDo);
        }
    }

    private void execute(Long dictionaryCategoryUid) throws Exception {
        DictionaryBuildDo dictionaryBuildDo = this.dictionaryBuildRepository.findByDictionaryCategoryUid(dictionaryCategoryUid);
        if (dictionaryBuildDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::dictionary_category_uid=%d", DictionaryBuildDo.RESOURCE_SYMBOL,
                    dictionaryCategoryUid));
        }

        DictionaryCategoryDo dictionaryCategoryDo =
                this.dictionaryCategoryRepository.findByUid(dictionaryBuildDo.getDictionaryCategoryUid());
        if (dictionaryCategoryDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DictionaryCategoryDo.RESOURCE_SYMBOL,
                    dictionaryBuildDo.getDictionaryCategoryUid()));
        }
        Long oldVersion = dictionaryCategoryDo.getVersion();

        DictionaryBuildHandler objectiveDictionaryBuildHandler =
                findDictionaryBuildHandler(dictionaryBuildDo.getType());
        if (objectiveDictionaryBuildHandler == null) {
            throw new AbcResourceConflictException(
                    String.format("cannot find dictionary build handler of type:%s",
                            dictionaryBuildDo.getType()));
        }

        //
        // Step 2, core-processing
        //

        // Step 2.1, 创建新版本
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        Long newVersion = Long.valueOf(timestamp);

        // Step 2.2, 分页取数，保存为新版本

        // 记录已经创建了 dictionary content node 的 column name --- column value 对
        // L1 key --- column name, L2 key --- column value, L2 value --- dictionary content node uid
        Map<String, Map<String, Long>> mapOfExistingNode = new HashMap<>(100);

        AbcPagination pagination = new AbcPagination();
        pagination.setPage(0);
        pagination.setSize(1000);
        do {
            QueryResult queryResult = objectiveDictionaryBuildHandler.execute(
                    dictionaryBuildDo.getLogic(), pagination);
            if (queryResult == null
                    || CollectionUtils.isEmpty(queryResult.getRows())) {
                break;
            }

            save(queryResult, mapOfExistingNode, newVersion, dictionaryBuildDo.getDictionaryCategoryUid());

            if (queryResult.getRows().size() < 1000) {
                break;
            }

            pagination.setPage(pagination.getPage() + 1);
        } while (true);

        // Step 2.3, 更新新版本
        dictionaryCategoryDo.setVersion(newVersion);
        BaseDo.update(dictionaryCategoryDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
        this.dictionaryCategoryRepository.save(dictionaryCategoryDo);

        // Step 2.4, 清理旧版本
        if (oldVersion != null) {
            dictionaryContentNodeRepository.deleteByDictionaryCategoryUidAndVersion(dictionaryCategoryDo.getUid(),
                    oldVersion);
        }

        //
        // Step 3, post-processing
        //
    }

    private void save(
            QueryResult queryResult,
            Map<String, Map<String, Long>> mapOfExistingNode,
            Long version,
            Long dictionaryCategoryUid) throws AbcUndefinedException {
        if (queryResult == null
                || CollectionUtils.isEmpty(queryResult.getColumnNames())
                || CollectionUtils.isEmpty(queryResult.getRows())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        List<DictionaryContentNodeDo> dictionaryContentNodeDoList = new LinkedList<>();

        // 遍历每一行
        float sequence = 1.0f;
        for (Map<String, Object> row : queryResult.getRows()) {
            // 遍历该行每一列
            for (int columnIndex = 0; columnIndex < queryResult.getColumnNames().size(); columnIndex++) {
                String columnName = queryResult.getColumnNames().get(columnIndex);
                Object columnValue = row.get(columnName);

                // TODO
                String columnValueAsString = String.valueOf(columnValue);

                if (!mapOfExistingNode.containsKey(columnName)) {
                    mapOfExistingNode.put(columnName, new HashMap<>(10));
                }

                if (!mapOfExistingNode.get(columnName).containsKey(columnValueAsString)) {
                    DictionaryContentNodeDo node = new DictionaryContentNodeDo();
                    node.setUid(this.idHelper.getNextDistributedId(DictionaryContentNodeDo.RESOURCE_NAME));
                    node.setLabelPhysical(columnValueAsString);
                    node.setLabel(columnValueAsString);
                    node.setValue(columnValueAsString);
                    node.setDictionaryCategoryUid(dictionaryCategoryUid);
                    node.setVersion(version);
                    node.setSequence(sequence++);
                    BaseDo.create(node, InfrastructureConstants.ROOT_USER_UID, now);
                    dictionaryContentNodeDoList.add(node);

                    mapOfExistingNode.get(columnName).put(columnValueAsString, node.getUid());

                    if (columnIndex == 0) {
                        // 如果是第1列，将 root node 设为parent
                        node.setParentUid(null);
                    } else {
                        // 如果是第2列及以后，则自己[行m,列n]一定是[行m,列n-1]的child
                        String parentColumnName = queryResult.getColumnNames().get(columnIndex - 1);
                        Object parentColumnValue = row.get(parentColumnName);
                        String parentColumnValueAsString = String.valueOf(parentColumnValue);
                        Long parentNodeUid =
                                mapOfExistingNode.get(parentColumnName).get(parentColumnValueAsString);
                        if (parentNodeUid == null) {
                            LOGGER.error("unexpected case, " +
                                    parentColumnName + ", " +
                                    parentColumnValueAsString);
                        } else {
                            node.setParentUid(parentNodeUid);
                        }
                    }
                }
            }
        }

        if (!CollectionUtils.isEmpty(dictionaryContentNodeDoList)) {
            this.dictionaryContentNodeRepository.saveAll(dictionaryContentNodeDoList);
        }
    }

    public List<TreeNode> testDictionaryBuild(
            DictionaryBuildTypeEnum type,
            JSONObject logic) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DictionaryBuildHandler objectiveDictionaryBuildHandler =
                findDictionaryBuildHandler(type);
        if (objectiveDictionaryBuildHandler == null) {
            throw new AbcResourceConflictException(
                    String.format("cannot find dictionary build handler of type:%s",
                            type));
        }

        //
        // Step 2, core-processing
        //
        AbcPagination pagination = new AbcPagination();
        pagination.setPage(0);
        pagination.setSize(1000);
        QueryResult queryResult = objectiveDictionaryBuildHandler.execute(logic, pagination);
        return buildTree(queryResult);
    }

    /**
     * 将表格（行和列）转换成树形结构，第N列即是第N层
     *
     * @param queryResult
     * @return
     */
    private List<TreeNode> buildTree(QueryResult queryResult) {
        if (queryResult == null
                || CollectionUtils.isEmpty(queryResult.getColumnNames())
                || CollectionUtils.isEmpty(queryResult.getRows())) {
            return null;
        }

        List<TreeNode> result = new LinkedList<>();

        // 记录已经在树上登记了的列名和列值(统一转换成字符串)
        // L1 key --- column name, L2 key --- column value
        Map<String, Map<String, TreeNode>> mapOfExistingTreeNode = new HashMap<>(10);

        // 遍历每一行
        for (Map<String, Object> row : queryResult.getRows()) {
            // 遍历该行每一列
            for (int columnIndex = 0; columnIndex < queryResult.getColumnNames().size(); columnIndex++) {
                String columnName = queryResult.getColumnNames().get(columnIndex);
                Object columnValue = row.get(columnName);

                // TODO
                String columnValueAsString = String.valueOf(columnValue);

                if (!mapOfExistingTreeNode.containsKey(columnName)) {
                    mapOfExistingTreeNode.put(columnName, new HashMap<>(10));
                }

                if (!mapOfExistingTreeNode.get(columnName).containsKey(columnValueAsString)) {
                    TreeNode treeNode = new TreeNode();
                    treeNode.setIds(this.idHelper.getNextStandaloneIdOfStr(TreeNode.class.getName()));
                    treeNode.setName(columnValueAsString);

                    mapOfExistingTreeNode.get(columnName).put(columnValueAsString, treeNode);

                    if (columnIndex == 0) {
                        // 如果是第1列，将root tree node设为parent
                        result.add(treeNode);
                    } else {
                        // 如果是第2列及以后，则自己[行m,列n]一定是[行m,列n-1]的child
                        String parentColumnName = queryResult.getColumnNames().get(columnIndex - 1);
                        Object parentColumnValue = row.get(parentColumnName);
                        String parentColumnValueAsString = String.valueOf(parentColumnValue);
                        TreeNode parentTreeNode =
                                mapOfExistingTreeNode.get(parentColumnName).get(parentColumnValueAsString);
                        if (parentTreeNode == null) {
                            LOGGER.error("unexpected case, " +
                                    parentColumnName + ", " +
                                    parentColumnValueAsString);
                        } else {
                            if (parentTreeNode.getChildren() == null) {
                                parentTreeNode.setChildren(new LinkedList<>());
                            }
                            parentTreeNode.getChildren().add(treeNode);
                        }
                    }
                }
            }
        }

        return result;
    }

    public void validateDictionaryBuild(
            DictionaryBuildTypeEnum type,
            JSONObject logic) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DictionaryBuildHandler objectiveDictionaryBuildHandler =
                findDictionaryBuildHandler(type);
        if (objectiveDictionaryBuildHandler == null) {
            throw new AbcResourceConflictException(
                    String.format("cannot find dictionary build handler of type:%s",
                            type));
        }

        //
        // Step 2, core-processing
        //
        objectiveDictionaryBuildHandler.validate(logic);

    }
}
