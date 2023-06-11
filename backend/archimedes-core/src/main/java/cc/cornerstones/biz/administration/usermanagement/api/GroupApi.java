package cc.cornerstones.biz.administration.usermanagement.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.constants.TreeNodePositionEnum;
import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.types.*;

import cc.cornerstones.biz.administration.usermanagement.dto.*;
import cc.cornerstones.biz.administration.usermanagement.service.inf.GroupService;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "[Biz] Admin / User management / Groups")
@RestController
@RequestMapping(value = "/admin/user-mgmt/groups")
public class GroupApi {
    @Autowired
    private UserService userService;

    @Autowired
    private GroupService groupService;


    @Operation(summary = "树形列出 Group hierarchy 的所有 Nodes")
    @GetMapping("/group-hierarchy/tree-listing-all-nodes")
    @ResponseBody
    public Response<List<TreeNode>> treeListingAllNodesOfGroupHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.groupService.treeListingAllNodesOfGroupHierarchy(
                        operatingUserProfile));
    }

    @Operation(summary = "为 Group hierarchy 创建 Entity node")
    @Parameters(value = {
            @Parameter(name = "parent_uid", description = "Parent node 的 UID", required = false)
    })
    @PostMapping("/group-hierarchy/entity-nodes")
    @ResponseBody
    public Response<TreeNode> createEntityNodeForGroupHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "parent_uid", required = false) Long parentUid,
            @Valid @RequestBody CreateEntityTreeNode createEntityTreeNode) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.groupService.createEntityNodeForGroupHierarchy(
                        parentUid, createEntityTreeNode,
                        operatingUserProfile));
    }

    @Operation(summary = "修改 Group hierarchy 上的指定 Entity node")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Directory node 的 UID", required = true)
    })
    @PatchMapping("/group-hierarchy/entity-nodes")
    @ResponseBody
    public Response updateDirectoryNodeOfGroupHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid") Long uid,
            @Valid @RequestBody UpdateEntityTreeNode updateEntityTreeNode) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.groupService.updateEntityNodeOfGroupHierarchy(
                uid,
                updateEntityTreeNode,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "替换 Group hierarchy 上的指定一个 (Directory / Entity) relationship (parent 和 " +
            "sequence)")
    @Parameters(value = {
            @Parameter(name = "uid", description = "(Directory / Entity) node 的 UID", required = true)
    })
    @PutMapping("/group-hierarchy/nodes")
    @ResponseBody
    public Response replaceNodeRelationshipOfGroupHierarchy(
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

        this.groupService.replaceNodeRelationshipOfFunctionalHierarchy(
                uid,
                replaceTreeNodeRelationship,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "列出针对 Group hierarchy 上的指定 (Directory / Entity) node 的所有引用")
    @Parameters(value = {
            @Parameter(name = "uid", description = "(Directory / Entity) node 的 UID", required = true)
    })
    @GetMapping("/group-hierarchy/nodes/references")
    @ResponseBody
    public Response<List<String>> listAllReferencesToNodeOfGroupHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.groupService.listAllReferencesToNodeOfGroupHierarchy(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "删除 Group hierarchy 上的指定 (Directory / Entity) node")
    @Parameters(value = {
            @Parameter(name = "uid", description = "(Directory / File) node 的 UID", required = true)
    })
    @DeleteMapping("/group-hierarchy/nodes")
    @ResponseBody
    public Response deleteNodeOfGroupHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid") Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.groupService.deleteNodeOfGroupHierarchy(
                uid,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "为全部 Groups 创建或替换共有的 Roles 或者为指定 Group 创建或替换 Roles")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Group 的 UID", required = false)
    })
    @PutMapping("/roles")
    @ResponseBody
    public Response createOrReplaceCommonRolesForAllGroupsOrRolesForGivenGroup(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long uid,
            @Valid @RequestBody CreateOrReplaceRolesDto createOrReplaceRolesDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (uid == null) {
            this.groupService.createOrReplaceCommonRolesForAllGroups(
                    createOrReplaceRolesDto,
                    operatingUserProfile);
        } else {
            this.groupService.createOrReplaceRolesForGivenGroup(
                    uid, createOrReplaceRolesDto,
                    operatingUserProfile);
        }

        return Response.buildSuccess();
    }

    @Operation(summary = "获取全部 Groups 共有的 Roles 或获取指定 Group 的 Roles")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Group 的 UID", required = false)
    })
    @GetMapping("/roles")
    @ResponseBody
    public Response<List<Long>> getCommonRolesOfAllGroupsOrRolesOfGivenGroup(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (uid == null) {
            return Response.buildSuccess(
                    this.groupService.getCommonRolesOfAllGroups(
                            operatingUserProfile));
        } else {
            return Response.buildSuccess(
                    this.groupService.getRolesOfGivenGroup(
                            uid,
                            operatingUserProfile));
        }
    }
}
