package com.ajay.models;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

public class BillItem {
    private final SimpleStringProperty productName;
    private final SimpleStringProperty unit;
    private final SimpleDoubleProperty quantity;
    private final SimpleDoubleProperty sellingPrice;
    private final SimpleDoubleProperty purchasePrice;
    private final SimpleDoubleProperty total;
    
    public BillItem(String productName, String unit, double quantity, 
                   double sellingPrice, double purchasePrice) {
        this.productName = new SimpleStringProperty(productName);
        this.unit = new SimpleStringProperty(unit);
        this.quantity = new SimpleDoubleProperty(quantity);
        this.sellingPrice = new SimpleDoubleProperty(sellingPrice);
        this.purchasePrice = new SimpleDoubleProperty(purchasePrice);
        this.total = new SimpleDoubleProperty(quantity * sellingPrice);
    }
    
    // Getters for properties
    public SimpleStringProperty productNameProperty() { return productName; }
    public SimpleStringProperty unitProperty() { return unit; }
    public SimpleDoubleProperty quantityProperty() { return quantity; }
    public SimpleDoubleProperty sellingPriceProperty() { return sellingPrice; }
    public SimpleDoubleProperty totalProperty() { return total; }
    
    // Regular getters
    public String getProductName() { return productName.get(); }
    public String getUnit() { return unit.get(); }
    public double getQuantity() { return quantity.get(); }
    public double getSellingPrice() { return sellingPrice.get(); }
    public double getPurchasePrice() { return purchasePrice.get(); }
    public double getTotal() { return total.get(); }
}