package io.openliberty.tools.langserver.codeactions;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
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
import static org.junit.Assert.assertFalse;

public class UnknownPropertyQuickfixTest extends AbstractQuickFixTest {


    @Test
    public void testReturnCodeActionForQuickfixForBootStrapProperties() throws FileNotFoundException, InterruptedException, ExecutionException {
        TextDocumentIdentifier textDocumentIdentifier = initAnLaunchDiagnostic("bootstrap.properties");


        Range expectedRange = new Range(new Position(0, 34), new Position(0, 38));
        List<Diagnostic> diagnostics = new ArrayList<>();
        Diagnostic diagnostic1 = new Diagnostic(expectedRange, "The value `DEVd` is not valid for the property `com.ibm.ws.logging.console.format`.");
        diagnostic1.setCode(ERROR_CODE_UNKNOWN_PROPERTY_VALUE);
        diagnostic1.setSeverity(DiagnosticSeverity.Error);
        diagnostic1.setSource("Liberty Config Language Server");
        diagnostics.add(diagnostic1);

        CompletableFuture<List<Either<Command, CodeAction>>> codeActionsCompletableFuture = retrieveCodeActions(textDocumentIdentifier, lastPublishedDiagnostics.getDiagnostics().get(0));
        List<CodeAction> expectedCodeActions=populateCodeActions(diagnostics,
                CodeActionKind.QuickFix,
                "Replace value with DEV","Replace value with SIMPLE",
                "Replace value with JSON","Replace value with TBASIC");
        checkRetrievedCodeAction(textDocumentIdentifier, diagnostics, codeActionsCompletableFuture, expectedRange, expectedCodeActions);
    }


    @Test
    public void testReturnCodeActionForQuickfixForServerEnv() throws FileNotFoundException, ExecutionException, InterruptedException {
        TextDocumentIdentifier textDocumentIdentifier = initAnLaunchDiagnostic("server.env");

        Diagnostic diagnostic = lastPublishedDiagnostics.getDiagnostics().get(0);

        Range expectedRange = new Range(new Position(0, 29), new Position(0, 32));
        List<Diagnostic> diagnostics = new ArrayList<>();
        Diagnostic diagnostic1 = new Diagnostic(expectedRange, "The value `abc` is not valid for the variable `WLP_LOGGING_CONSOLE_LOGLEVEL`.");
        diagnostic1.setCode(ERROR_CODE_UNKNOWN_PROPERTY_VALUE);
        diagnostic1.setSeverity(DiagnosticSeverity.Error);
        diagnostic1.setSource("Liberty Config Language Server");
        diagnostics.add(diagnostic1);
        CompletableFuture<List<Either<Command, CodeAction>>> codeActionsCompletableFuture = retrieveCodeActions(textDocumentIdentifier, diagnostic);
        List<CodeAction> expectedCodeActions=populateCodeActions(diagnostics,
                CodeActionKind.QuickFix,
                "Replace value with AUDIT","Replace value with INFO",
                "Replace value with WARNING","Replace value with ERROR","Replace value with OFF");
        checkRetrievedCodeAction(textDocumentIdentifier, diagnostics, codeActionsCompletableFuture, expectedRange, expectedCodeActions);
    }

    private void checkRetrievedCodeAction(TextDocumentIdentifier textDocumentIdentifier, List<Diagnostic> diagnostics, CompletableFuture<List<Either<Command, CodeAction>>> codeActions, Range expectedRange, List<CodeAction> expectedCodeActions)
            throws InterruptedException, ExecutionException {
        int index = 0;
        List<Either<Command, CodeAction>> actualCodeActionsEither = codeActions.get();
        assertEquals("number of code actions is different", expectedCodeActions.size(), actualCodeActionsEither.size());
        for (Either<Command, CodeAction> codeActionEither : actualCodeActionsEither) {
            CodeAction codeAction = codeActionEither.getRight();
            List<TextEdit> createdChanges = codeAction.getEdit().getChanges().get(textDocumentIdentifier.getUri());
            assertFalse(createdChanges.isEmpty());
            TextEdit textEdit = createdChanges.get(0);
            Range range = textEdit.getRange();
            assertEquals(expectedRange, range);
            CodeAction expectedCodeAction = expectedCodeActions.get(index);
            assertEqualCodeAction(expectedCodeAction, codeAction);
            index++;
        }

    }

}