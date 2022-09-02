package io.openliberty.tools.langserver.lemminx;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.xerces.impl.XMLEntityManager;
import org.apache.xerces.util.URI.MalformedURIException;
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.services.extensions.IDocumentLinkParticipant;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.Range;

import io.openliberty.tools.langserver.lemminx.util.LibertyUtils;


public class LibertyDocumentLinkParticipant implements IDocumentLinkParticipant {

    private static final Logger LOGGER = Logger.getLogger(LibertyDocumentLinkParticipant.class.getName());

    @Override
    public void findDocumentLinks(DOMDocument document, List<DocumentLink> links) {

        if (!LibertyUtils.isConfigXMLFile(document))
            return;

        List<DOMNode> childNodes = document.getDocumentElement().getChildren();

        // collect all <include> nodes that are children of the document element
        List<DOMNode> includeDomNodes = childNodes.stream().filter(n -> n.getNodeName().equals("include"))
                .collect(Collectors.toList());

        for (DOMNode includeNode : includeDomNodes) {
            DOMAttr includeAttr = includeNode.getAttributeNode("location");
            Range linkRange = XMLPositionUtility.selectAttributeValue(includeAttr);
            try {
                String location = getResolvedLocation(document.getDocumentURI(), includeAttr.getValue());
                DocumentLink link = new DocumentLink(linkRange, location);
                links.add(link);
            } catch (MalformedURIException e) {
                LOGGER.log(Level.SEVERE, "Creation of document link failed", e);
            }
            
        }

    }

    private static String getResolvedLocation(String documentURI, String location) throws MalformedURIException {
		if (location == null) {
			return null;
        }
        return XMLEntityManager.expandSystemId(location, documentURI, false);
	}

}