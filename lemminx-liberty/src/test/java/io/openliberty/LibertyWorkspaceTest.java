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
        assertNull(libertyWorkspace.getContainerType());
        assertFalse(libertyWorkspace.isContainerAlive());
        assertNull(libertyWorkspace.findDevcMetadata());    // no alive containers return null

        /* Uncomment to enable, 1) switch containerAlive to true, and 2) expect harmless runtime error */
        // assertNotNull(libertyWorkspace.findDevcMetadata());
        // assertEquals("liberty-dev", libertyWorkspace.getContainerName());
        // assertTrue(libertyWorkspace.isContainerAlive());
    }

    @Test
    public void testBackslashConfigDetection() throws IOException {
        // run test on Windows
        if (File.separator.equals("/")) {
            return;
        }

        File mockXML = new File("src/test/resources/configDropins/defaults/my.xml");
        String filePathString = mockXML.getCanonicalPath();
        URI filePathURI = mockXML.toURI();

        assertFalse(LibertyUtils.isConfigDirFile(filePathString));
        assertTrue(LibertyUtils.isConfigDirFile(filePathURI.toString()));
        // mock replacement
        filePathString = filePathString.replace("\\", "/");
        assertTrue(LibertyUtils.isConfigDirFile(filePathString));
    }
}
