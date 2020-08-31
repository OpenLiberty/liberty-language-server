package io.openliberty.lemminx.liberty;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.services.extensions.diagnostics.IDiagnosticsParticipant;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import io.openliberty.lemminx.liberty.services.FeatureService;
import io.openliberty.lemminx.liberty.util.*;
import java.io.IOException;
import java.util.*;

public class LibertyDiagnosticParticipant implements IDiagnosticsParticipant {

    @Override
    public void doDiagnostics(DOMDocument domDocument, List<Diagnostic> list, CancelChecker cancelChecker) {
        if (!LibertyUtils.isServerXMLFile(domDocument))
            return;

        try {
            validateFeatures(domDocument, list);
        } catch (IOException e) {
            System.err.println("Error validating features");
            System.err.println(e.getMessage());
        }
    }

    private void validateFeatures(DOMDocument domDocument, List<Diagnostic> list) throws IOException {
        NodeList nodes = domDocument.getDocumentElement().getChildNodes();
        Node featureManager = null;
        // find <featureManager> element if it exists
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (LibertyConstants.FEATURE_MANAGER_ELEMENT.equals(node.getNodeName())) {
                featureManager = node;
                break;
            }
        }
        // No need for validation if there is no <featureManager>
        if (featureManager == null) {
            return;
        }

        // Search for duplicate features
        // or features that do not exist
        Set<String> includedFeatures = new HashSet<>();
        NodeList features = featureManager.getChildNodes();
        for (int i = 0; i < features.getLength(); i++) {
            DOMNode featureNode = (DOMNode) features.item(i);
            DOMNode featureTextNode = (DOMNode) featureNode.getChildNodes().item(0);
            String featureName = featureTextNode.getTextContent();
            if (!FeatureService.getInstance().featureExists(featureName, "20.0.0.8")) {
                Range range = XMLPositionUtility.createRange(featureTextNode.getStart(), featureTextNode.getEnd(),
                        domDocument);
                String message = "ERROR: The " + featureName + " feature does not exist.";
                list.add(new Diagnostic(range, message));
            } else {
                if (includedFeatures.contains(featureName)) {
                    Range range = XMLPositionUtility.createRange(featureTextNode.getStart(), featureTextNode.getEnd(),
                            domDocument);
                    String message = "ERROR: " + featureName + " is already included.";
                    list.add(new Diagnostic(range, message));
                } else {
                    includedFeatures.add(featureName);
                }
            }
        }

    }
}
