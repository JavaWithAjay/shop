package com.ajay.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import com.ajay.models.InventoryItem;
import com.ajay.DatabaseConnection;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Pattern;

public class InventoryAlertController {

    @FXML private TableView<InventoryItem> inventoryTable;
    @FXML private TableColumn<InventoryItem, Integer> idCol;
    @FXML private TableColumn<InventoryItem, String> nameCol;
    @FXML private TableColumn<InventoryItem, String> categoryCol;
    @FXML private TableColumn<InventoryItem, String> unitCol;
    @FXML private TableColumn<InventoryItem, Double> currentStockCol;
    @FXML private TableColumn<InventoryItem, Double> alertThresholdCol;
    
    @FXML private TextField searchField;
    @FXML private TextField productNameField;
    @FXML private TextField currentStockField;
    @FXML private TextField alertThresholdField;
    
    @FXML private ComboBox<String> categoryCombo;
    @FXML private ComboBox<String> unitCombo;
    
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button clearButton;
    @FXML private Button checkStockButton;
    @FXML private Button refreshButton;
    @FXML private Button searchButton;
    
    @FXML private Text statusText;
    
    // Fixed values for dropdowns
    private final ObservableList<String> fixedCategories = 
        FXCollections.observableArrayList("study", "Electronics", "Food", "Clothing", "tobacco", "Spice", "sweet");
    private final ObservableList<String> fixedUnits = 
        FXCollections.observableArrayList("Piece", "Kg", "gram", "Liter", "Box", "cartoon", "pouch", "Packet");
    
    // Current dropdown values (fixed + dynamic)
    private ObservableList<String> categories = FXCollections.observableArrayList();
    private ObservableList<String> units = FXCollections.observableArrayList();
    
    private ObservableList<InventoryItem> inventoryItems = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTableColumns();
        loadInitialData();
        setupFormListeners();
        
        statusText.setVisible(false);
        
        // Add Enter key listener for search
        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleSearch(new ActionEvent());
            }
        });
    }

    private void setupTableColumns() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));
        currentStockCol.setCellValueFactory(new PropertyValueFactory<>("currentStock"));
        alertThresholdCol.setCellValueFactory(new PropertyValueFactory<>("alertThreshold"));
        
        inventoryTable.setItems(inventoryItems);
    }

    private void loadInitialData() {
        resetDropdowns();
        loadInventoryData();
    }

    private void resetDropdowns() {
        // Reset to fixed values only
        categories.setAll(fixedCategories);
        units.setAll(fixedUnits);
        
        categoryCombo.setItems(categories);
        unitCombo.setItems(units);
    }

    private void setupFormListeners() {
        inventoryTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    populateFormFields(newSelection);
                }
            });
        
        currentStockField.textProperty().addListener((obs, oldVal, newVal) -> updateStockStatus());
        alertThresholdField.textProperty().addListener((obs, oldVal, newVal) -> updateStockStatus());
    }

    private void updateStockStatus() {
        try {
            double currentStock = currentStockField.getText().isEmpty() ? 0 : 
                Double.parseDouble(currentStockField.getText());
            double alertThreshold = alertThresholdField.getText().isEmpty() ? 0 : 
                Double.parseDouble(alertThresholdField.getText());
            
            if (currentStock <= alertThreshold) {
                statusText.setFill(Color.RED);
                statusText.setText("Stock is below threshold!");
                statusText.setVisible(true);
            } else {
                statusText.setFill(Color.GREEN);
                statusText.setText("Stock is sufficient");
                statusText.setVisible(true);
            }
        } catch (NumberFormatException e) {
            statusText.setFill(Color.GRAY);
            statusText.setText("Enter valid numbers");
        }
    }

    private void populateFormFields(InventoryItem item) {
        productNameField.setText(item.getName());
        currentStockField.setText(String.valueOf(item.getCurrentStock()));
        alertThresholdField.setText(String.valueOf(item.getAlertThreshold()));
        categoryCombo.setValue(item.getCategory());
        unitCombo.setValue(item.getUnit());
        
        updateStockStatus();
    }

    private void loadInventoryData() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT i.id, i.name, i.current_stock, i.alert_threshold, " +
                 "c.name AS category, u.name AS unit " +
                 "FROM inventory_items i " +
                 "LEFT JOIN categories c ON i.category_id = c.id " +
                 "LEFT JOIN units u ON i.unit_id = u.id")) {
            
            inventoryItems.clear();
            while (rs.next()) {
                InventoryItem item = new InventoryItem(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getDouble("current_stock"),
                    rs.getDouble("alert_threshold"),
                    rs.getString("unit"),
                    rs.getString("category")
                );
                inventoryItems.add(item);
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load inventory items: " + e.getMessage());
        }
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        clearFormFields();
        loadInitialData();
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        String searchTerm = searchField.getText().trim().toLowerCase();
        String category = categoryCombo.getValue();

        ObservableList<InventoryItem> filteredList = FXCollections.observableArrayList();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT i.id, i.name, i.current_stock, i.alert_threshold, " +
                 "c.name AS category, u.name AS unit " +
                 "FROM inventory_items i " +
                 "LEFT JOIN categories c ON i.category_id = c.id " +
                 "LEFT JOIN units u ON i.unit_id = u.id " +
                 "WHERE (c.name = ? OR ? IS NULL OR ? = '')")) {

            stmt.setString(1, category);
            stmt.setString(2, category);
            stmt.setString(3, category);

            ResultSet rs = stmt.executeQuery();
            
            // Convert search term to flexible pattern
            String flexiblePattern = searchTerm.replaceAll("(\\D)(\\d)", "$1.*$2")
                                            .replaceAll("(\\d)(\\D)", "$1.*$2")
                                            .replaceAll("\\s+", ".*")
                                            .replaceAll("[^a-zA-Z0-9.*]", ".*");
            
            Pattern pattern = Pattern.compile(flexiblePattern);

            while (rs.next()) {
                String productName = rs.getString("name").toLowerCase();
                
                if (pattern.matcher(productName).find()) {
                    InventoryItem item = new InventoryItem(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("current_stock"),
                        rs.getDouble("alert_threshold"),
                        rs.getString("unit"),
                        rs.getString("category")
                    );
                    filteredList.add(item);
                }
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to search inventory: " + e.getMessage());
            return;
        }

        inventoryTable.setItems(filteredList);
        if (filteredList.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No Results", "No items found matching your criteria");
        }
    }

    @FXML
    private void handleAddItem(ActionEvent event) {
        if (!validateForm()) return;
        
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);
            
            String productName = productNameField.getText().trim();
            double currentStock = Double.parseDouble(currentStockField.getText());
            double alertThreshold = Double.parseDouble(alertThresholdField.getText());
            String category = categoryCombo.getValue();
            String unit = unitCombo.getValue();
            
            int categoryId = getOrCreateId(conn, "categories", category);
            int unitId = getOrCreateId(conn, "units", unit);
            
            String sql = "INSERT INTO inventory_items (name, current_stock, alert_threshold, " +
                        "category_id, unit_id) VALUES (?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, productName);
                stmt.setDouble(2, currentStock);
                stmt.setDouble(3, alertThreshold);
                stmt.setInt(4, categoryId);
                stmt.setInt(5, unitId);
                
                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating inventory item failed, no rows affected.");
                }
                
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        System.out.println("Added item with ID: " + generatedKeys.getInt(1));
                    }
                }
                
                conn.commit();
                loadInventoryData();
                clearFormFields();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Inventory item added successfully");
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Please enter valid numeric values");
            rollbackTransaction(conn);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to add inventory item: " + e.getMessage());
            rollbackTransaction(conn);
        } finally {
            closeConnection(conn);
        }
    }

    @FXML
    private void handleUpdateItem(ActionEvent event) {
        InventoryItem selectedItem = inventoryTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an item to update");
            return;
        }

        if (!validateForm()) return;
        
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);
            
            int itemId = selectedItem.getId();
            String productName = productNameField.getText().trim();
            double currentStock = Double.parseDouble(currentStockField.getText());
            double alertThreshold = Double.parseDouble(alertThresholdField.getText());
            String category = categoryCombo.getValue();
            String unit = unitCombo.getValue();
            
            int categoryId = getOrCreateId(conn, "categories", category);
            int unitId = getOrCreateId(conn, "units", unit);
            
            String sql = "UPDATE inventory_items SET name = ?, current_stock = ?, alert_threshold = ?, " +
                        "category_id = ?, unit_id = ? WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, productName);
                stmt.setDouble(2, currentStock);
                stmt.setDouble(3, alertThreshold);
                stmt.setInt(4, categoryId);
                stmt.setInt(5, unitId);
                stmt.setInt(6, itemId);
                
                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Updating item failed, no rows affected.");
                }
                
                conn.commit();
                loadInventoryData();
                clearFormFields();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Inventory item updated successfully");
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Please enter valid numeric values");
            rollbackTransaction(conn);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to update inventory item: " + e.getMessage());
            rollbackTransaction(conn);
        } finally {
            closeConnection(conn);
        }
    }

    @FXML
    private void handleDeleteItem(ActionEvent event) {
        InventoryItem selectedItem = inventoryTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an item to delete");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete Inventory Item");
        confirmation.setContentText("Are you sure you want to delete " + selectedItem.getName() + "?");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Connection conn = null;
            try {
                conn = DatabaseConnection.getInstance().getConnection();
                conn.setAutoCommit(false);
                
                String sql = "DELETE FROM inventory_items WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, selectedItem.getId());
                    
                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows == 0) {
                        throw new SQLException("Deleting item failed, no rows affected.");
                    }
                    
                    conn.commit();
                    loadInventoryData();
                    clearFormFields();
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Inventory item deleted successfully");
                }
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to delete item: " + e.getMessage());
                rollbackTransaction(conn);
            } finally {
                closeConnection(conn);
            }
        }
    }

 @FXML
    private void handleCheckStock(ActionEvent event) {
        showLowStockAlertDialog();
    }

    private void showLowStockAlertDialog() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT i.name, i.current_stock, i.alert_threshold, u.name AS unit " +
                 "FROM inventory_items i " +
                 "LEFT JOIN units u ON i.unit_id = u.id " +
                 "WHERE i.current_stock <= i.alert_threshold")) {

            VBox alertContent = new VBox(10);
            alertContent.setPadding(new Insets(15));

            // Create title
            Text titleText = new Text("LOW STOCK ALERT");
            titleText.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-fill: #d9534f;");

            // Create date
            Text dateText = new Text("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
            dateText.setStyle("-fx-font-size: 12px;");

            // Create table
            TableView<InventoryItem> alertTable = new TableView<>();
            alertTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

            // Product column
            TableColumn<InventoryItem, String> productCol = new TableColumn<>("Product");
            productCol.setCellValueFactory(new PropertyValueFactory<>("name"));

            // Current Stock column
            TableColumn<InventoryItem, Double> stockCol = new TableColumn<>("Current Stock");
            stockCol.setCellValueFactory(new PropertyValueFactory<>("currentStock"));

            // Threshold column
            TableColumn<InventoryItem, Double> thresholdCol = new TableColumn<>("Threshold");
            thresholdCol.setCellValueFactory(new PropertyValueFactory<>("alertThreshold"));

            // Unit column
            TableColumn<InventoryItem, String> unitCol = new TableColumn<>("Unit");
            unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));

            // Status column
            TableColumn<InventoryItem, String> statusCol = new TableColumn<>("Status");
            statusCol.setCellValueFactory(cellData -> {
                double stock = cellData.getValue().getCurrentStock();
                double threshold = cellData.getValue().getAlertThreshold();
                String status;
                if (stock <= 1) {
                    status = "CRITICAL (<=1)";
                } else if (stock <= 3) {
                    status = "URGENT (<=3)";
                } else {
                    status = "WARNING (<=10)";
                }
                return new SimpleStringProperty(status);
            });

           // To this:
            alertTable.getColumns().add(productCol);
            alertTable.getColumns().add(stockCol);
            alertTable.getColumns().add(thresholdCol);
            alertTable.getColumns().add(unitCol);
            alertTable.getColumns().add(statusCol);

            ObservableList<InventoryItem> alertItems = FXCollections.observableArrayList();

            while (rs.next()) {
                InventoryItem item = new InventoryItem(
                    0,
                    rs.getString("name"),
                    rs.getDouble("current_stock"),
                    rs.getDouble("alert_threshold"),
                    rs.getString("unit"),
                    ""
                );
                alertItems.add(item);
            }

            alertTable.setItems(alertItems);

            // Create PDF button
            Button pdfButton = new Button("Generate PDF");
            pdfButton.setStyle("-fx-background-color: #337ab7; -fx-text-fill: white;");
            pdfButton.setOnAction(e -> generatePdfReport(alertItems));

            alertContent.getChildren().addAll(
                titleText,
                dateText,
                new Separator(),
                alertTable,
                pdfButton
            );

            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Low Stock Alert");
            alert.setHeaderText(null);
            alert.getDialogPane().setContent(alertContent);
            alert.getDialogPane().setPrefSize(600, 400);

            if (alertItems.isEmpty()) {
                alert.setAlertType(Alert.AlertType.INFORMATION);
                alert.setContentText("All items are above their threshold levels");
            }

            alert.showAndWait();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error",
                "Failed to check stock levels: " + e.getMessage());
        }
    }

    private void generatePdfReport(ObservableList<InventoryItem> items) {
        if (items.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No Data", "No low stock items to generate PDF");
            return;
        }

        // Get the user's downloads folder
        String userHome = System.getProperty("user.home");
        File downloadsFolder = new File(userHome, "Downloads");
        String filePath = downloadsFolder.getPath() + File.separator + 
                         "Low_Stock_Alert_" + 
                         LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";

        Document document = new Document();
        try {
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            // Add title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.RED);
            Paragraph title = new Paragraph("LOW STOCK ALERT", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            // Add date
            Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.BLACK);
            Paragraph date = new Paragraph("Date: " + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")), dateFont);
            date.setAlignment(Element.ALIGN_CENTER);
            date.setSpacingAfter(20);
            document.add(date);

            // Create table with 5 columns
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // Add table headers
            String[] headers = {"Product", "Current Stock", "Threshold", "Unit", "Status"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header));
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                table.addCell(cell);
            }

            // Add table rows
            for (InventoryItem item : items) {
                table.addCell(createCell(item.getName()));
                
                // Highlight stock values in red
                table.addCell(createRedCell(String.valueOf(item.getCurrentStock())));
                table.addCell(createCell(String.valueOf(item.getAlertThreshold())));
                table.addCell(createCell(item.getUnit()));
                
                // Add status with appropriate color
                double stock = item.getCurrentStock();
                String status;
                if (stock <= 1) {
                    status = "CRITICAL (<=1)";
                } else if (stock <= 3) {
                    status = "URGENT (<=3)";
                } else {
                    status = "WARNING (<=10)";
                }
                table.addCell(createRedCell(status));
            }

            document.add(table);
            document.close();

            showAlert(Alert.AlertType.INFORMATION, "PDF Generated", 
                "Low stock report has been saved to:\n" + filePath);

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "PDF Generation Error", 
                "Failed to generate PDF: " + e.getMessage());
        }
    }

    private PdfPCell createCell(String content) {
        PdfPCell cell = new PdfPCell(new Phrase(content));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        return cell;
    }

    private PdfPCell createRedCell(String content) {
        PdfPCell cell = new PdfPCell(new Phrase(content, 
            FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.RED)));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        return cell;
    }



    @FXML
    public void handleNewCategory(ActionEvent event) {
        addNewDropdownValue("category", "categories", categories);
    }

    @FXML
    public void handleNewUnit(ActionEvent event) {
        addNewDropdownValue("unit", "units", units);
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
        if (categoryCombo.getValue() == null || categoryCombo.getValue().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Please select category");
            return false;
        }
        if (unitCombo.getValue() == null || unitCombo.getValue().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Please select unit");
            return false;
        }
        try {
            Double.parseDouble(currentStockField.getText());
            Double.parseDouble(alertThresholdField.getText());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Please enter valid numeric values");
            return false;
        }
        return true;
    }

    @FXML
    private void handleClear(ActionEvent event) {
        clearFormFields();
    }

    private void clearFormFields() {
        productNameField.clear();
        currentStockField.clear();
        alertThresholdField.clear();
        categoryCombo.getSelectionModel().clearSelection();
        unitCombo.getSelectionModel().clearSelection();
        statusText.setText("");
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