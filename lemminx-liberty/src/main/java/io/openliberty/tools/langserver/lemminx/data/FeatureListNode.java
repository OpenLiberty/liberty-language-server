/*******************************************************************************
* Copyright (c) 2023, 2024 IBM Corporation and others.
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


// Class to represent a feature OR config element in a feature list xml
public class FeatureListNode {
    protected String nodeName;
    protected String description;  // only used for features
    protected Set<String> enabledBy;
    protected Set<String> enables;

    public FeatureListNode(String nodeName) {
        enabledBy = new HashSet<String>();
        enables = new HashSet<String>();
        this.nodeName = nodeName;
    }

    public FeatureListNode(String nodeName, String description) {
        enabledBy = new HashSet<String>();
        enables = new HashSet<String>();
        this.nodeName = nodeName;
        this.description = description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description == null ? "" : this.description;
    }
    
    public void addEnabledBy(String nodeName) {
        enabledBy.add(nodeName);
    }

    public void addEnables(String nodeName) {
        enables.add(nodeName);
    }

    public Set<String> getEnabledBy() {
        return enabledBy;
    }

    public Set<String> getEnables() {
        return enables;
    }

    public Set<String> getEnablesFeatures() {
        Set<String> enablesFeatures = new HashSet<String>();
        for (String next: enables) {
            if (next.contains("-")) {
                enablesFeatures.add(next);
            }
        }
        return enablesFeatures;
    }

    // based on a heuristic that features use major versions and config elements don't use '.'
    public boolean isConfigElement() {
        return this.nodeName.indexOf('.') == -1;
    }
}