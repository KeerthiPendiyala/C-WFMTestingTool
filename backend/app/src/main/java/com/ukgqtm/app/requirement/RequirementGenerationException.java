package com.ukgqtm.app.requirement;

import org.springframework.http.HttpStatus;

public class RequirementGenerationException extends RuntimeException {
    private final HttpStatus status;

    public RequirementGenerationException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public RequirementGenerationException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
