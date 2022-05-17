package io.openliberty.tools.langserver.utils;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class Messages {
    private static ResourceBundle serverenvMessages = null;
    private static ResourceBundle bootstrapMessages = null;

    // Matches server.env key formats: all uppercase, possibly with underscores and additional uppercase words
    private static Pattern serverEnvKeyFormat = Pattern.compile("([A-Z]+)(_[A-Z]+)*");

    private static synchronized void initializeBundles() {
        Locale locale = Locale.getDefault(); //TODO: properly set/get locale
        serverenvMessages = ResourceBundle.getBundle("ServerEnv", locale);
        bootstrapMessages = ResourceBundle.getBundle("BootstrapProperties", locale);
    }

    public static String getPropDescription(String key) {
        if (serverenvMessages == null) {
            initializeBundles();
        }
        String message = null;
        if (serverEnvKeyFormat.matcher(key).matches()) { // server env
            message = serverenvMessages.getString(key);
        } else if (key.contains(".")) { // bootstrap property
            message = bootstrapMessages.getString(key);
            // 2nd lookup for properties that have a server.env equivalent
            if (serverEnvKeyFormat.matcher(message).matches()) {
                message = serverenvMessages.getString(message);
            }
        }
        return message == null ? key : message;
    }
}
