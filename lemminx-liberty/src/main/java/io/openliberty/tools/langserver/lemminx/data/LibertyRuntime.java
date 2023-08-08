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

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

public class LibertyRuntime {

    private String runtimeType = null;
    private String runtimeVersion = null;
    private String runtimeLocation = null;

    public LibertyRuntime(Path propertiesFilePath) {

        try {
            FileInputStream fis = new FileInputStream(propertiesFilePath.toFile());
            Properties prop = new Properties();
            prop.load(fis);

            this.runtimeVersion = prop.getProperty("com.ibm.websphere.productVersion");
            this.runtimeType = prop.getProperty("com.ibm.websphere.productId").equals("io.openliberty") ? "ol" : "wlp";

            // only set the location if this is not a container
            if (!propertiesFilePath.getFileName().equals("container.properties")) {
                this.runtimeLocation = propertiesFilePath.getParent().getParent().getParent().toString();
            }

        } catch (IOException e) {

        }
    }

    public LibertyRuntime(String type, String version, String location) {
        this.runtimeLocation = location;
        this.runtimeType = type;
        this.runtimeVersion = version;
    }
    
    public void setRuntimeType(String type) {
        this.runtimeType = type;
    }

    public String getRuntimeType() {
        return this.runtimeType;
    }

    public void setRuntimeVersion(String version) {
        this.runtimeVersion = version;
    }

    public String getRuntimeVersion() {
        return this.runtimeVersion;
    }

    public void setRuntimeLocation(Path location) {
        this.runtimeLocation = location.toString();
    }

    public String getRuntimeLocation() {
        return this.runtimeLocation;
    }

    public String getRuntimeInfo() {
        String runtime = this.runtimeType == null ? "" : this.runtimeType;
        String version = this.runtimeVersion == null ? "" : this.runtimeVersion;

        return runtime + "-" + version;
    }
}
