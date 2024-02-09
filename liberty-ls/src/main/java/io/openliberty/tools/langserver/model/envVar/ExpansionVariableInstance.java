/*******************************************************************************
* Copyright (c) 2024 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*******************************************************************************/
package io.openliberty.tools.langserver.model.envVar;

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.Position;

import io.openliberty.tools.langserver.ls.LibertyTextDocument;
import io.openliberty.tools.langserver.utils.ServerConfigUtil;

public class ExpansionVariableInstance {
    private final Logger LOGGER = Logger.getLogger(ExpansionVariableInstance.class.getName());
    private Properties documentProperties;

    private Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
    private String entryLine;

    public ExpansionVariableInstance(String entryLine, LibertyTextDocument textDocumentItem) {
        this.documentProperties = ServerConfigUtil.parseDocumentAndGetProperties(textDocumentItem.getUri());
        this.entryLine = entryLine;
    }

    /**
     * @param entryLine
     * @param charOffset
     * @return Returns the string of text wrapped in ${...} if the offset is enclosed. Returns a blank string otherwise.
     */
    public String captureVariableName(String entryLine, int charOffset) {
        Matcher matcher = pattern.matcher(entryLine);
        while (matcher.find()) {
            if (charOffset >= matcher.start() && charOffset <= matcher.end()) {
                return matcher.group(1);
            }
        }
        return "";
    }

    public CompletableFuture<Hover> getHover(Position position) {
        String variableName = captureVariableName(this.entryLine, position.getCharacter());
        if (variableName.isBlank() || !documentProperties.containsKey(variableName)) {
            return CompletableFuture.completedFuture(new Hover(new MarkupContent("plaintext", "")));
        }
        LOGGER.info("Hover has detected a variable: " + variableName);
        String hoverValue = documentProperties.getProperty(variableName);
        return CompletableFuture.completedFuture(new Hover(new MarkupContent("plaintext", hoverValue)));
    }

    public CompletableFuture<List<CompletionItem>> getCompletions(Position position) {
        int cursorIndex = position.getCharacter();
        int startIndex = this.entryLine.substring(0, cursorIndex).lastIndexOf("${");
        if (startIndex == -1 || cursorIndex < startIndex + 2 || documentProperties.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        // LOGGER.info("The list of pre-filtered completable items are: " + documentProperties.keySet().toString());
        String query = this.entryLine.substring(startIndex + 2, cursorIndex);
        Predicate<String> filter = s -> {
            for (int i = s.length() - query.length(); i >= 0; --i) {
                if (s.regionMatches(true, i, query, 0, query.length()))
                    return true;
            }
            return false;
        };

        List<CompletionItem> completionList = documentProperties.keySet().stream()
                .map(s -> s.toString()).filter(filter)
                .map(s -> new CompletionItem(s)).collect(Collectors.toList());
        for (CompletionItem item : completionList) {
            item.setInsertText(item.getLabel() + "}");
        }
        return CompletableFuture.completedFuture(completionList);
    }
}
