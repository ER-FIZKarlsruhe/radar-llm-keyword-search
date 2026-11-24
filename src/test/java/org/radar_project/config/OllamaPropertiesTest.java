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
                    "radar.ollama.connect-timeout=1s",
                    "radar.ollama.read-timeout=1s");

    @Test
    void bindsPositiveMaxKeywords() {
        contextRunner.withPropertyValues("radar.ollama.max-keywords=10", "radar.ollama.models=keyword-extractor")
                .run(context -> assertThat(context.getBean(OllamaProperties.class).maxKeywords()).isEqualTo(10));
    }

    @Test
    void bindsSelectableModels() {
        contextRunner.withPropertyValues("radar.ollama.max-keywords=10",
                        "radar.ollama.models=keyword-extractor,keyword-extractor1,keyword-extractor2")
                .run(context -> {
                    OllamaProperties properties = context.getBean(OllamaProperties.class);
                    assertThat(properties.models())
                            .containsExactly("keyword-extractor", "keyword-extractor1", "keyword-extractor2");
                    assertThat(properties.isSelectable("keyword-extractor2")).isTrue();
                    assertThat(properties.isSelectable("other")).isFalse();
                });
    }

    @Test
    void rejectsMissingModels() {
        contextRunner.withPropertyValues("radar.ollama.max-keywords=10")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsBlankModel() {
        contextRunner.withPropertyValues("radar.ollama.max-keywords=10", "radar.ollama.models[0]= ")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsZeroMaxKeywords() {
        contextRunner.withPropertyValues("radar.ollama.max-keywords=0", "radar.ollama.models=keyword-extractor")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsNegativeMaxKeywords() {
        contextRunner.withPropertyValues("radar.ollama.max-keywords=-1", "radar.ollama.models=keyword-extractor")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsMissingMaxKeywords() {
        contextRunner.withPropertyValues("radar.ollama.models=keyword-extractor").run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(OllamaProperties.class)
    static class PropertiesConfiguration {
    }
}
