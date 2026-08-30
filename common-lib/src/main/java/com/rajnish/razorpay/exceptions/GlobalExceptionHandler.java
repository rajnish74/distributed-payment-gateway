package com.rajnish.razorpay.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(DuplicateResourceException e){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.of(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessViolationException(BusinessRuleViolationException e){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponse.of(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException e){
        String errorCode=e.getResourceName().toUpperCase()+"_NOT_FOUND";
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponse.of(errorCode, e.getMessage())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e,
                                                                               HttpServletRequest request) {

        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(fe->new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponse.of("VALIDATION_FAILED", "Validation failed for one or more fields", fieldErrors)
        );


    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitException(RateLimitException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("X-RateLimit-Remaining", "0")
                .header("Retry-After", String.valueOf(e.getRetryAfterSeconds()))
                .header("X-RateLimit-Reset", String.valueOf(
                        Instant.now().plusSeconds(e.getRetryAfterSeconds()).toEpochMilli()
                ))
                .body(ErrorResponse.of("RATE_LIMIT_EXCEEDED", e.getMessage()));
    }
}
