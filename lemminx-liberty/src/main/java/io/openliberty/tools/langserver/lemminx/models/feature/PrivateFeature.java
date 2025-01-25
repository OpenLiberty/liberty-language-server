/*******************************************************************************
* Copyright (c) 2025 IBM Corporation and others.
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
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "privateFeature")
@XmlAccessorType(XmlAccessType.FIELD)
public class PrivateFeature {

    private String symbolicName;
    private List<String> platform;

    // Getter Methods

    public String getSymbolicName() {
        return symbolicName;
    }

    public List<String> getPlatforms() {
        return platform;
    }

    // Setter Methods

    public void setSymbolicName(String symbolicName) {
        this.symbolicName = symbolicName;
    }

    public void setPlatforms(List<String> platforms) {
        this.platform = platforms;
    }
}
