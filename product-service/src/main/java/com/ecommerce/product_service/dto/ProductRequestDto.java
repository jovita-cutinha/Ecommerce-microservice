package com.ecommerce.product_service.dto;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public record ProductRequestDto(
        @NotNull String name,
        String description,
        @NotNull Double price,
        @NotNull String category,
        @NotNull String subcategory,
        String brand,
        List<String> images,
        Map<String, String> specifications
) {}

