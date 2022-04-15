package io.openliberty.tools.langserver.model.serverenvfile;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;

import io.openliberty.tools.langserver.ls.LibertyTextDocument;

public class ServerEnvKeyInstance {

    private String serverEnvKey;
    private ServerEnvEntryInstance serverEnvEntryInstance;
    private LibertyTextDocument textDocumentItem;

    public ServerEnvKeyInstance(String serverEnvKeyInstanceString, ServerEnvEntryInstance serverEnvEntryInstance, LibertyTextDocument textDocumentItem) {
        this.serverEnvKey = serverEnvKeyInstanceString;
        this.serverEnvEntryInstance = serverEnvEntryInstance;
        this.textDocumentItem = textDocumentItem;
    }

    public int getEndPosition() {
        return serverEnvKey.length();
    }

    public CompletableFuture<Hover> getHover() {
        Hover hover = new Hover(new MarkupContent("plaintext", "testing hover on key"));
        return CompletableFuture.completedFuture(hover);
    }

    
}
