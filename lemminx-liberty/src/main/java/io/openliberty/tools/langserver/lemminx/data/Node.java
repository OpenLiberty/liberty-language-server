/*******************************************************************************
* Copyright (c) 2024 IBM Corporation and others.
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

public class Node {
    protected String nodeName;
    protected Set<String> enabledBy;

    public Node(String nodeName) {
        enabledBy = new HashSet<String>();
        this.nodeName = nodeName;
    }
    
    public void addEnabledBy(String nodeName) {
        enabledBy.add(nodeName);
    }

    public Set<String> getEnabledBy() {
        return enabledBy;
    }
}