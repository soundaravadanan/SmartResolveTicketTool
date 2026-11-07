package com.smartresolve.service;

import org.springframework.http.HttpStatus;

public class TicketTriageException extends RuntimeException {

    private final HttpStatus status;

    public TicketTriageException(String message, Throwable cause) {
        this(message, cause, HttpStatus.SERVICE_UNAVAILABLE);
    }

    public TicketTriageException(String message, Throwable cause, HttpStatus status) {
        super(message, cause);
        this.status = status;
    }

    public TicketTriageException(String message) {
        this(message, HttpStatus.SERVICE_UNAVAILABLE);
    }

    public TicketTriageException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
