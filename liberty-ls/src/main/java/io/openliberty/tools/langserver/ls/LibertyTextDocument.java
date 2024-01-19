/*******************************************************************************
* Copyright (c) 2020, 2024 IBM Corporation and others.
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

import java.util.List;

import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentItem;

/**
 * Text document extends LSP4j {@link TextDocumentItem} to provide methods to
 * retrieve position.
 *
 */
public class LibertyTextDocument extends TextDocumentItem {

    public LibertyTextDocument(TextDocumentItem document) {
        this(document.getText(), document.getUri());
        super.setVersion(document.getVersion());
        super.setLanguageId(document.getLanguageId());
    }

    public LibertyTextDocument(String text, String uri) {
        super.setUri(uri);
        super.setText(text);
    }

    /**
     * Update text of the document
     * TODO: Support incremental document updates
     *
     * @param changes the text document changes.
     */
    public void update(List<TextDocumentContentChangeEvent> changes) {
        if (changes.size() < 1) {
            // no changes, ignore it.
            return;
        }
        // like vscode does, get the last changes
        // see
        // https://github.com/Microsoft/vscode-languageserver-node/blob/master/server/src/main.ts
        TextDocumentContentChangeEvent last = changes.size() > 0 ? changes.get(changes.size() - 1) : null;
        if (last != null) {
            setText(last.getText());
        }
    }

}
