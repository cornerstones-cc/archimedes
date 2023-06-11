package cc.cornerstones.biz.openapi.api;

import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DfsServiceAgentService;
import cc.cornerstones.biz.authentication.service.inf.OpenApiAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Tag(name = "[Open API] Utilities / Distributed file system (dfs)")
@RestController
@RequestMapping(value = "/open-api/utilities/dfs")
public class OpenApiDfsApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenApiDfsApi.class);

    @Autowired
    private OpenApiAuthService openApiAuthService;

    @Autowired
    private DfsServiceAgentService dfsServiceAgentService;

    @Value("${private.dir.general.project.upload}")
    private String projectUploadPath;

    @Operation(summary = "下载文件")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false),
            @Parameter(name = "uid", description = "DFS (Distributed file system) 的 UID", required = false),
            @Parameter(name = "file_id", description = "File ID", required = true)
    })
    @GetMapping("/download")
    @ResponseBody
    public ResponseEntity<Resource> downloadFile(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "access_token") String accessToken,
            @RequestParam(name = "subject") String subject,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "file_id") String fileId) throws Exception {
        //
        // Step 1, Open api authentication & retrieving operating user's profile
        //
        this.openApiAuthService.authenticate(clientId, accessToken);
        UserProfile operatingUserProfile = this.openApiAuthService.getUserProfile(clientId, subject);

        //
        // Step 2, core-processing
        //
        File file = null;

        if (uid == null) {
            file = this.dfsServiceAgentService.downloadFile(fileId, operatingUserProfile);
        } else {
            file = this.dfsServiceAgentService.downloadFile(uid, fileId, operatingUserProfile);
        }

        // Load file as Resource
        try {
            Path filePath = file.toPath();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("application/force-download"))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\""
                                        + new String(resource.getFilename().getBytes(StandardCharsets.UTF_8),
                                        StandardCharsets.ISO_8859_1) + "\"")
                        .body(resource);
            } else {
                throw new AbcResourceNotFoundException(String.format("File not found:%s", file.getAbsolutePath()));
            }
        } catch (MalformedURLException e) {
            LOGGER.error("File not found:{}", file.getAbsolutePath(), e);
            throw new AbcResourceNotFoundException(String.format("File not found:%s", file.getAbsolutePath()));
        }
    }
    
    @Operation(summary = "上传文件")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false),
            @Parameter(name = "uid", description = "DFS (Distributed file system) 的 UID", required = false),
            @Parameter(name = "file", description = "File", required = true)
    })
    @PostMapping("/upload")
    @ResponseBody
    public Response<String> uploadFile(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "access_token") String accessToken,
            @RequestParam(name = "subject") String subject,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "file", required = true) MultipartFile file) throws Exception {
        //
        // Step 1, Open api authentication & retrieving operating user's profile
        //
        this.openApiAuthService.authenticate(clientId, accessToken);
        UserProfile operatingUserProfile = this.openApiAuthService.getUserProfile(clientId, subject);

        //
        // Step 2, core-processing
        //

        String uuid = UUID.randomUUID().toString();
        String fileId = Base64.encodeBase64String(uuid.getBytes(StandardCharsets.UTF_8)).toLowerCase();

        Path localDirectoryPath = Paths.get(this.projectUploadPath, fileId);
        Path localFilePath = Paths.get(this.projectUploadPath, fileId, file.getOriginalFilename());
        if (!localDirectoryPath.toFile().exists()) {
            localDirectoryPath.toFile().mkdirs();
        }

        try {
            // Copy file to the target location (Replacing existing file with the same name)
            Files.copy(file.getInputStream(), localFilePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("failed to store file: {}", localFilePath, e);
            throw new AbcResourceConflictException("Could not store file " + file.getOriginalFilename() + ". Please " +
                    "try again.");
        }

        try {
            if (uid == null) {
                return Response.buildSuccess(
                        this.dfsServiceAgentService.uploadFile(localFilePath.toFile(), operatingUserProfile));
            } else {
                return Response.buildSuccess(
                        this.dfsServiceAgentService.uploadFile(uid, localFilePath.toFile(), operatingUserProfile));
            }
        } finally {
            localFilePath.toFile().delete();
            localDirectoryPath.toFile().delete();
        }
    }

}
