package com.ecommerce.inventory_service.service;

import com.ecommerce.inventory_service.dto.ProductEvent;
import com.ecommerce.inventory_service.model.Inventory;
import com.ecommerce.inventory_service.respository.InventoryRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
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
}
