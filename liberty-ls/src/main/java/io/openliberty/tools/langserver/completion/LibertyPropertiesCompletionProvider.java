
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
package io.openliberty.tools.langserver.completion;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;

import io.openliberty.tools.langserver.LibertyConfigFileManager;
import io.openliberty.tools.langserver.ls.LibertyTextDocument;
import io.openliberty.tools.langserver.model.envVar.ExpansionVariableInstance;
import io.openliberty.tools.langserver.model.propertiesfile.PropertiesEntryInstance;
import io.openliberty.tools.langserver.utils.ParserFileHelperUtil;

public class LibertyPropertiesCompletionProvider {
    private LibertyTextDocument textDocumentItem;

    public LibertyPropertiesCompletionProvider(LibertyTextDocument textDocumentItem) {
        this.textDocumentItem = textDocumentItem;
    }

    public CompletableFuture<List<CompletionItem>> getCompletions(Position position) {
        String line = new ParserFileHelperUtil().getLine(textDocumentItem, position);
        if (LibertyConfigFileManager.isServerXml(textDocumentItem)) {
            return new ExpansionVariableInstance(line, textDocumentItem).getCompletions(position);
        }
        return new PropertiesEntryInstance(line, textDocumentItem).getCompletions(position);
    }
}
