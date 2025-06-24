package com.ecommerce.inventory_service.service;

import com.ecommerce.inventory_service.dto.ProductEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaService.class);

    private final InventoryService inventoryService;

    public KafkaService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @KafkaListener(topics = "${kafka.topics.product-events}", groupId = "inventory-service-group")
    public void handleProductEvent(ProductEvent event) {
        logger.info("Received product event: {}", event);

        switch (event.getEventType()) {
            case "PRODUCT_CREATED":
                inventoryService.handleProductCreated(event);
                break;
            case "PRODUCT_DELETED":
                inventoryService.handleProductDeleted(event);
                break;
            default:
                logger.warn("Unknown event type: {}", event.getEventType());
        }
    }


}