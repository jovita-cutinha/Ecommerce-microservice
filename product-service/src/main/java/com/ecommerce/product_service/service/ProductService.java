package com.ecommerce.product_service.service;

import com.ecommerce.product_service.dto.ApiResponseDto;
import com.ecommerce.product_service.dto.ProductEvent;
import com.ecommerce.product_service.dto.ProductRequestDto;
import com.ecommerce.product_service.exception.ProductServiceException;
import com.ecommerce.product_service.model.Product;
import com.ecommerce.product_service.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final SellerServiceClient sellerServiceClient;
    private final KafkaTemplate<String, ProductEvent> kafkaTemplate;

    @Value("${kafka.topics.product-events}")
    private String productEventsTopic;

    public ProductService(ProductRepository productRepository, SellerServiceClient sellerServiceClient, KafkaTemplate<String, ProductEvent> kafkaTemplate) {
        this.productRepository = productRepository;
        this.sellerServiceClient = sellerServiceClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    @CacheEvict(value = "products", allEntries = true)
    public ApiResponseDto createProducts(List<ProductRequestDto> requests, String authToken) {
        logger.info("Received request to create {} products", requests.size());

        UUID sellerId = sellerServiceClient.getSellerIdByToken(authToken);  // Cached Seller ID
        if (sellerId == null) {
            return new ApiResponseDto("error", "Seller ID not found", null);
        }

        logger.info("Seller ID retrieved successfully: {}", sellerId);

        List<Product> products = new ArrayList<>();
        for (ProductRequestDto request : requests) {
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
            products.add(product);
        }

        try {
            List<Product> savedProducts = productRepository.saveAll(products);
            // Publish events for each created product
            savedProducts.forEach(product -> {
                ProductEvent event = new ProductEvent(
                        "PRODUCT_CREATED",
                        product.getId(),
                        product.getSellerId(),
                        LocalDateTime.now()
                );
                kafkaTemplate.send(productEventsTopic, product.getId(),event);
            });

            logger.info("{} products saved and events published", savedProducts.size());
            return new ApiResponseDto("success", "Products added successfully", savedProducts);
        } catch (Exception e) {
            logger.error("Error saving products: {}", e.getMessage(), e);
            return new ApiResponseDto("error", "Failed to save products: " + e.getMessage(), null);
        }
    }

    @CacheEvict(value = "products", allEntries = true)
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

    @Cacheable(value = "products", key = "'allProducts_' + #category + '_' + #subcategory + '_' + #brand + '_' + #minPrice + '_' + #maxPrice + '_' + #page + '_' + #size")
    public ApiResponseDto getAllProducts(String category, String subcategory, String brand, Double minPrice, Double maxPrice, int page, int size) {

        logger.info("Fetching products with filters - Category: {}, Subcategory: {}, Brand: {}, Min Price: {}, Max Price: {}, Page: {}, Size: {}",
                category, subcategory, brand, minPrice, maxPrice, page, size);


        // Fetch paginated products from the repository
        List<Product> productList = productRepository.findByFilters(category, subcategory, brand, minPrice, maxPrice, page, size);

        // Check if the page is empty
        if (productList.isEmpty()) {
            logger.warn("No products found for the given filters and page");
            return new ApiResponseDto("error", "No products found", Collections.emptyList());
        }

        logger.info("Successfully fetched {} products ", productList.size());
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

    @Cacheable(value = "products", key = "'seller_' + #sellerId + '_page_' + #page + '_size_' + #size")
    public ApiResponseDto getProductsBySellerId(UUID sellerId, int page, int size) {

        logger.info("Fetching products for seller ID: {}", sellerId);

        Pageable pageable = PageRequest.of(page, size); // Create pagination object
        List<Product> products = productRepository.findBySellerId(sellerId, pageable);
        if (products.isEmpty()) {
            logger.warn("No products found for seller ID: {}", sellerId);
            return new ApiResponseDto("error", "No products found for the seller", Collections.emptyList());
        }

        logger.info("Successfully fetched {} products for seller ID: {}", products.size(), sellerId);
        return new ApiResponseDto("success", "Products retrieved successfully", products);
    }

    @Cacheable(value = "products", key = "'category_' + #category + '_page_' + #page + '_size_' + #size")
    public ApiResponseDto getProductsByCategory(String category, int page, int size) {
        logger.info("Fetching products for category: {}", category);

        Pageable pageable = PageRequest.of(page, size);
        List<Product> products = productRepository.findByCategory(category,pageable);
        if (products.isEmpty()) {
            logger.warn("No products found for category: {}", category);
            return new ApiResponseDto("error", "No products found for this category", null);
        }

        logger.info("Successfully fetched {} products for category: {}", products.size(), category);
        return new ApiResponseDto("success", "Products retrieved successfully", products);
    }

    @Cacheable(value = "products", key = "'category_' + #category + '_subcategory_' + #subcategory + '_page_' + #page + '_size_' + #size")
    public ApiResponseDto getAllProductsBySubcategory(String category, String subcategory, int page, int size) {
        logger.info("Fetching products for subcategory: {}", subcategory);

        Pageable pageable = PageRequest.of(page, size);
        List<Product> products = productRepository.findByCategoryAndSubcategory(category, subcategory, pageable);
        if (products.isEmpty()) {
            logger.warn("No products found for subcategory: {}", subcategory);
            return new ApiResponseDto("error", "No products found for this subcategory", null);
        }

        logger.info("Successfully fetched {} products for subcategory: {}", products.size(), subcategory);
        return new ApiResponseDto("success", "Products retrieved successfully", products);

    }

    @CacheEvict(value = "products", allEntries = true)
    public ApiResponseDto deleteProductById(String productId) {
        logger.info("Attempting to delete product with ID: {}", productId);

        productRepository.findById(productId)
                .orElseThrow(() -> new ProductServiceException("Product not found", HttpStatus.NOT_FOUND));

        try {
            // Delete from database first
            productRepository.deleteById(productId);

            // Publish deletion event
            ProductEvent event = new ProductEvent(
                    "PRODUCT_DELETED",
                    productId,
                    null,
                    LocalDateTime.now()
            );

            kafkaTemplate.send(productEventsTopic, productId, event);

            logger.info("Product with ID: {} successfully deleted and event published", productId);
            return new ApiResponseDto("success", "Product deleted successfully", null);

        } catch (Exception e) {
            logger.error("Error deleting product {}: {}", productId, e.getMessage());
            throw new ProductServiceException("Failed to delete product: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ApiResponseDto searchProducts(String query, int page, int size) {
        logger.info("Searching for products with query: {}", query);

        Pageable pageable = PageRequest.of(page, size);
        // Perform the search
        List<Product> products = productRepository.searchByNameOrDescription(query, pageable);

        if (products.isEmpty()) {
            logger.warn("No products found for query: {}", query);
            return new ApiResponseDto("error", "No products found", Collections.emptyList());
        }

        logger.info("Successfully found {} products for query: {}", products.size(), query);
        return new ApiResponseDto("success", "Products retrieved successfully", products);
    }
}