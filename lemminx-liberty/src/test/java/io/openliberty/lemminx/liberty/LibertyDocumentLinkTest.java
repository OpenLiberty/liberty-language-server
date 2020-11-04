package io.openliberty.lemminx.liberty;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lsp4j.DocumentLink;
import org.junit.jupiter.api.Test;

import static org.eclipse.lemminx.XMLAssert.dl;
import static org.eclipse.lemminx.XMLAssert.r;

import io.openliberty.lemminx.liberty.utils.XMLTestUtils;

public class LibertyDocumentLinkTest {

    static String serverXMLURI = "test/server.xml";

    @Test
    public void testDocumentLink() {
        String serverXML = XMLTestUtils.joinWithNewLine( // ,
                "<server description=\"Sample Liberty server\">", //
                "       <include location=\"foo.xml\"></include>", //
                "       <featureManager>", //
                "               <feature>j|axrs-2.1</feature>", //
                "       </featureManager>", //
                "</server>" //
        );

        DocumentLink expectedResult = dl(r(1, 25, 1, 34), "test/foo.xml");

        XMLAssert.testDocumentLinkFor(serverXML, serverXMLURI, expectedResult);
    }
}
