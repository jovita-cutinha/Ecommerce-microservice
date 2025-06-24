package com.ecommerce.inventory_service.dto;


import java.util.List;

public class LowStockItemDto {
    private String productId;
    private String productName;
    private List<String> productImage;
    private int availableQuantity;
    private int reservedQuantity;
    private String description;
    private Double price;
    private String category;
    private String subcategory;
    private String brand;

    public LowStockItemDto(String productId, String productName, List<String> productImage, int availableQuantity, int reservedQuantity, String description, Double price, String category, String subcategory, String brand) {
        this.productId = productId;
        this.productName = productName;
        this.productImage = productImage;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = reservedQuantity;
        this.description = description;
        this.price = price;
        this.category = category;
        this.subcategory = subcategory;
        this.brand = brand;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public List<String> getProductImage() {
        return productImage;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public String getDescription() {
        return description;
    }

    public Double getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public String getBrand() {
        return brand;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }


    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setProductImage(List<String> productImage) {
        this.productImage = productImage;
    }


    public void setDescription(String description) {
        this.description = description;
    }


    public void setPrice(Double price) {
        this.price = price;
    }


    public void setCategory(String category) {
        this.category = category;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }


    public void setBrand(String brand) {
        this.brand = brand;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }


    public void setReservedQuantity(int reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }
}