package com.ajay.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import com.ajay.models.PriceEntry;
import com.ajay.DatabaseConnection;
import java.sql.*;
import java.util.Optional;
import java.util.regex.Pattern;

public class PriceController {

    @FXML private TableView<PriceEntry> priceTable;
    @FXML private TableColumn<PriceEntry, Integer> idCol;
    @FXML private TableColumn<PriceEntry, String> nameCol;
    @FXML private TableColumn<PriceEntry, String> customerTypeCol;
    @FXML private TableColumn<PriceEntry, String> categoryCol;
    @FXML private TableColumn<PriceEntry, String> unitCol;
    @FXML private TableColumn<PriceEntry, Double> purchasePriceCol;
    @FXML private TableColumn<PriceEntry, Double> sellingPriceCol;
    @FXML private TableColumn<PriceEntry, Double> minSellingPriceCol;
    @FXML private TableColumn<PriceEntry, Double> quantityCol;
    
    @FXML private TextField searchField;
    @FXML private TextField productNameField;
    @FXML private TextField quantityField;
    @FXML private TextField purchasePriceField;
    @FXML private TextField sellingPriceField;
    @FXML private TextField minSellingPriceField;
    
    @FXML private ComboBox<String> customerTypeCombo;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private ComboBox<String> unitCombo;
    
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button clearButton;
    @FXML private Button adminModeButton;
    @FXML private Button refreshButton;
    @FXML private Button searchButton;
    
    @FXML private Text statusText;
    @FXML private Text minPriceStatusText;
    
    private boolean adminMode = false;
    
    // Fixed values for dropdowns
    private final ObservableList<String> fixedCustomerTypes = 
        FXCollections.observableArrayList("Distributor", "Wholesale", "Retail", "Customer", "Online", "Shopkeeper");
    private final ObservableList<String> fixedCategories = 
        FXCollections.observableArrayList("study", "Electronics", "Food", "Clothing", "tobacco", "Spice", "sweet");
    private final ObservableList<String> fixedUnits = 
        FXCollections.observableArrayList("Piece", "Kg", "gram", "Liter", "Box", "cartoon", "pouch" ,"Packet");
    
    // Current dropdown values (fixed + dynamic)
    private ObservableList<String> customerTypes = FXCollections.observableArrayList();
    private ObservableList<String> categories = FXCollections.observableArrayList();
    private ObservableList<String> units = FXCollections.observableArrayList();
    
    private ObservableList<PriceEntry> priceEntries = FXCollections.observableArrayList();

@FXML
public void initialize() {
    setupTableColumns();
    loadInitialData();
    setupFormListeners();

    // Initialize admin mode settings
    adminModeButton.setOnAction(this::handleAdminMode);
    adminModeButton.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
    minPriceStatusText.setVisible(false);
    statusText.setVisible(false);
    purchasePriceCol.setVisible(false);
    minSellingPriceCol.setVisible(false);

    // Add Enter key listener for search
    searchField.setOnKeyPressed(event -> {
        if (event.getCode() == KeyCode.ENTER) {
            handleSearch(new ActionEvent());
        }
    });

    // Setup dynamic dropdown entries
    setupDynamicDropdownAddition();
}

private void setupDynamicDropdownAddition() {
    setupComboWithAddOption(customerTypeCombo, customerTypes);
    setupComboWithAddOption(categoryCombo, categories);
    setupComboWithAddOption(unitCombo, units);
}

private void setupComboWithAddOption(ComboBox<String> comboBox, ObservableList<String> list) {
    final String ADD_NEW_OPTION = "➕ Add New";

    if (!list.contains(ADD_NEW_OPTION)) {
        list.add(ADD_NEW_OPTION);
    }

    comboBox.setItems(list);

    comboBox.setOnAction(event -> {
        String selected = comboBox.getSelectionModel().getSelectedItem();

        if (ADD_NEW_OPTION.equals(selected)) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Add New Entry");
            dialog.setHeaderText("Add a new item to the list:");
            dialog.setContentText("Enter new value:");

            dialog.showAndWait().ifPresent(input -> {
                String newItem = input.trim();
                if (!newItem.isEmpty() && !list.contains(newItem)) {
                    list.add(list.size() - 1, newItem);  // Add before "Add New"
                    comboBox.setItems(null);             // Reset the list
                    comboBox.setItems(list);             // Reassign updated list
                    comboBox.getSelectionModel().select(newItem);  // Select new item
                } else {
                    comboBox.getSelectionModel().clearSelection();
                }
            });
        }
    });
}




    @FXML
    private void handleClear(ActionEvent event) {
        clearFormFields();
    }

    private void setupTableColumns() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        customerTypeCol.setCellValueFactory(new PropertyValueFactory<>("customerType"));
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));
        purchasePriceCol.setCellValueFactory(new PropertyValueFactory<>("purchasePrice"));
        sellingPriceCol.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        minSellingPriceCol.setCellValueFactory(new PropertyValueFactory<>("minSellingPrice"));
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));
        
        priceTable.setItems(priceEntries);
    }

    private void loadInitialData() {
        resetDropdowns();
        loadPriceData();
    }

    private void resetDropdowns() {
        // Reset to fixed values only
        customerTypes.setAll(fixedCustomerTypes);
        categories.setAll(fixedCategories);
        units.setAll(fixedUnits);
        
        customerTypeCombo.setItems(customerTypes);
        categoryCombo.setItems(categories);
        unitCombo.setItems(units);
    }

    private void setupFormListeners() {
        priceTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    populateFormFields(newSelection);
                }
            });
        
        purchasePriceField.textProperty().addListener((obs, oldVal, newVal) -> updateProfitStatus());
        sellingPriceField.textProperty().addListener((obs, oldVal, newVal) -> updateProfitStatus());
        minSellingPriceField.textProperty().addListener((obs, oldVal, newVal) -> updateProfitStatus());
    }

    @FXML
    private void handleAdminMode(ActionEvent event) {
        adminMode = !adminMode;
        adminModeButton.setStyle(adminMode ? 
            "-fx-background-color: #27ae60; -fx-text-fill: white;" : 
            "-fx-background-color: #f39c12; -fx-text-fill: white;");
        
        // Toggle admin-only fields
        purchasePriceCol.setVisible(adminMode);
        minSellingPriceCol.setVisible(adminMode);
        minPriceStatusText.setVisible(adminMode);
        statusText.setVisible(adminMode);
        
        updateProfitStatus();
    }

    private void updateProfitStatus() {
        if (!adminMode) {
            statusText.setText("");
            minPriceStatusText.setText("");
            return;
        }
        
        try {
            double purchase = purchasePriceField.getText().isEmpty() ? 0 : 
                Double.parseDouble(purchasePriceField.getText());
            double selling = sellingPriceField.getText().isEmpty() ? 0 : 
                Double.parseDouble(sellingPriceField.getText());
            
            // First profit status
            if (selling >= purchase) {
                statusText.setFill(Color.GREEN);
                statusText.setText(String.format("Profit: ₹%.2f", (selling - purchase)));
            } else {
                statusText.setFill(Color.RED);
                statusText.setText(String.format("Loss: ₹%.2f", (purchase - selling)));
            }

            // Second profit status
            double minSelling = minSellingPriceField.getText().isEmpty() ? 0 : 
                Double.parseDouble(minSellingPriceField.getText());
            
            if (minSelling >= purchase) {
                minPriceStatusText.setFill(Color.GREEN);
                minPriceStatusText.setText(String.format("Min Price Profit: ₹%.2f", (minSelling - purchase)));
            } else {
                minPriceStatusText.setFill(Color.RED);
                minPriceStatusText.setText(String.format("Min Price Loss: ₹%.2f", (purchase - minSelling)));
            }
            
            if (minSelling < purchase) {
                minPriceStatusText.setText(minPriceStatusText.getText() + " (Warning!)");
            }
        } catch (NumberFormatException e) {
            statusText.setFill(Color.GRAY);
            statusText.setText("Enter valid numbers");
            minPriceStatusText.setText("");
        }
    }

    private void populateFormFields(PriceEntry entry) {
        productNameField.setText(entry.getName());
        purchasePriceField.setText(String.valueOf(entry.getPurchasePrice()));
        sellingPriceField.setText(String.valueOf(entry.getSellingPrice()));
        minSellingPriceField.setText(String.valueOf(entry.getMinSellingPrice()));
        quantityField.setText(String.valueOf(entry.getStockQuantity()));
        customerTypeCombo.setValue(entry.getCustomerType());
        categoryCombo.setValue(entry.getCategory());
        unitCombo.setValue(entry.getUnit());
        
        updateProfitStatus();
    }

    private void loadPriceData() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT p.id, p.name, p.purchase_price, p.selling_price, p.min_selling_price, " +
                 "p.stock_quantity, ct.name AS customer_type, c.name AS category, u.name AS unit " +
                 "FROM products p " +
                 "LEFT JOIN customer_types ct ON p.customer_type_id = ct.id " +
                 "LEFT JOIN categories c ON p.category_id = c.id " +
                 "LEFT JOIN units u ON p.unit_id = u.id")) {
            
            priceEntries.clear();
            while (rs.next()) {
                PriceEntry entry = new PriceEntry(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getDouble("purchase_price"),
                    rs.getDouble("selling_price"),
                    rs.getDouble("min_selling_price"),
                    rs.getDouble("stock_quantity"),
                    rs.getString("unit"),
                    rs.getString("customer_type"),
                    rs.getString("category")
                );
                priceEntries.add(entry);
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load product prices: " + e.getMessage());
        }
    }

    @FXML
private void handleRefresh(ActionEvent event) {
    // Clear form fields
    clearFormFields();

    // Reset dropdowns to fixed values only, removing dynamically added entries
    resetDropdowns();

    // Ensure the "➕ Add New" option is present at the end of each combo box list
    addAddNewOptionIfMissing(customerTypes);
    addAddNewOptionIfMissing(categories);
    addAddNewOptionIfMissing(units);

    // Refresh ComboBoxes with updated lists
    customerTypeCombo.setItems(customerTypes);
    categoryCombo.setItems(categories);
    unitCombo.setItems(units);

    // Select nothing initially in ComboBoxes
    customerTypeCombo.getSelectionModel().clearSelection();
    categoryCombo.getSelectionModel().clearSelection();
    unitCombo.getSelectionModel().clearSelection();

    // Reload data from the database into the table
    loadPriceData();
}

// Helper method to add "➕ Add New" if missing in list
private void addAddNewOptionIfMissing(ObservableList<String> list) {
    final String ADD_NEW_OPTION = "➕ Add New";
    if (!list.contains(ADD_NEW_OPTION)) {
        list.add(ADD_NEW_OPTION);
    } else {
        // Remove duplicates just in case
        while (list.lastIndexOf(ADD_NEW_OPTION) != list.indexOf(ADD_NEW_OPTION)) {
            list.remove(list.lastIndexOf(ADD_NEW_OPTION));
        }
    }
}


    @FXML
    private void handleSearch(ActionEvent event) {
        String searchTerm = searchField.getText().trim().toLowerCase();
        String customerType = customerTypeCombo.getValue();
        String category = categoryCombo.getValue();

        // Parse quantity (optional)
        double quantity = 0;
        try {
            if (!quantityField.getText().isEmpty()) {
                quantity = Double.parseDouble(quantityField.getText());
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter a valid numeric quantity");
            return;
        }

        ObservableList<PriceEntry> filteredList = FXCollections.observableArrayList();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT p.id, p.name, p.purchase_price, p.selling_price, p.min_selling_price, " +
                 "p.stock_quantity, ct.name AS customer_type, c.name AS category, u.name AS unit " +
                 "FROM products p " +
                 "LEFT JOIN customer_types ct ON p.customer_type_id = ct.id " +
                 "LEFT JOIN categories c ON p.category_id = c.id " +
                 "LEFT JOIN units u ON p.unit_id = u.id " +
                 "WHERE (ct.name = ? OR ? IS NULL OR ? = '') " +
                 "AND (c.name = ? OR ? IS NULL OR ? = '') " +
                 "AND (p.stock_quantity >= ? OR ? = 0)")) {

            stmt.setString(1, customerType);
            stmt.setString(2, customerType);
            stmt.setString(3, customerType);
            stmt.setString(4, category);
            stmt.setString(5, category);
            stmt.setString(6, category);
            stmt.setDouble(7, quantity);
            stmt.setDouble(8, quantity);

            ResultSet rs = stmt.executeQuery();
            
            // Convert search term to flexible pattern
            String flexiblePattern = searchTerm.replaceAll("(\\D)(\\d)", "$1.*$2")
                                            .replaceAll("(\\d)(\\D)", "$1.*$2")
                                            .replaceAll("\\s+", ".*")
                                            .replaceAll("[^a-zA-Z0-9.*]", ".*");
            
            Pattern pattern = Pattern.compile(flexiblePattern);

            while (rs.next()) {
                String productName = rs.getString("name").toLowerCase();
                
                // Check if product name matches our flexible pattern
                if (pattern.matcher(productName).find()) {
                    PriceEntry entry = new PriceEntry(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("purchase_price"),
                        rs.getDouble("selling_price"),
                        rs.getDouble("min_selling_price"),
                        rs.getDouble("stock_quantity"),
                        rs.getString("unit"),
                        rs.getString("customer_type"),
                        rs.getString("category")
                    );
                    filteredList.add(entry);
                }
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to search products: " + e.getMessage());
            return;
        }

        priceTable.setItems(filteredList);
        if (filteredList.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No Results", "No products found matching your criteria");
        }
    }

    @FXML
    private void handleAddPrice(ActionEvent event) {
        if (!validateForm()) return;
        
        // Create new entry from form data first (for immediate UI update)
        PriceEntry newEntry = createEntryFromForm();
        
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);
            
            String productName = productNameField.getText().trim();
            double purchasePrice = Double.parseDouble(purchasePriceField.getText());
            double sellingPrice = Double.parseDouble(sellingPriceField.getText());
            double minSellingPrice = Double.parseDouble(minSellingPriceField.getText());
            double quantity = Double.parseDouble(quantityField.getText());
            String customerType = customerTypeCombo.getValue();
            String category = categoryCombo.getValue();
            String unit = unitCombo.getValue();
            
            int customerTypeId = getOrCreateId(conn, "customer_types", customerType);
            int categoryId = getOrCreateId(conn, "categories", category);
            int unitId = getOrCreateId(conn, "units", unit);
            
            String sql = "INSERT INTO products (name, purchase_price, selling_price, min_selling_price, " +
                        "stock_quantity, customer_type_id, category_id, unit_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, productName);
                stmt.setDouble(2, purchasePrice);
                stmt.setDouble(3, sellingPrice);
                stmt.setDouble(4, minSellingPrice);
                stmt.setDouble(5, quantity);
                stmt.setInt(6, customerTypeId);
                stmt.setInt(7, categoryId);
                stmt.setInt(8, unitId);
                
                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating product failed, no rows affected.");
                }
                
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        // Update the entry with the generated ID
                        newEntry.setId(generatedKeys.getInt(1));
                        // Add to both main list and filtered list (if searching)
                        priceEntries.add(newEntry);
                        if (priceTable.getItems() != priceEntries) {
                            priceTable.getItems().add(newEntry);
                        }
                    }
                }
                
                conn.commit();
                clearFormFields();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Product added successfully");
            }
        } catch (NumberFormatException e) {
            // Remove the entry if there was an error
            priceEntries.remove(newEntry);
            if (priceTable.getItems() != priceEntries) {
                priceTable.getItems().remove(newEntry);
            }
            showAlert(Alert.AlertType.ERROR, "Input Error", "Please enter valid numeric values");
            rollbackTransaction(conn);
        } catch (SQLException e) {
            // Remove the entry if there was an error
            priceEntries.remove(newEntry);
            if (priceTable.getItems() != priceEntries) {
                priceTable.getItems().remove(newEntry);
            }
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to add product: " + e.getMessage());
            rollbackTransaction(conn);
        } finally {
            closeConnection(conn);
        }
    }

    private PriceEntry createEntryFromForm() {
        return new PriceEntry(
            0, // Temporary ID, will be updated after database insert
            productNameField.getText().trim(),
            Double.parseDouble(purchasePriceField.getText()),
            Double.parseDouble(sellingPriceField.getText()),
            Double.parseDouble(minSellingPriceField.getText()),
            Double.parseDouble(quantityField.getText()),
            unitCombo.getValue(),
            customerTypeCombo.getValue(),
            categoryCombo.getValue()
        );
    }

    @FXML
    private void handleUpdatePrice(ActionEvent event) {
        PriceEntry selectedEntry = priceTable.getSelectionModel().getSelectedItem();
        if (selectedEntry == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a product to update");
            return;
        }

        if (!validateForm()) return;
        
        // Create updated entry from form data
        PriceEntry updatedEntry = createEntryFromForm();
        updatedEntry.setId(selectedEntry.getId());
        
        // Store old values in case we need to revert
        PriceEntry oldEntry = new PriceEntry(selectedEntry);
        
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);
            
            // Update the entry in the UI immediately
            int index = priceEntries.indexOf(selectedEntry);
            if (index >= 0) {
                priceEntries.set(index, updatedEntry);
            }
            if (priceTable.getItems() != priceEntries) {
                int filteredIndex = priceTable.getItems().indexOf(selectedEntry);
                if (filteredIndex >= 0) {
                    priceTable.getItems().set(filteredIndex, updatedEntry);
                }
            }
            
            // Proceed with database update
            String productName = productNameField.getText().trim();
            double purchasePrice = Double.parseDouble(purchasePriceField.getText());
            double sellingPrice = Double.parseDouble(sellingPriceField.getText());
            double minSellingPrice = Double.parseDouble(minSellingPriceField.getText());
            double quantity = Double.parseDouble(quantityField.getText());
            String customerType = customerTypeCombo.getValue();
            String category = categoryCombo.getValue();
            String unit = unitCombo.getValue();
            
            int customerTypeId = getOrCreateId(conn, "customer_types", customerType);
            int categoryId = getOrCreateId(conn, "categories", category);
            int unitId = getOrCreateId(conn, "units", unit);
            
            String sql = "UPDATE products SET name = ?, purchase_price = ?, selling_price = ?, " +
                        "min_selling_price = ?, stock_quantity = ?, customer_type_id = ?, " +
                        "category_id = ?, unit_id = ? WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, productName);
                stmt.setDouble(2, purchasePrice);
                stmt.setDouble(3, sellingPrice);
                stmt.setDouble(4, minSellingPrice);
                stmt.setDouble(5, quantity);
                stmt.setInt(6, customerTypeId);
                stmt.setInt(7, categoryId);
                stmt.setInt(8, unitId);
                stmt.setInt(9, selectedEntry.getId());
                
                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Updating product failed, no rows affected.");
                }
                
                conn.commit();
                clearFormFields();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Product updated successfully");
            }
        } catch (NumberFormatException e) {
            // Revert UI changes if error occurred
            int index = priceEntries.indexOf(updatedEntry);
            if (index >= 0) {
                priceEntries.set(index, oldEntry);
            }
            if (priceTable.getItems() != priceEntries) {
                int filteredIndex = priceTable.getItems().indexOf(updatedEntry);
                if (filteredIndex >= 0) {
                    priceTable.getItems().set(filteredIndex, oldEntry);
                }
            }
            showAlert(Alert.AlertType.ERROR, "Input Error", "Please enter valid numeric values");
            rollbackTransaction(conn);
        } catch (SQLException e) {
            // Revert UI changes if error occurred
            int index = priceEntries.indexOf(updatedEntry);
            if (index >= 0) {
                priceEntries.set(index, oldEntry);
            }
            if (priceTable.getItems() != priceEntries) {
                int filteredIndex = priceTable.getItems().indexOf(updatedEntry);
                if (filteredIndex >= 0) {
                    priceTable.getItems().set(filteredIndex, oldEntry);
                }
            }
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to update product: " + e.getMessage());
            rollbackTransaction(conn);
        } finally {
            closeConnection(conn);
        }
    }

    @FXML
    private void handleDeletePrice(ActionEvent event) {
        PriceEntry selectedEntry = priceTable.getSelectionModel().getSelectedItem();
        if (selectedEntry == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a product to delete");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete Product");
        confirmation.setContentText("Are you sure you want to delete " + selectedEntry.getName() + "?");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Remove from UI immediately
            priceEntries.remove(selectedEntry);
            if (priceTable.getItems() != priceEntries) {
                priceTable.getItems().remove(selectedEntry);
            }
            
            Connection conn = null;
            try {
                conn = DatabaseConnection.getInstance().getConnection();
                conn.setAutoCommit(false);
                
                String sql = "DELETE FROM products WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, selectedEntry.getId());
                    
                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows == 0) {
                        throw new SQLException("Deleting product failed, no rows affected.");
                    }
                    
                    conn.commit();
                    clearFormFields();
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Product deleted successfully");
                }
            } catch (SQLException e) {
                // Add back if error occurred
                priceEntries.add(selectedEntry);
                if (priceTable.getItems() != priceEntries) {
                    priceTable.getItems().add(selectedEntry);
                }
                showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to delete product: " + e.getMessage());
                rollbackTransaction(conn);
            } finally {
                closeConnection(conn);
            }
        }
    }

    private void addNewDropdownValue(String typeName, String tableName, ObservableList<String> list) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New " + typeName);
        dialog.setHeaderText("Add a new " + typeName);
        dialog.setContentText("Enter " + typeName + " name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(value -> {
            if (value.trim().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Error", typeName + " name cannot be empty");
                return;
            }

            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO " + tableName + " (name) VALUES (?)")) {
                
                stmt.setString(1, value.trim());
                stmt.executeUpdate();
                list.add(value.trim());
                showAlert(Alert.AlertType.INFORMATION, "Success", typeName + " added successfully");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to add " + typeName + ": " + e.getMessage());
            }
        });
    }

    private int getOrCreateId(Connection conn, String tableName, String name) throws SQLException {
        if (name == null || name.isEmpty()) {
            throw new SQLException("Name cannot be null or empty");
        }

        // First try to get existing ID
        String sql = "SELECT id FROM " + tableName + " WHERE name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        
        // If not found, insert new record
        sql = "INSERT INTO " + tableName + " (name) VALUES (?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        
        throw new SQLException("Failed to get or create ID for " + tableName);
    }

    private boolean validateForm() {
        if (productNameField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Please enter product name");
            return false;
        }
        if (customerTypeCombo.getValue() == null || customerTypeCombo.getValue().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Please select customer type");
            return false;
        }
        if (categoryCombo.getValue() == null || categoryCombo.getValue().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Please select category");
            return false;
        }
        if (unitCombo.getValue() == null || unitCombo.getValue().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Please select unit");
            return false;
        }
        try {
            Double.parseDouble(purchasePriceField.getText());
            Double.parseDouble(sellingPriceField.getText());
            Double.parseDouble(minSellingPriceField.getText());
            Double.parseDouble(quantityField.getText());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Please enter valid numeric values");
            return false;
        }
        return true;
    }

    @FXML
    private void clearFormFields() {
        productNameField.clear();
        purchasePriceField.clear();
        sellingPriceField.clear();
        minSellingPriceField.clear();
        quantityField.clear();
        customerTypeCombo.getSelectionModel().clearSelection();
        categoryCombo.getSelectionModel().clearSelection();
        unitCombo.getSelectionModel().clearSelection();
        statusText.setText("");
        minPriceStatusText.setText("");
    }

    private void rollbackTransaction(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.rollback();
                }
            } catch (SQLException e) {
                System.err.println("Error rolling back transaction: " + e.getMessage());
            }
        }
    }

    private void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}