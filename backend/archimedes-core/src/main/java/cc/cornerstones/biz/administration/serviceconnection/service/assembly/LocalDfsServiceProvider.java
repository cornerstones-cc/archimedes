package cc.cornerstones.biz.administration.serviceconnection.service.assembly;

import cc.cornerstones.almond.constants.InfrastructureConstants;
import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceIntegrityException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcFileUtils;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.archimedes.extensions.DfsServiceProvider;
import cc.cornerstones.biz.administration.serviceconnection.entity.DfsServiceComponentDo;
import cc.cornerstones.biz.administration.serviceconnection.persistence.DfsServiceComponentRepository;
import cc.cornerstones.biz.administration.serviceconnection.share.constants.ServiceComponentTypeEnum;
import cc.cornerstones.biz.distributedfile.service.inf.FileStorageService;
import cc.cornerstones.biz.share.event.EventBusManager;
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
import java.time.LocalDateTime;

@Component
public class LocalDfsServiceProvider extends DfsServiceProvider
        implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalDfsServiceProvider.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private DfsServiceComponentRepository dfsServiceComponentRepository;

    private static final String NAME = "Local DFS";
    private static final String DESCRIPTION = "Local DFS";
    private static final Float SEQUENCE = 3.0f;

    @Value("${private.dir.general.project.download}")
    private String projectDownloadPath;

    @Autowired
    private FileStorageService fileStorageService;

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
            serviceComponentDo.setResourceName("extensions/archimedes-ext-dfs-local.js");
            serviceComponentDo.setEntryClassName(LocalDfsServiceProvider.class.getName());

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
                "/local_dfs_service_provider_configuration_template.xml");
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

        //
        // Step 2, core-processing
        //
        UserProfile operatingUserProfile = new UserProfile();
        operatingUserProfile.setUid(InfrastructureConstants.ROOT_USER_UID);
        operatingUserProfile.setDisplayName(InfrastructureConstants.ROOT_USER_DISPLAY_NAME);

        return this.fileStorageService.getFile(fileId,
                operatingUserProfile);

        //
        // Step 3, post-processing
        //
    }

    @Override
    public String uploadFile(File file, String configuration) throws Exception {
        //
        // Step 1, pre-processing
        //
        Configuration objectiveConfiguration = parseConfiguration(configuration);

        //
        // Step 2, core-processing
        //
        UserProfile operatingUserProfile = new UserProfile();
        operatingUserProfile.setUid(InfrastructureConstants.ROOT_USER_UID);
        operatingUserProfile.setDisplayName(InfrastructureConstants.ROOT_USER_DISPLAY_NAME);

        String fileId = this.fileStorageService.storeFile(file, objectiveConfiguration.getHomeDirectoryName(),
                operatingUserProfile);
        return fileId;
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
            if ("home-directory".equals(element.getName())) {
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
         * 在 local dfs 的 home directory
         */
        private String homeDirectoryName;
    }

    public static void main(String[] args) {

    }

}
