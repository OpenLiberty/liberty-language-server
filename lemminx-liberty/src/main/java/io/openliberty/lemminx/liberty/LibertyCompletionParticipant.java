package io.openliberty.lemminx.liberty;

import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.services.extensions.CompletionParticipantAdapter;
import org.eclipse.lemminx.services.extensions.ICompletionRequest;
import org.eclipse.lemminx.services.extensions.ICompletionResponse;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import io.openliberty.lemminx.liberty.models.feature.*;
import io.openliberty.lemminx.liberty.services.FeatureService;
import io.openliberty.lemminx.liberty.services.SettingsService;
import io.openliberty.lemminx.liberty.util.*;

import java.io.IOException;
import java.util.List;

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
            listFeatures(response);
        }
    }

    private void listFeatures(ICompletionResponse response) {
        final String libertyVersion = SettingsService.getInstance().getLibertyVersion();
        List<Feature> features = FeatureService.getInstance().getFeatures(libertyVersion);
        for (Feature feature : features) {
            CompletionItem item = new CompletionItem();
            item.setLabel(feature.getWlpInformation().getShortName());
            item.setDocumentation(Either.forLeft(feature.getShortDescription()));
            response.addCompletionItem(item);
        }

    }
}
