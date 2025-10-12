package com.smartcodeanalyzer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Test class specifically for Railway MySQL connection
 */
public class RailwayMySQLConnectionTest {

    public static void main(String[] args) {
        // Using the exact Railway MySQL connection string provided
        String railwayUrl = "mysql://root:yeALRPBHedbAuAPUwCkWBQcYnIBwMJTZ@metro.proxy.rlwy.net:28742/railway";
        
        // Parse connection details from the Railway URL
        String host = "metro.proxy.rlwy.net";
        String port = "28742";
        String database = "railway";
        String username = "root";
        String password = "yeALRPBHedbAuAPUwCkWBQcYnIBwMJTZ";
        
        // Convert to JDBC URL format
        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database;
        
        System.out.println("Testing connection to Railway MySQL...");
        System.out.println("URL: " + jdbcUrl);
        System.out.println("Username: " + username);
        
        try {
            // Load the MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL JDBC Driver loaded successfully");
            
            // First try with minimal connection properties and shorter timeout
            System.out.println("\nAttempt 1: Minimal configuration with 10 second timeout");
            tryConnect(jdbcUrl, username, password, false, 10000);
            
            // If first attempt fails, try with SSL enabled
            System.out.println("\nAttempt 2: With SSL enabled and 10 second timeout");
            tryConnect(jdbcUrl, username, password, true, 10000);
            
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found:");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error during connection tests:");
            e.printStackTrace();
        }
        
        System.out.println("\nConnection test completed. Please check Railway dashboard to confirm:");
        System.out.println("1. The database is active/running");
        System.out.println("2. Your network allows connections to Railway's servers");
        System.out.println("3. The connection parameters are correct");
    }
    
    /**
     * Attempts to connect to the database with specified parameters
     */
    private static void tryConnect(String jdbcUrl, String username, String password, 
                                  boolean useSSL, int timeout) {
        java.util.Properties props = new java.util.Properties();
        props.setProperty("user", username);
        props.setProperty("password", password);
        props.setProperty("useSSL", String.valueOf(useSSL));
        props.setProperty("allowPublicKeyRetrieval", "true");
        props.setProperty("serverTimezone", "UTC");
        props.setProperty("connectTimeout", String.valueOf(timeout));
        
        System.out.println("- Connecting with useSSL=" + useSSL + ", timeout=" + timeout + "ms");
        
        try {
            Connection connection = DriverManager.getConnection(jdbcUrl, props);
            System.out.println("- Connection successful!");
            
            // Check if the analysis_reports table exists
            ResultSet tables = connection.getMetaData().getTables(null, null, "analysis_reports", null);
            if (tables.next()) {
                System.out.println("- Table 'analysis_reports' exists in the database!");
            } else {
                System.out.println("- Table 'analysis_reports' does NOT exist yet. Creating table...");
                
                // Create the table if it doesn't exist
                String createTableSQL = 
                    "CREATE TABLE analysis_reports (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "file_name VARCHAR(255) NOT NULL," +
                    "file_path VARCHAR(500) NOT NULL," +
                    "line_count INT," +
                    "class_count INT," +
                    "method_count INT," +
                    "variable_count INT," +
                    "analysis_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "ai_explanation TEXT," +
                    "ai_debug_suggestions TEXT," +
                    "ai_refactoring_suggestions TEXT" +
                    ")";
                    
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute(createTableSQL);
                    System.out.println("- Table created successfully!");
                }
            }
            
            connection.close();
            System.out.println("- Connection closed successfully.");
            
        } catch (SQLException e) {
            System.out.println("- Connection failed: " + e.getMessage());
            
            if (e.getMessage().contains("Communications link failure")) {
                System.out.println("  This is likely a network connectivity issue or the database is not accessible.");
            }
            
            // Don't print the full stack trace to keep output clean
            // e.printStackTrace();
        }
    }
}