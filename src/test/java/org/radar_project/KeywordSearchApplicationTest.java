package org.radar_project;

import org.junit.jupiter.api.Test;
import org.radar_project.config.TibProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class KeywordSearchApplicationTest {

    @Autowired
    private TibProperties tibProperties;

    @Test
    void contextLoadsWithDefaultProperties() {
        assertThat(tibProperties.ontologies()).isEmpty();
    }
}
