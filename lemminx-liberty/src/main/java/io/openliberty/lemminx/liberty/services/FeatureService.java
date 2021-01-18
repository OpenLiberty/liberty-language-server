package io.openliberty.lemminx.liberty.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import org.eclipse.lsp4j.WorkspaceFolder;

import io.openliberty.lemminx.liberty.models.feature.Feature;
import io.openliberty.lemminx.liberty.models.feature.FeatureInfo;
import io.openliberty.lemminx.liberty.models.feature.WlpInformation;
import io.openliberty.lemminx.liberty.util.LibertyConstants;
import io.openliberty.lemminx.liberty.util.LibertyUtils;

public class FeatureService {

  private static final Logger LOGGER = Logger.getLogger(FeatureService.class.getName());

  // Singleton so that only 1 Feature Service can be initialized and is
  // shared between all Lemminx Language Feature Participants

  private static FeatureService instance;

  public static FeatureService getInstance() {
    if (instance == null) {
      instance = new FeatureService();
    }
    return instance;
  }

  // Cache of liberty version -> list of supported features
  private Map<String, List<Feature>> featureCache;
  private List<Feature> defaultFeatureList;
  private long featureUpdateTime;

  private FeatureService() {
    featureCache = new HashMap<>();
    featureUpdateTime = -1;
  }

  /**
   * Fetches information about liberty features from maven repo
   *
   * @param libertyVersion - version of liberty to fetch features for
   * @return list of features supported by the provided version of liberty
   */
  private List<Feature> fetchFeaturesForVersion(String libertyVersion) throws IOException, JsonParseException {
    String featureEndpoint = String.format(
        "https://repo1.maven.org/maven2/io/openliberty/features/features/%s/features-%s.json", libertyVersion,
        libertyVersion);
    InputStreamReader reader = new InputStreamReader(new URL(featureEndpoint).openStream());

    // Only need the public features
    ArrayList<Feature> publicFeatures = readPublicFeatures(reader);

    LOGGER.info("returning public features: " + publicFeatures.size());
    return publicFeatures;
  }

  /**
   * Returns the default feature list
   *
   * @return list of features supported by the default version of liberty
   */
  private List<Feature> getDefaultFeatureList() {
    try {
      if (defaultFeatureList == null) {
        InputStream is = getClass().getClassLoader().getResourceAsStream("features-20.0.0.9.json");
        InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);

        // Only need the public features
        defaultFeatureList = readPublicFeatures(reader);
      }
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

  public List<Feature> getFeatures(String libertyVersion, int requestDelay, String documentURI) {
    LOGGER.fine("Getting features for version: " + libertyVersion);
    // if the features are already cached in the feature cache
    if (featureCache.containsKey(libertyVersion)) {
      return featureCache.get(libertyVersion);
    }

    // else need to fetch the features from maven central
    try {
      // verify that request delay (seconds) has gone by since last fetch request
      long currentTime = System.currentTimeMillis();
      if (this.featureUpdateTime == -1 || currentTime >= (this.featureUpdateTime + (requestDelay * 1000))) {
        List<Feature> features = fetchFeaturesForVersion(libertyVersion);
        featureCache.put(libertyVersion, features);
        this.featureUpdateTime = System.currentTimeMillis();
        return features;
      }
    } catch (Exception e) {
      // do nothing, continue on to returning default feature list
    }

    // if the installed features are already cached, return that list
    if (featureCache.containsKey("installedFeatures")) {
      return featureCache.get("installedFeatures");
    }
    // else need to fetch installed features from installed Liberty
    // TODO: add logic to determine how often we should check for list of installed features
    List<Feature> installedFeatures = getInstalledFeaturesList(documentURI);
    if (installedFeatures.size() != 0) {
      featureCache.put("installedFeatures", installedFeatures);
      return installedFeatures;
    }

    // return default feature list
    List<Feature> defaultFeatures = getDefaultFeatureList();
    return defaultFeatures;
  }

  public Optional<Feature> getFeature(String featureName, String libertyVersion, int requestDelay, String documentURI) {
    List<Feature> features = getFeatures(libertyVersion, requestDelay, documentURI);
    return features.stream().filter(f -> f.getWlpInformation().getShortName().equalsIgnoreCase(featureName))
        .findFirst();
  }

  public boolean featureExists(String featureName, String libertyVersion, int requestDelay, String documentURI) {
    return this.getFeature(featureName, libertyVersion, requestDelay, documentURI).isPresent();
  }

  // get list of installed features from ws-featurelist.jar
  private List<Feature> getInstalledFeaturesList(String documentURI) {
    List<Feature> installedFeatures = new ArrayList<Feature>();
    try {
      String workspaceFolder = LibertyProjectsManager.getWorkspaceFolder(documentURI);
      File tempDir = LibertyUtils.getTempDir(workspaceFolder);
      Path featureListJAR = LibertyUtils.findFileInWorkspace(documentURI, "ws-featurelist.jar");

      // TODO: verify where we should generate this temporary file
      if (featureListJAR != null && featureListJAR.toFile().exists()) {
        File tempFeaturesList = File.createTempFile("featureslist", ".xml", tempDir);
        String[] cmd = { "java", "-jar", featureListJAR.toAbsolutePath().toString(),
            tempFeaturesList.getAbsolutePath() };

        Process proc = Runtime.getRuntime().exec(cmd);
        BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String s = null;
        while ((s = in.readLine()) != null) {
          LOGGER.info(s);
        }

        JAXBContext jaxbContext = JAXBContext.newInstance(FeatureInfo.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        FeatureInfo featureInfo = (FeatureInfo) jaxbUnmarshaller.unmarshal(tempFeaturesList);
        if (featureInfo.getFeatures().size() > 0) {
          for (int i = 0; i < featureInfo.getFeatures().size(); i++) {
            Feature f = featureInfo.getFeatures().get(i);
            f.setShortDescription(f.getDescription());
            WlpInformation wlpInfo = new WlpInformation(f.getName());
            f.setWlpInformation(wlpInfo);
          }
          installedFeatures = featureInfo.getFeatures();
        }

        tempFeaturesList.delete();
      }
    } catch (IOException | JAXBException e) {
      LOGGER.warning("Unable to get installed features: " + e);
    }

    return installedFeatures;
  }

}
