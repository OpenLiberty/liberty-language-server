package io.openliberty.tools.langserver.hover;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Position;

import io.openliberty.tools.langserver.ls.LibertyTextDocument;
import io.openliberty.tools.langserver.model.serverenvfile.ServerEnvEntryInstance;
import io.openliberty.tools.langserver.utils.ParserFileHelperUtil;

public class LibertyServerEnvHoverProvider {
    private LibertyTextDocument textDocumentItem;

    public LibertyServerEnvHoverProvider(LibertyTextDocument textDocumentItem) {
        this.textDocumentItem = textDocumentItem;
    }

    public CompletableFuture<Hover> getHover(Position position) {
        int line = position.getLine();
        String entryLine = new ParserFileHelperUtil().getLine(textDocumentItem, line);
        return new ServerEnvEntryInstance(entryLine, textDocumentItem).getHover(position);
    }
}
