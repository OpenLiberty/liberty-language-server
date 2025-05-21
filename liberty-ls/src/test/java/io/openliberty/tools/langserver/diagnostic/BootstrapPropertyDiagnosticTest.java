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

public class BootstrapPropertyDiagnosticTest extends AbstractDiagnosticTest {
    @Test
    public void testBootstrapProperties() throws Exception {
        testDiagnostic("bootstrap.properties", 9);
        checkDiagnosticsContainsAllRanges(
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
            createRange(7, 39, 42),
            // Checking invalid package list: org.osgi.framework.bootdelegation=com.ibm.websphere,com.
            createRange(8, 34, 56),
            // Checking invalid value: com.ibm.ws.logging.console.format=DEVd
            createRange(10, 34, 38),
            // Checking invalid value: com.ibm.ws.logging.console.source=trace,aud
            createRange(11, 34, 43)
        );
        checkDiagnosticsContainsErrorMessages(
            "The value `DEVd` is not valid for the property `com.ibm.ws.logging.console.format`.",
            "The value `binaryLogging-1.1` is not valid for the property `websphere.log.provider`.",
            "The value `0` is not within the valid range `[1..65535]` for the property `default.http.port`.",
            "The value `2147483648` is not within the valid range `[0..2147483647]` for the property `com.ibm.hpel.log.purgeMaxSize`.",
            "The value `yes` is not valid for the property `com.ibm.ws.logging.copy.system.streams`.",
            "This value must be a comma-delimited list of Java packages.",
            "The value `DEVd` is not valid for the property `com.ibm.ws.logging.console.format`.",
            "The value `aud` is not valid for the property `com.ibm.ws.logging.console.source`."
        );

        checkDiagnosticsContainsWarningMessages(
            "The value is empty for the property `com.ibm.ws.logging.console.source`. Check whether a value should be specified or whether the property should be removed."
        );
    }
}
