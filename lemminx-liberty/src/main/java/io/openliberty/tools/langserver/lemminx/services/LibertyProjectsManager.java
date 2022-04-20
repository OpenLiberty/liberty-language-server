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

import java.io.IOException;
import java.net.URI;
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
    public LibertyWorkspace getWorkspaceFolder(String serverXMLUri) {
        for (LibertyWorkspace folder : getInstance().getLibertyWorkspaceFolders()) {
            if (serverXMLUri.contains(folder.getWorkspaceString())) {
                return folder;
            }
        }
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

}
