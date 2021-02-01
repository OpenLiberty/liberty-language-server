package io.openliberty.lemminx.liberty.services;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.WorkspaceFolder;

public class LibertyProjectsManager {

    private static final Logger LOGGER = Logger.getLogger(LibertyProjectsManager.class.getName());

    private static final LibertyProjectsManager INSTANCE = new LibertyProjectsManager();

    private List<LibertyWorkspace> libertyWorkspaceFolders;

    public static LibertyProjectsManager getInstance() {
        return INSTANCE;
    }

    private LibertyProjectsManager() {
        libertyWorkspaceFolders = new ArrayList<LibertyWorkspace>();
    }

    public void setWorkspaceFolders(List<WorkspaceFolder> workspaceFolders) {
        for (WorkspaceFolder folder : workspaceFolders) {
            LibertyWorkspace libertyWorkspace = new LibertyWorkspace(folder.getUri());
            this.libertyWorkspaceFolders.add(libertyWorkspace);
        }
    }

    public List<LibertyWorkspace> getLibertyWorkspaceFolders() {
        return this.libertyWorkspaceFolders;
    }

    public String getLibertyVersion(LibertyWorkspace libertyWorkspace) {
        return libertyWorkspace.getLibertyVersion();
    }

    /**
     * Given a serverXML URI return the corresponding workspace folder URI
     * 
     * @param serverXMLUri
     * @return
     */
    public static LibertyWorkspace getWorkspaceFolder(String serverXMLUri) {
        for (LibertyWorkspace folder : getInstance().getLibertyWorkspaceFolders()) {
            if (serverXMLUri.contains(folder.getURI())) {
                return folder;
            }
        }
        return null;
    }

    public void cleanUpTempDirs() {
        for (LibertyWorkspace folder : getInstance().getLibertyWorkspaceFolders()) {
            // search for liberty ls directory
            String workspaceFolderURI = folder.getURI();
            try {
                if (workspaceFolderURI != null) {
                    URI rootURI = new URI(workspaceFolderURI);
                    Path rootPath = Paths.get(rootURI);
                    List<Path> matchingFiles = Files.walk(rootPath)
                            .filter(p -> (Files.isDirectory(p) && p.getFileName().endsWith(".libertyls")))
                            .collect(Collectors.toList());

                    // delete each liberty ls directory
                    for (Path libertylsDir : matchingFiles) {
                        if (!libertylsDir.toFile().delete()) {
                            LOGGER.warning("Could not delete " + libertylsDir);
                        }
                    }
                }
            } catch (IOException | URISyntaxException e) {
                LOGGER.warning("Could not clean up /.libertyls directory: " + e.getMessage());
            }
        }
    }

}
