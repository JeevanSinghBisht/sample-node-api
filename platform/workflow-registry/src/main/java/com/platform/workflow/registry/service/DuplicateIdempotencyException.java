package com.platform.workflow.registry.service;

public class DuplicateIdempotencyException extends RuntimeException {

    public DuplicateIdempotencyException(String message) {
        super(message);
    }
}
