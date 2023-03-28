/*******************************************************************************
* Copyright (c) 2022, 2023 IBM Corporation and others.
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

package io.openliberty.tools.langserver.lemminx.codeactions;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

import io.openliberty.tools.langserver.lemminx.LibertyExtension;
import io.openliberty.tools.langserver.lemminx.util.LibertyUtils;

public class CreateFile implements ICodeActionParticipant {
    private static final Logger LOGGER = Logger.getLogger(LibertyExtension.class.getName());

    private static final String EMPTY_SERVER_CONFIG = "<server>" + System.lineSeparator() + "</server>";

    @Override
    public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
        Diagnostic diagnostic = request.getDiagnostic();
        DOMDocument document = request.getDocument();
        try {
            File parentFile = LibertyUtils.getDocumentAsFile(document).getParentFile();
            String locationValue = document.findNodeAt(document.offsetAt(diagnostic.getRange().getEnd())).getAttribute("location");
            codeActions.add(CodeActionFactory.createFile(
                "Create the missing server config file relative from this file.",
                new File(parentFile, locationValue).toURI().toString(), 
                EMPTY_SERVER_CONFIG, diagnostic));

            /* Uncomment to add option when the need is found */
            // codeActions.add(CodeActionFactory.createFile(
            //     "Create the missing server config file with location value as the absolute path.",
            //     new File(locationValue).getCanonicalPath(), 
            //     SERVER_TAGS_CONTENT, diagnostic));
        } catch (Exception e) {
            // BadLocationException not expected
            LOGGER.warning("Could not generate code action for create file: " + e);
        }
    }
}
