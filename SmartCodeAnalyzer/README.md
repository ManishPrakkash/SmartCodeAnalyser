# Change to project directory
cd C:\Users\manis\OneDrive\Desktop\java\SmartCodeAnalyzer

# Build using Maven wrapper
cd C:\Users\manis\OneDrive\Desktop\java\SmartCodeAnalyzer; 
.\mvnw.cmd clean package -DskipTests



# RUN

java -jar target\SmartCodeAnalyzer-1.0-SNAPSHOT-jar-with-dependencies.jar

# Smart Code Analyzer

A Java application that analyzes Java source code files, extracts metrics, and leverages Google's Gemini AI API to provide explanations, debugging, and refactoring suggestions.

## Features

- **Code Structure Analysis:** Count lines, classes, methods, and variables in Java files
- **AI-Powered Analysis:** Uses Google's Gemini API to explain, debug, and suggest refactoring for code
- **Database Integration:** Stores analysis results and AI outputs in MySQL database (Railway)
- **User-Friendly Interface:** Menu-driven console interface for easy interaction

## System Requirements

- Java 11 or higher
- Maven for dependency management
- Google API key for accessing Gemini AI
- MySQL database (optional, can be set up later)

## Setup Instructions

### 1. Clone/Download the Repository

Download the project to your local machine.

### 2. Configure Dependencies

The project uses Maven for dependency management. All dependencies are defined in the `pom.xml` file:

- MySQL JDBC Driver for database connectivity
- Google Cloud AI Platform library for Gemini API integration
- Gson for JSON processing
- OkHttp for API requests

### 3. Build the Project

```bash
cd SmartCodeAnalyzer
mvn clean package
```

This will create a JAR file with all dependencies in the `target` directory.

### 4. Database Options

The application supports multiple database options:

1. **Railway MySQL** (recommended for shared access)
   - Railway provides a managed MySQL instance that can be accessed by multiple users
   - The connection string format is: `mysql://username:password@host:port/dbname`
   - Perfect for sharing the database with friends or team members

2. **H2 In-Memory Database**
   - Built-in option that requires no setup
   - Data is lost when the application exits
   - Perfect for testing or when you don't need to persist data

3. **Local MySQL Database**
   - MySQL running on your local machine (e.g., via XAMPP)
   - Only accessible from your computer

4. **Custom Database Connection**
   - Any other MySQL-compatible database you have access to

#### Using the Shared Railway MySQL Connection

To use the shared Railway MySQL database:

1. Launch the application
2. Select option 1 (Setup Database Connection)
3. Choose option 2 (Railway MySQL)
4. Paste this connection string when prompted:
   ```
   mysql://root:yeALRPBHedbAuAPUwCkWBQcYnIBwMJTZ@metro.proxy.rlwy.net:28742/railway
   ```

The application will automatically convert this to the proper JDBC format (`jdbc:mysql://...`) and extract the username, password, host, port, and database name.

> **Important Note**: If you're using option 4 (Custom database connection), the application will also accept and convert the Railway MySQL format.

### 5. Get Google Gemini API Key

1. Go to the Google AI Studio: https://makersuite.google.com/app/apikey
2. Create a new API key
3. Save the API key securely for use with this application

## Usage Guide

### Running the Application

Using the Maven wrapper (recommended):

```bash
# For Windows
.\mvnw.cmd compile
.\mvnw.cmd exec:java -Dexec.mainClass="com.smartcodeanalyzer.Main"

# For macOS/Linux
./mvnw compile
./mvnw exec:java -Dexec.mainClass="com.smartcodeanalyzer.Main"
```

Or using the JAR file:

```bash
java -jar target/SmartCodeAnalyzer-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### First-Time Configuration

1. **Configure Gemini API:** (Required)
   - Select option 2 from the main menu
   - Enter your Google API key for Gemini

2. **Setup Database Connection:** (Optional)
   - Select option 1 from the main menu
   - Choose from available database options:
     - Option 1: Local MySQL Workbench
     - Option 2: Railway MySQL (recommended for shared access)
     - Option 3: H2 In-Memory Database (no setup required)
     - Option 4: Custom database connection
   
   For Railway MySQL (shared access with your friend):
   - Select option 2
   - When prompted, paste this connection string:
     ```
     mysql://root:yeALRPBHedbAuAPUwCkWBQcYnIBwMJTZ@metro.proxy.rlwy.net:28742/railway
     ```
   
   Note: You can skip the database setup and still use the code analysis features.
   Analysis results won't be saved without a database connection.

### Analyzing Java Files

1. Select option 3 from the main menu
2. Enter the full path to a Java file
3. The application will analyze the file's structure
4. Choose from AI analysis options:
   - Explain Code: Get a detailed explanation of the code
   - Debug Code: Find potential issues and bugs
   - Refactor Code: Get suggestions for code improvements
5. The analysis results and AI output will be saved to the database

### Viewing Reports

- Select option 4 to view all analysis reports
- Select option 5 to view a specific report by ID (includes full AI output)

## Database Structure

The application uses a MySQL database with the following schema:

```sql
CREATE TABLE analysis_reports (
    id INT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    total_lines INT NOT NULL,
    class_count INT NOT NULL,
    method_count INT NOT NULL,
    variable_count INT NOT NULL,
    ai_action VARCHAR(50) NOT NULL,
    ai_output TEXT NOT NULL,
    date_analyzed DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

## Application Architecture

- **Main.java** - Menu-driven console interface that coordinates all components
- **FileAnalyzer.java** - Handles Java file reading and structure analysis
- **DatabaseManager.java** - Manages database connections and operations
- **GeminiAI.java** - Integrates with Google's Gemini API for code analysis

## Tips and Best Practices

- For best results with AI analysis, provide well-formatted Java code files
- Ensure your database connection is stable and properly configured
- Keep your Google API key secure and don't share it
- For large Java files, the analysis might take some time, especially the AI processing

## Troubleshooting

### Database Connection Issues

#### Railway MySQL Connection Issues:

If you encounter "Communications link failure" with Railway MySQL:

1. **Check Railway Service Status:**
   - Railway databases may become inactive after periods of inactivity
   - The database instance might have expired (free tier instances often expire after some time)
   - Ask the person who created the database to verify it's still active

2. **Network/Firewall Issues:**
   - Some networks (especially corporate or university networks) block outbound connections to database ports
   - Try connecting from a different network (e.g., mobile hotspot)
   
3. **Use H2 As Fallback:**
   - If you cannot connect to Railway MySQL, select option 3 (H2 In-Memory Database)
   - This works completely offline but data will be lost when you exit the application

#### Other Common Issues:

- **API Connectivity Problems:** Check your API key and internet connection
- **File Reading Errors:** Ensure the file path is correct and the file is accessible
- **Out of Memory Errors:** For very large files, increase JVM heap size with `-Xmx` parameter

## Sharing with Friends

This application is designed to support collaborative work:

1. **Share the Project Code:**
   - Send your friend this entire project folder
   - They need Java 11+ installed on their system
   - Maven wrapper is included, so they don't need to install Maven

2. **Share the Railway MySQL Connection:**
   - Railway MySQL allows multiple users to connect to the same database
   - Your friend can use the same connection string:
     ```
     mysql://root:yeALRPBHedbAuAPUwCkWBQcYnIBwMJTZ@metro.proxy.rlwy.net:28742/railway
     ```
   - This gives them access to all the analysis reports you've already created

3. **Each User Needs Their Own Gemini API Key:**
   - Everyone using the application needs their own Google API key
   - API keys have usage limits and should not be shared

4. **Collaborative Workflow:**
   - You can analyze different Java files
   - All analysis reports are stored in the shared database
   - Everyone can view and reference all reports

## Future Enhancements

- Support for analyzing multiple files or entire directories
- More detailed code metrics and complexity analysis
- Enhanced AI interaction with follow-up questions
- Export reports to various formats (PDF, HTML, etc.)
- Web-based user interface