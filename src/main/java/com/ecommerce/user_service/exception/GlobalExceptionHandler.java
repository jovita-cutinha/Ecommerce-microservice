package com.ecommerce.user_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.ZonedDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserServiceException.class)
    public ResponseEntity<UserException> handleUserServiceException(UserServiceException ex) {
        UserException errorResponse = new UserException(
                ex.getMessage(),
                ex.getStatus(),
                ZonedDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }

//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<UserException> handleGenericException(Exception ex) {
//        UserException errorResponse = new UserException(
//                "An unexpected error occurred",
//                HttpStatus.INTERNAL_SERVER_ERROR,
//                ZonedDateTime.now()
//        );
//        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
//    }
}

