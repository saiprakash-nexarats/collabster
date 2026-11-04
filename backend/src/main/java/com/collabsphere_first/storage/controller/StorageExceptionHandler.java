package com.collabsphere_first.storage.controller;

import com.collabsphere_first.community.dto.ApiResponse;
import com.collabsphere_first.storage.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(assignableTypes = StorageController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StorageExceptionHandler {

    @ExceptionHandler(StorageAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(StorageAccessDeniedException ex) {
        log.warn("Storage access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(StorageObjectNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(StorageObjectNotFoundException ex) {
        log.warn("Storage object not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(UploadExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleExpired(UploadExpiredException ex) {
        log.warn("Storage upload expired: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.GONE).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidUploadException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidUpload(InvalidUploadException ex) {
        log.warn("Invalid storage upload request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        log.warn("Storage validation error: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiResponse<Void>> handleStorageException(StorageException ex) {
        log.error("Storage error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Storage operation failed: " + ex.getMessage()));
    }
}

