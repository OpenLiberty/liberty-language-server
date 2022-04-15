package io.openliberty.tools.langserver.model.serverenvfile;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Position;

import io.openliberty.tools.langserver.ls.LibertyTextDocument;

public class ServerEnvEntryInstance {
    private ServerEnvKeyInstance serverEnvKeyInstance;
    private ServerEnvValueInstance serverEnvValueInstance;
    private String line;
    private LibertyTextDocument textDocumentItem;

    public ServerEnvEntryInstance(String entryLine, LibertyTextDocument textDocumentItem) {
        this.line = entryLine;
        this.textDocumentItem = textDocumentItem;
        int equalsIndex = line.indexOf("=");
        String serverEnvKeyInstanceString;
        String serverEnvValueInstanceString;
        if (equalsIndex != -1) {
            serverEnvKeyInstanceString = line.substring(0, equalsIndex);
            serverEnvValueInstanceString = line.substring(equalsIndex+1);
        } else {
            serverEnvKeyInstanceString = line;
            serverEnvValueInstanceString = null;
        }
        this.serverEnvKeyInstance = new ServerEnvKeyInstance(serverEnvKeyInstanceString, this, textDocumentItem);
        this.serverEnvValueInstance = new ServerEnvValueInstance(serverEnvValueInstanceString, serverEnvKeyInstance, textDocumentItem);
    }

    private boolean isOnEntryKey(Position position) {
        return position.getCharacter() <= serverEnvKeyInstance.getEndPosition();
    }

    public CompletableFuture<Hover> getHover(Position position) {
        if (isOnEntryKey(position)) {
            return serverEnvKeyInstance.getHover();
        } else {
            return serverEnvValueInstance.getHover();
        }
    }
}
