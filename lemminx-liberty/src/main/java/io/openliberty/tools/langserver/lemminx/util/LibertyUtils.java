/*******************************************************************************
* Copyright (c) 2020, 2024 IBM Corporation and others.
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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.lemminx.dom.DOMDocument;

import io.openliberty.tools.langserver.lemminx.data.LibertyRuntime;
import io.openliberty.tools.langserver.lemminx.models.feature.Feature;
import io.openliberty.tools.langserver.lemminx.models.settings.DevcMetadata;
import io.openliberty.tools.langserver.lemminx.services.ContainerService;
import io.openliberty.tools.langserver.lemminx.services.LibertyProjectsManager;
import io.openliberty.tools.langserver.lemminx.services.LibertyWorkspace;
import io.openliberty.tools.langserver.lemminx.services.SettingsService;

public class LibertyUtils {

    private static final Logger LOGGER = Logger.getLogger(LibertyUtils.class.getName());
    private static final String INCLUDE_PATTERN_REGEX = ".*/(?:usr/shared/config/.+|configDropins/(?:defaults|overrides)/.+|server\\.xml)$";
    private static final Pattern INCLUDE_PATTERN = Pattern.compile(INCLUDE_PATTERN_REGEX);

    private static final String EXCLUDE_PATTERN_REGEX = ".*/(?:target(?!/it/)|build)/.+$";
    private static final Pattern EXCLUDE_PATTERN = Pattern.compile(EXCLUDE_PATTERN_REGEX);

    private static Thread thread;

    private LibertyUtils() {
    }

    /*
     * Check the filePath to see if it is a Liberty config file. The following qualify as a Liberty config file:
     * - an XML file in the /usr/shared/config/, /configDropins/overrides/, or /configDropins/defaults/ directories
     * - an XML file in any directory ending with /server.xml
     * - an XML file with a <server> root element
     * 
     * If the rootPath is null, all XML files that do not fall into category 1 or 2 will be checked for a <server> root element.
     * If the rootPath is not null, only XML files that that do not fall into category 1 or 2 and are not located in a target/build 
     * directory will be checked for a <server> root element. The rootPath is non-null when called from getXmlFilesWithServerRootInDirectory method.
     * 
     * @param rootPath - String path of root directory for the filePath in URI format - only consider the portion of the filePath after the rootPath
     * @param filePath - String path of the XML file to check in URI format
     * @return boolean - true if the XML file is a Liberty config file, false otherwise
     */
    public static boolean isConfigXMLFile(String rootPath, String filePath) {
        String pathToCheck = rootPath == null ? filePath : filePath.substring(rootPath.length());

        // if path contains one of the pre-defined Liberty config dirs or ends with /server.xml, 
        // just return true without checking for server root
        Matcher m = INCLUDE_PATTERN.matcher(pathToCheck);
        if (m.find()) {
            return true;
        }

        if (rootPath != null) {
            // if path contains target/build dir (except for target/it for test purposes), just return false
            m = EXCLUDE_PATTERN.matcher(pathToCheck);
            if (m.find()) {
                return false;
            }
        }

        // need to check if file has a server root element
        return XmlReader.hasServerRoot(filePath);
    }

    public static boolean isConfigXMLFile(String filePath) {
        return isConfigXMLFile(null, filePath);
    }

    public static boolean isConfigXMLFile(DOMDocument file) {
        return isConfigXMLFile(null, file.getDocumentURI());
    }

    // Convenience methods
    public static File getDocumentAsFile(DOMDocument document) {
        return new File(getDocumentAsUri(document));
    }

    public static URI getDocumentAsUri(DOMDocument document) {
        return URI.create(document.getDocumentURI());
    }

    /*
     * Check the dirs collection of Path objects to see if any of them have a name that matches the passed collection of names.
     * 
     * @param dirs collection of Path objects that are directories
     * @param names collection of Strings that are names to match on
     * @return boolean true if any of the passed Path objects have a name that matches one in the collection of names
     */
    public static boolean containsDirectoryWithName(List<Path> dirs, Set<String> names) {
        for (Path nextDir : dirs) {
            if (names.contains(nextDir.toFile().getName())) {
                return true;
            }
        }
        return false;
    }

    /*
     * Check the files to see if any start with the Path for rootDir.
     * 
     * @param rootDir Path to compare using startsWith method
     * @param files collection of Path objects that are files to check
     * @return boolean true if any of the passed Path objects for files start with the rootDir Path
     */
    public static boolean containsFileStartingWithRootPath(Path rootDir, List<Path> files) {
        for (Path nextFile : files) {
            if (nextFile.startsWith(rootDir)) {
                return true;
            }
        }
        return false;
    }

    /*
     * Retrieve a collection of Path objects for xml files located in the passed Path dir that have a <server> root element.
     * 
     * @param dir Path of directory to check for xml files 
     * @return List<Path> collection of xml files with server root elements
     */
    public static List<Path> getXmlFilesWithServerRootInDirectory(Path dir) throws IOException {
        List<Path> serverRootXmlFiles = new ArrayList<Path>();
        String rootPath = dir.toFile().toURI().toString();

        List<Path> xmlFiles = findFilesEndsWithInDirectory(dir, ".xml");
        for (Path nextXmlFile : xmlFiles) {
            if (isConfigXMLFile(rootPath, nextXmlFile.toFile().toURI().toString())) {
                serverRootXmlFiles.add(nextXmlFile);
            }
        }       

        return serverRootXmlFiles;
    }

    /**
     * Search the dir path for all files that end with a name or extension. If none are found,
     * an empty List is returned.
     * 
     * @param dir Path to search under
     * @param nameOrExtension String to match
     * @return List<Path> collection of Path that match the given nameOrExtension in the specified dir Path.
     */
    public static List<Path> findFilesEndsWithInDirectory(Path dir, String nameOrExtension) throws IOException {
        List<Path> matchingFiles = Files.walk(dir)
                .filter(p -> (Files.isRegularFile(p) && p.toFile().getName().toLowerCase().endsWith(nameOrExtension)))
                .collect(Collectors.toList());

        return matchingFiles;
    }

    /**
     * Search the dir path for all files that match the given name. If none are found,
     * an empty List is returned.
     * 
     * @param dir Path to search under
     * @param filePath Path to match
     * @return List<Path> collection of Path that match the given filePath in the specified dir Path.
     */
    public static List<Path> findFilesInDirectory(Path dir, Path filePath) throws IOException {
        List<Path> matchingFiles = Files.walk(dir)
                .filter(p -> (Files.isRegularFile(p) && p.endsWith(filePath)))
                .collect(Collectors.toList());

        return matchingFiles;
    }

    /**
     * Search the dir path for all files that match the given name. If none are found,
     * null is returned. If one or more are found, the one that was last modified is returned.
     * 
     * @param dir Path to search under
     * @param filePath Path to match
     * @return path to given file or null if could not be found
     */
    public static Path findLastModifiedMatchingFileInDirectory(Path dir, Path filePath) throws IOException {
        Path foundFilePath = null;
        List<Path> matchingFiles = findFilesInDirectory(dir, filePath);

        if (matchingFiles.isEmpty()) {
            foundFilePath = null;
        } else if (matchingFiles.size() == 1) {
            foundFilePath = matchingFiles.get(0);
        } else {
            Path lastModified = matchingFiles.get(0);
            for (Path p : matchingFiles) {
                if (lastModified.toFile().lastModified() < p.toFile().lastModified()) {
                    lastModified = p;
                }
            }
            foundFilePath = lastModified;
        }

        return foundFilePath;
    }

    /**
     * Given a Path and a LibertyWorkspace, find the most recently edited file that matches the given Path.
     * 
     * @param libertyWorkspace
     * @param filePath
     * @return path to given file or null if could not be found
     */
    public static Path findFileInWorkspace(LibertyWorkspace libertyWorkspace, Path filePath) {
        if (libertyWorkspace.getWorkspaceURI() == null) {
            return null;
        }
        try {
            Path rootPath = Paths.get(libertyWorkspace.getWorkspaceURI());
            return findLastModifiedMatchingFileInDirectory(rootPath, filePath);
        } catch (IOException e) {
            LOGGER.warning("Could not find: " + filePath.toString() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Given a Path and a LibertyWorkspace, find the most recently edited file that matches the given Path in the Liberty installation for the workspace.
     * 
     * @param libertyWorkspace
     * @param filePath
     * @return path to given file or null if could not be found
     */
    public static Path findLibertyFileForWorkspace(LibertyWorkspace libertyWorkspace, Path filePath) {
        if (libertyWorkspace.getWorkspaceURI() == null || !libertyWorkspace.isLibertyInstalled()) {
            return null;
        }

        Path foundFilePath = null;

        try {
            if (libertyWorkspace.getLibertyInstallationDir() != null) {
                foundFilePath = LibertyUtils.findLastModifiedMatchingFileInDirectory(Paths.get(libertyWorkspace.getLibertyInstallationDir()), filePath);
            } else {
                foundFilePath = LibertyUtils.findFileInWorkspace(libertyWorkspace, filePath);
            }
        } catch (IOException e) {
            LOGGER.warning("Could not find: " + filePath.toString() + ": " + e.getMessage());
        }

        return foundFilePath;
    }

    /**
     * Given a server xml file, find the Liberty version and runtime associated with the corresponding Liberty
     * workspace. If the version or runtime has not been set via the Settings Service, check if they have been set
     * in the Liberty workspace. If not set, search for the liberty-plugin-config.xml file in the Liberty workspace 
     * and use the installDir location from that file to load the runtime info from the properties file
     * (WebSphereApplicationServer.properties or openliberty.properties). Otherwise, look for the properties file in the workspace. 
     * If found, load the runtime info from the properties. Otherwise, return null.
     * 
     * @param serverXML server xml associated
     * @return version of Liberty or null
     */    
    public static LibertyRuntime getLibertyRuntimeInfo(DOMDocument serverXML) {
        return getLibertyRuntimeInfo(serverXML.getDocumentURI());
    }

    public static LibertyRuntime getLibertyRuntimeInfo(String serverXMLUri) {
        // find workspace folder this serverXML belongs to
        LibertyWorkspace libertyWorkspace = LibertyProjectsManager.getInstance().getWorkspaceFolder(serverXMLUri);

        return getLibertyRuntimeInfo(libertyWorkspace);
    }

    public static LibertyRuntime getLibertyRuntimeInfo(LibertyWorkspace libertyWorkspace) {
        // return runtime set in settings if it exists
        String libertyRuntime = SettingsService.getInstance().getLibertyRuntime();
        String libertyVersion = SettingsService.getInstance().getLibertyVersion();
         
        if (libertyRuntime != null && libertyVersion != null) {
            return new LibertyRuntime(libertyRuntime, libertyVersion, null);
        }

        if (libertyWorkspace == null || libertyWorkspace.getWorkspaceURI() == null) {
            return null;
        }

        String runtime = libertyWorkspace.getLibertyRuntime();
        String version  = libertyWorkspace.getLibertyVersion();
        String location = libertyWorkspace.getLibertyInstallationDir();

        LibertyRuntime currentRuntimeInfo = null;
        Path propsFile = null;
        boolean updateRuntimeInfo = false;

        // return version from cache if set and Liberty is installed
        if (libertyWorkspace.isLibertyRuntimeAndVersionSet() && (libertyWorkspace.isLibertyInstalled() || libertyWorkspace.isContainerAlive())) {
            // double check that the location has not changed - rare scenario where Liberty was previously installed and then build file
            // is changed to install somewhere else - should not use old location and potentially wrong runtime/version
            if (libertyWorkspace.isLibertyInstalled()) {
                propsFile = getLibertyPropertiesFile(libertyWorkspace);
                if (propsFile != null && propsFile.toFile().exists()) {
                    currentRuntimeInfo = new LibertyRuntime(propsFile);
                    if ((isRuntimeLocationDifferent(currentRuntimeInfo, location))) {
                        updateRuntimeInfo = true;
                    }
                }
            }
            if (!updateRuntimeInfo) {
                return new LibertyRuntime(runtime, version, location);
            }
        }

        // workspace either has Liberty local or in running container
        Path devcMetadataFile = libertyWorkspace.findDevcMetadata();
        boolean devcOn = devcMetadataFile != null;

        if (devcOn || !updateRuntimeInfo) {
            propsFile = devcOn ? getLibertyPropertiesFileForDevc(libertyWorkspace) : getLibertyPropertiesFile(libertyWorkspace);
        }

        if (propsFile != null && propsFile.toFile().exists()) {
            // new properties file, reset the installed features stored in the feature cache
            // so that the installed features list will be regenerated as it may have
            // changed between Liberty installations
            libertyWorkspace.setInstalledFeatureList(new ArrayList<Feature>());

            // add a file watcher on this file
            if (!libertyWorkspace.isLibertyInstalled() || updateRuntimeInfo) {
                watchFiles(devcOn ? devcMetadataFile : propsFile, libertyWorkspace);
            }

            LibertyRuntime libertyRuntimeInfo = (devcOn || !updateRuntimeInfo) ? new LibertyRuntime(propsFile) : currentRuntimeInfo;

            if (libertyRuntimeInfo != null) {
                libertyWorkspace.setLibertyRuntime(libertyRuntimeInfo.getRuntimeType());
                libertyWorkspace.setLibertyVersion(libertyRuntimeInfo.getRuntimeVersion());

                // if not a container, set the Liberty installation dir
                if (!devcOn && (libertyRuntimeInfo.getRuntimeLocation() != null)) {
                    libertyWorkspace.setLibertyInstallationDir(libertyRuntimeInfo.getRuntimeLocation());
                }

                libertyWorkspace.setLibertyInstalled(!devcOn);
            }

            return libertyRuntimeInfo;
        }

        return null;

    }

    public static boolean isRuntimeLocationDifferent(LibertyRuntime runtimeInfo, String location) {
        String currentLocation = runtimeInfo.getRuntimeLocation();

        if (((currentLocation != null) && (location != null) && !currentLocation.equals(location)) ||
            (location ==  null && currentLocation != null) ||
            (currentLocation == null && location != null)) {
            return true;
        }

        return false;
    }

    /*
     * First search for liberty-plugin-config.xml to determine the installation location for Liberty in which to find the properties files. 
     * If not found, simply look for the properties files in the local libertyWorkspace. In either case, first look for WebSphereApplicatonServer.properties 
     * which is only present for WebSphere Liberty. If not found, then look for openliberty.properties which is present in both WebSphere Liberty and Open Liberty, 
     * but whose productId is only correct for Open Liberty.
     * 
     * @param libertyWorkspace
     * @return Path to the properties file to use, or null if not found
     */
    public static Path getLibertyPropertiesFile(LibertyWorkspace libertyWorkspace) {
        Path props = null;
 
        // check for Liberty installation using liberty-plugin-config.xml which should ensure using the latest Liberty install for the workspace
        Path pluginConfigFilePath = findFileInWorkspace(libertyWorkspace,Paths.get("liberty-plugin-config.xml"));
        if (pluginConfigFilePath != null) { //If liberty-plugin-config.xml exists, get installation directory from it
            String installationDirectory  = XmlReader.getElementValue(pluginConfigFilePath, "installDirectory");
            if (installationDirectory != null) {
                Path libertyInstallDir = Paths.get(installationDirectory);
                if (libertyInstallDir.toFile().exists()) {
                    try {
                        props = findLastModifiedMatchingFileInDirectory(libertyInstallDir, Paths.get("WebSphereApplicationServer.properties"));
                        if (props == null) {
                            props = findLastModifiedMatchingFileInDirectory(libertyInstallDir, Paths.get("openliberty.properties"));
                            if (props == null) {
                                LOGGER.info("Could not find openliberty.properties file in Liberty installation: " + libertyInstallDir.toString());                            
                            }
                        }
                    } catch (IOException e) {
                        LOGGER.warning("Error received loading properties file from Liberty installation: " + libertyInstallDir.toString() + ": " + e.getMessage());                            
                    }
                }
            }
        }

        // if props not found using liberty-plugin-config.xml, try checking for files in workspace
        if (props == null) {
            props = findFileInWorkspace(libertyWorkspace, Paths.get("WebSphereApplicationServer.properties"));

            if (props == null) {
                props = findFileInWorkspace(libertyWorkspace, Paths.get("openliberty.properties"));
            }
        }

        return props;
    }

    public static Path getLibertyPropertiesFileForDevc(LibertyWorkspace libertyWorkspace) {
        try {
            Path props = getFileFromContainer(libertyWorkspace, ContainerService.DEFAULT_CONTAINER_WLP_PROPERTIES_PATH, true);
            props = props.toFile().exists() ? props : getFileFromContainer(libertyWorkspace, ContainerService.DEFAULT_CONTAINER_OL_PROPERTIES_PATH, true);

            if (props.toFile().exists()) {
                return props;
            }
            LOGGER.warning("Could not find properties for container at location: "+ContainerService.DEFAULT_CONTAINER_OL_PROPERTIES_PATH);
        } catch (IOException e) {
            LOGGER.warning("Could not find properties for container at location: "+ContainerService.DEFAULT_CONTAINER_OL_PROPERTIES_PATH+" due to exception: "+e.getMessage());
        }

        return null;
    }

    public static Path getFileFromContainer(LibertyWorkspace libertyWorkspace, String fileLocation, boolean suppressError) throws IOException {
        ContainerService container = ContainerService.getInstance();
        Path tempDir = Files.createTempDirectory(null);
        File containerPropertiesFile = new File(tempDir.toFile(), "container.properties");
        container.containerCp(libertyWorkspace.getContainerType(), libertyWorkspace.getContainerName(), fileLocation, containerPropertiesFile.toString(), suppressError);
        return Paths.get(containerPropertiesFile.toString());
    }

    /**
     * Return temp directory to store generated feature lists and schema. Creates
     * temp directory if it does not exist.
     * 
     * @param libertyWorkspace LibertyWorkspace where the temporary directory is made
     *               
     * @return temporary directory File object
     */
    public static File getTempDir(LibertyWorkspace libertyWorkspace) {
        if (libertyWorkspace == null) {
            return null;
        }
        try {
            File libertyLSFolder = new File(libertyWorkspace.getDir(), ".libertyls"); //Default to workspaceDir/.libertyls
            Path pluginConfigFilePath = findFileInWorkspace(libertyWorkspace,Paths.get("liberty-plugin-config.xml"));
            if (pluginConfigFilePath != null) { //If liberty-plugin-config.xml exists use its parent directory: buildDir/.libertyls
                libertyLSFolder = new File(pluginConfigFilePath.getParent().toFile(), ".libertyls");
            }
            if (libertyLSFolder != null && !libertyLSFolder.exists()) {
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
     * thread or actively watches a liberty-devc-metadata file. If the the contents 
     * have been modified or deleted, the installation of Liberty has changed and 
     * the corresponding Liberty Workspace item is updated.
     * 
     * @param watchFile        openliberty.properties or *-liberty-devc.metadata.xml to watch
     * @param libertyWorkspace Liberty Workspace object, updated to indicate if
     *                         there is an associated installation of Liberty
     */
    public static void watchFiles(Path watchFile, LibertyWorkspace libertyWorkspace) {     
        boolean isProperties = watchFile.endsWith("openliberty.properties"); // if false, watchFile is a metadata file
        try {
            WatchService watcher = FileSystems.getDefault().newWatchService();
            watchFile.getParent().register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
            thread = new Thread(() -> {
                WatchKey watchKey = null;
                try {
                    while (true) {
                        watchKey = watcher.poll(5, TimeUnit.SECONDS);
                        if (watchKey != null) {
                            watchKey.pollEvents().stream().forEach(event -> {                                
                                if (isProperties) {
                                    // if modified re-calculate version
                                    LOGGER.info("Liberty properties file (" + watchFile + ") has been modified: "
                                    + event.context());
                                    libertyWorkspace.setLibertyInstalled(false);
                                } else if (((Path)event.context()).toString().endsWith("-liberty-devc-metadata.xml")){
                                    // watch and execute only on metadata files
                                    DevcMetadata devcMetadata = LibertyWorkspace.unmarshalDevcMetadataFile(watchFile);
                                    libertyWorkspace.setContainerAlive(devcMetadata.isContainerAlive());
                                }
                            });

                            // if watchkey.reset() returns false indicates that the parent folder has been
                            // deleted
                            boolean valid = watchKey.reset();
                            if (!valid) {
                                if (isProperties) {
                                    // if deleted re-calculate version
                                    LOGGER.info("Liberty properties file (" + watchFile + ") has been deleted");
                                    libertyWorkspace.setLibertyInstalled(false);
                                } else {
                                    // build directory deleted
                                    libertyWorkspace.setContainerAlive(false);
                                }
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    LOGGER.warning("Unable to watch properties file(s): " + e.getMessage());
                }
            });
            thread.start();
        } catch (IOException e) {
            LOGGER.warning("Unable to watch properties file(s): " + e.getMessage());
        }
    }
}
