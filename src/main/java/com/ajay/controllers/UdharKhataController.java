package com.ajay.controllers;

import com.ajay.models.UdharKhataEntry;
import com.ajay.DatabaseConnection;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public class UdharKhataController {

    @FXML private TableView<UdharKhataEntry> khataTable;
    @FXML private TableColumn<UdharKhataEntry, Integer> idCol;
    @FXML private TableColumn<UdharKhataEntry, String> customerNameCol;
    @FXML private TableColumn<UdharKhataEntry, String> phoneCol;
    @FXML private TableColumn<UdharKhataEntry, String> productCol;
    @FXML private TableColumn<UdharKhataEntry, Double> amountCol;
    @FXML private TableColumn<UdharKhataEntry, Double> paidCol;
    @FXML private TableColumn<UdharKhataEntry, String> dateCol;
    @FXML private TableColumn<UdharKhataEntry, String> statusCol;
    @FXML private TableColumn<UdharKhataEntry, String> descCol;
    
    @FXML private TextField searchField;
    @FXML private TextField customerNameField;
    @FXML private TextField phoneField;
    @FXML private TextField productField;
    @FXML private TextField amountField;
    @FXML private TextField paidField;
    @FXML private TextField dateField;
    @FXML private TextArea descArea;
    
    @FXML private Button addButton;
    //@FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button clearButton;
    @FXML private Button refreshButton;
    @FXML private Button clearSearchButton;
    @FXML private Button generatePdfButton;
    
    @FXML private Text totalUdharText;
    @FXML private Text totalPaidText;
    @FXML private Text balanceText;
    
    private ObservableList<UdharKhataEntry> khataEntries = FXCollections.observableArrayList();
    private FilteredList<UdharKhataEntry> filteredEntries = new FilteredList<>(khataEntries, p -> true);
    
    // Date formatters
    private final DateTimeFormatter displayDateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private final DateTimeFormatter dbDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
public void initialize() {
    try {
        setupTableColumns();
        // Set current date by default with validation
        dateField.setText(LocalDate.now().format(displayDateFormat));
        loadInitialData();
        setupFormListeners();
        
        // Real-time search filtering
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterEntries());
        phoneField.textProperty().addListener((obs, oldVal, newVal) -> filterEntries());
        
        khataEntries.addListener((ListChangeListener.Change<? extends UdharKhataEntry> c) -> {
            updateSummary();
        });
    } catch (Exception e) {
        showAlert(Alert.AlertType.ERROR, "Initialization Error", "Failed to initialize: " + e.getMessage());
    }
}

    private void setupTableColumns() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        customerNameCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));
        productCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        paidCol.setCellValueFactory(new PropertyValueFactory<>("paidAmount"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        
        khataTable.setItems(filteredEntries);
        
        // Add listener for table selection
        khataTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    populateFormFields(newSelection);
                }
            });
    }

    private void filterEntries() {
        String searchTerm = searchField.getText().trim().toLowerCase();
        String phoneTerm = phoneField.getText().trim().toLowerCase();
        
        filteredEntries.setPredicate(entry -> {
            if (searchTerm.isEmpty() && phoneTerm.isEmpty()) {
                return true;
            }
            
            // Safe handling of null values
            String productName = entry.getProductName() != null ? entry.getProductName().toLowerCase() : "";
            String customerName = entry.getCustomerName() != null ? entry.getCustomerName().toLowerCase() : "";
            String phone = entry.getPhone() != null ? entry.getPhone().toLowerCase() : "";
            String description = entry.getDescription() != null ? entry.getDescription().toLowerCase() : "";
            
            boolean matchesPhone = phone.contains(phoneTerm);
            boolean matchesSearch = customerName.contains(searchTerm) || 
                                  productName.contains(searchTerm) || 
                                  phone.contains(searchTerm) ||
                                  description.contains(searchTerm);
            
            if (!phoneTerm.isEmpty() && !searchTerm.isEmpty()) {
                return matchesPhone && matchesSearch;
            } else if (!phoneTerm.isEmpty()) {
                return matchesPhone;
            } else {
                return matchesSearch;
            }
        });
    }

    private void loadInitialData() {
        loadKhataData();
        updateSummary();
    }

    private void setupFormListeners() {
        amountField.textProperty().addListener((obs, oldVal, newVal) -> updateStatus());
        paidField.textProperty().addListener((obs, oldVal, newVal) -> updateStatus());
    }

    private void updateStatus() {
        try {
            double amount = amountField.getText().isEmpty() ? 0 : Double.parseDouble(amountField.getText());
            double paid = paidField.getText().isEmpty() ? 0 : Double.parseDouble(paidField.getText());
            
            if (amount > paid) {
                balanceText.setFill(Color.RED);
                balanceText.setText(String.format("Balance: ₹%.2f", (amount - paid)));
            } else {
                balanceText.setFill(Color.GREEN);
                balanceText.setText("Fully Paid");
            }
        } catch (NumberFormatException e) {
            balanceText.setFill(Color.GRAY);
            balanceText.setText("Enter valid numbers");
        }
    }

    private void populateFormFields(UdharKhataEntry entry) {
        customerNameField.setText(entry.getCustomerName());
        phoneField.setText(entry.getPhone());
        productField.setText(entry.getProductName() != null ? entry.getProductName() : "");
        amountField.setText(String.valueOf(entry.getAmount()));
        paidField.setText(String.valueOf(entry.getPaidAmount()));
        dateField.setText(entry.getDate());
        descArea.setText(entry.getDescription() != null ? entry.getDescription() : "");
        
        updateStatus();
    }

    private void loadKhataData() {
    try (Connection conn = DatabaseConnection.getInstance().getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(
             "SELECT id, customer_name, phone, product_name, amount, paid_amount, " +
             "date, description FROM udhar_khata ORDER BY date DESC")) {
        
        khataEntries.clear();
        while (rs.next()) {
            // Safely handle null dates
            String dbDate = rs.getString("date");
            String displayDate = "";
            
            if (dbDate != null && !dbDate.trim().isEmpty()) {
                try {
                    // First try to parse as DB format (YYYY-MM-DD)
                    LocalDate date = LocalDate.parse(dbDate, dbDateFormat);
                    displayDate = date.format(displayDateFormat);
                } catch (DateTimeParseException e1) {
                    try {
                        // If that fails, try display format (DD-MM-YYYY)
                        LocalDate date = LocalDate.parse(dbDate, displayDateFormat);
                        displayDate = dbDate; // already in display format
                    } catch (DateTimeParseException e2) {
                        // If both fail, use raw value
                        displayDate = dbDate;
                    }
                }
            }
            
            // Safely handle all other fields
            UdharKhataEntry entry = new UdharKhataEntry(
                rs.getInt("id"),
                rs.getString("customer_name") != null ? rs.getString("customer_name") : "",
                rs.getString("phone") != null ? rs.getString("phone") : "",
                rs.getString("product_name") != null ? rs.getString("product_name") : "",
                rs.getDouble("amount"),
                rs.getDouble("paid_amount"),
                displayDate,
                rs.getString("description") != null ? rs.getString("description") : ""
            );
            khataEntries.add(entry);
        }
    } catch (SQLException e) {
        showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load Udhar Khata: " + e.getMessage());
    }
}

    private void updateSummary() {
        double totalUdhar = khataEntries.stream().mapToDouble(UdharKhataEntry::getAmount).sum();
        double totalPaid = khataEntries.stream().mapToDouble(UdharKhataEntry::getPaidAmount).sum();
        double balance = totalUdhar - totalPaid;
        
        totalUdharText.setText(String.format("Total Udhar: ₹%.2f", totalUdhar));
        totalPaidText.setText(String.format("Total Paid: ₹%.2f", totalPaid));
        
        if (balance > 0) {
            balanceText.setFill(Color.RED);
            balanceText.setText(String.format("Balance: ₹%.2f", balance));
        } else {
            balanceText.setFill(Color.GREEN);
            balanceText.setText("All Cleared");
        }
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        clearFormFields();
        loadInitialData();
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        searchField.clear();
        phoneField.clear();
    }

    @FXML
private void handleAddEntry(ActionEvent event) {
    if (!validateForm()) return;

    LocalDate parsedDate;
    try {
        // Validate and parse the date early
        parsedDate = LocalDate.parse(dateField.getText().trim(), displayDateFormat);
    } catch (DateTimeParseException e) {
        showAlert(Alert.AlertType.ERROR, "Invalid Date", "Please enter date in DD-MM-YYYY format");
        return;
    }

    Connection conn = null;
    try {
        conn = DatabaseConnection.getInstance().getConnection();
        conn.setAutoCommit(false);

        String sql = "INSERT INTO udhar_khata (customer_name, phone, product_name, " +
                     "amount, paid_amount, date, description) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, customerNameField.getText().trim());
            stmt.setString(2, phoneField.getText().trim());
            stmt.setString(3, productField.getText().trim());
            stmt.setDouble(4, Double.parseDouble(amountField.getText().trim()));
            stmt.setDouble(5, Double.parseDouble(paidField.getText().trim()));
            stmt.setString(6, parsedDate.format(dbDateFormat));
            stmt.setString(7, descArea.getText().trim());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating entry failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    UdharKhataEntry newEntry = new UdharKhataEntry(
                        generatedKeys.getInt(1),
                        customerNameField.getText().trim(),
                        phoneField.getText().trim(),
                        productField.getText().trim(),
                        Double.parseDouble(amountField.getText().trim()),
                        Double.parseDouble(paidField.getText().trim()),
                        dateField.getText().trim(), // Keep original display format
                        descArea.getText().trim()
                    );
                    khataEntries.add(newEntry);
                }
            }

            conn.commit();
            clearFormFields();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Udhar entry added successfully");
        }
    } catch (NumberFormatException e) {
        showAlert(Alert.AlertType.ERROR, "Input Error", "Please enter valid numeric values for amount and paid amount.");
        rollbackTransaction(conn);
    } catch (SQLException e) {
        showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to add entry: " + e.getMessage());
        rollbackTransaction(conn);
    } finally {
        closeConnection(conn);
    }
}


    // @FXML
    // private void handleUpdateEntry(ActionEvent event) {
    //     UdharKhataEntry selectedEntry = khataTable.getSelectionModel().getSelectedItem();
    //     if (selectedEntry == null) {
    //         showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an entry to update");
    //         return;
    //     }

    //     if (!validateForm()) return;
        
    //     Connection conn = null;
    //     try {
    //         conn = DatabaseConnection.getInstance().getConnection();
    //         conn.setAutoCommit(false);
            
    //         String sql = "UPDATE udhar_khata SET customer_name = ?, phone = ?, product_name = ?, " +
    //                     "amount = ?, paid_amount = ?, date = ?, description = ? WHERE id = ?";
            
    //         try (PreparedStatement stmt = conn.prepareStatement(sql)) {
    //             stmt.setString(1, customerNameField.getText().trim());
    //             stmt.setString(2, phoneField.getText().trim());
    //             stmt.setString(3, productField.getText().trim());
    //             stmt.setDouble(4, Double.parseDouble(amountField.getText()));
    //             stmt.setDouble(5, Double.parseDouble(paidField.getText()));
                
    //             // Convert display date to DB format
    //             LocalDate date = LocalDate.parse(dateField.getText().trim(), displayDateFormat);
    //             stmt.setString(6, date.format(dbDateFormat));
                
    //             stmt.setString(7, descArea.getText().trim());
    //             stmt.setInt(8, selectedEntry.getId());
                
    //             int affectedRows = stmt.executeUpdate();
    //             if (affectedRows == 0) {
    //                 throw new SQLException("Updating entry failed, no rows affected.");
    //             }
                
    //             // Update the entry
    //             selectedEntry.setCustomerName(customerNameField.getText().trim());
    //             selectedEntry.setPhone(phoneField.getText().trim());
    //             selectedEntry.setProductName(productField.getText().trim());
    //             selectedEntry.setAmount(Double.parseDouble(amountField.getText()));
    //             selectedEntry.setPaidAmount(Double.parseDouble(paidField.getText()));
    //             selectedEntry.setDate(dateField.getText().trim());
    //             selectedEntry.setDescription(descArea.getText().trim());
                
    //             khataTable.refresh();
                
    //             conn.commit();
    //             clearFormFields();
    //             showAlert(Alert.AlertType.INFORMATION, "Success", "Udhar entry updated successfully");
    //         }
    //     } catch (DateTimeParseException e) {
    //         showAlert(Alert.AlertType.ERROR, "Date Error", "Please enter date in DD-MM-YYYY format");
    //         rollbackTransaction(conn);
    //     } catch (NumberFormatException e) {
    //         showAlert(Alert.AlertType.ERROR, "Input Error", "Please enter valid numeric values");
    //         rollbackTransaction(conn);
    //     } catch (SQLException e) {
    //         showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to update entry: " + e.getMessage());
    //         rollbackTransaction(conn);
    //     } finally {
    //         closeConnection(conn);
    //     }
    // }

    @FXML
    private void handleDeleteEntry(ActionEvent event) {
        UdharKhataEntry selectedEntry = khataTable.getSelectionModel().getSelectedItem();
        if (selectedEntry == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an entry to delete");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete Udhar Entry");
        confirmation.setContentText("Are you sure you want to delete entry for " + 
                                  selectedEntry.getCustomerName() + "?");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Connection conn = null;
            try {
                conn = DatabaseConnection.getInstance().getConnection();
                conn.setAutoCommit(false);
                
                String sql = "DELETE FROM udhar_khata WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, selectedEntry.getId());
                    
                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows == 0) {
                        throw new SQLException("Deleting entry failed, no rows affected.");
                    }
                    
                    khataEntries.remove(selectedEntry);
                    
                    conn.commit();
                    clearFormFields();
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Udhar entry deleted successfully");
                }
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to delete entry: " + e.getMessage());
                rollbackTransaction(conn);
            } finally {
                closeConnection(conn);
            }
        }
    }

    @FXML
    private void handleGeneratePdf(ActionEvent event) {
        ObservableList<UdharKhataEntry> entriesToPrint = filteredEntries.isEmpty() ? khataEntries : filteredEntries;
        
        if (entriesToPrint.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No Data", "There is no data to generate PDF");
            return;
        }

        String customerName = "";
        if (!filteredEntries.isEmpty()) {
            customerName = "_" + filteredEntries.get(0).getCustomerName().replaceAll("[^a-zA-Z0-9]", "");
        }

        String userHome = System.getProperty("user.home");
        File downloadsFolder = new File(userHome, "Downloads");
        String filePath = downloadsFolder.getPath() + File.separator + 
                         "Udhar_Khata_Report" + customerName + "_" + 
                         LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

        Document document = new Document();
        try {
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
            Paragraph title = new Paragraph("UDHAR KHATA REPORT", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.BLACK);
            Paragraph date = new Paragraph("Generated on: " + 
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")), dateFont);
            date.setAlignment(Element.ALIGN_CENTER);
            date.setSpacingAfter(20);
            document.add(date);

            double totalUdhar = entriesToPrint.stream().mapToDouble(UdharKhataEntry::getAmount).sum();
            double totalPaid = entriesToPrint.stream().mapToDouble(UdharKhataEntry::getPaidAmount).sum();
            double balance = totalUdhar - totalPaid;

            Font summaryFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Paragraph summary = new Paragraph();
            summary.add(new Chunk("Total Udhar: ₹" + String.format("%.2f", totalUdhar) + "  ", summaryFont));
            summary.add(new Chunk("Total Paid: ₹" + String.format("%.2f", totalPaid) + "  ", summaryFont));
            
            if (balance > 0) {
                summary.add(new Chunk("Balance: ₹" + String.format("%.2f", balance), 
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.RED)));
            } else {
                summary.add(new Chunk("All Cleared", 
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.GREEN)));
            }
            
            summary.setSpacingAfter(20);
            document.add(summary);

            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            String[] headers = {"ID", "Customer", "Phone", "Product", "Amount", "Paid", "Status", "Date"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header));
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                table.addCell(cell);
            }

            for (UdharKhataEntry entry : entriesToPrint) {
                table.addCell(createCell(String.valueOf(entry.getId())));
                table.addCell(createCell(entry.getCustomerName()));
                table.addCell(createCell(entry.getPhone()));
                table.addCell(createCell(entry.getProductName() != null ? entry.getProductName() : ""));
                table.addCell(createCell(String.format("₹%.2f", entry.getAmount())));
                table.addCell(createCell(String.format("₹%.2f", entry.getPaidAmount())));
                
                if (entry.getStatus().equals("Pending")) {
                    table.addCell(createRedCell(entry.getStatus()));
                } else {
                    table.addCell(createCell(entry.getStatus()));
                }
                
                table.addCell(createCell(entry.getDate()));
            }

            document.add(table);
            document.close();

            showAlert(Alert.AlertType.INFORMATION, "PDF Generated", 
                "Udhar Khata report has been saved to:\n" + filePath);

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

    private boolean validateForm() {
        if (customerNameField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Please enter customer name");
            return false;
        }
        if (productField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Please enter product name");
            return false;
        }
        try {
            Double.parseDouble(amountField.getText());
            Double.parseDouble(paidField.getText());
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
        customerNameField.clear();
        phoneField.clear();
        productField.clear();
        amountField.clear();
        paidField.clear();
        dateField.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        descArea.clear();
        balanceText.setText("");
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