package com.platform.cursor.bridge.api;

import com.platform.cursor.bridge.cursor.CursorApiException;
import com.platform.cursor.bridge.registry.RegistryClientException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BridgeExceptionHandler {

    @ExceptionHandler(CursorApiException.class)
    public ResponseEntity<Map<String, String>> cursorFailure(CursorApiException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(RegistryClientException.class)
    public ResponseEntity<Map<String, String>> registryFailure(RegistryClientException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", ex.getMessage()));
    }
}
