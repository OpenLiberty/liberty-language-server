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
import java.util.*;

public class LibertyDiagnosticParticipant implements IDiagnosticsParticipant {

    @Override
    public void doDiagnostics(DOMDocument domDocument, List<Diagnostic> diagnostics,
            XMLValidationSettings validationSettings, CancelChecker cancelChecker) {
        if (!LibertyUtils.isConfigXMLFile(domDocument))
            return;
        try {
            validateFeatures(domDocument, diagnostics);
        } catch (IOException e) {
            System.err.println("Error validating features");
            System.err.println(e.getMessage());
        }
    }

    private void validateFeatures(DOMDocument domDocument, List<Diagnostic> list) throws IOException {
        List<DOMNode> nodes = domDocument.getDocumentElement().getChildren();
        // DOMNode featureManager = null;
        // find <featureManager> element if it exists
        for (DOMNode node : nodes) {
            if (LibertyConstants.FEATURE_MANAGER_ELEMENT.equals(node.getNodeName())) {
                validateFeature(domDocument, list, node);
                // featureManager = node;
                // break;
            }
            if (LibertyConstants.INCLUDE_ELEMENT.equals(node.getNodeName())) {
                scanIncludeConfigFiles(domDocument, list, node);
            }

        }
        
    }

    private void validateFeature(DOMDocument domDocument, List<Diagnostic> list, DOMNode node) {
        // No need for validation if there is no <featureManager>
        if (node == null) {
            return;
        }

        String libertyVersion =  LibertyUtils.getVersion(domDocument);

        final int requestDelay = SettingsService.getInstance().getRequestDelay();

        // Search for duplicate features
        // or features that do not exist
        Set<String> includedFeatures = new HashSet<>();
        List<DOMNode> features = node.getChildren();
        for (DOMNode featureNode : features) {
            DOMNode featureTextNode = (DOMNode) featureNode.getChildNodes().item(0);
            // skip nodes that do not have any text value (ie. comments)
            if (featureTextNode != null && featureTextNode.getTextContent() != null) {
                String featureName = featureTextNode.getTextContent();
                // if the feature is not a user defined feature and the feature does not exist in the list of
                // supported features show a "Feature does not exist" diagnostic
                if (!featureName.startsWith("usr:") && !FeatureService.getInstance().featureExists(featureName, libertyVersion, requestDelay, domDocument.getDocumentURI())) {
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
     * Scans for configuration resource files and adds them into a growing workspace cache.
     * 
     * 1) Currently, this feature doesn't concern with disabling lang server support on invalidated config files.
     * 2) As part of the Diagnostics stack, paths to config resources get added only if the file exists.
     * @param domDocument
     * @param list
     * @param node
     * @throws IOException
     */
    private void scanIncludeConfigFiles(DOMDocument domDocument, List<Diagnostic> list, DOMNode node) throws IOException {
        String docXML = domDocument.getDocumentURI();
        String configFilePath = docXML.substring(0, docXML.lastIndexOf(File.separator) + 1) + node.getAttribute("location");
        File configFile = new File(configFilePath);
        if (configFilePath != null && configFile.exists()) {
            Range range = XMLPositionUtility.createRange(node.getStart(), node.getEnd(), domDocument);
            String message = "INFO: Detected config resource " + configFile.getCanonicalPath();
            LibertyProjectsManager.getInstance().getWorkspaceFolder(docXML).addConfigFile(configFile.getCanonicalPath());
            list.add(new Diagnostic(range, message, DiagnosticSeverity.Information, "liberty-lemminx"));
        }
    }
}
