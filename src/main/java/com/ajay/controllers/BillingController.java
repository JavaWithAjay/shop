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
import javafx.stage.DirectoryChooser;
import com.ajay.models.BillItem;
import com.ajay.DatabaseConnection;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.Optional;
import java.util.ResourceBundle;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.FileOutputStream;
import java.io.File;

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
            
            if (productField != null && productField.getScene() != null) {
                Stage stage = (Stage) productField.getScene().getWindow();
                stage.setMaximized(true);
            }
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
        if (customerTypeCombo != null) {
            customerTypeCombo.getItems().addAll("DISTRIBUTOR", "WHOLESALER", "RETAILER", "CUSTOMER");
            customerTypeCombo.setValue("RETAILER");
            
            customerTypeCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                updatePriceFieldLabels();
                updateProductPriceSilently();
            });
        }
    }

    private void setupUnits() {
        if (unitCombo == null) return;
        
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

    private ProductDetails getProductDetails(String productName) throws SQLException {
        if (customerTypeCombo == null) return null;
        
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

        if (totalLabel != null) totalLabel.setText(String.format("₹%.2f", totalAmount));
        if (purchaseTotalLabel != null) purchaseTotalLabel.setText(String.format("₹%.2f", totalPurchase));
        if (profitLabel != null) profitLabel.setText(String.format("₹%.2f", totalProfit));
    }

    @FXML
    private void removeSelected() {
        if (billTable == null) return;
        
        BillItem selected = billTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            billItems.remove(selected);
            updateTotals();
        } else {
            showAlert("Warning", "Please select an item to remove.");
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
            
            int billId = saveBillHeader(conn);
            saveBillItems(conn, billId);
            
            conn.commit();
            
            // Update totals from database after saving
            updateTotalsFromDatabase(billId);
        }
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

    private void updateTotalsFromDatabase(int billId) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT total, profit, purchase_total FROM bills WHERE id = ?")) {
            
            stmt.setInt(1, billId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                totalAmount = rs.getDouble("total");
                totalProfit = rs.getDouble("profit");
                totalPurchase = rs.getDouble("purchase_total");
                
                Platform.runLater(this::updateTotals);
            }
        }
    }

    @FXML
    private void retrieveData() {
        try {
            retrieveLastBill();
        } catch (SQLException e) {
            System.out.println(e);
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