package com.smartcodeanalyzer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages database operations for storing and retrieving code analysis reports
 */
public class DatabaseManager {
    // Database types
    public enum DatabaseType {
        RAILWAY_MYSQL,
        H2_IN_MEMORY
    }
    
    // Default Railway MySQL connection parameters
    // Connection string: mysql://root:yeALRPBHedbAuAPUwCkWBQcYnIBwMJTZ@metro.proxy.rlwy.net:28742/railway
    private static final String RAILWAY_URL = "jdbc:mysql://metro.proxy.rlwy.net:28742/";
    private static final String RAILWAY_SCHEMA = "railway";
    private static final String RAILWAY_USERNAME = "root";
    private static final String RAILWAY_PASSWORD = "yeALRPBHedbAuAPUwCkWBQcYnIBwMJTZ";
    
    // Default H2 in-memory connection parameters
    private static final String H2_URL = "jdbc:h2:mem:smartcodedb;DB_CLOSE_DELAY=-1";
    private static final String H2_USERNAME = "sa";
    private static final String H2_PASSWORD = "";
    
    private String jdbcUrl;
    private String username;
    private String password;
    private Connection connection;
    private DatabaseType dbType;

    /**
     * Constructor that lets you choose between Railway MySQL and H2 in-memory database
     * @param dbType The type of database to use (RAILWAY_MYSQL or H2_IN_MEMORY)
     */
    public DatabaseManager(DatabaseType dbType) {
        this.dbType = dbType;
        
        if (dbType == DatabaseType.RAILWAY_MYSQL) {
            this.jdbcUrl = RAILWAY_URL + RAILWAY_SCHEMA;
            this.username = RAILWAY_USERNAME;
            this.password = RAILWAY_PASSWORD;
        } else {
            // H2 in-memory database
            this.jdbcUrl = H2_URL;
            this.username = H2_USERNAME;
            this.password = H2_PASSWORD;
        }
    }

    /**
     * Constructor initializes database connection parameters
     * @param jdbcUrl JDBC URL for database connection
     * @param username Database username
     * @param password Database password
     * @param dbType The type of database (RAILWAY_MYSQL or H2_IN_MEMORY)
     */
    public DatabaseManager(String jdbcUrl, String username, String password, DatabaseType dbType) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.dbType = dbType;
    }
    
    /**
     * Constructor initializes database connection parameters (defaults to Railway MySQL)
     * @param jdbcUrl JDBC URL for MySQL connection
     * @param username Database username
     * @param password Database password
     */
    public DatabaseManager(String jdbcUrl, String username, String password) {
        this(jdbcUrl, username, password, DatabaseType.RAILWAY_MYSQL);
    }

    /**
     * Connects to the database
     * @return boolean indicating success or failure
     */
    public boolean connect() {
        try {
            if (dbType == DatabaseType.RAILWAY_MYSQL) {
                return connectToMySql();
            } else {
                return connectToH2();
            }
        } catch (Exception e) {
            System.err.println("Unexpected error connecting to database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Connects to Railway MySQL database
     * @return boolean indicating success or failure
     */
    private boolean connectToMySql() {
        try {
            // Load the MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Set connection properties with shorter timeout for testing
            java.util.Properties props = new java.util.Properties();
            props.setProperty("user", username);
            props.setProperty("password", password);
            props.setProperty("useSSL", "false");  // Try without SSL first
            props.setProperty("allowPublicKeyRetrieval", "true");
            props.setProperty("serverTimezone", "UTC");
            props.setProperty("connectTimeout", "10000");  // Shorter timeout for quicker feedback
            
            // Try to establish the connection
            connection = DriverManager.getConnection(jdbcUrl, props);
            
            // Ensure the required table exists
            createTableIfNotExists();
            
            return true;
        } catch (ClassNotFoundException e) {
            // Silent failure, will be handled by the calling method
            return false;
        } catch (SQLException e) {
            // Silent failure, will be handled by the calling method
            return false;
        }
    }
    
    /**
     * Connects to H2 in-memory database
     * @return boolean indicating success or failure
     */
    private boolean connectToH2() {
        try {
            // Load the H2 JDBC driver
            Class.forName("org.h2.Driver");
            
            // Connect to the H2 in-memory database
            connection = DriverManager.getConnection(jdbcUrl, username, password);
            
            // Ensure the required table exists
            createTableIfNotExists();
            
            return true;
        } catch (ClassNotFoundException e) {
            // Silent failure, will be handled by the calling method
            return false;
        } catch (SQLException e) {
            // Silent failure, will be handled by the calling method
            return false;
        }
    }

    /**
     * Creates the analysis_reports table if it doesn't exist
     */
    private void createTableIfNotExists() throws SQLException {
        String createTableSQL = 
            "CREATE TABLE IF NOT EXISTS analysis_reports (" +
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
        }
    }

    /**
     * Closes the database connection
     */
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }
    
    /**
     * Test if the analysis_reports table exists - useful for connection verification
     * @return true if the table exists
     * @throws SQLException if a database error occurs
     */
    public boolean testTableExists() throws SQLException {
        ResultSet tables = null;
        try {
            // Check if connection is valid
            if (connection == null || connection.isClosed()) {
                System.err.println("Cannot check table - no active connection");
                return false;
            }
            
            // Get metadata about the database
            tables = connection.getMetaData().getTables(
                null, null, "analysis_reports", null);
            
            // If the result set has any rows, the table exists
            boolean exists = tables.next();
            
            if (exists) {
                System.out.println("Table 'analysis_reports' exists in the database");
            } else {
                System.out.println("Table 'analysis_reports' does not exist yet");
            }
            
            return exists;
        } finally {
            if (tables != null) {
                tables.close();
            }
        }
    }

    /**
     * Saves an analysis report to the database
     * @param fileName The name of the analyzed file
     * @param totalLines Total number of lines in the file
     * @param classCount Number of classes found
     * @param methodCount Number of methods found
     * @param variableCount Number of variables found
     * @param aiAction The AI action performed (Explain, Debug, Refactor)
     * @param aiOutput The output from the AI
     * @return int representing the generated report ID, or -1 if failed
     */
    public int saveReport(String fileName, int totalLines, int classCount, int methodCount, 
                         int variableCount, String aiAction, String aiOutput) {
        String filePath = ""; // We will update the method signature in the next version
        String insertSQL;
        
        // Determine which column to populate based on the AI action
        if (aiAction.equals("Explain")) {
            insertSQL = 
                "INSERT INTO analysis_reports (file_name, file_path, line_count, class_count, method_count, " +
                "variable_count, ai_explanation) VALUES (?, ?, ?, ?, ?, ?, ?)";
        } else if (aiAction.equals("Debug")) {
            insertSQL = 
                "INSERT INTO analysis_reports (file_name, file_path, line_count, class_count, method_count, " +
                "variable_count, ai_debug_suggestions) VALUES (?, ?, ?, ?, ?, ?, ?)";
        } else { // Refactor
            insertSQL = 
                "INSERT INTO analysis_reports (file_name, file_path, line_count, class_count, method_count, " +
                "variable_count, ai_refactoring_suggestions) VALUES (?, ?, ?, ?, ?, ?, ?)";
        }
            
        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, fileName);
            pstmt.setString(2, filePath); // Empty for now, will update in next version
            pstmt.setInt(3, totalLines);
            pstmt.setInt(4, classCount);
            pstmt.setInt(5, methodCount);
            pstmt.setInt(6, variableCount);
            pstmt.setString(7, aiOutput);
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saving report: " + e.getMessage());
        }
        
        return -1;
    }

    /**
     * Overloaded saveReport to include file path in storage
     */
    public int saveReport(String fileName, String filePath, int totalLines, int classCount, int methodCount,
                          int variableCount, String aiAction, String aiOutput) {
        String insertSQL;
        if (aiAction.equals("Explain")) {
            insertSQL =
                "INSERT INTO analysis_reports (file_name, file_path, line_count, class_count, method_count, " +
                "variable_count, ai_explanation) VALUES (?, ?, ?, ?, ?, ?, ?)";
        } else if (aiAction.equals("Debug")) {
            insertSQL =
                "INSERT INTO analysis_reports (file_name, file_path, line_count, class_count, method_count, " +
                "variable_count, ai_debug_suggestions) VALUES (?, ?, ?, ?, ?, ?, ?)";
        } else { // Refactor
            insertSQL =
                "INSERT INTO analysis_reports (file_name, file_path, line_count, class_count, method_count, " +
                "variable_count, ai_refactoring_suggestions) VALUES (?, ?, ?, ?, ?, ?, ?)";
        }

        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, fileName);
            pstmt.setString(2, filePath);
            pstmt.setInt(3, totalLines);
            pstmt.setInt(4, classCount);
            pstmt.setInt(5, methodCount);
            pstmt.setInt(6, variableCount);
            pstmt.setString(7, aiOutput);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saving report: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Retrieves a list of all reports
     * @return List of analysis reports as strings
     */
    public List<String> getAllReports() {
        List<String> reports = new ArrayList<>();
        String selectSQL = "SELECT * FROM analysis_reports ORDER BY analysis_date DESC";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(selectSQL)) {
            
            while (rs.next()) {
                StringBuilder report = new StringBuilder();
                report.append("Report ID: ").append(rs.getInt("id")).append("\n");
                report.append("File: ").append(rs.getString("file_name")).append("\n");
                report.append("Lines: ").append(rs.getInt("line_count")).append("\n");
                report.append("Classes: ").append(rs.getInt("class_count")).append("\n");
                report.append("Methods: ").append(rs.getInt("method_count")).append("\n");
                report.append("Variables: ").append(rs.getInt("variable_count")).append("\n");
                
                // Determine which AI action was performed
                String aiAction = "Unknown";
                if (rs.getString("ai_explanation") != null) {
                    aiAction = "Explain";
                } else if (rs.getString("ai_debug_suggestions") != null) {
                    aiAction = "Debug";
                } else if (rs.getString("ai_refactoring_suggestions") != null) {
                    aiAction = "Refactor";
                }
                
                report.append("AI Action: ").append(aiAction).append("\n");
                report.append("Date: ").append(rs.getTimestamp("analysis_date")).append("\n");
                report.append("---------------------\n");
                
                reports.add(report.toString());
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving reports: " + e.getMessage());
        }
        
        return reports;
    }

    /**
     * Gets a specific report by ID
     * @param reportId The ID of the report to retrieve
     * @return String containing the full report details including AI output
     */
    public String getReportById(int reportId) {
        String selectSQL = "SELECT * FROM analysis_reports WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
            pstmt.setInt(1, reportId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    StringBuilder report = new StringBuilder();
                    report.append("Report ID: ").append(rs.getInt("id")).append("\n\n");
                    report.append("File: ").append(rs.getString("file_name")).append("\n");
                    report.append("Lines: ").append(rs.getInt("line_count")).append("\n");
                    report.append("Classes: ").append(rs.getInt("class_count")).append("\n");
                    report.append("Methods: ").append(rs.getInt("method_count")).append("\n");
                    report.append("Variables: ").append(rs.getInt("variable_count")).append("\n");
                    
                    // Determine which AI action was performed and get the output
                    String aiAction = "Unknown";
                    String aiOutput = "";
                    
                    if (rs.getString("ai_explanation") != null) {
                        aiAction = "Explain";
                        aiOutput = rs.getString("ai_explanation");
                    } else if (rs.getString("ai_debug_suggestions") != null) {
                        aiAction = "Debug";
                        aiOutput = rs.getString("ai_debug_suggestions");
                    } else if (rs.getString("ai_refactoring_suggestions") != null) {
                        aiAction = "Refactor";
                        aiOutput = rs.getString("ai_refactoring_suggestions");
                    }
                    
                    report.append("AI Action: ").append(aiAction).append("\n");
                    report.append("Date: ").append(rs.getTimestamp("analysis_date")).append("\n\n");
                    report.append("AI OUTPUT:\n").append(aiOutput).append("\n");
                    
                    return report.toString();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving report: " + e.getMessage());
        }
        
        return "Report not found.";
    }
}