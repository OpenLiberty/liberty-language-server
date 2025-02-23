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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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

    /**
     * retrieve list of completion items for a property key
     *
     * @param textDocumentItem text document
     * @param position         current position, used to compute key
     * @param prefix           prefix value to trigger completion with
     * @return list of string of completion item names
     */
    @Override
    protected List<String> retrieveCompletionValues(TextDocumentItem textDocumentItem,
                                                    Position position, String prefix) {
        List<String> completionValues = new ArrayList<>();
        try {
            LibertyTextDocument openedDocument = libertyLanguageServer.getTextDocumentService().getOpenedDocument(textDocumentItem.getUri());
            String line = new ParserFileHelperUtil().getLine(openedDocument, position);
            PropertiesEntryInstance propertiesEntryInstance = new PropertiesEntryInstance(line, openedDocument);
            CompletableFuture<List<CompletionItem>> completions;
            // get all completions for current property key
            completions = propertiesEntryInstance.getPropertyValueInstance().getCompletions(prefix, position);
            // map text values from completion items
            completionValues = completions.thenApply(completionItems -> completionItems.stream()
                            .map(it -> it.getTextEdit().getLeft().getNewText())
                            .collect(Collectors.toList()))
                    .get();
        } catch (InterruptedException e) {
            LOGGER.severe("Interruption while computing possible properties for quickfix. Error message is " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOGGER.severe("Exception while computing possible properties for quickfix. Error message is " + e.getMessage());
        }
        return completionValues;
    }
}
