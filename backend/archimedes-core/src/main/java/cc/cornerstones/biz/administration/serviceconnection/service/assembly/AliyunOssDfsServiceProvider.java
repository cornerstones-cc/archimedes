package cc.cornerstones.biz.administration.serviceconnection.service.assembly;

import cc.cornerstones.almond.constants.InfrastructureConstants;
import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceIntegrityException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.utils.AbcDateUtils;
import cc.cornerstones.almond.utils.AbcFileUtils;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.archimedes.extensions.DfsServiceProvider;
import cc.cornerstones.archimedes.extensions.types.DecodedFileId;
import cc.cornerstones.biz.administration.serviceconnection.entity.DfsServiceComponentDo;
import cc.cornerstones.biz.administration.serviceconnection.persistence.DfsServiceComponentRepository;
import cc.cornerstones.biz.administration.serviceconnection.share.constants.ServiceComponentTypeEnum;
import cc.cornerstones.biz.share.event.EventBusManager;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectRequest;
import lombok.Data;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class AliyunOssDfsServiceProvider extends DfsServiceProvider
        implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AliyunOssDfsServiceProvider.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private DfsServiceComponentRepository dfsServiceComponentRepository;

    private static final String NAME = "Alibaba Aliyun OSS";
    private static final String DESCRIPTION = "Alibaba Aliyun OSS";
    private static final Float SEQUENCE = 2.0f;

    @Value("${private.dir.general.project.download}")
    private String projectDownloadPath;

    /**
     * Handle an application event.
     *
     * @param event the event to respond to
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        DfsServiceComponentDo serviceComponentDo = this.dfsServiceComponentRepository.findByName(NAME);
        if (serviceComponentDo == null) {
            serviceComponentDo = new DfsServiceComponentDo();
            serviceComponentDo.setUid(this.idHelper.getNextDistributedId(DfsServiceComponentDo.RESOURCE_NAME));
            serviceComponentDo.setName(NAME);
            serviceComponentDo.setObjectName(NAME.replaceAll("_", "__")
                    .replaceAll("\\s", "_")
                    .toLowerCase());
            serviceComponentDo.setDescription(DESCRIPTION);
            serviceComponentDo.setSequence(SEQUENCE);
            serviceComponentDo.setType(ServiceComponentTypeEnum.BUILTIN);
            serviceComponentDo.setResourceName("extensions/archimedes-ext-dfs-aliyunoss.js");
            serviceComponentDo.setEntryClassName(AliyunOssDfsServiceProvider.class.getName());

            try {
                String configurationTemplate = getConfigurationTemplate();
                serviceComponentDo.setConfigurationTemplate(configurationTemplate);
            } catch (Exception e) {
                LOGGER.error("failed to load configuration template", e);
            }

            BaseDo.create(serviceComponentDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
            this.dfsServiceComponentRepository.save(serviceComponentDo);

            LOGGER.info("init built-in dfs service component::name = {}, uid = {}", serviceComponentDo.getName(),
                    serviceComponentDo.getUid());
        }
    }

    @Override
    public String getConfigurationTemplate() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("extensions" +
                "/aliyunoss_dfs_service_provider_configuration_template.xml");
        if (inputStream == null) {
            throw new AbcResourceIntegrityException("cannot find resource");
        } else {
            return AbcFileUtils.readContent(inputStream);
        }
    }

    @Override
    public File downloadFile(String fileId, String configuration) throws Exception {
        //
        // Step 1, pre-processing
        //
        if (ObjectUtils.isEmpty(configuration)) {
            throw new AbcResourceConflictException("configuration is null or empty");
        }
        Configuration objectiveConfiguration = parseConfiguration(configuration);
        if (objectiveConfiguration == null) {
            throw new AbcResourceConflictException("objective configuration is null or empty");
        }

        // build target path
        DecodedFileId decodedFileId = null;
        try {
            decodedFileId = decodeFromFileId(fileId);
        } catch (Exception e) {
            // unknown encoder
            LOGGER.warn("unknown encoded file id:{}, continue", fileId);
        }
        Path targetPath = null;
        if (decodedFileId != null && !ObjectUtils.isEmpty(decodedFileId.getFileName())) {
            targetPath = Paths.get(this.projectDownloadPath, UUID.randomUUID().toString(),
                    decodedFileId.getFileName());
        } else {
            targetPath = Paths.get(this.projectDownloadPath, UUID.randomUUID().toString(), fileId);
        }

        if (targetPath.toFile().exists()) {
            LOGGER.warn("delete existing file {} ({} bytes) in order to download a new file with the same file name",
                    targetPath.toString(), targetPath.toFile().length());
            targetPath.toFile().delete();
        }
        if (!targetPath.getParent().toFile().exists()) {
            targetPath.getParent().toFile().mkdirs();
        }

        //
        // Step 2, core-processing
        //

        OSS ossClient = new OSSClientBuilder().build(
                objectiveConfiguration.getOssEndpoint(),
                objectiveConfiguration.getRamUserAccessKeyId(),
                objectiveConfiguration.getRamUserAccessKeySecret());
        try {
            String objectKey = null;
            if (ObjectUtils.isEmpty(objectiveConfiguration.getHomeDirectoryName())) {
                objectKey = fileId;
            } else {
                objectKey = Paths.get(objectiveConfiguration.getHomeDirectoryName(), fileId).toString();
            }

            long beginTime = System.currentTimeMillis();
            LOGGER.info("begin to download file from {} ({}) to local {}",
                    objectKey, objectiveConfiguration.getBucketName(),
                    targetPath);

            GetObjectRequest getObjectRequest = new GetObjectRequest(objectiveConfiguration.getBucketName(), objectKey);
            OSSObject object = ossClient.getObject(getObjectRequest);

            Files.copy(object.getObjectContent(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            LOGGER.info("end to download file from {} ({}) to local {}, duration {}",
                    objectKey, objectiveConfiguration.getBucketName(),
                    targetPath, AbcDateUtils.format(System.currentTimeMillis() - beginTime));

            return targetPath.toFile();
        } finally {
            ossClient.shutdown();
        }

        //
        // Step 3, post-processing
        //
    }

    @Override
    public String uploadFile(File file, String configuration) throws Exception {
        //
        // Step 1, pre-processing
        //
        if (ObjectUtils.isEmpty(configuration)) {
            throw new AbcResourceConflictException("configuration is null or empty");
        }
        Configuration objectiveConfiguration = parseConfiguration(configuration);
        if (objectiveConfiguration == null) {
            throw new AbcResourceConflictException("objective configuration is null or empty");
        }

        //
        // Step 2, core-processing
        //
        OSS ossClient = new OSSClientBuilder().build(
                objectiveConfiguration.getOssEndpoint(),
                objectiveConfiguration.getRamUserAccessKeyId(),
                objectiveConfiguration.getRamUserAccessKeySecret());

        //
        // Step 2.1, 准备 file id
        //
        String fileId = null;
        if (ObjectUtils.isEmpty(objectiveConfiguration.getHomeDirectoryName())) {
            fileId = encodeToFileId(null, file.getName());
        } else {
            fileId = encodeToFileId(objectiveConfiguration.getHomeDirectoryName(), file.getName());
        }

        //
        // Step 2.2, 上传
        //
        String objectKey = null;
        if (ObjectUtils.isEmpty(objectiveConfiguration.getHomeDirectoryName())) {
            objectKey = fileId;
        } else {
            objectKey = Paths.get(objectiveConfiguration.getHomeDirectoryName(), fileId).toString();
        }

        long beginTime = System.currentTimeMillis();
        LOGGER.info("begin to upload file from {} ({} bytes) to {} ({})",
                file.getAbsolutePath(), file.length(), objectKey, objectiveConfiguration.getBucketName());

        try {
            PutObjectRequest pubObjectRequest = new PutObjectRequest(objectiveConfiguration.getBucketName(), objectKey, file);

            ossClient.putObject(pubObjectRequest);

            LOGGER.info("end to upload file from {} ({} bytes) to {} ({}), duration {}",
                    file.getAbsolutePath(), file.length(), objectKey, objectiveConfiguration.getBucketName(),
                    AbcDateUtils.format(System.currentTimeMillis() - beginTime));
        } finally {
            ossClient.shutdown();
        }

        //
        // Step 2.3, 校验本地文件和远程文件 的内容 MD5
        //

        return fileId;
    }

    private static Configuration parseConfiguration(
            String content) throws DocumentException {
        if (ObjectUtils.isEmpty(content)) {
            return null;
        }

        SAXReader saxReader = new SAXReader();
        Document document = saxReader.read(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));

        Element configurationElement = (Element) document.selectSingleNode("//configuration");
        if (configurationElement == null || configurationElement.elements().isEmpty()) {
            return null;
        }

        Configuration configuration = new Configuration();
        for (Element element : configurationElement.elements()) {
            if ("oss-endpoint".equals(element.getName())) {
                if (!CollectionUtils.isEmpty(element.attributes())) {
                    element.attributes().forEach(attribute -> {
                        if ("name".equals(attribute.getName())) {
                            configuration.setOssEndpoint(attribute.getValue());
                        }
                    });
                }
            } else if ("ram-user-access-key".equals(element.getName())) {
                if (!CollectionUtils.isEmpty(element.attributes())) {
                    element.attributes().forEach(attribute -> {
                        if ("id".equals(attribute.getName())) {
                            configuration.setRamUserAccessKeyId(attribute.getValue());
                        } else if ("secret".equals(attribute.getName())) {
                            configuration.setRamUserAccessKeySecret(attribute.getValue());
                        }
                    });
                }
            } else if ("bucket".equals(element.getName())) {
                if (!CollectionUtils.isEmpty(element.attributes())) {
                    element.attributes().forEach(attribute -> {
                        if ("name".equals(attribute.getName())) {
                            configuration.setBucketName(attribute.getValue());
                        }
                    });
                }
            } else if ("home-directory".equals(element.getName())) {
                if (!CollectionUtils.isEmpty(element.attributes())) {
                    element.attributes().forEach(attribute -> {
                        if ("name".equals(attribute.getName())) {
                            configuration.setHomeDirectoryName(attribute.getValue());
                        }
                    });
                }
            }
        }

        return configuration;
    }

    @Data
    private static class Configuration {
        /**
         * RAM (Resource Access Management) 用户的 AccessKey ID
         */
        private String ramUserAccessKeyId;

        /**
         * RAM 用户的 AccessKey Secret
         */
        private String ramUserAccessKeySecret;

        /**
         * Bucket 所在地域对应的 Endpoint
         */
        private String ossEndpoint;

        /**
         * Bucket name
         */
        private String bucketName;

        /**
         * 在 bucket 内被分配的 home directory without slash
         */
        private String homeDirectoryName;

        /**
         * STS （Security Token Service，是阿里云提供的一种临时访问权限管理服务）接入地址
         */
        private String stsEndpoint;

        /**
         * 用于获取临时访问凭证的角色
         */
        private String ramRoleName;

        /**
         * 自定义角色会话名称，用来区分不同的令牌
         */
        private String ramRoleSessionName;
    }

    public static void main(String[] args) {
        URL resource = AliyunOssDfsServiceProvider.class.getClassLoader().getResource("extensions" +
                "/aliyunoss_dfs_service_provider_configuration_template.xml");
        if (resource == null) {
            throw new IllegalArgumentException("file not found");
        } else {
            try {
                File file = new File(resource.toURI());

                String fileContent = AbcFileUtils.readContent(file, StandardCharsets.UTF_8);

                Configuration configuration = parseConfiguration(fileContent);

                System.out.println(configuration);
            } catch (URISyntaxException e1) {
                e1.printStackTrace();
            } catch (DocumentException e2) {
                e2.printStackTrace();
            }

        }
    }
}
