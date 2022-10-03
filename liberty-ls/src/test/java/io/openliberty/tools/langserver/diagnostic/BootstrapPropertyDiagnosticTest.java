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

import java.io.FileNotFoundException;

import org.junit.Test;

public class BootstrapPropertyDiagnosticTest extends AbstractDiagnosticTest {
    @Test
    public void testInvalidPort() throws Exception {
        testDiagnostic("invalidport", 1);
    }

    @Test
    public void testInvalidBoolean() throws Exception {
        testDiagnostic("invalidBoolean", 1);
    }

    @Test
    public void testBootstrapProperties() throws Exception {
        testDiagnostic("bootstrap", 4);
    }

    private void testDiagnostic(String file, int expectedNumberOfErrors) throws FileNotFoundException {
        super.testDiagnostic(file, ".properties", expectedNumberOfErrors);
    }
}
