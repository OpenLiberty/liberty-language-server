package io.openliberty;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.junit.jupiter.api.Test;

import io.openliberty.tools.langserver.lemminx.services.LibertyProjectsManager;

import static org.eclipse.lemminx.XMLAssert.r;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LibertyDiagnosticTest {

    static String newLine = System.lineSeparator();
    static String serverXMLURI = "test/server.xml";

    @Test
    public void testFeatureDuplicateDiagnostic() {
        String serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "               <feature>jaxrs-2.1</feature>", //
                "               <feature>jaxrs-2.1</feature>", //
                "               <feature>jsonp-1.1</feature>", //
                "               <!-- <feature>comment</feature> -->", //
                "               <feature>jsonp-1.1</feature>", //
                "       </featureManager>", //
                "</server>" //
        );

        Diagnostic dup1 = new Diagnostic();
        dup1.setRange(r(3, 24, 3, 33));
        dup1.setMessage("ERROR: jaxrs-2.1 is already included.");

        Diagnostic dup2 = new Diagnostic();
        dup2.setRange(r(6, 24, 6, 33));
        dup2.setMessage("ERROR: jsonp-1.1 is already included.");

        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI, dup1, dup2);
    }

    @Test
    public void testInvalidFeatureDiagnostic() {
        String serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "               <feature>jaxrs-2.1</feature>", //
                "               <feature>jax</feature>", //
                "               <feature>jsonp-1.1</feature>", //
                "               <!-- <feature>comment</feature> -->", //
                "               <feature>invalid</feature>", //
                "       </featureManager>", //
                "</server>" //
        );
        Diagnostic invalid1 = new Diagnostic();
        invalid1.setRange(r(3, 24, 3, 27));
        invalid1.setMessage("ERROR: The feature \"jax\" does not exist.");

        Diagnostic invalid2 = new Diagnostic();
        invalid2.setRange(r(6, 24, 6, 31));
        invalid2.setMessage("ERROR: The feature \"invalid\" does not exist.");

        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI, invalid1, invalid2);
    }

    @Test
    public void testScanServerXmlInclude() throws IOException {
        // LibertyWorkspace must be initialized
        List<WorkspaceFolder> initList = new ArrayList<WorkspaceFolder>();
        initList.add(new WorkspaceFolder("src"));
        LibertyProjectsManager.getInstance().setWorkspaceFolders(initList);

        String serverXML = String.join(newLine, //
                "<server description=\"default server\">", //
                "    <include optional=\"true\" location=\"./resources/empty_server.xml\"/>", //
                "    <include optional=\"false\" location=\"MISSING FILE\"/>", //
                "</server>"
        );
        Diagnostic location1 = new Diagnostic();
        location1.setRange(r(1, 4, 1, 70)); // range is whole include element
        location1.setMessage("INFO: Detected config resource " + new File("src/test/resources/empty_server.xml").getCanonicalPath());
        XMLAssert.testDiagnosticsFor(serverXML, null, null, "src/test/server.xml", location1);

        assertTrue(LibertyProjectsManager.getInstance().getLibertyWorkspaceFolders().get(0).hasConfigFile(new File("src/test/resources/empty_server.xml").getCanonicalPath()));
        assertFalse(LibertyProjectsManager.getInstance().getLibertyWorkspaceFolders().get(0).hasConfigFile("MISSING FILE"));
    }
}