/*******************************************************************************
* Copyright (c) 2020, 2022 IBM Corporation and others.
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

import org.eclipse.lemminx.utils.JSONUtility;
import io.openliberty.tools.langserver.lemminx.models.settings.*;

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
