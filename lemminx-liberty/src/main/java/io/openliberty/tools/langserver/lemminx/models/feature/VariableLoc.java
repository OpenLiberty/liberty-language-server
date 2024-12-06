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

public class VariableLoc {

    private String value;
    private int startLoc;
    private int endLoc;

    public VariableLoc(String value, int startLoc, int endLoc) {
        this.value = value;
        this.startLoc = startLoc;
        this.endLoc = endLoc;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getStartLoc() {
        return startLoc;
    }

    public void setStartLoc(int startLoc) {
        this.startLoc = startLoc;
    }

    public int getEndLoc() {
        return endLoc;
    }

    public void setEndLoc(int endLoc) {
        this.endLoc = endLoc;
    }
}
