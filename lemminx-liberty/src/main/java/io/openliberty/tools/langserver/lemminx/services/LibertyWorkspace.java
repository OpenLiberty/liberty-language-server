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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import io.openliberty.tools.langserver.lemminx.models.feature.Feature;
import io.openliberty.tools.langserver.lemminx.models.settings.DevcMetadata;

public class LibertyWorkspace {

    private static final Logger LOGGER = Logger.getLogger(LibertyWorkspace.class.getName());

    public static final String URI_SEPARATOR = "/";

    private String workspaceFolderURI;
    private String libertyVersion;
    private String libertyRuntime;
    private boolean isLibertyInstalled;
    private List<Feature> installedFeatureList;
    private String libertyInstallationDir;
    private boolean isExternalLibertyInstallation;

    // devc vars
    private String containerName;
    private boolean containerAlive;

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
        this.libertyRuntime = null;
        this.isLibertyInstalled = false;
        this.isExternalLibertyInstallation = false;
        this.libertyInstallationDir = null;
        this.installedFeatureList = new ArrayList<Feature>();
        this.containerName = null;
        this.containerAlive = false;
    }

    public String getWorkspaceString() {
        return this.workspaceFolderURI;
    }

    public String getWorkspaceStringWithTrailingSlash() {
        if (workspaceFolderURI != null && !workspaceFolderURI.endsWith(URI_SEPARATOR)) {
            return workspaceFolderURI + URI_SEPARATOR;
        }
        return this.workspaceFolderURI;
    }

    public URI getWorkspaceURI() {
        return URI.create(this.workspaceFolderURI);
    }

    public File getDir() {
        return new File(URI.create(getWorkspaceString()).getPath());
    }

    public void setLibertyVersion(String libertyVersion) {
        this.libertyVersion = libertyVersion;
    }

    public String getLibertyVersion() {
        return this.libertyVersion;
    }

    public void setLibertyRuntime(String libertyRuntime) {
        this.libertyRuntime = libertyRuntime;
    }

    public String getLibertyRuntime() {
        return libertyRuntime;
    }

    public void setLibertyInstalled(boolean isLibertyInstalled) {
        this.isLibertyInstalled = isLibertyInstalled;
        if (!isLibertyInstalled) {
            setExternalLibertyInstallation(isLibertyInstalled);
            // clear the cached feature list when Liberty is no longer installed
            this.installedFeatureList = new ArrayList<Feature>();
        }
    }

    public boolean isLibertyInstalled() {
        return this.isLibertyInstalled;
    }

    public void setExternalLibertyInstallation(boolean flag) {
        this.isExternalLibertyInstallation = flag;
    }

    public boolean isExternalLibertyInstallation() {
        return this.isExternalLibertyInstallation;
    }

    public void setLibertyInstallationDir(String dir) {
        this.libertyInstallationDir = dir;
    }

    public String getLibertyInstallationDir() {
        return this.libertyInstallationDir;
    }

    public List<Feature> getInstalledFeatureList() {
        return this.installedFeatureList;
    }

    public void setInstalledFeatureList(List<Feature> installedFeatureList) {
        this.installedFeatureList = installedFeatureList;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public boolean isContainerAlive() {
        return this.containerAlive;
    }

    public void setContainerAlive(boolean containerAlive) {
        this.containerAlive = containerAlive;
    }

    /**
     * Return the path of the first *-liberty-devc-metadata.xml 
     * in the workspace with a running container
     * @return Path of *-liberty-devc-metadata.xml
     */
    public Path findDevcMetadata() {
        try {
            List<Path> metaDataList = Files
                    .find(Paths.get(getWorkspaceURI()), Integer.MAX_VALUE,
                            (filePath, fileAttributes) -> filePath.toString().endsWith("-liberty-devc-metadata.xml"))
                    .collect(Collectors.toList());
            for (Path metaDataFile : metaDataList) {
                DevcMetadata devcMetadata = unmarshalDevcMetadataFile(metaDataFile);
                if (devcMetadata.isContainerAlive()) {
                    setContainerName(devcMetadata.getContainerName());
                    setContainerAlive(true);
                    return metaDataFile;
                }
            }
            setContainerAlive(false);
            return null;
        } catch (IOException e) {
            // workspace URI does not exist
            LOGGER.warning("Workspace URI does not exist: " + e.getMessage());
            return null;
        }
    }

    /**
     * Helper method to unmarshal/read the provided liberty-devc-metadata file.
     * @param devcMetadataFile
     * @return DevcMetadata object
     */
    public static DevcMetadata unmarshalDevcMetadataFile(Path devcMetadataFile) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(DevcMetadata.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            return (DevcMetadata)jaxbUnmarshaller.unmarshal(devcMetadataFile.toFile());
        } catch (JAXBException e) {
            // LOGGER.warning("Unable to unmarshal the devc metadata file: " + devcMetadataFile.toString());
            return null;
        }
    }

    @Override
    public String toString() {
        return workspaceFolderURI;
    }

}
