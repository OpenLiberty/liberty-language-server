package io.openliberty;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import io.openliberty.tools.langserver.lemminx.util.DocumentUtil;

public class DocumentUtilTest {
    File resourcesDir = new File("target/test-classes");

    @Test
    public void readEmptyXml() throws Exception {
        File sampleXsd = new File(resourcesDir, "sample.xsd");

        Document doc = DocumentUtil.getDocument(sampleXsd);

        List<Element> anyAttr = DocumentUtil.getElementsByName(doc, "anyAttribute");
        assertNotNull(anyAttr, "List is unexpectedly null.");
        assertTrue(anyAttr.size() == 3, "Expected 3 anyAttributes elements but found: "+anyAttr.size());

        List<Element> props = DocumentUtil.getElementsByName(doc, "extraProperties");
        assertNotNull(props, "List is unexpectedly null.");
        assertTrue(props.size() == 1, "Expected extraProperties element not found.");

        DocumentUtil.removeExtraneousAnyAttributeElements(sampleXsd);

        doc = DocumentUtil.getDocument(sampleXsd);
        props = DocumentUtil.getElementsByName(doc, "extraProperties");

        assertNotNull(props, "List is unexpectedly null.");
        assertTrue(props.size() == 1, "Expected extraProperties element not found.");

        anyAttr = DocumentUtil.getElementsByName(doc, "anyAttribute");
        assertNotNull(anyAttr, "List is unexpectedly null.");
        assertTrue(anyAttr.size() == 1, "Expected 1 anyAttributes element but found: "+anyAttr.size());

    }

    //@Test
    public void updateCachedSchema() throws Exception {
        // When uploading a new server schema, put a copy of the schema file in src/test/resources and uncomment this
        // test to get the updated schema without the extaneous anyAttribute elements. Then copy the updated schema 
        // from target/test-classes to src/main/resources/schema/xsd/liberty and remove it from src/test/resources.
        File sampleXsd = new File(resourcesDir, "server-cached-24.0.0.10.xsd");
        DocumentUtil.removeExtraneousAnyAttributeElements(sampleXsd);
    }

}
