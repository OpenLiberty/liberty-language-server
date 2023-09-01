package io.openliberty.tools.langserver.xml;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.lsp4j.WorkspaceFolder;
import org.junit.Test;

import io.openliberty.tools.langserver.LibertyConfigFileManager;

public class LibertyConfigFileManagerTest {
    File resourcesDir = new File("src/test/resources");
    public final String CUSTOM_SERVER_ENV_VALUE = "/user/sample-project/src/main/liberty/config/customserverenv.env";
    public final String CUSTOM_BOOTSTRAP_PROPERTIES_VALUE = "/user/sample-project/src/main/liberty/config/custombootstrapprops.properties";

    @Test
    public void processConfigXml() throws IOException {
        File lpcXml = new File(resourcesDir, "xml/liberty-plugin-config.xml");

        LibertyConfigFileManager.processLibertyPluginConfigXml(lpcXml.getCanonicalPath());
        // test breaks in Windows for now, because test resource uses unix path
        if (File.separator.equals("/")) {
            assertTrue(LibertyConfigFileManager.isServerEnvFile(CUSTOM_SERVER_ENV_VALUE));
            assertTrue(LibertyConfigFileManager.isBootstrapPropertiesFile(CUSTOM_BOOTSTRAP_PROPERTIES_VALUE));
        }
    }

    @Test
    public void initCustomConfigTest() {
        WorkspaceFolder folder = new WorkspaceFolder(resourcesDir.toURI().toString());
        LibertyConfigFileManager.processWorkspaceDir(folder);
        // test breaks in Windows for now, because test resource uses unix path
        if (File.separator.equals("/")) {
            assertTrue(LibertyConfigFileManager.isServerEnvFile(CUSTOM_SERVER_ENV_VALUE));
            assertTrue(LibertyConfigFileManager.isBootstrapPropertiesFile(CUSTOM_BOOTSTRAP_PROPERTIES_VALUE));
        }
    }
}
