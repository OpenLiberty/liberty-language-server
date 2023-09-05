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

    public static void processWorkspaceDir(WorkspaceFolder workspaceFolder) {
        String workspaceUriString = workspaceFolder.getUri();
        String normalizedUriString = workspaceUriString.replace("///", "/");
        URI workspaceURI = URI.create(normalizedUriString);
        Path workspacePath = Paths.get(workspaceURI);

        try {
            List<Path> lpcXmlList = findFilesEndsWithInDirectory(workspacePath, LIBERTY_PLUGIN_CONFIG_XML);
            for (Path lpcXml : lpcXmlList) {
                processLibertyPluginConfigXml(lpcXml.toString());
            }
        } catch (IOException e) {
            LOGGER.warning("Encountered an IOException on initial custom config processing: " + e.getMessage());
        }
    }

    public static void processLibertyPluginConfigXml(String uri) {
        if (!uri.endsWith("liberty-plugin-config.xml")) {
            return;
        }
        Map<String, String> customConfigFiles = XmlReader.readTagsFromXml(uri,
                CUSTOM_SERVER_ENV_XML_TAG,
                CUSTOM_BOOTSTRAP_PROPERTIES_XML_TAG);
        // TODO: handle deletions. maybe use map with <uri, path> ? and clear all that
        // match uri
        if (customConfigFiles.containsKey(CUSTOM_SERVER_ENV_XML_TAG)) {
            customServerEnvFiles.add(customConfigFiles.get(CUSTOM_SERVER_ENV_XML_TAG));
        }
        if (customConfigFiles.containsKey(CUSTOM_BOOTSTRAP_PROPERTIES_XML_TAG)) {
            customBootstrapFiles.add(customConfigFiles.get(CUSTOM_BOOTSTRAP_PROPERTIES_XML_TAG));
        }
    }

    /**
     * Returns true if fileUri is a custom bootstrap.properties or server.env
     * defined in the build file (after processed into liberty-plugin-config.xml)
     * 
     * @param fileUri
     * @return
     */
    public static boolean isCustomConfigFile(String fileUri) {
        return customBootstrapFiles.contains(fileUri) || customServerEnvFiles.contains(fileUri);
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
     * @param uri
     * @return
     */
    public static boolean isServerEnvFile(String uri) {
        Path finalPath = Paths.get(URI.create(uri));
        return finalPath.endsWith("src/main/liberty/config/server.env") || customServerEnvFiles.contains(finalPath.toString());
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
     * @param uri
     * @return
     */
    public static boolean isBootstrapPropertiesFile(String uri) {
        Path finalPath = Paths.get(URI.create(uri));
        return finalPath.endsWith("src/main/liberty/config/bootstrap.properties") || customBootstrapFiles.contains(finalPath.toString());
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
