package cc.cornerstones.zero.startup;

import cc.cornerstones.almond.constants.InfrastructureConstants;
import cc.cornerstones.almond.types.*;
import cc.cornerstones.arbutus.lock.entity.LockDo;
import cc.cornerstones.arbutus.lock.persistence.LockRepository;
import cc.cornerstones.biz.administration.usermanagement.entity.UserBasicDo;
import cc.cornerstones.biz.administration.usermanagement.entity.UserCredentialDo;
import cc.cornerstones.biz.administration.usermanagement.persistence.UserBasicRepository;
import cc.cornerstones.biz.administration.usermanagement.persistence.UserCredentialRepository;
import cc.cornerstones.biz.administration.usermanagement.service.inf.PermissionsService;
import cc.cornerstones.biz.administration.usermanagement.share.constants.UserTypeEnum;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 初始化数据
 *
 * @author bbottong
 */
@Component
public class DataInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private LockRepository lockRepository;

    @Autowired
    private UserBasicRepository userBasicRepository;

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Autowired
    private PermissionsService permissionsService;

    public void execute() throws Exception {
        UserProfile operatingUserProfile = new UserProfile();
        operatingUserProfile.setUid(InfrastructureConstants.ROOT_USER_UID);
        operatingUserProfile.setDisplayName(InfrastructureConstants.ROOT_USER_DISPLAY_NAME);

        LOGGER.info("begin to create root user");
        createRootUser(operatingUserProfile);
        LOGGER.info("end to create root user");

        LOGGER.info("begin to create navigation menus");
        createNavigationMenus(operatingUserProfile);
        LOGGER.info("end to create navigation menus");
    }

    private void createRootUser(UserProfile operatingUserProfile) throws Exception {
        //
        // Step 1, pre-processing
        //
        // 先要获取悲观锁
        LOGGER.info("[root-user] begin to get lock");
        try {
            // 获取悲观锁
            LockDo lockDo = new LockDo();
            lockDo.setName("Create root user");
            lockDo.setResource("NA");
            lockDo.setVersion(1L);
            lockDo.setCreatedTimestamp(LocalDateTime.now());
            lockDo.setLastModifiedTimestamp(LocalDateTime.now());
            this.lockRepository.save(lockDo);
            LOGGER.info("[root-user] end to get lock");
        } catch (Exception e) {
            LOGGER.info("[root-user] fail to get lock");
            return;
        }

        try {
            //
            // Step 2, core-processing
            //
            UserBasicDo userBasicDo = this.userBasicRepository.findByUid(InfrastructureConstants.ROOT_USER_UID);
            if (userBasicDo == null) {
                userBasicDo = new UserBasicDo();
                userBasicDo.setUid(InfrastructureConstants.ROOT_USER_UID);
                userBasicDo.setDisplayName(InfrastructureConstants.ROOT_USER_DISPLAY_NAME);
                userBasicDo.setType(UserTypeEnum.PERSONAL);
                userBasicDo.setEnabled(Boolean.TRUE);
                BaseDo.create(userBasicDo, operatingUserProfile.getUid(), LocalDateTime.now());
                this.userBasicRepository.save(userBasicDo);

                // create user credential
                UserCredentialDo userCredentialDo = new UserCredentialDo();
                userCredentialDo.setUserUid(userBasicDo.getUid());
                userCredentialDo.setCredential(DigestUtils.sha256Hex(DigestUtils.sha1Hex(InfrastructureConstants.ROOT_USER_DISPLAY_NAME)));
                BaseDo.create(userCredentialDo, userBasicDo.getUid(), LocalDateTime.now());
                this.userCredentialRepository.save(userCredentialDo);

                LOGGER.info("[root-user] created root user");
            }
        } finally {
            //
            // Step 3, post-processing
            //
            // 释放悲观锁
            LockDo lockDo = this.lockRepository.findByNameAndResourceAndVersion(
                    "Create root user", "NA", 1L);
            if (lockDo != null) {
                this.lockRepository.delete(lockDo);
            }
        }
    }

    private void createNavigationMenus(UserProfile operatingUserProfile) throws Exception {
        //
        // Step 1, pre-processing
        //
        // 先要获取悲观锁
        LOGGER.info("[nav-menu] begin to get lock");
        try {
            // 获取悲观锁
            LockDo lockDo = new LockDo();
            lockDo.setName("Create navigation menus");
            lockDo.setResource("NA");
            lockDo.setVersion(1L);
            lockDo.setCreatedTimestamp(LocalDateTime.now());
            lockDo.setLastModifiedTimestamp(LocalDateTime.now());
            this.lockRepository.save(lockDo);
            LOGGER.info("[nav-menu] end to get lock");
        } catch (Exception e) {
            LOGGER.info("[nav-menu] fail to get lock");
            return;
        }

        try {
            //
            // Step 2, core-processing
            //
            List<TreeNode> allNavigationMenus =
                    this.permissionsService.treeListingAllNodesOfNavigationMenuHierarchy(operatingUserProfile);
            if (CollectionUtils.isEmpty(allNavigationMenus)) {
                // level 1 menus:
                createEntityNavigationMenu("Explore", "Explore",
                        "/explore", "Explore", null,
                        operatingUserProfile);
                Long buildUid = createDirectoryNavigationMenu("Build", "Build",
                        null, operatingUserProfile);
                Long operationsUid = createDirectoryNavigationMenu("Operations", "Operations",
                        null, operatingUserProfile);
                Long administrationUid = createDirectoryNavigationMenu("Administration", "Administration",
                        null,
                        operatingUserProfile);

                // level 2 menus: build
                createEntityNavigationMenu("Data Facets", "Data Facets",
                        "/data-facets", "DataFacets",
                        buildUid,
                        operatingUserProfile);
                createEntityNavigationMenu("Dictionaries", "Dictionaries",
                        "/dictionaries", "Dictionaries",
                        buildUid,
                        operatingUserProfile);
                Long appsUid = createDirectoryNavigationMenu("Apps", "Apps",
                        buildUid,
                        operatingUserProfile);
                createEntityNavigationMenu("App creation", "App creation",
                        "/app-creation", "AppManage",
                        appsUid,
                        operatingUserProfile);
                createEntityNavigationMenu("Specify data facets for apps", "Specify data facets for apps",
                        "/specify-data-facets-for-apps", "SpecifyDataFacetsForApps",
                        appsUid,
                        operatingUserProfile);
                createEntityNavigationMenu("Grant user access to apps", "Grant user access to apps",
                        "/grant-user-access-to-apps", "GrantUserAccessToApps",
                        appsUid, operatingUserProfile);
                createEntityNavigationMenu("Grant openapi access to apps", "Grant openapi access to apps",
                        "/grant-openapi-access-to-apps", "GrantOpenapiAccessToApps",
                        appsUid,
                        operatingUserProfile);

                // level 2 menus: operations
                createEntityNavigationMenu("Statistical analysis", "Statistical analysis",
                        "/statistical-analysis", "StatisticalAnalysis",
                        operationsUid,
                        operatingUserProfile);
                createEntityNavigationMenu("Access logs", "Access logs",
                        "/access-logs", "AccessLogsTable",
                        operationsUid,
                        operatingUserProfile);
                createEntityNavigationMenu("Performance logs", "Performance logs",
                        "/performance-logs", "SlowAccessLogsTable",
                        operationsUid,
                        operatingUserProfile);

                // level 2 menus: administration
                Long usersUid = createDirectoryNavigationMenu("Users", "Users",
                        administrationUid, operatingUserProfile);
                Long serviceConnectionUid = createDirectoryNavigationMenu("Service connection", "Service connection",
                        administrationUid,
                        operatingUserProfile);
                createEntityNavigationMenu("System settings", "System settings",
                        "/system-settings", "PageInfoConfig",
                        administrationUid,
                        operatingUserProfile);

                // level 3 menus: users
                createEntityNavigationMenu("Users", "Users",
                        "/users", "UserManage",
                        usersUid,
                        operatingUserProfile);
                createEntityNavigationMenu("Roles", "Roles",
                        "/roles", "SiderRoleManage",
                        usersUid,
                        operatingUserProfile);
                createEntityNavigationMenu("Groups", "Groups",
                        "/groups", "SiderGroupManage",
                        usersUid,
                        operatingUserProfile);
                createEntityNavigationMenu("Permissions", "Permissions",
                        "/permissions", "PermisssionManage",
                        usersUid,
                        operatingUserProfile);
                createEntityNavigationMenu("Settings", "Settings",
                        "/settings", "Settings",
                        usersUid,
                        operatingUserProfile);

                // level 3 menus: Service connection
                createEntityNavigationMenu("User synchronization service", "User synchronization service",
                        "/user-synchronization-service", "BaseProvidersManger",
                        serviceConnectionUid,
                        operatingUserProfile);
                createEntityNavigationMenu("Authentication service", "Authentication service",
                        "/authentication-service", "AuthenticationServiceProvider",
                        serviceConnectionUid,
                        operatingUserProfile);
                createEntityNavigationMenu("Distributed file system service (DFS)", "Distributed file system (DFS) " +
                                "service",
                        "/dfs-service", "DistributedFileSystemServiceProvider",
                        serviceConnectionUid,
                        operatingUserProfile);
                createEntityNavigationMenu("Data permission service", "Data permission service",
                        "/data-permission-service", "DataPermissionServiceProvider",
                        serviceConnectionUid,
                        operatingUserProfile);

                LOGGER.info("[menu] created menus");
            }
        } finally {
            //
            // Step 3, post-processing
            //
            // 释放悲观锁
            LockDo lockDo = this.lockRepository.findByNameAndResourceAndVersion(
                    "Create navigation menus", "NA", 1L);
            if (lockDo != null) {
                this.lockRepository.delete(lockDo);
            }
        }
    }

    private Long createDirectoryNavigationMenu(
            String name,
            String description,
            Long parentUid,
            UserProfile operatingUserProfile) {
        CreateDirectoryTreeNode createDirectoryTreeNode = new CreateDirectoryTreeNode();
        createDirectoryTreeNode.setName(name);
        createDirectoryTreeNode.setDescription(description);
        TreeNode treeNode = this.permissionsService.createDirectoryNodeForNavigationMenuHierarchy(
                parentUid,
                createDirectoryTreeNode,
                operatingUserProfile);
        return treeNode.getUid();
    }

    private Long createEntityNavigationMenu(
            String name,
            String description,
            String uri,
            String componentName,
            Long parentUid,
            UserProfile operatingUserProfile) {
        CreateEntityTreeNode createEntityTreeNode = new CreateEntityTreeNode();
        createEntityTreeNode.setName(name);
        createEntityTreeNode.setDescription(description);

        JSONObject payload = new JSONObject();
        payload.put("uri", uri);
        payload.put("componentName", componentName);

        createEntityTreeNode.setPayload(payload);
        TreeNode treeNode = this.permissionsService.createEntityNodeForNavigationMenuHierarchy(
                parentUid,
                createEntityTreeNode,
                operatingUserProfile);
        return treeNode.getUid();
    }
}
