package com.platform.cursor.bridge.security;

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
        registration.addUrlPatterns("/internal/*", "/agents");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
