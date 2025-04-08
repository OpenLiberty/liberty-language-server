package io.openliberty.tools.test;

import java.util.List;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import io.openliberty.tools.langserver.lemminx.services.LibertyWorkspace;
import io.openliberty.tools.langserver.lemminx.services.SettingsService;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import io.openliberty.tools.langserver.lemminx.services.LibertyProjectsManager;
import io.openliberty.tools.langserver.lemminx.util.LibertyUtils;

import static org.eclipse.lemminx.XMLAssert.*;

public class LibertyWorkspaceIT {
    static String newLine = System.lineSeparator();

    @AfterAll
    public static void tearDown() {
        LibertyProjectsManager.getInstance().cleanInstance();
        assert(LibertyProjectsManager.getInstance().getLibertyWorkspaceFolders().isEmpty());
    }

    @Test
    public void testGetVariables() throws BadLocationException {
        File testFolder = new File(System.getProperty("user.dir"));
        File serverXmlFile = new File(testFolder, "src/main/liberty/config/server.xml");

        //Configure Liberty workspace for testing
        WorkspaceFolder testWorkspace = new WorkspaceFolder(testFolder.toURI().toString());
        List<WorkspaceFolder> testWorkspaceFolders = new ArrayList<WorkspaceFolder>();
        testWorkspaceFolders.add(testWorkspace);
        LibertyProjectsManager.getInstance().setWorkspaceFolders(testWorkspaceFolders);

        // Assert that the liberty plugin config is copied to the server
        LibertyWorkspace workspace = LibertyProjectsManager.getInstance().getWorkspaceFolder(testWorkspace.getUri());
        assert SettingsService.getInstance().isLibertyPluginConfigAvailableInServer(workspace);

        String serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "                <platform>javaee-6.0</platform>", //
                "                <feature>acmeCA-2.0</feature>", //
                "       </featureManager>", //
                " <httpEndpoint host=\"*\" httpPort=\"${default|\"\n",//
                "                  httpsPort=\"${default.https.port}\" id=\"defaultHttpEndpoint\"/>",//
                "</server>" //
        );

        CompletionItem httpCompletion = c("${default.http.port}", "${default.http.port}");
        CompletionItem httpsCompletion = c("${default.https.port}", "${default.https.port}");
        final int TOTAL_ITEMS = 2; // total number of available completion items containing "default"

        XMLAssert.testCompletionFor(serverXML, null, serverXmlFile.toURI().toString(), TOTAL_ITEMS, httpCompletion,
                httpsCompletion);

        // tests for checking completion of liberty pre defined variables
        // verifying variables starting with server. and wlp.
        serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "                <platform>javaee-6.0</platform>", //
                "                <feature>acmeCA-2.0</feature>", //
                "       </featureManager>", //
                " <webApplication contextRoot=\"/app-name\" location=\"${server.|}\" />",//
                "</server>" //
        );

        CompletionItem serverConfigDirCompletion= c("${server.config.dir}", "${server.config.dir}");
        CompletionItem serverOutputDirCompletion = c("${server.output.dir}", "${server.output.dir}");

        XMLAssert.testCompletionFor(serverXML, null, serverXmlFile.toURI().toString(), TOTAL_ITEMS, serverConfigDirCompletion,
                serverOutputDirCompletion);

        serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "                <platform>javaee-6.0</platform>", //
                "                <feature>acmeCA-2.0</feature>", //
                "       </featureManager>", //
                " <webApplication contextRoot=\"/app-name\" location=\"${wlp.|}\" />",//
                "</server>" //
        );

        CompletionItem wlpInstallDirCompletion = c("${wlp.install.dir}", "${wlp.install.dir}");
        CompletionItem wlpUsrDirCompletion = c("${wlp.user.dir}", "${wlp.user.dir}");

        XMLAssert.testCompletionFor(serverXML, null, serverXmlFile.toURI().toString(), TOTAL_ITEMS, wlpInstallDirCompletion,
                wlpUsrDirCompletion);

    }

    @Test
    public void testVariableHover() throws BadLocationException {

        File testFolder = new File(System.getProperty("user.dir"));
        File serverXmlFile = new File(testFolder, "src/main/liberty/config/server.xml");

        //Configure Liberty workspace for testing
        WorkspaceFolder testWorkspace = new WorkspaceFolder(testFolder.toURI().toString());
        List<WorkspaceFolder> testWorkspaceFolders = new ArrayList<WorkspaceFolder>();
        testWorkspaceFolders.add(testWorkspace);
        LibertyProjectsManager.getInstance().setWorkspaceFolders(testWorkspaceFolders);

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

        XMLAssert.assertHover(serverXML, serverXmlFile.toURI().toString(),
                "default.http.port = 9080",
                r(5, 33, 5, 55));
    }

    @Test
    public void testInvalidVariableDiagnostic() {

        File testFolder = new File(System.getProperty("user.dir"));
        File serverXmlFile = new File(testFolder, "src/main/liberty/config/server.xml");

        //Configure Liberty workspace for testing
        WorkspaceFolder testWorkspace = new WorkspaceFolder(testFolder.toURI().toString());
        List<WorkspaceFolder> testWorkspaceFolders = new ArrayList<WorkspaceFolder>();
        testWorkspaceFolders.add(testWorkspace);
        LibertyProjectsManager.getInstance().setWorkspaceFolders(testWorkspaceFolders);

        String serverXML = String.join(newLine, //
                "<server description=\"Sample Liberty server\">", //
                "       <featureManager>", //
                "                <platform>javaee-6.0</platform>", //
                "                <feature>acmeCA-2.0</feature>", //
                "       </featureManager>", //
                " <httpEndpoint host=\"*\" httpPort=\"${default.httpsl.port}\"\n",//
                "                  httpsPort=\"${default.httpsj.port}\" id=\"defaultHttpEndpoint\"/>",//
                "</server>" //
        );

        Diagnostic dup1 = new Diagnostic();
        dup1.setRange(r(5, 34, 5, 56));
        dup1.setCode("incorrect_variable");
        dup1.setSource("liberty-lemminx");
        dup1.setSeverity(DiagnosticSeverity.Error);
        dup1.setMessage("ERROR: The variable \"default.httpsl.port\" does not exist.");
        dup1.setData("default.httpsl.port");

        Diagnostic dup2 = new Diagnostic();
        dup2.setRange(r(7, 29, 7, 51));
        dup2.setCode("incorrect_variable");
        dup2.setSource("liberty-lemminx");
        dup2.setSeverity(DiagnosticSeverity.Error);
        dup2.setMessage("ERROR: The variable \"default.httpsj.port\" does not exist.");
        dup2.setData("default.httpsj.port");

        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXmlFile.toURI().toString(), false, dup1, dup2);
    }

    @Test
    public void testInvalidVariableDiagnosticWithCodeAction() throws IOException, BadLocationException {

        File testFolder = new File(System.getProperty("user.dir"));
        File serverXmlFile = new File(testFolder, "src/main/liberty/config/server.xml");

        //Configure Liberty workspace for testing
        WorkspaceFolder testWorkspace = new WorkspaceFolder(testFolder.toURI().toString());
        List<WorkspaceFolder> testWorkspaceFolders = new ArrayList<WorkspaceFolder>();
        testWorkspaceFolders.add(testWorkspace);
        LibertyProjectsManager.getInstance().setWorkspaceFolders(testWorkspaceFolders);
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

        Diagnostic invalid1 = new Diagnostic();
        invalid1.setRange(r(7, 29, 7, 45));
        invalid1.setCode("incorrect_variable");
        invalid1.setMessage("ERROR: The variable \"default.https\" does not exist.");
        invalid1.setData("default.https");
        invalid1.setSource("liberty-lemminx");
        invalid1.setSeverity(DiagnosticSeverity.Error);

        XMLAssert.testDiagnosticsFor(serverXML, null, null, serverXmlFile.toURI().toString(), false, invalid1);

        //  expecting code action to show only default.https.port
        //      1. user has entered "default.https"
        List<String> variables = new ArrayList<>();
        variables.add("default.https.port");
        List<CodeAction> codeActions = new ArrayList<>();
        TextEdit texted;
        CodeAction invalidCodeAction;
        for (String nextVar : variables) {
            String variableInDoc = String.format("${%s}", nextVar);
            texted = te(invalid1.getRange().getStart().getLine(), invalid1.getRange().getStart().getCharacter(),
                    invalid1.getRange().getEnd().getLine(), invalid1.getRange().getEnd().getCharacter(), variableInDoc);
            invalidCodeAction = ca(invalid1, texted);
            codeActions.add(invalidCodeAction);
            invalidCodeAction.getEdit()
                    .getDocumentChanges()
                    .get(0).getLeft().getTextDocument()
                    .setUri(serverXmlFile.toURI().toString());
        }
        texted = te(7, 0,
                7, 0, String.format("    <variable name=\"%s\" value=\"\"/> %s","default.https",System.lineSeparator()));
        invalidCodeAction = ca(invalid1, texted);
        invalidCodeAction.getEdit()
                .getDocumentChanges()
                .get(0).getLeft().getTextDocument()
                .setUri(serverXmlFile.toURI().toString());
        codeActions.add(invalidCodeAction);
        XMLAssert.testCodeActionsFor(serverXML, serverXmlFile.toURI().toString(), invalid1, codeActions.get(0), codeActions.get(1));
    }
}
