package cc.cornerstones.biz.app.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.constants.TreeNodePositionEnum;
import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.TreeNode;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.types.CreateDirectoryTreeNode;
import cc.cornerstones.almond.types.CreateEntityTreeNode;
import cc.cornerstones.almond.types.UpdateDirectoryTreeNode;
import cc.cornerstones.almond.types.ReplaceTreeNodeRelationship;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.app.service.inf.AppDataFacetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "[Biz] Build / App / Data facets")
@RestController
@RequestMapping(value = "/build/apps/data-facets")
public class AppDataFacetApi {
    @Autowired
    private UserService userService;
    
    @Autowired
    private AppDataFacetService appDataFacetService;

    @Operation(summary = "为指定 App 的 Data facet hierarchy 创建一个 Directory node")
    @Parameters(value = {
            @Parameter(name = "app_uid", description = "App 的 UID", required = true),
            @Parameter(name = "parent_uid", description = "Parent node 的 UID", required = false)
    })
    @PostMapping("/data-facet-hierarchy/directory-nodes")
    @ResponseBody
    public Response<TreeNode> createDirectoryNodeForDataFacetHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "app_uid", required = true) Long appUid,
            @RequestParam(name = "parent_uid", required = false) Long parentUid,
            @Valid @RequestBody CreateDirectoryTreeNode createDirectoryTreeNode) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.appDataFacetService.createDirectoryNodeForDataFacetHierarchy(
                        appUid, parentUid, createDirectoryTreeNode,
                        operatingUserProfile));
    }

    @Operation(summary = "修改 Data facet hierarchy 上的指定一个 Directory node")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Directory node 的 UID", required = true)
    })
    @PatchMapping("/data-facet-hierarchy/directory-nodes")
    @ResponseBody
    public Response updateDirectoryNodeOfDataFacetHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid") Long uid,
            @Valid @RequestBody UpdateDirectoryTreeNode updateDirectoryTreeNode) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.appDataFacetService.updateDirectoryNodeOfDataFacetHierarchy(
                uid,
                updateDirectoryTreeNode,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "为指定 App 的 Data facet hierarchy 创建 Entity 节点")
    @Parameters(value = {
            @Parameter(name = "app_uid", description = "App 的 UID", required = true),
            @Parameter(name = "parent_uid", description = "Parent node 的 UID", required = false)
    })
    @PostMapping("/data-facet-hierarchy/entity-nodes")
    @ResponseBody
    public Response<TreeNode> createEntityNodeForDataFacetHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "app_uid", required = true) Long appUid,
            @RequestParam(name = "parent_uid", required = false) Long parentUid,
            @Valid @RequestBody CreateEntityTreeNode createEntityTreeNode) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (createEntityTreeNode.getPayload() == null
                || createEntityTreeNode.getPayload().isEmpty()) {
            throw new AbcIllegalParameterException("payload should not be null or empty");
        }

        return Response.buildSuccess(
                this.appDataFacetService.createEntityNodeForDataFacetHierarchy(
                        appUid, parentUid, createEntityTreeNode,
                        operatingUserProfile));
    }

    @Operation(summary = "为指定 App 的 Data facet hierarchy 批量创建 Entity 节点")
    @Parameters(value = {
            @Parameter(name = "app_uid", description = "App 的 UID", required = true),
            @Parameter(name = "parent_uid", description = "Parent node 的 UID", required = false)
    })
    @PostMapping("/data-facet-hierarchy/entity-nodes/batch")
    @ResponseBody
    public Response<List<TreeNode>> batchCreateEntityNodesForDataFacetHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "app_uid", required = true) Long appUid,
            @RequestParam(name = "parent_uid", required = false) Long parentUid,
            @Valid @RequestBody List<CreateEntityTreeNode> createEntityTreeNodeList) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.appDataFacetService.batchCreateEntityNodesForDataFacetHierarchy(
                        appUid, parentUid, createEntityTreeNodeList,
                        operatingUserProfile));
    }

    @Operation(summary = "修改 Data facet hierarchy 上的指定一个 (Directory / Entity) node 的 relationship (parent 和 " +
            "sequence)")
    @Parameters(value = {
            @Parameter(name = "uid", description = "(Directory / Entity) node 的 UID", required = true)
    })
    @PatchMapping("/data-facet-hierarchy/nodes/relationship")
    @ResponseBody
    public Response replaceNodeRelationshipOfDataFacetHierarchy(
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

        this.appDataFacetService.replaceNodeRelationshipOfDataFacetHierarchy(
                uid,
                replaceTreeNodeRelationship,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "列出针对 Data facet hierarchy 上的指定一个 (Directory / Entity) node 的所有引用")
    @Parameters(value = {
            @Parameter(name = "uid", description = "(Directory / Entity) node 的 UID", required = true)
    })
    @GetMapping("/data-facet-hierarchy/nodes/references")
    @ResponseBody
    public Response<List<String>> listAllReferencesToNodeOfDataFacetHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.appDataFacetService.listAllReferencesToNodeOfDataFacetHierarchy(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "删除 Data facet hierarchy 上的指定一个 (Directory / Entity) node")
    @Parameters(value = {
            @Parameter(name = "uid", description = "(Directory / Entity) node 的 UID", required = true)
    })
    @DeleteMapping("/data-facet-hierarchy/nodes")
    @ResponseBody
    public Response deleteNodeOfDataFacetHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid") Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.appDataFacetService.deleteNodeOfDataFacetHierarchy(
                uid,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "树形列出指定 App 或所有 Apps 的 Data facet hierarchy 的所有 Nodes")
    @Parameters(value = {
            @Parameter(name = "app_uid", description = "App 的 UID", required = false)
    })
    @GetMapping("/data-facet-hierarchy/tree-listing-all-nodes")
    @ResponseBody
    public Response<List<TreeNode>> treeListingAllNodesOfDataFacetHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "app_uid", required = false) Long appUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (appUid == null) {
            return Response.buildSuccess(
                    this.appDataFacetService.treeListingAllNodesOfDataFacetHierarchyOfAllApps(
                            operatingUserProfile));
        } else {
            return Response.buildSuccess(
                    this.appDataFacetService.treeListingAllNodesOfDataFacetHierarchyOfOneApp(
                            appUid,
                            operatingUserProfile));
        }
    }

}
