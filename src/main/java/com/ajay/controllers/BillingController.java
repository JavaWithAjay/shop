package com.ajay.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.DirectoryChooser;

import com.ajay.models.BillItem;
import com.ajay.DatabaseConnection;

import java.io.FileOutputStream;
import java.io.File;
import java.net.URL;
import java.sql.*;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BillingController implements Initializable {

    @FXML private TableView<BillItem> billTable;
    @FXML private TableColumn<BillItem, String> productCol, unitCol, priceLevelCol;
    @FXML private TableColumn<BillItem, Double> quantityCol, sellingPriceCol, totalCol, purchasePriceCol, profitCol;

    @FXML private TextField productField, quantityField, purchasePriceField, sellingPriceField;
    @FXML private ComboBox<String> customerTypeCombo, unitCombo;
    @FXML private Label purchasePriceLabel, sellingPriceLabel, totalLabel, profitLabel, purchaseTotalLabel;

    @FXML private VBox adminPanel;
    @FXML private HBox adminButtons;
    @FXML private Button saveButton, retrieveButton, customerBillButton, adminBillButton, updateButton, refreshButton;

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

        // Maximize window if available
        if (productField != null && productField.getScene() != null) {
            Stage stage = (Stage) productField.getScene().getWindow();
            stage.setMaximized(true);
        }

        // Selection logic
        billTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        billTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                populateFormFields(newSelection);
            }
        });

        // Add key released listener
        if (productField != null) {
            productField.setOnKeyReleased(event -> searchProduct());
        }
    });
}

@FXML
private void searchProduct() {
    if (productField == null) return;
    
    String productName = productField.getText().trim();
    if (productName.isEmpty()) {
        clearPriceFields();
        return;
    }

    try {
        ProductDetails details = getProductDetails(productName);
        if (details != null && purchasePriceField != null && sellingPriceField != null && unitCombo != null) {
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

    // ✅ FIXED: Now fully implemented
    private void populateFormFields(BillItem newSelection) {
        // This will auto-fill the form fields when a row is selected
        productField.setText(newSelection.getProductName());
        quantityField.setText(String.valueOf(newSelection.getQuantity()));
        purchasePriceField.setText(String.valueOf(newSelection.getPurchasePrice()));
        sellingPriceField.setText(String.valueOf(newSelection.getSellingPrice()));

        unitCombo.setValue(newSelection.getUnit());
        customerTypeCombo.setValue(newSelection.getPriceLevel());

        // Optional: update total, profit, etc., if needed
        updatePriceFieldLabels();
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
        if (customerTypeCombo != null) {
            customerTypeCombo.getItems().addAll("DISTRIBUTOR", "WHOLESALER", "RETAILER", "CUSTOMER", "Shopkeeper", "Online");
            customerTypeCombo.setValue("RETAILER");
            
            customerTypeCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                updatePriceFieldLabels();
                updateProductPriceSilently();
            });
        }
    }

private final ObservableList<String> fixedUnits = FXCollections.observableArrayList("Kg", "gram", "Litre", "Piece", "packet", "pouch" ,"cartoon","other");
private final ObservableList<String> currentUnits = FXCollections.observableArrayList();

private void setupUnits() {
    if (unitCombo == null) return;

    unitCombo.setEditable(false); // Disable direct typing
    resetUnitCombo(); // Initialize with fixed units

    // Add listener for selection changes
    unitCombo.setOnAction(event -> {
        String selected = unitCombo.getValue();

        if ("other".equals(selected)) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Add New Unit");
            dialog.setHeaderText(null);
            dialog.setContentText("Enter new unit name:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(newUnit -> {
                String trimmed = newUnit.trim();
                if (!trimmed.isEmpty() && !currentUnits.contains(trimmed)) {
                    // Insert just before "other" if available
                    int otherIndex = currentUnits.indexOf("other");
                    if (otherIndex == -1) {
                        currentUnits.add(trimmed); // no "other" found
                    } else {
                        currentUnits.add(otherIndex, trimmed);
                    }

                    // Update ComboBox items with fresh list
                    unitCombo.setItems(FXCollections.observableArrayList(currentUnits));

                    // Select the newly added unit safely
                    Platform.runLater(() -> unitCombo.getSelectionModel().select(trimmed));
                } else {
                    unitCombo.getSelectionModel().clearSelection(); // Clear safely
                }
            });
        }
    });
}



private void showAddUnitDialog() {
    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("Add New Unit");
    dialog.setHeaderText(null);
    dialog.setContentText("Enter new unit name:");

    Optional<String> result = dialog.showAndWait();
    result.ifPresent(newUnit -> {
        String trimmed = newUnit.trim();
        if (!trimmed.isEmpty()) {
            // Add to database
            addNewUnitToDatabase(trimmed);
            // Refresh the combo box
            resetUnitCombo();
            // Select the newly added unit
            unitCombo.getSelectionModel().select(trimmed);
        }
    });
    
    // Reset selection after dialog closes
    unitCombo.getSelectionModel().clearSelection();
}

private void addNewUnitToDatabase(String unitName) {
    try (Connection conn = DatabaseConnection.getInstance().getConnection();
         PreparedStatement ps = conn.prepareStatement("INSERT INTO units (name) VALUES (?)")) {
        
        ps.setString(1, unitName);
        ps.executeUpdate();
    } catch (SQLException e) {
        showAlert("Error", "Failed to add new unit: " + e.getMessage());
    }
}

private void resetUnitCombo() {
    currentUnits.setAll(fixedUnits); // always refill list
    unitCombo.setItems(currentUnits);
    unitCombo.getSelectionModel().clearSelection();
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
    
                if (unitCombo != null) {
                    unitCombo.getItems().add(unitName);
                    unitCombo.getSelectionModel().select(unitName);
                }
            } catch (SQLException e) {
                showAlert("Error", "Failed to add new unit: " + e.getMessage());
            }
        });
    }

   private void updatePriceFieldLabels() {
    if (customerTypeCombo == null || purchasePriceLabel == null || sellingPriceLabel == null) return;
    
    String customerType = customerTypeCombo.getValue();
    if (customerType == null) {
        // Set default labels or clear them
        purchasePriceLabel.setText("Purchase Price:");
        sellingPriceLabel.setText("Selling Price:");
        return;
    }
    
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
        default: // CUSTOMER and other types
            purchasePriceLabel.setText("Customer Purchase Price:");
            sellingPriceLabel.setText("Customer Selling Price:");
            break;
    }
}
    

    private void hideAdminFeatures() {
        if (adminPanel != null) adminPanel.setVisible(false);
        if (adminButtons != null) adminButtons.setVisible(false);
        if (purchasePriceCol != null) purchasePriceCol.setVisible(false);
        if (profitCol != null) profitCol.setVisible(false);
        if (priceLevelCol != null) priceLevelCol.setVisible(false);
    }

    @FXML
    private void toggleAdminView() {
        if (adminPanel == null || adminButtons == null || 
            purchasePriceCol == null || profitCol == null || priceLevelCol == null) return;
            
        boolean showAdmin = !adminPanel.isVisible();
        adminPanel.setVisible(showAdmin);
        adminButtons.setVisible(showAdmin);
        purchasePriceCol.setVisible(showAdmin);
        profitCol.setVisible(showAdmin);
        priceLevelCol.setVisible(showAdmin);
    }

private ProductDetails getProductDetails(String productName) throws SQLException {
    if (customerTypeCombo == null) return null;
    
    String customerType = customerTypeCombo.getValue();
    if (customerType == null) {
        return null; // or handle differently if needed
    }
    
    String priceColumn = customerType.toLowerCase() + "_price";

    try (Connection conn = DatabaseConnection.getInstance().getConnection();
         PreparedStatement stmt = conn.prepareStatement(
             "SELECT p." + priceColumn + ", p.purchase_price, u.name as unit_name " +
             "FROM products p JOIN units u ON p.unit_id = u.id WHERE p.name = ?")) {
        
        stmt.setString(1, productName);
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return new ProductDetails(
                    rs.getDouble(priceColumn),
                    rs.getDouble("purchase_price"),
                    rs.getString("unit_name")
                );
            }
            return null;
        }
    }
}

    private void updateProductPriceSilently() {
        if (productField == null) return;
        
        String productName = productField.getText().trim();
        if (productName.isEmpty()) return;

        try {
            ProductDetails details = getProductDetails(productName);
            if (details != null && purchasePriceField != null && sellingPriceField != null && unitCombo != null) {
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
        if (productField == null || customerTypeCombo == null || unitCombo == null || 
            quantityField == null || purchasePriceField == null || sellingPriceField == null) {
            return null;
        }
        
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
                0, 0, productName, 
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

        if (totalLabel != null) totalLabel.setText(String.format("₹%.2f", totalAmount));
        if (purchaseTotalLabel != null) purchaseTotalLabel.setText(String.format("₹%.2f", totalPurchase));
        if (profitLabel != null) profitLabel.setText(String.format("₹%.2f", totalProfit));
    }

@FXML
private void refreshData() {
    // Clear fields
    productField.clear();
    quantityField.clear();
    purchasePriceField.clear();
    sellingPriceField.clear();
    
    // Reset units (this will reload from database)
    resetUnitCombo();
    unitCombo.getSelectionModel().clearSelection();
    
    // Clear other selections
    if (customerTypeCombo != null) {
        customerTypeCombo.getSelectionModel().clearSelection();
    }
    
    // Clear table
    billItems.clear();
    updateTotals();
    
    // Reset labels
    if (purchasePriceLabel != null) purchasePriceLabel.setText("Purchase Price:");
    if (sellingPriceLabel != null) sellingPriceLabel.setText("Selling Price:");
}


@FXML
private void removeSelected() {
    if (billTable == null) return;

    // Step 1: Get selected items from the TableView
    ObservableList<BillItem> selectedItems = 
        billTable.getSelectionModel().getSelectedItems();

    if (selectedItems == null || selectedItems.isEmpty()) {
        showAlert("Warning", "Please select items to remove.");
        return;
    }

    // Step 2: Show confirmation dialog
    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Confirm Deletion");
    confirm.setHeaderText("Delete Selected Items");
    confirm.setContentText("Are you sure you want to delete " + 
        selectedItems.size() + " selected items?");

    Optional<ButtonType> result = confirm.showAndWait();
    if (result.isPresent() && result.get() == ButtonType.OK) {
        
        // Step 3: Delete from DB (if item has a valid ID > 0)
        for (BillItem item : selectedItems) {
            if (item.getId() > 0) {
                deleteItemFromDatabase(item.getId());
            }
        }

        // Step 4: Remove from observable list (UI)
        billItems.removeAll(selectedItems);

        // Step 5: Update total if needed
        updateTotals();
    }
}


private void deleteItemFromDatabase(int itemId) {
    String query = "DELETE FROM bill_items WHERE id = ?";

    try (Connection conn = DatabaseConnection.getInstance().getConnection();
         PreparedStatement ps = conn.prepareStatement(query)) {

        ps.setInt(1, itemId);
        ps.executeUpdate();

    } catch (SQLException e) {
        showAlert("Error", "Failed to delete item from database: " + e.getMessage());
    }
}


// ❌ FIXED: Argument type should be int, not BillItem
private void updateBillTotals(Connection conn, int billId) throws SQLException {
    // Recalculate totals for the bill
    try (PreparedStatement stmt = conn.prepareStatement(
            "UPDATE bills SET " +
            "total = (SELECT COALESCE(SUM(quantity * selling_price), 0) FROM bill_items WHERE bill_id = ?), " +
            "purchase_total = (SELECT COALESCE(SUM(quantity * purchase_price), 0) FROM bill_items WHERE bill_id = ?), " +
            "profit = (SELECT COALESCE(SUM(quantity * (selling_price - purchase_price)), 0) FROM bill_items WHERE bill_id = ?) " +
            "WHERE id = ?")) {

        stmt.setInt(1, billId);
        stmt.setInt(2, billId);
        stmt.setInt(3, billId);
        stmt.setInt(4, billId);
        stmt.executeUpdate();
    }
}


@FXML
private void saveData() {
    if (billItems.isEmpty()) {
        showAlert("Error", "No items in the bill to save.");
        return;
    }

    try {
        int billId = saveBill();
        saveBillItems(billId);
        
        // Update items with their database IDs
        updateItemIdsFromDatabase(billId);
        
        showAlert("Success", "Bill saved successfully with ID: " + billId);
    } catch (SQLException e) {
        showAlert("Error", "Failed to save bill: " + e.getMessage());
    }
}

private int saveBill() throws SQLException {
    try (Connection conn = DatabaseConnection.getInstance().getConnection();
         PreparedStatement stmt = conn.prepareStatement(
             "INSERT INTO bills (total, profit, purchase_total, bill_date) " +
             "VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
        
        stmt.setDouble(1, totalAmount);
        stmt.setDouble(2, totalProfit);
        stmt.setDouble(3, totalPurchase);
        stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
        stmt.executeUpdate();
        
        try (ResultSet rs = stmt.getGeneratedKeys()) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }
}

private void saveBillItems(int billId) throws SQLException {
    try (Connection conn = DatabaseConnection.getInstance().getConnection();
         PreparedStatement stmt = conn.prepareStatement(
             "INSERT INTO bill_items (bill_id, product_name, quantity, unit, " +
             "selling_price, purchase_price, customer_type) " +
             "VALUES (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
        
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

private void updateItemIdsFromDatabase(int billId) throws SQLException {
    try (Connection conn = DatabaseConnection.getInstance().getConnection();
         PreparedStatement stmt = conn.prepareStatement(
             "SELECT id, product_name, quantity FROM bill_items " +
             "WHERE bill_id = ? ORDER BY id")) {
        
        stmt.setInt(1, billId);
        ResultSet rs = stmt.executeQuery();
        
        // Match items by product name and quantity since we don't have IDs yet
        for (BillItem item : billItems) {
            if (rs.next()) {
                // Verify this is the correct item
                if (item.getProductName().equals(rs.getString("product_name")) &&
                    item.getQuantity() == rs.getDouble("quantity")) {
                    item.setId(rs.getInt("id"));
                    item.setBillId(billId);
                }
            }
        }
    }
}

private boolean validateInputs() {
    if (productField.getText().trim().isEmpty()) {
        showAlert("Error", "Product name is required");
        return false;
    }
    
    try {
        double qty = Double.parseDouble(quantityField.getText());
        if (qty <= 0) {
            showAlert("Error", "Quantity must be positive");
            return false;
        }
    } catch (NumberFormatException e) {
        showAlert("Error", "Invalid quantity");
        return false;
    }
    
    // Similar validation for prices
    return true;
}

    private int saveBillHeader(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO bills (total, profit, purchase_total, bill_date) VALUES (?, ?, ?, ?)", 
                Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setDouble(1, totalAmount);
            stmt.setDouble(2, totalProfit);
            stmt.setDouble(3, totalPurchase);
            stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            return rs.next() ? rs.getInt(1) : -1;
        }
    }


@FXML
private void retrieveData() {
    try {
        String customerType = customerTypeCombo.getValue(); // This can be null if nothing is selected
        String productName = productField.getText().trim();
        String quantityStr = quantityField.getText().trim();
        String unit = unitCombo.getValue(); // This can also be null

        Double quantity = null;
        if (!quantityStr.isEmpty()) {
            try {
                quantity = Double.parseDouble(quantityStr);
            } catch (NumberFormatException e) {
                showAlert("Error", "Invalid quantity format");
                return;
            }
        }

        // Pass customerType to retrieveFilteredBills directly.
        // The null check for customerType is done in retrieveFilteredBills or getProductDetails
        // where it's used.
        retrieveFilteredBills(
            customerType, // Pass potentially null customerType
            !productName.isEmpty() ? productName : null,
            quantity,
            unit // Pass potentially null unit
        );
    } catch (SQLException e) {
        showAlert("Error", "Failed to retrieve data: " + e.getMessage());
    }
}

private void retrieveFilteredBills(String customerType, String productName,
                                   Double quantity, String unit) throws SQLException {
    StringBuilder query = new StringBuilder(
        "SELECT bi.id, bi.bill_id, bi.product_name, bi.quantity, bi.unit, " +
        "bi.selling_price, bi.purchase_price, bi.customer_type " +
        "FROM bill_items bi " +
        "JOIN bills b ON bi.bill_id = b.id " +
        "WHERE 1=1"
    );

    List<Object> params = new ArrayList<>();

    if (customerType != null && !customerType.isEmpty()) { // Add !customerType.isEmpty() for robustness
        query.append(" AND bi.customer_type = ?");
        params.add(customerType);
    }

    if (productName != null && !productName.isEmpty()) { // Add !productName.isEmpty() for robustness
        query.append(" AND bi.product_name LIKE ?");
        params.add("%" + productName + "%");
    }

    if (quantity != null) {
        query.append(" AND bi.quantity = ?");
        params.add(quantity);
    }

    if (unit != null && !unit.isEmpty()) { // Add !unit.isEmpty() for robustness
        query.append(" AND bi.unit = ?");
        params.add(unit);
    }

    query.append(" ORDER BY b.bill_date DESC");

    billItems.clear();
    totalAmount = 0;
    totalProfit = 0;
    totalPurchase = 0;

    try (Connection conn = DatabaseConnection.getInstance().getConnection();
         PreparedStatement stmt = conn.prepareStatement(query.toString())) {

        for (int i = 0; i < params.size(); i++) {
            stmt.setObject(i + 1, params.get(i));
        }

        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            BillItem item = new BillItem(
                rs.getInt("id"),
                rs.getInt("bill_id"),
                rs.getString("product_name"),
                rs.getString("unit"),
                rs.getDouble("quantity"),
                rs.getDouble("selling_price"),
                rs.getDouble("purchase_price"),
                rs.getString("customer_type"),
                (rs.getDouble("selling_price") - rs.getDouble("purchase_price")) * rs.getDouble("quantity")
            );

            billItems.add(item);

            totalAmount += item.getTotal();
            totalPurchase += item.getPurchasePrice() * item.getQuantity();
            totalProfit += item.getProfit();
        }

        updateTotals();
    }
}

// Ensure that in updatePriceFieldLabels and getProductDetails,
// you also perform null checks on customerType before using it.

// Example for updatePriceFieldLabels (assuming customerType is passed or retrieved)
private void updatePriceFieldLabels(String customerType) {
    if (customerType != null) {
        // Your existing logic that uses customerType, e.g., customerType.hashCode()
    } else {
        // Handle the null case, perhaps set default labels or log a warning
    }
}

// Example for getProductDetails (assuming customerType is passed or retrieved)
private void getProductDetails(String searchTerm, String customerType) {
    if (customerType != null) {
        // Your existing logic that uses customerType, e.g., customerType.toLowerCase()
    } else {
        // Handle the null case, perhaps use a default customer type or skip logic
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
                    billId, billId, rs.getString("product_name"),
                    rs.getString("unit"),
                    rs.getDouble("quantity"),
                    rs.getDouble("selling_price"),
                    rs.getDouble("purchase_price"),
                    rs.getString("customer_type"),
                    (rs.getDouble("selling_price") - rs.getDouble("purchase_price")) * rs.getDouble("quantity")
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
        
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Directory to Save Bill");
        File selectedDirectory = directoryChooser.showDialog(adminBillButton.getScene().getWindow());
        
        if (selectedDirectory == null) return;
        
        String fileName = selectedDirectory.getAbsolutePath() + File.separator + 
                         "Customer_Bill_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
        
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(fileName));
            
            document.open();
            addCustomerBillContent(document);
            document.close();
            
            showAlert("Success", "Customer bill generated:\n" + fileName);
        } catch (Exception e) {
            showAlert("Error", "Failed to generate customer bill: " + e.getMessage());
        }
    }

    @FXML
    private void generateAdminBill() {
        if (billItems.isEmpty()) {
            showAlert("Error", "No items in the bill to generate.");
            return;
        }
        
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Directory to Save Bill");
        File selectedDirectory = directoryChooser.showDialog(adminBillButton.getScene().getWindow());
        
        if (selectedDirectory == null) return;
        
        String fileName = selectedDirectory.getAbsolutePath() + File.separator + 
                         "Admin_Bill_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
        
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(fileName));
            
            document.open();
            addAdminBillContent(document);
            document.close();
            
            showAlert("Success", "Admin bill generated:\n" + fileName);
        } catch (Exception e) {
            showAlert("Error", "Failed to generate admin bill: " + e.getMessage());
        }
    }

    private void addCustomerBillContent(Document document) throws DocumentException {
        // Add title
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
        Paragraph title = new Paragraph("GROCERY SHOP - CUSTOMER BILL", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        
        // Add bill info
        Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.BLACK);
        Paragraph info = new Paragraph();
        info.add(new Chunk("Date: ", infoFont));
        info.add(new Chunk(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")), infoFont));
        info.add(Chunk.NEWLINE);
        info.add(new Chunk("Customer Type: ", infoFont));
        info.add(new Chunk(customerTypeCombo.getValue(), infoFont));
        info.setAlignment(Element.ALIGN_CENTER);
        document.add(info);
        
        document.add(Chunk.NEWLINE);
        
        // Create table for items
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        
        // Add table headers
        addTableHeader(table, "Product", "Quantity", "Unit Price", "Total");
        
        // Add bill items
        for (BillItem item : billItems) {
            addRowToTable(table, 
                item.getProductName(),
                String.format("%.2f", item.getQuantity()),
                String.format("₹%.2f", item.getPrice()),
                String.format("₹%.2f", item.getTotal())
            );
        }
        
        document.add(table);
        document.add(Chunk.NEWLINE);
        
        // Add total
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.BLACK);
        Paragraph total = new Paragraph(String.format("Total Amount: ₹%,.2f", totalAmount), totalFont);
        total.setAlignment(Element.ALIGN_RIGHT);
        document.add(total);
        
        // Add thank you message
        Font thankYouFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, BaseColor.GRAY);
        Paragraph thankYou = new Paragraph("Thank you for your business!", thankYouFont);
        thankYou.setAlignment(Element.ALIGN_CENTER);
        document.add(thankYou);
    }

    private void addAdminBillContent(Document document) throws DocumentException {
        // Add title
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
        Paragraph title = new Paragraph("GROCERY SHOP - ADMIN BILL DETAILS", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        
        // Add bill info
        Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.BLACK);
        Paragraph info = new Paragraph();
        info.add(new Chunk("Date: ", infoFont));
        info.add(new Chunk(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")), infoFont));
        info.add(Chunk.NEWLINE);
        info.add(new Chunk("Customer Type: ", infoFont));
        info.add(new Chunk(customerTypeCombo.getValue(), infoFont));
        info.setAlignment(Element.ALIGN_CENTER);
        document.add(info);
        
        document.add(Chunk.NEWLINE);
        
        // Create table for items
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        
        // Add table headers
        addTableHeader(table, "Product", "Qty", "Unit", "Purchase", "Selling", "Profit");
        
        // Add bill items
        for (BillItem item : billItems) {
            addRowToTable(table, 
                item.getProductName(),
                String.format("%.2f", item.getQuantity()),
                item.getUnit(),
                String.format("₹%.2f", item.getPurchasePrice()),
                String.format("₹%.2f", item.getPrice()),
                String.format("₹%.2f", item.getProfit())
            );
        }
        
        document.add(table);
        document.add(Chunk.NEWLINE);
        
        // Add totals
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.BLACK);
        
        Paragraph purchaseTotal = new Paragraph(String.format("Purchase Total: ₹%,.2f", totalPurchase), totalFont);
        purchaseTotal.setAlignment(Element.ALIGN_RIGHT);
        document.add(purchaseTotal);
        
        Paragraph salesTotal = new Paragraph(String.format("Sales Total: ₹%,.2f", totalAmount), totalFont);
        salesTotal.setAlignment(Element.ALIGN_RIGHT);
        document.add(salesTotal);
        
        Paragraph profitTotal = new Paragraph(String.format("Total Profit: ₹%,.2f", totalProfit), totalFont);
        profitTotal.setAlignment(Element.ALIGN_RIGHT);
        document.add(profitTotal);
    }

    private void addTableHeader(PdfPTable table, String... headers) {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.WHITE);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(new BaseColor(59, 89, 182));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private void addRowToTable(PdfPTable table, String... values) {
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);
        for (String value : values) {
            PdfPCell cell = new PdfPCell(new Phrase(value, cellFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private void clearPriceFields() {
        if (purchasePriceField != null) purchasePriceField.clear();
        if (sellingPriceField != null) sellingPriceField.clear();
    }

    private void clearProductFields() {
        if (productField != null) productField.clear();
        if (quantityField != null) quantityField.clear();
        clearPriceFields();
        if (unitCombo != null) unitCombo.setValue(null);
        if (productField != null) productField.requestFocus();
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
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