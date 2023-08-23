package io.openliberty.tools.langserver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.lsp4j.FileEvent;

import io.openliberty.tools.langserver.utils.XmlReader;

public class LibertyConfigFileManager {
    private static Set<String> customServerEnvFiles = new HashSet<String>();
    private static Set<String> customBootstrapFiles = new HashSet<String>();
    

    public static void processLibertyPluginConfigXml(FileEvent change) {
        XmlReader.readLibertyPluginCfgXml(change.getUri());
    }

    /**
     * Returns true if fileUri is a custom bootstrap.properties or server.env defined in the build file (after processed into liberty-plugin-config.xml)
     * @param fileUri
     * @return
     */
    public static boolean isCustomConfigFile(String fileUri) {
        return customBootstrapFiles.contains(fileUri) || customServerEnvFiles.contains(fileUri);
    }
}
