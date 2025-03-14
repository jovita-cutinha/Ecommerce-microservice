package com.ecommerce.product_service.repository;

import com.ecommerce.product_service.model.Product;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;


public interface ProductRepository extends ReactiveMongoRepository<Product, String> {

    Flux<Product> findBySellerId(UUID sellerId);

    Flux<Product> findByCategory(String category);
}
