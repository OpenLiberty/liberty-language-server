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
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CodeActionParticipant {

    private LibertyTextDocumentService libertyTextDocumentService;
    private LibertyLanguageServer libertyLanguageServer;

    public CodeActionParticipant(LibertyTextDocumentService libertyTextDocumentService, LibertyLanguageServer libertyLanguageServer) {
        this.libertyTextDocumentService = libertyTextDocumentService;
        this.libertyLanguageServer = libertyLanguageServer;
    }

    /**
     *
     * @param params
     * @return
     */
    public CompletableFuture<List<Either<Command, CodeAction>>> getCodeActions(CodeActionParams params) {
        CodeActionContext context = params.getContext();
        if (context != null) {
            List<Either<Command, CodeAction>> codeActions = new ArrayList<>();
            List<String> codeActionsType = context.getOnly();
            // code action type is coming null from vscode
            if (codeActionsType == null || codeActionsType.contains(CodeActionKind.QuickFix)) {
                codeActions.addAll(computeQuickfixes(params));
            }
            return CompletableFuture.supplyAsync(() -> codeActions);
        } else {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
    }

    private List<Either<Command, CodeAction>> computeQuickfixes(CodeActionParams params) {
        // compute InvalidPropertyValueQuickfix
        // need to change this logic when we have more types of quick fixes
        return new ArrayList<>(new InvalidPropertyValueQuickfix(libertyTextDocumentService, libertyLanguageServer)
                .apply(params));
    }
}
