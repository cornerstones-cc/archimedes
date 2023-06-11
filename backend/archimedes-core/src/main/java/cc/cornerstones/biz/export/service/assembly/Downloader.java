package cc.cornerstones.biz.export.service.assembly;

import cc.cornerstones.almond.constants.DatabaseConstants;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.AbcTuple3;
import cc.cornerstones.almond.utils.AbcDateUtils;
import cc.cornerstones.almond.utils.AbcFileUtils;
import cc.cornerstones.almond.utils.AbcStringUtils;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DfsServiceAgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Component
public class Downloader {
    private static final Logger LOGGER = LoggerFactory.getLogger(Downloader.class);

    @Autowired
    private DfsServiceAgentService dfsServiceAgentService;

    public void downloadByUrl(
            AbcTuple3<String, String, String> input) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        String serialNumber = UUID.randomUUID().toString();

        String url = input.f;

        String targetDirectory = input.s;

        String targetFileName = input.t;
        if (ObjectUtils.isEmpty(targetFileName)) {
            targetFileName = UUID.randomUUID().toString();
        } else {
            targetFileName = AbcFileUtils.replaceIllegalCharactersWithEmpty(targetFileName);
        }
        if (targetFileName.length() > DatabaseConstants.MAXIMUM_FILE_NAME_LENGTH) {
            targetFileName = targetFileName.substring(0, DatabaseConstants.MAXIMUM_FILE_NAME_LENGTH);
        } else if (targetFileName.length() == 0) {
            targetFileName = UUID.randomUUID().toString();
        }

        String prefixOfTargetFileName = null;
        String suffixOfTargetFileName = null;

        int suffixIndexOfTargetFileName = targetFileName.lastIndexOf(".");
        if (suffixIndexOfTargetFileName < 0) {
            prefixOfTargetFileName = targetFileName;
        } else {
            prefixOfTargetFileName = targetFileName.substring(0, suffixIndexOfTargetFileName);
            suffixOfTargetFileName = targetFileName.substring(suffixIndexOfTargetFileName + 1);
        }

        Path targetPath = Paths.get(targetDirectory, targetFileName);
        int copy = 1;
        while (targetPath.toFile().exists()) {
            if (suffixOfTargetFileName == null) {
                targetFileName = prefixOfTargetFileName + "(" + copy + ")";
            } else {
                targetFileName = prefixOfTargetFileName + "(" + copy + ")" + "." + suffixOfTargetFileName;
            }
            targetPath = Paths.get(targetDirectory, targetFileName);
            copy++;
        }

        //
        // Step 2, core-processing
        //
        long beginTime = System.currentTimeMillis();
        LOGGER.info("[download-by-url] serial number {}, trying download url {}, target path {}",
                serialNumber,
                url, targetPath);

        InputStream is = null;
        try {
            is = new URL(url).openStream();

            Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);

            is.close();

            LOGGER.info("[download-by-url] serial number {}, downloaded url {}, target path {}, duration:{}",
                    serialNumber,
                    url, targetPath, AbcDateUtils.format(System.currentTimeMillis() - beginTime));

            // 处理后缀名
            if (suffixOfTargetFileName == null) {
                FileInputStream fileInputStream = null;
                try {
                    fileInputStream = new FileInputStream(targetPath.toFile());
                    String contentType = URLConnection.guessContentTypeFromStream(new BufferedInputStream(fileInputStream));
                    LOGGER.warn("[download-by-url] serial number {}, detected content type {} of file {}",
                            serialNumber,
                            contentType, targetPath);
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
                            LOGGER.warn("[download-by-url] serial number {}, unknown content type {}",
                                    serialNumber,
                                    contentType);
                        }
                    }
                } finally {
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                }

                if (suffixOfTargetFileName != null) {
                    prefixOfTargetFileName = targetFileName;
                    targetFileName = prefixOfTargetFileName + "." + suffixOfTargetFileName;
                    Path newTargetPath = Paths.get(targetDirectory, targetFileName);

                    copy = 1;
                    while (newTargetPath.toFile().exists()) {
                        targetFileName = prefixOfTargetFileName + "(" + copy + ")" + "." + suffixOfTargetFileName;
                        newTargetPath = Paths.get(targetDirectory, targetFileName);
                        copy++;
                    }

                    beginTime = System.currentTimeMillis();
                    LOGGER.info("[download-by-url] serial number {}, trying to move file from target path {} to new target path {}",
                            serialNumber,
                            targetPath, newTargetPath);

                    Files.move(targetPath, newTargetPath, StandardCopyOption.REPLACE_EXISTING);

                    LOGGER.info("[download-by-url] serial number {}, moved file from target path {} to new target path {}, " +
                                    "duration:{}",
                            serialNumber,
                            targetPath, newTargetPath, AbcDateUtils.format(System.currentTimeMillis() - beginTime));
                }
            }
        } catch (IOException e) {
            LOGGER.error("[download-by-url] serial number {}, failed to download file from {}",
                    serialNumber,
                    url, e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    LOGGER.error("", e);
                }
            }
        }
    }

    public void moveFile(
            AbcTuple3<String, String, String> input) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        String serialNumber = UUID.randomUUID().toString();

        Path sourcePath = Paths.get(input.f);

        String targetDirectory = input.s;

        String targetFileName = input.t;
        if (ObjectUtils.isEmpty(targetFileName)) {
            targetFileName = sourcePath.getFileName().toString();
        } else {
            targetFileName = AbcFileUtils.replaceIllegalCharactersWithEmpty(targetFileName);
        }
        if (targetFileName.length() > DatabaseConstants.MAXIMUM_FILE_NAME_LENGTH) {
            targetFileName = targetFileName.substring(0, DatabaseConstants.MAXIMUM_FILE_NAME_LENGTH);
        } else if (targetFileName.length() == 0) {
            targetFileName = UUID.randomUUID().toString();
        }

        String prefixOfTargetFileName = null;
        String suffixOfTargetFileName = null;

        int suffixIndexOfTargetFileName = targetFileName.lastIndexOf(".");
        if (suffixIndexOfTargetFileName < 0) {
            prefixOfTargetFileName = targetFileName;
        } else {
            prefixOfTargetFileName = targetFileName.substring(0, suffixIndexOfTargetFileName);
            suffixOfTargetFileName = targetFileName.substring(suffixIndexOfTargetFileName + 1);
        }

        Path targetPath = Paths.get(targetDirectory, targetFileName);
        int copy = 1;
        while (targetPath.toFile().exists()) {
            if (suffixOfTargetFileName == null) {
                targetFileName = prefixOfTargetFileName + "(" + copy + ")";
            } else {
                targetFileName = prefixOfTargetFileName + "(" + copy + ")" + "." + suffixOfTargetFileName;
            }
            targetPath = Paths.get(targetDirectory, targetFileName);
            copy++;
        }

        //
        // Step 2, core-processing
        //
        try {
            long beginTime = System.currentTimeMillis();
            LOGGER.info("[move-file] serial number {}, trying to move file from source path {} to target path {}",
                    serialNumber,
                    sourcePath, targetPath);

            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            LOGGER.info("[move-file] serial number {}, moved file from source path {} to target path {}, duration:{}",
                    serialNumber,
                    sourcePath, targetPath, AbcDateUtils.format(System.currentTimeMillis() - beginTime));
        } catch (IOException e) {
            LOGGER.error("[move-file] serial number {}, failed to download file from {}",
                    serialNumber,
                    input.f, e);
        }
    }

    /**
     * @param inputList             Tuple 3, first -- file id, second -- target directory path, third -- target file name
     * @param dfsServiceAgentUid
     * @throws AbcUndefinedException
     */
    public void downloadByFileId(
            List<AbcTuple3<String, String, String>> inputList,
            Long dfsServiceAgentUid) throws AbcUndefinedException {
        try {
            this.dfsServiceAgentService.downloadFiles(dfsServiceAgentUid, inputList, null);
        } catch (Exception e) {
            LOGGER.error("[dfs] failed to download file {} from DFS {}", dfsServiceAgentUid,
                    AbcStringUtils.toString(inputList, ","), e);
        }
    }
}
