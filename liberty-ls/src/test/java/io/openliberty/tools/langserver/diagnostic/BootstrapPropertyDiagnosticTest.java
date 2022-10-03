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

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.Test;

public class BootstrapPropertyDiagnosticTest extends AbstractDiagnosticTest {
    @Test
    public void testInvalidPort() throws Exception {
        testDiagnostic("invalidport", 1);
        Diagnostic diag = lastPublishedDiagnostics.getDiagnostics().get(0);
        // Testing "default.http.port=0" on 2nd line (line 1, char 18-19)
        checkRange(diag.getRange(), 1, 18, 19);
    }

    @Test
    public void testInvalidBoolean() throws Exception {
        testDiagnostic("invalidBoolean", 1);
        Diagnostic diag = lastPublishedDiagnostics.getDiagnostics().get(0);
        // Testing "com.ibm.ws.logging.copy.system.streams=yes" on 1st line (line 0, char 39-42)
        checkRange(diag.getRange(), 0, 39, 42);
    }

    @Test
    public void testBootstrapProperties() throws Exception {
        testDiagnostic("bootstrap", 5);
        List<Diagnostic> diags = lastPublishedDiagnostics.getDiagnostics();
        List<Range> expectedDiagnosticRanges = new ArrayList<Range>();
        // Checking invalid value: com.ibm.ws.logging.console.format=DEVd
        expectedDiagnosticRanges.add(new Range(new Position(0, 34), new Position(0, 38)));
        // Checking empty property: com.ibm.ws.logging.console.source=
        expectedDiagnosticRanges.add(new Range(new Position(2, 34), new Position(2, 34)));
        // Checking invalid value: websphere.log.provider=binaryLogging-1.1
        expectedDiagnosticRanges.add(new Range(new Position(3, 23), new Position(3, 40)));
        // Checking invalid port: default.http.port=0;
        expectedDiagnosticRanges.add(new Range(new Position(4, 18), new Position(4, 19)));
        // Checking invalid integer: com.ibm.hpel.log.purgeMaxSize=2147483648
        expectedDiagnosticRanges.add(new Range(new Position(6, 30), new Position(6, 40)));
        for (Diagnostic diag: diags) {
            expectedDiagnosticRanges.remove(diag.getRange());
        }
        assertEquals(0, expectedDiagnosticRanges.size());
    }

    private void testDiagnostic(String file, int expectedNumberOfErrors) throws FileNotFoundException {
        super.testDiagnostic(file, ".properties", expectedNumberOfErrors);
    }
}
