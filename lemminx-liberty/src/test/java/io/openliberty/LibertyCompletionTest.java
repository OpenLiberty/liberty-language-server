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

                // would be 344 based on 24.0.0.9
                final int TOTAL_ITEMS = 344; // total number of available completion items

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
                CompletionItem javaee6Completion = c("javaee-6.0", "javaee-6.0");
                CompletionItem javaee7Completion = c("javaee-7.0", "javaee-7.0");
                CompletionItem javaee8Completion = c("javaee-8.0", "javaee-8.0");

                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, 5,
                        javaee6Completion, javaee7Completion, javaee8Completion);

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

                serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "               <platform>javaee-8.0</platform>", //
                        "               <platform>ja|</platform>", //
                        "       </featureManager>", //
                        "</server>" //
                );
                //here result should be 5 because it should show only jakartaee related completion as javaee is already added
                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, 5);

                serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "               <platform>microProfile-6.0</platform>", //
                        "               <platform>|</platform>", //
                        "       </featureManager>", //
                        "</server>" //
                );
                //here result should be 8 because it should show only jakartaee and javaee related completion as microprofile is already added
                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, 8);
        }

}
