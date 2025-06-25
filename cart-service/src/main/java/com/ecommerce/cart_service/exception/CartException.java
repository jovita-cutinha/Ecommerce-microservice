package com.ecommerce.cart_service.exception;

import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;

public class CartException {
    private  String message;
    private HttpStatus httpStatus;
    private ZonedDateTime timestamp;

    public CartException(String message, HttpStatus httpStatus, ZonedDateTime timestamp) {
        this.message = message;
        this.httpStatus = httpStatus;
        this.timestamp = timestamp;
    }

    public CartException(String message) {
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
