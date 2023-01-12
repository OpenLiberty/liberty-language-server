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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Range;

import io.openliberty.tools.langserver.ls.LibertyTextDocument;

/**
 * Maps property keys with their list of valid values for server.env and bootstrap.properties
 */
public class ServerPropertyValues {
    public final static List<String> LOGGING_SOURCE_VALUES = Arrays.asList("message", "trace", "accessLog", "ffdc", "audit");
    public final static List<String> BOOLEAN_VALUES_DEFAULT_TRUE = Arrays.asList("true", "false");
    public final static List<String> BOOLEAN_VALUES_DEFAULT_FALSE = Arrays.asList("false", "true");
    public final static List<String> YES_NO_VALUES = Arrays.asList("y", "n");
    
    private static HashMap<String, List<String>> predefinedServerValues = new HashMap<String, List<String>>() {{
        put("WLP_DEBUG_SUSPEND", YES_NO_VALUES); // default yes
        put("WLP_DEBUG_REMOTE", YES_NO_VALUES); // default undefined/disabled, but will preselect "y" to enable
        
        put("WLP_LOGGING_CONSOLE_FORMAT", Arrays.asList("DEV", "SIMPLE", "JSON", "TBASIC")); // default "DEV"
        put("WLP_LOGGING_CONSOLE_LOGLEVEL", Arrays.asList("AUDIT", "INFO", "WARNING", "ERROR", "OFF")); // default "AUDIT"
        put("WLP_LOGGING_CONSOLE_SOURCE", LOGGING_SOURCE_VALUES); //default "message"
        put("WLP_LOGGING_MESSAGE_FORMAT", Arrays.asList("SIMPLE", "JSON", "TBASIC")); // default "SIMPLE"
        put("WLP_LOGGING_MESSAGE_SOURCE", LOGGING_SOURCE_VALUES); //default "message"
        put("WLP_LOGGING_APPS_WRITE_JSON", BOOLEAN_VALUES_DEFAULT_FALSE); //default false
    }};

    private static HashMap<String, List<String>> predefinedBootstrapValues = new HashMap<String, List<String>>() {{
        put("com.ibm.ws.logging.copy.system.streams", BOOLEAN_VALUES_DEFAULT_TRUE); // default true
        put("com.ibm.ws.logging.newLogsOnStart", BOOLEAN_VALUES_DEFAULT_TRUE); // default true
        put("com.ibm.ws.logging.isoDateFormat", BOOLEAN_VALUES_DEFAULT_FALSE); // default false
        put("com.ibm.ws.logging.trace.format", Arrays.asList("ENHANCED", "BASIC", "TBASIC", "ADVANCED")); // default "ENHANCED"
        put("websphere.log.provider", Arrays.asList("binaryLogging-1.0"));
        put("com.ibm.hpel.log.bufferingEnabled", BOOLEAN_VALUES_DEFAULT_TRUE); // default true
        put("com.ibm.hpel.trace.bufferingEnabled", BOOLEAN_VALUES_DEFAULT_TRUE); // default true
        EquivalentProperties.getBootstrapKeys().forEach(
            bskey -> {
                String serverEnvEquivalent = EquivalentProperties.getEquivalentProperty(bskey);
                if(predefinedServerValues.containsKey(serverEnvEquivalent)) {
                    this.put(bskey, predefinedServerValues.get(serverEnvEquivalent));
                }
            }
        );
    }};

    private static Set<String> caseSensitiveProperties = new HashSet<String>() {{
        add("WLP_DEBUG_SUSPEND");
        add("WLP_DEBUG_REMOTE");
        add("WLP_LOGGING_CONSOLE_SOURCE");
        add("WLP_LOGGING_MESSAGE_SOURCE");
        EquivalentProperties.getServerVarKeys().forEach(
            serverKey -> {
                if(this.contains(serverKey)) {
                    this.add(EquivalentProperties.getEquivalentProperty(serverKey));
                }
            }
        );
    }};

    private static HashMap<String, Range<Integer>> integerRangeValues = new HashMap<String, Range<Integer>>() {{
        put("WLP_DEBUG_ADDRESS", Range.between(1,65535));
        put("default.http.port", Range.between(1,65535));
        put("default.https.port", Range.between(1,65535));
        put("command.port", Range.between(-1,65535));
        put("server.start.wait.time", Range.between(0, Integer.MAX_VALUE));
        put("osgi.console", Range.between(1,65535));
        put("com.ibm.ws.logging.max.files", Range.between(0, Integer.MAX_VALUE));
        put("com.ibm.ws.logging.max.file.size", Range.between(0, Integer.MAX_VALUE));
        put("com.ibm.hpel.log.purgeMaxSize", Range.between(0, Integer.MAX_VALUE));
        put("com.ibm.hpel.log.purgeMinTime", Range.between(0, Integer.MAX_VALUE));
        put("com.ibm.hpel.log.fileSwitchTime", Range.between(0, 23));
        put("com.ibm.hpel.trace.purgeMaxSize", Range.between(0, Integer.MAX_VALUE));
        put("com.ibm.hpel.trace.purgeMinTime", Range.between(0, Integer.MAX_VALUE));
        put("com.ibm.hpel.trace.fileSwitchTime", Range.between(0, 23));
    }};

    public static boolean usesPredefinedValues(LibertyTextDocument tdi, String key) {
        if (ParserFileHelperUtil.isBootstrapPropertiesFile(tdi)) {
            return predefinedBootstrapValues.containsKey(key);
        } else if (ParserFileHelperUtil.isServerEnvFile(tdi)) {
            return predefinedServerValues.containsKey(key);
        }
        return false;
    }

    public static boolean usesIntegerRangeValue(String key) {
        return integerRangeValues.containsKey(key);
    }

    public static boolean usesTimeUnit(String key) {
        return key.endsWith("purgeMinTime");
    }

    public static boolean usesPackageNames(String key) {
        return key.equals("org.osgi.framework.bootdelegation");
    }

    public static List<String> getValidValues(LibertyTextDocument tdi, String key) {
        if (ParserFileHelperUtil.isBootstrapPropertiesFile(tdi)) {
            return predefinedBootstrapValues.getOrDefault(key, Collections.emptyList());
        } else if (ParserFileHelperUtil.isServerEnvFile(tdi)) {
            return predefinedServerValues.getOrDefault(key, Collections.emptyList());
        }
        return Collections.emptyList();
    }

    public static boolean isCaseSensitive(String key) {
        return caseSensitiveProperties.contains(key);
    }

    /**
     * Get integer range for given property, or null if the property does not use integers.
     * @param key Property name
     * @return Integer range for given property, otherwise null if the property does not use integers.
     */
    public static Range<Integer> getIntegerRange(String key) {
        return integerRangeValues.get(key);
    }
}
