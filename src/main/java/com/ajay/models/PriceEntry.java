package com.ajay.models;

import javafx.beans.property.*;

public class PriceEntry {
    private final IntegerProperty id;
    private final StringProperty name;
    private final StringProperty unit;
    private final DoubleProperty stockQuantity;
    private final DoubleProperty purchasePrice;
    private final DoubleProperty sellingPrice;
    private final DoubleProperty minSellingPrice;
    private final StringProperty customerType;
    private final StringProperty category;

    public PriceEntry(int id, String name, double purchasePrice, double sellingPrice, 
                    double minSellingPrice, double stockQuantity, String unit, 
                    String customerType, String category) {
        this.id = new SimpleIntegerProperty(id);
        this.name = new SimpleStringProperty(name);
        this.unit = new SimpleStringProperty(unit);
        this.stockQuantity = new SimpleDoubleProperty(stockQuantity);
        this.purchasePrice = new SimpleDoubleProperty(purchasePrice);
        this.sellingPrice = new SimpleDoubleProperty(sellingPrice);
        this.minSellingPrice = new SimpleDoubleProperty(minSellingPrice);
        this.customerType = new SimpleStringProperty(customerType);
        this.category = new SimpleStringProperty(category);
    }

    public PriceEntry(PriceEntry selectedEntry) {
    this.id = new SimpleIntegerProperty(selectedEntry.getId());
    this.name = new SimpleStringProperty(selectedEntry.getName());
    this.unit = new SimpleStringProperty(selectedEntry.getUnit());
    this.stockQuantity = new SimpleDoubleProperty(selectedEntry.getStockQuantity());
    this.purchasePrice = new SimpleDoubleProperty(selectedEntry.getPurchasePrice());
    this.sellingPrice = new SimpleDoubleProperty(selectedEntry.getSellingPrice());
    this.minSellingPrice = new SimpleDoubleProperty(selectedEntry.getMinSellingPrice());
    this.customerType = new SimpleStringProperty(selectedEntry.getCustomerType());
    this.category = new SimpleStringProperty(selectedEntry.getCategory());
}


    // Property getters
    public IntegerProperty idProperty() { return id; }
    public StringProperty nameProperty() { return name; }
    public StringProperty unitProperty() { return unit; }
    public DoubleProperty stockQuantityProperty() { return stockQuantity; }
    public DoubleProperty purchasePriceProperty() { return purchasePrice; }
    public DoubleProperty sellingPriceProperty() { return sellingPrice; }
    public DoubleProperty minSellingPriceProperty() { return minSellingPrice; }
    public StringProperty customerTypeProperty() { return customerType; }
    public StringProperty categoryProperty() { return category; }

    // Regular getters
    public int getId() { return id.get(); }
    public String getName() { return name.get(); }
    public String getUnit() { return unit.get(); }
    public double getStockQuantity() { return stockQuantity.get(); }
    public double getPurchasePrice() { return purchasePrice.get(); }
    public double getSellingPrice() { return sellingPrice.get(); }
    public double getMinSellingPrice() { return minSellingPrice.get(); }
    public String getCustomerType() { return customerType.get(); }
    public String getCategory() { return category.get(); }

    // Setters
    public void setId(int id) { this.id.set(id); }
    public void setName(String name) { this.name.set(name); }
    public void setUnit(String unit) { this.unit.set(unit); }
    public void setStockQuantity(double stockQuantity) { this.stockQuantity.set(stockQuantity); }
    public void setPurchasePrice(double purchasePrice) { this.purchasePrice.set(purchasePrice); }
    public void setSellingPrice(double sellingPrice) { this.sellingPrice.set(sellingPrice); }
    public void setMinSellingPrice(double minSellingPrice) { this.minSellingPrice.set(minSellingPrice); }
    public void setCustomerType(String customerType) { this.customerType.set(customerType); }
    public void setCategory(String category) { this.category.set(category); }
}