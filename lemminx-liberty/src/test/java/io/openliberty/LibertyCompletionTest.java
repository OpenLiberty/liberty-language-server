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

                final int TOTAL_ITEMS = 171; // total number of available completion items

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

                // would be 269 if mpConfig-1.4 was not already specified
                final int TOTAL_ITEMS = 337; // total number of available completion items

                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, TOTAL_ITEMS, jaxrsCompletion, websocket,
                                microProfileCompletion);
        }

        // Tests the
        // availability of platform completion
        @Test
        public void testPlatformCompletionItem() throws BadLocationException {
                String serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "               <platform>|</platform>", //
                        "               <feature>mpConfig-1.4</feature>", //
                        "       </featureManager>", //
                        "</server>" //
                );

                // total number of available completion items
                // 3 for javaee
                //3 for jakartaee
                //15 for microprofile
                // one for CDATA and one for <-
                final int TOTAL_ITEMS = 23;

                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, TOTAL_ITEMS);

                serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "               <platform>java|</platform>", //
                        "       </featureManager>", //
                        "</server>" //
                );
                CompletionItem javaee6Completeion = c("javaee-6.0", "javaee-6.0");
                CompletionItem javaee7Completeion = c("javaee-7.0", "javaee-7.0");
                CompletionItem javaee8Completeion = c("javaee-8.0", "javaee-8.0");

                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, 5,
                        javaee6Completeion, javaee7Completeion, javaee8Completeion);

                serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "               <platform>jakarta|</platform>", //
                        "       </featureManager>", //
                        "</server>" //
                );
                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, 5);

                serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "               <platform>micro|</platform>", //
                        "       </featureManager>", //
                        "</server>" //
                );
                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, 17);
        }

}
