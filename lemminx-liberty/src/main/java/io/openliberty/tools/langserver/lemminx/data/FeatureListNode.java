/*******************************************************************************
* Copyright (c) 2023, 2025 IBM Corporation and others.
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
package io.openliberty.tools.langserver.lemminx.data;

import java.util.HashSet;
import java.util.Set;


// Class to represent a feature in a feature list xml
public class FeatureListNode extends Node {
    protected String description;
    protected Set<String> enablesFeatures;
    protected Set<String> enablesConfigElements;
    protected boolean isVersionless;

    public FeatureListNode(String nodeName) {
        super(nodeName);
        enablesFeatures = new HashSet<String>();
        enablesConfigElements = new HashSet<String>();
    }

    public FeatureListNode(String nodeName, String description) {
        this(nodeName);
        this.description = description;
    }

    public void setIsVersionless(boolean isVersionless) {
        this.isVersionless = true;
    }

    public boolean isVersionless() {
        return this.isVersionless;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description == null ? "" : this.description;
    }

    public void addEnablesFeature(String nodeName) {
        enablesFeatures.add(nodeName);
    }

    public void addEnablesConfigElement(String nodeName) {
        enablesConfigElements.add(nodeName);
    }

    public Set<String> getEnablesFeatures() {
        return enablesFeatures;
    }

    public Set<String> getEnablesConfigElements() {
        return enablesConfigElements;
    }
}