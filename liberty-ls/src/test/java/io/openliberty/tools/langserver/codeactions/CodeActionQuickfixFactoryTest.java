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
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public abstract class CodeActionQuickfixFactoryTest extends AbstractLibertyLanguageServerTest {

    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration AWAIT_POLL_INTERVAL = Duration.ofMillis(5);


    /**
     * @param fileName inside src/test/resources/workspace/diagnostic/ folder
     * @return
     * @throws FileNotFoundException
     */
    protected TextDocumentIdentifier initAndLaunchDiagnostic(String fileName) throws FileNotFoundException {
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

    protected void assertEqualCodeAction(CodeAction expected, CodeAction actual){
        assertEquals(CodeActionKind.QuickFix, actual.getKind());
        assertEquals(expected.getDiagnostics(),actual.getDiagnostics());
        assertEquals(expected.getTitle(),actual.getTitle());
    }

    protected List<CodeAction> populateCodeActions(List<Diagnostic> diagnostics, String... codeActionTitles){
       return Arrays.stream(codeActionTitles).sequential().map(codeActionTitle->{
            CodeAction codeAction = new CodeAction(codeActionTitle);
            codeAction.setDiagnostics(diagnostics);
            codeAction.setKind(CodeActionKind.QuickFix);
            return codeAction;
        }).collect(Collectors.toList());
    }
}
