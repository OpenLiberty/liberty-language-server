package io.openliberty.tools.langserver.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.nio.file.Path;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

import io.openliberty.tools.langserver.LibertyConfigFileManager;
import io.openliberty.tools.langserver.utils.XmlReader;

public class XmlReaderTest {
    File resourcesDir = new File("src/test/resources");

    @Test
    public void readLibertyPluginConfigXml() throws IOException {
        File lpcXml = new File(resourcesDir, "xml/liberty-plugin-config.xml");
        Map<String, String> tagValues = XmlReader.readTagsFromXml(lpcXml.toURI().toString(), 
                LibertyConfigFileManager.CUSTOM_BOOTSTRAP_PREOPERTIES_XML_TAG, 
                LibertyConfigFileManager.CUSTOM_SERVER_ENV_XML_TAG);

        assertNotNull(tagValues);
        assertEquals("Did not find serverEnvFile and bootstrapPropertiesFile", 2, tagValues.size());
        
        // assertFalse(XmlReader.hasServerRoot(lpcXml.getCanonicalPath()));
        // assertFalse(LibertyUtils.isConfigXMLFile(lpcXml.getCanonicalPath()));

        // Set<String> elementNames = new HashSet<String> ();
        // elementNames.add("configFile");
        // elementNames.add("bootstrapPropertiesFile");
        // elementNames.add("serverEnv");

        // Map<String, String> values = XmlReader.getElementValues(lpcXmlPath, elementNames);
        // assertTrue(values.size() == 2, "Did not find expected number of elements in liberty-plugin-config.xml file. Expected 2, found "+values.size());

        // assertTrue(values.containsKey("configFile"), "Expected configFile element not found");
        // assertTrue(values.get("configFile").equals("/user/sample-project/src/main/liberty/config/server.xml"), "Expected configFile value not found. Value found: "+values.get("configFile"));

        // assertTrue(values.containsKey("bootstrapPropertiesFile"), "Expected bootstrapPropertiesFile element not found");
        // assertTrue(values.get("bootstrapPropertiesFile").equals("/user/sample-project/src/main/liberty/config/bootstrap.properties"), "Expected bootstrapPropertiesFile value not found. Value found: "+values.get("configFile"));

        // assertFalse(values.containsKey("serverEnv"), "Unexpected serverEnv element found");
    }
}
