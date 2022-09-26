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
import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.Test;

import io.openliberty.tools.langserver.AbstractLibertyLanguageServerTest;
import io.openliberty.tools.langserver.LibertyLanguageServer;
import io.openliberty.tools.langserver.utils.ServerPropertyValues;

public class BootstrapPropertyCompletionTest extends AbstractLibertyLanguageServerTest {
    @Test
    public void testKeyCompletion() throws Exception {
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletion("webs", new Position(0, 4));

        assertEquals("websphere.log.provider", completions.get().getLeft().get(0).getLabel());
    }

    @Test
    public void testKeyCompletion2() throws Exception {
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletion("purge", new Position(0, 5));
        List<CompletionItem> completionItems = completions.get().getLeft();
        assertEquals(4, completionItems.size()); // expected 4 results for "purge" key

        // Check that completionItems contains all 4 results for "purge" autocomplete
        List<String> purgeKeys = new LinkedList<>(Arrays.asList("com.ibm.hpel.log.purgeMaxSize", "com.ibm.hpel.log.purgeMinTime", "com.ibm.hpel.trace.purgeMaxSize", "com.ibm.hpel.trace.purgeMinTime"));
        Iterator<CompletionItem> it = completionItems.iterator();
        while (it.hasNext()) {
            String itemLabel = it.next().getLabel();
            assertTrue(purgeKeys.remove(itemLabel));
        }
        assertTrue(purgeKeys.isEmpty()); // account for all keys containing "purge"
    }

    @Test
    public void testValueCompletion() throws Exception {
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletion("com.ibm.ws.logging.console.source=", new Position(0, 38));
        List<CompletionItem> completionItems = completions.get().getLeft();
        assertEquals(5, completionItems.size());

        List<String> keys = new LinkedList<>(ServerPropertyValues.LOGGING_SOURCE_VALUES);
        Iterator<CompletionItem> it = completionItems.iterator();
        while (it.hasNext()) {
            String itemLabel = it.next().getLabel();
            assertTrue(keys.remove(itemLabel));
        }
        assertTrue(keys.isEmpty());
    }

    protected CompletableFuture<Either<List<CompletionItem>, CompletionList>> getCompletion(String enteredText, Position position) throws URISyntaxException, InterruptedException, ExecutionException {
        String filename = "bootstrap.properties";
        LibertyLanguageServer lls = initializeLanguageServer(filename, new TextDocumentItem(filename, LibertyLanguageServer.LANGUAGE_ID, 0, enteredText));
        return getCompletionFor(lls, position, filename);
    }

    protected CompletionItem createExpectedCompletionItem(String value) {
        CompletionItem expectedCompletionItem = new CompletionItem(value);
        return expectedCompletionItem;
    }
}
