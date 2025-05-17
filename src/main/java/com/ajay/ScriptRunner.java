package com.ajay;

import java.io.*;
import java.sql.*;

public class ScriptRunner {

    private final Connection connection;

    public ScriptRunner(Connection connection) {
        this.connection = connection;
    }

    public void runScript(Reader reader) throws IOException, SQLException {
        StringBuilder command = new StringBuilder();
        try (BufferedReader lineReader = new BufferedReader(reader)) {
            String line;
            Statement statement = connection.createStatement();
            while ((line = lineReader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("--") || line.isEmpty()) {
                    continue; // Ignore comments and empty lines
                }
                command.append(line);
                if (line.endsWith(";")) {
                    statement.execute(command.toString());
                    command.setLength(0); // Clear buffer
                }
            }
        }
    }
}
