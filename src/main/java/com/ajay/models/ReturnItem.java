package com.ajay.models;

import javafx.beans.property.*;
import java.time.LocalDate;

public class ReturnItem {
    private final StringProperty customerName;
    private final StringProperty productName;
    private final DoubleProperty quantity;
    private final DoubleProperty price;
    private final ObjectProperty<LocalDate> returnDate;
    private final StringProperty reason;
    private final StringProperty status;

    public ReturnItem(String customerName, String productName, double quantity, double price, 
                     LocalDate returnDate, String reason, String status) {
        this.customerName = new SimpleStringProperty(customerName);
        this.productName = new SimpleStringProperty(productName);
        this.quantity = new SimpleDoubleProperty(quantity);
        this.price = new SimpleDoubleProperty(price);
        this.returnDate = new SimpleObjectProperty<>(returnDate);
        this.reason = new SimpleStringProperty(reason);
        this.status = new SimpleStringProperty(status);
    }

    // Property getters
    public StringProperty customerNameProperty() { return customerName; }
    public StringProperty productNameProperty() { return productName; }
    public DoubleProperty quantityProperty() { return quantity; }
    public DoubleProperty priceProperty() { return price; }
    public ObjectProperty<LocalDate> returnDateProperty() { return returnDate; }
    public StringProperty reasonProperty() { return reason; }
    public StringProperty statusProperty() { return status; }

    // Standard getters
    public String getCustomerName() { return customerName.get(); }
    public String getProductName() { return productName.get(); }
    public double getQuantity() { return quantity.get(); }
    public double getPrice() { return price.get(); }
    public LocalDate getReturnDate() { return returnDate.get(); }
    public String getReason() { return reason.get(); }
    public String getStatus() { return status.get(); }

    // Setters
    public void setCustomerName(String customerName) { this.customerName.set(customerName); }
    public void setProductName(String productName) { this.productName.set(productName); }
    public void setQuantity(double quantity) { this.quantity.set(quantity); }
    public void setPrice(double price) { this.price.set(price); }
    public void setReturnDate(LocalDate returnDate) { this.returnDate.set(returnDate); }
    public void setReason(String reason) { this.reason.set(reason); }
    public void setStatus(String status) { this.status.set(status); }

   private String billNumber;

public String getBillNumber() {
    return billNumber; // ✅ Correct way
}

}