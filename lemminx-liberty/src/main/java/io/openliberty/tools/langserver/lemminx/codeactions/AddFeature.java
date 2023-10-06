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

import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.extensions.contentmodel.utils.XMLGenerator;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lemminx.utils.XMLBuilder;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

import io.openliberty.tools.langserver.lemminx.LibertyDiagnosticParticipant;
import io.openliberty.tools.langserver.lemminx.services.FeatureService;
import io.openliberty.tools.langserver.lemminx.util.LibertyConstants;

public class AddFeature implements ICodeActionParticipant {

    // TODO: Support auto indentation
    private static final String NEW_LINE = System.lineSeparator();
    public static final String FEATURE_FORMAT = "<feature>%s</feature>";
    public static final String FEATUREMANAGER_FORMAT = 
            "<featureManager>" + NEW_LINE + FEATURE_FORMAT + NEW_LINE + "</featureManager>";

    @Override
    public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
        Diagnostic diagnostic = request.getDiagnostic();
        DOMDocument document = request.getDocument();   
        try {
            String insertText = NEW_LINE + FEATUREMANAGER_FORMAT;
            DOMNode insertNode = document.getDocumentElement().getFirstChild();
            Position insertPosition = XMLPositionUtility.createRange(insertNode.getStart(), insertNode.getEnd(), document).getStart();
            for (DOMNode node : document.getDocumentElement().getChildren()) {
                if (LibertyConstants.FEATURE_MANAGER_ELEMENT.equals(node.getNodeName())) {
                    insertText = NEW_LINE;
                    insertNode = node.getLastChild();
                    insertPosition = XMLPositionUtility.createRange(insertNode.getStart(), insertNode.getEnd(), document).getEnd();

                    // document.getDocumentElement().getEndTagCloseOffset();
                    // DOMElement de = (DOMElement) node;
                    // Position start = document.positionAt(de.getStartTagCloseOffset()+1);
                    // Position stop = document.positionAt(de.getEndTagCloseOffset());
                    // Range targetRange = new Range(start, stop);
                    // XMLGenerator generator = request.getXMLGenerator();                    
                    String indent = request.getXMLGenerator().getWhitespacesIndent();
                    int level = document.positionAt(((DOMElement)node).getStartTagOpenOffset()).getCharacter() / indent.length();
                    for (int i = 0; i < level+1; i++) {
                        insertText += indent;
                    }
                    insertText += FEATURE_FORMAT;
                    break;
                }
            }

            // String insertStrRequired = null;
            // String insertStrAll = null;

            // XMLGenerator generator = request.getXMLGenerator();
            // if (!request.canSupportResolve()) {
            //     insertStrRequired = generator.generateMissingElements(request.getComponent(ContentModelManager.class), document.getDocumentElement(), true);
            //     insertStrAll = generator.generateMissingElements(request.getComponent(ContentModelManager.class), document.getDocumentElement(), false);
            // }

            // getAllEnabledBy would return all transitive features, but is too many to offer without a filter
            Set<String> featureCandidates = FeatureService.getInstance().getFeatureListGraph().get(diagnostic.getSource()).getEnabledBy();
            for (String feature : featureCandidates) {
                String title = "Add feature " + feature;
                codeActions.add(CodeActionFactory.insert(
                        title, insertPosition, String.format(insertText, feature), document.getTextDocument(), diagnostic));
            }
        } catch (Exception e) {

        }
    }
}
