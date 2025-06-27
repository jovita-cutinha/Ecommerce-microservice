package com.ecommerce.cart_service.dto;

public class CartSummaryDTO {
    private int totalItems;
    private double totalPrice;
    private double discount;
    private double finalAmount;

    public CartSummaryDTO(int totalItems, double totalPrice, double discount, double finalAmount) {
        this.totalItems = totalItems;
        this.totalPrice = totalPrice;
        this.discount = discount;
        this.finalAmount = finalAmount;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(double finalAmount) {
        this.finalAmount = finalAmount;
    }
}

