package com.ecommerce.user_service.exception;

import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;

public class UserException {
    private  String message;
    private HttpStatus httpStatus;
    private ZonedDateTime timestamp;

    public UserException(String message, HttpStatus httpStatus, ZonedDateTime timestamp) {
        this.message = message;
        this.httpStatus = httpStatus;
        this.timestamp = timestamp;
    }

    public UserException(String message) {
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }
}

