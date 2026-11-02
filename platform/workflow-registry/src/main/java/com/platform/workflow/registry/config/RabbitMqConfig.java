package com.platform.workflow.registry.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    TopicExchange platformEventsExchange(@Value("${platform.messaging.exchange:platform.events}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }
}
