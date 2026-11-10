package io.openliberty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import io.openliberty.tools.langserver.lemminx.services.SettingsService;

@ExtendWith(MockitoExtension.class)
public class LibertyDeploymentDescriptorCodeActionParticipantTest {

    static final String[] STUB_FILES = { "cachespec.xml", "ibm-application-bnd.xml", "ibm-application-ext.xml",
            "ibm-ejb-jar-bnd.xml", "ibm-ejb-jar-ext.xml", "ibm-managed-bean-bnd.xml", "ibm-web-bnd.xml",
            "ibm-web-ext.xml", "ibm-ws-bnd.xml" };

    @Mock
    SettingsService settingsService;

    private List<CodeAction> getCodeActions(String text, String fileName) throws Exception {
        Diagnostic diagnostic = new Diagnostic(new Range(), "A diagnostic of the document.");
        return getCodeActions(text, fileName, new CodeActionContext(Collections.singletonList(diagnostic)));
    }

    private static String uriOf(String fileName) {
        return new File("src/test/resources/sample/test/WEB-INF", fileName).toURI().toString();
    }

    private List<CodeAction> getCodeActions(String text, String fileName, CodeActionContext context) throws Exception {
        String uri = uriOf(fileName);
        XMLLanguageService languageService = new XMLLanguageService();
        languageService.initializeIfNeeded();
        TextDocument textDocument = new TextDocument(text, uri);
        DOMDocument document = DOMParser.getInstance().parse(textDocument,
                languageService.getResolverExtensionManager());
        languageService.setDocumentProvider(u -> document);
        return languageService.doCodeActions(context, new Range(),
                document, new SharedSettings(), () -> {});
    }

    @Test
    public void emptyDescriptorGetsStubCodeAction() throws Exception {
        try (MockedStatic<SettingsService> settings = Mockito.mockStatic(SettingsService.class)) {
            settings.when(SettingsService::getInstance).thenReturn(settingsService);
            Mockito.lenient().when(settingsService.getCurrentLocale()).thenReturn(Locale.getDefault());
            Mockito.lenient().when(settingsService.getVariablesForServerXml(Mockito.any())).thenReturn(new Properties());

            for (String fileName : STUB_FILES) {
                List<CodeAction> actions = getCodeActions("", fileName);
                assertEquals(1, actions.size(), "Expected one code action for an empty " + fileName);
                TextDocumentEdit edit = actions.get(0).getEdit().getDocumentChanges().get(0).getLeft();
                List<TextEdit> edits = edit.getEdits();
                assertEquals(1, edits.size());
                assertNotNull(edits.get(0).getNewText());
                assertTrue(edits.get(0).getNewText().contains("<"), "Stub for " + fileName + " should be XML");
            }
        }
    }

    @Test
    public void stubsHaveNoDiagnostics() throws Exception {
        try (MockedStatic<SettingsService> settings = Mockito.mockStatic(SettingsService.class)) {
            settings.when(SettingsService::getInstance).thenReturn(settingsService);
            Mockito.lenient().when(settingsService.getCurrentLocale()).thenReturn(Locale.US);
            Mockito.lenient().when(settingsService.getLatestRuntimeVersion()).thenReturn(null);
            Mockito.lenient().when(settingsService.getVariablesForServerXml(Mockito.any())).thenReturn(new Properties());

            for (String fileName : STUB_FILES) {
                String stub = getCodeActions("", fileName).get(0).getEdit().getDocumentChanges().get(0).getLeft()
                        .getEdits().get(0).getNewText();
                XMLAssert.testDiagnosticsFor(stub, null, null, uriOf(fileName));
            }
        }
    }

    @Test
    public void noStubCodeActionWhenNotEmptyOrUnknownFile() throws Exception {
        try (MockedStatic<SettingsService> settings = Mockito.mockStatic(SettingsService.class)) {
            settings.when(SettingsService::getInstance).thenReturn(settingsService);
            Mockito.lenient().when(settingsService.getCurrentLocale()).thenReturn(Locale.getDefault());
            Mockito.lenient().when(settingsService.getVariablesForServerXml(Mockito.any())).thenReturn(new Properties());

            assertTrue(getCodeActions("<web-bnd/>", "ibm-web-bnd.xml").isEmpty(), "File is not empty");
            assertTrue(getCodeActions("", "pom.xml").isEmpty(), "No stub for this file name");
            assertTrue(getCodeActions("", "server.xml").isEmpty(), "No stub for server.xml");
        }
    }

    @Test
    public void noStubCodeActionWithoutDiagnostic() throws Exception {
        try (MockedStatic<SettingsService> settings = Mockito.mockStatic(SettingsService.class)) {
            settings.when(SettingsService::getInstance).thenReturn(settingsService);
            Mockito.lenient().when(settingsService.getCurrentLocale()).thenReturn(Locale.getDefault());
            Mockito.lenient().when(settingsService.getVariablesForServerXml(Mockito.any())).thenReturn(new Properties());

            assertTrue(getCodeActions("", "ibm-web-bnd.xml", new CodeActionContext()).isEmpty(),
                    "A request without a diagnostic gets no stub");
        }
    }
}
