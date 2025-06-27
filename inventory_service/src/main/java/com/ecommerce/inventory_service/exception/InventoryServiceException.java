package com.ecommerce.inventory_service.exception;

import org.springframework.http.HttpStatus;

public class InventoryServiceException extends RuntimeException{

    private final HttpStatus status;

    public InventoryServiceException(String message, HttpStatus status){
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
