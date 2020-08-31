package io.openliberty.lemminx.liberty.services;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.io.IOException;
import java.net.URL;
import io.openliberty.lemminx.liberty.models.feature.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import java.util.Map;
import java.util.Optional;
import io.openliberty.lemminx.liberty.util.LibertyConstants;

public class FeatureService {

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
    ObjectMapper objectMapper = new ObjectMapper();
    // Not all properties returned from maven are currently described in the Feature
    // POJO model. So this must be disabled or else it throws an error on unknown
    // properties.
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    String featureEndpoint = String.format(
        "https://repo1.maven.org/maven2/io/openliberty/features/features/%s/features-%s.json", libertyVersion,
        libertyVersion);
    Feature[] featureList = objectMapper.readValue(new URL(featureEndpoint), Feature[].class);
    // Only need the public features
    ArrayList<Feature> publicFeatures = new ArrayList<>();
    Arrays.asList(featureList).stream()
        .filter(f -> f.getWlpInformation().getVisibility().equals(LibertyConstants.PUBLIC_VISIBILITY))
        .forEach(publicFeatures::add);
    return publicFeatures;
  }

  public List<Feature> getFeatures(String libertyVersion) throws IOException {
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
