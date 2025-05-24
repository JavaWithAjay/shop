package com.ajay.models;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class BillItem {
    private final SimpleIntegerProperty id;
    private final SimpleIntegerProperty billId;
    private final SimpleStringProperty productName;
    private final SimpleStringProperty unit;
    private final SimpleDoubleProperty quantity;
    private final SimpleDoubleProperty price;
    private final SimpleDoubleProperty total;
    private final SimpleDoubleProperty purchasePrice;
    private final SimpleDoubleProperty profit;
    private final SimpleStringProperty priceLevel;

    public BillItem(int id, int billId, String productName, String unit, double quantity, 
                    double price, double purchasePrice, String priceLevel, double profit) {
        this.id = new SimpleIntegerProperty(id);
        this.billId = new SimpleIntegerProperty(billId);
        this.productName = new SimpleStringProperty(productName);
        this.unit = new SimpleStringProperty(unit);
        this.quantity = new SimpleDoubleProperty(quantity);
        this.price = new SimpleDoubleProperty(price);
        this.total = new SimpleDoubleProperty(price * quantity);
        this.purchasePrice = new SimpleDoubleProperty(purchasePrice);
        this.profit = new SimpleDoubleProperty(profit);
        this.priceLevel = new SimpleStringProperty(priceLevel);
    }

    // Property getters
    public SimpleIntegerProperty idProperty() { return id; }
    public SimpleIntegerProperty billIdProperty() { return billId; }
    public SimpleStringProperty productNameProperty() { return productName; }
    public SimpleStringProperty unitProperty() { return unit; }
    public SimpleDoubleProperty quantityProperty() { return quantity; }
    public SimpleDoubleProperty priceProperty() { return price; }
    public SimpleDoubleProperty totalProperty() { return total; }
    public SimpleDoubleProperty purchasePriceProperty() { return purchasePrice; }
    public SimpleDoubleProperty profitProperty() { return profit; }
    public SimpleStringProperty priceLevelProperty() { return priceLevel; }

    // Regular getters
    public int getId() { return id.get(); }
    public int getBillId() { return billId.get(); }
    public String getProductName() { return productName.get(); }
    public String getUnit() { return unit.get(); }
    public double getQuantity() { return quantity.get(); }
    public double getPrice() { return price.get(); }
    public double getTotal() { return total.get(); }
    public double getPurchasePrice() { return purchasePrice.get(); }
    public double getProfit() { return profit.get(); }
    public String getPriceLevel() { return priceLevel.get(); }

    // Setters
    public void setId(int id) { this.id.set(id); }
    public void setBillId(int billId) { this.billId.set(billId); }
    public void setProductName(String productName) { this.productName.set(productName); }
    public void setUnit(String unit) { this.unit.set(unit); }
    public void setQuantity(double quantity) {
        this.quantity.set(quantity);
        this.total.set(this.price.get() * quantity);
        this.profit.set((this.price.get() - this.purchasePrice.get()) * quantity);
    }
    public void setPrice(double price) {
        this.price.set(price);
        this.total.set(price * this.quantity.get());
        this.profit.set((price - this.purchasePrice.get()) * this.quantity.get());
    }
    public void setPurchasePrice(double purchasePrice) {
        this.purchasePrice.set(purchasePrice);
        this.profit.set((this.price.get() - purchasePrice) * this.quantity.get());
    }
    public void setPriceLevel(String priceLevel) { this.priceLevel.set(priceLevel); }

    public char[] getSellingPrice() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSellingPrice'");
    }
}