package cc.cornerstones.almond.utils;

import cc.cornerstones.almond.exceptions.AbcAuthorizationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author bbottong
 */
public class AbcApiUtils {
    public static final String BEARER_TOKEN_TYPE = "Bearer";

    public static final String OAUTH_GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";
    public static final String OAUTH_GRANT_TYPE_REFRESH_TOKEN = "refresh_token";

    public static final String PAGE_PARAMETER_NAME = "page";
    public static final String SIZE_PARAMETER_NAME = "size";
    public static final String SORT_PARAMETER_NAME = "sort";
    public static final String ACCESS_TOKEN_PARAMETER_NAME = "access_token";
    public static final String CLIENT_ID_PARAMETER_NAME = "client_id";
    public static final String USERNAME_PARAMETER_NAME = "username";
    public static final String TOKEN_TYPE_BEARER = "Bearer";

    /**
     * 拷贝一个Map<String, String[]>集合
     *
     * 在处理HttpServletRequest时，如果直接改动parameterMap，则会报错
     * "No modifications are allowed to a locked ParameterMap"
     * 则需要复制一个new parameter map，然后再在这个new parameter map上面进行改动
     *
     * @param parameterMap
     * @return
     */
    public static Map<String, String[]> copy(Map<String, String[]> parameterMap) {
        Map<String, String[]> newParameterMap = new HashMap<>();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            newParameterMap.put(entry.getKey(), entry.getValue());
        }
        return newParameterMap;
    }

    /**
     * 从authorization header中按照指定token type提取token
     *
     * @param authorization
     * @return
     * @throws Exception
     */
    public static String extractAccessTokenFromAuthorizationHeader(
            String tokenType,
            String authorization) throws Exception {

        String tokenTypeWithSpace = tokenType + " ";
        authorization = authorization.trim();
        if (authorization.length() <= tokenTypeWithSpace.length()) {
            throw new AbcAuthorizationException("illegal authorization header");
        }
        String accessTokenType = authorization.substring(0, tokenTypeWithSpace.length());
        if (!accessTokenType.equals(tokenTypeWithSpace)) {
            throw new AbcAuthorizationException("illegal authorization header");
        }
        String accessToken = authorization.substring(tokenTypeWithSpace.length()).trim();

        return accessToken;
    }

    public static Pageable transformPageable(Pageable pageable) {
        // pageable 中的 sort 特殊处理
        if (pageable != null && pageable.getSort() != null && pageable.getSort().isSorted()) {
            Sort sourceSort = pageable.getSort();
            List<Sort.Order> targetOrderList = new LinkedList<>();
            sourceSort.forEach(sourceOrder -> {
                String property = sourceOrder.getProperty();
                // 下划线转驼峰
                String transformedProperty = AbcStringUtils.snakeCaseToCamelCase(property);

                Sort.Order targetOrder = null;
                if (sourceOrder.isAscending()) {
                    targetOrder = Sort.Order.asc(transformedProperty);
                } else if (sourceOrder.isDescending()) {
                    targetOrder = Sort.Order.desc(transformedProperty);
                }

                targetOrderList.add(targetOrder);
            });
            Sort targetSort = Sort.by(targetOrderList);

            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), targetSort);
        }

        return pageable;
    }

    public static Sort transformSort(Sort sort) {
        // pageable 中的 sort 特殊处理
        if (sort != null && sort.isSorted()) {
            List<Sort.Order> targetOrderList = new LinkedList<>();
            sort.forEach(sourceOrder -> {
                String property = sourceOrder.getProperty();
                // 下划线转驼峰
                String transformedProperty = AbcStringUtils.snakeCaseToCamelCase(property);

                Sort.Order targetOrder = null;
                if (sourceOrder.isAscending()) {
                    targetOrder = Sort.Order.asc(transformedProperty);
                } else if (sourceOrder.isDescending()) {
                    targetOrder = Sort.Order.desc(transformedProperty);
                }

                targetOrderList.add(targetOrder);
            });
            Sort targetSort = Sort.by(targetOrderList);

            return targetSort;
        }

        return sort;
    }
}
