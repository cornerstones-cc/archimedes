package cc.cornerstones.archimedes.extensions.authentication;

import cc.cornerstones.archimedes.extensions.AuthenticationServiceProvider;
import cc.cornerstones.archimedes.extensions.authentication.uc.UcWorker;
import cc.cornerstones.archimedes.extensions.types.SignedInfo;
import com.alibaba.fastjson.JSONObject;
import org.pf4j.Extension;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class AuthenticationPlugin extends Plugin {
    private final static Logger LOGGER = LoggerFactory.getLogger(AuthenticationPlugin.class);

    public AuthenticationPlugin(PluginWrapper wrapper) {
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
    public static class AuthenticationServiceProviderImpl extends AuthenticationServiceProvider {
        private UcWorker worker;

        public AuthenticationServiceProviderImpl() {
            this.worker = new UcWorker();
        }

        @Override
        public SignedInfo signIn(JSONObject input, String configuration) throws Exception {
            return this.worker.signIn(input, configuration);
        }

        @Override
        public void signOut(JSONObject input, String configuration) throws Exception {
            this.worker.signOut(input, configuration);
        }

        @Override
        public String getConfigurationTemplate() throws Exception {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("extensions" +
                    "/uc_authentication_service_provider_configuration_template.xml");
            if (inputStream == null) {
                throw new Exception("cannot find resource");
            }
            return readContent(inputStream);
        }

        @Override
        public JSONObject getUserInfoSchema() throws Exception {
            return super.getUserInfoSchema();
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
