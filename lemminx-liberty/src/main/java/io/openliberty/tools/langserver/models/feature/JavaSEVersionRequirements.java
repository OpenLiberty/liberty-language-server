package io.openliberty.lemminx.liberty.models.feature;

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
