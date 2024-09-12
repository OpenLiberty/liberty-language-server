package io.openliberty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.lsp4j.WorkspaceFolder;
import org.junit.jupiter.api.Test;

import io.openliberty.tools.langserver.lemminx.data.FeatureListGraph;
import io.openliberty.tools.langserver.lemminx.models.feature.Feature;
import io.openliberty.tools.langserver.lemminx.services.FeatureService;
import io.openliberty.tools.langserver.lemminx.services.LibertyProjectsManager;
import io.openliberty.tools.langserver.lemminx.services.LibertyWorkspace;
import jakarta.xml.bind.JAXBException;

public class LibertyFeatureTest {
    
    @Test
    public void getInstalledFeaturesListTest() throws JAXBException {
        FeatureService fs = FeatureService.getInstance();
        File srcResourcesDir = new File("src/test/resources/sample");
        File featureListFile = new File(srcResourcesDir.getParentFile(), "featurelist-ol-24.0.0.9.xml");
        
        // LibertyWorkspace must be initialized
        List<WorkspaceFolder> initList = new ArrayList<WorkspaceFolder>();
        initList.add(new WorkspaceFolder(srcResourcesDir.toURI().toString()));
        LibertyProjectsManager.getInstance().cleanInstance();
        LibertyProjectsManager.getInstance().setWorkspaceFolders(initList);
        Collection<LibertyWorkspace> workspaceFolders = LibertyProjectsManager.getInstance().getLibertyWorkspaceFolders();
        assertTrue(workspaceFolders.size() == 1);

        LibertyWorkspace libWorkspace = workspaceFolders.iterator().next();

        List<Feature> installedFeatures = new ArrayList<Feature>();
        installedFeatures = fs.readFeaturesFromFeatureListFile(installedFeatures, libWorkspace, featureListFile);
        
        assertFalse(installedFeatures.isEmpty());
        assertTrue(installedFeatures.equals(libWorkspace.getInstalledFeatureList()));
        // Check that list contains a beta feature
        assertTrue(installedFeatures.removeIf(f -> (f.getName().equals("cdi-4.0"))));

        // Check if config map gets built
        FeatureListGraph fg = libWorkspace.getFeatureListGraph();
        assertEquals(90, fg.getAllEnabledBy("ssl-1.0").size());
        assertEquals(1, fg.getConfigElementNode("ssl").getEnabledBy().size());
        assertTrue(fg.getConfigElementNode("ssl").getEnabledBy().contains("ssl-1.0"));
        assertEquals(91, fg.getAllEnabledBy("ssl").size());
        assertEquals(285, fg.getAllEnabledBy("library").size());
        assertTrue(fg.getAllEnabledBy("ltpa").contains("admincenter-1.0"));  // direct enabler
        assertTrue(fg.getAllEnabledBy("ssl").contains("microprofile-5.0"));  // transitive enabler
    }
}
