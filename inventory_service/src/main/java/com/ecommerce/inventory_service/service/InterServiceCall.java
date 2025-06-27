package com.ecommerce.inventory_service.service;

import com.ecommerce.inventory_service.dto.ApiResponseDto;
import com.ecommerce.inventory_service.dto.ProductDto;
import com.ecommerce.inventory_service.exception.InventoryServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.http.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Service
public class InterServiceCall {

    private final WebClient webClient;
    private static final Logger logger = LoggerFactory.getLogger(InterServiceCall.class);

    @Value("${user-service.base-url}")
    private String userServiceBaseUrl;

    @Value("${product-service.base-url}")
    private String productServiceBaseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public InterServiceCall(WebClient webClient) {
        this.webClient = webClient;
    }

    @Cacheable(value = "sellerIds", key = "#token")  // Cache seller ID based on token
    public UUID getSellerIdByToken(String token) {

        logger.info("Fetching seller ID for token: {}", token);
        String url = userServiceBaseUrl + "/getSellerIdByToken";
        logger.debug("Making request to URL: {}", url);
        try {
            UUID sellerId = webClient.get()
                    .uri(url)
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> {
                        logger.error("User Service returned error status: {}", response.statusCode());
                        return Mono.error(new InventoryServiceException("Unable to fetch seller ID", HttpStatus.INTERNAL_SERVER_ERROR));
                    })
                    .bodyToMono(UUID.class)
                    .block();

            logger.info("Successfully fetched seller ID: {}", sellerId);
            return sellerId;

        } catch (Exception e) {
            logger.error("Error fetching seller ID", e);
            return null; // You may choose to throw a custom exception instead
        }
    }

    @Cacheable(value = "products", key = "#productId")
    public ProductDto getProductById(String productId, String token) {
        String url = productServiceBaseUrl + "/" + productId;

        logger.info("Fetching product details from Product Service: {}", url);
        try {
            ApiResponseDto response = webClient.get()
                    .uri(url)
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, status -> {
                        logger.error("Failed to fetch product. Status: {}", status.statusCode());
                        return Mono.error(new InventoryServiceException("Product fetch failed", HttpStatus.INTERNAL_SERVER_ERROR));
                    })
                    .bodyToMono(ApiResponseDto.class)
                    .block();

            ProductDto productDto = objectMapper.convertValue(response.getData(), ProductDto.class);
            logger.info("Successfully fetched product: {}", productId);
            return productDto;

        } catch (Exception e) {
            logger.error("Failed to fetch product details for ID: {}", productId, e);
            throw new ResourceNotFoundException("Product not found with ID: " + productId);
        }
    }
}