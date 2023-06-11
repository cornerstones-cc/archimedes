package cc.cornerstones.biz.share.assembly;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.biz.distributedtask.service.assembly.DistributedTaskExecutor;
import cc.cornerstones.biz.share.types.ResourceReferenceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class ResourceReferenceManager implements SmartInitializingSingleton, DisposableBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedTaskExecutor.class);

    @Autowired
    private ApplicationContext applicationContext;

    private ConcurrentMap<String, ResourceReferenceOperator> operatorCache = new ConcurrentHashMap<String,
            ResourceReferenceOperator>();

    public enum ResourceCategoryEnum {
        DATA_SOURCE,
        DATA_DICTIONARY,
        AUTHENTICATION_SERVICE_COMPONENT,
        DFS_SERVICE_COMPONENT,
        DATA_PERMISSION_SERVICE_COMPONENT,
        USER_SYNCHRONIZATION_SERVICE_COMPONENT,
        AUTHENTICATION_SERVICE_AGENT,
        DFS_SERVICE_AGENT,
        DATA_PERMISSION_SERVICE_AGENT,
        USER_SYNCHRONIZATION_SERVICE_AGENT,
        APP,
        ACCOUNT_TYPE;
    }

    public List<String> check(
            ResourceCategoryEnum resourceCategory,
            Long resourceUid,
            String resourceName) throws AbcUndefinedException {
        LOGGER.info("[resref] rcv assignment to check resource category {} and resource {} ({})",
                resourceCategory,
                resourceUid,
                resourceName);

        List<String> result = new LinkedList<>();
        this.operatorCache.forEach((handlerName, operator) -> {
            try {
                operator.init();
                Object object = operator.execute(
                        resourceCategory,
                        resourceUid,
                        resourceName);
                operator.destroy();

                if (!ObjectUtils.isEmpty(object)) {
                    if (object instanceof List) {
                        List list = (List) object;
                        result.addAll(list);
                    } else {
                        throw new AbcUndefinedException("unexpected return result type:" + object);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("[resref] failed to execute handler {}", handlerName, e);
            }
        });

        if (!CollectionUtils.isEmpty(result)) {
            return result;
        }

        return null;
    }

    @Override
    public void destroy() throws Exception {

    }

    @Override
    public void afterSingletonsInstantiated() {
        resolveAndRegisterAllHandlers();
    }

    private void resolveAndRegisterAllHandlers() {
        // init task handler from method
        String[] beanNames = this.applicationContext.getBeanNamesForType(Object.class, false, true);
        for (String beanName : beanNames) {
            Object bean = this.applicationContext.getBean(beanName);

            Map<Method, ResourceReferenceHandler> annotatedMethods = null;
            try {
                annotatedMethods = MethodIntrospector.selectMethods(bean.getClass(),
                        new MethodIntrospector.MetadataLookup<ResourceReferenceHandler>() {
                            @Override
                            public ResourceReferenceHandler inspect(Method method) {
                                return AnnotatedElementUtils.findMergedAnnotation(method, ResourceReferenceHandler.class);
                            }
                        });
            } catch (Throwable ex) {
                LOGGER.error("[resref] fail to resolve handler from bean {}", beanName, ex);
            }
            if (annotatedMethods == null || annotatedMethods.isEmpty()) {
                continue;
            }

            for (Map.Entry<Method, ResourceReferenceHandler> entry : annotatedMethods.entrySet()) {
                Method method = entry.getKey();
                ResourceReferenceHandler handler = entry.getValue();
                registerHandler(bean, method, handler);
            }
        }
    }

    private void registerHandler(Object bean, Method method, ResourceReferenceHandler handler) {
        //
        // Step 1, pre-processing
        //
        if (bean == null || method == null || handler == null) {
            return;
        }

        Class<?> clazz = bean.getClass();
        String methodName = method.getName();

        if (ObjectUtils.isEmpty(handler.name())) {
            throw new RuntimeException(String.format("[resref] illegal task handler name (null or empty) found in %s, %s",
                    clazz,
                    methodName));
        }

        if (this.operatorCache.containsKey(handler.name())) {
            throw new RuntimeException(String.format("[resref] illegal task handler name (duplicate) found in %s, %s, " +
                            "handler name is %s",
                    clazz,
                    methodName,
                    handler.name()));
        }

        //
        // Step 2, core-processing
        //
        //
        method.setAccessible(true);

        // init
        Method initMethod = null;
        if (!ObjectUtils.isEmpty(handler.init())) {
            try {
                initMethod = clazz.getDeclaredMethod(handler.init());
                initMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(String.format("[resref] illegal init property found in %s, %s, " +
                                "handler name is %s, init property is %s",
                        clazz,
                        methodName,
                        handler.name(),
                        handler.init()));
            }
        }

        // destroy
        Method destroyMethod = null;
        if (!ObjectUtils.isEmpty(handler.destroy())) {
            try {
                destroyMethod = clazz.getDeclaredMethod(handler.destroy());
                destroyMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(String.format("[resref] illegal destroy property found in %s, %s, " +
                                "handler name is %s, destroy property is %s",
                        clazz,
                        methodName,
                        handler.name(),
                        handler.destroy()));
            }
        }

        // build a builtin task operator
        ResourceReferenceBuiltinOperator builtinOperator = new ResourceReferenceBuiltinOperator(bean, method, initMethod,
                destroyMethod);
        this.operatorCache.put(handler.name(), builtinOperator);
        LOGGER.info("[resref] finish to register handler {} of {}", handler.name(), clazz);
    }
}
