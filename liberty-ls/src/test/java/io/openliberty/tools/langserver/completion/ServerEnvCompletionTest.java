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
import io.openliberty.tools.langserver.utils.ServerPropertyValues;

public class ServerEnvCompletionTest extends AbstractCompletionTest {
    
    @Test
    public void testKeyCompletionLogging() throws Exception {
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletion("logging", new Position(0, 5));
        List<CompletionItem> completionItems = completions.get().getLeft();
        assertEquals(8, completionItems.size());

        checkCompletionsContainAllStrings(completionItems, 
            "WLP_LOGGING_JSON_FIELD_MAPPINGS",
            "WLP_LOGGING_CONSOLE_FORMAT",
            "WLP_LOGGING_CONSOLE_LOGLEVEL",
            "WLP_LOGGING_CONSOLE_SOURCE",
            "WLP_LOGGING_MESSAGE_FORMAT",
            "WLP_LOGGING_MESSAGE_SOURCE",
            "WLP_LOGGING_APPS_WRITE_JSON",
            "WLP_LOGGING_JSON_ACCESS_LOG_FIELDS"
        );

        checkCompletionContainsDetail(completionItems, "WLP_LOGGING_APPS_WRITE_JSON", "When the message log or console is in JSON format, this setting allows applications to write JSON-formatted messages to those destinations, without modification.");
    }

    @Test
    public void testKeyCompletionDebug() throws Exception {
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletion("deb", new Position(0, 5));
        List<CompletionItem> completionItems = completions.get().getLeft();
        assertEquals(3, completionItems.size());

        checkCompletionsContainAllStrings(completionItems, 
            "WLP_DEBUG_ADDRESS",
            "WLP_DEBUG_SUSPEND",
            "WLP_DEBUG_REMOTE"
        );
    }

    @Test
    public void testKeyCompletionWLP() throws Exception {
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletion("wlp", new Position(0, 5));
        List<CompletionItem> completionItems = completions.get().getLeft();
        assertEquals(13, completionItems.size());
    }

    @Test
    public void testValueCompletionForLoggingSource() throws Exception {
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletion("WLP_LOGGING_CONSOLE_SOURCE=", new Position(0, 38));
        List<CompletionItem> completionItems = completions.get().getLeft();
        assertEquals(5, completionItems.size());

        checkCompletionsContainAllStrings(completionItems, ServerPropertyValues.LOGGING_SOURCE_VALUES);
    }

    @Test
    public void testValueCompletionForConsoleLoggingFormat() throws Exception {
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletion("WLP_LOGGING_CONSOLE_FORMAT=", new Position(0, 38));
        List<CompletionItem> completionItems = completions.get().getLeft();
        assertEquals(4, completionItems.size());

        checkCompletionsContainAllStrings(completionItems, "DEV", "SIMPLE", "JSON", "TBASIC");

        checkCompletionContainsDetail(completionItems, null, "This setting specifies the required format for the console. Valid values are `dev`, `simple`, or `json` format. By default, consoleFormat is set to `dev`.");
    }

    @Test
    public void testValueCompletionForMessageLoggingFormat() throws Exception {
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletion("WLP_LOGGING_MESSAGE_FORMAT=", new Position(0, 38));
        List<CompletionItem> completionItems = completions.get().getLeft();
        assertEquals(3, completionItems.size());

        checkCompletionsContainAllStrings(completionItems, "SIMPLE", "JSON", "TBASIC");
    }
    
    @Test
    public void testValueCompletionForYesNo() throws Exception {
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletion("WLP_DEBUG_SUSPEND=", new Position(0, 20));
        List<CompletionItem> completionItems = completions.get().getLeft();
        assertEquals(2, completionItems.size());

        checkCompletionsContainAllStrings(completionItems, ServerPropertyValues.YES_NO_VALUES);

    }
    
    @Test
    public void testValueCompletionForBoolean() throws Exception {
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletion("WLP_LOGGING_APPS_WRITE_JSON=", new Position(0, 38));
        List<CompletionItem> completionItems = completions.get().getLeft();
        assertEquals(2, completionItems.size());

        checkCompletionsContainAllStrings(completionItems, ServerPropertyValues.BOOLEAN_VALUES_DEFAULT_TRUE);
    }

    @Test
    public void testValueCompletionForConsoleLoggingFormatForUserEnteredKey() throws Exception {
        // also testing user entered value with trailing spaces. spaces are trimmed in code
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletion("WLP_LOGGING_CONSOLE_FORMAT=    S", new Position(0, 28));
        List<CompletionItem> completionItems = completions.get().getLeft();
        assertEquals(3, completionItems.size());

        checkCompletionsContainAllStrings(completionItems,  "SIMPLE", "JSON", "TBASIC");

        checkCompletionContainsDetail(completionItems, null, "This setting specifies the required format for the console. Valid values are `dev`, `simple`, or `json` format. By default, consoleFormat is set to `dev`.");
    }

    @Test
    public void testValueCompletionForConsoleLoggingFormatForUserEnteredInvalidKey() throws Exception {
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletion("WLP_LOGGING_CONSOLE_FORMAT=Asd", new Position(0, 30));
        List<CompletionItem> completionItems = completions.get().getLeft();
        assertEquals(0, completionItems.size());
    }

    @Test
    public void testValueCompletionForYesNoWhenUserEnteredY() throws Exception {
        //providing input in UPPERCASE to verify code is working case insensitive
        //also checking ending spaces
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletion("WLP_DEBUG_SUSPEND=Y   ", new Position(0, 21));
        List<CompletionItem> completionItems = completions.get().getLeft();
        assertEquals(1, completionItems.size());

        checkCompletionsContainAllStrings(completionItems, "y");

    }


    @Test
    public void testValueCompletionMultiPropertyValue() throws Exception {
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletion("WLP_LOGGING_MESSAGE_SOURCE=trace", new Position(0, 33));
        List<CompletionItem> completionItems = completions.get().getLeft();
        assertEquals(1, completionItems.size());
        checkCompletionsContainAllStrings(completionItems, "trace");

        completions = getCompletion("WLP_LOGGING_MESSAGE_SOURCE=trace,", new Position(0, 34));
        completionItems = completions.get().getLeft();
        assertEquals(4, completionItems.size());
        checkCompletionsContainAllStrings(completionItems, "trace,accessLog","trace,message","trace,ffdc","trace,audit");

        completions = getCompletion("WLP_LOGGING_MESSAGE_SOURCE=trace,a", new Position(0, 35));
        completionItems = completions.get().getLeft();
        assertEquals(3, completionItems.size());
        checkCompletionsContainAllStrings(completionItems, "trace,accessLog","trace,audit","trace,message");

        completions = getCompletion("WLP_LOGGING_MESSAGE_SOURCE=trace,message,", new Position(0, 42));
        completionItems = completions.get().getLeft();
        assertEquals(3, completionItems.size());
        checkCompletionsContainAllStrings(completionItems, "trace,message,accessLog","trace,message,ffdc","trace,message,audit");

        completions = getCompletion("WLP_LOGGING_MESSAGE_SOURCE=trace,message", new Position(0, 41));
        completionItems = completions.get().getLeft();
        assertEquals(0, completionItems.size());
    }

    protected CompletableFuture<Either<List<CompletionItem>, CompletionList>> getCompletion(String enteredText, Position position) throws URISyntaxException, InterruptedException, ExecutionException, IOException {
        String filename = "server.env";
        File file = new File(resourcesDir, filename);
        String fileURI = file.toURI().toString();
        
        LibertyLanguageServer lls = initializeLanguageServer(filename, new TextDocumentItem(fileURI, LibertyLanguageServer.LANGUAGE_ID, 0, enteredText));
        return getCompletionFor(lls, position, fileURI);
    }
}
