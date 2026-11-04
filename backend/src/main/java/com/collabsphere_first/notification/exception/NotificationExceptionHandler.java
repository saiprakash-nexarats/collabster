package com.collabsphere_first.notification.exception;

import com.collabsphere_first.community.dto.ApiResponse;
import com.collabsphere_first.notification.controller.NotificationController;
import com.collabsphere_first.notification.controller.NotificationDeviceController;
import com.collabsphere_first.notification.controller.NotificationPreferenceController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice(assignableTypes = {
        NotificationController.class,
        NotificationDeviceController.class,
        NotificationPreferenceController.class
})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class NotificationExceptionHandler {

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NotificationNotFoundException ex) {
        log.warn("Notification resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(NotificationAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(NotificationAccessDeniedException ex) {
        log.warn("Notification access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(NotificationCursorException.class)
    public ResponseEntity<ApiResponse<Void>> handleCursorException(NotificationCursorException ex) {
        log.warn("Invalid notification cursor: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Notification request validation error: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation error: " + errors));
    }
}
