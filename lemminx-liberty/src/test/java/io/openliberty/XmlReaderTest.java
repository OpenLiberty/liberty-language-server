package io.openliberty;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.junit.jupiter.api.Test;

import io.openliberty.tools.langserver.lemminx.util.XmlReader;

public class XmlReaderTest {
    File resourcesDir = new File("src/test/resources");

    @Test
    public void readEmptyXml() throws IOException, XMLStreamException {
        File emptyXml = new File(resourcesDir, "empty_server.xml");
        assertFalse(XmlReader.hasServerRoot(emptyXml));
    }

    @Test
    public void readServerXml() throws IOException, XMLStreamException {
        File sampleServerXml = new File(resourcesDir, "sample/server.xml");
        assertTrue(XmlReader.hasServerRoot(sampleServerXml));
    }

    @Test
    public void readLibertPluginConfigXml() throws IOException, XMLStreamException {
        File lpcXml = new File(resourcesDir, "sample/liberty-plugin-config.xml");
        assertFalse(XmlReader.hasServerRoot(lpcXml));
    }
}
