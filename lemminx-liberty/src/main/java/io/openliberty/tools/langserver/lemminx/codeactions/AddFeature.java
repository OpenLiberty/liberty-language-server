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

import java.util.ArrayList;
import java.util.Collections;
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

import io.openliberty.tools.langserver.lemminx.data.FeatureListNode;
import io.openliberty.tools.langserver.lemminx.services.FeatureService;
import io.openliberty.tools.langserver.lemminx.services.LibertyProjectsManager;
import io.openliberty.tools.langserver.lemminx.services.LibertyWorkspace;
import io.openliberty.tools.langserver.lemminx.util.LibertyConstants;

public class AddFeature implements ICodeActionParticipant {
    Logger LOGGER = Logger.getLogger(AddFeature.class.getName());

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
        // getAllEnabledBy would return all transitive features but typically offers too much
        LibertyWorkspace ws = LibertyProjectsManager.getInstance().getWorkspaceFolder(document.getDocumentURI());    
        
        if (ws == null) {
            LOGGER.warning("Could not add quick fix for missing feature because could not find Liberty workspace for document: "+document.getDocumentURI());
            return;
        }

        FeatureListNode flNode = ws.getFeatureListGraph().get(diagnostic.getSource());
        if (flNode == null) {
            // FeatureListGraph must be empty. Initialize it with the default cached feature list.
            if (ws.getFeatureListGraph().isEmpty()) {
                FeatureService.getInstance().loadCachedFeaturesList(ws);
                flNode = ws.getFeatureListGraph().get(diagnostic.getSource());
            } 
            
            if (flNode == null) {
                LOGGER.warning("Could not add quick fix for missing feature for config element due to missing information in the feature list: "+diagnostic.getSource());
                return;
            }
        }

        Set<String> featureCandidates = flNode.getEnabledBy();
        if (featureCandidates.isEmpty()) {
            return;
        }

        // Need to sort the collection of features so that they are in a reliable order for tests.
        ArrayList<String> sortedFeatures = new ArrayList<String>();
        sortedFeatures.addAll(featureCandidates);
        Collections.sort(sortedFeatures);

        String insertText = "";
        int referenceRangeStart = 0;
        int referenceRangeEnd = 0;

        for (DOMNode node : document.getDocumentElement().getChildren()) {
            if (LibertyConstants.FEATURE_MANAGER_ELEMENT.equals(node.getNodeName())) {
                DOMNode lastChild = node.getLastChild();
                if (node.getChildren().size() > 1) {
                    // Situation 2
                    insertText = "\n" + FEATURE_FORMAT;
                    referenceRangeStart = lastChild.getStart();
                    referenceRangeEnd = lastChild.getEnd();
                } else {
                    if (lastChild != null && (lastChild.hasChildNodes() || lastChild.isComment())) {
                        // Situation 2
                        insertText = "\n" + FEATURE_FORMAT;
                        referenceRangeStart = lastChild.getStart();
                        referenceRangeEnd = lastChild.getEnd();
                    } else {
                        // Situation 1
                        insertText = "\n\t" + FEATURE_FORMAT;
                        DOMElement featureManager = (DOMElement) node;
                        referenceRangeStart = featureManager.getStartTagOpenOffset();
                        referenceRangeEnd = featureManager.getStartTagCloseOffset()+1;
                    }
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

        String indent = "    ";
        try {
            indent = request.getXMLGenerator().getWhitespacesIndent();
        } catch (BadLocationException e) {
            LOGGER.info("Defaulting indent to four spaces.");
        }
        insertText = IndentUtil.formatText(insertText, indent, referenceRange.getStart().getCharacter());

        for (String feature : sortedFeatures) {
            String title = "Add feature " + feature;
            codeActions.add(CodeActionFactory.insert(
                    title, referenceRange.getEnd(), String.format(insertText, feature), textDocument, diagnostic));
        }
    }
}
