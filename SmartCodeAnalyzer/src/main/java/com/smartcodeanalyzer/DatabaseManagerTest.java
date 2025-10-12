package com.smartcodeanalyzer;

/**
 * Test class for the updated DatabaseManager with fallback to H2 database
 */
public class DatabaseManagerTest {

    public static void main(String[] args) {
        System.out.println("Smart Code Analyzer - DatabaseManager Test");
        System.out.println("=========================================");
        
        // First try Railway MySQL
        testDatabaseConnection(DatabaseManager.DatabaseType.RAILWAY_MYSQL);
        
        // Then try H2 in-memory as fallback
        testDatabaseConnection(DatabaseManager.DatabaseType.H2_IN_MEMORY);
        
        // Test saving and retrieving data with H2
        testDatabaseOperations();
    }
    
    /**
     * Tests database connection
     */
    private static void testDatabaseConnection(DatabaseManager.DatabaseType dbType) {
        System.out.println("\nTesting " + dbType + " connection...");
        
        // Create database manager with the specified type
        DatabaseManager dbManager = new DatabaseManager(dbType);
        
        // Try to connect
        boolean connected = dbManager.connect();
        
        if (connected) {
            try {
                // Test if table exists
                boolean tableExists = dbManager.testTableExists();
                System.out.println("Table exists: " + tableExists);
                
                // Disconnect
                dbManager.disconnect();
                System.out.println(dbType + " connection test SUCCESSFUL");
            } catch (Exception e) {
                System.err.println("Error testing table existence: " + e.getMessage());
                System.out.println(dbType + " connection test FAILED");
            }
        } else {
            System.out.println(dbType + " connection test FAILED");
        }
    }
    
    /**
     * Tests database operations using H2
     */
    private static void testDatabaseOperations() {
        System.out.println("\nTesting database operations with H2 in-memory database...");
        
        // Create database manager with H2
        DatabaseManager dbManager = new DatabaseManager(DatabaseManager.DatabaseType.H2_IN_MEMORY);
        
        // Try to connect
        boolean connected = dbManager.connect();
        
        if (connected) {
            try {
                // Save a test report
                System.out.println("\nSaving test report...");
                int reportId = dbManager.saveReport(
                    "TestFile.java",     // fileName
                    100,                 // totalLines
                    2,                   // classCount
                    5,                   // methodCount
                    10,                  // variableCount
                    "Explain",           // aiAction
                    "This is a test AI explanation" // aiOutput
                );
                
                if (reportId > 0) {
                    System.out.println("Report saved with ID: " + reportId);
                    
                    // Retrieve all reports
                    System.out.println("\nRetrieving all reports:");
                    for (String report : dbManager.getAllReports()) {
                        System.out.println(report);
                    }
                } else {
                    System.out.println("Failed to save report");
                }
                
                // Disconnect
                dbManager.disconnect();
                System.out.println("\nDatabase operations test completed");
            } catch (Exception e) {
                System.err.println("Error in database operations: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("H2 database connection FAILED - cannot test operations");
        }
    }
}