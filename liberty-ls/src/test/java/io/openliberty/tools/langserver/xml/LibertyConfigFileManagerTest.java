package io.openliberty.tools.langserver.xml;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.lsp4j.WorkspaceFolder;
import org.junit.Test;

import io.openliberty.tools.langserver.LibertyConfigFileManager;

public class LibertyConfigFileManagerTest {
    File resourcesDir = new File("src/test/resources");
    public final String CUSTOM_SERVER_ENV = "file:/user/sample-project/src/main/liberty/config/customserverenv.env";
    public final String CUSTOM_BOOTSTRAP_PROPERTIES = "file:/user/sample-project/src/main/liberty/config/custombootstrapprops.properties";
    public final String WINDOWS_CUSTOM_SERVER_ENV = "file:/C:/user/sample-project/src/main/liberty/config/customserverenv.env";
    public final String WINDOWS_CUSTOM_BOOTSTRAP_PROPERTIES = "file:/C:/user/sample-project/src/main/liberty/config/custombootstrapprops.properties";

    @Test
    public void processConfigXml() throws IOException {
        if (File.separator.equals("/")) {
            File lpcXml = new File(resourcesDir, "xml/unix/liberty-plugin-config.xml");

            LibertyConfigFileManager.processLibertyPluginConfigXml(lpcXml.toURI().toString());
            assertTrue(LibertyConfigFileManager.isServerEnvFile(CUSTOM_SERVER_ENV));
            assertTrue(LibertyConfigFileManager.isBootstrapPropertiesFile(CUSTOM_BOOTSTRAP_PROPERTIES));
        }
    }

    @Test
    public void processWindowsConfigXml() throws IOException {
        if (!File.separator.equals("/")) {
            File lpcXml = new File(resourcesDir, "xml/windows/liberty-plugin-config.xml");

            LibertyConfigFileManager.processLibertyPluginConfigXml(lpcXml.getCanonicalPath());
            assertTrue(LibertyConfigFileManager.isServerEnvFile(WINDOWS_CUSTOM_SERVER_ENV));
            assertTrue(LibertyConfigFileManager.isBootstrapPropertiesFile(WINDOWS_CUSTOM_BOOTSTRAP_PROPERTIES));
        }
    }

    @Test
    public void initCustomConfigTest() {
        WorkspaceFolder folder = new WorkspaceFolder(resourcesDir.toURI().toString());
        LibertyConfigFileManager.processWorkspaceDir(folder);

        if (File.separator.equals("/")) {
            assertTrue(LibertyConfigFileManager.isServerEnvFile(CUSTOM_SERVER_ENV));
            assertTrue(LibertyConfigFileManager.isBootstrapPropertiesFile(CUSTOM_BOOTSTRAP_PROPERTIES));
        } else {
            assertTrue(LibertyConfigFileManager.isServerEnvFile(WINDOWS_CUSTOM_SERVER_ENV));
            assertTrue(LibertyConfigFileManager.isBootstrapPropertiesFile(WINDOWS_CUSTOM_BOOTSTRAP_PROPERTIES));
        }

    }
}
