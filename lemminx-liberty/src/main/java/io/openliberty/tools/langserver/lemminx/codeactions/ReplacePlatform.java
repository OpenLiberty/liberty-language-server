/*******************************************************************************
* Copyright (c) 2024 IBM Corporation and others.
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

package io.openliberty.tools.langserver.lemminx.codeactions;

import io.openliberty.tools.langserver.lemminx.LibertyExtension;
import io.openliberty.tools.langserver.lemminx.data.LibertyRuntime;
import io.openliberty.tools.langserver.lemminx.services.FeatureService;
import io.openliberty.tools.langserver.lemminx.services.SettingsService;
import io.openliberty.tools.langserver.lemminx.util.LibertyUtils;
import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ReplacePlatform implements ICodeActionParticipant {
    private static final Logger LOGGER = Logger.getLogger(LibertyExtension.class.getName());

    @Override
    public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
        Diagnostic diagnostic = request.getDiagnostic();
        DOMDocument document = request.getDocument();
        try {
            // Get a list of platforms that partially match the specified invalid platform.
            // Create a code action to replace the invalid platform with each possible valid platform.
            // First, get the invalid platform.
            String invalidPlatform = document.findNodeAt(document.offsetAt(diagnostic.getRange().getEnd())).getTextContent();

            final boolean replacePlatformName = invalidPlatform != null && !invalidPlatform.isBlank();
            // strip off version number after the - so that we can provide all possible valid versions of a platform for completion
            final String platformNameToReplace = replacePlatformName && invalidPlatform.contains("-") ? invalidPlatform.substring(0, invalidPlatform.lastIndexOf("-")) : invalidPlatform;

            if (replacePlatformName) {
                LibertyRuntime runtimeInfo = LibertyUtils.getLibertyRuntimeInfo(document);
                Set<String> allPlatforms = getAllPlatforms(runtimeInfo, document);

                List<String> filteredPlatforms = allPlatforms.stream().
                        filter(it -> it.contains(platformNameToReplace))
                        .collect(Collectors.toList());
                // if no matching platform is found, show all platforms as quick fix actions
                List<String> replacementPlatforms = filteredPlatforms.isEmpty() ? new ArrayList<>(allPlatforms) : filteredPlatforms;
                replacementPlatforms.sort(Comparator.naturalOrder());
                for (String nextPlatform : replacementPlatforms) {
                    if (!nextPlatform.equals(platformNameToReplace)) {
                        String title = "Replace platform with " + nextPlatform;
                        codeActions.add(CodeActionFactory.replace(title, diagnostic.getRange(), nextPlatform, document.getTextDocument(), diagnostic));
                    }
                }
            }
        } catch (Exception e) {
            // BadLocationException not expected
            LOGGER.warning("Could not generate code action for replace platform: " + e);
        }
    }

    private static Set<String> getAllPlatforms(LibertyRuntime runtimeInfo, DOMDocument document) {
        String libertyVersion =  runtimeInfo == null ? null : runtimeInfo.getRuntimeVersion();
        String libertyRuntime =  runtimeInfo == null ? null : runtimeInfo.getRuntimeType();

        final int requestDelay = SettingsService.getInstance().getRequestDelay();
        return FeatureService.getInstance().getAllPlatforms(libertyVersion, libertyRuntime, requestDelay,
                document.getDocumentURI());
    }
}
