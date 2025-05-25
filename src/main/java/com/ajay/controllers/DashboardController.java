package com.ajay.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.application.Platform;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import com.ajay.DatabaseConnection;

public class DashboardController {

    // UI Components
    @FXML private Label usernameLabel;
    @FXML private Button logoutButton;
    @FXML private Label statusLabel;
    @FXML private Label lastSyncLabel;
    @FXML private TableView<Map<String, String>> udharKhataTable;
    
    // Controller state
    private String username;
    private Timer refreshTimer;
    private final Map<String, Stage> openChildWindows = new HashMap<>();

    @FXML
    public void initialize() {
        usernameLabel.setText(username != null ? username : "Admin");
        loadDashboardData();
        
        logoutButton.setOnAction(event -> handleLogout());
        
        // Initialize refresh timer
        refreshTimer = new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> loadDashboardData());
            }
        }, 0, 300000); // Refresh every 5 minutes
    }

    public void setUsername(String username) {
        this.username = username;
        if (usernameLabel != null) {
            usernameLabel.setText(username);
        }
    }

   private void loadDashboardData() {
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
        loadUdharKhataData(conn);
        
        lastSyncLabel.setText("Last Sync: " + new java.util.Date());
        statusLabel.setText("Status: Connected");
        
    } catch (SQLException e) {
        handleDatabaseError(e);
    }
}
private void handleDatabaseError(SQLException e) {
    e.printStackTrace();
    Platform.runLater(() -> {
        statusLabel.setText("Status: Error");
        
        showAlert("Database Error", "Failed to load dashboard data: " + e.getMessage());
    });
}

    private void loadUdharKhataData(Connection conn) throws SQLException {
        udharKhataTable.getColumns().clear();
        
        TableColumn<Map<String, String>, String> nameCol = new TableColumn<>("Customer");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get("name")));
        nameCol.setPrefWidth(150);
        
        TableColumn<Map<String, String>, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get("phone")));
        phoneCol.setPrefWidth(100);
        
        TableColumn<Map<String, String>, String> amountCol = new TableColumn<>("Amount Due");
        amountCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get("amount")));
        amountCol.setPrefWidth(100);
        
        udharKhataTable.getColumns().addAll(nameCol, phoneCol, amountCol);
        
        // Get top 10 customers with highest pending amounts
        String query = "SELECT customer_name as name, phone, " +
                      "(amount - IFNULL(paid_amount, 0)) as amount " +
                      "FROM udhar_khata " +
                      "WHERE (amount - IFNULL(paid_amount, 0)) > 0 " +
                      "ORDER BY amount DESC LIMIT 10";
        
        List<Map<String, String>> data = new ArrayList<>();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Map<String, String> row = new HashMap<>();
                row.put("name", rs.getString("name"));
                row.put("phone", rs.getString("phone"));
                row.put("amount", "₹" + String.format("%.2f", rs.getDouble("amount")));
                data.add(row);
            }
        }
        
        udharKhataTable.getItems().setAll(data);
    }


    @FXML
    private void handleLogout() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
        if (openChildWindows != null) {
            openChildWindows.values().forEach(Stage::close);
            openChildWindows.clear();
        }

        try {
            if (logoutButton != null && logoutButton.getScene() != null) {
                Stage stage = (Stage) logoutButton.getScene().getWindow();
                Parent root = FXMLLoader.load(getClass().getResource("/com/ajay/views/login.fxml"));
                if (root != null) {
                    stage.setScene(new Scene(root, 600, 500));
                    stage.setTitle("Grocery Shop Management - Login");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Logout Error", "Failed to return to login screen");
        }
    }

    @FXML
    private void showBilling() {
        try {
            if (usernameLabel != null && usernameLabel.getScene() != null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ajay/views/billing.fxml"));
                Parent root = loader.load();

                Stage billingStage = new Stage();
                billingStage.setTitle("Billing");
                billingStage.setScene(new Scene(root));
                billingStage.initOwner(usernameLabel.getScene().getWindow());
                billingStage.setWidth(900);
                billingStage.setHeight(700);

                Stage mainStage = (Stage) usernameLabel.getScene().getWindow();
                billingStage.setX(mainStage.getX() + 50);
                billingStage.setY(mainStage.getY() + 50);

                openChildWindows.put("Billing", billingStage);
                billingStage.setOnHidden(e -> openChildWindows.remove("Billing"));

                billingStage.show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open Billing window");
        }
    }

    @FXML
    private void showPrices() {
        try {
            if (usernameLabel != null && usernameLabel.getScene() != null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ajay/views/price.fxml"));
                Parent root = loader.load();

                Stage priceStage = new Stage();
                priceStage.setTitle("Price Management");
                priceStage.setScene(new Scene(root));
                priceStage.initOwner(usernameLabel.getScene().getWindow());
                priceStage.setWidth(800);
                priceStage.setHeight(600);

                Stage mainStage = (Stage) usernameLabel.getScene().getWindow();
                priceStage.setX(mainStage.getX() + 60);
                priceStage.setY(mainStage.getY() + 60);

                openChildWindows.put("PriceManagement", priceStage);
                priceStage.setOnHidden(e -> openChildWindows.remove("PriceManagement"));

                priceStage.show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open Price Management window");
        }
    }

    @FXML
    private void showInventory() {
        try {
            if (usernameLabel != null && usernameLabel.getScene() != null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ajay/views/inventory_alert.fxml"));
                Parent root = loader.load();
                
                Stage inventoryStage = new Stage();
                inventoryStage.setTitle("Inventory Management - Stock Alerts");
                inventoryStage.setScene(new Scene(root));
                inventoryStage.initOwner(usernameLabel.getScene().getWindow());
                inventoryStage.setWidth(900);
                inventoryStage.setHeight(700);

                Stage mainStage = (Stage) usernameLabel.getScene().getWindow();
                inventoryStage.setX(mainStage.getX() + 50);
                inventoryStage.setY(mainStage.getY() + 50);

                openChildWindows.put("Inventory Management", inventoryStage);
                inventoryStage.setOnHidden(e -> openChildWindows.remove("Inventory Management"));

                inventoryStage.show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open Inventory Management window");
        }
    }

    @FXML
    private void showUdharKhata() {
        try {
            if (usernameLabel != null && usernameLabel.getScene() != null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ajay/views/udhar-khata.fxml"));
                Parent root = loader.load();

                Stage udharStage = new Stage();
                udharStage.setTitle("Udhar Khata Management");
                udharStage.setScene(new Scene(root));
                udharStage.initOwner(usernameLabel.getScene().getWindow());
                udharStage.setWidth(1000);
                udharStage.setHeight(700);

                Stage mainStage = (Stage) usernameLabel.getScene().getWindow();
                udharStage.setX(mainStage.getX() + 50);
                udharStage.setY(mainStage.getY() + 50);

                openChildWindows.put("UdharKhata", udharStage);
                udharStage.setOnHidden(e -> openChildWindows.remove("UdharKhata"));

                udharStage.show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open Udhar Khata window");
        }
    }

    @FXML
    private void showReturnPage() {
        try {
            if (usernameLabel != null && usernameLabel.getScene() != null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ajay/views/return.fxml"));
                Parent root = loader.load();

                Stage returnStage = new Stage();
                returnStage.setTitle("Return Management");
                returnStage.setScene(new Scene(root));
                returnStage.initOwner(usernameLabel.getScene().getWindow());
                returnStage.setWidth(900);
                returnStage.setHeight(700);

                Stage mainStage = (Stage) usernameLabel.getScene().getWindow();
                returnStage.setX(mainStage.getX() + 50);
                returnStage.setY(mainStage.getY() + 50);

                openChildWindows.put("Return", returnStage);
                returnStage.setOnHidden(e -> openChildWindows.remove("Return"));

                returnStage.show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open Return Management window");
        }
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}