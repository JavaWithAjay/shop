package com.ajay;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    private static Stage primaryStageRef;

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStageRef = primaryStage;

        // Initialize database
        DatabaseInitializer.initialize();

        // Load the login view first
        Parent root = FXMLLoader.load(getClass().getResource("/com/ajay/views/login.fxml"));
        Scene scene = new Scene(root);

        // Add CSS styling
        scene.getStylesheets().add(getClass().getResource("/com/ajay/styles.css").toExternalForm());

        primaryStage.setTitle("Grocery Shop Management - Login");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true); // Set initial maximization
        primaryStage.show();
    }

    public static Stage getPrimaryStage() {
        return primaryStageRef;
    }

    public static void switchScene(String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(Main.class.getResource(fxmlPath));
            Scene scene = new Scene(root);
            scene.getStylesheets().add(Main.class.getResource("/com/ajay/styles.css").toExternalForm());
            primaryStageRef.setScene(scene);
            primaryStageRef.setTitle(title);
            primaryStageRef.setMaximized(true); // Ensure maximization on scene switch
        } catch (Exception e) {
            e.printStackTrace();
            // Handle the exception appropriately, maybe show an alert
        }
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