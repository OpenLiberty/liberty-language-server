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
package io.openliberty.tools.langserver.lemminx.models.feature;

import java.util.ArrayList;

public class JavaSEVersionRequirements {
  private String minVersion;
  ArrayList<String> rawRequirements;
  private String versionDisplayString;

  // Getter Methods

  public String getMinVersion() {
    return minVersion;
  }

  public String getVersionDisplayString() {
    return versionDisplayString;
  }

  public ArrayList<String> getRawRequirements() {
    return rawRequirements;
  }

  // Setter Methods

  public void setMinVersion(String minVersion) {
    this.minVersion = minVersion;
  }

  public void setVersionDisplayString(String versionDisplayString) {
    this.versionDisplayString = versionDisplayString;
  }

  public void setRawRequirements(ArrayList<String> rawRequirements) {
    this.rawRequirements = rawRequirements;
  }
}
