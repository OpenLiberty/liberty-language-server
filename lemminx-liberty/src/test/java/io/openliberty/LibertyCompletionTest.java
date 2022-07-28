package io.openliberty;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lsp4j.CompletionItem;
import org.junit.jupiter.api.Test;

import static org.eclipse.lemminx.XMLAssert.*;

public class LibertyCompletionTest {

        static String newLine = System.lineSeparator();
        static String serverXMLURI = "test/server.xml";

        // Tests the availability of completion of XML elements provided by the
        // server.xsd file
        @Test
        public void testXSDElementCompletionItem() throws BadLocationException {
                String serverXML = String.join(newLine, //
                                "<server description=\"Sample Liberty server\">", //
                                "       |<featureManager>", //
                                "               <feature>jaxrs-2.1</feature>", //
                                "       </featureManager>", //
                                "</server>" //
                );

                CompletionItem applicationManagerCompletion = c("applicationManager",
                                "<applicationManager></applicationManager>");
                CompletionItem webApplicationCompletion = c("webApplication",
                                "<webApplication location=\"\"></webApplication>");
                CompletionItem httpEndpointCompletion = c("httpEndpoint", "<httpEndpoint></httpEndpoint>");

                final int TOTAL_ITEMS = 166; // total number of available completion items

                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, TOTAL_ITEMS, applicationManagerCompletion,
                                webApplicationCompletion, httpEndpointCompletion);
        }

        // Tests the availability of completion of attributes inside XML elements
        // provided by the server.xsd file
        @Test
        public void testXSDAttributeCompletionItem() throws BadLocationException {
                String serverXML = String.join(newLine, //
                                "<server description=\"Sample Liberty server\">", //
                                "       <featureManager>", //
                                "               <feature>jaxrs-2.1</feature>", //
                                "       </featureManager>", //
                                "<httpEndpoint |></httpEndpoint>", //
                                "</server>" //
                );

                CompletionItem portCompletion = c("httpPort", "httpPort=\"\"");
                CompletionItem enabledCompletion = c("enabled", "enabled=\"true\"");

                final int TOTAL_ITEMS = 15; // total number of available completion items

                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, TOTAL_ITEMS, portCompletion,
                                enabledCompletion);
        }

        // Tests the
        // availability of feature completion
        @Test
        public void testFeatureCompletionItem() throws BadLocationException {
                String serverXML = String.join(newLine, //
                                "<server description=\"Sample Liberty server\">", //
                                "       <featureManager>", //
                                "               <feature>|</feature>", //
                                "               <feature>mpConfig-1.4</feature>", //
                                "       </featureManager>", //
                                "</server>" //
                );

                CompletionItem jaxrsCompletion = c("jaxrs-2.1", "jaxrs-2.1");
                CompletionItem websocket = c("websocket-1.1", "websocket-1.1");
                CompletionItem microProfileCompletion = c("microProfile-2.2", "microProfile-2.2");

                // would be 227 if mpConfig-1.4 was not already specified
                final int TOTAL_ITEMS = 226; // total number of available completion items

                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, TOTAL_ITEMS, jaxrsCompletion, websocket,
                                microProfileCompletion);
        }

}
