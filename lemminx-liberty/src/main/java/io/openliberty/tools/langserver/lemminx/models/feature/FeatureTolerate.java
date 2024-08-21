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
package io.openliberty.tools.langserver.lemminx.models.feature;

import java.util.ArrayList;

public class FeatureTolerate {


    ArrayList<String> tolerates;
    private String feature;

    public ArrayList<String> getTolerates() {
        return tolerates;
    }

    public void setTolerates(ArrayList<String> tolerates) {
        this.tolerates = tolerates;
    }

    public String getFeature() {
        return feature;
    }

    public void setFeature(String feature) {
        this.feature = feature;
    }
}

