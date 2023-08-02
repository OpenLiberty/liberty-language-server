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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
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
    ArrayList<Feature> publicFeatures = readPublicFeatures(reader);

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
   * Returns a list of public features
   *
   * @param reader - InputStreamReader for json feature list
   * @return list of public features
   */
  private ArrayList<Feature> readPublicFeatures(InputStreamReader reader) throws JsonParseException {
    Feature[] featureList = new Gson().fromJson(reader, Feature[].class);

    ArrayList<Feature> publicFeatures = new ArrayList<>();
    Arrays.asList(featureList).stream()
        .filter(f -> f.getWlpInformation().getVisibility().equals(LibertyConstants.PUBLIC_VISIBILITY))
        .forEach(publicFeatures::add);
    defaultFeatureList = publicFeatures;
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

    // else need to fetch the features from maven central
    try {
        // verify that request delay (seconds) has gone by since last fetch request
        long currentTime = System.currentTimeMillis();
        if (this.featureUpdateTime == -1 || currentTime >= (this.featureUpdateTime + (requestDelay * 1000))) {
            List<Feature> features = fetchFeaturesForVersion(libertyVersion, libertyRuntime);
            featureCache.put(featureCacheKey, features);
            this.featureUpdateTime = System.currentTimeMillis();
            return features;
        }
    } catch (Exception e) {
        // do nothing, continue on to returning default feature list
    }

    // fetch installed features list - this would only happen if a features.json was not able to be downloaded from Maven Central
    // which would not be the normal case
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

  public boolean featureExists(String featureName, String libertyVersion, String libertyRuntime, int requestDelay, String documentURI) {
    return this.getFeature(featureName, libertyVersion, libertyRuntime, requestDelay, documentURI).isPresent();
  }

  /**
   * Returns the list of installed features generated from ws-featurelist.jar.
   * Generated feature list is stored in the (target/build)/.libertyls directory.
   * Returns an empty list if cannot determine installed feature list.
   * 
   * @param documentURI xml document
   * @return list of installed features, or empty list
   */
  private List<Feature> getInstalledFeaturesList(String documentURI, String libertyRuntime, String libertyVersion) {
      List<Feature> installedFeatures = new ArrayList<Feature>();
      try {
          LibertyWorkspace libertyWorkspace = LibertyProjectsManager.getInstance().getWorkspaceFolder(documentURI);
          if (libertyWorkspace == null || libertyWorkspace.getWorkspaceString() == null) {
              return installedFeatures;
          }

          // return installed features from cache
          if (libertyWorkspace.getInstalledFeatureList().size() != 0) {
              return libertyWorkspace.getInstalledFeatureList();
          }

          Path featureListJAR = LibertyUtils.findFileInWorkspace(documentURI, Paths.get("bin", "tools", "ws-featurelist.jar"));

          if (featureListJAR != null && featureListJAR.toFile().exists()) {

              File tempDir = LibertyUtils.getTempDir(libertyWorkspace);
              String featureListFileName = "featurelist-"+libertyRuntime+"-"+libertyVersion+".xml";

              // If tempDir is null, issue a warning for the current LibertyWorkspace URI and use the default features.json
              if (tempDir == null) {
                  LOGGER.warning("Could not create a temporary directory for generating the " +  featureListFileName + " file. The cached features json file will be used for the current workspace: " + libertyWorkspace.getWorkspaceString());
                  return installedFeatures;
              }

              File featureListFile = new File(tempDir, featureListFileName);

              ProcessBuilder pb = new ProcessBuilder("java", "-jar", featureListJAR.toAbsolutePath().toString(), featureListFile.getCanonicalPath());
              pb.directory(tempDir);
              pb.redirectErrorStream(true);
              pb.redirectOutput(new File(tempDir, "ws-featurelist.log"));
    
              Process proc = pb.start();
              if (!proc.waitFor(30, TimeUnit.SECONDS)) {
                  proc.destroy();
                  LOGGER.warning("Exceeded 30 second timeout during feature list generation. Using cached features json file.");
                  return installedFeatures;
              }

              installedFeatures = readFeaturesFromFeatureListFile(installedFeatures, libertyWorkspace, featureListFile);
          } else {
              LOGGER.warning("Unable to generate the feature list for the current Liberty workspace:" + libertyWorkspace.getWorkspaceString());
          }
      } catch (IOException | JAXBException | InterruptedException e) {
          LOGGER.warning("Unable to get installed features: " + e);
      }

      LOGGER.info("Returning installed features: " + installedFeatures.size());
      return installedFeatures;
  }

  public List<Feature> readFeaturesFromFeatureListFile(List<Feature> installedFeatures, LibertyWorkspace libertyWorkspace,
      File featureListFile) throws JAXBException {
    JAXBContext jaxbContext = JAXBContext.newInstance(FeatureInfo.class);
    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    FeatureInfo featureInfo = (FeatureInfo) jaxbUnmarshaller.unmarshal(featureListFile);
     
    if ((featureInfo.getFeatures() != null) && (featureInfo.getFeatures().size() > 0)) {
        for (int i = 0; i < featureInfo.getFeatures().size(); i++) {
            Feature f = featureInfo.getFeatures().get(i);
            f.setShortDescription(f.getDescription());
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
