package io.openliberty.tools.langserver.hover;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;

import io.openliberty.tools.langserver.AbstractLibertyLanguageServerTest;
import io.openliberty.tools.langserver.LibertyLanguageServer;

public class AbstractHoverTest extends AbstractLibertyLanguageServerTest {
    
    File resourcesDir = new File("src/test/resources/workspace/diagnostic/src/main/liberty/config");

    protected CompletableFuture<Hover> getHover(LibertyLanguageServer lls, int position, String fileURI) {
        HoverParams hoverParams = new HoverParams(new TextDocumentIdentifier(fileURI), new Position(0, position));
        return lls.getTextDocumentService().hover(hoverParams);
    }
}
