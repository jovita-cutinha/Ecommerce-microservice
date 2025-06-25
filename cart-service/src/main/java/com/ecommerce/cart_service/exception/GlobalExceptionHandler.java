package com.ecommerce.cart_service.exception;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.ZonedDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CartServiceException.class)
    public ResponseEntity<CartException> handleUserServiceException(CartServiceException ex) {
        CartException errorResponse = new CartException(
                ex.getMessage(),
                ex.getStatus(),
                ZonedDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }

//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<CartException> handleGenericException(Exception ex) {
//        CartException errorResponse = new CartException(
//                "An unexpected error occurred",
//                HttpStatus.INTERNAL_SERVER_ERROR,
//                ZonedDateTime.now()
//        );
//        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
//    }

}
