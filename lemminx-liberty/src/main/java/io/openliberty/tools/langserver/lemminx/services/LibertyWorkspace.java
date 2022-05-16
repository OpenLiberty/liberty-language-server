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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.openliberty.tools.langserver.lemminx.models.feature.Feature;
import io.openliberty.tools.langserver.lemminx.util.LibertyUtils;

public class LibertyWorkspace {

    private String workspaceFolderURI;
    private String libertyVersion;
    private boolean isLibertyInstalled;
    private List<Feature> installedFeatureList;
    private Set<String> configFiles;

    /**
     * Model of a Liberty Workspace. Each workspace indicates the
     * workspaceFolderURI, the Liberty version associated (may be cached), and if an
     * installed Liberty instance has been detected.
     * 
     * @param workspaceFolderURI
     */
    public LibertyWorkspace(String workspaceFolderURI) {
        this.workspaceFolderURI = workspaceFolderURI;
        this.libertyVersion = null;
        this.isLibertyInstalled = false;
        this.installedFeatureList = new ArrayList<Feature>();

        this.configFiles = new HashSet<String>();
        initConfigFileList();
    }

    public String getWorkspaceString() {
        return this.workspaceFolderURI;
    }
    
    public URI getWorkspaceURI() {
        return URI.create(this.workspaceFolderURI);
    }

    public void setLibertyVersion(String libertyVersion) {
        this.libertyVersion = libertyVersion;
    }

    public String getLibertyVersion() {
        return this.libertyVersion;
    }

    public void setLibertyInstalled(boolean isLibertyInstalled) {
        this.isLibertyInstalled = isLibertyInstalled;
    }

    public boolean isLibertyInstalled() {
        return this.isLibertyInstalled;
    }

    public List<Feature> getInstalledFeatureList() {
        return this.installedFeatureList;
    }

    public void setInstalledFeatureList(List<Feature> installedFeatureList) {
        this.installedFeatureList = installedFeatureList;
    }

    public void initConfigFileList() {
        try {
            List<Path> serverXmlList = Files.find(Paths.get(getWorkspaceURI()), Integer.MAX_VALUE, (filePath, fileAttributes) -> 
                    LibertyUtils.isServerXMLFile(filePath.toString()))
                    .collect(Collectors.toList());
            for (Path serverXml : serverXmlList) {
                scanForConfigLocations(serverXml);
            }
        } catch (IOException e) {
            // workspace URI does not exist
            e.printStackTrace();
        }
    }

    // TODO: or use DOM
    public void scanForConfigLocations(Path filePath) {
        try {
            String content = new String(Files.readAllBytes(filePath));
            // [^<] = All characters, including whitespaces/newlines, not equivalent to '<'
            // [\"\'] = Accept either " or ' as string indicators
            // [\\s\\S]+? = Anything up to first closing tag
            String regex = "<include[^<]+location=[\"\'](.+)[\"\'][\\s\\S]+?/>";
            Matcher m = Pattern.compile(regex).matcher(content);
            while (m.find()) {
                // m.group(0) contains whole include element, m.group(1) contains only location value
                configFiles.add(new File(filePath.getParent().toFile(), m.group(1)).getCanonicalPath());
            }
        } catch (IOException e) {
            // specified config resources file does not exist
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addConfigFile(String fileString) {
        configFiles.add(fileString);
    }

    public boolean hasConfigFile(String fileString) {
        try {
            fileString = fileString.startsWith("file:") ? 
                    new File(URI.create(fileString)).getCanonicalPath() : 
                    new File(fileString).getCanonicalPath();
            return this.configFiles.contains(fileString);
        } catch (IOException e) {
            return false;
        }
    }
}
