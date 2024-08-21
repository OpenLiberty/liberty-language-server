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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
        String libertyVersion = runtimeInfo == null ? null : runtimeInfo.getRuntimeVersion();
        String libertyRuntime = runtimeInfo == null ? null : runtimeInfo.getRuntimeType();

        final int requestDelay = SettingsService.getInstance().getRequestDelay();

        Set<String> featuresWithoutVersions = new HashSet<String>();
        Set<String> versionlessFeatures = new HashSet<String>();
        Set<String> versionedFeatures = new HashSet<String>();
        Set<String> selectedPlatforms = new HashSet<String>();
        Set<String> selectedPlatformsWithoutVersion = new HashSet<String>();
        // Search for duplicate features
        // or features that do not exist
        List<DOMNode> features = featureManager.getChildren();
        for (DOMNode featureNode : features) {
            DOMNode featureTextNode = (DOMNode) featureNode.getChildNodes().item(0);
            // check for platform element
            if (LibertyConstants.PLATFORM_ELEMENT.equals(featureNode.getLocalName())) {
                validatePlatform(domDocument, list, featureTextNode, selectedPlatformsWithoutVersion, selectedPlatforms);
            } else {
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
                        String featureNameNoVersionLower=featureNameLower;
                        if(featureNameLower.contains("-")){
                            featureNameNoVersionLower = featureNameLower.substring(0,
                                    featureNameLower.lastIndexOf("-"));
                            versionedFeatures.add(featureName);
                        }else{
                            versionlessFeatures.add(featureName);
                        }
                        // if this exact feature already exists, or another version of this feature already exists, then show a diagnostic
                        if (includedFeatures.contains(featureNameLower)) {
                            Range range = XMLPositionUtility.createRange(featureTextNode.getStart(),
                                    featureTextNode.getEnd(), domDocument);
                            String message = "ERROR: " + featureName + " is already included.";
                            list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE));
                        } else if (featuresWithoutVersions.contains(featureNameNoVersionLower)) {
                            Range range = XMLPositionUtility.createRange(featureTextNode.getStart(),
                                    featureTextNode.getEnd(), domDocument);
                            String featureNameNoVersion = featureName.substring(0, featureName.lastIndexOf("-"));
                            String message = "ERROR: More than one version of feature " + featureNameNoVersion + " is included. Only one version of a feature may be specified.";
                            list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE));
                        }
                        includedFeatures.add(featureNameLower);
                        featuresWithoutVersions.add(featureNameNoVersionLower);
                    }
                }
            }
        }
        checkForPlatFormAndFeature(domDocument, list, versionlessFeatures, features, selectedPlatforms, versionedFeatures);
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

    /**
     * validate platform element. checks for
     *      1) if platform is invalid
     *      2) if platform is already included
     *      3) if another version of same platform is included
     *      4) if any conflicting platform is included. for eg, j2ee and jakartaee are conflicting
     * @param domDocument xml document
     * @param list diagnostics list
     * @param featureTextNode current feature manager node
     * @param selectedPlatformsWithoutVersion platforms in xml without version
     * @param selectedPlatforms platforms in xml
     */
    private static void validatePlatform(DOMDocument domDocument, List<Diagnostic> list, DOMNode featureTextNode, Set<String> selectedPlatformsWithoutVersion,
                                         Set<String> selectedPlatforms) {
        LibertyRuntime runtimeInfo = LibertyUtils.getLibertyRuntimeInfo(domDocument);
        String libertyVersion = runtimeInfo == null ? null : runtimeInfo.getRuntimeVersion();
        String libertyRuntime = runtimeInfo == null ? null : runtimeInfo.getRuntimeType();

        final int requestDelay = SettingsService.getInstance().getRequestDelay();
        if (featureTextNode != null && featureTextNode.getTextContent() != null) {
            String platformName = featureTextNode.getTextContent().trim();
            String platformNameLowerCase = platformName.toLowerCase();
            String platformNoVersionLower = platformNameLowerCase.contains("-") ? platformNameLowerCase.substring(0, platformNameLowerCase.lastIndexOf("-"))
                    : platformNameLowerCase;

            if (!FeatureService.getInstance().platformExists(platformName, libertyVersion, libertyRuntime, requestDelay, domDocument.getDocumentURI())) {
                Range range = XMLPositionUtility.createRange(featureTextNode.getStart(), featureTextNode.getEnd(),
                        domDocument);
                String message = "ERROR: The platform \"" + platformName + "\" does not exist.";
                list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE, INCORRECT_FEATURE_CODE));
            }
            // if this exact platform already exists, or another version of this feature already exists, then show a diagnostic
            else {
                checkForPlatformUniqueness(domDocument, list, featureTextNode, selectedPlatformsWithoutVersion, selectedPlatforms, platformNameLowerCase, platformName, platformNoVersionLower);
                Set<String> conflictingPlatforms = getConflictingPlatforms(platformNoVersionLower, selectedPlatforms);
                if (!conflictingPlatforms.isEmpty()) {
                    Range range = XMLPositionUtility.createRange(featureTextNode.getStart(),
                            featureTextNode.getEnd(), domDocument);
                    String message = "ERROR: " + platformName + " conflicts with already included platform(s) " + conflictingPlatforms;
                    list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE));
                }
                selectedPlatformsWithoutVersion.add(platformNoVersionLower);
                selectedPlatforms.add(platformNameLowerCase);

            }
        }
    }

    /**
     * check
     *  1)whether platform name is repeating
     *  2) another version of same platform is specified
     * @param domDocument
     * @param list
     * @param featureTextNode
     * @param selectedPlatformsWithoutVersion
     * @param selectedPlatforms
     * @param platformNameLowerCase
     * @param platformName
     * @param platformNoVersionLower
     */
    private static void checkForPlatformUniqueness(DOMDocument domDocument, List<Diagnostic> list, DOMNode featureTextNode, Set<String> selectedPlatformsWithoutVersion, Set<String> selectedPlatforms, String platformNameLowerCase, String platformName, String platformNoVersionLower) {
        if (selectedPlatforms.contains(platformNameLowerCase)) {
            Range range = XMLPositionUtility.createRange(featureTextNode.getStart(),
                    featureTextNode.getEnd(), domDocument);
            String message = "ERROR: " + platformName + " is already included.";
            list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE));
        } else if (selectedPlatformsWithoutVersion.contains(platformNoVersionLower)) {
            Range range = XMLPositionUtility.createRange(featureTextNode.getStart(),
                    featureTextNode.getEnd(), domDocument);
            String platformNameNoVersion = platformName.substring(0, platformName.lastIndexOf("-"));
            String message = "ERROR: More than one version of platform " + platformNameNoVersion + " is included. Only one version of a platform may be specified.";
            list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE));
        }
    }

    /**
     * get conflicting platform for selected platform
     * @param platformNoVersionLower
     * @param selectedPlatforms
     * @return
     */
    private static Set<String> getConflictingPlatforms(String platformNoVersionLower, Set<String> selectedPlatforms) {
        if(LibertyConstants.conflictingPlatforms.containsKey(platformNoVersionLower)){
            String conflictingPlatformName = LibertyConstants.conflictingPlatforms.get(platformNoVersionLower);
            return selectedPlatforms.stream()
                    .filter(p->p.contains(conflictingPlatformName)).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }


    /**
     * check for a combination of validations using selected platforms and selected features
     * @param domDocument
     * @param list
     * @param versionlessFeatures
     * @param features
     * @param selectedPlatforms
     * @param versionedFeatures
     */
    private static void checkForPlatFormAndFeature(DOMDocument domDocument, List<Diagnostic> list, Set<String> versionlessFeatures, List<DOMNode> features, Set<String> selectedPlatforms, Set<String> versionedFeatures) {

            for (DOMNode featureNode : features) {
                DOMNode featureTextNode = (DOMNode) featureNode.getChildNodes().item(0);
                if (LibertyConstants.FEATURE_ELEMENT.equals(featureNode.getLocalName())
                        && featureTextNode != null && featureTextNode.getTextContent() != null) {
                    String featureName = featureTextNode.getTextContent().trim();
                    String featureNameLower = featureName.toLowerCase();
                    if (!featureNameLower.contains("-")&& versionlessFeatures.contains(featureName)) {
                        // versionless feature
                        validateVersionLessFeatures(domDocument, list, selectedPlatforms, versionedFeatures, featureTextNode, featureName);
                    }
                    else if(featureNameLower.contains("-") && versionedFeatures.contains(featureName)){
                        validateVersionedFeatures(domDocument, list, selectedPlatforms, versionedFeatures, featureTextNode, featureName);
                    }
                }

        }
    }

    /**
     * for platform and feature validation of versioned features
     * platform list already added in xml should be matching with all the common platforms supported for selected features
     * @param domDocument
     * @param list
     * @param selectedPlatforms
     * @param versionedFeatures
     * @param featureTextNode
     * @param featureName
     */
    private static void validateVersionedFeatures(DOMDocument domDocument, List<Diagnostic> list, Set<String> selectedPlatforms, Set<String> versionedFeatures, DOMNode featureTextNode, String featureName) {
        // if any platform is selected, it should be allowed for all features, otherwise throw error
        LibertyRuntime runtimeInfo = LibertyUtils.getLibertyRuntimeInfo(domDocument);
        String libertyVersion = runtimeInfo == null ? null : runtimeInfo.getRuntimeVersion();
        String libertyRuntime = runtimeInfo == null ? null : runtimeInfo.getRuntimeType();

        final int requestDelay = SettingsService.getInstance().getRequestDelay();
        Set<String> commonPlatforms = checkForCommonPlatforms(domDocument, versionedFeatures, libertyVersion, libertyRuntime, requestDelay);
        if (commonPlatforms != null && !commonPlatforms.isEmpty() && !commonPlatforms.containsAll(selectedPlatforms)) {
            Range range = XMLPositionUtility.createRange(featureTextNode.getStart(), featureTextNode.getEnd(),
                    domDocument);
            String message = "ERROR: \"" + featureName + "\" cannot be used since selected platform(s) " + selectedPlatforms + " do not match with supported platform(s) " + commonPlatforms;
            list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE, INCORRECT_FEATURE_CODE));
        }
    }

    /**
     * check for versionless feature version identification
     *      1) if versionless feature is specified and no versioned feature or platform is specified, throw error
     *      2) if versionless feature is specified and no platform is specified
     *          if no common platform is found for all versioned features, throw error
     *      3) if common platform is found, then
     *          find all required and required tolerates features for versionless feature
     *          find all platforms for above features
     *          check common platforms is present in feature platforms
     *          throw error if not found
     *
     * @param domDocument
     * @param list
     * @param selectedPlatforms
     * @param versionedFeatures
     * @param versionLessFeatureTextNode
     * @param featureName
     */
    private static void validateVersionLessFeatures(DOMDocument domDocument, List<Diagnostic> list, Set<String> selectedPlatforms, Set<String> versionedFeatures,DOMNode versionLessFeatureTextNode, String featureName) {
        LibertyRuntime runtimeInfo = LibertyUtils.getLibertyRuntimeInfo(domDocument);
        String libertyVersion = runtimeInfo == null ? null : runtimeInfo.getRuntimeVersion();
        String libertyRuntime = runtimeInfo == null ? null : runtimeInfo.getRuntimeType();

        final int requestDelay = SettingsService.getInstance().getRequestDelay();
        if (versionedFeatures.isEmpty() && selectedPlatforms.isEmpty()) {
            Range range = XMLPositionUtility.createRange(versionLessFeatureTextNode.getStart(),
                    versionLessFeatureTextNode.getEnd(), domDocument);
            String message = "ERROR: Need to specify any platform or at least one versioned feature.";
            list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE, INCORRECT_FEATURE_CODE));
        } else if (!versionedFeatures.isEmpty() && selectedPlatforms.isEmpty()) {
            Set<String> commonPlatforms = checkForCommonPlatforms(domDocument, versionedFeatures, libertyVersion, libertyRuntime, requestDelay);
            if (commonPlatforms == null ||
                    commonPlatforms.isEmpty()) {
                Range range = XMLPositionUtility.createRange(versionLessFeatureTextNode.getStart(), versionLessFeatureTextNode.getEnd(),
                        domDocument);
                String message = "ERROR: \"" + featureName + "\" cannot be used since there is no common platform for all versioned features.";
                list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE, INCORRECT_FEATURE_CODE));
            }
            else {
                checkForVersionlessPlatforms(domDocument, list, commonPlatforms, versionLessFeatureTextNode, featureName, libertyVersion, libertyRuntime, requestDelay);
            }
        }
        if (!selectedPlatforms.isEmpty()) {
            checkForVersionlessPlatforms(domDocument, list, selectedPlatforms, versionLessFeatureTextNode, featureName, libertyVersion, libertyRuntime, requestDelay);
        }
    }

    /**
     * check all platforms for versionless features
     * @param domDocument
     * @param list
     * @param selectedPlatforms
     * @param versionLessFeatureTextNode
     * @param featureName
     * @param libertyVersion
     * @param libertyRuntime
     * @param requestDelay
     */
    private static void checkForVersionlessPlatforms(DOMDocument domDocument, List<Diagnostic> list, Set<String> selectedPlatforms, DOMNode versionLessFeatureTextNode, String featureName, String libertyVersion, String libertyRuntime, int requestDelay) {
        Set<String> allPlatforrmsForVersionLess = FeatureService.getInstance()
                .getAllPlatformsForVersionLessFeature(featureName, libertyVersion, libertyRuntime, requestDelay, domDocument.getDocumentURI());
        if (!allPlatforrmsForVersionLess.containsAll(selectedPlatforms)) {
            Range range = XMLPositionUtility.createRange(versionLessFeatureTextNode.getStart(), versionLessFeatureTextNode.getEnd(),
                    domDocument);
            String message = "ERROR: The \"" + featureName + "\" versionless feature cannot be resolved";
            list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE, INCORRECT_FEATURE_CODE));
        }
    }

    /**
     * check for common platforms for all versioned features
     *
     * @param domDocument
     * @param selectedPlatforms
     * @param versionedFeatures
     * @param versionLessFeatureTextNode
     * @param featureName
     * @param libertyVersion
     * @param libertyRuntime
     * @param requestDelay
     * @return
     */
    private static Set<String> checkForCommonPlatforms(DOMDocument domDocument, Set<String> versionedFeatures, String libertyVersion, String libertyRuntime, int requestDelay) {
        Set<String> commonPlatforms = FeatureService.getInstance()
                .getCommonPlatformsForFeatures(versionedFeatures, libertyVersion, libertyRuntime, requestDelay, domDocument.getDocumentURI());

        return commonPlatforms;
    }
}
