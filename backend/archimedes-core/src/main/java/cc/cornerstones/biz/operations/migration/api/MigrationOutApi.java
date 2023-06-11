package cc.cornerstones.biz.operations.migration.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.operations.migration.dto.MigrationDeploymentDto;
import cc.cornerstones.biz.operations.migration.dto.PrepareMigrateOutDto;
import cc.cornerstones.biz.operations.migration.dto.PreparedMigrateOutDto;
import cc.cornerstones.biz.operations.migration.dto.StartMigrateOutDto;
import cc.cornerstones.biz.operations.migration.service.inf.MigrationService;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.PropertyNamingStrategy;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Tag(name = "[Biz] Operations / Migration / Migrate out")
@RestController
@RequestMapping(value = "/operations/migration/out")
public class MigrationOutApi {
    @Autowired
    private UserService userService;

    @Autowired
    private MigrationService migrationService;

    @Value("${private.dir.general.project.download}")
    private String projectDownloadPath;


    @Operation(summary = "Prepare")
    @PostMapping("/prepare")
    @ResponseBody
    public Response<PreparedMigrateOutDto> prepareMigrateOut(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestBody PrepareMigrateOutDto prepareMigrateOutDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.migrationService.prepareMigrateOut(prepareMigrateOutDto,
                        operatingUserProfile));
    }

    @Operation(summary = "Start")
    @PostMapping("/start")
    @ResponseBody
    public ResponseEntity<Resource> startMigrateOut(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestBody StartMigrateOutDto startMigrateOutDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // generate migration deployment
        Path path = this.migrationService.startMigrateOut(
                startMigrateOutDto,
                operatingUserProfile);

        Resource resource = new UrlResource(path.toUri());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/force-download"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + new String(path.getFileName().toString().getBytes(StandardCharsets.UTF_8),
                                StandardCharsets.ISO_8859_1) + "\"")
                .body(resource);
    }
}
