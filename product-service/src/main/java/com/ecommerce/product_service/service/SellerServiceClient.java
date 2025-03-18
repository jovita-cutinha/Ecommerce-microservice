package com.ecommerce.product_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.UUID;

@Service
public class SellerServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(SellerServiceClient.class);

    @Value("${user-service.base-url}") // Fetch value from application.yml
    private String userServiceBaseUrl;

    private final RestTemplate restTemplate;

    public SellerServiceClient(RestTemplate restTemplate) {
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
}