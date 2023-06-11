package cc.cornerstones.biz.administration.serviceconnection.service.assembly;

import cc.cornerstones.almond.constants.DatabaseConstants;
import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.AbcTuple3;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcDateUtils;
import cc.cornerstones.almond.utils.AbcFileUtils;
import cc.cornerstones.archimedes.extensions.DfsServiceProvider;
import cc.cornerstones.biz.administration.serviceconnection.entity.DfsServiceAgentDo;
import cc.cornerstones.biz.administration.serviceconnection.entity.DfsServiceComponentDo;
import cc.cornerstones.biz.administration.serviceconnection.persistence.DfsServiceAgentRepository;
import cc.cornerstones.biz.administration.serviceconnection.persistence.DfsServiceComponentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class DfsServiceHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DfsServiceHandler.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DfsServiceComponentRepository dfsServiceComponentRepository;

    @Autowired
    private DfsServiceAgentRepository dfsServiceAgentRepository;

    public File downloadFile(
            String fileId,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        List<DfsServiceAgentDo> dfsServiceAgentDoList =
                this.dfsServiceAgentRepository.findByEnabledAndPreferred(Boolean.TRUE, Boolean.TRUE);
        if (CollectionUtils.isEmpty(dfsServiceAgentDoList)) {
            throw new AbcResourceNotFoundException("cannot find preferred dfs service");
        }
        if (dfsServiceAgentDoList.size() > 1) {
            throw new AbcResourceConflictException("found out more than 1 preferred dfs services");
        }
        DfsServiceAgentDo dfsServiceAgentDo = dfsServiceAgentDoList.get(0);

        //
        // Step 2, core-processing
        //
        return downloadFile(dfsServiceAgentDo, fileId, operatingUserProfile);
    }

    private File downloadFile(
            DfsServiceAgentDo dfsServiceAgentDo,
            String fileId,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        if (!Boolean.TRUE.equals(dfsServiceAgentDo.getEnabled())) {
            throw new AbcResourceConflictException(String.format("%s::disabled", DfsServiceAgentDo.RESOURCE_SYMBOL));
        }
        Long serviceComponentUid = dfsServiceAgentDo.getServiceComponentUid();
        DfsServiceComponentDo dfsServiceComponentDo = this.dfsServiceComponentRepository.findByUid(serviceComponentUid);
        if (dfsServiceComponentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DfsServiceComponentDo.RESOURCE_SYMBOL));
        }
        String configuration = dfsServiceAgentDo.getConfiguration();

        switch (dfsServiceComponentDo.getType()) {
            case BUILTIN: {
                String entryClassName = dfsServiceComponentDo.getEntryClassName();
                DfsServiceProvider dfsServiceProvider = null;
                Map<String, DfsServiceProvider> candidateDfsServiceProviderMap =
                        this.applicationContext.getBeansOfType(DfsServiceProvider.class);
                if (!CollectionUtils.isEmpty(candidateDfsServiceProviderMap)) {
                    for (DfsServiceProvider candidateDfsServiceProvider : candidateDfsServiceProviderMap.values()) {
                        if (candidateDfsServiceProvider.getClass().getName().equals(entryClassName)) {
                            dfsServiceProvider = candidateDfsServiceProvider;
                            break;
                        }
                    }
                }
                if (dfsServiceProvider == null) {
                    throw new AbcResourceConflictException(String.format("cannot find dfs service provider:%s",
                            dfsServiceComponentDo.getName()));
                }

                try {
                    LOGGER.info("going to download file {} with dfs service provider {} ({})", fileId,
                            dfsServiceComponentDo.getUid(), dfsServiceComponentDo.getName());
                    File file = dfsServiceProvider.downloadFile(fileId, configuration);
                    LOGGER.info("downloaded file {} to local {}", fileId, file.getAbsolutePath());
                    return file;
                } catch (Exception e) {
                    LOGGER.error("failed to download file {} with dfs service provider {} ({})", fileId,
                            dfsServiceComponentDo.getUid(), dfsServiceComponentDo.getName(), e);
                    throw new AbcResourceConflictException("failed to download file");
                }
            }
            case PLUGIN: {
                LOGGER.error("unsupported service provider {} ({})", dfsServiceComponentDo.getUid(),
                        dfsServiceComponentDo.getName());
                return null;
            }
            default:
                throw new AbcResourceConflictException(String.format("unsupported service provider type:%s",
                        dfsServiceComponentDo.getType()));
        }
    }

    public File downloadFile(
            Long dfsServiceAgentUid,
            String fileId,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DfsServiceAgentDo dfsServiceAgentDo = this.dfsServiceAgentRepository.findByUid(dfsServiceAgentUid);
        if (dfsServiceAgentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DfsServiceAgentDo.RESOURCE_SYMBOL,
                    dfsServiceAgentUid));
        }
        if (!Boolean.TRUE.equals(dfsServiceAgentDo.getEnabled())) {
            throw new AbcResourceConflictException(String.format("%s::disabled", DfsServiceAgentDo.RESOURCE_SYMBOL));
        }

        //
        // Step 2, core-processing
        //
        return downloadFile(dfsServiceAgentDo, fileId, operatingUserProfile);
    }

    public void downloadFiles(
            Long uid,
            List<AbcTuple3<String, String, String>> inputList,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DfsServiceAgentDo dfsServiceAgentDo = this.dfsServiceAgentRepository.findByUid(uid);
        if (dfsServiceAgentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DfsServiceAgentDo.RESOURCE_SYMBOL,
                    uid));
        }
        if (!Boolean.TRUE.equals(dfsServiceAgentDo.getEnabled())) {
            throw new AbcResourceConflictException(String.format("%s::disabled", DfsServiceAgentDo.RESOURCE_SYMBOL));
        }
        Long serviceComponentUid = dfsServiceAgentDo.getServiceComponentUid();
        DfsServiceComponentDo dfsServiceComponentDo = this.dfsServiceComponentRepository.findByUid(serviceComponentUid);
        if (dfsServiceComponentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DfsServiceComponentDo.RESOURCE_SYMBOL));
        }
        String configuration = dfsServiceAgentDo.getConfiguration();

        //
        // Step 2, core-processing
        //
        switch (dfsServiceComponentDo.getType()) {
            case BUILTIN: {
                String entryClassName = dfsServiceComponentDo.getEntryClassName();
                DfsServiceProvider dfsServiceProvider = null;
                Map<String, DfsServiceProvider> candidateDfsServiceProviderMap =
                        this.applicationContext.getBeansOfType(DfsServiceProvider.class);
                if (!CollectionUtils.isEmpty(candidateDfsServiceProviderMap)) {
                    for (DfsServiceProvider candidateDfsServiceProvider : candidateDfsServiceProviderMap.values()) {
                        if (candidateDfsServiceProvider.getClass().getName().equals(entryClassName)) {
                            dfsServiceProvider = candidateDfsServiceProvider;
                            break;
                        }
                    }
                }
                if (dfsServiceProvider == null) {
                    throw new AbcResourceConflictException(String.format("cannot find dfs service provider:%s",
                            dfsServiceComponentDo.getName()));
                }

                for (AbcTuple3<String, String, String> input : inputList) {
                    String serialNumber = UUID.randomUUID().toString();

                    String fileId = input.f;

                    String targetDirectory = input.s;

                    String targetFileName = input.t;
                    // target file name 可能为空，空的意思是沿用下载文件的名称
                    // 如果 target file name 不为空，就要替换非法文件名字符
                    if (!ObjectUtils.isEmpty(targetFileName)) {
                        targetFileName = AbcFileUtils.replaceIllegalCharactersWithEmpty(targetFileName);

                        if (targetFileName.length() > DatabaseConstants.MAXIMUM_FILE_NAME_LENGTH) {
                            targetFileName = targetFileName.substring(0, DatabaseConstants.MAXIMUM_FILE_NAME_LENGTH);
                        } else if (targetFileName.length() == 0) {
                            targetFileName = UUID.randomUUID().toString();
                        }
                    }

                    String prefixOfTargetFileName = null;
                    String suffixOfTargetFileName = null;

                    try {
                        LOGGER.info("[dfs] serial number {}, trying download file {}, target directory {}, target " +
                                        "file name {}, by " +
                                        "dfs " +
                                        "service provider {} ({})",
                                serialNumber,
                                fileId, targetDirectory, targetFileName,
                                dfsServiceComponentDo.getUid(),
                                dfsServiceComponentDo.getName());

                        File file = dfsServiceProvider.downloadFile(fileId, configuration);

                        LOGGER.info("[dfs] serial number {}, downloaded file {} to local {} path {}, target file name {}",
                                serialNumber,
                                fileId,
                                file.getName(), file.getAbsolutePath(), targetFileName);

                        // target file name 为空，空的意思是沿用下载文件的名称
                        if (ObjectUtils.isEmpty(targetFileName)) {
                            targetFileName = file.getName();
                        }

                        // 处理文件后缀名缺失可能性
                        int suffixIndexOfTargetFileName = targetFileName.lastIndexOf(".");
                        if (suffixIndexOfTargetFileName < 0) {
                            prefixOfTargetFileName = targetFileName;

                            int suffixIndexOfSourceFileName = file.getName().lastIndexOf(".");
                            if (suffixIndexOfSourceFileName >= 0) {
                                String suffixOfSourceFileName =
                                        file.getName().substring(suffixIndexOfSourceFileName + 1);
                                suffixOfTargetFileName = suffixOfSourceFileName;
                            } else {
                                FileInputStream fileInputStream = null;
                                try {
                                    fileInputStream = new FileInputStream(file);
                                    String contentType = URLConnection.guessContentTypeFromStream(new BufferedInputStream(fileInputStream));
                                    LOGGER.warn("[dfs] serial number {}, detected content type {} of file {} path {}",
                                            serialNumber,
                                            contentType,
                                            file.getName(), file.getAbsolutePath());
                                    if (!ObjectUtils.isEmpty(contentType)) {
                                        if (contentType.equalsIgnoreCase("image/png")) {
                                            suffixOfTargetFileName = "png";
                                        } else if (contentType.equalsIgnoreCase("image/jpeg")) {
                                            suffixOfTargetFileName = "jpg";
                                        } else if (contentType.equalsIgnoreCase("text/plain")) {
                                            suffixOfTargetFileName = "txt";
                                        } else if (contentType.equalsIgnoreCase("text/html")) {
                                            suffixOfTargetFileName = "html";
                                        } else {
                                            LOGGER.warn("[dfs] serial number {}, unknown content type {}",
                                                    serialNumber,
                                                    contentType);
                                        }
                                    }
                                } finally {
                                    if (fileInputStream != null) {
                                        try {
                                            fileInputStream.close();
                                        } catch (IOException e) {
                                            LOGGER.error("", e);
                                        }
                                    }
                                }
                            }
                        } else {
                            prefixOfTargetFileName = targetFileName.substring(0, suffixIndexOfTargetFileName);
                            suffixOfTargetFileName = targetFileName.substring(suffixIndexOfTargetFileName + 1);
                        }

                        if (suffixOfTargetFileName == null) {
                            targetFileName = prefixOfTargetFileName;
                        } else {
                            targetFileName = prefixOfTargetFileName + "." + suffixOfTargetFileName;
                        }

                        // 处理文件重名可能性
                        Path targetPath = Paths.get(targetDirectory, targetFileName);
                        int copy = 1;
                        while (targetPath.toFile().exists()) {
                            if (suffixOfTargetFileName == null) {
                                targetPath = Paths.get(targetDirectory, prefixOfTargetFileName + "(" + copy + ")");
                            } else {
                                targetPath = Paths.get(targetDirectory, prefixOfTargetFileName + "(" + copy + ")" +
                                        "." + suffixOfTargetFileName);
                            }
                            copy++;
                        }

                        long beginTime = System.currentTimeMillis();
                        LOGGER.info("[dfs] serial number {}, trying to move file from source path {} to target path {}",
                                serialNumber,
                                file.toPath(), targetPath);

                        Files.move(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

                        LOGGER.info("[dfs] serial number {}, moved file from source path {} to target path {}, duration:{}",
                                serialNumber,
                                file.toPath(), targetPath, AbcDateUtils.format(System.currentTimeMillis() - beginTime));
                    } catch (Exception e) {
                        LOGGER.error("[dfs] serial number {}, failed to download file {} with dfs service provider {} ({})",
                                serialNumber,
                                fileId,
                                dfsServiceComponentDo.getUid(), dfsServiceComponentDo.getName(), e);
                    }
                }
            }
            break;
            case PLUGIN: {
                LOGGER.error("[dfs] unsupported service provider {} ({})", dfsServiceComponentDo.getUid(),
                        dfsServiceComponentDo.getName());
            }
            break;
            default:
                throw new AbcResourceConflictException(String.format("unsupported service provider type:%s",
                        dfsServiceComponentDo.getType()));
        }
    }

    public String uploadFile(
            File file,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        List<DfsServiceAgentDo> dfsServiceAgentDoList =
                this.dfsServiceAgentRepository.findByEnabledAndPreferred(Boolean.TRUE, Boolean.TRUE);
        if (CollectionUtils.isEmpty(dfsServiceAgentDoList)) {
            throw new AbcResourceNotFoundException("cannot find preferred dfs service");
        }
        if (dfsServiceAgentDoList.size() > 1) {
            throw new AbcResourceConflictException("found out more than 1 preferred dfs services");
        }
        DfsServiceAgentDo dfsServiceAgentDo = dfsServiceAgentDoList.get(0);

        //
        // Step 2, core-processing
        //
        return uploadFile(dfsServiceAgentDo, file, operatingUserProfile);
    }

    public String uploadFile(
            Long dfsServiceAgentUid,
            File file,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DfsServiceAgentDo dfsServiceAgentDo = this.dfsServiceAgentRepository.findByUid(dfsServiceAgentUid);
        if (dfsServiceAgentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DfsServiceAgentDo.RESOURCE_SYMBOL,
                    dfsServiceAgentUid));
        }
        if (!Boolean.TRUE.equals(dfsServiceAgentDo.getEnabled())) {
            throw new AbcResourceConflictException(String.format("%s::disabled", DfsServiceAgentDo.RESOURCE_SYMBOL));
        }

        //
        // Step 2, core-processing
        //
        return uploadFile(dfsServiceAgentDo, file, operatingUserProfile);
    }

    public String uploadFile(
            DfsServiceAgentDo dfsServiceAgentDo,
            File file,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        if (!Boolean.TRUE.equals(dfsServiceAgentDo.getEnabled())) {
            throw new AbcResourceConflictException(String.format("%s::disabled", DfsServiceAgentDo.RESOURCE_SYMBOL));
        }
        Long serviceComponentUid = dfsServiceAgentDo.getServiceComponentUid();
        DfsServiceComponentDo dfsServiceComponentDo = this.dfsServiceComponentRepository.findByUid(serviceComponentUid);
        if (dfsServiceComponentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DfsServiceComponentDo.RESOURCE_SYMBOL));
        }
        String configuration = dfsServiceAgentDo.getConfiguration();

        switch (dfsServiceComponentDo.getType()) {
            case BUILTIN: {
                String entryClassName = dfsServiceComponentDo.getEntryClassName();
                DfsServiceProvider dfsServiceProvider = null;
                Map<String, DfsServiceProvider> candidateDfsServiceProviderMap =
                        this.applicationContext.getBeansOfType(DfsServiceProvider.class);
                if (!CollectionUtils.isEmpty(candidateDfsServiceProviderMap)) {
                    for (DfsServiceProvider candidateDfsServiceProvider : candidateDfsServiceProviderMap.values()) {
                        if (candidateDfsServiceProvider.getClass().getName().equals(entryClassName)) {
                            dfsServiceProvider = candidateDfsServiceProvider;
                            break;
                        }
                    }
                }
                if (dfsServiceProvider == null) {
                    throw new AbcResourceConflictException(String.format("cannot find dfs service provider:%s",
                            dfsServiceComponentDo.getName()));
                }

                try {
                    return dfsServiceProvider.uploadFile(file, configuration);
                } catch (Exception e) {
                    LOGGER.error("failed to upload file {} with dfs service provider {}", file.getAbsolutePath(),
                            dfsServiceComponentDo.getName(), e);
                    throw new AbcResourceConflictException("failed to upload file");
                }
            }
            case PLUGIN: {
                LOGGER.error("unsupported service provider {}", dfsServiceComponentDo);
                return null;
            }
            default:
                throw new AbcResourceConflictException(String.format("unsupported service provider type:%s",
                        dfsServiceComponentDo.getType()));
        }
    }
}
