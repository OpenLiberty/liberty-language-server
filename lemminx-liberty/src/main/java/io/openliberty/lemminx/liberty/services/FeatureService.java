package io.openliberty.lemminx.liberty.services;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

import io.openliberty.lemminx.liberty.models.feature.*;
import io.openliberty.lemminx.liberty.util.LibertyConstants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.logging.Logger;


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

  private FeatureService() {
    featureCache = new HashMap<>();
  }

  /**
   * Fetches information about liberty features from maven repo
   *
   * @param libertyVersion - version of liberty to fetch features for
   * @return list of features supported by the provided version of liberty
   */
  private List<Feature> fetchFeaturesForVersion(String libertyVersion) throws IOException {
    String featureEndpoint = String.format(
        "https://repo1.maven.org/maven2/io/openliberty/features/features/%s/features-%s.json", libertyVersion,
        libertyVersion);
    InputStreamReader reader = new InputStreamReader(new URL(featureEndpoint).openStream());
    Gson gson = new GsonBuilder().create();
    Feature[] featureList = new Gson().fromJson(reader, Feature[].class);

    // Only need the public features
    ArrayList<Feature> publicFeatures = new ArrayList<>();
    Arrays.asList(featureList).stream()
        .filter(f -> f.getWlpInformation().getVisibility().equals(LibertyConstants.PUBLIC_VISIBILITY))
        .forEach(publicFeatures::add);
    return publicFeatures;
  }

  public List<Feature> getFeatures(String libertyVersion) throws IOException {
    LOGGER.fine("Getting features for version: " + libertyVersion);
    // if the features are already cached in the feature cache
    if (featureCache.containsKey(libertyVersion)) {
      return featureCache.get(libertyVersion);
    }
    // else need to fetch the features from maven central
    List<Feature> features = fetchFeaturesForVersion(libertyVersion);
    featureCache.put(libertyVersion, features);
    return features;
  }

  public Optional<Feature> getFeature(String featureName, String libertyVersion) throws IOException {
    List<Feature> features = getFeatures(libertyVersion);
    return features.stream().filter(f -> f.getWlpInformation().getShortName().equalsIgnoreCase(featureName))
        .findFirst();
  }

  public boolean featureExists(String featureName, String libertyVersion) throws IOException {
    return this.getFeature(featureName, libertyVersion).isPresent();
  }
}
