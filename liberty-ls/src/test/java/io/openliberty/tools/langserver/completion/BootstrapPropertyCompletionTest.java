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
import io.openliberty.tools.langserver.utils.ServerPropertyValues;

public class BootstrapPropertyCompletionTest extends AbstractCompletionTest {
    @Test
    public void testKeyCompletion() throws Exception {
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletion("webs", new Position(0, 4));

        assertEquals("websphere.log.provider", completions.get().getLeft().get(0).getLabel());
    }

    @Test
    public void testKeyCompletionPurge() throws Exception {
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletion("purge", new Position(0, 5));
        List<CompletionItem> completionItems = completions.get().getLeft();
        assertEquals(4, completionItems.size());

        checkCompletionsContainAllStrings(completionItems, 
            "com.ibm.hpel.log.purgeMaxSize", "com.ibm.hpel.log.purgeMinTime", "com.ibm.hpel.trace.purgeMaxSize", "com.ibm.hpel.trace.purgeMinTime");

        checkCompletionContainsDetail(completionItems, "com.ibm.hpel.log.purgeMaxSize", "Specifies the maximum size for the binary log repository in megabytes. When the value for purgeMaxSize is specified with a value of more than `0`, cleanup based on repository size is enabled, otherwise it is disabled; a value of `0` means no limit. The default value is `50`.");
    }

    @Test
    public void testKeyCompletionOnEquivalentProperty() throws Exception {
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletion("com.ibm.ws.logging.message", new Position(0, 5));
        List<CompletionItem> completionItems = completions.get().getLeft();
        assertEquals(3, completionItems.size()); // expected 4 results for "purge" key
        
        checkCompletionsContainAllStrings(completionItems, "com.ibm.ws.logging.message.file.name", "com.ibm.ws.logging.message.format", "com.ibm.ws.logging.message.source");
    }

    @Test
    public void testKeyCompletionDir() throws Exception {
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletion("dir", new Position(0, 5));
        List<CompletionItem> completionItems = completions.get().getLeft();
        assertEquals(8, completionItems.size());
    }

    @Test
    public void testValueCompletionForTraceLoggingSource() throws Exception {
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletion("com.ibm.ws.logging.console.source=", new Position(0, 38));
        List<CompletionItem> completionItems = completions.get().getLeft();
        assertEquals(5, completionItems.size());

        checkCompletionsContainAllStrings(completionItems, ServerPropertyValues.LOGGING_SOURCE_VALUES);
    }

    @Test
    public void testValueCompletionForTraceLoggingFormat() throws Exception {
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletion("com.ibm.ws.logging.trace.format=", new Position(0, 38));
        List<CompletionItem> completionItems = completions.get().getLeft();
        assertEquals(4, completionItems.size());

        checkCompletionsContainAllStrings(completionItems, "ENHANCED", "BASIC", "TBASIC", "ADVANCED");
    }

    @Test
    public void testValueCompletionForBoolean() throws Exception {
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletion("com.ibm.ws.logging.copy.system.streams=", new Position(0, 38));
        List<CompletionItem> completionItems = completions.get().getLeft();
        assertEquals(2, completionItems.size());

        checkCompletionsContainAllStrings(completionItems, ServerPropertyValues.BOOLEAN_VALUES_DEFAULT_TRUE);

        checkCompletionContainsDetail(completionItems, null, "If this setting is set to true, messages that are written to the `System.out` and `System.err` streams are copied to process stdout and stderr streams and so appear in the console.log file. If this setting is set to `false`, those messages are written to configured logs such as the `messages.log` file or `trace.log` file, but they are not copied to stdout and stderr and do not appear in console.log. The default value is `true`.");
    }

    @Test
    public void testValueCompletionLogProvider() throws Exception {
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletion("websphere.log.provider=", new Position(0, 38));
        List<CompletionItem> completionItems = completions.get().getLeft();
        assertEquals(1, completionItems.size());

        checkCompletionsContainAllStrings(completionItems, "binaryLogging-1.0");
    }

    protected CompletableFuture<Either<List<CompletionItem>, CompletionList>> getCompletion(String enteredText, Position position) throws URISyntaxException, InterruptedException, ExecutionException {
        String filename = "bootstrap.properties";
        LibertyLanguageServer lls = initializeLanguageServer(filename, new TextDocumentItem(filename, LibertyLanguageServer.LANGUAGE_ID, 0, enteredText));
        return getCompletionFor(lls, position, filename);
    }
}
