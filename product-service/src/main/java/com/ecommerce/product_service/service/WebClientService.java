package com.ecommerce.product_service.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class WebClientService {

    private final WebClient webClient;

    public WebClientService(WebClient webClient) {
        this.webClient = webClient;
    }

    // Call User Service to get Seller ID from Token
    public Mono<UUID> getSellerIdByToken(String authToken) {
        return webClient.get()
                .uri("/getSellerIdByToken")  // Relative URI (Base URL is set in WebClientConfig)
                .header("Authorization", authToken) // Pass the Bearer Token
                .retrieve()
                .bodyToMono(UUID.class) // Convert response to String (Seller ID)
                .onErrorResume(e -> {
                    System.err.println("Error fetching Seller ID: " + e.getMessage());
                    return Mono.empty(); // Return empty Mono if an error occurs
                });
    }
}

