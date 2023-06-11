package cc.cornerstones.biz.openapi.api;

import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.TreeNode;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.authentication.service.inf.OpenApiAuthService;
import cc.cornerstones.biz.serve.dto.FlexibleQueryRequestDto;
import cc.cornerstones.biz.serve.service.inf.ExploreDataFacetService;
import cc.cornerstones.biz.share.types.QueryContentResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "[Open API] Explore / Data facets")
@RestController
@RequestMapping(value = "/open-api/explore/data-facets")
public class OpenApiExploreDataFacetApi {
    @Autowired
    private OpenApiAuthService openApiAuthService;

    @Autowired
    private ExploreDataFacetService exploreDataFacetService;

    @Operation(summary = "树形列出被授权的 Apps 以及每个 App 的 Data facet hierarchy 的所有 Nodes")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false)
    })
    @GetMapping("/data-facet-hierarchy/tree-listing-all-nodes")
    @ResponseBody
    public Response<List<TreeNode>> treeListingAllNodesOfDataFacetHierarchyOfAllApps(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "access_token") String accessToken,
            @RequestParam(name = "subject", required = false) String subject) throws Exception {
        //
        // Step 1, Open api authentication & retrieving operating user's profile
        //
        this.openApiAuthService.authenticate(clientId, accessToken);
        UserProfile operatingUserProfile = this.openApiAuthService.getUserProfile(clientId, subject);

        //
        // Step 2, core-processing
        //

        return Response.buildSuccess(
                this.exploreDataFacetService.treeListingAllNodesOfDataFacetHierarchyOfApp(
                        operatingUserProfile.getAppUid(), operatingUserProfile));
    }

    @Operation(summary = "灵活查询内容")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false),
            @Parameter(name = "data_facet_uid", description = "Data facet 的 UID", required = true)
    })
    @PostMapping("/content/flexible-query")
    @ResponseBody
    public Response<QueryContentResult> flexibleQuery(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "access_token") String accessToken,
            @RequestParam(name = "subject", required = false) String subject,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid,
            @Valid @RequestBody FlexibleQueryRequestDto flexibleQueryRequestDto) throws Exception {
        //
        // Step 1, Open api authentication & retrieving operating user's profile
        //
        this.openApiAuthService.authenticate(clientId, accessToken);
        UserProfile operatingUserProfile = this.openApiAuthService.getUserProfile(clientId, subject);
        this.openApiAuthService.authorizeDataFacet(dataFacetUid, operatingUserProfile);


        //
        // Step 2, core-processing
        //

        return Response.buildSuccess(
                this.exploreDataFacetService.flexibleQuery(
                        dataFacetUid, flexibleQueryRequestDto, operatingUserProfile));
    }
}
