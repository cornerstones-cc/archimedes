package cc.cornerstones.archimedes.extensions.usync.uc;

import cc.cornerstones.archimedes.extensions.types.UserInfo;
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
import java.nio.charset.StandardCharsets;
import java.util.*;

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

        configuration.setRoleMappingUnits(new LinkedList<>());
        configuration.setGroupMappingUnits(new LinkedList<>());
        configuration.setAccountTypeMappingUnits(new LinkedList<>());
        configuration.setExtendedPropertyMappingUnits(new LinkedList<>());

        for (Element element : configurationElement.elements()) {
            if ("user-list-uri".equals(element.getName())) {
                if (element.attributes() != null && !element.attributes().isEmpty()) {
                    element.attributes().forEach(attribute -> {
                        if ("name".equals(attribute.getName())) {
                            configuration.setUserListUri(attribute.getValue());
                        } else if ("username".equals(attribute.getName())) {
                            configuration.setUsername(attribute.getValue());
                        }
                    });
                }
            } else if ("role-mapping".equals(element.getName())) {
                if (element.elements() != null && !element.elements().isEmpty()) {
                    for (Element childElement : element.elements()) {
                        if ("item".equals(childElement.getName())) {
                            if (childElement.attributes() != null && !childElement.attributes().isEmpty()) {
                                RoleMappingUnit roleMappingUnit = new RoleMappingUnit();
                                childElement.attributes().forEach(attribute -> {
                                    if ("source-role-code".equals(attribute.getName())) {
                                        roleMappingUnit.setSourceRoleCode(attribute.getValue());
                                    } else if ("source-role-name".equals(attribute.getName())) {
                                        roleMappingUnit.setSourceRoleName(attribute.getValue());
                                    } else if ("target-role-uid".equals(attribute.getName())) {
                                        roleMappingUnit.setTargetRoleUid(Long.valueOf(attribute.getValue()));
                                    } else if ("target-role-name".equals(attribute.getName())) {
                                        roleMappingUnit.setTargetRoleName(attribute.getValue());
                                    }
                                });

                                configuration.getRoleMappingUnits().add(roleMappingUnit);
                            }
                        }
                    }
                }
            } else if ("group-mapping".equals(element.getName())) {
                if (element.elements() != null && !element.elements().isEmpty()) {
                    for (Element childElement : element.elements()) {
                        if ("item".equals(childElement.getName())) {
                            if (childElement.attributes() != null && !childElement.attributes().isEmpty()) {
                                GroupMappingUnit groupMappingUnit = new GroupMappingUnit();
                                childElement.attributes().forEach(attribute -> {
                                    if ("source-group-code".equals(attribute.getName())) {
                                        groupMappingUnit.setSourceGroupCode(attribute.getValue());
                                    } else if ("source-group-name".equals(attribute.getName())) {
                                        groupMappingUnit.setSourceGroupName(attribute.getValue());
                                    } else if ("target-group-uid".equals(attribute.getName())) {
                                        groupMappingUnit.setTargetGroupUid(Long.valueOf(attribute.getValue()));
                                    } else if ("target-group-name".equals(attribute.getName())) {
                                        groupMappingUnit.setTargetGroupName(attribute.getValue());
                                    }
                                });

                                configuration.getGroupMappingUnits().add(groupMappingUnit);
                            }
                        }
                    }
                }
            } else if ("account-type-mapping".equals(element.getName())) {
                if (element.elements() != null && !element.elements().isEmpty()) {
                    for (Element childElement : element.elements()) {
                        if ("item".equals(childElement.getName())) {
                            if (childElement.attributes() != null && !childElement.attributes().isEmpty()) {
                                AccountTypeMappingUnit accountTypeMappingUnit = new AccountTypeMappingUnit();
                                childElement.attributes().forEach(attribute -> {
                                    if ("name".equals(attribute.getName())) {
                                        accountTypeMappingUnit.setName(attribute.getValue());
                                    } else if ("target-account-type-uid".equals(attribute.getName())) {
                                        accountTypeMappingUnit.setTargetAccountTypeUid(Long.valueOf(attribute.getValue()));
                                    } else if ("target-account-type-name".equals(attribute.getName())) {
                                        accountTypeMappingUnit.setTargetAccountTypeName(attribute.getValue());
                                    }
                                });

                                configuration.getAccountTypeMappingUnits().add(accountTypeMappingUnit);
                            }
                        }
                    }
                }
            } else if ("extended-property-mapping".equals(element.getName())) {
                if (element.elements() != null && !element.elements().isEmpty()) {
                    for (Element childElement : element.elements()) {
                        if ("item".equals(childElement.getName())) {
                            if (childElement.attributes() != null && !childElement.attributes().isEmpty()) {
                                ExtendedPropertyMappingUnit extendedPropertyMappingUnit = new ExtendedPropertyMappingUnit();
                                childElement.attributes().forEach(attribute -> {
                                    if ("name".equals(attribute.getName())) {
                                        extendedPropertyMappingUnit.setName(attribute.getValue());
                                    } else if ("target-extended-property-uid".equals(attribute.getName())) {
                                        extendedPropertyMappingUnit.setTargetExtendedPropertyUid(Long.valueOf(attribute.getValue()));
                                    } else if ("target-extended-property-name".equals(attribute.getName())) {
                                        extendedPropertyMappingUnit.setTargetExtendedPropertyName(attribute.getValue());
                                    }
                                });

                                configuration.getExtendedPropertyMappingUnits().add(extendedPropertyMappingUnit);
                            }
                        }
                    }
                }
            }
        }

        return configuration;
    }

    public List<UserInfo> listingQueryAllUsers(String configuration) throws Exception {
        Configuration objectiveConfiguration = parseConfiguration(configuration);

        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(5000)
                .setSocketTimeout(15000)
                .build();

        HttpGet httpGet = new HttpGet(objectiveConfiguration.getUserListUri());
        httpGet.setConfig(config);

        httpGet.addHeader("X-USERNAME", objectiveConfiguration.getUsername());
        httpGet.addHeader("Content-Type", "application/json");

        String serialNo = UUID.randomUUID().toString();
        LOGGER.info("serial no:{}, begin to request user list uri:{}, username:{}",
                serialNo, objectiveConfiguration.getUserListUri(), objectiveConfiguration.getUsername());

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
                                    LOGGER.error("serial no:{}, failed to request user list uri:{}, status " +
                                                    "line:{}-{}, " +
                                                    "empty entity",
                                            serialNo,
                                            objectiveConfiguration.getUserListUri(),
                                            httpResponse.getStatusLine().getStatusCode(),
                                            httpResponse.getStatusLine().getReasonPhrase());

                                    throw new IOException("failed to request user list uri");
                                } else {
                                    String entityBodyAsStr = EntityUtils.toString(entity);
                                    try {
                                        UcResponsePayloadDto ucResponsePayloadDto =
                                                JSONObject.parseObject(entityBodyAsStr,
                                                        UcResponsePayloadDto.class);

                                        LOGGER.info("serial no:{}, done to request user list uri:{}, status line:{}-{}, " +
                                                        "entity:{}",
                                                serialNo,
                                                objectiveConfiguration.getUserListUri(),
                                                httpResponse.getStatusLine().getStatusCode(),
                                                httpResponse.getStatusLine().getReasonPhrase(),
                                                entityBodyAsStr);

                                        return ucResponsePayloadDto;
                                    } catch (Exception e) {
                                        LOGGER.error("serial no:{}, failed to requestuser list uri:{}, status " +
                                                        "line:{}-{}, " +
                                                        "illegal entity format:{}",
                                                serialNo,
                                                objectiveConfiguration.getUserListUri(),
                                                httpResponse.getStatusLine().getStatusCode(),
                                                httpResponse.getStatusLine().getReasonPhrase(),
                                                entityBodyAsStr, e);

                                        throw new IOException("failed to request user list uri");
                                    }
                                }
                            } else {
                                LOGGER.error("serial no:{}, failed to request user list uri:{}, status line:{}-{}",
                                        serialNo,
                                        objectiveConfiguration.getUserListUri(),
                                        httpResponse.getStatusLine().getStatusCode(),
                                        httpResponse.getStatusLine().getReasonPhrase());

                                throw new IOException("failed to request user list uri");
                            }
                        }
                    });

            List<ApiResponseUser> object = responseBody.getObject();
            if (object == null || object.isEmpty()) {
                LOGGER.info("null object");
            } else {
                return transform(object, objectiveConfiguration);
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

    private List<UserInfo> transform(
            List<ApiResponseUser> apiResponseUserList,
            Configuration configuration) {
        //
        // Step 1, pre-processing
        //
        if (apiResponseUserList == null || apiResponseUserList.isEmpty()) {
            return null;
        }

        // role mapping
        Map<String, RoleMappingUnit> roleMappingUnitMap = new HashMap<>();
        if (configuration.getRoleMappingUnits() != null && !configuration.getRoleMappingUnits().isEmpty()) {
            configuration.getRoleMappingUnits().forEach(roleMappingUnit -> {
                roleMappingUnitMap.put(roleMappingUnit.getSourceRoleCode(), roleMappingUnit);
            });
        }

        // group mapping
        Map<String, GroupMappingUnit> groupMappingUnitMap = new HashMap<>();
        if (configuration.getGroupMappingUnits() != null && !configuration.getGroupMappingUnits().isEmpty()) {
            configuration.getGroupMappingUnits().forEach(groupMappingUnit -> {
                groupMappingUnitMap.put(groupMappingUnit.getSourceGroupCode(), groupMappingUnit);
            });
        }

        // account type mapping
        Map<String, AccountTypeMappingUnit> accountTypeMappingUnitMap = new HashMap<>();
        if (configuration.getAccountTypeMappingUnits() != null && !configuration.getAccountTypeMappingUnits().isEmpty()) {
            configuration.getAccountTypeMappingUnits().forEach(accountTypeMappingUnit -> {
                accountTypeMappingUnitMap.put(accountTypeMappingUnit.getName(), accountTypeMappingUnit);
            });
        }

        // extended property mapping
        Map<String, ExtendedPropertyMappingUnit> extendedPropertyMappingUnitMap = new HashMap<>();
        if (configuration.getExtendedPropertyMappingUnits() != null && !configuration.getExtendedPropertyMappingUnits().isEmpty()) {
            configuration.getExtendedPropertyMappingUnits().forEach(extendedPropertyMappingUnit -> {
                extendedPropertyMappingUnitMap.put(extendedPropertyMappingUnit.getName(), extendedPropertyMappingUnit);
            });
        }


        //
        // Step 2, core-processing
        //

        List<UserInfo> result = new LinkedList<>();

        apiResponseUserList.forEach(apiResponseUser -> {
            //
            // Role(s)
            //
            List<Long> roleUidList = new LinkedList<>();
            if (apiResponseUser.getRoleInfo() != null && !apiResponseUser.getRoleInfo().isEmpty()) {
                String[] roles = apiResponseUser.getRoleInfo().split(";");
                for (String role : roles) {
                    String[] roleSlices = role.split(",");
                    if (roleSlices.length == 2) {
                        String sourceRoleCode = roleSlices[0].trim();
                        String sourceRoleName = roleSlices[1].trim();

                        if (roleMappingUnitMap.containsKey(sourceRoleCode)) {
                            if (!roleUidList.contains(roleMappingUnitMap.get(sourceRoleCode).getTargetRoleUid())) {
                                roleUidList.add(roleMappingUnitMap.get(sourceRoleCode).getTargetRoleUid());
                            }
                        }
                    }
                }
            }
            if (roleUidList.isEmpty()) {
                // ignore this user, because it is not an expected role
                return;
            }

            UserInfo userInfo = new UserInfo();
            userInfo.setDisplayName(apiResponseUser.getName());
            userInfo.setRoleUidList(roleUidList);

            //
            // Account(s) & Extended property(ies)
            //

            // 玛氏工号
            if (apiResponseUser.getCode() != null && !apiResponseUser.getCode().isEmpty()) {
                // Account - 玛氏工号
                if (accountTypeMappingUnitMap.containsKey("玛氏工号")) {
                    if (userInfo.getAccounts() == null) {
                        userInfo.setAccounts(new LinkedList<>());
                    }

                    UserInfo.AccountInfo accountInfo = new UserInfo.AccountInfo();
                    accountInfo.setAccountTypeUid(accountTypeMappingUnitMap.get("玛氏工号").getTargetAccountTypeUid());
                    accountInfo.setAccountName(apiResponseUser.getCode());
                    userInfo.getAccounts().add(accountInfo);
                }

                // Extended property - 玛氏工号
                if (extendedPropertyMappingUnitMap.containsKey("玛氏工号")) {
                    if (userInfo.getExtendedProperties() == null) {
                        userInfo.setExtendedProperties(new LinkedList<>());
                    }

                    UserInfo.ExtendedPropertyInfo extendedPropertyInfo = new UserInfo.ExtendedPropertyInfo();
                    extendedPropertyInfo.setExtendedPropertyUid(extendedPropertyMappingUnitMap.get("玛氏工号").getTargetExtendedPropertyUid());
                    extendedPropertyInfo.setExtendedPropertyValue(apiResponseUser.getCode());
                    userInfo.getExtendedProperties().add(extendedPropertyInfo);
                }
            }

            // 玛氏邮箱
            if (apiResponseUser.getEmail() != null && !apiResponseUser.getEmail().isEmpty()) {
                // Account - 玛氏邮箱
                if (accountTypeMappingUnitMap.containsKey("玛氏邮箱")) {
                    if (userInfo.getAccounts() == null) {
                        userInfo.setAccounts(new LinkedList<>());
                    }

                    UserInfo.AccountInfo accountInfo = new UserInfo.AccountInfo();
                    accountInfo.setAccountTypeUid(accountTypeMappingUnitMap.get("玛氏邮箱").getTargetAccountTypeUid());
                    accountInfo.setAccountName(apiResponseUser.getEmail());
                    userInfo.getAccounts().add(accountInfo);
                }

                // Extended property - 玛氏邮箱
                if (extendedPropertyMappingUnitMap.containsKey("玛氏邮箱")) {
                    if (userInfo.getExtendedProperties() == null) {
                        userInfo.setExtendedProperties(new LinkedList<>());
                    }

                    UserInfo.ExtendedPropertyInfo extendedPropertyInfo = new UserInfo.ExtendedPropertyInfo();
                    extendedPropertyInfo.setExtendedPropertyUid(extendedPropertyMappingUnitMap.get("玛氏邮箱").getTargetExtendedPropertyUid());
                    extendedPropertyInfo.setExtendedPropertyValue(apiResponseUser.getEmail());
                    userInfo.getExtendedProperties().add(extendedPropertyInfo);
                }
            }

            // Mars AD Number
            if (apiResponseUser.getAdNumber() != null && !apiResponseUser.getAdNumber().isEmpty()) {
                // Account - Mars AD Number
                if (accountTypeMappingUnitMap.containsKey("Mars AD Number")) {
                    if (userInfo.getAccounts() == null) {
                        userInfo.setAccounts(new LinkedList<>());
                    }

                    UserInfo.AccountInfo accountInfo = new UserInfo.AccountInfo();
                    accountInfo.setAccountTypeUid(accountTypeMappingUnitMap.get("Mars AD Number").getTargetAccountTypeUid());
                    accountInfo.setAccountName(apiResponseUser.getAdNumber());
                    userInfo.getAccounts().add(accountInfo);
                }

                // Extended property - Mars AD Number
                if (extendedPropertyMappingUnitMap.containsKey("Mars AD Number")) {
                    if (userInfo.getExtendedProperties() == null) {
                        userInfo.setExtendedProperties(new LinkedList<>());
                    }

                    UserInfo.ExtendedPropertyInfo extendedPropertyInfo = new UserInfo.ExtendedPropertyInfo();
                    extendedPropertyInfo.setExtendedPropertyUid(extendedPropertyMappingUnitMap.get("Mars AD Number").getTargetExtendedPropertyUid());
                    extendedPropertyInfo.setExtendedPropertyValue(apiResponseUser.getAdNumber());
                    userInfo.getExtendedProperties().add(extendedPropertyInfo);
                }
            }

            if (userInfo.getRoleUidList() != null && !userInfo.getRoleUidList().isEmpty()) {
                result.add(userInfo);
            }
        });

        return result;
    }
}
