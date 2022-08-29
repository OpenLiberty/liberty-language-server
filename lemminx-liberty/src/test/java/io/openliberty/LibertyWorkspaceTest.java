package io.openliberty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

import io.openliberty.tools.langserver.lemminx.services.LibertyWorkspace;

public class LibertyWorkspaceTest {
    
    @Test
    public void readDevcMetadata() throws URISyntaxException {
        File srcResourcesDir = new File("src/test/resources");
        URI resourcesDir = srcResourcesDir.toURI();
        LibertyWorkspace libertyWorkspace = new LibertyWorkspace(resourcesDir.toString());
        assertNull(libertyWorkspace.getContainerName());
        assertFalse(libertyWorkspace.isContainerAlive());
        
        assertNotNull(libertyWorkspace.findDevcMetadata());
        
        assertEquals("liberty-dev", libertyWorkspace.getContainerName());
        assertTrue(libertyWorkspace.isContainerAlive());
    }

}
