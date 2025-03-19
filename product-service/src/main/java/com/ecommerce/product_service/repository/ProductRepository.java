package com.ecommerce.product_service.repository;

import com.ecommerce.product_service.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.UUID;


public interface ProductRepository extends MongoRepository<Product, String>, CustomProductRepository {

   List<Product> findBySellerId(UUID sellerId);

    List<Product> findByCategory(String category);

    List<Product> findByCategoryAndSubcategory(String category, String subcategory);

    @Query("{ '$or': [ { 'name': { '$regex': ?0, '$options': 'i' } }, { 'description': { '$regex': ?0, '$options': 'i' } } ] }")
    List<Product> searchByNameOrDescription(String query);
}
