/*******************************************************************************
* Copyright (c) 2020, 2024 IBM Corporation and others.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
    public static final Map<String, String> platformDescriptionMap = Collections.unmodifiableMap(new HashMap<>() {{
        put("javaee", "Description: This platform resolves the Liberty features that support the Java EE %s platform.");
        put("jakartaee", "Description: This platform resolves the Liberty features that support the Jakarta EE %s platform.");
        put("microprofile", "Description: This platform resolves the Liberty features that support the MicroProfile %s for Cloud Native Java platform.");
    }});

    public static final Map<String, String> conflictingPlatforms = Collections.unmodifiableMap(new HashMap<>() {{
        put("javaee", "jakartaee");
        put("jakartaee", "javaee");
    }});

    public static final Map<String, String> changedFeatureNameMap = Collections.unmodifiableMap(new HashMap<>() {{
        put("jsp-", "pages-");
        put("ejb-", "enterprisebeans-");
    }});
}
