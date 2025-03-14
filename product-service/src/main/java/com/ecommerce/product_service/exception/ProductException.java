package com.ecommerce.product_service.exception;

import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;

public class ProductException {
    private  String message;
    private HttpStatus httpStatus;
    private ZonedDateTime timestamp;

    public ProductException(String message, HttpStatus httpStatus, ZonedDateTime timestamp) {
        this.message = message;
        this.httpStatus = httpStatus;
        this.timestamp = timestamp;
    }

    public ProductException(String message) {
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


