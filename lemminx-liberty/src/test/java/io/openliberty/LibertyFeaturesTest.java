package io.openliberty;

import static io.openliberty.LibertyLemminxTestsUtils.createServerXMLDOMDocument;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.lemminx.XMLLanguageServer;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLValidationSettings;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LibertyFeaturesTest {

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
    public void testFeatureDiagnostic() throws IOException, URISyntaxException {
        DOMDocument document = createServerXMLDOMDocument("/invalid-feature.xml", languageServer.getXMLLanguageService());
        List<Diagnostic> diagnosticsList = languageServer.getXMLLanguageService().doDiagnostics(document, () -> {
        }, new XMLValidationSettings());
        assertTrue(diagnosticsList.stream()
                .anyMatch(diag -> diag.getMessage().contains("ERROR: The mpConfi feature does not exist.")));
    }

    @Test
    public void testFeatureHover() throws IOException, URISyntaxException {
        DOMDocument document = createServerXMLDOMDocument("/simple-server.xml", languageServer.getXMLLanguageService());
        String hover = languageServer.getXMLLanguageService()
                .doHover(document, new Position(2, 18), new SharedSettings()).getContents().getRight().getValue();
        assertNotNull(hover);
    }

}
