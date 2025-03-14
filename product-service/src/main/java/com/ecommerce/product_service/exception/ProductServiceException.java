package com.ecommerce.product_service.exception;


import org.springframework.http.HttpStatus;

public class ProductServiceException extends RuntimeException {
    private final HttpStatus status;

    public ProductServiceException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
