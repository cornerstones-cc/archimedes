package cc.cornerstones.biz.operations.performancelogging.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.operations.accesslogging.dto.SimpleQueryLogDto;
import cc.cornerstones.biz.operations.performancelogging.service.inf.PerformanceLoggingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "[Biz] Operations / Performance logging")
@RestController
@RequestMapping(value = "/operations/performance-logging")
public class PerformanceLoggingApi {
    @Autowired
    private UserService userService;

    @Autowired
    private PerformanceLoggingService performanceLoggingService;

    @Operation(summary = "分页查询 Slow query logs")
    @GetMapping("/slow-query-logs/paging-query")
    public Response<Page<SimpleQueryLogDto>> pagingQuerySlowQueryLogs(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "tracking_serial_number", required = false) String trackingSerialNumber,
            @RequestParam(name = "data_facet_uid", required = false) Long dataFacetUid,
            @RequestParam(name = "data_facet_name", required = false) String dataFacetName,
            @RequestParam(name = "user_uid", required = false) Long userUid,
            @RequestParam(name = "display_name", required = false) String displayName,
            @RequestParam(name = "created_timestamp", required = false) List<String> createdTimestampAsStringList,
            Pageable pageable) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // pageable 中的 sort 特殊处理
        pageable = AbcApiUtils.transformPageable(pageable);

        // 业务逻辑
        return Response.buildSuccess(
                this.performanceLoggingService.pagingQuerySlowQueryLogs(
                        trackingSerialNumber, dataFacetUid, dataFacetName, userUid, displayName, createdTimestampAsStringList,
                        pageable,
                        operatingUserProfile));
    }
}
