package com.ecommerce.cart_service.service;

import com.ecommerce.cart_service.dto.ApiResponseDTO;
import com.ecommerce.cart_service.dto.InventoryResponseDTO;
import com.ecommerce.cart_service.exception.CartServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class InterServiceCall {

    private final WebClient webClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${inventory-service.base-url}")
    private String inventoryServiceBaseUrl;

    public InterServiceCall(WebClient webClient) {
        this.webClient = webClient;
    }

    public InventoryResponseDTO getInventoryByProductId(String productId, String bearerToken) {
        ApiResponseDTO response = webClient.get()
                .uri(inventoryServiceBaseUrl + "/product/{productId}", productId)
                .headers(headers -> headers.setBearerAuth(bearerToken))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        res -> Mono.error(new CartServiceException("Product not found", HttpStatus.NOT_FOUND)))
                .onStatus(HttpStatusCode::is5xxServerError,
                        res -> Mono.error(new CartServiceException("Inventory service error", HttpStatus.SERVICE_UNAVAILABLE)))
                .bodyToMono(ApiResponseDTO.class)
                .block(); // Synchronous call

        return objectMapper.convertValue(response.getData(), InventoryResponseDTO.class);
    }
}
