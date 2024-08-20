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
package io.openliberty.tools.langserver.lemminx.util;

import java.util.HashMap;

public final class LibertyConstants {
    private LibertyConstants() {
    }

    public static final String SERVER_XML = "server.xml";

    public static final String SERVER_ELEMENT = "server";

    public static final String FEATURE_MANAGER_ELEMENT = "featureManager";
    public static final String FEATURE_ELEMENT = "feature";
    public static final String INCLUDE_ELEMENT = "include";
    public static final String PLATFORM_ELEMENT = "platform";
    public static final String PUBLIC_VISIBILITY = "PUBLIC";

    // following URI standard of using "/"
    public static final String WLP_USER_CONFIG_DIR = "/usr/shared/config/";
    public static final String SERVER_CONFIG_DROPINS_DEFAULTS = "/configDropins/defaults/";
    public static final String SERVER_CONFIG_DROPINS_OVERRIDES = "/configDropins/overrides/";

    // map to load description for features if description is not present in feature.json
    public static final HashMap<String, String> featureDescriptionMap = new HashMap<>() {{
        put("javaee-6.0", "Description: This feature combines the Liberty features that support the Java EE 6.0 Full Platform.");
        put("microProfile-7.0", "Description: This feature combines the Liberty features that support MicroProfile 7.0 for Cloud Native Java.");
    }};

    public static final HashMap<String, String> conflictingPlatforms = new HashMap<>() {{
        put("javaee", "jakartaee");
        put("jakartaee", "javaee");
    }};
}
