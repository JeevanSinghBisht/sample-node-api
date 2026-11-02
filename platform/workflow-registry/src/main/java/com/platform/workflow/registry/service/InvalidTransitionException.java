package com.platform.workflow.registry.service;

public class InvalidTransitionException extends RuntimeException {

    public InvalidTransitionException(String message) {
        super(message);
    }
}
