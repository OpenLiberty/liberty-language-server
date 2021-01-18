package io.openliberty.lemminx.liberty.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.lemminx.dom.DOMDocument;

import io.openliberty.lemminx.liberty.services.LibertyProjectsManager;

public class LibertyUtils {

    private static final Logger LOGGER = Logger.getLogger(LibertyUtils.class.getName());

    private LibertyUtils() {
    }

    public static boolean isServerXMLFile(String filePath) {
        return filePath.endsWith("/" + LibertyConstants.SERVER_XML);
    }

    public static boolean isServerXMLFile(DOMDocument file) {
        return file.getDocumentURI().endsWith("/" + LibertyConstants.SERVER_XML);
    }

    /**
     * Given a server xml uri find the associated workspace folder and search that
     * folder for the most recently edited file that matches the given name
     * 
     * @param serverXmlURI
     * @param filename
     * @return path to given file or null if could not be found
     */
    public static Path findFileInWorkspace(String serverXmlURI, String filename) {

        String workspaceFolderURI = LibertyProjectsManager.getWorkspaceFolder(serverXmlURI);
        if (workspaceFolderURI == null) {
            return null;
        }
        try {
            URI rootURI = new URI(workspaceFolderURI);
            Path rootPath = Paths.get(rootURI);
            List<Path> matchingFiles = Files.walk(rootPath)
                    .filter(p -> (Files.isRegularFile(p) && p.getFileName().endsWith(filename)))
                    .collect(Collectors.toList());
            if (matchingFiles.isEmpty()) {
                return null;
            }
            if (matchingFiles.size() == 1) {
                return matchingFiles.get(0);
            }
            Path lastModified = matchingFiles.get(0);
            for (Path p : matchingFiles) {
                if (lastModified.toFile().lastModified() < p.toFile().lastModified()) {
                    lastModified = p;
                }
            }
            return lastModified;
        } catch (IOException | URISyntaxException e) {
            LOGGER.warning("Could not find: " + filename + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Get the version from the installed Liberty instance Searched for and gets the
     * version from an openliberty.properties file
     * 
     * @param serverXML server xml associated
     * @return version of Liberty or null
     */
    public static String getVersion(DOMDocument serverXML) {
        // TODO: add logic to determine how often we should check for list of installed
        // features

        // find workspace folder this serverXML belongs to
        String workspaceFolderURI = LibertyProjectsManager.getWorkspaceFolder(serverXML.getDocumentURI());
        if (workspaceFolderURI == null) {
            return null;
        }
        String version;

        LibertyProjectsManager projectsManager = LibertyProjectsManager.getInstance();
        version = projectsManager.getLibertyVersion(workspaceFolderURI);
        LOGGER.info("---- version from cache: " + version);
        if (version == null) {
            Path propertiesFile = findFileInWorkspace(serverXML.getDocumentURI(), "openliberty.properties");
            if (propertiesFile != null && propertiesFile.toFile().exists()) {
                Properties prop = new Properties();
                try {
                    FileInputStream fis = new FileInputStream(propertiesFile.toFile());
                    prop.load(fis);
                    version = prop.getProperty("com.ibm.websphere.productVersion");
                    LOGGER.info("---- version from property file: " + version);
                    projectsManager.updateLibertyVersionCache(workspaceFolderURI, version);
                    return version;
                } catch (IOException e) {
                    LOGGER.warning("Unable to get version from properties file: " + propertiesFile.toString() + ": "
                            + e.getMessage());
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Return temp dir to store generated feature lists and schema Creates temp dir
     * if it does not exist
     * 
     * @param folder
     * @return
     */
    public static File getTempDir(String workspaceFolderURI) {
        if (workspaceFolderURI == null) {
            return null;
        }
        try {
            URI rootURI = new URI(workspaceFolderURI);
            Path rootPath = Paths.get(rootURI);
            File file = rootPath.toFile();
            File libertyLSFolder = new File(file, ".libertyls");

            if (!libertyLSFolder.exists()) {
                if (!libertyLSFolder.mkdir()) {
                    return null;
                }
            }
            return file;
        } catch (Exception e) {
            // unable to create temp dir
            LOGGER.warning("Unable to create temp dir: " + e.getMessage());
        }
        return null;
    }

}
