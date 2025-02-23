/*******************************************************************************
* Copyright (c) 2022, 2023 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*******************************************************************************/
package io.openliberty.tools.langserver.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import io.openliberty.tools.langserver.LibertyConfigFileManager;
import io.openliberty.tools.langserver.ls.LibertyTextDocument;

public class Messages {
    private static ResourceBundle serverenvMessages = null;
    private static ResourceBundle bootstrapMessages = null;

    private static List<String> serverPropertyKeys = null;
    private static List<String> bootstrapPropertyKeys = null;

    private static final Logger LOGGER = Logger.getLogger(Messages.class.getName());

    // Matches server.env key formats: all uppercase, possibly with underscores and additional uppercase words
    private static Pattern serverEnvKeyFormat = Pattern.compile("([A-Z]+)(_[A-Z]+)*");

    private static synchronized void initializeBundles() {
        Locale locale = Locale.getDefault();
        serverenvMessages = ResourceBundle.getBundle("ServerEnv", locale);
        bootstrapMessages = ResourceBundle.getBundle("BootstrapProperties", locale);

        serverPropertyKeys = Collections.list(serverenvMessages.getKeys());
        bootstrapPropertyKeys = Collections.list(bootstrapMessages.getKeys());
        bootstrapPropertyKeys.addAll(EquivalentProperties.getBootstrapKeys()); // add bootstrap properties that are not listed in BootstrapProperties.properties
    }

    /**
     * Return description for property defined in server.env and bootstrap.properties files
     * @param key
     * @return Description of property
     */
    public static String getPropDescription(String key) {
        if (serverenvMessages == null) {
            initializeBundles();
        }
        String message = null;
        try {
            if (serverEnvKeyFormat.matcher(key).matches()) { // server env
                message = serverenvMessages.getString(key);
            } else if (key.contains(".")) { // bootstrap property
                if (EquivalentProperties.hasEquivalentProperty(key)) { // bootstrap property has equivalent server.env property
                    message = serverenvMessages.getString(EquivalentProperties.getEquivalentProperty(key));
                } else {
                    message = bootstrapMessages.getString(key);
                }
            }
        } catch (MissingResourceException e) {
            // Caught to avoid unnecessary console output when hovering over invalid keys
            LOGGER.info("No property description found for: " + key);
        }
        return message == null ? key : message;
    }

    /**
     * Returns available property keys for server.env and bootstrap.properties files
     * @param query
     * @param textDocument
     * @return List of completion items for either server.env or bootstrap.properties
     */
    public static List<String> getMatchingKeys(String query, LibertyTextDocument textDocument) {
        if (serverenvMessages == null) {
            initializeBundles();
        }
        
        // remove completion results that don't contain the query string (case-insensitive search)
        Predicate<String> filter = s -> {
            for (int i = s.length() - query.length(); i >= 0; --i) {
                if (s.regionMatches(true, i, query, 0, query.length()))
                    return false;
            }
            return true;
        };
        if (LibertyConfigFileManager.isServerEnvFile(textDocument)) { // server.env file
            List<String> keys = new ArrayList<String>(serverPropertyKeys);
            keys.removeIf(filter);
            return keys;
        } else if (LibertyConfigFileManager.isBootstrapPropertiesFile(textDocument)) { // bootstrap.properties file
            List<String> keys = new ArrayList<String>(bootstrapPropertyKeys);
            keys.removeIf(filter);
            return keys;
        } else {
            return Collections.emptyList();
        }
    }
}
