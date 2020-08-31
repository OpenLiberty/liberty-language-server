package io.openliberty.lemminx.liberty.models.feature;

public class Feature {
  private String description;
  private String licenseId;
  private String licenseType;
  private String name;
  Provider providerObject;
  private String shortDescription;
  private String type;
  private String version;
  WlpInformation wlpInformationObject;
 
 
  // Getter Methods 
 
  public String getDescription() {
   return description;
  }
 
  public String getLicenseId() {
   return licenseId;
  }
 
  public String getLicenseType() {
   return licenseType;
  }
 
  public String getName() {
   return name;
  }
 
  public Provider getProvider() {
   return providerObject;
  }
 
  public String getShortDescription() {
   return shortDescription;
  }
 
  public String getType() {
   return type;
  }
 
  public String getVersion() {
   return version;
  }
 
  public WlpInformation getWlpInformation() {
   return wlpInformationObject;
  }
 
  // Setter Methods 
 
  public void setDescription(String description) {
   this.description = description;
  }
 
  public void setLicenseId(String licenseId) {
   this.licenseId = licenseId;
  }
 
  public void setLicenseType(String licenseType) {
   this.licenseType = licenseType;
  }
 
  public void setName(String name) {
   this.name = name;
  }
 
  public void setProvider(Provider providerObject) {
   this.providerObject = providerObject;
  }
 
  public void setShortDescription(String shortDescription) {
   this.shortDescription = shortDescription;
  }
 
  public void setType(String type) {
   this.type = type;
  }
 
  public void setVersion(String version) {
   this.version = version;
  }
 
  public void setWlpInformation(WlpInformation wlpInformationObject) {
   this.wlpInformationObject = wlpInformationObject;
  }
 }
 
 
 

