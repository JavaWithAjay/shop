package com.ajay.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.ajay.DatabaseConnection;

public class DashboardController {
    @FXML private Label usernameLabel;
    @FXML private Button logoutButton;
    @FXML private StackPane contentPane;
    @FXML private Text todaySalesText;
    @FXML private Text lowStockText;
    @FXML private Text recentTransactionsText;
    @FXML private ListView<String> activityListView;
    @FXML private Label statusLabel;
    @FXML private Label lastSyncLabel;

    @FXML
    public void initialize() {
        // Set current username
        usernameLabel.setText("Admin User");
        
        // Load dashboard data
        loadDashboardData();
        
        // Set up logout button
        logoutButton.setOnAction(event -> handleLogout());
    }

    private void loadDashboardData() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            // Get today's sales
            String salesQuery = "SELECT SUM(total_amount) FROM transactions " +
                              "WHERE transaction_type = 'SALE' AND DATE(transaction_date) = CURDATE()";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(salesQuery)) {
                if (rs.next()) {
                    double sales = rs.getDouble(1);
                    todaySalesText.setText(String.format("₹%,.2f", sales));
                }
            }
            
            // Get low stock count
            String stockQuery = "SELECT COUNT(*) FROM product_variations pv " +
                              "JOIN products p ON pv.product_id = p.product_id " +
                              "WHERE pv.stock_quantity <= p.min_stock_level";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(stockQuery)) {
                if (rs.next()) {
                    int lowStockCount = rs.getInt(1);
                    lowStockText.setText(lowStockCount + " Items");
                }
            }
            
            // Get recent transaction count
            String transQuery = "SELECT COUNT(*) FROM transactions " +
                              "WHERE transaction_date >= NOW() - INTERVAL 1 DAY";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(transQuery)) {
                if (rs.next()) {
                    int transCount = rs.getInt(1);
                    recentTransactionsText.setText(String.valueOf(transCount));
                }
            }
            
            // Get recent activities
            String activityQuery = "SELECT 'Sale #' || transaction_id || ' - ₹' || total_amount || ' - ' || " +
                                 "TIMESTAMPDIFF(MINUTE, transaction_date, NOW()) || ' min ago' AS activity " +
                                 "FROM transactions WHERE transaction_type = 'SALE' " +
                                 "ORDER BY transaction_date DESC LIMIT 5";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(activityQuery)) {
                while (rs.next()) {
                    activityListView.getItems().add(rs.getString("activity"));
                }
            }
            
            // Update sync time
            lastSyncLabel.setText("Last Sync: " + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, h:mm a")));
            
        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Status: Connection Error");
        }
    }

    @FXML
    private void handleLogout() {
        try {
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/com/ajay/views/login.fxml"));
            stage.setScene(new Scene(root, 600, 500));
            stage.setTitle("Grocery Shop Management - Login");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Logout Error", "Failed to return to login screen");
        }
    }

    @FXML
    private void showProducts() {
        showAlert("Info", "products view will be implemented next");
    }

    @FXML
    private void showCustomers() {
        showAlert("Info", "Customers view will be implemented next");
    }

    @FXML
private void showBilling() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ajay/views/billing.fxml"));
        Node billingView = loader.load();
        contentPane.getChildren().setAll(billingView);
    } catch (IOException e) {
        e.printStackTrace();
        showAlert("Error", "Failed to load billing screen");
    }
}

    @FXML
    private void showInventory() {
        showAlert("Info", "Inventory view will be implemented next");
    }

    @FXML
    private void showReports() {
        showAlert("Info", "Reports view will be implemented next");
    }

    @FXML
    private void showSettings() {
        showAlert("Info", "Settings view will be implemented next");
    }

    @FXML
    private void startNewSale() {
        showAlert("Info", "New sale functionality will be implemented next");
    }

    @FXML
    private void addNewProduct() {
        showAlert("Info", "Add product functionality will be implemented next");
    }

    @FXML
    private void addNewCustomer() {
        showAlert("Info", "Add customer functionality will be implemented next");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void setUsername(String username) {
        usernameLabel.setText(username);
    }
}