package cc.cornerstones.biz.distributedfile.service.impl;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcFileUtils;
import cc.cornerstones.biz.distributedserver.entity.DistributedServerDo;
import cc.cornerstones.biz.distributedserver.persistence.DistributedServerRepository;
import cc.cornerstones.biz.distributedserver.service.assembly.DistributedServer;
import cc.cornerstones.biz.distributedserver.share.constants.DistributedServerStatus;
import cc.cornerstones.biz.distributedfile.entity.FileDo;
import cc.cornerstones.biz.distributedfile.persistence.FileRepository;
import cc.cornerstones.biz.distributedfile.service.inf.FileStorageService;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileStorageServiceImpl.class);

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private DistributedServer distributedServer;

    @Autowired
    private DistributedServerRepository distributedServerRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${server.port}")
    private Integer serverPort;

    @Value("${server.ssl.enabled}")
    private Boolean serverSslEnabled;

    @Value("${private.dir.general.project.download}")
    private String projectDownloadPath;

    @Value("${private.dir.general.project.upload}")
    private String projectUploadPath;

    @Override
    public Resource loadFileAsResource(
            String fileId,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        FileDo fileDo = this.fileRepository.findByFileId(fileId);
        if (fileDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::file_id=%s", FileDo.RESOURCE_SYMBOL, fileId));
        }

        //
        // Step 1.1, 获取本机信息
        //
        String serverHostname = this.distributedServer.getServerHostname();
        String serverIpAddress = this.distributedServer.getServerIpAddress();
        if (ObjectUtils.isEmpty(serverHostname)
                || ObjectUtils.isEmpty(serverIpAddress)) {
            LOGGER.info("[d-file] waiting for file server hostname and ip address initialized");
            throw new AbcResourceConflictException("");
        }
        DistributedServerDo localDistributedServerDo = this.distributedServerRepository.findByHostnameAndIpAddress(serverHostname, serverIpAddress);
        if (localDistributedServerDo == null) {
            LOGGER.error("[d-file] cannot find file server, by hostname={} and ip_address={}", serverHostname,
                    serverIpAddress);
            throw new AbcResourceConflictException("");
        }
        if (!DistributedServerStatus.UP.equals(localDistributedServerDo.getStatus())) {
            LOGGER.info("[d-file] the status of this file server {} ({}) is {}, not {}, not working", serverHostname,
                    serverIpAddress,
                    localDistributedServerDo.getStatus(),
                    DistributedServerStatus.UP);
            throw new AbcResourceConflictException("");
        }

        //
        // Step 2, core-processing
        //
        if (fileDo.getServerHostname().equals(serverHostname)
                && fileDo.getServerIpAddress().equals(serverIpAddress)) {
            //
            // Step 2.1, 文件就在本 file server
            //
            try {
                Path filePath = Paths.get(fileDo.getFilePath())
                        .toAbsolutePath().normalize();
                Resource resource = new UrlResource(filePath.toUri());
                if (resource.exists()) {
                    return resource;
                } else {
                    throw new AbcResourceNotFoundException(String.format("File not found:%s", fileDo.getFilePath()));
                }
            } catch (MalformedURLException e) {
                LOGGER.error("File not found:{}", fileDo.getFilePath(), e);
                throw new AbcResourceNotFoundException(String.format("File not found:%s", fileDo.getFilePath()));
            }
        } else {
            //
            // Step 2.2, 文件在其它 file server
            //
            StringBuilder url = new StringBuilder();
            if (Boolean.TRUE.equals(this.serverSslEnabled)) {
                url.append("https://");
            } else {
                url.append("http://");
            }
            url.append(fileDo.getServerIpAddress()).append(":").append(this.serverPort)
                    .append("/df/storage/download").append("?file_id=").append(fileId);

            File localFile = new File(String.format("%s%s%s%s%s", this.projectDownloadPath, File.separator, fileId,
                    File.separator,
                    fileDo.getFileName()));
            if (!localFile.getParentFile().exists()) {
                localFile.getParentFile().mkdirs();
            }
            ResponseEntity<File> responseEntity = this.restTemplate.execute(url.toString(), HttpMethod.GET,
                    new RequestCallback() {
                        @Override
                        public void doWithRequest(ClientHttpRequest request) throws IOException {
                            request.getHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM,
                                    MediaType.ALL));
                            request.getHeaders().set(NetworkingConstants.HEADER_USER_UID,
                                    String.valueOf(operatingUserProfile.getUid()));
                        }
                    },
                    new ResponseExtractor<ResponseEntity<File>>() {
                        @Override
                        public ResponseEntity<File> extractData(ClientHttpResponse response) throws IOException {
                            Files.copy(response.getBody(), localFile.toPath());
                            return null;
                        }
                    });

            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                try {
                    Path filePath = localFile.toPath();
                    Resource resource = new UrlResource(filePath.toUri());
                    if (resource.exists()) {
                        return resource;
                    } else {
                        throw new AbcResourceNotFoundException(String.format("File not found:%s", fileDo.getFilePath()));
                    }
                } catch (MalformedURLException e) {
                    LOGGER.error("File not found:{}", fileDo.getFilePath(), e);
                    throw new AbcResourceNotFoundException(String.format("File not found:%s", fileDo.getFilePath()));
                }
            } else {
                LOGGER.error("failed to download file from {} -- {}, status code:%d", fileDo.getServerHostname(),
                        fileDo.getServerIpAddress(), responseEntity.getStatusCode());
                throw new AbcResourceNotFoundException(String.format("File not found:%s", fileDo.getFilePath()));
            }
        }
    }

    @Override
    public String storeFile(
            MultipartFile file,
            String homeDirectory,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        String uuid = UUID.randomUUID().toString();
        String fileId = Base64.encodeBase64String(uuid.getBytes(StandardCharsets.UTF_8)).toLowerCase();

        String storageHomeDirectory = null;
        if (ObjectUtils.isEmpty(homeDirectory)) {
            storageHomeDirectory = this.projectUploadPath;
        } else {
            storageHomeDirectory = homeDirectory;
        }

        String localPath = String.format("%s%s%s%s%s", storageHomeDirectory, File.separator, fileId, File.separator
                , file.getOriginalFilename());
        File localFile = new File(localPath);
        if (!localFile.getParentFile().exists()) {
            localFile.getParentFile().mkdirs();
        }

        try {
            // Copy file to the target location (Replacing existing file with the same name)
            Path targetLocation = localFile.toPath();
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("failed to store file: {}", localPath, e);
            throw new AbcResourceConflictException("Could not store file " + file.getOriginalFilename() + ". Please " +
                    "try again.");
        }

        FileDo fileDo = new FileDo();
        fileDo.setFileId(fileId);
        fileDo.setFileLengthInBytes(localFile.length());
        fileDo.setFileLengthRemark(AbcFileUtils.formatFileLength(localFile.length()));
        fileDo.setFileName(file.getOriginalFilename());
        fileDo.setFilePath(localPath);
        fileDo.setServerHostname(this.distributedServer.getServerHostname());
        fileDo.setServerIpAddress(this.distributedServer.getServerIpAddress());
        BaseDo.create(fileDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.fileRepository.save(fileDo);

        return fileId;
    }

    @Override
    public String storeFile(
            File file,
            String homeDirectory,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        String uuid = UUID.randomUUID().toString();
        String fileId = Base64.encodeBase64String(uuid.getBytes(StandardCharsets.UTF_8)).toLowerCase();

        String storageHomeDirectory = null;
        if (ObjectUtils.isEmpty(homeDirectory)) {
            storageHomeDirectory = this.projectUploadPath;
        } else {
            storageHomeDirectory = homeDirectory;
        }

        String localPath = String.format("%s%s%s%s%s", storageHomeDirectory, File.separator, fileId, File.separator
                , file.getName());
        File localFile = new File(localPath);
        if (!localFile.getParentFile().exists()) {
            localFile.getParentFile().mkdirs();
        }

        try {
            Files.copy(file.toPath(), localFile.toPath());
        } catch (IOException e) {
            LOGGER.error("failed to store file: {}", localPath, e);
            throw new AbcResourceConflictException("Could not store file " + file.getName() + ". Please " +
                    "try again.");
        }

        FileDo fileDo = new FileDo();
        fileDo.setFileId(fileId);
        fileDo.setFileLengthInBytes(localFile.length());
        fileDo.setFileLengthRemark(AbcFileUtils.formatFileLength(localFile.length()));
        fileDo.setFileName(file.getName());
        fileDo.setFilePath(localPath);
        fileDo.setServerHostname(this.distributedServer.getServerHostname());
        fileDo.setServerIpAddress(this.distributedServer.getServerIpAddress());
        BaseDo.create(fileDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.fileRepository.save(fileDo);

        return fileId;
    }

    @Override
    public File getFile(
            String fileId,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        FileDo fileDo = this.fileRepository.findByFileId(fileId);
        if (fileDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::file_id=%s", FileDo.RESOURCE_SYMBOL, fileId));
        }

        //
        // Step 1.1, 获取本机信息
        //
        String serverHostname = this.distributedServer.getServerHostname();
        String serverIpAddress = this.distributedServer.getServerIpAddress();
        if (ObjectUtils.isEmpty(serverHostname)
                || ObjectUtils.isEmpty(serverIpAddress)) {
            LOGGER.info("[d-file] waiting for file server hostname and ip address initialized");
            throw new AbcResourceConflictException("");
        }

        //
        // Step 2, core-processing
        //
        if (fileDo.getServerHostname().equals(serverHostname)
                && fileDo.getServerIpAddress().equals(serverIpAddress)) {
            //
            // Step 2.1, 文件就在本 file server
            //
            Path filePath = Paths.get(fileDo.getFilePath())
                    .toAbsolutePath().normalize();
            return filePath.toFile();
        } else {
            //
            // Step 2.2, 文件在其它 file server
            //
            StringBuilder url = new StringBuilder();
            if (Boolean.TRUE.equals(this.serverSslEnabled)) {
                url.append("https://");
            } else {
                url.append("http://");
            }
            url.append(fileDo.getServerIpAddress()).append(":").append(this.serverPort)
                    .append("/df/storage/download").append("?file_id=").append(fileId);

            File localFile = new File(String.format("%s%s%s%s%s", this.projectDownloadPath, File.separator, fileId,
                    File.separator,
                    fileDo.getFileName()));
            if (!localFile.getParentFile().exists()) {
                localFile.getParentFile().mkdirs();
            }
            ResponseEntity<File> responseEntity = this.restTemplate.execute(url.toString(), HttpMethod.GET,
                    new RequestCallback() {
                        @Override
                        public void doWithRequest(ClientHttpRequest request) throws IOException {
                            request.getHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM,
                                    MediaType.ALL));
                            request.getHeaders().set(NetworkingConstants.HEADER_USER_UID,
                                    String.valueOf(operatingUserProfile.getUid()));
                        }
                    },
                    new ResponseExtractor<ResponseEntity<File>>() {
                        @Override
                        public ResponseEntity<File> extractData(ClientHttpResponse response) throws IOException {
                            Files.copy(response.getBody(), localFile.toPath());
                            return null;
                        }
                    });

            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                return localFile;
            } else {
                LOGGER.error("failed to download file from {} -- {}, status code:%d", fileDo.getServerHostname(),
                        fileDo.getServerIpAddress(), responseEntity.getStatusCode());
                throw new AbcResourceNotFoundException(String.format("File not found:%s", fileDo.getFilePath()));
            }
        }
    }
}
