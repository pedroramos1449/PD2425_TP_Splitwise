package com.pd2025.splitwise.database;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class database {
    private String dbUrl = "jdbc:sqlite:C:/Users/Mario/IdeaProjects/PD2425_TP_Splitwise/your_database.db"; // Change this to your database file path

    public void loadSqlScript() {
        try (Connection connection = DriverManager.getConnection(dbUrl)) {
            if (connection != null) {
                // Read SQL script from resources
                InputStream inputStream = getClass().getClassLoader().getResourceAsStream("init.sql");
                if (inputStream != null) {
                    String sqlScript = new Scanner(inputStream, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next();

                    // Execute SQL script
                    try (Statement statement = connection.createStatement()) {
                        statement.execute(sqlScript);
                        System.out.println("Database initialized successfully!");
                    }

                    // Optional: Fetch and display the inserted message
                    String query = "SELECT message FROM test;";
                    try (var resultSet = statement.executeQuery(query)) {
                        while (resultSet.next()) {
                            System.out.println("Message from database: " + resultSet.getString("message"));
                        }
                    }
                } else {
                    System.out.println("SQL script not found!");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}