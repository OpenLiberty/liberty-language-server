package io.microshed.libertyls;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;


public class LibertyLanguageServer implements LanguageServer {

    private static final Logger LOGGER = Logger.getLogger(LibertyLanguageServer.class.getName());

    private final WorkspaceService workspaceService;
    private final TextDocumentService textDocumentService;

    private LanguageClient languageClient;


    public LibertyLanguageServer() {
        // Workspace service handles workspace settings changes and calls update settings. 
        workspaceService = new LibertyWorkspaceService(this);
        textDocumentService = new LibertyTextDocumentService(this);
    }


    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        LOGGER.info("Initializing Liberty Language server");

        ServerCapabilities serverCapabilities = new ServerCapabilities();
        InitializeResult initializeResult = new InitializeResult(serverCapabilities);
        return CompletableFuture.completedFuture(initializeResult);
    }

    public synchronized void updateSettings(Object initializationOptionsSettings) {
        if (initializationOptionsSettings == null) {
            return;
        }
        // TODO: else update settings
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void exit() {
        System.exit(0);
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return this.textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return this.workspaceService;
    }

    public LanguageClient getLanguageClient() {
        return this.languageClient;
    }

    public void setLanguageClient(LanguageClient languageClient) {
        this.languageClient = languageClient;
    }
}
