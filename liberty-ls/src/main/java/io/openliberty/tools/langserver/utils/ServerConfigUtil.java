package io.openliberty.tools.langserver.utils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;
import java.util.logging.Logger;

import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;

import io.openliberty.tools.common.plugins.config.ServerConfigDocument;
import io.openliberty.tools.langserver.CommonLogger;

//TODO: rename class to relate to ci.common
public class ServerConfigUtil {
    private final static Logger LOGGER = Logger.getLogger(ServerConfigUtil.class.getName());
    private static CommonLogger log = CommonLogger.getInstance(LOGGER);
    private static ServerConfigDocument configDocument = new ServerConfigDocument(log);

    // need method to request ci.common to parse vars from an xml file
    public static void requestParseXml(String uri) {
        Document doc;
        try {
            doc = configDocument.parseDocument(new File(URI.create(uri)));
            configDocument.parseVariablesForBothValues(doc);
        } catch (XPathExpressionException | IOException e) {
            // TODO Auto-generated catch block
            LOGGER.warning("Error trying to parse document: " + uri);
            e.printStackTrace();
        }
    }

    // need method to request Properties from ci.common
    public static Properties getProperties() {
        return configDocument.getProperties();
    }
}
