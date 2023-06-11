package cc.cornerstones.biz.operations.migration.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcFileUtils;
import cc.cornerstones.biz.administration.serviceconnection.service.assembly.DfsServiceHandler;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.operations.migration.dto.MigrationDeploymentDto;
import cc.cornerstones.biz.operations.migration.service.inf.MigrationService;
import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Tag(name = "[Biz] Operations / Migration / Migrate in")
@RestController
@RequestMapping(value = "/operations/migration/in")
public class MigrationInApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(MigrationInApi.class);


    @Autowired
    private UserService userService;

    @Autowired
    private DfsServiceHandler dfsServiceHandler;

    @Autowired
    private MigrationService migrationService;

    @Value("${private.dir.general.project.upload}")
    private String projectUploadPath;

    @Value("${private.dir.general.project.download}")
    private String projectDownloadPath;

    @Operation(summary = "Prepare")
    @PostMapping("/prepare")
    @ResponseBody
    public Response<MigrationDeploymentDto> prepareMigrateIn(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam("dfs_service_agent_uid") Long dfsServiceAgentUid,
            @RequestParam("file_id") String fileId) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // 下载 migration.zip 文件
        File file = this.dfsServiceHandler.downloadFile(dfsServiceAgentUid, fileId, operatingUserProfile);

        // 解压
        Path unzipDirectoryPath = Paths.get(this.projectDownloadPath, UUID.randomUUID().toString());

        AbcFileUtils.unzipFile(file.getAbsolutePath(), unzipDirectoryPath.toString());

        // 提取 deployment.json 文件路径
        // 提取 export_templates 文件目录路径
        Path deploymentFilePath = Paths.get(unzipDirectoryPath.toString(), "deployment.json");
        Path exportTemplatesDirectoryPath = Paths.get(unzipDirectoryPath.toString(), "export_templates");

        // 解析 deployment.json 文件 to deployment 结构化对象
        String deploymentAsString = new String(Files.readAllBytes(deploymentFilePath), StandardCharsets.UTF_8);
        MigrationDeploymentDto migrationDeploymentDto = JSONObject.parseObject(deploymentAsString,
                MigrationDeploymentDto.class);

        return Response.buildSuccess(migrationDeploymentDto);
    }

    @Operation(summary = "Start")
    @PostMapping("/start")
    @ResponseBody
    public Response<Long> startMigrateIn(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam("dfs_service_agent_uid") Long dfsServiceAgentUid,
            @RequestParam("file_id") String fileId) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // 同时最多允许一个 migrate in task 存在
        Long taskUid = this.migrationService.findTaskUidOfTheMigrateInTaskInProgress(
                operatingUserProfile);
        if (taskUid != null) {
            throw new AbcResourceConflictException("There is already a migration in progress, please try again later.");
        }

        // 下载 migration.zip 文件
        File file = this.dfsServiceHandler.downloadFile(dfsServiceAgentUid, fileId, operatingUserProfile);

        // 解压
        Path unzipDirectoryPath = Paths.get(this.projectDownloadPath, UUID.randomUUID().toString());

        AbcFileUtils.unzipFile(file.getAbsolutePath(), unzipDirectoryPath.toString());

        // 提取 deployment.json 文件路径
        // 提取 export_templates 文件目录路径
        Path deploymentFilePath = Paths.get(unzipDirectoryPath.toString(), "deployment.json");
        Path exportTemplatesDirectoryPath = Paths.get(unzipDirectoryPath.toString(), "export_templates");

        // 解析 deployment.json 文件 to deployment 结构化对象
        String deploymentAsString = new String(Files.readAllBytes(deploymentFilePath), StandardCharsets.UTF_8);
        MigrationDeploymentDto migrationDeploymentDto = JSONObject.parseObject(deploymentAsString,
                MigrationDeploymentDto.class);

        return Response.buildSuccess(this.migrationService.startMigrateIn(
                migrationDeploymentDto,
                exportTemplatesDirectoryPath,
                operatingUserProfile));
    }


    @Operation(summary = "Find task uid of the migrate in task in progress")
    @PostMapping("/find")
    @ResponseBody
    public Response<Long> findTaskUidOfTheMigrateInTaskInProgress(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(this.migrationService.findTaskUidOfTheMigrateInTaskInProgress(
                operatingUserProfile));
    }
}
