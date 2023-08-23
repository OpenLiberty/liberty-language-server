package io.openliberty.tools.langserver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.lsp4j.FileEvent;

import io.openliberty.tools.langserver.utils.XmlReader;

public class LibertyConfigFileManager {
    private static final String CUSTOM_SERVER_ENV_XML_TAG = "serverEnvFile";
    private static final String CUSTOM_BOOTSTRAP_PREOPERTIES_XML_TAG = "bootstrapPropertiesFile";

    private static Set<String> customServerEnvFiles = new HashSet<String>();
    private static Set<String> customBootstrapFiles = new HashSet<String>();

    public static void processLibertyPluginConfigXml(FileEvent change) {
        Map<String, String> customConfigFiles = XmlReader.readLibertyPluginCfgXml(change.getUri(), CUSTOM_SERVER_ENV_XML_TAG, CUSTOM_BOOTSTRAP_PREOPERTIES_XML_TAG);
        for (Entry<String, String> entry : customConfigFiles.entrySet()) {
            processCustomConfigFileEntry(entry);
        }

    }

    private static void processCustomConfigFileEntry(Entry<String, String> entry) {
        String fileType = entry.getKey().toString();
        switch (fileType) {
            case CUSTOM_SERVER_ENV_XML_TAG:
                customServerEnvFiles.add(entry.getValue());
                break;
            case CUSTOM_BOOTSTRAP_PREOPERTIES_XML_TAG:
                customBootstrapFiles.add(entry.getValue());
                break;
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
}
