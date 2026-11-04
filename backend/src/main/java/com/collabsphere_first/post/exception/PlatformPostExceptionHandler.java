package com.collabsphere_first.post.exception;

import com.collabsphere_first.community.dto.ApiResponse;
import com.collabsphere_first.post.controller.PlatformPostController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller advice scoped strictly to PlatformPostController.
 * Using assignableTypes guarantees that exceptions from Community, Auth, Profile,
 * or Workspace controllers are NEVER intercepted by this handler.
 */
@RestControllerAdvice(assignableTypes = PlatformPostController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class PlatformPostExceptionHandler {

    @ExceptionHandler(PlatformPostNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(PlatformPostNotFoundException ex) {
        log.warn("PlatformPost not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler({PlatformPostAccessDeniedException.class, AccessDeniedException.class})
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(Exception ex) {
        log.warn("PlatformPost access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidPlatformPostException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidPost(InvalidPlatformPostException ex) {
        log.warn("Invalid PlatformPost: {}", ex.getMessage());
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
                .body(new ApiResponse<>(false, "Validation failed", errors, java.time.Instant.now()));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(OptimisticLockingFailureException ex) {
        log.warn("Optimistic locking conflict on platform post: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Post was updated concurrently. Please refresh and try again."));
    }
}
