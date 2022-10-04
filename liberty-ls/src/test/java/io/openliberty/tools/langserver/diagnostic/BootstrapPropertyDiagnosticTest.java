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

import org.junit.Test;

public class BootstrapPropertyDiagnosticTest extends AbstractDiagnosticTest {
    @Test
    public void testBootstrapProperties() throws Exception {
        testDiagnostic("bootstrap.properties", 6);
        checkDiagnosticsContainsRanges(
            // Checking invalid value: com.ibm.ws.logging.console.format=DEVd
            createRange(0, 34, 38),
            // Checking empty property: com.ibm.ws.logging.console.source=
            createRange(2, 34, 34),
            // Checking invalid value: websphere.log.provider=binaryLogging-1.1
            createRange(3, 23, 40),
            // Checking invalid port: default.http.port=0;
            createRange(4, 18, 19),
            // Checking invalid integer: com.ibm.hpel.log.purgeMaxSize=2147483648
            createRange(6, 30, 40),
            // Checking invalid boolean: com.ibm.ws.logging.copy.system.streams=yes
            createRange(7, 39, 42)
        );
    }
}
