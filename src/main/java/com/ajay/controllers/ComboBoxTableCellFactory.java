package com.ajay.controllers;

import javafx.scene.control.TableCell;
import javafx.scene.control.ComboBox;
import javafx.collections.FXCollections;
import com.ajay.models.ReturnItem;

public class ComboBoxTableCellFactory extends TableCell<ReturnItem, String> {
    private ComboBox<String> comboBox;

    public ComboBoxTableCellFactory() {
        comboBox = new ComboBox<>(FXCollections.observableArrayList(
            "Pending", "Approved", "Rejected", "Processed"));
        
        comboBox.setOnAction(event -> {
            if (isEditing()) {
                commitEdit(comboBox.getValue());
                ReturnItem item = getTableView().getItems().get(getIndex());
                ((ReturnController) getTableView().getScene().getRoot().getProperties().get("controller"))
                    .updateReturnStatusInDB(item, comboBox.getValue());
            }
        });
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setGraphic(null);
        } else {
            comboBox.setValue(item);
            setGraphic(comboBox);
        }
    }
}