package com.smartcodeanalyzer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Simple test class to verify database connectivity
 */
public class DatabaseConnectionTest {
    
    public static void main(String[] args) {
        // Use the parameters from your MySQL Workbench connection
        String url = "jdbc:mysql://127.0.0.1:3306/";
        String dbName = "smart_code_analyzer_db";  // Using the database name we created in MySQL Workbench
        String username = "root";
        String password = "1zabiti1";
        
        System.out.println("Testing database connection...");
        System.out.println("URL: " + url + dbName);
        System.out.println("Username: " + username);
        
        try {
            // Load the MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL JDBC Driver loaded successfully");
            
            // Set connection properties
            java.util.Properties props = new java.util.Properties();
            props.setProperty("user", username);
            props.setProperty("password", password);
            props.setProperty("useSSL", "false");
            props.setProperty("allowPublicKeyRetrieval", "true");
            props.setProperty("serverTimezone", "UTC");
            
            // Connect to the database
            Connection connection = DriverManager.getConnection(url + dbName, props);
            System.out.println("Connection to the database established successfully!");
            
            // Check if connection is valid
            if (connection.isValid(5)) {
                System.out.println("Connection is valid and working properly.");
            }
            
            // Close the connection
            connection.close();
            System.out.println("Connection closed successfully.");
            
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found:");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Database connection error:");
            e.printStackTrace();
            
            // Try connecting without database name (to check if server is accessible)
            System.out.println("\nAttempting to connect to MySQL server without specifying a database...");
            try {
                Connection conn = DriverManager.getConnection(url, username, password);
                System.out.println("Connected to MySQL server successfully, but the database '" + dbName + "' might not exist.");
                System.out.println("You may need to create the database first using: CREATE DATABASE " + dbName);
                conn.close();
            } catch (SQLException e2) {
                System.err.println("Failed to connect to MySQL server:");
                e2.printStackTrace();
            }
        }
    }
}