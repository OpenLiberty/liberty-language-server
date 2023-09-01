package io.openliberty.tools.langserver;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.PathMatcher;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import io.openliberty.tools.langserver.ls.LibertyTextDocument;
import io.openliberty.tools.langserver.utils.XmlReader;

public class LibertyConfigFileManager {
    public static final String CUSTOM_SERVER_ENV_XML_TAG = "serverEnvFile";
    public static final String CUSTOM_BOOTSTRAP_PREOPERTIES_XML_TAG = "bootstrapPropertiesFile";

    private static Set<String> customServerEnvFiles = new HashSet<String>();
    private static Set<String> customBootstrapFiles = new HashSet<String>();

    private static final Logger LOGGER = Logger.getLogger(LibertyConfigFileManager.class.getName());

    public static void processLibertyPluginConfigXml(String uri) {
        if (!uri.endsWith("liberty-plugin-config.xml")) {
            return;
        }
        Map<String, String> customConfigFiles = XmlReader.readTagsFromXml(uri, 
                CUSTOM_SERVER_ENV_XML_TAG, 
                CUSTOM_BOOTSTRAP_PREOPERTIES_XML_TAG);
        // TODO: handle deletions. maybe use map with <uri, path> ? and clear all that match uri
        if (customConfigFiles.containsKey(CUSTOM_SERVER_ENV_XML_TAG)) {
            customServerEnvFiles.add(customConfigFiles.get(CUSTOM_SERVER_ENV_XML_TAG));
        }
        if (customConfigFiles.containsKey(CUSTOM_BOOTSTRAP_PREOPERTIES_XML_TAG)) {
            customBootstrapFiles.add(customConfigFiles.get(CUSTOM_BOOTSTRAP_PREOPERTIES_XML_TAG));
        }
    }

    /**
     * Returns true if fileUri is a custom bootstrap.properties or server.env defined in the build file (after processed into liberty-plugin-config.xml)
     * @param fileUri
     * @return
     */
    public static boolean isCustomConfigFile(String fileUri) {
        return customBootstrapFiles.contains(fileUri) || customServerEnvFiles.contains(fileUri);
    }

    public static boolean isServerEnvFile(LibertyTextDocument tdi) {
        return isServerEnvFile(tdi.getUri());
    }

    /**
     * Checks if file matches one of these conditions:
     * - is default server.env file in `src/main/liberty/config`
     * - is custom env file specified in liberty-plugin-config.xml (generated from build file)
     * @param uri
     * @return
     */
    public static boolean isServerEnvFile(String uri) {
        String path = null;
        try {
            path = new URI(uri).getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return uri.endsWith("src/main/liberty/config/server.env") || customServerEnvFiles.contains(path);
    }

    public static boolean isBootstrapPropertiesFile(LibertyTextDocument tdi) {
        return isBootstrapPropertiesFile(tdi.getUri());
    }

    /**
     * Checks if file matches one of these conditions:
     * - is default bootstrap.properties file in `src/main/liberty/config`
     * - is custom properties file specified in liberty-plugin-config.xml (generated from build file)
     * @param uri
     * @return
     */
    public static boolean isBootstrapPropertiesFile(String uri) {
        String path = null;
        try {
            path = new URI(uri).getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return uri.endsWith("src/main/liberty/config/bootstrap.properties") || customBootstrapFiles.contains(path);
    }
}
