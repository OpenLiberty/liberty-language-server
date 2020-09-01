package io.openliberty;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.TextDocumentItem;

public interface LibertyLemminxTestsUtils {

    public static DOMDocument createDOMDocument(String resourcePath, XMLLanguageService languageService)
            throws IOException, URISyntaxException {
        return org.eclipse.lemminx.dom.DOMParser.getInstance().parse(
                new TextDocument(createTextDocumentItem(resourcePath)), languageService.getResolverExtensionManager());
    }

    public static TextDocumentItem createTextDocumentItem(String resourcePath) throws IOException, URISyntaxException {
        URI uri = LibertyLemminxTestsUtils.class.getResource(resourcePath).toURI();
        File file = new File(uri);

        Path targetPath = Paths.get(file.getParent(), "server.xml");
        Files.copy(file.toPath(), targetPath);

        String contents = new String(Files.readAllBytes(targetPath));

        return new TextDocumentItem(targetPath.toUri().toString(), "xml", 1, contents);
    }

    public static boolean completionContains(List<CompletionItem> completionItems, String searchString) {
        return completionItems.stream().map(CompletionItem::getLabel).anyMatch(label -> label.contains(searchString));
    }

    public static void cleanUpServerXML() throws URISyntaxException {
        URI uri = LibertyLemminxTestsUtils.class.getResource("/server.xml").toURI();
        File file = new File(uri);
        if (file.exists()) {
            file.delete();
        }
    }

}