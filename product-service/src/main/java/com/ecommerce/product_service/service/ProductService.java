package com.ecommerce.product_service.service;

import com.ecommerce.product_service.dto.ApiResponseDto;
import com.ecommerce.product_service.dto.ProductRequestDto;
import com.ecommerce.product_service.exception.ProductServiceException;
import com.ecommerce.product_service.model.Product;
import com.ecommerce.product_service.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.security.auth.callback.CallbackHandler;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final SellerServiceClient sellerServiceClient;

    public ProductService(ProductRepository productRepository, SellerServiceClient sellerServiceClient) {
        this.productRepository = productRepository;
        this.sellerServiceClient = sellerServiceClient;
    }

    @CacheEvict(value = "products", key = "'allProducts'")
    public ApiResponseDto createProduct(ProductRequestDto request, String authToken) {
        logger.info("Received request to create product: {}", request.name());

        UUID sellerId = sellerServiceClient.getSellerIdByToken(authToken);  // Cached Seller ID
        if (sellerId == null) {
            return new ApiResponseDto("error", "Seller ID not found", null);
        }

        logger.info("Seller ID retrieved successfully: {}", sellerId);

        Product product = new Product(
                null,
                request.name(),
                request.description(),
                request.price(),
                request.category(),
                request.subcategory(),
                request.brand(),
                sellerId,
                request.images(),
                request.specifications(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        try {
            Product savedProduct = productRepository.save(product);
            logger.info("Product saved successfully: {}", savedProduct.getId());
            return new ApiResponseDto("success", "Product added successfully", savedProduct);
        } catch (Exception e) {
            logger.error("Error saving product: {}", e.getMessage(), e);
            return new ApiResponseDto("error", "Failed to save product: " + e.getMessage(), null);
        }
    }

    @CacheEvict(value = "products", key = "{#productId, 'allProducts'}")
    public ApiResponseDto updateProduct(String productId, ProductRequestDto request, String authToken) {
        logger.info("Received request to update product: {}", productId);

        UUID sellerId = sellerServiceClient.getSellerIdByToken(authToken);  // Cached Seller ID
        if (sellerId == null) {
            return new ApiResponseDto("error", "Unauthorized access", null);
        }

        logger.info("Seller ID retrieved: {}", sellerId);

        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new ProductServiceException("Product not found", HttpStatus.NOT_FOUND));

        // Update the product details
        existingProduct.setName(request.name());
        existingProduct.setDescription(request.description());
        existingProduct.setPrice(request.price());
        existingProduct.setCategory(request.category());
        existingProduct.setSubcategory(request.subcategory());
        existingProduct.setBrand(request.brand());
        existingProduct.setImages(request.images());
        existingProduct.setSpecifications(request.specifications());
        existingProduct.setUpdatedAt(LocalDateTime.now());

        try {
            Product updatedProduct = productRepository.save(existingProduct);
            logger.info("Product updated successfully: {}", updatedProduct.getId());
            return new ApiResponseDto("success", "Product updated successfully", updatedProduct);
        } catch (Exception e) {
            logger.error("Error updating product: {}", e.getMessage(), e);
            return new ApiResponseDto("error", "Failed to update product: " + e.getMessage(), null);
        }
    }

    @Cacheable(value = "products", key = "'allProducts'")
    public ApiResponseDto getAllProducts() {

        logger.info("Fetching all products from database.");

        List<Product> productList = productRepository.findAll();
        if (productList.isEmpty()) {
            logger.warn("No products found");
            return new ApiResponseDto("error", "No products found", Collections.emptyList());
        }

        logger.info("Successfully fetched {} products.", productList.size());
        return new ApiResponseDto("success", "Products retrieved successfully", productList);
    }

    @Cacheable(value = "products", key = "#productId")
    public ApiResponseDto getProductById(String productId) {
        logger.info("Fetching product with ID: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductServiceException("Product not found", HttpStatus.NOT_FOUND));

        logger.info("Product found: {}", product.getName());
        return new ApiResponseDto("success", "Product retrieved successfully", product);
    }

    @Cacheable(value = "products", key = "'seller_' + #sellerId")
    public ApiResponseDto getProductsBySellerId(UUID sellerId) {

        logger.info("Fetching products for seller ID: {}", sellerId);

        List<Product> products = productRepository.findBySellerId(sellerId);
        if (products.isEmpty()) {
            logger.warn("No products found for seller ID: {}", sellerId);
            return new ApiResponseDto("error", "No products found for the seller", Collections.emptyList());
        }

        logger.info("Successfully fetched {} products for seller ID: {}", products.size(), sellerId);
        return new ApiResponseDto("success", "Products retrieved successfully", products);
    }

    @Cacheable(value = "products", key = "'category_' + #category")
    public ApiResponseDto getProductsByCategory(String category) {
        logger.info("Fetching products for category: {}", category);

        List<Product> products = productRepository.findByCategory(category);
        if (products.isEmpty()) {
            logger.warn("No products found for category: {}", category);
            return new ApiResponseDto("error", "No products found for this category", null);
        }

        logger.info("Successfully fetched {} products for category: {}", products.size(), category);
        return new ApiResponseDto("success", "Products retrieved successfully", products);
    }

    @Cacheable(value = "products", key = "'subcategory_' + #subcategory")
    public ApiResponseDto getAllProductsBySubcategory(String category, String subcategory) {
        logger.info("Fetching products for subcategory: {}", subcategory);

        List<Product> products = productRepository.findByCategoryAndSubcategory(category, subcategory);
        if (products.isEmpty()) {
            logger.warn("No products found for subcategory: {}", subcategory);
            return new ApiResponseDto("error", "No products found for this subcategory", null);
        }

        logger.info("Successfully fetched {} products for subcategory: {}", products.size(), subcategory);
        return new ApiResponseDto("success", "Products retrieved successfully", products);

    }

    @CacheEvict(value = "products", key = "'product_' + #id")
    public ApiResponseDto deleteProductById(String productId) {
        logger.info("Attempting to delete product with ID: {}", productId);

        productRepository.findById(productId)
                .orElseThrow(() -> new ProductServiceException("Product not found", HttpStatus.NOT_FOUND));

        productRepository.deleteById(productId);

        logger.info("Product with ID: {} successfully deleted from database", productId);
        return new ApiResponseDto("success", "Product deleted successfully", null);
    }
}