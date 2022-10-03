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
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.junit.Test;

public class BootstrapPropertyDiagnosticTest extends AbstractDiagnosticTest {
    @Test
    public void testBootstrapProperties() throws Exception {
        testDiagnostic("bootstrap.properties", 6);
        List<Diagnostic> diags = lastPublishedDiagnostics.getDiagnostics();
        List<Range> expectedDiagnosticRanges = new ArrayList<Range>();
        // Checking invalid value: com.ibm.ws.logging.console.format=DEVd
        expectedDiagnosticRanges.add(createRange(0, 34, 38));
        // Checking empty property: com.ibm.ws.logging.console.source=
        expectedDiagnosticRanges.add(createRange(2, 34, 34));
        // Checking invalid value: websphere.log.provider=binaryLogging-1.1
        expectedDiagnosticRanges.add(createRange(3, 23, 40));
        // Checking invalid port: default.http.port=0;
        expectedDiagnosticRanges.add(createRange(4, 18, 19));
        // Checking invalid integer: com.ibm.hpel.log.purgeMaxSize=2147483648
        expectedDiagnosticRanges.add(createRange(6, 30, 40));
        // Checking invalid boolean: com.ibm.ws.logging.copy.system.streams=yes
        expectedDiagnosticRanges.add(createRange(7, 39, 42));
        
        for (Diagnostic diag: diags) {
            boolean found = expectedDiagnosticRanges.remove(diag.getRange());
            assertTrue("Found diagnostic which the test did not account for: " + diag, found);
        }
        assertEquals("Did not find all the expected diagnostics. These expected ranges were not found: " + expectedDiagnosticRanges.toString(), 0, expectedDiagnosticRanges.size());
    }
}
