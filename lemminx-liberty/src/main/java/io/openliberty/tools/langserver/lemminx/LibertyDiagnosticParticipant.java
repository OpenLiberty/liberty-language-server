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
import io.openliberty.tools.langserver.lemminx.services.LibertyProjectsManager;
import io.openliberty.tools.langserver.lemminx.services.LibertyWorkspace;
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

    public static final String NOT_XML_OR_DIR = "The specified resource is not an XML file. If it is a directory, it must end with a trailing slash.";

    public static final String MISSING_FILE_MESSAGE = "The resource at the specified location could not be found.";
    public static final String MISSING_FILE_CODE = "missing_file";

    public static final String MISSING_CONFIGURED_FEATURE_MESSAGE = "This config element does not relate to a feature configured in the featureManager. If the relevant feature is specified in another server configuration file, this message can be ignored. Otherwise, remove this element or add a relevant feature.";
    public static final String MISSING_CONFIGURED_FEATURE_CODE = "lost_config_element";

    public static final String NOT_OPTIONAL_MESSAGE = "The specified resource cannot be skipped. Check location value or set optional to true.";
    public static final String NOT_OPTIONAL_CODE = "not_optional";
    public static final String IMPLICIT_NOT_OPTIONAL_MESSAGE = "The specified resource cannot be skipped. Check location value or add optional attribute.";
    public static final String IMPLICIT_NOT_OPTIONAL_CODE = "implicit_not_optional";

    public static final String SPECIFIED_DIR_IS_FILE = "Path specified a directory, but resource exists as a file. Please remove the trailing slash.";
    public static final String SPECIFIED_FILE_IS_DIR = "Path specified a file, but resource exists as a directory. Please add a trailing slash.";
    public static final String IS_FILE_NOT_DIR_CODE = "is_file_not_dir";
    public static final String Is_DIR_NOT_FILE_CODE = "is_dir_not_file";

    public static final String INCORRECT_FEATURE_CODE = "incorrect_feature";
    
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
        Set<String> includedFeatures = new HashSet<String>();
        boolean featureManagerPresent = false;
        LibertyWorkspace workspace = LibertyProjectsManager.getInstance().getWorkspaceFolder(domDocument.getDocumentURI());
        if (workspace == null) {
            LOGGER.warning("Could not get workspace, using default cached feature list");
        }
        FeatureListGraph featureGraph = (workspace == null) ? FeatureService.getInstance().getDefaultFeatureList() : workspace.getFeatureListGraph();
        for (DOMNode node : nodes) {
            String nodeName = node.getNodeName();
            if (LibertyConstants.FEATURE_MANAGER_ELEMENT.equals(nodeName)) {
                featureManagerPresent = true;
                validateFeature(domDocument, diagnosticsList, node, includedFeatures);
            } else if (LibertyConstants.INCLUDE_ELEMENT.equals(nodeName)) {
                validateIncludeLocation(domDocument, diagnosticsList, node);
            } else if (featureGraph.isConfigElement(nodeName)) {    // defaults to false
                holdConfigElement(domDocument, node, tempDiagnosticsList);
            }
        }
        validateConfigElements(domDocument, diagnosticsList, tempDiagnosticsList, featureGraph, includedFeatures, featureManagerPresent);
    }

    private void validateFeature(DOMDocument domDocument, List<Diagnostic> list, DOMNode featureManager, Set<String> includedFeatures) {

        LibertyRuntime runtimeInfo = LibertyUtils.getLibertyRuntimeInfo(domDocument);
        String libertyVersion =  runtimeInfo == null ? null : runtimeInfo.getRuntimeVersion();
        String libertyRuntime =  runtimeInfo == null ? null : runtimeInfo.getRuntimeType();

        final int requestDelay = SettingsService.getInstance().getRequestDelay();

        Set<String> featuresWithoutVersions = new HashSet<String>();

        // Search for duplicate features
        // or features that do not exist
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
                    String featureNameLower = featureName.toLowerCase();
                    String featureNameNoVersionLower = featureNameLower.substring(0,featureNameLower.lastIndexOf("-"));
                    // if this exact feature already exists, or another version of this feature already exists, then show a diagnostic
                    if (includedFeatures.contains(featureNameLower)) {
                        Range range = XMLPositionUtility.createRange(featureTextNode.getStart(),
                                featureTextNode.getEnd(), domDocument);
                        String message = "ERROR: " + featureName + " is already included.";
                        list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE));
                    } else if (featuresWithoutVersions.contains(featureNameNoVersionLower)) {
                        Range range = XMLPositionUtility.createRange(featureTextNode.getStart(),
                                featureTextNode.getEnd(), domDocument);
                        String featureNameNoVersion = featureName.substring(0,featureName.lastIndexOf("-"));
                        String message = "ERROR: More than one version of feature " + featureNameNoVersion + " is included. Only one version of a feature may be specified.";
                        list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE));
                    }
                    includedFeatures.add(featureNameLower);
                    featuresWithoutVersions.add(featureNameNoVersionLower);
                }
            }
        }
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
        // Liberty uses this to determine if directory. 
        boolean isLibertyDirectory = locAttribute.endsWith("/") || locAttribute.endsWith(File.separator);

        DOMNode locNode = node.getAttributeNode("location");
        Range range = XMLPositionUtility.createRange(locNode.getStart(), locNode.getEnd(), domDocument);
        if (!locAttribute.endsWith(".xml")
         && !isLibertyDirectory) {
            diagnosticsList.add(new Diagnostic(range, NOT_XML_OR_DIR, DiagnosticSeverity.Warning, LIBERTY_LEMMINX_SOURCE));
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
            validateFileOrDirIncludeLocation(configFile, isLibertyDirectory, range, diagnosticsList);
        } catch (IllegalArgumentException e) {
            diagnosticsList.add(new Diagnostic(range, MISSING_FILE_MESSAGE, DiagnosticSeverity.Warning, "liberty-lemminx-exception", MISSING_FILE_CODE));
        }
    }

    /**
     * Checks if specified file or dir is the correct filetype.
     * Adds diagnostics if it is mismatched.
     * @param f - <include> location file
     * @param isLibertyDirectory - whether Liberty considers this specified as a dir (path ends in slash)
     * @param range - Range to apply diagnostic message
     * @param diagnosticsList
     */
    private void validateFileOrDirIncludeLocation(File f, boolean isLibertyDirectory, Range range, List<Diagnostic> diagnosticsList) {
        if (f.isFile() && isLibertyDirectory) {
            diagnosticsList.add(new Diagnostic(range, SPECIFIED_DIR_IS_FILE, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE, IS_FILE_NOT_DIR_CODE));
        } else if (f.isDirectory() && !isLibertyDirectory) {
            diagnosticsList.add(new Diagnostic(range, SPECIFIED_FILE_IS_DIR, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE, Is_DIR_NOT_FILE_CODE));
        }
    }

    /**
     * Create temporary diagnostics for validation for single pass-through.
     * @param domDocument
     * @param diagnosticsList
     * @param configElementNode
     * @param tempDiagnosticsList
     */
    private void holdConfigElement(DOMDocument domDocument, DOMNode configElementNode, List<Diagnostic> tempDiagnosticsList) {
        String configElementName = configElementNode.getNodeName();
        Range range = XMLPositionUtility
                .createRange(configElementNode.getStart(), configElementNode.getEnd(), domDocument);
        Diagnostic tempDiagnostic = new Diagnostic(range, MISSING_CONFIGURED_FEATURE_MESSAGE, DiagnosticSeverity.Warning, LIBERTY_LEMMINX_SOURCE, MISSING_CONFIGURED_FEATURE_CODE);
        tempDiagnostic.setSource(configElementName);
        tempDiagnosticsList.add(tempDiagnostic);
    }

    /**
     * Compare the required feature set with included feature set for each config element.
     * @param diagnosticsList
     * @param tempDiagnosticsList
     * @param featureGraph
     */
    private void validateConfigElements(DOMDocument domDocument, List<Diagnostic> diagnosticsList, List<Diagnostic> tempDiagnosticsList, 
                                        FeatureListGraph featureGraph, Set<String> includedFeatures, boolean featureManagerPresent) {
        if (featureGraph.isEmpty()) {
            return;
        }
        if (includedFeatures.isEmpty()) {
            if (featureManagerPresent) {
                diagnosticsList.addAll(tempDiagnosticsList);
            } else {
                LOGGER.warning("No featureManager element found in document. Config element validation for missing features disabled for this document: " + domDocument.getDocumentURI());
            }
            return;
        }
        for (Diagnostic tempDiagnostic : tempDiagnosticsList) {
            String configElement = tempDiagnostic.getSource();
            Set<String> includedFeaturesCopy = new HashSet<String>(includedFeatures);
            Set<String> compatibleFeaturesList = featureGraph.getAllEnabledBy(configElement);
            includedFeaturesCopy.retainAll(compatibleFeaturesList);
            if (includedFeaturesCopy.isEmpty()) {
                diagnosticsList.add(tempDiagnostic);
            }
        }
    }
}