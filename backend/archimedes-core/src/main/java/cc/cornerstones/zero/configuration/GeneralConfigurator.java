package cc.cornerstones.zero.configuration;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.*;

import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.stream.Collectors;

@Configuration
public class GeneralConfigurator {
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setThreadNamePrefix("TT-");
        threadPoolTaskScheduler.setPoolSize(10);
        return threadPoolTaskScheduler;
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        CloseableHttpClient httpClient = null;
        try {
            httpClient = HttpClients.custom().setSSLSocketFactory(getSSLSocketFactory()).build();
        } catch (Exception e) {
            throw new RuntimeException("fail to init restTemplate", e);
        }

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);

        Duration connectTimeoutDuration = Duration.ofSeconds(5);
        Duration readTimeoutDuration = Duration.ofSeconds(33);
        RestTemplate restTemplate =
                builder.setConnectTimeout(connectTimeoutDuration).setReadTimeout(readTimeoutDuration).build();

        restTemplate.setRequestFactory(requestFactory);

        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            protected void handleError(ClientHttpResponse response, HttpStatus statusCode) throws IOException {
                InputStream bodyInputStream = response.getBody();
                InputStreamReader isr = new InputStreamReader(bodyInputStream, "UTF-8");
                BufferedReader bf = new BufferedReader(isr);
                String errorMsg = bf.lines().collect(Collectors.joining());
                String newStatusText = response.getStatusText() + "::" + errorMsg;
                switch (statusCode.series()) {
                    case CLIENT_ERROR:
                        throw new HttpClientErrorException(
                                statusCode,
                                newStatusText);
                    case SERVER_ERROR:
                        throw new HttpServerErrorException(
                                statusCode,
                                newStatusText);
                    default:
                        throw new RestClientException(
                                String.format("[RestTemplate] error, %s, %s, %s, %s",
                                        statusCode,
                                        newStatusText));
                }
            }
        });

        return restTemplate;
    }

    private static SSLConnectionSocketFactory getSSLSocketFactory() throws Exception {
        TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
        SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();
        return new SSLConnectionSocketFactory(sslContext);
    }

}
