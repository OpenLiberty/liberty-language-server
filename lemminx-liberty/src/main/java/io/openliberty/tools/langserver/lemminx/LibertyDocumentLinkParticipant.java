/*******************************************************************************
* Copyright (c) 2022 IBM Corporation and others.
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

import io.openliberty.tools.langserver.lemminx.util.LibertyConstants;
import io.openliberty.tools.langserver.lemminx.util.LibertyUtils;

public class LibertyDocumentLinkParticipant implements IDocumentLinkParticipant {

    private static final Logger LOGGER = Logger.getLogger(LibertyDocumentLinkParticipant.class.getName());

    @Override
    public void findDocumentLinks(DOMDocument document, List<DocumentLink> links) {
        if (!LibertyUtils.isConfigXMLFile(document)) {
            return;
        }

        List<DOMNode> nodes = document.getDocumentElement().getChildren();

        // collect all <include> nodes that are children of the document element
        List<DOMNode> includeDomNodes = nodes.stream().filter(n -> ((n.getNodeName() != null) && n.getNodeName().equals(LibertyConstants.INCLUDE_ELEMENT)))
                .collect(Collectors.toList());

        for (DOMNode includeNode : includeDomNodes) {
            DOMAttr includeAttr = includeNode.getAttributeNode("location");
            
            String locAttr = includeAttr.getValue();
            if (!locAttr.endsWith(".xml") || locAttr.startsWith("http") || locAttr.contains("$")) {
                // ignoring http or $ vars until feature supported
                continue;
            }
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