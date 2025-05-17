package com.ajay.models;

import javafx.beans.property.*;

public class InventoryItem {
    private final IntegerProperty id;
    private final StringProperty name;
    private final StringProperty category;
    private final StringProperty unit;
    private final DoubleProperty currentStock;
    private final DoubleProperty alertThreshold;

    public InventoryItem(int id, String name, double currentStock, 
                       double alertThreshold, String unit, String category) {
        this.id = new SimpleIntegerProperty(id);
        this.name = new SimpleStringProperty(name);
        this.currentStock = new SimpleDoubleProperty(currentStock);
        this.alertThreshold = new SimpleDoubleProperty(alertThreshold);
        this.unit = new SimpleStringProperty(unit);
        this.category = new SimpleStringProperty(category);
    }

    // Property getters
    public IntegerProperty idProperty() { return id; }
    public StringProperty nameProperty() { return name; }
    public StringProperty categoryProperty() { return category; }
    public StringProperty unitProperty() { return unit; }
    public DoubleProperty currentStockProperty() { return currentStock; }
    public DoubleProperty alertThresholdProperty() { return alertThreshold; }

    // Regular getters
    public int getId() { return id.get(); }
    public String getName() { return name.get(); }
    public String getCategory() { return category.get(); }
    public String getUnit() { return unit.get(); }
    public double getCurrentStock() { return currentStock.get(); }
    public double getAlertThreshold() { return alertThreshold.get(); }

    // Setters
    public void setId(int id) { this.id.set(id); }
    public void setName(String name) { this.name.set(name); }
    public void setCategory(String category) { this.category.set(category); }
    public void setUnit(String unit) { this.unit.set(unit); }
    public void setCurrentStock(double currentStock) { this.currentStock.set(currentStock); }
    public void setAlertThreshold(double alertThreshold) { this.alertThreshold.set(alertThreshold); }

    // In InventoryItem.java
@Override
public String toString() {
    return String.format("%s - Current: %.2f %s, Threshold: %.2f", 
        getName(), getCurrentStock(), getUnit(), getAlertThreshold());
}
}