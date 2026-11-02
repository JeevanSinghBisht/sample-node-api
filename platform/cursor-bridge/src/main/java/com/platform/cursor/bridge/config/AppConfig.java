package com.platform.cursor.bridge.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(BridgeProperties.class)
public class AppConfig {

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
