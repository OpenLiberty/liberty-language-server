package io.openliberty.tools.langserver;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

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
    // TODO: separate into two methods for server/bootstrap files
    public static boolean isCustomConfigFile(String fileUri) {
        return customBootstrapFiles.contains(fileUri) || customServerEnvFiles.contains(fileUri);
    }

    public static boolean isServerEnvFile(LibertyTextDocument tdi) {
        return isServerEnvFile(tdi.getUri());
    }

    public static boolean isServerEnvFile(String uri) {
        String path = null;
        try {
            path = new URI(uri).getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return uri.endsWith("server.env") || customServerEnvFiles.contains(path);
    }

    public static boolean isBootstrapPropertiesFile(LibertyTextDocument tdi) {
        return isBootstrapPropertiesFile(tdi.getUri());
    }

    public static boolean isBootstrapPropertiesFile(String uri) {
        String path = null;
        try {
            path = new URI(uri).getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return uri.endsWith("bootstrap.properties") || customBootstrapFiles.contains(path);
    }
}
