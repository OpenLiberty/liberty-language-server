/*******************************************************************************
* Copyright (c) 2022 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*******************************************************************************/
package io.openliberty.tools.langserver.utils;

import java.util.Locale;
import java.util.MissingResourceException;
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
        if (serverEnvKeyFormat.matcher(key).matches()) { // server env
            message = serverenvMessages.getString(key);
        } else if (key.contains(".")) { // bootstrap property
            if (EquivalentProperties.hasEquivalentProperty(key)) { // bootstrap property has equivalent server.env property
                message = serverenvMessages.getString(EquivalentProperties.getEquivalentProperty(key));
            } else {
                message = bootstrapMessages.getString(key);
            }
        }
        return message == null ? key : message;
    }
}