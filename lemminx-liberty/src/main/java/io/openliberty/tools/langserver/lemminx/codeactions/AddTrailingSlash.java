/*******************************************************************************
* Copyright (c) 2023, 2025 IBM Corporation and others.
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

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import io.openliberty.tools.langserver.lemminx.util.ResourceBundleMappingConstants;
import io.openliberty.tools.langserver.lemminx.util.ResourceBundleUtil;
import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

public class AddTrailingSlash implements ICodeActionParticipant {
    private static final Logger LOGGER = Logger.getLogger(AddTrailingSlash.class.getName());

    public static final String FORWARD_SLASH = "/";
    public static final String BACK_SLASH = "\\";
    
    @Override
    public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
        Diagnostic diagnostic = request.getDiagnostic();
        DOMDocument document = request.getDocument();
        try {
            String fileSeparator = FORWARD_SLASH;
            String locationText = document.findNodeAt(document.offsetAt(diagnostic.getRange().getEnd())).getAttribute("location");
            String replaceText = getReplaceText(fileSeparator, locationText);
            codeActions.add(CodeActionFactory.replace(ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.TITLE_ADD_TRAILING_SLASH), diagnostic.getRange(), replaceText, document.getTextDocument(), diagnostic));
        } catch (Exception e) {
            LOGGER.warning("Could not generate code action for adding trailing slash." + e);
        }
    }

    /**
     * Gets replace text based on OS and slash usage
     * @param fileSeparator
     * @param locationText
     * @return
     */
    public static String getReplaceText(String fileSeparator, String locationText) {
        if (locationText.contains(BACK_SLASH) && locationText.contains(FORWARD_SLASH)) {
            // if using mismatched slashes, replace all with /
            locationText = locationText.replace(BACK_SLASH, FORWARD_SLASH);
        } else if (File.separator.equals(BACK_SLASH) && locationText.contains(BACK_SLASH)) {
            // if Windows and path using \, continue using it
            fileSeparator = BACK_SLASH;
        }
        String replaceText = "location=\"" + locationText + fileSeparator + "\"";
        return replaceText;
    }
}
