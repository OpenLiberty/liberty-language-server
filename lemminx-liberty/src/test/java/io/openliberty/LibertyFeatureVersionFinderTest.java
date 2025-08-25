package io.openliberty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import io.openliberty.tools.langserver.lemminx.util.DocumentUtil;
import io.openliberty.tools.langserver.lemminx.util.LibertyFeatureVersionFinder;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

public class LibertyFeatureVersionFinderTest {

    @TempDir
    Path tempDir;

    private Path cacheFilePath;
    private String originalCacheBaseDirValue;
    private MockedStatic<HttpClient> mockedStaticHttpClient;
    private  HttpClient httpClient;

    @BeforeEach
    public void setUp() throws Exception {
        // Save the original CACHE_BASE_DIR value
        Field cacheBaseDirField = LibertyFeatureVersionFinder.class.getDeclaredField("CACHE_BASE_DIR");
        cacheBaseDirField.setAccessible(true);
        originalCacheBaseDirValue = (String) cacheBaseDirField.get(null);

        // Set the CACHE_BASE_DIR to our temp directory for testing
        cacheBaseDirField.set(null, tempDir.toString());

        // Update the CACHE_FILE_PATH to use our temp directory
        Field cacheFilePathField = LibertyFeatureVersionFinder.class.getDeclaredField("CACHE_FILE_PATH");
        cacheFilePathField.setAccessible(true);
        String newCacheFilePath = tempDir.toString() + "/https/repo1.maven.org/maven2/io/openliberty/features/open_liberty_featurelist/maven-metadata.xml";
        cacheFilePathField.set(null, newCacheFilePath);

        // Store the cache file path for use in tests
        cacheFilePath = Paths.get(newCacheFilePath);

        // Create parent directories for the cache file
        Files.createDirectories(cacheFilePath.getParent());

        // Mock the static HttpClient methods
        mockedStaticHttpClient = mockStatic(HttpClient.class);
        // Create a mock for HttpClient and its Builder
        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpClient.Builder mockBuilder = mock(HttpClient.Builder.class);
        mockedStaticHttpClient.when(HttpClient::newBuilder).thenReturn(mockBuilder);
        when(mockBuilder.connectTimeout(any(Duration.class))).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockHttpClient);
        // Assign the mock to the instance variable for use in tests
        this.httpClient = mockHttpClient;

    }

    @AfterEach
    public void tearDown() throws Exception {
        // Restore the original CACHE_BASE_DIR value
        Field cacheBaseDirField = LibertyFeatureVersionFinder.class.getDeclaredField("CACHE_BASE_DIR");
        cacheBaseDirField.setAccessible(true);
        cacheBaseDirField.set(null, originalCacheBaseDirValue);

        // Restore the original CACHE_FILE_PATH
        Field cacheFilePathField = LibertyFeatureVersionFinder.class.getDeclaredField("CACHE_FILE_PATH");
        cacheFilePathField.setAccessible(true);
        String originalCacheFilePath = originalCacheBaseDirValue + "/https/repo1.maven.org/maven2/io/openliberty/features/open_liberty_featurelist/maven-metadata.xml";
        cacheFilePathField.set(null, originalCacheFilePath);
        mockedStaticHttpClient.close();
    }

    @Test
    public void testGetLatestVersionFromRemote() throws Exception {
        String expectedVersion = "25.0.0.7";
        String xmlContent = createMavenMetadataXml(expectedVersion);

        // Mock the HTTP response
        HttpResponse<String> mockResponse = (HttpResponse<String>) org.mockito.Mockito.mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(xmlContent);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandlers.ofString().getClass()))).thenReturn(mockResponse);


        // Call the method under test
        String actualVersion = LibertyFeatureVersionFinder.getLatestVersion();

        // Verify the result
        assertEquals(expectedVersion, actualVersion);

        // Verify the cache file was created
        assertTrue(Files.exists(cacheFilePath));
    }

    @Test
    public void testGetLatestVersionFromLocalCache() throws Exception {
        String expectedVersion = "25.0.0.6";
        String xmlContent = createMavenMetadataXml(expectedVersion);

        // Create a cache file
        Files.writeString(cacheFilePath, xmlContent);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandlers.ofString().getClass())))
                .thenThrow(new IOException("Simulated network error"));

        // Call the method under test
        String actualVersion = LibertyFeatureVersionFinder.getLatestVersion();

        // Verify the result
        assertEquals(expectedVersion, actualVersion);

    }

    @Test
    public void testGetLatestVersionBothRemoteAndCacheFail() throws Exception {
        // Mock HTTP client to throw an exception
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandlers.ofString().getClass())))
                .thenThrow(new IOException("Simulated network error"));

        // Ensure cache file doesn't exist
        Files.deleteIfExists(cacheFilePath);

        // Call the method under test
        String actualVersion = LibertyFeatureVersionFinder.getLatestVersion();

        // Verify the result
        assertNull(actualVersion);

    }

    @Test
    public void testHttpResponseNon200Status() throws Exception {
        // Mock the HTTP response with non-200 status
        HttpResponse<String> mockResponse = org.mockito.Mockito.mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(404);

        // This works because the second argument is of type HttpResponse.BodyHandler
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        // Ensure cache file doesn't exist
        Files.deleteIfExists(cacheFilePath);

        // Call the method under test
        String actualVersion = LibertyFeatureVersionFinder.getLatestVersion();

        // Verify the result
        assertNull(actualVersion);

    }

    @Test
    public void testParseVersionFromInvalidXml() throws Exception {
        String invalidXml = "<metadata><invalid>";

        // Create a cache file with invalid XML
        Files.writeString(cacheFilePath, invalidXml);

        // Mock DocumentUtil to throw an exception when parsing the XML
        try (MockedStatic<DocumentUtil> mockedDocumentUtil = mockStatic(DocumentUtil.class)) {
            mockedDocumentUtil.when(() -> DocumentUtil.getDocument(invalidXml))
                    .thenThrow(new Exception("Invalid XML"));

            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandlers.ofString().getClass())))
                    .thenThrow(new IOException("Simulated network error"));

                // Call the method under test
                String actualVersion = LibertyFeatureVersionFinder.getLatestVersion();

                // Verify the result
                assertNull(actualVersion);
            }

    }

    @Test
    public void testParseVersionFromXmlWithoutLatestTag() throws Exception {
        String xmlWithoutLatestTag = "<metadata><versioning><versions><version>25.0.0.6</version></versions></versioning></metadata>";

        // Mock the HTTP response
        HttpResponse<String> mockResponse = (HttpResponse<String>) org.mockito.Mockito.mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(xmlWithoutLatestTag);

        // Mock the Document and NodeList
        Document mockDocument = org.mockito.Mockito.mock(Document.class);
        Element mockElement = org.mockito.Mockito.mock(Element.class);
        NodeList emptyNodeList = org.mockito.Mockito.mock(NodeList.class);

        when(mockDocument.getDocumentElement()).thenReturn(mockElement);
        when(mockDocument.getElementsByTagName("latest")).thenReturn(emptyNodeList);
        when(emptyNodeList.getLength()).thenReturn(0);

        // Mock DocumentUtil
        try (MockedStatic<DocumentUtil> mockedDocumentUtil = mockStatic(DocumentUtil.class)) {
            mockedDocumentUtil.when(() -> DocumentUtil.getDocument(xmlWithoutLatestTag))
                    .thenReturn(mockDocument);

            // Mock the HttpClient
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandlers.ofString().getClass())))
                    .thenReturn(mockResponse);

            // Call the method under test
            String actualVersion = LibertyFeatureVersionFinder.getLatestVersion();

            // Verify the result
            assertNull(actualVersion);

        }
    }

    private String createMavenMetadataXml(String version) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<metadata>\n" +
                "  <groupId>io.openliberty.features</groupId>\n" +
                "  <artifactId>open_liberty_featurelist</artifactId>\n" +
                "  <versioning>\n" +
                "    <latest>" + version + "</latest>\n" +
                "    <release>" + version + "</release>\n" +
                "    <versions>\n" +
                "      <version>" + version + "</version>\n" +
                "    </versions>\n" +
                "    <lastUpdated>20240825000000</lastUpdated>\n" +
                "  </versioning>\n" +
                "</metadata>";
    }
}

