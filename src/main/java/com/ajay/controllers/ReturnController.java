package com.ajay.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import java.time.LocalDate;
import com.ajay.models.ReturnItem;
import com.ajay.DatabaseConnection;
import java.sql.*;
import java.util.Optional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ReturnController<MyType> {

    @FXML private TableView<ReturnItem> returnTable;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private Label todayReturns;
    @FXML private Label pendingReturns;
    @FXML private Label totalValue;

    private ObservableList<ReturnItem> returnItems = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Initialize status filter with unique values
        statusFilter.setItems(FXCollections.observableArrayList("All", "Pending", "Approved", "Rejected", "Processed"));
        statusFilter.setValue("All");
        
        // Set date pickers to today
        fromDate.setValue(LocalDate.now());
        toDate.setValue(LocalDate.now());
        
        // Enable multiple selection and editing
        returnTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        returnTable.setEditable(true);
        
        loadReturnData();
        updateStatistics();
        // Add context menu to table
        setupContextMenu();
    }

    private void setupContextMenu() {
    ContextMenu contextMenu = new ContextMenu();
    
    // MenuItem editItem = new MenuItem("Edit");
    // editItem.setOnAction(e -> editSelectedReturn());
    
    MenuItem deleteItem = new MenuItem("Delete");
    deleteItem.setOnAction(e -> deleteSelectedReturn());
    
    contextMenu.getItems().addAll( deleteItem);
    returnTable.setContextMenu(contextMenu);
}



    private void deleteSelectedReturn() {
    ReturnItem selected = returnTable.getSelectionModel().getSelectedItem();
    if (selected != null) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Return");
        confirm.setContentText("Are you sure you want to delete this return record?");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteReturnFromDatabase(selected);
            loadReturnData(); // Refresh table
        }
    } else {
        showAlert("No Selection", "Please select a return to delete");
    }
}

private void deleteReturnFromDatabase(ReturnItem item) {
    String query = "DELETE FROM returns WHERE customer_name=? AND product_name=?";
    try (Connection conn = DatabaseConnection.getInstance().getConnection();
         PreparedStatement ps = conn.prepareStatement(query)) {
        ps.setString(1, item.getCustomerName());
        ps.setString(2, item.getProductName());
        ps.executeUpdate();
    } catch (SQLException e) {
        showAlert("Error", "Failed to delete return: " + e.getMessage());
        e.printStackTrace();
    }
}

    private void loadReturnData() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM returns ORDER BY return_date DESC")) {
            
            returnItems.clear();
            while (rs.next()) {
                returnItems.add(new ReturnItem(
                    rs.getString("customer_Name"),
                    rs.getString("product_name"),
                    rs.getDouble("quantity"),
                    rs.getDouble("price"),
                    rs.getDate("return_date").toLocalDate(),
                    rs.getString("reason"),
                    rs.getString("status")
                ));
            }
            returnTable.setItems(returnItems);
        } catch (SQLException e) {
            showAlert("Error", "Failed to load return data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText().trim().toLowerCase();
        if (searchTerm.isEmpty()) {
            returnTable.setItems(returnItems);
            return;
        }

        // Convert search term to flexible pattern
        String flexiblePattern = searchTerm.replaceAll("(\\D)(\\d)", "$1.*$2")
                                        .replaceAll("(\\d)(\\D)", "$1.*$2")
                                        .replaceAll("\\s+", ".*")
                                        .replaceAll("[^a-zA-Z0-9.*]", ".*");

        ObservableList<ReturnItem> filtered = FXCollections.observableArrayList();
        for (ReturnItem item : returnItems) {
            if (item.getProductName().toLowerCase().matches(".*" + flexiblePattern + ".*") || 
                String.valueOf(item.getCustomerName()).matches(".*" + flexiblePattern + ".*")) {
                filtered.add(item);
            }
        }
        returnTable.setItems(filtered);
    }

    @FXML
    private void clearSearch() {
        searchField.clear();
        returnTable.setItems(returnItems);
    }

    @FXML
    private void applyFilters() {
        String status = statusFilter.getValue();
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();

        ObservableList<ReturnItem> filtered = FXCollections.observableArrayList();
        for (ReturnItem item : returnItems) {
            boolean statusMatch = status.equals("All") || item.getStatus().equals(status);
            boolean dateMatch = true;
            
            if (from != null && item.getReturnDate().isBefore(from)) {
                dateMatch = false;
            }
            if (to != null && item.getReturnDate().isAfter(to)) {
                dateMatch = false;
            }
            
            if (statusMatch && dateMatch) {
                filtered.add(item);
            }
        }

        returnTable.setItems(filtered);
    }

    @FXML
    private void showAddReturnDialog() {
        Dialog<ReturnItem> dialog = new Dialog<>();
        dialog.setTitle("Add New Return");
        dialog.setHeaderText("Enter Return Details");

        // Set the button types
        ButtonType addButton = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButton, ButtonType.CANCEL);

        // Create the form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField customerNameField = new TextField();
        TextField productNameField = new TextField();
        TextField quantityField = new TextField();
        TextField priceField = new TextField();
        DatePicker returnDatePicker = new DatePicker(LocalDate.now());
        TextField reasonField = new TextField();
        ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList(
            "Pending", "Approved", "Rejected", "Processed"));

        grid.add(new Label("Customer"), 0, 0);
        grid.add(customerNameField, 1, 0);
        grid.add(new Label("Product Name:"), 0, 1);
        grid.add(productNameField, 1, 1);
        grid.add(new Label("Quantity:"), 0, 2);
        grid.add(quantityField, 1, 2);
        grid.add(new Label("Price:"), 0, 3);
        grid.add(priceField, 1, 3);
        grid.add(new Label("Return Date:"), 0, 4);
        grid.add(returnDatePicker, 1, 4);
        grid.add(new Label("Reason:"), 0, 5);
        grid.add(reasonField, 1, 5);
        grid.add(new Label("Status:"), 0, 6);
        grid.add(statusCombo, 1, 6);

        dialog.getDialogPane().setContent(grid);

        // Convert the result to a ReturnItem when the add button is clicked
       dialog.setResultConverter(dialogButton -> {
    if (dialogButton == addButton) {
        try {
            return new ReturnItem(
                customerNameField.getText(), // Now it's String instead of Integer
                productNameField.getText(),
                Double.parseDouble(quantityField.getText()),
                Double.parseDouble(priceField.getText()),
                returnDatePicker.getValue(),
                reasonField.getText(),
                statusCombo.getValue()
            );
        } catch (NumberFormatException e) {
            showAlert("Error", "Please enter valid numbers for quantity and price");
            return null;
        }
    }
    return null;
});


        Optional<ReturnItem> result = dialog.showAndWait();
        result.ifPresent(returnItem -> {
            try {
                addNewReturn(returnItem);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
    }

    private void addNewReturn(ReturnItem returnItem) throws Exception {
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {

        // Step: Insert return data directly (without checking bill number)
        String queryInsertReturn = "INSERT INTO returns (bill_number, customer_name, product_name, quantity, price, return_date, reason, status) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
PreparedStatement insertReturn = conn.prepareStatement(queryInsertReturn);

insertReturn.setString(1, returnItem.getBillNumber());
insertReturn.setString(2, returnItem.getCustomerName());
insertReturn.setString(3, returnItem.getProductName());
insertReturn.setInt(4, (int) returnItem.getQuantity());
insertReturn.setDouble(5, returnItem.getPrice());
insertReturn.setDate(6, java.sql.Date.valueOf(returnItem.getReturnDate()));
insertReturn.setString(7, returnItem.getReason());
insertReturn.setString(8, returnItem.getStatus());


        insertReturn.executeUpdate();

    } catch (SQLException e) {
        showAlert("Error", "Failed to add return: " + e.getMessage());
        e.printStackTrace();
    }
}





    @FXML
private void generatePdfReport() {
    try {
        // Create document and page
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);

        // Create content stream
        PDPageContentStream contentStream = new PDPageContentStream(document, page);

        // Add title
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
        contentStream.newLineAtOffset(100, 700);
        contentStream.showText("RETURN MANAGEMENT REPORT");
        contentStream.endText();

        // Add date
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 12);
        contentStream.newLineAtOffset(100, 670);
        contentStream.showText("Generated on: " + LocalDate.now());
        contentStream.endText();

        // Add table headers
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
        contentStream.newLineAtOffset(100, 640);
        contentStream.showText(String.format("%-10s %-20s %-6s %-8s %-10s", "Bill No", "Product", "Qty", "Price", "Status"));
        contentStream.endText();

        int y = 620;
        for (ReturnItem item : returnTable.getItems()) {
            if (y < 100) {
                contentStream.close();
                page = new PDPage();
                document.addPage(page);
                contentStream = new PDPageContentStream(document, page);
                y = 700;
            }

            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 12);
            contentStream.newLineAtOffset(100, y);
            contentStream.showText(String.format("%-10s %-20s %-6s %-8s %-10s",
                    item.getCustomerName(),
                    truncate(item.getProductName(), 20),
                    item.getQuantity(),
                    item.getPrice(),
                    item.getStatus()));
            contentStream.endText();
            y -= 20;
        }

        contentStream.close();

        // Save PDF
        String fileName = "Return_Report_" + LocalDate.now() + ".pdf";
        Path outputPath = Paths.get(System.getProperty("user.home"), "Downloads", fileName);
        document.save(outputPath.toFile());
        document.close();

        showAlert("PDF Generated", "Report saved to: " + outputPath.toString());
    } catch (IOException e) {
        showAlert("Error", "Failed to generate PDF: " + e.getMessage());
        e.printStackTrace();
    }
}

private String truncate(String text, int maxLength) {
    return text.length() > maxLength ? text.substring(0, maxLength - 1) + "…" : text;
}


    // Helper method to update status in database
    void updateReturnStatusInDB(ReturnItem item, String newStatus) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            String query = "UPDATE returns SET status = ? WHERE bill_number = ? AND product_name = ?";
            
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, newStatus);
                ps.setString(2, item.getCustomerName());
                ps.setString(3, item.getProductName());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to update status: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateStatistics() {
        long todayCount = 0;
        long pendingCount = 0;
        double total = 0;
        
        for (ReturnItem item : returnItems) {
            if (item.getReturnDate().equals(LocalDate.now())) {
                todayCount++;
            }
            if (item.getStatus().equals("Pending")) {
                pendingCount++;
            }
            total += item.getPrice() * item.getQuantity();
        }
        
        todayReturns.setText(String.valueOf(todayCount));
        pendingReturns.setText(String.valueOf(pendingCount));
        totalValue.setText(String.format("₹%.2f", total));
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void deleteReturnItem(ReturnItem item) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteReturnItem'");
    }

    
}