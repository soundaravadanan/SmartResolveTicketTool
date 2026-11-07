package com.smartresolve.api;

import com.smartresolve.api.ro.ApiErrorRO;
import com.smartresolve.service.TicketTriageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorRO> handleResponseStatus(ResponseStatusException exception) {
        HttpStatusCode status = exception.getStatusCode();
        String error = HttpStatus.valueOf(status.value()).getReasonPhrase();
        String message = exception.getReason() == null ? error : exception.getReason();
        return ResponseEntity.status(status)
                .body(new ApiErrorRO(LocalDateTime.now(), status.value(), error, message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorRO> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Request validation failed");
        return ResponseEntity.badRequest()
                .body(new ApiErrorRO(LocalDateTime.now(), 400, "Bad Request", message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorRO> handleUnreadableRequest(HttpMessageNotReadableException exception) {
        return badRequest("Request body is invalid");
    }

    @ExceptionHandler(TicketTriageException.class)
    public ResponseEntity<ApiErrorRO> handleTriageUnavailable(TicketTriageException exception) {
        HttpStatus status = exception.getStatus();
        return ResponseEntity.status(status)
                .body(new ApiErrorRO(
                        LocalDateTime.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorRO> handleInvalidRequestParameter(MethodArgumentTypeMismatchException exception) {
        return badRequest("Request parameter is invalid");
    }

    private ResponseEntity<ApiErrorRO> badRequest(String message) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorRO(LocalDateTime.now(), 400, "Bad Request", message));
    }
}
