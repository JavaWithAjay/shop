package com.ajay.controllers;

import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;
import com.ajay.models.ReturnItem;

public class ButtonCellFactory implements Callback<TableColumn<ReturnItem, Void>, TableCell<ReturnItem, Void>> {
    
    @Override
    public TableCell<ReturnItem, Void> call(final TableColumn<ReturnItem, Void> param) {
        return new TableCell<ReturnItem, Void>() {
            private final Button btn = new Button("View Details");
            
            {
                btn.setOnAction(event -> {
                    ReturnItem data = getTableView().getItems().get(getIndex());
                    // Handle button action here
                    System.out.println("Selected: " + data.getProductName());
                });
            }

            @Override
            public void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                }
            }
        };
    }
}