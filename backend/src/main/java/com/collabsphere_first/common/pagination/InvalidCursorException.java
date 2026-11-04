package com.collabsphere_first.common.pagination;

public class InvalidCursorException extends RuntimeException {
    public InvalidCursorException(String message) {
        super(message);
    }
}
