package cc.cornerstones.biz.administration.usermanagement.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.constants.TreeNodePositionEnum;
import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.TreeNode;
import cc.cornerstones.almond.types.UserProfile;

import cc.cornerstones.almond.types.CreateEntityTreeNode;
import cc.cornerstones.biz.administration.usermanagement.dto.CreateOrReplacePermissionsDto;
import cc.cornerstones.almond.types.ReplaceTreeNodeRelationship;
import cc.cornerstones.almond.types.UpdateEntityTreeNode;
import cc.cornerstones.biz.administration.usermanagement.service.inf.RoleService;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.administration.usermanagement.share.types.SimplePermissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "[Biz] Admin / User management / Roles")
@RestController
@RequestMapping(value = "/admin/user-mgmt/roles")
public class RoleApi {
    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Operation(summary = "树形列出 Role hierarchy 的所有 Nodes")
    @GetMapping("/role-hierarchy/tree-listing-all-nodes")
    @ResponseBody
    public Response<List<TreeNode>> treeListingAllNodesOfRoleHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.roleService.treeListingAllNodesOfRoleHierarchy(
                        operatingUserProfile));
    }

    @Operation(summary = "为 Role hierarchy 创建 Entity node")
    @Parameters(value = {
            @Parameter(name = "parent_uid", description = "Parent node 的 UID", required = false)
    })
    @PostMapping("/role-hierarchy/entity-nodes")
    @ResponseBody
    public Response<TreeNode> createEntityNodeForRoleHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "parent_uid", required = false) Long parentUid,
            @Valid @RequestBody CreateEntityTreeNode createEntityTreeNode) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.roleService.createEntityNodeForRoleHierarchy(
                        parentUid, createEntityTreeNode,
                        operatingUserProfile));
    }

    @Operation(summary = "修改 Role hierarchy 上的指定 Entity node")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Entity node 的 UID", required = true)
    })
    @PatchMapping("/role-hierarchy/entity-nodes")
    @ResponseBody
    public Response updateEntityNodeOfRoleHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid") Long uid,
            @Valid @RequestBody UpdateEntityTreeNode updateEntityTreeNode) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.roleService.updateEntityNodeOfRoleHierarchy(
                uid,
                updateEntityTreeNode,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "替换 Role hierarchy 上的指定 (Directory / Entity) relationship (parent 和 sequence)")
    @Parameters(value = {
            @Parameter(name = "uid", description = "(Directory / Entity) node 的 UID", required = true)
    })
    @PutMapping("/role-hierarchy/nodes/relationship")
    @ResponseBody
    public Response replaceNodeRelationshipOfRoleHierarchy(
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

        this.roleService.replaceNodeRelationshipOfRoleHierarchy(
                uid,
                replaceTreeNodeRelationship,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "列出针对 Role hierarchy 上的指定 (Directory / Entity) node 的所有引用")
    @Parameters(value = {
            @Parameter(name = "uid", description = "(Directory / Entity) node 的 UID", required = true)
    })
    @GetMapping("/role-hierarchy/nodes/references")
    @ResponseBody
    public Response<List<String>> listAllReferencesToNodeOfRoleHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.roleService.listAllReferencesToNodeOfRoleHierarchy(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "删除 Role hierarchy 上的指定 (Directory / Entity) node")
    @Parameters(value = {
            @Parameter(name = "uid", description = "(Directory / Entity) node 的 UID", required = true)
    })
    @DeleteMapping("/role-hierarchy/nodes")
    @ResponseBody
    public Response deleteNodeOfRoleHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid") Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.roleService.deleteNodeOfRoleHierarchy(
                uid,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "为全部 Roles 创建或替换共有的 Permissions 或者为指定 Role 创建或替换 Permissions")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Role 的 UID", required = false)
    })
    @PutMapping("/permissions")
    @ResponseBody
    public Response createOrReplaceCommonPermissionsForAllRolesOrPermissionsForGivenRole(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long uid,
            @Valid @RequestBody CreateOrReplacePermissionsDto createOrReplacePermissionsDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (uid == null) {
            this.roleService.createOrReplaceCommonPermissionsForAllRoles(
                    createOrReplacePermissionsDto,
                    operatingUserProfile);
        } else {
            this.roleService.createOrReplacePermissionsForGivenRole(
                    uid,
                    createOrReplacePermissionsDto,
                    operatingUserProfile);
        }

        return Response.buildSuccess();
    }

    @Operation(summary = "获取全部 Roles 共有的 Permissions 或获取指定 Role 的 Permissions")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Role 的 UID", required = false)
    })
    @GetMapping("/permissions")
    @ResponseBody
    public Response<SimplePermissions> getPermissionsCommonToAllRolesOrPermissionsOfGivenRole(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (uid == null) {
            return Response.buildSuccess(
                    this.roleService.getCommonPermissionsOfAllRoles(
                            operatingUserProfile));
        } else {
            return Response.buildSuccess(
                    this.roleService.getPermissionsOfGivenRole(
                            uid,
                            operatingUserProfile));
        }
    }
}
