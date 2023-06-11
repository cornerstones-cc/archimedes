package cc.cornerstones.zero.startup;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 运行环境保证
 */
@Component
public class InfraInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(InfraInitializer.class);

    /**
     * 几个通用的目录，要求系统启动时创建
     */
    @Value("${private.dir.general.project}")
    private String dirGeneralProject;
    @Value("${private.dir.general.project.download}")
    private String dirGeneralProjectDownload;
    @Value("${private.dir.general.project.upload}")
    private String dirGeneralProjectUpload;
    @Value("${private.dir.general.project.export}")
    private String dirGeneralProjectExport;
    @Value("${private.dir.general.project.logs}")
    private String dirGeneralProjectLogs;
    @Value("${private.dir.general.project.plugins}")
    private String dirGeneralProjectPlugins;
    @Value("${private.dir.general.project.extract}")
    private String dirGeneralProjectExtract;

    @Value("${private.audit.delete-local-files-exists-days}")
    private Integer auditDeleteLocalFilesExistsDays;

    public void execute() throws Exception {
        try {
            LOGGER.info("begin to init local directories");
            initLocalDirectories();
            LOGGER.info("end to init local directories");
        } catch (Exception e) {
            LOGGER.error("fail to init local directories");
            throw e;
        }
    }

    private void initLocalDirectories() throws Exception {
        Files.createDirectories(Paths.get(this.dirGeneralProject));
        Files.createDirectories(Paths.get(this.dirGeneralProjectDownload));
        Files.createDirectories(Paths.get(this.dirGeneralProjectUpload));
        Files.createDirectories(Paths.get(this.dirGeneralProjectExport));
        Files.createDirectories(Paths.get(this.dirGeneralProjectLogs));
        Files.createDirectories(Paths.get(this.dirGeneralProjectPlugins));
        Files.createDirectories(Paths.get(this.dirGeneralProjectExtract));
    }

    /**
     * 定时清理本地临时文件
     */
    @Scheduled(cron = "0 15 0 * * ?")
    public void handleCleanupLocalFilesJob() {
        cleanupDirectory(this.dirGeneralProjectDownload);
        cleanupDirectory(this.dirGeneralProjectUpload);
        cleanupDirectory(this.dirGeneralProjectExport);
        cleanupDirectory(this.dirGeneralProjectLogs);
        cleanupDirectory(this.dirGeneralProjectExtract);
    }

    private void cleanupDirectory(String directory) {
        LOGGER.info("[AUDIT100]cleanup local temp files in directory: {}", directory);

        LocalDateTime nowDateTime = java.time.LocalDateTime.now();
        LocalDateTime thresholdDateTime = nowDateTime.minusDays(this.auditDeleteLocalFilesExistsDays);
        long thresholdInMillis = thresholdDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        List<Path> result = null;
        try {
            Stream<Path> walk = Files.walk(Paths.get(directory));
            result = walk.filter(Files::isRegularFile)
                    .filter(path -> (path.toFile().lastModified() < thresholdInMillis))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("fail to walk directory: " + directory + ". " + e.getMessage(), e);
        }

        if (!CollectionUtils.isEmpty(result)) {
            result.forEach(path -> {
                try {
                    FileUtils.forceDelete(path.toFile());
                    LOGGER.warn("[AUDIT101]end to delete expired local file: " + path);
                } catch (IOException e) {
                    LOGGER.warn("[AUDIT102]fail to delete expired local file: " + path);
                }
            });
        }
    }
}
