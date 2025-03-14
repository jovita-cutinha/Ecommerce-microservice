package com.ecommerce.product_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${user-service.base-url}") // Fetch value from application.yml
    private String userServiceBaseUrl;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(userServiceBaseUrl)
                .build();
    }
}

