package com.ukgqtm.app.api;

import com.ukgqtm.app.requirement.RequirementGenerationException;
import com.ukgqtm.app.testcase.TestCaseOperationException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ProblemDetailsExceptionHandler {
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> accessDenied(AccessDeniedException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setTitle("Forbidden");
        problem.setDetail("The requested resource is not available.");
        problem.setType(URI.create("https://ukgqtm.local/problems/forbidden"));
        problem.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ProblemDetail> badCredentials(BadCredentialsException exception, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", "Authentication is required.", "unauthorized", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid request");
        problem.setDetail("The request body failed validation.");
        problem.setType(URI.create("https://ukgqtm.local/problems/invalid-request"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("violations", exception.getBindingResult().getFieldErrors().stream()
                .map(ProblemDetailsExceptionHandler::violation)
                .toList());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> unreadableJson(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", "The request body must be valid JSON.", "invalid-request", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ProblemDetail> unsupportedMedia(HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type", "Use application/json for JSON requests.", "unsupported-media-type", request);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    ResponseEntity<ProblemDetail> notAcceptable(HttpMediaTypeNotAcceptableException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_ACCEPTABLE, "Not Acceptable", "Use application/json or application/problem+json.", "not-acceptable", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ProblemDetail> notFound(NoResourceFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Not Found", "The requested resource is not available.", "not-found", request);
    }

    @ExceptionHandler(ApiConflictException.class)
    ResponseEntity<ProblemDetail> apiConflict(ApiConflictException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Conflict", exception.getMessage(), "conflict", request);
    }

    @ExceptionHandler(RequirementGenerationException.class)
    ResponseEntity<ProblemDetail> generationFailure(
            RequirementGenerationException exception, HttpServletRequest request) {
        return problem(
                exception.status(),
                "Requirement generation failed",
                exception.getMessage(),
                "requirement-generation-failed",
                request);
    }

    @ExceptionHandler(TestCaseOperationException.class)
    ResponseEntity<ProblemDetail> testCaseFailure(
            TestCaseOperationException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(exception.status());
        problem.setTitle("Test case operation failed");
        problem.setDetail(exception.getMessage());
        problem.setType(URI.create("https://ukgqtm.local/problems/test-case-operation-failed"));
        problem.setInstance(URI.create(request.getRequestURI()));
        if (!exception.rowErrors().isEmpty()) {
            problem.setProperty("rowErrors", exception.rowErrors());
        }
        return ResponseEntity.status(exception.status()).body(problem);
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    ResponseEntity<ProblemDetail> conflict(Exception exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Conflict", "The request conflicts with the current resource state.", "conflict", request);
    }

    private static String violation(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private static ResponseEntity<ProblemDetail> problem(
            HttpStatus status, String title, String detail, String type, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(title);
        problem.setDetail(detail);
        problem.setType(URI.create("https://ukgqtm.local/problems/" + type));
        problem.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(status).body(problem);
    }
}
