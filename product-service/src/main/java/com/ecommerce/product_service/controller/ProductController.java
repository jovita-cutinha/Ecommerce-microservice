package com.ecommerce.product_service.controller;

import com.ecommerce.product_service.dto.ApiResponseDto;
import com.ecommerce.product_service.dto.ProductRequestDto;
import com.ecommerce.product_service.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PreAuthorize("hasRole('SELLER')")
    @PostMapping("/add")
    public ResponseEntity<Mono<ApiResponseDto>> createProduct(@RequestBody ProductRequestDto request, @RequestHeader("Authorization") String authToken) {
        return ResponseEntity.ok(productService.createProduct(request, authToken));
    }

    @PreAuthorize("hasRole('SELLER')")
    @PutMapping("/update")
    public ResponseEntity<Mono<ApiResponseDto>> updateProduct(@RequestParam String productId, @RequestBody ProductRequestDto request, @RequestHeader("Authorization") String authToken) {
        return ResponseEntity.ok(productService.updateProduct(productId, request, authToken));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/getAllProducts")
    public ResponseEntity<Mono<ApiResponseDto>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('SELLER')")
    @GetMapping("/getProduct")
    public ResponseEntity<Mono<ApiResponseDto>> getProduct(@RequestParam String productId) {
        return ResponseEntity.ok(productService.getProduct(productId));
    }





}
