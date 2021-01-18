package io.openliberty.lemminx.liberty;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLValidationSettings;
import org.eclipse.lemminx.services.extensions.diagnostics.IDiagnosticsParticipant;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import io.openliberty.lemminx.liberty.services.FeatureService;
import io.openliberty.lemminx.liberty.services.SettingsService;
import io.openliberty.lemminx.liberty.util.*;
import java.io.IOException;
import java.util.*;

public class LibertyDiagnosticParticipant implements IDiagnosticsParticipant {

    @Override
    public void doDiagnostics(DOMDocument domDocument, List<Diagnostic> diagnostics,
            XMLValidationSettings validationSettings, CancelChecker cancelChecker) {
        if (!LibertyUtils.isServerXMLFile(domDocument))
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
        DOMNode featureManager = null;
        // find <featureManager> element if it exists
        for (DOMNode node : nodes) {
            if (LibertyConstants.FEATURE_MANAGER_ELEMENT.equals(node.getNodeName())) {
                featureManager = node;
                break;
            }
        }
        // No need for validation if there is no <featureManager>
        if (featureManager == null) {
            return;
        }

        String libertyVersion = SettingsService.getInstance().getLibertyVersion();
        if (libertyVersion == null) {
            // try to get version from installed Liberty
            libertyVersion = LibertyUtils.getVersion(domDocument);
        }
        final int requestDelay = SettingsService.getInstance().getRequestDelay();

        // Search for duplicate features
        // or features that do not exist
        Set<String> includedFeatures = new HashSet<>();
        List<DOMNode> features = featureManager.getChildren();
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
                    list.add(new Diagnostic(range, message));
                } else {
                    if (includedFeatures.contains(featureName)) {
                        Range range = XMLPositionUtility.createRange(featureTextNode.getStart(),
                                featureTextNode.getEnd(), domDocument);
                        String message = "ERROR: " + featureName + " is already included.";
                        list.add(new Diagnostic(range, message));
                    } else {
                        includedFeatures.add(featureName);
                    }
                }
            }
        }
    }
}
