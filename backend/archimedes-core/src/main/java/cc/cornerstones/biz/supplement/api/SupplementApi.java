package cc.cornerstones.biz.supplement.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.supplement.service.inf.SupplementService;
import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@Tag(name = "[Biz] Supplements")
@RestController
@RequestMapping(value = "/supplements")
public class SupplementApi {
    @Autowired
    private UserService userService;

    @Autowired
    private SupplementService supplementService;

    @Operation(summary = "Enable supplement")
    @PostMapping("/enable")
    public Response enableSupplement(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "name", required = false) String name,
            @RequestBody JSONObject configuration) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // 业务逻辑
        this.supplementService.enableSupplement(
                name, configuration,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "Disable supplement")
    @GetMapping("/disable")
    public Response disableSupplement(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "name", required = false) String name) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // 业务逻辑
        this.supplementService.disableSupplement(
                name,
                operatingUserProfile);

        return Response.buildSuccess();
    }
}
