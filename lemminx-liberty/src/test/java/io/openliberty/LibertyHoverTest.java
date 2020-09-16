package io.openliberty;

import org.junit.jupiter.api.Test;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;

import static org.eclipse.lemminx.XMLAssert.r;
import org.eclipse.lemminx.services.XMLLanguageService;

public class LibertyHoverTest {

    @Test
    public void testFeatureHover() throws BadLocationException {

        String serverXML = "<server description=\"Sample Liberty server\">\r\n" + //
                "<featureManager>\r\n" + //
                "<feature>j|axrs-2.1</feature>\r\n" + //
                "</featureManager>\r\n" + //
                "</server>";

        XMLAssert.assertHover(new XMLLanguageService(), serverXML, null, "test/server.xml",
                "This feature enables support for Java API for RESTful Web Services v2.1.  "
                        + "JAX-RS annotations can be used to define web service clients and endpoints that comply with the REST architectural style. "
                        + "Endpoints are accessed through a common interface that is based on the HTTP standard methods.",
                r(2, 9, 2, 18));

    }

}
