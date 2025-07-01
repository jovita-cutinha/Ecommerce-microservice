package com.ecommerce.inventory_service.controller;

import com.ecommerce.inventory_service.dto.ApiResponseDto;
import com.ecommerce.inventory_service.model.Inventory;
import com.ecommerce.inventory_service.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    public final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @PutMapping("/{inventoryId}")
    public ResponseEntity<ApiResponseDto> updateInventory(
            @PathVariable Long inventoryId,
            @RequestParam int availableQuantity) {
        return ResponseEntity.ok(inventoryService.updateInventory(inventoryId, availableQuantity));
    }

    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN') or hasRole('CUSTOMER')")
    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponseDto> getInventoryByProductId(
            @PathVariable String productId) {
        return ResponseEntity.ok(inventoryService.getInventoryByProductId(productId));
    }

    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponseDto> getLowStockItems(
            JwtAuthenticationToken principal,
            @RequestParam(defaultValue = "5") int threshold,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(inventoryService.getLowStockItemsForSeller(principal, threshold, page, size));
    }

   // ------------------- Inter service call------------------------

    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')")
    @PutMapping("/reserve/{productId}")
    public ResponseEntity<Inventory> reserveProduct(
            @PathVariable String productId,
            @RequestParam int quantity) {
        return ResponseEntity.ok(inventoryService.reserveProductQuantity(productId, quantity));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')")
    @PutMapping("/release-reserve/{productId}")
    public ResponseEntity<Inventory> releaseReserveProduct(
            @PathVariable String productId,
            @RequestParam int quantity) {
        return ResponseEntity.ok(inventoryService.releaseReserveProductQuantity(productId, quantity));
    }

}
