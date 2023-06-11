package cc.cornerstones.biz.administration.usermanagement.service.assembly;

import cc.cornerstones.almond.constants.InfrastructureConstants;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.administration.usermanagement.entity.ApiDo;
import cc.cornerstones.biz.administration.usermanagement.persistence.ApiRepository;
import cc.cornerstones.biz.administration.usermanagement.share.types.SwaggerApiMetadata;
import cc.cornerstones.biz.share.event.EventBusManager;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.*;

@Component
public class ApiResolver implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiResolver.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private ApiRepository apiRepository;

    @Value("${server.port}")
    private Integer serverPort;

    /**
     * Handle an application event.
     *
     * @param event the event to respond to
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        StringBuilder swaggerUrl = new StringBuilder();
        swaggerUrl.append("http://localhost:").append(serverPort).append("/v3/api-docs");

        List<SwaggerApiMetadata> swaggerApiMetadata = loadAvailableApis(swaggerUrl.toString());
        persist(swaggerApiMetadata);
    }

    private List<SwaggerApiMetadata> loadAvailableApis(String url) {
        OpenAPI openApi = new OpenAPIV3Parser().read(url);

        if (openApi != null
                && !CollectionUtils.isEmpty(openApi.getPaths())) {
            List<SwaggerApiMetadata> result = new ArrayList<>();
            openApi.getPaths().forEach((key, path) -> {
                SwaggerApiMetadata item = null;
                if (path.getGet() != null) {
                    item = buildFromOperation(
                            "GET", key, path.getGet());
                }
                if (path.getPost() != null) {
                    item = buildFromOperation(
                            "POST", key, path.getPost());
                }
                if (path.getPut() != null) {
                    item = buildFromOperation(
                            "PUT", key, path.getPut());
                }
                if (path.getPatch() != null) {
                    item = buildFromOperation(
                            "PATCH", key, path.getPatch());
                }
                if (path.getDelete() != null) {
                    item = buildFromOperation(
                            "DELETE", key, path.getDelete());
                }

                if (item != null) {
                    result.add(item);
                }
            });
            return result;
        }

        return null;
    }

    private SwaggerApiMetadata buildFromOperation(
            String method,
            String key,
            Operation operation) {
        SwaggerApiMetadata swaggerApiMetadata = new SwaggerApiMetadata();
        swaggerApiMetadata.setSummary(operation.getSummary());
        swaggerApiMetadata.setMethod(method);
        swaggerApiMetadata.setUri(key);
        if (!CollectionUtils.isEmpty(operation.getTags())) {
            // 只取第1个 tag
            swaggerApiMetadata.setTag(operation.getTags().get(0));
        }

        return swaggerApiMetadata;
    }

    private void persist(List<SwaggerApiMetadata> swaggerApiMetadataList) {
        //
        // Step 1, pre-processing
        //
        List<ApiDo> existingItemDoList = this.apiRepository.findAll();
        Map<String, ApiDo> existingItemDoMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(existingItemDoList)) {
            existingItemDoList.forEach(existingItemDo -> {
                existingItemDoMap.put(existingItemDo.getUri() + " " + existingItemDo.getMethod(), existingItemDo);
            });
        }

        List<SwaggerApiMetadata> inputItemList = swaggerApiMetadataList;
        Map<String, SwaggerApiMetadata> inputItemMap = new HashMap();
        if (!CollectionUtils.isEmpty(inputItemList)) {
            for (int i = 0; i < inputItemList.size(); i++) {
                SwaggerApiMetadata inputItem = inputItemList.get(i);
                inputItemMap.put(inputItem.getUri() + " " + inputItem.getMethod(), inputItem);
            }
        }

        List<ApiDo> toAddItemDoList = new LinkedList<>();
        List<ApiDo> toUpdateItemDoList = new LinkedList<>();
        List<ApiDo> toDeleteItemDoList = new LinkedList<>();

        //
        // Step 2, core-processing
        //
        existingItemDoMap.forEach((key, existingItemDo) -> {
            if (inputItemMap.containsKey(key)) {
                // existing 有，input 有
                // 可能是更新

                SwaggerApiMetadata inputItem = inputItemMap.get(key);

                boolean requiredUpdate = false;

                if (!ObjectUtils.isEmpty(inputItem.getSummary())
                        && !inputItem.getSummary().equals(existingItemDo.getSummary())) {
                    existingItemDo.setSummary(inputItem.getSummary());
                    requiredUpdate = true;
                }

                if (!ObjectUtils.isEmpty(inputItem.getTag())
                        && !inputItem.getTag().equals(existingItemDo.getTag())) {
                    existingItemDo.setTag(inputItem.getTag());
                    requiredUpdate = true;
                }

                if (requiredUpdate) {
                    BaseDo.update(existingItemDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
                    toUpdateItemDoList.add(existingItemDo);
                }

            } else {
                // existing 有，input 没有
                // 删除
                existingItemDo.setDeleted(Boolean.TRUE);
                BaseDo.update(existingItemDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
                toDeleteItemDoList.add(existingItemDo);
            }
        });

        inputItemMap.forEach((key, inputItem) -> {
            if (!existingItemDoMap.containsKey(key)) {
                // input 有，existing 没有
                // 新增
                ApiDo newItemDo = new ApiDo();
                newItemDo.setUid(this.idHelper.getNextDistributedId(ApiDo.RESOURCE_NAME));
                newItemDo.setSummary(inputItem.getSummary());
                newItemDo.setMethod(inputItem.getMethod());
                newItemDo.setUri(inputItem.getUri());
                newItemDo.setTag(inputItem.getTag());
                BaseDo.create(newItemDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
                toAddItemDoList.add(newItemDo);
            }
        });

        if (!CollectionUtils.isEmpty(toAddItemDoList)) {
            this.apiRepository.saveAll(toAddItemDoList);
        }
        if (!CollectionUtils.isEmpty(toUpdateItemDoList)) {
            this.apiRepository.saveAll(toUpdateItemDoList);
        }
        if (!CollectionUtils.isEmpty(toDeleteItemDoList)) {
            this.apiRepository.saveAll(toDeleteItemDoList);
        }

        //
        // Step 3, post-processing
        //
    }

}
