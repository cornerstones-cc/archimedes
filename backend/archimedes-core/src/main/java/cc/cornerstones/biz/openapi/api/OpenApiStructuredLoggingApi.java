package cc.cornerstones.biz.openapi.api;

import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.arbutus.structuredlogging.dto.LogDto;
import cc.cornerstones.arbutus.structuredlogging.service.inf.LogService;
import cc.cornerstones.biz.authentication.service.inf.OpenApiAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

/**
 * @author bbottong
 */

@Tag(name = "[Open API] Utilities / Structured logging APIs")
@RestController
@RequestMapping(value = "/open-api/utilities/structured-logging")
public class OpenApiStructuredLoggingApi {
    @Autowired
    private OpenApiAuthService openApiAuthService;

    @Autowired
    private LogService logService;

    @Operation(summary = "获取日志")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false),
            @Parameter(name = "job_category", description = "Job 的 Category", required = false),
            @Parameter(name = "job_uid", description = "Job 的 UID", required = false)
    })
    @GetMapping("/logs")
    @ResponseBody
    public Response<LogDto> getLog(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "access_token") String accessToken,
            @RequestParam(name = "subject", required = false) String subject,
            @RequestParam(name = "job_category") String jobCategory,
            @RequestParam(name = "job_uid") String jobUid) throws Exception {
        //
        // Step 1, Open api authentication & retrieving operating user's profile
        //
        this.openApiAuthService.authenticate(clientId, accessToken);
        UserProfile operatingUserProfile = this.openApiAuthService.getUserProfile(clientId, subject);

        //
        // Step 2, core-processing
        //

        return Response.buildSuccess(this.logService.getLog(jobCategory, jobUid));
    }
}
