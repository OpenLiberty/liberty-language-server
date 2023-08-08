/*******************************************************************************
* Copyright (c) 2020, 2022 IBM Corporation and others.
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
import io.openliberty.tools.langserver.lemminx.services.FeatureService;
import io.openliberty.tools.langserver.lemminx.services.LibertyProjectsManager;
import io.openliberty.tools.langserver.lemminx.services.SettingsService;
import io.openliberty.tools.langserver.lemminx.util.*;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LibertyDiagnosticParticipant implements IDiagnosticsParticipant {
    public static final String MISSING_FILE_MESSAGE = "The resource at the specified location could not be found.";
    public static final String MISSING_FILE_CODE = "missing_file";

    public static final String NOT_OPTIONAL_MESSAGE = "The specified resource cannot be skipped. Check location value or set optional to true.";
    public static final String NOT_OPTIONAL_CODE = "not_optional";
    public static final String IMPLICIT_NOT_OPTIONAL_MESSAGE = "The specified resource cannot be skipped. Check location value or add optional attribute.";
    public static final String IMPLICIT_NOT_OPTIONAL_CODE = "implicit_not_optional";
    
    @Override
    public void doDiagnostics(DOMDocument domDocument, List<Diagnostic> diagnostics,
            XMLValidationSettings validationSettings, CancelChecker cancelChecker) {
        if (!LibertyUtils.isConfigXMLFile(domDocument))
            return;
        try {
            validateDom(domDocument, diagnostics);
        } catch (IOException e) {
            System.err.println("Error validating document " + domDocument.getDocumentURI());
            System.err.println(e.getMessage());
        }
    }

    private void validateDom(DOMDocument domDocument, List<Diagnostic> list) throws IOException {
        List<DOMNode> nodes = domDocument.getDocumentElement().getChildren();

        for (DOMNode node : nodes) {
            if (LibertyConstants.FEATURE_MANAGER_ELEMENT.equals(node.getNodeName())) {
                validateFeature(domDocument, list, node);
            } else if (LibertyConstants.INCLUDE_ELEMENT.equals(node.getNodeName())) {
                validateIncludeLocation(domDocument, list, node);
            }
        }
        
    }

    private void validateFeature(DOMDocument domDocument, List<Diagnostic> list, DOMNode featureManager) {
        String libertyRuntimeVersionInfo = LibertyUtils.getRuntimeAndVersionInfo(domDocument);
        String libertyVersion =  LibertyUtils.getVersionFromInfo(libertyRuntimeVersionInfo);
        String libertyRuntime =  LibertyUtils.getRuntimeFromInfo(libertyRuntimeVersionInfo);

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
                    list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, "liberty-lemminx"));
                } else {
                    if (includedFeatures.contains(featureName)) {
                        Range range = XMLPositionUtility.createRange(featureTextNode.getStart(),
                                featureTextNode.getEnd(), domDocument);
                        String message = "ERROR: " + featureName + " is already included.";
                        list.add(new Diagnostic(range, message, DiagnosticSeverity.Error, "liberty-lemminx"));
                    } else {
                        includedFeatures.add(featureName);
                    }
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
    private void validateIncludeLocation(DOMDocument domDocument, List<Diagnostic> list, DOMNode node) {
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
            list.add(new Diagnostic(range, message, DiagnosticSeverity.Warning, "liberty-lemminx"));
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
                    list.add(new Diagnostic(range, IMPLICIT_NOT_OPTIONAL_MESSAGE, DiagnosticSeverity.Error, "liberty-lemminx", IMPLICIT_NOT_OPTIONAL_CODE));
                } else if (optNode.getValue().equals("false")) {
                    Range optRange = XMLPositionUtility.createRange(optNode.getStart(), optNode.getEnd(), domDocument);
                    list.add(new Diagnostic(optRange, NOT_OPTIONAL_MESSAGE, DiagnosticSeverity.Error, "liberty-lemminx", NOT_OPTIONAL_CODE));
                }
                list.add(new Diagnostic(range, MISSING_FILE_MESSAGE, DiagnosticSeverity.Warning, "liberty-lemminx", MISSING_FILE_CODE));
            }
        } catch (IllegalArgumentException e) {
            list.add(new Diagnostic(range, MISSING_FILE_MESSAGE, DiagnosticSeverity.Warning, "liberty-lemminx-exception", MISSING_FILE_CODE));
        }
    }
}
