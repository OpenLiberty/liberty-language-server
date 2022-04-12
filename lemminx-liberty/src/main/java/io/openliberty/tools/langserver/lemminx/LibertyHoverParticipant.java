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
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.services.extensions.IHoverParticipant;
import org.eclipse.lemminx.services.extensions.IHoverRequest;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import io.openliberty.tools.langserver.lemminx.models.feature.*;
import io.openliberty.tools.langserver.lemminx.services.FeatureService;
import io.openliberty.tools.langserver.lemminx.services.SettingsService;
import io.openliberty.tools.langserver.lemminx.util.*;

import java.util.Optional;

public class LibertyHoverParticipant implements IHoverParticipant {

	@Override
	public Hover onAttributeName(IHoverRequest request) {
		return null;
	}

	@Override
	public Hover onAttributeValue(IHoverRequest request) {
		return null;
	}

	@Override
	public Hover onTag(IHoverRequest request) {
		return null;
	}

	@Override
	public Hover onText(IHoverRequest request) {
		if (!LibertyUtils.isServerXMLFile(request.getXMLDocument()))
			return null;

		DOMElement parentElement = request.getParentElement();
		if (parentElement == null || parentElement.getTagName() == null)
			return null;

		// if we are hovering over text inside a <feature> element
		if (LibertyConstants.FEATURE_ELEMENT.equals(parentElement.getTagName())) {
			String featureName = request.getNode().getTextContent();
			return getHoverFeatureDescription(featureName, request.getXMLDocument());
		}

		return null;
	}

	private Hover getHoverFeatureDescription(String featureName, DOMDocument domDocument) {
		String libertyVersion = LibertyUtils.getVersion(domDocument);

		final int requestDelay = SettingsService.getInstance().getRequestDelay();
		Optional<Feature> feature = FeatureService.getInstance().getFeature(featureName, libertyVersion, requestDelay, domDocument.getDocumentURI());
		if (feature.isPresent()) {
			return new Hover(new MarkupContent("plaintext", feature.get().getShortDescription()));
		}

		return null;
	}
}