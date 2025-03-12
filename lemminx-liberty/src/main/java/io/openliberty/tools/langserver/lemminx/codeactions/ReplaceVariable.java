/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
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

import com.google.gson.JsonPrimitive;
import io.openliberty.tools.langserver.lemminx.services.SettingsService;
import io.openliberty.tools.langserver.lemminx.util.LibertyUtils;
import io.openliberty.tools.langserver.lemminx.util.ResourceBundleMappingConstants;
import io.openliberty.tools.langserver.lemminx.util.ResourceBundleUtil;
import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ReplaceVariable implements ICodeActionParticipant {
    private static final Logger LOGGER = Logger.getLogger(ReplaceVariable.class.getName());

    @Override
    public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
        Diagnostic diagnostic = request.getDiagnostic();
        DOMDocument document = request.getDocument();
        try {
            // Get a list of variables that partially match the specified invalid variables.
            // Create a code action to replace the invalid variable with each possible valid variable.
            // First, get the invalid variable.
            String invalidVariable = null;
            if (diagnostic.getData() instanceof JsonPrimitive) {
                invalidVariable = ((JsonPrimitive) diagnostic.getData()).getAsString();
            }
            if (diagnostic.getData() instanceof String) {
                invalidVariable = (String) diagnostic.getData();
            }
            final boolean replaceVariable = invalidVariable != null && !invalidVariable.isBlank();

            if (replaceVariable) {
                Properties existingVariables = SettingsService.getInstance().getVariablesForServerXml(document.getDocumentURI());
                LibertyUtils.checkAndAddNewVariables(document, existingVariables);
                // filter with entered word -> may not be required
                String finalInvalidVariable = invalidVariable;
                Set<Map.Entry<Object, Object>> filteredVariables = existingVariables
                        .entrySet().stream()
                        .filter(entry -> LibertyUtils.containsEachOther(entry.toString(), finalInvalidVariable, false))
                        .collect(Collectors.toSet());
                for (Map.Entry<Object, Object> nextVariable : filteredVariables) {
                    String title = ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.TITLE_REPLACE_VARIABLE_VALUE, nextVariable.getKey());
                    String variableInDoc = String.format("${%s}", nextVariable.getKey().toString());
                    codeActions.add(CodeActionFactory.replace(title, diagnostic.getRange(), variableInDoc, document.getTextDocument(), diagnostic));
                }
                // code action for add variable
                Position insertPos = new Position();
                // variable will be added in the current line where diagnostic is showing
                // line separator in end of text insert will move current diagnostic line to 1 line
                insertPos.setLine(diagnostic.getRange().getEnd().getLine());
                codeActions.add(CodeActionFactory.insert(ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.TITLE_ADD_VARIABLE, invalidVariable), insertPos,
                        String.format("    <variable name=\"%s\" value=\"\"/> %s", invalidVariable, System.lineSeparator()),
                        document.getTextDocument(), diagnostic));
            }
        } catch (Exception e) {
            // BadLocationException not expected
            LOGGER.warning("Could not generate code action for replace attribute value: " + e);
        }
    }
}
