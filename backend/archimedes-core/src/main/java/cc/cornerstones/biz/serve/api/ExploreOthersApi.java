package cc.cornerstones.biz.serve.api;

import cc.cornerstones.almond.types.Response;
import cc.cornerstones.biz.administration.serviceconnection.dto.DfsServiceAgentDto;
import cc.cornerstones.biz.administration.serviceconnection.dto.DfsServiceComponentDto;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DfsServiceAgentService;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DfsServiceComponentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author bbottong
 */

@Tag(name = "[Biz] Explore / Others APIs")
@RestController
@RequestMapping(value = "/explore/others")
public class ExploreOthersApi {
    @Autowired
    private DfsServiceAgentService dfsServiceAgentService;

    @Operation(summary = "获取指定 DFS service agent")
    @GetMapping("/dfs-service/service-agents")
    @ResponseBody
    public Response<DfsServiceAgentDto> getDfsServiceAgent(
            @RequestParam(name = "uid") Long uid) throws Exception {
        return Response.buildSuccess(
                this.dfsServiceAgentService.getDfsServiceAgent(
                        uid,
                        null));
    }
}
