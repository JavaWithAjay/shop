package com.ajay.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import com.ajay.models.PriceEntry;
import com.ajay.DatabaseConnection;
import java.sql.*;
import javafx.scene.control.Alert.AlertType;

public class PriceController {

    @FXML private TableView<PriceEntry> priceTable;
    @FXML private TableColumn<PriceEntry, Integer> idCol;
    @FXML private TableColumn<PriceEntry, String> nameCol;
    @FXML private TableColumn<PriceEntry, String> unitCol;
    @FXML private TableColumn<PriceEntry, Double> distributorCol;
    @FXML private TableColumn<PriceEntry, Double> wholesaleCol;
    @FXML private TableColumn<PriceEntry, Double> retailCol;
    @FXML private TableColumn<PriceEntry, Double> customerCol;
    
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCombo;
    @FXML private ComboBox<String> priceTypeCombo;
    @FXML private ComboBox<String> unitCombo;
    
    @FXML private TextField distributorPriceField;
    @FXML private TextField wholesalePriceField;
    @FXML private TextField retailPriceField;
    @FXML private TextField customerPriceField;
    
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    
    private ObservableList<PriceEntry> priceEntries = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Initialize table columns
        idCol.setCellValueFactory(new PropertyValueFactory<>("productId"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));
        distributorCol.setCellValueFactory(new PropertyValueFactory<>("distributorPrice"));
        wholesaleCol.setCellValueFactory(new PropertyValueFactory<>("wholesalePrice"));
        retailCol.setCellValueFactory(new PropertyValueFactory<>("retailPrice"));
        customerCol.setCellValueFactory(new PropertyValueFactory<>("customerPrice"));
        
        priceTable.setItems(priceEntries);
        
        // Initialize combo boxes
        priceTypeCombo.getItems().addAll("Distributor", "Wholesale", "Retail", "Customer");
        priceTypeCombo.getSelectionModel().selectFirst();
        
        loadUnits();
        loadFilterOptions();
        loadPriceData();
        
        // Set up selection listener
        priceTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    populateFormFields(newSelection);
                }
            });
    }
    
    private void loadUnits() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM units")) {
            
            while (rs.next()) {
                unitCombo.getItems().add(rs.getString("name"));
            }
            unitCombo.getItems().add("Other");
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load units");
        }
    }
    
    private void loadFilterOptions() {
        filterCombo.getItems().addAll(
            "All Products", 
            "Grains", 
            "Spices", 
            "Oil", 
            "Snacks", 
            "Beverages", 
            "Dairy", 
            "Other"
        );
        filterCombo.getSelectionModel().selectFirst();
    }
    
    private void loadPriceData() {
        priceEntries.clear();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT p.product_id, p.name, pv.quantity, u.name AS unit_name, " +
                 "pv.distributor_price, pv.wholesale_price, pv.retail_price, pv.selling_price " +
                 "FROM product_variations pv " +
                 "JOIN products p ON pv.product_id = p.product_id " +
                 "JOIN units u ON pv.unit_id = u.unit_id")) {
            
            while (rs.next()) {
                priceEntries.add(new PriceEntry(
                    rs.getInt("product_id"),
                    rs.getString("name"),
                    rs.getDouble("quantity") + " " + rs.getString("unit_name"),
                    rs.getDouble("distributor_price"),
                    rs.getDouble("wholesale_price"),
                    rs.getDouble("retail_price"),
                    rs.getDouble("selling_price")
                ));
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load price data");
        }
    }
    
    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText().toLowerCase();
        String filter = filterCombo.getValue();
        
        if (searchTerm.isEmpty() && filter.equals("All Products")) {
            priceTable.setItems(priceEntries);
            return;
        }
        
        ObservableList<PriceEntry> filteredList = FXCollections.observableArrayList();
        for (PriceEntry entry : priceEntries) {
            boolean matchesSearch = entry.getProductName().toLowerCase().contains(searchTerm);
            boolean matchesFilter = filter.equals("All Products") || 
                                  entry.getProductName().toLowerCase().contains(filter.toLowerCase());
            
            if (matchesSearch && matchesFilter) {
                filteredList.add(entry);
            }
        }
        priceTable.setItems(filteredList);
    }
    
    @FXML
    private void handleAddPrice() {
        try {
            // First we need to get the selected product from table
            PriceEntry selected = priceTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Selection Error", "Please select a product to update prices");
                return;
            }
            
            double distributorPrice = Double.parseDouble(distributorPriceField.getText());
            double wholesalePrice = Double.parseDouble(wholesalePriceField.getText());
            double retailPrice = Double.parseDouble(retailPriceField.getText());
            double customerPrice = Double.parseDouble(customerPriceField.getText());
            
            // Update in database
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE product_variations SET " +
                     "distributor_price = ?, wholesale_price = ?, retail_price = ?, selling_price = ? " +
                     "WHERE product_id = ?")) {
                
                stmt.setDouble(1, distributorPrice);
                stmt.setDouble(2, wholesalePrice);
                stmt.setDouble(3, retailPrice);
                stmt.setDouble(4, customerPrice);
                stmt.setInt(5, selected.getProductId());
                
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    showAlert("Success", "Prices updated successfully");
                    loadPriceData(); // Refresh data
                }
            }
        } catch (NumberFormatException e) {
            showAlert("Input Error", "Please enter valid prices");
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to update prices");
        }
    }
    
    @FXML
    private void handleUpdatePrice() {
        handleAddPrice(); // Same functionality for update in this case
    }
    
    @FXML
    private void handleDeletePrice() {
        PriceEntry selected = priceTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Error", "Please select a price entry to delete");
            return;
        }
        
        Alert confirmation = new Alert(AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete Price Entry");
        confirmation.setContentText("Are you sure you want to delete this price entry?");
        
        if (confirmation.showAndWait().get() == ButtonType.OK) {
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM product_variations WHERE product_id = ?")) {
                
                stmt.setInt(1, selected.getProductId());
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    showAlert("Success", "Price entry deleted successfully");
                    loadPriceData(); // Refresh data
                    clearFormFields();
                }
            } catch (SQLException e) {
                showAlert("Database Error", "Failed to delete price entry");
            }
        }
    }
    
    @FXML
    private void handleUnitOther() {
        if (unitCombo.getValue().equals("Other")) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Custom Unit");
            dialog.setHeaderText("Enter custom unit name");
            dialog.setContentText("Unit:");
            
            dialog.showAndWait().ifPresent(unit -> {
                unitCombo.getItems().add(unit);
                unitCombo.setValue(unit);
            });
        }
    }
    
    @FXML
    private void handleFilterOther() {
        if (filterCombo.getValue().equals("Other")) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Custom Filter");
            dialog.setHeaderText("Enter custom filter");
            dialog.setContentText("Filter:");
            
            dialog.showAndWait().ifPresent(filter -> {
                filterCombo.getItems().add(filter);
                filterCombo.setValue(filter);
            });
        }
    }
    
    @FXML
    private void handlePriceTypeChange() {
        // This can be used to show different price columns based on selection
        // For now we'll just highlight the selected price type column
        String selected = priceTypeCombo.getValue();
        resetColumnStyles();
        
        switch (selected) {
            case "Distributor":
                distributorCol.setStyle("-fx-background-color: #f5d76e;");
                break;
            case "Wholesale":
                wholesaleCol.setStyle("-fx-background-color: #f5d76e;");
                break;
            case "Retail":
                retailCol.setStyle("-fx-background-color: #f5d76e;");
                break;
            case "Customer":
                customerCol.setStyle("-fx-background-color: #f5d76e;");
                break;
        }
    }
    
    private void resetColumnStyles() {
        distributorCol.setStyle("");
        wholesaleCol.setStyle("");
        retailCol.setStyle("");
        customerCol.setStyle("");
    }
    
    private void populateFormFields(PriceEntry entry) {
        distributorPriceField.setText(String.valueOf(entry.getDistributorPrice()));
        wholesalePriceField.setText(String.valueOf(entry.getWholesalePrice()));
        retailPriceField.setText(String.valueOf(entry.getRetailPrice()));
        customerPriceField.setText(String.valueOf(entry.getCustomerPrice()));
    }
    
    private void clearFormFields() {
        distributorPriceField.clear();
        wholesalePriceField.clear();
        retailPriceField.clear();
        customerPriceField.clear();
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}