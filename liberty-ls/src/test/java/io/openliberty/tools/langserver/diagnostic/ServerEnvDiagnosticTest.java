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

public class ServerEnvDiagnosticTest extends AbstractDiagnosticTest {
    
    @Test
    public void testServerEnv() throws Exception {
        // has invalid, case-sensitive, case-insensitive, and negative port integer values.
        testDiagnostic("server.env", 3);
        checkDiagnosticsContainsAllRanges(
            // Checking invalid value: WLP_LOGGING_CONSOLE_FORMAT=asdf
            createRange(0, 27, 31),
            // Checking invalid case-sensitive property: WLP_LOGGING_CONSOLE_SOURCE=messagE
            createRange(2, 27, 34),
            // Checking invalid port: WLP_DEBUG_ADDRESS=-2
            createRange(3, 18, 20)
        );
        checkDiagnosticsContainsMessages(
            "The value `asdf` is not valid for the variable `WLP_LOGGING_CONSOLE_FORMAT`.",
            "The value `messagE` is not valid for the variable `WLP_LOGGING_CONSOLE_SOURCE`.",
            "The value `-2` is not within the valid range `[1..65535]` for the variable `WLP_DEBUG_ADDRESS`."
        );
    }
}
