/*******************************************************************************
* Copyright (c) 2020, 2025 IBM Corporation and others.
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
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class LibertyDiagnosticParticipant implements IDiagnosticsParticipant {
    private static final Logger LOGGER = Logger.getLogger(LibertyDiagnosticParticipant.class.getName());

    public static final String LIBERTY_LEMMINX_SOURCE = "liberty-lemminx";

    public static final String NOT_XML_OR_DIR = "The specified resource is not an XML file. If it is a directory, it must end with a trailing slash.";

    public static final String MISSING_FILE_CODE = "missing_file";

    public static final String MISSING_CONFIGURED_FEATURE_CODE = "lost_config_element";

    public static final String NOT_OPTIONAL_CODE = "not_optional";
    public static final String IMPLICIT_NOT_OPTIONAL_CODE = "implicit_not_optional";

    public static final String IS_FILE_NOT_DIR_CODE = "is_file_not_dir";
    public static final String Is_DIR_NOT_FILE_CODE = "is_dir_not_file";

    public static final String INCORRECT_FEATURE_CODE = "incorrect_feature";
    public static final String INCORRECT_PLATFORM_CODE = "incorrect_platform";
    public static final String INCORRECT_VARIABLE_CODE = "incorrect_variable";

    public static final String DUPLICATE_FEATURE_CODE = "multiple_features";
    public static final String FEATURE_NAME_CHANGED_CODE = "feature_name_changed";

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
            LibertyWorkspace workspace = LibertyProjectsManager.getInstance().getWorkspaceFolder(domDocument.getDocumentURI());
            String message = ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.WARN_VARIABLE_RESOLUTION_NOT_AVAILABLE, workspace.getWorkspaceURI().getPath());
            Range range = XMLPositionUtility.createRange(domDocument.getDocumentElement().getStartTagOpenOffset(), domDocument.getDocumentElement().getStartTagCloseOffset(),
                    domDocument);
            Diagnostic diag = new Diagnostic(range, message, DiagnosticSeverity.Warning, LIBERTY_LEMMINX_SOURCE);
            diagnosticsList.add(diag);
            // set config copied to server as false, so that hover or completion do not add variables into server.xml variablesMap
            SettingsService.getInstance().setConfigCopiedToServer(false);
            return;
        }
        // set config copied to server for checkAndAddNewVariables()
        SettingsService.getInstance().setConfigCopiedToServer(true);
        LibertyUtils.checkAndAddNewVariables(domDocument, variablesMap);
        validateVariableExists(domDocument, diagnosticsList, variables, variablesMap);
        validateVariableDataTypeValues(domDocument,diagnosticsList,variablesMap);
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

        // Feature compatibility validation
        validateFeatureCompatibility(domDocument, list);
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
                String message = ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.ERR_FEATURE_NOT_EXIST, featureName);
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
            String message = ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.ERR_FEATURE_ALREADY_INCLUDED, featureName);
            list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE));
        } else if (featuresWithoutVersions.contains(featureNameNoVersionLower)) {
            Range range = XMLPositionUtility.createRange(featureTextNode.getStart(),
                    featureTextNode.getEnd(), domDocument);
            String message = ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.ERR_FEATURE_MULTIPLE_VERSIONS, featureNameNoVersion);
            list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE, DUPLICATE_FEATURE_CODE));
        } else if (featuresWithOldNames.contains(featureNameNoVersionLower + "-")) {
            String otherFeatureName = getOtherFeatureName(featureList, changedFeatureNameMapLowerReversed, featureNameNoVersionLower);
            //check for features whose name is changed such as jsp is changed to pages
            Range range = XMLPositionUtility.createRange(featureTextNode.getStart(),
                    featureTextNode.getEnd(), domDocument);
            String message = ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.ERR_FEATURE_NAME_CHANGED, featureName, otherFeatureName,
                    LibertyUtils.stripVersion(otherFeatureName),
                    LibertyUtils.stripVersion(featureName));
            list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE, FEATURE_NAME_CHANGED_CODE));
        } else if (featuresWithChangedNames.contains(featureNameNoVersionLower + "-")) {
            String otherFeatureName = getOtherFeatureName(featureList, changedFeatureNameMapLower, featureNameNoVersionLower);
            Range range = XMLPositionUtility.createRange(featureTextNode.getStart(),
                    featureTextNode.getEnd(), domDocument);
            String message = ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.ERR_FEATURE_NAME_CHANGED, featureName, otherFeatureName,
                    LibertyUtils.stripVersion(featureName),
                    LibertyUtils.stripVersion(otherFeatureName));
            list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE, FEATURE_NAME_CHANGED_CODE));
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
                    String message = ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.ERR_IMPLICIT_NOT_OPTIONAL_MESSAGE);
                    diagnosticsList.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE, IMPLICIT_NOT_OPTIONAL_CODE));
                } else if (optNode.getValue().equals("false")) {
                    Range optRange = XMLPositionUtility.createRange(optNode.getStart(), optNode.getEnd(), domDocument);
                    String message = ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.ERR_NOT_OPTIONAL_MESSAGE);
                    diagnosticsList.add(new Diagnostic(optRange, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE, NOT_OPTIONAL_CODE));
                }
                String message = ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.WARN_MISSING_FILE_MESSAGE);
                diagnosticsList.add(new Diagnostic(range, message, DiagnosticSeverity.Warning, LIBERTY_LEMMINX_SOURCE, MISSING_FILE_CODE));
            }
            validateFileOrDirIncludeLocation(configFile, isLibertyDirectory, range, diagnosticsList);
        } catch (IllegalArgumentException e) {
            String message = ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.WARN_MISSING_FILE_MESSAGE);
            diagnosticsList.add(new Diagnostic(range, message, DiagnosticSeverity.Warning, "liberty-lemminx-exception", MISSING_FILE_CODE));
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
            String message = ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.ERR_SPECIFIED_DIR_IS_FILE);
            diagnosticsList.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE, IS_FILE_NOT_DIR_CODE));
        } else if (f.isDirectory() && !isLibertyDirectory) {
            String message = ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.ERR_SPECIFIED_FILE_IS_DIR);
            diagnosticsList.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE, Is_DIR_NOT_FILE_CODE));
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
        String message = ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.ERR_MISSING_CONFIGURED_FEATURE_MESSAGE);
        Diagnostic tempDiagnostic = new Diagnostic(range, message, DiagnosticSeverity.Warning, LIBERTY_LEMMINX_SOURCE, MISSING_CONFIGURED_FEATURE_CODE);
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
                String message = ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.ERR_PLATFORM_NOT_EXIST, platformName);
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
                    String message = ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.ERR_PLATFORMS_IN_CONFLICT, conflictingPlatforms);
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
     */
    private void checkForPlatformUniqueness(DOMDocument domDocument, List<Diagnostic> list, DOMNode featureTextNode, Set<String> preferredPlatformsWithoutVersion, Set<String> preferredPlatforms, String platformNameLowerCase, String platformName) {
        String platformNoVersionLower = platformNameLowerCase.contains("-") ? platformNameLowerCase.substring(0, platformNameLowerCase.lastIndexOf("-"))
                : platformNameLowerCase;
        if (preferredPlatforms.contains(platformNameLowerCase)) {
            Range range = XMLPositionUtility.createRange(featureTextNode.getStart(),
                    featureTextNode.getEnd(), domDocument);
            String message = ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.ERR_PLATFORM_ALREADY_INCLUDED, platformName);
            list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE));
        } else if (preferredPlatformsWithoutVersion.contains(platformNoVersionLower)) {
            Range range = XMLPositionUtility.createRange(featureTextNode.getStart(),
                    featureTextNode.getEnd(), domDocument);
            String platformNameNoVersion = platformName.substring(0, platformName.lastIndexOf("-"));
            String message = ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.ERR_PLATFORM_MULTIPLE_VERSIONS, platformNameNoVersion);
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
            String message = ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.ERR_VERSIONLESS_FEATURE_NO_PLATFORM_OR_FEATURE, featureName);
            list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE, INCORRECT_FEATURE_CODE));
        } else if (!versionedFeatures.isEmpty() && preferredPlatforms.isEmpty()) {
            Set<String> commonPlatforms = FeatureService.getInstance()
                    .getCommonPlatformsForFeatures(versionedFeatures, libertyVersion, libertyRuntime, requestDelay, domDocument.getDocumentURI());
            if (commonPlatforms == null ||
                    commonPlatforms.isEmpty()) {
                Range range = XMLPositionUtility.createRange(versionLessFeatureTextNode.getStart(), versionLessFeatureTextNode.getEnd(),
                        domDocument);
                String message = ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.ERR_VERSIONLESS_FEATURE_NO_COMMON_PLATFORM, featureName);
                list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE, INCORRECT_FEATURE_CODE));
            }
            else if(commonPlatforms.size()>1) {
                Range range = XMLPositionUtility.createRange(versionLessFeatureTextNode.getStart(), versionLessFeatureTextNode.getEnd(),
                        domDocument);
                String message = ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.ERR_VERSIONLESS_FEATURE_MULTIPLE_COMMON_PLATFORM, featureName);
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
                message = ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.ERR_VERSIONLESS_FEATURE_NO_CONFIGURED_PLATFORM, featureName);
            } else {
                message = ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.ERR_VERSIONLESS_FEATURE_NO_SUPPORTED_PLATFORM, featureName);
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


    /**
     * Validate variable node attributes and values
     * TODO add DataType level validations for each variables
     * ie, make sure any variable used is of correct datatype
     * for example, when a variable is used in one xml element and the valid value is number, but variable defined value is NaN
     * @param domDocument xml document
     * @param diagnosticsList existing diagnostic list
     * @param variablesMap populated variables from all liberty config files
     */
    private void validateVariableDataTypeValues(DOMDocument domDocument, List<Diagnostic> diagnosticsList, Properties variablesMap) {
        List<DOMNode> nodes = domDocument.getDocumentElement().getChildren();

        for (DOMNode node : nodes) {
            String nodeName = node.getNodeName();
            if (LibertyConstants.VARIABLE_ELEMENT.equals(nodeName)) {
                String varName =  node.getAttribute("name");
                Range range = XMLPositionUtility.createRange(node.getStart(), node.getEnd(),
                        domDocument);
                if (!varName.isEmpty()) {
                    validateVariableValueNonEmpty(diagnosticsList, node, varName, range);
                }
                else{
                    String message = ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.ERR_VARIABLE_INVALID_NAME_ATTRIBUTE);
                    Diagnostic diag = new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE, INCORRECT_VARIABLE_CODE);
                    diagnosticsList.add(diag);
                }
            }
        }
    }

    /**
     * validate variable value is not null or empty
     * @param diagnosticsList diagnostics list
     * @param varNode current variable DomNode
     * @param varName current variable Name Attribute value
     * @param range diagnostics range
     */
    private static void validateVariableValueNonEmpty(List<Diagnostic> diagnosticsList, DOMNode varNode, String varName, Range range) {
        // A variable can have either a value attribute OR a defaultValue attribute.
        String varValue = varNode.getAttribute("value");
        String varDefaultValue = varNode.getAttribute("defaultValue");
        if(varValue==null && varDefaultValue==null){
            String message = ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.ERR_VARIABLE_INVALID_VALUE_ATTRIBUTE, varName);
            Diagnostic diag = new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE, INCORRECT_VARIABLE_CODE);
            diagnosticsList.add(diag);
        }
        else if((varValue!=null && varValue.trim().isEmpty())||(varDefaultValue!=null && varDefaultValue.trim().isEmpty())){
            String message = ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.WARN_VARIABLE_INVALID_VALUE, varName);
            Diagnostic diag = new Diagnostic(range, message, DiagnosticSeverity.Warning, LIBERTY_LEMMINX_SOURCE, INCORRECT_VARIABLE_CODE);
            diagnosticsList.add(diag);
        }
    }

    /**
     * validate variable is defined for any usage
     * @param domDocument xml document
     * @param diagnosticsList diagnostics list
     * @param variables variables in use in server xml
     * @param variablesMap all variables defined in liberty config files
     */
    private static void validateVariableExists(DOMDocument domDocument, List<Diagnostic> diagnosticsList, List<VariableLoc> variables, Properties variablesMap) {
        for (VariableLoc variable : variables) {
            if (!variablesMap.containsKey(variable.getValue())) {
                //range is used in ReplaceVariable to provide quick fix.
                // we just need the variable value range here as ${} is added in replace variable message
                Range range = XMLPositionUtility.createRange(variable.getStartLoc() - 2, variable.getEndLoc() + 1,
                        domDocument);
                String message = ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.ERR_VARIABLE_NOT_EXIST, variable.getValue());
                Diagnostic diag = new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE, INCORRECT_VARIABLE_CODE);
                diag.setData(variable.getValue());
                diagnosticsList.add(diag);
            }
        }
    }

    /**
     * Validates feature compatibility by checking if features share common platforms
     * within each platform type (Java EE, Jakarta EE, MicroProfile).
     *
     * @param domDocument The DOM document (server.xml)
     * @param diagnosticsList List to add diagnostics to
     */
    private void validateFeatureCompatibility(DOMDocument domDocument, List<Diagnostic> diagnosticsList) {
        // Find the featureManager element
        DOMNode featureManagerNode = LibertyUtils.getFeatureManagerElement(domDocument);
        if (featureManagerNode == null) {
            return; // No featureManager element found
        }

        // Get Liberty version and runtime from the document
        LibertyRuntime runtimeInfo = LibertyUtils.getLibertyRuntimeInfo(domDocument);
        String libertyVersion = runtimeInfo == null ? null : runtimeInfo.getRuntimeVersion();
        String libertyRuntime = runtimeInfo == null ? null : runtimeInfo.getRuntimeType();
        int requestDelay = SettingsService.getInstance().getRequestDelay();

        // Get all features and their nodes from the featureManager
        Map<String, DOMNode> featureNodes = LibertyUtils.getAllFeatureNodes(featureManagerNode);
        List<String> configuredFeatures = new ArrayList<>(featureNodes.keySet());

        if (configuredFeatures.size() <= 1) {
            return; // Need at least two features to check compatibility
        }

        // Skip if already there are some diagnostics (This is implemented to ensure that some of the Unit Tests are not broken).
        if (diagnosticsList != null && !diagnosticsList.isEmpty()) {
            for (Diagnostic diagnostic : diagnosticsList) {
                if (diagnostic.getCode() != null
                        && List.of(DUPLICATE_FEATURE_CODE, FEATURE_NAME_CHANGED_CODE).contains(diagnostic.getCode().getLeft())) {
                    return;
                }
            }
        }

        // Maps to track features by platform type (Assuming there will be only three types of platforms)
        Map<String, Map<String, Object>> javaEEFeatures = new HashMap<>();
        Map<String, Map<String, Object>> jakartaEEFeatures = new HashMap<>();
        Map<String, Map<String, Object>> microProfileFeatures = new HashMap<>();

        // Categorize features by platform type
        for (String feature : configuredFeatures) {
            Set<String> platforms = FeatureService.getInstance().getAllPlatformsForFeature(feature, libertyVersion, libertyRuntime, requestDelay, domDocument.getDocumentURI());
            DOMNode featureNode = featureNodes.get(feature);

            if (platforms == null || platforms.isEmpty()) {
                continue;
            }

            // Group features by platform type
            for (String platform : platforms) {
                if (platform.toLowerCase().startsWith("javaee-")) {
                    Map<String, Object> featureData = javaEEFeatures.computeIfAbsent(feature, k -> new HashMap<>());
                    featureData.computeIfAbsent("features", k -> new HashSet<String>());
                    ((Set<String>) featureData.get("features")).add(platform);
                    // Store feature node
                    featureData.put("node", featureNode);
                } else if (platform.toLowerCase().startsWith("jakartaee-")) {
                    Map<String, Object> featureData = jakartaEEFeatures.computeIfAbsent(feature, k -> new HashMap<>());
                    featureData.computeIfAbsent("features", k -> new HashSet<String>());
                    ((Set<String>) featureData.get("features")).add(platform);
                    // Store feature node
                    featureData.put("node", featureNode);
                } else if (platform.toLowerCase().startsWith("microprofile-")) {
                    Map<String, Object> featureData = microProfileFeatures.computeIfAbsent(feature, k -> new HashMap<>());
                    featureData.computeIfAbsent("features", k -> new HashSet<String>());
                    ((Set<String>) featureData.get("features")).add(platform);
                    // Store feature node
                    featureData.put("node", featureNode);
                }
            }
        }

        // Validate compatibility within each platform type
        validatePlatformTypeCompatibility(javaEEFeatures, "Java EE", featureManagerNode, diagnosticsList, libertyVersion, libertyRuntime, requestDelay, domDocument);
        validatePlatformTypeCompatibility(jakartaEEFeatures, "Jakarta EE", featureManagerNode, diagnosticsList, libertyVersion, libertyRuntime, requestDelay, domDocument);
        validatePlatformTypeCompatibility(microProfileFeatures, "MicroProfile", featureManagerNode, diagnosticsList, libertyVersion, libertyRuntime, requestDelay, domDocument);
    }

    /**
     * Validates compatibility of features within a specific platform type.
     *
     * @param featureMap Map of features and their supported platforms
     * @param platformType The platform type name (Java EE, Jakarta EE, MicroProfile)
     * @param featureManagerNode The featureManager DOM node
     * @param diagnosticsList List to add diagnostics to
     * @param libertyVersion Liberty version
     * @param libertyRuntime Liberty runtime
     * @param requestDelay Request delay for feature service
     * @param domDocument The DOM document
     */
    private void validatePlatformTypeCompatibility(Map<String, Map<String, Object>> featureMap, String platformType,
                                                   DOMNode featureManagerNode, List<Diagnostic> diagnosticsList,
                                                   String libertyVersion, String libertyRuntime, int requestDelay,
                                                   DOMDocument domDocument) {
        if (featureMap.size() <= 1) {
            // Only one or zero features of this type, no compatibility issues
            return;
        }

        // Get all features in this platform type
        Set<String> featureNames = featureMap.keySet();

        // Find common platforms across all features in this platform type
        Set<String> commonPlatforms = FeatureService.getInstance().getCommonPlatformsForFeatures(featureNames, libertyVersion, libertyRuntime, requestDelay, domDocument.getDocumentURI());

        if (commonPlatforms == null || commonPlatforms.isEmpty()) {
            // No common platform version - incompatible features

            // Create diagnostic warning for each incompatible feature
            for (Map.Entry<String, Map<String, Object>> entry : featureMap.entrySet()) {
                DOMNode featureNode = (DOMNode) entry.getValue().get("node");
                String featureName = featureNode.getChildren().get(0).getTextContent();
                String otherFeatures = featureMap.keySet().stream().filter(key -> !key.equals(featureName)).map(Objects::toString).collect(Collectors.joining(","));
                String message = ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.ERR_INCOMPATIBLE_FEATURES, featureName, otherFeatures);
                if (featureNode != null) {
                    Range range = XMLPositionUtility.createRange(featureNode.getStart(), featureNode.getEnd(), domDocument);
                    Diagnostic diagnostic = new Diagnostic(range, message, DiagnosticSeverity.Error, LIBERTY_LEMMINX_SOURCE);
                    diagnosticsList.add(diagnostic);
                }
            }
        }
    }
}
