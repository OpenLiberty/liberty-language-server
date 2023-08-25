package io.openliberty.tools.langserver.xml;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import io.openliberty.tools.langserver.LibertyConfigFileManager;

public class LibertyConfigFileManagerTest {
    File resourcesDir = new File("src/test/resources");

    @Test
    public void processConfigXml() throws IOException {
        File lpcXml = new File(resourcesDir, "xml/liberty-plugin-config.xml");

        LibertyConfigFileManager.processLibertyPluginConfigXml(lpcXml.getCanonicalPath());
        assertTrue(LibertyConfigFileManager.isServerEnvFile("/user/sample-project/src/main/liberty/config/customserverenv.env"));
        assertTrue(LibertyConfigFileManager.isBootstrapPropertiesFile("/user/sample-project/src/main/liberty/config/custombootstrapprops.properties"));
    }
}
