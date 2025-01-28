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
import io.openliberty.tools.langserver.utils.ParserFileHelperUtil;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class CodeActionQuickfixFactory {

    protected LibertyTextDocumentService libertyTextDocumentService;
    protected LibertyLanguageServer libertyLanguageServer;

    protected CodeActionQuickfixFactory(LibertyTextDocumentService libertyTextDocumentService, LibertyLanguageServer libertyLanguageServer) {
        this.libertyTextDocumentService = libertyTextDocumentService;
        this.libertyLanguageServer = libertyLanguageServer;
    }

    public List<Either<Command, CodeAction>> apply(CodeActionParams params) {
        TextDocumentItem openedDocument = libertyTextDocumentService.getOpenedDocument(params.getTextDocument().getUri());
        List<Diagnostic> diagnostics = params.getContext().getDiagnostics();
        List<Either<Command, CodeAction>> res = new ArrayList<>();
        for (Diagnostic diagnostic : diagnostics) {
            if (diagnostic.getCode() != null && getErrorCode().equals(diagnostic.getCode().getLeft())) {
                String line = new ParserFileHelperUtil().getLine(new LibertyTextDocument(openedDocument), diagnostic.getRange().getStart().getLine());
                if (line != null) {
                    List<String> possibleProperties = retrieveCompletionValues(openedDocument, diagnostic.getRange().getStart());
                    for (String mostProbableProperty : possibleProperties) {
                        res.add(Either.forRight(createCodeAction(params, diagnostic, mostProbableProperty)));
                    }
                }
            }
        }
        return res;
    }

    protected CodeAction createCodeAction(CodeActionParams params, Diagnostic diagnostic, String possibleProperty) {
        CodeAction codeAction = new CodeAction("Replace value with " + possibleProperty);
        codeAction.setDiagnostics(Collections.singletonList(diagnostic));
        codeAction.setKind(CodeActionKind.QuickFix);
        Map<String, List<TextEdit>> changes = new HashMap<>();
        TextEdit textEdit = new TextEdit(diagnostic.getRange(), possibleProperty);
        changes.put(params.getTextDocument().getUri(), List.of(textEdit));
        codeAction.setEdit(new WorkspaceEdit(changes));
        return codeAction;
    }

    protected abstract List<String> retrieveCompletionValues(TextDocumentItem textDocumentItem, Position position);

    protected abstract String getErrorCode();
}
