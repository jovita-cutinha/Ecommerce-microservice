package com.ecommerce.cart_service.exception;

import org.springframework.http.HttpStatus;

public class CartServiceException extends RuntimeException {

        private final HttpStatus status;

        public CartServiceException(String message, HttpStatus status) {
            super(message);
            this.status = status;
        }

        public HttpStatus getStatus() {
            return status;
        }
    }

