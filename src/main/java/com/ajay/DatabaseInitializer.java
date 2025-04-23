package com.ajay;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {
    public static void initialize() {
        try (Connection connection = DatabaseConnection.getInstance().getConnection()) {
            // Check if tables exist (you already have the SQL, this is just a check)
            System.out.println("Database connection established successfully.");
            
            // You can add verification queries here to check if tables exist
            // For example:
            try (Statement stmt = connection.createStatement()) {
                stmt.executeQuery("SELECT 1 FROM admins LIMIT 1");
                System.out.println("Database tables already exist.");
            } catch (SQLException e) {
                System.out.println("Some tables might be missing. Please ensure you've run all SQL scripts.");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}