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

import com.google.common.collect.Sets;
import io.openliberty.tools.langserver.lemminx.models.feature.VariableLoc;
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
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static io.openliberty.tools.langserver.lemminx.util.LibertyConstants.changedFeatureNameDiagMessage;

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
    public static final String INCORRECT_PLATFORM_CODE = "incorrect_platform";
    public static final String INCORRECT_VARIABLE_CODE = "incorrect_variable";

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
                validateFeaturesAndPlatforms(domDocument, diagnosticsList, node, includedFeatures);
            } else if (LibertyConstants.INCLUDE_ELEMENT.equals(nodeName)) {
                validateIncludeLocation(domDocument, diagnosticsList, node);
            } else if (featureGraph.isConfigElement(nodeName)) {    // defaults to false
                holdConfigElement(domDocument, node, tempDiagnosticsList);
            }
        }
        validateConfigElements(domDocument, diagnosticsList, tempDiagnosticsList, featureGraph, includedFeatures, featureManagerPresent);
        validateVariables(domDocument,diagnosticsList);
    }

    private void validateVariables(DOMDocument domDocument, List<Diagnostic> diagnosticsList) {
        String docContent = domDocument.getTextDocument().getText();
        List<VariableLoc> variables = LibertyUtils.getVariablesFromTextContent(docContent);
        Properties variablesMap = SettingsService.getInstance().getVariablesForServerXml(domDocument.getDocumentURI());
        if (variablesMap.isEmpty() && !variables.isEmpty()) {
            String message = "WARNING: Variable resolution is not available for workspace %s. Please start the Liberty server for the workspace to enable variable resolution.";
            LibertyWorkspace workspace = LibertyProjectsManager.getInstance().getWorkspaceFolder(domDocument.getDocumentURI());
            Range range = XMLPositionUtility.createRange(domDocument.getDocumentElement().getStartTagOpenOffset(), domDocument.getDocumentElement().getStartTagCloseOffset(),
                    domDocument);
            Diagnostic diag = new Diagnostic(range, message.formatted(workspace.getWorkspaceURI().getPath()), DiagnosticSeverity.Warning, LIBERTY_LEMMINX_SOURCE);
            diagnosticsList.add(diag);
            return;
        }
        for (VariableLoc variable : variables) {
            if (!variablesMap.containsKey(variable.getValue())) {
                String variableInDoc = String.format("${%s}", variable.getValue());
                Range range = XMLPositionUtility.createRange(variable.getStartLoc()-2,variable.getEndLoc()+1,
                        domDocument);
                String message = "ERROR: The variable \"" + variable.getValue() + "\" does not exist.";

                Diagnostic diag = new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE, INCORRECT_VARIABLE_CODE);
                diag.setData(variable.getValue());
                diagnosticsList.add(diag);
            }
        }
    }



    private void validateFeaturesAndPlatforms(DOMDocument domDocument, List<Diagnostic> list, DOMNode featureManager, Set<String> includedFeatures) {

        LibertyRuntime runtimeInfo = LibertyUtils.getLibertyRuntimeInfo(domDocument);
        String libertyVersion = runtimeInfo == null ? null : runtimeInfo.getRuntimeVersion();
        String libertyRuntime = runtimeInfo == null ? null : runtimeInfo.getRuntimeType();

        final int requestDelay = SettingsService.getInstance().getRequestDelay();

        Set<String> featuresWithoutVersions = new HashSet<String>();
        Set<String> versionlessFeatures = new HashSet<String>();
        Set<String> versionedFeatures = new HashSet<String>();
        Set<String> preferredPlatforms = new HashSet<String>();
        Set<String> preferredPlatformsWithoutVersion = new HashSet<String>();
        Set<String> featureList = new HashSet<String>();
        // Search for duplicate features
        // or features that do not exist
        List<DOMNode> features = featureManager.getChildren();
        for (DOMNode featureNode : features) {
            DOMNode featureTextNode = (DOMNode) featureNode.getChildNodes().item(0);
            // check for platform element
            if (LibertyConstants.PLATFORM_ELEMENT.equals(featureNode.getLocalName())) {
                validatePlatform(domDocument, list, featureTextNode, preferredPlatformsWithoutVersion, preferredPlatforms);
            } else {
                validateFeature(domDocument, list, includedFeatures, featureTextNode, libertyVersion, libertyRuntime, requestDelay, versionedFeatures, versionlessFeatures, featuresWithoutVersions, featureList);
            }
        }
        checkForPlatFormAndFeature(domDocument, list, versionlessFeatures, features, preferredPlatforms, versionedFeatures);
    }

    private void validateFeature(DOMDocument domDocument, List<Diagnostic> list, Set<String> includedFeatures, DOMNode featureTextNode, String libertyVersion, String libertyRuntime, int requestDelay, Set<String> versionedFeatures, Set<String> versionlessFeatures, Set<String> featuresWithoutVersions, Set<String> featureList) {
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
                checkForFeatureUniqueness(domDocument, list, includedFeatures, featureTextNode, versionedFeatures, versionlessFeatures, featuresWithoutVersions, featureName, featureList);
            }
        }
    }

    /**
     * check whether feature name is unique
     * throw error if another version of same feature exists as well
     *
     * @param domDocument
     * @param list
     * @param includedFeatures
     * @param featureTextNode
     * @param versionedFeatures
     * @param versionlessFeatures
     * @param featuresWithoutVersions
     * @param featureName
     * @param featureList
     */
    private void checkForFeatureUniqueness(DOMDocument domDocument, List<Diagnostic> list, Set<String> includedFeatures, DOMNode featureTextNode, Set<String> versionedFeatures, Set<String> versionlessFeatures, Set<String> featuresWithoutVersions, String featureName, Set<String> featureList) {
        String featureNameLower = featureName.toLowerCase();
        String featureNameNoVersionLower;
        if(featureNameLower.contains("-")){
            featureNameNoVersionLower = featureNameLower.substring(0,
                    featureNameLower.lastIndexOf("-"));
            versionedFeatures.add(featureName);
        }else{
            featureNameNoVersionLower = featureNameLower;
            versionlessFeatures.add(featureName);
        }
        String featureNameNoVersion = LibertyUtils.stripVersion(featureName);
        Map<String, String> changedFeatureNameMapLower = LibertyConstants.changedFeatureNameMap.entrySet().parallelStream().collect(
                Collectors.toMap(entry -> entry.getKey().toLowerCase(),
                        entry -> entry.getValue().toLowerCase()));
        Map<String, String> changedFeatureNameMapLowerReversed =
                changedFeatureNameMapLower.entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        Set<String> featuresWithOldNames = featuresWithoutVersions.stream().map(
                        v -> changedFeatureNameMapLower.get(v + "-"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<String> featuresWithChangedNames = featuresWithoutVersions.stream().map(
                        v -> changedFeatureNameMapLowerReversed.get(v + "-"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        // if this exact feature already exists, or another version of this feature already exists, then show a diagnostic
        if (includedFeatures.contains(featureNameLower)) {
            Range range = XMLPositionUtility.createRange(featureTextNode.getStart(),
                    featureTextNode.getEnd(), domDocument);
            String message = "ERROR: " + featureName + " is already included.";
            list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE));
        } else if (featuresWithoutVersions.contains(featureNameNoVersionLower)) {
            Range range = XMLPositionUtility.createRange(featureTextNode.getStart(),
                    featureTextNode.getEnd(), domDocument);
            String message = "ERROR: More than one version of feature " + featureNameNoVersion + " is included. Only one version of a feature may be specified.";
            list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE));
        } else if (featuresWithOldNames.contains(featureNameNoVersionLower + "-")) {
            String otherFeatureName = getOtherFeatureName(featureList, changedFeatureNameMapLowerReversed, featureNameNoVersionLower);
            //check for features whose name is changed such as jsp is changed to pages
            Range range = XMLPositionUtility.createRange(featureTextNode.getStart(),
                    featureTextNode.getEnd(), domDocument);
            String message = String.format(changedFeatureNameDiagMessage, featureName, otherFeatureName,
                    LibertyUtils.stripVersion(otherFeatureName),
                    LibertyUtils.stripVersion(featureName));
            list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE));
        } else if (featuresWithChangedNames.contains(featureNameNoVersionLower + "-")) {
            String otherFeatureName = getOtherFeatureName(featureList, changedFeatureNameMapLower, featureNameNoVersionLower);
            Range range = XMLPositionUtility.createRange(featureTextNode.getStart(),
                    featureTextNode.getEnd(), domDocument);
            String message = String.format(changedFeatureNameDiagMessage, featureName, otherFeatureName,
                    LibertyUtils.stripVersion(featureName),
                    LibertyUtils.stripVersion(otherFeatureName));
            list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE));
        }
        includedFeatures.add(featureNameLower);
        featureList.add(featureName);
        featuresWithoutVersions.add(featureNameNoVersionLower);
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
            Set<String> compatibleFeaturesList = featureGraph.getAllEnabledBy(configElement);
            Set<String> includedFeaturesCopy = getCompatibleFeatures(includedFeatures, compatibleFeaturesList);
            if (includedFeaturesCopy.isEmpty()) {
                diagnosticsList.add(tempDiagnostic);
            }
        }
    }

    /**
     * get compatible features for both versioned and versionless features
     *
     * @param includedFeatures       ll selected features
     * @param compatibleFeaturesList enabled by feature list
     * @return any compitable feature list
     */
    private Set<String> getCompatibleFeatures(Set<String> includedFeatures, Set<String> compatibleFeaturesList) {
        Set<String> versionLessCompatibleFeatureList = compatibleFeaturesList.stream()
                .map(f -> LibertyUtils.stripVersion(f))
                .collect(Collectors.toSet());
        return includedFeatures.stream().filter(included ->
                compatibleFeaturesList.contains(included) || versionLessCompatibleFeatureList.contains(included)
        ).collect(Collectors.toSet());
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
     * @param preferredPlatformsWithoutVersion platforms in xml without version
     * @param preferredPlatforms platforms in xml
     */
    private void validatePlatform(DOMDocument domDocument, List<Diagnostic> list, DOMNode featureTextNode, Set<String> preferredPlatformsWithoutVersion,
                                         Set<String> preferredPlatforms) {
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
                list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE, INCORRECT_PLATFORM_CODE));
            }
            // if this exact platform already exists, or another version of this feature already exists, then show a diagnostic
            else {
                checkForPlatformUniqueness(domDocument, list, featureTextNode, preferredPlatformsWithoutVersion, preferredPlatforms, platformNameLowerCase, platformName);
                Set<String> conflictingPlatforms = getConflictingPlatforms(platformNoVersionLower, preferredPlatforms);
                if (!conflictingPlatforms.isEmpty()) {
                    Range range = XMLPositionUtility.createRange(featureTextNode.getStart(),
                            featureTextNode.getEnd(), domDocument);
                    conflictingPlatforms.add(platformName);
                    String message = "ERROR: The following configured platform versions are in conflict " + conflictingPlatforms;
                    list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE));
                }
                preferredPlatformsWithoutVersion.add(platformNoVersionLower);
                preferredPlatforms.add(platformNameLowerCase);

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
     * @param preferredPlatformsWithoutVersion
     * @param preferredPlatforms
     * @param platformNameLowerCase
     * @param platformName
     * @param platformNoVersionLower
     */
    private void checkForPlatformUniqueness(DOMDocument domDocument, List<Diagnostic> list, DOMNode featureTextNode, Set<String> preferredPlatformsWithoutVersion, Set<String> preferredPlatforms, String platformNameLowerCase, String platformName) {
        String platformNoVersionLower = platformNameLowerCase.contains("-") ? platformNameLowerCase.substring(0, platformNameLowerCase.lastIndexOf("-"))
                : platformNameLowerCase;
        if (preferredPlatforms.contains(platformNameLowerCase)) {
            Range range = XMLPositionUtility.createRange(featureTextNode.getStart(),
                    featureTextNode.getEnd(), domDocument);
            String message = "ERROR: " + platformName + " is already included.";
            list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE));
        } else if (preferredPlatformsWithoutVersion.contains(platformNoVersionLower)) {
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
     * @param preferredPlatforms
     * @return
     */
    private Set<String> getConflictingPlatforms(String platformNoVersionLower, Set<String> preferredPlatforms) {
        if(LibertyConstants.conflictingPlatforms.containsKey(platformNoVersionLower)){
            String conflictingPlatformName = LibertyConstants.conflictingPlatforms.get(platformNoVersionLower);
            return preferredPlatforms.stream()
                    .filter(p->p.contains(conflictingPlatformName)).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }


    /**
     * check for a combination of validations using preferred platforms and selected features
     *
     * @param domDocument
     * @param list
     * @param versionlessFeatures
     * @param features
     * @param preferredPlatforms
     * @param versionedFeatures
     */
    private void checkForPlatFormAndFeature(DOMDocument domDocument, List<Diagnostic> list, Set<String> versionlessFeatures, List<DOMNode> features, Set<String> preferredPlatforms, Set<String> versionedFeatures) {

        for (DOMNode featureNode : features) {
            DOMNode featureTextNode = (DOMNode) featureNode.getChildNodes().item(0);
            if (LibertyConstants.FEATURE_ELEMENT.equals(featureNode.getLocalName())
                    && featureTextNode != null && featureTextNode.getTextContent() != null) {
                String featureName = featureTextNode.getTextContent().trim();
                if (versionlessFeatures.contains(featureName)) {
                    // versionless feature
                    validateVersionLessFeatures(domDocument, list, preferredPlatforms, versionedFeatures, featureTextNode, featureName);
                }
            }

        }
    }

    /**
     * check for versionless feature version identification
     *      1) if versionless feature is specified and no versioned feature or platform is specified, throw error
     *      2) if versionless feature is specified and no platform is specified and atleast one versioned feature is specified
     *          if no common platform is found for all versioned features, throw error
     *          if there are more than one common platform, throw error
     *          if common platform is found, then
     *              find all required and required tolerates features for versionless feature
     *              find all platforms for above features
     *              check common platforms is present in feature platforms
     *              throw error if not found
     *
     *      3) if versionless feature is specified and platform(s) is specified
     *          find all required and required tolerates features for versionless feature
     *          find all platforms for above features
     *          check common platforms is present in feature platforms
     *          throw error if not found
     *
     * @param domDocument
     * @param list
     * @param preferredPlatforms
     * @param versionedFeatures
     * @param versionLessFeatureTextNode
     * @param featureName
     */
    private void validateVersionLessFeatures(DOMDocument domDocument, List<Diagnostic> list, Set<String> preferredPlatforms, Set<String> versionedFeatures,DOMNode versionLessFeatureTextNode, String featureName) {
        LibertyRuntime runtimeInfo = LibertyUtils.getLibertyRuntimeInfo(domDocument);
        String libertyVersion = runtimeInfo == null ? null : runtimeInfo.getRuntimeVersion();
        String libertyRuntime = runtimeInfo == null ? null : runtimeInfo.getRuntimeType();

        final int requestDelay = SettingsService.getInstance().getRequestDelay();
        if (versionedFeatures.isEmpty() && preferredPlatforms.isEmpty()) {
            Range range = XMLPositionUtility.createRange(versionLessFeatureTextNode.getStart(),
                    versionLessFeatureTextNode.getEnd(), domDocument);
                String message = "ERROR: The " + featureName + " versionless feature cannot be resolved. Specify a platform or a feature with a version to enable resolution.";
            list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE, INCORRECT_FEATURE_CODE));
        } else if (!versionedFeatures.isEmpty() && preferredPlatforms.isEmpty()) {
            Set<String> commonPlatforms = FeatureService.getInstance()
                    .getCommonPlatformsForFeatures(versionedFeatures, libertyVersion, libertyRuntime, requestDelay, domDocument.getDocumentURI());
            if (commonPlatforms == null ||
                    commonPlatforms.isEmpty()) {
                Range range = XMLPositionUtility.createRange(versionLessFeatureTextNode.getStart(), versionLessFeatureTextNode.getEnd(),
                        domDocument);
                String message = "ERROR: \"" + featureName + "\" versionless feature cannot be resolved. The versioned features do not have a platform in common.";
                list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE, INCORRECT_FEATURE_CODE));
            }
            else if(commonPlatforms.size()>1) {
                Range range = XMLPositionUtility.createRange(versionLessFeatureTextNode.getStart(), versionLessFeatureTextNode.getEnd(),
                        domDocument);
                String message = "ERROR: The \"" + featureName + "\" versionless feature cannot be resolved since there are more than one common platform. Specify a platform or a feature with a version to enable resolution.";
                list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE, INCORRECT_FEATURE_CODE));
            }
            else{
                checkForVersionlessPlatforms(domDocument, list, commonPlatforms, versionLessFeatureTextNode, featureName, false);
            }
        }
        if (!preferredPlatforms.isEmpty()) {
            checkForVersionlessPlatforms(domDocument, list, preferredPlatforms, versionLessFeatureTextNode, featureName,true);
        }
    }


    private void checkForVersionlessPlatforms(DOMDocument domDocument,
                                              List<Diagnostic> list, Set<String> platformsToCompare,
                                              DOMNode versionLessFeatureTextNode, String featureName, boolean isPlatformFromXml) {
        LibertyRuntime runtimeInfo = LibertyUtils.getLibertyRuntimeInfo(domDocument);
        String libertyVersion = runtimeInfo == null ? null : runtimeInfo.getRuntimeVersion();
        String libertyRuntime = runtimeInfo == null ? null : runtimeInfo.getRuntimeType();
        final int requestDelay = SettingsService.getInstance().getRequestDelay();
        Set<String> allPlatforrmsForVersionLess = FeatureService.getInstance()
                .getAllPlatformsForVersionLessFeature(featureName, libertyVersion, libertyRuntime, requestDelay, domDocument.getDocumentURI());
        if (Sets.intersection(allPlatforrmsForVersionLess, platformsToCompare).isEmpty()) {
            String message;
            if (isPlatformFromXml) {
                message = "ERROR: The \"" + featureName + "\" versionless feature does not have a configured platform.";
            } else {
                message = "ERROR: The \"" + featureName + "\" versionless feature cannot be resolved. Specify a platform or a versioned feature from a supported platform to enable resolution.";
            }
            Range range = XMLPositionUtility.createRange(versionLessFeatureTextNode.getStart(), versionLessFeatureTextNode.getEnd(),
                    domDocument);
            list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE, INCORRECT_FEATURE_CODE));
        }
    }

    /**
     * Get conflicting featurename for any feature
     * @param includedFeatures
     * @param changedFeatureNameMap
     * @param featureNameNoVersionLower
     * @return
     */
    private static String getOtherFeatureName(Set<String> includedFeatures, Map<String, String> changedFeatureNameMapLowerReversed, String featureNameNoVersionLower) {
        String otherFeatureNameWithoutVersion = changedFeatureNameMapLowerReversed.get(featureNameNoVersionLower + "-");
        String otherFeatureName = includedFeatures
                .stream()
                .filter(f -> f.toLowerCase().contains(LibertyUtils.stripVersion(otherFeatureNameWithoutVersion)))
                .findFirst().orElse(null);
        return otherFeatureName;
    }
}
