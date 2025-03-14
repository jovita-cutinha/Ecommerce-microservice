package com.ecommerce.product_service.service;

import com.ecommerce.product_service.dto.ApiResponseDto;
import com.ecommerce.product_service.dto.ProductRequestDto;
import com.ecommerce.product_service.model.Product;
import com.ecommerce.product_service.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Optional;
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

                    logger.info("Saving product: {}", product.getName());

                    productRepository.save(product);

                    logger.info("Product saved successfully ");

                    return Mono.just(new ApiResponseDto("success", "Product added successfully", request));
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

                    // Find the product by ID (blocking call, returns Optional<Product>)
                    Optional<Product> existingProductOptional = productRepository.findById(productId);

                    logger.info("Product found: {}", existingProductOptional.get().getId());

                    // Convert Optional<Product> to Mono<Product>
                    return Mono.justOrEmpty(existingProductOptional)
                            .flatMap(existingProduct -> {
                                // Check if the seller owns the product
                                if (!existingProduct.getSellerId().equals(sellerId)) {
                                    logger.warn("Unauthorized update attempt. Seller ID mismatch for product: {}", productId);
                                    return Mono.just(new ApiResponseDto("error", "Unauthorized: You can only update your own products", null));
                                }

                                // Update product details
                                existingProduct.setName(request.name());
                                existingProduct.setDescription(request.description());
                                existingProduct.setPrice(request.price());
                                existingProduct.setCategory(request.category());
                                existingProduct.setSubcategory(request.subcategory());
                                existingProduct.setBrand(request.brand());
                                existingProduct.setImages(request.images());
                                existingProduct.setSpecifications(request.specifications());
                                existingProduct.setUpdatedAt(LocalDateTime.now());

                                logger.info("Updating product: {}", existingProduct.getId());

                                // Save the updated product (blocking call)
                                Product updatedProduct = productRepository.save(existingProduct);

                                logger.info("Product updated successfully: {}", updatedProduct.getId());

                                // Return a success response
                                return Mono.just(new ApiResponseDto("success", "Product updated successfully", updatedProduct));
                            })
                            .switchIfEmpty(Mono.just(new ApiResponseDto("error", "Product not found", null)));
                });
    }


}