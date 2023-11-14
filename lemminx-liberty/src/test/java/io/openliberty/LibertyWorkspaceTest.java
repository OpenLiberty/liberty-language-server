package io.openliberty;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

import io.openliberty.tools.langserver.lemminx.services.LibertyWorkspace;
import io.openliberty.tools.langserver.lemminx.util.LibertyUtils;

public class LibertyWorkspaceTest {
    
    @Test
    public void testReadDevcMetadata() throws URISyntaxException {
        File srcResourcesDir = new File("src/test/resources");
        URI resourcesDir = srcResourcesDir.toURI();
        LibertyWorkspace libertyWorkspace = new LibertyWorkspace(resourcesDir.toString());
        assertNull(libertyWorkspace.getContainerName());
        assertTrue(libertyWorkspace.getContainerType().equals("docker"));
        assertFalse(libertyWorkspace.isContainerAlive());
        assertNull(libertyWorkspace.findDevcMetadata());    // no alive containers return null

        /* Uncomment to enable, 1) switch containerAlive to true, and 2) expect harmless runtime error */
        // assertNotNull(libertyWorkspace.findDevcMetadata());
        // assertEquals("liberty-dev", libertyWorkspace.getContainerName());
        // assertTrue(libertyWorkspace.isContainerAlive());
    }

    @Test
    public void testConfigDropinsDefaults() throws IOException {
        File mockXML = new File("src/test/resources/configDropins/defaults/my.xml");
        URI filePathURI = mockXML.toURI();

        assertTrue(LibertyUtils.isConfigXMLFile(filePathURI.toString()));

    }

    @Test
    public void testBackslashConfigDetection() throws IOException {
        // run test on Windows
        if (File.separator.equals("/")) {
            return;
        }

        File mockXML = new File("src/test/resources/sample/custom_server.xml");
        String filePathString = mockXML.getCanonicalPath();
        URI filePathURI = mockXML.toURI();

        assertTrue(LibertyUtils.isConfigXMLFile(filePathURI.toString()));

        // method expects URI formatted string and so should fail on Windows
        boolean test1 = LibertyUtils.isConfigXMLFile(filePathString);
        assertFalse(test1);

        // mock replacement
        filePathString = filePathString.replace("\\", "/");
        // method expects URI formatted string and so should fail on Windows
        boolean test2 = LibertyUtils.isConfigXMLFile(filePathString);
        assertFalse(test2);

    }
}
