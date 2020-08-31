package io.openliberty.lemminx.liberty.util;

import org.eclipse.lemminx.dom.DOMDocument;

public class LibertyUtils {
    private LibertyUtils() {}

    public static boolean isServerXMLFile(String filePath) {
        return filePath.endsWith("/" + LibertyConstants.SERVER_XML);
    }

    public static boolean isServerXMLFile(DOMDocument file) {
        return file.getDocumentURI().endsWith("/" + LibertyConstants.SERVER_XML);
    }
}
