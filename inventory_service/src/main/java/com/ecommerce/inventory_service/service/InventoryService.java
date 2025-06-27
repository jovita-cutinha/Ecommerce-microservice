package com.ecommerce.inventory_service.service;

import com.ecommerce.inventory_service.dto.ApiResponseDto;
import com.ecommerce.inventory_service.dto.LowStockItemDto;
import com.ecommerce.inventory_service.dto.ProductDto;
import com.ecommerce.inventory_service.dto.ProductEvent;
import com.ecommerce.inventory_service.exception.InventoryServiceException;
import com.ecommerce.inventory_service.model.Inventory;
import com.ecommerce.inventory_service.respository.InventoryRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InterServiceCall interServiceCall;

    public InventoryService(InventoryRepository inventoryRepository, InterServiceCall sellerServiceClient) {
        this.inventoryRepository = inventoryRepository;
        this.interServiceCall = sellerServiceClient;
    }

    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);

    public void handleProductCreated(ProductEvent event) {
        logger.info("Received event: {}", event);
        // Create new inventory record with 0 initial quantity
        Inventory inventory = new Inventory();
        inventory.setProductId(event.getProductId());
        inventory.setSellerId(event.getSellerId());
        inventory.setAvailableQuantity(0);
        inventory.setReservedQuantity(0);
        inventory.setSoldQuantity(0);
        inventory.setCreatedAt(LocalDateTime.now());
        inventory.setUpdatedAt(LocalDateTime.now());
        inventoryRepository.save(inventory);
        logger.info("Created inventory record for product: {}", event.getProductId());
    }


    @Transactional
    public void handleProductDeleted(ProductEvent event) {
        inventoryRepository.deleteByProductId(event.getProductId());
        logger.info("Deleted inventory record for product: {}", event.getProductId());
    }

    @Transactional
    public ApiResponseDto updateInventory(Long inventoryId, int availableQuantity) {
        logger.info("Updating inventory with ID: {} | New availableQuantity: {}", inventoryId, availableQuantity);
        // Find inventory by ID
        Inventory updatedInventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> {
                    logger.error("Inventory not found with ID: {}", inventoryId);
                    return new InventoryServiceException("Inventory not found with ID: " + inventoryId, HttpStatus.NOT_FOUND);
                });
        // Log the old quantity before update
        logger.debug("Current availableQuantity for inventory ID {}: {}", inventoryId, updatedInventory.getAvailableQuantity());
        // Update available quantity
        updatedInventory.setAvailableQuantity(availableQuantity);
        // Save updated entity
        inventoryRepository.save(updatedInventory);
        // Log successful update
        logger.info("Successfully updated inventory ID {}. New availableQuantity: {}", inventoryId, availableQuantity);
        // Return response DTO
        return new ApiResponseDto("success","Inventory updated successfully", updatedInventory);
    }

    public ApiResponseDto getInventoryByProductId(String productId) {
        logger.info("Fetching inventory for productId: {}", productId);
        Inventory inventory = inventoryRepository.findByProductId(productId);
        if (inventory == null) {
            logger.warn("Inventory not found for productId: {}", productId);
            throw new InventoryServiceException("Inventory not found for productId: " + productId, HttpStatus.NOT_FOUND);
        }
        logger.debug("Retrieved Inventory: {}", inventory);
        return new ApiResponseDto("success", "Inventory retrieved successfully", inventory);
    }

    public ApiResponseDto getLowStockItemsForSeller(String token, int threshold, int page, int size) {
        // Step 1: Get Seller ID from token
        UUID sellerId = interServiceCall.getSellerIdByToken(token);
        if (sellerId == null) {
            logger.error("Seller ID not found for token: {}", token);
            throw new InventoryServiceException("Unauthorized: seller not found", HttpStatus.UNAUTHORIZED);
        }
        logger.info("Fetching low-stock items for seller ID: {}", sellerId);
        // Step 2: Fetch inventory items for the seller
        Pageable pageable = PageRequest.of(page, size, Sort.by("availableQuantity").ascending());
        List<Inventory> lowStockItems = inventoryRepository.findBySellerIdAndAvailableQuantityLessThanEqual(sellerId, threshold, pageable);
        if (lowStockItems.isEmpty()) {
            return new ApiResponseDto("success", "No low stock items found for seller", Collections.emptyList());
        }
        // Step 3: Fetch product details for each low stock inventory item
        List<LowStockItemDto> results = lowStockItems.stream()
                .map(inventory -> {
                    try {
                        ProductDto product = interServiceCall.getProductById(inventory.getProductId(), token);
                        return new LowStockItemDto(
                                inventory.getProductId(),
                                product.getName(),
                                product.getImages(),
                                inventory.getAvailableQuantity(),
                                inventory.getReservedQuantity(),
                                product.getDescription(),
                                product.getPrice(),
                                product.getCategory(),
                                product.getSubcategory(),
                                product.getBrand()
                        );
                    } catch (Exception e) {
                        logger.error("Error fetching product info for productId: {}", inventory.getProductId(), e);
                        throw new InventoryServiceException("Failed to fetch product details", HttpStatus.BAD_GATEWAY);
                    }
                }).toList();
        logger.info("Found {} low stock items for seller ID: {}", results.size(), sellerId);
        return new ApiResponseDto("success", "Low stock items retrieved for seller", results);
    }
}
