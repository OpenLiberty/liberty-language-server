package io.openliberty;

import io.openliberty.tools.langserver.lemminx.services.SettingsService;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lsp4j.CompletionItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.eclipse.lemminx.XMLAssert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LibertyCompletionTest {

        @Mock
        SettingsService settingsService;

        MockedStatic settings;
        static String newLine = System.lineSeparator();
        static File srcResourcesDir = new File("src/test/resources/sample");
        static String serverXMLURI = new File(srcResourcesDir, "test/server.xml").toURI().toString();

        @BeforeEach
        public void setup(){
                settings = Mockito.mockStatic(SettingsService.class);
                settings.when(SettingsService::getInstance).thenReturn(settingsService);
        }

        @AfterEach
        public void cleanup(){
                settings.close();
        }

        // Tests the availability of completion of XML elements provided by the
        // server.xsd file
        @Test
        public void testXSDElementCompletionItem() throws BadLocationException {
                String serverXML = String.join(newLine, //
                                "<server description=\"Sample Liberty server\">", //
                                "       |<featureManager>", //
                                "               <feature>jaxrs-2.1</feature>", //
                                "       </featureManager>", //
                                "</server>" //
                );

                CompletionItem applicationManagerCompletion = c("applicationManager",
                                "<applicationManager></applicationManager>");
                CompletionItem webApplicationCompletion = c("webApplication",
                                "<webApplication location=\"\"></webApplication>");
                CompletionItem httpEndpointCompletion = c("httpEndpoint", "<httpEndpoint></httpEndpoint>");

                final int TOTAL_ITEMS = 172; // total number of available completion items

                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, TOTAL_ITEMS, applicationManagerCompletion,
                                webApplicationCompletion, httpEndpointCompletion);
        }

        // Tests the availability of completion of attributes inside XML elements
        // provided by the server.xsd file
        @Test
        public void testXSDAttributeCompletionItem() throws BadLocationException {
                String serverXML = String.join(newLine, //
                                "<server description=\"Sample Liberty server\">", //
                                "       <featureManager>", //
                                "               <feature>jaxrs-2.1</feature>", //
                                "       </featureManager>", //
                                "<httpEndpoint |></httpEndpoint>", //
                                "</server>" //
                );

                CompletionItem portCompletion = c("httpPort", "httpPort=\"\"");
                CompletionItem enabledCompletion = c("enabled", "enabled=\"true\"");

                final int TOTAL_ITEMS = 15; // total number of available completion items

                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, TOTAL_ITEMS, portCompletion,
                                enabledCompletion);
        }

        // Tests the
        // availability of feature completion
        @Test
        public void testFeatureCompletionItem() throws BadLocationException {
                String serverXML = String.join(newLine, //
                                "<server description=\"Sample Liberty server\">", //
                                "       <featureManager>", //
                                "               <feature>|</feature>", //
                                "               <feature>mpConfig-1.4</feature>", //
                                "       </featureManager>", //
                                "</server>" //
                );

                CompletionItem jaxrsCompletion = c("jaxrs-2.1", "jaxrs-2.1");
                CompletionItem websocket = c("websocket-1.1", "websocket-1.1");
                CompletionItem microProfileCompletion = c("microProfile-2.2", "microProfile-2.2");

                final int TOTAL_ITEMS = 345; // total number of available completion items excluding all mpConfig versions

                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, TOTAL_ITEMS, jaxrsCompletion, websocket,
                                microProfileCompletion);
                // tests for lowercase
                serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "               <feature>|</feature>", //
                        "               <feature>mpconfig</feature>", //
                        "       </featureManager>", //
                        "</server>" //
                );

                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, TOTAL_ITEMS, jaxrsCompletion, websocket,
                        microProfileCompletion);
        }

        // Tests the
        // availability of platform completion
        @Test
        public void testPlatformCompletionItem() throws BadLocationException {
                String serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "               <platform>|</platform>", //
                        "               <feature>mpConfig-1.4</feature>", //
                        "       </featureManager>", //
                        "</server>" //
                );

                // total number of available completion items
                // 3 for javaee
                //3 for jakartaee
                //16 for microprofile
                // one for CDATA and one for <-
                final int TOTAL_ITEMS = 24;

                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, TOTAL_ITEMS);

                serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "               <platform>java|</platform>", //
                        "       </featureManager>", //
                        "</server>" //
                );
                CompletionItem javaee6Completion = c("javaee-6.0", "javaee-6.0");
                CompletionItem javaee7Completion = c("javaee-7.0", "javaee-7.0");
                CompletionItem javaee8Completion = c("javaee-8.0", "javaee-8.0");

                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, 5,
                        javaee6Completion, javaee7Completion, javaee8Completion);

                serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "               <platform>jakarta|</platform>", //
                        "       </featureManager>", //
                        "</server>" //
                );

                //CompletionItem jakartaee11Completion = c("jakartaee-11.0", "jakartaee-11.0"); // this is in beta and should not be included yet
                CompletionItem jakartaee10Completion = c("jakartaee-10.0", "jakartaee-10.0");
                CompletionItem jakartaee91Completion = c("jakartaee-9.1", "jakartaee-9.1");
                CompletionItem jakartaee80Completion = c("jakartaee-8.0", "jakartaee-8.0");

                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, 5, jakartaee80Completion, jakartaee91Completion, jakartaee10Completion);

                serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "               <platform>jakartaee-11|</platform>", //
                        "       </featureManager>", //
                        "</server>" //
                );

                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, 2);  // one for CDATA and one for <-, no completion for jakartaee-11.0

                serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "               <platform>micro|</platform>", //
                        "       </featureManager>", //
                        "</server>" //
                );
                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, 18);

                serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "               <platform>javaee-8.0</platform>", //
                        "               <platform>ja|</platform>", //
                        "       </featureManager>", //
                        "</server>" //
                );
                //here result should be 2 because it should show only one for CDATA and one for <-
                // since ja is entered and javaee-8.0 is included, jakartaee should not be shown because its conflicting with javaee
                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, 2);

                serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "               <platform>jakartaee-9.0</platform>", //
                        "               <platform>ja|</platform>", //
                        "       </featureManager>", //
                        "</server>" //
                );
                //here result should be 2 because it should show only one for CDATA and one for <-
                // since ja is entered and jakartaee-8.0 is included, javaee should not be shown because its conflicting with jakartaee
                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, 2);

                serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "               <platform>microProfile-6.0</platform>", //
                        "               <platform>|</platform>", //
                        "       </featureManager>", //
                        "</server>" //
                );
                //here result should be 8 because it should show only jakartaee and javaee related completion as microprofile is already added
                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, 8);

                // repeating same platform to see for any issues
                serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "               <platform>jakartaee-9.0</platform>", //
                        "               <platform>jakartaee|</platform>", //
                        "       </featureManager>", //
                        "</server>" //
                );
                //here result should be 2 because it should show only one for CDATA and one for <-
                // since jakartaee is entered and jakartaee-9.0 is included, javaee should not be shown because its conflicting with jakartaee
                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, 2);

                // repeating same platform to see for any issues
                serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "               <platform>jakartaee-9.0</platform>", //
                        "               <platform>jakartaee-9.0</platform>", //
                        "               <platform>jakartaee|</platform>", //
                        "       </featureManager>", //
                        "</server>" //
                );
                //here result should be 2 because it should show only one for CDATA and one for <-
                // since jakartaee is entered and jakartaee-9.0 is included, javaee should not be shown because its conflicting with jakartaee
                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, 2);
        }

        // Tests the feature completion for same feature repetition
        @Test
        public void testFeatureRepetitionCompletionItem() throws BadLocationException {
                String serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "               <feature>servlet</feature>", //
                        "               <feature>servlet|</feature>", //
                        "       </featureManager>", //
                        "</server>" //
                );

                // total number of available completion items
                // 1 for sipServlet-1.1
                // one for CDATA and one for <-
                CompletionItem sipServletCompletionItem = c("sipServlet-1.1", "sipServlet-1.1");
                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, 3, sipServletCompletionItem);

                serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "               <feature>servlet-|</feature>", //
                        "               <feature>servlet</feature>", //
                        "       </featureManager>", //
                        "</server>" //
                );

                // total number of available completion items
                // 1 for sipServlet-1.1
                // one for CDATA and one for <-
                sipServletCompletionItem = c("sipServlet-1.1", "sipServlet-1.1");
                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, 3, sipServletCompletionItem);

                serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "               <feature>servlet-3.1</feature>", //
                        "               <feature>servlet|</feature>", //
                        "       </featureManager>", //
                        "</server>" //
                );

                // total number of available completion items
                // 1 for sipServlet-1.1
                // one for CDATA and one for <-
                sipServletCompletionItem = c("sipServlet-1.1", "sipServlet-1.1");
                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, 3, sipServletCompletionItem);

                serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "               <feature>servlet-</feature>", //
                        "               <feature>servlet</feature>", //
                        "               <feature>servlet|</feature>", //
                        "       </featureManager>", //
                        "</server>" //
                );

                // total number of available completion items
                // 1 for sipServlet-1.1
                // one for CDATA and one for <-
                sipServletCompletionItem = c("sipServlet-1.1", "sipServlet-1.1");
                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, 3, sipServletCompletionItem);
        }

        // Tests the
        // availability of variable completion
        @Test
        public void testVariableCompletionItem() throws BadLocationException {
                String serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "                <platform>javaee-6.0</platform>", //
                        "                <feature>acmeCA-2.0</feature>", //
                        "       </featureManager>", //
                        " <httpEndpoint host=\"*\" httpPort=\"${de|\"\n",//
                        "                  httpsPort=\"${default.https.port}\" id=\"defaultHttpEndpoint\"/>",//
                        "</server>" //
                );
                Map<String,String> propsMap=new HashMap<>();
                propsMap.put("default.http.port","9080");
                propsMap.put("default.https.port","9443");
                propsMap.put("testVar","false");
                propsMap.put("testVar2","true");
                Properties props = new Properties();
                props.putAll(propsMap);

                when(settingsService.getVariablesForServerXml(any())).thenReturn(props);
                CompletionItem httpCompletion = c("${default.http.port}", "${default.http.port}");
                CompletionItem httpsCompletion = c("${default.https.port}", "${default.https.port}");
                final int TOTAL_ITEMS = 2; // total number of available completion items

                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, TOTAL_ITEMS, httpCompletion,
                        httpsCompletion);

                serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "                <platform>javaee-6.0</platform>", //
                        "                <feature>acmeCA-2.0</feature>", //
                        "       </featureManager>", //
                        " <httpEndpoint host=\"*\" httpPort=\"kkpp|\"\n",//
                        "                  httpsPort=\"${default.https.port}\" id=\"defaultHttpEndpoint\"/>",//
                        "</server>" //
                );
                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, 0);

                // here completion should be zero, even though user has used a valid variable prefix,
                // because its not started with ${
                serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "                <platform>javaee-6.0</platform>", //
                        "                <feature>acmeCA-2.0</feature>", //
                        "       </featureManager>", //
                        " <httpEndpoint host=\"*\" httpPort=\"default|\"\n",//
                        "                  httpsPort=\"${default.https.port}\" id=\"defaultHttpEndpoint\"/>",//
                        "</server>" //
                );
                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, 0);
        }

        @Test
        public void testVariableCompletionItemWithDefaultXsdValues() throws BadLocationException {
                String serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "                <platform>javaee-6.0</platform>", //
                        "                <feature>acmeCA-2.0</feature>", //
                        "       </featureManager>", //
                        " <httpEndpoint host=\"*\" httpPort=\"${f|\"\n",//
                        "                  httpsPort=\"${default.https.port}\" id=\"defaultHttpEndpoint\"/>",//
                        "</server>" //
                );
                Map<String,String> propsMap=new HashMap<>();
                propsMap.put("default.http.port","9080");
                propsMap.put("default.https.port","9443");
                propsMap.put("testVar","false");
                propsMap.put("testVar2","true");
                Properties props = new Properties();
                props.putAll(propsMap);

                Map<String,Properties> variables=new HashMap<>();
                variables.put(srcResourcesDir.toURI().toString(),props);
                when(settingsService.getVariablesForServerXml(any())).thenReturn(props);
                CompletionItem httpCompletion = c("${default.http.port}", "${default.http.port}");
                CompletionItem httpsCompletion = c("${default.https.port}", "${default.https.port}");
                final int TOTAL_ITEMS = 2; // total number of available completion items
                // variables values -> default.http.port, default.https.port
                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, TOTAL_ITEMS, httpCompletion,
                        httpsCompletion);
        }

        // Tests the
        // availability of variable completion with ${, ${} and invalid patterns
        @Test
        public void testVariableCompletionItemWithVariablePatterns() throws BadLocationException {
                String serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "                <platform>javaee-6.0</platform>", //
                        "                <feature>acmeCA-2.0</feature>", //
                        "       </featureManager>", //
                        " <httpEndpoint host=\"*\" httpPort=\"${de|\"\n",//
                        "                  httpsPort=\"${default.https.port}\" id=\"defaultHttpEndpoint\"/>",//
                        "</server>" //
                );
                Map<String,String> propsMap=new HashMap<>();
                propsMap.put("default.http.port","9080");
                propsMap.put("default.https.port","9443");
                propsMap.put("testVar","false");
                propsMap.put("testVar2","true");
                propsMap.put("appName","app-name.war");
                propsMap.put("appName2","app-name-new.war");
                Properties props = new Properties();
                props.putAll(propsMap);

                when(settingsService.getVariablesForServerXml(any())).thenReturn(props);
                CompletionItem httpCompletion = c("${default.http.port}", "${default.http.port}");
                CompletionItem httpsCompletion = c("${default.https.port}", "${default.https.port}");
                final int TOTAL_ITEMS = 2; // total number of available completion items

                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, TOTAL_ITEMS, httpCompletion,
                        httpsCompletion);

                serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "                <platform>javaee-6.0</platform>", //
                        "                <feature>acmeCA-2.0</feature>", //
                        "       </featureManager>", //
                        " <httpEndpoint host=\"*\" httpPort=\"${de|\"\n",//
                        "                  httpsPort=\"${default.https.port}\" id=\"defaultHttpEndpoint\"/>",//
                        "</server>" //
                );
                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, TOTAL_ITEMS, httpCompletion,
                        httpsCompletion);

                serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "                <platform>javaee-6.0</platform>", //
                        "                <feature>acmeCA-2.0</feature>", //
                        "       </featureManager>", //
                        " <httpEndpoint host=\"*\" httpPort=\"${de|}\"\n",//
                        "                  httpsPort=\"${default.https.port}\" id=\"defaultHttpEndpoint\"/>",//
                        "</server>" //
                );
                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, TOTAL_ITEMS, httpCompletion,
                        httpsCompletion);

                serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "                <platform>javaee-6.0</platform>", //
                        "                <feature>acmeCA-2.0</feature>", //
                        "       </featureManager>", //
                        " <httpEndpoint host=\"*\" httpPort=\"d$e|\"\n",//
                        "                  httpsPort=\"${default.https.port}\" id=\"defaultHttpEndpoint\"/>",//
                        "</server>" //
                );
                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, 0);

                // single variable completion with prefix
                serverXML = String.join(newLine,
                        "<server description=\"Sample Liberty server\">",
                        "   <featureManager>",
                        "       <feature>servlet</feature>",
                        "       <platform>jakartaee-9.1</platform>",
                        "   </featureManager>",
                        "   <webApplication contextRoot=\"/app-name\" location=\"apps/${app|\" />",
                        "   <httpEndpoint id=\"defaultHttpEndpoint\" httpPort=\"9080\" httpsPort=\"9443\"/>",
                        "   <ssl id=\"defaultSSLConfig\" trustDefaultCerts=\"true\" />",
                        "</server>"
                );

                CompletionItem testVarCompletion = c("apps/${appName}", "apps/${appName}");
                CompletionItem testVar2Completion = c("apps/${appName2}", "apps/${appName2}");

                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, TOTAL_ITEMS, testVarCompletion,
                        testVar2Completion);
        }

        // Tests the
        // availability of multiple variable completion
        @Test
        public void testVariableCompletionItemWithMultipleVars() throws BadLocationException {
                Map<String,String> propsMap=new HashMap<>();
                propsMap.put("default.http.port","9080");
                propsMap.put("default.https.port","9443");
                propsMap.put("appLocation","root");
                propsMap.put("testVar","app-name.war");
                propsMap.put("testVar2","app-name2.war");
                Properties props = new Properties();
                props.putAll(propsMap);

                when(settingsService.getVariablesForServerXml(any())).thenReturn(props);

                String serverXML = String.join(newLine,
                        "<server description=\"Sample Liberty server\">",
                        "   <featureManager>",
                        "       <feature>servlet</feature>",
                        "       <platform>jakartaee-9.1</platform>",
                        "   </featureManager>",
                        "   <webApplication contextRoot=\"/app-name\" location=\"${testVar2}/${tes|\" />",
                        "   <httpEndpoint id=\"defaultHttpEndpoint\" httpPort=\"9080\" httpsPort=\"9443\"/>",
                        "   <ssl id=\"defaultSSLConfig\" trustDefaultCerts=\"true\" />",
                        "</server>"
                );

                CompletionItem testVarCompletion = c("${testVar2}/${testVar}", "${testVar2}/${testVar}");
                CompletionItem testVar2Completion = c("${testVar2}/${testVar2}", "${testVar2}/${testVar2}");
                final int TOTAL_ITEMS = 2; // total number of available completion items

                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, TOTAL_ITEMS, testVarCompletion,
                        testVar2Completion);


                serverXML = String.join(newLine,
                        "<server description=\"Sample Liberty server\">",
                        "   <featureManager>",
                        "       <feature>servlet</feature>",
                        "       <platform>jakartaee-9.1</platform>",
                        "   </featureManager>",
                        "   <webApplication contextRoot=\"/app-name\" location=\"${testVar2}/apps/${appLocation}/${tes|}\" />",
                        "   <httpEndpoint id=\"defaultHttpEndpoint\" httpPort=\"9080\" httpsPort=\"9443\"/>",
                        "   <ssl id=\"defaultSSLConfig\" trustDefaultCerts=\"true\" />",
                        "</server>"
                );

                testVarCompletion = c("${testVar2}/apps/${appLocation}/${testVar}", "${testVar2}/apps/${appLocation}/${testVar}");
                testVar2Completion = c("${testVar2}/apps/${appLocation}/${testVar2}", "${testVar2}/apps/${appLocation}/${testVar2}");

                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, TOTAL_ITEMS, testVarCompletion,
                        testVar2Completion);

                // for showing completion of second variable, variable should always prefix with ${ . Hence show no completion here
                serverXML = String.join(newLine,
                        "<server description=\"Sample Liberty server\">",
                        "   <featureManager>",
                        "       <feature>servlet</feature>",
                        "       <platform>jakartaee-9.1</platform>",
                        "   </featureManager>",
                        "   <webApplication contextRoot=\"/app-name\" location=\"${testVar2}/tes|\" />",
                        "   <httpEndpoint id=\"defaultHttpEndpoint\" httpPort=\"9080\" httpsPort=\"9443\"/>",
                        "   <ssl id=\"defaultSSLConfig\" trustDefaultCerts=\"true\" />",
                        "</server>"
                );
                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, 0);

                // in this case, completion will be shown for second variable as there is no special characters in the prefix string
                serverXML = String.join(newLine,
                        "<server description=\"Sample Liberty server\">",
                        "   <featureManager>",
                        "       <feature>servlet</feature>",
                        "       <platform>jakartaee-9.1</platform>",
                        "   </featureManager>",
                        "   <webApplication contextRoot=\"/app-name\" location=\"${testVar2}tes|\" />",
                        "   <httpEndpoint id=\"defaultHttpEndpoint\" httpPort=\"9080\" httpsPort=\"9443\"/>",
                        "   <ssl id=\"defaultSSLConfig\" trustDefaultCerts=\"true\" />",
                        "</server>"
                );

                testVarCompletion = c("${testVar2}${testVar}", "${testVar2}${testVar}");
                testVar2Completion = c("${testVar2}${testVar2}", "${testVar2}${testVar2}");

                XMLAssert.testCompletionFor(serverXML, null, serverXMLURI, TOTAL_ITEMS, testVarCompletion,
                        testVar2Completion);
        }
}
