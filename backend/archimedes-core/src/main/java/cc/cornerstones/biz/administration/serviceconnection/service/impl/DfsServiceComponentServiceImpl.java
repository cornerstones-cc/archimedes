package cc.cornerstones.biz.administration.serviceconnection.service.impl;

import cc.cornerstones.almond.exceptions.*;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserBriefInformation;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcFileUtils;
import cc.cornerstones.arbutus.pf4j.service.assembly.PluginHelper;
import cc.cornerstones.arbutus.pf4j.share.types.PluginProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.archimedes.extensions.DfsServiceProvider;
import cc.cornerstones.biz.administration.serviceconnection.dto.*;
import cc.cornerstones.biz.administration.serviceconnection.entity.DataPermissionServiceComponentDo;
import cc.cornerstones.biz.administration.serviceconnection.entity.DfsServiceComponentDo;
import cc.cornerstones.biz.administration.serviceconnection.persistence.DfsServiceComponentRepository;
import cc.cornerstones.biz.administration.serviceconnection.service.assembly.DfsServiceHandler;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DfsServiceComponentService;
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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DfsServiceComponentServiceImpl implements DfsServiceComponentService, ApplicationListener<ApplicationReadyEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DfsServiceComponentServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private DfsServiceComponentRepository dfsServiceComponentRepository;

    @Autowired
    private ApplicationContext applicationContext;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    private PluginHelper pluginHelper;

    @Autowired
    private DfsServiceHandler dfsServiceHandler;

    @Autowired
    private UserService userService;

    @Autowired
    private ResourceReferenceManager resourceReferenceManager;


    /**
     * Handle an application event.
     *
     * @param event the event to respond to
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {

    }

    @Override
    public DfsServiceComponentDto createPluginDfsServiceComponent(
            CreatePluginDfsServiceComponentDto createPluginDfsServiceComponentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        boolean existsDuplicate =
                this.dfsServiceComponentRepository.existsByName(createPluginDfsServiceComponentDto.getName());
        if (existsDuplicate) {
            throw new AbcResourceDuplicateException(String.format("%s::name=%s", DfsServiceComponentDo.RESOURCE_SYMBOL,
                    createPluginDfsServiceComponentDto.getName()));
        }

        //
        // Step 2, core-processing
        //
        DfsServiceComponentDo dfsServiceComponentDo = new DfsServiceComponentDo();
        dfsServiceComponentDo.setUid(this.idHelper.getNextDistributedId(DfsServiceComponentDo.RESOURCE_NAME));
        dfsServiceComponentDo.setName(createPluginDfsServiceComponentDto.getName());
        dfsServiceComponentDo.setObjectName(
                createPluginDfsServiceComponentDto.getName()
                        .replaceAll("_", "__")
                        .replaceAll("\\s", "_")
                        .toLowerCase());
        dfsServiceComponentDo.setDescription(createPluginDfsServiceComponentDto.getDescription());
        dfsServiceComponentDo.setSequence(createPluginDfsServiceComponentDto.getSequence());
        dfsServiceComponentDo.setType(ServiceComponentTypeEnum.PLUGIN);
        dfsServiceComponentDo.setFrontEndComponentFileId(
                createPluginDfsServiceComponentDto.getFrontEndComponentFileId());
        dfsServiceComponentDo.setDfsServiceAgentUidOfFrontEndComponentFileId(
                createPluginDfsServiceComponentDto.getDfsServiceAgentUidOfFrontEndComponentFileId());
        dfsServiceComponentDo.setBackEndComponentFileId(
                createPluginDfsServiceComponentDto.getBackEndComponentFileId());
        dfsServiceComponentDo.setDfsServiceAgentUidOfBackEndComponentFileId(
                createPluginDfsServiceComponentDto.getDfsServiceAgentUidOfBackEndComponentFileId());

        // back end component metadata & configuration template
        File file = this.dfsServiceHandler.downloadFile(
                createPluginDfsServiceComponentDto.getDfsServiceAgentUidOfBackEndComponentFileId(),
                createPluginDfsServiceComponentDto.getBackEndComponentFileId(),
                operatingUserProfile);
        BackEndComponentParsedResultDto backEndComponentParsedResultDto =
                parseBackEndComponent(file, operatingUserProfile);
        dfsServiceComponentDo.setBackEndComponentMetadata(backEndComponentParsedResultDto.getMetadata());
        dfsServiceComponentDo.setConfigurationTemplate(backEndComponentParsedResultDto.getConfigurationTemplate());

        BaseDo.create(dfsServiceComponentDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dfsServiceComponentRepository.save(dfsServiceComponentDo);

        //
        // Step 3, post-processing
        //

        DfsServiceComponentDto dfsServiceComponentDto = new DfsServiceComponentDto();
        BeanUtils.copyProperties(dfsServiceComponentDto, dfsServiceComponentDto);
        return dfsServiceComponentDto;
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
            List<DfsServiceProvider> listOfProcessors =
                    this.pluginHelper.getPluginManager().getExtensions(
                            DfsServiceProvider.class,
                            pluginId);
            if (listOfProcessors == null || listOfProcessors.isEmpty()) {
                throw new AbcUndefinedException("cannot find DfsServiceProvider");
            }

            if (listOfProcessors.size() > 1) {
                throw new AbcUndefinedException("found " + listOfProcessors.size() + " DfsServiceProvider");
            }

            DfsServiceProvider processor = listOfProcessors.get(0);

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
    public void updateDfsServiceComponent(
            Long uid,
            UpdateDfsServiceComponentDto updateDfsServiceComponentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DfsServiceComponentDo dfsServiceComponentDo = this.dfsServiceComponentRepository.findByUid(uid);
        if (dfsServiceComponentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DfsServiceComponentDo.RESOURCE_SYMBOL,
                    uid));
        }

        //
        // Step 2, core-processing
        //
        switch (dfsServiceComponentDo.getType()) {
            case BUILTIN: {
                updateBuiltinDfsServiceComponent(dfsServiceComponentDo, updateDfsServiceComponentDto,
                        operatingUserProfile);
            }
            break;
            case PLUGIN: {
                updatePluginDfsServiceComponent(dfsServiceComponentDo, updateDfsServiceComponentDto,
                        operatingUserProfile);
            }
            break;
            default:
                throw new AbcResourceConflictException(String.format("unknown service component type:%s",
                        dfsServiceComponentDo.getType()));
        }

        //
        // Step 3, post-processing
        //
    }

    private void updateBuiltinDfsServiceComponent(
            DfsServiceComponentDo dfsServiceComponentDo,
            UpdateDfsServiceComponentDto updateDfsServiceComponentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        boolean requiredToUpdate = false;

        if (updateDfsServiceComponentDto.getDescription() != null
                && !updateDfsServiceComponentDto.getDescription().equalsIgnoreCase(dfsServiceComponentDo.getDescription())) {
            dfsServiceComponentDo.setDescription(updateDfsServiceComponentDto.getDescription());
            requiredToUpdate = true;
        }
        if (updateDfsServiceComponentDto.getSequence() != null
                && !updateDfsServiceComponentDto.getSequence().equals(dfsServiceComponentDo.getSequence())) {
            dfsServiceComponentDo.setSequence(updateDfsServiceComponentDto.getSequence());
            requiredToUpdate = true;
        }

        if (requiredToUpdate) {
            BaseDo.update(dfsServiceComponentDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.dfsServiceComponentRepository.save(dfsServiceComponentDo);
        }
    }

    private void updatePluginDfsServiceComponent(
            DfsServiceComponentDo dfsServiceComponentDo,
            UpdateDfsServiceComponentDto updateDfsServiceComponentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        if (!ObjectUtils.isEmpty(updateDfsServiceComponentDto.getName())
                && !updateDfsServiceComponentDto.getName().equalsIgnoreCase(dfsServiceComponentDo.getName())) {
            boolean existsDuplicate =
                    this.dfsServiceComponentRepository.existsByName(updateDfsServiceComponentDto.getName());
            if (existsDuplicate) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s", DfsServiceComponentDo.RESOURCE_SYMBOL,
                        updateDfsServiceComponentDto.getName()));
            }
        }

        //
        // Step 2, core-processing
        //
        boolean requiredToUpdate = false;

        if (!ObjectUtils.isEmpty(updateDfsServiceComponentDto.getName())
                && !updateDfsServiceComponentDto.getName().equalsIgnoreCase(dfsServiceComponentDo.getName())) {
            dfsServiceComponentDo.setName(updateDfsServiceComponentDto.getName());
            dfsServiceComponentDo.setObjectName(
                    updateDfsServiceComponentDto.getName()
                            .replaceAll("_", "__")
                            .replaceAll("\\s", "_")
                            .toLowerCase());
            requiredToUpdate = true;
        }
        if (updateDfsServiceComponentDto.getDescription() != null
                && !updateDfsServiceComponentDto.getDescription().equalsIgnoreCase(dfsServiceComponentDo.getDescription())) {
            dfsServiceComponentDo.setDescription(updateDfsServiceComponentDto.getDescription());
            requiredToUpdate = true;
        }
        if (updateDfsServiceComponentDto.getSequence() != null
                && !updateDfsServiceComponentDto.getSequence().equals(dfsServiceComponentDo.getSequence())) {
            dfsServiceComponentDo.setSequence(updateDfsServiceComponentDto.getSequence());
            requiredToUpdate = true;
        }
        if (!ObjectUtils.isEmpty(updateDfsServiceComponentDto.getFrontEndComponentFileId())
                && !updateDfsServiceComponentDto.getFrontEndComponentFileId().equalsIgnoreCase(dfsServiceComponentDo.getFrontEndComponentFileId())) {
            dfsServiceComponentDo.setFrontEndComponentFileId(updateDfsServiceComponentDto.getFrontEndComponentFileId());
            requiredToUpdate = true;
        }
        if (updateDfsServiceComponentDto.getDfsServiceAgentUidOfFrontEndComponentFileId() != null
                && !updateDfsServiceComponentDto.getDfsServiceAgentUidOfFrontEndComponentFileId().equals(dfsServiceComponentDo.getDfsServiceAgentUidOfFrontEndComponentFileId())) {
            dfsServiceComponentDo.setDfsServiceAgentUidOfFrontEndComponentFileId(updateDfsServiceComponentDto.getDfsServiceAgentUidOfFrontEndComponentFileId());
            requiredToUpdate = true;
        }
        if (!ObjectUtils.isEmpty(updateDfsServiceComponentDto.getBackEndComponentFileId())
                && !updateDfsServiceComponentDto.getBackEndComponentFileId().equalsIgnoreCase(dfsServiceComponentDo.getBackEndComponentFileId())) {
            dfsServiceComponentDo.setBackEndComponentFileId(updateDfsServiceComponentDto.getBackEndComponentFileId());

            // back end component metadata & configuration template
            File file = this.dfsServiceHandler.downloadFile(
                    updateDfsServiceComponentDto.getDfsServiceAgentUidOfBackEndComponentFileId(),
                    updateDfsServiceComponentDto.getBackEndComponentFileId(),
                    operatingUserProfile);
            BackEndComponentParsedResultDto backEndComponentParsedResultDto =
                    parseBackEndComponent(file, operatingUserProfile);
            dfsServiceComponentDo.setBackEndComponentMetadata(backEndComponentParsedResultDto.getMetadata());
            dfsServiceComponentDo.setConfigurationTemplate(backEndComponentParsedResultDto.getConfigurationTemplate());

            requiredToUpdate = true;
        }
        if (updateDfsServiceComponentDto.getDfsServiceAgentUidOfBackEndComponentFileId() != null
                && !updateDfsServiceComponentDto.getDfsServiceAgentUidOfBackEndComponentFileId().equals(dfsServiceComponentDo.getDfsServiceAgentUidOfBackEndComponentFileId())) {
            dfsServiceComponentDo.setDfsServiceAgentUidOfBackEndComponentFileId(updateDfsServiceComponentDto.getDfsServiceAgentUidOfBackEndComponentFileId());
            requiredToUpdate = true;
        }

        if (requiredToUpdate) {
            BaseDo.update(dfsServiceComponentDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.dfsServiceComponentRepository.save(dfsServiceComponentDo);
        }
    }

    @Override
    public List<String> listAllReferencesToDfsServiceComponent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DfsServiceComponentDo dfsServiceComponentDo = this.dfsServiceComponentRepository.findByUid(uid);
        if (dfsServiceComponentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DfsServiceComponentDo.RESOURCE_SYMBOL,
                    uid));
        }

        //
        // Step 2, core-processing
        //
        return this.resourceReferenceManager.check(
                ResourceReferenceManager.ResourceCategoryEnum.DFS_SERVICE_COMPONENT,
                dfsServiceComponentDo.getUid(),
                dfsServiceComponentDo.getName());
    }

    @Override
    public void deleteDfsServiceComponent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        DfsServiceComponentDo dfsServiceComponentDo = this.dfsServiceComponentRepository.findByUid(uid);
        if (dfsServiceComponentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DfsServiceComponentDo.RESOURCE_SYMBOL,
                    uid));
        }

        dfsServiceComponentDo.setDeleted(Boolean.TRUE);
        BaseDo.update(dfsServiceComponentDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dfsServiceComponentRepository.save(dfsServiceComponentDo);
    }

    @Override
    public DfsServiceComponentDto getDfsServiceComponent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        DfsServiceComponentDo dfsServiceComponentDo = this.dfsServiceComponentRepository.findByUid(uid);
        if (dfsServiceComponentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DfsServiceComponentDo.RESOURCE_SYMBOL,
                    uid));
        }

        DfsServiceComponentDto dfsServiceComponentDto = new DfsServiceComponentDto();
        BeanUtils.copyProperties(dfsServiceComponentDo, dfsServiceComponentDto);

        //
        // 构造 front-end component
        //
        switch (dfsServiceComponentDo.getType()) {
            case BUILTIN: {
                if (!ObjectUtils.isEmpty(dfsServiceComponentDo.getResourceName())) {
                    InputStream inputStream = getClass().getClassLoader().getResourceAsStream(dfsServiceComponentDo.getResourceName());
                    if (inputStream == null) {
                        throw new AbcResourceIntegrityException("cannot find resource");
                    } else {
                        String content = AbcFileUtils.readContent(inputStream);
                        dfsServiceComponentDto.setFrontEndComponent(content);
                    }
                }
            }
            break;
            case PLUGIN: {
                if (!ObjectUtils.isEmpty(dfsServiceComponentDo.getFrontEndComponentFileId())
                        && dfsServiceComponentDo.getDfsServiceAgentUidOfFrontEndComponentFileId() != null) {
                    File file = null;

                    try {
                        file = this.dfsServiceHandler.downloadFile(
                                dfsServiceComponentDo.getDfsServiceAgentUidOfFrontEndComponentFileId(),
                                dfsServiceComponentDo.getFrontEndComponentFileId(),
                                operatingUserProfile);
                    } catch (Exception e) {
                        LOGGER.error("failed to load file from file id::{}, {}",
                                dfsServiceComponentDo.getDfsServiceAgentUidOfFrontEndComponentFileId(),
                                dfsServiceComponentDo.getFrontEndComponentFileId(),
                                e);
                        throw new AbcResourceIntegrityException("file integrity issue");
                    }
                    String content = AbcFileUtils.readContent(file, StandardCharsets.UTF_8);
                    dfsServiceComponentDto.setFrontEndComponent(content);
                }
            }
            break;
            default:
                break;
        }

        return dfsServiceComponentDto;
    }

    @Override
    public List<DfsServiceComponentDto> listingQueryDfsServiceComponents(
            Long uid,
            String name,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Specification<DfsServiceComponentDo> specification = new Specification<DfsServiceComponentDo>() {
            @Override
            public Predicate toPredicate(Root<DfsServiceComponentDo> root, CriteriaQuery<?> query,
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

        List<DfsServiceComponentDo> itemDoList = this.dfsServiceComponentRepository.findAll(specification, sort);
        if (CollectionUtils.isEmpty(itemDoList)) {
            return null;
        }
        List<DfsServiceComponentDto> content = new LinkedList<>();
        itemDoList.forEach(itemDo -> {
            DfsServiceComponentDto itemDto = new DfsServiceComponentDto();
            BeanUtils.copyProperties(itemDo, itemDto);
            content.add(itemDto);
        });
        return content;
    }

    @Override
    public Page<DfsServiceComponentDto> pagingQueryDfsServiceComponents(
            Long uid,
            String name,
            String description,
            List<ServiceComponentTypeEnum> typeList,
            Boolean enabled,
            Boolean preferred,
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
        Specification<DfsServiceComponentDo> specification = new Specification<DfsServiceComponentDo>() {
            @Override
            public Predicate toPredicate(Root<DfsServiceComponentDo> root, CriteriaQuery<?> query,
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
                if (preferred != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("preferred"), preferred));
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

        Page<DfsServiceComponentDo> itemDoPage = this.dfsServiceComponentRepository.findAll(specification, pageable);

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
        List<DfsServiceComponentDto> content = new ArrayList<>(itemDoPage.getSize());
        itemDoPage.forEach(itemDo -> {
            DfsServiceComponentDto itemDto = new DfsServiceComponentDto();
            BeanUtils.copyProperties(itemDo, itemDto);

            // 为 created by, last modified by 补充 user brief information
            itemDto.setCreatedByUser(userBriefInformationMap.get(itemDo.getCreatedBy()));
            itemDto.setLastModifiedByUser(userBriefInformationMap.get(itemDo.getLastModifiedBy()));

            content.add(itemDto);
        });
        Page<DfsServiceComponentDto> itemDtoPage = new PageImpl<>(content, pageable, itemDoPage.getTotalElements());
        return itemDtoPage;
    }
}
