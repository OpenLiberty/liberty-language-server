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
import org.apache.commons.lang3.LocaleUtils;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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

    /**
     * handles several common locale string conventions in a specific order of precedence:
     * It first attempts to parse the string using {@link Locale#forLanguageTag(String)}, which supports modern formats like "en-US". It replaces underscores with hyphens to ensure compatibility
     * If the first attempt fails, it uses {@link org.apache.commons.lang3.LocaleUtils#toLocale(String)} to handle older, underscore-separated formats like "en_US".
     * As a final fallback, it manually parses the string based on underscores to create a {@link Locale} object, supporting formats with up to three parts (language, country, variant)
     * If the input string is {@code null}, empty, or cannot be parsed by any of the methods, it returns a default locale, which is {@link Locale#US}.
     *
     * @param localeString The locale string to convert (e.g., "en-US", "fr_FR", "zh-Hans").
     * @return A valid {@link java.util.Locale} object, or {@link Locale#US} if the input is invalid.
     */
    public static Locale toLocale(String localeString) {

        // Try the modern standard first (e.g., "en-US", "en", "de")
        Locale locale = Locale.forLanguageTag(localeString.replace('_', '-'));
        if (!locale.getLanguage().isEmpty() || !locale.getCountry().isEmpty()) {
            return locale;
        }
        // Fallback 1: Use Apache Commons Lang's LocaleUtils (e.g., "en_US")
        try {
            return LocaleUtils.toLocale(localeString);
        } catch (IllegalArgumentException e) {
            // Ignore and proceed to the next fallback
        }
        // Fallback 2: Manual parsing for older formats or non-standard strings
        String[] parts = localeString.split("_");
        if (parts.length == 1) {
            return new Locale(parts[0]);
        } else if (parts.length == 2) {
            return new Locale(parts[0], parts[1]);
        } else if (parts.length == 3) {
            return new Locale(parts[0], parts[1], parts[2]);
        }
        // If all else fails, return a default locale
        LOGGER.warning("Setting locale as en_US as initializeParams locale is null");
        return Locale.US;
    }

    /**
     * lists all locales available in liberty
     * @return
     */
    public static List<Locale> getAvailableLocales() {
        return Arrays.asList(
                new Locale("pt", "BR"), Locale.SIMPLIFIED_CHINESE, Locale.TRADITIONAL_CHINESE,
                new Locale("cs", "CZ"), Locale.ENGLISH, Locale.FRENCH, Locale.GERMAN,
                new Locale("hu", "HU"), Locale.ITALIAN, Locale.JAPANESE, Locale.KOREAN,
                new Locale("pl", "PL"), new Locale("ro", "RO"), new Locale("ru", "RU"),
                new Locale("es", "ES")
        );
    }

    /**
     * Matches a locale string to the best available locale from a list.
     *
     * @param userLocale The locale string to match (e.g., "en-US", "en_GB", "fr").
     * @return The best matching locale from the list, or null if no match is found.
     */
    public static Locale findBestMatchingLocale(Locale userLocale) {
        // Check for an exact match first
        for (Locale availableLocale : getAvailableLocales()) {
            if (availableLocale.equals(userLocale)) {
                return availableLocale;
            }
        }
        // Check for a language-only match
        for (Locale availableLocale : getAvailableLocales()) {
            if (availableLocale.getLanguage().equals(userLocale.getLanguage())) {
                return availableLocale;
            }
        }
        // Check for a "parent" locale match (e.g., "en" for "en-US")
        for (Locale availableLocale : getAvailableLocales()) {
            if (userLocale.toLanguageTag().startsWith(availableLocale.toLanguageTag())) {
                return availableLocale;
            }
        }
        return null;
    }
}