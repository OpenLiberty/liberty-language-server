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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import io.openliberty.tools.langserver.lemminx.util.LibertyConstants;

public class FeaturesAndPlatforms {
    private List<Feature> publicFeatures;
    private List<Feature> privateFeatures;
    private Set<String> platforms;
    
    public FeaturesAndPlatforms(List<Feature> publicFeatures, List<Feature> privateFeatures) {
        this.publicFeatures = publicFeatures;
        this.privateFeatures = privateFeatures;

        this.platforms = privateFeatures.stream()
                .map(Feature::getWlpInformation)
                .filter(Objects::nonNull)
                .filter(w -> Objects.nonNull(w.getVisibility()))
                .filter(w -> w.getVisibility().equals(LibertyConstants.PRIVATE_VISIBILITY))
                .map(WlpInformation::getPlatforms)
                .filter(Objects::nonNull)
                .flatMap(List::stream).collect(Collectors.toSet());
    }

    public FeaturesAndPlatforms() {
        this.publicFeatures = new ArrayList<>();
        this.privateFeatures = new ArrayList<>();
        this.platforms = new HashSet<>();
    }

    public void addFeaturesAndPlatforms(FeaturesAndPlatforms fp) {
        this.publicFeatures.addAll(fp.getPublicFeatures());
        this.privateFeatures.addAll(fp.getPrivateFeatures());
        this.platforms.addAll(fp.getPlatforms());
    }

    public List<Feature> getPublicFeatures() {
        return this.publicFeatures;
    }

    public List<Feature> getPrivateFeatures() {
        return this.privateFeatures;
    }

    public Set<String> getPlatforms() {
        return this.platforms;
    }
}
