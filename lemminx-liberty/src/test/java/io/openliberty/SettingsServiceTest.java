package io.openliberty;

import io.openliberty.tools.langserver.lemminx.services.LibertyConfigGenerationService;
import io.openliberty.tools.langserver.lemminx.services.LibertyProjectsManager;
import io.openliberty.tools.langserver.lemminx.services.LibertyWorkspace;
import io.openliberty.tools.langserver.lemminx.services.SettingsService;
import io.openliberty.tools.langserver.lemminx.util.LibertyUtils;
import io.openliberty.tools.langserver.lemminx.util.ResourceBundleUtil;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

public class SettingsServiceTest {
    File resourcesDir = new File("src/test/resources/serverConfig");
    File resourcesLibertyDir = new File("src/test/resources/serverConfig", "liberty");
    File serverDir = new File("src/test/resources/serverConfig/liberty/wlp/usr/servers/defaultServer");
    File serverXmlFile = new File(serverDir,"server.xml");
    File installDir = new File("src/test/resources/serverConfig/liberty/wlp");
    File userDir = new File("src/test/resources/serverConfig/liberty/wlp/usr");

    MockedStatic libertyUtils;
    static List<WorkspaceFolder> initList = new ArrayList<WorkspaceFolder>();
    LibertyProjectsManager libPM;
    LibertyWorkspace libWorkspace;

    @BeforeEach
    public void setupWorkspace() {
        initList.add(new WorkspaceFolder(resourcesDir.toURI().toString()));
        initList.add(new WorkspaceFolder(resourcesLibertyDir.toURI().toString())); // initialize workspace that test method uses also
        libPM = LibertyProjectsManager.getInstance();
        libPM.setWorkspaceFolders(initList);
        libWorkspace = libPM.getLibertyWorkspaceFolders().iterator().next();
        libertyUtils = Mockito.mockStatic(LibertyUtils.class);
        libertyUtils.when(() -> LibertyUtils.findFileInWorkspace(any(), any()))
                .thenReturn(Path.of(resourcesDir.getPath()));
        libertyUtils.when(() -> LibertyUtils.getFileFromLibertyPluginXml(any(), eq("serverDirectory")))
                .thenReturn(serverDir);
        libertyUtils.when(() -> LibertyUtils.getFileFromLibertyPluginXml(any(), eq("installDirectory")))
                .thenReturn(installDir);
        libertyUtils.when(() -> LibertyUtils.getFileFromLibertyPluginXml(any(), eq("userDirectory")))
                .thenReturn(userDir);
        libertyUtils.when(() -> LibertyUtils.getFileFromLibertyPluginXml(any(), eq("serverOutputDirectory")))
                .thenReturn(serverDir);
    }

    @AfterEach
    public void cleanup() {
        libertyUtils.close();
    }

    @Test
    public void testPopulateAllVariables() throws IOException {
        List<LibertyWorkspace> initList = new ArrayList<>();
        initList.add(new LibertyWorkspace(resourcesLibertyDir.toURI().toString()));
        initList.add(new LibertyWorkspace(installDir.toURI().toString()));
        SettingsService.getInstance().populateAllVariables(initList);
        Properties variables = SettingsService.getInstance().getVariablesForServerXml(serverXmlFile.toURI().toString()); // point to a file not a workspace dir

        assertNotNull(variables);
        // bootstrap.properties.override added in server.env and bootstrap.properties
        // bootstrap.properties gets highest priority
        assertEquals("true", variables.get("bootstrap.properties.override"));

        /* server.env file read order. http.port is specified in 3 places
         *   1. {wlp.install.dir}/etc/
         *   2. {wlp.user.dir}/shared/
         *   3. {server.config.dir}/
         */
        assertEquals("1111", variables.get("http.port"));

        // httpPort defined in multiple places. highest precedence is for configDropins/overrides
        assertEquals("7777", variables.get("httpPort"));

        // variable defined in variables.override
        assertEquals("true", variables.get("variables.override"));

        // checking default properties are set or not
        assertEquals("includes", variables.get("includeLocation"));
    }

    @Test
    public void testInitializeLocale(){
        InitializeParams initParams = new InitializeParams();
        //language only
        initParams.setLocale("en");
        SettingsService.getInstance().initializeLocale(initParams);
        assertEquals("en",SettingsService.getInstance().getCurrentLocale().toString());

        initParams.setLocale("FR");
        SettingsService.getInstance().initializeLocale(initParams);
        assertEquals("fr",SettingsService.getInstance().getCurrentLocale().toString());

        initParams.setLocale("ja");
        SettingsService.getInstance().initializeLocale(initParams);
        assertEquals("ja",SettingsService.getInstance().getCurrentLocale().toString());

        // language and region, should return matching locale en as liberty only uses "en"
        // testing with different formats
        initParams.setLocale("en-us");
        SettingsService.getInstance().initializeLocale(initParams);
        assertEquals("en",SettingsService.getInstance().getCurrentLocale().toString());
        initParams.setLocale("en-Us");
        SettingsService.getInstance().initializeLocale(initParams);
        assertEquals("en",SettingsService.getInstance().getCurrentLocale().toString());
        initParams.setLocale("en-US");
        SettingsService.getInstance().initializeLocale(initParams);
        assertEquals("en",SettingsService.getInstance().getCurrentLocale().toString());
        initParams.setLocale("EN-US");
        SettingsService.getInstance().initializeLocale(initParams);
        assertEquals("en",SettingsService.getInstance().getCurrentLocale().toString());
        initParams.setLocale("en");
        SettingsService.getInstance().initializeLocale(initParams);
        assertEquals("en",SettingsService.getInstance().getCurrentLocale().toString());
        initParams.setLocale("EN_US");
        SettingsService.getInstance().initializeLocale(initParams);
        assertEquals("en",SettingsService.getInstance().getCurrentLocale().toString());
        initParams.setLocale("en_us");
        SettingsService.getInstance().initializeLocale(initParams);
        assertEquals("en",SettingsService.getInstance().getCurrentLocale().toString());
        initParams.setLocale("de_AT");
        SettingsService.getInstance().initializeLocale(initParams);
        assertEquals("de",SettingsService.getInstance().getCurrentLocale().toString());

        // testing with language code and country, liberty supports "es_ES" locale
        initParams.setLocale("es-Es");
        SettingsService.getInstance().initializeLocale(initParams);
        assertEquals("es_ES",SettingsService.getInstance().getCurrentLocale().toString());
        initParams.setLocale("es_Es");
        SettingsService.getInstance().initializeLocale(initParams);
        assertEquals("es_ES",SettingsService.getInstance().getCurrentLocale().toString());

        // testing with language code and country, liberty supports "zh_TW" locale
        initParams.setLocale("zh_TW");
        SettingsService.getInstance().initializeLocale(initParams);
        assertEquals("zh_TW",SettingsService.getInstance().getCurrentLocale().toString());
        initParams.setLocale("zh-tw");
        SettingsService.getInstance().initializeLocale(initParams);
        assertEquals("zh_TW",SettingsService.getInstance().getCurrentLocale().toString());
        initParams.setLocale("zh_tw");
        SettingsService.getInstance().initializeLocale(initParams);
        assertEquals("zh_TW",SettingsService.getInstance().getCurrentLocale().toString());


        // testing with language code and country, liberty supports "zh_CN" locale
        initParams.setLocale("zh_CN");
        SettingsService.getInstance().initializeLocale(initParams);
        assertEquals("zh_CN",SettingsService.getInstance().getCurrentLocale().toString());
        initParams.setLocale("zh-cn");
        SettingsService.getInstance().initializeLocale(initParams);
        assertEquals("zh_CN",SettingsService.getInstance().getCurrentLocale().toString());
        initParams.setLocale("ZH-CN");
        SettingsService.getInstance().initializeLocale(initParams);
        assertEquals("zh_CN",SettingsService.getInstance().getCurrentLocale().toString());


        // Language, Script, and Region format
        initParams.setLocale("en-Latn-US");
        SettingsService.getInstance().initializeLocale(initParams);
        assertEquals("en",SettingsService.getInstance().getCurrentLocale().toString());

        // default to en for valid, but not matching with liberty locales
        initParams.setLocale("hi_IN");
        SettingsService.getInstance().initializeLocale(initParams);
        assertEquals("en",SettingsService.getInstance().getCurrentLocale().toString());

        // default to en for invalid
        initParams.setLocale("en-krp");
        SettingsService.getInstance().initializeLocale(initParams);
        assertEquals("en",SettingsService.getInstance().getCurrentLocale().toString());
        // all liberty supported locales are working as-is
        for (Locale locale : ResourceBundleUtil.getAvailableLocales()) {
            initParams.setLocale(locale.toString());
            SettingsService.getInstance().initializeLocale(initParams);
            assertEquals(locale.toString(), SettingsService.getInstance().getCurrentLocale().toString());
        }
    }

    @Test
    public void testPopulateVariablesForWorkspace_WithConfigGeneration() throws IOException {
        // Test that populateVariablesForWorkspace triggers config generation when needed
        MockedStatic<LibertyConfigGenerationService> configServiceMock =
                Mockito.mockStatic(LibertyConfigGenerationService.class);
        
        try {
            LibertyConfigGenerationService mockService = Mockito.mock(LibertyConfigGenerationService.class);
            configServiceMock.when(LibertyConfigGenerationService::getInstance).thenReturn(mockService);
            
            // Setup mock behavior
            Mockito.when(mockService.hasLibertyPlugin(any())).thenReturn(true);
            Mockito.when(mockService.needsConfigGeneration(any())).thenReturn(false);
            
            // Call populateVariablesForWorkspace
            SettingsService.getInstance().populateVariablesForWorkspace(libWorkspace);
            
            // Verify that config generation check was called
            Mockito.verify(mockService).hasLibertyPlugin(any());
            Mockito.verify(mockService).needsConfigGeneration(any());
        } finally {
            configServiceMock.close();
        }
    }

    @Test
    public void testGetVariablesForServerXml_TriggersPopulation() {
        // Test that getVariablesForServerXml triggers population if not already done
        SettingsService.getInstance().initializeVariablesMap();
        
        Properties variables = SettingsService.getInstance().getVariablesForServerXml(
                serverXmlFile.toURI().toString());
        
        assertNotNull(variables, "Variables should not be null");
    }

    @Test
    public void testPopulateVariablesForWorkspace_NoLibertyPlugin() throws IOException {
        // Test behavior when project doesn't have Liberty plugin
        MockedStatic<LibertyConfigGenerationService> configServiceMock =
                Mockito.mockStatic(LibertyConfigGenerationService.class);
        
        try {
            LibertyConfigGenerationService mockService = Mockito.mock(LibertyConfigGenerationService.class);
            configServiceMock.when(LibertyConfigGenerationService::getInstance).thenReturn(mockService);
            
            // Setup mock to return false for hasLibertyPlugin
            Mockito.when(mockService.hasLibertyPlugin(any())).thenReturn(false);
            
            // Call populateVariablesForWorkspace
            SettingsService.getInstance().populateVariablesForWorkspace(libWorkspace);
            
            // Verify that needsConfigGeneration was NOT called since plugin is not present
            Mockito.verify(mockService).hasLibertyPlugin(any());
            Mockito.verify(mockService, Mockito.never()).needsConfigGeneration(any());
        } finally {
            configServiceMock.close();
        }
    }

    @Test
    public void testPopulateVariablesForWorkspace_ConfigGenerationNeeded() throws Exception {
        // Test that config is regenerated when needed
        MockedStatic<LibertyConfigGenerationService> configServiceMock =
                Mockito.mockStatic(LibertyConfigGenerationService.class);
        
        try {
            LibertyConfigGenerationService mockService = Mockito.mock(LibertyConfigGenerationService.class);
            configServiceMock.when(LibertyConfigGenerationService::getInstance).thenReturn(mockService);
            
            // Setup mock behavior - config needs generation
            Mockito.when(mockService.hasLibertyPlugin(any())).thenReturn(true);
            Mockito.when(mockService.needsConfigGeneration(any())).thenReturn(true);
            
            // Mock the async generation
            LibertyConfigGenerationService.ConfigGenerationResult successResult =
                    LibertyConfigGenerationService.ConfigGenerationResult.success(
                            "/path/to/config", 1000);
            java.util.concurrent.CompletableFuture<LibertyConfigGenerationService.ConfigGenerationResult> future =
                    java.util.concurrent.CompletableFuture.completedFuture(successResult);
            Mockito.when(mockService.generateConfigAsync(any())).thenReturn(future);
            
            // Call populateVariablesForWorkspace
            SettingsService.getInstance().populateVariablesForWorkspace(libWorkspace);
            
            // Verify that config generation was triggered
            Mockito.verify(mockService).hasLibertyPlugin(any());
            Mockito.verify(mockService).needsConfigGeneration(any());
            Mockito.verify(mockService).generateConfigAsync(any());
        } finally {
            configServiceMock.close();
        }
    }

    @Test
    public void testPopulateVariablesForWorkspace_ConfigGenerationFailed() throws Exception {
        // Test behavior when config generation fails
        MockedStatic<LibertyConfigGenerationService> configServiceMock =
                Mockito.mockStatic(LibertyConfigGenerationService.class);
        
        try {
            LibertyConfigGenerationService mockService = Mockito.mock(LibertyConfigGenerationService.class);
            configServiceMock.when(LibertyConfigGenerationService::getInstance).thenReturn(mockService);
            
            // Setup mock behavior - config needs generation but fails
            Mockito.when(mockService.hasLibertyPlugin(any())).thenReturn(true);
            Mockito.when(mockService.needsConfigGeneration(any())).thenReturn(true);
            
            // Mock the async generation with failure
            LibertyConfigGenerationService.ConfigGenerationResult failureResult =
                    LibertyConfigGenerationService.ConfigGenerationResult.failure(
                            "Build failed", "/path/to/log", 1000);
            java.util.concurrent.CompletableFuture<LibertyConfigGenerationService.ConfigGenerationResult> future =
                    java.util.concurrent.CompletableFuture.completedFuture(failureResult);
            Mockito.when(mockService.generateConfigAsync(any())).thenReturn(future);
            
            // Call populateVariablesForWorkspace
            SettingsService.getInstance().populateVariablesForWorkspace(libWorkspace);
            
            // Verify that markProjectAsFailed was called
            Mockito.verify(mockService).markProjectAsFailed(any(), eq("Build failed"), eq("/path/to/log"));
        } finally {
            configServiceMock.close();
        }
    }

    @Test
    public void testInitializeVariablesMap() {
        // Test that variables map is initialized
        SettingsService service = SettingsService.getInstance();
        service.initializeVariablesMap();
        
        // Should not throw exception when getting variables
        Properties variables = service.getVariablesForServerXml(serverXmlFile.toURI().toString());
        assertNotNull(variables, "Variables should be initialized");
    }
}
