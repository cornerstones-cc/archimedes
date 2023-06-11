package cc.cornerstones.zero.configuration;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.exceptions.AbcAuthenticationException;
import cc.cornerstones.almond.exceptions.AbcResourceIntegrityException;
import cc.cornerstones.almond.exceptions.ControllerExceptionHandler;
import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.almond.utils.AbcFileUtils;
import cc.cornerstones.biz.authentication.service.assembly.AuthenticationHandler;
import org.apache.http.protocol.HTTP;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

@Component
public class AuthorizationHeaderInterceptor implements HandlerInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationHeaderInterceptor.class);

    @Autowired
    private AuthenticationHandler authenticationHandler;

    private List<String> AUTHORIZATION_IGNORE_LIST = null;

    private void init() throws Exception {
        AUTHORIZATION_IGNORE_LIST = new LinkedList<>();

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(
                "auth-ignore-list.xml");
        if (inputStream == null) {
            throw new AbcResourceIntegrityException("cannot find resource auth-ignore-list.xml");
        }

        String content = AbcFileUtils.readContent(inputStream);

        if (ObjectUtils.isEmpty(content)) {
            return;
        }

        SAXReader saxReader = new SAXReader();
        Document document = saxReader.read(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));

        Element configurationElement = (Element) document.selectSingleNode("//configuration");
        if (configurationElement == null || configurationElement.elements().isEmpty()) {
            return;
        }

        for (Element element : configurationElement.elements()) {
            if ("list".equals(element.getName())) {
                if (!CollectionUtils.isEmpty(element.elements())) {
                    for (Element childElement : element.elements()) {
                        if ("item".equals(childElement.getName())) {
                            String trim = childElement.getStringValue().trim();
                            if (!AUTHORIZATION_IGNORE_LIST.contains(trim)) {
                                AUTHORIZATION_IGNORE_LIST.add(trim);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //
        // Step 1, pre-processing
        //

        if (request.getMethod().equalsIgnoreCase(HttpMethod.OPTIONS.name())) {
            return true;
        }

        if (request.getServletPath().startsWith("/open-api/")) {
            return true;
        }

        if (AUTHORIZATION_IGNORE_LIST == null) {
            init();
        }

        //
        // Step 2, core-processing
        //
        if (AUTHORIZATION_IGNORE_LIST.isEmpty()
                || AUTHORIZATION_IGNORE_LIST.contains(request.getServletPath())) {
            return true;
        }

        String authorization = request.getHeader(NetworkingConstants.HEADER_AUTHORIZATION);
        if (ObjectUtils.isEmpty(authorization)) {
            LOGGER.warn("illegal authorization header: {}", authorization);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        String accessToken = null;
        try {
            accessToken = AbcApiUtils.extractAccessTokenFromAuthorizationHeader(
                    AbcApiUtils.BEARER_TOKEN_TYPE,
                    authorization);
        } catch (Exception e) {
            LOGGER.warn("illegal authorization header: {}", authorization);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        if (ObjectUtils.isEmpty(accessToken)) {
            LOGGER.warn("illegal authorization header: {}", authorization);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        try {
            this.authenticationHandler.validateAccessToken(accessToken);
        } catch (Exception e) {
            LOGGER.warn("illegal authorization header: {}", authorization);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
