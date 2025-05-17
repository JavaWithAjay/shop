package com.ajay;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseInitializer {

    public static void initialize() {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/grocery_shop_management", "root", "12345")) {

            ScriptRunner runner = new ScriptRunner(conn);

            // ✅ Use absolute or relative path correctly
            Reader reader = new BufferedReader(new FileReader("src/main/resources/com/ajay/mysql_setup.sql"));
            runner.runScript(reader);
            System.out.println("✅ Database setup complete.");

        } catch (Exception e) {
            System.out.println("❌ Database setup failed:");
            e.printStackTrace();
        }
    }
}
