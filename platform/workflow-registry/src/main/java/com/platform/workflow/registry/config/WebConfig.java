package com.platform.workflow.registry.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final PlatformProperties platformProperties;

    public WebConfig(PlatformProperties platformProperties) {
        this.platformProperties = platformProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins =
                platformProperties.getDashboard().getAllowedOrigins().split("\\s*,\\s*");
        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*");
    }
}
