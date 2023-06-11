package cc.cornerstones.biz.serve.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.TreeNode;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.serve.dto.FlexibleQueryRequestDto;
import cc.cornerstones.biz.serve.service.inf.ExploreDataFacetService;
import cc.cornerstones.biz.share.types.QueryContentResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "[Biz] Explore / Data facets")
@RestController
@RequestMapping(value = "/explore/data-facets")
public class ExploreDataFacetApi {
    @Autowired
    private UserService userService;

    @Autowired
    private ExploreDataFacetService exploreDataFacetService;

    @Operation(summary = "树形列出所有 App 以及每个 App 的 Data facet hierarchy 的所有 Nodes")
    @GetMapping("/data-facet-hierarchy/tree-listing-all-nodes")
    @ResponseBody
    public Response<List<TreeNode>> treeListingAllNodesOfDataFacetHierarchyOfAllApps(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.exploreDataFacetService.treeListingAllNodesOfDataFacetHierarchyOfAllApps(
                        operatingUserProfile));
    }

    @Operation(summary = "灵活查询内容")
    @PostMapping("/content/flexible-query")
    @ResponseBody
    public Response<QueryContentResult> flexibleQuery(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid,
            @Valid @RequestBody FlexibleQueryRequestDto flexibleQueryRequestDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.exploreDataFacetService.flexibleQuery(
                        dataFacetUid, flexibleQueryRequestDto, operatingUserProfile));
    }
}
