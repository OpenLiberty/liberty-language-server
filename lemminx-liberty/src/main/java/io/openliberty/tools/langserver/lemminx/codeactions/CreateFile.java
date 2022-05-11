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
*     IBM Corporation - initial API and implementation
*******************************************************************************/

package io.openliberty.tools.langserver.lemminx.codeactions;

import java.io.File;
import java.net.URI;
import java.util.List;

import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.services.extensions.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.IComponentProvider;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;

import io.openliberty.tools.langserver.lemminx.LibertyDiagnosticParticipant;

public class CreateFile implements ICodeActionParticipant {
    private static final String CREATE_CONFIG_FILE_TITLE = "Create missing config file with <server> tags included";
    private static final String SERVER_TAGS_CONTENT = "<server>\n\n</server>";

    @Override
    public void doCodeAction(Diagnostic diagnostic, Range range, DOMDocument document, List<CodeAction> codeActions,
            SharedSettings sharedSettings, IComponentProvider componentProvider) {
        String fileURI;

        if (diagnostic.getCode().getLeft().equals(LibertyDiagnosticParticipant.MISSING_FILE_CODE)) {
            String message = diagnostic.getMessage();
            fileURI = message.substring(message.indexOf("\n")+1);
            codeActions.add(CodeActionFactory.createFile(CREATE_CONFIG_FILE_TITLE, fileURI, SERVER_TAGS_CONTENT, diagnostic));
        } else {
            try {
                // attempt to extract location value from same node in Diagnostic range
                String docURIString = document.getDocumentURI().replace(File.separator, "/");
                DOMNode includeNode = document.findNodeAt(document.offsetAt(range.getEnd()));
                fileURI = includeNode.getAttribute("location").replace(File.separator, "/");
                fileURI = URI.create(docURIString.substring(0, docURIString.lastIndexOf("/")+1)).resolve(fileURI).normalize().toString();
                codeActions.add(CodeActionFactory.createFile(CREATE_CONFIG_FILE_TITLE, fileURI, SERVER_TAGS_CONTENT, diagnostic));
            } catch (Exception e) {
                // do nothing
            }
        }
    }
}
