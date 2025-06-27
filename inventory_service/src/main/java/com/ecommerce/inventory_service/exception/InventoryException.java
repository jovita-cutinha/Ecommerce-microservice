package com.ecommerce.inventory_service.exception;

import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;

public class InventoryException {
    private String message;
    private HttpStatus httpStatus;
    private ZonedDateTime timestamp;

    public InventoryException(String message, HttpStatus httpStatus, ZonedDateTime timestamp) {
        this.message = message;
        this.httpStatus = httpStatus;
        this.timestamp = timestamp;
    }

    public InventoryException(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }
}
