/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.tools.langserver.lemminx.util;

import io.openliberty.tools.langserver.lemminx.services.SettingsService;

import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Util class for loading a localized resource bundle
 */
public final class ResourceBundleUtil {
    private static final Logger LOGGER = Logger.getLogger(ResourceBundleUtil.class.getName());
    private static ResourceBundle resourceBundle = null;

    private ResourceBundleUtil() {
    }

    private static synchronized void initializeBundles() {
        resourceBundle = ResourceBundle.getBundle("messages.libertyBundles", SettingsService.getInstance().getCurrentLocale());
    }

    /**
     * Returns message for the given key defined in resource bundle file.
     *
     * @param key  the given key
     * @param args replacements
     * @return Returns message for the given key defined in resource bundle file
     */
    public static String getMessage(String key, Object... args) {
        if (resourceBundle == null) {
            initializeBundles();
        }
        String msg = null;
        try {
            msg = resourceBundle.getString(key);
            if (args != null && args.length > 0) {
                msg = MessageFormat.format(msg, args);
            }
        } catch (Exception e) {
            LOGGER.info("Failed to get message for '" + key + "'");
        }
        return (msg == null) ? key : msg;
    }

}