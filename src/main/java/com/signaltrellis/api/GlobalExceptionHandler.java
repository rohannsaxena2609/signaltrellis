package com.signaltrellis.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.signaltrellis.capture.DocumentNotFoundException;
import com.signaltrellis.capture.InvalidDocumentException;
import com.signaltrellis.context.ContextRecordNotFoundException;
import com.signaltrellis.context.DuplicateContextRecordException;
import com.signaltrellis.context.InvalidContextRequestException;
import com.signaltrellis.signals.InvalidSignalRequestException;
import com.signaltrellis.signals.SignalNotFoundException;
import com.signaltrellis.identity.EmailAlreadyRegisteredException;
import com.signaltrellis.identity.PasswordTooLongException;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SignalNotFoundException.class)
    public ResponseEntity<ApiError> handleSignalNotFound(SignalNotFoundException exception,
                                                          HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "SIGNAL_NOT_FOUND", "Signal not found",
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(InvalidSignalRequestException.class)
    public ResponseEntity<ApiError> handleSignalRequest(InvalidSignalRequestException exception,
                                                         HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_SIGNAL_REQUEST", exception.getMessage(),
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(ContextRecordNotFoundException.class)
    public ResponseEntity<ApiError> handleContextNotFound(ContextRecordNotFoundException exception,
                                                           HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "CONTEXT_RECORD_NOT_FOUND", "Context record not found",
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(DuplicateContextRecordException.class)
    public ResponseEntity<ApiError> handleDuplicateContext(DuplicateContextRecordException exception,
                                                            HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "DUPLICATE_CONTEXT_RECORD", exception.getMessage(),
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(InvalidContextRequestException.class)
    public ResponseEntity<ApiError> handleInvalidContext(InvalidContextRequestException exception,
                                                         HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_CONTEXT_REQUEST", exception.getMessage(),
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<ApiError> handleDocumentNotFound(DocumentNotFoundException exception,
                                                            HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "Document not found",
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(InvalidDocumentException.class)
    public ResponseEntity<ApiError> handleInvalidDocument(InvalidDocumentException exception,
                                                           HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_DOCUMENT_REQUEST", exception.getMessage(),
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleUploadTooLarge(MaxUploadSizeExceededException exception,
                                                          HttpServletRequest request) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "UPLOAD_TOO_LARGE", "File exceeds the 10 MB upload limit",
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException exception,
                                                        HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "A request parameter has an invalid value",
                request.getRequestURI(), Map.of(exception.getName(), "Invalid value"));
    }

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
