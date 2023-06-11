package cc.cornerstones.biz.distributedserver.api;

import cc.cornerstones.almond.types.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;


@Tag(name = "[Biz] Utilities / Distributed server")
@RestController
@RequestMapping(value = "/utilities/d-server")
public class DistributedServerApi {

    @Operation(summary = "获取 Server IP Address")
    @GetMapping("/server-ip-address")
    @ResponseBody
    public Response<String> getServerIpAddress(
            HttpServletRequest request) throws Exception {
        return Response.buildSuccess(request.getLocalAddr());
    }
}
