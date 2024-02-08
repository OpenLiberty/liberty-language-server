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
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.Position;

import io.openliberty.tools.langserver.ls.LibertyTextDocument;
import io.openliberty.tools.langserver.utils.ServerConfigUtil;

public class ExpansionVariableInstance {
    // regex for "${...}"
    private Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
    private final Logger LOGGER = Logger.getLogger(ExpansionVariableInstance.class.getName());
    private Properties documentProperties;
    private String entryLine;

    public ExpansionVariableInstance(String entryLine, LibertyTextDocument textDocumentItem) {
        ServerConfigUtil.requestParseXml(textDocumentItem.getUri());
        this.documentProperties = ServerConfigUtil.getProperties();
        this.entryLine = entryLine;
    }

    public String captureProperty(String input, int charOffset) {
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            if (charOffset >= matcher.start() && charOffset <= matcher.end()) {
                return matcher.group(1);
            }
        }
        return "";
    }

    public CompletableFuture<Hover> getHover(Position position) {
        String propertyString = captureProperty(this.entryLine, position.getCharacter());
        LOGGER.warning("The captured word is: " + propertyString);
        if (documentProperties.containsKey(propertyString)) {
            return CompletableFuture.completedFuture(new Hover(new MarkupContent("plaintext", documentProperties.getProperty(propertyString))));
        }
        return CompletableFuture.completedFuture(new Hover(new MarkupContent("plaintext", "")));
    }

    public CompletableFuture<List<CompletionItem>> getCompletions(Position position) {
        // return list of variables
        LOGGER.warning("Completions is revealing: " + documentProperties.keySet().toString());
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    /**
     * Loaned from Messages.java
     */
    public List<String> getMatchingKeys(String query, LibertyTextDocument textDocument) {
        // remove completion results that don't contain the query string (case-insensitive search)
        // Predicate<String> filter = s -> {
        //     for (int i = s.length() - query.length(); i >= 0; --i) {
        //         if (s.regionMatches(true, i, query, 0, query.length()))
        //             return false;
        //     }
        //     return true;
        // };
        return Collections.emptyList();
    }
}
