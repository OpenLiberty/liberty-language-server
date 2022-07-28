/*******************************************************************************
* Copyright (c) 2020, 2022 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     IBM Corporation - initial API and implementation
*******************************************************************************/
package io.openliberty.tools.langserver.lemminx.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.lemminx.dom.DOMDocument;

import io.openliberty.tools.langserver.lemminx.models.feature.Feature;
import io.openliberty.tools.langserver.lemminx.services.LibertyProjectsManager;
import io.openliberty.tools.langserver.lemminx.services.LibertyWorkspace;
import io.openliberty.tools.langserver.lemminx.services.SettingsService;

public class LibertyUtils {

    private static final Logger LOGGER = Logger.getLogger(LibertyUtils.class.getName());

    private static Thread thread;

    private LibertyUtils() {
    }

    public static boolean isServerXMLFile(String filePath) {
        return filePath.endsWith("/" + LibertyConstants.SERVER_XML);
    }

    public static boolean isServerXMLFile(DOMDocument file) {
        return file.getDocumentURI().endsWith("/" + LibertyConstants.SERVER_XML);
    }

    public static boolean isConfigDirFile(String filePath) {
        return filePath.contains(LibertyConstants.WLP_USER_CONFIG_DIR) ||
                filePath.contains(LibertyConstants.SERVER_CONFIG_DROPINS_DEFAULTS) ||
                filePath.contains(LibertyConstants.SERVER_CONFIG_DROPINS_OVERRIDES);
    }

    public static boolean isConfigXMLFile(String filePath) {
        return isServerXMLFile(filePath) || isConfigDirFile(filePath) ||
                LibertyProjectsManager.getInstance().getWorkspaceFolder(filePath).hasConfigFile(filePath);
    }

    public static boolean isConfigXMLFile(DOMDocument file) {
        return isConfigXMLFile(file.getDocumentURI());
    }

    // Convenience methods
    public static File getDocumentAsFile(DOMDocument document) {
        return new File(getDocumentAsUri(document));
    }

    public static URI getDocumentAsUri(DOMDocument document) {
        return URI.create(document.getDocumentURI());
    }

    /**
     * Given a server.xml URI find the associated workspace folder and search that
     * folder for the most recently edited file that matches the given name.
     * 
     * @param serverXmlURI
     * @param filename
     * @return path to given file or null if could not be found
     */
    public static Path findFileInWorkspace(String serverXmlURI, String filename) {
        LibertyWorkspace libertyWorkspace = LibertyProjectsManager.getInstance().getWorkspaceFolder(serverXmlURI);
        return findFileInWorkspace(libertyWorkspace, Paths.get(filename));
    }

    public static Path findFileInWorkspace(LibertyWorkspace libertyWorkspace, Path filePath) {
        if (libertyWorkspace.getWorkspaceURI() == null) {
            return null;
        }
        try {
            Path rootPath = Paths.get(libertyWorkspace.getWorkspaceURI());
            List<Path> matchingFiles = Files.walk(rootPath)
                    .filter(p -> (Files.isRegularFile(p) && p.endsWith(filePath)))
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
        } catch (IOException e) {
            LOGGER.warning("Could not find: " + filePath.toString() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Given a server.xml, determine Liberty runtime type of the LibertyWorkspace.
     * 
     * Will return 'wlp', 'ol', or null depending on the relevant property files found in the wlp/libs/version folder.
     * 
     * @param serverXMLUri server xml uri
     * @return Liberty runtime or null
     */
    public static String getRuntimeInfo(String serverXMLUri) {

        // return runtime set in settings if it exists
        String libertyRuntime = SettingsService.getInstance().getLibertyRuntime();
        if (libertyRuntime != null) {
            return libertyRuntime;
        }
        // find workspace folder this serverXML belongs to
        LibertyWorkspace libertyWorkspace = LibertyProjectsManager.getInstance().getWorkspaceFolder(serverXMLUri);

        if (libertyWorkspace == null || libertyWorkspace.getWorkspaceURI() == null) {
            return null;
        }

        String runtime = libertyWorkspace.getLibertyRuntime();

        // return version from cache if set and Liberty is installed
        if (runtime != null && libertyWorkspace.isLibertyInstalled()) {
            return runtime;
        }

        if (findFileInWorkspace(serverXMLUri, "WebSphereApplicationServer.properties") != null ) {
            runtime = "wlp";
            libertyWorkspace.setLibertyRuntime(runtime);
            return runtime;
        } else if (findFileInWorkspace(serverXMLUri, "openliberty.properties") != null ) {
            runtime = "ol";
            libertyWorkspace.setLibertyRuntime(runtime);
            return runtime;
        } else {
            // did not detect a new liberty properties file, return version from cache
            return runtime;
        }
    }

    /**
     * Given a server.xml find the version associated with the corresponding Liberty
     * workspace. If the version has not been set via the Settings Service, search for an
     * openliberty.properties file in the workspace and return the version from that
     * file. Otherwise, return null.
     * 
     * @param serverXML server xml associated
     * @return version of Liberty or null
     */
    public static String getVersion(DOMDocument serverXML) {
        return getVersion(serverXML.getDocumentURI());
    }

    public static String getVersion(String serverXMLUri) {
        // return version set in settings if it exists
        String libertyVersion = SettingsService.getInstance().getLibertyVersion();
        if (libertyVersion != null) {
            return libertyVersion;
        }
        // find workspace folder this serverXMLUri belongs to
        LibertyWorkspace libertyWorkspace = LibertyProjectsManager.getInstance().getWorkspaceFolder(serverXMLUri);

        if (libertyWorkspace == null || libertyWorkspace.getWorkspaceString() == null) {
            return null;
        }

        String version = libertyWorkspace.getLibertyVersion();

        // return version from cache if set and Liberty is installed
        if (version != null && libertyWorkspace.isLibertyInstalled()) {
            return version;
        }
        Path propertiesFile = findFileInWorkspace(serverXMLUri, "openliberty.properties");

        // detected a new Liberty properties file, re-calculate version
        if (propertiesFile != null && propertiesFile.toFile().exists()) {
            // new properties file, reset the installed features stored in the feature cache
            // so that the installed features list will be regenerated as it may have
            // changed between Liberty installations
            libertyWorkspace.setInstalledFeatureList(new ArrayList<Feature>());
            Properties prop = new Properties();
            try {
                // add a file watcher on this file
                if (!libertyWorkspace.isLibertyInstalled()) {
                    watchFiles(propertiesFile, libertyWorkspace);
                }

                FileInputStream fis = new FileInputStream(propertiesFile.toFile());
                prop.load(fis);
                version = prop.getProperty("com.ibm.websphere.productVersion");
                libertyWorkspace.setLibertyVersion(version);
                libertyWorkspace.setLibertyInstalled(true);
                return version;
            } catch (IOException e) {
                LOGGER.warning("Unable to get version from properties file: " + propertiesFile.toString() + ": "
                        + e.getMessage());
                return null;
            }
        } else {
            // did not detect a new liberty properties file, return version from cache
            return version;
        }
    }

    /**
     * Return temp directory to store generated feature lists and schema. Creates
     * temp directory if it does not exist.
     * 
     * @param folder WorkspaceFolderURI indicates where to create the temporary
     *               directory
     * @return temporary directory File object
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
            return libertyLSFolder;
        } catch (Exception e) {
            LOGGER.warning("Unable to create temp dir: " + e.getMessage());
        }
        return null;
    }

    /**
     * Watches the parent directory of the Liberty properties file in a separate
     * thread. If the the contents of the directory have been modified or deleted,
     * the installation of Liberty has changed and the corresponding Liberty
     * Workspace item is updated.
     * 
     * @param propertiesFile   openliberty.properties file to watch
     * @param libertyWorkspace Liberty Workspace object, updated to indicate if
     *                         there is an associated installation of Liberty
     */
    public static void watchFiles(Path propertiesFile, LibertyWorkspace libertyWorkspace) {
        try {
            WatchService watcher = FileSystems.getDefault().newWatchService();
            propertiesFile.getParent().register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
            thread = new Thread(() -> {
                WatchKey watchKey = null;
                try {
                    while (true) {
                        watchKey = watcher.poll(5, TimeUnit.SECONDS);
                        if (watchKey != null) {
                            watchKey.pollEvents().stream().forEach(event -> {
                                LOGGER.fine("Liberty properties file (" + propertiesFile + ") has been modified: "
                                        + event.context());
                                // if modified re-calculate version
                                libertyWorkspace.setLibertyInstalled(false);
                            });

                            // if watchkey.reset() returns false indicates that the parent folder has been
                            // deleted
                            boolean valid = watchKey.reset();
                            if (!valid) {
                                // if deleted re-calculate version
                                LOGGER.fine("Liberty properties file (" + propertiesFile + ") has been deleted");
                                libertyWorkspace.setLibertyInstalled(false);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    LOGGER.warning("Unable to watch properties file(s): " + e.toString());
                }
            });
            thread.start();
        } catch (IOException e) {
            LOGGER.warning("Unable to watch properties file(s): " + e.toString());
        }
    }
}
