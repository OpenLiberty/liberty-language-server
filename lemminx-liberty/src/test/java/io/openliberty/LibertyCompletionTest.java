package io.openliberty;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lsp4j.TextEdit;

import static org.eclipse.lemminx.XMLAssert.c;

import org.junit.jupiter.api.Test;

public class LibertyCompletionTest {

    @Test
    public void testCompletionItem() throws BadLocationException {
        String serverXML = "<server description=\"Sample Liberty server\">\r\n" + //
                "|<featureManager>\r\n" + //
                "<feature>jaxrs-2.1</feature>\r\n" + //
                "</featureManager>\r\n" + //
                "</server>";

        TextEdit apTextEdit = new TextEdit();
        apTextEdit.setNewText("<applicationManager></applicationManager>");
        XMLAssert.testCompletionFor(serverXML, null, "test/server.xml", 156,
                c("applicationManager", apTextEdit, "applicationManager",
                        "Properties that control the behavior of the application manager\n\nSource: server.xsd",
                        "plaintext"));
    }
}
