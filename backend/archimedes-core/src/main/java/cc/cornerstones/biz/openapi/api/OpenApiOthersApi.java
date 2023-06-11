package cc.cornerstones.biz.openapi.api;

import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.serviceconnection.dto.DataPermissionServiceAgentDto;
import cc.cornerstones.biz.administration.serviceconnection.dto.DfsServiceAgentDto;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DataPermissionServiceAgentService;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DfsServiceAgentService;
import cc.cornerstones.biz.authentication.service.inf.OpenApiAuthService;
import cc.cornerstones.biz.resourceownership.service.inf.ResourceOwnershipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author bbottong
 */

@Tag(name = "[Open API] Others APIs")
@RestController
@RequestMapping(value = "/open-api")
public class OpenApiOthersApi {
    @Autowired
    private OpenApiAuthService openApiAuthService;

    @Autowired
    private DfsServiceAgentService dfsServiceAgentService;

    @Autowired
    private DataPermissionServiceAgentService dataPermissionServiceService;

    @Autowired
    private ResourceOwnershipService resourceOwnershipService;

    @Operation(summary = "获取指定 DFS service agent")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false),
            @Parameter(name = "uid", description = "DFS (Distributed file system) service agent 的 UID", required = true)
    })
    @GetMapping("/admin/service-connection/dfs-service/service-agents")
    @ResponseBody
    public Response<DfsServiceAgentDto> getDfsServiceAgent(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "access_token") String accessToken,
            @RequestParam(name = "subject", required = false) String subject,
            @RequestParam(name = "uid") Long uid) throws Exception {
        //
        // Step 1, Open api authentication & retrieving operating user's profile
        //
        this.openApiAuthService.authenticate(clientId, accessToken);
        UserProfile operatingUserProfile = this.openApiAuthService.getUserProfile(clientId, subject);

        //
        // Step 2, core-processing
        //
        return Response.buildSuccess(
                this.dfsServiceAgentService.getDfsServiceAgent(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "列表查询 Data permission service agents")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false),
            @Parameter(name = "uid", description = "Data permission service agent 的 UID", required = false),
            @Parameter(name = "name", description = "Data permission service agent 的 Name", required = false)
    })
    @GetMapping("/admin/service-connection/data-permission-service/service-agents/listing-query")
    public Response<List<DataPermissionServiceAgentDto>> listingQueryDataPermissionServiceAgents(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "access_token") String accessToken,
            @RequestParam(name = "subject", required = false) String subject,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "name", required = false) String name,
            Sort sort) throws Exception {
        //
        // Step 1, Open api authentication & retrieving operating user's profile
        //
        this.openApiAuthService.authenticate(clientId, accessToken);
        UserProfile operatingUserProfile = this.openApiAuthService.getUserProfile(clientId, subject);

        // sort 特殊处理
        sort = AbcApiUtils.transformSort(sort);

        //
        // Step 2, core-processing
        //
        return Response.buildSuccess(
                this.dataPermissionServiceService.listingQueryDataPermissionServiceAgents(
                        uid, name, sort,
                        operatingUserProfile));
    }

    @Operation(summary = "树形列出指定 Data permission service agent 的所有 Resource categories")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false),
            @Parameter(name = "data_permission_service_agent_uid", description = "Data permission service agent 的 UID",
                    required = true)
    })
    @GetMapping("/resource-ownership/resource-category-hierarchy/tree-listing-all-nodes")
    @ResponseBody
    public Response<List<cc.cornerstones.archimedes.extensions.types.TreeNode>> treeListingAllNodesOfResourceCategoryHierarchy(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "access_token") String accessToken,
            @RequestParam(name = "subject", required = false) String subject,
            @RequestParam(name = "data_permission_service_agent_uid", required = true) Long dataPermissionServiceAgentUid) throws Exception {
        //
        // Step 1, Open api authentication & retrieving operating user's profile
        //
        this.openApiAuthService.authenticate(clientId, accessToken);
        UserProfile operatingUserProfile = this.openApiAuthService.getUserProfile(clientId, subject);

        //
        // Step 2, core-processing
        //

        return Response.buildSuccess(
                this.resourceOwnershipService.treeListingAllNodesOfResourceCategoryHierarchy(
                        dataPermissionServiceAgentUid,
                        operatingUserProfile));
    }

    @Operation(summary = "树形列出指定 Data permission service agent 和指定 Resource category 的 Resource structure 的所有 " +
            "Levels")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false),
            @Parameter(name = "data_permission_service_agent_uid", description = "Data permission service agent" +
                    " 的 UID", required = true),
            @Parameter(name = "resource_category_uid", description = "Resource category 的 UID", required = true)
    })
    @GetMapping("/resource-ownership/resource-structure-hierarchy/tree-listing-all-nodes")
    @ResponseBody
    public Response<cc.cornerstones.archimedes.extensions.types.TreeNode> treeListingAllNodesOfResourceStructureHierarchy(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "access_token") String accessToken,
            @RequestParam(name = "subject", required = false) String subject,
            @RequestParam(name = "data_permission_service_agent_uid", required = true) Long dataPermissionServiceAgentUid,
            @RequestParam(name = "resource_category_uid", required = true) Long resourceCategoryUid) throws Exception {
        //
        // Step 1, Open api authentication & retrieving operating user's profile
        //
        this.openApiAuthService.authenticate(clientId, accessToken);
        UserProfile operatingUserProfile = this.openApiAuthService.getUserProfile(clientId, subject);

        //
        // Step 2, core-processing
        //

        return Response.buildSuccess(
                this.resourceOwnershipService.treeListingAllNodesOfResourceStructureHierarchy(
                        dataPermissionServiceAgentUid, resourceCategoryUid,
                        operatingUserProfile));
    }

    @Operation(summary = "树形列出指定 Data permission service agent 和指定 Resource category 的 Resource content 的所有 " +
            "Items")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false),
            @Parameter(name = "data_permission_service_agent_uid", description = "Data permission service agent" +
                    " 的 UID", required = true),
            @Parameter(name = "resource_category_uid", description = "Resource category 的 UID", required = true)
    })
    @GetMapping("/resource-ownership/resource-content-hierarchy/tree-listing-all-nodes")
    @ResponseBody
    public Response<List<cc.cornerstones.archimedes.extensions.types.TreeNode>> treeListingAllNodesOfResourceContentHierarchy(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "access_token") String accessToken,
            @RequestParam(name = "subject", required = false) String subject,
            @RequestParam(name = "data_permission_service_agent_uid", required = true) Long dataPermissionServiceAgentUid,
            @RequestParam(name = "resource_category_uid", required = true) Long resourceCategoryUid) throws Exception {
        //
        // Step 1, Open api authentication & retrieving operating user's profile
        //
        this.openApiAuthService.authenticate(clientId, accessToken);
        UserProfile operatingUserProfile = this.openApiAuthService.getUserProfile(clientId, subject);

        //
        // Step 2, core-processing
        //
        return Response.buildSuccess(
                this.resourceOwnershipService.treeListingAllNodesOfResourceContentHierarchy(
                        dataPermissionServiceAgentUid, resourceCategoryUid,
                        operatingUserProfile));
    }

}
