/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package io.openliberty.tools.langserver.codeactions;

import io.openliberty.tools.langserver.LibertyLanguageServer;
import io.openliberty.tools.langserver.LibertyTextDocumentService;
import io.openliberty.tools.langserver.ls.LibertyTextDocument;
import io.openliberty.tools.langserver.model.propertiesfile.PropertiesEntryInstance;
import io.openliberty.tools.langserver.utils.ParserFileHelperUtil;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static io.openliberty.tools.langserver.diagnostic.LibertyPropertiesDiagnosticService.ERROR_CODE_INVALID_PROPERTY_VALUE;

public class InvalidPropertyValueQuickfix extends CodeActionQuickfixFactory {

    private static final Logger LOGGER = Logger.getLogger(InvalidPropertyValueQuickfix.class.getName());

    public InvalidPropertyValueQuickfix(LibertyTextDocumentService libertyTextDocumentService, LibertyLanguageServer libertyLanguageServer) {
        super(libertyTextDocumentService, libertyLanguageServer);
    }

    protected String getErrorCode() {
        return ERROR_CODE_INVALID_PROPERTY_VALUE;
    }

    @Override
    protected List<String> retrieveCompletionValues(TextDocumentItem textDocumentItem,
                                                    Position position) {
        try {
            LibertyTextDocument openedDocument = libertyLanguageServer.getTextDocumentService().getOpenedDocument(textDocumentItem.getUri());
            String line = new ParserFileHelperUtil().getLine(openedDocument, position);
            PropertiesEntryInstance propertiesEntryInstance = new PropertiesEntryInstance(line, openedDocument);
            // get all completions for current property key
            CompletableFuture<List<CompletionItem>> completions = propertiesEntryInstance.getPropertyValueInstance().getCompletions("", position);
            return completions.thenApply(completionItems -> completionItems.stream().map(it -> it.getTextEdit().getLeft().getNewText())
                            .collect(Collectors.toList()))
                    .get();
        } catch (InterruptedException e) {
            LOGGER.severe("Interruption while computing possible properties for quickfix. Error message is " + e.getMessage());
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        } catch (ExecutionException e) {
            LOGGER.severe("Exception while computing possible properties for quickfix. Error message is " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
