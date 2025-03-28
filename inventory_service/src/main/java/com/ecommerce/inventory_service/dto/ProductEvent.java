package com.ecommerce.inventory_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class ProductEvent {
    private String eventType;    // "PRODUCT_CREATED", "PRODUCT_UPDATED", "PRODUCT_DELETED"
    private String productId;
    private UUID sellerId;
    private LocalDateTime timestamp;

    public ProductEvent(String eventType, String productId, UUID sellerId, LocalDateTime timestamp) {
        this.eventType = eventType;
        this.productId = productId;
        this.sellerId = sellerId;
        this.timestamp = timestamp;
    }

    public ProductEvent() {
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public void setSellerId(UUID sellerId) {
        this.sellerId = sellerId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

