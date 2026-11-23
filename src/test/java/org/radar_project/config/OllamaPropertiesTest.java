package org.radar_project.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class OllamaPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class)
            .withPropertyValues(
                    "radar.ollama.base-url=https://ollama.test/ollama",
                    "radar.ollama.model=keyword-extractor",
                    "radar.ollama.connect-timeout=1s",
                    "radar.ollama.read-timeout=1s");

    @Test
    void bindsPositiveMaxKeywords() {
        contextRunner.withPropertyValues("radar.ollama.max-keywords=10")
                .run(context -> assertThat(context.getBean(OllamaProperties.class).maxKeywords()).isEqualTo(10));
    }

    @Test
    void rejectsZeroMaxKeywords() {
        contextRunner.withPropertyValues("radar.ollama.max-keywords=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsNegativeMaxKeywords() {
        contextRunner.withPropertyValues("radar.ollama.max-keywords=-1")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsMissingMaxKeywords() {
        contextRunner.run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(OllamaProperties.class)
    static class PropertiesConfiguration {
    }
}
