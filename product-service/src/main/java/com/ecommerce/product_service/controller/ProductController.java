package com.ecommerce.product_service.controller;

import com.ecommerce.product_service.dto.ApiResponseDto;
import com.ecommerce.product_service.dto.ProductRequestDto;
import com.ecommerce.product_service.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PreAuthorize("hasRole('SELLER')")
    @PostMapping("/add")
    public ResponseEntity<ApiResponseDto> createProduct(@RequestBody ProductRequestDto request, @RequestHeader("Authorization") String authToken) {
        return ResponseEntity.ok(productService.createProduct(request, authToken));
    }

    @PreAuthorize("hasRole('SELLER')")
    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponseDto> updateProduct(@PathVariable String productId, @RequestBody ProductRequestDto request, @RequestHeader("Authorization") String authToken) {
        return ResponseEntity.ok(productService.updateProduct(productId, request, authToken));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('SELLER') or hasRole('CUSTOMER')")
    @GetMapping("/getAllProducts")
    public ResponseEntity<ApiResponseDto> getAllProducts(@RequestParam(required = false) String category,
                                                         @RequestParam(required = false) String subcategory,
                                                         @RequestParam(required = false) String brand,
                                                         @RequestParam(required = false) Double minPrice,
                                                         @RequestParam(required = false) Double maxPrice) {
        return ResponseEntity.ok(productService.getAllProducts(category, subcategory, brand, minPrice, maxPrice));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('SELLER') or hasRole('CUSTOMER')")
    @GetMapping("/getProduct")
    public ResponseEntity<ApiResponseDto> getProductById(@RequestParam String productId) {
        return ResponseEntity.ok(productService.getProductById(productId));
    }

    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<ApiResponseDto> getProductsBySellerId(@PathVariable UUID sellerId) {
        return ResponseEntity.ok(productService.getProductsBySellerId(sellerId));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('SELLER') or hasRole('CUSTOMER')")
    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponseDto> getProductsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(productService.getProductsByCategory(category));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('SELLER') or hasRole('CUSTOMER')")
    @GetMapping("/category/{category}/subcategory/{subcategory}")
    public ResponseEntity<ApiResponseDto> getAllProductsBySubcategory(@PathVariable String category, @PathVariable String subcategory) {
        return ResponseEntity.ok(productService.getAllProductsBySubcategory(category, subcategory));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('SELLER')")
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponseDto> deleteProductById(@PathVariable String productId) {
        return ResponseEntity.ok(productService.deleteProductById(productId));
    }








}
