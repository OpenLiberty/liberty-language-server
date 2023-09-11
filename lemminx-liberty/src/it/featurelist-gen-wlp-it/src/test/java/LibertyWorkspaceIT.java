package io.openliberty.tools.test;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.lang.StringBuilder;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
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
    public void testWorkspace() throws BadLocationException, IOException, URISyntaxException {
        File testFolder = new File(System.getProperty("user.dir"));
        File serverXmlFile = new File(testFolder, "src/main/liberty/config/server.xml");

        //Configure Liberty workspace for testing
        WorkspaceFolder testWorkspace = new WorkspaceFolder(testFolder.toURI().toString());
        List<WorkspaceFolder> testWorkspaceFolders = new ArrayList<WorkspaceFolder>();
        testWorkspaceFolders.add(testWorkspace);
        LibertyProjectsManager.getInstance().setWorkspaceFolders(testWorkspaceFolders);

        String schemaFileName = "ol-22.0.0.13-beta.xsd";
        File schemaFile = new File(LibertyUtils.getTempDir(LibertyProjectsManager.getInstance().getWorkspaceFolder(serverXmlFile.toURI().toString())), schemaFileName);
        String serverGenXSDURI = schemaFile.toPath().toUri().toString().replace("///", "/");

        String serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <feature|Manager>", //
                        "               <feature>jaxrs-2.1</feature>", //
                        "       </featureManager>", //
                        "</server>" //
        );

        XMLAssert.assertHover(serverXML, serverXmlFile.toURI().toString(), "Defines how the server loads features." + //
                        System.lineSeparator() + System.lineSeparator() + //
                        "Source: [" + schemaFileName + "](" + serverGenXSDURI + ")", //
                        r(1, 8, 1, 22));
    }

    @Test
    public void testGetFeatures() throws BadLocationException {
        File testFolder = new File(System.getProperty("user.dir"));
        File serverXmlFile = new File(testFolder, "src/main/liberty/config/server.xml");

        //Configure Liberty workspace for testing
        WorkspaceFolder testWorkspace = new WorkspaceFolder(testFolder.toURI().toString());
        List<WorkspaceFolder> testWorkspaceFolders = new ArrayList<WorkspaceFolder>();
        testWorkspaceFolders.add(testWorkspace);
        LibertyProjectsManager.getInstance().setWorkspaceFolders(testWorkspaceFolders);

        String featureListName = "featurelist-ol-22.0.0.13-beta.xml";
        File featurelistFile = new File(LibertyUtils.getTempDir(LibertyProjectsManager.getInstance().getWorkspaceFolder(serverXmlFile.toURI().toString())), featureListName);

                String serverXML = String.join(newLine, //
                                "<server description=\"Sample Liberty server\">", //
                                "       <featureManager>", //
                                "               <feature>|</feature>", //
                                "               <feature>servlet-3.1</feature>", //
                                "       </featureManager>", //
                                "</server>" //
                );

        CompletionItem batchCompletion = c("batch-1.0", "batch-1.0");

        // this is using a beta runtime which does not have any features.json in Maven Central
        // this causes the featurelist xml file to get generated in the .libertyls folder
        final int TOTAL_ITEMS = 264; // total number of available completion items

        XMLAssert.testCompletionFor(serverXML, null, serverXmlFile.toURI().toString(), TOTAL_ITEMS, batchCompletion);
                
        String serverXML2 = String.join(newLine, //
                                "<server description=\"Sample Liberty server\">", //
                                "       <featureManager>", //
                                "               <feature>cdi-5.0|</feature>", //
                                "               <feature>servlet-3.1</feature>", //
                                "       </featureManager>", //
                                "</server>" //
                );
        CompletionItem cdiCompletion1 = c("cdi-1.2", "cdi-1.2");
        CompletionItem cdiCompletion2 = c("cdi-2.0", "cdi-2.0");
        CompletionItem cdiCompletion3 = c("cdi-3.0", "cdi-3.0");
        CompletionItem cdiCompletion4 = c("cdi-4.0", "cdi-4.0");

        Set<String> cdiFeatures = new HashSet<String> ();
        cdiFeatures.add("cdi-1.2");
        cdiFeatures.add("cdi-2.0");
        cdiFeatures.add("cdi-3.0");
        cdiFeatures.add("cdi-4.0");

        // changed to only return features that contain the passed in partial feature name (Note: if a version was listed after the hyphen, it gets stripped off in order to match all available versions of a feature)
        // - includes the four cdi features and two random completion items with labels "<![CDATA[" and "<!--"
        CompletionList completionList = XMLAssert.testCompletionFor(serverXML2, null, serverXmlFile.toURI().toString(), 6, cdiCompletion1, cdiCompletion2, cdiCompletion3, cdiCompletion4);

        Set<String> foundLabels = new HashSet<String> ();
        Set<String> unexpectedLabels = new HashSet<String> ();
        StringBuilder sbGood = new StringBuilder();
        StringBuilder sbBad = new StringBuilder();

        for (CompletionItem nextItem : completionList.getItems()) {
            if (!cdiFeatures.contains(nextItem.getLabel())) {
                unexpectedLabels.add(nextItem.getLabel());
                sbBad.append(nextItem.getLabel()+", ");
            } else {
                foundLabels.add(nextItem.getLabel());
                sbGood.append(nextItem.getLabel());
            }
        }

        org.junit.jupiter.api.Assertions.assertTrue(foundLabels.size() == 4, "Did not find all 4 expected labels: "+sbGood.toString());
        //org.junit.jupiter.api.Assertions.assertTrue(unexpectedLabels.size() == 0, "Found unexpected completion items with labels "+sbBad.toString());

        org.junit.jupiter.api.Assertions.assertTrue(featurelistFile.exists(), "Did not find generated featurelist file: "+featureListName);

    }
}
