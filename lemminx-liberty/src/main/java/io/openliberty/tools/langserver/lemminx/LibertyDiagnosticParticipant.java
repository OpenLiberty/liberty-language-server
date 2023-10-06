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

import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLValidationSettings;
import org.eclipse.lemminx.services.extensions.diagnostics.IDiagnosticsParticipant;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

import io.openliberty.tools.langserver.lemminx.data.FeatureListGraph;
import io.openliberty.tools.langserver.lemminx.data.LibertyRuntime;
import io.openliberty.tools.langserver.lemminx.services.FeatureService;
import io.openliberty.tools.langserver.lemminx.services.SettingsService;
import io.openliberty.tools.langserver.lemminx.util.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class LibertyDiagnosticParticipant implements IDiagnosticsParticipant {
    private static final Logger LOGGER = Logger.getLogger(LibertyDiagnosticParticipant.class.getName());

    public static final String LIBERTY_LEMMINX_SOURCE = "liberty-lemminx";

    public static final String MISSING_FILE_MESSAGE = "The resource at the specified location could not be found.";
    public static final String MISSING_FILE_CODE = "missing_file";

    public static final String MISSING_CONFIGURED_FEATURE_MESSAGE = "This config element does not configure a feature in the featureManager. Remove this element or add a relevant feature.";
    public static final String MISSING_CONFIGURED_FEATURE_CODE = "lost_config_element";

    public static final String NOT_OPTIONAL_MESSAGE = "The specified resource cannot be skipped. Check location value or set optional to true.";
    public static final String NOT_OPTIONAL_CODE = "not_optional";
    public static final String IMPLICIT_NOT_OPTIONAL_MESSAGE = "The specified resource cannot be skipped. Check location value or add optional attribute.";
    public static final String IMPLICIT_NOT_OPTIONAL_CODE = "implicit_not_optional";

    public static final String INCORRECT_FEATURE_CODE = "incorrect_feature";

    private Set<String> includedFeatures = null;
    
    @Override
    public void doDiagnostics(DOMDocument domDocument, List<Diagnostic> diagnostics,
            XMLValidationSettings validationSettings, CancelChecker cancelChecker) {
        if (!LibertyUtils.isConfigXMLFile(domDocument))
            return;
        try {
            validateDom(domDocument, diagnostics);
        } catch (IOException e) {
            LOGGER.severe("Error validating document " + domDocument.getDocumentURI());
            LOGGER.severe(e.getMessage());
        }
    }

    private void validateDom(DOMDocument domDocument, List<Diagnostic> diagnosticsList) throws IOException {
        List<DOMNode> nodes = domDocument.getDocumentElement().getChildren();
        List<Diagnostic> tempDiagnosticsList = new ArrayList<Diagnostic>();

        FeatureListGraph featureGraph = FeatureService.getInstance().getFeatureListGraph();
        // TODO: Consider adding a cached feature list onto repo to optimize
        if (featureGraph.isEmpty()) {
            LibertyRuntime runtimeInfo = LibertyUtils.getLibertyRuntimeInfo(domDocument);
            String libertyVersion =  runtimeInfo == null ? null : runtimeInfo.getRuntimeVersion();
            String libertyRuntime =  runtimeInfo == null ? null : runtimeInfo.getRuntimeType();
            FeatureService.getInstance().getInstalledFeaturesList(domDocument.getDocumentURI(), libertyRuntime, libertyVersion);
        }

        for (DOMNode node : nodes) {
            String nodeName = node.getNodeName();
            if (LibertyConstants.FEATURE_MANAGER_ELEMENT.equals(nodeName)) {
                validateFeature(domDocument, diagnosticsList, node);
            } else if (LibertyConstants.INCLUDE_ELEMENT.equals(nodeName)) {
                validateIncludeLocation(domDocument, diagnosticsList, node);
            } else if (featureGraph.isConfigElement(nodeName)) {    // defaults to false
                holdConfigElement(domDocument, diagnosticsList, node, tempDiagnosticsList);
            }
        }
        validateConfigElements(diagnosticsList, tempDiagnosticsList, featureGraph);
    }

    private void validateFeature(DOMDocument domDocument, List<Diagnostic> list, DOMNode featureManager) {
        LibertyRuntime runtimeInfo = LibertyUtils.getLibertyRuntimeInfo(domDocument);
        String libertyVersion =  runtimeInfo == null ? null : runtimeInfo.getRuntimeVersion();
        String libertyRuntime =  runtimeInfo == null ? null : runtimeInfo.getRuntimeType();

        final int requestDelay = SettingsService.getInstance().getRequestDelay();

        // Search for duplicate features
        // or features that do not exist
        Set<String> includedFeatures = new HashSet<>();
        List<DOMNode> features = featureManager.getChildren();
        for (DOMNode featureNode : features) {
            DOMNode featureTextNode = (DOMNode) featureNode.getChildNodes().item(0);
            // skip nodes that do not have any text value (ie. comments)
            if (featureTextNode != null && featureTextNode.getTextContent() != null) {
                String featureName = featureTextNode.getTextContent().trim();
                // if the feature is not a user defined feature and the feature does not exist in the list of
                // supported features show a "Feature does not exist" diagnostic
                if (!featureName.startsWith("usr:") && !FeatureService.getInstance().featureExists(featureName, libertyVersion, libertyRuntime, requestDelay, domDocument.getDocumentURI())) {
                    Range range = XMLPositionUtility.createRange(featureTextNode.getStart(), featureTextNode.getEnd(),
                            domDocument);
                    String message = "ERROR: The feature \"" + featureName + "\" does not exist.";
                    list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE, INCORRECT_FEATURE_CODE));
                } else {
                    if (includedFeatures.contains(featureName)) {
                        Range range = XMLPositionUtility.createRange(featureTextNode.getStart(),
                                featureTextNode.getEnd(), domDocument);
                        String message = "ERROR: " + featureName + " is already included.";
                        list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE));
                    } else {
                        includedFeatures.add(featureName);
                    }
                }
            }
        }
        this.includedFeatures = includedFeatures;
    }

    /**
     * Location checks:
     * 1) Relative path; 2) Server config dir; 
     * 3) Absolute path; 4) Web server
     * 
     * Checks in method: 1), 3)
     * 2) performed in isConfigXMLFile
     * 4) not yet implemented/determined
     */
    private void validateIncludeLocation(DOMDocument domDocument, List<Diagnostic> diagnosticsList, DOMNode node) {
        String locAttribute = node.getAttribute("location");
        if (locAttribute == null) {
            return;
        }
        // skip diagnostic for not yet implemented behaviors/checks (URL + vars)
        if (locAttribute.startsWith("http") || locAttribute.contains("$")) {
            return;
        }

        DOMNode locNode = node.getAttributeNode("location");
        Range range = XMLPositionUtility.createRange(locNode.getStart(), locNode.getEnd(), domDocument);
        if (!locAttribute.endsWith(".xml")) {
            String message = "The specified resource is not an XML file.";
            diagnosticsList.add(new Diagnostic(range, message, DiagnosticSeverity.Warning, LIBERTY_LEMMINX_SOURCE));
            return;
        }

        File docParentFile = LibertyUtils.getDocumentAsFile(domDocument).getParentFile();
        File configFile = new File(docParentFile, locAttribute);
        if (!configFile.exists()) {
            configFile = new File(locAttribute);
        }
        try {
            if (!configFile.exists()) {
                DOMAttr optNode = node.getAttributeNode("optional");
                if (optNode == null) {
                    diagnosticsList.add(new Diagnostic(range, IMPLICIT_NOT_OPTIONAL_MESSAGE, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE, IMPLICIT_NOT_OPTIONAL_CODE));
                } else if (optNode.getValue().equals("false")) {
                    Range optRange = XMLPositionUtility.createRange(optNode.getStart(), optNode.getEnd(), domDocument);
                    diagnosticsList.add(new Diagnostic(optRange, NOT_OPTIONAL_MESSAGE, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE, NOT_OPTIONAL_CODE));
                }
                diagnosticsList.add(new Diagnostic(range, MISSING_FILE_MESSAGE, DiagnosticSeverity.Warning, LIBERTY_LEMMINX_SOURCE, MISSING_FILE_CODE));
            }
        } catch (IllegalArgumentException e) {
            diagnosticsList.add(new Diagnostic(range, MISSING_FILE_MESSAGE, DiagnosticSeverity.Warning, "liberty-lemminx-exception", MISSING_FILE_CODE));
        }
    }

    /**
     * Create temporary diagnostics for validation for single pass-through.
     * @param domDocument
     * @param diagnosticsList
     * @param configElementNode
     * @param tempDiagnosticsList
     */
    private void holdConfigElement(DOMDocument domDocument, List<Diagnostic> diagnosticsList, DOMNode configElementNode, List<Diagnostic> tempDiagnosticsList) {
        String configElementName = configElementNode.getNodeName();
        Range range = XMLPositionUtility.createRange(configElementNode.getStart(), configElementNode.getEnd(), domDocument);
        Diagnostic tempDiagnostic = new Diagnostic(range, MISSING_CONFIGURED_FEATURE_MESSAGE, null, LIBERTY_LEMMINX_SOURCE, MISSING_CONFIGURED_FEATURE_CODE);
        tempDiagnostic.setSource(configElementName);
        tempDiagnosticsList.add(tempDiagnostic);
    }

    /**
     * Compare the required feature set with included feature set for each config element.
     * @param diagnosticsList
     * @param tempDiagnosticsList
     * @param featureGraph
     */
    private void validateConfigElements(List<Diagnostic> diagnosticsList, List<Diagnostic> tempDiagnosticsList, FeatureListGraph featureGraph) {
        for (Diagnostic tempDiagnostic : tempDiagnosticsList) {
            String configElement = tempDiagnostic.getSource();
            Set<String> includedFeaturesCopy = (includedFeatures == null) ? new HashSet<String>() : new HashSet<String>(includedFeatures);
            Set<String> compatibleFeaturesList = featureGraph.getAllEnabledBy(configElement);
            includedFeaturesCopy.retainAll(compatibleFeaturesList);
            if (includedFeaturesCopy.isEmpty()) {
                diagnosticsList.add(tempDiagnostic);
            }
        }
    }
}