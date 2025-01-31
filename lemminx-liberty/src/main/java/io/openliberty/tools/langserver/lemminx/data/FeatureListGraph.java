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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FeatureListGraph {
    private String runtime = "";
    private Map<String, FeatureListNode> featureNodes;
    private Map<String, ConfigElementNode> configElementNodes;
    private Map<String, Node> nodes;
    private Map<String, Set<String>> enabledByCache; 
    private Map<String, Set<String>> enabledByCacheLowerCase; // storing in lower case to enable diagnostics with configured features
    
    public FeatureListGraph() {
        nodes = new HashMap<String, Node>();
        featureNodes = new HashMap<String, FeatureListNode>();
        configElementNodes = new HashMap<String, ConfigElementNode>();
        enabledByCacheLowerCase = new HashMap<String, Set<String>>();
        enabledByCache = new HashMap<String, Set<String>>();
    }

    public FeatureListNode addFeature(String nodeName) {
        if (featureNodes.containsKey(nodeName)) {
            return featureNodes.get(nodeName);
        }
        FeatureListNode node = new FeatureListNode(nodeName);
        featureNodes.put(nodeName, node);
        nodes.put(nodeName, node);
        return node;
    }

    public FeatureListNode addFeature(String nodeName, String description) {
        if (featureNodes.containsKey(nodeName)) {
            FeatureListNode node = featureNodes.get(nodeName);
            if (node.getDescription().isEmpty()) {
                node.setDescription(description);
            }
            return node;
        }
        FeatureListNode node = new FeatureListNode(nodeName, description);
        featureNodes.put(nodeName, node);
        // in case of versionless features,
        // there are some config elements with same name as version less feature
        // such as mpMetrics, currently only one
        // use putIfAbsent because there might be already a configElement with enabledBy added
        // for version less features, config elements are not present in xsd
        nodes.putIfAbsent(nodeName, node);
        return node;
    }
    
    public ConfigElementNode addConfigElement(String nodeName) {
        if (configElementNodes.containsKey(nodeName)) {
            return configElementNodes.get(nodeName);
        }
        ConfigElementNode node = new ConfigElementNode(nodeName);
        configElementNodes.put(nodeName, node);
        nodes.put(nodeName, node);
        return node;
    }

    public FeatureListNode getFeatureListNode(String nodeName) {
        return featureNodes.get(nodeName);
    }

    public ConfigElementNode getConfigElementNode(String nodeName) {
        return configElementNodes.get(nodeName);
    }

    public boolean isEmpty() {
        return configElementNodes.isEmpty() && featureNodes.isEmpty();
    }
    
    public boolean isConfigElement(String name) {
        return configElementNodes.containsKey(name);
    }
    
    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public String getRuntime() {
        return this.runtime;
    }

    /**
     * Returns a superset of 'owning' features that enable a given config element or feature.
     * The features are returned in lower case to make the diagnostic code easier.
     * @param elementName
     * @return
     */
    public Set<String> getAllEnabledBy(String elementName) {
        return getAllEnabledBy(elementName, true);
    }
            
    /**
     * Returns a superset of 'owning' features that enable a given config element or feature.
     * The features are returned in lower case if the 'lowerCase' boolean is true. Otherwise,
     * the features are returned in their original case.
     * @param elementName
     * @return
     */
    public Set<String> getAllEnabledBy(String elementName, boolean lowerCase) {

        if (lowerCase && enabledByCacheLowerCase.containsKey(elementName)) {
            return enabledByCacheLowerCase.get(elementName);
        }

        if (enabledByCache.containsKey(elementName)) {
            return enabledByCache.get(elementName);
        }

        if (!nodes.containsKey(elementName)) {
            return null;
        }

        // Implements a breadth-first-search on parent nodes
        Set<String> allEnabledBy = new HashSet<String>(nodes.get(elementName).getEnabledBy());;
        Deque<String> queue = new ArrayDeque<String>(allEnabledBy);
        Set<String> visited = new HashSet<String>();
        while (!queue.isEmpty()) {
            String node = queue.getFirst();
            queue.removeFirst();
            if (visited.contains(node)) {
                continue;
            }
            Set<String> enablers = nodes.get(node).getEnabledBy();
            visited.add(node);
            allEnabledBy.addAll(enablers);
            queue.addAll(enablers);
        }
        return addToEnabledByCache(elementName, allEnabledBy, lowerCase);
    }

    private Set<String> addToEnabledByCache(String configElement, Set<String> allEnabledBy, boolean lowerCase) {
        Set<String> lowercaseEnabledBy = new HashSet<String>();
        Set<String> originalcaseEnabledBy = new HashSet<String>();
        originalcaseEnabledBy.addAll(allEnabledBy);

        for (String nextFeature: allEnabledBy) {
            lowercaseEnabledBy.add(nextFeature.toLowerCase());
        }

        enabledByCacheLowerCase.put(configElement, lowercaseEnabledBy);
        enabledByCache.put(configElement, originalcaseEnabledBy);

        return lowerCase ? lowercaseEnabledBy : originalcaseEnabledBy;
    }
   
}