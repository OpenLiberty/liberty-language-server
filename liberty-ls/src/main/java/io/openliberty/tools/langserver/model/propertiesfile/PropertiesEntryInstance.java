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

import io.openliberty.tools.langserver.ls.LibertyTextDocument;

public class PropertiesEntryInstance {
    private PropertiesKeyInstance propertyKeyInstance;
    private PropertiesValueInstance propertyValueInstance;
    private String line;
    private LibertyTextDocument textDocumentItem;
    private boolean isComment;

    public PropertiesEntryInstance(String entryLine, LibertyTextDocument textDocumentItem) {
        this.line = entryLine;
        this.textDocumentItem = textDocumentItem;
        if (line.trim().startsWith("#")){
            isComment = true;
            return;
        }
        isComment = false;
        int equalsIndex = line.indexOf("=");
        String propertyKeyInstanceString;
        String propertyValueInstanceString;
        if (equalsIndex != -1) {
            propertyKeyInstanceString = line.substring(0, equalsIndex);
            propertyValueInstanceString = line.substring(equalsIndex+1);
        } else {
            propertyKeyInstanceString = line;
            propertyValueInstanceString = null;
        }
        this.propertyKeyInstance = new PropertiesKeyInstance(propertyKeyInstanceString, this, textDocumentItem);
        this.propertyValueInstance = new PropertiesValueInstance(propertyValueInstanceString, propertyKeyInstance, textDocumentItem);
    }

    private boolean isOnEntryKey(Position position) {
        return position.getCharacter() <= propertyKeyInstance.getEndPosition();
    }

    public CompletableFuture<Hover> getHover(Position position) {
        if (!isComment) {
            if (isOnEntryKey(position)) {
                return propertyKeyInstance.getHover();
            } else {
                return propertyValueInstance.getHover();
            }
        }
        return CompletableFuture.completedFuture(new Hover(new MarkupContent("plaintext", "this is a comment")));
    }
}
