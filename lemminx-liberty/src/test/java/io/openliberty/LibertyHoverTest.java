package io.openliberty;

import io.openliberty.tools.langserver.lemminx.services.LibertyProjectsManager;
import io.openliberty.tools.langserver.lemminx.services.LibertyWorkspace;
import io.openliberty.tools.langserver.lemminx.services.SettingsService;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.openliberty.tools.langserver.lemminx.LibertyXSDURIResolver.SERVER_XSD_RESOURCE_DEFAULT;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.eclipse.lemminx.XMLAssert.r;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

@ExtendWith(MockitoExtension.class)
public class LibertyHoverTest {

        @Mock
        SettingsService settingsService;

        MockedStatic settings;
        static String newLine = System.lineSeparator();
        static File srcResourcesDir = new File("src/test/resources/sample");
        static List<WorkspaceFolder> initList = new ArrayList<WorkspaceFolder>();
        LibertyProjectsManager libPM;
        LibertyWorkspace libWorkspace;
        static String serverXMLURI = new File(srcResourcesDir, "test/server.xml").toURI().toString();


        @BeforeEach
        public void setup(){
                initList.add(new WorkspaceFolder(srcResourcesDir.toURI().toString()));
                libPM = LibertyProjectsManager.getInstance();
                libPM.setWorkspaceFolders(initList);
                libWorkspace = libPM.getLibertyWorkspaceFolders().iterator().next();
                settings=Mockito.mockStatic(SettingsService.class);
                settings.when(SettingsService::getInstance).thenReturn(settingsService);
                when(settingsService.getCurrentLocale()).thenReturn(Locale.US);
        }

        @AfterEach
        public void cleanup(){
                settings.close();
        }

        @Test
        public void testFeatureHover() throws BadLocationException {
                String serverXML = String.join(newLine, //
                                "<server description=\"Sample Liberty server\">", //
                                "       <featureManager>", //
                                "               <feature>j|axrs-2.1</feature>", //
                                "       </featureManager>", //
                                "</server>" //
                );

                XMLAssert.assertHover(serverXML, serverXMLURI,
                            "Description: This feature enables support for Java API for RESTful Web Services v2.1.  "
                                            + "JAX-RS annotations can be used to define web service clients and endpoints that comply with the REST architectural style. "
                                            + "Endpoints are accessed through a common interface that is based on the HTTP standard methods."
                                            + "  \n  \n"
                                            + "Enabled by: microProfile-2.0, microProfile-2.1, microProfile-2.2, microProfile-3.0, microProfile-3.2, microProfile-3.3, microProfile-4.0, microProfile-4.1, mpOpenAPI-2.0, opentracing-1.3, opentracing-2.0, webProfile-8.0"
                                            + "  \n  \n"
                                            + "Enables: jaxrsClient-2.1, servlet-4.0",
                            r(2, 24, 2, 33));

        }

        @Test
        public void testFeatureHoverTrim() throws BadLocationException {

                String serverXML = String.join(newLine, //
                                "<server description=\"Sample Liberty server\">", //
                                "       <featureManager>", //
                                "               <feature>j|axrs-2.1 </feature>", //
                                "       </featureManager>", //
                                "</server>" //
                );

                XMLAssert.assertHover(serverXML, serverXMLURI,
                                "Description: This feature enables support for Java API for RESTful Web Services v2.1.  "
                                                + "JAX-RS annotations can be used to define web service clients and endpoints that comply with the REST architectural style. "
                                                + "Endpoints are accessed through a common interface that is based on the HTTP standard methods."
                                                + "  \n  \n"
                                                + "Enabled by: microProfile-2.0, microProfile-2.1, microProfile-2.2, microProfile-3.0, microProfile-3.2, microProfile-3.3, microProfile-4.0, microProfile-4.1, mpOpenAPI-2.0, opentracing-1.3, opentracing-2.0, webProfile-8.0"
                                                + "  \n  \n"
                                                + "Enables: jaxrsClient-2.1, servlet-4.0",
                                r(2, 24, 2, 34));

        }

        @Test
        public void testVersionlessFeatureHover() throws BadLocationException {
                String serverXML = String.join(newLine, //
                                "<server description=\"Sample Liberty server\">", //
                                "       <featureManager>", //
                                "               <feature>j|dbc</feature>", //
                                "       </featureManager>", //
                                "</server>" //
                );

                XMLAssert.assertHover(serverXML, serverXMLURI,
                            "Description: This feature enables support for versionless jdbc.",
                            r(2, 24, 2, 28));

        }

        @Test
        public void testXSDSchemaHover() throws BadLocationException, IOException {
                String serverXSDURI = SERVER_XSD_RESOURCE_DEFAULT.getDeployedPath().toUri().toString().replace("///", "/");

                String serverXML = String.join(newLine, //
                                "<server description=\"Sample Liberty server\">", //
                                "       <feature|Manager>", //
                                "               <feature>jaxrs-2.1</feature>", //
                                "       </featureManager>", //
                                "</server>" //
                );

                XMLAssert.assertHover(serverXML, serverXMLURI, "Defines how the server loads features." + //
                                System.lineSeparator() + System.lineSeparator() + //
                                "Source: [server-cached-25.0.0.5.xsd](" + serverXSDURI + ")", //
                                r(1, 8, 1, 22));
        }

        @Test
        public void testPlatformHover() throws BadLocationException {

                String serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "               <platform>j|avaee-6.0</platform>", //
                        "       </featureManager>", //
                        "</server>" //
                );
                XMLAssert.assertHover(serverXML, serverXMLURI,
                        "Description: This platform resolves the Liberty features that support the Java EE 6.0 platform.",
                        r(2, 25, 2, 35));

                serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "               <platform>j|akartaee-10.0</platform>", //
                        "       </featureManager>", //
                        "</server>" //
                );
                XMLAssert.assertHover(serverXML, serverXMLURI,
                        "Description: This platform resolves the Liberty features that support the Jakarta EE 10.0 platform.",
                        r(2, 25, 2, 39));

                serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "               <platform>mi|croProfile-1.2</platform>", //
                        "       </featureManager>", //
                        "</server>" //
                );
                XMLAssert.assertHover(serverXML, serverXMLURI,
                        "Description: This platform resolves the Liberty features that support the MicroProfile 1.2 for Cloud Native Java platform.",
                        r(2, 25, 2, 41));

                // test hover for invalid value - should be null
                serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "               <platform>j|akartaee-10.00</platform>", //
                        "       </featureManager>", //
                        "</server>" //
                );
                XMLAssert.assertHover(serverXML, serverXMLURI,
                        null,
                        r(2, 25, 2, 39));
        }

        @Test
        public void testVariableHover() throws BadLocationException {
                String serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "                <platform>javaee-6.0</platform>", //
                        "                <feature>acmeCA-2.0</feature>", //
                        "       </featureManager>", //
                        " <httpEndpoint host=\"*\" httpPort=\"${default.|http.port}\"\n",//
                        "                  httpsPort=\"${default.https.port}\" id=\"defaultHttpEndpoint\"/>",//
                        "</server>" //
                );
                Map<String, String> propsMap = new HashMap<>();
                propsMap.put("default.http.port", "9080");
                Properties props = new Properties();
                props.putAll(propsMap);
                when(settingsService.getVariablesForServerXml(any())).thenReturn(props);
                XMLAssert.assertHover(serverXML, serverXMLURI,
                        "default.http.port = 9080",
                        r(5, 33, 5, 55));
        }
}
