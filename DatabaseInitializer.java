package database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class DatabaseInitializer {
    public static void initialize() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             InputStream is = DatabaseInitializer.class.getResourceAsStream("/sql/database_schema.sql");
             Scanner scanner = new Scanner(is)) {
            
            // Read and execute SQL file line by line
            scanner.useDelimiter(";\\s*");
            while (scanner.hasNext()) {
                String sql = scanner.next().trim();
                if (!sql.isEmpty()) {
                    stmt.execute(sql);
                }
            }
        } catch (SQLException | IOException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }
    }
}