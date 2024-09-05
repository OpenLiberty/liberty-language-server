/*******************************************************************************
* Copyright (c) 2020, 2024 IBM Corporation and others.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import io.openliberty.tools.langserver.lemminx.models.feature.FeatureTolerate;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.uriresolver.CacheResourcesManager;
import org.eclipse.lemminx.uriresolver.CacheResourcesManager.ResourceToDeploy;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import io.openliberty.tools.langserver.lemminx.data.ConfigElementNode;
import io.openliberty.tools.langserver.lemminx.data.FeatureListGraph;
import io.openliberty.tools.langserver.lemminx.data.FeatureListNode;
import io.openliberty.tools.langserver.lemminx.models.feature.Feature;
import io.openliberty.tools.langserver.lemminx.models.feature.FeatureInfo;
import io.openliberty.tools.langserver.lemminx.models.feature.WlpInformation;
import io.openliberty.tools.langserver.lemminx.util.LibertyConstants;
import io.openliberty.tools.langserver.lemminx.util.LibertyUtils;

public class FeatureService {

    private static final Logger LOGGER = Logger.getLogger(FeatureService.class.getName());
    public static final String IO_OPENLIBERTY_INTERNAL_VERSIONLESS = "io.openliberty.internal.versionless.";

    // Singleton so that only 1 Feature Service can be initialized and is
    // shared between all Lemminx Language Feature Participants

    private static FeatureService instance;
    private static String olFeatureEndpoint = "https://repo1.maven.org/maven2/io/openliberty/features/features/%1$s/features-%1$s.json";
    private static String wlpFeatureEndpoint = "https://repo1.maven.org/maven2/com/ibm/websphere/appserver/features/features/%1$s/features-%1$s.json";

    // This file is copied to the local .lemminx cache. 
    // This is how we ensure the latest default featurelist xml gets used in each developer environment. 
    private static final String FEATURELIST_XML_RESOURCE_URL = "https://github.com/OpenLiberty/liberty-language-server/blob/master/lemminx-liberty/src/main/resources/featurelist-cached-24.0.0.8.xml";
    private static final String FEATURELIST_XML_CLASSPATH_LOCATION = "/featurelist-cached-24.0.0.8.xml";

    /**
     * FEATURELIST_XML_RESOURCE is the featurelist xml that is located at FEATURELIST_XML_CLASSPATH_LOCATION
     * that gets deployed (copied) to the .lemminx cache. The FEATURELIST_XML_RESOURCE_URL is
     * used by lemmix to determine the path to store the file in the cache. So for the
     * featurelist xml it takes the resource located at FEATURELIST_XML_CLASSPATH_LOCATION and deploys
     * it to:
     * ~/.lemminx/cache/https/github.com/OpenLiberty/liberty-language-server/master/lemminx-liberty/featurelist-cached-<version>.xml
     * 
     * Declared public to be used by tests
     */
    public static final ResourceToDeploy FEATURELIST_XML_RESOURCE = new ResourceToDeploy(FEATURELIST_XML_RESOURCE_URL,
            FEATURELIST_XML_CLASSPATH_LOCATION);

    public static FeatureService getInstance() {
        if (instance == null) {
            instance = new FeatureService();
        }
        return instance;
    }

    // Cache of Liberty version -> list of supported features
    private Map<String, List<Feature>> featureCache;   // the key consists of runtime-version, where runtime is 'ol' or 'wlp'
    private List<Feature> defaultFeatures;
    private FeatureListGraph defaultFeatureList;
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
     * Returns the default list of features
     *
     * @return list of features supported by the default version of Liberty
     */
    private List<Feature> getDefaultFeatures() {
        try {
            if (defaultFeatures == null) {
                // Changing this to contain the version in the file name since the file is copied to the local .lemminx cache. 
                // This is how we ensure the latest default features json gets used in each developer environment. 
                InputStream is = getClass().getClassLoader().getResourceAsStream("features-cached-24.0.0.8.json");
                InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);

                // Only need the public features
                defaultFeatures = readPublicFeatures(reader);
            }
            LOGGER.info("Returning default list of features");
            return defaultFeatures;

        } catch (JsonParseException e) {
            // unable to read json in resources file, return empty list
            LOGGER.severe("Error: Unable to get default features.");
            return defaultFeatures;
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
     * attempts to fetch the feature json from Maven, otherwise falls back to the
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
            // return default list of features
            List<Feature> defaultFeatures = getDefaultFeatures(); 
            getDefaultFeatureList();
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
                // Note that the default delay is 10 seconds and can cause us to generate a feature list instead of download from MC when
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

        // return default list of features
        List<Feature> defaultFeatures = getDefaultFeatures(); 
        return defaultFeatures;
    }

    public Optional<Feature> getFeature(String featureName, String libertyVersion, String libertyRuntime, int requestDelay, String documentURI) {
        List<Feature> features = getFeatures(libertyVersion, libertyRuntime, requestDelay, documentURI);
        return features.stream().filter(f -> f.getWlpInformation().getShortName().equalsIgnoreCase(featureName))
            .findFirst();
    }

    public List<String> getFeatureShortNames(List<Feature> features) {
        return getFeatureShortNames(features, false);
    }

    public List<String> getFeatureShortNames(List<Feature> features, boolean lowerCase) {
        List<String> featureShortNames = new ArrayList<String> ();

        for (Feature nextFeature: features) {
            featureShortNames.add(lowerCase ? nextFeature.getWlpInformation().getShortName().toLowerCase() : nextFeature.getWlpInformation().getShortName());
        }

        return featureShortNames;
    }

    public boolean featureExists(String featureName, String libertyVersion, String libertyRuntime, int requestDelay, String documentURI) {
        return this.getFeature(featureName, libertyVersion, libertyRuntime, requestDelay, documentURI).isPresent();
    }

    public List<Feature> getFeatureReplacements(String featureName, DOMNode featureManagerNode, String libertyVersion, String libertyRuntime, int requestDelay, String documentURI) {
        List<Feature> features = getFeatures(libertyVersion, libertyRuntime, requestDelay, documentURI);
        List<String> featureNamesLowerCase = getFeatureShortNames(features, true);

        // get list of existing features to exclude from list of possible replacements
        List<String> existingFeatures = collectExistingFeatures(featureManagerNode, featureName);

        // also exclude any feature with a different version that matches an existing feature
        Set<String> featuresWithoutVersionsToExclude = new HashSet<String>();
        for (String nextFeatureName : featureNamesLowerCase) {
            if (existingFeatures.contains(nextFeatureName)) {
                // collect feature name minus version number to know which other features to exclude
                String featureNameMinusVersion = nextFeatureName.contains("-") ?
                        nextFeatureName.substring(0, nextFeatureName.lastIndexOf("-") + 1) : nextFeatureName + "-";
                featuresWithoutVersionsToExclude.add(featureNameMinusVersion);
            }
        }

        List<Feature> replacementFeatures = new ArrayList<Feature>();
        String featureNameLowerCase = featureName.toLowerCase();

        for (int i = 0; i < featureNamesLowerCase.size(); i++) {
            String nextFeatureName = featureNamesLowerCase.get(i);
            if (nextFeatureName.contains(featureNameLowerCase)) {
                String comparingFeatureName = nextFeatureName.contains("-") ?
                        nextFeatureName.substring(0, nextFeatureName.lastIndexOf("-") + 1) : nextFeatureName + "-";
                if (!featuresWithoutVersionsToExclude.contains(comparingFeatureName)) {
                    replacementFeatures.add(features.get(i));
                }
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
    public List<Feature> getInstalledFeaturesList(LibertyWorkspace libertyWorkspace, String libertyRuntime, String libertyVersion) {
        List<Feature> installedFeatures = new ArrayList<Feature>();
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
                ContainerService container = ContainerService.getInstance();
                featureListFile = container.generateFeatureListFromContainer(libertyWorkspace);
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
    
    public List<Feature> getInstalledFeaturesList(String documentURI, String libertyRuntime, String libertyVersion) {
        LibertyWorkspace libertyWorkspace = LibertyProjectsManager.getInstance().getWorkspaceFolder(documentURI);
        return getInstalledFeaturesList(libertyWorkspace, libertyRuntime, libertyVersion);
    }

    public FeatureListGraph getDefaultFeatureList() {
        if (defaultFeatureList != null) {
            return defaultFeatureList;
        }

        try {
            Path featurelistXmlFile = CacheResourcesManager.getResourceCachePath(FEATURELIST_XML_RESOURCE);
            LOGGER.info("Using cached Liberty featurelist xml file located at: " + featurelistXmlFile.toString());

            File featureListFile = featurelistXmlFile.toFile();

            if (featureListFile != null && featureListFile.exists()) {
                try {
                    readFeaturesFromFeatureListFile(null, null, featureListFile, true);
                } catch (JAXBException e) {
                    LOGGER.severe("Error: Unable to load the default cached featurelist file due to exception: "+e.getMessage());
                }
            } else {
                LOGGER.warning("Unable to find the default cached featurelist at location: "+featurelistXmlFile.toString());
            }
        } catch (Exception e) {
            LOGGER.severe("Error: Unable to retrieve default cached featurelist file due to exception: "+e.getMessage());
        }

        if (defaultFeatureList == null) {
            defaultFeatureList = new FeatureListGraph();
        }

        return defaultFeatureList;
    }

    public boolean doesGeneratedFeatureListExist(LibertyWorkspace libertyWorkspace) {
        File tempDir = LibertyUtils.getTempDir(libertyWorkspace);

        //If tempDir is null, issue a warning for the current LibertyWorkspace URI
        if (tempDir == null) {
            LOGGER.warning("Unable to locate the feature list for the current Liberty workspace:" + libertyWorkspace.getWorkspaceString());
            return false;
        }

        File featureListFile = getGeneratedFeatureListFileLocation(libertyWorkspace, tempDir);

        return featureListFile.exists();
    }

    public File getGeneratedFeatureListFileLocation(LibertyWorkspace libertyWorkspace, File tempDir) {
        File featureListFile = new File(tempDir, "featurelist.xml");
        if (libertyWorkspace.isLibertyRuntimeAndVersionSet()) {
            featureListFile = new File(tempDir, "featurelist-" + libertyWorkspace.getLibertyRuntime() + "-" + libertyWorkspace.getLibertyVersion() + ".xml");
        }

        return featureListFile;
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

        File featureListFile = getGeneratedFeatureListFileLocation(libertyWorkspace, tempDir);

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
            return readFeaturesFromFeatureListFile(installedFeatures, libertyWorkspace, featureListFile, false);
    }

    // If the graphOnly boolean is true, the libertyWorkspace parameter may be null. Also, the defaultFeatureList should be initialized
    // after calling this method with graphOnly set to true.
    public List<Feature> readFeaturesFromFeatureListFile(List<Feature> installedFeatures, LibertyWorkspace libertyWorkspace,
        File featureListFile, boolean graphOnly) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(FeatureInfo.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        FeatureInfo featureInfo = (FeatureInfo) jaxbUnmarshaller.unmarshal(featureListFile);
        FeatureListGraph featureListGraph = new FeatureListGraph();
        
        // Note: Only the public features are loaded when unmarshalling the passed featureListFile.
        if ((featureInfo.getFeatures() != null) && (featureInfo.getFeatures().size() > 0)) {
            for (Feature f : featureInfo.getFeatures()) {
                f.setShortDescription(f.getDescription());
                // The xml featureListFile does not have a wlpInformation element like the json does, but our code depends on looking up 
                // features by the shortName found in wlpInformation. So create a WlpInformation object and initialize the shortName to 
                // the feature name.
                WlpInformation wlpInfo = new WlpInformation(f.getName());
                f.setWlpInformation(wlpInfo);

                String currentFeature = f.getName();            
                List<String> enables = f.getEnables();
                List<String> configElements = f.getConfigElements();
                FeatureListNode currentFeatureNode = featureListGraph.addFeature(currentFeature, f.getDescription());
                if (enables != null) {
                    for (String enabledFeature : enables) {
                        FeatureListNode feature = featureListGraph.addFeature(enabledFeature);
                        feature.addEnabledBy(currentFeature);
                        currentFeatureNode.addEnablesFeature(enabledFeature);
                    }
                }
                if (configElements != null) {
                    for (String configElement : configElements) {
                        ConfigElementNode configNode = featureListGraph.addConfigElement(configElement);
                        configNode.addEnabledBy(currentFeature);
                        currentFeatureNode.addEnablesConfigElement(configElement);
                    }
                }
            }

            if (!graphOnly) {
                installedFeatures = featureInfo.getFeatures();
                libertyWorkspace.setInstalledFeatureList(installedFeatures);
                libertyWorkspace.setFeatureListGraph(featureListGraph);
            } else {
                defaultFeatureList = featureListGraph;
            }
        } else {
            if (!graphOnly) {
                LOGGER.warning("Unable to get installed features for current Liberty workspace: " + libertyWorkspace.getWorkspaceString());
                libertyWorkspace.setFeatureListGraph(new FeatureListGraph());
            } else {
                defaultFeatureList = featureListGraph;
            }
        }
        return installedFeatures;
    }

    /**
     * get all platforms for all features from feature json
     * @param libertyVersion liberty version
     * @param libertyRuntime runtime
     * @param requestDelay request delay
     * @param documentURI document uri link
     * @return set of unique platforms
     */
    public Set<String> getAllPlatforms(String libertyVersion, String libertyRuntime, int requestDelay, String documentURI) {
        List<Feature> features = this.getFeatures(libertyVersion, libertyRuntime, requestDelay, documentURI);
        return features.stream()
                .map(Feature::getWlpInformation)
                .filter(Objects::nonNull)
                .map(WlpInformation::getPlatforms)
                .filter(Objects::nonNull)
                .flatMap(List::stream).collect(Collectors.toSet());
    }

    /**
     * check platform name exists or not
     * @param platformName platform name
     * @param libertyVersion liberty version
     * @param libertyRuntime liberty runtime
     * @param requestDelay request delay
     * @param documentURI xml document uri
     * @return true or false
     */
    public boolean platformExists(String platformName, String libertyVersion, String libertyRuntime, int requestDelay, String documentURI) {
        Set<String> platforms = this.getAllPlatforms(libertyVersion, libertyRuntime, requestDelay, documentURI);
        return platforms.stream()
                .anyMatch(platform -> platform.equalsIgnoreCase(platformName));
    }


    /**
     * return all allowed platforms for a feature
     * @param featureName feature shortname
     * @param libertyVersion liberty version
     * @param libertyRuntime runtime
     * @param requestDelay request delay
     * @param documentURI document uri
     * @return set of platforms
     */
    public Set<String> getAllPlatformsForFeature(String featureName, String libertyVersion, String libertyRuntime,
                                                     int requestDelay, String documentURI) {
        Optional<Feature> feature=this.getFeature(featureName,libertyVersion, libertyRuntime, requestDelay, documentURI);
        if (feature.isPresent() && feature.get().getWlpInformation().getPlatforms() != null) {
            return new HashSet<>(feature.get().getWlpInformation().getPlatforms());
        }
       return Collections.emptySet();
    }

    /**
     * get all common platforms for a list of features
     * @param featureNames feature list
     * @param libertyVersion liberty version
     * @param libertyRuntime liberty runtime
     * @param requestDelay request delay
     * @param documentURI document uri
     * @return platform list
     */
    public Set<String> getCommonPlatformsForFeatures(Set<String> featureNames, String libertyVersion, String libertyRuntime,
                                                      int requestDelay, String documentURI) {
        Set<String> commonPlatforms = null;
        for (String featureName : featureNames) {
            Set<String> platforms = this.getAllPlatformsForFeature(featureName, libertyVersion, libertyRuntime, requestDelay, documentURI);
            if (commonPlatforms == null) {
                commonPlatforms = platforms;
            } else {
                commonPlatforms.retainAll(platforms);
            }
        }
        return commonPlatforms;
    }

    /**
     * get all platforms for a version less feature by
     *  1) iterating through required features array in feature wlpinformation
     *  3) iterating through required tolerates feature array in feature wlpinformation
     * @param featureName    current feature name
     * @param libertyVersion liberty version
     * @param libertyRuntime liberty runtime
     * @param requestDelay   request delay
     * @param documentURI    xml document
     * @return platform list
     */
    public Set<String> getAllPlatformsForVersionLessFeature(String featureName, String libertyVersion, String libertyRuntime, int requestDelay, String documentURI) {
        Optional<Feature> feature = this.getFeature(featureName, libertyVersion, libertyRuntime, requestDelay, documentURI);
        Set<String> featureNames = new HashSet<>();
        if (feature.isPresent()) {
            this.addRequiredFeatureNames(feature.get(), featureNames);
            this.addRequireTolerateFeatureNames(feature.get(), featureNames);
        }
        return featureNames.stream()
                .map(f -> getAllPlatformsForFeature(f, libertyVersion, libertyRuntime, requestDelay, documentURI))
                .flatMap(Set::stream).map(String::toLowerCase).collect(Collectors.toSet());
    }

    /**
     * find require to tolerate feature names. find all versions
     * @param feature current feature
     * @param featureNames feature name list
     */
    private void addRequireTolerateFeatureNames(Feature feature, Set<String> featureNames) {
        List<FeatureTolerate> featureTolerates = feature.getWlpInformation().getRequireFeatureWithTolerates();

        for(FeatureTolerate featureTolerate: featureTolerates) {
            String extractedFeatureName = featureTolerate.getFeature().contains(IO_OPENLIBERTY_INTERNAL_VERSIONLESS) ?
                    featureTolerate.getFeature().substring(
                            featureTolerate.getFeature()
                                    .lastIndexOf(IO_OPENLIBERTY_INTERNAL_VERSIONLESS) + IO_OPENLIBERTY_INTERNAL_VERSIONLESS.length())
                    : featureTolerate.getFeature();
            String extractedFeatureNameWithoutVersion = extractedFeatureName.contains("-") ?
                    extractedFeatureName.substring(0,extractedFeatureName.lastIndexOf("-") + 1) :
                    extractedFeatureName;
            featureNames.add(extractedFeatureName);
            if(featureTolerate.getTolerates()!=null && !featureTolerate.getTolerates().isEmpty()){
                for (String tolerateVersion:featureTolerate.getTolerates()){
                    featureNames.add(extractedFeatureNameWithoutVersion + tolerateVersion);
                    // if feature names are changed recently. like ejb-3.2 to enterprisebeans-4.0
                    if (LibertyConstants.changedFeatureNameMap.containsKey(extractedFeatureNameWithoutVersion)) {
                        featureNames.add(LibertyConstants.changedFeatureNameMap.get(extractedFeatureNameWithoutVersion) + tolerateVersion);
                    }
                }
            }
        }
    }

    /**
     * add all required feature names
     * @param feature current feature
     * @param requiredFeatureNames required feature name array
     */
    private void addRequiredFeatureNames(Feature feature, Set<String> requiredFeatureNames) {
        ArrayList<String> requireFeatures = feature.getWlpInformation().getRequireFeature();

        for(String requireFeature: requireFeatures) {
            String extractedFeatureName = requireFeature.contains(IO_OPENLIBERTY_INTERNAL_VERSIONLESS) ?
                    requireFeature.substring(requireFeature.lastIndexOf(IO_OPENLIBERTY_INTERNAL_VERSIONLESS)
                            + IO_OPENLIBERTY_INTERNAL_VERSIONLESS.length())
                    : requireFeature;
            requiredFeatureNames.add(extractedFeatureName);
        }
    }
}
