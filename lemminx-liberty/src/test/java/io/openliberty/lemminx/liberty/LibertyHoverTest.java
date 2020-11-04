package io.openliberty.lemminx.liberty;

import org.junit.jupiter.api.Test;

import io.openliberty.lemminx.liberty.utils.XMLTestUtils;

import static io.openliberty.lemminx.liberty.LibertyXSDURIResolver.SERVER_XSD_RESOURCE;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import static org.eclipse.lemminx.XMLAssert.r;
import java.io.IOException;

public class LibertyHoverTest {

        static String serverXMLURI = "test/server.xml";

        @Test
        public void testFeatureHover() throws BadLocationException {

                String serverXML = XMLTestUtils.joinWithNewLine( //
                                "<server description=\"Sample Liberty server\">", //
                                "       <featureManager>", //
                                "               <feature>j|axrs-2.1</feature>", //
                                "       </featureManager>", //
                                "</server>" //
                );

                XMLAssert.assertHover(serverXML, serverXMLURI,
                                "This feature enables support for Java API for RESTful Web Services v2.1.  "
                                                + "JAX-RS annotations can be used to define web service clients and endpoints that comply with the REST architectural style. "
                                                + "Endpoints are accessed through a common interface that is based on the HTTP standard methods.",
                                r(2, 24, 2, 33));

        }

        @Test
        public void testXSDSchemaHover() throws BadLocationException, IOException {
                String serverXSDURI = SERVER_XSD_RESOURCE.getDeployedPath().toUri().toString().replace("///", "/");

                String serverXML = XMLTestUtils.joinWithNewLine( //
                                "<server description=\"Sample Liberty server\">", //
                                "       <feature|Manager>", //
                                "               <feature>jaxrs-2.1</feature>", //
                                "       </featureManager>", //
                                "</server>" //
                );

                XMLAssert.assertHover(serverXML, serverXMLURI, "Defines how the server loads features." + //
                                System.lineSeparator() + System.lineSeparator() + //
                                "Source: [server.xsd](" + serverXSDURI + ")", //
                                r(1, 8, 1, 22));
        }

}
