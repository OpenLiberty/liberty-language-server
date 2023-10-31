/*******************************************************************************
* Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.tools.langserver.lemminx.codeactions;

import java.util.List;
import java.util.logging.Logger;

import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

public class RemoveTrailingSlash implements ICodeActionParticipant {
    private static final Logger LOGGER = Logger.getLogger(RemoveTrailingSlash.class.getName());

    public static final String CODEACTION_TITLE = "Remove trailing slash to specify file.";
    @Override
    public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
        Diagnostic diagnostic = request.getDiagnostic();
        DOMDocument document = request.getDocument();
        try {
            String locationText = document.findNodeAt(document.offsetAt(diagnostic.getRange().getEnd())).getAttribute("location");
            String replaceText = "location=\"" + locationText.substring(0, locationText.length()-1) + "\"";
            codeActions.add(CodeActionFactory.replace(CODEACTION_TITLE, diagnostic.getRange(), replaceText, document.getTextDocument(), diagnostic));
        } catch (Exception e) {
            LOGGER.warning("Could not generate code action for removing trailing slash." + e);
        }
    }
}
