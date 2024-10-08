package io.openliberty.tools.langserver.xml;


import java.io.File;
import java.io.IOException;

import org.eclipse.lsp4j.WorkspaceFolder;

import io.openliberty.tools.langserver.LibertyConfigFileManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LibertyConfigFileManagerTest {
    File resourcesDir = new File("src/test/resources");
    File resourcesDir2 = new File("src/test/resources2");

    public final String CUSTOM_SERVER_ENV = "file:/user/sample-project/src/main/liberty/config/customserverenv.env";
    public final String CUSTOM_BOOTSTRAP_PROPERTIES = "file:/user/sample-project/src/main/liberty/config/custombootstrapprops.properties";
    public final String CUSTOM_CONFIG_DIR_ENV = "file:/user/sample-project/src/main/liberty/config2/server.env";
    public final String CUSTOM_CONFIG_DIR_PROP = "file:/user/sample-project/src/main/liberty/config2/bootstrap.properties";
    public final String WINDOWS_CUSTOM_SERVER_ENV = "file:/C:/user/sample-project/src/main/liberty/config/customserverenv.env";
    public final String WINDOWS_CUSTOM_BOOTSTRAP_PROPERTIES = "file:/C:/user/sample-project/src/main/liberty/config/custombootstrapprops.properties";
    public final String WINDOWS_CUSTOM_CONFIG_DIR_ENV = "file:/C:/user/sample-project/src/main/liberty/config2/server.env";
    public final String WINDOWS_CUSTOM_CONFIG_DIR_PROP = "file:/C:/user/sample-project/src/main/liberty/config2/bootstrap.properties";

    public final String ANOTHER_BOOTSTRAP = "file:/user/sample-project/src/main/another.properties";

    @Test
    public void processConfigXml() throws IOException {
        if (File.separator.equals("/")) {
            File lpcXml = new File(resourcesDir, "xml/unix/liberty-plugin-config.xml");

            LibertyConfigFileManager.processLibertyPluginConfigXml(lpcXml.toURI().toString());
            assertTrue(LibertyConfigFileManager.isServerEnvFile(CUSTOM_SERVER_ENV));
            assertTrue(LibertyConfigFileManager.isBootstrapPropertiesFile(CUSTOM_BOOTSTRAP_PROPERTIES));
            assertTrue(LibertyConfigFileManager.isServerEnvFile(CUSTOM_CONFIG_DIR_ENV));
            assertTrue(LibertyConfigFileManager.isBootstrapPropertiesFile(CUSTOM_CONFIG_DIR_PROP));
        }
    }

    @Test
    public void processWindowsConfigXml() throws IOException {
        if (!File.separator.equals("/")) {
            File lpcXml = new File(resourcesDir, "xml/windows/liberty-plugin-config.xml");

            LibertyConfigFileManager.processLibertyPluginConfigXml(lpcXml.toURI().toString());
            assertTrue(LibertyConfigFileManager.isServerEnvFile(WINDOWS_CUSTOM_SERVER_ENV));
            assertTrue(LibertyConfigFileManager.isBootstrapPropertiesFile(WINDOWS_CUSTOM_BOOTSTRAP_PROPERTIES));
            assertTrue(LibertyConfigFileManager.isServerEnvFile(WINDOWS_CUSTOM_CONFIG_DIR_ENV));
            assertTrue(LibertyConfigFileManager.isBootstrapPropertiesFile(WINDOWS_CUSTOM_CONFIG_DIR_PROP));
        }
    }

    @Test
    public void initCustomConfigTest() {
        assertFalse(LibertyConfigFileManager.isBootstrapPropertiesFile(ANOTHER_BOOTSTRAP));
        if (File.separator.equals("/")) {
            WorkspaceFolder folder = new WorkspaceFolder(resourcesDir2.toURI().toString());
            LibertyConfigFileManager.processWorkspaceDir(folder);
            assertTrue(LibertyConfigFileManager.isServerEnvFile(CUSTOM_SERVER_ENV));
            assertTrue(LibertyConfigFileManager.isBootstrapPropertiesFile(ANOTHER_BOOTSTRAP));
        } else {
            WorkspaceFolder folder = new WorkspaceFolder(resourcesDir.toURI().toString());
            LibertyConfigFileManager.processWorkspaceDir(folder);
            assertTrue(LibertyConfigFileManager.isServerEnvFile(WINDOWS_CUSTOM_SERVER_ENV));
            assertTrue(LibertyConfigFileManager.isBootstrapPropertiesFile(WINDOWS_CUSTOM_BOOTSTRAP_PROPERTIES));
        }
    }
}
