package org.radar_project.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProxyPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void noProxyWhenHostIsMissing() {
        contextRunner.run(context -> {
            ProxyProperties proxy = context.getBean(ProxyProperties.class);
            assertThat(proxy.address()).isEmpty();
            assertThat(HttpClientConfig.proxySelector(proxy)).isEmpty();
        });
    }

    @Test
    void noProxyWhenHostAndPortAreEmpty() {
        contextRunner.withPropertyValues("radar.http.proxy.host=", "radar.http.proxy.port=")
                .run(context -> assertThat(context.getBean(ProxyProperties.class).address()).isEmpty());
    }

    @Test
    void routesAllRequestsThroughConfiguredProxy() {
        contextRunner.withPropertyValues("radar.http.proxy.host=proxy.test", "radar.http.proxy.port=8888")
                .run(context -> {
                    ProxyProperties proxy = context.getBean(ProxyProperties.class);
                    ProxySelector selector = HttpClientConfig.proxySelector(proxy).orElseThrow();
                    List<Proxy> selected = selector.select(URI.create("https://ollama.test/api/chat"));
                    assertThat(selected).singleElement().satisfies(p -> {
                        assertThat(p.type()).isEqualTo(Proxy.Type.HTTP);
                        InetSocketAddress address = (InetSocketAddress) p.address();
                        assertThat(address.getHostString()).isEqualTo("proxy.test");
                        assertThat(address.getPort()).isEqualTo(8888);
                    });
                });
    }

    @Test
    void rejectsHostWithoutPort() {
        contextRunner.withPropertyValues("radar.http.proxy.host=proxy.test")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsInvalidPort() {
        contextRunner.withPropertyValues("radar.http.proxy.host=proxy.test", "radar.http.proxy.port=70000")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ProxyProperties.class)
    static class PropertiesConfiguration {
    }
}
