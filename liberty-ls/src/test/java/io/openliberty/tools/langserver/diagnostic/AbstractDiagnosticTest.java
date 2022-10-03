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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.awaitility.Awaitility.await;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.Duration;

import org.awaitility.core.ConditionFactory;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;

import io.openliberty.tools.langserver.AbstractLibertyLanguageServerTest;
import io.openliberty.tools.langserver.LibertyLanguageServer;

public class AbstractDiagnosticTest extends AbstractLibertyLanguageServerTest {

    protected static final Duration AWAIT_TIMEOUT = Duration.ofMillis(10000);
	private static final Duration AWAIT_POLL_INTERVAL = Duration.ofMillis(5);

    protected LibertyLanguageServer libertyLanguageServer;

    protected void checkRange(Range range, int startLine, int startCharacter, int endLine, int endCharacter) {
        
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
    }

    private ConditionFactory createAwait() {
        return await().pollDelay(Duration.ZERO).pollInterval(AWAIT_POLL_INTERVAL).timeout(AWAIT_TIMEOUT);
    }
}
