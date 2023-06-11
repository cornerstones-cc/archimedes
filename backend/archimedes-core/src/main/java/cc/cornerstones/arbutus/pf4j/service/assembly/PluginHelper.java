package cc.cornerstones.arbutus.pf4j.service.assembly;

import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcDateTimeFormatterUtils;
import cc.cornerstones.almond.utils.AbcDateUtils;
import cc.cornerstones.arbutus.pf4j.share.types.PluginProfile;
import org.pf4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Component
public class PluginHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginHelper.class);

    @Autowired
    private PluginManager pluginManager;

    private Object lock = new Object();

    public PluginManager getPluginManager() {
        return this.pluginManager;
    }

    public void ensureStartPluginIdentifiedByPluginId(String pluginId) throws Exception {
        startPluginIdentifiedByPluginId(pluginId);
    }

    public void ensureStartPluginIdentifiedByPath(String pluginProgramPackageFilePath) throws Exception {
        startPluginIdentifiedByPath(pluginProgramPackageFilePath);
    }

    public String startPluginIdentifiedByPath(String pluginProgramPackageFilePath) throws Exception {
        String pluginId = null;

        if (ObjectUtils.isEmpty(pluginProgramPackageFilePath)) {
            throw new Exception("pluginProgramPackageFilePath is null or empty");
        }
        if (Files.notExists(Paths.get(pluginProgramPackageFilePath))) {
            throw new Exception(String.format("cannot find file at %s", pluginProgramPackageFilePath));
        }

        try {
            pluginId = loadPluginFromPath(pluginProgramPackageFilePath);
        } catch (Exception e) {
            String logMsg = String.format("failed to load plugin at %s",
                    pluginProgramPackageFilePath);
            LOGGER.error(logMsg, e);
            throw new Exception("failed to load plugin");
        }

        try {
            startPluginIdentifiedByPluginId(pluginId);
        } catch (Exception e) {
            String logMsg = String.format("failed to start plugin %s retrieved from %s", pluginId,
                    pluginProgramPackageFilePath);
            LOGGER.error(logMsg, e);
            throw new Exception("failed to start plugin");
        }

        return pluginId;
    }

    public String loadPluginFromPath(String pluginProgramPackageFilePath) throws Exception {
        // 并发控制，避免同时操作同一个 plugin
        String pluginId = null;
        synchronized (this.lock) {
            try {
                pluginId = this.pluginManager.loadPlugin(
                        Paths.get(pluginProgramPackageFilePath));

                return pluginId;
            } catch (PluginAlreadyLoadedException e1) {
                PluginWrapper pluginWrapper = this.pluginManager.getPlugin(e1.getPluginId());
                if (pluginWrapper == null) {
                    String logMsg = String.format("found conflict while loading plugin at %s, found an already loaded " +
                                    "plugin %s at %s, but cannot get this plugin",
                            pluginProgramPackageFilePath, e1.getPluginId(), e1.getPluginPath());
                    LOGGER.error(logMsg, e1);
                    throw new Exception(String.format("failed to load plugin"));
                } else {
                    String logMsg = String.format("found an already loaded plugin at %s, it is %s at %s and the " +
                                    "plugin state is %s",
                            pluginProgramPackageFilePath, e1.getPluginId(), e1.getPluginPath(),
                            pluginWrapper.getPluginState());
                    LOGGER.info(logMsg);

                    return pluginWrapper.getPluginId();
                }
            } catch (PluginRuntimeException e2) {
                String logMsg = String.format("failed to load plugin from %s", pluginProgramPackageFilePath);
                LOGGER.error(logMsg, e2);
                throw new Exception("failed to load plugin");
            }
        }
    }

    public void startPluginIdentifiedByPluginId(String pluginId) throws Exception {
        PluginWrapper pluginWrapper = this.pluginManager.getPlugin(pluginId);
        if (pluginWrapper == null) {
            String logMsg = String.format("cannot get this plugin %s", pluginId);
            LOGGER.error(logMsg);
            throw new Exception(String.format("failed to start plugin"));
        } else {
            if (PluginState.STARTED.equals(pluginWrapper.getPluginState())) {
                return;
            }
        }

        try {
            this.pluginManager.startPlugin(pluginId);
        } catch (PluginRuntimeException e) {
            String logMsg = String.format("failed to start plugin %s", pluginId);
            LOGGER.error(logMsg, e);
            throw new Exception("failed to start plugin");
        }
    }

    public PluginProfile parsePlugin
            (File programPackageFile,
             Class pluginInterfaceClass,
             UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        //
        // Step 2, core-processing
        //

        //
        // Step 2.1, 从指定文件中加载 Plugin extension processor (插件算子）
        //

        // PF4J 在 loadPlugin 时，如果发现已经有同一 pluginId 被 loaded，则会抛出异常。
        // PF4J 的官方解释是：Simultaneous loading of plugins with the same PluginId is not
        // currently supported.
        // 本系统约定 PluginId 由"<:传统的PluginId>-<:PluginVersion>"组成

        // 先通过文件路径检查该 plugin 是否已经 loaded
        String pluginId = null;
        List<PluginWrapper> loadedPlugins = this.pluginManager.getPlugins();
        if (!CollectionUtils.isEmpty(loadedPlugins)) {
            for (PluginWrapper loadedPlugin : loadedPlugins) {
                if (programPackageFile.toPath().equals(loadedPlugin.getPluginPath())) {
                    LOGGER.info("found existing loaded plugin at {}, the plugin id is {}, the plugin state is {}",
                            loadedPlugin.getPluginPath()
                            , loadedPlugin.getPluginId()
                            , loadedPlugin.getPluginState());
                    pluginId = loadedPlugin.getPluginId();
                    break;
                }
            }
        }

        // 是否本次 load
        boolean newLoaded = false;

        // 如果没有 loaded
        if (pluginId == null) {
            try {
                // 全新 load
                pluginId = this.pluginManager.loadPlugin(programPackageFile.toPath());
            } catch (Exception e) {
                LOGGER.error("failed to load plugin at {}", programPackageFile.getAbsolutePath(), e);
                throw new AbcResourceConflictException("failed to load plugin");
            }

            // 只有 started plugin 才能 get extensions，否则取不到任何 extension
            try {
                this.pluginManager.startPlugin(pluginId);
            } catch (Exception e) {
                LOGGER.error("failed to start plugin {} at {}", pluginId, programPackageFile.getAbsolutePath(), e);
                throw new AbcResourceConflictException("failed to start plugin");
            }

            // 校验 extension 是否遵循 the interface extension processor
            List extensions =
                    this.pluginManager.getExtensions(pluginInterfaceClass, pluginId);
            if (CollectionUtils.isEmpty(extensions)) {
                try {
                    this.pluginManager.stopPlugin(pluginId);
                    this.pluginManager.unloadPlugin(pluginId);
                } catch (Exception e) {
                    LOGGER.error("failed to stop and unload plugin {} loaded at {}", pluginId,
                            programPackageFile.getAbsolutePath(), e);
                    throw new AbcResourceConflictException("failed to stop and unload plugin");
                }

                LOGGER.error("cannot find extension of class {} from plugin {} loaded at {}",
                        pluginInterfaceClass.getName(), pluginId,
                        programPackageFile.getAbsolutePath());
                throw new AbcResourceConflictException("cannot find requested extension");
            }
            if (extensions.size() > 1) {
                this.pluginManager.stopPlugin(pluginId);
                this.pluginManager.unloadPlugin(pluginId);

                LOGGER.error("found out more than 1 extension of class {} from plugin {} loaded at {}",
                        pluginInterfaceClass.getName(), pluginId,
                        programPackageFile.getAbsolutePath());
                throw new AbcResourceConflictException("illegal extension");
            }

            newLoaded = true;
        }


        //
        // Step 2.2, 获取 plugin 的 metadata
        //
        PluginWrapper pluginWrapper = this.pluginManager.getPlugin(pluginId);
        if (pluginWrapper == null) {
            if (newLoaded) {
                try {
                    this.pluginManager.stopPlugin(pluginId);
                    this.pluginManager.unloadPlugin(pluginId);
                } catch (Exception e) {
                    LOGGER.error("failed to stop and unload plugin {} loaded at {}", pluginId,
                            programPackageFile.getAbsolutePath(), e);
                    throw new AbcResourceConflictException("failed to stop and unload plugin");
                }
            }

            LOGGER.error("cannot get plugin {}", pluginId);
            throw new AbcResourceConflictException("cannot get plugin");
        }
        PluginDescriptor pluginDescriptor = pluginWrapper.getDescriptor();
        PluginProfile pluginProfile = new PluginProfile();
        pluginProfile.setPluginId(pluginId);
        pluginProfile.setPluginClass(pluginDescriptor.getPluginClass());
        pluginProfile.setPluginDescription(pluginDescriptor.getPluginDescription());
        pluginProfile.setVersion(pluginDescriptor.getVersion());
        pluginProfile.setRequires(pluginDescriptor.getRequires());
        pluginProfile.setProvider(pluginDescriptor.getProvider());
        pluginProfile.setLicense(pluginDescriptor.getLicense());

        if (newLoaded) {
            try {
                this.pluginManager.stopPlugin(pluginId);
                this.pluginManager.unloadPlugin(pluginId);
            } catch (Exception e) {
                LOGGER.error("failed to stop and unload plugin {} loaded at {}", pluginId,
                        programPackageFile.getAbsolutePath(), e);
                throw new AbcResourceConflictException("failed to stop and unload plugin");
            }
        }

        return pluginProfile;
    }

    public PluginProfile parsePluginProfile(String pluginId) throws AbcUndefinedException {
        PluginWrapper pluginWrapper = this.pluginManager.getPlugin(pluginId);
        if (pluginWrapper == null) {
            LOGGER.error("cannot get plugin {}", pluginId);
            throw new AbcResourceConflictException("cannot get plugin");
        }
        PluginDescriptor pluginDescriptor = pluginWrapper.getDescriptor();
        PluginProfile pluginProfile = new PluginProfile();
        pluginProfile.setPluginId(pluginId);
        pluginProfile.setPluginClass(pluginDescriptor.getPluginClass());
        pluginProfile.setPluginDescription(pluginDescriptor.getPluginDescription());
        pluginProfile.setVersion(pluginDescriptor.getVersion());
        pluginProfile.setRequires(pluginDescriptor.getRequires());
        pluginProfile.setProvider(pluginDescriptor.getProvider());
        pluginProfile.setLicense(pluginDescriptor.getLicense());

        return pluginProfile;
    }

    @Scheduled(cron = "0 0/5 * * * ?")
    public void report() throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //

        long beginTime = System.currentTimeMillis();
        LOGGER.info("[plugin] begin to list the plugins on this instance");

        //
        // Step 2, core-processing
        //
        List<PluginWrapper> pluginWrapperList = this.pluginManager.getPlugins();
        if (CollectionUtils.isEmpty(pluginWrapperList)) {
            LOGGER.info("[plugin] no plugin found");
        } else {
            StringBuilder msg = new StringBuilder();
            pluginWrapperList.forEach(pluginWrapper -> {
                msg.append("\r\n")
                        .append("plugin_id=").append(pluginWrapper.getPluginId())
                        .append("plugin_state=").append(pluginWrapper.getPluginState())
                        .append("plugin_path=").append(pluginWrapper.getPluginPath().toString())
                        .append("plugin_version=").append(pluginWrapper.getDescriptor().getVersion());
            });
            LOGGER.info("[plugin] found plugins:{}", msg);
        }

        LOGGER.info("[plugin] end to list the plugins on this instance, duration:{}",
                AbcDateUtils.format(System.currentTimeMillis() - beginTime));
    }

}
