package io.openliberty;

import io.openliberty.tools.langserver.lemminx.services.LibertyProjectsManager;
import io.openliberty.tools.langserver.lemminx.services.LibertyWorkspace;
import io.openliberty.tools.langserver.lemminx.services.SettingsService;
import io.openliberty.tools.langserver.lemminx.util.LibertyUtils;
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
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        initList.add(new WorkspaceFolder(resourcesLibertyDir.toURI().toString()));
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
    }

    @AfterEach
    public void cleanup() {
        libertyUtils.close();
    }

    @Test
    public void testPopulateAllVariables() throws IOException {
        List<LibertyWorkspace> initList = new ArrayList<>();
        initList.add(new LibertyWorkspace(resourcesLibertyDir.toURI().toString()));
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

}
