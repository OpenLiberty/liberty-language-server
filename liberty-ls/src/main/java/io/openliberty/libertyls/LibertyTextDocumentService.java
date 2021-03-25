package io.openliberty.libertyls;

import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.services.TextDocumentService;

import io.openliberty.libertyls.ls.LibertyTextDocument;
import io.openliberty.libertyls.ls.LibertyTextDocuments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentHighlightParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

public class LibertyTextDocumentService implements TextDocumentService {

    private static final Logger LOGGER = Logger.getLogger(LibertyTextDocumentService.class.getName());

    private final LibertyLanguageServer libertyLanguageServer;

    // Text document manager that maintains the contexts of the text documents
    private final LibertyTextDocuments<LibertyTextDocument> documents = new LibertyTextDocuments<LibertyTextDocument>();

    public LibertyTextDocumentService(LibertyLanguageServer libertyls) {
        this.libertyLanguageServer = libertyls;
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        LibertyTextDocument document = documents.onDidOpenTextDocument(params);
        String uri = document.getUri();
        validate(Arrays.asList(uri));
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        LibertyTextDocument document = documents.onDidChangeTextDocument(params);
        String uri = document.getUri();
        validate(Arrays.asList(uri));
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        documents.onDidCloseTextDocument(params);
        String uri = params.getTextDocument().getUri();
        libertyLanguageServer.getLanguageClient()
            .publishDiagnostics(new PublishDiagnosticsParams(uri, new ArrayList<Diagnostic>()));
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        validate(Arrays.asList(params.getTextDocument().getUri()));
    }
    
    /**
     * Run diagnostic validation all files
     */
    private void validateAll() {
        List<String> allDocs = documents.all().stream().map(doc -> doc.getUri()).collect(Collectors.toList());
        validate(allDocs);
    }


    private void validate(List<String> uris) {
        if (uris.isEmpty()) {
            return;
        }
        LOGGER.info("Running validation for: " + uris.toString());
    }
}
