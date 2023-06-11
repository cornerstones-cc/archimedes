package cc.cornerstones.biz.resourceownership.service.impl;

import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.arbutus.pf4j.service.assembly.PluginHelper;
import cc.cornerstones.arbutus.pf4j.share.types.PluginProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.archimedes.extensions.DataPermissionServiceProvider;
import cc.cornerstones.biz.administration.serviceconnection.entity.DataPermissionServiceAgentDo;
import cc.cornerstones.biz.administration.serviceconnection.entity.DataPermissionServiceComponentDo;
import cc.cornerstones.biz.administration.serviceconnection.persistence.DataPermissionServiceComponentRepository;
import cc.cornerstones.biz.administration.serviceconnection.persistence.DataPermissionServiceAgentRepository;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DfsServiceAgentService;
import cc.cornerstones.biz.administration.usermanagement.entity.AccountTypeDo;
import cc.cornerstones.biz.administration.usermanagement.entity.UserAccountDo;
import cc.cornerstones.biz.administration.usermanagement.persistence.AccountTypeRepository;
import cc.cornerstones.biz.administration.usermanagement.persistence.UserAccountRepository;
import cc.cornerstones.biz.resourceownership.service.inf.ResourceOwnershipService;
import cc.cornerstones.biz.share.event.EventBusManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.util.*;

@Service
public class ResourceOwnershipServiceImpl implements ResourceOwnershipService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceOwnershipServiceImpl.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private PluginHelper pluginHelper;

    @Autowired
    private DataPermissionServiceAgentRepository dataPermissionServiceAgentRepository;

    @Autowired
    private DataPermissionServiceComponentRepository dataPermissionServiceComponentRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private DfsServiceAgentService dfsServiceAgentService;

    @Autowired
    private AccountTypeRepository accountTypeRepository;

    @Override
    public List<cc.cornerstones.archimedes.extensions.types.TreeNode> treeListingAllNodesOfResourceCategoryHierarchy(
            Long dataPermissionServiceAgentUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DataPermissionServiceAgentDo dataPermissionServiceAgentDo =
                this.dataPermissionServiceAgentRepository.findByUid(dataPermissionServiceAgentUid);
        if (dataPermissionServiceAgentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DataPermissionServiceAgentDo.RESOURCE_SYMBOL, dataPermissionServiceAgentUid));
        }
        Long dataPermissionServiceComponentUid = dataPermissionServiceAgentDo.getServiceComponentUid();
        DataPermissionServiceComponentDo dataPermissionServiceComponentDo =
                this.dataPermissionServiceComponentRepository.findByUid(dataPermissionServiceComponentUid);
        if (dataPermissionServiceComponentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DataPermissionServiceComponentDo.RESOURCE_SYMBOL, dataPermissionServiceComponentUid));
        }

        //
        // Step 2, core-processing
        //

        switch (dataPermissionServiceComponentDo.getType()) {
            case BUILTIN: {
                String entryClassName = dataPermissionServiceComponentDo.getEntryClassName();
                DataPermissionServiceProvider dataPermissionServiceProvider = null;
                Map<String, DataPermissionServiceProvider> candidateDataPermissionServiceProviderMap =
                        this.applicationContext.getBeansOfType(DataPermissionServiceProvider.class);
                if (!CollectionUtils.isEmpty(candidateDataPermissionServiceProviderMap)) {
                    for (DataPermissionServiceProvider candidateDataPermissionServiceProvider : candidateDataPermissionServiceProviderMap.values()) {
                        if (candidateDataPermissionServiceProvider.getClass().getName().equals(entryClassName)) {
                            dataPermissionServiceProvider = candidateDataPermissionServiceProvider;
                            break;
                        }
                    }
                }
                if (dataPermissionServiceProvider == null) {
                    throw new AbcResourceConflictException(String.format("cannot find data permission service " +
                                    "provider:%s",
                            dataPermissionServiceComponentDo.getName()));
                }

                try {
                    List<cc.cornerstones.archimedes.extensions.types.TreeNode> treeNodeList =
                            dataPermissionServiceProvider.treeListingAllNodesOfResourceCategoryHierarchy(
                                    dataPermissionServiceAgentDo.getConfiguration());

                    return treeNodeList;
                } catch (Exception e) {
                    LOGGER.error("failed to tree listing all nodes of resource category hierarchy through data " +
                                    "permission " +
                                    "service provider {}",
                            dataPermissionServiceComponentDo.getName(), e);
                    throw new AbcResourceConflictException("failed to tree listing all nodes of resource category hierarchy");
                }
            }
            case PLUGIN: {
                PluginProfile pluginProfile = dataPermissionServiceComponentDo.getBackEndComponentMetadata();
                if (pluginProfile == null || ObjectUtils.isEmpty(pluginProfile.getPluginId())) {
                    throw new AbcResourceConflictException("illegal plugin");
                }
                try {
                    this.pluginHelper.ensureStartPluginIdentifiedByPluginId(pluginProfile.getPluginId());
                } catch (Exception e) {
                    File pluginFile = this.dfsServiceAgentService.downloadFile(
                            dataPermissionServiceComponentDo.getDfsServiceAgentUidOfBackEndComponentFileId(),
                            dataPermissionServiceComponentDo.getBackEndComponentFileId(),
                            operatingUserProfile);
                    try {
                        this.pluginHelper.ensureStartPluginIdentifiedByPath(pluginFile.getAbsolutePath());
                    } catch (Exception e3) {
                        LOGGER.error("failed to ensure start plugin:{}", pluginProfile.getPluginId(), e3);
                        throw new AbcResourceConflictException("failed to load plugin");
                    }
                }

                List<DataPermissionServiceProvider> listOfProcessors =
                        this.pluginHelper.getPluginManager().getExtensions(
                                DataPermissionServiceProvider.class,
                                pluginProfile.getPluginId());
                if (listOfProcessors == null || listOfProcessors.isEmpty()) {
                    this.pluginHelper.getPluginManager().unloadPlugin(pluginProfile.getPluginId());
                    throw new AbcUndefinedException("cannot find DataPermissionServiceProvider");
                }

                if (listOfProcessors.size() > 1) {
                    this.pluginHelper.getPluginManager().unloadPlugin(pluginProfile.getPluginId());
                    throw new AbcUndefinedException("found " + listOfProcessors.size() + " DataPermissionServiceProvider");
                }

                DataPermissionServiceProvider dataPermissionServiceProvider = listOfProcessors.get(0);
                try {
                    List<cc.cornerstones.archimedes.extensions.types.TreeNode> treeNodeList =
                            dataPermissionServiceProvider.treeListingAllNodesOfResourceCategoryHierarchy(
                                    dataPermissionServiceAgentDo.getConfiguration());

                    return treeNodeList;
                } catch (Exception e) {
                    LOGGER.error("failed to tree listing all nodes of resource category hierarchy through data " +
                                    "permission " +
                                    "service provider {}",
                            dataPermissionServiceComponentDo.getName(), e);
                    throw new AbcResourceConflictException("failed to tree listing all nodes of resource category hierarchy");
                }
            }
            default:
                throw new AbcResourceConflictException(String.format("unsupported service provider type:%s",
                        dataPermissionServiceComponentDo.getType()));
        }

        //
        // Step 3, post-processing
        //
    }

    @Override
    public cc.cornerstones.archimedes.extensions.types.TreeNode treeListingAllNodesOfResourceStructureHierarchy(
            Long dataPermissionServiceAgentUid,
            Long resourceCategoryUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DataPermissionServiceAgentDo dataPermissionServiceAgentDo =
                this.dataPermissionServiceAgentRepository.findByUid(dataPermissionServiceAgentUid);
        if (dataPermissionServiceAgentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DataPermissionServiceAgentDo.RESOURCE_SYMBOL, dataPermissionServiceAgentUid));
        }
        Long dataPermissionServiceComponentUid = dataPermissionServiceAgentDo.getServiceComponentUid();
        DataPermissionServiceComponentDo dataPermissionServiceComponentDo =
                this.dataPermissionServiceComponentRepository.findByUid(dataPermissionServiceComponentUid);
        if (dataPermissionServiceComponentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DataPermissionServiceComponentDo.RESOURCE_SYMBOL, dataPermissionServiceComponentUid));
        }

        //
        // Step 2, core-processing
        //

        switch (dataPermissionServiceComponentDo.getType()) {
            case BUILTIN: {
                String entryClassName = dataPermissionServiceComponentDo.getEntryClassName();
                DataPermissionServiceProvider dataPermissionServiceProvider = null;
                Map<String, DataPermissionServiceProvider> candidateDataPermissionServiceProviderMap =
                        this.applicationContext.getBeansOfType(DataPermissionServiceProvider.class);
                if (!CollectionUtils.isEmpty(candidateDataPermissionServiceProviderMap)) {
                    for (DataPermissionServiceProvider candidateDataPermissionServiceProvider : candidateDataPermissionServiceProviderMap.values()) {
                        if (candidateDataPermissionServiceProvider.getClass().getName().equals(entryClassName)) {
                            dataPermissionServiceProvider = candidateDataPermissionServiceProvider;
                            break;
                        }
                    }
                }
                if (dataPermissionServiceProvider == null) {
                    throw new AbcResourceConflictException(String.format("cannot find data permission service " +
                                    "provider:%s",
                            dataPermissionServiceComponentDo.getName()));
                }

                try {
                    cc.cornerstones.archimedes.extensions.types.TreeNode treeNode =
                            dataPermissionServiceProvider.treeListingAllNodesOfResourceStructureHierarchy(
                                    resourceCategoryUid,
                                    dataPermissionServiceAgentDo.getConfiguration());

                    return treeNode;
                } catch (Exception e) {
                    LOGGER.error("failed to tree listing all nodes of resource structure hierarchy through data " +
                                    "permission " +
                                    "service provider {}",
                            dataPermissionServiceComponentDo.getName(), e);
                    throw new AbcResourceConflictException("failed to tree listing all nodes of resource structure hierarchy");
                }
            }
            case PLUGIN: {
                PluginProfile pluginProfile = dataPermissionServiceComponentDo.getBackEndComponentMetadata();
                if (pluginProfile == null || ObjectUtils.isEmpty(pluginProfile.getPluginId())) {
                    throw new AbcResourceConflictException("illegal plugin");
                }
                try {
                    this.pluginHelper.ensureStartPluginIdentifiedByPluginId(pluginProfile.getPluginId());
                } catch (Exception e) {
                    File pluginFile = this.dfsServiceAgentService.downloadFile(
                            dataPermissionServiceComponentDo.getDfsServiceAgentUidOfBackEndComponentFileId(),
                            dataPermissionServiceComponentDo.getBackEndComponentFileId(),
                            operatingUserProfile);
                    try {
                        this.pluginHelper.ensureStartPluginIdentifiedByPath(pluginFile.getAbsolutePath());
                    } catch (Exception e3) {
                        LOGGER.error("failed to ensure start plugin:{}", pluginProfile.getPluginId(), e3);
                        throw new AbcResourceConflictException("failed to load plugin");
                    }
                }

                List<DataPermissionServiceProvider> listOfProcessors =
                        this.pluginHelper.getPluginManager().getExtensions(
                                DataPermissionServiceProvider.class,
                                pluginProfile.getPluginId());
                if (listOfProcessors == null || listOfProcessors.isEmpty()) {
                    this.pluginHelper.getPluginManager().unloadPlugin(pluginProfile.getPluginId());
                    throw new AbcUndefinedException("cannot find DataPermissionServiceProvider");
                }

                if (listOfProcessors.size() > 1) {
                    this.pluginHelper.getPluginManager().unloadPlugin(pluginProfile.getPluginId());
                    throw new AbcUndefinedException("found " + listOfProcessors.size() + " DataPermissionServiceProvider");
                }

                DataPermissionServiceProvider dataPermissionServiceProvider = listOfProcessors.get(0);
                try {
                    cc.cornerstones.archimedes.extensions.types.TreeNode treeNode =
                            dataPermissionServiceProvider.treeListingAllNodesOfResourceStructureHierarchy(
                                    resourceCategoryUid,
                                    dataPermissionServiceAgentDo.getConfiguration());

                    return treeNode;
                } catch (Exception e) {
                    LOGGER.error("failed to tree listing all nodes of resource structure hierarchy through data " +
                                    "permission " +
                                    "service provider {}",
                            dataPermissionServiceComponentDo.getName(), e);
                    throw new AbcResourceConflictException("failed to tree listing all nodes of resource structure hierarchy");
                }
            }
            default:
                throw new AbcResourceConflictException(String.format("unsupported service provider type:%s",
                        dataPermissionServiceComponentDo.getType()));
        }

        //
        // Step 3, post-processing
        //
    }

    @Override
    public List<cc.cornerstones.archimedes.extensions.types.TreeNode> treeListingAllNodesOfResourceContentHierarchy(
            Long dataPermissionServiceAgentUid,
            Long resourceCategoryUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DataPermissionServiceAgentDo dataPermissionServiceAgentDo =
                this.dataPermissionServiceAgentRepository.findByUid(dataPermissionServiceAgentUid);
        if (dataPermissionServiceAgentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DataPermissionServiceAgentDo.RESOURCE_SYMBOL, dataPermissionServiceAgentUid));
        }
        Long dataPermissionServiceComponentUid = dataPermissionServiceAgentDo.getServiceComponentUid();
        DataPermissionServiceComponentDo dataPermissionServiceComponentDo =
                this.dataPermissionServiceComponentRepository.findByUid(dataPermissionServiceComponentUid);
        if (dataPermissionServiceComponentDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    DataPermissionServiceComponentDo.RESOURCE_SYMBOL, dataPermissionServiceComponentUid));
        }

        String accountName = null;
        if (dataPermissionServiceAgentDo.getAccountTypeUid() != null) {
            UserAccountDo userAccountDo =
                    this.userAccountRepository.findByUserUidAndAccountTypeUid(
                            operatingUserProfile.getUid(),
                            dataPermissionServiceAgentDo.getAccountTypeUid());
            if (userAccountDo == null) {
                AccountTypeDo accountTypeDo =
                        this.accountTypeRepository.findByUid(dataPermissionServiceAgentDo.getAccountTypeUid());
                throw new AbcResourceNotFoundException(String.format("此访问要求您先拥有帐户类型:%s", accountTypeDo.getName()));
            }

            accountName = userAccountDo.getName();
        } else {
            LOGGER.error("no account type, ignore data permission filtering");
            return null;
        }

        //
        // Step 2, core-processing
        //
        switch (dataPermissionServiceComponentDo.getType()) {
            case BUILTIN: {
                String entryClassName = dataPermissionServiceComponentDo.getEntryClassName();
                DataPermissionServiceProvider dataPermissionServiceProvider = null;
                Map<String, DataPermissionServiceProvider> candidateDataPermissionServiceProviderMap =
                        this.applicationContext.getBeansOfType(DataPermissionServiceProvider.class);
                if (!CollectionUtils.isEmpty(candidateDataPermissionServiceProviderMap)) {
                    for (DataPermissionServiceProvider candidateDataPermissionServiceProvider : candidateDataPermissionServiceProviderMap.values()) {
                        if (candidateDataPermissionServiceProvider.getClass().getName().equals(entryClassName)) {
                            dataPermissionServiceProvider = candidateDataPermissionServiceProvider;
                            break;
                        }
                    }
                }
                if (dataPermissionServiceProvider == null) {
                    throw new AbcResourceConflictException(String.format("cannot find data permission service " +
                                    "provider:%s",
                            dataPermissionServiceComponentDo.getName()));
                }

                try {
                    List<cc.cornerstones.archimedes.extensions.types.TreeNode> treeNodeList =
                            dataPermissionServiceProvider.treeListingAllNodesOfResourceContentHierarchy(
                                    resourceCategoryUid,
                                    dataPermissionServiceAgentDo.getConfiguration(),
                                    accountName);

                    return treeNodeList;
                } catch (Exception e) {
                    LOGGER.error("failed to tree listing all nodes of resource content hierarchy through data " +
                                    "permission " +
                                    "service provider {}",
                            dataPermissionServiceComponentDo.getName(), e);
                    throw new AbcResourceConflictException("failed to tree listing all nodes of resource content " +
                            "hierarchy");
                }
            }
            case PLUGIN: {
                PluginProfile pluginProfile = dataPermissionServiceComponentDo.getBackEndComponentMetadata();
                if (pluginProfile == null || ObjectUtils.isEmpty(pluginProfile.getPluginId())) {
                    throw new AbcResourceConflictException("illegal plugin");
                }
                try {
                    this.pluginHelper.ensureStartPluginIdentifiedByPluginId(pluginProfile.getPluginId());
                } catch (Exception e) {
                    File pluginFile = this.dfsServiceAgentService.downloadFile(
                            dataPermissionServiceComponentDo.getDfsServiceAgentUidOfBackEndComponentFileId(),
                            dataPermissionServiceComponentDo.getBackEndComponentFileId(),
                            operatingUserProfile);
                    try {
                        this.pluginHelper.ensureStartPluginIdentifiedByPath(pluginFile.getAbsolutePath());
                    } catch (Exception e3) {
                        LOGGER.error("failed to ensure start plugin:{}", pluginProfile.getPluginId(), e3);
                        throw new AbcResourceConflictException("failed to load plugin");
                    }
                }

                List<DataPermissionServiceProvider> listOfProcessors =
                        this.pluginHelper.getPluginManager().getExtensions(
                                DataPermissionServiceProvider.class,
                                pluginProfile.getPluginId());
                if (listOfProcessors == null || listOfProcessors.isEmpty()) {
                    this.pluginHelper.getPluginManager().unloadPlugin(pluginProfile.getPluginId());
                    throw new AbcUndefinedException("cannot find DataPermissionServiceProvider");
                }

                if (listOfProcessors.size() > 1) {
                    this.pluginHelper.getPluginManager().unloadPlugin(pluginProfile.getPluginId());
                    throw new AbcUndefinedException("found " + listOfProcessors.size() + " DataPermissionServiceProvider");
                }

                DataPermissionServiceProvider dataPermissionServiceProvider = listOfProcessors.get(0);
                try {
                    List<cc.cornerstones.archimedes.extensions.types.TreeNode> treeNodeList =
                            dataPermissionServiceProvider.treeListingAllNodesOfResourceContentHierarchy(
                                    resourceCategoryUid,
                                    dataPermissionServiceAgentDo.getConfiguration(),
                                    accountName);

                    return treeNodeList;
                } catch (Exception e) {
                    LOGGER.error("failed to tree listing all nodes of resource content hierarchy through data " +
                                    "permission " +
                                    "service provider {}",
                            dataPermissionServiceComponentDo.getName(), e);
                    throw new AbcResourceConflictException("failed to tree listing all nodes of resource content hierarchy");
                }
            }
            default:
                throw new AbcResourceConflictException(String.format("unsupported service provider type:%s",
                        dataPermissionServiceComponentDo.getType()));
        }

        //
        // Step 3, post-processing
        //
    }
}
