package io.openliberty.lemminx.liberty.services;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.lang.ClassLoader;

import io.openliberty.lemminx.liberty.models.feature.*;
import io.openliberty.lemminx.liberty.util.LibertyConstants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.logging.Logger;
import com.google.gson.JsonParseException;

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
    defaultFeatureList = new ArrayList<>();
    featureUpdateTime = System.currentTimeMillis();
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
    ArrayList<Feature> publicFeatures = getPublicFeatureList(reader);

    return publicFeatures;
  }

  /**
   * Returns the default feature list
   *
   * @return list of features supported by the default version of liberty
   */
  private List<Feature> getDefaultFeatureList() {
    try {
      if (defaultFeatureList.isEmpty()) {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream("features-20.0.0.9.json");
        InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);

        // Only need the public features
        defaultFeatureList = getPublicFeatureList(reader);
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
  private ArrayList<Feature> getPublicFeatureList(InputStreamReader reader) throws JsonParseException {
    Gson gson = new GsonBuilder().create();
    Feature[] featureList = new Gson().fromJson(reader, Feature[].class);

    ArrayList<Feature> publicFeatures = new ArrayList<>();
    Arrays.asList(featureList).stream()
            .filter(f -> f.getWlpInformation().getVisibility().equals(LibertyConstants.PUBLIC_VISIBILITY))
            .forEach(publicFeatures::add);
    defaultFeatureList = publicFeatures;
    return publicFeatures;
  }

  public List<Feature> getFeatures(String libertyVersion) {
    LOGGER.fine("Getting features for version: " + libertyVersion);
    // if the features are already cached in the feature cache
    if (featureCache.containsKey(libertyVersion)) {
      return featureCache.get(libertyVersion);
    }
    // else need to fetch the features from maven central
    try {
      // verify that 2 min has gone by since last fetch request
      long currentTime = System.currentTimeMillis();
      if (currentTime >= (this.featureUpdateTime + 120000)) {
        List<Feature> features = fetchFeaturesForVersion(libertyVersion);
        featureCache.put(libertyVersion, features);
        this.featureUpdateTime = System.currentTimeMillis();
        return features;
      }
    } catch (Exception e) {
      // do nothing, continue on to returning default feature list
    }
    // return default feature list
    List<Feature> defaultFeatures = getDefaultFeatureList();
    return defaultFeatures;
  }

  public Optional<Feature> getFeature(String featureName, String libertyVersion) {
    List<Feature> features = getFeatures(libertyVersion);
    return features.stream().filter(f -> f.getWlpInformation().getShortName().equalsIgnoreCase(featureName))
        .findFirst();
  }

  public boolean featureExists(String featureName, String libertyVersion) {
    return this.getFeature(featureName, libertyVersion).isPresent();
  }
}
