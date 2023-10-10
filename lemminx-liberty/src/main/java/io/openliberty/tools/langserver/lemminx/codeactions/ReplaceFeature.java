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

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

import io.openliberty.tools.langserver.lemminx.LibertyExtension;
import io.openliberty.tools.langserver.lemminx.data.LibertyRuntime;
import io.openliberty.tools.langserver.lemminx.models.feature.Feature;
import io.openliberty.tools.langserver.lemminx.services.FeatureService;
import io.openliberty.tools.langserver.lemminx.services.SettingsService;
import io.openliberty.tools.langserver.lemminx.util.LibertyUtils;
import io.openliberty.tools.langserver.lemminx.util.LibertyConstants;

public class ReplaceFeature implements ICodeActionParticipant {
    private static final Logger LOGGER = Logger.getLogger(LibertyExtension.class.getName());

    @Override
    public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
        Diagnostic diagnostic = request.getDiagnostic();
        DOMDocument document = request.getDocument();
        try {
            // Get a list of features that partially match the specified invalid feature.
            // Create a code action to replace the invalid feature with each possible valid feature.
            // First, get the invalid feature.
            String invalidFeature = document.findNodeAt(document.offsetAt(diagnostic.getRange().getEnd())).getTextContent();

            List<DOMNode> nodes = document.getDocumentElement().getChildren();
            DOMNode featureManagerNode = null;

            for (DOMNode node : nodes) {
                if (LibertyConstants.FEATURE_MANAGER_ELEMENT.equals(node.getNodeName())) {
                    featureManagerNode = node;
                    break;
                }
            }

            final boolean replaceFeatureName = invalidFeature != null && !invalidFeature.isBlank();
            // strip off version number after the - so that we can provide all possible valid versions of a feature for completion
            final String featureNameToReplace = replaceFeatureName && invalidFeature.contains("-") ? invalidFeature.substring(0, invalidFeature.lastIndexOf("-")+1) : invalidFeature;
    
            if (replaceFeatureName) {
                LibertyRuntime runtimeInfo = LibertyUtils.getLibertyRuntimeInfo(document);
                String libertyVersion =  runtimeInfo == null ? null : runtimeInfo.getRuntimeVersion();
                String libertyRuntime =  runtimeInfo == null ? null : runtimeInfo.getRuntimeType();
        
                final int requestDelay = SettingsService.getInstance().getRequestDelay();
                FeatureService fs = FeatureService.getInstance();
                List<Feature> replacementFeatures = fs.getFeatureReplacements(featureNameToReplace, featureManagerNode, libertyVersion, libertyRuntime, requestDelay, document.getDocumentURI());
                List<String> replacementFeatureNames = fs.getFeatureShortNames(replacementFeatures);
                Collections.sort(replacementFeatureNames); // sort these so they appear in alphabetical order in quick fixes - also helps the test case pass reliably

                for (String nextFeature : replacementFeatureNames) {
                    String title = "Replace feature with "+nextFeature;

                    codeActions.add(CodeActionFactory.replace(title, diagnostic.getRange(), nextFeature, document.getTextDocument(), diagnostic));
                }
            }
        } catch (Exception e) {
            // BadLocationException not expected
            LOGGER.warning("Could not generate code action for replace feature: " + e);
        }
    }
}
