package com.ecommerce.order_service.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.ZonedDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(OrderServiceException.class)
    public ResponseEntity<OrderException> handleUserServiceException(OrderServiceException ex) {
        OrderException errorResponse = new OrderException(
                ex.getMessage(),
                ex.getStatus(),
                ZonedDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }

//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<OrderException> handleGenericException(Exception ex) {
//        OrderException errorResponse = new OrderException(
//                "An unexpected error occurred",
//                HttpStatus.INTERNAL_SERVER_ERROR,
//                ZonedDateTime.now()
//        );
//        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
//    }
}
