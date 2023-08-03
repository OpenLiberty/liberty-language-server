package io.openliberty;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.junit.jupiter.api.Test;

import io.openliberty.tools.langserver.lemminx.services.XmlReader;

public class StaxReaderTest {
    static XmlReader reader = XmlReader.getInstance();

    File resourcesDir = new File("src/test/resources");

    @Test
    public void readEmptyXml() throws IOException, XMLStreamException {
        File emptyXml = new File(resourcesDir, "empty_server.xml");
        assertFalse(reader.hasServerRoot(emptyXml));
    }

    @Test
    public void readServerXml() throws IOException, XMLStreamException {
        File sampleServerXml = new File(resourcesDir, "sample/server.xml");
        assertTrue(reader.hasServerRoot(sampleServerXml));
    }

    @Test
    public void readLibertPluginConfigXml() throws IOException, XMLStreamException {
        File lpcXml = new File(resourcesDir, "sample/liberty-plugin-config.xml");
        assertFalse(reader.hasServerRoot(lpcXml));
    }
}
