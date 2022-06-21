package io.openliberty.tools.langserver.completion;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;

import io.openliberty.tools.langserver.ls.LibertyTextDocument;
import io.openliberty.tools.langserver.model.propertiesfile.PropertiesEntryInstance;
import io.openliberty.tools.langserver.utils.ParserFileHelperUtil;

public class LibertyPropertiesCompletionProvider {
    private LibertyTextDocument textDocumentItem;

    public LibertyPropertiesCompletionProvider(LibertyTextDocument textDocumentItem) {
        this.textDocumentItem = textDocumentItem;
    }

    public CompletableFuture<List<CompletionItem>> getCompletions(Position position) {
        String line = new ParserFileHelperUtil().getLine(textDocumentItem, position);
        return new PropertiesEntryInstance(line, textDocumentItem).getCompletions(position);
    }
}
