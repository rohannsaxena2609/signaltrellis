package com.signaltrellis.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.signaltrellis.identity.EmailAlreadyRegisteredException;
import com.signaltrellis.identity.PasswordTooLongException;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiError> handleDuplicateEmail(EmailAlreadyRegisteredException exception,
                                                          HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "An account with this email already exists",
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException exception,
                                                          HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is incorrect",
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(PasswordTooLongException.class)
    public ResponseEntity<ApiError> handlePasswordTooLong(PasswordTooLongException exception,
                                                           HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "PASSWORD_TOO_LONG", "Password must be at most 72 UTF-8 bytes",
                request.getRequestURI(), Map.of("password", "Must be at most 72 UTF-8 bytes"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage(),
                        (first, ignored) -> first));
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed",
                request.getRequestURI(), fieldErrors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred", request.getRequestURI(), Map.of());
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message,
                                           String path, Map<String, String> fieldErrors) {
        return ResponseEntity.status(status).body(new ApiError(
                Instant.now(), status.value(), code, message, path, fieldErrors));
    }
}
