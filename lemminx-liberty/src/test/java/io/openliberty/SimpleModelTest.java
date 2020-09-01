package io.openliberty;

import static io.openliberty.LibertyLemminxTestsUtils.createDOMDocument;
import static io.openliberty.LibertyLemminxTestsUtils.cleanUpServerXML;


import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLValidationSettings;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lsp4j.Diagnostic;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SimpleModelTest {

    private XMLLanguageService languageService;
    private static Logger log = Logger.getLogger("Logger");

    @Before
    public void setUp() throws IOException {
        languageService = new XMLLanguageService();
    }

    @After
    public void tearDown() throws InterruptedException, ExecutionException, URISyntaxException {
        languageService.dispose();
        languageService = null;
        cleanUpServerXML();
    }

    @Test(timeout = 10000)
    public void testFeatureDiagnostic() throws IOException, URISyntaxException {

        DOMDocument document = createDOMDocument("/invalid-feature.xml", languageService); 
        List<Diagnostic> diagnosticsList = languageService.doDiagnostics(document, () -> {}, new XMLValidationSettings());
        for (Diagnostic diag : diagnosticsList) {
            log.info(diag.toString());
        }
    }
}
