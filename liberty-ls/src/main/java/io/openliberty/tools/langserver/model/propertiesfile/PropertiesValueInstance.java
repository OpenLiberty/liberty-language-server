/*******************************************************************************
* Copyright (c) 2022 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*******************************************************************************/
package io.openliberty.tools.langserver.model.propertiesfile;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.Position;

import io.openliberty.tools.langserver.ls.LibertyTextDocument;

public class PropertiesValueInstance {

    private String propertyValue;
    private PropertiesKeyInstance key;
    private LibertyTextDocument textDocumentItem;

    public PropertiesValueInstance(String propertyValueInstanceString, PropertiesKeyInstance propertyKeyInstance, LibertyTextDocument textDocumentItem) {
        this.propertyValue = propertyValueInstanceString;
        this.key = propertyKeyInstance;
        this.textDocumentItem = textDocumentItem;
    }

    public PropertiesKeyInstance getKey() {
        return key;
    }

    public CompletableFuture<Hover> getHover() {
        Hover hover = new Hover();
        hover.setContents(new MarkupContent("plaintext", "This value is set for: " + this.key));
        return CompletableFuture.completedFuture(hover);
    }

    public CompletableFuture<List<CompletionItem>> getCompletions(Position position) {
        // TODO: read the property key, lookup what possible values can be used for it
        return CompletableFuture.completedFuture(Collections.emptyList());
    }
}
