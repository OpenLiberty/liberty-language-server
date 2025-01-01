package io.openliberty.tools.langserver.codeactions;

import io.openliberty.tools.langserver.AbstractLibertyLanguageServerTest;
import org.awaitility.core.ConditionFactory;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public abstract class AbstractQuickFixTest extends AbstractLibertyLanguageServerTest {

    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration AWAIT_POLL_INTERVAL = Duration.ofMillis(5);

    protected TextEdit retrieveTextEdit(TextDocumentIdentifier textDocumentIdentifier, Diagnostic diagnostic, CompletableFuture<List<Either<Command, CodeAction>>> codeActions)
            throws InterruptedException, ExecutionException {
        CodeAction codeAction = codeActions.get().get(0).getRight();
        assertEquals(diagnostic, codeAction.getDiagnostics().get(0));
        assertEquals(CodeActionKind.QuickFix, codeAction.getKind());
        List<TextEdit> createdChanges = codeAction.getEdit().getChanges().get(textDocumentIdentifier.getUri());
        assertFalse(createdChanges.isEmpty());
        return createdChanges.get(0);
    }

    /**
     * @param fileName inside src/test/resources/workspace/diagnostic/ folder
     * @return
     * @throws FileNotFoundException
     */
    protected TextDocumentIdentifier initAnLaunchDiagnostic(String fileName) throws FileNotFoundException {
        File f = new File("src/test/resources/workspace/codeaction/src/main/liberty/config/" + fileName);
        FileInputStream streamWithContentToTest = new FileInputStream(f);
        return initAndLaunchDiagnostic(f, streamWithContentToTest);
    }

    protected TextDocumentIdentifier initAndLaunchDiagnostic(File f, InputStream streamWithContentToTest) {
        libertyLanguageServer = initializeLanguageServerWithFileUriString(streamWithContentToTest, f.toURI().toString());

        TextDocumentIdentifier textDocumentIdentifier = new TextDocumentIdentifier(f.toURI().toString());
        DidSaveTextDocumentParams params = new DidSaveTextDocumentParams(textDocumentIdentifier);
        libertyLanguageServer.getTextDocumentService().didSave(params);

        createAwait().untilAsserted(() -> assertNotNull(lastPublishedDiagnostics));
        await().timeout(AWAIT_TIMEOUT).untilAsserted(() -> assertEquals(1, lastPublishedDiagnostics.getDiagnostics().size()));
        return textDocumentIdentifier;
    }

    protected CompletableFuture<List<Either<Command, CodeAction>>> retrieveCodeActions(TextDocumentIdentifier textDocumentIdentifier, Diagnostic diagnostic) {
        CodeActionContext context = new CodeActionContext(lastPublishedDiagnostics.getDiagnostics(), Collections.singletonList(CodeActionKind.QuickFix));
        return libertyLanguageServer.getTextDocumentService().codeAction(new CodeActionParams(textDocumentIdentifier, diagnostic.getRange(), context));
    }

    protected ConditionFactory createAwait() {
        return await().pollDelay(Duration.ZERO).pollInterval(AWAIT_POLL_INTERVAL).timeout(AWAIT_TIMEOUT);
    }
}
