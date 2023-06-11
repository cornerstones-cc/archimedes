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
import com.google.common.io.Files;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.OperationContext;
import com.microsoft.azure.storage.blob.*;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class AzureBlobDfsServiceProvider extends DfsServiceProvider
        implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureBlobDfsServiceProvider.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private DfsServiceComponentRepository dfsServiceComponentRepository;

    private static final String NAME = "Microsoft Azure Blob";
    private static final String DESCRIPTION = "Microsoft Azure Blob";
    private static final Float SEQUENCE = 1.0f;

    private static final String FORMAT = "DefaultEndpointsProtocol={0};AccountName={1};AccountKey={2};" +
            "EndpointSuffix={3}";

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
            serviceComponentDo.setResourceName("extensions/archimedes-ext-dfs-azureblob.js");
            serviceComponentDo.setEntryClassName(AzureBlobDfsServiceProvider.class.getName());

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
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("extensions/azureblob_dfs_service_provider_configuration_template.xml");
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

        // build target file path
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
            targetPath = Paths.get(this.projectDownloadPath, UUID.randomUUID().toString(),
                    fileId);
        }

        if (targetPath.toFile().exists()) {
            LOGGER.warn("delete existing file {} ({} bytes) in order to download a new file with the same file name",
                    targetPath.toString(), targetPath.toFile().length());
            targetPath.toFile().delete();
        }
        if (!targetPath.getParent().toFile().exists()) {
            targetPath.getParent().toFile().mkdirs();
        }

        String blobPath = null;
        if (ObjectUtils.isEmpty(objectiveConfiguration.getHomeDirectoryName())) {
            blobPath = fileId;
        } else {
            blobPath = Paths.get(objectiveConfiguration.getHomeDirectoryName(), fileId).toString();
        }

        //
        // Step 2, core-processing
        //
        CloudBlobContainer cloudBlobContainer = createCloudBlobContainer(objectiveConfiguration);

        CloudBlockBlob cloudBlockBlob = cloudBlobContainer.getBlockBlobReference(blobPath);

        long beginTime = System.currentTimeMillis();
        LOGGER.info("begin to download file from {} ({}) to local {}",
                blobPath, cloudBlockBlob.getStorageUri(),
                targetPath);

        cloudBlockBlob.downloadToFile(targetPath.toString());

        LOGGER.info("end to download file from {} ({}) to local {}, duration {}",
                blobPath, cloudBlockBlob.getStorageUri(),
                targetPath, AbcDateUtils.format(System.currentTimeMillis() - beginTime));

        return targetPath.toFile();

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

        //
        // Step 2.1, 本地复制出一个名叫 file id 的文件
        //
        String fileId = null;
        if (ObjectUtils.isEmpty(objectiveConfiguration.getHomeDirectoryName())) {
            fileId = encodeToFileId(null, file.getName());
        } else {
            fileId = encodeToFileId(objectiveConfiguration.getHomeDirectoryName(), file.getName());
        }
        File copiedFileForUpload = Paths.get(file.getParent(), fileId).toFile();
        Files.copy(file, copiedFileForUpload);

        //
        // Step 2.2, 上传
        //

        long beginTime = System.currentTimeMillis();
        LOGGER.info("begin to upload file from {} ({} bytes) to {}",
                file.getAbsolutePath(), file.length(), fileId);

        CloudBlobContainer cloudBlobContainer = createCloudBlobContainer(objectiveConfiguration);
        CloudBlobDirectory cloudBlobDirectory = cloudBlobContainer.getDirectoryReference(objectiveConfiguration.getHomeDirectoryName());
        CloudBlockBlob cloudBlockBlob = cloudBlobDirectory.getBlockBlobReference(fileId);
        cloudBlockBlob.uploadFromFile(copiedFileForUpload.getAbsolutePath());

        LOGGER.info("end to upload file from {} ({} bytes) to {}, duration {}",
                file.getAbsolutePath(), file.length(), cloudBlockBlob.getStorageUri(),
                AbcDateUtils.format(System.currentTimeMillis() - beginTime));

        //
        // Step 2.3, 校验本地文件和远程文件的内容 MD5
        //

        //
        // Step 2.4, 删除本地复制出的名叫 file id 的文件
        //
        copiedFileForUpload.delete();

        //
        // Step 2.5, 返回
        //
        return fileId;
    }

    private CloudBlobClient createCloudBlobClient(Configuration configuration) throws Exception {
        CloudStorageAccount cloudStorageAccount =
                CloudStorageAccount.parse(MessageFormat.format(FORMAT,
                        configuration.getProtocol(),
                        configuration.getAccountName(),
                        configuration.getAccountKey(),
                        configuration.getEndpoint()));
        return cloudStorageAccount.createCloudBlobClient();
    }

    private CloudBlobContainer createCloudBlobContainer(Configuration configuration) throws Exception {
        CloudBlobClient cloudBlobClient = createCloudBlobClient(configuration);
        CloudBlobContainer cloudBlobContainer = cloudBlobClient.getContainerReference(configuration.getContainerName());
        cloudBlobContainer.createIfNotExists(
                BlobContainerPublicAccessType.CONTAINER,
                new BlobRequestOptions(),
                new OperationContext());
        return cloudBlobContainer;
    }

    public Iterable<ListBlobItem> listBlobs(
            Configuration configuration,
            String prefix,
            boolean useFlatBlobListing) throws Exception {
        CloudBlobContainer cloudBlobContainer = createCloudBlobContainer(configuration);
        return cloudBlobContainer.listBlobs(prefix, useFlatBlobListing);
    }

    private static Configuration parseConfiguration(String content) throws DocumentException {
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
            if ("account".equals(element.getName())) {
                if (!CollectionUtils.isEmpty(element.attributes())) {
                    element.attributes().forEach(attribute -> {
                        if ("name".equals(attribute.getName())) {
                            configuration.setAccountName(attribute.getValue());
                        } else if ("key".equals(attribute.getName())) {
                            configuration.setAccountKey(attribute.getValue());
                        }
                    });
                }
            } else if ("endpoint".equals(element.getName())) {
                if (!CollectionUtils.isEmpty(element.attributes())) {
                    element.attributes().forEach(attribute -> {
                        if ("name".equals(attribute.getName())) {
                            configuration.setEndpoint(attribute.getValue());
                        }
                    });
                }
            } else if ("protocol".equals(element.getName())) {
                if (!CollectionUtils.isEmpty(element.attributes())) {
                    element.attributes().forEach(attribute -> {
                        if ("name".equals(attribute.getName())) {
                            configuration.setProtocol(attribute.getValue());
                        }
                    });
                }
            } else if ("container".equals(element.getName())) {
                if (!CollectionUtils.isEmpty(element.attributes())) {
                    element.attributes().forEach(attribute -> {
                        if ("name".equals(attribute.getName())) {
                            configuration.setContainerName(attribute.getValue());
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
        private String accountName;
        private String accountKey;
        private String endpoint;
        private String protocol;
        private String containerName;

        /**
         * 在 container 内被分配的 home directory
         */
        private String homeDirectoryName;
    }

    public static void main(String[] args) {

    }

}
