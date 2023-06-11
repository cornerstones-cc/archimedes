package cc.cornerstones.biz.openapi.api;

import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.serviceconnection.dto.DfsServiceAgentDto;
import cc.cornerstones.biz.administration.serviceconnection.dto.DfsServiceComponentDto;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DfsServiceAgentService;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DfsServiceComponentService;
import cc.cornerstones.biz.authentication.service.inf.OpenApiAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author bbottong
 */

@Tag(name = "[Open API] Explore / Others APIs")
@RestController
@RequestMapping(value = "/open-api/explore/others")
public class OpenApiExploreOthersApi {
    @Autowired
    private OpenApiAuthService openApiAuthService;

    @Autowired
    private DfsServiceAgentService dfsServiceAgentService;

    @Operation(summary = "获取指定 DFS service agent")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false),
            @Parameter(name = "uid", description = "DFS (Distributed file system) service agent 的 UID", required =
                    true)
    })
    @GetMapping("/dfs-service/service-agents")
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

}
