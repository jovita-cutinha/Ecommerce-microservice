package com.ecommerce.product_service.repository;

import com.ecommerce.product_service.model.Product;
import java.util.List;

public interface CustomProductRepository {
    List<Product> findByFilters(String category, String subcategory, String brand, Double minPrice, Double maxPrice);
}
