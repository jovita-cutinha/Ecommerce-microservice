package com.ecommerce.product_service.dto;

public record ApiResponseDto(
         String status,
         String message,
         Object data
) {
}
