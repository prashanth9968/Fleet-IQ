package com.fleetiq.auth.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetails> handleApiException(ApiException ex, HttpServletRequest request) {
        log.error("API Exception occurred: {} - Status: {}", ex.getMessage(), ex.getStatus(), ex);
        
        String correlationId = MDC.get("traceId");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        ProblemDetails details = new ProblemDetails(
            "https://api.fleetiq.com/errors/" + ex.getStatus().name().toLowerCase().replace("_", "-"),
            ex.getTitle(),
            ex.getStatus().value(),
            ex.getMessage(),
            request.getRequestURI(),
            OffsetDateTime.now(),
            correlationId,
            null
        );

        return ResponseEntity.status(ex.getStatus()).body(details);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetails> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.error("Validation failed: {}", ex.getMessage());

        String correlationId = MDC.get("traceId");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        List<ProblemDetails.InvalidParam> invalidParams = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> new ProblemDetails.InvalidParam(error.getField(), error.getDefaultMessage()))
            .collect(Collectors.toList());

        ProblemDetails details = new ProblemDetails(
            "https://api.fleetiq.com/errors/validation-error",
            "Invalid Request Content",
            HttpStatus.BAD_REQUEST.value(),
            "One or more validation constraints failed. Please check the 'invalid_params' list.",
            request.getRequestURI(),
            OffsetDateTime.now(),
            correlationId,
            invalidParams
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(details);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetails> handleGeneralException(Exception ex, HttpServletRequest request) {
        log.error("Internal Server Error occurred: {}", ex.getMessage(), ex);

        String correlationId = MDC.get("traceId");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        ProblemDetails details = new ProblemDetails(
            "https://api.fleetiq.com/errors/internal-server-error",
            "Internal Server Error",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An unexpected error occurred. Please contact support.",
            request.getRequestURI(),
            OffsetDateTime.now(),
            correlationId,
            null
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(details);
    }
}
