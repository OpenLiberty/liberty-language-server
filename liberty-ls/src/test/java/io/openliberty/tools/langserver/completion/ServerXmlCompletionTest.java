package io.openliberty.tools.langserver.completion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.Test;

import io.openliberty.tools.langserver.LibertyLanguageServer;

public class ServerXmlCompletionTest extends AbstractCompletionTest {

    static CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions;
    static List<CompletionItem> completionItems;

    @Test
    public void completionRecognition() throws Exception {
        // failed trigger -- needs {
        completions = getCompletion("    <include location=\"$", new Position(0, 24));
        completionItems = completions.get().getLeft();
        assertEquals(0, completionItems.size());

        // failed trigger -- cursor position still at $
        completions = getCompletion("    <include location=\"${", new Position(0, 24));
        completionItems = completions.get().getLeft();
        assertEquals(0, completionItems.size());

        // trigger
        completions = getCompletion("    <include location=\"${", new Position(0, 25));
        completionItems = completions.get().getLeft();
        assertEquals(2, completionItems.size());
    }

    @Test
    public void completionAmongstText() throws Exception {
        // the cursor is at ${, therefore all completion items are available without filtration
        completions = getCompletion("    <include location=\"${abc", new Position(0, 25));
        completionItems = completions.get().getLeft();
        assertEquals(2, completionItems.size());
    }

    @Test
    public void filtration() throws Exception {
        completions = getCompletion("    <include location=\"${a", new Position(0, 26));
        completionItems= completions.get().getLeft();
        assertEquals(1, completionItems.size());
        assertEquals("abc", completionItems.get(0).getLabel());
        assertEquals("abc}", completionItems.get(0).getInsertText());

        completions = getCompletion("    <include location=\"${http.", new Position(0, 30));
        completionItems = completions.get().getLeft();
        assertEquals(1, completionItems.size());
        assertEquals("http.port", completionItems.get(0).getLabel());
        assertEquals("http.port}", completionItems.get(0).getInsertText());
    }

    @Test
    public void multipleBrackets() throws Exception {
        completions = getCompletion("${ab${http}", new Position(0, 4));
        completionItems= completions.get().getLeft();
        assertEquals(1, completionItems.size());
        assertEquals("abc", completionItems.get(0).getLabel());

        completions = getCompletion("${ab${http}", new Position(0, 10));
        completionItems= completions.get().getLeft();
        assertEquals(1, completionItems.size());
        assertEquals("http.port", completionItems.get(0).getLabel());

        completions = getCompletion("${ab${http}", new Position(0, 11));
        completionItems= completions.get().getLeft();
        assertEquals(0, completionItems.size());
    }

    protected CompletableFuture<Either<List<CompletionItem>, CompletionList>> getCompletion(String enteredText, Position position) throws URISyntaxException, InterruptedException, ExecutionException, IOException {
        String filename = "server.xml";
        String resourcesDir = "src/test/resources/xml/";
        File file = new File(resourcesDir, filename);
        String fileURI = file.toURI().toString();
        
        LibertyLanguageServer lls = initializeLanguageServer(filename, new TextDocumentItem(fileURI, LibertyLanguageServer.LANGUAGE_ID, 0, enteredText));
        return getCompletionFor(lls, position, fileURI);
    }
}
