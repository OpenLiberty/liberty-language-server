package io.openliberty.lemminx.liberty.services;

import java.util.ArrayList;
import java.util.List;

import io.openliberty.lemminx.liberty.models.feature.Feature;

public class LibertyWorkspace {

    private String workspaceFolderURI;
    private String libertyVersion;
    private boolean isLibertyInstalled;
    private List<Feature> installedFeatureList;

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
    }

    public String getURI() {
        return this.workspaceFolderURI;
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

    public void setInstalledFeatureList(List<Feature> installedFeatureList){
        this.installedFeatureList = installedFeatureList;
    }

}
