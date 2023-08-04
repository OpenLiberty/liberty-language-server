package io.openliberty;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.openliberty.tools.langserver.lemminx.util.XmlReader;

public class XmlReaderTest {
    File resourcesDir = new File("src/test/resources");

    @Test
    public void readEmptyXml() {
        File emptyXml = new File(resourcesDir, "empty_server.xml");
        assertFalse(XmlReader.hasServerRoot(emptyXml));
    }

    @Test
    public void readServerXml() {
        File sampleServerXml = new File(resourcesDir, "sample/server.xml");
        assertTrue(XmlReader.hasServerRoot(sampleServerXml));
    }

    @Test
    public void readLibertyPluginConfigXml() {
        File lpcXml = new File(resourcesDir, "sample/liberty-plugin-config.xml");
        assertFalse(XmlReader.hasServerRoot(lpcXml));

        Set<String> elementNames = new HashSet<String> ();
        elementNames.add("configFile");
        elementNames.add("bootstrapPropertiesFile");
        elementNames.add("serverEnv");

        Map<String, String> values = XmlReader.getElementValues(lpcXml, elementNames);
        assertTrue(values.size() == 2, "Did not find expected number of elements in liberty-plugin-config.xml file. Expected 2, found "+values.size());

        assertTrue(values.containsKey("configFile"), "Expected configFile element not found");
        assertTrue(values.get("configFile").equals("/user/sample-project/src/main/liberty/config/server.xml"), "Expected configFile value not found. Value found: "+values.get("configFile"));

        assertTrue(values.containsKey("bootstrapPropertiesFile"), "Expected bootstrapPropertiesFile element not found");
        assertTrue(values.get("bootstrapPropertiesFile").equals("/user/sample-project/src/main/liberty/config/bootstrap.properties"), "Expected bootstrapPropertiesFile value not found. Value found: "+values.get("configFile"));

        assertFalse(values.containsKey("serverEnv"), "Unexpected serverEnv element found");
    }
}
