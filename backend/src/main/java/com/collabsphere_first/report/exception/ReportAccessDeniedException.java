package com.collabsphere_first.report.exception;

public class ReportAccessDeniedException extends RuntimeException {
    public ReportAccessDeniedException(String message) {
        super(message);
    }
}
