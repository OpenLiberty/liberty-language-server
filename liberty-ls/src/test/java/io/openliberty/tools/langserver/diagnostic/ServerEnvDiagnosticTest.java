/*******************************************************************************
* Copyright (c) 2022, 2025 IBM Corporation and others.
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
        testDiagnostic("server.env", 8);
        checkDiagnosticsContainsAllRanges(
            // Checking invalid value: WLP_LOGGING_CONSOLE_FORMAT=asdf
            createRange(0, 27, 31),
            // Checking invalid case-sensitive property: WLP_LOGGING_CONSOLE_SOURCE=messagE
            createRange(2, 27, 34),
            // Checking invalid port: WLP_DEBUG_ADDRESS=-2
            createRange(3, 18, 20),
            // Checking invalid whitespace before equal sign: WLP_DEBUG_REMOTE =n
            createRange(4,16,18),
            // Checking invalid whitespace after equal sign: WLP_LOGGING_MESSAGE_FORMAT= SIMPLE
            createRange(7,26,28),
            // Checking invalid case-sensitive property: WLP_LOGGING_CONSOLE_SOURCE=messagE
            createRange(9, 27, 34),
            // Checking invalid case-sensitive property: WLP_LOGGING_MESSAGE_SOURCE=message,au
            createRange(10, 27, 37),
            // Checking invalid empty property: WLP_DEBUG_SUSPEND=
            createRange(11, 18, 18)
        );
        checkDiagnosticsContainsErrorMessages(
            "The value `asdf` is not valid for the variable `WLP_LOGGING_CONSOLE_FORMAT`.",
            "The value `messagE` is not valid for the variable `WLP_LOGGING_CONSOLE_SOURCE`.",
            "The value `-2` is not within the valid range `[1..65535]` for the variable `WLP_DEBUG_ADDRESS`.",
            "There should be no whitespace surrounding the equal sign (=).",
            "There should be no whitespace surrounding the equal sign (=).",
            "The value `messagE` is not valid for the variable `WLP_LOGGING_CONSOLE_SOURCE`.",
            "The value `au` is not valid for the variable `WLP_LOGGING_MESSAGE_SOURCE`."
        );

        checkDiagnosticsContainsWarningMessages(
            "The value is empty for the variable `WLP_DEBUG_SUSPEND`. Check whether a value should be specified or whether the variable should be removed."
        );
    }
}
