package cc.cornerstones.biz.administration.serviceconnection.service.impl;

import cc.cornerstones.almond.exceptions.*;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserBriefInformation;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcFileUtils;
import cc.cornerstones.arbutus.pf4j.service.assembly.PluginHelper;
import cc.cornerstones.arbutus.pf4j.share.types.PluginProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.archimedes.extensions.DataPermissionServiceProvider;
import cc.cornerstones.biz.administration.serviceconnection.dto.*;
import cc.cornerstones.biz.administration.serviceconnection.entity.AuthenticationServiceComponentDo;
import cc.cornerstones.biz.administration.serviceconnection.entity.DataPermissionServiceComponentDo;
import cc.cornerstones.biz.administration.serviceconnection.persistence.DataPermissionServiceComponentRepository;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DataPermissionServiceComponentService;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DfsServiceAgentService;
import cc.cornerstones.biz.administration.serviceconnection.share.constants.ServiceComponentTypeEnum;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.share.assembly.ResourceReferenceManager;
import cc.cornerstones.biz.share.event.EventBusManager;
import org.pf4j.PluginAlreadyLoadedException;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DataPermissionServiceComponentServiceImpl implements DataPermissionServiceComponentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataPermissionServiceComponentServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private DataPermissionServiceComponentRepository dataPermissionServiceComponentRepository;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    private PluginHelper pluginHelper;

    @Autowired
    private DfsServiceAgentService dfsServiceAgentService;

    @Autowired
    private UserService userService;

    @Autowired
    private ResourceReferenceManager resourceReferenceManager;


    @Override
    public DataPermissionServiceComponentDto createPluginDataPermissionServiceComponent(
            CreatePluginDataPermissionServiceComponentDto createPluginDataPermissionServiceComponentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        boolean existsDuplicate =
                this.dataPermissionServiceComponentRepository.existsByName(createPluginDataPermissionServiceComponentDto.getName());
        if (existsDuplicate) {
            throw new AbcResourceDuplicateException(String.format("%s::name=%s", DataPermissionServiceComponentDo.RESOURCE_SYMBOL,
                    createPluginDataPermissionServiceComponentDto.getName()));
        }

        //
        // Step 2, core-processing
        //
        DataPermissionServiceComponentDo dataPermissionServiceComponentDo = new DataPermissionServiceComponentDo();
        dataPermissionServiceComponentDo.setUid(this.idHelper.getNextDistributedId(DataPermissionServiceComponentDo.RESOURCE_NAME));
        dataPermissionServiceComponentDo.setName(createPluginDataPermissionServiceComponentDto.getName());
        dataPermissionServiceComponentDo.setObjectName(
                createPluginDataPermissionServiceComponentDto.getName()
                        .replaceAll("_", "__")
                        .replaceAll("\\s", "_")
                        .toLowerCase());
        dataPermissionServiceComponentDo.setDescription(createPluginDataPermissionServiceComponentDto.getDescription());
        dataPermissionServiceComponentDo.setSequence(createPluginDataPermissionServiceComponentDto.getSequence());
        dataPermissionServiceComponentDo.setType(ServiceComponentTypeEnum.PLUGIN);
        dataPermissionServiceComponentDo.setFrontEndComponentFileId(
                createPluginDataPermissionServiceComponentDto.getFrontEndComponentFileId());
        dataPermissionServiceComponentDo.setDfsServiceAgentUidOfFrontEndComponentFileId(
                createPluginDataPermissionServiceComponentDto.getDfsServiceAgentUidOfFrontEndComponentFileId());
        dataPermissionServiceComponentDo.setBackEndComponentFileId(
                createPluginDataPermissionServiceComponentDto.getBackEndComponentFileId());
        dataPermissionServiceComponentDo.setDfsServiceAgentUidOfBackEndComponentFileId(
                createPluginDataPermissionServiceComponentDto.getDfsServiceAgentUidOfBackEndComponentFileId());

        // back end component metadata & configuration template
        File file = this.dfsServiceAgentService.downloadFile(
                createPluginDataPermissionServiceComponentDto.getDfsServiceAgentUidOfBackEndComponentFileId(),
                createPluginDataPermissionServiceComponentDto.getBackEndComponentFileId(),
                operatingUserProfile);
        BackEndComponentParsedResultDto backEndComponentParsedResultDto =
                parseBackEndComponent(file, operatingUserProfile);
        dataPermissionServiceComponentDo.setBackEndComponentMetadata(backEndComponentParsedResultDto.getMetadata());
        dataPermissionServiceComponentDo.setConfigurationTemplate(backEndComponentParsedResultDto.getConfigurationTemplate());

        BaseDo.create(dataPermissionServiceComponentDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dataPermissionServiceComponentRepository.save(dataPermissionServiceComponentDo);

        //
        // Step 3, post-processing
        //
        DataPermissionServiceComponentDto dataPermissionServiceComponentDto = new DataPermissionServiceComponentDto();
        BeanUtils.copyProperties(dataPermissionServiceComponentDto, dataPermissionServiceComponentDto);
        return dataPermissionServiceComponentDto;
    }

    @Override
    public BackEndComponentParsedResultDto parseBackEndComponent(
            File file,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        String configurationTemplate = null;

        String pluginId = null;

        // 是否本次调用 loaded 该 plugin，如果是，本次调用结束时候要主动 unloaded
        boolean loadedHere = false;
        // 是否本次调用 started 该 plugin，如果是，本次调用结束时候要主动 stopped
        boolean startedHere = false;

        //
        // Step 2, core-processing
        //

        // Step 2.1, load plugin
        try {
            pluginId = this.pluginHelper.getPluginManager().loadPlugin(file.toPath());
            loadedHere = true;
        } catch (PluginAlreadyLoadedException e) {
            // 已经 loaded
            pluginId = e.getPluginId();

            PluginWrapper pluginWrapper = this.pluginHelper.getPluginManager().getPlugin(pluginId);
            if (pluginWrapper == null) {
                String logMsg = String.format("found conflict while loading plugin at %s, found an already loaded " +
                                "plugin %s at %s, but cannot get this plugin",
                        file.getAbsolutePath(), pluginId, e.getPluginPath());
                LOGGER.error(logMsg, e);
                throw new AbcResourceConflictException(String.format("failed to load plugin"));
            } else {
                String logMsg = String.format("found an already loaded plugin at %s, it is %s at %s and the " +
                                "plugin state is %s",
                        file.getAbsolutePath(), pluginId, e.getPluginPath(),
                        pluginWrapper.getPluginState());
                LOGGER.info(logMsg);
            }
        }


        // Step 2.2, start plugin
        PluginWrapper pluginWrapper = null;
        try {
            pluginWrapper = this.pluginHelper.getPluginManager().getPlugin(pluginId);
        } catch (Exception e) {
            String logMsg = String.format("failed to get plugin %s", pluginId);
            LOGGER.error(logMsg, e);

            if (loadedHere) {
                try {
                    this.pluginHelper.getPluginManager().unloadPlugin(pluginId);
                } catch (Exception e2) {
                    LOGGER.error("failed to unload plugin {}", pluginId, e2);
                }
            }

            throw new AbcResourceConflictException("failed to get plugin");
        }

        try {
            if (!PluginState.STARTED.equals(pluginWrapper.getPluginState())) {
                this.pluginHelper.getPluginManager().startPlugin(pluginId);
                startedHere = true;
            }
        } catch (Exception e) {
            String logMsg = String.format("failed to start plugin %s", pluginId);
            LOGGER.error(logMsg, e);

            if (loadedHere) {
                try {
                    this.pluginHelper.getPluginManager().unloadPlugin(pluginId);
                } catch (Exception e2) {
                    LOGGER.error("failed to unload plugin {}", pluginId, e2);
                }
            }

            throw new AbcResourceConflictException("failed to start plugin");
        }

        // Step 2.3
        try {
            List<DataPermissionServiceProvider> listOfProcessors =
                    this.pluginHelper.getPluginManager().getExtensions(
                            DataPermissionServiceProvider.class,
                            pluginId);
            if (listOfProcessors == null || listOfProcessors.isEmpty()) {
                throw new AbcUndefinedException("cannot find DataPermissionServiceProvider");
            }

            if (listOfProcessors.size() > 1) {
                throw new AbcUndefinedException("found " + listOfProcessors.size() + " DataPermissionServiceProvider");
            }

            DataPermissionServiceProvider processor = listOfProcessors.get(0);

            configurationTemplate = processor.getConfigurationTemplate();

            PluginProfile pluginProfile = this.pluginHelper.parsePluginProfile(pluginId);

            //
            BackEndComponentParsedResultDto result = new BackEndComponentParsedResultDto();
            result.setMetadata(pluginProfile);
            result.setConfigurationTemplate(configurationTemplate);
            return result;
        } catch (Exception e) {
            LOGGER.error("failed to get configuration template from the plugin {}",
                    file.getAbsolutePath(), e);
            throw new AbcResourceConflictException("failed to parse back end component");
        } finally {
            if (startedHere) {
                try {
                    this.pluginHelper.getPluginManager().stopPlugin(pluginId);
                } catch (Exception e) {
                    LOGGER.error("failed to stop plugin {}", pluginId, e);
                }
            }
            if (loadedHere) {
                try {
                    this.pluginHelper.getPluginManager().unloadPlugin(pluginId);
                } catch (Exception e) {
                    LOGGER.error("failed to unload plugin {}", pluginId, e);
                }
            }
        }
    }

    @Override
    public void updateDataPermissionServiceComponent(
            Long uid,
            UpdateDataPermissionServiceComponentDto updateDataPermissionServiceComponentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DataPermissionServiceComponentDo dataPermissionServiceComponentDo = this.dataPermissionServiceComponentRepository.findByUid(uid);
        if (dataPermissionServiceComponentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataPermissionServiceComponentDo.RESOURCE_SYMBOL,
                    uid));
        }

        //
        // Step 2, core-processing
        //
        switch (dataPermissionServiceComponentDo.getType()) {
            case BUILTIN: {
                updateBuiltinDataPermissionsServiceComponent(dataPermissionServiceComponentDo, updateDataPermissionServiceComponentDto,
                        operatingUserProfile);
            }
            break;
            case PLUGIN: {
                updatePluginDataPermissionsServiceComponent(dataPermissionServiceComponentDo, updateDataPermissionServiceComponentDto,
                        operatingUserProfile);
            }
            break;
            default:
                throw new AbcResourceConflictException(String.format("unknown service provider type:%s",
                        dataPermissionServiceComponentDo.getType()));
        }
    }

    private void updateBuiltinDataPermissionsServiceComponent(
            DataPermissionServiceComponentDo dataPermissionServiceComponentDo,
            UpdateDataPermissionServiceComponentDto updateDataPermissionServiceComponentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        boolean requiredToUpdate = false;

        if (updateDataPermissionServiceComponentDto.getDescription() != null
                && !updateDataPermissionServiceComponentDto.getDescription().equalsIgnoreCase(dataPermissionServiceComponentDo.getDescription())) {
            dataPermissionServiceComponentDo.setDescription(updateDataPermissionServiceComponentDto.getDescription());
            requiredToUpdate = true;
        }
        if (updateDataPermissionServiceComponentDto.getSequence() != null
                && !updateDataPermissionServiceComponentDto.getSequence().equals(dataPermissionServiceComponentDo.getSequence())) {
            dataPermissionServiceComponentDo.setSequence(updateDataPermissionServiceComponentDto.getSequence());
            requiredToUpdate = true;
        }

        if (requiredToUpdate) {
            BaseDo.update(dataPermissionServiceComponentDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.dataPermissionServiceComponentRepository.save(dataPermissionServiceComponentDo);
        }
    }

    private void updatePluginDataPermissionsServiceComponent(
            DataPermissionServiceComponentDo dataPermissionServiceComponentDo,
            UpdateDataPermissionServiceComponentDto updateDataPermissionServiceComponentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        if (!ObjectUtils.isEmpty(updateDataPermissionServiceComponentDto.getName())
                && !updateDataPermissionServiceComponentDto.getName().equalsIgnoreCase(dataPermissionServiceComponentDo.getName())) {
            boolean existsDuplicate =
                    this.dataPermissionServiceComponentRepository.existsByName(updateDataPermissionServiceComponentDto.getName());
            if (existsDuplicate) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s", DataPermissionServiceComponentDo.RESOURCE_SYMBOL,
                        updateDataPermissionServiceComponentDto.getName()));
            }
        }

        //
        // Step 2, core-processing
        //
        boolean requiredToUpdate = false;

        if (!ObjectUtils.isEmpty(updateDataPermissionServiceComponentDto.getName())
                && !updateDataPermissionServiceComponentDto.getName().equalsIgnoreCase(dataPermissionServiceComponentDo.getName())) {
            dataPermissionServiceComponentDo.setName(updateDataPermissionServiceComponentDto.getName());
            dataPermissionServiceComponentDo.setObjectName(
                    updateDataPermissionServiceComponentDto.getName()
                            .replaceAll("_", "__")
                            .replaceAll("\\s", "_")
                            .toLowerCase());
            requiredToUpdate = true;
        }
        if (updateDataPermissionServiceComponentDto.getDescription() != null
                && !updateDataPermissionServiceComponentDto.getDescription().equalsIgnoreCase(dataPermissionServiceComponentDo.getDescription())) {
            dataPermissionServiceComponentDo.setDescription(updateDataPermissionServiceComponentDto.getDescription());
            requiredToUpdate = true;
        }
        if (updateDataPermissionServiceComponentDto.getSequence() != null
                && !updateDataPermissionServiceComponentDto.getSequence().equals(dataPermissionServiceComponentDo.getSequence())) {
            dataPermissionServiceComponentDo.setSequence(updateDataPermissionServiceComponentDto.getSequence());
            requiredToUpdate = true;
        }
        if (!ObjectUtils.isEmpty(updateDataPermissionServiceComponentDto.getFrontEndComponentFileId())
                && !updateDataPermissionServiceComponentDto.getFrontEndComponentFileId().equalsIgnoreCase(dataPermissionServiceComponentDo.getFrontEndComponentFileId())) {
            dataPermissionServiceComponentDo.setFrontEndComponentFileId(updateDataPermissionServiceComponentDto.getFrontEndComponentFileId());
            requiredToUpdate = true;
        }
        if (updateDataPermissionServiceComponentDto.getDfsServiceAgentUidOfFrontEndComponentFileId() != null
                && !updateDataPermissionServiceComponentDto.getDfsServiceAgentUidOfFrontEndComponentFileId().equals(dataPermissionServiceComponentDo.getDfsServiceAgentUidOfFrontEndComponentFileId())) {
            dataPermissionServiceComponentDo.setDfsServiceAgentUidOfFrontEndComponentFileId(updateDataPermissionServiceComponentDto.getDfsServiceAgentUidOfFrontEndComponentFileId());
            requiredToUpdate = true;
        }
        if (!ObjectUtils.isEmpty(updateDataPermissionServiceComponentDto.getBackEndComponentFileId())
                && !updateDataPermissionServiceComponentDto.getBackEndComponentFileId().equalsIgnoreCase(dataPermissionServiceComponentDo.getBackEndComponentFileId())) {
            dataPermissionServiceComponentDo.setBackEndComponentFileId(updateDataPermissionServiceComponentDto.getBackEndComponentFileId());

            // back end component metadata & configuration template
            File file = this.dfsServiceAgentService.downloadFile(
                    updateDataPermissionServiceComponentDto.getDfsServiceAgentUidOfBackEndComponentFileId(),
                    updateDataPermissionServiceComponentDto.getBackEndComponentFileId(),
                    operatingUserProfile);
            BackEndComponentParsedResultDto backEndComponentParsedResultDto =
                    parseBackEndComponent(file, operatingUserProfile);
            dataPermissionServiceComponentDo.setBackEndComponentMetadata(backEndComponentParsedResultDto.getMetadata());
            dataPermissionServiceComponentDo.setConfigurationTemplate(backEndComponentParsedResultDto.getConfigurationTemplate());

            requiredToUpdate = true;
        }
        if (updateDataPermissionServiceComponentDto.getDfsServiceAgentUidOfBackEndComponentFileId() != null
                && !updateDataPermissionServiceComponentDto.getDfsServiceAgentUidOfBackEndComponentFileId().equals(dataPermissionServiceComponentDo.getDfsServiceAgentUidOfBackEndComponentFileId())) {
            dataPermissionServiceComponentDo.setDfsServiceAgentUidOfBackEndComponentFileId(updateDataPermissionServiceComponentDto.getDfsServiceAgentUidOfBackEndComponentFileId());
            requiredToUpdate = true;
        }

        if (requiredToUpdate) {
            BaseDo.update(dataPermissionServiceComponentDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.dataPermissionServiceComponentRepository.save(dataPermissionServiceComponentDo);
        }
    }

    @Override
    public List<String> listAllReferencesToDataPermissionServiceComponent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DataPermissionServiceComponentDo dataPermissionServiceComponentDo = this.dataPermissionServiceComponentRepository.findByUid(uid);
        if (dataPermissionServiceComponentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataPermissionServiceComponentDo.RESOURCE_SYMBOL,
                    uid));
        }

        //
        // Step 2, core-processing
        //
        return this.resourceReferenceManager.check(
                ResourceReferenceManager.ResourceCategoryEnum.DATA_PERMISSION_SERVICE_COMPONENT,
                dataPermissionServiceComponentDo.getUid(),
                dataPermissionServiceComponentDo.getName());
    }

    @Override
    public void deleteDataPermissionServiceComponent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        DataPermissionServiceComponentDo dataPermissionServiceComponentDo = this.dataPermissionServiceComponentRepository.findByUid(uid);
        if (dataPermissionServiceComponentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataPermissionServiceComponentDo.RESOURCE_SYMBOL,
                    uid));
        }

        dataPermissionServiceComponentDo.setDeleted(Boolean.TRUE);
        BaseDo.update(dataPermissionServiceComponentDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dataPermissionServiceComponentRepository.save(dataPermissionServiceComponentDo);
    }

    @Override
    public DataPermissionServiceComponentDto getDataPermissionServiceComponent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        DataPermissionServiceComponentDo dataPermissionServiceComponentDo = this.dataPermissionServiceComponentRepository.findByUid(uid);
        if (dataPermissionServiceComponentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataPermissionServiceComponentDo.RESOURCE_SYMBOL,
                    uid));
        }

        DataPermissionServiceComponentDto dataPermissionServiceComponentDto = new DataPermissionServiceComponentDto();
        BeanUtils.copyProperties(dataPermissionServiceComponentDo, dataPermissionServiceComponentDto);

        //
        // 构造 front-end component
        //
        switch (dataPermissionServiceComponentDo.getType()) {
            case BUILTIN: {
                if (!ObjectUtils.isEmpty(dataPermissionServiceComponentDo.getResourceName())) {
                    InputStream inputStream = getClass().getClassLoader().getResourceAsStream(dataPermissionServiceComponentDo.getResourceName());
                    if (inputStream == null) {
                        throw new AbcResourceIntegrityException("cannot find resource");
                    } else {
                        String content = AbcFileUtils.readContent(inputStream);
                        dataPermissionServiceComponentDto.setFrontEndComponent(content);
                    }
                }
            }
            break;
            case PLUGIN: {
                if (!ObjectUtils.isEmpty(dataPermissionServiceComponentDo.getFrontEndComponentFileId())
                        && dataPermissionServiceComponentDo.getDfsServiceAgentUidOfFrontEndComponentFileId() != null) {
                    File file = null;

                    try {
                        file = this.dfsServiceAgentService.downloadFile(
                                dataPermissionServiceComponentDo.getDfsServiceAgentUidOfFrontEndComponentFileId(),
                                dataPermissionServiceComponentDo.getFrontEndComponentFileId(),
                                operatingUserProfile);
                    } catch (Exception e) {
                        LOGGER.error("failed to load file from file id::{}, {}",
                                dataPermissionServiceComponentDo.getDfsServiceAgentUidOfFrontEndComponentFileId(),
                                dataPermissionServiceComponentDo.getFrontEndComponentFileId(),
                                e);
                        throw new AbcResourceIntegrityException("file integrity issue");
                    }
                    String content = AbcFileUtils.readContent(file, StandardCharsets.UTF_8);
                    dataPermissionServiceComponentDto.setFrontEndComponent(content);
                }
            }
            break;
            default:
                break;
        }

        return dataPermissionServiceComponentDto;
    }

    @Override
    public List<DataPermissionServiceComponentDto> listingQueryDataPermissionServiceComponents(
            Long uid,
            String name,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Specification<DataPermissionServiceComponentDo> specification = new Specification<DataPermissionServiceComponentDo>() {
            @Override
            public Predicate toPredicate(Root<DataPermissionServiceComponentDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (uid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("uid"), uid));
                }
                if (!ObjectUtils.isEmpty(name)) {
                    predicateList.add(criteriaBuilder.like(root.get("name"), "%" + name + "%"));
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        if (sort == null || sort.isUnsorted()) {
            sort = Sort.by(Sort.Order.asc("sequence"));
        }

        List<DataPermissionServiceComponentDo> itemDoList = this.dataPermissionServiceComponentRepository.findAll(specification, sort);
        if (CollectionUtils.isEmpty(itemDoList)) {
            return null;
        }
        List<DataPermissionServiceComponentDto> content = new LinkedList<>();
        itemDoList.forEach(itemDo -> {
            DataPermissionServiceComponentDto itemDto = new DataPermissionServiceComponentDto();
            BeanUtils.copyProperties(itemDo, itemDto);
            content.add(itemDto);
        });
        return content;
    }

    @Override
    public Page<DataPermissionServiceComponentDto> pagingQueryDataPermissionServiceComponents(
            Long uid,
            String name,
            String description,
            List<ServiceComponentTypeEnum> typeList,
            Boolean enabled,
            List<Long> userUidListOfLastModifiedBy,
            List<String> lastModifiedTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // step 1, pre-processing
        //

        //
        // step 2, core-processing
        //
        Specification<DataPermissionServiceComponentDo> specification = new Specification<DataPermissionServiceComponentDo>() {
            @Override
            public Predicate toPredicate(Root<DataPermissionServiceComponentDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (uid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("uid"), uid));
                }
                if (!ObjectUtils.isEmpty(name)) {
                    predicateList.add(criteriaBuilder.like(root.get("name"), "%" + name + "%"));
                }
                if (!ObjectUtils.isEmpty(description)) {
                    predicateList.add(criteriaBuilder.like(root.get("description"), "%" + description + "%"));
                }
                if (!CollectionUtils.isEmpty(typeList)) {
                    CriteriaBuilder.In<ServiceComponentTypeEnum> in =
                            criteriaBuilder.in(root.get("type"));
                    typeList.forEach(type -> {
                        in.value(type);
                    });
                    predicateList.add(in);
                }
                if (enabled != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("enabled"), enabled));
                }
                if (!CollectionUtils.isEmpty(userUidListOfLastModifiedBy)) {
                    CriteriaBuilder.In<Long> in = criteriaBuilder.in(root.get("lastModifiedBy"));
                    userUidListOfLastModifiedBy.forEach(item -> {
                        in.value(item);
                    });
                    predicateList.add(in);
                }
                if (!CollectionUtils.isEmpty(lastModifiedTimestampAsStringList)) {
                    if (lastModifiedTimestampAsStringList.size() == 2) {
                        LocalDateTime dateTime0 = LocalDateTime.parse(lastModifiedTimestampAsStringList.get(0),
                                dateTimeFormatter);
                        LocalDateTime dateTime1 = LocalDateTime.parse(lastModifiedTimestampAsStringList.get(1), dateTimeFormatter);
                        if (dateTime0.isAfter(dateTime1)) {
                            predicateList.add(criteriaBuilder.between(root.get(BaseDo.LAST_MODIFIED_TIMESTAMP_FIELD_NAME),
                                    dateTime1, dateTime0));
                        } else {
                            predicateList.add(criteriaBuilder.between(root.get(BaseDo.LAST_MODIFIED_TIMESTAMP_FIELD_NAME),
                                    dateTime0, dateTime1));
                        }
                    } else if (lastModifiedTimestampAsStringList.size() == 1) {
                        LocalDateTime dateTime0 = LocalDateTime.parse(lastModifiedTimestampAsStringList.get(0), dateTimeFormatter);
                        predicateList.add(criteriaBuilder.equal(root.get(BaseDo.LAST_MODIFIED_TIMESTAMP_FIELD_NAME),
                                dateTime0));
                    } else {
                        CriteriaBuilder.In<LocalDateTime> in =
                                criteriaBuilder.in(root.get(BaseDo.LAST_MODIFIED_TIMESTAMP_FIELD_NAME));
                        lastModifiedTimestampAsStringList.forEach(createdTimestampAsString -> {
                            LocalDateTime dateTime0 = LocalDateTime.parse(createdTimestampAsString, dateTimeFormatter);
                            in.value(dateTime0);
                        });
                        predicateList.add(in);
                    }
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        if (pageable == null) {
            pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.asc(
                    "sequence")));
        } else if (pageable.getSort() == null || pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Order.asc(
                    "sequence")));
        }

        Page<DataPermissionServiceComponentDo> itemDoPage = this.dataPermissionServiceComponentRepository.findAll(specification, pageable);

        //
        // step 3, post-processing
        //

        //
        // step 3.1, 为 created by, last modified by 补充 user brief information
        //
        List<Long> userUidList = new LinkedList<>();
        itemDoPage.forEach(itemDo -> {
            if (itemDo.getCreatedBy() != null && !userUidList.contains(itemDo.getCreatedBy())) {
                userUidList.add(itemDo.getCreatedBy());
            }
            if (itemDo.getLastModifiedBy() != null && !userUidList.contains(itemDo.getLastModifiedBy())) {
                userUidList.add(itemDo.getLastModifiedBy());
            }
        });

        Map<Long, UserBriefInformation> userBriefInformationMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(userUidList)) {
            List<UserBriefInformation> userBriefInformationList =
                    this.userService.listingUserBriefInformation(userUidList, operatingUserProfile);
            if (!CollectionUtils.isEmpty(userBriefInformationList)) {
                userBriefInformationList.forEach(userBriefInformation -> {
                    userBriefInformationMap.put(userBriefInformation.getUid(), userBriefInformation);
                });
            }
        }

        //
        // step 3.2, 构造返回内容
        //
        List<DataPermissionServiceComponentDto> content = new ArrayList<>(itemDoPage.getSize());
        itemDoPage.forEach(itemDo -> {
            DataPermissionServiceComponentDto itemDto = new DataPermissionServiceComponentDto();
            BeanUtils.copyProperties(itemDo, itemDto);

            // 为 created by, last modified by 补充 user brief information
            itemDto.setCreatedByUser(userBriefInformationMap.get(itemDo.getCreatedBy()));
            itemDto.setLastModifiedByUser(userBriefInformationMap.get(itemDo.getLastModifiedBy()));

            content.add(itemDto);
        });
        Page<DataPermissionServiceComponentDto> itemDtoPage = new PageImpl<>(content, pageable, itemDoPage.getTotalElements());
        return itemDtoPage;
    }
}
