package cc.cornerstones.biz.distributedfile.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.exceptions.AbcResourceIntegrityException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;

import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.distributedfile.service.inf.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.io.FileUtils;
import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Tag(name = "[Biz] Utilities / Distributed file / Storage")
@RestController
@RequestMapping(value = "/utilities/d-file/storage")
public class FileStorageApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileStorageApi.class);

    @Autowired
    private UserService userService;

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${private.dir.general.project.download}")
    private String projectDownloadPath;

    @Operation(summary = "下载文件")
    @GetMapping("/download")
    @ResponseBody
    public ResponseEntity<Resource> downloadFile(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "file_id") String fileId,
            HttpServletRequest request) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // Load file as Resource
        Resource resource = this.fileStorageService.loadFileAsResource(fileId, operatingUserProfile);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/force-download"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + new String(resource.getFilename().getBytes(StandardCharsets.UTF_8),
                                StandardCharsets.ISO_8859_1) + "\"")
                .body(resource);
    }

    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    @ResponseBody
    public Response<String> uploadFile(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam("file") MultipartFile file) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(this.fileStorageService.storeFile(file, null, operatingUserProfile));
    }

    @Operation(summary = "批量上传文件")
    @PostMapping("/upload-multiple")
    @ResponseBody
    public Response<List<String>> uploadMultipleFiles(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam("files") MultipartFile[] files) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        List<String> fileIdList =
                Arrays.asList(files).stream().map(file -> this.fileStorageService.storeFile(file, null,
                        operatingUserProfile)).collect(Collectors.toList());

        return Response.buildSuccess(fileIdList);
    }

    @Operation(summary = "下载本地文件")
    @GetMapping("/download-local-file")
    @ResponseBody
    public ResponseEntity<Resource> downloadLocalFile(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "absolute_file_path") String absoluteFilePath,
            HttpServletRequest request) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // Load file as Resource
        try {
            Path filePath = Paths.get(absoluteFilePath)
                    .toAbsolutePath().normalize();
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
                throw new AbcResourceNotFoundException(String.format("File not found:%s", absoluteFilePath));
            }
        } catch (MalformedURLException e) {
            LOGGER.error("File not found:{}", absoluteFilePath, e);
            throw new AbcResourceNotFoundException(String.format("File not found:%s", absoluteFilePath));
        }
    }

    @Operation(summary = "下载 Extension resource")
    @GetMapping("/download-ext-resource")
    @ResponseBody
    public ResponseEntity<Resource> downloadExtensionResource(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "resource_name") String resourceName,
            HttpServletRequest request) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("extensions/" + resourceName);
        if (inputStream == null) {
            throw new AbcResourceIntegrityException("cannot find resource");
        } else {
            String fileName = resourceName.replaceAll("/", "_");
            fileName = new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
            File file = Paths.get(this.projectDownloadPath, fileName).toFile();
            FileUtils.copyInputStreamToFile(inputStream, file);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/force-download"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(new FileSystemResource(file));
        }
    }
}
