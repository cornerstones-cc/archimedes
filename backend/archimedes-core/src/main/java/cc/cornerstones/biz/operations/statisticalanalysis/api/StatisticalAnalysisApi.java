package cc.cornerstones.biz.operations.statisticalanalysis.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.operations.statisticalanalysis.dto.StatisticalAnalysisDetailsDto;
import cc.cornerstones.biz.operations.statisticalanalysis.dto.StatisticalAnalysisOverallDto;
import cc.cornerstones.biz.operations.statisticalanalysis.dto.StatisticalAnalysisRankingDto;
import cc.cornerstones.biz.operations.statisticalanalysis.dto.StatisticalAnalysisTrendingDto;
import cc.cornerstones.biz.operations.statisticalanalysis.service.inf.StatisticalAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "[Biz] Operations / Statistical analysis")
@RestController
@RequestMapping(value = "/operations/statistical-analysis")
public class StatisticalAnalysisApi {
    @Autowired
    private UserService userService;

    @Autowired
    private StatisticalAnalysisService statisticalAnalysisService;

    @Operation(summary = "Overall")
    @GetMapping("/overall")
    public Response<StatisticalAnalysisOverallDto> getOverall(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // 业务逻辑
        return Response.buildSuccess(
                this.statisticalAnalysisService.getOverall(
                        operatingUserProfile));
    }

    @Operation(summary = "获取 Trending")
    @GetMapping("/trending")
    public Response<StatisticalAnalysisTrendingDto> getTrending(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "begin_date", required = true) String beginDateAsString,
            @RequestParam(name = "end_date", required = true) String endDateAsString) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // 业务逻辑
        return Response.buildSuccess(
                this.statisticalAnalysisService.getTrending(
                        beginDateAsString, endDateAsString,
                        operatingUserProfile));
    }

    @Operation(summary = "获取 Ranking")
    @GetMapping("/ranking")
    public Response<StatisticalAnalysisRankingDto> getRanking(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "begin_date", required = true) String beginDateAsString,
            @RequestParam(name = "end_date", required = true) String endDateAsString) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // 业务逻辑
        return Response.buildSuccess(
                this.statisticalAnalysisService.getRanking(
                        beginDateAsString, endDateAsString,
                        operatingUserProfile));
    }

    @Operation(summary = "获取 Details / query")
    @GetMapping("/details/query")
    public Response<StatisticalAnalysisDetailsDto> getQueryDetails(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "begin_date", required = true) String beginDateAsString,
            @RequestParam(name = "end_date", required = true) String endDateAsString,
            @RequestParam(name = "data_facet_uid", required = true) List<Long> dataFacetUidList) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // 业务逻辑
        return Response.buildSuccess(
                this.statisticalAnalysisService.getDetailsOfQuery(
                        beginDateAsString, endDateAsString, dataFacetUidList,
                        operatingUserProfile));
    }

    @Operation(summary = "获取 Details / export")
    @GetMapping("/details/export")
    public Response<StatisticalAnalysisDetailsDto> getExportDetails(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "begin_date", required = true) String beginDateAsString,
            @RequestParam(name = "end_date", required = true) String endDateAsString,
            @RequestParam(name = "data_facet_uid", required = true) List<Long> dataFacetUidList) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // 业务逻辑
        return Response.buildSuccess(
                this.statisticalAnalysisService.getDetailsOfExport(
                        beginDateAsString, endDateAsString, dataFacetUidList,
                        operatingUserProfile));
    }
}
