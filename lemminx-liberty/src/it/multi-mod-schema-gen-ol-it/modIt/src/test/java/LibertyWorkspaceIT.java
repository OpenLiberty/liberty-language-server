package io.openliberty.tools.test;

import static org.eclipse.lemminx.XMLAssert.r;

import java.util.List;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lsp4j.CompletionItem;

import org.eclipse.lsp4j.WorkspaceFolder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import io.openliberty.tools.langserver.lemminx.services.LibertyProjectsManager;
import io.openliberty.tools.langserver.lemminx.services.LibertyWorkspace;
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
        try {
            File testFolder = new File(System.getProperty("user.dir"));

            File parentModuleFolder = testFolder.getParentFile();
            String parentFolderXSDURI = parentModuleFolder.toPath().toUri().toString().replace("///", "/");

            // Both sub-modules install the openliberty-kernel.
            // modA installs the jaxrs-2.1 feature.
            // modB installs the jaxrs-2.1, grpc-1.0, and grpcClient-1.0 features.
            File modAFolder = new File(parentModuleFolder, "modA");
            File modBFolder = new File(parentModuleFolder, "modB");
       
            File serverModAXmlFile = new File(modAFolder, "src/main/liberty/config/server.xml");
            String serverModAGenXSDURI = serverModAXmlFile.toPath().toUri().toString().replace("///", "/");

            File serverModBXmlFile = new File(modBFolder, "src/main/liberty/config/server.xml");
            String serverModBGenXSDURI = serverModBXmlFile.toPath().toUri().toString().replace("///", "/");

            //Configure Liberty workspace for testing - use parent module folder on purpose for multi-mod scenario
            WorkspaceFolder testWorkspace = new WorkspaceFolder(parentModuleFolder.toURI().toString());
            List<WorkspaceFolder> testWorkspaceFolders = new ArrayList<WorkspaceFolder>();
            testWorkspaceFolders.add(testWorkspace);
            LibertyProjectsManager.getInstance().setWorkspaceFolders(testWorkspaceFolders);

            String schemaFileName = "ol-22.0.0.12.xsd";

            LibertyWorkspace libWorkspaceA = LibertyProjectsManager.getInstance().getWorkspaceFolder(serverModAGenXSDURI);
            LibertyWorkspace libWorkspaceB = LibertyProjectsManager.getInstance().getWorkspaceFolder(serverModBGenXSDURI);

            org.junit.jupiter.api.Assertions.assertFalse(libWorkspaceA.getWorkspaceString().equals(libWorkspaceB.getWorkspaceString()), "Same workspace was returned for both sub-modules: "+ libWorkspaceA.getWorkspaceString());
    
            File schemaFileA = new File(LibertyUtils.getTempDir(libWorkspaceA), schemaFileName);
            String serverAGenXSDURI = schemaFileA.toPath().toUri().toString().replace("///", "/");
            org.junit.jupiter.api.Assertions.assertTrue(serverAGenXSDURI.contains("modA"), "Wrong schema file location was returned for modA: "+ serverAGenXSDURI);

            String serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <feature|Manager>", //
                        "               <feature>jaxrs-2.1</feature>", //
                        "       </featureManager>", //
                        "</server>" //
            );

            XMLAssert.assertHover(serverXML, serverModAXmlFile.toURI().toString(), "Defines how the server loads features." + //
                        System.lineSeparator() + System.lineSeparator() + //
                        "Source: [" + schemaFileName + "](" + serverAGenXSDURI + ")", //
                        r(1, 8, 1, 22));

            File schemaFileB = new File(LibertyUtils.getTempDir(libWorkspaceB), schemaFileName);
            String serverBGenXSDURI = schemaFileB.toPath().toUri().toString().replace("///", "/");
            org.junit.jupiter.api.Assertions.assertTrue(serverBGenXSDURI.contains("modB"), "Wrong schema file location was returned for modB: "+ serverBGenXSDURI);

            XMLAssert.assertHover(serverXML, serverModBXmlFile.toURI().toString(), "Defines how the server loads features." + //
                        System.lineSeparator() + System.lineSeparator() + //
                        "Source: [" + schemaFileName + "](" + serverBGenXSDURI + ")", //
                        r(1, 8, 1, 22));

            serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <featureManager>", //
                        "               <feature>jaxrs-2.1</feature>", //
                        "       </featureManager>", //
                        "       <grpcCli|ent></grpcClient>", //
                        "</server>" //
            );

            // The grpcClient config element should be recognized for modB since that feature is installed.
            XMLAssert.assertHover(serverXML, serverModBXmlFile.toURI().toString(), "Configuration properties to be applied to gRPC targets that match the specified URI." + //
                        System.lineSeparator() + System.lineSeparator() + //
                        "Source: [" + schemaFileName + "](" + serverBGenXSDURI + ")", //
                        r(4, 8, 4, 18));

            // The grpcClient config element should not be recognized for modA since that feature is not installed.
            XMLAssert.assertHover(serverXML, serverModAXmlFile.toURI().toString(), null, //
                        r(4, 8, 4, 18));

       } catch (Exception e) {
           e.printStackTrace();
           org.junit.jupiter.api.Assertions.fail("Test FAILED...received unexpected exception.");
       }
    }

    @Test
    public void testGetFeatures() throws BadLocationException {
        try {
            File testFolder = new File(System.getProperty("user.dir"));

            File parentModuleFolder = testFolder.getParentFile();
            String parentFolderXSDURI = parentModuleFolder.toPath().toUri().toString().replace("///", "/");

            File modAFolder = new File(parentModuleFolder, "modA");
            File modBFolder = new File(parentModuleFolder, "modB");
       
            File serverModAXmlFile = new File(modAFolder, "src/main/liberty/config/server.xml");
            String serverModAGenXSDURI = serverModAXmlFile.toPath().toUri().toString().replace("///", "/");

            File serverModBXmlFile = new File(modBFolder, "src/main/liberty/config/server.xml");
            String serverModBGenXSDURI = serverModBXmlFile.toPath().toUri().toString().replace("///", "/");

            //Configure Liberty workspace for testing
            WorkspaceFolder testWorkspace = new WorkspaceFolder(parentModuleFolder.toURI().toString());
            List<WorkspaceFolder> testWorkspaceFolders = new ArrayList<WorkspaceFolder>();
            testWorkspaceFolders.add(testWorkspace);
            LibertyProjectsManager.getInstance().setWorkspaceFolders(testWorkspaceFolders);

                String serverXML = String.join(newLine, //
                                "<server description=\"Sample Liberty server\">", //
                                "       <featureManager>", //
                                "               <feature>|</feature>", //
                                "               <feature>mpConfig-1.4</feature>", //
                                "       </featureManager>", //
                                "</server>" //
                );

            CompletionItem jaxrsCompletion = c("jaxrs-2.1", "jaxrs-2.1");

            // would be 228 if mpConfig-1.4 was not already specified - this is using ol-22.0.0.12
            final int TOTAL_ITEMS = 227; // total number of available completion items

            XMLAssert.testCompletionFor(serverXML, null, serverModAXmlFile.toURI().toString(), TOTAL_ITEMS, jaxrsCompletion);
            XMLAssert.testCompletionFor(serverXML, null, serverModBXmlFile.toURI().toString(), TOTAL_ITEMS, jaxrsCompletion);
        
            CompletionItem websocket = c("websocket-1.1", "websocket-1.1");

            XMLAssert.testCompletionFor(serverXML, null, serverModAXmlFile.toURI().toString(), TOTAL_ITEMS, websocket);
        
            // Verify that a feature list was NOT generated. It should have downloaded the features.json from Maven Central.
            String featureListName = "featurelist-ol-22.0.0.12.xml";
            File featurelistFile = new File(LibertyUtils.getTempDir(LibertyProjectsManager.getInstance().getWorkspaceFolder(serverModAXmlFile.toURI().toString())), featureListName);

            org.junit.jupiter.api.Assertions.assertFalse(featurelistFile.exists(), "Found unexpected generated featurelist file: "+featureListName);
        } catch (Exception e) {
            e.printStackTrace();
            org.junit.jupiter.api.Assertions.fail("Test FAILED...received unexpected exception.");
        }

    }
}
