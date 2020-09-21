package io.openliberty.lemminx.liberty;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.services.extensions.CompletionParticipantAdapter;
import org.eclipse.lemminx.services.extensions.ICompletionRequest;
import org.eclipse.lemminx.services.extensions.ICompletionResponse;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import io.openliberty.lemminx.liberty.models.feature.*;
import io.openliberty.lemminx.liberty.services.FeatureService;
import io.openliberty.lemminx.liberty.services.SettingsService;
import io.openliberty.lemminx.liberty.util.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.lemminx.commons.BadLocationException;

public class LibertyCompletionParticipant extends CompletionParticipantAdapter {

    @Override
    public void onXMLContent(ICompletionRequest request, ICompletionResponse response)
            throws IOException, BadLocationException {
        if (!LibertyUtils.isServerXMLFile(request.getXMLDocument()))
            return;

        DOMElement parentElement = request.getParentElement();
        if (parentElement == null || parentElement.getTagName() == null)
            return;

        // if the parent element of cursor is a <feature>
        // provide the liberty features as completion options
        if (parentElement.getTagName().equals(LibertyConstants.FEATURE_ELEMENT)) {
            List<CompletionItem> featureCompletionItems = buildCompletionItems(parentElement, request.getXMLDocument());
            featureCompletionItems.stream().forEach(item -> response.addCompletionItem(item));
        }
    }

    private CompletionItem buildFeatureCompletionItem(Feature feature, DOMElement featureElement,
            DOMDocument document) {
        String featureName = feature.getWlpInformation().getShortName();

        // Build a text edit to replace whatever is inside <feature></feature>
        // with the completion result
        Range range = XMLPositionUtility.createRange(featureElement.getStartTagCloseOffset() + 1,
                featureElement.getEndTagOpenOffset(), document);
        TextEdit edit = new TextEdit(range, featureName);

        // Build the completion item to return to the client
        CompletionItem item = new CompletionItem();
        item.setTextEdit(edit);
        item.setLabel(featureName);
        item.setDocumentation(Either.forLeft(feature.getShortDescription()));
        return item;
    }

    private List<CompletionItem> buildCompletionItems(DOMElement featureElement, DOMDocument document) {
        final String libertyVersion = SettingsService.getInstance().getLibertyVersion();
        final int requestDelay = SettingsService.getInstance().getRequestDelay();
        List<Feature> features = FeatureService.getInstance().getFeatures(libertyVersion, requestDelay);

        List<CompletionItem> items = features.stream()
                .map(feat -> buildFeatureCompletionItem(feat, featureElement, document)).collect(Collectors.toList());

        return items;
    }
}
