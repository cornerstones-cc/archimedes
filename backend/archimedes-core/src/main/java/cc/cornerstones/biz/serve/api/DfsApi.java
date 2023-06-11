package cc.cornerstones.biz.serve.api;

import cc.cornerstones.almond.constants.InfrastructureConstants;
import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DfsServiceAgentService;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "[Biz] Utilities / Distributed file system (dfs)")
@RestController
@RequestMapping(value = "/utilities/dfs")
public class DfsApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(DfsApi.class);

    @Autowired
    private UserService userService;

    @Autowired
    private DfsServiceAgentService dfsServiceAgentService;

    @Value("${private.dir.general.project.upload}")
    private String projectUploadPath;

    @Operation(summary = "下载文件")
    @GetMapping("/download")
    @ResponseBody
    public ResponseEntity<Resource> downloadFile(
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "file_id") String fileId) throws Exception {
        UserProfile operatingUserProfile = new UserProfile();
        operatingUserProfile.setUid(InfrastructureConstants.ROOT_USER_UID);
        operatingUserProfile.setDisplayName(InfrastructureConstants.ROOT_USER_DISPLAY_NAME);

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
    @PostMapping("/upload")
    @ResponseBody
    public Response<String> uploadFile(
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam("file") MultipartFile file) throws Exception {
        UserProfile operatingUserProfile = new UserProfile();
        operatingUserProfile.setUid(InfrastructureConstants.ROOT_USER_UID);
        operatingUserProfile.setDisplayName(InfrastructureConstants.ROOT_USER_DISPLAY_NAME);


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
