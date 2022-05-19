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

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import io.openliberty.tools.langserver.ls.LibertyTextDocument;
import io.openliberty.tools.langserver.utils.Messages;

public class PropertiesKeyInstance {

    private String propertyKey;
    private PropertiesEntryInstance propertyEntryInstance;
    private LibertyTextDocument textDocumentItem;

    public PropertiesKeyInstance(String propertyKeyInstanceString, PropertiesEntryInstance propertyEntryInstance, LibertyTextDocument textDocumentItem) {
        this.propertyKey = propertyKeyInstanceString;
        this.propertyEntryInstance = propertyEntryInstance;
        this.textDocumentItem = textDocumentItem;
    }

    /**
     * Returns the index of the end of the property key
     * @return index of the end of the property key
     */
    public int getEndPosition() {
        return propertyKey.length()-1;
    }

    public CompletableFuture<Hover> getHover(Position position) {
        String message = null;
        message = Messages.getPropDescription(propertyKey);

        // set hover range to highlight the entire key
        int line = position.getLine();
        Position rangeStart = new Position(line, 0);
        Position rangeEnd = new Position(line, propertyKey.length());
        Range range = new Range(rangeStart, rangeEnd);

        Hover hover = new Hover(new MarkupContent("markdown", message), range);
        return CompletableFuture.completedFuture(hover);
    }

    @Override
    public String toString() {
        return this.propertyKey;
    }
}
