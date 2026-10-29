/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.tools.test;

import static org.eclipse.lemminx.XMLAssert.c;
import static org.eclipse.lemminx.XMLAssert.ca;
import static org.eclipse.lemminx.XMLAssert.d;
import static org.eclipse.lemminx.XMLAssert.te;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.extensions.contentmodel.participants.XMLSchemaErrorCode;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for IBM deployment descriptors, using ibm-web-bnd.xml as the example.
 *
 * Unlike the unit tests, these run against the packaged liberty-langserver-lemminx jar, loaded the way the
 * real LemMinX server loads it. A Liberty install is not needed because the schemas are bundled in the jar.
 *
 * Sample files are in src/main/descriptors.
 */
public class IBMDescriptorsIT {

    private static final File DESCRIPTORS = new File(System.getProperty("user.dir"), "src/main/descriptors");
    private static final String VALID_FILE = "WEB-INF/ibm-web-bnd.xml";
    // <virtual-host/> is missing the required "name" attribute
    private static final String INVALID_FILE = "invalid/WEB-INF/ibm-web-bnd.xml";

    private static String uriOf(String relativePath) {
        return new File(DESCRIPTORS, relativePath).toURI().toString();
    }

    private static String contentOf(String relativePath) throws IOException {
        return new String(Files.readAllBytes(new File(DESCRIPTORS, relativePath).toPath()), "UTF-8");
    }

    @Test
    public void testSchemaIsInsideTheJar() {
        assertNotNull(getClass().getResource("/schema/xsd/ibm/ibm-web-bnd_1_2.xsd"),
                "ibm-web-bnd_1_2.xsd is missing from the jar");
    }

    @Test
    public void testValidDescriptorHasNoDiagnostics() throws IOException {
        XMLAssert.testDiagnosticsFor(contentOf(VALID_FILE), null, null, uriOf(VALID_FILE));
    }

    @Test
    public void testMissingRequiredAttributeDiagnostic() throws IOException {
        XMLAssert.testDiagnosticsFor(contentOf(INVALID_FILE), null, null, uriOf(INVALID_FILE),
                d(8, 5, 8, 17, XMLSchemaErrorCode.cvc_complex_type_4));
    }

    @Test
    public void testElementCompletion() throws BadLocationException {
        String xml = "<web-bnd xmlns=\"http://websphere.ibm.com/xml/ns/javaee\" version=\"1.2\">\n"
                + "    |\n"
                + "</web-bnd>";
        XMLAssert.testCompletionFor(xml, null, uriOf(VALID_FILE), null,
                c("virtual-host", "<virtual-host name=\"\" />"));
    }

    @Test
    public void testMissingRequiredAttributeQuickFix() throws IOException, BadLocationException {
        Diagnostic diagnostic = d(8, 5, 8, 17, XMLSchemaErrorCode.cvc_complex_type_4);
        CodeAction insertName = ca(diagnostic, te(8, 17, 8, 17, " name=\"\""));
        insertName.getEdit().getDocumentChanges().get(0).getLeft().getTextDocument().setUri(uriOf(INVALID_FILE));
        XMLAssert.testCodeActionsFor(contentOf(INVALID_FILE), uriOf(INVALID_FILE), diagnostic, (String) null,
                insertName);
    }
}
