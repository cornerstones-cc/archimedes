package cc.cornerstones.biz.resourceownership.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.resourceownership.service.inf.ResourceOwnershipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "[Biz] Resource ownership")
@RestController
@RequestMapping(value = "/resource-ownership")
public class ResourceOwnershipApi {
    @Autowired
    private UserService userService;

    @Autowired
    private ResourceOwnershipService resourceOwnershipService;

    @Operation(summary = "树形列出指定 Data permission service agent 的所有 Resource categories")
    @GetMapping("/resource-category-hierarchy/tree-listing-all-nodes")
    @ResponseBody
    public Response<List<cc.cornerstones.archimedes.extensions.types.TreeNode>> treeListingAllNodesOfResourceCategoryHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_permission_service_agent_uid", required = true) Long dataPermissionServiceAgentUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.resourceOwnershipService.treeListingAllNodesOfResourceCategoryHierarchy(
                        dataPermissionServiceAgentUid,
                        operatingUserProfile));
    }

    @Operation(summary = "树形列出指定 Data permission service agent 和指定 Resource category 的 Resource structure 的所有 " +
            "Levels")
    @GetMapping("/resource-structure-hierarchy/tree-listing-all-nodes")
    @ResponseBody
    public Response<cc.cornerstones.archimedes.extensions.types.TreeNode> treeListingAllNodesOfResourceStructureHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_permission_service_agent_uid", required = true) Long dataPermissionServiceAgentUid,
            @RequestParam(name = "resource_category_uid", required = true) Long resourceCategoryUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.resourceOwnershipService.treeListingAllNodesOfResourceStructureHierarchy(
                        dataPermissionServiceAgentUid, resourceCategoryUid,
                        operatingUserProfile));
    }

    @Operation(summary = "树形列出指定 Data permission service agent 和指定 Resource category 的 Resource content 的所有 " +
            "Items")
    @GetMapping("/resource-content-hierarchy/tree-listing-all-nodes")
    @ResponseBody
    public Response<List<cc.cornerstones.archimedes.extensions.types.TreeNode>> treeListingAllNodesOfResourceContentHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_permission_service_agent_uid", required = true) Long dataPermissionServiceAgentUid,
            @RequestParam(name = "resource_category_uid", required = true) Long resourceCategoryUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.resourceOwnershipService.treeListingAllNodesOfResourceContentHierarchy(
                        dataPermissionServiceAgentUid, resourceCategoryUid,
                        operatingUserProfile));
    }
}
