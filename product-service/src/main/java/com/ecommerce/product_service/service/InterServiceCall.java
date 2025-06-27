package com.ecommerce.product_service.service;

import com.ecommerce.product_service.exception.ProductServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class InterServiceCall {

    private final WebClient webClient;
    private static final Logger logger = LoggerFactory.getLogger(InterServiceCall.class);

    @Value("${user-service.base-url}") // Fetch value from application.yml
    private String userServiceBaseUrl;

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
                        return Mono.error(new ProductServiceException("Unable to fetch seller ID", HttpStatus.INTERNAL_SERVER_ERROR));
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
}