package io.openliberty.lemminx.liberty;

import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.services.extensions.IHoverParticipant;
import org.eclipse.lemminx.services.extensions.IHoverRequest;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import io.openliberty.lemminx.liberty.models.feature.*;
import io.openliberty.lemminx.liberty.services.FeatureService;
import io.openliberty.lemminx.liberty.util.*;

import java.io.IOException;
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
		if (parentElement.getTagName().equals(LibertyConstants.FEATURE_ELEMENT)) {
			try {
				String featureName = request.getNode().getTextContent();
				return getHoverFeatureDescription(featureName, "20.0.0.8");
			} catch (IOException e) {
				System.err.println("Error getting features");
				System.err.println(e.getMessage());
			}
		}

		return null;
	}

	private Hover getHoverFeatureDescription(String featureName, String libertyVersion) throws IOException {
		Optional<Feature> feature = FeatureService.getInstance().getFeature(featureName, libertyVersion);
		if (feature.isPresent()) {
			return new Hover(new MarkupContent("plaintext", feature.get().getShortDescription()));
		}

		return null;
	}
}