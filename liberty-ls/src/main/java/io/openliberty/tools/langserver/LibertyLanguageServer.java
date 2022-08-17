/*******************************************************************************
* Copyright (c) 2020, 2022 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     IBM Corporation - initial API and implementation
*******************************************************************************/
package io.openliberty.tools.langserver;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;


public class LibertyLanguageServer implements LanguageServer {

    private static final Logger LOGGER = Logger.getLogger(LibertyLanguageServer.class.getName());

    private final WorkspaceService workspaceService;
    private final LibertyTextDocumentService textDocumentService;

    private LanguageClient languageClient;


    public LibertyLanguageServer() {
        // Workspace service handles workspace settings changes and calls update settings. 
        this.textDocumentService = new LibertyTextDocumentService(this);
        this.workspaceService = new LibertyWorkspaceService(this);
    }


    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        LOGGER.info("Initializing Liberty Language server");

        ServerCapabilities serverCapabilities = createServerCapabilities();
        InitializeResult initializeResult = new InitializeResult(serverCapabilities);
        return CompletableFuture.completedFuture(initializeResult);
    }

    @Override
    public void initialized(InitializedParams params) {
        LOGGER.info("Initialized Liberty Language server");
        LanguageServer.super.initialized(params);
    }

    private ServerCapabilities createServerCapabilities() {
        ServerCapabilities capabilities = new ServerCapabilities();
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
        capabilities.setHoverProvider(Boolean.TRUE);
        capabilities.setCompletionProvider(new CompletionOptions(Boolean.TRUE, Arrays.asList("=")));
        return capabilities;
    }

    public synchronized void updateSettings(Object initializationOptionsSettings) {
        LOGGER.info("Updating settings...");
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
    public LibertyTextDocumentService getTextDocumentService() {
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
