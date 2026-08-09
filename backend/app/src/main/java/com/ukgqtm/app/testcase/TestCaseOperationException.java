package com.ukgqtm.app.testcase;

import java.util.List;
import org.springframework.http.HttpStatus;

public class TestCaseOperationException extends RuntimeException {
    private final HttpStatus status;
    private final List<String> rowErrors;

    public TestCaseOperationException(HttpStatus status, String message) {
        this(status, message, List.of());
    }

    public TestCaseOperationException(HttpStatus status, String message, List<String> rowErrors) {
        super(message);
        this.status = status;
        this.rowErrors = List.copyOf(rowErrors);
    }

    public HttpStatus status() {
        return status;
    }

    public List<String> rowErrors() {
        return rowErrors;
    }
}
