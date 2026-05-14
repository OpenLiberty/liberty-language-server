package io.openliberty;

import io.openliberty.tools.langserver.lemminx.services.LibertyConfigGenerationService;
import io.openliberty.tools.langserver.lemminx.services.LibertyProjectsManager;
import io.openliberty.tools.langserver.lemminx.services.LibertyWorkspace;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LibertyConfigGenerationService.
 * Tests the automatic generation of liberty-plugin-config.xml.
 */
public class LibertyConfigGenerationServiceTest {

    @TempDir
    Path tempDir;

    private LibertyConfigGenerationService service;
    private LibertyWorkspace workspace;

    @BeforeEach
    public void setUp() {
        service = LibertyConfigGenerationService.getInstance();
        service.clearCache();
        service.clearAllFailures();
    }

    @AfterEach
    public void tearDown() {
        service.clearCache();
        service.clearAllFailures();
        if (workspace != null) {
            LibertyProjectsManager.getInstance().cleanInstance();
        }
    }

    @Test
    public void testHasLibertyPlugin_MavenProject() throws IOException {
        // Create a Maven project with liberty-maven-plugin
        Path pomFile = tempDir.resolve("pom.xml");
        String pomContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>io.openliberty.tools</groupId>
                                <artifactId>liberty-maven-plugin</artifactId>
                                <version>3.10</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;
        Files.writeString(pomFile, pomContent);

        workspace = new LibertyWorkspace(tempDir.toUri().toString());
        assertTrue(service.hasLibertyPlugin(workspace), "Should detect liberty-maven-plugin");
    }

    @Test
    public void testHasLibertyPlugin_GradleProject() throws IOException {
        // Create a Gradle project with liberty-gradle-plugin
        Path buildFile = tempDir.resolve("build.gradle");
        String buildContent = """
                plugins {
                    id 'liberty-gradle-plugin' version '3.8'
                }
                
                dependencies {
                    libertyRuntime group: 'io.openliberty', name: 'openliberty-runtime', version: '25.0.0.6'
                }
                """;
        Files.writeString(buildFile, buildContent);

        workspace = new LibertyWorkspace(tempDir.toUri().toString());
        assertTrue(service.hasLibertyPlugin(workspace), "Should detect liberty-gradle-plugin");
    }

    @Test
    public void testHasLibertyPlugin_GradleKtsProject() throws IOException {
        // Create a Gradle Kotlin DSL project
        Path buildFile = tempDir.resolve("build.gradle.kts");
        String buildContent = """
                plugins {
                    id("liberty") version "3.8"
                }
                
                dependencies {
                    libertyRuntime("io.openliberty:openliberty-runtime:25.0.0.6")
                }
                """;
        Files.writeString(buildFile, buildContent);

        workspace = new LibertyWorkspace(tempDir.toUri().toString());
        assertTrue(service.hasLibertyPlugin(workspace), "Should detect liberty plugin in build.gradle.kts");
    }

    @Test
    public void testHasLibertyPlugin_NoPlugin() throws IOException {
        // Create a Maven project without Liberty plugin
        Path pomFile = tempDir.resolve("pom.xml");
        String pomContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                </project>
                """;
        Files.writeString(pomFile, pomContent);

        workspace = new LibertyWorkspace(tempDir.toUri().toString());
        assertFalse(service.hasLibertyPlugin(workspace), "Should not detect Liberty plugin");
    }

    @Test
    public void testHasLibertyPlugin_NoBuildFile() {
        workspace = new LibertyWorkspace(tempDir.toUri().toString());
        assertFalse(service.hasLibertyPlugin(workspace), "Should return false when no build file exists");
    }

    @Test
    public void testNeedsConfigGeneration_NoConfigFile() throws IOException {
        // Create a Maven project
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, "<project></project>");

        workspace = new LibertyWorkspace(tempDir.toUri().toString());
        assertTrue(service.needsConfigGeneration(workspace), 
                "Should need generation when config file doesn't exist");
    }

    @Test
    public void testNeedsConfigGeneration_ConfigExists() throws IOException {
        // Create a Maven project with config file
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, "<project></project>");

        // Create config file in target directory
        Path targetDir = tempDir.resolve("target");
        Files.createDirectories(targetDir);
        Path configFile = targetDir.resolve("liberty-plugin-config.xml");
        String configContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <liberty-plugin-config>
                    <installDirectory>/path/to/liberty</installDirectory>
                    <serverDirectory>/path/to/server</serverDirectory>
                    <userDirectory>/path/to/usr</userDirectory>
                    <serverOutputDirectory>/path/to/output</serverOutputDirectory>
                </liberty-plugin-config>
                """;
        Files.writeString(configFile, configContent);

        workspace = new LibertyWorkspace(tempDir.toUri().toString());
        
        // Config exists and is not stale, so should NOT need generation
        boolean needsGen = service.needsConfigGeneration(workspace);
        
        assertFalse(needsGen, "Should not need generation when config exists and is up-to-date");
    }

    @Test
    public void testNeedsConfigGeneration_StaleConfig() throws IOException, InterruptedException {
        // Create a Maven project
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, "<project></project>");

        // Create config file
        Path targetDir = tempDir.resolve("target");
        Files.createDirectories(targetDir);
        Path configFile = targetDir.resolve("liberty-plugin-config.xml");
        Files.writeString(configFile, "<liberty-plugin-config></liberty-plugin-config>");

        // Wait a bit and update pom.xml to make it newer
        Thread.sleep(100);
        Files.writeString(pomFile, "<project><!-- updated --></project>");

        workspace = new LibertyWorkspace(tempDir.toUri().toString());
        assertTrue(service.needsConfigGeneration(workspace), 
                "Should need generation when config is older than build file");
    }

    @Test
    public void testMarkProjectAsFailed() throws IOException {
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, "<project></project>");

        workspace = new LibertyWorkspace(tempDir.toUri().toString());
        
        String errorMessage = "Build failed";
        String logPath = "/path/to/log";
        
        service.markProjectAsFailed(workspace, errorMessage, logPath);
        
        assertTrue(service.hasProjectFailed(workspace), "Project should be marked as failed");
        assertEquals(errorMessage, service.getFailureMessage(workspace), 
                "Should return correct error message");
        assertEquals(logPath, service.getLogFilePath(workspace), 
                "Should return correct log path");
    }

    @Test
    public void testClearProjectFailure() throws IOException {
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, "<project></project>");

        workspace = new LibertyWorkspace(tempDir.toUri().toString());
        
        service.markProjectAsFailed(workspace, "Error", null);
        assertTrue(service.hasProjectFailed(workspace), "Project should be marked as failed");
        
        service.clearProjectFailure(workspace);
        assertFalse(service.hasProjectFailed(workspace), "Project failure should be cleared");
        assertNull(service.getFailureMessage(workspace), "Error message should be null");
    }

    @Test
    public void testClearAllFailures() throws IOException {
        // Create two projects
        Path project1 = tempDir.resolve("project1");
        Files.createDirectories(project1);
        Files.writeString(project1.resolve("pom.xml"), "<project></project>");
        
        Path project2 = tempDir.resolve("project2");
        Files.createDirectories(project2);
        Files.writeString(project2.resolve("pom.xml"), "<project></project>");

        LibertyWorkspace workspace1 = new LibertyWorkspace(project1.toUri().toString());
        LibertyWorkspace workspace2 = new LibertyWorkspace(project2.toUri().toString());
        
        service.markProjectAsFailed(workspace1, "Error 1", null);
        service.markProjectAsFailed(workspace2, "Error 2", null);
        
        assertTrue(service.hasProjectFailed(workspace1), "Project 1 should be marked as failed");
        assertTrue(service.hasProjectFailed(workspace2), "Project 2 should be marked as failed");
        
        service.clearAllFailures();
        
        assertFalse(service.hasProjectFailed(workspace1), "Project 1 failure should be cleared");
        assertFalse(service.hasProjectFailed(workspace2), "Project 2 failure should be cleared");
    }

    @Test
    public void testGenerateConfigAsync() throws Exception {
        // Create a Maven project with Liberty plugin
        Path pomFile = tempDir.resolve("pom.xml");
        String pomContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>io.openliberty.tools</groupId>
                                <artifactId>liberty-maven-plugin</artifactId>
                                <version>3.10</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;
        Files.writeString(pomFile, pomContent);

        workspace = new LibertyWorkspace(tempDir.toUri().toString());
        
        CompletableFuture<LibertyConfigGenerationService.ConfigGenerationResult> future = 
                service.generateConfigAsync(workspace);
        
        assertNotNull(future, "Should return a CompletableFuture");
        
        // Wait for completion (will likely fail since mvn is not available, but tests async behavior)
        LibertyConfigGenerationService.ConfigGenerationResult result = 
                future.get(5, TimeUnit.SECONDS);
        
        assertNotNull(result, "Should return a result");
        // Result will likely be a failure since we don't have Maven installed in test environment
    }

    @Test
    public void testClearCache() throws IOException {
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, "<project></project>");

        workspace = new LibertyWorkspace(tempDir.toUri().toString());
        
        // Simulate processing
        service.generateConfig(workspace);
        
        // Clear cache
        service.clearCache();
        
        // After clearing cache, should need generation again
        assertTrue(service.needsConfigGeneration(workspace), 
                "Should need generation after cache is cleared");
    }

    @Test
    public void testConfigGenerationResult_Success() {
        LibertyConfigGenerationService.ConfigGenerationResult result = 
                LibertyConfigGenerationService.ConfigGenerationResult.success("/path/to/config", 1000);
        
        assertTrue(result.isSuccess(), "Result should be successful");
        assertEquals("/path/to/config", result.getConfigPath(), "Should return correct config path");
        assertNull(result.getError(), "Error should be null for success");
        assertNull(result.getLogFilePath(), "Log file path should be null for success");
        assertEquals(1000, result.getDuration(), "Should return correct duration");
    }

    @Test
    public void testConfigGenerationResult_Failure() {
        LibertyConfigGenerationService.ConfigGenerationResult result = 
                LibertyConfigGenerationService.ConfigGenerationResult.failure(
                        "Build failed", "/path/to/log", 2000);
        
        assertFalse(result.isSuccess(), "Result should be failure");
        assertNull(result.getConfigPath(), "Config path should be null for failure");
        assertEquals("Build failed", result.getError(), "Should return correct error message");
        assertEquals("/path/to/log", result.getLogFilePath(), "Should return correct log path");
        assertEquals(2000, result.getDuration(), "Should return correct duration");
    }

    @Test
    public void testNeedsConfigGeneration_MockServerDirectory() throws IOException {
        // Create a Maven project
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, "<project></project>");

        // Create config file with mock server reference
        Path targetDir = tempDir.resolve("target");
        Files.createDirectories(targetDir);
        
        // Create the .libertyls/mockServer directory structure
        Path mockServerDir = targetDir.resolve(".libertyls/mockServer");
        Files.createDirectories(mockServerDir);
        
        Path configFile = targetDir.resolve("liberty-plugin-config.xml");
        String configContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <liberty-plugin-config>
                    <installDirectory>/path/to/liberty</installDirectory>
                    <serverDirectory>""" + mockServerDir.toString() + """
                    </serverDirectory>
                    <userDirectory>/path/to/usr</userDirectory>
                    <serverOutputDirectory>/path/to/output</serverOutputDirectory>
                </liberty-plugin-config>
                """;
        Files.writeString(configFile, configContent);

        workspace = new LibertyWorkspace(tempDir.toUri().toString());
        
        // Test that the method handles mock server directory correctly without throwing exceptions
        assertDoesNotThrow(() -> service.needsConfigGeneration(workspace),
                "Should handle mock server directory check without exceptions");
    }

    @Test
    public void testHasLibertyPlugin_WithIoOpenLibertyTools() throws IOException {
        // Create a Maven project with io.openliberty.tools reference
        Path pomFile = tempDir.resolve("pom.xml");
        String pomContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>io.openliberty.tools</groupId>
                                <artifactId>some-plugin</artifactId>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;
        Files.writeString(pomFile, pomContent);

        workspace = new LibertyWorkspace(tempDir.toUri().toString());
        assertTrue(service.hasLibertyPlugin(workspace), 
                "Should detect io.openliberty.tools reference");
    }
}