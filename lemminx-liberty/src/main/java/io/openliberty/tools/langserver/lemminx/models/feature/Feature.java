/*******************************************************************************
* Copyright (c) 2020, 2023 IBM Corporation and others.
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

import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "feature")
@XmlAccessorType(XmlAccessType.FIELD)
public class Feature {

    private String description;
    private String licenseId;
    private String licenseType;

    @XmlAttribute
    private String name;
    private String shortDescription;

    private String type;
    private String version;
    WlpInformation wlpInformation;

    private List<String> configElement;
    private List<String> enables;

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
        return wlpInformation;
    }

    public List<String> getConfigElements() {
        return configElement;
    }

    public List<String> getEnables() {
        return enables;
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

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setWlpInformation(WlpInformation wlpInformation) {
        this.wlpInformation = wlpInformation;
    }

    public void setConfigElements(List<String> configElement) {
        this.configElement = configElement;
    }

    public void setEnables(List<String> enables) {
        this.enables = enables;
    }
}
