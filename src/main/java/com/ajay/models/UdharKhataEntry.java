package com.ajay.models;

import javafx.beans.property.*;

public class UdharKhataEntry {
    private final IntegerProperty id;
    private final StringProperty customerName;
    private final StringProperty phone;
    private final StringProperty productName;
    private final DoubleProperty amount;
    private final DoubleProperty paidAmount;
    private final StringProperty date;
    private final StringProperty status;
    private final StringProperty description;

    public UdharKhataEntry(int id, String customerName, String phone, String productName, 
                          double amount, double paidAmount, String date, String description) {
        this.id = new SimpleIntegerProperty(id);
        this.customerName = new SimpleStringProperty(customerName);
        this.phone = new SimpleStringProperty(phone);
        this.productName = new SimpleStringProperty(productName);
        this.amount = new SimpleDoubleProperty(amount);
        this.paidAmount = new SimpleDoubleProperty(paidAmount);
        this.date = new SimpleStringProperty(date);
        this.status = new SimpleStringProperty(amount - paidAmount > 0 ? "Pending" : "Paid");
        this.description = new SimpleStringProperty(description);
    }

    // Property getters
    public IntegerProperty idProperty() { return id; }
    public StringProperty customerNameProperty() { return customerName; }
    public StringProperty phoneProperty() { return phone; }
    public StringProperty productNameProperty() { return productName; }
    public DoubleProperty amountProperty() { return amount; }
    public DoubleProperty paidAmountProperty() { return paidAmount; }
    public StringProperty dateProperty() { return date; }
    public StringProperty statusProperty() { return status; }
    public StringProperty descriptionProperty() { return description; }

    // Regular getters
    public int getId() { return id.get(); }
    public String getCustomerName() { return customerName.get(); }
    public String getPhone() { return phone.get(); }
    public String getProductName() { return productName.get(); }
    public double getAmount() { return amount.get(); }
    public double getPaidAmount() { return paidAmount.get(); }
    public String getDate() { return date.get(); }
    public String getStatus() { return status.get(); }
    public String getDescription() { return description.get(); }

    // Setters
    public void setId(int id) { this.id.set(id); }
    public void setCustomerName(String name) { this.customerName.set(name); }
    public void setPhone(String phone) { this.phone.set(phone); }
    public void setProductName(String product) { this.productName.set(product); }
    public void setAmount(double amount) { 
        this.amount.set(amount); 
        this.status.set(amount - getPaidAmount() > 0 ? "Pending" : "Paid");
    }
    public void setPaidAmount(double paidAmount) { 
        this.paidAmount.set(paidAmount); 
        this.status.set(getAmount() - paidAmount > 0 ? "Pending" : "Paid");
    }
    public void setDate(String date) { this.date.set(date); }
    public void setDescription(String desc) { this.description.set(desc); }
}