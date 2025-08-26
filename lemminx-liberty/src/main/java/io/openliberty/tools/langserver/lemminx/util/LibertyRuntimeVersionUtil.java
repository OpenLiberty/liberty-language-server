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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Locale;
import java.util.logging.Logger;

import io.openliberty.tools.langserver.lemminx.services.SettingsService;
import org.apache.tools.ant.taskdefs.Local;
import org.eclipse.lemminx.uriresolver.CacheResourcesManager;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * A utility to find the latest version of the Open Liberty feature list,
 * first by attempting an HTTP download and then by falling back to a local cache.
 */
public class LibertyRuntimeVersionUtil {

    private LibertyRuntimeVersionUtil() {
    }
    // The URL for the maven-metadata.xml file.
    private static String METADATA_URL = "https://repo1.maven.org/maven2/io/openliberty/features/open_liberty_featurelist/maven-metadata.xml";

    // The path to the local cache file, following the specified convention.
    private static String CACHE_BASE_DIR = System.getProperty("user.home") + File.separator + ".lemminx" + File.separator + "cache";
    private static String CACHE_FILE_PATH = CACHE_BASE_DIR + File.separator + "https" + File.separator + "repo1.maven.org" + File.separator + "maven2" + File.separator + "io" + File.separator + "openliberty" + File.separator + "features" + File.separator + "open_liberty_featurelist" + File.separator + "maven-metadata.xml";
    private static final Logger LOGGER = Logger.getLogger(LibertyRuntimeVersionUtil.class.getName());

    /**
     * Attempts to get the latest version from either the remote repository or the local cache.
     *
     * @return The latest version string, or null if it cannot be found.
     */
    public static String getLatestVersion() {
        // Attempt to fetch from the remote URL first.
        String versionFromRemote = getVersionFromRemote();
        if (versionFromRemote != null) {
            LOGGER.fine("Successfully retrieved version from remote URL.");
            return versionFromRemote;
        }

        // If the remote call fails, fall back to the local cache.
        String versionFromLocalCache = getVersionFromLocalCache();
        if (versionFromLocalCache != null) {
            LOGGER.fine("Falling back to local cache.");
            return versionFromLocalCache;
        }

        // If both attempts fail, return null.
        return null;
    }

    /**
     * Fetches the maven-metadata.xml file from the remote repository and parses it.
     * If successful, it also saves the file to the local cache.
     *
     * @return The latest version string, or null on failure.
     */
    private static String getVersionFromRemote() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(METADATA_URL))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String xmlContent = response.body();
                // Save the content to the local cache before parsing.
                saveToLocalCache(xmlContent);
                return parseVersionFromXml(xmlContent);
            } else {
                LOGGER.warning("HTTP request failed with status code: %d".formatted(response.statusCode()));
                return null;
            }
        } catch (IOException | InterruptedException e) {
            // Handle network errors or request interruption.
            LOGGER.warning("Failed to get version from remote URL: " + e.getMessage());
            return null;
        }
    }

    /**
     * Reads the maven-metadata.xml file from the local cache and parses it.
     *
     * @return The latest version string, or null if the file does not exist or parsing fails.
     */
    private static String getVersionFromLocalCache() {
        Path filePath = Paths.get(CACHE_FILE_PATH);
        if (Files.exists(filePath)) {
            LOGGER.fine("Found local cache file at: %s".formatted(filePath));
            try {
                // Read the file content and parse the XML.
                String xmlContent = Files.readString(filePath);
                return parseVersionFromXml(xmlContent);
            } catch (IOException e) {
                LOGGER.fine("Failed to read local cache file: " + e.getMessage());
                return null;
            }
        } else {
            LOGGER.warning("Local cache file not found at: %s".formatted(filePath));
            return null;
        }
    }

    /**
     * Saves the XML content to the specified local cache file path.
     *
     * @param xmlContent The XML content to save.
     */
    private static void saveToLocalCache(String xmlContent) {
        Path filePath = Paths.get(CACHE_FILE_PATH);
        try {
            // Ensure the parent directories exist before writing the file.
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, xmlContent);
            LOGGER.fine("Successfully saved content to local cache.");
        } catch (IOException e) {
            LOGGER.warning("Failed to save content to local cache: " + e.getMessage());
        }
    }

    /**
     * A helper method to parse the 'latest' version from a maven-metadata.xml string.
     *
     * @param xmlContent The XML content as a string.
     * @return The version string, or null if not found or on parsing error.
     */
    private static String parseVersionFromXml(String xmlContent) {
        try {
            Document doc = DocumentUtil.getDocument(xmlContent);
            doc.getDocumentElement().normalize();

            // Find the <latest> tag.
            NodeList nodeList = doc.getElementsByTagName("latest");
            if (nodeList.getLength() > 0) {
                return nodeList.item(0).getTextContent();
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to parse metadata XML: " + e.getMessage());
        }
        return null;
    }

    /**
     * get url from maven repo
     * if locale is provided, use different url
     * @return schemaURl parsed
     */
    private static String parseURL(String url, String urlWithLocale) {
        String rtVersion = SettingsService.getInstance().getLatestRuntimeVersion();
        String schemaURL;
        if (SettingsService.getInstance().getCurrentLocale() == null || SettingsService.getInstance().getCurrentLocale().equals(Locale.US)
                || SettingsService.getInstance().getCurrentLocale().equals(Locale.ENGLISH) || urlWithLocale==null) {
            schemaURL = url.replace("$VERSION", rtVersion);
        } else {
            Locale currentLocale=SettingsService.getInstance().getCurrentLocale();
            // liberty published file names contain only language code as locale for all locales except Portuguese (Brazil) and Traditional Chinese
            // filename samples
            // https://repo1.maven.org/maven2/io/openliberty/features/open_liberty_schema_es/25.0.0.8/open_liberty_schema_es-25.0.0.8.xsd
            // https://repo1.maven.org/maven2/io/openliberty/features/open_liberty_schema_pt_BR/25.0.0.8/open_liberty_schema_pt_BR-25.0.0.8.xsd
            schemaURL = urlWithLocale.replace("$VERSION", rtVersion).replace("$LOCALE", currentLocale.getLanguage());
            if(Locale.TAIWAN.equals(currentLocale) || new Locale("pt","BR").equals(currentLocale)){
                schemaURL = urlWithLocale.replace("$VERSION", rtVersion).replace("$LOCALE", currentLocale.toString());
            }
        }
        return schemaURL;
    }

    /**
     * @param url           schema or featurelist url
     * @param urlWithLocale url with locale placeholder
     * @return downloaded
     */
    public static Path downloadAndCacheLatestResource(String url, String urlWithLocale) {
        Path serverResourceFile = null;
        // check and download latest resource
        if (SettingsService.getInstance().getLatestRuntimeVersion() != null) {
            String resourceURL = parseURL(url, urlWithLocale);
            try {
                LOGGER.info("Trying to download and cache resource %s, cached data will be returned if found".formatted(resourceURL));
                // getResource first checks in cache and if not exist, download resources
                serverResourceFile = new CacheResourcesManager().getResource(resourceURL);
            } catch (Exception e) {
                LOGGER.warning("Unable to download latest version schema and file is not in cache as well.. Falling back to default version 25.0.0.6");
            }
        }
        return serverResourceFile;
    }
}
