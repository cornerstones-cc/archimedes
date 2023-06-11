package cc.cornerstones.biz.administration.usermanagement.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.constants.TreeNodePositionEnum;
import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.types.*;

import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.usermanagement.dto.*;
import cc.cornerstones.biz.administration.usermanagement.service.inf.PermissionsService;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.almond.types.ReplaceTreeNodeRelationship;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "[Biz] Admin / User management / Permissions")
@RestController
@RequestMapping(value = "/admin/user-mgmt/permissions")
public class PermissionsApi {
    @Autowired
    private UserService userService;

    @Autowired
    private PermissionsService permissionsService;

    @Operation(summary = "列表查询系统提供的 APIs")
    @GetMapping("/apis/listing-query")
    @ResponseBody
    public Response<List<ApiDto>> listingQueryApis(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "tag", required = false) String tag,
            @RequestParam(name = "method", required = false) String method,
            @RequestParam(name = "uri", required = false) String uri,
            @RequestParam(name = "summary", required = false) String summary,
            Sort sort) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // sort 特殊处理
        sort = AbcApiUtils.transformSort(sort);

        return Response.buildSuccess(
                this.permissionsService.listingQueryApis(
                        tag, method, uri, summary,
                        sort,
                        operatingUserProfile));
    }

    @Operation(summary = "树形列出 API hierarchy 的所有 Nodes")
    @GetMapping("/apis/api-hierarchy/tree-listing-all-nodes")
    @ResponseBody
    public Response<List<TreeNode>> treeListingAllNodesOfApiHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.permissionsService.treeListingAllNodesOfApiHierarchy(
                        operatingUserProfile));
    }

    @Operation(summary = "树形列出 Function hierarchy 的所有 Nodes")
    @GetMapping("/function-hierarchy/tree-listing-all-nodes")
    @ResponseBody
    public Response<List<TreeNode>> treeListingAllNodesOfFunctionHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.permissionsService.treeListingAllNodesOfFunctionHierarchy(
                        operatingUserProfile));
    }

    @Operation(summary = "为 Function hierarchy 创建 Directory node")
    @Parameters(value = {
            @Parameter(name = "parent_uid", description = "Parent node 的 UID", required = false)
    })
    @PostMapping("/function-hierarchy/directory-nodes")
    @ResponseBody
    public Response<TreeNode> createDirectoryNodeForFunctionHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "parent_uid", required = false) Long parentUid,
            @Valid @RequestBody CreateDirectoryTreeNode createDirectoryTreeNode) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.permissionsService.createDirectoryNodeForFunctionHierarchy(
                        parentUid, createDirectoryTreeNode,
                        operatingUserProfile));
    }

    @Operation(summary = "修改 Function hierarchy 上的指定 Directory node")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Directory node 的 UID", required = true)
    })
    @PatchMapping("/function-hierarchy/directory-nodes")
    @ResponseBody
    public Response updateDirectoryNodeOfFunctionHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid") Long uid,
            @Valid @RequestBody UpdateDirectoryTreeNode updateDirectoryTreeNode) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.permissionsService.updateDirectoryNodeOfFunctionHierarchy(
                uid,
                updateDirectoryTreeNode,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "为 Function hierarchy 创建 Entity 节点")
    @Parameters(value = {
            @Parameter(name = "parent_uid", description = "Parent node 的 UID", required = false)
    })
    @PostMapping("/function-hierarchy/entity-nodes")
    @ResponseBody
    public Response<TreeNode> createEntityNodeForFunctionHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "parent_uid", required = false) Long parentUid,
            @Valid @RequestBody CreateEntityTreeNode createEntityTreeNode) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.permissionsService.createEntityNodeForFunctionHierarchy(
                        parentUid, createEntityTreeNode,
                        operatingUserProfile));
    }

    @Operation(summary = "为 Function hierarchy 批量创建 Entity 节点")
    @Parameters(value = {
            @Parameter(name = "parent_uid", description = "Parent node 的 UID", required = false)
    })
    @PostMapping("/function-hierarchy/entity-nodes/batch")
    @ResponseBody
    public Response<List<TreeNode>> batchCreateEntityNodesForFunctionHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "parent_uid", required = false) Long parentUid,
            @Valid @RequestBody List<CreateEntityTreeNode> createEntityTreeNodeList) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.permissionsService.batchCreateEntityNodesForFunctionHierarchy(
                        parentUid, createEntityTreeNodeList,
                        operatingUserProfile));
    }

    @Operation(summary = "替换 Function hierarchy 上的指定一个 (Directory / Entity) node 的 relationship (parent 和 " +
            "sequence)")
    @Parameters(value = {
            @Parameter(name = "uid", description = "(Directory / Entity) node 的 UID", required = true)
    })
    @PutMapping("/function-hierarchy/nodes/relationship")
    @ResponseBody
    public Response replaceNodeRelationshipOfFunctionHierarchy(
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

        this.permissionsService.replaceNodeRelationshipOfFunctionHierarchy(
                uid,
                replaceTreeNodeRelationship,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "列出针对 Function hierarchy 上的指定一个 Directory node 的所有引用")
    @Parameters(value = {
            @Parameter(name = "uid", description = "(Directory / Entity) node 的 UID", required = true)
    })
    @GetMapping("/function-hierarchy/nodes/references")
    @ResponseBody
    public Response<List<String>> listAllReferencesToNodeOfFunctionHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.permissionsService.listAllReferencesToNodeOfFunctionHierarchy(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "删除 Function hierarchy 上的指定一个 (Directory / Entity) node")
    @Parameters(value = {
            @Parameter(name = "uid", description = "(Directory / Entity) node 的 UID", required = true)
    })
    @DeleteMapping("/function-hierarchy/nodes")
    @ResponseBody
    public Response deleteNodeOfFunctionHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid") Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.permissionsService.deleteNodeOfFunctionHierarchy(
                uid,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "树形列出 Navigation menu hierarchy 的所有 Nodes")
    @GetMapping("/navigation-menu-hierarchy/tree-listing-all-nodes")
    @ResponseBody
    public Response<List<TreeNode>> treeListingAllNodesOfNavigationMenuHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.permissionsService.treeListingAllNodesOfNavigationMenuHierarchy(
                        operatingUserProfile));
    }

    @Operation(summary = "为 Navigation menu hierarchy 创建一个 Directory node")
    @Parameters(value = {
            @Parameter(name = "parent_uid", description = "Parent node 的 UID", required = false)
    })
    @PostMapping("/navigation-menu-hierarchy/directory-nodes")
    @ResponseBody
    public Response<TreeNode> createDirectoryNodeForNavigationMenuHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "parent_uid", required = false) Long parentUid,
            @Valid @RequestBody CreateDirectoryTreeNode createDirectoryTreeNode) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.permissionsService.createDirectoryNodeForNavigationMenuHierarchy(
                        parentUid, createDirectoryTreeNode,
                        operatingUserProfile));
    }

    @Operation(summary = "修改 Navigation menu hierarchy 上的指定一个 Directory node")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Directory node 的 UID", required = true)
    })
    @PatchMapping("/navigation-menu-hierarchy/directory-nodes")
    @ResponseBody
    public Response updateDirectoryNodeOfNavigationMenuHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid") Long uid,
            @Valid @RequestBody UpdateDirectoryTreeNode updateDirectoryTreeNode) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.permissionsService.updateDirectoryNodeOfNavigationMenuHierarchy(
                uid,
                updateDirectoryTreeNode,
                operatingUserProfile);

        return Response.buildSuccess(
               );
    }

    @Operation(summary = "为 Navigation menu hierarchy 创建 Entity 节点")
    @Parameters(value = {
            @Parameter(name = "parent_uid", description = "Parent node 的 UID", required = false)
    })
    @PostMapping("/navigation-menu-hierarchy/entity-nodes")
    @ResponseBody
    public Response<TreeNode> createEntityNodeForNavigationMenuHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
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
                this.permissionsService.createEntityNodeForNavigationMenuHierarchy(
                        parentUid, createEntityTreeNode,
                        operatingUserProfile));
    }

    @Operation(summary = "修改 Navigation menu hierarchy 上的指定 Entity 节点")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Entity node 的 UID", required = true)
    })
    @PatchMapping("/navigation-menu-hierarchy/entity-nodes")
    @ResponseBody
    public Response updateEntityNodeOfNavigationMenuHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid") Long uid,
            @Valid @RequestBody UpdateEntityTreeNode updateEntityTreeNode) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.permissionsService.updateEntityNodeOfNavigationMenuHierarchy(
                uid,
                updateEntityTreeNode,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "替换 Navigation menu hierarchy 上的指定 (Directory / Entity) node 的 relationship (parent 和 " +
            "sequence)")
    @Parameters(value = {
            @Parameter(name = "uid", description = "(Directory / Entity) node 的 UID", required = true)
    })
    @PutMapping("/navigation-menu-hierarchy/nodes/relationship")
    @ResponseBody
    public Response replaceNodeRelationshipOfNavigationMenuHierarchy(
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

        this.permissionsService.replaceNodeRelationshipOfNavigationMenuHierarchy(
                uid,
                replaceTreeNodeRelationship,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "列出针对 Navigation menu hierarchy 上的指定一个 (Directory / Entity) node 的所有引用")
    @Parameters(value = {
            @Parameter(name = "uid", description = "(Directory / Entity) node 的 UID", required = true)
    })
    @GetMapping("/navigation-menu-hierarchy/nodes/references")
    @ResponseBody
    public Response<List<String>> listAllReferencesToNodeOfNavigationMenuHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.permissionsService.listAllReferencesToNodeOfNavigationMenuHierarchy(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "删除 Navigation menu hierarchy 上的指定一个 (Directory / Entity) node")
    @Parameters(value = {
            @Parameter(name = "uid", description = "(Directory / Entity) node 的 UID", required = true)
    })
    @DeleteMapping("/navigation-menu-hierarchy/nodes")
    @ResponseBody
    public Response deleteNodeOfNavigationMenuHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid") Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.permissionsService.deleteNodeOfNavigationMenuHierarchy(
                uid,
                operatingUserProfile);

        return Response.buildSuccess();
    }
}
