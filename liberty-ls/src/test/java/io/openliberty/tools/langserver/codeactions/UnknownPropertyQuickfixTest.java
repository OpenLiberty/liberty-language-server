package io.openliberty.tools.langserver.codeactions;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static io.openliberty.tools.langserver.diagnostic.LibertyPropertiesDiagnosticService.ERROR_CODE_UNKNOWN_PROPERTY_VALUE;
import static org.junit.Assert.assertEquals;

public class UnknownPropertyQuickfixTest extends AbstractQuickFixTest {


    @Test
    public void testReturnCodeActionForQuickfixForBootStrapProperties() throws FileNotFoundException, InterruptedException, ExecutionException {
        TextDocumentIdentifier textDocumentIdentifier = initAnLaunchDiagnostic("bootstrap.properties");

        Diagnostic diagnostic = lastPublishedDiagnostics.getDiagnostics().get(0);

        Range expectedRange = new Range(new Position(0, 34), new Position(0, 38));
        List<Diagnostic> diagnostics = new ArrayList<>();
        Diagnostic diagnostic1 = new Diagnostic(expectedRange, "The value `DEVd` is not valid for the property `com.ibm.ws.logging.console.format`.");
        diagnostic1.setCode(ERROR_CODE_UNKNOWN_PROPERTY_VALUE);
        diagnostics.add(diagnostic1);
        diagnostic.setSource("Liberty Config Language Server");
        diagnostics.addAll(lastPublishedDiagnostics.getDiagnostics());

        CompletableFuture<List<Either<Command, CodeAction>>> codeActionsCompletableFuture = retrieveCodeActions(textDocumentIdentifier, diagnostic);
        checkRetrievedCodeAction(textDocumentIdentifier, diagnostic, codeActionsCompletableFuture, expectedRange, "DEV");
    }


    @Test
    public void testReturnCodeActionForQuickfixForServerEnv() throws FileNotFoundException, InterruptedException, ExecutionException {
        TextDocumentIdentifier textDocumentIdentifier = initAnLaunchDiagnostic("server.env");

        Diagnostic diagnostic = lastPublishedDiagnostics.getDiagnostics().get(0);

        Range expectedRange = new Range(new Position(0, 27), new Position(0, 30));
        List<Diagnostic> diagnostics = new ArrayList<>();
        Diagnostic diagnostic1 = new Diagnostic(expectedRange, "The value `asdf` is not valid for the variable `WLP_LOGGING_CONSOLE_FORMAT`.");
        diagnostic1.setCode(ERROR_CODE_UNKNOWN_PROPERTY_VALUE);
        diagnostics.add(diagnostic1);
        diagnostic.setSource("Liberty Config Language Server");
        diagnostics.addAll(lastPublishedDiagnostics.getDiagnostics());

        CompletableFuture<List<Either<Command, CodeAction>>> codeActionsCompletableFuture = retrieveCodeActions(textDocumentIdentifier, diagnostic);
        checkRetrievedCodeAction(textDocumentIdentifier, diagnostic, codeActionsCompletableFuture, expectedRange, "SIMPLE");
    }

    private void checkRetrievedCodeAction(TextDocumentIdentifier textDocumentIdentifier, Diagnostic diagnostic, CompletableFuture<List<Either<Command, CodeAction>>> codeActions, Range expectedRange, String expectedCodeActionText)
            throws InterruptedException, ExecutionException {
        TextEdit textEdit = retrieveTextEdit(textDocumentIdentifier, diagnostic, codeActions);
        Range range = textEdit.getRange();
        assertEquals(expectedRange, range);
        assertEquals(expectedCodeActionText, textEdit.getNewText());
    }

}
