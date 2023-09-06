/*******************************************************************************
* Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.tools.langserver;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.WorkspaceFolder;

import io.openliberty.tools.langserver.ls.LibertyTextDocument;
import io.openliberty.tools.langserver.utils.XmlReader;

public class LibertyConfigFileManager {
    public static final String LIBERTY_PLUGIN_CONFIG_XML = "liberty-plugin-config.xml";
    public static final String CUSTOM_SERVER_ENV_XML_TAG = "serverEnv";
    public static final String CUSTOM_BOOTSTRAP_PROPERTIES_XML_TAG = "bootstrapPropertiesFile";

    private static final String DEFAULT_SERVER_ENV = "src/main/liberty/config/server.env".replace("/", File.separator);
    private static final String DEFAULT_BOOTSTRAP_PROPERTIES = "src/main/liberty/config/bootstrap.properties".replace("/", File.separator);


    private static Set<String> customServerEnvFiles = new HashSet<String>();
    private static Set<String> customBootstrapFiles = new HashSet<String>();

    private static final Logger LOGGER = Logger.getLogger(LibertyConfigFileManager.class.getName());

    public static void initLibertyConfigFileManager(List<WorkspaceFolder> workspaceFolders) {
        if (workspaceFolders == null) {
            return;
        }
        for (WorkspaceFolder folder : workspaceFolders) {
            processWorkspaceDir(folder);
        }
    }

    /**
     * Given a workspace folder, find and process all Liberty plugin config xmls
     */
    public static void processWorkspaceDir(WorkspaceFolder workspaceFolder) {
        String workspaceUriString = workspaceFolder.getUri();
        String normalizedUriString = workspaceUriString.replace("///", "/");
        URI workspaceURI = URI.create(normalizedUriString);
        Path workspacePath = Paths.get(workspaceURI);

        try {
            List<Path> lpcXmlList = findFilesEndsWithInDirectory(workspacePath, LIBERTY_PLUGIN_CONFIG_XML);
            for (Path lpcXml : lpcXmlList) {
                processLibertyPluginConfigXml(lpcXml.toUri().toString());
            }
        } catch (IOException e) {
            LOGGER.warning("Encountered an IOException on initial custom config processing: " + e.getMessage());
        }
        // LOGGER.info("Found custom files: server-" + customServerEnvFiles + "; bootstrap-"+customBootstrapFiles);
    }

    /**
     * Given a Liberty plugin config xml, store custom config file paths to memory.
     * @param uri - URI-formatted string
     */
    public static void processLibertyPluginConfigXml(String uri) {
        if (!uri.endsWith(LIBERTY_PLUGIN_CONFIG_XML)) {
            return;
        }
        Map<String, String> customConfigFiles = XmlReader.readTagsFromXml(uri,
                CUSTOM_SERVER_ENV_XML_TAG,
                CUSTOM_BOOTSTRAP_PROPERTIES_XML_TAG);
        // TODO: handle deletions
        // match uri
        if (customConfigFiles.containsKey(CUSTOM_SERVER_ENV_XML_TAG)) {
            customServerEnvFiles.add(customConfigFiles.get(CUSTOM_SERVER_ENV_XML_TAG));
        }
        if (customConfigFiles.containsKey(CUSTOM_BOOTSTRAP_PROPERTIES_XML_TAG)) {
            customBootstrapFiles.add(customConfigFiles.get(CUSTOM_BOOTSTRAP_PROPERTIES_XML_TAG));
        }
    }

    public static boolean isServerEnvFile(LibertyTextDocument tdi) {
        return isServerEnvFile(tdi.getUri());
    }

    /**
     * Checks if file matches one of these conditions:
     * - is default server.env file in `src/main/liberty/config`
     * - is custom env file specified in liberty-plugin-config.xml (generated from
     * build file)
     * 
     * @param uri - normally comes from LibertyTextDocument.getUri() which is a URI formatted string (file:///path/to/file)
     * @return
     */
    public static boolean isServerEnvFile(String uri) {
        String filePath = normalizeFilePath(uri);
        return filePath.endsWith(DEFAULT_SERVER_ENV) || customServerEnvFiles.contains(filePath);
    }

    public static boolean isBootstrapPropertiesFile(LibertyTextDocument tdi) {
        return isBootstrapPropertiesFile(tdi.getUri());
    }

    /**
     * Checks if file matches one of these conditions:
     * - is default bootstrap.properties file in `src/main/liberty/config`
     * - is custom properties file specified in liberty-plugin-config.xml (generated
     * from build file)
     * 
     * @param uri - normally comes from LibertyTextDocument.getUri() which is a URI formatted string (file:///path/to/file)
     * @return
     */
    public static boolean isBootstrapPropertiesFile(String uri) {
        String filePath = normalizeFilePath(uri);
        return filePath.endsWith(DEFAULT_BOOTSTRAP_PROPERTIES) || customBootstrapFiles.contains(filePath);
    }

    /**
     * Normalize and fix file path, starting from uri-formatted string.
     * - Converts to OS-specific filepaths (/ for unix, \ for windows)
     * - Handles URL encoding, Windows drive letter discrepancies
     * @param uri - URI-formatted string
     * @return - OS-specific filepath
     */
    public static String normalizeFilePath(String uri) {
        // make sure Windows backslashes are replaced with forwardslash for URI.create
        String normalizedUriString = uri.replace("\\","/");
        String finalPath = null;
        if (File.separator.equals("/")) { //unix
            Path path = Paths.get(URI.create(normalizedUriString));
            finalPath = path.toString();
        } else { // windows - URI.create with string instead of URI to handle drive letters. normalize drive letter
            String filepath = URI.create(normalizedUriString).getPath();
            if (filepath.charAt(0) == '/') {
                filepath = filepath.substring(1);
            }
            Path path = Paths.get(filepath);
            finalPath = path.toString();
            finalPath = normalizeDriveLetter(finalPath);
        }
        return finalPath;
    }
    
    /**
     * Necessary adjustment for Windows URI: 
     * https://github.com/Microsoft/vscode/issues/42159#issuecomment-360533151
     * Notes: LSP uses lowercase drive letters, but Windows file system uses uppercase drive letters
     */
    public static String normalizeDriveLetter(String path) {
        return path.substring(0, 1).toUpperCase() + path.substring(1);
    }

    /**
     * Search the dir path for all files that end with a name or extension. If none
     * are found,
     * an empty List is returned.
     * 
     * @param dir             Path to search under
     * @param nameOrExtension String to match
     * @return List<Path> collection of Path that match the given nameOrExtension in
     *         the specified dir Path.
     */
    protected static List<Path> findFilesEndsWithInDirectory(Path dir, String nameOrExtension) throws IOException {
        List<Path> matchingFiles = Files.walk(dir)
                .filter(p -> (Files.isRegularFile(p) && p.toFile().getName().endsWith(nameOrExtension)))
                .collect(Collectors.toList());

        return matchingFiles;
    }
}
