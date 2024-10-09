/*******************************************************************************
* Copyright (c) 2022, 2024 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*******************************************************************************/
package io.openliberty.tools.langserver.diagnostic;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.awaitility.core.ConditionFactory;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import io.openliberty.tools.langserver.AbstractLibertyLanguageServerTest;
import io.openliberty.tools.langserver.LibertyLanguageServer;

public class AbstractDiagnosticTest extends AbstractLibertyLanguageServerTest {
    File resourcesDir = new File("src/test/resources/workspace/diagnostic/src/main/liberty/config");

    protected static final Duration AWAIT_TIMEOUT = Duration.ofMillis(10000);
    private static final Duration AWAIT_POLL_INTERVAL = Duration.ofMillis(5);

    protected LibertyLanguageServer libertyLanguageServer;

    protected Range createRange(int lineNumber, int startChar, int endChar) {
        return new Range(new Position(lineNumber, startChar), new Position(lineNumber, endChar));
    }

    protected void testDiagnostic(String fileToTest, int expectedNumberOfErrors) throws FileNotFoundException {
        File f = new File(resourcesDir, fileToTest);
        testDiagnostic(f, expectedNumberOfErrors);
    }

    protected void testDiagnostic(File file, int expectedNumberOfErrors) throws FileNotFoundException {
        String fileURI = file.toURI().toString();
        libertyLanguageServer = initializeLanguageServerWithFileUriString(new FileInputStream(file), fileURI);

        DidOpenTextDocumentParams params = new DidOpenTextDocumentParams(libertyLanguageServer.getTextDocumentService().getOpenedDocument(fileURI));
        libertyLanguageServer.getTextDocumentService().didOpen(params);

        createAwait().untilAsserted(() -> assertNotNull(lastPublishedDiagnostics));
        createAwait().untilAsserted(() -> assertEquals(expectedNumberOfErrors, lastPublishedDiagnostics.getDiagnostics().size()));
    }

    protected void checkDiagnosticsContainsAllRanges(Range... ranges) {
        List<Diagnostic> diags = lastPublishedDiagnostics.getDiagnostics();
        List<Range> expectedDiagnosticRanges = new LinkedList<Range>(Arrays.asList(ranges));

        for (Diagnostic diag: diags) {
            boolean found = expectedDiagnosticRanges.remove(diag.getRange());
            assertTrue(found, "Found diagnostic which the test did not account for: \"" + diag.getMessage() + "\" at " + diag.getRange());
        }
        assertEquals(0, expectedDiagnosticRanges.size(),"Did not find all the expected diagnostics. These expected ranges were not found: " + expectedDiagnosticRanges.toString());
    }

    protected void checkDiagnosticsContainsMessages(String... messages) {
        List<Diagnostic> diags = lastPublishedDiagnostics.getDiagnostics();
        List<String> expectedMessages = new LinkedList<String>(Arrays.asList(messages));

        for (Diagnostic diag : diags) {
            assertFalse(diag.getMessage().isEmpty(),"Diagnostic message is unexpectedly empty.");
            assertTrue(diag.getSeverity() == DiagnosticSeverity.Error, "Diagnostic severity not set to Error as expected.");
            expectedMessages.remove(diag.getMessage());
        }
        assertEquals(0, expectedMessages.size(),"Did not find all the expected diagnostic messages. These messages were not found: " + expectedMessages.toString());
    }

    private ConditionFactory createAwait() {
        return await().pollDelay(Duration.ZERO).pollInterval(AWAIT_POLL_INTERVAL).timeout(AWAIT_TIMEOUT);
    }
}
