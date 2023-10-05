/*******************************************************************************
* Copyright (c) 2023 IBM Corporation and others.
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

public class FeatureListNode {
    protected String nodeName;
    protected Set<String> enabledBy;
    protected Set<String> enables;

    public FeatureListNode(String nodeName) {
        enabledBy = new HashSet<String>();
        enables = new HashSet<String>();
        this.nodeName = nodeName;
    }

    public void addEnabler(String nodeName) {
        enabledBy.add(nodeName);
    }

    public void addEnables(String nodeName) {
        enables.add(nodeName);
    }

    public Set<String> getEnablers() {
        return enabledBy;
    }

    public Set<String> getEnables() {
        return enables;
    }

    public boolean isConfigElement() {
        return this.nodeName.indexOf('.') == -1;
    }
}