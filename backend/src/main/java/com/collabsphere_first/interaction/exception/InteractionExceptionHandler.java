package com.collabsphere_first.interaction.exception;

import com.collabsphere_first.common.pagination.InvalidCursorException;
import com.collabsphere_first.community.dto.ApiResponse;
import com.collabsphere_first.interaction.controller.InteractionController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = InteractionController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class InteractionExceptionHandler {

    @ExceptionHandler(ContentInaccessibleException.class)
    public ResponseEntity<ApiResponse<Void>> handleContentInaccessible(ContentInaccessibleException ex) {
        log.warn("Content inaccessible for interaction: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidInteractionException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidInteraction(InvalidInteractionException ex) {
        log.warn("Invalid interaction: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidCursorException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCursor(InvalidCursorException ex) {
        log.warn("Invalid cursor in interaction request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, "Validation failed", errors, Instant.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled interaction error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred in interaction domain"));
    }
}
