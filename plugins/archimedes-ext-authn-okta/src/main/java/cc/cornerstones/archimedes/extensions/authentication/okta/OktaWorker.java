package cc.cornerstones.archimedes.extensions.authentication.okta;

import cc.cornerstones.archimedes.extensions.types.SignedInfo;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
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

public class OktaWorker {
    private final static Logger LOGGER = LoggerFactory.getLogger(OktaWorker.class);

    public SignedInfo signIn(JSONObject input, String configuration) throws Exception {
        //
        // Step 1, pre-processing
        //
        OktaSignInDto oktaSignInDto = JSONObject.toJavaObject(input, OktaSignInDto.class);
        if (oktaSignInDto.getCode() == null || oktaSignInDto.getCode().isEmpty()) {
            throw new Exception("code should not be null or empty");
        }
        if (oktaSignInDto.getRedirectUri() == null || oktaSignInDto.getRedirectUri().isEmpty()) {
            throw new Exception("redirect_uri should not be null or empty");
        }

        Configuration oktaConfiguration = parseConfiguration(configuration);

        //
        // Step 2, core-processing
        //

        //
        // get access token from okta sso service
        //
        OktaAccessTokenRequestPayloadDto oktaAccessTokenRequestPayloadDto =
                new OktaAccessTokenRequestPayloadDto();
        oktaAccessTokenRequestPayloadDto.setCode(oktaSignInDto.getCode());
        oktaAccessTokenRequestPayloadDto.setRedirectUri(oktaSignInDto.getRedirectUri());
        OktaAccessTokenResponsePayloadDto oktaAccessTokenResponsePayloadDto =
                getAccessToken(oktaAccessTokenRequestPayloadDto, oktaConfiguration);

        //
        // get user info from okta sso service
        //
        OktaUserInfoResponsePayloadDto oktaUserInfoResponsePayloadDto =
                getUserInfo(oktaAccessTokenResponsePayloadDto.getAccessToken(), oktaConfiguration);

        //
        // Step 3, post-processing
        //
        SignedInfo signedInfo = new SignedInfo();
        signedInfo.setAccountName(oktaUserInfoResponsePayloadDto.getAdusername());
        signedInfo.setUserInfo((JSONObject) JSONObject.toJSON(oktaUserInfoResponsePayloadDto));

        return signedInfo;
    }

    public OktaAccessTokenResponsePayloadDto getAccessToken(
            OktaAccessTokenRequestPayloadDto oktaAccessTokenRequestPayloadDto,
            Configuration configuration) throws Exception {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(10000)
                .setConnectionRequestTimeout(10000)
                .setSocketTimeout(15000)
                .build();

        HttpPost httpPost = new HttpPost(configuration.getTokenUri());
        httpPost.setConfig(config);
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded;charset=utf-8");

        List<NameValuePair> nameValuePairList = new LinkedList<>();
        nameValuePairList.add(new BasicNameValuePair("client_id", configuration.getClientId()));
        nameValuePairList.add(new BasicNameValuePair("client_secret", configuration.getClientSecret()));
        nameValuePairList.add(new BasicNameValuePair("grant_type", configuration.getGrantType()));
        nameValuePairList.add(new BasicNameValuePair("redirect_uri", oktaAccessTokenRequestPayloadDto.getRedirectUri()));
        nameValuePairList.add(new BasicNameValuePair("code", oktaAccessTokenRequestPayloadDto.getCode()));

        String serialNo = UUID.randomUUID().toString();
        LOGGER.info("serial no:{}, begin to request token_uri:{}, request payload:{}", serialNo,
                configuration.getTokenUri(),
                nameValuePairList);

        HttpEntity httpEntity = new UrlEncodedFormEntity(nameValuePairList, "UTF-8");
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

                    OktaAccessTokenResponsePayloadDto oktaAccessTokenResponsePayloadDto = JSONObject.parseObject(EntityUtils.toString(entity),
                            OktaAccessTokenResponsePayloadDto.class);

                    return oktaAccessTokenResponsePayloadDto;
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

    public OktaUserInfoResponsePayloadDto getUserInfo(
            String accessToken,
            Configuration configuration) throws Exception {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(10000)
                .setConnectionRequestTimeout(10000)
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
            OktaUserInfoResponsePayloadDto responseBody =
                    httpClient.execute(httpGet, new ResponseHandler<OktaUserInfoResponsePayloadDto>() {
                        @Override
                        public OktaUserInfoResponsePayloadDto handleResponse(
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
                                        OktaUserInfoResponsePayloadDto oktaUserInfoResponsePayloadDto =
                                                JSONObject.parseObject(entityBodyAsStr,
                                                        OktaUserInfoResponsePayloadDto.class);

                                        LOGGER.info("serial no:{}, done to request token_uri:{}, status line:{}-{}, " +
                                                        "entity:{}",
                                                serialNo,
                                                configuration.getTokenUri(),
                                                httpResponse.getStatusLine().getStatusCode(),
                                                httpResponse.getStatusLine().getReasonPhrase(),
                                                entityBodyAsStr);

                                        return oktaUserInfoResponsePayloadDto;
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
            if ("client".equals(element.getName())) {
                if (element.attributes() != null && !element.attributes().isEmpty()) {
                    element.attributes().forEach(attribute -> {
                        if ("id".equals(attribute.getName())) {
                            configuration.setClientId(attribute.getValue());
                        } else if ("secret".equals(attribute.getName())) {
                            configuration.setClientSecret(attribute.getValue());
                        }
                    });
                }
            } else if ("grant-type".equals(element.getName())) {
                if (element.attributes() != null && !element.attributes().isEmpty()) {
                    element.attributes().forEach(attribute -> {
                        if ("name".equals(attribute.getName())) {
                            configuration.setGrantType(attribute.getValue());
                        }
                    });
                }
            } else if ("token-uri".equals(element.getName())) {
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
            }
        }

        return configuration;
    }

    public void signOut(JSONObject input, String configuration) throws Exception {

    }

    public JSONObject getUserInfoSchema() throws Exception {
        OktaUserInfoResponsePayloadDto oktaUserInfoResponsePayloadDto = new OktaUserInfoResponsePayloadDto();
        oktaUserInfoResponsePayloadDto.setName("placeholder");
        oktaUserInfoResponsePayloadDto.setPreferredUsername("placeholder");
        oktaUserInfoResponsePayloadDto.setAdusername("placeholder");

        return (JSONObject) JSONObject.toJSON(oktaUserInfoResponsePayloadDto);
    }
}
