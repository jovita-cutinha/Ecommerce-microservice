package com.ecommerce.order_service.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userKeycloakId;

    private LocalDateTime orderDate;
    private String status;

    private BigDecimal totalAmount;

    // Unidirectional OneToMany
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "order_id") // creates FK in order_items table
    private List<OrderItem> orderItems = new ArrayList<>();

    @Embedded
    private ShippingAddress shippingAddress;

    public Order() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserKeycloakId() {
        return userKeycloakId;
    }

    public void setUserKeycloakId(String userKeycloakId) {
        this.userKeycloakId = userKeycloakId;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    public ShippingAddress getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(ShippingAddress shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public Order(Long id, String userKeycloakId, LocalDateTime orderDate, String status, BigDecimal totalAmount, List<OrderItem> orderItems, ShippingAddress shippingAddress) {
        this.id = id;
        this.userKeycloakId = userKeycloakId;
        this.orderDate = orderDate;
        this.status = status;
        this.totalAmount = totalAmount;
        this.orderItems = orderItems;
        this.shippingAddress = shippingAddress;
    }
}

