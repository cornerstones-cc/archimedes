package cc.cornerstones.biz.datadictionary.api;


import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.constants.TreeNodePositionEnum;
import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.types.*;

import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.datadictionary.dto.CreateOrReplaceDictionaryBuildDto;
import cc.cornerstones.biz.datadictionary.dto.DictionaryBuildDto;
import cc.cornerstones.biz.datadictionary.dto.DictionaryBuildInstanceDto;
import cc.cornerstones.biz.datadictionary.dto.TestDictionaryBuildDto;
import cc.cornerstones.biz.datadictionary.service.inf.DictionaryService;
import cc.cornerstones.almond.types.ReplaceTreeNodeRelationship;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "[Biz] Build / Data dictionary")
@RestController
@RequestMapping(value = "/build/data-dictionary")
public class BuildDictionaryApi {
    @Autowired
    private UserService userService;

    @Autowired
    private DictionaryService dictionaryService;

    @Operation(summary = "为 Dictionary category hierarchy 创建一个 Directory node")
    @Parameters(value = {
            @Parameter(name = "parent_uid", description = "Parent node 的 UID", required = false)
    })
    @PostMapping("/dictionary-category-hierarchy/directory-nodes")
    @ResponseBody
    public Response<TreeNode> createDirectoryNodeForDictionaryCategoryHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "parent_uid", required = false) Long parentUid,
            @Valid @RequestBody CreateDirectoryTreeNode createDirectoryTreeNode) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dictionaryService.createDirectoryNodeForDictionaryCategoryHierarchy(
                        parentUid, createDirectoryTreeNode,
                        operatingUserProfile));
    }

    @Operation(summary = "修改 Dictionary category hierarchy 上的指定一个 Directory node")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Directory node 的 UID", required = true)
    })
    @PatchMapping("/dictionary-category-hierarchy/directory-nodes")
    @ResponseBody
    public Response updateDirectoryNodeOfDictionaryCategoryHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid") Long uid,
            @Valid @RequestBody UpdateDirectoryTreeNode updateDirectoryTreeNode) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dictionaryService.updateDirectoryNodeOfDictionaryCategoryHierarchy(
                uid,
                updateDirectoryTreeNode,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "为 Dictionary category hierarchy 创建一个 Entity node")
    @Parameters(value = {
            @Parameter(name = "parent_uid", description = "Parent node 的 UID", required = false)
    })
    @PostMapping("/dictionary-category-hierarchy/entity-nodes")
    @ResponseBody
    public Response<TreeNode> createEntityNodeForDictionaryCategoryHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "parent_uid", required = false) Long parentUid,
            @Valid @RequestBody CreateEntityTreeNode createEntityTreeNode) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dictionaryService.createEntityNodeForDictionaryCategoryHierarchy(
                        parentUid, createEntityTreeNode,
                        operatingUserProfile));
    }

    @Operation(summary = "修改 Dictionary category hierarchy 上的指定一个 Entity node")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Entity node 的 UID", required = true)
    })
    @PatchMapping("/dictionary-category-hierarchy/entity-nodes")
    @ResponseBody
    public Response updateEntityNodeOfDictionaryCategoryHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid") Long uid,
            @Valid @RequestBody UpdateEntityTreeNode updateEntityTreeNode) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dictionaryService.updateEntityNodeOfDictionaryCategoryHierarchy(
                uid,
                updateEntityTreeNode,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "修改 Dictionary category hierarchy 上的指定一个 (Directory / Entity) node 的 relationship (parent 和 " +
            "sequence)")
    @Parameters(value = {
            @Parameter(name = "uid", description = "(Directory / Entity) node 的 UID", required = true)
    })
    @PatchMapping("/dictionary-category-hierarchy/nodes/relationship")
    @ResponseBody
    public Response replaceNodeRelationshipOfDictionaryCategoryHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid") Long uid,
            @Valid @RequestBody ReplaceTreeNodeRelationship replaceTreeNodeRelationship) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (replaceTreeNodeRelationship.getReferenceTreeNodeUid() == null) {
            if (!TreeNodePositionEnum.CENTER.equals(replaceTreeNodeRelationship.getPosition())) {
                throw new AbcIllegalParameterException("position can only be CENTER if reference_tree_node_uid is " +
                        "null");
            }
        }

        this.dictionaryService.replaceNodeRelationshipOfDictionaryCategoryHierarchy(
                uid,
                replaceTreeNodeRelationship,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "列出针对 Dictionary category hierarchy 上的指定一个 (Directory / Entity) node 的所有引用")
    @Parameters(value = {
            @Parameter(name = "uid", description = "(Directory / Entity) node 的 UID", required = true)
    })
    @GetMapping("/dictionary-category-hierarchy/nodes/references")
    @ResponseBody
    public Response<List<String>> listAllReferencesToNodeOfDictionaryCategoryHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dictionaryService.listAllReferencesToNodeOfDictionaryCategoryHierarchy(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "删除 Dictionary category hierarchy 上的指定一个 (Directory / Entity) node")
    @Parameters(value = {
            @Parameter(name = "uid", description = "(Directory / Entity) node 的 UID", required = true)
    })
    @DeleteMapping("/dictionary-category-hierarchy/nodes")
    @ResponseBody
    public Response deleteNodeOfDictionaryCategoryHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid") Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dictionaryService.deleteNodeOfDictionaryCategoryHierarchy(
                uid,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "树形列出 Dictionary category hierarchy 的所有 Nodes")
    @GetMapping("/dictionary-category-hierarchy/tree-listing-all-nodes")
    @ResponseBody
    public Response<List<TreeNode>> treeListingAllNodesOfDictionaryCategoryHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dictionaryService.treeListingAllNodesOfDictionaryCategoryHierarchy(
                        operatingUserProfile));
    }

    @Operation(summary = "为指定 Dictionary category 的 Dictionary structure hierarchy 创建一个 Entity node")
    @Parameters(value = {
            @Parameter(name = "dictionary_category_uid", description = "Dictionary category 的 UID", required = true),
            @Parameter(name = "parent_uid", description = "Parent node 的 UID", required = false)
    })
    @PostMapping("/dictionary-structure-hierarchy/entity-nodes")
    @ResponseBody
    public Response<TreeNode> createEntityNodeForDictionaryStructureHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "dictionary_category_uid", required = true) Long dictionaryCategoryUid,
            @RequestParam(name = "parent_uid", required = false) Long parentUid,
            @Valid @RequestBody CreateEntityTreeNode createEntityTreeNode) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dictionaryService.createEntityNodeForDictionaryStructureHierarchy(
                        dictionaryCategoryUid, parentUid, createEntityTreeNode,
                        operatingUserProfile));
    }

    @Operation(summary = "修改 Dictionary structure hierarchy 上的指定一个 Entity node")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Entity node 的 UID", required = true)
    })
    @PatchMapping("/dictionary-structure-hierarchy/entity-nodes")
    @ResponseBody
    public Response updateEntityNodeOfDictionaryStructureHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid") Long uid,
            @Valid @RequestBody UpdateEntityTreeNode updateEntityTreeNode) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dictionaryService.updateEntityNodeOfDictionaryStructureHierarchy(
                uid,
                updateEntityTreeNode,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "修改 Dictionary structure hierarchy 上的指定一个 (Directory / Entity) node 的 relationship (parent 和 " +
            "sequence)")
    @Parameters(value = {
            @Parameter(name = "uid", description = "(Directory / Entity) node 的 UID", required = true)
    })
    @PatchMapping("/dictionary-structure-hierarchy/nodes/relationship")
    @ResponseBody
    public Response replaceNodeRelationshipOfDictionaryStructureHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid") Long uid,
            @Valid @RequestBody ReplaceTreeNodeRelationship replaceTreeNodeRelationship) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (replaceTreeNodeRelationship.getReferenceTreeNodeUid() == null) {
            if (!TreeNodePositionEnum.CENTER.equals(replaceTreeNodeRelationship.getPosition())) {
                throw new AbcIllegalParameterException("position can only be CENTER if reference_tree_node_uid is " +
                        "null");
            }
        }

        this.dictionaryService.replaceNodeRelationshipOfDictionaryStructureHierarchy(
                uid,
                replaceTreeNodeRelationship,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "列出针对 Dictionary structure hierarchy 上的指定一个 (Directory / Entity) node 的所有引用")
    @Parameters(value = {
            @Parameter(name = "uid", description = "(Directory / Entity) node 的 UID", required = true)
    })
    @GetMapping("/dictionary-structure-hierarchy/nodes/references")
    @ResponseBody
    public Response<List<String>> listAllReferencesToNodeOfDictionaryStructureHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dictionaryService.listAllReferencesToNodeOfDictionaryStructureHierarchy(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "删除 Dictionary structure hierarchy 上的指定一个 (Directory / Entity) node")
    @Parameters(value = {
            @Parameter(name = "uid", description = "(Directory / Entity) node 的 UID", required = true)
    })
    @DeleteMapping("/dictionary-structure-hierarchy/nodes")
    @ResponseBody
    public Response deleteNodeOfDictionaryStructureHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid") Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dictionaryService.deleteNodeOfDictionaryStructureHierarchy(
                uid,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "树形列出指定 Dictionary category 的 Dictionary structure hierarchy 的所有 Nodes")
    @Parameters(value = {
            @Parameter(name = "dictionary_category_uid", description = "Dictionary category 的 UID", required = true)
    })
    @GetMapping("/dictionary-structure-hierarchy/tree-listing-all-nodes")
    @ResponseBody
    public Response<TreeNode> treeListingAllNodesOfDictionaryStructureHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "dictionary_category_uid", required = true) Long dictionaryCategoryUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dictionaryService.treeListingAllNodesOfDictionaryStructureHierarchy(
                        dictionaryCategoryUid,
                        operatingUserProfile));
    }

    @Operation(summary = "为 Dictionary content hierarchy 创建一个 Entity node")
    @Parameters(value = {
            @Parameter(name = "dictionary_category_uid", description = "Dictionary category 的 UID", required = true),
            @Parameter(name = "parent_uid", description = "Parent node 的 UID", required = false)
    })
    @PostMapping("/dictionary-content-hierarchy/entity-nodes")
    @ResponseBody
    public Response<TreeNode> createEntityNodeForDictionaryContentHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "dictionary_category_uid", required = true) Long dictionaryCategoryUid,
            @RequestParam(name = "parent_uid", required = false) Long parentUid,
            @Valid @RequestBody CreateEntityTreeNode createEntityTreeNode) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dictionaryService.createEntityNodeForDictionaryContentHierarchy(
                        dictionaryCategoryUid, parentUid, createEntityTreeNode,
                        operatingUserProfile));
    }

    @Operation(summary = "修改 Dictionary content hierarchy 上的指定一个 Entity node")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Entity node 的 UID", required = true)
    })
    @PatchMapping("/dictionary-content-hierarchy/entity-nodes")
    @ResponseBody
    public Response updateEntityNodeOfDictionaryContentHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid") Long uid,
            @Valid @RequestBody UpdateEntityTreeNode updateEntityTreeNode) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dictionaryService.updateEntityNodeOfDictionaryContentHierarchy(
                uid,
                updateEntityTreeNode,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "修改 Dictionary content hierarchy 上的指定一个 (Directory / Entity) node 的 relationship (parent 和 " +
            "sequence)")
    @Parameters(value = {
            @Parameter(name = "uid", description = "(Directory / Entity) node 的 UID", required = true)
    })
    @PatchMapping("/dictionary-content-hierarchy/nodes/relationship")
    @ResponseBody
    public Response replaceNodeRelationshipOfDictionaryContentHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid") Long uid,
            @Valid @RequestBody ReplaceTreeNodeRelationship replaceTreeNodeRelationship) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dictionaryService.replaceNodeRelationshipOfDictionaryContentHierarchy(
                uid,
                replaceTreeNodeRelationship,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "列出针对 Dictionary content hierarchy 上的指定一个 (Directory / Entity) node 的所有引用")
    @Parameters(value = {
            @Parameter(name = "uid", description = "(Directory / Entity) node 的 UID", required = true)
    })
    @GetMapping("/dictionary-content-hierarchy/nodes/references")
    @ResponseBody
    public Response<List<String>> listAllReferencesToNodeOfDictionaryContentHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dictionaryService.listAllReferencesToNodeOfDictionaryContentHierarchy(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "删除 Dictionary content hierarchy 上的指定一个 (Directory / Entity) node")
    @Parameters(value = {
            @Parameter(name = "uid", description = "(Directory / Entity) node 的 UID", required = true)
    })
    @DeleteMapping("/dictionary-content-hierarchy/nodes")
    @ResponseBody
    public Response deleteNodeOfDictionaryContentHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid") Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dictionaryService.deleteNodeOfDictionaryContentHierarchy(
                uid,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "树形列出指定 Dictionary category 的 Dictionary content hierarchy 的所有 Nodes")
    @Parameters(value = {
            @Parameter(name = "dictionary_category_uid", description = "Dictionary category 的 UID", required = true)
    })
    @GetMapping("/dictionary-content-hierarchy/tree-listing-all-nodes")
    @ResponseBody
    public Response<List<TreeNode>> treeListingAllNodesOfDictionaryContentHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "dictionary_category_uid", required = true) Long dictionaryCategoryUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dictionaryService.treeListingAllNodesOfDictionaryContentHierarchy(
                        dictionaryCategoryUid,
                        operatingUserProfile));
    }

    @Operation(summary = "树形列出指定 Dictionary category 的 Dictionary content hierarchy 上的第1级节点")
    @Parameters(value = {
            @Parameter(name = "dictionary_category_uid", description = "Dictionary category 的 UID", required = true)
    })
    @GetMapping("/dictionary-content-hierarchy/tree-listing-first-level")
    @ResponseBody
    public Response<List<TreeNode>> treeListingFirstLevelOfDictionaryContentHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "dictionary_category_uid", required = true) Long dictionaryCategoryUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // 参数校验

        // 业务逻辑
        return Response.buildSuccess(
                this.dictionaryService.treeListingFirstLevelOfDictionaryContentHierarchy(
                        dictionaryCategoryUid, operatingUserProfile));
    }

    @Operation(summary = "树形列出指定 Dictionary category 的 Dictionary content hierarchy 上指定节点的下一级节点")
    @Parameters(value = {
            @Parameter(name = "dictionary_content_node_uid", description = "Dictionary Content Node 的 UID", required = true)
    })
    @GetMapping("/dictionary-content-hierarchy/tree-listing-next-level")
    @ResponseBody
    public Response<List<TreeNode>> treeListingNextLevelOfDictionaryContentHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "dictionary_content_node_uid", required = true) Long dictionaryContentNodeUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // 参数校验

        // 业务逻辑
        return Response.buildSuccess(
                this.dictionaryService.treeListingNextLevelOfDictionaryContentHierarchy(
                        dictionaryContentNodeUid, operatingUserProfile));
    }

    @Operation(summary = "树形搜索指定 Dictionary category 的 Dictionary content hierarchy 的节点")
    @Parameters(value = {
            @Parameter(name = "dictionary_category_uid", description = "Dictionary Category 的 UID", required =
                    false),
            @Parameter(name = "value", description = "Dictionary Content Node 的 Value", required =
                    false),
            @Parameter(name = "label", description = "Dictionary Content Node 的 Label", required =
                    false)
    })
    @GetMapping("/dictionary-content-hierarchy/tree-querying-nodes")
    @ResponseBody
    public Response<List<TreeNode>> treeQueryingNodesOfDictionaryContentHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "dictionary_category_uid", required = false) Long dictionaryCategoryUid,
            @RequestParam(name = "value", required = false) String value,
            @RequestParam(name = "label", required = false) String label) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // 参数校验

        // 业务逻辑
        return Response.buildSuccess(
                this.dictionaryService.treeQueryingNodesOfDictionaryContentHierarchy(
                        dictionaryCategoryUid, value, label,
                        operatingUserProfile));
    }

    @ApiOperation("树形列出指定 Dictionary category 的 Dictionary content hierarchy 上的第1级节点，以及输入选中节点的完整层级")
    @GetMapping("/dictionary-content-hierarchy/tree-listing-first-level-w-selected")
    @ResponseBody
    public Response<List<TreeNode>> treeListingFirstLevelOfDictionaryContentHierarchyWithTaggingSelected(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "dictionary_category_uid") Long dictionaryCategoryUid,
            @RequestParam(name = "uid", required = false) List<Long> uidList,
            @RequestParam(name = "value", required = false) List<String> valueList,
            @RequestParam(name = "label", required = false) List<String> labelList) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // 参数校验

        // 业务逻辑
        return Response.buildSuccess(
                this.dictionaryService.treeListingFirstLevelOfDictionaryContentHierarchyWithTaggingSelected(
                        dictionaryCategoryUid, uidList, valueList, labelList, operatingUserProfile));
    }

    @Operation(summary = "为指定 Dictionary category (字典类目) 创建或替换 Dictionary build (字典构建)")
    @Parameters(value = {
            @Parameter(name = "dictionary_category_uid", description = "Dictionary category 的 UID", required = true)
    })
    @PutMapping("/builds")
    @ResponseBody
    public Response<DictionaryBuildDto> createOrReplaceDictionaryBuild(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "dictionary_category_uid", required = true) Long dictionaryCategoryUid,
            @Valid @RequestBody CreateOrReplaceDictionaryBuildDto createOrReplaceDictionaryBuildDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // 验证 cron 表达式
        boolean valid = CronExpression.isValidExpression(createOrReplaceDictionaryBuildDto.getCronExpression());
        if (!valid) {
            throw new AbcIllegalParameterException(String.format("illegal cron_expression"));
        }

        if (createOrReplaceDictionaryBuildDto.getType() == null) {
            throw new AbcIllegalParameterException("type is required");
        }

        if (createOrReplaceDictionaryBuildDto.getLogic() == null) {
            throw new AbcIllegalParameterException("logic is required");
        }

        return Response.buildSuccess(
                this.dictionaryService.createOrReplaceDictionaryBuild(
                        dictionaryCategoryUid, createOrReplaceDictionaryBuildDto,
                        operatingUserProfile));
    }

    @Operation(summary = "获取指定 Dictionary category (字典类目) 的 Dictionary build (字典构建)")
    @Parameters(value = {
            @Parameter(name = "dictionary_category_uid", description = "Dictionary category 的 UID", required = true)
    })
    @GetMapping("/builds/find-by-data-dictionary-uid")
    @ResponseBody
    public Response<DictionaryBuildDto> findDictionaryBuildByDataDictionaryUid(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "dictionary_category_uid", required = true) Long dictionaryCategoryUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // 参数校验

        return Response.buildSuccess(
                this.dictionaryService.findDictionaryBuildByDataDictionaryUid(
                        dictionaryCategoryUid,
                        operatingUserProfile));
    }

    @Operation(summary = "测试 Dictionary build (字典构建) 策略")
    @PostMapping("/builds/test")
    @ResponseBody
    public Response<List<TreeNode>> testDictionaryBuild(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestBody TestDictionaryBuildDto testDictionaryBuildDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // 参数校验
        if (testDictionaryBuildDto.getType() == null) {
            throw new AbcIllegalParameterException("type is required");
        }

        if (testDictionaryBuildDto.getLogic() == null) {
            throw new AbcIllegalParameterException("logic is required");
        }

        // 业务逻辑
        return Response.buildSuccess(
                this.dictionaryService.testDictionaryBuild(
                        testDictionaryBuildDto, operatingUserProfile));
    }

    @Operation(summary = "运行一次指定 Dictionary category (字典类目) 的 Dictionary build (字典构建)")
    @PostMapping("/builds/instances")
    @ResponseBody
    public Response executeOnceDictionaryBuild(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "dictionary_category_uid", required = true) Long dictionaryCategoryUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // 业务逻辑
        this.dictionaryService.executeOnceDictionaryBuild(
                dictionaryCategoryUid, operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "分页查询 Dictionary build (字典构建) 的实例")
    @Parameters(value = {
            @Parameter(name = "dictionary_category_uid", description = "Dictionary category 的 UID", required = false),
            @Parameter(name = "uid", description = "Dictionary Build Instance 的 UID", required = false),
            @Parameter(name = "status", description = "Dictionary Build Instance 的 Status", required = false),
            @Parameter(name = "created_timestamp", description = "Dictionary Build Instance 的 Created timestamp",
                    required =
                            false)
    })
    @GetMapping("/builds/instances/paging-query")
    @ResponseBody
    public Response<Page<DictionaryBuildInstanceDto>> pagingQueryDictionaryBuildInstances(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "dictionary_category_uid", required = false) Long dictionaryCategoryUid,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "status", required = false) List<JobStatusEnum> statuses,
            @RequestParam(name = "created_timestamp", required = false) List<String> createdTimestampAsStringList,
            Pageable pageable) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // pageable 中的 sort 特殊处理
        pageable = AbcApiUtils.transformPageable(pageable);

        // 业务逻辑
        return Response.buildSuccess(
                this.dictionaryService.pagingQueryDictionaryBuildInstances(
                        dictionaryCategoryUid, uid, statuses, createdTimestampAsStringList,
                        pageable,
                        operatingUserProfile));
    }

}
