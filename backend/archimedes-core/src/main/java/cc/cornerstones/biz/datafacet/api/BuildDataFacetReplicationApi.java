package cc.cornerstones.biz.datafacet.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.datafacet.service.inf.DataFacetReplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "[Biz] Build / Data facets / Replication")
@RestController
@RequestMapping(value = "/build/data-facets/replication")
public class BuildDataFacetReplicationApi {
    @Autowired
    private UserService userService;

    @Autowired
    private DataFacetReplicationService dataFacetReplicationService;

    @Operation(summary = "复制 Source data facet 的配置到 Target data facet")
    @Parameters(value = {
            @Parameter(name = "source_data_facet_uid", description = "Source data facet 的 UID", required = true),
            @Parameter(name = "target_data_facet_uid", description = "Target data facet 的 UID", required = true)
    })
    @PostMapping("")
    @ResponseBody
    public Response copyDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "source_data_facet_uid", required = true) Long sourceDataFacetUid,
            @RequestParam(name = "target_data_facet_uid", required = true) Long targetDataFacetUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dataFacetReplicationService.copyDataFacet(
                sourceDataFacetUid, targetDataFacetUid,
                operatingUserProfile);

        return Response.buildSuccess();
    }
}
