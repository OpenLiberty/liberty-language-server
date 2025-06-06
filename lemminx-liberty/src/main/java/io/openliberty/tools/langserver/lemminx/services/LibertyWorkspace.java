/*******************************************************************************
* Copyright (c) 2020, 2025 IBM Corporation and others.
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
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import io.openliberty.tools.langserver.lemminx.data.FeatureListGraph;
import io.openliberty.tools.langserver.lemminx.models.feature.Feature;
import io.openliberty.tools.langserver.lemminx.models.feature.FeaturesAndPlatforms;
import io.openliberty.tools.langserver.lemminx.models.settings.DevcMetadata;

public class LibertyWorkspace {

    private static final Logger LOGGER = Logger.getLogger(LibertyWorkspace.class.getName());

    public static final String URI_SEPARATOR = "/";

    private String workspaceFolderURI;
    private String libertyVersion;
    private String libertyRuntime;
    private boolean isLibertyInstalled;
    private FeaturesAndPlatforms installedFeaturesAndPlatformsList;
    private String libertyInstallationDir;
    private FeatureListGraph featureListGraph;

    // devc vars
    private String containerName;
    private String containerType;
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
        this.libertyInstallationDir = null;
        this.installedFeaturesAndPlatformsList = new FeaturesAndPlatforms();
        this.containerName = null;
        this.containerType = "docker";
        this.containerAlive = false;
        this.featureListGraph = new FeatureListGraph();
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
            // do not clear out the libertyRuntime or libertyVersion since those could be set for a live container
            setLibertyInstallationDir(null);
            // clear the cached feature list when Liberty is no longer installed
            this.installedFeaturesAndPlatformsList = new FeaturesAndPlatforms();
        }
    }

    public boolean isLibertyInstalled() {
        return this.isLibertyInstalled;
    }

    public void setLibertyInstallationDir(String dir) {
        this.libertyInstallationDir = dir;
    }

    public String getLibertyInstallationDir() {
        return this.libertyInstallationDir;
    }

    public FeaturesAndPlatforms getInstalledFeaturesAndPlatformsList() {
        return this.installedFeaturesAndPlatformsList;
    }

    public void setInstalledFeaturesAndPlatformsList(FeaturesAndPlatforms installedFeaturesAndPlatforms) {
        this.installedFeaturesAndPlatformsList = installedFeaturesAndPlatforms;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public String getContainerType() {
        return containerType;
    }

    public void setContainerType(String containerType) {
        this.containerType = containerType != null && !containerType.isEmpty() ? containerType : "docker";
    }

    public boolean isContainerAlive() {
        return this.containerAlive;
    }

    public void setContainerAlive(boolean containerAlive) {
        this.containerAlive = containerAlive;
    }

    public boolean isLibertyRuntimeAndVersionSet() {
        return getLibertyVersion()!= null && !getLibertyVersion().isEmpty() &&
        getLibertyRuntime()!= null && !getLibertyRuntime().isEmpty();
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
                    setContainerType(devcMetadata.getContainerType());
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

    public String getWorkspaceRuntime() {
        if (libertyRuntime == null || libertyVersion == null) {
            return "";
        }
        return libertyRuntime + "-" + libertyVersion;
    }

    public void setFeatureListGraph(FeatureListGraph featureListGraph) {
        this.featureListGraph = featureListGraph;
        if (isLibertyInstalled || isContainerAlive()) {
            this.featureListGraph.setRuntime(libertyRuntime + "-" + libertyVersion);
        } else {
            this.featureListGraph.setRuntime("");
        }
    }

    public FeatureListGraph getFeatureListGraph() {
        FeatureListGraph useFeatureListGraph = this.featureListGraph;
        boolean generateGraph = featureListGraph.isEmpty() || !featureListGraph.getRuntime().equals(getWorkspaceRuntime());

        if (!generateGraph && (isLibertyInstalled || isContainerAlive())) {
            // Check if FeatureListGraph needs to be reinitialized. This can happen if new features are installed.
            // The contents of the .libertyls folder are deleted when features are installed, which means we need to 
            // regenerate the feature list xml and load the FeatureListGraph.
            if (!FeatureService.getInstance().doesGeneratedFeatureListExist(this)) {
                generateGraph = true;
                // clear out cached feature list
                this.installedFeaturesAndPlatformsList = new FeaturesAndPlatforms();

            }
        }

        if (generateGraph) {
            if (isLibertyInstalled || isContainerAlive()) {
                LOGGER.info("Generating installed features list and storing to cache for workspace " + workspaceFolderURI);
                FeatureService.getInstance().getInstalledFeaturesAndPlatformsList(this, libertyRuntime, libertyVersion);
                useFeatureListGraph = this.featureListGraph;
            } else {
                 LOGGER.info("Retrieving default cached feature list for workspace " + workspaceFolderURI);
                 useFeatureListGraph = FeatureService.getInstance().getDefaultFeatureList();
            }
            if (!useFeatureListGraph.isEmpty()) {
                LOGGER.info("Config element validation for missing features enabled for workspace: " + workspaceFolderURI);
            }
        }
        return useFeatureListGraph;
    }

}
