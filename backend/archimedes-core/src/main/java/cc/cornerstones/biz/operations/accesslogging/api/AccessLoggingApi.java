package cc.cornerstones.biz.operations.accesslogging.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.operations.accesslogging.dto.QueryLogDto;
import cc.cornerstones.biz.operations.accesslogging.dto.SimpleQueryLogDto;
import cc.cornerstones.biz.operations.accesslogging.service.inf.AccessLoggingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedList;
import java.util.List;

@Tag(name = "[Biz] Operations / Access logging")
@RestController
@RequestMapping(value = "/operations/access-logging")
public class AccessLoggingApi {
    @Autowired
    private UserService userService;

    @Autowired
    private AccessLoggingService accessLoggingService;

    @Operation(summary = "分页查询 Query logs")
    @GetMapping("/query-logs/paging-query")
    public Response<Page<SimpleQueryLogDto>> pagingQueryQueryLogs(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "tracking_serial_number", required = false) String trackingSerialNumber,
            @RequestParam(name = "data_facet_uid", required = false) Long dataFacetUid,
            @RequestParam(name = "data_facet_name", required = false) String dataFacetName,
            @RequestParam(name = "user", required = false) String user,
            @RequestParam(name = "created_timestamp", required = false) List<String> createdTimestampAsStringList,
            Pageable pageable) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // pageable 中的 sort 特殊处理
        pageable = AbcApiUtils.transformPageable(pageable);

        // 查询用户 user
        // 查询用户 lastModifiedBy
        List<Long> userUidListOfUser = null;
        if (!ObjectUtils.isEmpty(user)) {
            userUidListOfUser = this.userService.listingQueryUidOfUsers(
                    user, operatingUserProfile);
            if (CollectionUtils.isEmpty(userUidListOfUser)) {
                Page<SimpleQueryLogDto> result = new PageImpl<>(new LinkedList<>(), pageable, 0);
                return Response.buildSuccess(result);
            }
        }

        // 业务逻辑
        return Response.buildSuccess(
                this.accessLoggingService.pagingQueryQueryLogs(
                        trackingSerialNumber, dataFacetUid, dataFacetName, userUidListOfUser, createdTimestampAsStringList,
                        pageable,
                        operatingUserProfile));
    }

    @Operation(summary = "获取指定 Query logs")
    @GetMapping("/query-logs")
    public Response<QueryLogDto> getQueryLog(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "tracking_serial_number", required = true) String trackingSerialNumber) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // 业务逻辑
        return Response.buildSuccess(
                this.accessLoggingService.getQueryLog(
                        trackingSerialNumber,
                        operatingUserProfile));
    }
}
