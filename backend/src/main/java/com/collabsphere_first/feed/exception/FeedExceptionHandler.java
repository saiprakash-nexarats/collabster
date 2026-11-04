package com.collabsphere_first.feed.exception;

import com.collabsphere_first.community.dto.ApiResponse;
import com.collabsphere_first.feed.controller.FeedController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice(assignableTypes = FeedController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class FeedExceptionHandler {

    @ExceptionHandler(InvalidFeedCursorException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCursor(InvalidFeedCursorException ex) {
        log.warn("Invalid feed cursor: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(FeedSessionExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleSessionExpired(FeedSessionExpiredException ex) {
        log.info("Feed session expired: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.GONE)
                .body(new ApiResponse<>(false, ex.getErrorCode(), null, Instant.now()));
    }

    @ExceptionHandler(FeedSessionAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(FeedSessionAccessDeniedException ex) {
        log.warn("Feed session access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(FeedSessionStorageException.class)
    public ResponseEntity<ApiResponse<Void>> handleStorageException(FeedSessionStorageException ex) {
        log.error("Feed storage unavailable: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("Feed session service is temporarily unavailable. Please retry."));
    }

    @ExceptionHandler(FeedException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeedException(FeedException ex) {
        log.warn("Feed error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled error in feed controller: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred in feed domain"));
    }
}
