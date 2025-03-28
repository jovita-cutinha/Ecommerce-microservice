package com.ecommerce.inventory_service.dto;

public record ApiResponseDto(
        String status,
        String message,
        Object data
) {
}
