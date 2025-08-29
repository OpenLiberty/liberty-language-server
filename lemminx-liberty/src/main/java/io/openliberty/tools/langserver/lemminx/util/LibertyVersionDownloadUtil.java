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
import org.eclipse.lemminx.uriresolver.CacheResourceDownloadingException;
import org.eclipse.lemminx.uriresolver.CacheResourcesManager;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import static io.openliberty.tools.langserver.lemminx.util.LibertyConstants.BRAZIL_PORTUGESE_LOCALE;
import static io.openliberty.tools.langserver.lemminx.util.LibertyConstants.DEFAULT_LIBERTY_VERSION;
import static io.openliberty.tools.langserver.lemminx.util.LibertyConstants.LOCALE;
import static io.openliberty.tools.langserver.lemminx.util.LibertyConstants.VERSION;
import static io.openliberty.tools.langserver.lemminx.util.ResourceBundleUtil.findBestMatchingLocale;

/**
 * A utility to find the latest version of the Open Liberty feature list,
 * first by attempting an HTTP download and then by falling back to a local cache.
 */
public class LibertyVersionDownloadUtil {

    private LibertyVersionDownloadUtil() {
    }

    // The URL for the maven-metadata.xml file.
    private static String METADATA_URL = "https://repo1.maven.org/maven2/io/openliberty/features/open_liberty_featurelist/maven-metadata.xml";

    // The path to the local cache file, following the specified convention.
    // Base directory
    private static String CACHE_BASE_DIR = Paths.get(System.getProperty("user.home"), ".lemminx", "cache").toString();
    // Full file path
    private static String CACHE_FILE_PATH = Paths.get(CACHE_BASE_DIR, "https", "repo1.maven.org", "maven2", "io", "openliberty", "features", "open_liberty_featurelist", "maven-metadata.xml").toString();
    private static final Logger LOGGER = Logger.getLogger(LibertyVersionDownloadUtil.class.getName());
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 5000;

    /**
     * Attempts to get the latest version from either the remote repository or the local cache.
     *
     * @return The latest version string, or null if it cannot be found.
     */
    public static String getLatestVersionFromMetadata() {
        // Attempt to fetch from the remote URL first.
        String resourceContent = getResource(METADATA_URL, CACHE_FILE_PATH);
        String versionFromRemote = resourceContent != null ? parseVersionFromXml(resourceContent) : null;
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
     * fetches a resource from remote repository
     * if fetch is successful and cache path is present, file will be written to cache path as well
     *
     * @param resourceUrl   resource url
     * @param cacheFilePath cache location
     * @return resource content in string
     */
    public static String getResource(String resourceUrl, String cacheFilePath) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(resourceUrl))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String xmlContent = response.body();
                if (cacheFilePath != null) {
                    // Save the content to the local cache before parsing.
                    saveToLocalCache(cacheFilePath, xmlContent);
                }
                return xmlContent;
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
        Path filePath = Path.of(CACHE_FILE_PATH);
        if (Files.exists(filePath)) {
            LOGGER.fine("Found local cache file at: %s".formatted(filePath));
            try {
                // Read the file content and parse the XML.
                String xmlContent = Files.readString(filePath);
                return parseVersionFromXml(xmlContent);
            } catch (IOException e) {
                LOGGER.warning("Failed to read local cache file: " + e.getMessage());
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
     * @param cacheFilePath path to save file
     * @param xmlContent    The XML content to save.
     */
    private static void saveToLocalCache(String cacheFilePath, String xmlContent) {
        Path filePath = Paths.get(cacheFilePath);
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
     * Constructs a URL for downloading resources from Maven repository based on runtime version and locale.
     * 
     * @param url Base URL template with version placeholder
     * @param urlWithLocale URL template with version and locale placeholders
     * @return The constructed URL with appropriate version and locale
     */
    private static String parseURL(String url, String urlWithLocale) {
        final SettingsService settingsService = SettingsService.getInstance();
        final String rtVersion = settingsService.getLatestRuntimeVersion();
        
        // Default to using the base URL with just version replacement
        final Locale currentLocale = settingsService.getCurrentLocale();
        
        // Use base URL in these cases:
        // 1. No locale information available
        // 2. No URL with locale template provided
        // 3. Using English/US locale
        // 4. No matching locale found in available liberty locales
        if (currentLocale == null || 
            urlWithLocale == null || 
            Locale.US.equals(currentLocale) || 
            Locale.ENGLISH.equals(currentLocale) || 
            findBestMatchingLocale(currentLocale) == null) {
            return url.replace(VERSION, rtVersion != null ? rtVersion : DEFAULT_LIBERTY_VERSION);
        }
        
        // Special handling for specific locales that need full locale code
        if (Locale.TAIWAN.equals(currentLocale) || BRAZIL_PORTUGESE_LOCALE.equals(currentLocale)) {
            return urlWithLocale.replace(VERSION, rtVersion)
                               .replace(LOCALE, currentLocale.toString());
        }
        
        // For all other locales, use just the language code
        return urlWithLocale.replace(VERSION, rtVersion)
                           .replace(LOCALE, currentLocale.getLanguage());
    }

    /**
     * @param url           schema or featurelist url
     * @param urlWithLocale url with locale placeholder
     * @return downloaded resource path
     */
    public static Path downloadAndCacheLatestResource(String url, String urlWithLocale) {
        Path serverResourceFile = null;
        if (SettingsService.getInstance().getLatestRuntimeVersion() != null) {
            String resourceURL = parseURL(url, urlWithLocale);
            serverResourceFile = downloadWithRetry(resourceURL);
        }
        return serverResourceFile;
    }

    /**
     * Downloads a resource with retry capability.
     *
     * @param resourceURL The URL of the resource to download
     * @return Path to the downloaded or cached resource, or null if download failed
     */
    private static Path downloadWithRetry(String resourceURL) {
        int retryCount = 0;
        Path serverResourceFile = null;
        CacheResourcesManager resourceManager = new CacheResourcesManager();

        while (retryCount < MAX_RETRIES) {
            try {
                LOGGER.fine("Downloading resource: %s".formatted(resourceURL));
                // getResource first checks in cache and if not exist, downloads resources
                // lemminx library api to download and cache file
                serverResourceFile = resourceManager.getResource(resourceURL);
                return serverResourceFile; // Success - return immediately

            } catch (CacheResourceDownloadingException e) {
                // Only retry for RESOURCE_LOADING errors
                // API sometime return RESOURCE_LOADING error which will fix once resource is fully downloaded
                if (CacheResourceDownloadingException.CacheResourceDownloadingError.RESOURCE_LOADING.equals(e.getErrorCode())) {
                    retryCount++;
                    LOGGER.fine("Download in progress, retrying... (Attempt %s/%s)".formatted(retryCount, MAX_RETRIES));
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt(); // Restore interrupt status
                        logFallbackWarning("Download retry interrupted");
                        return null;
                    }
                } else {
                    // Other download errors - no retry
                    logFallbackWarning("Resource download failed: " + e.getMessage());
                    return null;
                }
            } catch (Exception e) {
                // Unexpected errors - no retry
                logFallbackWarning("Unexpected error during resource download: " + e.getMessage());
                return null;
            }
        }
        // Max retries reached without success
        logFallbackWarning("Resource download failed after " + MAX_RETRIES + " attempts");
        return null;
    }

    /**
     * log warning message
     * @param message message
     */
    private static void logFallbackWarning(String message) {
        LOGGER.warning(message);
        LOGGER.warning("Falling back to default version "+ DEFAULT_LIBERTY_VERSION);
    }
}