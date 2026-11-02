package com.platform.workflow.registry.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class SecurityFilterConfig {

    @Bean
    FilterRegistrationBean<HmacAuthFilter> hmacAuthFilterRegistration(HmacAuthFilter filter) {
        FilterRegistrationBean<HmacAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/internal/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
