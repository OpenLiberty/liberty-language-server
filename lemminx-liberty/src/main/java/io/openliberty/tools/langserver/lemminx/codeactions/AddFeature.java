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
*     IBM Corporation - initial API and implementation
*******************************************************************************/
package io.openliberty.tools.langserver.lemminx.codeactions;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

import io.openliberty.tools.langserver.lemminx.services.LibertyProjectsManager;
import io.openliberty.tools.langserver.lemminx.util.LibertyConstants;

public class AddFeature implements ICodeActionParticipant {

    /** This code action adresses 3 main situations:
     *      1) Add a feature to an existing empty featureManager
     *      2) Add a feature to an existing featureManager with children
     *      3) Add a feature and new featureManager
     *  
     *  To calculate where to insert, each scenario will use a reference point to calculate range
     *      1) The startTag of the featureManager
     *      2) The last child of the featureManager
     *      3) The startTag of the server.xml
     */
    public static final String FEATURE_FORMAT = "<feature>%s</feature>";
    public static final String FEATUREMANAGER_FORMAT = 
            "\n\t<featureManager>"+ 
            "\n\t\t<feature>%s</feature>"+
            "\n\t</featureManager>";

    @Override
    public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
        Diagnostic diagnostic = request.getDiagnostic();
        DOMDocument document = request.getDocument();
        TextDocument textDocument = document.getTextDocument();
        String insertText = "";
        int referenceRangeStart = 0;
        int referenceRangeEnd = 0;

        for (DOMNode node : document.getDocumentElement().getChildren()) {
            if (LibertyConstants.FEATURE_MANAGER_ELEMENT.equals(node.getNodeName())) {
                DOMNode lastChild = node.getLastChild();
                if (lastChild == null || !lastChild.hasChildNodes()) {
                    // Situation 1
                    insertText = "\n\t" + FEATURE_FORMAT;
                    DOMElement featureManager = (DOMElement) node;
                    referenceRangeStart = featureManager.getStartTagOpenOffset();
                    referenceRangeEnd = featureManager.getStartTagCloseOffset()+1;
                } else {
                    // Situation 2
                    insertText = "\n" + FEATURE_FORMAT;
                    referenceRangeStart = lastChild.getStart();
                    referenceRangeEnd = lastChild.getEnd();
                }
                break;
            }
        }
        // Situation 3
        if (insertText.isEmpty()) {
            insertText = FEATUREMANAGER_FORMAT;
            DOMElement server = document.getDocumentElement();
            referenceRangeStart = server.getStart();
            referenceRangeEnd = server.getStartTagCloseOffset()+1;
        }
        Range referenceRange = XMLPositionUtility.createRange(referenceRangeStart, referenceRangeEnd, document);

        try {
            String indent = request.getXMLGenerator().getWhitespacesIndent();
            insertText = IndentUtil.formatText(insertText, indent, referenceRange.getStart().getCharacter());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        // getAllEnabledBy would return all transitive features but typically offers too much
        Set<String> featureCandidates = LibertyProjectsManager.getInstance()
                .getWorkspaceFolder(document.getDocumentURI())
                .getFeatureListGraph().get(diagnostic.getSource()).getEnabledBy();

        for (String feature : featureCandidates) {
            String title = "Add feature " + feature;
            codeActions.add(CodeActionFactory.insert(
                    title, referenceRange.getEnd(), String.format(insertText, feature), textDocument, diagnostic));
        }
    }
}
