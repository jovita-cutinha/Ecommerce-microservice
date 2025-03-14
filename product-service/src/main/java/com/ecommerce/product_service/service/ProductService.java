package com.ecommerce.product_service.service;

import com.ecommerce.product_service.dto.ApiResponseDto;
import com.ecommerce.product_service.dto.ProductRequestDto;
import com.ecommerce.product_service.model.Product;
import com.ecommerce.product_service.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final WebClientService webClientService;

    public ProductService(ProductRepository productRepository, WebClientService webClientService) {
        this.productRepository = productRepository;
        this.webClientService = webClientService;
    }

    public Mono<ApiResponseDto> createProduct(ProductRequestDto request, String authToken) {
        logger.info("Received request to create product: {}", request.name());

        return webClientService.getSellerIdByToken(authToken) // Fetch Seller ID using WebClient
                .flatMap(sellerId -> {
                    if (sellerId == null) {
                        return Mono.just(new ApiResponseDto("error", "Seller ID not found", null));
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
                            sellerId,  // Dynamically fetched Seller ID
                            request.images(),
                            request.specifications(),
                            LocalDateTime.now(),
                            LocalDateTime.now()
                    );

                    return productRepository.save(product)
                            .doOnSuccess(savedProduct -> logger.info("Product saved successfully: {}", savedProduct.getId()))
                            .map(savedProduct -> new ApiResponseDto("success", "Product added successfully", savedProduct))
                            .onErrorResume(e -> {
                                logger.error("Error saving product: {}", e.getMessage(), e);
                                return Mono.just(new ApiResponseDto("error", "Failed to save product: " + e.getMessage(), null));
                            });
                })
                .onErrorResume(e -> {
                    logger.error("Error fetching seller ID: {}", e.getMessage(), e);
                    return Mono.just(new ApiResponseDto("error", "Failed to fetch seller ID: " + e.getMessage(), null));
                })
                .defaultIfEmpty(new ApiResponseDto("error", "Unauthorized access", null));
    }

    public Mono<ApiResponseDto> updateProduct(String productId, ProductRequestDto request, String authToken) {

        logger.info("Received request to update product: {}", productId);

        return webClientService.getSellerIdByToken(authToken) // Fetch Seller ID from User Service
                .flatMap(sellerId -> {
                    if (sellerId == null) {
                        return Mono.just(new ApiResponseDto("error", "Unauthorized access", null));
                    }

                    logger.info("Seller ID retrieved: {}", sellerId);

                    return productRepository.findById(productId)
                            .flatMap(existingProduct -> {
                                // Check if the seller owns the product
                                if (!existingProduct.getSellerId().equals(sellerId)) {
                                    return Mono.just(new ApiResponseDto("error", "Unauthorized access", null));
                                }

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

                                return productRepository.save(existingProduct)
                                        .doOnSuccess(updatedProduct -> logger.info("Product updated successfully: {}", updatedProduct.getId()))
                                        .map(updatedProduct -> new ApiResponseDto("success", "Product updated successfully", updatedProduct))
                                        .onErrorResume(e -> {
                                            logger.error("Error updating product: {}", e.getMessage(), e);
                                            return Mono.just(new ApiResponseDto("error", "Failed to update product: " + e.getMessage(), null));
                                        });
                            })
                            .switchIfEmpty(Mono.just(new ApiResponseDto("error", "Product not found", null)))
                            .onErrorResume(e -> {
                                logger.error("Error finding product: {}", e.getMessage(), e);
                                return Mono.just(new ApiResponseDto("error", "Failed to find product: " + e.getMessage(), null));
                            });
                })
                .onErrorResume(e -> {
                    logger.error("Error fetching seller ID: {}", e.getMessage(), e);
                    return Mono.just(new ApiResponseDto("error", "Failed to fetch seller ID: " + e.getMessage(), null));
                });
    }


    public Mono<ApiResponseDto> getAllProducts() {
        logger.info("Fetching all products from database.");

        return productRepository.findAll()
                .collectList()  // Convert Flux<Product> to Mono<List<Product>>
                .flatMap(productList -> {
                    if (productList.isEmpty()) {
                        logger.warn("No products found");
                        return Mono.just(new ApiResponseDto("error", "No products found", Collections.emptyList()));
                    }
                    logger.info("Successfully fetched {} products.", productList.size());
                    return Mono.just(new ApiResponseDto("success", "Products retrieved successfully", productList));
                })
                .onErrorResume(e -> {
                    logger.error("Error fetching products: {}", e.getMessage(), e);
                    return Mono.just(new ApiResponseDto("error", "An error occurred while fetching products", null));
                });
    }

    public Mono<ApiResponseDto> getProductById(String productId) {
        logger.info("Fetching product with ID: {}", productId);

        return productRepository.findById(productId)
                .map(product -> {
                    logger.info("Product found: {}", product.getName());
                    return new ApiResponseDto("success", "Product retrieved successfully", product);
                })
                .defaultIfEmpty(new ApiResponseDto("error", "Product not found", null))
                .doOnError(e -> logger.error("Error fetching product: {}", e.getMessage()));
    }

    public Mono<ApiResponseDto> getProductsBySellerId(UUID sellerId) {
        logger.info("Fetching products for seller ID: {}", sellerId);

        // Retrieve all products for the given sellerId
        Flux<Product> productsFlux = productRepository.findBySellerId(sellerId);

        return productsFlux.hasElements()
                .flatMap(hasElements -> {
                    if (!hasElements) {
                        logger.warn("No products found for seller ID: {}", sellerId);
                        return Mono.just(new ApiResponseDto("error", "No products found for the seller", Collections.emptyList()));
                    }

                    return productsFlux.collectList()
                            .doOnSuccess(productList ->
                                    logger.info("Successfully fetched {} products for seller ID: {}", productList.size(), sellerId))
                            .map(productList -> new ApiResponseDto("success", "Products retrieved successfully", productList));
                })
                .onErrorResume(e -> {
                    logger.error("Error fetching products for seller ID {}: {}", sellerId, e.getMessage());
                    return Mono.just(new ApiResponseDto("error", "An error occurred while fetching products", null));
                });
    }

    public Mono<ApiResponseDto> getProductsByCategory(String category) {
        logger.info("Fetching products for category: {}", category);

        return productRepository.findByCategory(category)
                .collectList()
                .flatMap(products -> {
                    if(products.isEmpty()) {
                        logger.warn("No products found for category: {}", category);
                        return Mono.just(new ApiResponseDto("error", "No products found for this category", null));
                    }
                    logger.info("Successfully fetched {} products for category: {}", products.size(), category);
                    return Mono.just(new ApiResponseDto("success", "Products retrieved successfully", products));
                })
                .doOnError(error -> logger.error("Error fetching products for category {}: {}", category, error.getMessage()))
                .onErrorResume(e -> Mono.just(new ApiResponseDto("error", "An error occurred while fetching products", null)));
    }
}