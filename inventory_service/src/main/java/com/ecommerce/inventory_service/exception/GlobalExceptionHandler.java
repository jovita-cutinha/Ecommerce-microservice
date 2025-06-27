package com.ecommerce.inventory_service.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.ZonedDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InventoryServiceException.class)
    public ResponseEntity<InventoryException> handleUserServiceException(InventoryServiceException ex) {
        InventoryException errorResponse = new InventoryException(
                ex.getMessage(),
                ex.getStatus(),
                ZonedDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }

//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<CartException> handleGenericException(Exception ex) {
//        InventoryException errorResponse = new InventoryException(
//                "An unexpected error occurred",
//                HttpStatus.INTERNAL_SERVER_ERROR,
//                ZonedDateTime.now()
//        );
//        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
//    }

}

