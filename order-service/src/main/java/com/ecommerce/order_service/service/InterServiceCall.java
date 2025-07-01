package com.ecommerce.order_service.service;


import com.ecommerce.order_service.dto.ApiResponseDTO;
import com.ecommerce.order_service.dto.InventoryDTO;
import com.ecommerce.order_service.exception.OrderServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class InterServiceCall {

    private final WebClient webClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${inventory-service.base-url}")
    private String inventoryServiceBaseUrl;

    @Value("${cart-service.base-url}")
    private String cartServiceBaseUrl;

    public InterServiceCall(WebClient webClient) {
        this.webClient = webClient;
    }

    public InventoryDTO getInventoryByProductId(String productId, String bearerToken) {
        ApiResponseDTO response = webClient.get()
                .uri(inventoryServiceBaseUrl + "/product/{productId}", productId)
                .headers(headers -> headers.setBearerAuth(bearerToken))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        res -> Mono.error(new OrderServiceException("Product not found", HttpStatus.NOT_FOUND)))
                .onStatus(HttpStatusCode::is5xxServerError,
                        res -> Mono.error(new OrderServiceException("Inventory service error", HttpStatus.SERVICE_UNAVAILABLE)))
                .bodyToMono(ApiResponseDTO.class)
                .block(); // Synchronous call

        return objectMapper.convertValue(response.getData(), InventoryDTO.class);
    }

    public void reserveStock(String productId, Integer quantity, String bearerToken) {

         webClient.put()
                 .uri(inventoryServiceBaseUrl + "/reserve/{productId}?quantity={quantity}", productId, quantity)
                .headers(headers -> headers.setBearerAuth(bearerToken))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        res -> Mono.error(new OrderServiceException("Product not found", HttpStatus.NOT_FOUND)))
                .onStatus(HttpStatusCode::is5xxServerError,
                        res -> Mono.error(new OrderServiceException("Inventory service error", HttpStatus.SERVICE_UNAVAILABLE)))
                 .toBodilessEntity()
                .block(); // Synchronous call

    }

    public void clearCart(String token) {
        webClient.delete()
                .uri(cartServiceBaseUrl)
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        res -> Mono.error(new OrderServiceException("Cart not found", HttpStatus.NOT_FOUND)))
                .onStatus(HttpStatusCode::is5xxServerError,
                        res -> Mono.error(new OrderServiceException("Cart service error", HttpStatus.SERVICE_UNAVAILABLE)))
                .toBodilessEntity()
                .block(); // Synchronous call

    }

    public void releaseStock(String productId, Integer quantity, String bearerToken) {

        webClient.put()
                .uri(inventoryServiceBaseUrl + "/release-reserve/{productId}?quantity={quantity}", productId, quantity)
                .headers(headers -> headers.setBearerAuth(bearerToken))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        res -> Mono.error(new OrderServiceException("Product not found", HttpStatus.NOT_FOUND)))
                .onStatus(HttpStatusCode::is5xxServerError,
                        res -> Mono.error(new OrderServiceException("Inventory service error", HttpStatus.SERVICE_UNAVAILABLE)))
                .toBodilessEntity()
                .block(); // Synchronous call

    }
}
