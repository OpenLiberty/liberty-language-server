package io.openliberty.tools.langserver.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
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
        assertEquals("Did not find custom config files", 2, tagValues.size());
        
        Set<String> elementNames = new HashSet<String> ();
        elementNames.add("configFile");
        elementNames.add("bootstrapPropertiesFile");
        elementNames.add("serverEnvFile");

        Map<String, String> values = XmlReader.getElementValues(lpcXml, elementNames);
        assertTrue("Did not find expected number of elements in liberty-plugin-config.xml file. Expected 3, found "+values.size(), values.size() == 3);

        assertTrue("Expected configFile element not found", values.containsKey("configFile"));
        assertTrue("Expected configFile value not found. Value found: "+values.get("configFile"), values.get("configFile").equals("/user/sample-project/src/main/liberty/config/server.xml"));

        assertTrue("Expected bootstrapPropertiesFile element not found", values.containsKey("bootstrapPropertiesFile"));
        assertTrue("Expected bootstrapPropertiesFile value not found. Value found: "+values.get("bootstrapPropertiesFile"), values.get("bootstrapPropertiesFile").equals("/user/sample-project/src/main/liberty/config/custombootstrapprops.properties"));

        assertTrue("Expected serverEnvFile element not found", values.containsKey("serverEnvFile"));
        assertTrue("Expected serverEnvFile value not found. Value found: "+values.get("serverEnvFile"), values.get("serverEnvFile").equals("/user/sample-project/src/main/liberty/config/customserverenv.env"));
    }
}
