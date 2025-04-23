package com.ajay;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Initialize database
        DatabaseInitializer.initialize();
        
        // Load the login view first
        Parent root = FXMLLoader.load(getClass().getResource("/com/ajay/views/login.fxml"));
        Scene scene = new Scene(root, 600, 500);
        
        // Add CSS styling
        scene.getStylesheets().add(getClass().getResource("/com/ajay/styles.css").toExternalForm());
        
        primaryStage.setTitle("Grocery Shop Management - Login");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop() {
        // Close DB connection when app exits
        DatabaseConnection.getInstance().closeConnection();
    }
}
