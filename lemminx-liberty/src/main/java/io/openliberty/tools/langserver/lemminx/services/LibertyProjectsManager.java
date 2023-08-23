/*******************************************************************************
* Copyright (c) 2020, 2023 IBM Corporation and others.
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
package io.openliberty.tools.langserver.lemminx.services;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.openliberty.tools.langserver.lemminx.util.LibertyUtils;

import org.eclipse.lsp4j.WorkspaceFolder;

public class LibertyProjectsManager {

    private static final Logger LOGGER = Logger.getLogger(LibertyProjectsManager.class.getName());

    private static final LibertyProjectsManager INSTANCE = new LibertyProjectsManager();

    private static final Set<String> SRC_AND_BUILD_DIRS = Stream.of("target", "build", "src").collect(Collectors.toCollection(HashSet::new));

    private Map<String, LibertyWorkspace> libertyWorkspaceFolders;

    public static LibertyProjectsManager getInstance() {
        return INSTANCE;
    }

    private LibertyProjectsManager() {
        libertyWorkspaceFolders = new HashMap<String,LibertyWorkspace>();
    }

    public void setWorkspaceFolders(List<WorkspaceFolder> workspaceFolders) {
        for (WorkspaceFolder folder : workspaceFolders) {
            // Add logic here to see if child folders are sub-modules in a multi-module project.
            // If child folder is a Liberty project (has any xml files with a <server> root element),
            // then create a LibertyWorkspace for it and do not create one for the parent folder.

            String workspaceUriString = folder.getUri();
            String normalizedUriString = workspaceUriString.replace("///", "/");

            if (this.libertyWorkspaceFolders.containsKey(normalizedUriString)) {
                LOGGER.info("Skipping already added Liberty workspace: " + normalizedUriString);
                continue;
            }

            URI workspaceUri =  URI.create(normalizedUriString);
            Path workspacePath = Paths.get(workspaceUri);

            // Added logic to find all *.xml files with <server> root elements instead of only considering the default src/main/liberty/config/server.xml file.
            List<Path> serverXmlFiles = null;

            try {
                serverXmlFiles = LibertyUtils.getXmlFilesWithServerRootInDirectory(workspacePath);
            } catch (IOException e) {
                LOGGER.warning("Received exception while searching for xml files with a <server> root element in: " + workspacePath + ": " + e.getMessage());
            }

            if ((serverXmlFiles == null) || serverXmlFiles.isEmpty() || (serverXmlFiles.size() == 1)) {
                LOGGER.info("Adding Liberty workspace: " + normalizedUriString);
                LibertyWorkspace libertyWorkspace = new LibertyWorkspace(normalizedUriString);
                this.libertyWorkspaceFolders.put(normalizedUriString, libertyWorkspace);
            } else {
                boolean addedSubModule = false;

                List<Path> childrenDirs = null;
                String lastChildDirPath = null;

                try {
                    childrenDirs = Files.walk(workspacePath, 1)
                                        .filter(Files::isDirectory)
                                        .collect(Collectors.toList());

                    boolean containsSrcOrBuildDir = LibertyUtils.containsDirectoryWithName(childrenDirs, SRC_AND_BUILD_DIRS);

                    for (Path nextChildDir : childrenDirs) {
                        lastChildDirPath = nextChildDir.toUri().toString().replace("///", "/");
                        if (nextChildDir.equals(workspacePath)) {
                            continue; // skip parent module
                        } else if (this.libertyWorkspaceFolders.containsKey(lastChildDirPath)) {
                            // this sub-module was already added but we still don't want to add the parent module
                            addedSubModule = true; 
                            continue;
                        }
                        // Do not add child dirs as sub-modules if there are src/target/build dirs as siblings. This is not a sub-module.
                        if (!containsSrcOrBuildDir) {
                            // Since we already found all server root xml files earlier, just check if any start with this path.
                            if (LibertyUtils.containsFileStartingWithRootPath(nextChildDir, serverXmlFiles)) {
                                LibertyWorkspace libertyWorkspace = new LibertyWorkspace(lastChildDirPath);
                                this.libertyWorkspaceFolders.put(lastChildDirPath, libertyWorkspace);
                                addedSubModule = true;
                                LOGGER.info("Adding Liberty workspace for sub-module: " + lastChildDirPath);
                            }
                        }
                    }
                } catch (IOException e) {
                    LOGGER.warning("Received exception while processing workspace folder: " + lastChildDirPath);
                }

                if (!addedSubModule) {
                    LibertyWorkspace libertyWorkspace = new LibertyWorkspace(normalizedUriString);
                    this.libertyWorkspaceFolders.put(normalizedUriString, libertyWorkspace);
                    LOGGER.info("Adding Liberty workspace by default: " + normalizedUriString);
                }
            }
        }
    }

    public Collection<LibertyWorkspace> getLibertyWorkspaceFolders() {
        return this.libertyWorkspaceFolders.values();
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
    public LibertyWorkspace getWorkspaceFolder(String serverXMLUri) {
        String normalizeUri = serverXMLUri.replace("///", "/");
        for (LibertyWorkspace folder : getInstance().getLibertyWorkspaceFolders()) {
            //Append workspaceString with file separator to avoid bad matches
            if (normalizeUri.contains(folder.getWorkspaceStringWithTrailingSlash())) {
                return folder;
            }
        }
        LOGGER.warning("Could not find LibertyWorkspace for file: " + serverXMLUri);
        return null;
    }

    public void cleanUpTempDirs() {
        for (LibertyWorkspace folder : getInstance().getLibertyWorkspaceFolders()) {
            // search for liberty ls directory
            URI workspaceFolderURI = folder.getWorkspaceURI();
            try {
                if (workspaceFolderURI != null) {
                    Path rootPath = Paths.get(workspaceFolderURI);
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
            } catch (IOException e) {
                LOGGER.warning("Could not clean up /.libertyls directory: " + e.getMessage());
            }
        }
    }

    public void cleanInstance() {
        libertyWorkspaceFolders = new HashMap<String, LibertyWorkspace>();
    }
}
