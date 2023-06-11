package cc.cornerstones.biz.administration.serviceconnection.service.impl;

import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.exceptions.*;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserBriefInformation;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcFileUtils;
import cc.cornerstones.arbutus.pf4j.service.assembly.PluginHelper;
import cc.cornerstones.arbutus.pf4j.share.types.PluginProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.archimedes.extensions.UserSynchronizationServiceProvider;
import cc.cornerstones.biz.administration.serviceconnection.dto.*;
import cc.cornerstones.biz.administration.serviceconnection.entity.DfsServiceComponentDo;
import cc.cornerstones.biz.administration.serviceconnection.entity.UserSynchronizationServiceComponentDo;
import cc.cornerstones.biz.administration.serviceconnection.persistence.UserSynchronizationServiceComponentRepository;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DfsServiceAgentService;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.UserSynchronizationServiceComponentService;
import cc.cornerstones.biz.administration.serviceconnection.share.constants.ServiceComponentTypeEnum;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.distributedjob.service.inf.DistributedJobService;
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
public class UserSynchronizationServiceComponentServiceImpl implements UserSynchronizationServiceComponentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserSynchronizationServiceComponentServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private UserSynchronizationServiceComponentRepository userSynchronizationServiceComponentRepository;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    private PluginHelper pluginHelper;

    @Autowired
    private DfsServiceAgentService dfsServiceAgentService;

    @Autowired
    private DistributedJobService distributedJobService;

    @Autowired
    private UserService userService;

    @Autowired
    private ResourceReferenceManager resourceReferenceManager;


    @Override
    public UserSynchronizationServiceComponentDto createPluginUserSynchronizationServiceComponent(
            CreatePluginUserSynchronizationServiceComponentDto createPluginUserSynchronizationServiceComponentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        boolean existsDuplicate =
                this.userSynchronizationServiceComponentRepository.existsByName(createPluginUserSynchronizationServiceComponentDto.getName());
        if (existsDuplicate) {
            throw new AbcResourceDuplicateException(String.format("%s::name=%s", UserSynchronizationServiceComponentDo.RESOURCE_SYMBOL,
                    createPluginUserSynchronizationServiceComponentDto.getName()));
        }

        //
        // Step 2, core-processing
        //
        UserSynchronizationServiceComponentDo userSynchronizationServiceComponentDo = new UserSynchronizationServiceComponentDo();
        userSynchronizationServiceComponentDo.setUid(this.idHelper.getNextDistributedId(UserSynchronizationServiceComponentDo.RESOURCE_NAME));
        userSynchronizationServiceComponentDo.setName(createPluginUserSynchronizationServiceComponentDto.getName());
        userSynchronizationServiceComponentDo.setObjectName(
                createPluginUserSynchronizationServiceComponentDto.getName()
                        .replaceAll("_", "__")
                        .replaceAll("\\s", "_")
                        .toLowerCase());
        userSynchronizationServiceComponentDo.setDescription(createPluginUserSynchronizationServiceComponentDto.getDescription());
        userSynchronizationServiceComponentDo.setSequence(createPluginUserSynchronizationServiceComponentDto.getSequence());
        userSynchronizationServiceComponentDo.setType(ServiceComponentTypeEnum.PLUGIN);
        userSynchronizationServiceComponentDo.setFrontEndComponentFileId(
                createPluginUserSynchronizationServiceComponentDto.getFrontEndComponentFileId());
        userSynchronizationServiceComponentDo.setDfsServiceAgentUidOfFrontEndComponentFileId(
                createPluginUserSynchronizationServiceComponentDto.getDfsServiceAgentUidOfFrontEndComponentFileId());
        userSynchronizationServiceComponentDo.setBackEndComponentFileId(
                createPluginUserSynchronizationServiceComponentDto.getBackEndComponentFileId());
        userSynchronizationServiceComponentDo.setDfsServiceAgentUidOfBackEndComponentFileId(
                createPluginUserSynchronizationServiceComponentDto.getDfsServiceAgentUidOfBackEndComponentFileId());

        // back end component metadata & configuration template
        File file = this.dfsServiceAgentService.downloadFile(
                createPluginUserSynchronizationServiceComponentDto.getDfsServiceAgentUidOfBackEndComponentFileId(),
                createPluginUserSynchronizationServiceComponentDto.getBackEndComponentFileId(),
                operatingUserProfile);
        BackEndComponentParsedResultDto backEndComponentParsedResultDto =
                parseBackEndComponent(file, operatingUserProfile);
        userSynchronizationServiceComponentDo.setBackEndComponentMetadata(backEndComponentParsedResultDto.getMetadata());
        userSynchronizationServiceComponentDo.setConfigurationTemplate(backEndComponentParsedResultDto.getConfigurationTemplate());

        BaseDo.create(userSynchronizationServiceComponentDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.userSynchronizationServiceComponentRepository.save(userSynchronizationServiceComponentDo);

        //
        // Step 3, post-processing
        //
        UserSynchronizationServiceComponentDto userSynchronizationServiceComponentDto = new UserSynchronizationServiceComponentDto();
        BeanUtils.copyProperties(userSynchronizationServiceComponentDto, userSynchronizationServiceComponentDto);
        return userSynchronizationServiceComponentDto;
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
            List<UserSynchronizationServiceProvider> listOfProcessors =
                    this.pluginHelper.getPluginManager().getExtensions(
                            UserSynchronizationServiceProvider.class,
                            pluginId);
            if (listOfProcessors == null || listOfProcessors.isEmpty()) {
                throw new AbcUndefinedException("cannot find UserSynchronizationServiceProvider");
            }

            if (listOfProcessors.size() > 1) {
                throw new AbcUndefinedException("found " + listOfProcessors.size() + " UserSynchronizationServiceProvider");
            }

            UserSynchronizationServiceProvider processor = listOfProcessors.get(0);

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
    public void updateUserSynchronizationServiceComponent(
            Long uid,
            UpdateUserSynchronizationServiceComponentDto updateUserSynchronizationServiceComponentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        UserSynchronizationServiceComponentDo userSynchronizationServiceComponentDo = this.userSynchronizationServiceComponentRepository.findByUid(uid);
        if (userSynchronizationServiceComponentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", UserSynchronizationServiceComponentDo.RESOURCE_SYMBOL,
                    uid));
        }

        //
        // Step 2, core-processing
        //
        switch (userSynchronizationServiceComponentDo.getType()) {
            case BUILTIN: {
                updateBuiltinUserSynchronizationServiceComponent(userSynchronizationServiceComponentDo, updateUserSynchronizationServiceComponentDto,
                        operatingUserProfile);
            }
            break;
            case PLUGIN: {
                updatePluginUserSynchronizationServiceComponent(userSynchronizationServiceComponentDo, updateUserSynchronizationServiceComponentDto,
                        operatingUserProfile);
            }
            break;
            default:
                throw new AbcResourceConflictException(String.format("unknown service component type:%s",
                        userSynchronizationServiceComponentDo.getType()));
        }

        //
        // Step 3, post-processing
        //
    }

    private void updateBuiltinUserSynchronizationServiceComponent(
            UserSynchronizationServiceComponentDo userSynchronizationServiceComponentDo,
            UpdateUserSynchronizationServiceComponentDto updateUserSynchronizationServiceComponentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        boolean requiredToUpdate = false;

        if (updateUserSynchronizationServiceComponentDto.getDescription() != null
                && !updateUserSynchronizationServiceComponentDto.getDescription().equalsIgnoreCase(userSynchronizationServiceComponentDo.getDescription())) {
            userSynchronizationServiceComponentDo.setDescription(updateUserSynchronizationServiceComponentDto.getDescription());
            requiredToUpdate = true;
        }
        if (updateUserSynchronizationServiceComponentDto.getSequence() != null
                && !updateUserSynchronizationServiceComponentDto.getSequence().equals(userSynchronizationServiceComponentDo.getSequence())) {
            userSynchronizationServiceComponentDo.setSequence(updateUserSynchronizationServiceComponentDto.getSequence());
            requiredToUpdate = true;
        }

        if (requiredToUpdate) {
            BaseDo.update(userSynchronizationServiceComponentDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.userSynchronizationServiceComponentRepository.save(userSynchronizationServiceComponentDo);
        }
    }

    private void updatePluginUserSynchronizationServiceComponent(
            UserSynchronizationServiceComponentDo userSynchronizationServiceComponentDo,
            UpdateUserSynchronizationServiceComponentDto updateUserSynchronizationServiceComponentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        if (!ObjectUtils.isEmpty(updateUserSynchronizationServiceComponentDto.getName())
                && !updateUserSynchronizationServiceComponentDto.getName().equalsIgnoreCase(userSynchronizationServiceComponentDo.getName())) {
            boolean existsDuplicate =
                    this.userSynchronizationServiceComponentRepository.existsByName(updateUserSynchronizationServiceComponentDto.getName());
            if (existsDuplicate) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s", UserSynchronizationServiceComponentDo.RESOURCE_SYMBOL,
                        updateUserSynchronizationServiceComponentDto.getName()));
            }
        }

        //
        // Step 2, core-processing
        //
        boolean requiredToUpdate = false;
        if (!ObjectUtils.isEmpty(updateUserSynchronizationServiceComponentDto.getName())
                && !updateUserSynchronizationServiceComponentDto.getName().equalsIgnoreCase(userSynchronizationServiceComponentDo.getName())) {
            userSynchronizationServiceComponentDo.setName(updateUserSynchronizationServiceComponentDto.getName());
            userSynchronizationServiceComponentDo.setObjectName(
                    updateUserSynchronizationServiceComponentDto.getName()
                            .replaceAll("_", "__")
                            .replaceAll("\\s", "_")
                            .toLowerCase());
            requiredToUpdate = true;
        }
        if (updateUserSynchronizationServiceComponentDto.getDescription() != null
                && !updateUserSynchronizationServiceComponentDto.getDescription().equalsIgnoreCase(userSynchronizationServiceComponentDo.getDescription())) {
            userSynchronizationServiceComponentDo.setDescription(updateUserSynchronizationServiceComponentDto.getDescription());
            requiredToUpdate = true;
        }
        if (updateUserSynchronizationServiceComponentDto.getSequence() != null
                && !updateUserSynchronizationServiceComponentDto.getSequence().equals(userSynchronizationServiceComponentDo.getSequence())) {
            userSynchronizationServiceComponentDo.setSequence(updateUserSynchronizationServiceComponentDto.getSequence());
            requiredToUpdate = true;
        }
        if (!ObjectUtils.isEmpty(updateUserSynchronizationServiceComponentDto.getFrontEndComponentFileId())
                && !updateUserSynchronizationServiceComponentDto.getFrontEndComponentFileId().equalsIgnoreCase(userSynchronizationServiceComponentDo.getFrontEndComponentFileId())) {
            userSynchronizationServiceComponentDo.setFrontEndComponentFileId(updateUserSynchronizationServiceComponentDto.getFrontEndComponentFileId());
            requiredToUpdate = true;
        }
        if (updateUserSynchronizationServiceComponentDto.getDfsServiceAgentUidOfFrontEndComponentFileId() != null
                && !updateUserSynchronizationServiceComponentDto.getDfsServiceAgentUidOfFrontEndComponentFileId().equals(userSynchronizationServiceComponentDo.getDfsServiceAgentUidOfFrontEndComponentFileId())) {
            userSynchronizationServiceComponentDo.setDfsServiceAgentUidOfFrontEndComponentFileId(updateUserSynchronizationServiceComponentDto.getDfsServiceAgentUidOfFrontEndComponentFileId());
            requiredToUpdate = true;
        }
        if (!ObjectUtils.isEmpty(updateUserSynchronizationServiceComponentDto.getBackEndComponentFileId())
                && !updateUserSynchronizationServiceComponentDto.getBackEndComponentFileId().equalsIgnoreCase(userSynchronizationServiceComponentDo.getBackEndComponentFileId())) {
            userSynchronizationServiceComponentDo.setBackEndComponentFileId(updateUserSynchronizationServiceComponentDto.getBackEndComponentFileId());

            // back end component metadata & configuration template
            File file = this.dfsServiceAgentService.downloadFile(
                    updateUserSynchronizationServiceComponentDto.getDfsServiceAgentUidOfBackEndComponentFileId(),
                    updateUserSynchronizationServiceComponentDto.getBackEndComponentFileId(),
                    operatingUserProfile);
            BackEndComponentParsedResultDto backEndComponentParsedResultDto =
                    parseBackEndComponent(file, operatingUserProfile);
            userSynchronizationServiceComponentDo.setBackEndComponentMetadata(backEndComponentParsedResultDto.getMetadata());
            userSynchronizationServiceComponentDo.setConfigurationTemplate(backEndComponentParsedResultDto.getConfigurationTemplate());

            requiredToUpdate = true;
        }
        if (updateUserSynchronizationServiceComponentDto.getDfsServiceAgentUidOfBackEndComponentFileId() != null
                && !updateUserSynchronizationServiceComponentDto.getDfsServiceAgentUidOfBackEndComponentFileId().equals(userSynchronizationServiceComponentDo.getDfsServiceAgentUidOfBackEndComponentFileId())) {
            userSynchronizationServiceComponentDo.setDfsServiceAgentUidOfBackEndComponentFileId(updateUserSynchronizationServiceComponentDto.getDfsServiceAgentUidOfBackEndComponentFileId());
            requiredToUpdate = true;
        }

        if (requiredToUpdate) {
            BaseDo.update(userSynchronizationServiceComponentDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.userSynchronizationServiceComponentRepository.save(userSynchronizationServiceComponentDo);
        }
    }

    @Override
    public List<String> listAllReferencesToUserSynchronizationServiceComponent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        UserSynchronizationServiceComponentDo userSynchronizationServiceComponentDo = this.userSynchronizationServiceComponentRepository.findByUid(uid);
        if (userSynchronizationServiceComponentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", UserSynchronizationServiceComponentDo.RESOURCE_SYMBOL,
                    uid));
        }

        //
        // Step 2, core-processing
        //
        return this.resourceReferenceManager.check(
                ResourceReferenceManager.ResourceCategoryEnum.USER_SYNCHRONIZATION_SERVICE_COMPONENT,
                userSynchronizationServiceComponentDo.getUid(),
                userSynchronizationServiceComponentDo.getName());
    }

    @Override
    public void deleteUserSynchronizationServiceComponent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        UserSynchronizationServiceComponentDo userSynchronizationServiceComponentDo = this.userSynchronizationServiceComponentRepository.findByUid(uid);
        if (userSynchronizationServiceComponentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", UserSynchronizationServiceComponentDo.RESOURCE_SYMBOL,
                    uid));
        }

        userSynchronizationServiceComponentDo.setDeleted(Boolean.TRUE);
        BaseDo.update(userSynchronizationServiceComponentDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.userSynchronizationServiceComponentRepository.save(userSynchronizationServiceComponentDo);
    }

    @Override
    public UserSynchronizationServiceComponentDto getUserSynchronizationServiceComponent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        UserSynchronizationServiceComponentDo userSynchronizationServiceComponentDo = this.userSynchronizationServiceComponentRepository.findByUid(uid);
        if (userSynchronizationServiceComponentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", UserSynchronizationServiceComponentDo.RESOURCE_SYMBOL,
                    uid));
        }

        UserSynchronizationServiceComponentDto userSynchronizationServiceComponentDto = new UserSynchronizationServiceComponentDto();
        BeanUtils.copyProperties(userSynchronizationServiceComponentDo, userSynchronizationServiceComponentDto);

        //
        // 构造 front-end component
        //
        switch (userSynchronizationServiceComponentDo.getType()) {
            case BUILTIN: {
                if (!ObjectUtils.isEmpty(userSynchronizationServiceComponentDo.getResourceName())) {
                    InputStream inputStream = getClass().getClassLoader().getResourceAsStream(userSynchronizationServiceComponentDo.getResourceName());
                    if (inputStream == null) {
                        throw new AbcResourceIntegrityException("cannot find resource");
                    } else {
                        String content = AbcFileUtils.readContent(inputStream);
                        userSynchronizationServiceComponentDto.setFrontEndComponent(content);
                    }
                }
            }
            break;
            case PLUGIN: {
                if (!ObjectUtils.isEmpty(userSynchronizationServiceComponentDo.getFrontEndComponentFileId())
                        && userSynchronizationServiceComponentDo.getDfsServiceAgentUidOfFrontEndComponentFileId() != null) {
                    File file = null;

                    try {
                        file = this.dfsServiceAgentService.downloadFile(
                                userSynchronizationServiceComponentDo.getDfsServiceAgentUidOfFrontEndComponentFileId(),
                                userSynchronizationServiceComponentDo.getFrontEndComponentFileId(),
                                operatingUserProfile);
                    } catch (Exception e) {
                        LOGGER.error("failed to load file from file id::{}, {}",
                                userSynchronizationServiceComponentDo.getDfsServiceAgentUidOfFrontEndComponentFileId(),
                                userSynchronizationServiceComponentDo.getFrontEndComponentFileId(),
                                e);
                        throw new AbcResourceIntegrityException("file integrity issue");
                    }
                    String content = AbcFileUtils.readContent(file, StandardCharsets.UTF_8);
                    userSynchronizationServiceComponentDto.setFrontEndComponent(content);
                }
            }
            break;
            default:
                break;
        }

        return userSynchronizationServiceComponentDto;
    }

    @Override
    public List<UserSynchronizationServiceComponentDto> listingQueryUserSynchronizationServiceComponents(
            Long uid,
            String name,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Specification<UserSynchronizationServiceComponentDo> specification = new Specification<UserSynchronizationServiceComponentDo>() {
            @Override
            public Predicate toPredicate(Root<UserSynchronizationServiceComponentDo> root, CriteriaQuery<?> query,
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

        List<UserSynchronizationServiceComponentDo> itemDoList = this.userSynchronizationServiceComponentRepository.findAll(specification, sort);
        if (CollectionUtils.isEmpty(itemDoList)) {
            return null;
        }
        List<UserSynchronizationServiceComponentDto> content = new LinkedList<>();
        itemDoList.forEach(itemDo -> {
            UserSynchronizationServiceComponentDto itemDto = new UserSynchronizationServiceComponentDto();
            BeanUtils.copyProperties(itemDo, itemDto);
            content.add(itemDto);
        });
        return content;
    }

    @Override
    public Page<UserSynchronizationServiceComponentDto> pagingQueryUserSynchronizationServiceComponents(
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
        Specification<UserSynchronizationServiceComponentDo> specification = new Specification<UserSynchronizationServiceComponentDo>() {
            @Override
            public Predicate toPredicate(Root<UserSynchronizationServiceComponentDo> root, CriteriaQuery<?> query,
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

        Page<UserSynchronizationServiceComponentDo> itemDoPage = this.userSynchronizationServiceComponentRepository.findAll(specification, pageable);

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
        List<UserSynchronizationServiceComponentDto> content = new ArrayList<>(itemDoPage.getSize());
        itemDoPage.forEach(itemDo -> {
            UserSynchronizationServiceComponentDto itemDto = new UserSynchronizationServiceComponentDto();
            BeanUtils.copyProperties(itemDo, itemDto);

            // 为 created by, last modified by 补充 user brief information
            itemDto.setCreatedByUser(userBriefInformationMap.get(itemDo.getCreatedBy()));
            itemDto.setLastModifiedByUser(userBriefInformationMap.get(itemDo.getLastModifiedBy()));

            content.add(itemDto);
        });
        Page<UserSynchronizationServiceComponentDto> itemDtoPage = new PageImpl<>(content, pageable, itemDoPage.getTotalElements());
        return itemDtoPage;
    }

    @Override
    public Page<UserSynchronizationExecutionInstanceDto> pagingQueryUserSynchronizationExecutionInstances(
            Long userSynchronizationServiceAgentUid,
            Long uid,
            List<JobStatusEnum> jobStatusList,
            List<Long> userUidListOfLastModifiedBy,
            List<String> lastModifiedTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return null;
    }

    @Override
    public UserSynchronizationExecutionInstanceDto createUserSynchronizationExecutionInstance(
            Long userSynchronizationServiceAgentUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return null;
    }
}
