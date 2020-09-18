package io.openliberty;

import org.junit.jupiter.api.Test;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;

import static org.eclipse.lemminx.XMLAssert.r;
import org.eclipse.lemminx.services.XMLLanguageService;

public class LibertyHoverTest {

        static String newLine = System.getProperty("line.separator");
        static String serverXMLURI = "test/server.xml";

        @Test
        public void testFeatureHover() throws BadLocationException {

                String serverXML = String.join(newLine, //
                                "<server description=\"Sample Liberty server\">", //
                                "       <featureManager>", //
                                "               <feature>j|axrs-2.1</feature>", //
                                "       </featureManager>", //
                                "</server>" //
                );

                XMLAssert.assertHover(serverXML, "test/server.xml",
                                "This feature enables support for Java API for RESTful Web Services v2.1.  "
                                                + "JAX-RS annotations can be used to define web service clients and endpoints that comply with the REST architectural style. "
                                                + "Endpoints are accessed through a common interface that is based on the HTTP standard methods.",
                                r(2, 24, 2, 33));

        }

}
