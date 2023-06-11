package cc.cornerstones.biz.administration.serviceconnection.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.serviceconnection.dto.CreateDfsServiceAgentDto;
import cc.cornerstones.biz.administration.serviceconnection.dto.DfsServiceAgentDto;
import cc.cornerstones.biz.administration.serviceconnection.dto.UpdateDfsServiceAgentDto;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DfsServiceAgentService;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Tag(name = "[Biz] Admin / Service connection / Distributed file system (dfs) service / Service agent")
@RestController
@RequestMapping(value = "/admin/service-connection/dfs-service/service-agents")
public class DfsServiceAgentApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(DfsServiceAgentApi.class);

    @Autowired
    private UserService userService;

    @Autowired
    private DfsServiceAgentService dfsServiceAgentService;

    @Value("${private.dir.general.project.upload}")
    private String projectUploadPath;

    @Operation(summary = "创建一个 DFS service agent")
    @PostMapping("")
    @ResponseBody
    public Response<DfsServiceAgentDto> createDfsServiceAgent(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @Valid @RequestBody CreateDfsServiceAgentDto createDfsServiceAgentDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (createDfsServiceAgentDto.getEnabled() == null) {
            createDfsServiceAgentDto.setEnabled(Boolean.FALSE);
        }
        if (createDfsServiceAgentDto.getPreferred() == null) {
            createDfsServiceAgentDto.setPreferred(Boolean.FALSE);
        }

        return Response.buildSuccess(
                this.dfsServiceAgentService.createDfsServiceAgent(
                        createDfsServiceAgentDto,
                        operatingUserProfile));
    }

    @Operation(summary = "修改指定 Dfs service agent")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Dfs service agent 的 UID", required = true)
    })
    @PatchMapping("")
    @ResponseBody
    public Response updateDfsServiceAgent(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid,
            @Valid @RequestBody UpdateDfsServiceAgentDto updateDfsServiceAgentDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dfsServiceAgentService.updateDfsServiceAgent(
                uid, updateDfsServiceAgentDto,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "列出针对指定 Dfs service agent 的所有引用")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Dfs service agent 的 UID", required = true)
    })
    @GetMapping("/references")
    @ResponseBody
    public Response<List<String>> listAllReferencesToDfsServiceAgent(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dfsServiceAgentService.listAllReferencesToDfsServiceAgent(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "删除指定 Dfs service agent")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Dfs service agent 的 UID", required = true)
    })
    @DeleteMapping("")
    @ResponseBody
    public Response deleteDfsServiceAgent(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dfsServiceAgentService.deleteDfsServiceAgent(
                uid,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "获取指定 Dfs service agent")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Dfs service agent 的 UID", required = true)
    })
    @GetMapping("")
    @ResponseBody
    public Response<DfsServiceAgentDto> getDfsServiceAgent(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dfsServiceAgentService.getDfsServiceAgent(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "获取 Preferred Dfs service agent")
    @GetMapping("/preferred")
    @ResponseBody
    public Response<DfsServiceAgentDto> getPreferredDfsServiceAgent(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dfsServiceAgentService.getPreferredDfsServiceAgent(
                        operatingUserProfile));
    }

    @Operation(summary = "列表查询 Dfs service agents")
    @GetMapping("/listing-query")
    public Response<List<DfsServiceAgentDto>> listingQueryDfsServiceAgents(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "name", required = false) String name,
            Sort sort) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // sort 特殊处理
        sort = AbcApiUtils.transformSort(sort);

        // 业务逻辑
        return Response.buildSuccess(
                this.dfsServiceAgentService.listingQueryDfsServiceAgents(
                        uid, name, sort,
                        operatingUserProfile));
    }

    @Operation(summary = "分页查询 Dfs service agents")
    @GetMapping("/paging-query")
    public Response<Page<DfsServiceAgentDto>> pagingQueryDfsServiceAgents(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "description", required = false) String description,
            @RequestParam(name = "enabled", required = false) Boolean enabled,
            @RequestParam(name = "preferred", required = false) Boolean preferred,
            @RequestParam(name = "last_modified_by", required = false) String lastModifiedBy,
            @RequestParam(name = "last_modified_timestamp", required = false) List<String> lastModifiedTimestampAsStringList,
            Pageable pageable) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // pageable 中的 sort 特殊处理
        pageable = AbcApiUtils.transformPageable(pageable);

        // 查询用户 lastModifiedBy
        List<Long> userUidListOfLastModifiedBy = null;
        if (!ObjectUtils.isEmpty(lastModifiedBy)) {
            userUidListOfLastModifiedBy = this.userService.listingQueryUidOfUsers(
                    lastModifiedBy, operatingUserProfile);
            if (CollectionUtils.isEmpty(userUidListOfLastModifiedBy)) {
                Page<DfsServiceAgentDto> result = new PageImpl<>(new LinkedList<>(), pageable, 0);
                return Response.buildSuccess(result);
            }
        }

        // 业务逻辑
        return Response.buildSuccess(
                this.dfsServiceAgentService.pagingQueryDfsServiceAgents(
                        uid, name, description, enabled, preferred, userUidListOfLastModifiedBy,
                        lastModifiedTimestampAsStringList,
                        pageable,
                        operatingUserProfile));
    }

    @Operation(summary = "下载文件")
    @GetMapping("/download")
    @ResponseBody
    public ResponseEntity<Resource> downloadFile(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "file_id") String fileId) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

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
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam("file") MultipartFile file) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

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
