package com.ecommerce.inventory_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDto {
    private String id;
    private String name;
    private String description;
    private Double price;
    private String category;
    private String subcategory;
    private String brand;
    private UUID sellerId;
    private List<String> images;
    private Map<String, String> specifications;

    public String getId() {
        return id;
    }

    public ProductDto() {
    }

    public String getName() {
        return name;
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

    public UUID getSellerId() {
        return sellerId;
    }

    public List<String> getImages() {
        return images;
    }

    public Map<String, String> getSpecifications() {
        return specifications;
    }

    public void setId(String id) {
        this.id = id;
    }


    public void setName(String name) {
        this.name = name;
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


    public void setSellerId(UUID sellerId) {
        this.sellerId = sellerId;
    }


    public void setImages(List<String> images) {
        this.images = images;
    }

    public void setSpecifications(Map<String, String> specifications) {
        this.specifications = specifications;
    }

    public ProductDto(String id, String name, String description, Double price, String category, String subcategory, String brand, UUID sellerId, List<String> images, Map<String, String> specifications) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.subcategory = subcategory;
        this.brand = brand;
        this.sellerId = sellerId;
        this.images = images;
        this.specifications = specifications;
    }

    @Override
    public String toString() {
        return "ProductDto{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", category='" + category + '\'' +
                ", subcategory='" + subcategory + '\'' +
                ", brand='" + brand + '\'' +
                ", sellerId=" + sellerId +
                ", images=" + images +
                ", specifications=" + specifications +
                '}';
    }
}