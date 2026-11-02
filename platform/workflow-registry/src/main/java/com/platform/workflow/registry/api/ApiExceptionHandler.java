package com.platform.workflow.registry.api;

import com.platform.workflow.registry.integration.CursorBridgeClientException;
import com.platform.workflow.registry.service.DuplicateIdempotencyException;
import com.platform.workflow.registry.service.InvalidTransitionException;
import com.platform.workflow.registry.service.ResourceNotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InvalidTransitionException.class)
    public ResponseEntity<Map<String, String>> invalidTransition(InvalidTransitionException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateIdempotencyException.class)
    public ResponseEntity<Map<String, String>> duplicate(DuplicateIdempotencyException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(CursorBridgeClientException.class)
    public ResponseEntity<Map<String, String>> bridgeFailure(CursorBridgeClientException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", ex.getBindingResult().getAllErrors().getFirst().getDefaultMessage()));
    }
}
