package com.platform.cursor.bridge.cursor;

public class CursorApiException extends RuntimeException {

    public CursorApiException(String message) {
        super(message);
    }

    public CursorApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
