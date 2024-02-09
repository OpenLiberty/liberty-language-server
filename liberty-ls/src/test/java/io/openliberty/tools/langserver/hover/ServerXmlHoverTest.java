package io.openliberty.tools.langserver.hover;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.TextDocumentItem;
import org.junit.Test;

import io.openliberty.tools.langserver.LibertyLanguageServer;

public class ServerXmlHoverTest extends AbstractHoverTest {
    static String entryLine;
    static CompletableFuture<Hover> hover;

    @Test
    public void testHover() throws Exception {
        // Hover over blank space
        entryLine = "    <httpEndpoint httpPort=\"${http.port}\" httpsPort=\"${not.defined}\" id=\"defaultHttpEndpoint\"/>";
        hover = getHover(entryLine, 0);
        assertEquals("", hover.get().getContents().getRight().getValue());

        // Hover over undefined variable
        entryLine = "    <httpEndpoint httpPort=\"${http.port}\" httpsPort=\"${not.defined}\" id=\"defaultHttpEndpoint\"/>";
        hover = getHover(entryLine, 65);
        assertEquals("", hover.get().getContents().getRight().getValue());

        // Hover over defined variable
        entryLine = "    <httpEndpoint httpPort=\"${http.port}\" httpsPort=\"9443\" id=\"defaultHttpEndpoint\"/>";
        hover = getHover(entryLine, 33);
        assertEquals("9080", hover.get().getContents().getRight().getValue());
    }

    private CompletableFuture<Hover> getHover(String propertyEntry, int position) throws URISyntaxException, InterruptedException, ExecutionException {
        String filename = "server.xml";
        String resourcesDir = "src/test/resources/xml/";
        File file = new File(resourcesDir, filename);
        String fileURI = file.toURI().toString();

        LibertyLanguageServer lls = initializeLanguageServer(filename, new TextDocumentItem(fileURI, LibertyLanguageServer.LANGUAGE_ID, 0, propertyEntry));
        return getHover(lls, position, fileURI);
    }
}
