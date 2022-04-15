package io.openliberty.tools.langserver.model.serverenvfile;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;

import io.openliberty.tools.langserver.ls.LibertyTextDocument;

public class ServerEnvValueInstance {

    private String serverEnvValue;
    private ServerEnvKeyInstance key;
    private LibertyTextDocument textDocumentItem;

    public ServerEnvValueInstance(String serverEnvValueInstanceString, ServerEnvKeyInstance serverEnvKeyInstance, LibertyTextDocument textDocumentItem) {
        this.serverEnvValue = serverEnvValueInstanceString;
        this.key = serverEnvKeyInstance;
        this.textDocumentItem = textDocumentItem;
    }

    public ServerEnvKeyInstance getKey() {
        return key;
    }

    public CompletableFuture<Hover> getHover() {
        Hover hover = new Hover(new MarkupContent("plaintext", "testing hover on value"));
        return CompletableFuture.completedFuture(hover);
    }
}
