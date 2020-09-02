package io.openliberty;

import static io.openliberty.LibertyLemminxTestsUtils.createDOMDocument;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.lemminx.XMLLanguageServer;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLValidationSettings;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ServerSchemaTest {

    private XMLLanguageServer languageServer;

    @Before
    public void setUp() throws IOException {
        languageServer = new XMLLanguageServer();
    }

    @After
    public void tearDown() throws InterruptedException, ExecutionException, URISyntaxException {
        languageServer = null;
    }

    @Test
    public void testInvalidElementDiagnostic() throws IOException, URISyntaxException {
        DOMDocument document = createDOMDocument("/invalid-element.xml", languageServer.getXMLLanguageService());
        List<Diagnostic> diagnosticsList = languageServer.getXMLLanguageService().doDiagnostics(document, () -> {
        }, new XMLValidationSettings());
        assertTrue(diagnosticsList.stream().anyMatch(diag -> diag.getMessage().contains(
                "Invalid element name:\n - invalidElement\n\nOne of the following is expected:\n - feature")));
    }

    @Test
    public void testServerXSDCompletion() throws IOException, URISyntaxException {
        DOMDocument document = createDOMDocument("/invalid-element.xml", languageServer.getXMLLanguageService());
        List<CompletionItem> completionsList = languageServer.getXMLLanguageService()
                .doComplete(document, new Position(1, 0), new SharedSettings()).getItems();
        // completionsList.get(0).getLab
        assertTrue(completionsList.stream().anyMatch(completion -> (completion.getLabel().equals("applicationManager")
                && completion.getDocumentation().getRight().getValue().contains("Source: server.xsd"))));
    }

}
