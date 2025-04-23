package com.ajay.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import com.ajay.models.BillItem;
import com.ajay.DatabaseConnection;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.scene.layout.HBox;

public class BillingController {
    
    @FXML private TableView<BillItem> billTable;
    @FXML private TableColumn<BillItem, String> productCol;
    @FXML private TableColumn<BillItem, Double> quantityCol;
    @FXML private TableColumn<BillItem, Double> priceCol;
    @FXML private TableColumn<BillItem, Double> totalCol;
    
    @FXML private ComboBox<String> productCombo;
    @FXML private ComboBox<String> unitCombo;
    @FXML private TextField quantityField;
    @FXML private TextField sellingPriceField;
    @FXML private TextField purchasePriceField;
    
    @FXML private Label totalLabel;
    @FXML private Label profitLabel;
    @FXML private HBox adminButtonsBox;
    
    private ObservableList<BillItem> billItems = FXCollections.observableArrayList();
    private double totalSellingPrice = 0;
    private double totalPurchasePrice = 0;
    
    @FXML
    public void initialize() {
        // Initialize table columns
        productCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        totalCol.setCellValueFactory(new PropertyValueFactory<>("total"));
        
        billTable.setItems(billItems);
        
        // Load products and units
        loadProducts();
        loadUnits();
        
        // Initially hide admin buttons
        adminButtonsBox.setVisible(false);
    }
    
    private void loadProducts() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM products")) {
            
            while (rs.next()) {
                productCombo.getItems().add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
            e.printStackTrace();
        }
    }
    
    @FXML
    private void addToBill() {
        try {
            String product = productCombo.getValue();
            String unit = unitCombo.getValue();
            double quantity = Double.parseDouble(quantityField.getText());
            double sellingPrice = Double.parseDouble(sellingPriceField.getText());
            double purchasePrice = Double.parseDouble(purchasePriceField.getText());
            
            BillItem item = new BillItem(product, unit, quantity, sellingPrice, purchasePrice);
            billItems.add(item);
            
            // Update totals
            totalSellingPrice += item.getTotal();
            totalPurchasePrice += (purchasePrice * quantity);
            
            updateTotals();
            
            // Clear fields
            quantityField.clear();
            sellingPriceField.clear();
            purchasePriceField.clear();
            
        } catch (NumberFormatException e) {
            showAlert("Input Error", "Please enter valid numbers for quantity and prices");
        }
    }
    
    private void updateTotals() {
        totalLabel.setText(String.format("₹%.2f", totalSellingPrice));
        double profit = totalSellingPrice - totalPurchasePrice;
        profitLabel.setText(String.format("₹%.2f (%.2f%%)", 
            profit, (profit/totalPurchasePrice)*100));
    }
    
    @FXML
    private void removeSelected() {
        BillItem selected = billTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            totalSellingPrice -= selected.getTotal();
            totalPurchasePrice -= (selected.getPurchasePrice() * selected.getQuantity());
            
            billItems.remove(selected);
            updateTotals();
        }
    }
    
    @FXML
    private void toggleAdminView() {
        adminButtonsBox.setVisible(!adminButtonsBox.isVisible());
    }
    
    @FXML
    private void generateCustomerBill() {
        // Generate PDF with only selling prices
        String billContent = generateBillContent(false);
        // PDF generation code would go here
        showAlert("Bill Generated", "Customer bill Pdf create in the next version");
    }
    
    @FXML
    private void generateAdminBill() {
        // Generate PDF with all details including profit
        String billContent = generateBillContent(true);
        // PDF generation code would go here
        showAlert("Bill Generated", "Admin bill PDF create in the next version");
    }
    
    @FXML
    private void saveBillToDB() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            // Start transaction
            conn.setAutoCommit(false);
            
            // Insert transaction
            String transSql = "INSERT INTO transactions (transaction_type, total_amount, profit) VALUES (?, ?, ?)";
            PreparedStatement transStmt = conn.prepareStatement(transSql, Statement.RETURN_GENERATED_KEYS);
            transStmt.setString(1, "SALE");
            transStmt.setDouble(2, totalSellingPrice);
            transStmt.setDouble(3, totalSellingPrice - totalPurchasePrice);
            transStmt.executeUpdate();
            
            // Get generated transaction ID
            ResultSet rs = transStmt.getGeneratedKeys();
            int transId = rs.next() ? rs.getInt(1) : 0;
            
            // Insert bill items
            String itemSql = "INSERT INTO transaction_items (transaction_id, product_name, quantity, " +
                           "purchase_price, selling_price) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement itemStmt = conn.prepareStatement(itemSql);
            
            for (BillItem item : billItems) {
                itemStmt.setInt(1, transId);
                itemStmt.setString(2, item.getProductName());
                itemStmt.setDouble(3, item.getQuantity());
                itemStmt.setDouble(4, item.getPurchasePrice());
                itemStmt.setDouble(5, item.getSellingPrice());
                itemStmt.addBatch();
            }
            
            itemStmt.executeBatch();
            conn.commit();
            
            showAlert("Success", "Bill saved to database successfully");
            resetBill();
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to save bill to database");
        }
    }
    
    private String generateBillContent(boolean isAdmin) {
        StringBuilder sb = new StringBuilder();
        String header = "Grocery Shop Bill\n" +
                       "Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")) + "\n" +
                       "----------------------------------------\n";
        
        sb.append(header);
        
        for (BillItem item : billItems) {
            sb.append(String.format("%s (%s) - %.2f x %.2f = ₹%.2f\n",
                item.getProductName(), item.getUnit(),
                item.getQuantity(), item.getSellingPrice(), item.getTotal()));
        }
        
        sb.append("----------------------------------------\n");
        sb.append(String.format("Total: ₹%.2f\n", totalSellingPrice));
        
        if (isAdmin) {
            sb.append("\nADMIN DETAILS:\n");
            sb.append(String.format("Total Purchase Cost: ₹%.2f\n", totalPurchasePrice));
            sb.append(String.format("Total Profit: ₹%.2f\n", totalSellingPrice - totalPurchasePrice));
        }
        
        return sb.toString();
    }
    
    private void resetBill() {
        billItems.clear();
        totalSellingPrice = 0;
        totalPurchasePrice = 0;
        updateTotals();
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    @FXML
    private void handleUnitOther() {
        if (unitCombo.getValue().equals("Other")) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Custom Unit");
            dialog.setHeaderText("Enter custom unit name");
            dialog.setContentText("Unit:");
            
            dialog.showAndWait().ifPresent(unit -> {
                // Save to database if needed
                unitCombo.getItems().add(unit);
                unitCombo.setValue(unit);
            });
        }
    }
}