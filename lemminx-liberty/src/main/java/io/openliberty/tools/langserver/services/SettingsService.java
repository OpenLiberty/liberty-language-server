package io.openliberty.lemminx.liberty.services;

import org.eclipse.lemminx.utils.JSONUtility;
import io.openliberty.lemminx.liberty.models.settings.*;

public class SettingsService {

  // Singleton so that only 1 Settings Service can be initialized and is
  // shared between all Lemminx Language Feature Participants

  private static SettingsService instance = new SettingsService();

  public static SettingsService getInstance() {
    return instance;
  }

  // default request delay is 120 seconds
  private static int DEFAULT_REQUEST_DELAY = 120;

  private SettingsService() {
  }

  private LibertySettings settings;

  /**
   * Takes the xml settings object and parses out the Liberty Settings
   * @param xmlSettings - All xml settings provided by the client
   */
  public void updateLibertySettings(Object xmlSettings) {
    AllSettings rootSettings = JSONUtility.toModel(xmlSettings, AllSettings.class);
    if (rootSettings != null) {
      settings = JSONUtility.toModel(rootSettings.getLiberty(), LibertySettings.class);
    }
  }

  public String getLibertyVersion() {
    if (settings != null) {
      String version = settings.getVersion();
      if (version != null) {
        return version;
      }
    }

    return null;
  }

  public int getRequestDelay() {
    if (settings != null) {
      int requestDelay = settings.getRequestDelay();
      if (requestDelay > 0) {
        return requestDelay;
      }
    }

    return DEFAULT_REQUEST_DELAY;
  }

}
