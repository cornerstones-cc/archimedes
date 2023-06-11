package cc.cornerstones.archimedes.extensions.usync;

import cc.cornerstones.archimedes.extensions.UserSynchronizationServiceProvider;
import cc.cornerstones.archimedes.extensions.types.UserInfo;
import cc.cornerstones.archimedes.extensions.usync.uc.UcWorker;
import cc.cornerstones.archimedes.extensions.types.TreeNode;
import org.pf4j.Extension;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class UserSynchronizationPlugin extends Plugin {
    private final static Logger LOGGER = LoggerFactory.getLogger(UserSynchronizationPlugin.class);

    public UserSynchronizationPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    public void delete() {
        super.delete();
    }

    /**
     * @author bbottong
     */
    @Extension
    public static class UserSynchronizationServiceProviderImpl extends UserSynchronizationServiceProvider {
        private UcWorker worker;

        public UserSynchronizationServiceProviderImpl() {
            this.worker = new UcWorker();
        }

        @Override
        public String getConfigurationTemplate() throws Exception {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("extensions" +
                    "/uc_user_synchronization_service_provider_configuration_template.xml");
            if (inputStream == null) {
                throw new Exception("cannot find resource");
            }
            return readContent(inputStream);
        }

        @Override
        public List<UserInfo> listingQueryAllUsers(String configuration) throws Exception {
            return this.worker.listingQueryAllUsers(configuration);
        }
    }

    public static String readContent(InputStream inputStream) {
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
            inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            bufferedReader = new BufferedReader(inputStreamReader);
            return bufferedReader.lines().collect(Collectors.joining("\n"));
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                LOGGER.error("", e);
            }

            try {
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
            } catch (IOException e) {
                LOGGER.error("", e);
            }

            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                LOGGER.error("", e);
            }
        }
    }

    public static String readContent(File file) {
        FileInputStream fileInputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        StringBuilder sb = null;
        try {
            fileInputStream = new FileInputStream(file);

            inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);

            bufferedReader = new BufferedReader(inputStreamReader);

            sb = new StringBuilder();
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            LOGGER.error("", e);
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                LOGGER.error("", e);
            }

            try {
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
            } catch (IOException e) {
                LOGGER.error("", e);
            }

            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (IOException e) {
                LOGGER.error("", e);
            }
        }
        if (sb != null) {
            return sb.toString();
        }
        return null;
    }
}
