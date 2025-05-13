/*******************************************************************************
* Copyright (c) 2020, 2024 IBM Corporation and others.
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

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.services.extensions.hover.IHoverParticipant;
import org.eclipse.lemminx.services.extensions.hover.IHoverRequest;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

import io.openliberty.tools.langserver.lemminx.data.FeatureListGraph;
import io.openliberty.tools.langserver.lemminx.data.FeatureListNode;
import io.openliberty.tools.langserver.lemminx.data.LibertyRuntime;
import io.openliberty.tools.langserver.lemminx.models.feature.*;
import io.openliberty.tools.langserver.lemminx.services.FeatureService;
import io.openliberty.tools.langserver.lemminx.services.SettingsService;
import io.openliberty.tools.langserver.lemminx.util.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

public class LibertyHoverParticipant implements IHoverParticipant {
    private static final Logger LOGGER = Logger.getLogger(LibertyHoverParticipant.class.getName());
    private static final String MARKDOWN_NEW_LINE = "  \n  \n";

    @Override
    public Hover onAttributeName(IHoverRequest request, CancelChecker cancelChecker) {
        return null;
    }

    @Override
    public Hover onAttributeValue(IHoverRequest request, CancelChecker cancelChecker) {
        List<VariableLoc> variables = LibertyUtils.getVariablesFromTextContent(request.getNode().getTextContent());
        Properties variableMap = SettingsService.getInstance()
                .getVariablesForServerXml(request.getXMLDocument()
                        .getDocumentURI());
        LibertyUtils.checkAndAddNewVariables(request.getXMLDocument(), variableMap);
        StringBuilder stringBuilder = new StringBuilder();
        Iterator<VariableLoc> varIter = variables.iterator();
        while (varIter.hasNext()) {
            VariableLoc variable = varIter.next();
            if (variableMap.containsKey(variable.getValue())) {
                stringBuilder.append(String.format("%s = %s", variable.getValue(), variableMap.get(variable.getValue())));
            }
            if (varIter.hasNext()) {
                stringBuilder.append(MARKDOWN_NEW_LINE);
            }
        }
        if (!stringBuilder.isEmpty()) {
            return new Hover(new MarkupContent(MarkupKind.MARKDOWN, stringBuilder.toString()));
        }
        return null;
    }

    @Override
    public Hover onTag(IHoverRequest request, CancelChecker cancelChecker) {
        return null;
    }

    @Override
    public Hover onText(IHoverRequest request, CancelChecker cancelChecker) {
        if (!LibertyUtils.isConfigXMLFile(request.getXMLDocument()))
            return null;

        DOMElement parentElement = request.getParentElement();
        if (parentElement == null || parentElement.getTagName() == null)
            return null;

        // if we are hovering over text inside a <feature> element
        if (LibertyConstants.FEATURE_ELEMENT.equals(parentElement.getTagName())) {
            String featureName = request.getNode().getTextContent().trim();
            return getHoverFeatureDescription(featureName, request.getXMLDocument());
        }
        if (LibertyConstants.PLATFORM_ELEMENT.equals(parentElement.getTagName())) {
            String platformName = request.getNode().getTextContent().trim();
            return getHoverPlatformDescription(platformName, request.getXMLDocument());
        }

        return null;
    }

    /**
     * get description for platform from the feature json
     * there will be a feature with same shortname as platform.
     *
     * @param platformName platform name
     * @return hover
     */
    private Hover getHoverPlatformDescription(String platformName, DOMDocument domDocument) {
        LibertyRuntime runtimeInfo = LibertyUtils.getLibertyRuntimeInfo(domDocument);
        String libertyVersion = runtimeInfo == null ? null : runtimeInfo.getRuntimeVersion();
        String libertyRuntime = runtimeInfo == null ? null : runtimeInfo.getRuntimeType();
        final int requestDelay = SettingsService.getInstance().getRequestDelay();

        // check first that the platform is a valid one
        if (FeatureService.getInstance().platformExists(platformName, libertyVersion, libertyRuntime, requestDelay, domDocument.getDocumentURI())) {
            String description = LibertyUtils.getPlatformDescription(platformName);
            if (description != null) {
                return new Hover(new MarkupContent(MarkupKind.MARKDOWN, description));
            }
        } else {
            LOGGER.warning("Could not get description for platform: "+platformName+"  from features.json file. Platform does not exist."); 
        }

        return null;
    }

    private Hover getFeatureDescription(String featureName, DOMDocument domDocument) {
        LibertyRuntime runtimeInfo = LibertyUtils.getLibertyRuntimeInfo(domDocument);
        String libertyVersion =  runtimeInfo == null ? null : runtimeInfo.getRuntimeVersion();
        String libertyRuntime =  runtimeInfo == null ? null : runtimeInfo.getRuntimeType();

        final int requestDelay = SettingsService.getInstance().getRequestDelay();
        Optional<Feature> feature = FeatureService.getInstance().getFeature(featureName, libertyVersion, libertyRuntime, requestDelay, domDocument.getDocumentURI());
        if (feature.isPresent()) {
            return new Hover(new MarkupContent(MarkupKind.MARKDOWN, feature.get().getShortDescription()));
        }

        return null;
    }

    private Hover getHoverFeatureDescription(String featureName, DOMDocument document) {
        // Choosing to use the default feature list to get the full enables/enabled by information to display, as quite often the generated
        // feature list will only be a subset of the default one. If the feature is not found in the default feature list, this code will 
        // default to the original description only which is available from the downloaded features.json file.
        FeatureListGraph featureGraph = FeatureService.getInstance().getDefaultFeatureList();
        FeatureListNode flNode = featureGraph.getFeatureListNode(featureName);
        if (flNode == null) {
            LOGGER.warning("Could not get full description for feature: "+featureName+"  from cached feature list. Using description from features.json file.");
            return getFeatureDescription(featureName, document);
        }

        StringBuilder sb = new StringBuilder();
        String description = flNode.getDescription();
        sb.append(ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.TITLE_HOVER_DESCRIPTION) + " ");
        sb.append(description);

        // if this is a versionless feature, do not add any info about enables or enabled by
        if (flNode.isVersionless()) {
            if (!description.endsWith(".")) {
                sb.append(".");
            }
            return new Hover(new MarkupContent(MarkupKind.MARKDOWN, sb.toString()));
        }

        sb.append(MARKDOWN_NEW_LINE);

        // get features that directly enable this feature
        Set<String> featureEnabledBy = flNode.getEnabledBy();
        if (!featureEnabledBy.isEmpty()) {
            sb.append(ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.TITLE_HOVER_ENABLED_BY) + " ");
            // Need to sort the collection of features so that they are in a reliable order for tests.
            ArrayList<String> sortedFeatures = new ArrayList<String>();
            sortedFeatures.addAll(featureEnabledBy);
            Collections.sort(sortedFeatures);
            for (String nextFeature : sortedFeatures) {
                sb.append(nextFeature);
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2);
            sb.append(MARKDOWN_NEW_LINE);
        }

        // get features that this feature directly enables
        Set<String> featureEnables = flNode.getEnablesFeatures();
        if (!featureEnables.isEmpty()) {
            sb.append(ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.TITLE_HOVER_ENABLES) + " ");
            // Need to sort the collection of features so that they are in a reliable order for tests.
            ArrayList<String> sortedFeatures = new ArrayList<String>();
            sortedFeatures.addAll(featureEnables);
            Collections.sort(sortedFeatures);
            for (String nextFeature : sortedFeatures) {
                sb.append(nextFeature);
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2);
        }

       return new Hover(new MarkupContent(MarkupKind.MARKDOWN, sb.toString()));
    }
}