package com.platform.workflow.registry.integration;

public class CursorBridgeClientException extends RuntimeException {

    public CursorBridgeClientException(String message) {
        super(message);
    }

    public CursorBridgeClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
