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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import io.openliberty.tools.langserver.lemminx.models.feature.FeatureTolerate;
import io.openliberty.tools.langserver.lemminx.models.feature.FeaturesAndPlatforms;
import io.openliberty.tools.langserver.lemminx.models.feature.PrivateFeature;

import io.openliberty.tools.langserver.lemminx.util.LibertyRuntimeVersionUtil;
import io.openliberty.tools.langserver.lemminx.util.SchemaAndFeatureListGeneratorUtil;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.uriresolver.CacheResourcesManager;
import org.eclipse.lemminx.uriresolver.CacheResourcesManager.ResourceToDeploy;

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
    private static final String FEATURELIST_XML_RESOURCE_URL = "https://github.com/OpenLiberty/liberty-language-server/blob/master/lemminx-liberty/src/main/resources/featurelist.cached/featurelist-cached-25.0.0.6_%s.xml";
    private static final String FEATURELIST_XML_CLASSPATH_LOCATION = "/featurelist.cached/featurelist-cached-25.0.0.6_%s.xml";
    private static final String FEATURELIST_XML_RESOURCE_URL_DEFAULT = "https://github.com/OpenLiberty/liberty-language-server/blob/master/lemminx-liberty/src/main/resources/featurelist.cached/featurelist-cached-25.0.0.6.xml";
    private static final String FEATURELIST_XML_CLASSPATH_LOCATION_DEFAULT = "/featurelist.cached/featurelist-cached-25.0.0.6.xml";

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
    private static ResourceToDeploy FEATURELIST_XML_RESOURCE;
    private static final ResourceToDeploy FEATURELIST_XML_RESOURCE_DEFAULT = new ResourceToDeploy(FEATURELIST_XML_RESOURCE_URL_DEFAULT,
            FEATURELIST_XML_CLASSPATH_LOCATION_DEFAULT) ;
    public static final String LIBERTY_FEATURELIST_VERSION_XSD = "https://repo1.maven.org/maven2/io/openliberty/features/open_liberty_featurelist/$VERSION/open_liberty_featurelist-$VERSION.xml";
    public static final String LIBERTY_FEATURELIST_VERSION_WITH_LOCALE_XSD = "https://repo1.maven.org/maven2/io/openliberty/features/open_liberty_featurelist_$LOCALE/$VERSION/open_liberty_featurelist_$LOCALE-$VERSION.xml";


    public static FeatureService getInstance() {
        if (instance == null) {
            instance = new FeatureService();
            FEATURELIST_XML_RESOURCE = new ResourceToDeploy(FEATURELIST_XML_RESOURCE_URL.formatted(SettingsService.getInstance().getCurrentLocale().toString()),
                    FEATURELIST_XML_CLASSPATH_LOCATION.formatted(SettingsService.getInstance().getCurrentLocale().toString()));
        }
        return instance;
    }

    // Cache of Liberty version -> list of supported features
    private Map<String, FeaturesAndPlatforms> featureAndPlatformCache;   // the key consists of runtime-version, where runtime is 'ol' or 'wlp'
    private FeaturesAndPlatforms defaultFeaturesAndPlatforms;
    private FeatureListGraph defaultFeatureList;
    private long featureUpdateTime;

    private FeatureService() {
        featureAndPlatformCache = new HashMap<>();
        featureUpdateTime = -1;
    }


    /**
     * Fetches information about Liberty features from Maven repo
     *
     * @param libertyVersion  - version of Liberty to fetch features for
     * @param workspaceFolder - liberty workspace details
     * @return list of features supported by the provided version of Liberty
     */
    private FeaturesAndPlatforms fetchFeaturesForVersion(String libertyVersion, String libertyRuntime, LibertyWorkspace workspaceFolder) throws IOException, JsonParseException, URISyntaxException {
        String featureEndpoint = libertyRuntime.equals("wlp") ? String.format(wlpFeatureEndpoint, libertyVersion) :
                                                                String.format(olFeatureEndpoint, libertyVersion);
        URL featureJsonURL = new URL(featureEndpoint);
        File tempDir = LibertyUtils.getTempDir(workspaceFolder);
        File jsonDestFile = new File(tempDir, featureJsonURL.getFile());
        LibertyRuntimeVersionUtil.getResource(featureEndpoint,jsonDestFile.getPath());
        // saving feature.json to .libertyls folder first and then reading from there
        // saved because this would help to show URL in hover
        InputStreamReader reader = new InputStreamReader(new FileInputStream(jsonDestFile));

        // Only need the public features
        FeaturesAndPlatforms fp = readFeaturesAndPlatforms(reader);

        if (libertyRuntime.equals("wlp")) {
            // need to also get the OpenLiberty features and add them to the list to return
            FeaturesAndPlatforms olFP = fetchFeaturesForVersion(libertyVersion, "ol", workspaceFolder);
            fp.addFeaturesAndPlatforms(olFP);
        }

        LOGGER.info("Returning public features and platforms from Maven - features: " + fp.getPublicFeatures().size()+" platforms: "+fp.getPlatforms().size());
        LOGGER.info("Setting feature json path using endpoint %s cached to %s".formatted(featureEndpoint,jsonDestFile.toURI()));
        SettingsService.getInstance().setFeatureJsonFilePath(Path.of(jsonDestFile.toURI()));
        return fp;
    }

    /**
     * Returns the default list of features and platforms
     *
     * @return list of features and platforms supported by the default version of Liberty
     */
    private FeaturesAndPlatforms getDefaultFeaturesAndPlatforms() {
        InputStream is = null;

            if (defaultFeaturesAndPlatforms == null) {
                try {
                    Path featureVersionPath = LibertyRuntimeVersionUtil.downloadAndCacheLatestResource("https://repo1.maven.org/maven2/io/openliberty/features/features/$VERSION/features-$VERSION.json", null);
                    if (featureVersionPath != null) {
                        is = new FileInputStream(featureVersionPath.toFile());
                        LOGGER.info("Setting feature json by downloading latest version cached to %s".formatted(featureVersionPath));
                        SettingsService.getInstance().setFeatureJsonFilePath(featureVersionPath);
                    } else {
                        // falling back to the json stored in local
                        // caching this to .lemminx folder as well to show URL in hover
                        ResourceToDeploy featureJsonResource = new ResourceToDeploy("https://repo1.maven.org/maven2/io/openliberty/features/features/features-cached-25.0.0.6.json", "features-cached-25.0.0.6.json");
                        Path deployedPath=CacheResourcesManager.getResourceCachePath(featureJsonResource);
                        is = new FileInputStream(deployedPath.toFile());
                        LOGGER.info("Setting feature json by caching local version stored in classpath to %s".formatted(deployedPath));
                        SettingsService.getInstance().setFeatureJsonFilePath(deployedPath);
                    }
                    InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                    // Only need the public features
                    defaultFeaturesAndPlatforms = readFeaturesAndPlatforms(reader);
                } catch (JsonParseException | IOException e) {
                    // unable to read json in resources file, return empty list
                    LOGGER.severe("Error: Unable to get default features and platforms.");
                    defaultFeaturesAndPlatforms = new FeaturesAndPlatforms();
                    return defaultFeaturesAndPlatforms;
                } catch (Exception e){
                    LOGGER.severe("Error: " + e.getMessage());
                    defaultFeaturesAndPlatforms = new FeaturesAndPlatforms();
                    return defaultFeaturesAndPlatforms;
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            LOGGER.severe("Error: Unable to close input stream " + e.getMessage());
                        }
                    }
                }
            }
            LOGGER.info("Returning default list of features and platforms");
            return defaultFeaturesAndPlatforms;
    }

    /**
     * Returns an object with a list of public features found in the passed input stream. Does not affect the default feature list.
     * The returned object also contains a list of private features, and a set of available platforms.
     *
     * @param reader - InputStreamReader for json feature list
     * @return FeaturesAndPlatforms with a list of public features, list of private features, and set of available platforms.
     */
    private FeaturesAndPlatforms readFeaturesAndPlatforms(InputStreamReader reader) throws JsonParseException {
        Feature[] featureList = new Gson().fromJson(reader, Feature[].class);

        ArrayList<Feature> publicFeatures = new ArrayList<>();
        ArrayList<Feature> privateFeatures = new ArrayList<>();

        // Guard against null visibility field. Ran into this during manual testing of a wlp installation.
        for (int i=0; i < featureList.length; i++) {
            if (featureList[i].getWlpInformation().getVisibility() != null) {
                if (featureList[i].getWlpInformation().getVisibility().equals(LibertyConstants.PUBLIC_VISIBILITY)) {
                    publicFeatures.add(featureList[i]);
                } else if (featureList[i].getWlpInformation().getVisibility().equals(LibertyConstants.PRIVATE_VISIBILITY)) {
                    privateFeatures.add(featureList[i]);
                }
            }
        }
        return new FeaturesAndPlatforms(publicFeatures, privateFeatures);
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
    public FeaturesAndPlatforms getFeaturesAndPlatforms(String libertyVersion, String libertyRuntime, int requestDelay, String documentURI) {
        if (libertyRuntime == null || libertyVersion == null) {
            // return default list of features
            FeaturesAndPlatforms defaultFeatures = getDefaultFeaturesAndPlatforms();
            getDefaultFeatureList();
            return defaultFeatures;
        }

        String featureCacheKey = libertyRuntime + "-" + libertyVersion;

        // if the features are already cached in the feature cache
        if (featureAndPlatformCache.containsKey(featureCacheKey)) {
            LOGGER.info("Getting cached features and platforms for: " + featureCacheKey);
            return featureAndPlatformCache.get(featureCacheKey);
        }

        LOGGER.info("Getting features and platforms for: " + featureCacheKey);

        // if not a beta runtime, fetch features from maven central
        // - beta runtimes do not have a published features.json in mc
        if (!libertyVersion.endsWith("-beta")) {
            try {
                // verify that request delay (seconds) has gone by since last fetch request
                // Note that the default delay is 10 seconds and can cause us to generate a feature list instead of download from MC when
                // switching back and forth between projects.
                long currentTime = System.currentTimeMillis();
                if (this.featureUpdateTime == -1 || currentTime >= (this.featureUpdateTime + (requestDelay * 1000))) {
                    LibertyWorkspace workspaceFolder = LibertyProjectsManager.getInstance().getWorkspaceFolder(documentURI);
                    FeaturesAndPlatforms features = fetchFeaturesForVersion(libertyVersion, libertyRuntime, workspaceFolder);
                    featureAndPlatformCache.put(featureCacheKey, features);
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
        FeaturesAndPlatforms installedFeatures = getInstalledFeaturesList(documentURI, libertyRuntime, libertyVersion);
        if (installedFeatures.getPublicFeatures().size() != 0) {
            return installedFeatures;
        }

        // return default list of features
        getDefaultFeaturesAndPlatforms();
        getDefaultFeatureList();
        return defaultFeaturesAndPlatforms;
    }

    public Optional<Feature> getFeature(String featureName, String libertyVersion, String libertyRuntime, int requestDelay, String documentURI) {
        FeaturesAndPlatforms fp = getFeaturesAndPlatforms(libertyVersion, libertyRuntime, requestDelay, documentURI);
        List<Feature> features = fp.getPublicFeatures();
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
        FeaturesAndPlatforms fp = getFeaturesAndPlatforms(libertyVersion, libertyRuntime, requestDelay, documentURI);
        List<Feature> features = fp.getPublicFeatures();
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
        boolean alreadyIgnoredOnce = false;
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
                // if same feature is repeated, ignore it first time, but add for second time onwards
                if (featureNameLowerCase.equalsIgnoreCase(currentFeatureName) && !alreadyIgnoredOnce) {
                    alreadyIgnoredOnce = true;
                } else {
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
    public FeaturesAndPlatforms getInstalledFeaturesAndPlatformsList(LibertyWorkspace libertyWorkspace, String libertyRuntime, String libertyVersion) {
        FeaturesAndPlatforms installedFeaturesAndPlatforms = new FeaturesAndPlatforms();
        if (libertyWorkspace == null || libertyWorkspace.getWorkspaceString() == null) {
            return installedFeaturesAndPlatforms;
        }

        // return installed features from cache
        FeaturesAndPlatforms cachedFeaturesAndPlatforms = libertyWorkspace.getInstalledFeaturesAndPlatformsList();
        if (cachedFeaturesAndPlatforms.getPublicFeatures().size() != 0) {
            LOGGER.info("Getting cached features from previously generated feature list: " + cachedFeaturesAndPlatforms.getPublicFeatures().size());
            return cachedFeaturesAndPlatforms;
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
                    installedFeaturesAndPlatforms = readFeaturesFromFeatureListFile(libertyWorkspace, featureListFile);
                } catch (JAXBException e) {
                    LOGGER.severe("Error: Unable to load the generated feature list file for the target Liberty runtime due to exception: "+e.getMessage());
                }
            } else {
                LOGGER.warning("Unable to generate the feature list for the current Liberty workspace:" + libertyWorkspace.getWorkspaceString());
            }
        } catch (IOException e) {
            LOGGER.severe("Error: Unable to generate the feature list file from the target Liberty runtime due to exception: "+e.getMessage());
        }

        LOGGER.info("Returning installed features: " + installedFeaturesAndPlatforms.getPublicFeatures().size());
        return installedFeaturesAndPlatforms;
    }

    public FeaturesAndPlatforms getInstalledFeaturesList(String documentURI, String libertyRuntime, String libertyVersion) {
        LibertyWorkspace libertyWorkspace = LibertyProjectsManager.getInstance().getWorkspaceFolder(documentURI);
        return getInstalledFeaturesAndPlatformsList(libertyWorkspace, libertyRuntime, libertyVersion);
    }

    public FeatureListGraph getDefaultFeatureList() {
        if (defaultFeatureList != null) {
            return defaultFeatureList;
        }

        try {
            Path featurelistXmlFile = getFeaturelistXmlFile();
            LOGGER.info("Using cached Liberty featurelist xml file located at: " + featurelistXmlFile.toString());

            File featureListFile = featurelistXmlFile.toFile();

            if (featureListFile != null && featureListFile.exists()) {
                try {
                    readFeaturesFromFeatureListFile(null, featureListFile, true);
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

    private static Path getFeaturelistXmlFile() throws IOException {
        Path featurelistXmlFile = LibertyRuntimeVersionUtil.downloadAndCacheLatestResource(LIBERTY_FEATURELIST_VERSION_XSD, LIBERTY_FEATURELIST_VERSION_WITH_LOCALE_XSD);
        // fallback to classpath cached file
        if (featurelistXmlFile == null) {
            if (Locale.US.equals(SettingsService.getInstance().getCurrentLocale())) {
                LOGGER.info("Locale is %s. Using default feature list cache xml in %s".formatted(SettingsService.getInstance().getCurrentLocale(), FEATURELIST_XML_RESOURCE));
                return CacheResourcesManager.getResourceCachePath(FEATURELIST_XML_RESOURCE_DEFAULT);
            }
            try {
                LOGGER.info("Using Locale %s to find feature list xml in %s".formatted(SettingsService.getInstance().getCurrentLocale(), FEATURELIST_XML_RESOURCE));
                featurelistXmlFile = CacheResourcesManager.getResourceCachePath(FEATURELIST_XML_RESOURCE);
            } catch (Exception e) {
                LOGGER.warning("Unable to find localized feature list cache using current locale %s. Using default feature list cache xml in %s".formatted(SettingsService.getInstance().getCurrentLocale(), FEATURELIST_XML_RESOURCE_DEFAULT));
                featurelistXmlFile = CacheResourcesManager.getResourceCachePath(FEATURELIST_XML_RESOURCE_DEFAULT);
            }
        }
        return featurelistXmlFile;
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

            SchemaAndFeatureListGeneratorUtil.generateFile(
                    SchemaAndFeatureListGeneratorUtil.ProcessType.FEATURE_LIST,
                    tempDir.toPath(),
                    featurelistJarPath,
                    featureListFile,
                    SettingsService.getInstance().getCurrentLocale().toString()
            );
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
            LOGGER.warning("Due to an exception during feature list file generation, a cached features json file will be used.");
            return null;
        }

        LOGGER.info("Using feature list file at: " + featureListFile.toURI().toString());
        return featureListFile;
    }

    public FeaturesAndPlatforms readFeaturesFromFeatureListFile(LibertyWorkspace libertyWorkspace,
        File featureListFile) throws JAXBException {
            return readFeaturesFromFeatureListFile(libertyWorkspace, featureListFile, false);
    }

    // If the graphOnly boolean is true, the libertyWorkspace parameter may be null. Also, the defaultFeatureList should be initialized
    // after calling this method with graphOnly set to true.
    public FeaturesAndPlatforms readFeaturesFromFeatureListFile(LibertyWorkspace libertyWorkspace,
        File featureListFile, boolean graphOnly) throws JAXBException {
        FeaturesAndPlatforms installedFeatures = new FeaturesAndPlatforms();
        JAXBContext jaxbContext = JAXBContext.newInstance(FeatureInfo.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        FeatureInfo featureInfo = (FeatureInfo) jaxbUnmarshaller.unmarshal(featureListFile);
        FeatureListGraph featureListGraph = new FeatureListGraph();

        // Note: The public features are loaded in the getFeatures() collection when unmarshalling the passed featureListFile.
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

                // check symbolicName to see if this is a versionless feature
                if (f.getSymbolicName().contains(".versionless.")) {
                    currentFeatureNode.setIsVersionless(true);
                }

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

            // Note: The private features are loaded in the getPrivateFeatures() collection when unmarshalling the passed featureListFile.
            List<Feature> privateFeatures = new ArrayList<>();
            if ((featureInfo.getPrivateFeatures() != null) && (featureInfo.getPrivateFeatures().size() > 0)) {
                for (PrivateFeature pf : featureInfo.getPrivateFeatures()) {
                    Feature f = new Feature();
                    f.setName(pf.getSymbolicName());
                    WlpInformation wlpInfo = new WlpInformation(f.getName());
                    f.setWlpInformation(wlpInfo);
                    wlpInfo.setVisibility(LibertyConstants.PRIVATE_VISIBILITY);
                    wlpInfo.setPlatforms(pf.getPlatforms());

                    privateFeatures.add(f);
                }
            }

            if (!graphOnly) {
                installedFeatures = new FeaturesAndPlatforms(featureInfo.getFeatures(), privateFeatures);
                libertyWorkspace.setInstalledFeaturesAndPlatformsList(installedFeatures);
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
        return getFeaturesAndPlatforms(libertyVersion, libertyRuntime, requestDelay, documentURI).getPlatforms();
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
            // only include available platforms - a feature can list a platform that is in beta
            Set<String> availablePlatforms = getAllPlatforms(libertyVersion, libertyRuntime, requestDelay, documentURI);
            Set<String> returnSet = new HashSet<>(feature.get().getWlpInformation().getPlatforms());
            returnSet.retainAll(availablePlatforms);

            return returnSet;
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
    /**
     * Returns the platform names specified in the featureManager element in lower case,
     * excluding the currentPlatformName if specified.
     * @param document DOM document
     * @param currentPlatformName current platform name
     * @return all platforms in document
     */
    public List<String> collectExistingPlatforms(DOMDocument document, String currentPlatformName) {
        List<String> includedPlatforms = new ArrayList<>();
        List<DOMNode> nodes = document.getDocumentElement().getChildren();
        boolean alreadyIgnoredOnce = false;
        DOMNode featureManager = null;

        for (DOMNode node : nodes) {
            if (LibertyConstants.FEATURE_MANAGER_ELEMENT.equals(node.getNodeName())) {
                featureManager = node;
                break;
            }
        }
        if (featureManager == null) {
            return includedPlatforms;
        }

        List<DOMNode> platforms = featureManager.getChildren();
        for (DOMNode platformNode : platforms) {
            DOMNode platformTextNode = (DOMNode) platformNode.getChildNodes().item(0);
            // skip nodes that do not have any text value (ie. comments)
            if (platformNode.getNodeName().equals(LibertyConstants.PLATFORM_ELEMENT) && platformTextNode != null) {
                String platformName = platformTextNode.getTextContent();
                String platformNameLowerCase = platformName.toLowerCase();
                if (platformNameLowerCase.equalsIgnoreCase(currentPlatformName) && !alreadyIgnoredOnce) {
                    alreadyIgnoredOnce = true;
                } else {
                    includedPlatforms.add(platformNameLowerCase);
                }
            }
        }
        return includedPlatforms;
    }

    /**
     * get version less feature list for specified list of versioned features
     * @param versionedFeatureNames list of versioned feature names
     * @param libertyRuntime librty runtime
     * @param libertyVersion runtime version
     * @param requestDelay request delay
     * @param documentURI server xml document uri
     * @return version less feature list
     */
    public Set<String> getVersionLessFeaturesForVersioned(List<String> versionedFeatureNames, String libertyRuntime, String libertyVersion,int requestDelay, String documentURI) {
        FeaturesAndPlatforms featuresAndPlatforms = getFeaturesAndPlatforms( libertyVersion,libertyRuntime, requestDelay, documentURI);
        Set<String> featureNames = featuresAndPlatforms.getPublicFeatures().stream()
                .filter(feature -> feature.getWlpInformation() != null)
                .map(feature -> feature.getWlpInformation().getShortName().toLowerCase())
                .collect(Collectors.toSet());

        return versionedFeatureNames.stream()
                .map(LibertyUtils::stripVersion)
                .filter(feature -> featureNames.contains(feature.toLowerCase()))
                .collect(Collectors.toSet());
    }

    /**
     * Clean featurelist and platforms cache when needed
     * Used for tests
     */
    public void evictCache() {
        featureAndPlatformCache = new HashMap<>();
        featureUpdateTime = -1;
        defaultFeatureList = null;
        defaultFeaturesAndPlatforms = null;
    }
}
