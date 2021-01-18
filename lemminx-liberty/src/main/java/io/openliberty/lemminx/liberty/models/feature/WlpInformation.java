package io.openliberty.lemminx.liberty.models.feature;

import java.util.ArrayList;

public class WlpInformation {
  private String appliesTo;
  private String displayPolicy;
  private String downloadPolicy;
  private String ibmInstallTo;
  private String installPolicy;
  JavaSEVersionRequirements javaSEVersionRequirements;
  ArrayList<String> provideFeature;
  ArrayList<String> requireFeature;
  private String singleton;
  private String typeLabel;
  private String visibility;
  private String webDisplayPolicy;
  private String mavenCoordinates;
  private String shortName;

  // Getter Methods

  public WlpInformation(String shortName) {
    this.shortName = shortName;
  }

  public String getAppliesTo() {
    return appliesTo;
  }

  public String getDisplayPolicy() {
    return displayPolicy;
  }

  public String getDownloadPolicy() {
    return downloadPolicy;
  }

  public String getIbmInstallTo() {
    return ibmInstallTo;
  }

  public String getInstallPolicy() {
    return installPolicy;
  }

  public JavaSEVersionRequirements getJavaSEVersionRequirements() {
    return javaSEVersionRequirements;
  }

  public String getSingleton() {
    return singleton;
  }

  public String getTypeLabel() {
    return typeLabel;
  }

  public String getVisibility() {
    return visibility;
  }

  public String getWebDisplayPolicy() {
    return webDisplayPolicy;
  }

  public String getMavenCoordinates() {
    return mavenCoordinates;
  }

  public ArrayList<String> getProvideFeature() {
    return provideFeature;
  }

  public ArrayList<String> getRequireFeature() {
    return requireFeature;
  }

  public String getShortName() {
    return shortName;
  }

  // Setter Methods

  public void setAppliesTo(String appliesTo) {
    this.appliesTo = appliesTo;
  }

  public void setDisplayPolicy(String displayPolicy) {
    this.displayPolicy = displayPolicy;
  }

  public void setDownloadPolicy(String downloadPolicy) {
    this.downloadPolicy = downloadPolicy;
  }

  public void setIbmInstallTo(String ibmInstallTo) {
    this.ibmInstallTo = ibmInstallTo;
  }

  public void setInstallPolicy(String installPolicy) {
    this.installPolicy = installPolicy;
  }

  public void setJavaSEVersionRequirements(JavaSEVersionRequirements javaSEVersionRequirements) {
    this.javaSEVersionRequirements = javaSEVersionRequirements;
  }

  public void setSingleton(String singleton) {
    this.singleton = singleton;
  }

  public void setTypeLabel(String typeLabel) {
    this.typeLabel = typeLabel;
  }

  public void setVisibility(String visibility) {
    this.visibility = visibility;
  }

  public void setWebDisplayPolicy(String webDisplayPolicy) {
    this.webDisplayPolicy = webDisplayPolicy;
  }

  public void setMavenCoordinates(String mavenCoordinates) {
    this.mavenCoordinates = mavenCoordinates;
  }

  public void setProvideFeature(ArrayList<String> provideFeature) {
    this.provideFeature = provideFeature;
  }

  public void setRequireFeature(ArrayList<String> requireFeature) {
    this.requireFeature = requireFeature;
  }

  public void setShortName(String shortName) {
    this.shortName = shortName;
  }
}

