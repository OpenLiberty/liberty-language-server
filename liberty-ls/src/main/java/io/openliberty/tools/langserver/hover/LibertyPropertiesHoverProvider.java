/*******************************************************************************
* Copyright (c) 2022, 2024 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*******************************************************************************/
package io.openliberty.tools.langserver.hover;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.Position;

import io.openliberty.tools.langserver.LibertyConfigFileManager;
import io.openliberty.tools.langserver.ls.LibertyTextDocument;
import io.openliberty.tools.langserver.model.envVar.ExpansionVariableInstance;
import io.openliberty.tools.langserver.model.propertiesfile.PropertiesEntryInstance;
import io.openliberty.tools.langserver.utils.ParserFileHelperUtil;

public class LibertyPropertiesHoverProvider {
    private static final Logger LOGGER = Logger.getLogger(LibertyPropertiesHoverProvider.class.getName());
    private LibertyTextDocument textDocumentItem;

    public LibertyPropertiesHoverProvider(LibertyTextDocument textDocumentItem) {
        this.textDocumentItem = textDocumentItem;
    }

    public CompletableFuture<Hover> getHover(Position position) {
        String entryLine = new ParserFileHelperUtil().getLine(textDocumentItem, position);
        if (!LibertyConfigFileManager.isConfigFile(textDocumentItem)) {
            // return empty Hover if not a server config file
            return CompletableFuture.completedFuture(new Hover(new MarkupContent("plaintext", "")));
        }
        if (LibertyConfigFileManager.isServerXml(textDocumentItem)) {
            return new ExpansionVariableInstance(entryLine, textDocumentItem).getHover(position);
        }

        return new PropertiesEntryInstance(entryLine, textDocumentItem).getHover(position);
    }
}
