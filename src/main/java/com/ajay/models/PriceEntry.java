package com.ajay.models;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class PriceEntry {
    private final SimpleIntegerProperty productId;
    private final SimpleStringProperty productName;
    private final SimpleStringProperty unit;
    private final SimpleDoubleProperty distributorPrice;
    private final SimpleDoubleProperty wholesalePrice;
    private final SimpleDoubleProperty retailPrice;
    private final SimpleDoubleProperty customerPrice;
    
    public PriceEntry(int productId, String productName, String unit, 
                    double distributorPrice, double wholesalePrice, 
                    double retailPrice, double customerPrice) {
        this.productId = new SimpleIntegerProperty(productId);
        this.productName = new SimpleStringProperty(productName);
        this.unit = new SimpleStringProperty(unit);
        this.distributorPrice = new SimpleDoubleProperty(distributorPrice);
        this.wholesalePrice = new SimpleDoubleProperty(wholesalePrice);
        this.retailPrice = new SimpleDoubleProperty(retailPrice);
        this.customerPrice = new SimpleDoubleProperty(customerPrice);
    }
    
    // Getters for properties
    public SimpleIntegerProperty productIdProperty() { return productId; }
    public SimpleStringProperty productNameProperty() { return productName; }
    public SimpleStringProperty unitProperty() { return unit; }
    public SimpleDoubleProperty distributorPriceProperty() { return distributorPrice; }
    public SimpleDoubleProperty wholesalePriceProperty() { return wholesalePrice; }
    public SimpleDoubleProperty retailPriceProperty() { return retailPrice; }
    public SimpleDoubleProperty customerPriceProperty() { return customerPrice; }
    
    // Regular getters
    public int getProductId() { return productId.get(); }
    public String getProductName() { return productName.get(); }
    public String getUnit() { return unit.get(); }
    public double getDistributorPrice() { return distributorPrice.get(); }
    public double getWholesalePrice() { return wholesalePrice.get(); }
    public double getRetailPrice() { return retailPrice.get(); }
    public double getCustomerPrice() { return customerPrice.get(); }
}