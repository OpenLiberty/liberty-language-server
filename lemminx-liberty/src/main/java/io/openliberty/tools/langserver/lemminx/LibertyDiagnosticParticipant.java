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
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class LibertyDiagnosticParticipant implements IDiagnosticsParticipant {

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
                monitorConfigFiles(domDocument, list, node);
            }
        }
        
    }

    private void validateFeature(DOMDocument domDocument, List<Diagnostic> list, DOMNode node) {
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

    private void monitorConfigFiles(DOMDocument domDocument, List<Diagnostic> list, DOMNode node) {
        String locAttribute = node.getAttribute("location");
        if (locAttribute == null) {
            return;
        }
        String docURIString = domDocument.getDocumentURI().replace(File.separator, "/");    // URI requires
        locAttribute = locAttribute.replace(File.separator, "/");
        File configFile = locAttribute.startsWith("./") ? 
                new File(URI.create(docURIString.substring(0, docURIString.lastIndexOf(File.separator) + 1))
                        .resolve(locAttribute).normalize()) :
                new File(locAttribute);

        DOMNode locNode = node.getAttributeNode("location");
        Range range = XMLPositionUtility.createRange(locNode.getStart(), locNode.getEnd(), domDocument);
        try {
            if (configFile.exists()) {
                LibertyProjectsManager.getInstance().getWorkspaceFolder(docURIString).addConfigFile(configFile.getCanonicalPath());
            } else {
                String message = "The file at the specified location could not be found.";
                list.add(new Diagnostic(range, message, DiagnosticSeverity.Hint, "liberty-lemminx"));
            }
        } catch (IllegalArgumentException | IOException e) {
            String message = "The file at the specified location could not be found.";
            list.add(new Diagnostic(range, message, DiagnosticSeverity.Hint, "liberty-lemminx-exception"));
        }
    }
}
