/*******************************************************************************
* Copyright (c) 2020, 2023 IBM Corporation and others.
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
package io.openliberty.tools.langserver.lemminx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.services.extensions.completion.CompletionParticipantAdapter;
import org.eclipse.lemminx.services.extensions.completion.ICompletionRequest;
import org.eclipse.lemminx.services.extensions.completion.ICompletionResponse;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.InsertReplaceEdit;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import io.openliberty.tools.langserver.lemminx.data.LibertyRuntime;
import io.openliberty.tools.langserver.lemminx.models.feature.Feature;
import io.openliberty.tools.langserver.lemminx.services.FeatureService;
import io.openliberty.tools.langserver.lemminx.services.SettingsService;
import io.openliberty.tools.langserver.lemminx.util.LibertyConstants;
import io.openliberty.tools.langserver.lemminx.util.LibertyUtils;

public class LibertyCompletionParticipant extends CompletionParticipantAdapter {

    @Override
    public void onXMLContent(ICompletionRequest request, ICompletionResponse response, CancelChecker cancelChecker)
            throws IOException, BadLocationException {
        if (!LibertyUtils.isConfigXMLFile(request.getXMLDocument()))
            return;    

        LibertyUtils.getLibertyRuntimeInfo(request.getXMLDocument());

        DOMElement parentElement = request.getParentElement();
        if (parentElement == null || parentElement.getTagName() == null)
            return;

        // if the parent element of cursor is a <feature>
        // provide the liberty features as completion options
        if (parentElement.getTagName().equals(LibertyConstants.FEATURE_ELEMENT)) {
            // narrow down completion list based on what was already entered
            DOMNode featureTextNode = (DOMNode) parentElement.getChildNodes().item(0);
            String featureName = featureTextNode != null ? featureTextNode.getTextContent() : null;

            // collect existing features
            List<String> existingFeatures = new ArrayList<String>();
            if (parentElement.getParentNode() != null
                    && parentElement.getParentNode().getNodeName().equals(LibertyConstants.FEATURE_MANAGER_ELEMENT)) {
                existingFeatures = FeatureService.getInstance().collectExistingFeatures(parentElement.getParentNode(), featureName);
            }

            List<CompletionItem> featureCompletionItems = buildCompletionItems(parentElement, request.getXMLDocument(),
                    existingFeatures, featureName);
            featureCompletionItems.stream().forEach(item -> response.addCompletionItem(item));
        }
    }

    private CompletionItem buildFeatureCompletionItem(Feature feature, DOMElement featureElement,
            DOMDocument document) {
        String featureName = feature.getWlpInformation().getShortName();

        // Build a text edit to replace whatever is inside <feature></feature>
        // with the completion result
        Range range = XMLPositionUtility.createRange(featureElement.getStartTagCloseOffset() + 1,
                featureElement.getEndTagOpenOffset(), document);
        Either<TextEdit, InsertReplaceEdit> edit = Either.forLeft(new TextEdit(range, featureName));

        // Build the completion item to return to the client
        CompletionItem item = new CompletionItem();
        item.setTextEdit(edit);
        item.setLabel(featureName);
        item.setDocumentation(Either.forLeft(feature.getShortDescription()));
        return item;
    }

    private List<CompletionItem> buildCompletionItems(DOMElement featureElement, DOMDocument domDocument,
            List<String> existingFeatures, String featureName) {

        LibertyRuntime runtimeInfo = LibertyUtils.getLibertyRuntimeInfo(domDocument);
        String libertyVersion =  runtimeInfo == null ? null : runtimeInfo.getRuntimeVersion();
        String libertyRuntime =  runtimeInfo == null ? null : runtimeInfo.getRuntimeType();

        final int requestDelay = SettingsService.getInstance().getRequestDelay();
        List<Feature> features = FeatureService.getInstance().getFeatures(libertyVersion, libertyRuntime, requestDelay, domDocument.getDocumentURI());

        final boolean checkFeatureName = featureName != null && !featureName.isBlank();
        // strip off version number after the - so that we can provide all possible valid versions of a feature for completion
        final String featureNameToCompare = checkFeatureName && featureName.contains("-") ? featureName.substring(0, featureName.lastIndexOf("-")+1) : featureName;

        // filter out features that are already specified in the featureManager block
 
        return getUniqueFeatureCompletionItems(featureElement, domDocument, features, existingFeatures, checkFeatureName, featureNameToCompare);
    }

    private List<CompletionItem> getUniqueFeatureCompletionItems(DOMElement featureElement, DOMDocument domDocument, List<Feature> allFeatures, List<String> existingFeatureNames, boolean useRequestedFeatureName, String requestedFeatureName) {
        List<CompletionItem> uniqueFeatureCompletionItems = new ArrayList<CompletionItem>();

        for (Feature nextFeature : allFeatures) {
            String nextFeatureName = nextFeature.getWlpInformation().getShortName();
            if (!existingFeatureNames.contains(nextFeatureName) && (!useRequestedFeatureName ||
                (useRequestedFeatureName && nextFeatureName.contains(requestedFeatureName)))) {
                CompletionItem ci = buildFeatureCompletionItem(nextFeature, featureElement, domDocument);
                uniqueFeatureCompletionItems.add(ci);
            }
        }

        return uniqueFeatureCompletionItems;
    }

}
