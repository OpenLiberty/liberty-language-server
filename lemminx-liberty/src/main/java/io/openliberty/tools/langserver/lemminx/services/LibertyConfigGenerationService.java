/*******************************************************************************
* Copyright (c) 2026 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     IBM Corporation - initial API and implementation
*******************************************************************************/
package io.openliberty.tools.langserver.lemminx.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service to manage automatic generation of liberty-plugin-config.xml
 * when Liberty configuration files are opened.
 */
public class LibertyConfigGenerationService {
    
    private static final Logger LOGGER = Logger.getLogger(LibertyConfigGenerationService.class.getName());
    
    private static final LibertyConfigGenerationService INSTANCE = new LibertyConfigGenerationService();
    
    private static final String CONFIG_FILE_NAME = "liberty-plugin-config.xml";
    private static final String MAVEN_TARGET_DIR = "target";
    private static final String GRADLE_BUILD_DIR = "build";
    private static final String TMP_DIR = "tmp";
    private static final String BUILD_LOG_FILE = "build.log";
    private static final int COMMAND_TIMEOUT_SECONDS = 30;
    
    // Cache to track which projects have been processed
    private final Set<String> processedProjects = new HashSet<>();
    
    // Track projects where config generation failed (for showing warning diagnostic)
    // Maps project path to FailureInfo containing error message and log file path
    private final Map<String, FailureInfo> failedProjects = new HashMap<>();
    
    public static LibertyConfigGenerationService getInstance() {
        return INSTANCE;
    }
    
    private LibertyConfigGenerationService() {
    }
    
    /**
     * Check if liberty-plugin-config.xml needs to be generated.
     *
     * @param workspace The Liberty workspace
     * @return true if config generation is needed, false otherwise
     */
    public boolean needsConfigGeneration(LibertyWorkspace workspace) {
        String projectPath = workspace.getWorkspaceURI().getPath();
        Path configPath = getConfigPath(projectPath);
        
        // Check 1: Config file doesn't exist
        if (!Files.exists(configPath)) {
            LOGGER.info("Config file does not exist: " + configPath);
            processedProjects.remove(projectPath);
            return true;
        }
        
        // Check 2: Mock server directory missing (after mvn clean)
        if (isMockServerInConfig(configPath) && !mockServerDirectoryExists(projectPath)) {
            LOGGER.info("Mock server directory missing, regeneration needed");
            processedProjects.remove(projectPath);
            return true;
        }
        
        // Check 3: Already processed in this session
        if (processedProjects.contains(projectPath)) {
            LOGGER.fine("Project already processed: " + projectPath);
            return false;
        }
        
        // Check 4: Config is stale (older than build file)
        try {
            long configTime = Files.getLastModifiedTime(configPath).toMillis();
            Long buildFileTime = getBuildFileModificationTime(projectPath);
            
            if (buildFileTime != null && configTime < buildFileTime) {
                LOGGER.info("Config file is stale, regeneration needed");
                return true;
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error checking file modification times", e);
            return true; // Regenerate on error to be safe
        }
        
        return false;
    }
    
    /**
     * Check if the config file points to a mock server directory.
     */
    private boolean isMockServerInConfig(Path configPath) {
        try {
            String content = new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8);
            return content.contains(TMP_DIR);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error reading config file", e);
            return false;
        }
    }
    
    /**
     * Check if the mock server directory exists.
     */
    private boolean mockServerDirectoryExists(String projectPath) {
        Path mockServerPath = Paths.get(projectPath, MAVEN_TARGET_DIR, TMP_DIR);
        if (Files.exists(mockServerPath)) {
            return true;
        }
        
        mockServerPath = Paths.get(projectPath, GRADLE_BUILD_DIR, TMP_DIR);
        return Files.exists(mockServerPath);
    }
    
    /**
     * Generate liberty-plugin-config.xml asynchronously.
     * 
     * @param workspace The Liberty workspace
     * @return CompletableFuture with the generation result
     */
    public CompletableFuture<ConfigGenerationResult> generateConfigAsync(LibertyWorkspace workspace) {
        return CompletableFuture.supplyAsync(() -> generateConfig(workspace));
    }
    
    /**
     * Generate liberty-plugin-config.xml synchronously.
     * 
     * @param workspace The Liberty workspace
     * @return The generation result
     */
    public ConfigGenerationResult generateConfig(LibertyWorkspace workspace) {
        String projectPath = workspace.getWorkspaceURI().getPath();
        long startTime = System.currentTimeMillis();
        
        try {
            LOGGER.info("Starting config generation for: " + projectPath);
            
            // Check if Liberty plugin is configured
            if (!hasLibertyPlugin(workspace)) {
                String errorMsg = "Liberty Maven or Gradle plugin not found in build configuration";
                return ConfigGenerationResult.failure(
                    errorMsg,
                    null,
                    System.currentTimeMillis() - startTime
                );
            }
            
            // Determine build tool (Maven or Gradle)
            BuildTool buildTool = detectBuildTool(projectPath);
            
            if (buildTool == BuildTool.NONE) {
                return ConfigGenerationResult.failure(
                    "No Maven or Gradle build file found",
                    null,
                    System.currentTimeMillis() - startTime
                );
            }
            
            // Execute prepare-config goal
            String command = buildTool == BuildTool.MAVEN
                ? "mvn liberty:prepare-config"
                : "./gradlew libertyPrepareConfig";
            
            ProcessResult processResult = executeCommand(command, projectPath);
            
            if (!processResult.success) {
                // Save error log to file
                String logFilePath = saveErrorLog(projectPath, processResult.error);
                return ConfigGenerationResult.failure(
                    processResult.error,
                    logFilePath,
                    System.currentTimeMillis() - startTime
                );
            }
            
            // Verify config file was created
            Path configPath = getConfigPath(projectPath);
            
            if (!Files.exists(configPath)) {
                String errorMsg = "Config file was not generated";
                String logFilePath = saveErrorLog(projectPath, errorMsg);
                return ConfigGenerationResult.failure(
                    errorMsg,
                    logFilePath,
                    System.currentTimeMillis() - startTime
                );
            }
            
            // Mark project as processed and clear any previous failure
            processedProjects.add(projectPath);
            clearProjectFailure(workspace);
            
            long duration = System.currentTimeMillis() - startTime;
            
            LOGGER.info(String.format("Config generated successfully in %dms: %s",
                duration, configPath));
            
            return ConfigGenerationResult.success(
                configPath.toString(),
                duration
            );
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            LOGGER.log(Level.SEVERE, "Error generating Liberty configuration", e);
            
            String logFilePath = saveErrorLog(projectPath, e.getMessage());
            return ConfigGenerationResult.failure(
                e.getMessage(),
                logFilePath,
                duration
            );
        }
    }
    
    /**
     * Get the path to liberty-plugin-config.xml.
     */
    private Path getConfigPath(String projectPath) {
        // Try Maven target directory first
        Path configPath = Paths.get(projectPath, MAVEN_TARGET_DIR, CONFIG_FILE_NAME);
        if (Files.exists(configPath)) {
            return configPath;
        }
        
        // Try Gradle build directory
        configPath = Paths.get(projectPath, GRADLE_BUILD_DIR, CONFIG_FILE_NAME);
        return configPath;
    }
    
    /**
     * Detect build tool (Maven or Gradle).
     */
    private BuildTool detectBuildTool(String projectPath) {
        if (Files.exists(Paths.get(projectPath, "pom.xml"))) {
            return BuildTool.MAVEN;
        }
        
        if (Files.exists(Paths.get(projectPath, "build.gradle")) ||
            Files.exists(Paths.get(projectPath, "build.gradle.kts"))) {
            return BuildTool.GRADLE;
        }
        
        return BuildTool.NONE;
    }
    
    /**
     * Get modification time of build file (pom.xml or build.gradle).
     */
    private Long getBuildFileModificationTime(String projectPath) {
        try {
            Path pomPath = Paths.get(projectPath, "pom.xml");
            if (Files.exists(pomPath)) {
                return Files.getLastModifiedTime(pomPath).toMillis();
            }
            
            Path gradlePath = Paths.get(projectPath, "build.gradle");
            if (Files.exists(gradlePath)) {
                return Files.getLastModifiedTime(gradlePath).toMillis();
            }
            
            Path gradleKtsPath = Paths.get(projectPath, "build.gradle.kts");
            if (Files.exists(gradleKtsPath)) {
                return Files.getLastModifiedTime(gradleKtsPath).toMillis();
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error getting build file modification time", e);
        }
        
        return null;
    }
    
    /**
     * Check if the project has Liberty Maven or Gradle plugin configured.
     * This prevents attempting config generation on projects without Liberty tooling.
     */
    public boolean hasLibertyPlugin(LibertyWorkspace workspace) {
        if (workspace == null) {
            return false;
        }
        
        String projectPath = workspace.getDir().getAbsolutePath();
        BuildTool buildTool = detectBuildTool(projectPath);
        
        if (buildTool == BuildTool.NONE) {
            return false;
        }
        
        try {
            if (buildTool == BuildTool.MAVEN) {
                // Check if pom.xml contains liberty-maven-plugin
                Path pomPath = Paths.get(projectPath, "pom.xml");
                if (Files.exists(pomPath)) {
                    String pomContent = new String(Files.readAllBytes(pomPath));
                    return pomContent.contains("liberty-maven-plugin") ||
                           pomContent.contains("io.openliberty.tools");
                }
            } else if (buildTool == BuildTool.GRADLE) {
                // Check if build.gradle contains liberty-gradle-plugin
                Path gradlePath = Paths.get(projectPath, "build.gradle");
                if (Files.exists(gradlePath)) {
                    String gradleContent = new String(Files.readAllBytes(gradlePath));
                    if (gradleContent.contains("liberty") ||
                        gradleContent.contains("io.openliberty.tools")) {
                        return true;
                    }
                }
                
                // Check build.gradle.kts
                Path gradleKtsPath = Paths.get(projectPath, "build.gradle.kts");
                if (Files.exists(gradleKtsPath)) {
                    String gradleKtsContent = new String(Files.readAllBytes(gradleKtsPath));
                    return gradleKtsContent.contains("liberty") ||
                           gradleKtsContent.contains("io.openliberty.tools");
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error checking for Liberty plugin in build file", e);
        }
        
        return false;
    }
    
    /**
     * Execute a command in the specified directory.
     */
    private ProcessResult executeCommand(String command, String workingDirectory) {
        try {
            LOGGER.info("Executing command: " + command + " in " + workingDirectory);
            
            ProcessBuilder processBuilder = new ProcessBuilder();
            
            // Split command into parts
            String[] commandParts = command.split("\\s+");
            processBuilder.command(commandParts);
            processBuilder.directory(new File(workingDirectory));
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            
            // Read output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    LOGGER.fine(line);
                }
            }
            
            // Wait for completion with timeout
            boolean completed = process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (!completed) {
                process.destroyForcibly();
                return ProcessResult.failure("Command timed out after " + 
                    COMMAND_TIMEOUT_SECONDS + " seconds");
            }
            
            int exitCode = process.exitValue();
            
            if (exitCode == 0) {
                return ProcessResult.success(output.toString());
            } else {
                return ProcessResult.failure("Command failed with exit code " + 
                    exitCode + ": " + output.toString());
            }
            
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Error executing command", e);
            return ProcessResult.failure(e.getMessage());
        }
    }
    
    /**
     * Save error log to .libertyls/prepare-config/build.log file.
     *
     * @param projectPath The project path
     * @param errorMessage The error message to save
     * @return The path to the log file, or null if save failed
     */
    private String saveErrorLog(String projectPath, String errorMessage) {
        try {
            // Create .libertyls/prepare-config directory if it doesn't exist
            Path libertylsDir = Paths.get(projectPath, ".libertyls");
            Path prepareConfigDir = libertylsDir.resolve("prepare-config");
            if (!Files.exists(prepareConfigDir)) {
                Files.createDirectories(prepareConfigDir);
            }
            
            // Create log file path
            Path logFilePath = prepareConfigDir.resolve(BUILD_LOG_FILE);
            
            // Write error log with timestamp
            try (BufferedWriter writer = new BufferedWriter(
                    new FileWriter(logFilePath.toFile(), StandardCharsets.UTF_8))) {
                
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String timestamp = LocalDateTime.now().format(formatter);
                
                writer.write("=".repeat(80));
                writer.newLine();
                writer.write("Liberty Configuration Generation Error Log");
                writer.newLine();
                writer.write("Timestamp: " + timestamp);
                writer.newLine();
                writer.write("=".repeat(80));
                writer.newLine();
                writer.newLine();
                writer.write(errorMessage);
                writer.newLine();
                writer.newLine();
                writer.write("=".repeat(80));
                writer.newLine();
            }
            
            LOGGER.info("Error log saved to: " + logFilePath);
            return logFilePath.toString();
            
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save error log", e);
            return null;
        }
    }
    
    /**
     * Clear the processed projects cache.
     */
    public void clearCache() {
        processedProjects.clear();
    }
    
    /**
     * Build tool enum.
     */
    private enum BuildTool {
        MAVEN, GRADLE, NONE
    }
    
    /**
     * Result of config generation.
     */
    public static class ConfigGenerationResult {
        private final boolean success;
        private final String configPath;
        private final String error;
        private final String logFilePath;
        private final long duration;
        
        private ConfigGenerationResult(boolean success, String configPath,
                                       String error, String logFilePath, long duration) {
            this.success = success;
            this.configPath = configPath;
            this.error = error;
            this.logFilePath = logFilePath;
            this.duration = duration;
        }
        
        public static ConfigGenerationResult success(String configPath, long duration) {
            return new ConfigGenerationResult(true, configPath, null, null, duration);
        }
        
        public static ConfigGenerationResult failure(String error, String logFilePath, long duration) {
            return new ConfigGenerationResult(false, null, error, logFilePath, duration);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getConfigPath() {
            return configPath;
        }
        
        public String getError() {
            return error;
        }
        
        public String getLogFilePath() {
            return logFilePath;
        }
        
        public long getDuration() {
            return duration;
        }
    }
    
    /**
     * Result of process execution.
     */
    private static class ProcessResult {
        private final boolean success;
        private final String output;
        private final String error;
        
        private ProcessResult(boolean success, String output, String error) {
            this.success = success;
            this.output = output;
            this.error = error;
        }
        
        public static ProcessResult success(String output) {
            return new ProcessResult(true, output, null);
        }
        
        public static ProcessResult failure(String error) {
            return new ProcessResult(false, null, error);
        }
    }
    
    /**
     * Mark a project as failed with error message (for showing warning diagnostic).
     *
     * @param workspace The Liberty workspace that failed
     * @param errorMessage The error message from the failed generation attempt
     * @param logFilePath The path to the log file containing detailed error information
     */
    public void markProjectAsFailed(LibertyWorkspace workspace, String errorMessage, String logFilePath) {
        String projectPath = workspace.getWorkspaceURI().getPath();
        failedProjects.put(projectPath, new FailureInfo(errorMessage, logFilePath));
        LOGGER.info("Project marked as failed: " + projectPath + " - " + errorMessage);
    }
    
    /**
     * Get the error message for a failed project.
     *
     * @param workspace The Liberty workspace
     * @return The error message, or null if project hasn't failed
     */
    public String getFailureMessage(LibertyWorkspace workspace) {
        String projectPath = workspace.getWorkspaceURI().getPath();
        FailureInfo info = failedProjects.get(projectPath);
        return info != null ? info.errorMessage : null;
    }
    
    /**
     * Get the log file path for a failed project.
     *
     * @param workspace The Liberty workspace
     * @return The log file path, or null if project hasn't failed or no log exists
     */
    public String getLogFilePath(LibertyWorkspace workspace) {
        String projectPath = workspace.getWorkspaceURI().getPath();
        FailureInfo info = failedProjects.get(projectPath);
        return info != null ? info.logFilePath : null;
    }
    
    /**
     * Check if a project has failed config generation.
     *
     * @param workspace The Liberty workspace
     * @return true if the project has failed, false otherwise
     */
    public boolean hasProjectFailed(LibertyWorkspace workspace) {
        String projectPath = workspace.getWorkspaceURI().getPath();
        return failedProjects.containsKey(projectPath);
    }
    
    /**
     * Clear the failure status for a project (called after successful generation).
     *
     * @param workspace The Liberty workspace
     */
    public void clearProjectFailure(LibertyWorkspace workspace) {
        String projectPath = workspace.getWorkspaceURI().getPath();
        failedProjects.remove(projectPath);
        LOGGER.info("Cleared failure status for project: " + projectPath);
    }
    
    /**
     * Clear all failed projects cache (useful for testing or reset).
     */
    public void clearAllFailures() {
        failedProjects.clear();
        LOGGER.info("Cleared all failed projects cache");
    }
    
    /**
     * Information about a failed config generation attempt.
     */
    private static class FailureInfo {
        private final String errorMessage;
        private final String logFilePath;
        
        public FailureInfo(String errorMessage, String logFilePath) {
            this.errorMessage = errorMessage;
            this.logFilePath = logFilePath;
        }
    }
}
