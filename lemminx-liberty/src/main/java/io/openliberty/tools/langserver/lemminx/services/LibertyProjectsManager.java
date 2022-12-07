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
package io.openliberty.tools.langserver.lemminx.services;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.openliberty.tools.langserver.lemminx.util.LibertyUtils;

import org.eclipse.lsp4j.WorkspaceFolder;

public class LibertyProjectsManager {

    private static final Logger LOGGER = Logger.getLogger(LibertyProjectsManager.class.getName());

    private static final LibertyProjectsManager INSTANCE = new LibertyProjectsManager();

    private static final String URI_SEPARATOR = "/";

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
            // If child folder is a Liberty project (has src/main/liberty/config/server.xml),
            // then create a LibertyWorkspace for it and do not create one for the parent folder.

            String workspaceUriString = folder.getUri();
            String normalizedUriString = workspaceUriString.replace("///", "/");

            if (this.libertyWorkspaceFolders.containsKey(normalizedUriString)) {
                LOGGER.info("Skipping already added Liberty workspace: " + normalizedUriString);
                continue;
            }

            URI workspaceUri =  URI.create(normalizedUriString);
            Path workspacePath = Paths.get(workspaceUri);
            Path serverXmlPath = Paths.get("src", "main", "liberty", "config", "server.xml");
            List<Path> serverXmlFiles = null;

            try {
                serverXmlFiles = LibertyUtils.findFilesInDirectory(workspacePath, serverXmlPath);
            } catch (IOException e) {
                LOGGER.warning("Received exception while searching for server.xml files in: " + workspacePath + " exception: " + e.getMessage());
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

                    for (Path nextChildDir : childrenDirs) {
                        lastChildDirPath = nextChildDir.toUri().toString().replace("///", "/");
                        if (nextChildDir.equals(workspacePath) || this.libertyWorkspaceFolders.containsKey(lastChildDirPath)) {
                            continue;
                        }
                        List<Path> serverXmlFile = LibertyUtils.findFilesInDirectory(nextChildDir, serverXmlPath);
                        if (!serverXmlFile.isEmpty()) {
                            LibertyWorkspace libertyWorkspace = new LibertyWorkspace(lastChildDirPath);
                            this.libertyWorkspaceFolders.put(lastChildDirPath, libertyWorkspace);
                            addedSubModule = true;
                            LOGGER.info("Adding Liberty workspace for sub-module: " + lastChildDirPath);
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
            //Append workspaceDirUri with file separator to avoid bad matches
            String workspaceDirUri = folder.getWorkspaceString();
            if (workspaceDirUri != null && !workspaceDirUri.endsWith(URI_SEPARATOR)) {
                workspaceDirUri = workspaceDirUri + URI_SEPARATOR;
            }

            if (normalizeUri.contains(workspaceDirUri)) {
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
