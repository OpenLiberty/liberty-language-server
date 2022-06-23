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

    /**
     * Returns the equivalent server.env property for the provided bootstrap property
     * @param bootstrapProperty
     * @return
     */
    public static String getEquivalentProperty(String bootstrapProperty) {
        return (String) bootstrapToServerEnvMap.get(bootstrapProperty);
    }

    public static boolean hasEquivalentProperty(String key) {
        return bootstrapToServerEnvMap.containsKey(key);
    }

    public static Set<String> getBootstrapKeys() {
        return bootstrapToServerEnvMap.keySet();
    }
}
