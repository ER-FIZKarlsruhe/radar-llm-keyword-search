package org.radar_project.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

/**
 * Provides one {@link RestClient} per external service, each with explicit connect and read timeouts
 * and the optional proxy from {@link ProxyProperties}.
 */
@Configuration(proxyBeanMethods = false)
public class HttpClientConfig {

    @Bean
    @Qualifier("ollama")
    RestClient ollamaRestClient(RestClient.Builder builder, OllamaProperties properties,
                                 ProxyProperties proxy) {
        return create(builder, proxy, properties.baseUrl(), properties.connectTimeout(), properties.readTimeout());
    }

    @Bean
    @Qualifier("gnd")
    RestClient gndRestClient(RestClient.Builder builder, GndProperties properties, ProxyProperties proxy) {
        return create(builder, proxy, properties.baseUrl(), properties.connectTimeout(), properties.readTimeout());
    }

    @Bean
    @Qualifier("tib")
    RestClient tibRestClient(RestClient.Builder builder, TibProperties properties, ProxyProperties proxy) {
        return create(builder, proxy, properties.baseUrl(), properties.connectTimeout(), properties.readTimeout());
    }

    private static RestClient create(RestClient.Builder builder, ProxyProperties proxy, String baseUrl,
                                     Duration connectTimeout, Duration readTimeout) {
        return builder.clone()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory(proxy, connectTimeout, readTimeout))
                .build();
    }

    private static ClientHttpRequestFactory requestFactory(ProxyProperties proxy,
                                                           Duration connectTimeout, Duration readTimeout) {
        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NORMAL);
        proxySelector(proxy).ifPresent(httpClientBuilder::proxy);
        HttpClient httpClient = httpClientBuilder.build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);
        return factory;
    }

    /**
     * @return a selector routing every request through the configured proxy, or empty for direct connections
     */
    static Optional<ProxySelector> proxySelector(ProxyProperties proxy) {
        return proxy.address().map(ProxySelector::of);
    }
}
