package io.openliberty.lemminx.liberty.services;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.WorkspaceFolder;

public class LibertyProjectsManager {

    private static final Logger LOGGER = Logger.getLogger(LibertyProjectsManager.class.getName());

    private static final LibertyProjectsManager INSTANCE = new LibertyProjectsManager();

    private List<WorkspaceFolder> workspaceFolders;
    private static HashMap<String, String> libertyVersionCache;

    public static LibertyProjectsManager getInstance() {
        return INSTANCE;
    }

    private LibertyProjectsManager() {
        libertyVersionCache = new HashMap<String, String>();
    }

    public void setWorkspaceFolders(List<WorkspaceFolder> workspaceFolders) {
        this.workspaceFolders = workspaceFolders;
    }

    public List<WorkspaceFolder> getWorkspaceFolders() {
        return this.workspaceFolders;
    }

    public void updateLibertyVersionCache(String workspaceFolderURI, String version) {
        libertyVersionCache.put(workspaceFolderURI, version);
    }

    public String getLibertyVersion(String workspaceFolderURI) {
        return libertyVersionCache.get(workspaceFolderURI);
    }

    /**
     * Given a serverXML URI return the corresponding workspace folder URI
     * 
     * @param serverXMLUri
     * @return
     */
    public static String getWorkspaceFolder(String serverXMLUri) {
        for (WorkspaceFolder folder : getInstance().getWorkspaceFolders()) {
            if (serverXMLUri.contains(folder.getUri())) {
                return folder.getUri();
            }
        }
        return null;
    }

    public void cleanUpTempDirs() {
        for (WorkspaceFolder folder : getInstance().getWorkspaceFolders()) {
            // search for liberty ls directory
            String workspaceFolderURI = folder.getUri();
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
