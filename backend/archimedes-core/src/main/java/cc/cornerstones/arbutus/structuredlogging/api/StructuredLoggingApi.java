package cc.cornerstones.arbutus.structuredlogging.api;

import cc.cornerstones.arbutus.structuredlogging.dto.LogDto;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.arbutus.structuredlogging.service.inf.LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author bbottong
 */

@Tag(name = "[Arbutus] Utilities / Structured logging APIs")
@RestController
@RequestMapping(value = "/utilities/structured-logging")
public class StructuredLoggingApi {
    @Autowired
    private LogService logService;

    @Operation(summary = "获取日志")
    @GetMapping("/logs")
    @ResponseBody
    public Response<LogDto> getLog(
            @RequestParam(name = "job_category") String jobCategory,
            @RequestParam(name = "job_uid") String jobUid) throws Exception {
        return Response.buildSuccess(this.logService.getLog(jobCategory, jobUid));
    }
}
