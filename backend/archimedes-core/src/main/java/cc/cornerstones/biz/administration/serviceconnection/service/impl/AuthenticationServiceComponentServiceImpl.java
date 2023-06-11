package cc.cornerstones.biz.administration.serviceconnection.service.impl;

import cc.cornerstones.almond.exceptions.*;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserBriefInformation;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcFileUtils;
import cc.cornerstones.arbutus.pf4j.service.assembly.PluginHelper;
import cc.cornerstones.arbutus.pf4j.share.types.PluginProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.archimedes.extensions.AuthenticationServiceProvider;
import cc.cornerstones.biz.administration.serviceconnection.dto.*;
import cc.cornerstones.biz.administration.serviceconnection.entity.AuthenticationServiceComponentDo;
import cc.cornerstones.biz.administration.serviceconnection.persistence.AuthenticationServiceComponentRepository;
import cc.cornerstones.biz.administration.serviceconnection.service.assembly.DfsServiceHandler;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.AuthenticationServiceComponentService;
import cc.cornerstones.biz.administration.serviceconnection.share.constants.ServiceComponentTypeEnum;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.datadictionary.entity.DictionaryCategoryDo;
import cc.cornerstones.biz.share.assembly.ResourceReferenceManager;
import cc.cornerstones.biz.share.event.AuthenticationServiceComponentDeletedEvent;
import cc.cornerstones.biz.share.event.EventBusManager;
import com.alibaba.fastjson.JSONObject;
import org.pf4j.PluginAlreadyLoadedException;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
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
public class AuthenticationServiceComponentServiceImpl implements AuthenticationServiceComponentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationServiceComponentServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private AuthenticationServiceComponentRepository authenticationServiceComponentRepository;

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


    @Override
    public AuthenticationServiceComponentDto createPluginAuthenticationServiceComponent(
            CreatePluginAuthenticationServiceComponentDto createPluginAuthenticationServiceComponentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        boolean existsDuplicate =
                this.authenticationServiceComponentRepository.existsByName(createPluginAuthenticationServiceComponentDto.getName());
        if (existsDuplicate) {
            throw new AbcResourceDuplicateException(String.format("%s::name=%s", AuthenticationServiceComponentDo.RESOURCE_SYMBOL,
                    createPluginAuthenticationServiceComponentDto.getName()));
        }

        //
        // Step 2, core-processing
        //
        AuthenticationServiceComponentDo authenticationServiceComponentDo = new AuthenticationServiceComponentDo();
        authenticationServiceComponentDo.setUid(this.idHelper.getNextDistributedId(AuthenticationServiceComponentDo.RESOURCE_NAME));
        authenticationServiceComponentDo.setName(createPluginAuthenticationServiceComponentDto.getName());
        authenticationServiceComponentDo.setObjectName(
                createPluginAuthenticationServiceComponentDto.getName()
                        .replaceAll("_", "__")
                        .replaceAll("\\s", "_")
                        .toLowerCase());
        authenticationServiceComponentDo.setDescription(createPluginAuthenticationServiceComponentDto.getDescription());
        authenticationServiceComponentDo.setSequence(createPluginAuthenticationServiceComponentDto.getSequence());
        authenticationServiceComponentDo.setType(ServiceComponentTypeEnum.PLUGIN);
        authenticationServiceComponentDo.setFrontEndComponentFileId(
                createPluginAuthenticationServiceComponentDto.getFrontEndComponentFileId());
        authenticationServiceComponentDo.setDfsServiceAgentUidOfFrontEndComponentFileId(
                createPluginAuthenticationServiceComponentDto.getDfsServiceAgentUidOfFrontEndComponentFileId());
        authenticationServiceComponentDo.setBackEndComponentFileId(
                createPluginAuthenticationServiceComponentDto.getBackEndComponentFileId());
        authenticationServiceComponentDo.setDfsServiceAgentUidOfBackEndComponentFileId(
                createPluginAuthenticationServiceComponentDto.getDfsServiceAgentUidOfBackEndComponentFileId());

        // back end component metadata & configuration template & user info schema
        File file = this.dfsServiceHandler.downloadFile(
                createPluginAuthenticationServiceComponentDto.getDfsServiceAgentUidOfBackEndComponentFileId(),
                createPluginAuthenticationServiceComponentDto.getBackEndComponentFileId(),
                operatingUserProfile);
        BackEndComponentParsedResultDto backEndComponentParsedResultDto =
                parseBackEndComponent(file, operatingUserProfile);
        authenticationServiceComponentDo.setBackEndComponentMetadata(backEndComponentParsedResultDto.getMetadata());
        authenticationServiceComponentDo.setConfigurationTemplate(backEndComponentParsedResultDto.getConfigurationTemplate());
        authenticationServiceComponentDo.setUserInfoSchema(backEndComponentParsedResultDto.getUserInfoSchema());

        BaseDo.create(authenticationServiceComponentDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.authenticationServiceComponentRepository.save(authenticationServiceComponentDo);

        //
        // Step 3, post-processing
        //
        AuthenticationServiceComponentDto authenticationServiceComponentDto = new AuthenticationServiceComponentDto();
        BeanUtils.copyProperties(authenticationServiceComponentDto, authenticationServiceComponentDto);
        return authenticationServiceComponentDto;
    }

    @Override
    public BackEndComponentParsedResultDto parseBackEndComponent(
            File file,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        String configurationTemplate = null;
        JSONObject userInfoSchema = null;

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
            List<AuthenticationServiceProvider> listOfProcessors =
                    this.pluginHelper.getPluginManager().getExtensions(
                            AuthenticationServiceProvider.class,
                            pluginId);
            if (listOfProcessors == null || listOfProcessors.isEmpty()) {
                throw new AbcUndefinedException("cannot find AuthenticationServiceComponent");
            }

            if (listOfProcessors.size() > 1) {
                throw new AbcUndefinedException("found " + listOfProcessors.size() + " AuthenticationServiceComponent");
            }

            AuthenticationServiceProvider processor = listOfProcessors.get(0);

            configurationTemplate = processor.getConfigurationTemplate();
            userInfoSchema = processor.getUserInfoSchema();

            PluginProfile pluginProfile = this.pluginHelper.parsePluginProfile(pluginId);

            //
            BackEndComponentParsedResultDto result = new BackEndComponentParsedResultDto();
            result.setMetadata(pluginProfile);
            result.setConfigurationTemplate(configurationTemplate);
            result.setUserInfoSchema(userInfoSchema);
            return result;
        } catch (Exception e) {
            LOGGER.error("failed to get configuration template and user info schema from the plugin {}",
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
    public void updateAuthenticationServiceComponent(
            Long uid,
            UpdateAuthenticationServiceComponentDto updateAuthenticationServiceComponentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        AuthenticationServiceComponentDo authenticationServiceComponentDo = this.authenticationServiceComponentRepository.findByUid(uid);
        if (authenticationServiceComponentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", AuthenticationServiceComponentDo.RESOURCE_SYMBOL,
                    uid));
        }

        //
        // Step 2, core-processing
        //
        switch (authenticationServiceComponentDo.getType()) {
            case BUILTIN: {
                updateBuiltinAuthenticationServiceComponent(authenticationServiceComponentDo, updateAuthenticationServiceComponentDto,
                        operatingUserProfile);
            }
            break;
            case PLUGIN: {
                updatePluginAuthenticationServiceComponent(authenticationServiceComponentDo, updateAuthenticationServiceComponentDto,
                        operatingUserProfile);
            }
            break;
            default:
                throw new AbcResourceConflictException(String.format("unknown service Component type:%s",
                        authenticationServiceComponentDo.getType()));
        }

        //
        // Step 3, post-processing
        //
    }

    private void updateBuiltinAuthenticationServiceComponent(
            AuthenticationServiceComponentDo authenticationServiceComponentDo,
            UpdateAuthenticationServiceComponentDto updateAuthenticationServiceComponentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        boolean requiredToUpdate = false;

        if (updateAuthenticationServiceComponentDto.getDescription() != null
                && !updateAuthenticationServiceComponentDto.getDescription().equalsIgnoreCase(authenticationServiceComponentDo.getDescription())) {
            authenticationServiceComponentDo.setDescription(updateAuthenticationServiceComponentDto.getDescription());
            requiredToUpdate = true;
        }
        if (updateAuthenticationServiceComponentDto.getSequence() != null
                && !updateAuthenticationServiceComponentDto.getSequence().equals(authenticationServiceComponentDo.getSequence())) {
            authenticationServiceComponentDo.setSequence(updateAuthenticationServiceComponentDto.getSequence());
            requiredToUpdate = true;
        }

        if (requiredToUpdate) {
            BaseDo.update(authenticationServiceComponentDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.authenticationServiceComponentRepository.save(authenticationServiceComponentDo);
        }
    }

    private void updatePluginAuthenticationServiceComponent(
            AuthenticationServiceComponentDo authenticationServiceComponentDo,
            UpdateAuthenticationServiceComponentDto updateAuthenticationServiceComponentDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        if (!ObjectUtils.isEmpty(updateAuthenticationServiceComponentDto.getName())
                && !updateAuthenticationServiceComponentDto.getName().equalsIgnoreCase(authenticationServiceComponentDo.getName())) {
            boolean existsDuplicate =
                    this.authenticationServiceComponentRepository.existsByName(updateAuthenticationServiceComponentDto.getName());
            if (existsDuplicate) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s", AuthenticationServiceComponentDo.RESOURCE_SYMBOL,
                        updateAuthenticationServiceComponentDto.getName()));
            }
        }

        //
        // Step 2, core-processing
        //
        boolean requiredToUpdate = false;

        if (!ObjectUtils.isEmpty(updateAuthenticationServiceComponentDto.getName())
                && !updateAuthenticationServiceComponentDto.getName().equalsIgnoreCase(authenticationServiceComponentDo.getName())) {
            authenticationServiceComponentDo.setName(updateAuthenticationServiceComponentDto.getName());
            authenticationServiceComponentDo.setObjectName(
                    updateAuthenticationServiceComponentDto.getName()
                            .replaceAll("_", "__")
                            .replaceAll("\\s", "_")
                            .toLowerCase());
            requiredToUpdate = true;
        }
        if (updateAuthenticationServiceComponentDto.getDescription() != null
                && !updateAuthenticationServiceComponentDto.getDescription().equalsIgnoreCase(authenticationServiceComponentDo.getDescription())) {
            authenticationServiceComponentDo.setDescription(updateAuthenticationServiceComponentDto.getDescription());
            requiredToUpdate = true;
        }
        if (updateAuthenticationServiceComponentDto.getSequence() != null
                && !updateAuthenticationServiceComponentDto.getSequence().equals(authenticationServiceComponentDo.getSequence())) {
            authenticationServiceComponentDo.setSequence(updateAuthenticationServiceComponentDto.getSequence());
            requiredToUpdate = true;
        }
        if (!ObjectUtils.isEmpty(updateAuthenticationServiceComponentDto.getFrontEndComponentFileId())
                && !updateAuthenticationServiceComponentDto.getFrontEndComponentFileId().equalsIgnoreCase(authenticationServiceComponentDo.getFrontEndComponentFileId())) {
            authenticationServiceComponentDo.setFrontEndComponentFileId(updateAuthenticationServiceComponentDto.getFrontEndComponentFileId());
            requiredToUpdate = true;
        }
        if (updateAuthenticationServiceComponentDto.getDfsServiceAgentUidOfFrontEndComponentFileId() != null
                && !updateAuthenticationServiceComponentDto.getDfsServiceAgentUidOfFrontEndComponentFileId().equals(authenticationServiceComponentDo.getDfsServiceAgentUidOfFrontEndComponentFileId())) {
            authenticationServiceComponentDo.setDfsServiceAgentUidOfFrontEndComponentFileId(updateAuthenticationServiceComponentDto.getDfsServiceAgentUidOfFrontEndComponentFileId());
            requiredToUpdate = true;
        }
        if (!ObjectUtils.isEmpty(updateAuthenticationServiceComponentDto.getBackEndComponentFileId())
                && !updateAuthenticationServiceComponentDto.getBackEndComponentFileId().equalsIgnoreCase(authenticationServiceComponentDo.getBackEndComponentFileId())) {
            authenticationServiceComponentDo.setBackEndComponentFileId(updateAuthenticationServiceComponentDto.getBackEndComponentFileId());

            // back end component metadata & configuration template & user info schema
            File file = this.dfsServiceHandler.downloadFile(
                    updateAuthenticationServiceComponentDto.getDfsServiceAgentUidOfBackEndComponentFileId(),
                    updateAuthenticationServiceComponentDto.getBackEndComponentFileId(),
                    operatingUserProfile);
            BackEndComponentParsedResultDto backEndComponentParsedResultDto =
                    parseBackEndComponent(file, operatingUserProfile);
            authenticationServiceComponentDo.setBackEndComponentMetadata(backEndComponentParsedResultDto.getMetadata());
            authenticationServiceComponentDo.setConfigurationTemplate(backEndComponentParsedResultDto.getConfigurationTemplate());
            authenticationServiceComponentDo.setUserInfoSchema(backEndComponentParsedResultDto.getUserInfoSchema());

            requiredToUpdate = true;
        }
        if (updateAuthenticationServiceComponentDto.getDfsServiceAgentUidOfBackEndComponentFileId() != null
                && !updateAuthenticationServiceComponentDto.getDfsServiceAgentUidOfBackEndComponentFileId().equals(authenticationServiceComponentDo.getDfsServiceAgentUidOfBackEndComponentFileId())) {
            authenticationServiceComponentDo.setDfsServiceAgentUidOfBackEndComponentFileId(updateAuthenticationServiceComponentDto.getDfsServiceAgentUidOfBackEndComponentFileId());
            requiredToUpdate = true;
        }

        if (requiredToUpdate) {
            BaseDo.update(authenticationServiceComponentDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.authenticationServiceComponentRepository.save(authenticationServiceComponentDo);
        }
    }

    @Override
    public List<String> listAllReferencesToAuthenticationServiceComponent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        AuthenticationServiceComponentDo authenticationServiceComponentDo = this.authenticationServiceComponentRepository.findByUid(uid);
        if (authenticationServiceComponentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", AuthenticationServiceComponentDo.RESOURCE_SYMBOL,
                    uid));
        }

        //
        // Step 2, core-processing
        //
        return this.resourceReferenceManager.check(
                ResourceReferenceManager.ResourceCategoryEnum.AUTHENTICATION_SERVICE_COMPONENT,
                authenticationServiceComponentDo.getUid(),
                authenticationServiceComponentDo.getName());
    }

    @Override
    public void deleteAuthenticationServiceComponent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        AuthenticationServiceComponentDo authenticationServiceComponentDo = this.authenticationServiceComponentRepository.findByUid(uid);
        if (authenticationServiceComponentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", AuthenticationServiceComponentDo.RESOURCE_SYMBOL,
                    uid));
        }

        deleteAuthenticationServiceComponent(authenticationServiceComponentDo, operatingUserProfile);
    }

    private void deleteAuthenticationServiceComponent(
            AuthenticationServiceComponentDo authenticationServiceComponentDo,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        authenticationServiceComponentDo.setDeleted(Boolean.TRUE);
        BaseDo.update(authenticationServiceComponentDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.authenticationServiceComponentRepository.save(authenticationServiceComponentDo);

        // post event
        AuthenticationServiceComponentDeletedEvent event = new AuthenticationServiceComponentDeletedEvent();
        event.setAuthenticationServiceComponentDo(authenticationServiceComponentDo);
        event.setOperatingUserProfile(operatingUserProfile);
        this.eventBusManager.post(event);
    }

    @Override
    public AuthenticationServiceComponentDto getAuthenticationServiceComponent(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        AuthenticationServiceComponentDo authenticationServiceComponentDo = this.authenticationServiceComponentRepository.findByUid(uid);
        if (authenticationServiceComponentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", AuthenticationServiceComponentDo.RESOURCE_SYMBOL,
                    uid));
        }

        AuthenticationServiceComponentDto authenticationServiceComponentDto = new AuthenticationServiceComponentDto();
        BeanUtils.copyProperties(authenticationServiceComponentDo, authenticationServiceComponentDto);

        //
        // 构造 front-end component
        //
        switch (authenticationServiceComponentDo.getType()) {
            case BUILTIN: {
                if (!ObjectUtils.isEmpty(authenticationServiceComponentDo.getResourceName())) {
                    InputStream inputStream = getClass().getClassLoader().getResourceAsStream(authenticationServiceComponentDo.getResourceName());
                    if (inputStream == null) {
                        throw new AbcResourceIntegrityException("cannot find resource");
                    } else {
                        String content = AbcFileUtils.readContent(inputStream);
                        authenticationServiceComponentDto.setFrontEndComponent(content);
                    }
                }
            }
            break;
            case PLUGIN: {
                if (!ObjectUtils.isEmpty(authenticationServiceComponentDo.getFrontEndComponentFileId())
                        && authenticationServiceComponentDo.getDfsServiceAgentUidOfFrontEndComponentFileId() != null) {
                    File file = null;

                    try {
                        file = this.dfsServiceHandler.downloadFile(
                                authenticationServiceComponentDo.getDfsServiceAgentUidOfFrontEndComponentFileId(),
                                authenticationServiceComponentDo.getFrontEndComponentFileId(),
                                operatingUserProfile);
                    } catch (Exception e) {
                        LOGGER.error("failed to load file from file id::{}, {}",
                                authenticationServiceComponentDo.getDfsServiceAgentUidOfFrontEndComponentFileId(),
                                authenticationServiceComponentDo.getFrontEndComponentFileId(),
                                e);
                        throw new AbcResourceIntegrityException("file integrity issue");
                    }
                    String content = AbcFileUtils.readContent(file, StandardCharsets.UTF_8);
                    authenticationServiceComponentDto.setFrontEndComponent(content);
                }
            }
            break;
            default:
                break;
        }

        return authenticationServiceComponentDto;
    }

    @Override
    public List<AuthenticationServiceComponentDto> listingQueryAuthenticationServiceComponents(
            Long uid,
            String name,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Specification<AuthenticationServiceComponentDo> specification = new Specification<AuthenticationServiceComponentDo>() {
            @Override
            public Predicate toPredicate(Root<AuthenticationServiceComponentDo> root, CriteriaQuery<?> query,
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

        List<AuthenticationServiceComponentDo> itemDoList = this.authenticationServiceComponentRepository.findAll(specification, sort);
        if (CollectionUtils.isEmpty(itemDoList)) {
            return null;
        }
        List<AuthenticationServiceComponentDto> content = new LinkedList<>();
        itemDoList.forEach(itemDo -> {
            AuthenticationServiceComponentDto itemDto = new AuthenticationServiceComponentDto();
            BeanUtils.copyProperties(itemDo, itemDto);
            content.add(itemDto);
        });
        return content;
    }

    @Override
    public Page<AuthenticationServiceComponentDto> pagingQueryAuthenticationServiceComponents(
            Long uid,
            String name,
            String description,
            List<ServiceComponentTypeEnum> typeList,
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
        Specification<AuthenticationServiceComponentDo> specification = new Specification<AuthenticationServiceComponentDo>() {
            @Override
            public Predicate toPredicate(Root<AuthenticationServiceComponentDo> root, CriteriaQuery<?> query,
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

        Page<AuthenticationServiceComponentDo> itemDoPage = this.authenticationServiceComponentRepository.findAll(specification, pageable);

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
        List<AuthenticationServiceComponentDto> content = new ArrayList<>(itemDoPage.getSize());
        itemDoPage.forEach(itemDo -> {
            AuthenticationServiceComponentDto itemDto = new AuthenticationServiceComponentDto();
            BeanUtils.copyProperties(itemDo, itemDto);

            // 为 created by, last modified by 补充 user brief information
            itemDto.setCreatedByUser(userBriefInformationMap.get(itemDo.getCreatedBy()));
            itemDto.setLastModifiedByUser(userBriefInformationMap.get(itemDo.getLastModifiedBy()));

            content.add(itemDto);
        });
        Page<AuthenticationServiceComponentDto> itemDtoPage = new PageImpl<>(content, pageable, itemDoPage.getTotalElements());
        return itemDtoPage;
    }

    @Override
    public Resource getFrontEndComponentInterface(
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return new ClassPathResource("extensions/authentication_service_component_front_end_component_interface.js");
    }

    @Override
    public Resource getBackEndComponentInterface(
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return new ClassPathResource("extensions/authentication_service_Component_back_end_component_interface.jar");
    }
}
