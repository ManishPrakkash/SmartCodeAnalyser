package com.smartcodeanalyzer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analyzes Java source files for metrics such as line count, class count, method count, and variable count.
 */
public class FileAnalyzer {
    private String fileName;
    private String filePath;
    private String fileContent;
    private int totalLines;
    private int classCount;
    private int methodCount;
    private int variableCount;

    /**
     * Constructor initializes with file path
     * @param filePath Path to the Java file to analyze
     */
    public FileAnalyzer(String filePath) {
        this.filePath = filePath;
        File file = new File(filePath);
        this.fileName = file.getName();
    }

    /**
     * Reads the file content and performs analysis
     * @return boolean indicating success or failure
     */
    public boolean analyzeFile() {
        try {
            // Read the file
            StringBuilder contentBuilder = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                String line;
                totalLines = 0;
                while ((line = br.readLine()) != null) {
                    contentBuilder.append(line).append("\n");
                    totalLines++;
                }
            }

            this.fileContent = contentBuilder.toString();
            
            // Calculate metrics
            countClasses();
            countMethods();
            countVariables();
            
            return true;
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Counts the number of classes in the Java file
     */
    private void countClasses() {
        // Simple regex to match class or interface declarations
        Pattern pattern = Pattern.compile("(public|private|protected|static|\\s)*\\s(class|interface|enum)\\s+\\w+");
        Matcher matcher = pattern.matcher(fileContent);
        
        classCount = 0;
        while (matcher.find()) {
            classCount++;
        }
    }

    /**
     * Counts the number of methods in the Java file
     */
    private void countMethods() {
        // Regex to match method declarations, excluding constructors
        Pattern pattern = Pattern.compile(
            "(public|private|protected|static|final|native|synchronized|abstract|transient)+\\s+[\\w\\<\\>\\[\\]]+\\s+(\\w+)\\s*\\([^\\)]*\\)\\s*(\\{?|[^;])");
        Matcher matcher = pattern.matcher(fileContent);
        
        methodCount = 0;
        while (matcher.find()) {
            methodCount++;
        }
    }

    /**
     * Counts the number of variables in the Java file
     */
    private void countVariables() {
        // Regex to match variable declarations
        Pattern pattern = Pattern.compile(
            "(?<!\\.)\\b(?:byte|short|int|long|float|double|boolean|char|[A-Z][A-Za-z0-9_]*)\\b(?!\\s*\\()\\s+[a-zA-Z_][a-zA-Z0-9_]*(?:\\s*=\\s*[^,;]+)?(?:,\\s*[a-zA-Z_][a-zA-Z0-9_]*(?:\\s*=\\s*[^,;]+)?)*\\s*;");
        Matcher matcher = pattern.matcher(fileContent);
        
        variableCount = 0;
        while (matcher.find()) {
            String declaration = matcher.group(0);
            // Count commas to find multiple declarations in one line
            int commas = 0;
            for (char c : declaration.toCharArray()) {
                if (c == ',') commas++;
            }
            // Add one for the first variable, plus one for each comma
            variableCount += commas + 1;
        }
    }

    /**
     * Gets the original file content
     * @return String containing the file content
     */
    public String getFileContent() {
        return fileContent;
    }

    /**
     * Gets the file name
     * @return String containing the file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Gets the full file path
     * @return String containing the file path
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Gets the total line count
     * @return int representing the total number of lines
     */
    public int getTotalLines() {
        return totalLines;
    }

    /**
     * Gets the class count
     * @return int representing the number of classes found
     */
    public int getClassCount() {
        return classCount;
    }

    /**
     * Gets the method count
     * @return int representing the number of methods found
     */
    public int getMethodCount() {
        return methodCount;
    }

    /**
     * Gets the variable count
     * @return int representing the number of variables found
     */
    public int getVariableCount() {
        return variableCount;
    }

    /**
     * String representation of the analysis results
     * @return String containing summary of analysis
     */
    @Override
    public String toString() {
        return "File Analysis for: " + fileName + "\n" +
               "Total Lines: " + totalLines + "\n" +
               "Class Count: " + classCount + "\n" +
               "Method Count: " + methodCount + "\n" +
               "Variable Count: " + variableCount;
    }
}