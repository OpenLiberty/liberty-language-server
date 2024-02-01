/*******************************************************************************
* Copyright (c) 2022 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*******************************************************************************/
package io.openliberty.tools.langserver.completion;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.Test;

import io.openliberty.tools.langserver.LibertyLanguageServer;

public class ServerXmlCompletionTest extends AbstractCompletionTest {
    // @Test
    // public void testValueCompletionForBoolean() throws Exception {
    //     CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletion("WLP_LOGGING_APPS_WRITE_JSON=", new Position(0, 38));
    //     List<CompletionItem> completionItems = completions.get().getLeft();
    //     assertEquals(2, completionItems.size());

    //     checkCompletionsContainAllStrings(completionItems, ServerPropertyValues.BOOLEAN_VALUES_DEFAULT_TRUE);
    // }

    @Test
    public void test1() throws Exception {
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletion("${", new Position(1, 4));
        List<CompletionItem> completionItems = completions.get().getLeft();
        assertEquals(0, completionItems.size());
    }

    protected CompletableFuture<Either<List<CompletionItem>, CompletionList>> getCompletion(String enteredText, Position position) throws URISyntaxException, InterruptedException, ExecutionException, IOException {
        String filename = "server.xml";
        String resourcesDir = "src/test/resources/xml/";
        File file = new File(resourcesDir, filename);
        String fileURI = file.toURI().toString();
        
        LibertyLanguageServer lls = initializeLanguageServer(filename, new TextDocumentItem(fileURI, LibertyLanguageServer.LANGUAGE_ID, 0, enteredText));
        return getCompletionFor(lls, position, fileURI);
    }
}
