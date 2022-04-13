/*******************************************************************************
* Copyright (c) 2020, 2022 IBM Corporation and others.
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
package io.openliberty.tools.langserver.ls;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;

// TODO: Add support for incimental updates
public class LibertyTextDocuments<T extends LibertyTextDocument> {

    private final Map<String, T> documents;

    public LibertyTextDocuments() {
        documents = new HashMap<>();
    }

    /**
     * Returns the document for the given URI. Returns undefined if the document is
     * not managed by this instance.
     *
     * @param uri The text document's URI to retrieve.
     * @return the text document or null.
     */
    public T get(String uri) {
        synchronized (documents) {
            return documents.get(uri);
        }
    }

    public T createDocument(TextDocumentItem document) {
        LibertyTextDocument doc = new LibertyTextDocument(document);
        return (T) doc;
    }

    public T onDidChangeTextDocument(DidChangeTextDocumentParams params) {
        synchronized (documents) {
            T document = getDocument(params.getTextDocument());
            if (document != null) {
                document.setVersion(params.getTextDocument().getVersion());
                document.update(params.getContentChanges());
                return document;
            }
        }
        return null;
    }

    public T onDidOpenTextDocument(DidOpenTextDocumentParams params) {
        TextDocumentItem item = params.getTextDocument();
        synchronized (documents) {
            T document = createDocument(item);
            documents.put(document.getUri(), document);
            return document;
        }
    }

    public T onDidCloseTextDocument(DidCloseTextDocumentParams params) {
        synchronized (documents) {
            T document = getDocument(params.getTextDocument());
            if (document != null) {
                documents.remove(params.getTextDocument().getUri());
            }
            return document;
        }
    }

    private T getDocument(TextDocumentIdentifier identifier) {
        return documents.get(identifier.getUri());
    }

    /**
     * Returns the all opened documents.
     *
     * @return the all opened documents.
     */
    public Collection<T> all() {
        synchronized (documents) {
            return documents.values();
        }
    }

}
