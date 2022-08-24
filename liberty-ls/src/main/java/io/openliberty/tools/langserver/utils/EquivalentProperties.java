/*******************************************************************************
* Copyright (c) 2022 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*******************************************************************************/
package io.openliberty.tools.langserver.utils;

import java.util.HashMap;
import java.util.Set;

/**
 * Maps bootstrap.properties keys to equivalent keys used in server.env
 */
public class EquivalentProperties {
    private static HashMap<String, String> bootstrapToServerEnvMap = new HashMap<String, String>() {{
        put("com.ibm.ws.logging.json.field.mappings", "WLP_LOGGING_JSON_FIELD_MAPPINGS");
        put("com.ibm.ws.logging.log.directory", "LOG_DIR");
        put("com.ibm.ws.logging.console.format", "WLP_LOGGING_CONSOLE_FORMAT");
        put("com.ibm.ws.logging.console.log.level", "WLP_LOGGING_CONSOLE_LOGLEVEL");
        put("com.ibm.ws.logging.console.source", "WLP_LOGGING_CONSOLE_SOURCE");
        put("com.ibm.ws.logging.message.format", "WLP_LOGGING_MESSAGE_FORMAT");
        put("com.ibm.ws.logging.message.source", "WLP_LOGGING_MESSAGE_SOURCE");
        put("com.ibm.ws.logging.apps.write.json", "WLP_LOGGING_APPS_WRITE_JSON");
        put("com.ibm.ws.json.access.log.fields", "WLP_LOGGING_JSON_ACCESS_LOG_FIELDS");
    }};

    private static HashMap<String, String> serverEnvToBootstrapMap = new HashMap<String, String>() {{
        bootstrapToServerEnvMap.forEach((key, value) -> this.put(value, key));
    }};

    /**
     * Returns the equivalent server.env property for the provided bootstrap property, or vice versa
     * @param key
     * @return
     */
    public static String getEquivalentProperty(String key) {
        String equivalentProperty = bootstrapToServerEnvMap.get(key);
        if (equivalentProperty == null) {
            equivalentProperty = serverEnvToBootstrapMap.get(key);
        }
        return equivalentProperty;
    }

    public static boolean hasEquivalentProperty(String key) {
        return bootstrapToServerEnvMap.containsKey(key) || serverEnvToBootstrapMap.containsKey(key);
    }

    public static Set<String> getBootstrapKeys() {
        return bootstrapToServerEnvMap.keySet();
    }

    public static Set<String> getServerVarKeys() {
        return serverEnvToBootstrapMap.keySet();
    }
}
