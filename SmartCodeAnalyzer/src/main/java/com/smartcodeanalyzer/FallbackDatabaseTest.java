package com.smartcodeanalyzer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Fallback database connection test that attempts to use H2 in-memory database
 * if Railway MySQL is not available.
 */
public class FallbackDatabaseTest {

    public static void main(String[] args) {
        System.out.println("Smart Code Analyzer - Database Connectivity Test");
        System.out.println("==============================================");
        
        // First try Railway MySQL
        tryRailwayConnection();
        
        // If that fails, try H2 in-memory database as a fallback
        tryH2InMemoryConnection();
    }
    
    /**
     * Try to connect to Railway MySQL
     */
    private static void tryRailwayConnection() {
        System.out.println("\n1. Testing Railway MySQL Connection");
        System.out.println("--------------------------------");
        
        String host = "metro.proxy.rlwy.net";
        String port = "28742";
        String database = "railway";
        String username = "root";
        String password = "yeALRPBHedbAuAPUwCkWBQcYnIBwMJTZ";
        
        // Convert to JDBC URL format
        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database;
        
        System.out.println("URL: " + jdbcUrl);
        System.out.println("Username: " + username);
        
        try {
            // Load the MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Set connection properties with short timeout
            java.util.Properties props = new java.util.Properties();
            props.setProperty("user", username);
            props.setProperty("password", password);
            props.setProperty("useSSL", "false");
            props.setProperty("allowPublicKeyRetrieval", "true");
            props.setProperty("connectTimeout", "5000");
            
            System.out.println("Attempting connection (5 second timeout)...");
            Connection connection = DriverManager.getConnection(jdbcUrl, props);
            
            System.out.println("SUCCESS: Connected to Railway MySQL!");
            connection.close();
            System.out.println("Connection closed.");
            
        } catch (ClassNotFoundException e) {
            System.out.println("FAILED: MySQL JDBC Driver not found.");
        } catch (SQLException e) {
            System.out.println("FAILED: Could not connect to Railway MySQL.");
            System.out.println("Error: " + e.getMessage());
            System.out.println("\nPlease check the Railway dashboard to verify:");
            System.out.println("1. The MySQL database service is active and running");
            System.out.println("2. The connection credentials are correct");
            System.out.println("3. Your network allows connections to Railway's servers");
        }
    }
    
    /**
     * Try to connect to H2 in-memory database as a fallback
     */
    private static void tryH2InMemoryConnection() {
        System.out.println("\n2. Testing H2 In-Memory Database (Fallback)");
        System.out.println("----------------------------------------");
        
        // H2 database doesn't require an external server, so it's a good fallback
        String h2Url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
        String h2User = "sa";
        String h2Password = "";
        
        System.out.println("URL: " + h2Url);
        System.out.println("Username: " + h2User);
        
        try {
            // Try to dynamically load H2 driver if available
            Class.forName("org.h2.Driver");
            
            System.out.println("Attempting connection...");
            Connection connection = DriverManager.getConnection(h2Url, h2User, h2Password);
            
            System.out.println("SUCCESS: Connected to H2 in-memory database!");
            
            // Create a test table
            try (java.sql.Statement stmt = connection.createStatement()) {
                String createTableSQL = 
                    "CREATE TABLE test_table (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "test_column VARCHAR(255)" +
                    ")";
                stmt.execute(createTableSQL);
                System.out.println("Test table created successfully.");
                
                // Insert a test record
                stmt.execute("INSERT INTO test_table (test_column) VALUES ('Test successful')");
                System.out.println("Test record inserted.");
                
                // Query the test record
                try (java.sql.ResultSet rs = stmt.executeQuery("SELECT * FROM test_table")) {
                    if (rs.next()) {
                        System.out.println("Test query successful: " + rs.getString("test_column"));
                    }
                }
            }
            
            connection.close();
            System.out.println("Connection closed.");
            
            System.out.println("\nRECOMMENDATION:");
            System.out.println("Since Railway MySQL is not accessible, you can use H2 in-memory");
            System.out.println("database as a fallback for development and testing purposes.");
            System.out.println("Add this dependency to your pom.xml:");
            System.out.println("\n<dependency>");
            System.out.println("    <groupId>com.h2database</groupId>");
            System.out.println("    <artifactId>h2</artifactId>");
            System.out.println("    <version>2.2.224</version>");
            System.out.println("</dependency>");
            
        } catch (ClassNotFoundException e) {
            System.out.println("FAILED: H2 driver not found. Please add H2 dependency to your project.");
            System.out.println("Add this to your pom.xml:");
            System.out.println("\n<dependency>");
            System.out.println("    <groupId>com.h2database</groupId>");
            System.out.println("    <artifactId>h2</artifactId>");
            System.out.println("    <version>2.2.224</version>");
            System.out.println("</dependency>");
        } catch (SQLException e) {
            System.out.println("FAILED: Could not connect to H2 database.");
            System.out.println("Error: " + e.getMessage());
        }
    }
}