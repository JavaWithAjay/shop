package com.ajay.models;

public class BillItem {
    private String productName;
    private String unit;
    private double quantity;
    private double price; // selling price
    private double purchasePrice;
    private String priceLevel; // Customer type like RETAILER, WHOLESALER, etc.
    private double profit;

    public BillItem(String productName, String unit, double quantity, double price,
                    double purchasePrice, String priceLevel, double profit) {
        this.productName = productName;
        this.unit = unit;
        this.quantity = quantity;
        this.price = price;
        this.purchasePrice = purchasePrice;
        this.priceLevel = priceLevel;
        this.profit = profit;
    }

    // Getters
    public String getProductName() {
        return productName;
    }

    public String getUnit() {
        return unit;
    }

    public double getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }

    public double getPurchasePrice() {
        return purchasePrice;
    }

    public String getPriceLevel() {
        return priceLevel;
    }

    public double getProfit() {
        return profit;
    }

    public double getTotal() {
        return quantity * price;
    }

    // Optional setters if needed
    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setPurchasePrice(double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public void setProfit(double profit) {
        this.profit = profit;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setPriceLevel(String priceLevel) {
        this.priceLevel = priceLevel;
    }
}
