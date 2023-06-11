package cc.cornerstones.archimedes.extensions.datapermission.uc;

import cc.cornerstones.archimedes.extensions.types.TreeNode;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class UcWorker {
    private final static Logger LOGGER = LoggerFactory.getLogger(UcWorker.class);

    private static Configuration parseConfiguration(String content) throws DocumentException {
        SAXReader saxReader = new SAXReader();
        Document document = saxReader.read(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));

        Element configurationElement = (Element) document.selectSingleNode("//configuration");
        if (configurationElement == null || configurationElement.elements().isEmpty()) {
            return null;
        }

        Configuration configuration = new Configuration();
        for (Element element : configurationElement.elements()) {
            if ("resource-structure-list-uri".equals(element.getName())) {
                if (element.attributes() != null && !element.attributes().isEmpty()) {
                    element.attributes().forEach(attribute -> {
                        if ("name".equals(attribute.getName())) {
                            configuration.setResourceStructureListUri(attribute.getValue());
                        } else if ("username".equals(attribute.getName())) {
                            configuration.setUsername(attribute.getValue());
                        }
                    });
                }
            } else if ("resource-content-list-uri".equals(element.getName())) {
                if (element.attributes() != null && !element.attributes().isEmpty()) {
                    element.attributes().forEach(attribute -> {
                        if ("name".equals(attribute.getName())) {
                            configuration.setResourceContentListUri(attribute.getValue());
                        }
                    });
                }
            }
        }

        return configuration;
    }

    public List<TreeNode> treeListingAllNodesOfResourceCategoryHierarchy(
            String configuration) throws Exception {
        Configuration ucConfiguration = parseConfiguration(configuration);

        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(5000)
                .setSocketTimeout(15000)
                .build();

        HttpGet httpGet = new HttpGet(ucConfiguration.getResourceStructureListUri());
        httpGet.setConfig(config);

        httpGet.addHeader("X-USERNAME", ucConfiguration.getUsername());
        httpGet.addHeader("Content-Type", "application/json");

        String serialNo = UUID.randomUUID().toString();
        LOGGER.info("serial no:{}, begin to request resource structure list uri:{}, username:{}",
                serialNo, ucConfiguration.getResourceStructureListUri(), ucConfiguration.getUsername());

        CloseableHttpClient httpClient = HttpClients.createDefault();

        try {
            UcResponsePayloadDto responseBody =
                    httpClient.execute(httpGet, new ResponseHandler<UcResponsePayloadDto>() {
                        @Override
                        public UcResponsePayloadDto handleResponse(
                                HttpResponse httpResponse) throws ClientProtocolException, IOException {
                            int statusCode = httpResponse.getStatusLine().getStatusCode();
                            if (statusCode >= 200 && statusCode < 300) {
                                HttpEntity entity = httpResponse.getEntity();

                                if (entity == null) {
                                    LOGGER.error("serial no:{}, failed to request resource structure list uri:{}, status line:{}-{}, " +
                                                    "empty entity",
                                            serialNo,
                                            ucConfiguration.getResourceStructureListUri(),
                                            httpResponse.getStatusLine().getStatusCode(),
                                            httpResponse.getStatusLine().getReasonPhrase());

                                    throw new IOException("failed to request resource structure list uri");
                                } else {
                                    String entityBodyAsStr = EntityUtils.toString(entity);
                                    try {
                                        UcResponsePayloadDto ucResponsePayloadDto =
                                                JSONObject.parseObject(entityBodyAsStr,
                                                        UcResponsePayloadDto.class);

                                        LOGGER.info("serial no:{}, done to request resource structure list uri:{}, status line:{}-{}, " +
                                                        "entity:{}",
                                                serialNo,
                                                ucConfiguration.getResourceStructureListUri(),
                                                httpResponse.getStatusLine().getStatusCode(),
                                                httpResponse.getStatusLine().getReasonPhrase(),
                                                entityBodyAsStr);

                                        return ucResponsePayloadDto;
                                    } catch (Exception e) {
                                        LOGGER.error("serial no:{}, failed to request resource structure list uri:{}, status " +
                                                        "line:{}-{}, " +
                                                        "illegal entity format:{}",
                                                serialNo,
                                                ucConfiguration.getResourceStructureListUri(),
                                                httpResponse.getStatusLine().getStatusCode(),
                                                httpResponse.getStatusLine().getReasonPhrase(),
                                                entityBodyAsStr, e);

                                        throw new IOException("failed to request resource structure list uri");
                                    }
                                }
                            } else {
                                LOGGER.error("serial no:{}, failed to request resource structure list uri:{}, status line:{}-{}",
                                        serialNo,
                                        ucConfiguration.getResourceStructureListUri(),
                                        httpResponse.getStatusLine().getStatusCode(),
                                        httpResponse.getStatusLine().getReasonPhrase());

                                throw new IOException("failed to request resource structure list uri");
                            }
                        }
                    });

            TreeNode object = responseBody.getObject();
            if (object == null || object.getChildren() == null || object.getChildren().isEmpty()) {
                LOGGER.info("null object");
            } else {
                List<TreeNode> result = new LinkedList<>();
                object.getChildren().forEach(child -> {
                    TreeNode treeNode = new TreeNode();
                    treeNode.setIds(UUID.randomUUID().toString());
                    treeNode.setUid(child.getUid());
                    treeNode.setName(child.getName());
                    treeNode.setDescription(child.getDescription());
                    treeNode.setType("entity");
                    treeNode.setTags(child.getTags());
                    result.add(treeNode);
                });
                return result;
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    LOGGER.error("failed to close http client", e);
                }
            }
        }

        return null;
    }

    public TreeNode treeListingAllNodesOfResourceStructureHierarchy(
            Long resourceCategoryUid,
            String configuration) throws Exception {
        Configuration ucConfiguration = parseConfiguration(configuration);

        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(5000)
                .setSocketTimeout(15000)
                .build();

        HttpGet httpGet = new HttpGet(ucConfiguration.getResourceStructureListUri());
        httpGet.setConfig(config);

        httpGet.addHeader("X-USERNAME", ucConfiguration.getUsername());
        httpGet.addHeader("Content-Type", "application/json");

        String serialNo = UUID.randomUUID().toString();
        LOGGER.info("serial no:{}, begin to request resource structure list uri:{}, username:{}",
                serialNo, ucConfiguration.getResourceStructureListUri(), ucConfiguration.getUsername());

        CloseableHttpClient httpClient = HttpClients.createDefault();

        try {
            UcResponsePayloadDto responseBody =
                    httpClient.execute(httpGet, new ResponseHandler<UcResponsePayloadDto>() {
                        @Override
                        public UcResponsePayloadDto handleResponse(
                                HttpResponse httpResponse) throws ClientProtocolException, IOException {
                            int statusCode = httpResponse.getStatusLine().getStatusCode();
                            if (statusCode >= 200 && statusCode < 300) {
                                HttpEntity entity = httpResponse.getEntity();

                                if (entity == null) {
                                    LOGGER.error("serial no:{}, failed to request resource structure list uri:{}, status line:{}-{}, " +
                                                    "empty entity",
                                            serialNo,
                                            ucConfiguration.getResourceStructureListUri(),
                                            httpResponse.getStatusLine().getStatusCode(),
                                            httpResponse.getStatusLine().getReasonPhrase());

                                    throw new IOException("failed to request resource structure list uri");
                                } else {
                                    String entityBodyAsStr = EntityUtils.toString(entity);
                                    try {
                                        UcResponsePayloadDto ucResponsePayloadDto =
                                                JSONObject.parseObject(entityBodyAsStr,
                                                        UcResponsePayloadDto.class);

                                        LOGGER.info("serial no:{}, done to request resource structure list uri:{}, status line:{}-{}, " +
                                                        "entity:{}",
                                                serialNo,
                                                ucConfiguration.getResourceStructureListUri(),
                                                httpResponse.getStatusLine().getStatusCode(),
                                                httpResponse.getStatusLine().getReasonPhrase(),
                                                entityBodyAsStr);

                                        return ucResponsePayloadDto;
                                    } catch (Exception e) {
                                        LOGGER.error("serial no:{}, failed to request resource structure list uri:{}, status " +
                                                        "line:{}-{}, " +
                                                        "illegal entity format:{}",
                                                serialNo,
                                                ucConfiguration.getResourceStructureListUri(),
                                                httpResponse.getStatusLine().getStatusCode(),
                                                httpResponse.getStatusLine().getReasonPhrase(),
                                                entityBodyAsStr, e);

                                        throw new IOException("failed to request resource structure list uri");
                                    }
                                }
                            } else {
                                LOGGER.error("serial no:{}, failed to request resource structure list uri:{}, status line:{}-{}",
                                        serialNo,
                                        ucConfiguration.getResourceStructureListUri(),
                                        httpResponse.getStatusLine().getStatusCode(),
                                        httpResponse.getStatusLine().getReasonPhrase());

                                throw new IOException("failed to request resource structure list uri");
                            }
                        }
                    });

            TreeNode object = responseBody.getObject();
            if (object == null || object.getChildren() == null || object.getChildren().isEmpty()) {
                LOGGER.warn("null object");
            } else {
                for (TreeNode child : object.getChildren()) {
                    if (child.getUid().equals(resourceCategoryUid)) {
                        if (child.getChildren() == null || child.getChildren().isEmpty()) {
                            LOGGER.warn("null children in resource category {}", resourceCategoryUid);
                        }
                        return child.getChildren().get(0);
                    }
                }
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    LOGGER.error("failed to close http client", e);
                }
            }
        }

        return null;
    }

    public List<TreeNode> treeListingAllNodesOfResourceContentHierarchy(
            Long resourceCategoryUid,
            String configuration,
            String username) throws Exception {
        Configuration ucConfiguration = parseConfiguration(configuration);

        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(5000)
                .setSocketTimeout(15000)
                .build();

        StringBuilder url = new StringBuilder();
        url.append(ucConfiguration.getResourceContentListUri())
                .append("?")
                .append("username=").append(URLEncoder.encode(username, "UTF-8"))
                .append("&resource_category_uid=").append(resourceCategoryUid);

        HttpGet httpGet = new HttpGet(url.toString());
        httpGet.setConfig(config);

        httpGet.addHeader("X-USERNAME", ucConfiguration.getUsername());
        httpGet.addHeader("Content-Type", "application/json");

        String serialNo = UUID.randomUUID().toString();
        LOGGER.info("serial no:{}, begin to request resource structure list uri:{}, username:{}",
                serialNo, ucConfiguration.getResourceStructureListUri(), ucConfiguration.getUsername());

        CloseableHttpClient httpClient = HttpClients.createDefault();

        try {
            UcResponsePayloadDto responseBody =
                    httpClient.execute(httpGet, new ResponseHandler<UcResponsePayloadDto>() {
                        @Override
                        public UcResponsePayloadDto handleResponse(
                                HttpResponse httpResponse) throws ClientProtocolException, IOException {
                            int statusCode = httpResponse.getStatusLine().getStatusCode();
                            if (statusCode >= 200 && statusCode < 300) {
                                HttpEntity entity = httpResponse.getEntity();

                                if (entity == null) {
                                    LOGGER.error("serial no:{}, failed to request resource structure list uri:{}, status line:{}-{}, " +
                                                    "empty entity",
                                            serialNo,
                                            ucConfiguration.getResourceStructureListUri(),
                                            httpResponse.getStatusLine().getStatusCode(),
                                            httpResponse.getStatusLine().getReasonPhrase());

                                    throw new IOException("failed to request resource structure list uri");
                                } else {
                                    String entityBodyAsStr = EntityUtils.toString(entity);
                                    try {
                                        UcResponsePayloadDto ucResponsePayloadDto =
                                                JSONObject.parseObject(entityBodyAsStr,
                                                        UcResponsePayloadDto.class);

                                        LOGGER.info("serial no:{}, done to request resource structure list uri:{}, status line:{}-{}, " +
                                                        "entity:{}",
                                                serialNo,
                                                ucConfiguration.getResourceStructureListUri(),
                                                httpResponse.getStatusLine().getStatusCode(),
                                                httpResponse.getStatusLine().getReasonPhrase(),
                                                entityBodyAsStr);

                                        return ucResponsePayloadDto;
                                    } catch (Exception e) {
                                        LOGGER.error("serial no:{}, failed to request resource structure list uri:{}, status " +
                                                        "line:{}-{}, " +
                                                        "illegal entity format:{}",
                                                serialNo,
                                                ucConfiguration.getResourceStructureListUri(),
                                                httpResponse.getStatusLine().getStatusCode(),
                                                httpResponse.getStatusLine().getReasonPhrase(),
                                                entityBodyAsStr, e);

                                        throw new IOException("failed to request resource structure list uri");
                                    }
                                }
                            } else {
                                LOGGER.error("serial no:{}, failed to request resource structure list uri:{}, status line:{}-{}",
                                        serialNo,
                                        ucConfiguration.getResourceStructureListUri(),
                                        httpResponse.getStatusLine().getStatusCode(),
                                        httpResponse.getStatusLine().getReasonPhrase());

                                throw new IOException("failed to request resource structure list uri");
                            }
                        }
                    });

            TreeNode object = responseBody.getObject();
            if (object == null || object.getChildren() == null || object.getChildren().isEmpty()) {
                LOGGER.warn("null object");
            } else {
                for (TreeNode child : object.getChildren()) {
                    if (child.getUid().equals(resourceCategoryUid)) {
                        return child.getChildren();
                    }
                }
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    LOGGER.error("failed to close http client", e);
                }
            }
        }

        return null;
    }
}
