package com.smartcodeanalyzer;

import java.io.File;
import java.util.Scanner;
import java.util.List;

/**
 * Main class for the Smart Code Analyzer application
 * Provides a menu-driven console interface for analyzing Java code files
 */
public class Main {
    private static Scanner scanner = new Scanner(System.in);
    private static FileAnalyzer fileAnalyzer;
    private static DatabaseManager dbManager;
    private static GeminiAI geminiAI;
    private static boolean dbConnected = false;
    private static boolean aiConfigured = false;
    
    // Hardcoded credentials
    private static final String RAILWAY_CONNECTION = "mysql://root:yeALRPBHedbAuAPUwCkWBQcYnIBwMJTZ@metro.proxy.rlwy.net:28742/railway";
    private static final String GEMINI_API_KEY = "AIzaSyBEeDJ4YeOQsLT-cxTOvw7IIs4FzjCz-5g"; // Gemini API Key

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("   Welcome to Smart Code Analyzer v1.0   ");
        System.out.println("=========================================");
        
        // Auto-configure database and API
        autoConfigureDatabase();
        autoConfigureGeminiAPI();
        
        boolean exit = false;
        while (!exit) {
            printMainMenu();
            
            int choice = getChoice();
            
            switch (choice) {
                case 1:
                    analyzeJavaFile();
                    break;
                case 2:
                    if (dbConnected) {
                        viewReports();
                    } else {
                        System.out.println(" Database connection required for this feature.");
                    }
                    break;
                case 3:
                    if (dbConnected) {
                        viewDetailedReport();
                    } else {
                        System.out.println(" Database connection required for this feature.");
                    }
                    break;
                case 4:
                    about();
                    break;
                case 0:
                    exit = true;
                    cleanupResources();
                    System.out.println("Exiting Smart Code Analyzer. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    /**
     * Auto-configures the database connection using the hardcoded Railway MySQL connection string
     */
    private static void autoConfigureDatabase() {
        System.out.println("Setting up database...");
        
        try {
            // Parse the Railway connection string
            String connectionString = RAILWAY_CONNECTION;
            
            if (connectionString.startsWith("mysql://")) {
                // Extract credentials and host info from the connection string
                String withoutProtocol = connectionString.substring(8); // Remove "mysql://"
                
                // Split username:password from the rest
                String[] credentialsAndHostPart = withoutProtocol.split("@", 2);
                String[] credentials = credentialsAndHostPart[0].split(":", 2);
                String username = credentials[0];
                String password = credentials[1];
                
                // Split host:port/dbname
                String[] hostPortAndDb = credentialsAndHostPart[1].split("/", 2);
                String[] hostAndPort = hostPortAndDb[0].split(":", 2);
                String host = hostAndPort[0];
                String port = hostAndPort[1];
                String dbName = hostPortAndDb[1];
                
                // Construct JDBC URL
                String jdbcUrl = String.format("jdbc:mysql://%s:%s/%s", host, port, dbName);
                
                // Initialize database manager
                dbManager = new DatabaseManager(jdbcUrl, username, password);
                dbConnected = dbManager.connect();
                
                if (dbConnected) {
                    System.out.println("✅ Using Railway MySQL database");
                } else {
                    System.out.println("⚠️ Using H2 local database (Railway MySQL not available)");
                    dbManager = new DatabaseManager(DatabaseManager.DatabaseType.H2_IN_MEMORY);
                    dbConnected = dbManager.connect();
                    
                    if (!dbConnected) {
                        System.out.println("❌ Database connection failed. Features will be limited.");
                    }
                }
            } else {
                System.out.println("⚠️ Using H2 local database");
                dbManager = new DatabaseManager(DatabaseManager.DatabaseType.H2_IN_MEMORY);
                dbConnected = dbManager.connect();
                
                if (dbConnected) {
                    System.out.println(" Connected to H2 in-memory database as fallback!");
                } else {
                    System.out.println(" Failed to initialize H2 database. Database features will be unavailable.");
                }
            }
        } catch (Exception e) {
            System.out.println(" Error during database auto-configuration: " + e.getMessage());
            System.out.println("Falling back to H2 in-memory database...");
            
            dbManager = new DatabaseManager(DatabaseManager.DatabaseType.H2_IN_MEMORY);
            dbConnected = dbManager.connect();
            
            if (dbConnected) {
                System.out.println(" Connected to H2 in-memory database as fallback!");
            } else {
                System.out.println(" Failed to initialize H2 database. Database features will be unavailable.");
            }
        }
    }
    
    /**
     * Auto-configures the Gemini AI API using the hardcoded API key
     */
    private static void autoConfigureGeminiAPI() {
        System.out.println("Setting up AI service...");
        
        try {
            // Prefer environment variable, fall back to constant
            String apiKey = System.getenv("GEMINI_API_KEY");
            if (apiKey == null || apiKey.isBlank()) {
                apiKey = GEMINI_API_KEY;
            }
            
            if (apiKey == null || apiKey.isBlank() || apiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
                System.out.println("⚠️ AI service not configured (API key missing)");
                aiConfigured = false;
                return;
            }
            
            // Initialize the Gemini AI service
            geminiAI = new GeminiAI(apiKey);
            aiConfigured = true;
            System.out.println("✅ AI service ready");
            
            // Don't run a test call at startup - it will be tested when used
        } catch (Exception e) {
            System.out.println("❌ AI service initialization failed");
            System.err.println("AI Error: " + e.getMessage());
            aiConfigured = false;
        }
    }

    private static void printMainMenu() {
        System.out.println("\nMAIN MENU");
        System.out.println("1. Analyze Java File");
        System.out.println("2. View Analysis Reports" + (!dbConnected ? " (Unavailable - No Database Connection)" : ""));
        System.out.println("3. View Detailed Report" + (!dbConnected ? " (Unavailable - No Database Connection)" : ""));
        System.out.println("4. About");
        System.out.println("0. Exit");
        System.out.print("Enter your choice: ");
    }

    /**
     * Gets integer input from the user
     * @return int representing the user's choice
     */
    private static int getChoice() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1; // Invalid choice
        }
    }

    /**
     * Analyzes a Java file
     */
    private static void analyzeJavaFile() {
        System.out.println("\n==== Java File Analysis ====");
        
        // File selection
        System.out.print("Enter path to Java file: ");
        String filePath = scanner.nextLine().trim();
        File file = new File(filePath);
        
        if (!file.exists() || !file.isFile() || !filePath.endsWith(".java")) {
            System.out.println(" Invalid file path or not a Java file.");
            return;
        }
        
        System.out.println("Analyzing file: " + file.getName());
        
        try {
            // Create a new FileAnalyzer for this file
            fileAnalyzer = new FileAnalyzer(filePath);
            
            // Analyze the file
            boolean success = fileAnalyzer.analyzeFile();
            
            if (!success) {
                System.out.println(" Error analyzing file.");
                return;
            }
            
            // Get counts from analyzer
            int lineCount = fileAnalyzer.getTotalLines();
            int classCount = fileAnalyzer.getClassCount();
            int methodCount = fileAnalyzer.getMethodCount();
            int variableCount = fileAnalyzer.getVariableCount();
            
            // Display results
            System.out.println("\n==== Analysis Results ====");
            System.out.println("Total lines: " + lineCount);
            System.out.println("Class count: " + classCount);
            System.out.println("Method count: " + methodCount);
            System.out.println("Variable count: " + variableCount);
            
            // Enhanced analysis with Gemini AI
            String aiAnalysis = "";
            if (aiConfigured && geminiAI != null) {
                System.out.println("\nGenerating AI insights...");
                aiAnalysis = geminiAI.explainCode(fileAnalyzer.getFileContent());
                System.out.println("\n==== AI Insights ====");
                System.out.println(aiAnalysis);
            } else {
                System.out.println("\nAI insights unavailable. Gemini API not configured.");
            }
            
            // Save to database if connected
            if (dbConnected && dbManager != null) {
                saveAnalysisToDatabase(fileAnalyzer, aiAnalysis);
                System.out.println("\n Analysis results saved to database.");
            } else {
                System.out.println("\n Analysis results not saved (no database connection).");
            }
            
        } catch (Exception e) {
            System.out.println(" Error analyzing file: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }
    
    /**
     * Saves the analysis to the database
     */
    private static void saveAnalysisToDatabase(FileAnalyzer analyzer, String aiInsights) {
        if (dbManager != null && dbConnected && analyzer != null) {
            try {
                String filename = analyzer.getFileName();
                String filePath = analyzer.getFilePath();
                int totalLines = analyzer.getTotalLines();
                int classCount = analyzer.getClassCount();
                int methodCount = analyzer.getMethodCount();
                int variableCount = analyzer.getVariableCount();
                
                // Use the correct method name and parameters from DatabaseManager
                dbManager.saveReport(
                    filename,
                    filePath,
                    totalLines,
                    classCount,
                    methodCount,
                    variableCount,
                    "Explain", // Using "Explain" as the AI action
                    aiInsights
                );
            } catch (Exception e) {
                System.out.println("Error saving to database: " + e.getMessage());
            }
        }
    }
    
    /**
     * Views analysis reports from the database
     */
    private static void viewReports() {
        if (!dbConnected || dbManager == null) {
            System.out.println(" Database not connected. Cannot view reports.");
            return;
        }
        
        try {
            System.out.println("\n==== Analysis Reports ====");
            List<String> reports = dbManager.getAllReports();
            
            if (reports.isEmpty()) {
                System.out.println("No analysis reports found in the database.");
                return;
            }
            
            System.out.println("Found " + reports.size() + " reports:\n");
            
            for (String report : reports) {
                System.out.println(report);
                System.out.println("-".repeat(50));
            }
            
        } catch (Exception e) {
            System.out.println(" Error retrieving reports: " + e.getMessage());
        }
        
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }
    
    /**
     * Views detailed report for a specific analysis
     */
    private static void viewDetailedReport() {
        if (!dbConnected || dbManager == null) {
            System.out.println(" Database not connected. Cannot view detailed report.");
            return;
        }
        
        try {
            // First show all reports
            List<String> reports = dbManager.getAllReports();
            
            if (reports.isEmpty()) {
                System.out.println("No analysis reports found in the database.");
                return;
            }
            
            System.out.println("\n==== Available Reports ====");
            
            // Display the reports - each report string already contains the ID and other details
            for (String reportSummary : reports) {
                // Just display the first line of each report which contains the ID
                String[] lines = reportSummary.split("\n");
                if (lines.length > 0) {
                    System.out.println(lines[0]);
                }
            }
            System.out.println("-".repeat(50));
            
            // Ask for report ID
            System.out.print("\nEnter report ID to view details (0 to cancel): ");
            int reportId = getChoice();
            
            if (reportId <= 0) {
                return;
            }
            
            // Get detailed report
            String report = dbManager.getReportById(reportId);
            
            if (report == null || report.isEmpty()) {
                System.out.println(" Report with ID " + reportId + " not found.");
                return;
            }
            
            // Display detailed report - it's already formatted by DatabaseManager.getReportById()
            System.out.println("\n==== Detailed Analysis Report ====");
            System.out.println(report);
            
        } catch (Exception e) {
            System.out.println(" Error retrieving detailed report: " + e.getMessage());
        }
        
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }
    
    /**
     * Displays information about the application
     */
    private static void about() {
        System.out.println("\n==== About Smart Code Analyzer ====");
        System.out.println("Smart Code Analyzer v1.0");
        System.out.println("A Java application for analyzing Java source code files");
        System.out.println("\nFeatures:");
        System.out.println("- Basic code metrics (lines of code, comments, methods, classes)");
        System.out.println("- AI-powered code analysis using Google Gemini API");
        System.out.println("- Database storage of analysis results");
        System.out.println("- Report generation and viewing");
        
        System.out.println("\nDatabase Status: " + (dbConnected ? "Connected " : "Not Connected "));
        System.out.println("AI Status: " + (aiConfigured ? "Configured " : "Not Configured "));
        
        if (dbManager != null && dbConnected) {
            // Display basic database connection information
            System.out.println("\nDatabase Information:");
            System.out.println("Type: " + (dbManager.getClass().getName()));
            System.out.println("Status: Connected");
        }
        
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }
    
    /**
     * Cleans up resources before exiting
     */
    private static void cleanupResources() {
        if (dbManager != null) {
            dbManager.disconnect();
            System.out.println("Database disconnected.");
        }
        
        if (scanner != null) {
            scanner.close();
        }
    }
}
