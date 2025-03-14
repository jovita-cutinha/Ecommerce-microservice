package com.ecommerce.product_service.repository;

import com.ecommerce.product_service.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface ProductRepository extends MongoRepository<Product, String> {
}
