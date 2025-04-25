package com.ajay.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.ajay.models.BillItem;
import com.ajay.DatabaseConnection;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.Optional;
import java.util.ResourceBundle;

public class BillingController implements Initializable {

    @FXML private TableView<BillItem> billTable;
    @FXML private TableColumn<BillItem, String> productCol, unitCol, priceLevelCol;
    @FXML private TableColumn<BillItem, Double> quantityCol, sellingPriceCol, totalCol, purchasePriceCol, profitCol;
    
    @FXML private TextField productField, quantityField, purchasePriceField, sellingPriceField;
    @FXML private ComboBox<String> customerTypeCombo, unitCombo;
    @FXML private Label purchasePriceLabel, sellingPriceLabel, totalLabel, profitLabel, purchaseTotalLabel;
    
    @FXML private VBox adminPanel;
    @FXML private HBox adminButtons;
    @FXML private Button saveButton, retrieveButton, customerBillButton, adminBillButton;

    private ObservableList<BillItem> billItems = FXCollections.observableArrayList();
    private double totalAmount = 0;
    private double totalProfit = 0;
    private double totalPurchase = 0;




        @Override
public void initialize(URL location, ResourceBundle resources) {
    Platform.runLater(() -> {
        setupTableColumns();
        setupCustomerTypes();
        setupUnits();
        hideAdminFeatures();
        updatePriceFieldLabels();
        
        // Set stage to be maximized
        Stage stage = (Stage) productField.getScene().getWindow();
        stage.setMaximized(true);
        
    });
}

    private void setupTableColumns() {
        productCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));
        sellingPriceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        totalCol.setCellValueFactory(new PropertyValueFactory<>("total"));
        purchasePriceCol.setCellValueFactory(new PropertyValueFactory<>("purchasePrice"));
        profitCol.setCellValueFactory(new PropertyValueFactory<>("profit"));
        priceLevelCol.setCellValueFactory(new PropertyValueFactory<>("priceLevel"));
        
        billTable.setItems(billItems);
    }

    private void setupCustomerTypes() {
        customerTypeCombo.getItems().addAll("DISTRIBUTOR", "WHOLESALER", "RETAILER", "CUSTOMER");
        customerTypeCombo.setValue("RETAILER");
        
        customerTypeCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updatePriceFieldLabels();
            updateProductPriceSilently();
        });
    }

    private void setupUnits() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM units ORDER BY name")) {
            
            unitCombo.getItems().clear();
            while (rs.next()) {
                unitCombo.getItems().add(rs.getString("name"));
            }
            unitCombo.getItems().add("Other");
            
            unitCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if ("Other".equals(newVal)) {
                    handleNewUnitAddition();
                }
            });
        } catch (SQLException e) {
            showAlert("Error", "Failed to load units: " + e.getMessage());
        }
    }

    private void handleNewUnitAddition() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add New Unit");
        dialog.setHeaderText("Enter the name of the new unit:");
        dialog.setContentText("Unit name:");
    
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(unitName -> {
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement("INSERT INTO units (name) VALUES (?)")) {
                ps.setString(1, unitName);
                ps.executeUpdate();
    
                unitCombo.getItems().add(unitName);
                unitCombo.getSelectionModel().select(unitName);
            } catch (SQLException e) {
                showAlert("Error", "Failed to add new unit: " + e.getMessage());
            }
        });
    }
      

    private void saveNewUnit(String unitName) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO units (name) VALUES (?)")) {
            stmt.setString(1, unitName);
            stmt.executeUpdate();
        }
    }

    private void updatePriceFieldLabels() {
        String customerType = customerTypeCombo.getValue();
        switch (customerType) {
            case "DISTRIBUTOR":
                purchasePriceLabel.setText("Distributor Purchase Price:");
                sellingPriceLabel.setText("Distributor Selling Price:");
                break;
            case "WHOLESALER":
                purchasePriceLabel.setText("Wholesaler Purchase Price:");
                sellingPriceLabel.setText("Wholesaler Selling Price:");
                break;
            case "RETAILER":
                purchasePriceLabel.setText("Retailer Purchase Price:");
                sellingPriceLabel.setText("Retailer Selling Price:");
                break;
            default: // CUSTOMER
                purchasePriceLabel.setText("Customer Purchase Price:");
                sellingPriceLabel.setText("Customer Selling Price:");
                break;
        }
    }

    private void hideAdminFeatures() {
        adminPanel.setVisible(false);
        adminButtons.setVisible(false);
        purchasePriceCol.setVisible(false);
        profitCol.setVisible(false);
        priceLevelCol.setVisible(false);
    }

    @FXML
    private void toggleAdminView() {
        boolean showAdmin = !adminPanel.isVisible();
        adminPanel.setVisible(showAdmin);
        adminButtons.setVisible(showAdmin);
        purchasePriceCol.setVisible(showAdmin);
        profitCol.setVisible(showAdmin);
        priceLevelCol.setVisible(showAdmin);
    }

    @FXML
    private void searchProduct() {
        String productName = productField.getText().trim();
        if (productName.isEmpty()) {
            clearPriceFields();
            return;
        }

        try {
            ProductDetails details = getProductDetails(productName);
            if (details != null) {
                purchasePriceField.setText(String.format("%.2f", details.purchasePrice));
                sellingPriceField.setText(String.format("%.2f", details.sellingPrice));
                unitCombo.setValue(details.unit);
            } else {
                clearPriceFields();
            }
        } catch (SQLException e) {
            clearPriceFields();
        }
    }

    private ProductDetails getProductDetails(String productName) throws SQLException {
        String customerType = customerTypeCombo.getValue();
        String priceColumn = customerType.toLowerCase() + "_price";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT p." + priceColumn + ", p.purchase_price, u.name as unit_name " +
                 "FROM products p JOIN units u ON p.unit_id = u.id WHERE p.name = ?")) {

            stmt.setString(1, productName);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new ProductDetails(
                    rs.getDouble(priceColumn),
                    rs.getDouble("purchase_price"),
                    rs.getString("unit_name")
                );
            }
        }
        return null;
    }

    private void updateProductPriceSilently() {
        String productName = productField.getText().trim();
        if (productName.isEmpty()) return;

        try {
            ProductDetails details = getProductDetails(productName);
            if (details != null) {
                purchasePriceField.setText(String.format("%.2f", details.purchasePrice));
                sellingPriceField.setText(String.format("%.2f", details.sellingPrice));
                unitCombo.setValue(details.unit);
            }
        } catch (SQLException e) {
            // Silently ignore
        }
    }

    @FXML
    private void addToBill() {
        try {
            BillItem item = createBillItemFromInput();
            if (item != null) {
                billItems.add(item);
                updateTotals();
                clearProductFields();
            }
        } catch (NumberFormatException e) {
            showAlert("Error", "Please enter valid quantity and prices.");
        }
    }

    private BillItem createBillItemFromInput() {
        String productName = productField.getText().trim();
        String customerType = customerTypeCombo.getValue();
        String unit = unitCombo.getValue();
        
        if (productName.isEmpty() || unit == null || unit.isEmpty()) {
            showAlert("Error", "Please enter product name and select unit.");
            return null;
        }

        try {
            double quantity = Double.parseDouble(quantityField.getText());
            double purchasePrice = Double.parseDouble(purchasePriceField.getText());
            double sellingPrice = Double.parseDouble(sellingPriceField.getText());
            
            if (quantity <= 0 || purchasePrice < 0 || sellingPrice < 0) {
                showAlert("Error", "Values must be positive numbers.");
                return null;
            }
            
            double profit = (sellingPrice - purchasePrice) * quantity;
            
            return new BillItem(
                productName, 
                unit, 
                quantity, 
                sellingPrice, 
                purchasePrice, 
                customerType, 
                profit
            );
        } catch (NumberFormatException e) {
            throw e;
        }
    }

    private void updateTotals() {
        totalAmount = billItems.stream().mapToDouble(BillItem::getTotal).sum();
        totalPurchase = billItems.stream().mapToDouble(item -> item.getPurchasePrice() * item.getQuantity()).sum();
        totalProfit = totalAmount - totalPurchase;

        totalLabel.setText(String.format("₹%.2f", totalAmount));
        purchaseTotalLabel.setText(String.format("₹%.2f", totalPurchase));
        profitLabel.setText(String.format("₹%.2f", totalProfit));
    }

    @FXML
    private void removeSelected() {
        BillItem selected = billTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            billItems.remove(selected);
            updateTotals();
        } else {
            showAlert("Warning", "Please select an item to remove.");
        }
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ajay/views/dashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) productField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            showAlert("Error", "Failed to go back: " + e.getMessage());
        }
    }

    @FXML
    private void saveData() {
        if (billItems.isEmpty()) {
            showAlert("Error", "No items in the bill to save.");
            return;
        }

        try {
            saveBill();
            showAlert("Success", "Bill saved successfully.");
            billItems.clear();
            updateTotals();
        } catch (SQLException e) {
            showAlert("Error", "Failed to save bill: " + e.getMessage());
        }
    }

    private void saveBill() throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            
            // Save bill header
            int billId = saveBillHeader(conn);
            
            // Save bill items
            saveBillItems(conn, billId);
            
            conn.commit();
        }
    }

    private int saveBillHeader(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO bills (total, profit, purchase_total) VALUES (?, ?, ?)", 
                Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setDouble(1, totalAmount);
            stmt.setDouble(2, totalProfit);
            stmt.setDouble(3, totalPurchase);
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    private void saveBillItems(Connection conn, int billId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO bill_items (bill_id, product_name, quantity, unit, selling_price, purchase_price, customer_type) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            
            for (BillItem item : billItems) {
                stmt.setInt(1, billId);
                stmt.setString(2, item.getProductName());
                stmt.setDouble(3, item.getQuantity());
                stmt.setString(4, item.getUnit());
                stmt.setDouble(5, item.getPrice());
                stmt.setDouble(6, item.getPurchasePrice());
                stmt.setString(7, item.getPriceLevel());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    @FXML
    private void retrieveData() {
        try {
            retrieveLastBill();
        } catch (SQLException e) {
            showAlert("Error", "Failed to retrieve bill: " + e.getMessage());
        }
    }

    private void retrieveLastBill() throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM bills ORDER BY id DESC LIMIT 1")) {
            
            if (rs.next()) {
                int billId = rs.getInt("id");
                loadBillItems(billId);
                
                totalAmount = rs.getDouble("total");
                totalProfit = rs.getDouble("profit");
                totalPurchase = rs.getDouble("purchase_total");
                
                updateTotals();
                showAlert("Success", "Last bill retrieved successfully.");
            } else {
                showAlert("Information", "No saved bills found.");
            }
        }
    }

    private void loadBillItems(int billId) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT * FROM bill_items WHERE bill_id = ?")) {
            
            stmt.setInt(1, billId);
            ResultSet rs = stmt.executeQuery();
            
            billItems.clear();
            while (rs.next()) {
                billItems.add(new BillItem(
                    rs.getString("product_name"),
                    rs.getString("unit"),
                    rs.getDouble("quantity"),
                    rs.getDouble("selling_price"),
                    rs.getDouble("purchase_price"),
                    rs.getString("customer_type"),
                    rs.getDouble("selling_price") * rs.getDouble("quantity") - 
                    rs.getDouble("purchase_price") * rs.getDouble("quantity")
                ));
            }
        }
    }

    @FXML
    private void generateCustomerBill() {
        if (billItems.isEmpty()) {
            showAlert("Error", "No items in the bill to generate.");
            return;
        }
        showAlert("Success", "Customer bill will be generated in next version.");
    }

    @FXML
    private void generateAdminBill() {
        if (billItems.isEmpty()) {
            showAlert("Error", "No items in the bill to generate.");
            return;
        }
        showAlert("Success", "Admin bill will be generated in next version.");
    }

    private void clearPriceFields() {
        purchasePriceField.clear();
        sellingPriceField.clear();
    }

    private void clearProductFields() {
        productField.clear();
        quantityField.clear();
        clearPriceFields();
        unitCombo.setValue(null);
        productField.requestFocus();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static class ProductDetails {
        final double sellingPrice;
        final double purchasePrice;
        final String unit;

        ProductDetails(double sellingPrice, double purchasePrice, String unit) {
            this.sellingPrice = sellingPrice;
            this.purchasePrice = purchasePrice;
            this.unit = unit;
        }
    }
}