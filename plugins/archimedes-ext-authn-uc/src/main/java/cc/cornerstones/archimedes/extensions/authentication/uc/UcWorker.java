package cc.cornerstones.archimedes.extensions.authentication.uc;

import cc.cornerstones.archimedes.extensions.types.SignedInfo;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.PropertyNamingStrategy;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class UcWorker {
    private final static Logger LOGGER = LoggerFactory.getLogger(UcWorker.class);

    public SignedInfo signIn(JSONObject input, String configuration) throws Exception {
        //
        // Step 1, pre-processing
        //
        UcSignInDto ucSignInDto = JSONObject.toJavaObject(input, UcSignInDto.class);
        if (ucSignInDto.getAccounts() == null || ucSignInDto.getAccounts().isEmpty()) {
            throw new Exception("accounts should not be null or empty");
        }
        if (ucSignInDto.getPassword() == null || ucSignInDto.getPassword().isEmpty()) {
            throw new Exception("password should not be null or empty");
        }

        Configuration oktaConfiguration = parseConfiguration(configuration);

        //
        // Step 2, core-processing
        //

        //
        // get access token from okta sso service
        //
        UcAccessTokenRequestPayloadDto ucAccessTokenRequestPayloadDto =
                new UcAccessTokenRequestPayloadDto();
        ucAccessTokenRequestPayloadDto.setAccounts(ucSignInDto.getAccounts());
        ucAccessTokenRequestPayloadDto.setPassword(ucSignInDto.getPassword());
        ucAccessTokenRequestPayloadDto.setPsSalt(ucSignInDto.getPsSalt());
        UcAccessTokenResponsePayloadDto ucAccessTokenResponsePayloadDto =
                getAccessToken(ucAccessTokenRequestPayloadDto, oktaConfiguration);

        //
        // get user info from okta sso service
        //


        //
        // Step 3, post-processing
        //
        SignedInfo signedInfo = new SignedInfo();
        signedInfo.setAccountName(ucSignInDto.getAccounts());

        return signedInfo;
    }

    public UcAccessTokenResponsePayloadDto getAccessToken(
            UcAccessTokenRequestPayloadDto ucAccessTokenRequestPayloadDto,
            Configuration configuration) throws Exception {
        String serialNo = UUID.randomUUID().toString();
        LOGGER.info("serial no:{}, begin to request token_uri:{}, request payload:{}", serialNo,
                configuration.getTokenUri(),
                ucAccessTokenRequestPayloadDto);

        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(5000)
                .setSocketTimeout(15000)
                .build();

        HttpPost httpPost = new HttpPost(configuration.getTokenUri());
        httpPost.setConfig(config);
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        SerializeConfig serializeConfig = new SerializeConfig();
        serializeConfig.setPropertyNamingStrategy(PropertyNamingStrategy.SnakeCase);
        String requestPayload = JSONObject.toJSONString(ucAccessTokenRequestPayloadDto, serializeConfig,
                SerializerFeature.DisableCircularReferenceDetect);

        HttpEntity httpEntity = new StringEntity(requestPayload);
        httpPost.setEntity(httpEntity);

        CloseableHttpClient httpClient = HttpClients.createDefault();

        CloseableHttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.execute(httpPost);

            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode >= 200 && statusCode < 300) {
                HttpEntity entity = httpResponse.getEntity();
                if (entity != null) {
                    LOGGER.info("serial no:{}, done to request token_uri:{}, status line:{}, {}", serialNo,
                            configuration.getTokenUri(), httpResponse.getStatusLine().getStatusCode(),
                            httpResponse.getStatusLine().getReasonPhrase());

                    UcAccessTokenResponsePayloadDto ucAccessTokenResponsePayloadDto = JSONObject.parseObject(EntityUtils.toString(entity),
                            UcAccessTokenResponsePayloadDto.class);

                    return ucAccessTokenResponsePayloadDto;
                } else {
                    LOGGER.error("serial no:{}, failed to request token_uri:{}, status line:{}, {}, but found empty " +
                                    "entity", serialNo,
                            configuration.getTokenUri(), httpResponse.getStatusLine().getStatusCode(),
                            httpResponse.getStatusLine().getReasonPhrase());
                    throw new Exception("failed to request token");
                }
            } else {
                LOGGER.error("serial no:{}, failed to request token_uri:{}, status line:{}, {}", serialNo,
                        configuration.getTokenUri(), httpResponse.getStatusLine().getStatusCode(),
                        httpResponse.getStatusLine().getReasonPhrase());
                throw new Exception("failed to request token");
            }
        } catch (ClientProtocolException e) {
            LOGGER.error("serial no:{}, failed to request token_uri:{}", serialNo,
                    configuration.getTokenUri(), e);
            throw new Exception("failed to request token");
        } catch (IOException e) {
            LOGGER.error("serial no:{}, failed to request token_uri:{}", serialNo,
                    configuration.getTokenUri(), e);
            throw new Exception("failed to request token");
        } finally {
            if (httpResponse != null) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    LOGGER.error("failed to close http response", e);
                }
            }
            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    LOGGER.error("failed to close http client", e);
                }
            }
        }
    }

    public UcUserInfoResponsePayloadDto getUserInfo(
            String accessToken,
            Configuration configuration) throws Exception {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(5000)
                .setSocketTimeout(15000)
                .build();

        HttpGet httpGet = new HttpGet(configuration.getUserinfoUri());
        httpGet.setConfig(config);

        httpGet.addHeader("Authorization", "Bearer " + accessToken);

        String serialNo = UUID.randomUUID().toString();
        LOGGER.info("serial no:{}, begin to request userinfo_uri:{}, access token:{}",
                serialNo, configuration.getUserinfoUri(), accessToken);

        CloseableHttpClient httpClient = HttpClients.createDefault();

        try {
            UcUserInfoResponsePayloadDto responseBody =
                    httpClient.execute(httpGet, new ResponseHandler<UcUserInfoResponsePayloadDto>() {
                        @Override
                        public UcUserInfoResponsePayloadDto handleResponse(
                                HttpResponse httpResponse) throws ClientProtocolException, IOException {
                            int statusCode = httpResponse.getStatusLine().getStatusCode();
                            if (statusCode >= 200 && statusCode < 300) {
                                HttpEntity entity = httpResponse.getEntity();

                                if (entity == null) {
                                    LOGGER.error("serial no:{}, failed to request token_uri:{}, status line:{}-{}, " +
                                                    "empty entity",
                                            serialNo,
                                            configuration.getTokenUri(),
                                            httpResponse.getStatusLine().getStatusCode(),
                                            httpResponse.getStatusLine().getReasonPhrase());

                                    throw new IOException("failed to request userinfo");
                                } else {
                                    String entityBodyAsStr = EntityUtils.toString(entity);
                                    try {
                                        UcUserInfoResponsePayloadDto ucUserInfoResponsePayloadDto =
                                                JSONObject.parseObject(entityBodyAsStr,
                                                        UcUserInfoResponsePayloadDto.class);

                                        LOGGER.info("serial no:{}, done to request token_uri:{}, status line:{}-{}, " +
                                                        "entity:{}",
                                                serialNo,
                                                configuration.getTokenUri(),
                                                httpResponse.getStatusLine().getStatusCode(),
                                                httpResponse.getStatusLine().getReasonPhrase(),
                                                entityBodyAsStr);

                                        return ucUserInfoResponsePayloadDto;
                                    } catch (Exception e) {
                                        LOGGER.error("serial no:{}, failed to request token_uri:{}, status " +
                                                        "line:{}-{}, " +
                                                        "illegal entity format:{}",
                                                serialNo,
                                                configuration.getTokenUri(),
                                                httpResponse.getStatusLine().getStatusCode(),
                                                httpResponse.getStatusLine().getReasonPhrase(),
                                                entityBodyAsStr, e);

                                        throw new IOException("failed to request userinfo");
                                    }
                                }
                            } else {
                                LOGGER.error("serial no:{}, failed to request token_uri:{}, status line:{}-{}",
                                        serialNo,
                                        configuration.getTokenUri(),
                                        httpResponse.getStatusLine().getStatusCode(),
                                        httpResponse.getStatusLine().getReasonPhrase());

                                throw new IOException("failed to request userinfo");
                            }
                        }
                    });
            return responseBody;
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
    }

    private static Configuration parseConfiguration(String content) throws DocumentException {
        SAXReader saxReader = new SAXReader();
        Document document = saxReader.read(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));

        Element configurationElement = (Element) document.selectSingleNode("//configuration");
        if (configurationElement == null || configurationElement.elements().isEmpty()) {
            return null;
        }

        Configuration configuration = new Configuration();
        for (Element element : configurationElement.elements()) {
            if ("token-uri".equals(element.getName())) {
                if (element.attributes() != null && !element.attributes().isEmpty()) {
                    element.attributes().forEach(attribute -> {
                        if ("name".equals(attribute.getName())) {
                            configuration.setTokenUri(attribute.getValue());
                        }
                    });
                }
            } else if ("userinfo-uri".equals(element.getName())) {
                if (element.attributes() != null && !element.attributes().isEmpty()) {
                    element.attributes().forEach(attribute -> {
                        if ("name".equals(attribute.getName())) {
                            configuration.setUserinfoUri(attribute.getValue());
                        }
                    });
                }
            } else if ("username".equals(element.getName())) {
                if (element.attributes() != null && !element.attributes().isEmpty()) {
                    element.attributes().forEach(attribute -> {
                        if ("name".equals(attribute.getName())) {
                            configuration.setUsername(attribute.getValue());
                        }
                    });
                }
            }
        }

        return configuration;
    }

    public void signOut(JSONObject input, String configuration) throws Exception {

    }

    public JSONObject getUserInfoSchema() throws Exception {
        UcUserInfoResponsePayloadDto ucUserInfoResponsePayloadDto = new UcUserInfoResponsePayloadDto();

        return (JSONObject) JSONObject.toJSON(ucUserInfoResponsePayloadDto);
    }
}
