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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.lemminx.dom.DOMNode;

import java.util.concurrent.TimeUnit;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;


import io.openliberty.tools.langserver.lemminx.models.feature.Feature;
import io.openliberty.tools.langserver.lemminx.models.feature.FeatureInfo;
import io.openliberty.tools.langserver.lemminx.models.feature.WlpInformation;
import io.openliberty.tools.langserver.lemminx.util.LibertyConstants;
import io.openliberty.tools.langserver.lemminx.util.LibertyUtils;

public class FeatureService {

    private static final Logger LOGGER = Logger.getLogger(FeatureService.class.getName());

    // Singleton so that only 1 Feature Service can be initialized and is
    // shared between all Lemminx Language Feature Participants

    private static FeatureService instance;
    private static String olFeatureEndpoint = "https://repo1.maven.org/maven2/io/openliberty/features/features/%1$s/features-%1$s.json";
    private static String wlpFeatureEndpoint = "https://repo1.maven.org/maven2/com/ibm/websphere/appserver/features/features/%1$s/features-%1$s.json";

    public static FeatureService getInstance() {
        if (instance == null) {
            instance = new FeatureService();
        }
        return instance;
    }

    // Cache of Liberty version -> list of supported features
    private Map<String, List<Feature>> featureCache;   // the key consists of runtime-version, where runtime is 'ol' or 'wlp'
    private List<Feature> defaultFeatureList;
    private long featureUpdateTime;

    private FeatureService() {
        featureCache = new HashMap<>();
        featureUpdateTime = -1;
    }

    /**
     * Fetches information about Liberty features from Maven repo
     *
     * @param libertyVersion - version of Liberty to fetch features for
     * @return list of features supported by the provided version of Liberty
     */
    private List<Feature> fetchFeaturesForVersion(String libertyVersion, String libertyRuntime) throws IOException, JsonParseException {
        String featureEndpoint = libertyRuntime.equals("wlp") ? String.format(wlpFeatureEndpoint, libertyVersion) : 
                                                                String.format(olFeatureEndpoint, libertyVersion);

        InputStreamReader reader = new InputStreamReader(new URL(featureEndpoint).openStream());

        // Only need the public features
        List<Feature> publicFeatures = readPublicFeatures(reader);

        if (libertyRuntime.equals("wlp")) {
            // need to also get the OpenLiberty features and add them to the list to return
            List<Feature> olFeatures = fetchFeaturesForVersion(libertyVersion, "ol");
            publicFeatures.addAll(olFeatures);
        }

        LOGGER.info("Returning public features from Maven: " + publicFeatures.size());
        return publicFeatures;
    }

    /**
     * Returns the default feature list
     *
     * @return list of features supported by the default version of Liberty
     */
    private List<Feature> getDefaultFeatureList() {
        try {
            if (defaultFeatureList == null) {
                // Changing this to contain the version in the file name since the file is copied to the local .lemminx cache. 
                // This is how we ensure the latest default features json gets used in each developer environment. 
                InputStream is = getClass().getClassLoader().getResourceAsStream("features-cached-23006.json");
                InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);

                // Only need the public features
                defaultFeatureList = readPublicFeatures(reader);
            }
            LOGGER.info("Returning default feature list");
            return defaultFeatureList;

        } catch (JsonParseException e) {
            // unable to read json in resources file, return empty list
            LOGGER.severe("Error: Unable to get default features.");
            return defaultFeatureList;
        }
    }

    /**
     * Returns a list of public features found in the passed input stream. Does not affect the default feature list.
     *
     * @param reader - InputStreamReader for json feature list
     * @return list of public features
     */
    private ArrayList<Feature> readPublicFeatures(InputStreamReader reader) throws JsonParseException {
        Feature[] featureList = new Gson().fromJson(reader, Feature[].class);

        ArrayList<Feature> publicFeatures = new ArrayList<>();
        // Guard against null visibility field. Ran into this during manual testing of a wlp installation.
        Arrays.asList(featureList).stream()
            .filter(f -> f.getWlpInformation().getVisibility() != null && f.getWlpInformation().getVisibility().equals(LibertyConstants.PUBLIC_VISIBILITY))
            .forEach(publicFeatures::add);
        return publicFeatures;
    }

    /**
     * Returns the Liberty features corresponding to the Liberty version. First
     * attempts to fetch the feature list from Maven, otherwise falls back to the
     * list of installed features. If the installed features list cannot be
     * gathered, falls back to the default cached features json file.
     * 
     * @param libertyVersion Liberty version (corresponds to XML document)
     * @param libertyRuntime Liberty runtime (corresponds to XML document)
     * @param requestDelay Time to wait in between feature list requests to Maven
     * @param documentURI Liberty XML document
     * @return List of possible features
     */
    public List<Feature> getFeatures(String libertyVersion, String libertyRuntime, int requestDelay, String documentURI) {
        if (libertyRuntime == null || libertyVersion == null) {
            // return default feature list
            List<Feature> defaultFeatures = getDefaultFeatureList(); 
            return defaultFeatures;
        }

        String featureCacheKey = libertyRuntime + "-" + libertyVersion;

        // if the features are already cached in the feature cache
        if (featureCache.containsKey(featureCacheKey)) {
            LOGGER.info("Getting cached features for: " + featureCacheKey);
            return featureCache.get(featureCacheKey);
        }

        LOGGER.info("Getting features for: " + featureCacheKey);

        // if not a beta runtime, fetch features from maven central
        // - beta runtimes do not have a published features.json in mc
        if (!libertyVersion.endsWith("-beta")) {
            try {
                // verify that request delay (seconds) has gone by since last fetch request
                // Note that the default delay is 120 seconds and can cause us to generate a feature list instead of download from MC when
                // switching back and forth between projects.
                long currentTime = System.currentTimeMillis();
                if (this.featureUpdateTime == -1 || currentTime >= (this.featureUpdateTime + (requestDelay * 1000))) {
                    List<Feature> features = fetchFeaturesForVersion(libertyVersion, libertyRuntime);
                    featureCache.put(featureCacheKey, features);
                    this.featureUpdateTime = System.currentTimeMillis();
                    return features;
                }
            } catch (Exception e) {
                // do nothing, continue on to returning default feature list
                LOGGER.warning("Received exception when trying to download features from Maven Central: "+e.getMessage());
            }
        }

        // fetch installed features list - this would only happen if a features.json was not able to be downloaded from Maven Central
        // This is the case for beta runtimes and for very old runtimes pre 18.0.0.2 (or if within the requestDelay window of 120 seconds).
        List<Feature> installedFeatures = getInstalledFeaturesList(documentURI, libertyRuntime, libertyVersion);
        if (installedFeatures.size() != 0) {
            return installedFeatures;
        }

        // return default feature list
        List<Feature> defaultFeatures = getDefaultFeatureList(); 
        return defaultFeatures;
    }

    public Optional<Feature> getFeature(String featureName, String libertyVersion, String libertyRuntime, int requestDelay, String documentURI) {
        List<Feature> features = getFeatures(libertyVersion, libertyRuntime, requestDelay, documentURI);
        return features.stream().filter(f -> f.getWlpInformation().getShortName().equalsIgnoreCase(featureName))
            .findFirst();
    }

    public List<String> getFeatureShortNamesLowerCase(List<Feature> features) {
        List<String> featureShortNames = new ArrayList<String> ();

        for (Feature nexFeature: features) {
            featureShortNames.add(nexFeature.getWlpInformation().getShortName().toLowerCase());
        }

        return featureShortNames;
    }

    public boolean featureExists(String featureName, String libertyVersion, String libertyRuntime, int requestDelay, String documentURI) {
        return this.getFeature(featureName, libertyVersion, libertyRuntime, requestDelay, documentURI).isPresent();
    }

    public List<String> getFeatureReplacements(String featureName, DOMNode featureManagerNode, String libertyVersion, String libertyRuntime, int requestDelay, String documentURI) {
        List<Feature> features = getFeatures(libertyVersion, libertyRuntime, requestDelay, documentURI);
        List<String> featureNamesLowerCase = getFeatureShortNamesLowerCase(features);

        // get list of existing features to exclude from list of possible replacements
        List<String> existingFeatures = collectExistingFeatures(featureManagerNode, featureName);

        // also exclude any feature with a different version that matches an existing feature
        Set<String> featuresWithoutVersionsToExclude = new HashSet<String>();
        for (String nextFeatureName : featureNamesLowerCase) {
            if (existingFeatures.contains(nextFeatureName)) {
                // collect feature name minus version number to know which other features to exclude
                String featureNameMinusVersion = nextFeatureName.substring(0, nextFeatureName.lastIndexOf("-") + 1);
                featuresWithoutVersionsToExclude.add(featureNameMinusVersion);
            }
        }

        List<String> replacementFeatures = new ArrayList<String>();
        String featureNameLowerCase = featureName.toLowerCase();

        for (int i=0; i < featureNamesLowerCase.size(); i++) {
            String nextFeatureName = featureNamesLowerCase.get(i);
            if (nextFeatureName.contains(featureNameLowerCase) && (!nextFeatureName.contains("-") || 
                !featuresWithoutVersionsToExclude.contains(nextFeatureName.substring(0, nextFeatureName.lastIndexOf("-") + 1)))) {
                    replacementFeatures.add(features.get(i).getWlpInformation().getShortName()); // add the original feature shortName - not the lower case one
            }
        }

        return replacementFeatures;
    }

    /*
     * Returns the feature names specified in the featureManager element in lower case, excluding the currentFeatureName if specified.
     */
    public List<String> collectExistingFeatures(DOMNode featureManager, String currentFeatureName) {
        List<String> includedFeatures = new ArrayList<>();

        if (featureManager == null) {
            return includedFeatures;
        }

        List<DOMNode> features = featureManager.getChildren();
        for (DOMNode featureNode : features) {
            DOMNode featureTextNode = (DOMNode) featureNode.getChildNodes().item(0);
            // skip nodes that do not have any text value (ie. comments)
            if (featureNode.getNodeName().equals(LibertyConstants.FEATURE_ELEMENT) && featureTextNode != null) {
                String featureName = featureTextNode.getTextContent();
                String featureNameLowerCase = featureName.toLowerCase();
                if (currentFeatureName == null || (currentFeatureName != null && !featureNameLowerCase.equalsIgnoreCase(currentFeatureName))) {
                    includedFeatures.add(featureNameLowerCase);
                }
            }
        }
        return includedFeatures;
    }

    /**
     * Returns the list of installed features generated from ws-featurelist.jar.
     * Generated feature list is stored in the (target/build)/.libertyls directory.
     * Returns an empty list if cannot determine installed feature list.
     * 
     * @param documentURI xml document
     * @param libertyRuntime must not be null and should be either 'ol' or 'wlp'
     * @param libertyVersion must not be null and should be a valid Liberty version (e.g. 23.0.0.6)
     * @return list of installed features, or empty list
     */
    private List<Feature> getInstalledFeaturesList(String documentURI, String libertyRuntime, String libertyVersion) {
        List<Feature> installedFeatures = new ArrayList<Feature>();

        LibertyWorkspace libertyWorkspace = LibertyProjectsManager.getInstance().getWorkspaceFolder(documentURI);
        if (libertyWorkspace == null || libertyWorkspace.getWorkspaceString() == null) {
            return installedFeatures;
        }

        // return installed features from cache
        List<Feature> cachedFeatures = libertyWorkspace.getInstalledFeatureList();
        if (cachedFeatures.size() != 0) {
            LOGGER.info("Getting cached features from previously generated feature list: " + cachedFeatures.size());
            return cachedFeatures;
        }

        try {
            // Need to handle both local installation and container
            File featureListFile = null;

            if (libertyWorkspace.isLibertyInstalled()) {
                Path featureListJAR = LibertyUtils.findLibertyFileForWorkspace(libertyWorkspace, Paths.get("bin", "tools", "ws-featurelist.jar"));
                if (featureListJAR != null && featureListJAR.toFile().exists()) {
                    //Generate featurelist file
                    featureListFile = generateFeatureListXml(libertyWorkspace, featureListJAR);
                }
            } else if (libertyWorkspace.isContainerAlive()) {
                DockerService docker = DockerService.getInstance();
                featureListFile = docker.generateFeatureListFromContainer(libertyWorkspace);
            }

            if (featureListFile != null && featureListFile.exists()) {
                try {
                    installedFeatures = readFeaturesFromFeatureListFile(installedFeatures, libertyWorkspace, featureListFile);
                } catch (JAXBException e) {
                    LOGGER.severe("Error: Unable to load the generated feature list file for the target Liberty runtime due to exception: "+e.getMessage());
                }
            } else {
                LOGGER.warning("Unable to generate the feature list for the current Liberty workspace:" + libertyWorkspace.getWorkspaceString());
            }
        } catch (IOException e) {
            LOGGER.severe("Error: Unable to generate the feature list file from the target Liberty runtime due to exception: "+e.getMessage());
        }

        LOGGER.info("Returning installed features: " + installedFeatures.size());
        return installedFeatures;
    }

    /**
     * Generate the featurelist file for a LibertyWorkspace using the ws-featurelist.jar in the corresponding Liberty installation
     * @param libertyWorkspace
     * @param featurelistJarPath
     * @return File the generated featurelist file.
     */
    private File generateFeatureListXml(LibertyWorkspace libertyWorkspace, Path featurelistJarPath) {
        // java -jar {path to ws-featurelist.jar} {outputFile}
        File tempDir = LibertyUtils.getTempDir(libertyWorkspace);

        //If tempDir is null, issue a warning for the current LibertyWorkspace URI
        if (tempDir == null) {
            LOGGER.warning("Unable to generate the feature list for the current Liberty workspace:" + libertyWorkspace.getWorkspaceString());
            return null;
        }

        File featureListFile = new File(tempDir, "featurelist.xml");
        if (libertyWorkspace.getLibertyVersion()!= null && !libertyWorkspace.getLibertyVersion().isEmpty() &&
                libertyWorkspace.getLibertyRuntime()!= null && !libertyWorkspace.getLibertyRuntime().isEmpty()) {
            featureListFile = new File(tempDir, "featurelist-" + libertyWorkspace.getLibertyRuntime() + "-" + libertyWorkspace.getLibertyVersion() + ".xml");
        }

        try {
            LOGGER.info("Generating feature list file from: " + featurelistJarPath.toString());
            String xmlDestPath = featureListFile.getCanonicalPath();

            LOGGER.info("Generating feature list file at: " + xmlDestPath);

            ProcessBuilder pb = new ProcessBuilder("java", "-jar", featurelistJarPath.toAbsolutePath().toString(), xmlDestPath);
            pb.directory(tempDir);
            pb.redirectErrorStream(true);
            pb.redirectOutput(new File(tempDir, "ws-featurelist.log"));

            Process proc = pb.start();
            if (!proc.waitFor(30, TimeUnit.SECONDS)) {
                proc.destroy();
                LOGGER.warning("Exceeded 30 second timeout during feature list generation. Using cached features json file.");
                return null;
            }

        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
            LOGGER.warning("Due to an exception during feature list file generation, a cached features json file will be used.");
            return null;
        }

        LOGGER.info("Using feature list file at: " + featureListFile.toURI().toString());
        return featureListFile;
    }

    public List<Feature> readFeaturesFromFeatureListFile(List<Feature> installedFeatures, LibertyWorkspace libertyWorkspace,
        File featureListFile) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(FeatureInfo.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        FeatureInfo featureInfo = (FeatureInfo) jaxbUnmarshaller.unmarshal(featureListFile);
        
        // Note: Only the public features are loaded when unmarshalling the passed featureListFile.
        if ((featureInfo.getFeatures() != null) && (featureInfo.getFeatures().size() > 0)) {
            for (int i = 0; i < featureInfo.getFeatures().size(); i++) {
                Feature f = featureInfo.getFeatures().get(i);
                f.setShortDescription(f.getDescription());
                // The xml featureListFile does not have a wlpInformation element like the json does, but our code depends on looking up 
                // features by the shortName found in wlpInformation. So create a WlpInformation object and initialize the shortName to 
                // the feature name.
                WlpInformation wlpInfo = new WlpInformation(f.getName());
                f.setWlpInformation(wlpInfo);
            }
            installedFeatures = featureInfo.getFeatures();
            libertyWorkspace.setInstalledFeatureList(installedFeatures);
        } else {
            LOGGER.warning("Unable to get installed features for current Liberty workspace: " + libertyWorkspace.getWorkspaceString());
        }
        return installedFeatures;
    }

}
