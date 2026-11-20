package org.radar_project.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Provides one {@link RestClient} per external service, each with explicit connect and read timeouts.
 */
@Configuration(proxyBeanMethods = false)
public class HttpClientConfig {

    @Bean
    @Qualifier("ollama")
    RestClient ollamaRestClient(RestClient.Builder builder, OllamaProperties properties) {
        return create(builder, properties.baseUrl(), properties.connectTimeout(), properties.readTimeout());
    }

    @Bean
    @Qualifier("gnd")
    RestClient gndRestClient(RestClient.Builder builder, GndProperties properties) {
        return create(builder, properties.baseUrl(), properties.connectTimeout(), properties.readTimeout());
    }

    @Bean
    @Qualifier("tib")
    RestClient tibRestClient(RestClient.Builder builder, TibProperties properties) {
        return create(builder, properties.baseUrl(), properties.connectTimeout(), properties.readTimeout());
    }

    private static RestClient create(RestClient.Builder builder, String baseUrl,
                                     Duration connectTimeout, Duration readTimeout) {
        return builder.clone()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory(connectTimeout, readTimeout))
                .build();
    }

    private static ClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);
        return factory;
    }
}
