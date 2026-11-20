package org.radar_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Copyright (c) 2026 Fachinformationszentrum Karlsruhe
 * <p>
 * Created by Michael Hoppe at 11/20/26
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class KeywordSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(KeywordSearchApplication.class, args);
    }
}
