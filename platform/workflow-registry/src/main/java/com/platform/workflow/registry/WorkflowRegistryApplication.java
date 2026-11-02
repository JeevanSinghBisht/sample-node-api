package com.platform.workflow.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WorkflowRegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkflowRegistryApplication.class, args);
    }
}
