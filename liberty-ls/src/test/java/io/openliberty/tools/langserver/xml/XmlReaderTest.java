package io.openliberty.tools.langserver.xml;


import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


import io.openliberty.tools.langserver.LibertyConfigFileManager;
import io.openliberty.tools.langserver.utils.XmlReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class XmlReaderTest {
    File resourcesDir = new File("src/test/resources");

    @Test
    public void readLibertyPluginConfigXml() throws IOException {
        File lpcXml = new File(resourcesDir, "xml/unix/liberty-plugin-config.xml");
        Map<String, String> tagValues = XmlReader.readTagsFromXml(lpcXml.toURI().toString(), 
                LibertyConfigFileManager.CUSTOM_BOOTSTRAP_PROPERTIES_XML_TAG, 
                LibertyConfigFileManager.CUSTOM_SERVER_ENV_XML_TAG);

        assertNotNull(tagValues);
        assertEquals(2, tagValues.size(),"Did not find custom config files");
        
        Set<String> elementNames = new HashSet<String> ();
        elementNames.add("configFile");
        elementNames.add(LibertyConfigFileManager.CUSTOM_BOOTSTRAP_PROPERTIES_XML_TAG);
        elementNames.add(LibertyConfigFileManager.CUSTOM_SERVER_ENV_XML_TAG);

        Map<String, String> values = XmlReader.getElementValues(lpcXml, elementNames);
        assertTrue(values.size() == 3,"Did not find expected number of elements in liberty-plugin-config.xml file. Expected 3, found "+values.size());

        assertTrue(values.containsKey("configFile"),"Expected configFile element not found");
        assertTrue(values.get("configFile").equals("/user/sample-project/src/main/liberty/config/server.xml"),"Expected configFile value not found. Value found: "+values.get("configFile"));

        assertTrue(values.containsKey(LibertyConfigFileManager.CUSTOM_BOOTSTRAP_PROPERTIES_XML_TAG),"Expected bootstrapPropertiesFile element not found");
        assertTrue(values.get("bootstrapPropertiesFile").equals("/user/sample-project/src/main/liberty/config/custombootstrapprops.properties"),"Expected bootstrapPropertiesFile value not found. Value found: "+values.get(LibertyConfigFileManager.CUSTOM_BOOTSTRAP_PROPERTIES_XML_TAG));

        assertTrue(values.containsKey(LibertyConfigFileManager.CUSTOM_SERVER_ENV_XML_TAG), "Expected serverEnvFile element not found");
        assertTrue(values.get(LibertyConfigFileManager.CUSTOM_SERVER_ENV_XML_TAG).equals("/user/sample-project/src/main/liberty/config/customserverenv.env"), "Expected serverEnvFile value not found. Value found: "+values.get(LibertyConfigFileManager.CUSTOM_SERVER_ENV_XML_TAG));
    }
}
