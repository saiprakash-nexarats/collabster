package com.collabsphere_first.network.exception;

import com.collabsphere_first.common.pagination.InvalidCursorException;
import com.collabsphere_first.community.dto.ApiResponse;
import com.collabsphere_first.network.controller.NetworkController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = NetworkController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class NetworkExceptionHandler {

    @ExceptionHandler(RelationshipNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(RelationshipNotFoundException ex) {
        log.warn("Network resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(NetworkAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(NetworkAccessDeniedException ex) {
        log.warn("Network access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidRelationshipException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRelationship(InvalidRelationshipException ex) {
        log.warn("Invalid network relationship: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(RelationshipConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(RelationshipConflictException ex) {
        log.warn("Network relationship conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidCursorException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCursor(InvalidCursorException ex) {
        log.warn("Invalid cursor in network request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled network error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred in network domain"));
    }
}
