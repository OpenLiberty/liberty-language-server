/*******************************************************************************
* Copyright (c) 2022 IBM Corporation and others.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.Duration;
import java.util.List;

import org.awaitility.core.ConditionFactory;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import io.openliberty.tools.langserver.AbstractLibertyLanguageServerTest;
import io.openliberty.tools.langserver.LibertyLanguageServer;

public class AbstractDiagnosticTest extends AbstractLibertyLanguageServerTest {

    protected static final Duration AWAIT_TIMEOUT = Duration.ofMillis(10000);
	private static final Duration AWAIT_POLL_INTERVAL = Duration.ofMillis(5);

    protected LibertyLanguageServer libertyLanguageServer;

    protected Range createRange(int lineNumber, int startChar, int endChar) {
        return new Range(new Position(lineNumber, startChar), new Position(lineNumber, endChar));
    }

    protected void testDiagnostic(String fileToTest, String extension, int expectedNumberOfErrors) throws FileNotFoundException {
        File f = new File("src/test/resources/workspace/diagnostic/" + fileToTest + extension);
        testDiagnostic(f, extension, expectedNumberOfErrors);
    }

    protected void testDiagnostic(File file, String extension, int expectedNumberOfErrors) throws FileNotFoundException {
        libertyLanguageServer = initializeLanguageServerWithFilename(new FileInputStream(file), file.toString());

        DidOpenTextDocumentParams params = new DidOpenTextDocumentParams(libertyLanguageServer.getTextDocumentService().getOpenedDocument(file.toString()));
        libertyLanguageServer.getTextDocumentService().didOpen(params);

        createAwait().untilAsserted(() -> assertNotNull(lastPublishedDiagnostics));
        createAwait().untilAsserted(() -> assertEquals(expectedNumberOfErrors, lastPublishedDiagnostics.getDiagnostics().size()));

        checkHasNonEmptyMessage(lastPublishedDiagnostics.getDiagnostics());
    }

    private void checkHasNonEmptyMessage(List<Diagnostic> diagnostics) {
        for (Diagnostic diag : diagnostics) {
            assertFalse(diag.getMessage().isEmpty());
        }
    }

    private ConditionFactory createAwait() {
        return await().pollDelay(Duration.ZERO).pollInterval(AWAIT_POLL_INTERVAL).timeout(AWAIT_TIMEOUT);
    }
}
