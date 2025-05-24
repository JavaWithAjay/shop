package com.ajay.controllers;

import javafx.scene.control.TableCell;
import javafx.scene.control.Button;
import com.ajay.models.ReturnItem;

public class ButtonTableCellFactory extends TableCell<ReturnItem, Void> {
    private final Button deleteButton = new Button("Delete");

    public ButtonTableCellFactory() {
        deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        deleteButton.setOnAction(event -> {
            ReturnItem item = getTableView().getItems().get(getIndex());
            ((ReturnController) getTableView().getScene().getRoot().getProperties().get("controller"))
                .deleteReturnItem(item);
        });
    }

    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setGraphic(null);
        } else {
            setGraphic(deleteButton);
        }
    }
}