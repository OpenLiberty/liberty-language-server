package io.openliberty;

import io.openliberty.tools.langserver.lemminx.services.SettingsService;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.openliberty.tools.langserver.lemminx.LibertyDiagnosticParticipant;
import io.openliberty.tools.langserver.lemminx.data.FeatureListGraph;
import io.openliberty.tools.langserver.lemminx.services.FeatureService;
import io.openliberty.tools.langserver.lemminx.services.LibertyProjectsManager;
import io.openliberty.tools.langserver.lemminx.services.LibertyWorkspace;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.eclipse.lemminx.XMLAssert.r;
import static org.eclipse.lemminx.XMLAssert.ca;
import static org.eclipse.lemminx.XMLAssert.te;
import static org.eclipse.lemminx.XMLAssert.tde;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

@ExtendWith(MockitoExtension.class)
public class LibertyDiagnosticTest {

    @Mock
    SettingsService settingsService;

    static String newLine = System.lineSeparator();

    static File srcResourcesDir = new File("src/test/resources/sample");
    static File featureList = new File("src/test/resources/featurelist-ol-24.0.0.11.xml");
    static String serverXMLURI = new File(srcResourcesDir, "test/server.xml").toURI().toString();
    static String sampleserverXMLURI = new File(srcResourcesDir, "sample-server.xml").toURI().toString();
    static List<WorkspaceFolder> initList = new ArrayList<WorkspaceFolder>();
    LibertyProjectsManager libPM;
    LibertyWorkspace libWorkspace;
    MockedStatic settings;

    @BeforeEach
    public void setupWorkspace() {
        initList.add(new WorkspaceFolder(srcResourcesDir.toURI().toString()));
        libPM = LibertyProjectsManager.getInstance();
        libPM.setWorkspaceFolders(initList);
        libWorkspace = libPM.getLibertyWorkspaceFolders().iterator().next();
        settings= Mockito.mockStatic(SettingsService.class);
        settings.when(SettingsService::getInstance).thenReturn(settingsService);
    }

    @AfterEach
    public void cleanup(){
        settings.close();
    }

    @Test
    public void testFeatureDuplicateDiagnostic() {
        String serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "               <feature>jaxrs-2.1</feature>", //
                "               <feature>jaxrs-2.1</feature>", //
                "               <feature>jsonp-1.1</feature>", //
                "               <!-- <feature>comment</feature> -->", //
                "               <feature>jsonp-1.1</feature>", //
                "       </featureManager>", //
                "</server>" //
        );

        Diagnostic dup1 = new Diagnostic();
        dup1.setRange(r(3, 24, 3, 33));
        dup1.setMessage("ERROR: jaxrs-2.1 is already included.");

        Diagnostic dup2 = new Diagnostic();
        dup2.setRange(r(6, 24, 6, 33));
        dup2.setMessage("ERROR: jsonp-1.1 is already included.");

        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI, dup1, dup2);
    }

    @Test
    public void testAnotherVersionOfFeatureDuplicateDiagnostic() {
        String serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "               <feature>jaxrs-2.0</feature>", //
                "               <feature>jaxrs-2.1</feature>", //
                "               <feature>jsonp-1.1</feature>", //
                "       </featureManager>", //
                "</server>" //
        );

        Diagnostic dup1 = new Diagnostic();
        dup1.setRange(r(3, 24, 3, 33));
        dup1.setMessage("ERROR: More than one version of feature jaxrs is included. Only one version of a feature may be specified.");

        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI, dup1);
    }

    @Test
    public void testInvalidFeatureDiagnostic() throws BadLocationException{
        String serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "               <feature>jaxrs-2.1</feature>", //
                "               <feature>jaX</feature>", //
                "               <feature>jsonp-1.1</feature>", //
                "               <!-- <feature>comment</feature> -->", //
                "               <feature>invalid</feature>", //
                "       </featureManager>", //
                "</server>" //
        );
        Diagnostic invalid1 = new Diagnostic();
        invalid1.setRange(r(3, 24, 3, 27));
        invalid1.setCode(LibertyDiagnosticParticipant.INCORRECT_FEATURE_CODE);
        invalid1.setMessage("ERROR: The feature \"jaX\" does not exist.");

        Diagnostic invalid2 = new Diagnostic();
        invalid2.setRange(r(6, 24, 6, 31));
        invalid2.setCode(LibertyDiagnosticParticipant.INCORRECT_FEATURE_CODE);
        invalid2.setMessage("ERROR: The feature \"invalid\" does not exist.");

        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI, invalid1, invalid2);

        List<Diagnostic> diagnostics = new ArrayList<Diagnostic>();
        diagnostics.add(invalid1);

        List<String> featuresStartWithJAX = new ArrayList<String>();
        featuresStartWithJAX.add("jaxb-2.2");
        //featuresStartWithJAX.add("jaxrs-2.0"); excluded because it matches an existing feature with a different version
        //featuresStartWithJAX.add("jaxrs-2.1"); excluded because it matches an existing feature
        featuresStartWithJAX.add("jaxrsClient-2.0");
        featuresStartWithJAX.add("jaxrsClient-2.1");
        featuresStartWithJAX.add("jaxws-2.2");
        //adding versionless features
        featuresStartWithJAX.add("jaxws");
        featuresStartWithJAX.add("jaxb");
        featuresStartWithJAX.add("jaxrsClient");
        //featuresStartWithJAX.add("jaxrs"); excluded because it matches an existing feature
        Collections.sort(featuresStartWithJAX);

        List<CodeAction> codeActions = new ArrayList<CodeAction>();
        for (String nextFeature: featuresStartWithJAX) {
            TextEdit texted = te(invalid1.getRange().getStart().getLine(), invalid1.getRange().getStart().getCharacter(),
                                invalid1.getRange().getEnd().getLine(), invalid1.getRange().getEnd().getCharacter(), nextFeature);
            CodeAction invalidCodeAction = ca(invalid1, texted);

            codeActions.add(invalidCodeAction);
        }

        XMLAssert.testCodeActionsFor(serverXML, invalid1, codeActions.get(0), codeActions.get(1),
                codeActions.get(2), codeActions.get(3), codeActions.get(4), codeActions.get(5), codeActions.get(6));

    }

    @Test
    public void testTrimmedFeatureDiagnostic() {
        String serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "               <feature>jaxrs-2.1 </feature>",
                "       </featureManager>", //
                "</server>" //
        );

        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI, (Diagnostic[]) null);
    }

    @Test
    public void testDiagnosticsForInclude() throws IOException, BadLocationException {
        // LibertyWorkspace must be initialized
        List<WorkspaceFolder> initList = new ArrayList<WorkspaceFolder>();
        initList.add(new WorkspaceFolder(new File("src/test/resources").toURI().toString()));
        LibertyProjectsManager.getInstance().setWorkspaceFolders(initList);

        String serverXML = String.join(newLine, //
                "<server description=\"default server\">", //
                "    <include optional=\"true\" location=\"./empty_server.xml\"/>", //
                "    <include optional=\"true\" location=\"/empty_server.xml\"/>", //
                "    <include optional=\"true\" location=\"MISSING FILE\"/>", //
                "    <include", //
                "            optional=\"true\" location=\"MULTI LINER\"/>", //
                "    <include optional=\"false\" location=\"MISSING FILE.xml\"/>", //
                "    <include location=\"MISSING FILE.xml\"/>", //
                "    <include location=\"/empty_server.xml/\"/>", //
                "    <include location=\"/testDir.xml\"/>", //
                "</server>"
        );

        // Diagnostic location1 = new Diagnostic();
        File serverXMLFile = new File("src/test/resources/server.xml");
        assertFalse(serverXMLFile.exists());
        // Diagnostic will not be made if found
        assertTrue(new File("src/test/resources/empty_server.xml").exists());

        Diagnostic not_xml = new Diagnostic();
        not_xml.setRange(r(3, 29, 3, 52));
        not_xml.setMessage("The specified resource is not an XML file. If it is a directory, it must end with a trailing slash.");

        Diagnostic multi_liner = new Diagnostic();
        multi_liner.setRange(r(5, 28, 5, 50));
        multi_liner.setMessage("The specified resource is not an XML file. If it is a directory, it must end with a trailing slash.");

        Diagnostic not_optional = new Diagnostic();
        not_optional.setRange(r(6, 13, 6, 29));
        not_optional.setCode("not_optional");
        not_optional.setMessage("The specified resource cannot be skipped. Check location value or set optional to true.");

        Diagnostic missing_xml = new Diagnostic();
        missing_xml.setRange(r(6, 30, 6, 57));
        missing_xml.setCode("missing_file");
        missing_xml.setMessage("The resource at the specified location could not be found.");

        Diagnostic optional_not_defined = new Diagnostic();
        optional_not_defined.setRange(r(7, 13, 7, 40));
        optional_not_defined.setCode("implicit_not_optional");
        optional_not_defined.setMessage("The specified resource cannot be skipped. Check location value or add optional attribute.");

        Diagnostic missing_xml2 = new Diagnostic();
        missing_xml2.setRange(r(7, 13, 7, 40));
        missing_xml2.setCode("missing_file");
        missing_xml2.setMessage("The resource at the specified location could not be found.");

        Diagnostic dirIsFile = new Diagnostic();
        dirIsFile.setRange(r(8, 13, 8, 42));
        dirIsFile.setCode("is_file_not_dir");
        dirIsFile.setMessage("Path specified a directory, but resource exists as a file. Please remove the trailing slash.");

        Diagnostic fileIsDir = new Diagnostic();
        fileIsDir.setRange(r(9, 13, 9, 36));
        fileIsDir.setCode("is_dir_not_file");
        fileIsDir.setMessage("Path specified a file, but resource exists as a directory. Please add a trailing slash.");

        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLFile.toURI().toString(),
                not_xml, multi_liner, not_optional, missing_xml, optional_not_defined, missing_xml2,
                dirIsFile, fileIsDir);

        // Check code actions for add/remove trailing slashes
        String fixedFilePath = "location=\"/empty_server.xml\"";
        TextEdit dirIsFileTextEdit = te(dirIsFile.getRange().getStart().getLine(), dirIsFile.getRange().getStart().getCharacter(),
                            dirIsFile.getRange().getEnd().getLine(), dirIsFile.getRange().getEnd().getCharacter(), fixedFilePath);
        CodeAction dirIsFileCodeAction = ca(dirIsFile, dirIsFileTextEdit);

        String fixedDirPath = "location=\"/testDir.xml/\"";
        TextEdit fileIsDirTextEdit = te(fileIsDir.getRange().getStart().getLine(), fileIsDir.getRange().getStart().getCharacter(),
                            fileIsDir.getRange().getEnd().getLine(), fileIsDir.getRange().getEnd().getCharacter(), fixedDirPath);
        CodeAction fileIsDirCodeAction = ca(fileIsDir, fileIsDirTextEdit);


        XMLAssert.testCodeActionsFor(serverXML, dirIsFile, dirIsFileCodeAction);

        XMLAssert.testCodeActionsFor(serverXML, fileIsDir, fileIsDirCodeAction);
    }

    @Test
    public void testDiagnosticsForIncludeWindows() throws BadLocationException {
        if (!File.separator.equals("\\")) { // skip test if not Windows
            return;
        }
        // LibertyWorkspace must be initialized
        List<WorkspaceFolder> initList = new ArrayList<WorkspaceFolder>();
        initList.add(new WorkspaceFolder(new File("src/test/resources").toURI().toString()));
        LibertyProjectsManager.getInstance().setWorkspaceFolders(initList);

        String serverXML = String.join(newLine, //
                "<server description=\"default server\">", //
                "    <include location=\"\\empty_server.xml\\\"/>", //
                "    <include location=\"\\testDir.xml\"/>", //
                "</server>"
        );

        // Diagnostic location1 = new Diagnostic();
        File serverXMLFile = new File("src/test/resources/server.xml");
        assertFalse(serverXMLFile.exists());
        // Diagnostic will not be made if found
        assertTrue(new File("src/test/resources/empty_server.xml").exists());

        Diagnostic dirIsFile = new Diagnostic();
        dirIsFile.setRange(r(1, 13, 1, 42));
        dirIsFile.setCode("is_file_not_dir");
        dirIsFile.setMessage("Path specified a directory, but resource exists as a file. Please remove the trailing slash.");

        Diagnostic fileIsDir = new Diagnostic();
        fileIsDir.setRange(r(2, 13, 2, 36));
        fileIsDir.setCode("is_dir_not_file");
        fileIsDir.setMessage("Path specified a file, but resource exists as a directory. Please add a trailing slash.");

        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLFile.toURI().toString(),
                dirIsFile, fileIsDir);

        String fixedFilePath = "location=\"\\empty_server.xml\"";
        TextEdit dirIsFileTextEdit = te(dirIsFile.getRange().getStart().getLine(), dirIsFile.getRange().getStart().getCharacter(),
                            dirIsFile.getRange().getEnd().getLine(), dirIsFile.getRange().getEnd().getCharacter(), fixedFilePath);
        CodeAction dirIsFileCodeAction = ca(dirIsFile, dirIsFileTextEdit);

        String fixedDirPath = "location=\"\\testDir.xml\\\"";
        TextEdit fileIsDirTextEdit = te(fileIsDir.getRange().getStart().getLine(), fileIsDir.getRange().getStart().getCharacter(),
                            fileIsDir.getRange().getEnd().getLine(), fileIsDir.getRange().getEnd().getCharacter(), fixedDirPath);
        CodeAction fileIsDirCodeAction = ca(fileIsDir, fileIsDirTextEdit);

        XMLAssert.testCodeActionsFor(serverXML, dirIsFile, dirIsFileCodeAction);

        XMLAssert.testCodeActionsFor(serverXML, fileIsDir, fileIsDirCodeAction);
    }

    @Test
    public void testConfigElementMissingFeatureManager() throws JAXBException {
        assertTrue(featureList.exists());
        FeatureService.getInstance().readFeaturesFromFeatureListFile(libWorkspace, featureList);

        String serverXml = "<server><ssl id=\"\"/></server>";
        // Temporarily disabling config element diagnostics if featureManager element is missing (until issue 230 is addressed)
        // Diagnostic config_for_missing_feature = new Diagnostic();
        // config_for_missing_feature.setRange(r(0, serverXml.indexOf("<ssl"), 0, serverXml.length()-"</server>".length()));
        // config_for_missing_feature.setCode(LibertyDiagnosticParticipant.MISSING_CONFIGURED_FEATURE_CODE);
        // config_for_missing_feature.setMessage(LibertyDiagnosticParticipant.MISSING_CONFIGURED_FEATURE_MESSAGE);

        // XMLAssert.testDiagnosticsFor(serverXml, null, null, serverXMLURI, config_for_missing_feature);
        XMLAssert.testDiagnosticsFor(serverXml, null, null, serverXMLURI); // expect no diagnostic for this scenario right now
    }

    @Test
    public void testConfigElementMissingFeatureUsingCachedFeaturelist() throws JAXBException, BadLocationException {
        LibertyWorkspace ws = libPM.getWorkspaceFolder(sampleserverXMLURI);
        ws.setFeatureListGraph(new FeatureListGraph()); // need to clear out the already loaded featureList from other test methods
        FeatureService.getInstance().getDefaultFeatureList();

        String correctFeature   = "        <feature>%s</feature>";
        String incorrectFeature = "        <feature>jaxrs-2.0</feature>";
        String configElement    = "    <springBootApplication location=\"\"/>";
        int diagnosticStart = configElement.indexOf("<");
        int diagnosticLength = configElement.trim().length();

        String serverXML = String.join(newLine,
                "<server description=\"Sample Liberty server\">",
                "    <featureManager>",
                    incorrectFeature,
                "    </featureManager>",
                    configElement,
                "</server>"
        );

        Diagnostic config_for_missing_feature = new Diagnostic();
        config_for_missing_feature.setRange(r(4, diagnosticStart, 4, diagnosticStart + diagnosticLength));
        config_for_missing_feature.setCode(LibertyDiagnosticParticipant.MISSING_CONFIGURED_FEATURE_CODE);
        config_for_missing_feature.setMessage(LibertyDiagnosticParticipant.MISSING_CONFIGURED_FEATURE_MESSAGE);

        XMLAssert.testDiagnosticsFor(serverXML, null, null, sampleserverXMLURI, config_for_missing_feature);

        // TODO: Add code to check the CodeActions also.
        config_for_missing_feature.setSource("springBootApplication");

        List<String> featuresToAdd = new ArrayList<String>();
        featuresToAdd.add("springBoot-1.5");
        featuresToAdd.add("springBoot-2.0");
        featuresToAdd.add("springBoot-3.0");
        Collections.sort(featuresToAdd);

        List<CodeAction> codeActions = new ArrayList<CodeAction>();
        for (String nextFeature: featuresToAdd) {
            String addFeature = System.lineSeparator()+String.format(correctFeature, nextFeature);
            TextEdit texted = te(2, 36, 2, 36, addFeature);
            CodeAction invalidCodeAction = ca(config_for_missing_feature, texted);

            TextDocumentEdit textDoc = tde(sampleserverXMLURI, 0, texted);
            WorkspaceEdit workspaceEdit = new WorkspaceEdit(Collections.singletonList(Either.forLeft(textDoc)));

            invalidCodeAction.setEdit(workspaceEdit);
            codeActions.add(invalidCodeAction);
        }

        XMLAssert.testCodeActionsFor(serverXML, sampleserverXMLURI, config_for_missing_feature, (String) null,
                codeActions.get(0), codeActions.get(1), codeActions.get(2));

    }


    @Test
    public void testConfigElementDirect() throws JAXBException {
        assertTrue(featureList.exists());
        FeatureService.getInstance().readFeaturesFromFeatureListFile(libWorkspace, featureList);

        String correctFeature   = "           <feature>Ssl-1.0</feature>";
        String incorrectFeature = "           <feature>jaxrs-2.0</feature>";
        String configElement    = "   <ssl id=\"\"/>";
        int diagnosticStart = configElement.indexOf("<");
        int diagnosticLength = configElement.trim().length();

        String serverXML1 = String.join(newLine,
                "<server description=\"Sample Liberty server\">",
                "   <featureManager>",
                        correctFeature,
                "   </featureManager>",
                    configElement,
                "</server>"
        );
        XMLAssert.testDiagnosticsFor(serverXML1, null, null, serverXMLURI);

        String serverXML2 = String.join(newLine,
                "<server description=\"Sample Liberty server\">",
                "   <featureManager>",
                        incorrectFeature,
                "   </featureManager>",
                    configElement,
                "</server>"
        );

        Diagnostic config_for_missing_feature = new Diagnostic();
        config_for_missing_feature.setRange(r(4, diagnosticStart, 4, diagnosticStart + diagnosticLength));
        config_for_missing_feature.setCode(LibertyDiagnosticParticipant.MISSING_CONFIGURED_FEATURE_CODE);
        config_for_missing_feature.setMessage(LibertyDiagnosticParticipant.MISSING_CONFIGURED_FEATURE_MESSAGE);

        XMLAssert.testDiagnosticsFor(serverXML2, null, null, serverXMLURI, config_for_missing_feature);
    }

    @Test
    public void testConfigElementTransitive() throws JAXBException {
        assertTrue(featureList.exists());
        FeatureService.getInstance().readFeaturesFromFeatureListFile(libWorkspace, featureList);
        String serverXML1 = String.join(newLine,
                "<server description=\"Sample Liberty server\">",
                "   <featureManager>",
                "       <feature>microProfile-5.0</feature>",
                "   </featureManager>",
                "   <ssl id=\"\"/>",
                "</server>"
        );
        XMLAssert.testDiagnosticsFor(serverXML1, null, null, serverXMLURI);
    }

    @Test
    public void testInvalidPlatformDiagnostic() throws BadLocationException {
        String serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "               <feature>batch-1.0</feature>", //
                "               <platform>jaX</platform>", //
                "               <platform>javaee-7.0</platform>", //
                "               <!-- <feature>comment</feature> -->", //
                "               <platform>javaee-7.0</platform>", //
                "               <platform>javaee-8.0</platform>", //
                "               <platform>jakartaee-9.1</platform>", //
                "               <platform>jakartaee-11.0</platform>", //
                "       </featureManager>", //
                "</server>" //
        );
        Diagnostic invalid1 = new Diagnostic();
        invalid1.setRange(r(3, 25, 3, 28));
        invalid1.setCode(LibertyDiagnosticParticipant.INCORRECT_PLATFORM_CODE);
        invalid1.setMessage("ERROR: The platform \"jaX\" does not exist.");

        Diagnostic invalid2 = new Diagnostic();
        invalid2.setRange(r(6, 25, 6, 35));
        invalid2.setMessage("ERROR: javaee-7.0 is already included.");


        Diagnostic invalid3 = new Diagnostic();
        invalid3.setRange(r(7, 25, 7, 35));
        invalid3.setMessage("ERROR: More than one version of platform javaee is included. Only one version of a platform may be specified.");

        Diagnostic invalid4 = new Diagnostic();
        invalid4.setRange(r(8, 25, 8, 38));
        invalid4.setMessage("ERROR: The following configured platform versions are in conflict [javaee-7.0, javaee-8.0, jakartaee-9.1]");

        Diagnostic invalid5 = new Diagnostic();
        invalid5.setRange(r(9, 25, 9, 39));
        invalid5.setCode(LibertyDiagnosticParticipant.INCORRECT_PLATFORM_CODE);
        invalid5.setMessage("ERROR: The platform \"jakartaee-11.0\" does not exist."); // beta platform should not be valid


        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI,
                invalid1, invalid2, invalid3, invalid4, invalid5);
    }

    @Test
    public void testInvalidPlatformForVersionlessFeatureDiagnostic() throws BadLocationException {
        String serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "               <feature>servlet</feature>", //
                "       </featureManager>", //
                "</server>" //
        );
        Diagnostic invalid1 = new Diagnostic();
        invalid1.setRange(r(2, 24, 2, 31));
        invalid1.setCode(LibertyDiagnosticParticipant.INCORRECT_FEATURE_CODE);
        invalid1.setMessage("ERROR: The servlet versionless feature cannot be resolved. Specify a platform or a feature with a version to enable resolution.");

        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI,
                invalid1);
        // same as example in open liberty pages. error should be thrown because there are multiple platforms in mpConfig-2.0
        serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "               <feature>mpConfig-2.0</feature>", //
                "               <feature>mpMetrics</feature>", //
                "       </featureManager>", //
                "</server>" //
        );
        invalid1 = new Diagnostic();
        invalid1.setRange(r(3, 24, 3, 33));
        invalid1.setCode(LibertyDiagnosticParticipant.INCORRECT_FEATURE_CODE);
        invalid1.setMessage("ERROR: The \"mpMetrics\" versionless feature cannot be resolved since there are more than one common platform. Specify a platform or a feature with a version to enable resolution.");

        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI,
                invalid1);

        serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "               <feature>servlet</feature>", //
                "               <feature>acmeCA-2.0</feature>", //
                "       </featureManager>", //
                "</server>" //
        );
        invalid1 = new Diagnostic();
        invalid1.setRange(r(2, 24, 2, 31));
        invalid1.setMessage("ERROR: \"servlet\" versionless feature cannot be resolved. The versioned features do not have a platform in common.");
        invalid1.setCode(LibertyDiagnosticParticipant.INCORRECT_FEATURE_CODE);

        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI,
                invalid1);

        serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "               <feature>servlet</feature>", //
                "               <feature>beanValidation-1.1</feature>", //
                "               <feature>jsonb-2.0</feature>", //
                "       </featureManager>", //
                "</server>" //
        );
        invalid1 = new Diagnostic();
        invalid1.setRange(r(2, 24, 2, 31));
        invalid1.setMessage("ERROR: \"servlet\" versionless feature cannot be resolved. The versioned features do not have a platform in common.");
        invalid1.setCode(LibertyDiagnosticParticipant.INCORRECT_FEATURE_CODE);

        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI,
                invalid1);
    }
    @Test
    public void testUnresolvedPlatformForVersionlessFeatureDiagnostic() throws BadLocationException {
        //jdbc-4.0 only supports javaee-6.0 but servlet only supports javaee-7.0, javaee-8.0,jakartaee-9.1 and jakartaee-10.0
        String serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "               <feature>servlet</feature>", //
                "               <feature>jdbc-4.0</feature>", //
                "       </featureManager>", //
                "</server>" //
        );
        Diagnostic invalid1 = new Diagnostic();
        invalid1.setRange(r(2, 24, 2, 31));
        invalid1.setCode(LibertyDiagnosticParticipant.INCORRECT_FEATURE_CODE);
        invalid1.setMessage("ERROR: The \"servlet\" versionless feature cannot be resolved. Specify a platform or a versioned feature from a supported platform to enable resolution.");

        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI,
                invalid1);

        serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "               <feature>jsp</feature>", //
                "               <platform>microProfile-1.2</platform>", //
                "       </featureManager>", //
                "</server>" //
        );

        invalid1 = new Diagnostic();
        invalid1.setRange(r(2, 24, 2, 27));
        invalid1.setCode(LibertyDiagnosticParticipant.INCORRECT_FEATURE_CODE);
        invalid1.setMessage("ERROR: The \"jsp\" versionless feature does not have a configured platform.");

        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI,
                invalid1);

        serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "               <feature>servlet</feature>", //
                "               <feature>jaxws-2.2</feature>", //
                "       </featureManager>", //
                "</server>" //
        );
        invalid1 = new Diagnostic();
        invalid1.setRange(r(2, 24, 2, 31));
        invalid1.setCode(LibertyDiagnosticParticipant.INCORRECT_FEATURE_CODE);
        invalid1.setMessage("ERROR: The \"servlet\" versionless feature cannot be resolved since there are more than one common platform. Specify a platform or a feature with a version to enable resolution.");

        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI,
                invalid1);

        //  mpConfig-3.0 and mpJwt-2.0 have a single common platform(microProfile-5.0) but servlet feature does not support that
       serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "               <feature>servlet</feature>", //
                "               <feature>mpConfig-3.0</feature>", //
                "               <feature>mpJwt-2.0</feature>", //
                "       </featureManager>", //
                "</server>" //
        );

        invalid1 = new Diagnostic();
        invalid1.setRange(r(2, 24, 2, 31));
        invalid1.setCode(LibertyDiagnosticParticipant.INCORRECT_FEATURE_CODE);
        invalid1.setMessage("ERROR: The \"servlet\" versionless feature cannot be resolved. Specify a platform or a versioned feature from a supported platform to enable resolution.");

        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI,
                invalid1);
    }

    @Test
    public void testValidPlatformDiagnostic() throws BadLocationException {

       String serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "               <feature>servlet</feature>", //
                "               <platform>jakartaee-9.1</platform>", //
                "       </featureManager>", //
                "</server>" //
        );
        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI);

        serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "               <feature>servlet</feature>", //
                "               <feature>jaxws-2.2</feature>", //
                "               <platform>javaee-7.0</platform>", //
                "       </featureManager>", //
                "</server>" //
        );

        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI);

        serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "               <feature>servlet</feature>", //
                "               <feature>mail-2.0</feature>", //
                "               <feature>jsonb-2.0</feature>", //
                "       </featureManager>", //
                "</server>" //
        );
        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI);

        serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "               <feature>mpMetrics</feature>", //
                "               <feature>mpHealth</feature>", //
                "               <platform>microProfile-5.0</platform>", //
                "       </featureManager>", //
                "</server>" //
        );

        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI);

        serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "               <feature>servlet</feature>", //
                "               <feature>jpa</feature>", //
                "               <feature>jaxrs</feature>", //
                "               <platform>jakartaee-9.1</platform>", //
                "       </featureManager>", //
                "</server>" //
        );

        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI);
    }

    @Test
    public void testConfigElementVersionLess() throws JAXBException {
        assertTrue(featureList.exists());
        FeatureService.getInstance().readFeaturesFromFeatureListFile(libWorkspace, featureList);
        String serverXML1 = String.join(newLine,
                "<server description=\"Sample Liberty server\">",
                "   <featureManager>",
                "       <feature>servlet</feature>",
                "       <platform>jakartaee-9.1</platform>",
                "   </featureManager>",
                "   <webApplication contextRoot=\"/app-name\" location=\"app-name.war\" />",
                "   <httpEndpoint id=\"defaultHttpEndpoint\" httpPort=\"9080\" httpsPort=\"9443\"/>",
                "   <ssl id=\"defaultSSLConfig\" trustDefaultCerts=\"true\" />",
                "</server>"
        );
        Diagnostic configForMissingFeature = new Diagnostic();
        configForMissingFeature.setRange(r(7, 3, 7, 57));
        configForMissingFeature.setCode(LibertyDiagnosticParticipant.MISSING_CONFIGURED_FEATURE_CODE);
        configForMissingFeature.setMessage(LibertyDiagnosticParticipant.MISSING_CONFIGURED_FEATURE_MESSAGE);
        XMLAssert.testDiagnosticsFor(serverXML1, null, null, serverXMLURI,configForMissingFeature);
    }

    @Test
    public void testUniquenessForNameChangedFeatureDiagnostic() throws BadLocationException {
        String serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "               <feature>jms-2.0</feature>", //
                "               <feature>messaging-3.0</feature>", //
                "       </featureManager>", //
                "</server>" //
        );
        Diagnostic invalid = new Diagnostic();
        invalid.setRange(r(3, 24, 3, 37));
        invalid.setMessage("ERROR: The messaging-3.0 feature cannot be configured with the jms-2.0 feature because they are two different versions of the same feature. The feature name changed from jms to messaging for Jakarta EE. Remove one of the features.");
        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI,
                invalid);

        serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "               <feature>messaging-3.0</feature>", //
                "               <feature>jms-2.0</feature>", //
                "       </featureManager>", //
                "</server>" //
        );
        invalid = new Diagnostic();
        invalid.setRange(r(3, 24, 3, 31));
        invalid.setMessage("ERROR: The jms-2.0 feature cannot be configured with the messaging-3.0 feature because they are two different versions of the same feature. The feature name changed from jms to messaging for Jakarta EE. Remove one of the features.");
        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI,
                invalid);

        serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "               <platform>javaee-7.0</platform>", //
                "               <feature>enterpriseBeans-4.0</feature>", //
                "               <feature>ejb-3.2</feature>", //
                "       </featureManager>", //

                "</server>" //
        );
        invalid = new Diagnostic();
        invalid.setRange(r(4, 24, 4, 31));
        invalid.setMessage("ERROR: The ejb-3.2 feature cannot be configured with the enterpriseBeans-4.0 feature because they are two different versions of the same feature. The feature name changed from ejb to enterpriseBeans for Jakarta EE. Remove one of the features.");
        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI,
                invalid);

        serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "               <platform>javaee-7.0</platform>", //
                "               <feature>enterpriseBeans</feature>", //
                "               <feature>ejb</feature>", //
                "       </featureManager>", //

                "</server>" //
        );
        invalid = new Diagnostic();
        invalid.setRange(r(4, 24, 4, 27));
        invalid.setMessage("ERROR: The ejb feature cannot be configured with the enterpriseBeans feature because they are two different versions of the same feature. The feature name changed from ejb to enterpriseBeans for Jakarta EE. Remove one of the features.");
        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI,
                invalid);
    }

    @Test
    public void testInvalidPlatformDiagnosticWithCodeCompletion() throws BadLocationException {

        String serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "               <platform>javaEE</platform>", //
                "       </featureManager>", //
                "</server>" //
        );
        Diagnostic invalid1 = new Diagnostic();
        invalid1.setRange(r(2, 25, 2, 31));
        invalid1.setCode(LibertyDiagnosticParticipant.INCORRECT_PLATFORM_CODE);
        invalid1.setMessage("ERROR: The platform \"javaEE\" does not exist.");

        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI,
                invalid1);
        //expecting code action to show all javaee platforms ignoring input case
        List<String> featuresStartWithJavaEE = new ArrayList<>();
        featuresStartWithJavaEE.add("javaee-6.0");
        featuresStartWithJavaEE.add("javaee-7.0");
        featuresStartWithJavaEE.add("javaee-8.0");
        Collections.sort(featuresStartWithJavaEE);

        List<CodeAction> codeActions = new ArrayList<>();
        for (String nextFeature : featuresStartWithJavaEE) {
            TextEdit texted = te(invalid1.getRange().getStart().getLine(), invalid1.getRange().getStart().getCharacter(),
                    invalid1.getRange().getEnd().getLine(), invalid1.getRange().getEnd().getCharacter(), nextFeature);
            CodeAction invalidCodeAction = ca(invalid1, texted);

            codeActions.add(invalidCodeAction);
        }

        XMLAssert.testCodeActionsFor(serverXML, invalid1, codeActions.get(0), codeActions.get(1),
                codeActions.get(2));

        serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "               <feature>batch-1.0</feature>", //
                "               <platform>ja</platform>", //
                "               <platform>javaee-7.0</platform>", //
                "       </featureManager>", //
                "</server>" //
        );
        invalid1 = new Diagnostic();
        invalid1.setRange(r(3, 25, 3, 27));
        invalid1.setCode(LibertyDiagnosticParticipant.INCORRECT_PLATFORM_CODE);
        invalid1.setMessage("ERROR: The platform \"ja\" does not exist.");

        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI,
                invalid1);
        //  expecting code action to show nothing since
        //      1. javaee is already included
        //      2. jakartaee is conflicting with javaee platforms
        //      3. user has entered value "ja", no platform is available which matches this input

        XMLAssert.testCodeActionsFor(serverXML, invalid1);
        serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "               <feature>batch-1.0</feature>", //
                "               <platform>microProfile-5</platform>", //
                "               <platform>javaee-7.0</platform>", //
                "       </featureManager>", //
                "</server>" //
        );
        invalid1 = new Diagnostic();
        invalid1.setRange(r(3, 25, 3, 39));
        invalid1.setCode(LibertyDiagnosticParticipant.INCORRECT_PLATFORM_CODE);
        invalid1.setMessage("ERROR: The platform \"microProfile-5\" does not exist.");

        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI,
                invalid1);

        //  expecting code action to show only microprofile platforms since
        //      1. user has entered "microProfile-5"
        List<String> microProfilePlatforms = new ArrayList<>();
        microProfilePlatforms.add("microProfile-1.0");
        microProfilePlatforms.add("microProfile-1.2");
        microProfilePlatforms.add("microProfile-1.3");
        microProfilePlatforms.add("microProfile-1.4");
        microProfilePlatforms.add("microProfile-2.0");
        microProfilePlatforms.add("microProfile-2.1");
        microProfilePlatforms.add("microProfile-2.2");
        microProfilePlatforms.add("microProfile-3.0");
        microProfilePlatforms.add("microProfile-3.2");
        microProfilePlatforms.add("microProfile-3.3");
        microProfilePlatforms.add("microProfile-4.0");
        microProfilePlatforms.add("microProfile-4.1");
        microProfilePlatforms.add("microProfile-5.0");
        microProfilePlatforms.add("microProfile-6.0");
        microProfilePlatforms.add("microProfile-6.1");
        microProfilePlatforms.add("microProfile-7.0");
        Collections.sort(microProfilePlatforms);

        codeActions = new ArrayList<>();
        for (String nextFeature : microProfilePlatforms) {
            TextEdit texted = te(invalid1.getRange().getStart().getLine(), invalid1.getRange().getStart().getCharacter(),
                    invalid1.getRange().getEnd().getLine(), invalid1.getRange().getEnd().getCharacter(), nextFeature);
            CodeAction invalidCodeAction = ca(invalid1, texted);

            codeActions.add(invalidCodeAction);
        }

        XMLAssert.testCodeActionsFor(serverXML, invalid1, codeActions.get(0),
                codeActions.get(1), codeActions.get(2),
                codeActions.get(3), codeActions.get(4),
                codeActions.get(5), codeActions.get(6),
                codeActions.get(7), codeActions.get(8),
                codeActions.get(9), codeActions.get(10),
                codeActions.get(11), codeActions.get(12),
                codeActions.get(13), codeActions.get(14),
                codeActions.get(15));
    }

    @Test
    public void testInvalidVariableDiagnostic() {
        String serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "                <platform>javaee-6.0</platform>", //
                "                <feature>acmeCA-2.0</feature>", //
                "       </featureManager>", //
                " <httpEndpoint host=\"*\" httpPort=\"${default.http.port}\"\n",//
                "                  httpsPort=\"${default.https.port}\" id=\"defaultHttpEndpoint\"/>",//
                "</server>" //
        );
        Map<String, String> propsMap = new HashMap<>();
        propsMap.put("default.http.port", "9080");
        Properties props = new Properties();
        props.putAll(propsMap);
        when(settingsService.getVariablesForServerXml(any())).thenReturn(props);
        Diagnostic dup1 = new Diagnostic();
        dup1.setRange(r(7, 29, 7, 50));
        dup1.setCode(LibertyDiagnosticParticipant.INCORRECT_VARIABLE_CODE);
        dup1.setSource("liberty-lemminx");
        dup1.setSeverity(DiagnosticSeverity.Error);
        dup1.setMessage("ERROR: The variable \"default.https.port\" does not exist.");
        dup1.setData("default.https.port");

        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI, false, dup1);
    }

    @Test
    public void testInvalidVariableDiagnosticWithCodeAction() throws BadLocationException {
        String serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "                <platform>javaee-6.0</platform>", //
                "                <feature>acmeCA-2.0</feature>", //
                "       </featureManager>", //
                " <httpEndpoint host=\"*\" httpPort=\"${default.http.port}\"\n",//
                "                  httpsPort=\"${default.https}\" id=\"defaultHttpEndpoint\"/>",//
                "</server>" //
        );
        Map<String, String> propsMap = new HashMap<>();
        propsMap.put("default.http.port", "9080");
        propsMap.put("default.https.port", "9443");
        Properties props = new Properties();
        props.putAll(propsMap);
        when(settingsService.getVariablesForServerXml(any())).thenReturn(props);
        Diagnostic invalid1 = new Diagnostic();
        invalid1.setRange(r(7, 29, 7, 45));
        invalid1.setCode(LibertyDiagnosticParticipant.INCORRECT_VARIABLE_CODE);
        invalid1.setMessage("ERROR: The variable \"default.https\" does not exist.");
        invalid1.setData("default.https");
        invalid1.setSource("liberty-lemminx");
        invalid1.setSeverity(DiagnosticSeverity.Error);

        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI, false, invalid1);

        //  expecting code action to show only default.https.port
        //      1. user has entered "default.https"
        List<String> variables = new ArrayList<>();
        variables.add("default.https.port");


        List<CodeAction> codeActions = new ArrayList<>();
        for (String nextVar : variables) {
            String variableInDoc = String.format("${%s}", nextVar);
            TextEdit texted = te(invalid1.getRange().getStart().getLine(), invalid1.getRange().getStart().getCharacter(),
                    invalid1.getRange().getEnd().getLine(), invalid1.getRange().getEnd().getCharacter(), variableInDoc);
            CodeAction invalidCodeAction = ca(invalid1, texted);
            codeActions.add(invalidCodeAction);
            invalidCodeAction.getEdit()
                    .getDocumentChanges()
                    .get(0).getLeft().getTextDocument()
                    .setUri(serverXMLURI);
        }
        XMLAssert.testCodeActionsFor(serverXML, serverXMLURI, invalid1, codeActions.get(0));
    }


    @Test
    public void testNoVariableMappedDiagnostic() throws IOException {
        String serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "                <platform>javaee-6.0</platform>", //
                "                <feature>acmeCA-2.0</feature>", //
                "       </featureManager>", //
                " <httpEndpoint host=\"*\" httpPort=\"${default.http.port}\"\n",//
                "                  httpsPort=\"${default.https.port}\" id=\"defaultHttpEndpoint\"/>",//
                "</server>" //
        );
        when(settingsService.getVariablesForServerXml(any())).thenReturn(new Properties());
        Diagnostic dup1 = new Diagnostic();
        dup1.setRange(r(0, 0, 0, 43));
        String message="WARNING: Variable resolution is not available for workspace %s. Please start the Liberty server for the workspace to enable variable resolution.";
        dup1.setMessage(message.formatted(srcResourcesDir.toURI().getPath()));

        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI,
                dup1);
    }

    @Test
    public void testInvalidVariableRepeatedDiagnostic() {
        String serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "                <platform>javaee-6.0</platform>", //
                "                <feature>acmeCA-2.0</feature>", //
                "       </featureManager>", //
                " <httpEndpoint host=\"*\" httpPort=\"${default.https.port}\"\n",//
                "                  httpsPort=\"${default.https.port}\" id=\"defaultHttpEndpoint\"/>",//
                "</server>" //
        );
        Map<String, String> propsMap = new HashMap<>();
        propsMap.put("default.http.port", "9080");
        Properties props = new Properties();
        props.putAll(propsMap);
        when(settingsService.getVariablesForServerXml(any())).thenReturn(props);
        Diagnostic dup1 = new Diagnostic();
        dup1.setRange(r(5, 34, 5, 55));
        dup1.setCode(LibertyDiagnosticParticipant.INCORRECT_VARIABLE_CODE);
        dup1.setSource("liberty-lemminx");
        dup1.setSeverity(DiagnosticSeverity.Error);
        dup1.setMessage("ERROR: The variable \"default.https.port\" does not exist.");
        dup1.setData("default.https.port");

        Diagnostic dup2 = new Diagnostic();
        dup2.setRange(r(7, 29, 7, 50));
        dup2.setCode(LibertyDiagnosticParticipant.INCORRECT_VARIABLE_CODE);
        dup2.setSource("liberty-lemminx");
        dup2.setSeverity(DiagnosticSeverity.Error);
        dup2.setMessage("ERROR: The variable \"default.https.port\" does not exist.");
        dup2.setData("default.https.port");

        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI, false, dup1, dup2);
    }

    @Test
    public void testMultipleVariablesInSameAttributeDiagnostic() {
        String serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "                <platform>javaee-6.0</platform>", //
                "                <feature>acmeCA-2.0</feature>", //
                "       </featureManager>", //
                " <httpEndpoint host=\"*\" httpPort=\"${default.https.port}\"\n",//
                "                  httpsPort=\"${default.https.port}\" id=\"defaultHttpEndpoint\"/>",//
                " <webApplication contextRoot=\"/app-name\" location=\"${testVar2}/${testVar1}\" />",
                "</server>" //
        );
        Map<String, String> propsMap = new HashMap<>();
        propsMap.put("default.http.port", "9080");
        propsMap.put("default.https.port", "9443");
        propsMap.put("testVar2", "apps");
        Properties props = new Properties();
        props.putAll(propsMap);
        when(settingsService.getVariablesForServerXml(any())).thenReturn(props);
        Diagnostic dup1 = new Diagnostic();
        dup1.setRange(r(8, 63, 8, 74));
        dup1.setCode(LibertyDiagnosticParticipant.INCORRECT_VARIABLE_CODE);
        dup1.setSource("liberty-lemminx");
        dup1.setSeverity(DiagnosticSeverity.Error);
        dup1.setMessage("ERROR: The variable \"testVar1\" does not exist.");
        dup1.setData("testVar1");
        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXMLURI, false, dup1);
    }

    @Test
    public void testConfigElementSameNameAsVersionlessFeatureNoDiagnostics() throws BadLocationException {
        String configElement = "<mpMetrics authentication=\"false\"></mpMetrics>";

        String serverXML = String.join(newLine,
                "<server description=\"Sample Liberty server\">",
                "    <featureManager>",
                "        <feature>mpMetrics</feature>",
                "        <platform>microProfile-2.2</platform>",
                "    </featureManager>",
                configElement,
                "</server>"
        );
        // no diagnostics expected, as we have the correct feature
        XMLAssert.testDiagnosticsFor(serverXML, null, null, sampleserverXMLURI);
    }

    @Test
    public void testConfigElementSameNameAsVersionlessFeatureWithDiagnosticsAndCodeAction() throws BadLocationException {
        String configElement = "<mpMetrics authentication=\"false\"></mpMetrics>";
        String serverXML = String.join(newLine,
                "<server description=\"Sample Liberty server\">",
                "    <featureManager>",
                "        <platform>microProfile-2.2</platform>",
                "    </featureManager>",
                configElement,
                "</server>"
        );
        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setRange(r(4, 0, 4, 46));
        diagnostic.setCode(LibertyDiagnosticParticipant.MISSING_CONFIGURED_FEATURE_CODE);
        diagnostic.setMessage(LibertyDiagnosticParticipant.MISSING_CONFIGURED_FEATURE_MESSAGE);

        XMLAssert.testDiagnosticsFor(serverXML, null, null, sampleserverXMLURI,diagnostic);
        diagnostic.setSource("mpMetrics");

        List<String> featuresToAdd = new ArrayList<String>();
        featuresToAdd.add("mpMetrics-1.0");
        featuresToAdd.add("mpMetrics-1.1");
        featuresToAdd.add("mpMetrics-2.0");
        featuresToAdd.add("mpMetrics-2.2");
        featuresToAdd.add("mpMetrics-2.3");
        featuresToAdd.add("mpMetrics-3.0");
        featuresToAdd.add("mpMetrics-4.0");
        featuresToAdd.add("mpMetrics-5.0");
        featuresToAdd.add("mpMetrics-5.1");

        Collections.sort(featuresToAdd);

        List<CodeAction> codeActions = new ArrayList<>();
        for (String nextFeature: featuresToAdd) {
            String addFeature = String.format("%s<feature>%s</feature>",System.lineSeparator(),nextFeature);
            TextEdit texted = te(2, 45, 2, 45, addFeature);
            CodeAction invalidCodeAction = ca(diagnostic, texted);

            TextDocumentEdit textDoc = tde(sampleserverXMLURI, 0, texted);
            WorkspaceEdit workspaceEdit = new WorkspaceEdit(Collections.singletonList(Either.forLeft(textDoc)));

            invalidCodeAction.setEdit(workspaceEdit);
            codeActions.add(invalidCodeAction);
        }

        // diagnostic with code action expected
        XMLAssert.testCodeActionsFor(serverXML, sampleserverXMLURI, diagnostic, (String) null,
                codeActions.get(0), codeActions.get(1), codeActions.get(2),
                codeActions.get(3), codeActions.get(4), codeActions.get(5),
                codeActions.get(6), codeActions.get(7), codeActions.get(8)
        );
    }
}