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
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

import io.openliberty.tools.langserver.lemminx.services.FeatureService;
import io.openliberty.tools.langserver.lemminx.util.LibertyConstants;

public class AddFeature implements ICodeActionParticipant {

    // This method is still a work in progress.
    @Override
    public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
        Diagnostic diagnostic = request.getDiagnostic();
        DOMDocument document = request.getDocument();   
        try {
            List<DOMNode> nodes = document.getDocumentElement().getChildren();
            DOMNode featureManagerNode = null;

            for (DOMNode node : nodes) {
                if (LibertyConstants.FEATURE_MANAGER_ELEMENT.equals(node.getNodeName())) {
                    featureManagerNode = node;
                    break;
                }
            }
            
            Position insertPosition;
            String insertText;
            DOMNode locationNode;
            if (featureManagerNode == null) {
                locationNode = document.getDocumentElement().getFirstChild();
                insertPosition = XMLPositionUtility.createRange(locationNode.getStart(), locationNode.getEnd(), document).getStart();
                insertText = "<featureManager>\n<feature>asdf</feature>\n</featureManager>";

            } else {
                locationNode = featureManagerNode.getLastChild();
                insertPosition = XMLPositionUtility.createRange(locationNode.getStart(), locationNode.getEnd(), document).getEnd();
                insertText = "\n\t<feature>microProfile-5.0</feature>";
            }

            String configElement = diagnostic.getSource();
            if (configElement.equals("ssl")) {
                FeatureService.getInstance().getFeatureListGraph().get("ssl").getEnablers();
            }
            Set<String> featureCandidates = FeatureService.getInstance().getFeatureListGraph().get(configElement).getEnablers();
            codeActions.add(CodeActionFactory.insert("This is a test: " + featureCandidates, insertPosition, insertText, document.getTextDocument(), diagnostic));
            for (String feature : featureCandidates) {
                String title = "Add feature " + feature;
                codeActions.add(CodeActionFactory.insert(title, insertPosition, feature, 
                        document.getTextDocument(), diagnostic));
            }
        } catch (Exception e) {

        }
    }
}
