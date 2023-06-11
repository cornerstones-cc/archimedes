package cc.cornerstones.biz.datafacet.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.datafacet.dto.*;
import cc.cornerstones.biz.datafacet.service.inf.DataFacetAdvancedFeatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Tag(name = "[Biz] Build / Data facets / Advanced features")
@RestController
@RequestMapping(value = "/build/data-facets/advanced-features")
public class BuildDataFacetAdvancedFeatureApi {
    @Autowired
    private UserService userService;

    @Autowired
    private DataFacetAdvancedFeatureService dataFacetAdvancedFeatureService;

    @Operation(summary = "获取指定 Data facet 的 Advanced feature 配置")
    @Parameters(value = {
            @Parameter(name = "data_facet_uid", description = "Data facet 的 UID", required = true)
    })
    @GetMapping("")
    @ResponseBody
    public Response<AdvancedFeatureDto> getAdvancedFeatureOfDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataFacetAdvancedFeatureService.getAdvancedFeatureOfDataFacet(
                        dataFacetUid,
                        operatingUserProfile));
    }

    @Operation(summary = "替换指定 Data facet 的 Advanced feature 配置")
    @Parameters(value = {
            @Parameter(name = "data_facet_uid", description = "Data facet 的 UID", required = true)
    })
    @PutMapping("")
    @ResponseBody
    public Response replaceAdvancedFeatureOfDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid,
            @Valid @RequestBody AdvancedFeatureDto advancedFeatureDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (advancedFeatureDto.getContent() == null) {
            throw new AbcIllegalParameterException("content should not be null or empty");
        }

        if (Boolean.TRUE.equals(advancedFeatureDto.getContent().getEnabledMaintenanceWindow())) {
            if (CollectionUtils.isEmpty(advancedFeatureDto.getContent().getMaintenanceWindowList())) {
                throw new AbcIllegalParameterException("maintenance_window_list should not be null or empty if " +
                        "enabled_maintenance_window is true");
            }

            for (AdvancedFeatureContentDto.MaintenanceWindow maintenanceWindow :
                    advancedFeatureDto.getContent().getMaintenanceWindowList()) {
                if (ObjectUtils.isEmpty(maintenanceWindow.getCronExpression())) {
                    throw new AbcIllegalParameterException("cron_expression should not be null or empty if " +
                            "enabled_maintenance_window is true");
                }
                if (maintenanceWindow.getDurationInMinutes() == null) {
                    throw new AbcIllegalParameterException("duration_in_minutes should not be null if " +
                            "enabled_maintenance_window is true");
                }

                if (!CronExpression.isValidExpression(maintenanceWindow.getCronExpression())) {
                    throw new AbcIllegalParameterException("invalid cron_expression");
                }
            }

        }

        this.dataFacetAdvancedFeatureService.replaceAdvancedFeatureOfDataFacet(
                dataFacetUid, advancedFeatureDto,
                operatingUserProfile);

        return Response.buildSuccess();
    }
}
