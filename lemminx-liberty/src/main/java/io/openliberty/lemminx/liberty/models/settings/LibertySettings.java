package io.openliberty.lemminx.liberty.models.settings;

/**
 * Model for settings under the 'liberty' key in xml settings
 * Ie. version refers to: xml.liberty.version
 */
public class LibertySettings {

  private String version;
  private int requestDelay; // in seconds

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public int getRequestDelay() {
    return requestDelay;
  }

  public void setRequestDelay(int requestDelay) {
    this.requestDelay = requestDelay;
  }

}
