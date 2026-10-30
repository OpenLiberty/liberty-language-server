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
package io.openliberty;

import static org.eclipse.lemminx.XMLAssert.c;
import static org.eclipse.lemminx.XMLAssert.ca;
import static org.eclipse.lemminx.XMLAssert.te;
import static org.eclipse.lemminx.XMLAssert.d;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Position;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.extensions.contentmodel.participants.XMLSchemaErrorCode;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.openliberty.tools.langserver.lemminx.services.LibertyProjectsManager;
import io.openliberty.tools.langserver.lemminx.services.SettingsService;
import io.openliberty.tools.langserver.lemminx.util.DeploymentDescriptorSchemaUtil;

/**
 * Tests for IBM deployment descriptors (ibm-web-bnd.xml and similar).
 * The sample files are in src/test/resources/deployment-descriptors.
 */
@ExtendWith(MockitoExtension.class)
public class DeploymentDescriptorTest {

    @Mock
    SettingsService settingsService;

    MockedStatic<SettingsService> settings;

    static File srcResourcesDir = new File("src/test/resources/sample");
    static File descriptorDir = new File("src/test/resources/deployment-descriptors");

    @BeforeEach
    public void setup() {
        List<WorkspaceFolder> initList = new ArrayList<>();
        initList.add(new WorkspaceFolder(srcResourcesDir.toURI().toString()));
        LibertyProjectsManager.getInstance().setWorkspaceFolders(initList);
        settings = Mockito.mockStatic(SettingsService.class);
        settings.when(SettingsService::getInstance).thenReturn(settingsService);
        Mockito.lenient().when(settingsService.getCurrentLocale()).thenReturn(Locale.US);
        Mockito.lenient().when(settingsService.getLatestRuntimeVersion()).thenReturn(null);
    }

    @AfterEach
    public void cleanup() {
        settings.close();
    }

    private static String uriOf(String relativePath) {
        return new File(descriptorDir, relativePath).toURI().toString();
    }

    private static String contentOf(String relativePath) throws IOException {
        return Files.readString(new File(descriptorDir, relativePath).toPath());
    }

    // ---- detection ----

    @Test
    public void testDetectionByFileNameWhenFileCannotBeRead() {
        // files that do not exist on disk (new files): the file name is used, in any folder
        assertTrue(DeploymentDescriptorSchemaUtil.isDeploymentDescriptor("file:///app/WEB-INF/ibm-web-bnd.xml"));
        assertTrue(DeploymentDescriptorSchemaUtil.isDeploymentDescriptor("file:///app/META-INF/ibm-application-ext.xml"));
        assertTrue(DeploymentDescriptorSchemaUtil.isDeploymentDescriptor("file:///app/WEB-INF/cachespec.xml"));
        assertTrue(DeploymentDescriptorSchemaUtil.isDeploymentDescriptor("file:///app/config/ibm-web-bnd.xml"));
        // other files
        assertFalse(DeploymentDescriptorSchemaUtil.isDeploymentDescriptor("file:///app/WEB-INF/web.xml"));
        assertFalse(DeploymentDescriptorSchemaUtil.isDeploymentDescriptor("file:///app/src/main/liberty/config/server.xml"));
        assertFalse(DeploymentDescriptorSchemaUtil.isDeploymentDescriptor("file:///app/WEB-INF/ibm-web-bnd.txt"));
        assertFalse(DeploymentDescriptorSchemaUtil.isDeploymentDescriptor(null));
    }

    @Test
    public void testDetectionByRootElementInAnyFolder() {
        // any file name, any folder, as long as the root element and namespace match
        assertTrue(DeploymentDescriptorSchemaUtil.isDeploymentDescriptor(uriOf("any-folder/config/web-bnd-schema-1_2-version-1_2.xml")));
        assertTrue(DeploymentDescriptorSchemaUtil.isDeploymentDescriptor(uriOf("any-folder/other-name.xml")));
        // same root element name but another namespace is not a deployment descriptor
        assertFalse(DeploymentDescriptorSchemaUtil.isDeploymentDescriptor(uriOf("any-folder/other-namespace.xml")));
        // IBM file name but another root element: the root element wins
        assertFalse(DeploymentDescriptorSchemaUtil.isDeploymentDescriptor(uriOf("any-folder/ibm-web-bnd.xml")));
    }

    @Test
    public void testSchemaVersionFromSchemaLocationHint() {
        String uri = "file:///app/WEB-INF/ibm-web-bnd.xml";
        String latest = DeploymentDescriptorSchemaUtil.getSchemaUri(uri, null);
        assertNotNull(latest);
        assertTrue(latest.endsWith("ibm-web-bnd_1_2.xsd"), latest);

        String older = DeploymentDescriptorSchemaUtil.getSchemaUri(uri, "http://websphere.ibm.com/xml/ns/javaee/ibm-web-bnd_1_0.xsd");
        assertTrue(older.endsWith("ibm-web-bnd_1_0.xsd"), older);

        // a hint for a different descriptor, or an unknown file, falls back to the latest version
        String other = DeploymentDescriptorSchemaUtil.getSchemaUri(uri, "http://websphere.ibm.com/xml/ns/javaee/ibm-web-ext_1_0.xsd");
        assertTrue(other.endsWith("ibm-web-bnd_1_2.xsd"), other);
        String unknown = DeploymentDescriptorSchemaUtil.getSchemaUri(uri, "http://example.com/foo.xsd");
        assertTrue(unknown.endsWith("ibm-web-bnd_1_2.xsd"), unknown);

        assertNull(DeploymentDescriptorSchemaUtil.getSchemaUri("file:///app/WEB-INF/web.xml", null));
    }

    @Test
    public void testSchemaLocationHint(@TempDir File tempDir) throws IOException {
        // the last value of the "namespace url" pair
        String hint = DeploymentDescriptorSchemaUtil.getSchemaLocationHint(uriOf("any-folder/config/web-bnd-schema-1_0-version-1_0.xml"));
        assertEquals("http://websphere.ibm.com/xml/ns/javaee/ibm-web-bnd_1_0.xsd", hint);
        // deployment descriptor without xsi:schemaLocation, not a deployment descriptor, and a file that is not there
        File noLocation = new File(tempDir, "no-location.xml");
        Files.writeString(noLocation.toPath(), "<web-bnd xmlns=\"http://websphere.ibm.com/xml/ns/javaee\" version=\"1.2\"/>");
        assertNull(DeploymentDescriptorSchemaUtil.getSchemaLocationHint(noLocation.toURI().toString()));
        assertNull(DeploymentDescriptorSchemaUtil.getSchemaLocationHint(uriOf("any-folder/other-namespace.xml")));
        assertNull(DeploymentDescriptorSchemaUtil.getSchemaLocationHint("file:///app/WEB-INF/web.xml"));
        assertNull(DeploymentDescriptorSchemaUtil.getSchemaLocationHint(null));
    }

    @Test
    public void testAllBundledSchemasAreOnTheClasspath() {
        String[] schemas = { "cachespec.xsd",
                "ibm-application-bnd_1_0.xsd", "ibm-application-bnd_1_1.xsd", "ibm-application-bnd_1_2.xsd",
                "ibm-application-ext_1_0.xsd", "ibm-application-ext_1_1.xsd",
                "ibm-common-bnd_1_0.xsd", "ibm-common-bnd_1_1.xsd", "ibm-common-bnd_1_2.xsd",
                "ibm-common-ext_1_0.xsd", "ibm-common-ext_1_1.xsd",
                "ibm-ejb-jar-bnd_1_0.xsd", "ibm-ejb-jar-bnd_1_1.xsd", "ibm-ejb-jar-bnd_1_2.xsd",
                "ibm-ejb-jar-ext_1_0.xsd", "ibm-ejb-jar-ext_1_1.xsd",
                "ibm-managed-bean-bnd_1_0.xsd", "ibm-managed-bean-bnd_1_1.xsd",
                "ibm-web-bnd_1_0.xsd", "ibm-web-bnd_1_1.xsd", "ibm-web-bnd_1_2.xsd",
                "ibm-web-ext_1_0.xsd", "ibm-web-ext_1_1.xsd",
                "ibm-ws-bnd_1_0.xsd" };
        for (String schema : schemas) {
            assertNotNull(getClass().getResource("/schema/xsd/ibm/" + schema), schema + " is missing");
        }
    }

    // ---- validation: schema version named in xsi:schemaLocation ----

    @Test
    public void testSchemaLocationVersionIsUsed() throws IOException {
        String dir = "any-folder/config/";
        // schema 1.0 with version 1.0 and schema 1.2 with version 1.2 are fine
        String ok10 = dir + "web-bnd-schema-1_0-version-1_0.xml";
        String ok12 = dir + "web-bnd-schema-1_2-version-1_2.xml";
        XMLAssert.testDiagnosticsFor(contentOf(ok10), null, null, uriOf(ok10));
        XMLAssert.testDiagnosticsFor(contentOf(ok12), null, null, uriOf(ok12));

        // the version attribute is fixed by each schema, so a mismatch is an error
        String bad10 = dir + "web-bnd-schema-1_0-version-1_2.xml";
        String bad12 = dir + "web-bnd-schema-1_2-version-1_0.xml";
        XMLAssert.testDiagnosticsFor(contentOf(bad10), null, null, uriOf(bad10),
                d(6, 12, 6, 17, XMLSchemaErrorCode.cvc_complex_type_3_1));
        XMLAssert.testDiagnosticsFor(contentOf(bad12), null, null, uriOf(bad12),
                d(6, 12, 6, 17, XMLSchemaErrorCode.cvc_complex_type_3_1));
    }

    // ---- validation: every valid sample has no diagnostics ----

    @Test
    public void testValidDescriptorsHaveNoDiagnostics() throws IOException {
        String[] files = {
                "WEB-INF/ibm-web-ext.xml",
                "WEB-INF/ibm-web-bnd.xml",
                "WEB-INF/ibm-managed-bean-bnd.xml",
                "WEB-INF/ibm-ws-bnd.xml",
                "WEB-INF/cachespec.xml",
                "META-INF/ibm-application-ext.xml",
                "META-INF/ibm-application-bnd.xml",
                "META-INF/ibm-ejb-jar-ext.xml",
                "META-INF/ibm-ejb-jar-bnd.xml" };
        for (String file : files) {
            XMLAssert.testDiagnosticsFor(contentOf(file), null, null, uriOf(file));
        }
    }

    // ---- validation: invalid samples show errors ----

    @Test
    public void testWebBndMissingRequiredAttribute() throws IOException {
        // <virtual-host/> is missing the required "name" attribute
        String file = "invalid/WEB-INF/ibm-web-bnd.xml";
        XMLAssert.testDiagnosticsFor(contentOf(file), null, null, uriOf(file),
                d(8, 5, 8, 17, XMLSchemaErrorCode.cvc_complex_type_4));
    }

    @Test
    public void testApplicationBndInvalidEnumValue() throws IOException {
        String file = "invalid/META-INF/ibm-application-bnd.xml";
        XMLAssert.testDiagnosticsFor(contentOf(file), null, null, uriOf(file),
                d(11, 30, 11, 50, XMLSchemaErrorCode.cvc_enumeration_valid),
                d(11, 30, 11, 50, XMLSchemaErrorCode.cvc_attribute_3));
    }

    @Test
    public void testApplicationExtWrongElementOrder() throws IOException {
        String file = "invalid/META-INF/ibm-application-ext.xml";
        XMLAssert.testDiagnosticsFor(contentOf(file), null, null, uriOf(file),
                d(11, 5, 11, 21, XMLSchemaErrorCode.cvc_complex_type_2_4_d));
    }

    @Test
    public void testEjbJarExtWrongElementOrder() throws IOException {
        String file = "invalid/META-INF/ibm-ejb-jar-ext.xml";
        XMLAssert.testDiagnosticsFor(contentOf(file), null, null, uriOf(file),
                d(10, 9, 10, 19, XMLSchemaErrorCode.cvc_complex_type_2_4_d));
    }

    @Test
    public void testCachespecInvalidEnumValue() throws IOException {
        // type="pathinfo" must be "pathInfo"
        String file = "invalid/WEB-INF/cachespec.xml";
        XMLAssert.testDiagnosticsFor(contentOf(file), null, null, uriOf(file),
                d(11, 39, 11, 49, XMLSchemaErrorCode.cvc_enumeration_valid),
                d(11, 39, 11, 49, XMLSchemaErrorCode.cvc_attribute_3));
    }

    @Test
    public void testWebBndMissingRequiredAttributeQuickFix() throws IOException, BadLocationException {
        // quick fix for the missing "name" attribute of <virtual-host/>
        String file = "invalid/WEB-INF/ibm-web-bnd.xml";
        Diagnostic diagnostic = d(8, 5, 8, 17, XMLSchemaErrorCode.cvc_complex_type_4);
        CodeAction insertName = ca(diagnostic, te(8, 17, 8, 17, " name=\"\""));
        insertName.getEdit().getDocumentChanges().get(0).getLeft().getTextDocument().setUri(uriOf(file));
        XMLAssert.testCodeActionsFor(contentOf(file), uriOf(file), diagnostic, (String) null, insertName);
    }

    // ---- completion ----

    @Test
    public void testElementCompletion() throws BadLocationException {
        String xml = "<web-bnd xmlns=\"http://websphere.ibm.com/xml/ns/javaee\" version=\"1.2\">\n"
                + "    |\n"
                + "</web-bnd>";
        XMLAssert.testCompletionFor(xml, null, uriOf("WEB-INF/ibm-web-bnd.xml"), null,
                c("virtual-host", "<virtual-host name=\"\" />"));
    }

    @Test
    public void testAttributeValueCompletion() throws BadLocationException {
        String xml = "<application-bnd xmlns=\"http://websphere.ibm.com/xml/ns/javaee\" version=\"1.2\">\n"
                + "    <security-role name=\"admin\">\n"
                + "        <special-subject type=\"|\"/>\n"
                + "    </security-role>\n"
                + "</application-bnd>";
        XMLAssert.testCompletionFor(xml, null, uriOf("META-INF/ibm-application-bnd.xml"), null,
                c("ALL_AUTHENTICATED_USERS", "ALL_AUTHENTICATED_USERS"),
                c("EVERYONE", "EVERYONE"));
    }

    @Test
    public void testCompletionForEveryDescriptorRoot() {
        String[][] cases = {
                { "web-ext", "WEB-INF/ibm-web-ext.xml" },
                { "web-bnd", "WEB-INF/ibm-web-bnd.xml" },
                { "managed-bean-bnd", "WEB-INF/ibm-managed-bean-bnd.xml" },
                { "webservices-bnd", "WEB-INF/ibm-ws-bnd.xml" },
                { "cache", "WEB-INF/cachespec.xml" },
                { "application-ext", "META-INF/ibm-application-ext.xml" },
                { "application-bnd", "META-INF/ibm-application-bnd.xml" },
                { "ejb-jar-ext", "META-INF/ibm-ejb-jar-ext.xml" },
                { "ejb-jar-bnd", "META-INF/ibm-ejb-jar-bnd.xml" } };
        for (String[] testCase : cases) {
            String xml = "<" + testCase[0] + " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\">\n    \n</" + testCase[0] + ">";
            XMLLanguageService languageService = new XMLLanguageService();
            DOMDocument document = DOMParser.getInstance().parse(xml, uriOf(testCase[1]),
                    languageService.getResolverExtensionManager());
            languageService.setDocumentProvider(uri -> document);
            CompletionList list = languageService.doComplete(document, new Position(1, 4), new SharedSettings(),
                    () -> {
                    });
            assertFalse(list.getItems().isEmpty(), "no schema completion for <" + testCase[0] + ">");
        }
    }

    // ---- hover ----

    @Test
    public void testHover() {
        // XMLAssert.assertHover does not set a document provider, which the real server always does
        // and which schema hover needs, so call the language service directly.
        String xml = "<web-bnd xmlns=\"http://websphere.ibm.com/xml/ns/javaee\" version=\"1.2\">\n"
                + "    <resource-ref name=\"jdbc/ds\" binding-name=\"jdbc/ds\"/>\n"
                + "</web-bnd>";
        XMLLanguageService languageService = new XMLLanguageService();
        DOMDocument document = DOMParser.getInstance().parse(xml, uriOf("WEB-INF/ibm-web-bnd.xml"),
                languageService.getResolverExtensionManager());
        languageService.setDocumentProvider(uri -> document);

        Hover root = languageService.doHover(document, new Position(0, 3), new SharedSettings());
        assertNotNull(root);
        assertTrue(root.getContents().getRight().getValue().startsWith("The root of the ibm-web-bnd deployment descriptor."),
                root.getContents().getRight().getValue());

        Hover resourceRef = languageService.doHover(document, new Position(1, 8), new SharedSettings());
        assertNotNull(resourceRef);
        assertTrue(resourceRef.getContents().getRight().getValue().contains("Specifies the binding for a resource-ref"),
                resourceRef.getContents().getRight().getValue());
    }

    @Test
    public void testHoverUsesTheVersionFromSchemaLocation() throws IOException {
        String file = "any-folder/config/web-bnd-schema-1_0-version-1_0.xml";
        XMLLanguageService languageService = new XMLLanguageService();
        DOMDocument document = DOMParser.getInstance().parse(contentOf(file), uriOf(file),
                languageService.getResolverExtensionManager());
        languageService.setDocumentProvider(uri -> document);

        Hover root = languageService.doHover(document, new Position(1, 3), new SharedSettings());
        assertNotNull(root);
        assertTrue(root.getContents().getRight().getValue().contains("Source: ibm-web-bnd_1_0.xsd"),
                root.getContents().getRight().getValue());
    }

    // ---- server.xml must keep working ----

    @Test
    public void testServerXmlIsNotTreatedAsDescriptor() {
        assertFalse(DeploymentDescriptorSchemaUtil.isDeploymentDescriptor(new File(srcResourcesDir, "test/server.xml").toURI().toString()));
    }
}
