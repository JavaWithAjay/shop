package com.ajay.controllers;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import com.ajay.DatabaseConnection;

public class DashboardController {

    @FXML private Label usernameLabel;
    @FXML private Button logoutButton;
    @FXML private StackPane contentPane;
    @FXML private Label statusLabel;
    @FXML private Label lastSyncLabel;
    @FXML private Button refreshButton;
    @FXML private Label lowStockLabel;
    @FXML private Label recentTransactionsLabel;

    @FXML private BarChart<String, Number> priceTrendChart;
    @FXML private PieChart billingPieChart;
    @FXML private LineChart<String, Number> inventoryTrendChart;
    @FXML private CategoryAxis priceDateAxis;
    @FXML private NumberAxis priceValueAxis;
    @FXML private CategoryAxis inventoryDateAxis;
    @FXML private NumberAxis inventoryValueAxis;
    @FXML private TableView<Map<String, String>> udharKhataTable;

    private Map<String, Stage> openChildWindows = new HashMap<>();
    private Timer refreshTimer;
    private String username;

    @FXML
    public void initialize() {
        // Initialize with null checks
        if (usernameLabel != null) {
            usernameLabel.setText(username != null ? username : "Admin User");
        }

        initializeCharts();
        initializeTableColumns();
        loadDashboardData();

        if (logoutButton != null) {
            logoutButton.setOnAction(event -> handleLogout());
        }
        if (refreshButton != null) {
            refreshButton.setOnAction(event -> loadDashboardData());
        }

        startAutoRefresh();
    }

    public void setUsername(String username) {
        this.username = username;
        if (usernameLabel != null) {
            usernameLabel.setText(username);
        }
    }

    private void initializeCharts() {
        if (priceDateAxis != null) priceDateAxis.setLabel("Date");
        if (priceValueAxis != null) priceValueAxis.setLabel("Price (₹)");
        if (inventoryDateAxis != null) inventoryDateAxis.setLabel("Date");
        if (inventoryValueAxis != null) inventoryValueAxis.setLabel("Quantity");
    }

    private void initializeTableColumns() {
        if (udharKhataTable == null) return;

        TableColumn<Map<String, String>, String> customerCol = new TableColumn<>("Customer Name");
        customerCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get("customer")));

        TableColumn<Map<String, String>, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get("phone")));

        TableColumn<Map<String, String>, String> creditCol = new TableColumn<>("Credit Amount");
        creditCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get("credit")));

        TableColumn<Map<String, String>, String> dateCol = new TableColumn<>("Last Transaction");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get("lastDate")));

        // To this:
        udharKhataTable.getColumns().clear();
        udharKhataTable.getColumns().add(customerCol);
        udharKhataTable.getColumns().add(phoneCol);
        udharKhataTable.getColumns().add(creditCol);
        udharKhataTable.getColumns().add(dateCol);
    }

    private void startAutoRefresh() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }

        refreshTimer = new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> loadDashboardData());
            }
        }, 0, 300000); // Every 5 minutes
    }

    private void loadDashboardData() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            loadSummaryData(conn);
            loadPriceTrends(conn);
            loadBillingAnalytics(conn);
            loadInventoryTrends(conn);
            loadUdharKhataData(conn);

            Platform.runLater(() -> {
                if (lastSyncLabel != null) {
                    lastSyncLabel.setText("Last Sync: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, h:mm a")));
                }
                if (statusLabel != null) {
                    statusLabel.setText("Status: Connected");
                }
            });

        } catch (SQLException e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                if (statusLabel != null) {
                    statusLabel.setText("Status: Connection Error");
                }
                showAlert("Database Error", "Failed to load dashboard data: " + e.getMessage());
            });
        }
    }

    private void loadSummaryData(Connection conn) throws SQLException {

        String salesQuery = "SELECT IFNULL(SUM(total_amount), 0) as total FROM transactions WHERE DATE(transaction_date) = CURDATE()";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(salesQuery)) {
            //if (rs.next()) todaySales = rs.getDouble("total");
        }

        String stockQuery = "SELECT COUNT(*) as low_stock FROM inventory WHERE stock_quantity < 10";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(stockQuery)) {
           // if (rs.next()) lowStockItems = rs.getInt("low_stock");
        }

        String transQuery = "SELECT COUNT(*) as recent_trans FROM transactions WHERE DATE(transaction_date) = CURDATE()";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(transQuery)) {
            //if (rs.next()) recentTrans = rs.getInt("recent_trans");
        }

    }

    private void loadPriceTrends(Connection conn) throws SQLException {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Price Trends");

        String query = "SELECT DATE(price_date) as date, AVG(price) as avg_price " +
                       "FROM price_history WHERE price_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
                       "GROUP BY DATE(price_date) ORDER BY date";

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                series.getData().add(new XYChart.Data<>(rs.getString("date"), rs.getDouble("avg_price")));
            }
        }

        Platform.runLater(() -> {
            if (priceTrendChart != null) {
                priceTrendChart.getData().clear();
                priceTrendChart.getData().add(series);
            }
        });
    }

private void loadBillingAnalytics(Connection conn) throws SQLException {
    ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

    String query = "SELECT COALESCE(payment_method, 'Unknown') as payment_method, " +
                   "SUM(total_amount) as amount " +
                   "FROM transactions WHERE transaction_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
                   "GROUP BY COALESCE(payment_method, 'Unknown')";

    try (Statement stmt = conn.createStatement(); 
         ResultSet rs = stmt.executeQuery(query)) {
        double totalAmount = 0;
        
        while (rs.next()) {
            String method = rs.getString("payment_method");
            double amount = rs.getDouble("amount");
            pieChartData.add(new PieChart.Data(method + " (₹" + String.format("%,.2f", amount) + ")", amount));
            totalAmount += amount;
        }

        // Add a default entry if no data found
        if (pieChartData.isEmpty()) {
            pieChartData.add(new PieChart.Data("No Transactions", 1));
        }
    }

    Platform.runLater(() -> {
        if (billingPieChart != null) {
            billingPieChart.setData(pieChartData);
            billingPieChart.setTitle("Payment Method Distribution (Last 7 Days)");
            
            // Add tooltips to show amounts
            for (PieChart.Data data : pieChartData) {
                data.getNode().setOnMouseEntered(e -> {
                    Tooltip.install(data.getNode(), 
                        new Tooltip(String.format("%s: ₹%,.2f", 
                            data.getName(), 
                            data.getPieValue())));
                });
            }
        }
    });
}

    private void loadInventoryTrends(Connection conn) throws SQLException {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Inventory Trends");

        String query = "SELECT DATE(last_updated) as date, AVG(stock_quantity) as avg_stock " +
                       "FROM inventory_history WHERE last_updated >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
                       "GROUP BY DATE(last_updated) ORDER BY date";

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                series.getData().add(new XYChart.Data<>(rs.getString("date"), rs.getInt("avg_stock")));
            }
        }

        Platform.runLater(() -> {
            if (inventoryTrendChart != null) {
                inventoryTrendChart.getData().clear();
                inventoryTrendChart.getData().add(series);
            }
        });
    }

    private void loadUdharKhataData(Connection conn) throws SQLException {
        ObservableList<Map<String, String>> data = FXCollections.observableArrayList();

        String query = "SELECT customer_name, phone, " +
                   "(GREATEST(IFNULL(total_credit, 0) - IFNULL(paid_amount, 0), 0)) AS outstanding, " +
                   "last_transaction_date " +
                   "FROM udhar_khata " +
                   "WHERE (GREATEST(IFNULL(total_credit, 0) - IFNULL(paid_amount, 0), 0)) > 0 " +
                   "ORDER BY outstanding DESC, last_transaction_date DESC " +
                   "LIMIT 10";

        try (Statement stmt = conn.createStatement(); 
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Map<String, String> row = new HashMap<>();
                row.put("customer", rs.getString("customer_name"));
                row.put("phone", rs.getString("phone"));
                row.put("credit", String.format("₹%,.2f", rs.getDouble("outstanding")));
                row.put("lastDate", rs.getDate("last_transaction_date") != null 
                        ? rs.getDate("last_transaction_date").toString() 
                        : "N/A");
                data.add(row);
            }
        }

        Platform.runLater(() -> {
            if (udharKhataTable != null) {
                udharKhataTable.getItems().setAll(data);
                
                if (data.isEmpty()) {
                    Map<String, String> emptyRow = new HashMap<>();
                    emptyRow.put("customer", "No outstanding Udhar records");
                    emptyRow.put("phone", "");
                    emptyRow.put("credit", "");
                    emptyRow.put("lastDate", "");
                    udharKhataTable.getItems().add(emptyRow);
                }
            }
        });
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