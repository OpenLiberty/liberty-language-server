package io.openliberty.tools.langserver.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import io.openliberty.tools.langserver.ls.LibertyTextDocument;

public class ServerPropertyValues {
    private final static List<String> LOGGING_SOURCE_VALUES = Arrays.asList("message", "trace", "accessLog", "ffdc", "audit");
    
    private static HashMap<String, List<String>> validServerValues = new HashMap<String, List<String>>() {{
        put("WLP_LOGGING_CONSOLE_FORMAT", Arrays.asList("dev", "simple", "json"));
        put("WLP_LOGGING_CONSOLE_LOGLEVEL", Arrays.asList("INFO", "AUDIT", "WARNING", "ERROR", "OFF"));
        put("WLP_LOGGING_CONSOLE_SOURCE", LOGGING_SOURCE_VALUES);
        put("WLP_LOGGING_MESSAGE_FORMAT", Arrays.asList("simple", "json"));
        put("WLP_LOGGING_MESSAGE_SOURCE", LOGGING_SOURCE_VALUES);
    }};

    private static HashMap<String, List<String>> validBootstrapValues = new HashMap<String, List<String>>() {{
        put("com.ibm.ws.logging.trace.format", Arrays.asList("ENHANCED", "BASIC", "ADVANCED"));
        EquivalentProperties.getBootstrapKeys().forEach(
            bskey -> {
                String serverEnvEquivalent = EquivalentProperties.getEquivalentProperty(bskey);
                if(validServerValues.containsKey(serverEnvEquivalent)) {
                    this.put(bskey, validServerValues.get(serverEnvEquivalent));
                }
            }
        );
    }};

    public static List<String> getValidValues(LibertyTextDocument tdi, String key) {
        if (ParserFileHelperUtil.isBootstrapPropertiesFile(tdi)) {
            return validBootstrapValues.get(key);
        } else if (ParserFileHelperUtil.isServerEnvFile(tdi)) {
            return validServerValues.get(key);
        }
        return null;
    }
}
