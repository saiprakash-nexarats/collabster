package com.collabsphere_first.explore.controller;

import com.collabsphere_first.community.dto.ApiResponse;
import com.collabsphere_first.explore.exception.InvalidExploreCursorException;
import com.collabsphere_first.explore.exception.InvalidExploreQueryException;
import com.collabsphere_first.explore.exception.ExploreException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(assignableTypes = ExploreController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExploreExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument in explore: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidExploreQueryException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidQuery(InvalidExploreQueryException ex) {
        log.warn("Invalid explore search query: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidExploreCursorException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCursor(InvalidExploreCursorException ex) {
        log.warn("Invalid explore cursor: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ExploreException.class)
    public ResponseEntity<ApiResponse<Void>> handleExploreException(ExploreException ex) {
        log.warn("Explore domain exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled explore error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred in explore domain"));
    }
}
