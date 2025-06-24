package com.ecommerce.inventory_service.service;

import com.ecommerce.inventory_service.dto.ApiResponseDto;
import com.ecommerce.inventory_service.dto.ProductDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.UUID;

@Service
public class RestTemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(RestTemplateClient.class);

    @Value("${user-service.base-url}")
    private String userServiceBaseUrl;

    @Value("${product-service.base-url}")
    private String productServiceBaseUrl;

    private final RestTemplate restTemplate;

    public RestTemplateClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Cacheable(value = "sellerIds", key = "#token")  // Cache seller ID based on token
    public UUID getSellerIdByToken(String token) {

        logger.info("Fetching seller ID for token: {}", token);

        String url = userServiceBaseUrl + "/getSellerIdByToken";

        logger.debug("Making request to URL: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);  // Pass the auth token in the request header
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<UUID> response = restTemplate.exchange(url, HttpMethod.GET, entity, UUID.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            UUID sellerId = response.getBody();
            logger.info("Successfully fetched seller ID: {}", sellerId);
            return sellerId;  // Return Seller ID
        } else {
            logger.error("Failed to fetch seller ID. Status code: {}", response.getStatusCode());
            return null;
        }
    }

    @Cacheable(value = "products", key = "#productId")
    public ProductDto getProductById(String productId, String token) {
        String url = productServiceBaseUrl + "/getProductById?productId=" + productId;

        logger.info("Fetching product details from Product Service: {}", url);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<ApiResponseDto> response = restTemplate.exchange(url, HttpMethod.GET, entity, ApiResponseDto.class);
            ObjectMapper mapper = new ObjectMapper();
            ProductDto product = mapper.convertValue(response.getBody().getData(), ProductDto.class);
            return product;
        } catch (RestClientException e) {
            logger.error("Failed to fetch product details for ID: {}", productId, e);
            throw new ResourceNotFoundException("Product not found with ID: " + productId);
        }
    }
}
