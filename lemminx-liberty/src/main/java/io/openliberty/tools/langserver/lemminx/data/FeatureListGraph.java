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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FeatureListGraph {
    private Map<String, FeatureListNode> nodes;
    // private Map<String, Set<String>> enablersCache;
    // private Map<String, Set<String>> enablesCache;

    public FeatureListGraph() {
        nodes = new HashMap<String, FeatureListNode>();
        // enablersCache = new HashMap<String, Set<String>>();
        // enablesCache = new HashMap<String, Set<String>>();
    }

    public FeatureListNode addFeature(String nodeName) {
        if (nodes.containsKey(nodeName)) {
            return nodes.get(nodeName);
        }
        FeatureListNode node = new FeatureListNode(nodeName);
        nodes.put(nodeName, node);
        return node;
    }

    public FeatureListNode addConfigElement(String nodeName) {
        if (nodes.containsKey(nodeName)) {
            return nodes.get(nodeName);
        }
        FeatureListNode node = new FeatureListNode(nodeName);
        nodes.put(nodeName, node);
        return node;
    }

    public FeatureListNode get(String nodeName) {
        return nodes.get(nodeName);
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }
    
    public boolean isConfigElement(String featureListNode) {
        if (!nodes.containsKey(featureListNode)) {
            return false;
        }
        return nodes.get(featureListNode).isConfigElement();
    } 

    /**
     * Returns a superset of 'owning' features that enable a given config element or feature.
     * @param elementName
     * @return
     */
    public Set<String> getAllEnablers(String elementName) {
        // if (enablersCache.containsKey(elementName)) {
        //     return enablersCache.get(elementName);
        // }
        if (!nodes.containsKey(elementName)) {
            return null;
        }
        Set<String> allEnablers = nodes.get(elementName).getEnablers();
        Deque<String> queue = new ArrayDeque<String>(allEnablers);
        Set<String> visited = new HashSet<String>();
        while (!queue.isEmpty()) {
            String node = queue.getFirst();
            queue.removeFirst();
            if (visited.contains(node)) {
                continue;
            }
            Set<String> enablers = nodes.get(node).getEnablers();
            visited.add(node);
            allEnablers.addAll(enablers);
            queue.addAll(enablers);
        }
        return allEnablers;
    }

    /**
     * Returns the set of supported features or config elements for a given feature.
     * @param feature
     * @return
     */
    public Set<String> getAllEnables(String feature) {
        // if (enablesCache.containsKey(feature)) {
        //     return enablesCache.get(feature);
        // }
        if (!nodes.containsKey(feature)) {
            return null;
        }
        Set<String> allEnables = nodes.get(feature).getEnables();
        Deque<String> queue = new ArrayDeque<String>(allEnables);
        Set<String> visited = new HashSet<String>();
        while (!queue.isEmpty()) {
            String node = queue.getFirst();
            queue.removeFirst();
            if (visited.contains(node)) {
                continue;
            }
            Set<String> enablers = nodes.get(node).getEnables();
            visited.add(node);
            allEnables.addAll(enablers);
            queue.addAll(enablers);
        }
        // enablesCache.put(feature, allEnables);
        return allEnables;
    }

    // public Set<String> getAllConfigElements(String feature) {
    //     Set<String> configElements = new HashSet<String>();
    //     for (String node : getAllEnables(feature)) {
    //         if (isConfigElement(node)) {
    //             configElements.add(node);
    //         }
    //     }
    //     return configElements;
    // }

    // public Set<String> getAllEnabledFeatures(String feature) {
    //     Set<String> enabledFeatures = new HashSet<String>();
    //     for (String node : getAllEnables(feature)) {
    //         if (!isConfigElement(node)) {
    //             enabledFeatures.add(node);
    //         }
    //     }
    //     return enabledFeatures;
    // }
}