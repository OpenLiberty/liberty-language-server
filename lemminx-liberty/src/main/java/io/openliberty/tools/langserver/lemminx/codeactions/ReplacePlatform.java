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
import io.openliberty.tools.langserver.lemminx.util.LibertyConstants;
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
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

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
                List<String> existingPlatforms = FeatureService.getInstance().collectExistingPlatforms(document, platformNameToReplace);
                List<String> replacementPlatforms = getReplacementPlatforms(
                        allPlatforms, existingPlatforms);
                // check for conflicting platforms for already existing platforms. remove them from quick fix items
                List<String> replacementPlatformsWithoutConflicts = getReplacementPlatformsWithoutConflicts(replacementPlatforms, existingPlatforms);
                // filter with entered word
                List<String> filteredPlatforms = replacementPlatformsWithoutConflicts.stream().
                        filter(it -> it.toLowerCase().contains(platformNameToReplace.toLowerCase()))
                        .toList();
                for (String nextPlatform : filteredPlatforms) {
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

    /**
     * get list of existing platforms to exclude from list of possible replacements
     * also exclude any platform with a different version that matches an existing platform
     * @param allPlatforms all platforms
     * @return replacement platforms
     */
    private static List<String> getReplacementPlatforms(Set<String> allPlatforms, List<String> existingPlatforms) {
        List<String> existingPlatformsWithoutVersionLowerCase = existingPlatforms.stream().map(p->LibertyUtils.stripVersion(p).toLowerCase()).toList();
        return allPlatforms.stream().filter(
                p -> !existingPlatformsWithoutVersionLowerCase.contains(LibertyUtils.stripVersion(p.toLowerCase()))
        ).sorted(Comparator.naturalOrder()).toList();
    }

    /**
     * find and remove conflicting platforms
     * @param replacementPlatforms replacement platform  list
     * @param existingPlatforms existing platforms in doc
     * @return non-conflicting platforms
     */
    private static List<String> getReplacementPlatformsWithoutConflicts(List<String> replacementPlatforms, List<String> existingPlatforms) {
        List<String> replacementPlatformsWithoutConflicts=new ArrayList<>();
        replacementPlatforms.forEach(
                p->{
                    String pWithoutVersion = LibertyUtils.stripVersion(p.toLowerCase());

                    Optional<String> conflictingPlatform = existingPlatforms.stream().filter(
                            existingPlatform -> {
                                String conflictingPlatformName = LibertyConstants.conflictingPlatforms.get(pWithoutVersion);
                                return conflictingPlatformName != null && existingPlatform.toLowerCase().contains(conflictingPlatformName);
                            }
                    ).findFirst();
                    if(conflictingPlatform.isEmpty()){
                        replacementPlatformsWithoutConflicts.add(p);
                    }
                }
        );
        return replacementPlatformsWithoutConflicts;
    }

    private static Set<String> getAllPlatforms(LibertyRuntime runtimeInfo, DOMDocument document) {
        String libertyVersion =  runtimeInfo == null ? null : runtimeInfo.getRuntimeVersion();
        String libertyRuntime =  runtimeInfo == null ? null : runtimeInfo.getRuntimeType();

        final int requestDelay = SettingsService.getInstance().getRequestDelay();
        return FeatureService.getInstance().getAllPlatforms(libertyVersion, libertyRuntime, requestDelay,
                document.getDocumentURI());
    }
}
