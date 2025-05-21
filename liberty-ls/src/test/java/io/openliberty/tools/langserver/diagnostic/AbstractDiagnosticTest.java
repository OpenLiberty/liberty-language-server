/*******************************************************************************
* Copyright (c) 2022, 2023 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*******************************************************************************/
package io.openliberty.tools.langserver.diagnostic;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
            assertTrue("Found diagnostic which the test did not account for: \"" + diag.getMessage() + "\" at " + diag.getRange(), found);
        }
        assertEquals("Did not find all the expected diagnostics. These expected ranges were not found: " + expectedDiagnosticRanges.toString(), 0, expectedDiagnosticRanges.size());
    }

    protected void checkDiagnosticsContainsErrorMessages(String... messages) {
        List<Diagnostic> diags = lastPublishedDiagnostics.getDiagnostics();
        List<String> expectedMessages = new LinkedList<String>(Arrays.asList(messages));

        for (Diagnostic diag : diags) {
            if (diag.getSeverity() == DiagnosticSeverity.Error) {
                assertFalse("Diagnostic message is unexpectedly empty.", diag.getMessage().isEmpty());
                assertTrue("Diagnostic severity not set to Error as expected.", diag.getSeverity() == DiagnosticSeverity.Error);
                expectedMessages.remove(diag.getMessage());
            }
        }
        assertEquals("Did not find all the expected diagnostic error messages. These messages were not found: " + expectedMessages.toString(), 0, expectedMessages.size());
    }

    protected void checkDiagnosticsContainsWarningMessages(String... messages) {
        List<Diagnostic> diags = lastPublishedDiagnostics.getDiagnostics();
        List<String> expectedMessages = new LinkedList<String>(Arrays.asList(messages));

        for (Diagnostic diag : diags) {
            if (diag.getSeverity() == DiagnosticSeverity.Warning) {
                assertFalse("Diagnostic message is unexpectedly empty.", diag.getMessage().isEmpty());
                assertTrue("Diagnostic severity not set to Warning as expected.", diag.getSeverity() == DiagnosticSeverity.Warning);
                expectedMessages.remove(diag.getMessage());
            }
        }
        assertEquals("Did not find all the expected diagnostic warning messages. These messages were not found: " + expectedMessages.toString(), 0, expectedMessages.size());
    }
    private ConditionFactory createAwait() {
        return await().pollDelay(Duration.ZERO).pollInterval(AWAIT_POLL_INTERVAL).timeout(AWAIT_TIMEOUT);
    }
}
