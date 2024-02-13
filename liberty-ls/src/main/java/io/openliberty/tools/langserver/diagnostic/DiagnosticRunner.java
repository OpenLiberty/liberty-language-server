/*******************************************************************************
* Copyright (c) 2022 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     IBM Corporation
*******************************************************************************/
package io.openliberty.tools.langserver.diagnostic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;

import io.openliberty.tools.langserver.LibertyConfigFileManager;
import io.openliberty.tools.langserver.LibertyLanguageServer;
import io.openliberty.tools.langserver.ls.LibertyTextDocument;
import io.openliberty.tools.langserver.utils.PropertiesValidationResult;

public class DiagnosticRunner {
    private LibertyLanguageServer libertyLanguageServer;
    private LibertyPropertiesDiagnosticService libertyPropertiesDiagnosticService;
    
    private static final Logger LOGGER = Logger.getLogger(DiagnosticRunner.class.getName());

    public DiagnosticRunner(LibertyLanguageServer libertyLanguageServer){
        this.libertyLanguageServer = libertyLanguageServer;
        libertyPropertiesDiagnosticService = new LibertyPropertiesDiagnosticService();
    }

    public void compute(DidOpenTextDocumentParams params) {
        String text = params.getTextDocument().getText();
        computeDiagnostics(text, params.getTextDocument());
    }

    public void compute(DidChangeTextDocumentParams params) {
        String text = params.getContentChanges().get(0).getText();
        computeDiagnostics(text, libertyLanguageServer.getTextDocumentService().getOpenedDocument(params.getTextDocument().getUri()));
    }

    public void computeDiagnostics(String text, TextDocumentItem documentItem) {
        String uri = documentItem.getUri();
        CompletableFuture.runAsync(() -> {
            LibertyTextDocument openedDocument = libertyLanguageServer.getTextDocumentService().getOpenedDocument(uri);
            List<Diagnostic> diagnostics = new ArrayList<>();
            Map<String, PropertiesValidationResult> propertiesErrors = libertyPropertiesDiagnosticService.compute(text, openedDocument);
            if (LibertyConfigFileManager.isServerXml(openedDocument)) {
                Diagnostic testDiagnostic = new Diagnostic(new Range(new Position(0, 0), new Position(0,1)), "This is our proof of concept diagnostic.");
                diagnostics.add(testDiagnostic);
            }
            diagnostics.addAll(libertyPropertiesDiagnosticService.convertToLSPDiagnostics(propertiesErrors));
            libertyLanguageServer.getLanguageClient().publishDiagnostics(new PublishDiagnosticsParams(uri, diagnostics));
        });
    }
}
