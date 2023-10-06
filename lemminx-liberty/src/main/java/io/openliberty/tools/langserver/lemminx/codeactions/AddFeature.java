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

import org.eclipse.lemminx.commons.CodeActionFactory;
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

import io.openliberty.tools.langserver.lemminx.services.FeatureService;
import io.openliberty.tools.langserver.lemminx.util.LibertyConstants;

public class AddFeature implements ICodeActionParticipant {
    public static final String FEATURE_FORMAT = "\n<feature>%s</feature>";
    public static final String FEATUREMANAGER_FORMAT = 
            "\n\t<featureManager>" + 
            "\n\t\t<feature>%s</feature>"  +
            "\n\t</featureManager>";

    @Override
    public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
        Diagnostic diagnostic = request.getDiagnostic();
        DOMDocument document = request.getDocument();   
        try {
            String insertText = "";
            DOMNode insertNode = null;
            String indent = request.getXMLGenerator().getWhitespacesIndent();

            for (DOMNode node : document.getDocumentElement().getChildren()) {
                if (LibertyConstants.FEATURE_MANAGER_ELEMENT.equals(node.getNodeName())) {
                    // If featureManager is empty, add new feature after featureManager start tag.
                    // Otherwise, add feature after last feature child.
                    insertNode = node.getLastChild();
                    insertText = (insertNode == null) ? FEATURE_FORMAT.replace("\n", "\n\t") : FEATURE_FORMAT;
                    insertNode = (insertNode == null) ? node : insertNode;
                    break;
                }
            }

            int insertEndOffset = (insertNode == null) ? ((DOMElement)document.getDocumentElement()).getStartTagCloseOffset()+1 : insertNode.getEnd();
            // featureManager not found
            if (insertNode == null) {
                insertNode = document.getDocumentElement();
                insertText = FEATUREMANAGER_FORMAT;
            }

            Range nodeRange = XMLPositionUtility.createRange(insertNode.getStart(), insertEndOffset, document);
            insertText = IndentUtil.formatText(insertText, indent, nodeRange.getStart().getCharacter());

            // getAllEnabledBy would return all transitive features, but is too many to offer without a filter
            Set<String> featureCandidates = FeatureService.getInstance().getFeatureListGraph().get(diagnostic.getSource()).getEnabledBy();
            for (String feature : featureCandidates) {
                String title = "Add feature " + feature;
                codeActions.add(CodeActionFactory.insert(
                        title, nodeRange.getEnd(), String.format(insertText, feature), document.getTextDocument(), diagnostic));
            }
        } catch (Exception e) {

        }
    }
}
